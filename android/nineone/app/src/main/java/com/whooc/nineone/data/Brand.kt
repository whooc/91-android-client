package com.whooc.nineone.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * The user-visible name and logo.
 *
 * Held as Compose state rather than read straight from SharedPreferences so that
 * changing the name in 设置 repaints the header, the login screen and the profile
 * card at once, instead of on the next launch.
 *
 * The picked logo is copied into the app's private files directory rather than
 * keeping the `content://` URI: a URI handed out by the picker can be revoked
 * when the process is recreated, and it may point at something the user later
 * deletes or moves.
 */
object Brand {

    const val DEFAULT_NAME = "91"

    /** 512 px is comfortably above what any of the in-app slots draw. */
    private const val LOGO_PX = 512
    private const val DIR = "brand"

    private var nameState by mutableStateOf(DEFAULT_NAME)
    private var logoState by mutableStateOf<File?>(null)

    /** Never blank — an empty field falls back to the default rather than nothing. */
    val name: String get() = nameState

    val logo: File? get() = logoState

    fun init(ctx: Context) {
        nameState = Prefs.brandName.ifBlank { DEFAULT_NAME }
        logoState = storedLogo()
    }

    fun setName(value: String) {
        val trimmed = value.trim()
        Prefs.brandName = trimmed
        nameState = trimmed.ifBlank { DEFAULT_NAME }
    }

    private fun storedLogo(): File? {
        val path = Prefs.brandLogoFile
        if (path.isBlank()) return null
        val file = File(path)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    private fun dir(ctx: Context) = File(ctx.filesDir, DIR)

    /**
     * Copies the picked image in, downscaled and centre-cropped to a square.
     *
     * Decoding is done in two passes with `inJustDecodeBounds` first — a modern
     * phone camera JPEG is large enough that decoding it whole just to throw
     * almost all of it away will happily OOM.
     */
    fun importLogo(ctx: Context, uri: Uri): Boolean {
        return try {
            val resolver = ctx.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= LOGO_PX &&
                bounds.outHeight / (sample * 2) >= LOGO_PX
            ) {
                sample *= 2
            }

            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )
            } ?: return false

            val side = minOf(decoded.width, decoded.height)
            if (side <= 0) return false

            val square = Bitmap.createBitmap(
                decoded,
                (decoded.width - side) / 2,
                (decoded.height - side) / 2,
                side,
                side
            )
            val scaled = Bitmap.createScaledBitmap(square, LOGO_PX, LOGO_PX, true)

            // A fresh filename every time: Coil keys its cache on the model, so
            // overwriting one path would keep serving the previous logo.
            val target = File(dir(ctx).apply { mkdirs() }, "logo-${System.currentTimeMillis()}.png")
            target.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }

            Prefs.brandLogoFile = target.absolutePath
            logoState = target
            pruneLogos(ctx, keep = target)
            true
        } catch (t: Throwable) {
            false
        }
    }

    fun clearLogo(ctx: Context) {
        Prefs.brandLogoFile = ""
        logoState = null
        pruneLogos(ctx, keep = null)
    }

    private fun pruneLogos(ctx: Context, keep: File?) {
        runCatching {
            dir(ctx).listFiles()?.forEach { file ->
                if (file.absolutePath != keep?.absolutePath) file.delete()
            }
        }
    }
}
