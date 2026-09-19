package com.whooc.nineone.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models. Field names mirror the Go DTOs in `backend/internal/api`
 * (`VideoDTO`, `VideoCardDTO`, `VideoDetailDTO`, `TagDTO`, ...).
 *
 * Every URL field the server hands back is *relative* (`/p/thumb/xxx`,
 * `/p/stream/drive/file`), so it must go through [MediaUrls.resolve] before use.
 */

@Serializable
data class Video(
    val id: String = "",
    val href: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val previewSrc: String = "",
    val previewDuration: Int = 0,
    val previewStrategy: String = "",
    val duration: String = "",
    val badges: List<String> = emptyList(),
    val sourceLabel: String = "",
    val author: String = "",
    val views: Int = 0,
    val favorites: Int = 0,
    val comments: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val publishedAt: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class VideoDetail(
    val id: String = "",
    val href: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val previewSrc: String = "",
    val previewDuration: Int = 0,
    val previewStrategy: String = "",
    val duration: String = "",
    val badges: List<String> = emptyList(),
    val sourceLabel: String = "",
    val author: String = "",
    val views: Int = 0,
    val favorites: Int = 0,
    val comments: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val publishedAt: String = "",
    val tags: List<String> = emptyList(),
    val videoSrc: String = "",
    val mediaType: String = "",
    val poster: String = "",
    val description: String = "",
    val embedUrl: String = "",
    val authorProfile: AuthorProfile = AuthorProfile(),
    val commentsList: List<Comment> = emptyList()
) {
    fun toVideo(): Video = Video(
        id = id, href = href, title = title, thumbnail = thumbnail,
        previewSrc = previewSrc, previewDuration = previewDuration,
        previewStrategy = previewStrategy, duration = duration, badges = badges,
        sourceLabel = sourceLabel, author = author, views = views,
        favorites = favorites, comments = comments, likes = likes,
        dislikes = dislikes, publishedAt = publishedAt, tags = tags
    )
}

@Serializable
data class AuthorProfile(
    val id: String = "",
    val name: String = "",
    val href: String = "",
    val badges: List<String> = emptyList()
)

@Serializable
data class Comment(
    val id: String = "",
    val author: String = "",
    val body: String = "",
    val createdAt: String = "",
    val likes: Int = 0
)

@Serializable
data class Tag(
    val id: String = "",
    val label: String = "",
    val count: Int = 0
)

@Serializable
data class Subtitle(
    val name: String = "",
    val label: String = "",
    val language: String = "",
    val ext: String = "",
    val type: String = "",
    val url: String = "",
    val source: String = ""
)

@Serializable
data class ListPage(
    val items: List<Video> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 24
)

@Serializable
data class FeedPage(
    val items: List<Video> = emptyList(),
    val total: Int = 0,
    val feedToken: String = "",
    val nextCursor: Int = 0,
    val exhausted: Boolean = false
)

/** `/api/shorts/next` — a feed item that already carries a playable URL. */
@Serializable
data class ShortsItem(
    val id: String = "",
    val href: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val previewSrc: String = "",
    val previewDuration: Int = 0,
    val previewStrategy: String = "",
    val duration: String = "",
    val badges: List<String> = emptyList(),
    val sourceLabel: String = "",
    val author: String = "",
    val views: Int = 0,
    val favorites: Int = 0,
    val comments: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val publishedAt: String = "",
    val tags: List<String> = emptyList(),
    val videoSrc: String = "",
    val poster: String = "",
    val backgroundPoster: String = "",
    val sizeBytes: Long = 0,
    val durationSeconds: Int = 0,
    val feedCursor: Int = 0
) {
    fun toVideo(): Video = Video(
        id = id, href = href, title = title, thumbnail = thumbnail,
        previewSrc = previewSrc, previewDuration = previewDuration,
        previewStrategy = previewStrategy, duration = duration, badges = badges,
        sourceLabel = sourceLabel, author = author, views = views,
        favorites = favorites, comments = comments, likes = likes,
        dislikes = dislikes, publishedAt = publishedAt, tags = tags
    )
}

@Serializable
data class ShortsPage(
    val items: List<ShortsItem> = emptyList(),
    val total: Int = 0,
    val feedToken: String = "",
    val nextCursor: Int = 0,
    val roundComplete: Boolean = false
)

@Serializable
data class SessionInfo(
    val authenticated: Boolean = false,
    val role: String = ""
)

@Serializable
data class LoginResult(
    val ok: Boolean = false,
    val role: String = ""
)

@Serializable
data class ThemeInfo(
    val theme: String = "dark"
)

@Serializable
data class PreviewSettings(
    val previewEnabled: Boolean = true
)

@Serializable
data class ViewsResult(
    val views: Int = 0
)

@Serializable
data class LikesResult(
    val likes: Int = 0
)

/** Error envelope used by `writeErr` on the server. */
@Serializable
data class ApiError(
    @SerialName("error") val message: String = ""
)
