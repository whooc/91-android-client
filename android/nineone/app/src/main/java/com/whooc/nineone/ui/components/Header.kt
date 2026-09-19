package com.whooc.nineone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.ui.theme.LocalTokens

/**
 * In-content header. Each tab draws its own so the toolbar can carry
 * tab-specific actions without a shared Scaffold topBar fighting the
 * immersive shorts mode.
 *
 * [leading] is for pushed screens (settings) that need a back affordance on
 * the left, where the platform convention puts it.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val tokens = LocalTokens.current
    Column(
        modifier
            .fillMaxWidth()
            .background(tokens.bgPage)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = if (leading != null) 4.dp else 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(2.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.textStrong,
                    fontWeight = FontWeight.SemiBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, fontSize = 11.sp, color = tokens.textMuted)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
