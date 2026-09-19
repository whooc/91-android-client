package com.whooc.nineone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.ui.theme.LocalTokens

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.Home),
    LIST("列表", Icons.AutoMirrored.Filled.List),
    SHORTS("短视频", Icons.Default.PlayCircleOutline),
    LIBRARY("片库", Icons.Default.VideoLibrary),
    MINE("我的", Icons.Default.Person)
}

/** Content height of the bar, excluding the system navigation inset. */
private val BottomBarHeight = 50.dp

/**
 * Bottom-navigation shell.
 *
 * The bar is a hand-rolled Row rather than Material3's [androidx.compose.material3.NavigationBar]
 * because that component pins itself to 80dp and cannot be shortened — this one
 * is deliberately compact so the immersive shorts feed still gets most of the
 * screen.
 *
 * Insets are handled explicitly (Scaffold's own content insets are switched off)
 * so the padding maths stays predictable on Android 15.
 */
@Composable
fun MainScreen(
    onOpenDetail: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val tokens = LocalTokens.current
    var tabIndex by rememberSaveable {
        mutableStateOf(Prefs.lastTab.coerceIn(0, MainTab.entries.size - 1))
    }
    val tab = MainTab.entries[tabIndex]

    Scaffold(
        containerColor = tokens.bgPage,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomBar(
                selected = tabIndex,
                onSelect = {
                    tabIndex = it
                    Prefs.lastTab = it
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(tokens.bgPage)
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    onOpenDetail = onOpenDetail,
                    onOpenSettings = onOpenSettings
                )
                MainTab.LIST -> ListScreen(
                    onOpenDetail = onOpenDetail,
                    onOpenSettings = onOpenSettings
                )
                MainTab.SHORTS -> ShortsScreen(
                    onOpenDetail = onOpenDetail,
                    onOpenPlayer = onOpenPlayer,
                    // Sideways swipe leaves the feed for 首页.
                    onExitShorts = {
                        tabIndex = MainTab.HOME.ordinal
                        Prefs.lastTab = tabIndex
                    }
                )
                MainTab.LIBRARY -> LibraryScreen(onOpenDetail = onOpenDetail)
                MainTab.MINE -> MineScreen(
                    onOpenSettings = onOpenSettings,
                    // Favourites/history live in the 片库 tab, so the "我的"
                    // counters switch tabs instead of pushing a screen.
                    onOpenLibrary = {
                        tabIndex = MainTab.LIBRARY.ordinal
                        Prefs.lastTab = tabIndex
                    },
                    onOpenPlayer = onOpenPlayer
                )
            }

            // App-wide mute. Lives in the shell rather than in any one screen so
            // it is reachable from every tab and survives tab switches.
            GlobalMuteButton(
                muted = Prefs.globalMuted,
                onToggle = { Prefs.globalMuted = !Prefs.globalMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
            )
        }
    }
}

/**
 * Floating mute toggle, pinned to the bottom-right corner just above the
 * bottom bar. Reads and writes the same [Prefs.globalMuted] the players use.
 */
@Composable
private fun GlobalMuteButton(
    muted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTokens.current
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .border(1.dp, tokens.divider, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = if (muted) "取消全局静音" else "全局静音",
            tint = if (muted) tokens.accentText else tokens.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val tokens = LocalTokens.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tokens.divider)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(BottomBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEachIndexed { index, item ->
                val active = index == selected
                val tint = if (active) tokens.accentText else tokens.textMuted
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        color = tint,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}
