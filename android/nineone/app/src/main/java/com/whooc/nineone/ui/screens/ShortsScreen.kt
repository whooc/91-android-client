package com.whooc.nineone.ui.screens

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.whooc.nineone.data.MediaUrls
import com.whooc.nineone.data.Players
import com.whooc.nineone.data.Prefs
import com.whooc.nineone.data.ShortsItem
import com.whooc.nineone.data.Store
import com.whooc.nineone.ui.components.ErrorBox
import com.whooc.nineone.ui.components.LoadingBox
import com.whooc.nineone.ui.components.formatCount
import com.whooc.nineone.ui.theme.LocalTokens
import com.whooc.nineone.ui.vm.ShortsData
import com.whooc.nineone.ui.vm.ShortsViewModel
import com.whooc.nineone.ui.vm.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How long the on-video chrome (title, counters, action rail) stays up. */
private const val CHROME_TIMEOUT_MS = 5_000L

/**
 * Vertical short-video feed.
 *
 * Each settled page owns an ExoPlayer that is created when the page is composed
 * and released when it scrolls out of the pager's beyond-viewport window, so at
 * most three players exist at any time and only the settled one is playing.
 *
 * The app's bottom bar stays on screen (the feed is a normal tab, not a modal),
 * so there is no in-page back button — leaving is a sideways swipe.
 */
@Composable
fun ShortsScreen(
    onOpenDetail: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onExitShorts: () -> Unit,
    vm: ShortsViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val view = LocalView.current
    val activity = view.context as? Activity

    // Deliberately NOT immersive: the app's bottom bar stays on screen so the
    // feed is never a dead end. Only the keep-awake flag is managed here.
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null && Prefs.keepScreenOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            is UiState.Loading -> LoadingBox()
            is UiState.Failed -> ErrorBox(s.message, onRetry = { vm.loadMore(reset = true) })
            is UiState.Ready -> ShortsFeed(s.data, vm, onOpenDetail, onOpenPlayer, onExitShorts)
        }
    }
}

@Composable
private fun ShortsFeed(
    data: ShortsData,
    vm: ShortsViewModel,
    onOpenDetail: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onExitShorts: () -> Unit
) {
    if (data.items.isEmpty()) {
        ErrorBox("短视频流为空", onRetry = { vm.loadMore(reset = true) })
        return
    }
    val pagerState = rememberPagerState(pageCount = { data.items.size })

    // Keep a couple of pages of runway ahead of the user.
    LaunchedEffect(pagerState.settledPage, data.items.size) {
        if (pagerState.settledPage >= data.items.size - 2) vm.loadMore()
    }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val item = data.items.getOrNull(page) ?: return@VerticalPager
        ShortsPage(
            item = item,
            active = pagerState.settledPage == page,
            onOpenDetail = onOpenDetail,
            onOpenPlayer = onOpenPlayer,
            onExitShorts = onExitShorts
        )
    }
}

@Composable
private fun ShortsPage(
    item: ShortsItem,
    active: Boolean,
    onOpenDetail: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onExitShorts: () -> Unit
) {
    val tokens = LocalTokens.current
    val context = LocalContext.current
    val favorites by Store.favorites.collectAsState()
    val isFavorite = favorites.any { it.id == item.id }

    val scope = rememberCoroutineScope()
    val dragX = remember(item.id) { Animatable(0f) }
    val exitThreshold = with(LocalDensity.current) { 72.dp.toPx() }

    val player = remember(item.id) { Players.build(context) }
    var buffering by remember(item.id) { mutableStateOf(true) }
    var playing by remember(item.id) { mutableStateOf(false) }
    var ended by remember(item.id) { mutableStateOf(false) }
    var failed by remember(item.id) { mutableStateOf<String?>(null) }

    // Mute is app-wide: this page and the floating shell button drive the same
    // preference, so the two can never disagree.
    val muted = Prefs.globalMuted

    // Real video dimensions decide portrait vs. landscape framing. Until the
    // decoder reports them we assume portrait (the common case for a shorts
    // feed) and the player view simply fills the page.
    var videoWidth by remember(item.id) { mutableIntStateOf(0) }
    var videoHeight by remember(item.id) { mutableIntStateOf(0) }

    // Playback position for the scrub bar.
    var position by remember(item.id) { mutableLongStateOf(0L) }
    var duration by remember(item.id) { mutableLongStateOf(0L) }
    var scrubbing by remember(item.id) { mutableStateOf(false) }
    var scrubValue by remember(item.id) { mutableFloatStateOf(0f) }

    // Chrome auto-hides after a few seconds; any touch brings it straight back.
    var chromeVisible by remember(item.id) { mutableStateOf(true) }
    var chromeEpoch by remember(item.id) { mutableIntStateOf(0) }

    // Watching a short counts as watching, exactly like the full player — the
    // local history is the only per-user record the backend has no endpoint for.
    fun persistProgress() {
        val dur = player.duration.takeIf { it > 0 } ?: 0L
        Store.recordProgress(item.toVideo(), player.currentPosition, dur)
    }

    DisposableEffect(item.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                ended = playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY) {
                    player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                failed = error.errorCodeName
                buffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            persistProgress()
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(item.id) {
        val media = Players.mediaItem(item.id, item.videoSrc, item.title)
        if (media == null) {
            failed = "服务端未返回播放地址"
            return@LaunchedEffect
        }
        player.setMediaItem(media)
        player.prepare()
    }

    LaunchedEffect(active) {
        if (active) {
            if (ended) player.seekTo(0)
            if (Prefs.autoplayShorts) player.play() else player.pause()
            chromeVisible = true
            chromeEpoch++
        } else {
            player.pause()
            persistProgress()
        }
    }

    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }

    // Progress ticker for the scrub bar.
    LaunchedEffect(item.id) {
        while (true) {
            delay(400)
            if (!scrubbing) {
                position = player.currentPosition.coerceAtLeast(0L)
                player.duration.takeIf { it > 0 }?.let { duration = it }
            }
        }
    }

    // Fall back to the duration the API already told us about, so the bar is
    // usable even before the decoder reports one.
    LaunchedEffect(item.id, item.durationSeconds) {
        if (duration <= 0 && item.durationSeconds > 0) duration = item.durationSeconds * 1_000L
    }

    // Auto-hide. `chromeEpoch` is bumped on every fresh touch, which restarts
    // the countdown; dragging the scrubber suspends it entirely so the bar
    // never vanishes mid-gesture.
    LaunchedEffect(chromeVisible, chromeEpoch, scrubbing) {
        if (chromeVisible && !scrubbing) {
            delay(CHROME_TIMEOUT_MS)
            chromeVisible = false
        }
    }

    val aspect = if (videoWidth > 0 && videoHeight > 0) {
        videoWidth.toFloat() / videoHeight.toFloat()
    } else 0f
    val landscape = aspect > 1.02f

    Box(
        Modifier
            .fillMaxSize()
            // Swipe sideways to leave the feed; the whole page follows the
            // finger so the gesture reads as "throwing the video away".
            .offset { IntOffset(dragX.value.roundToInt(), 0) }
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragX.value) > exitThreshold) {
                            onExitShorts()
                        } else {
                            scope.launch { dragX.animateTo(0f, tween(180)) }
                        }
                    },
                    onDragCancel = { scope.launch { dragX.animateTo(0f, tween(180)) } },
                    onHorizontalDrag = { _, delta ->
                        scope.launch { dragX.snapTo(dragX.value + delta) }
                    }
                )
            }
            // Watch the Initial pass only, so this never steals events from the
            // scrubber or the action rail — it just wakes the chrome up.
            .pointerInput(item.id) {
                awaitPointerEventScope {
                    var wasPressed = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.any { it.pressed }
                        if (pressed && !wasPressed) {
                            chromeVisible = true
                            chromeEpoch++
                        }
                        wasPressed = pressed
                    }
                }
            }
            .clickable {
                if (ended) {
                    player.seekTo(0)
                    player.play()
                } else if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
    ) {
        // Poster underneath so the first frame is never a black flash.
        val poster = MediaUrls.resolve(
            item.backgroundPoster.ifBlank { item.poster.ifBlank { item.thumbnail } }
        )
        if (poster.isNotEmpty()) {
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Portrait clips to fill the page (RESIZE_MODE_ZOOM); landscape is
            // letterboxed at its true aspect ratio instead of being cropped to
            // a vertical frame, and gets an explicit fullscreen entry point
            // underneath it.
            val videoHeight = if (landscape && aspect > 0f) maxWidth / aspect else maxHeight

            Box(
                Modifier
                    .align(Alignment.Center)
                    .then(
                        if (landscape && aspect > 0f) {
                            Modifier.fillMaxWidth().height(videoHeight)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
            ) {
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
                            // Let taps and the swipe-to-exit gesture fall through
                            // to the Compose layer instead of being eaten here.
                            isClickable = false
                            isFocusable = false
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.player = player
                        view.resizeMode = if (landscape) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (landscape && aspect > 0f) {
                Row(
                    Modifier
                        .align(Alignment.Center)
                        .offset(y = videoHeight / 2 + 26.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x99000000))
                        .clickable { onOpenPlayer(item.id) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("全屏播放", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // Scrims so the chrome stays readable over bright frames.
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
        )

        if (buffering && failed == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(38.dp)
            )
        }

        if (!playing && !buffering && failed == null) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (ended) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        failed?.let { message ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("播放失败", color = Color.White, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(message, color = Color(0xFFB0B0B0), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点按重试",
                        color = tokens.accentText,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            failed = null
                            player.prepare()
                        }
                    )
                }
            }
        }

        // ------------------------------------------------------------ overlay

        // Title + counters live at the very top of the page now.
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.84f)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
            ) {
                Text(
                    item.title.ifBlank { "未命名" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.author.isNotBlank()) {
                        Text(item.author, color = Color(0xFFDDDDDD), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "${formatCount(item.views)} 次观看",
                        color = Color(0xFFBBBBBB),
                        fontSize = 12.sp
                    )
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.tags.take(3).joinToString("  ") { "#$it" },
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Column(
                Modifier.padding(end = 12.dp, bottom = 76.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ShortsAction(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (isFavorite) "已收藏" else "收藏",
                    tint = if (isFavorite) Color(0xFFFF5B8A) else Color.White
                ) { Store.toggleFavorite(item.toVideo()) }

                ShortsAction(
                    icon = if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    label = if (muted) "已静音" else "声音"
                ) { Prefs.globalMuted = !muted }

                ShortsAction(
                    icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (playing) "暂停" else "播放"
                ) {
                    if (ended) player.seekTo(0)
                    if (player.isPlaying) player.pause() else player.play()
                }

                ShortsAction(icon = Icons.Default.Info, label = "详情") { onOpenDetail(item.id) }
            }
        }

        // Scrub bar — part of the chrome, so it hides and returns with it.
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    // Leaves room for the shell's floating mute button, which
                    // sits in the bottom-right corner just above the bottom bar.
                    .padding(start = 14.dp, end = 64.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatMs(if (scrubbing) (scrubValue * duration).toLong() else position),
                    color = Color.White,
                    fontSize = 11.sp
                )
                Slider(
                    value = if (scrubbing) {
                        scrubValue
                    } else if (duration > 0) {
                        (position.toFloat() / duration).coerceIn(0f, 1f)
                    } else 0f,
                    onValueChange = {
                        scrubbing = true
                        scrubValue = it
                    },
                    onValueChangeFinished = {
                        if (duration > 0) player.seekTo((scrubValue * duration).toLong())
                        scrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0x66FFFFFF)
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(
                    if (duration > 0) formatMs(duration) else "--:--",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ShortsAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 10.sp)
    }
}
