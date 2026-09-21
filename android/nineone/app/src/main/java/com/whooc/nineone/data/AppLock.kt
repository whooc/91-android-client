package com.whooc.nineone.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The local app-entry gate.
 *
 * Stored as a salted PBKDF2 hash in SharedPreferences — never the code itself,
 * and nothing that can be replayed against the 91 server. This is a device
 * lock, not an account credential, and it is checked entirely offline.
 *
 * `PBKDF2WithHmacSHA256` only exists from API 26 while minSdk here is 24, so
 * the algorithm actually used is recorded next to the hash and reused when
 * verifying. Without that, a device that set its code on API 24 would fail to
 * unlock the moment it was upgraded past 26.
 */
object AppLock {

    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16

    private const val ALGO_SHA256 = "PBKDF2WithHmacSHA256"
    private const val ALGO_SHA1 = "PBKDF2WithHmacSHA1"

    /** Anything shorter is not worth the screen it is typed on. */
    const val MIN_LENGTH = 4

    val isEnabled: Boolean
        get() = Prefs.appLockEnabled && Prefs.appLockHash.isNotEmpty()

    fun tooShort(code: String): Boolean = code.length < MIN_LENGTH

    fun set(code: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val algo = availableAlgorithm()
        val hash = derive(algo, code, salt, ITERATIONS)
        Prefs.appLockAlgo = algo
        Prefs.appLockSalt = encode(salt)
        Prefs.appLockHash = encode(hash)
        Prefs.appLockEnabled = true
    }

    /**
     * Compared with [MessageDigest.isEqual] so the answer does not leak how many
     * leading bytes were right.
     */
    fun verify(code: String): Boolean {
        if (!isEnabled) return true
        if (code.isEmpty()) return false

        val salt = decode(Prefs.appLockSalt)
        val expected = decode(Prefs.appLockHash)
        if (salt == null || expected == null || salt.isEmpty() || expected.isEmpty()) return false

        val algo = Prefs.appLockAlgo.ifBlank { ALGO_SHA256 }
        val actual = runCatching { derive(algo, code, salt, ITERATIONS, expected.size * 8) }
            .getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    fun clear() {
        Prefs.appLockEnabled = false
        Prefs.appLockHash = ""
        Prefs.appLockSalt = ""
        Prefs.appLockAlgo = ""
    }

    private fun availableAlgorithm(): String = runCatching {
        SecretKeyFactory.getInstance(ALGO_SHA256)
        ALGO_SHA256
    }.getOrDefault(ALGO_SHA1)

    private fun derive(
        algo: String,
        code: String,
        salt: ByteArray,
        iterations: Int,
        bits: Int = 256
    ): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), salt, iterations, bits)
        return try {
            SecretKeyFactory.getInstance(algo).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray? =
        runCatching { Base64.decode(text, Base64.NO_WRAP) }.getOrNull()
}
