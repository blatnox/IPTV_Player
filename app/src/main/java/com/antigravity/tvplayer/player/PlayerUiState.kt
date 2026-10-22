package com.antigravity.tvplayer.player

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val currentStreamUrl: String = "",
    val isUsingFallback: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
)
