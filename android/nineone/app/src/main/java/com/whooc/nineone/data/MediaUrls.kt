package com.whooc.nineone.data

/**
 * The 91 backend answers with root-relative URLs (`/p/thumb/xxx`,
 * `/p/stream/drive/file`, `/api/...`). Everything must be turned into an
 * absolute URL against the *current* server before it reaches OkHttp, Coil or
 * ExoPlayer — and the server can be changed at runtime, so resolve lazily.
 */
object MediaUrls {

    fun resolve(path: String?): String {
        val p = path?.trim().orEmpty()
        if (p.isEmpty()) return ""
        if (p.startsWith("http://", true) || p.startsWith("https://", true)) return p
        val base = Prefs.serverUrl.trimEnd('/')
        return if (p.startsWith("/")) base + p else "$base/$p"
    }

    /** True when the value already points at a host we do not control. */
    fun isExternal(path: String?): Boolean {
        val p = path?.trim().orEmpty()
        return p.startsWith("http://", true) || p.startsWith("https://", true)
    }
}
