package com.whooc.nineone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the active palette. The server theme is pulled
 * once per launch (`GET /api/settings/theme` is public, so it works on the
 * login screen too) and the user can pin a palette in settings.
 */
object ThemeController {

    private val _theme = MutableStateFlow(currentTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun sync() {
        _theme.value = currentTheme()
    }

    fun setMode(mode: String) {
        Prefs.themeMode = mode
        sync()
    }

    /** Best effort — a failure just keeps the cached palette. */
    suspend fun refreshFromServer() {
        runCatching { Api.theme() }
        sync()
    }
}

private val NineOneTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp),
        bodyMedium = bodyMedium.copy(fontSize = 14.sp),
        bodySmall = bodySmall.copy(fontSize = 12.sp),
        labelSmall = labelSmall.copy(fontSize = 11.sp)
    )
}

@Composable
fun NineOneTheme(
    @Suppress("UNUSED_PARAMETER") systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val theme by ThemeController.theme.collectAsState()
    val scheme = schemeFor(theme)
    val tokens = tokensFor(theme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = tokens.isLight
                isAppearanceLightNavigationBars = tokens.isLight
            }
        }
    }

    MaterialTheme(colorScheme = scheme, typography = NineOneTypography) {
        CompositionLocalProvider(LocalTokens provides tokens, content = content)
    }
}
