package com.whooc.nineone.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicBoolean

sealed interface Session {
    data object Unknown : Session
    data object LoggedOut : Session
    data class LoggedIn(val role: String) : Session
}

class ApiException(val code: Int, message: String) : Exception(message) {
    val isUnauthorized: Boolean get() = code == 401 || code == 403
}

/**
 * Thin, dependency-free client over the 91 REST API.
 *
 * Deliberately hand-rolled instead of Retrofit: every call needs the current
 * (mutable) base URL and a cookie-aware OkHttp stack that is *shared* with
 * ExoPlayer, and there are only ~15 endpoints.
 */
object Api {

    private val client get() = Http.client
    private val json get() = Http.json

    private val _session = MutableStateFlow<Session>(Session.Unknown)
    val session: StateFlow<Session> = _session.asStateFlow()

    /** Last failed call, for the diagnostics card in 设置. */
    private val _lastProblem = MutableStateFlow<String?>(null)
    val lastProblem: StateFlow<String?> = _lastProblem.asStateFlow()

    /**
     * True once a session has been confirmed valid at least once in this
     * process. The login screen uses it to tell "I was kicked out mid-session"
     * (worth re-checking, the backend has lied about this) apart from "I opened
     * the app with a dead cookie" (nothing to re-check, show the form at once).
     */
    @Volatile
    var confirmedThisRun: Boolean = false
        private set

    // ---------------------------------------------------------------- plumbing

    private class Resp(val code: Int, val body: String)

    private val revalidateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val revalidating = AtomicBoolean(false)

    private fun execute(request: Request): Resp {
        client.newCall(request).execute().use { response ->
            return Resp(response.code, response.body?.string().orEmpty())
        }
    }

    /**
     * Decide — off the caller's thread — whether a 401 really means the session
     * is over. Single-flight: a burst of 401s triggers one re-check, not ten.
     */
    private fun revalidateInBackground() {
        if (!revalidating.compareAndSet(false, true)) return
        revalidateScope.launch {
            try {
                refreshSession()
            } catch (t: Throwable) {
                // refreshSession already swallows everything it can; this is
                // only here so a surprise cannot kill the scope.
            } finally {
                revalidating.set(false)
            }
        }
    }

    private fun note(resp: Resp, what: String) {
        _lastProblem.value = "$what · HTTP ${resp.code}"
    }

    private fun requireOk(resp: Resp, what: String): String {
        if (resp.code in 200..299) return resp.body

        // Deliberately *not* logging out here. A single 401 used to park the
        // user on the login screen until the app was restarted, even though the
        // stored cookie was still good — the 91 backend answered 401 for valid
        // sessions while its SQLite wal-index was stale, and it also answers
        // 401 for transient failures. The verdict is delegated to
        // refreshSession(), which asks /admin/api/me twice before demoting.
        // 403 ("authenticated but not allowed") never ends a session either.
        if (resp.code == 401 && _session.value is Session.LoggedIn) {
            revalidateInBackground()
        }

        note(resp, what)
        val detail = runCatching { json.decodeFromString<ApiError>(resp.body).message }
            .getOrNull()?.takeIf { it.isNotBlank() }
        throw ApiException(resp.code, detail ?: "$what 失败 · HTTP ${resp.code}")
    }

    private fun builder(path: String, vararg query: Pair<String, String?>): HttpUrl.Builder {
        val url = (Prefs.serverUrl.trimEnd('/') + path).toHttpUrl()
        val b = url.newBuilder()
        query.forEach { (k, v) -> if (!v.isNullOrBlank()) b.addQueryParameter(k, v) }
        return b
    }

    private fun get(path: String, vararg query: Pair<String, String?>): Resp =
        execute(Request.Builder().url(builder(path, *query).build()).get().build())

    private fun postJson(path: String, body: String): Resp =
        execute(
            Request.Builder()
                .url(builder(path).build())
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
        )

    private fun post(path: String): Resp =
        execute(
            Request.Builder()
                .url(builder(path).build())
                .post(ByteArray(0).toRequestBody(null))
                .build()
        )

    private fun putJson(path: String, body: String): Resp =
        execute(
            Request.Builder()
                .url(builder(path).build())
                .put(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
        )

    private fun delete(path: String): Resp =
        execute(Request.Builder().url(builder(path).build()).delete().build())

    // `block` is a suspend lambda so callers can wait between retries; every
    // existing body stays valid because a non-suspend body is a valid suspend
    // one.
    private suspend fun <T> io(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { block() }

    // --------------------------------------------------------------- endpoints

    /** One authoritative read of `GET /admin/api/me`. */
    private fun me(): Session {
        val resp = get("/admin/api/me")
        return when {
            // A bare 401 is not enough on its own; the caller cross-checks.
            resp.code == 401 -> Session.LoggedOut
            resp.code !in 200..299 -> throw ApiException(resp.code, "会话复核失败")
            else -> {
                val info = json.decodeFromString<SessionInfo>(resp.body)
                if (info.authenticated) Session.LoggedIn(info.role) else Session.LoggedOut
            }
        }
    }

    /**
     * `GET /admin/api/me` — the authoritative session check, used on launch and
     * by the self-heal path behind the login screen.
     *
     * The 91 backend has been observed answering 401 for cookies that were
     * perfectly valid — while its SQLite wal-index was stale, `GetSession`
     * missed rows that existed in the database, so `validateSession` reported
     * "not found" and every protected route replied `401 unauthorized`. A
     * timeout, a 503, or a single 401 must therefore never be enough to end the
     * session; only two consecutive answers that the account is *not*
     * authenticated will.
     */
    suspend fun refreshSession(): Session = io {
        val current = _session.value
        var last: Session? = null
        repeat(2) { attempt ->
            val answer = try {
                me()
            } catch (t: Throwable) {
                null
            }
            if (answer is Session.LoggedIn) {
                confirmedThisRun = true
                _session.value = answer
                return@io answer
            }
            if (answer != null) last = answer
            if (attempt == 0) delay(700)
        }
        // Either both probes said "logged out", or we never got an answer at
        // all — in which case an existing login is kept rather than destroyed.
        val result = last ?: current.keepOrLoggedOut()
        _session.value = result
        result
    }

    /** Keep an existing login across a transient failure; cold start → logged out. */
    private fun Session.keepOrLoggedOut(): Session =
        if (this is Session.LoggedIn) this else Session.LoggedOut

    /** True when a session cookie is still on disk, i.e. a login may be worth re-checking. */
    fun hasStoredCookie(): Boolean = runCatching { Http.cookies.hasSession() }.getOrDefault(false)

    /**
     * `POST /admin/api/login`. The response sets the HttpOnly session cookie,
     * which the persistent cookie jar picks up automatically.
     */
    suspend fun login(username: String, password: String): String = io {
        val payload = json.encodeToString(mapOf("username" to username, "password" to password))
        val resp = postJson("/admin/api/login", payload)
        when {
            resp.code == 428 -> throw ApiException(resp.code, "服务端尚未初始化管理员，请先在网页端完成初始化")
            resp.code == 403 -> throw ApiException(resp.code, "账号或 IP 已被封禁")
            resp.code == 400 -> throw ApiException(resp.code, "用户名或密码错误")
        }
        val result = runCatching { json.decodeFromString<LoginResult>(requireOk(resp, "登录")) }
            .getOrElse { throw ApiException(resp.code, "登录响应无法解析") }
        val role = result.role
        confirmedThisRun = true
        _session.value = Session.LoggedIn(role)
        role
    }

    suspend fun logout() = io {
        runCatching { post("/admin/api/logout") }
        Http.cookies.clear()
        // An explicit logout must land on the login form immediately — no
        // self-heal probe, nothing left to find.
        confirmedThisRun = false
        _session.value = Session.LoggedOut
    }

    /** Public endpoint, no login required. */
    suspend fun theme(): String = io {
        val info = json.decodeFromString<ThemeInfo>(requireOk(get("/api/settings/theme"), "主题"))
        Prefs.serverTheme = info.theme
        info.theme
    }

    /** Public endpoint. Whether the server has teaser previews turned on. */
    suspend fun previewEnabled(): Boolean = io {
        runCatching {
            json.decodeFromString<PreviewSettings>(
                requireOk(get("/api/settings/preview"), "预览设置")
            ).previewEnabled
        }.getOrDefault(true)
    }

    suspend fun home(count: Int = 12): List<Video> = io {
        json.decodeFromString(requireOk(get("/api/home", "count" to count.toString()), "首页"))
    }

    suspend fun homeLatest(count: Int = 12): List<Video> = io {
        json.decodeFromString(requireOk(get("/api/home/latest", "count" to count.toString()), "最新"))
    }

    suspend fun list(
        page: Int = 1,
        size: Int = 24,
        q: String? = null,
        tag: String? = null,
        sort: String? = null
    ): ListPage = io {
        json.decodeFromString(
            requireOk(
                get(
                    "/api/list",
                    "page" to page.toString(),
                    "size" to size.toString(),
                    "q" to q,
                    "tag" to tag,
                    "sort" to sort
                ),
                "列表"
            )
        )
    }

    /**
     * `/api/feed` — a snapshot feed. `kind` is mandatory; the server answers
     * `400 invalid video feed kind` without it. Valid values:
     *   * `listing`   — honours `q` / `tag` / `sort`
     *   * `latest`    — newest first, thumbnails-ready preferred
     *   * `recommend` — shuffled
     */
    suspend fun feed(
        count: Int = 24,
        cursor: Int = 0,
        feedToken: String? = null,
        kind: String = "listing",
        q: String? = null,
        tag: String? = null,
        sort: String? = null
    ): FeedPage = io {
        json.decodeFromString(
            requireOk(
                get(
                    "/api/feed",
                    "kind" to kind,
                    "count" to count.toString(),
                    "cursor" to cursor.toString(),
                    "feedToken" to feedToken,
                    "q" to q,
                    "tag" to tag,
                    "sort" to sort
                ),
                "内容流"
            )
        )
    }

    suspend fun shortsNext(
        count: Int = 6,
        cursor: Int = 0,
        feedToken: String? = null
    ): ShortsPage = io {
        json.decodeFromString(
            requireOk(
                get(
                    "/api/shorts/next",
                    "count" to count.toString(),
                    "cursor" to cursor.toString(),
                    "feedToken" to feedToken
                ),
                "短视频"
            )
        )
    }

    suspend fun video(id: String): VideoDetail = io {
        json.decodeFromString(requireOk(get("/api/video/$id"), "视频详情"))
    }

    suspend fun recommendations(id: String): List<Video> = io {
        json.decodeFromString(requireOk(get("/api/video/$id/recommendations"), "相关推荐"))
    }

    suspend fun tags(): List<Tag> = io {
        json.decodeFromString(requireOk(get("/api/tags"), "标签"))
    }

    suspend fun subtitles(id: String): List<Subtitle> = io {
        json.decodeFromString(requireOk(get("/api/video/$id/subtitles"), "字幕"))
    }

    suspend fun markViewed(id: String): Int = io {
        val result = runCatching {
            json.decodeFromString<ViewsResult>(requireOk(post("/api/video/$id/view"), "计数"))
        }.getOrNull()
        result?.views ?: 0
    }

    suspend fun like(id: String, liked: Boolean): Int = io {
        val resp = if (liked) post("/api/video/$id/like") else delete("/api/video/$id/like")
        runCatching {
            json.decodeFromString<LikesResult>(requireOk(resp, "点赞"))
        }.getOrNull()?.likes ?: 0
    }

    /** Full URL for a drive-backed stream, ready for ExoPlayer. */
    fun streamUrl(videoSrc: String): String = MediaUrls.resolve(videoSrc)
}
