package com.whooc.nineone.ui.components

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.whooc.nineone.data.Players
import com.whooc.nineone.data.Video
import kotlin.math.abs

/**
 * The native equivalent of the web grid's hover preview.
 *
 * One shared muted player follows whichever card sits closest to the middle of
 * the viewport and plays that video's ~12s teaser (`/p/preview/{id}`). Cards
 * only attach a `PlayerView` while they are the active one, so there is never
 * more than a single surface bound to the player.
 */
class PreviewHostState(val player: ExoPlayer) {

    var activeId by mutableStateOf<String?>(null)
        private set

    /** Mirrors `GET /api/settings/preview`. */
    var enabled by mutableStateOf(true)

    fun follow(video: Video?) {
        val id = video?.id
        if (id == activeId) return

        if (video == null || !enabled || video.previewSrc.isBlank()) {
            activeId = null
            player.stop()
            player.clearMediaItems()
            return
        }
        val item = Players.mediaItem(video.id, video.previewSrc, video.title)
        if (item == null) {
            activeId = null
            player.stop()
            return
        }
        activeId = id
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    fun release() {
        player.stop()
        player.release()
    }
}

@Composable
fun rememberPreviewHost(context: Context = LocalContext.current): PreviewHostState {
    val host = remember { PreviewHostState(Players.buildPreview(context)) }
    DisposableEffect(host) {
        onDispose { host.release() }
    }
    return host
}

/** No-op unless [video] is the card the host is currently playing. */
@Composable
fun PreviewOverlay(
    host: PreviewHostState,
    video: Video,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (host.activeId != video.id) return
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setKeepContentOnPlayerReset(true)
                isClickable = false
                isFocusable = false
                player = host.player
            }
        },
        update = { it.player = host.player },
        modifier = modifier
    )
}

/**
 * Picks the grid cell whose centre is nearest the viewport centre. Only cells
 * registered with a `v-` / `r-` / `l-` key take part, which keeps the
 * full-span section headers out of the running.
 */
@Composable
fun rememberPreviewTarget(
    gridState: LazyGridState,
    byId: Map<String, Video>
): Video? {
    val id by remember(gridState, byId) {
        derivedStateOf {
            val info = gridState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf null
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .mapNotNull { item ->
                    val key = item.key as? String ?: return@mapNotNull null
                    val videoId = previewKeyToId(key) ?: return@mapNotNull null
                    val itemCenter = item.offset.y + item.size.height / 2
                    videoId to abs(itemCenter - center)
                }
                .minByOrNull { it.second }
                ?.first
        }
    }
    return id?.let { byId[it] }
}

private fun previewKeyToId(key: String): String? = when {
    key.startsWith("v-") -> key.substring(2)
    key.startsWith("r-") -> key.substring(2)
    key.startsWith("l-") -> key.substring(2)
    else -> null
}
