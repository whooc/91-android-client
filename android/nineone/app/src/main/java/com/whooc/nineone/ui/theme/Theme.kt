package com.whooc.nineone.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.whooc.nineone.data.Prefs

/**
 * The 91 design tokens, mirrored 1:1 from `src/styles/tokens.css`.
 *
 * The web app has three themes and switches them by writing `data-theme` onto
 * `<html>`. We reproduce the same three here so the native UI is visually the
 * same product, not a lookalike.
 */
enum class AppTheme(val key: String, val label: String, val isLight: Boolean) {
    DARK("dark", "暗黑 + 暖橙", false),
    PINK("pink", "奶油白 + 樱花粉", true),
    SKY("sky", "星空蓝 + 暖星黄", true);

    companion object {
        fun of(key: String?): AppTheme = entries.firstOrNull { it.key == key } ?: DARK
    }
}

/**
 * Extra tokens Material3 has no slot for. Components read them via
 * [LocalTokens] so screens can style things like the page grid background.
 */
data class Tokens(
    val bgPage: Color,
    val bgElevated: Color,
    val bgSunken: Color,
    val textStrong: Color,
    val textDefault: Color,
    val textMuted: Color,
    val textFaint: Color,
    val divider: Color,
    val accent: Color,
    /** Accent used as *foreground* — the pure sky yellow is unreadable on white. */
    val accentText: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val isLight: Boolean
)

val LocalTokens = staticCompositionLocalOf { darkTokens }

// --------------------------------------------------------------------- dark

private val DarkTokens = Tokens(
    bgPage = Color(0xFF0B0C10),
    bgElevated = Color(0xFF1B1E26),
    bgSunken = Color(0xFF0A0B0F),
    textStrong = Color(0xFFF5F5F7),
    textDefault = Color(0xFFD8D9DE),
    textMuted = Color(0xFF9A9BA4),
    textFaint = Color(0xFF6C6E78),
    divider = Color(0x17FFFFFF),
    accent = Color(0xFFFF8A3C),
    accentText = Color(0xFFFF8A3C),
    onAccent = Color(0xFF1A0F04),
    success = Color(0xFF3FCF8E),
    warning = Color(0xFFF5B54A),
    danger = Color(0xFFF1556C),
    info = Color(0xFF5AA2FF),
    isLight = false
)

private val darkTokens: Tokens get() = DarkTokens

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = DarkTokens.accent,
    onPrimary = DarkTokens.onAccent,
    primaryContainer = Color(0xFF3A2410),
    onPrimaryContainer = Color(0xFFFFD9BC),
    secondary = DarkTokens.accent,
    onSecondary = DarkTokens.onAccent,
    background = DarkTokens.bgPage,
    onBackground = DarkTokens.textStrong,
    surface = Color(0xFF14161C),
    onSurface = DarkTokens.textStrong,
    surfaceVariant = DarkTokens.bgElevated,
    onSurfaceVariant = DarkTokens.textDefault,
    surfaceContainer = Color(0xFF14161C),
    surfaceContainerHigh = DarkTokens.bgElevated,
    surfaceContainerLow = Color(0xFF101218),
    surfaceContainerLowest = DarkTokens.bgSunken,
    outline = Color(0x2EFFFFFF),
    outlineVariant = DarkTokens.divider,
    error = DarkTokens.danger,
    onError = Color(0xFF2A0A10)
)

// --------------------------------------------------------------------- pink

private val PinkTokens = Tokens(
    bgPage = Color(0xFFFFF5F7),
    bgElevated = Color(0xFFFFE9EE),
    bgSunken = Color(0xFFFBEAEF),
    textStrong = Color(0xFF2A1820),
    textDefault = Color(0xFF4A3A44),
    textMuted = Color(0xFF8A6E78),
    textFaint = Color(0xFFB7A3AC),
    divider = Color(0x2EFF5B8A),
    accent = Color(0xFFFF5B8A),
    accentText = Color(0xFFF43D75),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF1EA974),
    warning = Color(0xFFD99022),
    danger = Color(0xFFE43B5C),
    info = Color(0xFF3479D6),
    isLight = true
)

private val PinkScheme: ColorScheme = lightColorScheme(
    primary = PinkTokens.accent,
    onPrimary = PinkTokens.onAccent,
    primaryContainer = PinkTokens.bgElevated,
    onPrimaryContainer = Color(0xFF7A1035),
    secondary = PinkTokens.accent,
    onSecondary = PinkTokens.onAccent,
    background = PinkTokens.bgPage,
    onBackground = PinkTokens.textStrong,
    surface = Color(0xFFFFFFFF),
    onSurface = PinkTokens.textStrong,
    surfaceVariant = PinkTokens.bgElevated,
    onSurfaceVariant = PinkTokens.textDefault,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = PinkTokens.bgElevated,
    surfaceContainerLow = Color(0xFFFFFAFB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0x4DFF5B8A),
    outlineVariant = Color(0x2EFF5B8A),
    error = PinkTokens.danger,
    onError = Color(0xFFFFFFFF)
)

// ---------------------------------------------------------------------- sky

private val SkyTokens = Tokens(
    bgPage = Color(0xFFC9E4FF),
    bgElevated = Color(0xFFEAF4FF),
    bgSunken = Color(0xFFB8D8F5),
    textStrong = Color(0xFF1B2547),
    textDefault = Color(0xFF324063),
    textMuted = Color(0xFF6A7898),
    textFaint = Color(0xFFA5B1C8),
    divider = Color(0x2E3C64AA),
    accent = Color(0xFFFFC83D),
    // Darkened gold: raw #ffc83d only works as a fill behind --text-on-accent.
    accentText = Color(0xFFA87A00),
    onAccent = Color(0xFF3A2400),
    success = Color(0xFF1EA974),
    warning = Color(0xFFD99022),
    danger = Color(0xFFE43B5C),
    info = Color(0xFF2F6FD6),
    isLight = true
)

private val SkyScheme: ColorScheme = lightColorScheme(
    primary = SkyTokens.accent,
    onPrimary = SkyTokens.onAccent,
    primaryContainer = SkyTokens.bgElevated,
    onPrimaryContainer = Color(0xFF4A3200),
    secondary = SkyTokens.accent,
    onSecondary = SkyTokens.onAccent,
    background = SkyTokens.bgPage,
    onBackground = SkyTokens.textStrong,
    surface = Color(0xFFFFFFFF),
    onSurface = SkyTokens.textStrong,
    surfaceVariant = SkyTokens.bgElevated,
    onSurfaceVariant = SkyTokens.textDefault,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = SkyTokens.bgElevated,
    surfaceContainerLow = Color(0xFFEFF7FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0x4D3C64AA),
    outlineVariant = Color(0x2E3C64AA),
    error = SkyTokens.danger,
    onError = Color(0xFFFFFFFF)
)

// ------------------------------------------------------------------ resolve

/** `follow` mirrors whatever the server says; anything else pins a palette. */
fun resolveTheme(mode: String, serverTheme: String): AppTheme =
    if (mode == "follow") AppTheme.of(serverTheme) else AppTheme.of(mode)

fun schemeFor(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.DARK -> DarkScheme
    AppTheme.PINK -> PinkScheme
    AppTheme.SKY -> SkyScheme
}

fun tokensFor(theme: AppTheme): Tokens = when (theme) {
    AppTheme.DARK -> DarkTokens
    AppTheme.PINK -> PinkTokens
    AppTheme.SKY -> SkyTokens
}

fun currentTheme(): AppTheme = resolveTheme(Prefs.themeMode, Prefs.serverTheme)

@Composable
fun currentTokens(): Tokens = tokensFor(currentTheme())
