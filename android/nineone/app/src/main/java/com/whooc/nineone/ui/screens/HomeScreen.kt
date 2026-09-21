package com.whooc.nineone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whooc.nineone.data.Api
import com.whooc.nineone.data.Store
import com.whooc.nineone.ui.components.ScreenHeader
import com.whooc.nineone.ui.components.StateBox
import com.whooc.nineone.ui.components.VideoGridCard
import com.whooc.nineone.ui.components.rememberPreviewHost
import com.whooc.nineone.ui.components.rememberPreviewTarget
import com.whooc.nineone.ui.theme.LocalTokens
import com.whooc.nineone.ui.vm.HomeData
import com.whooc.nineone.ui.vm.HomeViewModel

@Composable
fun HomeScreen(
    onOpenDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val tokens = LocalTokens.current
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val history by Store.history.collectAsState()
    val gridState = rememberLazyGridState()
    val previewHost = rememberPreviewHost()

    LaunchedEffect(Unit) { previewHost.enabled = Api.previewEnabled() }

    // A fresh batch should start at the top, otherwise the user sees the same
    // scrolled position over new content.
    LaunchedEffect(state) {
        if (state is com.whooc.nineone.ui.vm.UiState.Ready && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = com.whooc.nineone.data.Brand.name,
            subtitle = com.whooc.nineone.data.Prefs.serverUrl,
            actions = {
                IconButton(onClick = { vm.load(showSpinner = false) }, enabled = !refreshing) {
                    Icon(
                        if (refreshing) Icons.Default.Cached else Icons.Default.Refresh,
                        contentDescription = "换一批",
                        tint = tokens.textDefault
                    )
                }
            }
        )

        StateBox(
            state = state,
            onRetry = { vm.load() },
            onSettings = onOpenSettings
        ) { data: HomeData ->
            val progress = remember(history) { history.associate { it.id to it.progress } }
            val byId = remember(data) {
                (data.recommended + data.latest).associateBy { it.id }
            }
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
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "推荐",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textStrong,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.load(showSpinner = false) }) {
                            Text("换一批", color = tokens.accentText, fontSize = 13.sp)
                        }
                    }
                }

                items(data.recommended, key = { "r-${it.id}" }) { video ->
                    VideoGridCard(
                        video = video,
                        progress = progress[video.id] ?: 0f,
                        preview = previewHost,
                        onClick = { onOpenDetail(video.id) }
                    )
                }

                if (data.latest.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "最新入库",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textStrong
                        )
                    }
                    items(data.latest, key = { "l-${it.id}" }) { video ->
                        VideoGridCard(
                            video = video,
                            progress = progress[video.id] ?: 0f,
                            preview = previewHost,
                            onClick = { onOpenDetail(video.id) }
                        )
                    }
                }
            }
        }
    }
}
