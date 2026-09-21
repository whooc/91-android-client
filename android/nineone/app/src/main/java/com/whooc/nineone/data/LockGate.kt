package com.whooc.nineone.data

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Whether the local gate is currently in the way.
 *
 * Deliberately not a [Prefs] entry: the preference records that a code *exists*,
 * this records that it has been *entered*. It is per-process state and resets on
 * every launch.
 *
 * The grace window exists because plenty of ordinary actions briefly push the app
 * into the background — the photo picker, a system dialog, glancing at a
 * notification. Re-locking on each of those makes the feature feel broken, so the
 * gate only re-arms once the app has been away for [GRACE_MS].
 */
object LockGate {

    private const val GRACE_MS = 30_000L
    private const val DEFAULT_SUPPRESS_MS = 120_000L

    private var lockedState by mutableStateOf(false)
    val locked: Boolean get() = lockedState

    private var awaySince = 0L
    private var suppressUntil = 0L

    /** Called once at startup, after [Prefs] and [AppLock] are usable. */
    fun sync() {
        lockedState = AppLock.isEnabled
        awaySince = 0L
        suppressUntil = 0L
    }

    fun onBackground() {
        awaySince = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        if (!AppLock.isEnabled) {
            lockedState = false
            return
        }
        val since = awaySince
        awaySince = 0L
        // Zero means we were never told the app went away — first launch, or a
        // stop that never happened. Nothing to re-arm from.
        if (since == 0L) return
        if (lockedState) return
        if (SystemClock.elapsedRealtime() < suppressUntil) return
        if (SystemClock.elapsedRealtime() - since >= GRACE_MS) lockedState = true
    }

    fun unlock() {
        lockedState = false
        awaySince = 0L
    }

    fun lockNow() {
        if (AppLock.isEnabled) lockedState = true
    }

    /**
     * Stops the gate from re-arming for a while. Used around anything that hands
     * the screen to another app and comes back on the user's schedule — picking a
     * logo can take minutes, and returning straight into a lock screen would look
     * like the app had thrown the choice away.
     */
    fun suppress(durationMs: Long = DEFAULT_SUPPRESS_MS) {
        suppressUntil = SystemClock.elapsedRealtime() + durationMs
    }
}
