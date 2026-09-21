package com.whooc.nineone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.BuildConfig
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.data.Session
import com.whooc.nineone.data.Store
import com.whooc.nineone.ui.components.BrandMark
import com.whooc.nineone.ui.components.ScreenHeader
import com.whooc.nineone.ui.components.formatCount
import com.whooc.nineone.ui.theme.LocalTokens
import kotlinx.coroutines.launch

/**
 * Native "我的" tab: account, server and the local library at a glance.
 * "继续观看" jumps straight into the player rather than the detail page, which
 * is what you actually want after a break.
 */
@Composable
fun MineScreen(
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: (String) -> Unit
) {
    val tokens = LocalTokens.current
    val scope = rememberCoroutineScope()
    val session by Api.session.collectAsState()
    val lastProblem by Api.lastProblem.collectAsState()
    val favorites by Store.favorites.collectAsState()
    val history by Store.history.collectAsState()

    var confirmLogout by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    val resume = remember(history) { history.firstOrNull { it.positionMs > 0L } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "我的", subtitle = Prefs.serverUrl)

        // ------------------------------------------------------------ profile
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tokens.bgElevated)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tokens.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                BrandMark(
                    logoSize = 52.dp,
                    fontSize = 19.sp,
                    letterSpacing = 0.sp,
                    color = tokens.accentText,
                    // Half the box, so a logo lands as a circle rather than a
                    // rounded square.
                    cornerRadius = 26.dp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (session is Session.LoggedIn) "已登录" else "未登录",
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.textStrong
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    roleLabel(session),
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
                // Last failed request, so a report of "it kicked me to the login
                // screen" comes with the endpoint and status attached.
                lastProblem?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "最近错误：$it",
                        fontSize = 11.sp,
                        color = tokens.textFaint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                "v${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                color = tokens.textFaint
            )
        }

        // ----------------------------------------------------------- counters
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("收藏", favorites.size, Modifier.weight(1f), onOpenLibrary)
            StatCard("观看记录", history.size, Modifier.weight(1f), onOpenLibrary)
        }

        // ------------------------------------------------------ resume banner
        resume?.let { entry ->
            Column(Modifier.padding(horizontal = 14.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.accent.copy(alpha = 0.14f))
                        .clickable { onOpenPlayer(entry.id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = tokens.accentText,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("继续观看", fontSize = 12.sp, color = tokens.accentText)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textStrong,
                            maxLines = 1
                        )
                        Text(
                            "上次看到 ${formatMs(entry.positionMs)}",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = tokens.textMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // -------------------------------------------------------------- rows
        RowGroup {
            SettingRow(
                icon = Icons.Default.Dns,
                title = "服务器",
                value = Prefs.serverUrl.removePrefix("http://").removePrefix("https://"),
                onClick = onOpenSettings
            )
            SettingRow(
                icon = Icons.Default.Settings,
                title = "设置",
                value = "主题 · 播放 · 账号",
                onClick = onOpenSettings
            )
            SettingRow(
                icon = Icons.Default.Star,
                title = "我的收藏",
                value = "${favorites.size} 个视频",
                onClick = onOpenLibrary
            )
            SettingRow(
                icon = Icons.Default.History,
                title = "观看记录",
                value = "${history.size} 条",
                onClick = onOpenLibrary
            )
        }

        Spacer(Modifier.height(10.dp))

        RowGroup {
            SettingRow(
                icon = Icons.Default.DeleteOutline,
                title = "清除本机数据",
                value = "收藏与观看记录",
                danger = true,
                onClick = { confirmWipe = true }
            )
            SettingRow(
                icon = Icons.Default.Logout,
                title = "退出登录",
                value = null,
                danger = true,
                onClick = { confirmLogout = true }
            )
        }

        Spacer(Modifier.height(10.dp))

        RowGroup {
            SettingRow(
                icon = Icons.Default.Info,
                title = "关于",
                value = "原生客户端 · 非网页套壳",
                onClick = null
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "本应用通过 91 服务端 REST API 直连，媒体流经服务端代理下发。\n收藏与观看记录只保存在本机。",
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = tokens.textFaint,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Spacer(Modifier.height(30.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("退出登录？") },
            text = { Text("会清除本机保存的会话 Cookie，下次需要重新输入账号密码。", color = tokens.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch { Api.logout() }
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("取消", color = tokens.textMuted) }
            }
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("清除本机数据？") },
            text = {
                Text(
                    "将删除本机的 ${favorites.size} 条收藏和 ${history.size} 条观看记录，服务器上的视频不受影响。",
                    color = tokens.textMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Store.clearFavorites()
                    Store.clearHistory()
                    confirmWipe = false
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("取消", color = tokens.textMuted) }
            }
        )
    }
}

private fun roleLabel(session: Session): String = when (session) {
    is Session.LoggedIn -> if (session.role.isBlank()) "会话有效" else "角色：${session.role}"
    Session.LoggedOut -> "会话已失效"
    Session.Unknown -> "正在校验会话…"
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalTokens.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.bgElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            formatCount(count),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = tokens.textStrong
        )
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = tokens.textMuted)
    }
}

@Composable
private fun RowGroup(content: @Composable () -> Unit) {
    val tokens = LocalTokens.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.bgElevated)
    ) { content() }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String?,
    danger: Boolean = false,
    onClick: (() -> Unit)?
) {
    val tokens = LocalTokens.current
    val tint = if (danger) MaterialTheme.colorScheme.error else tokens.textDefault
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error else tokens.textStrong,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                value,
                fontSize = 12.sp,
                color = tokens.textMuted,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 170.dp)
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens.textFaint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
