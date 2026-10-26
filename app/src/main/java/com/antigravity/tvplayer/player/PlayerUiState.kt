package com.antigravity.tvplayer.player

import androidx.compose.runtime.Immutable

@Immutable
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val retryAttempt: Int = 0,
    val errorMessage: String? = null,
    val currentStreamUrl: String = "",
    val isUsingFallback: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
)
