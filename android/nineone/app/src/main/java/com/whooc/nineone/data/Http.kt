package com.whooc.nineone.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Session cookies survive process death. The 91 backend issues an HttpOnly
 * `vs_admin` cookie with a 7-day TTL, and every `api` plus `p` route depends
 * on it — including the media requests ExoPlayer makes.
 *
 * NOTE: never write a bare slash-star sequence inside a Kotlin KDoc — Kotlin
 * block comments nest, so a path like `/api/` followed by a star silently
 * swallows the rest of the file.
 */
class PersistentCookieJar(context: Context) : CookieJar {

    @Serializable
    private data class Entry(val url: String, val cookie: String)

    private val file = File(context.filesDir, "session-cookies.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    /**
     * Keyed by `domain|path|name`, not by the raw cookie text.
     *
     * The backend renews the session cookie whenever less than half its 7-day
     * TTL is left, re-sending the same name with a new expiry. Keying on the
     * whole serialised cookie would file that as a *second* cookie, and every
     * later request would carry `vs_admin` twice — so the key has to be the
     * cookie's identity, and a renewal has to replace the previous row.
     */
    private val entries = LinkedHashMap<String, Entry>()

    init {
        runCatching {
            if (file.exists()) {
                val stored = json.decodeFromString<List<Entry>>(file.readText())
                stored.forEach { entry ->
                    if (entry.cookie.isBlank()) return@forEach
                    val parsed = parse(entry.url, entry.cookie) ?: return@forEach
                    entries[keyOf(parsed)] = entry
                }
                // Older builds keyed the store by the whole cookie string, so a
                // file written back then can hold a dozen rows all named
                // `vs_admin` for the same host. Collapsing them here (the map
                // already did) is not enough — the file has to be rewritten,
                // otherwise the next launch loads the duplicates again and every
                // request goes out with `Cookie: vs_admin=…; vs_admin=…; …`.
                // Go's r.Cookie() only reads the first one, so the request 401s
                // whenever that first entry happens to be a dead token.
                if (entries.size != stored.size) persist()
            }
        }
    }

    private fun parse(url: String, raw: String): Cookie? =
        url.toHttpUrlOrNull()?.let { Cookie.parse(it, raw) }

    private fun keyOf(cookie: Cookie) = "${cookie.domain}|${cookie.path}|${cookie.name}"

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            var changed = false
            cookies.forEach { cookie ->
                if (cookie.expiresAt <= System.currentTimeMillis()) return@forEach
                val raw = cookie.toString()
                val key = keyOf(cookie)
                if (entries[key]?.cookie != raw) {
                    entries[key] = Entry(url.toString(), raw)
                    changed = true
                }
            }
            if (changed) persist()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        // Keyed by name so a request can never carry the same cookie twice.
        // The 91 backend only ever issues one cookie (`vs_admin`, path `/`),
        // and Go's `r.Cookie()` silently reads just the first match — a second
        // entry with the same name is therefore not a harmless duplicate but a
        // coin flip on whether the request is authenticated.
        val out = LinkedHashMap<String, Cookie>()
        synchronized(lock) {
            val stale = mutableListOf<String>()
            entries.forEach { (key, entry) ->
                val cookie = parse(entry.url, entry.cookie)
                when {
                    cookie == null -> stale += key
                    cookie.expiresAt <= now -> stale += key
                    cookie.matches(url) -> {
                        val kept = out[cookie.name]
                        if (kept == null || cookie.expiresAt >= kept.expiresAt) {
                            out[cookie.name] = cookie
                        }
                    }
                }
            }
            if (stale.isNotEmpty()) {
                stale.forEach { entries.remove(it) }
                persist()
            }
        }
        return out.values.toList()
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            persist()
        }
    }

    fun hasSession(): Boolean = synchronized(lock) { entries.isNotEmpty() }

    private fun persist() {
        runCatching { file.writeText(json.encodeToString(entries.values.toList())) }
    }
}

/** Shared OkHttp stack: one cookie jar for the API and for media playback. */
object Http {

    lateinit var cookies: PersistentCookieJar
        private set

    lateinit var client: OkHttpClient
        private set

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    fun init(ctx: Context) {
        if (::client.isInitialized) return
        cookies = PersistentCookieJar(ctx.applicationContext)
        client = OkHttpClient.Builder()
            .cookieJar(cookies)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
