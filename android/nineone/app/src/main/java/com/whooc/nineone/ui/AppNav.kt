package com.whooc.nineone.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.LockGate
import com.whooc.nineone.data.Session
import com.whooc.nineone.ui.components.BrandMark
import com.whooc.nineone.ui.screens.DetailScreen
import com.whooc.nineone.ui.screens.LockScreen
import com.whooc.nineone.ui.screens.LoginScreen
import com.whooc.nineone.ui.screens.MainScreen
import com.whooc.nineone.ui.screens.PlayerScreen
import com.whooc.nineone.ui.screens.SettingsScreen
import com.whooc.nineone.ui.theme.LocalTokens
import com.whooc.nineone.ui.theme.NineOneTheme
import com.whooc.nineone.ui.theme.ThemeController

object Routes {
    const val MAIN = "main"
    const val DETAIL = "detail/{id}"
    const val PLAYER = "player/{id}"
    const val SETTINGS = "settings"

    fun detail(id: String) = "detail/$id"
    fun player(id: String) = "player/$id"
}

/**
 * The whole app hangs off two gates: the local one, then the session.
 *
 * The local gate comes first and short-circuits everything, including the
 * session probe — if a password has been set, nothing touches the network until
 * it has been entered. `Unknown` → splash, `LoggedOut` → login, `LoggedIn` → the
 * navigable shell. Keeping login out of the back stack entirely is what you want
 * on mobile.
 */
@Composable
fun NineOneApp() {
    val session by Api.session.collectAsState()
    val locked = LockGate.locked

    LaunchedEffect(locked) {
        if (locked) return@LaunchedEffect
        ThemeController.refreshFromServer()
        Api.refreshSession()
    }

    NineOneTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (locked) {
                LockScreen()
            } else {
                when (session) {
                    Session.Unknown -> SplashScreen()
                    Session.LoggedOut -> LoggedOutGate()
                    is Session.LoggedIn -> MainNavHost()
                }
            }
        }
    }
}

/**
 * Shown instead of the login form when the session state says "logged out".
 *
 * The 91 backend has answered `401 unauthorized` for cookies that were still
 * perfectly valid — while its SQLite wal-index was stale, `GetSession` could
 * not see session rows that did exist, so every protected route looked
 * unauthenticated. The client used to take that at face value and drop the user
 * on the login form until they restarted the app, which is exactly the bug
 * being fixed here.
 *
 * So: if we had already been inside the app during this process, quietly ask
 * `/admin/api/me` again before believing it. If the cookie turns out to be good
 * after all, `Api.session` flips back to LoggedIn and this composable is
 * replaced by the shell — the "just restart the app" workaround, automated.
 * A cold start with a genuinely dead cookie skips all of this and shows the
 * form immediately.
 */
@Composable
private fun LoggedOutGate() {
    var healing by remember { mutableStateOf(Api.confirmedThisRun && Api.hasStoredCookie()) }

    LaunchedEffect(Unit) {
        if (!healing) return@LaunchedEffect
        repeat(2) { attempt ->
            if (attempt > 0) kotlinx.coroutines.delay(1200)
            if (Api.refreshSession() is Session.LoggedIn) return@LaunchedEffect
        }
        healing = false
    }

    if (healing) SplashScreen() else LoginScreen()
}

@Composable
private fun MainNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onOpenDetail = { nav.navigate(Routes.detail(it)) },
                onOpenPlayer = { nav.navigate(Routes.player(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            DetailScreen(
                videoId = id,
                onBack = { nav.popBackStack() },
                onPlay = { nav.navigate(Routes.player(it)) },
                onOpenDetail = { nav.navigate(Routes.detail(it)) }
            )
        }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            PlayerScreen(videoId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun SplashScreen() {
    val tokens = LocalTokens.current
    var showHint by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        showHint = true
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(tokens.bgPage),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(
                logoSize = 76.dp,
                fontSize = 44.sp,
                color = tokens.textStrong
            )
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(
                color = tokens.accentText,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("正在连接服务器…", color = tokens.textMuted, fontSize = 13.sp)
            AnimatedVisibility(showHint) {
                Text(
                    com.whooc.nineone.data.Prefs.serverUrl,
                    color = tokens.textFaint,
                    fontSize = 12.sp,
                    modifier = Modifier.height(18.dp)
                )
            }
        }
    }
}
