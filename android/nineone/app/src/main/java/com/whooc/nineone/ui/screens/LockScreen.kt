package com.whooc.nineone.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.data.AppLock
import com.whooc.nineone.data.LockGate
import com.whooc.nineone.ui.components.BrandMark
import com.whooc.nineone.ui.theme.LocalTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The local gate. Drawn before anything else — before the splash, before the
 * login form, before a single request goes out.
 *
 * The stored code is verified off the main thread: PBKDF2 at 120k iterations
 * takes long enough to drop frames, and doing it inline would show up as a
 * visible hitch on every unlock attempt.
 *
 * Back leaves the app rather than doing nothing. A screen where the back gesture
 * is silently swallowed reads as frozen.
 */
@Composable
fun LockScreen() {
    val tokens = LocalTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }

    BackHandler { (context as? Activity)?.finish() }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }

    fun submit() {
        if (busy || code.isEmpty()) return
        busy = true
        error = null
        scope.launch {
            val ok = withContext(Dispatchers.Default) { AppLock.verify(code) }
            busy = false
            if (ok) {
                LockGate.unlock()
            } else {
                attempts += 1
                error = if (attempts >= 3) "密码不正确（已错 $attempts 次）" else "密码不正确"
                code = ""
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(tokens.bgPage)
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            BrandMark(
                logoSize = 80.dp,
                fontSize = 46.sp,
                color = tokens.textStrong
            )

            Spacer(Modifier.height(22.dp))
            Text("请输入进入密码", color = tokens.textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    error = null
                },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                isError = error != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { submit() },
                enabled = !busy && code.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.onAccent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = tokens.onAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("解锁", fontWeight = FontWeight.Medium)
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = tokens.danger, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "密码只保存在本机，忘记后清除应用数据即可重置（会同时清掉登录状态）。",
                color = tokens.textFaint,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
