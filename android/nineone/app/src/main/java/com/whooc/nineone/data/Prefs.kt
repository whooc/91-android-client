package com.whooc.nineone.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Local preferences. The server address is user configurable so the same APK
 * works against the LAN address, the relay address or a domain name.
 */
object Prefs {

    private const val FILE = "nineone_prefs"
    private const val KEY_SERVER = "server_url"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_WEB_THEME = "web_theme"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_AUTOPLAY_SHORTS = "autoplay_shorts"
    private const val KEY_PLAYBACK_SPEED = "playback_speed"
    private const val KEY_LAST_TAB = "last_tab"
    private const val KEY_GLOBAL_MUTED = "global_muted"

    // Local gate. Only the salted hash is stored — see AppLock.
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_APP_LOCK_HASH = "app_lock_hash"
    private const val KEY_APP_LOCK_SALT = "app_lock_salt"
    private const val KEY_APP_LOCK_ALGO = "app_lock_algo"

    /** Whether the server session is dropped when the app is closed. */
    private const val KEY_LOGOUT_ON_EXIT = "logout_on_exit"

    // Branding. The logo is a path inside filesDir, not a content:// URI.
    private const val KEY_BRAND_NAME = "brand_name"
    private const val KEY_BRAND_LOGO = "brand_logo_file"

    /**
     * Deliberately empty. The APK is meant to be handed to other people, so it
     * must not carry the author's own relay/LAN addresses — no preset list, no
     * prefilled host. Whoever installs it types their own 91 server.
     */
    const val DEFAULT_SERVER = ""

    /** Shown as the field placeholder; never written into prefs. */
    const val SERVER_HINT = "http://192.168.1.10:9191"

    private lateinit var sp: SharedPreferences

    /**
     * Mute is mirrored into a Compose state so the floating shell button, the
     * shorts feed and the full player all recompose together — a plain
     * SharedPreferences read would leave the other two stale.
     */
    private var mutedState by mutableStateOf(false)

    fun init(ctx: Context) {
        if (!::sp.isInitialized) {
            sp = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            mutedState = sp.getBoolean(KEY_GLOBAL_MUTED, false)
        }
    }

    var serverUrl: String
        get() = sp.getString(KEY_SERVER, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) {
            sp.edit().putString(KEY_SERVER, normalize(value)).apply()
        }

    /** `follow` mirrors the server theme, anything else pins a palette. */
    var themeMode: String
        get() = sp.getString(KEY_THEME_MODE, "follow") ?: "follow"
        set(value) {
            sp.edit().putString(KEY_THEME_MODE, value).apply()
        }

    /** Last theme reported by `GET /api/settings/theme`. */
    var serverTheme: String
        get() = sp.getString(KEY_WEB_THEME, "dark") ?: "dark"
        set(value) {
            sp.edit().putString(KEY_WEB_THEME, value).apply()
        }

    var keepScreenOn: Boolean
        get() = sp.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) {
            sp.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
        }

    var autoplayShorts: Boolean
        get() = sp.getBoolean(KEY_AUTOPLAY_SHORTS, true)
        set(value) {
            sp.edit().putBoolean(KEY_AUTOPLAY_SHORTS, value).apply()
        }

    var playbackSpeed: Float
        get() = sp.getFloat(KEY_PLAYBACK_SPEED, 1f)
        set(value) {
            sp.edit().putFloat(KEY_PLAYBACK_SPEED, value).apply()
        }

    var lastTab: Int
        get() = sp.getInt(KEY_LAST_TAB, 0)
        set(value) {
            sp.edit().putInt(KEY_LAST_TAB, value).apply()
        }

    /** App-wide mute, driven by the floating button in the shell. */
    var globalMuted: Boolean
        get() = mutedState
        set(value) {
            mutedState = value
            sp.edit().putBoolean(KEY_GLOBAL_MUTED, value).apply()
        }

    // ------------------------------------------------------------- local gate

    var appLockEnabled: Boolean
        get() = sp.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) {
            sp.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()
        }

    var appLockHash: String
        get() = sp.getString(KEY_APP_LOCK_HASH, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_APP_LOCK_HASH, value).apply()
        }

    var appLockSalt: String
        get() = sp.getString(KEY_APP_LOCK_SALT, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_APP_LOCK_SALT, value).apply()
        }

    /** Which PBKDF2 variant produced [appLockHash]; see AppLock for why. */
    var appLockAlgo: String
        get() = sp.getString(KEY_APP_LOCK_ALGO, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_APP_LOCK_ALGO, value).apply()
        }

    // ---------------------------------------------------------------- session

    /**
     * Off by default, matching the behaviour the client has always had: the
     * server's 7-day cookie keeps you signed in across restarts. Turning it on
     * means closing the app ends the session on this device.
     */
    var logoutOnExit: Boolean
        get() = sp.getBoolean(KEY_LOGOUT_ON_EXIT, false)
        set(value) {
            sp.edit().putBoolean(KEY_LOGOUT_ON_EXIT, value).apply()
        }

    // ------------------------------------------------------------------ brand

    var brandName: String
        get() = sp.getString(KEY_BRAND_NAME, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_BRAND_NAME, value).apply()
        }

    /** Absolute path inside filesDir, or empty when the default mark is used. */
    var brandLogoFile: String
        get() = sp.getString(KEY_BRAND_LOGO, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_BRAND_LOGO, value).apply()
        }

    /** Accepts "host", "host:port", "http://host:port" and normalises it. */
    fun normalize(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        return s.trimEnd('/')
    }
}
