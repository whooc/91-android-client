package com.whooc.nineone.data

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * ExoPlayer factory.
 *
 * The important part is the data source: every `/p/stream/...` request is
 * authenticated by the `vs_admin` cookie, and the browser gets that for free
 * because it is same-origin. A bare `DefaultHttpDataSource` would send no
 * cookies and get a 401, so the media stack has to share the same OkHttp client
 * (and therefore the same persistent cookie jar) as the API.
 */
object Players {

    const val USER_AGENT = "NineOneApp/2.0"

    /** Every media request is cookie-authenticated, so both players share it. */
    private fun cookieDataSource(context: Context): DefaultDataSource.Factory {
        val okhttp = OkHttpDataSource.Factory(Http.client).setUserAgent(USER_AGENT)
        return DefaultDataSource.Factory(context, okhttp)
    }

    fun build(context: Context): ExoPlayer {
        val mediaSourceFactory = DefaultMediaSourceFactory(cookieDataSource(context))

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    /**
     * Muted, looping player for the grid previews (`/p/preview/{id}`, a ~12s
     * teaser). It never takes audio focus and never keeps the screen awake —
     * it is decoration, not playback.
     */
    fun buildPreview(context: Context): ExoPlayer =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cookieDataSource(context)))
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }

    /** Builds a playable item, or null when the server gave us no source. */
    fun mediaItem(videoId: String, videoSrc: String, title: String? = null): MediaItem? {
        val url = MediaUrls.resolve(videoSrc)
        if (url.isBlank()) return null
        return MediaItem.Builder()
            .setUri(url)
            .setMediaId(videoId)
            .apply { title?.let { setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(it).build()) } }
            .build()
    }

    fun isPlaying(player: Player?): Boolean = player?.isPlaying == true
}
