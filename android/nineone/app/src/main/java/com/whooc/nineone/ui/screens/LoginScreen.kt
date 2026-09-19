package com.whooc.nineone.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.ui.theme.LocalTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Native login. The backend sets an HttpOnly `vs_admin` cookie which the
 * persistent cookie jar stores, so nothing sensitive is kept in plain prefs.
 */
@Composable
fun LoginScreen() {
    val tokens = LocalTokens.current
    val scope = rememberCoroutineScope()

    var server by remember { mutableStateOf(Prefs.serverUrl) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var probe by remember { mutableStateOf<String?>(null) }
    val serverOk = Prefs.normalize(server).isNotEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .background(tokens.bgPage)
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "91",
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textStrong,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text("个人私有视频站", color = tokens.textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(34.dp))

            OutlinedTextField(
                value = server,
                onValueChange = { server = it; probe = null },
                label = { Text("服务器地址") },
                placeholder = { Text(Prefs.SERVER_HINT, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "填写你自己的 91 服务端地址，例如 http://192.168.1.10:9191 或 https://你的域名",
                color = tokens.textFaint,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = {
                        val base = Prefs.normalize(server)
                        probe = "正在测试…"
                        scope.launch {
                            probe = withContext(Dispatchers.IO) { testConnection(base) }
                        }
                    },
                    enabled = !busy && serverOk
                ) { Text("测试连接", color = tokens.accentText) }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val base = Prefs.normalize(server)
                        message = null
                        busy = true
                        scope.launch {
                            runCatching {
                                Prefs.serverUrl = base
                                Api.login(username.trim(), password)
                            }.onSuccess {
                                // Session flow flips to LoggedIn and this screen goes away.
                            }.onFailure {
                                message = it.message ?: "登录失败"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && serverOk && username.isNotBlank() && password.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.onAccent
                    )
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            color = tokens.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("登录", fontWeight = FontWeight.Medium)
                    }
                }
            }

            probe?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = tokens.textMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "首次使用请在网页端完成管理员初始化，再用同一组账号登录。",
                color = tokens.textFaint,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

private fun testConnection(base: String): String = try {
    val conn = (URL("$base/api/settings/theme").openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 8000
        requestMethod = "GET"
    }
    conn.connect()
    val code = conn.responseCode
    conn.disconnect()
    if (code in 200..499) "连接成功 · HTTP $code" else "连接失败 · HTTP $code"
} catch (e: Exception) {
    "连接失败 · ${e.javaClass.simpleName}"
}
