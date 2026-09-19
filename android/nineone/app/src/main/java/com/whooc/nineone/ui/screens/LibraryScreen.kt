package com.whooc.nineone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whooc.nineone.data.FavoriteEntry
import com.whooc.nineone.data.HistoryEntry
import com.whooc.nineone.data.Store
import com.whooc.nineone.data.Video
import com.whooc.nineone.ui.components.EmptyBox
import com.whooc.nineone.ui.components.ScreenHeader
import com.whooc.nineone.ui.components.VideoRowCard
import com.whooc.nineone.ui.theme.LocalTokens

/**
 * Local-only library. The backend has no per-user watch history (only a global
 * view counter), so favourites and "continue watching" live in [Store] on the
 * device and are keyed by video ID — which also makes them survive a server
 * address change.
 */
@Composable
fun LibraryScreen(onOpenDetail: (String) -> Unit) {
    val tokens = LocalTokens.current
    val favorites by Store.favorites.collectAsState()
    val history by Store.history.collectAsState()

    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var confirmClear by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "片库",
            subtitle = "本地收藏与观看记录 · 仅保存在本机",
            actions = {
                val count = if (tabIndex == 0) favorites.size else history.size
                if (count > 0) {
                    IconButton(onClick = { confirmClear = tabIndex }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "清空",
                            tint = tokens.textDefault
                        )
                    }
                }
            }
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SegmentTab("收藏 ${favorites.size}", tabIndex == 0, Modifier.weight(1f)) { tabIndex = 0 }
            SegmentTab("历史 ${history.size}", tabIndex == 1, Modifier.weight(1f)) { tabIndex = 1 }
        }

        when (tabIndex) {
            0 -> if (favorites.isEmpty()) {
                EmptyBox("还没有收藏。在详情页点「收藏」就会出现在这里。")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites, key = { it.id }) { entry ->
                        VideoRowCard(
                            video = entry.toVideo(),
                            subtitle = "收藏于 ${entry.addedAt.toDay()}",
                            onClick = { onOpenDetail(entry.id) }
                        )
                    }
                }
            }

            else -> if (history.isEmpty()) {
                EmptyBox("还没有观看记录。看过的视频会自动记在这里。")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history, key = { it.id }) { entry ->
                        VideoRowCard(
                            video = entry.toVideo(),
                            progress = entry.progress,
                            subtitle = entry.subtitle(),
                            onClick = { onOpenDetail(entry.id) }
                        )
                    }
                }
            }
        }
    }

    confirmClear?.let { which ->
        val isFavorites = which == 0
        AlertDialog(
            onDismissRequest = { confirmClear = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(if (isFavorites) "清空收藏？" else "清空观看记录？") },
            text = {
                Text(
                    if (isFavorites) {
                        "本机保存的 ${favorites.size} 条收藏会被删除，服务器上的数据不受影响。"
                    } else {
                        "本机保存的 ${history.size} 条观看记录会被删除，服务器上的数据不受影响。"
                    },
                    color = tokens.textMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isFavorites) Store.clearFavorites() else Store.clearHistory()
                    confirmClear = null
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = null }) {
                    Text("取消", color = tokens.textMuted)
                }
            }
        )
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalTokens.current
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) tokens.accent.copy(alpha = 0.16f) else tokens.bgElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) tokens.accentText else tokens.textMuted
        )
    }
}

// ------------------------------------------------------------------ adapters

private fun FavoriteEntry.toVideo(): Video = Video(
    id = id,
    title = title,
    thumbnail = thumbnail,
    duration = duration
)

private fun HistoryEntry.toVideo(): Video = Video(
    id = id,
    title = title,
    thumbnail = thumbnail,
    duration = duration
)

private fun HistoryEntry.subtitle(): String = when {
    positionMs <= 0L -> "已看完"
    else -> "上次看到 ${formatMs(positionMs)}" + if (duration.isNotBlank()) " / $duration" else ""
}

private fun Long.toDay(): String {
    if (this <= 0L) return "未知时间"
    val days = (System.currentTimeMillis() - this) / 86_400_000L
    return when {
        days <= 0 -> "今天"
        days == 1L -> "昨天"
        days < 30 -> "$days 天前"
        else -> "${days / 30} 个月前"
    }
}
