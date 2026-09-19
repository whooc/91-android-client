package com.whooc.nineone.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.MediaUrls
import com.whooc.nineone.data.Players
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.data.Store
import com.whooc.nineone.data.Video
import com.whooc.nineone.ui.components.ErrorBox
import com.whooc.nineone.ui.theme.LocalTokens
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * Native player. Resume position comes from the local store (the backend has no
 * per-user progress endpoint) and is flushed on pause, on dispose and every few
 * seconds while playing.
 */
@Composable
fun PlayerScreen(videoId: String, onBack: () -> Unit) {
    val tokens = LocalTokens.current
    val context = LocalContext.current
    val view = LocalView.current
    val activity = view.context as? Activity

    var video by remember { mutableStateOf<Video?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }

    val player = remember { Players.build(context) }
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(Prefs.playbackSpeed) }
    var controlsVisible by remember { mutableStateOf(true) }
    var speedMenu by remember { mutableStateOf(false) }
    var subtitleLabel by remember { mutableStateOf<String?>(null) }
    var subtitlesOn by remember { mutableStateOf(true) }

    // ------------------------------------------------------------ lifecycle

    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null && Prefs.keepScreenOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            video?.let { v ->
                val dur = player.duration.takeIf { it > 0 } ?: 0L
                Store.recordProgress(v, player.currentPosition, dur)
            }
            player.release()
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let {
                WindowCompat.getInsetsController(it.window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Hide the chrome while fullscreen; keep it otherwise.
    LaunchedEffect(fullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        if (fullscreen) {
            fullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onBack()
        }
    }

    // ------------------------------------------------------------ load media

    LaunchedEffect(videoId) {
        runCatching {
            val detail = Api.video(videoId)
            val v = detail.toVideo()
            video = v

            val subs = runCatching { Api.subtitles(videoId) }.getOrDefault(emptyList())
                .filter { it.ext.equals("srt", true) || it.ext.equals("vtt", true) }

            val configs = subs.mapIndexed { index, sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(MediaUrls.resolve(sub.url)))
                    .setMimeType(
                        if (sub.ext.equals("vtt", true)) MimeTypes.TEXT_VTT
                        else MimeTypes.APPLICATION_SUBRIP
                    )
                    .setLanguage(sub.language.ifBlank { "und" })
                    .setLabel(sub.label.ifBlank { sub.name }.ifBlank { "字幕 ${index + 1}" })
                    .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
            subtitleLabel = if (configs.isEmpty()) null else "已加载 ${configs.size} 条字幕"

            val url = MediaUrls.resolve(detail.videoSrc)
            if (url.isBlank()) throw IllegalStateException("服务端未返回播放地址")

            val item = MediaItem.Builder()
                .setUri(url)
                .setMediaId(detail.id)
                .setSubtitleConfigurations(configs)
                .build()
            player.setMediaItem(item)
            player.setPlaybackSpeed(speed)
            player.prepare()

            val resume = Store.positionOf(detail.id)
            if (resume > 3_000L) player.seekTo(resume)
            player.play()
        }.onFailure {
            loadError = it.message ?: "无法加载视频"
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                ended = playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (!isPlaying) video?.let {
                    Store.recordProgress(it, player.currentPosition, player.duration.coerceAtLeast(0L))
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                loadError = error.errorCodeName
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Progress ticker + periodic persistence.
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            if (!scrubbing) {
                position = player.currentPosition.coerceAtLeast(0L)
                if (player.duration > 0) duration = player.duration
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            video?.let {
                if (player.isPlaying) {
                    Store.recordProgress(it, player.currentPosition, player.duration.coerceAtLeast(0L))
                }
            }
        }
    }

    // Auto-hide the controls.
    LaunchedEffect(controlsVisible, playing) {
        if (controlsVisible && playing) {
            delay(4_000)
            controlsVisible = false
        }
    }

    // Volume follows the app-wide mute switch, so the floating button in the
    // shell and this screen can never disagree.
    LaunchedEffect(Prefs.globalMuted) {
        player.volume = if (Prefs.globalMuted) 0f else 1f
    }

    // ------------------------------------------------------------------- UI

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier
                .fillMaxSize()
                .clickable { controlsVisible = !controlsVisible }
        )

        if (buffering && loadError == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(38.dp)
            )
        }

        loadError?.let { message ->
            ErrorBox(
                message = message,
                onRetry = { loadError = null; onBack() },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    Text(
                        video?.title.orEmpty(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "后退 10 秒",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable {
                                if (ended) {
                                    player.seekTo(0)
                                    player.play()
                                } else if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    IconButton(
                        onClick = { player.seekTo(player.currentPosition + 10_000) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "前进 10 秒",
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(-1f, 1f)
                        )
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatMs(if (scrubbing) (scrubValue * duration).toLong() else position),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = if (scrubbing) scrubValue else {
                                if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                            },
                            onValueChange = {
                                scrubbing = true
                                scrubValue = it
                            },
                            onValueChangeFinished = {
                                if (duration > 0) player.seekTo((scrubValue * duration).toLong())
                                scrubbing = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = tokens.accent,
                                activeTrackColor = tokens.accent
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                        )
                        Text(
                            if (duration > 0) formatMs(duration) else "--:--",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            subtitleLabel ?: "无字幕",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(onClick = { speedMenu = true }) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = "倍速",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                                SPEEDS.forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${s}x" + if (s == speed) "  ✓" else "",
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            speed = s
                                            Prefs.playbackSpeed = s
                                            player.setPlaybackSpeed(s)
                                            speedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Text("${speed}x", color = Color.White, fontSize = 11.sp)
                        IconButton(onClick = { Prefs.globalMuted = !Prefs.globalMuted }) {
                            Icon(
                                if (Prefs.globalMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (Prefs.globalMuted) "取消静音" else "静音",
                                tint = if (Prefs.globalMuted) Color(0xFF888888) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                subtitlesOn = !subtitlesOn
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesOn)
                                    .build()
                            }
                        ) {
                            Icon(
                                Icons.Default.Subtitles,
                                contentDescription = "字幕开关",
                                tint = if (subtitlesOn) tokens.accent else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                fullscreen = !fullscreen
                                activity?.requestedOrientation = if (fullscreen) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        ) {
                            Icon(
                                if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "全屏",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
