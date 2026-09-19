package com.whooc.nineone.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whooc.nineone.data.Store
import com.whooc.nineone.ui.components.Badge
import com.whooc.nineone.ui.components.SectionHeader
import com.whooc.nineone.ui.components.StateBox
import com.whooc.nineone.ui.components.Thumbnail
import com.whooc.nineone.ui.components.VideoRowCard
import com.whooc.nineone.ui.components.formatCount
import com.whooc.nineone.ui.theme.LocalTokens
import com.whooc.nineone.ui.vm.DetailData
import com.whooc.nineone.ui.vm.DetailViewModel

@Composable
fun DetailScreen(
    videoId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val tokens = LocalTokens.current
    val context = LocalContext.current
    val vm: DetailViewModel = viewModel(key = "detail-$videoId") { DetailViewModel(videoId) }
    val state by vm.state.collectAsState()
    val likes by vm.likes.collectAsState()
    val favorites by Store.favorites.collectAsState()
    val history by Store.history.collectAsState()
    var liked by remember { mutableStateOf(false) }

    LaunchedEffect(videoId) { vm.markViewed() }

    Column(Modifier.fillMaxSize().background(tokens.bgPage)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = tokens.textStrong
                )
            }
            Text(
                "详情",
                style = MaterialTheme.typography.titleMedium,
                color = tokens.textStrong,
                modifier = Modifier.weight(1f)
            )
        }

        StateBox(state = state, onRetry = { vm.load() }) { data: DetailData ->
            val detail = data.detail
            val isFavorite = favorites.any { it.id == detail.id }
            val progress = history.firstOrNull { it.id == detail.id }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp)
            ) {
                item {
                    Box(Modifier.padding(horizontal = 14.dp)) {
                        Thumbnail(
                            path = detail.poster.ifBlank { detail.thumbnail },
                            duration = detail.duration,
                            corner = 14
                        )
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(tokens.accent.copy(alpha = 0.92f))
                                .clickable { onPlay(detail.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "播放",
                                tint = tokens.onAccent,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        if (progress != null && progress.positionMs > 0) {
                            Text(
                                "上次看到 ${formatMs(progress.positionMs)}",
                                color = tokens.textStrong,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tokens.accent.copy(alpha = 0.85f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            detail.title.ifBlank { "未命名" },
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textStrong,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                buildString {
                                    if (detail.views > 0) append("${formatCount(detail.views)} 次观看")
                                    if (detail.publishedAt.isNotBlank()) {
                                        if (isNotEmpty()) append(" · ")
                                        append(detail.publishedAt)
                                    }
                                    if (detail.sourceLabel.isNotBlank()) {
                                        if (isNotEmpty()) append(" · ")
                                        append(detail.sourceLabel)
                                    }
                                },
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (detail.badges.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                detail.badges.take(4).forEach { Badge(it) }
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionButton(
                            icon = Icons.Default.ThumbUp,
                            label = formatCount(likes.coerceAtLeast(0)),
                            tint = if (liked) tokens.accentText else tokens.textDefault
                        ) {
                            liked = !liked
                            vm.toggleLike(liked)
                        }
                        ActionButton(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isFavorite) "已收藏" else "收藏",
                            tint = if (isFavorite) tokens.accentText else tokens.textDefault
                        ) { Store.toggleFavorite(detail.toVideo()) }
                        ActionButton(
                            icon = Icons.Default.Share,
                            label = "分享链接",
                            tint = tokens.textDefault
                        ) {
                            val url = "${com.whooc.nineone.data.Prefs.serverUrl}/video/${detail.id}"
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, url)
                                        },
                                        "分享"
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.accent.copy(alpha = 0.10f))
                            .clickable { onPlay(detail.id) }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (progress != null && progress.positionMs > 0) "继续播放" else "立即播放",
                            color = tokens.accentText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (detail.tags.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text("标签", color = tokens.textMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                detail.tags.joinToString("  ") { "#$it" },
                                color = tokens.textDefault,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                if (detail.description.isNotBlank()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("简介", color = tokens.textMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                detail.description,
                                color = tokens.textDefault,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                if (data.related.isNotEmpty()) {
                    item { SectionHeader("相关推荐") }
                    items(data.related, key = { "rel-${it.id}" }) { video ->
                        VideoRowCard(video = video, onClick = { onOpenDetail(video.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

fun formatMs(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
