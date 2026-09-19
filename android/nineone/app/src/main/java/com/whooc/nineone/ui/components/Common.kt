package com.whooc.nineone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.whooc.nineone.data.MediaUrls
import com.whooc.nineone.data.Video
import com.whooc.nineone.ui.theme.LocalTokens

/** 16:9 thumbnail with a duration badge, used everywhere a video is listed. */
@Composable
fun Thumbnail(
    path: String,
    duration: String,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    corner: Int = 10,
    /** Drawn between the still image and the badges — the moving preview. */
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val tokens = LocalTokens.current
    val url = MediaUrls.resolve(path)
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(corner.dp))
            .background(tokens.bgElevated)
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = tokens.textFaint,
                modifier = Modifier.align(Alignment.Center).size(36.dp)
            )
        }

        overlay()
        if (duration.isNotBlank()) {
            Text(
                text = duration,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                color = tokens.accent,
                trackColor = Color(0x66000000),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}

/** Grid cell: thumbnail on top, two-line title, one meta line. */
@Composable
fun VideoGridCard(
    video: Video,
    progress: Float = 0f,
    preview: PreviewHostState? = null,
    onClick: () -> Unit
) {
    val tokens = LocalTokens.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Thumbnail(
            path = video.thumbnail,
            duration = video.duration,
            progress = progress
        ) {
            if (preview != null) PreviewOverlay(preview, video)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textStrong,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = metaLine(video),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Horizontal list row — used by search results, favourites and history. */
@Composable
fun VideoRowCard(
    video: Video,
    progress: Float = 0f,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val tokens = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail(
            path = video.thumbnail,
            duration = video.duration,
            progress = progress,
            modifier = Modifier.width(150.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textStrong,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle ?: metaLine(video),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun metaLine(video: Video): String {
    val parts = mutableListOf<String>()
    if (video.views > 0) parts += "${formatCount(video.views)} 次观看"
    if (video.publishedAt.isNotBlank()) parts += video.publishedAt
    if (video.author.isNotBlank()) parts += video.author
    if (parts.isEmpty() && video.sourceLabel.isNotBlank()) parts += video.sourceLabel
    return parts.joinToString(" · ")
}

fun formatCount(n: Int): String = when {
    n >= 100_000_000 -> "%.1f亿".format(n / 100_000_000.0)
    n >= 10_000 -> "%.1f万".format(n / 10_000.0)
    else -> n.toString()
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier) {
    val tokens = LocalTokens.current
    Text(
        text = text,
        color = tokens.accentText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(tokens.accent.copy(alpha = 0.16f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val tokens = LocalTokens.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = tokens.textStrong,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    val tokens = LocalTokens.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = tokens.accentText, strokeWidth = 3.dp)
    }
}

@Composable
fun ErrorBox(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    val tokens = LocalTokens.current
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = tokens.textFaint,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "加载失败",
            style = MaterialTheme.typography.titleMedium,
            color = tokens.textStrong
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textMuted
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("重试", color = tokens.accentText) }
            }
            if (onSettings != null) {
                TextButton(onClick = onSettings) { Text("修改服务器", color = tokens.accentText) }
            }
        }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    val tokens = LocalTokens.current
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = tokens.textFaint,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tokens.textMuted)
    }
}

@Composable
fun InlineProgress(modifier: Modifier = Modifier) {
    val tokens = LocalTokens.current
    Box(modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = tokens.accentText, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}

val GridPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
