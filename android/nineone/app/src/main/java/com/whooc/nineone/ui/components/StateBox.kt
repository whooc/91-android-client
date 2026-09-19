package com.whooc.nineone.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whooc.nineone.ui.vm.UiState

/** Renders the three loading states with a single retry hook. */
@Composable
fun <T> StateBox(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onSettings: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    Box(modifier.fillMaxSize()) {
        when (state) {
            is UiState.Loading -> LoadingBox()
            is UiState.Failed -> ErrorBox(state.message, onRetry = onRetry, onSettings = onSettings)
            is UiState.Ready -> content(state.data)
        }
    }
}
