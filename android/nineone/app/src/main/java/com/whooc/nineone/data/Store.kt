package com.whooc.nineone.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class HistoryEntry(
    val id: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L
) {
    /** 0..1, or 0 when the duration is still unknown. */
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

@Serializable
data class FavoriteEntry(
    val id: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val addedAt: Long = 0L
)

/**
 * Watch progress and favourites, kept entirely on device.
 *
 * The backend only exposes a global view counter — there is no per-user watch
 * history endpoint — so "continue watching" and "favourites" are local. That is
 * also why they survive a server address change: they are keyed by video ID.
 */
object Store {

    private const val HISTORY_LIMIT = 500

    private lateinit var historyFile: File
    private lateinit var favoriteFile: File

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoriteEntry>>(emptyList())
    val favorites: StateFlow<List<FavoriteEntry>> = _favorites.asStateFlow()

    fun init(ctx: Context) {
        if (::historyFile.isInitialized) return
        historyFile = File(ctx.filesDir, "history.json")
        favoriteFile = File(ctx.filesDir, "favorites.json")
        _history.value = read(historyFile)
        _favorites.value = read(favoriteFile)
    }

    private inline fun <reified T> read(file: File): List<T> = runCatching {
        if (file.exists()) Http.json.decodeFromString<List<T>>(file.readText()) else emptyList()
    }.getOrDefault(emptyList())

    private inline fun <reified T> write(file: File, value: List<T>) {
        runCatching { file.writeText(Http.json.encodeToString(value)) }
    }

    // --------------------------------------------------------------- history

    fun positionOf(videoId: String): Long =
        _history.value.firstOrNull { it.id == videoId }?.positionMs ?: 0L

    fun entryOf(videoId: String): HistoryEntry? =
        _history.value.firstOrNull { it.id == videoId }

    fun recordProgress(video: Video, positionMs: Long, durationMs: Long) {
        if (video.id.isBlank()) return
        // Ignore the first second of playback and anything past the end — both
        // would otherwise poison "continue watching".
        if (positionMs < 3_000L) return
        val finished = durationMs > 0 && positionMs > durationMs - 10_000L
        val entry = HistoryEntry(
            id = video.id,
            title = video.title,
            thumbnail = video.thumbnail,
            duration = video.duration,
            positionMs = if (finished) 0L else positionMs,
            durationMs = durationMs,
            updatedAt = System.currentTimeMillis()
        )
        val next = buildList {
            add(entry)
            addAll(_history.value.filter { it.id != video.id })
        }.take(HISTORY_LIMIT)
        _history.value = next
        write(historyFile, next)
    }

    fun removeHistory(videoId: String) {
        val next = _history.value.filterNot { it.id == videoId }
        _history.value = next
        write(historyFile, next)
    }

    fun clearHistory() {
        _history.value = emptyList()
        write(historyFile, emptyList<HistoryEntry>())
    }

    // ------------------------------------------------------------- favourites

    fun isFavorite(videoId: String): Boolean = _favorites.value.any { it.id == videoId }

    fun toggleFavorite(video: Video): Boolean {
        if (video.id.isBlank()) return false
        val exists = isFavorite(video.id)
        val next = if (exists) {
            _favorites.value.filterNot { it.id == video.id }
        } else {
            buildList {
                add(
                    FavoriteEntry(
                        id = video.id,
                        title = video.title,
                        thumbnail = video.thumbnail,
                        duration = video.duration,
                        addedAt = System.currentTimeMillis()
                    )
                )
                addAll(_favorites.value)
            }
        }
        _favorites.value = next
        write(favoriteFile, next)
        return !exists
    }

    fun clearFavorites() {
        _favorites.value = emptyList()
        write(favoriteFile, emptyList<FavoriteEntry>())
    }
}
