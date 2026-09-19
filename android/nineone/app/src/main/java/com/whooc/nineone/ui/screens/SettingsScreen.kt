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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.BuildConfig
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.data.Store
import com.whooc.nineone.ui.components.ScreenHeader
import com.whooc.nineone.ui.theme.AppTheme
import com.whooc.nineone.ui.theme.LocalTokens
import com.whooc.nineone.ui.theme.ThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private data class ThemeOption(val mode: String, val label: String)

private val THEME_OPTIONS = listOf(
    ThemeOption("follow", "跟随服务器"),
    ThemeOption("dark", "暗黑"),
    ThemeOption("pink", "奶油白"),
    ThemeOption("sky", "星空蓝")
)

/**
 * Settings, all native. The server address is the only thing that has to be
 * right for the app to work, so it gets a real connectivity probe instead of
 * failing silently on the next screen.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val tokens = LocalTokens.current
    val scope = rememberCoroutineScope()

    var server by remember { mutableStateOf(Prefs.serverUrl) }
    var themeMode by remember { mutableStateOf(Prefs.themeMode) }
    var keepScreenOn by remember { mutableStateOf(Prefs.keepScreenOn) }
    var autoplayShorts by remember { mutableStateOf(Prefs.autoplayShorts) }
    var speed by remember { mutableStateOf(Prefs.playbackSpeed) }

    var probe by remember { mutableStateOf<String?>(null) }
    var probing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        ScreenHeader(
            title = "设置",
            subtitle = "v${BuildConfig.VERSION_NAME}",
            leading = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = tokens.textDefault
                    )
                }
            }
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ------------------------------------------------------ 服务器
            SectionLabel("服务器")

            Column(Modifier.padding(horizontal = 14.dp)) {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it; probe = null; saved = false },
                    label = { Text("服务器地址") },
                    placeholder = { Text(Prefs.SERVER_HINT, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    "没有内置任何地址，换成你自己的 91 服务端即可。",
                    fontSize = 11.sp,
                    color = tokens.textFaint
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val base = Prefs.normalize(server)
                            if (base.isEmpty()) {
                                probe = "请先填写服务器地址"
                            } else {
                                probing = true
                                probe = null
                                scope.launch {
                                    probe = withContext(Dispatchers.IO) { probeServer(base) }
                                    probing = false
                                }
                            }
                        },
                        enabled = !probing
                    ) {
                        if (probing) {
                            CircularProgressIndicator(
                                color = tokens.accentText,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("测试连接", color = tokens.accentText)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            val base = Prefs.normalize(server)
                            if (base.isEmpty()) {
                                probe = "请先填写服务器地址"
                                return@Button
                            }
                            Prefs.serverUrl = base
                            server = base
                            saved = true
                            scope.launch {
                                // A different host means a different cookie jar
                                // namespace, so the session is re-validated and
                                // will fall back to the login screen if needed.
                                ThemeController.refreshFromServer()
                                Api.refreshSession()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.onAccent
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存并重连", fontWeight = FontWeight.Medium)
                    }
                }

                probe?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 12.sp, color = tokens.textMuted)
                }
                if (saved) {
                    Spacer(Modifier.height(8.dp))
                    Text("已保存 · 当前 ${Prefs.serverUrl}", fontSize = 12.sp, color = tokens.success)
                }
            }

            // -------------------------------------------------------- 外观
            SectionLabel("外观")

            Column(Modifier.padding(horizontal = 14.dp)) {
                Text(
                    "服务器当前主题：${AppTheme.of(Prefs.serverTheme).label}",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    THEME_OPTIONS.forEach { option ->
                        val selected = themeMode == option.mode
                        FilterChip(
                            selected = selected,
                            onClick = {
                                themeMode = option.mode
                                ThemeController.setMode(option.mode)
                            },
                            label = { Text(option.label, fontSize = 12.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = tokens.bgElevated,
                                labelColor = tokens.textMuted,
                                selectedContainerColor = tokens.accent.copy(alpha = 0.18f),
                                selectedLabelColor = tokens.accentText
                            ),
                            border = null
                        )
                    }
                }
            }

            // -------------------------------------------------------- 播放
            SectionLabel("播放")

            RowGroup {
                SwitchRow(
                    title = "保持屏幕常亮",
                    subtitle = "播放时不让屏幕自动息屏",
                    checked = keepScreenOn
                ) {
                    keepScreenOn = it
                    Prefs.keepScreenOn = it
                }
                SwitchRow(
                    title = "短视频自动播放",
                    subtitle = "进入沉浸流后自动开始播放",
                    checked = autoplayShorts
                ) {
                    autoplayShorts = it
                    Prefs.autoplayShorts = it
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.bgElevated)
                    .padding(14.dp)
            ) {
                Text("默认倍速", style = MaterialTheme.typography.bodyLarge, color = tokens.textStrong)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SPEEDS.forEach { value ->
                        val selected = speed == value
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) tokens.accent.copy(alpha = 0.18f) else tokens.bgSunken)
                                .clickable {
                                    speed = value
                                    Prefs.playbackSpeed = value
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                if (value == value.toInt().toFloat()) "${value.toInt()}x" else "${value}x",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) tokens.accentText else tokens.textMuted
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------- 本地数据
            SectionLabel("本地数据")

            RowGroup {
                TextRow(
                    title = "清空收藏与观看记录",
                    subtitle = "只删除本机数据，服务器不受影响",
                    danger = true
                ) { confirmWipe = true }
            }

            // -------------------------------------------------------- 账号
            SectionLabel("账号")

            RowGroup {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { confirmLogout = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "退出登录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "91 原生客户端 · 通过服务端 REST API 直连\n播放请求复用同一套带 Cookie 的 HTTP 栈，媒体流经服务端代理下发",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = tokens.textFaint,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("退出登录？") },
            text = { Text("会清除本机保存的会话 Cookie。", color = tokens.textMuted) },
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
            title = { Text("清空本机数据？") },
            text = { Text("收藏和观看记录会被删除，且无法恢复。", color = tokens.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    Store.clearFavorites()
                    Store.clearHistory()
                    confirmWipe = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("取消", color = tokens.textMuted) }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val tokens = LocalTokens.current
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = tokens.accentText,
        modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp)
    )
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
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val tokens = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tokens.textStrong)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = tokens.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tokens.accent,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = tokens.textFaint,
                uncheckedTrackColor = tokens.bgSunken,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun TextRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tokens = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (danger) MaterialTheme.colorScheme.error else tokens.textStrong
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = tokens.textMuted)
        }
    }
}

/** `/api/settings/theme` is public, so it doubles as a reachability probe. */
private fun probeServer(base: String): String = try {
    val conn = (URL("$base/api/settings/theme").openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 8000
        requestMethod = "GET"
    }
    conn.connect()
    val code = conn.responseCode
    val theme = runCatching { conn.inputStream.bufferedReader().readText() }.getOrNull().orEmpty()
    conn.disconnect()
    if (code in 200..499) {
        val name = Regex("\"theme\"\\s*:\\s*\"([^\"]+)\"").find(theme)?.groupValues?.get(1)
        "连接成功 · HTTP $code" + if (name != null) " · 主题 $name" else ""
    } else {
        "连接失败 · HTTP $code"
    }
} catch (e: Exception) {
    "连接失败 · ${e.javaClass.simpleName}"
}
