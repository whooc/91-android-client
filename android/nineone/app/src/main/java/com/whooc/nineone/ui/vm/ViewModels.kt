package com.whooc.nineone.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.FeedPage
import com.whooc.nineone.data.ShortsItem
import com.whooc.nineone.data.ShortsPage
import com.whooc.nineone.data.Tag
import com.whooc.nineone.data.Video
import com.whooc.nineone.data.VideoDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Failed(val message: String) : UiState<Nothing>
}

fun Throwable.readable(): String =
    message?.takeIf { it.isNotBlank() } ?: (this::class.simpleName ?: "未知错误")

// ------------------------------------------------------------------- 首页

data class HomeData(
    val recommended: List<Video> = emptyList(),
    val latest: List<Video> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    /** `/api/home` reshuffles per session, so this is the "换一批" action too. */
    fun load(showSpinner: Boolean = true) {
        job?.cancel()
        job = viewModelScope.launch {
            if (showSpinner) _state.value = UiState.Loading else _refreshing.value = true
            runCatching {
                val recommended = Api.home(12)
                val latest = runCatching { Api.homeLatest(12) }.getOrDefault(emptyList())
                HomeData(recommended, latest)
            }.onSuccess {
                _state.value = UiState.Ready(it)
            }.onFailure {
                if (showSpinner) _state.value = UiState.Failed(it.readable())
            }
            _refreshing.value = false
        }
    }
}

// ------------------------------------------------------- 列表 / 搜索 / 标签

data class ListData(
    val items: List<Video> = emptyList(),
    val total: Int = 0,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false
)

class ListViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<ListData>>(UiState.Loading)
    val state: StateFlow<UiState<ListData>> = _state.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    private val _sort = MutableStateFlow<String?>(null)
    val sort: StateFlow<String?> = _sort.asStateFlow()

    private var feedToken: String? = null
    private var cursor = 0
    private var job: Job? = null

    init {
        loadTags()
        reload()
    }

    private fun loadTags() {
        viewModelScope.launch {
            runCatching { Api.tags() }.onSuccess { _tags.value = it }
        }
    }

    fun setQuery(value: String) {
        if (_query.value == value) return
        _query.value = value
        reload()
    }

    fun setTag(tag: String?) {
        if (_activeTag.value == tag) return
        _activeTag.value = tag
        reload()
    }

    fun setSort(value: String?) {
        if (_sort.value == value) return
        _sort.value = value
        reload()
    }

    /** A new snapshot feed must be requested whenever the filters change. */
    fun reload() {
        job?.cancel()
        feedToken = null
        cursor = 0
        job = viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                Api.feed(
                    count = BATCH,
                    cursor = 0,
                    feedToken = null,
                    kind = "listing",
                    q = _query.value,
                    tag = _activeTag.value,
                    sort = _sort.value
                )
            }
                .onSuccess { apply(it, reset = true) }
                .onFailure { _state.value = UiState.Failed(it.readable()) }
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        if (current.loadingMore || current.endReached) return
        _state.value = UiState.Ready(current.copy(loadingMore = true))
        viewModelScope.launch {
            runCatching {
                Api.feed(
                    count = BATCH,
                    cursor = cursor,
                    feedToken = feedToken,
                    kind = "listing",
                    q = _query.value,
                    tag = _activeTag.value,
                    sort = _sort.value
                )
            }.onSuccess { apply(it, reset = false) }
                .onFailure {
                    _state.value = UiState.Ready(current.copy(loadingMore = false))
                }
        }
    }

    private fun apply(page: FeedPage, reset: Boolean) {
        feedToken = page.feedToken.ifBlank { feedToken }
        cursor = page.nextCursor
        val previous = if (reset) emptyList() else (_state.value as? UiState.Ready)?.data?.items.orEmpty()
        val merged = previous + page.items.filter { item -> previous.none { it.id == item.id } }
        _state.value = UiState.Ready(
            ListData(
                items = merged,
                total = page.total,
                loadingMore = false,
                endReached = page.exhausted || page.items.isEmpty()
            )
        )
    }

    private companion object {
        const val BATCH = 24
    }
}

// -------------------------------------------------------------- 短视频流

data class ShortsData(
    val items: List<ShortsItem> = emptyList(),
    val loadingMore: Boolean = false
)

class ShortsViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<ShortsData>>(UiState.Loading)
    val state: StateFlow<UiState<ShortsData>> = _state.asStateFlow()

    private var feedToken: String? = null
    private var cursor = 0
    private var loading = false

    init {
        loadMore(reset = true)
    }

    fun loadMore(reset: Boolean = false) {
        if (loading) return
        loading = true
        if (reset) {
            feedToken = null
            cursor = 0
        }
        viewModelScope.launch {
            val previous = if (reset) emptyList() else (_state.value as? UiState.Ready)?.data?.items.orEmpty()
            if (reset) _state.value = UiState.Loading
            runCatching { Api.shortsNext(BATCH, cursor, feedToken) }
                .onSuccess { page: ShortsPage ->
                    feedToken = page.feedToken.ifBlank { feedToken }
                    cursor = page.nextCursor
                    _state.value = UiState.Ready(
                        ShortsData(
                            items = previous + page.items.filter { item -> previous.none { it.id == item.id } },
                            loadingMore = false
                        )
                    )
                }
                .onFailure {
                    if (reset || previous.isEmpty()) {
                        _state.value = UiState.Failed(it.readable())
                    } else {
                        _state.value = UiState.Ready(ShortsData(previous, false))
                    }
                }
            loading = false
        }
    }

    private companion object {
        const val BATCH = 6
    }
}

// ---------------------------------------------------------------- 详情页

data class DetailData(
    val detail: VideoDetail,
    val related: List<Video> = emptyList()
)

class DetailViewModel(private val videoId: String) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DetailData>>(UiState.Loading)
    val state: StateFlow<UiState<DetailData>> = _state.asStateFlow()

    private val _likes = MutableStateFlow(0)
    val likes: StateFlow<Int> = _likes.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                val detail = Api.video(videoId)
                val related = runCatching { Api.recommendations(videoId) }.getOrDefault(emptyList())
                DetailData(detail, related)
            }.onSuccess {
                _likes.value = it.detail.likes
                _state.value = UiState.Ready(it)
            }.onFailure {
                _state.value = UiState.Failed(it.readable())
            }
        }
    }

    /** Fire-and-forget: the server only keeps a global counter. */
    fun markViewed() {
        viewModelScope.launch { runCatching { Api.markViewed(videoId) } }
    }

    fun toggleLike(liked: Boolean) {
        val before = _likes.value
        _likes.update { if (liked) it + 1 else (it - 1).coerceAtLeast(0) }
        viewModelScope.launch {
            runCatching { Api.like(videoId, liked) }
                .onSuccess { _likes.value = it }
                .onFailure { _likes.value = before }
        }
    }
}
