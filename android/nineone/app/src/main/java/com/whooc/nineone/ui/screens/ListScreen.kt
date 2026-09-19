package com.whooc.nineone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Store
import com.whooc.nineone.ui.components.EmptyBox
import com.whooc.nineone.ui.components.InlineProgress
import com.whooc.nineone.ui.components.ScreenHeader
import com.whooc.nineone.ui.components.StateBox
import com.whooc.nineone.ui.components.VideoGridCard
import com.whooc.nineone.ui.components.rememberPreviewHost
import com.whooc.nineone.ui.components.rememberPreviewTarget
import com.whooc.nineone.ui.theme.LocalTokens
import com.whooc.nineone.ui.vm.ListData
import com.whooc.nineone.ui.vm.ListViewModel
import kotlinx.coroutines.launch

private val SORTS = listOf(
    null to "默认",
    "latest" to "最新",
    "hot" to "最热"
)

@Composable
fun ListScreen(
    onOpenDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    vm: ListViewModel = viewModel()
) {
    val tokens = LocalTokens.current
    val state by vm.state.collectAsState()
    val tags by vm.tags.collectAsState()
    val query by vm.query.collectAsState()
    val activeTag by vm.activeTag.collectAsState()
    val sort by vm.sort.collectAsState()
    val history by Store.history.collectAsState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val previewHost = rememberPreviewHost()

    LaunchedEffect(Unit) { previewHost.enabled = Api.previewEnabled() }

    // Infinite scroll: fire loadMore a screen before the end.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore, state) {
        if (shouldLoadMore) vm.loadMore()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "列表")

        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            placeholder = { Text("搜索标题 / 标签", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = tokens.textMuted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除", tint = tokens.textMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.reload() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tokens.accentText,
                unfocusedBorderColor = tokens.divider,
                cursorColor = tokens.accentText,
                focusedTextColor = tokens.textStrong,
                unfocusedTextColor = tokens.textStrong
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Plain forEach instead of `items(...)`: the lazy-list and lazy-grid
            // `items` extensions share a name and cannot both be imported.
            SORTS.forEach { (key, label) ->
                item(key = "sort-${key ?: "default"}") {
                    FilterChip(
                        selected = sort == key,
                        onClick = { vm.setSort(key) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            if (tags.isNotEmpty()) {
                item(key = "sep") {
                    Box(
                        Modifier.height(32.dp).padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("·", color = tokens.textFaint)
                    }
                }
            }
            tags.forEach { tag ->
                item(key = "tag-${tag.id}") {
                    FilterChip(
                        selected = activeTag == tag.label,
                        onClick = { vm.setTag(if (activeTag == tag.label) null else tag.label) },
                        label = { Text("${tag.label} ${tag.count}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.accent.copy(alpha = 0.20f),
                            selectedLabelColor = tokens.accentText
                        )
                    )
                }
            }
        }

        StateBox(
            state = state,
            onRetry = { vm.reload() },
            onSettings = onOpenSettings
        ) { data: ListData ->
            if (data.items.isEmpty()) {
                EmptyBox("没有匹配的内容")
                return@StateBox
            }
            val progress = remember(history) { history.associate { it.id to it.progress } }
            val byId = remember(data.items) { data.items.associateBy { it.id } }
            val previewTarget = rememberPreviewTarget(gridState, byId)
            LaunchedEffect(previewTarget?.id) { previewHost.follow(previewTarget) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildString {
                                append("共 ")
                                append(if (data.total > 0) data.total else data.items.size)
                                append(" 个结果")
                            },
                            fontSize = 12.sp,
                            color = tokens.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        if (data.items.isNotEmpty()) {
                            androidx.compose.material3.TextButton(onClick = {
                                scope.launch { gridState.scrollToItem(0) }
                            }) { Text("回到顶部", fontSize = 12.sp, color = tokens.accentText) }
                        }
                    }
                }
                items(data.items, key = { "v-${it.id}" }) { video ->
                    VideoGridCard(
                        video = video,
                        progress = progress[video.id] ?: 0f,
                        preview = previewHost,
                        onClick = { onOpenDetail(video.id) }
                    )
                }
                if (data.loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) { InlineProgress() }
                } else if (data.endReached && data.items.size > 8) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "已经到底了",
                            fontSize = 12.sp,
                            color = tokens.textFaint,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
