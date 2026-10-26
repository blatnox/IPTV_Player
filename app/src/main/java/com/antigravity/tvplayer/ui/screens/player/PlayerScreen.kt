package com.antigravity.tvplayer.ui.screens.player

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.antigravity.tvplayer.domain.model.Channel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackToLiveTv: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isOverlayVisible) {
        if (!uiState.isOverlayVisible) {
            focusRequester.requestFocus()
        }
    }

    BackHandler {
        viewModel.playerManager.stop()
        onBackToLiveTv()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.playerManager.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            viewModel.zapPrevious()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.zapNext()
                            true
                        }
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            viewModel.zapNext()
                            true
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            viewModel.zapPrevious()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            viewModel.zapLastChannel()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            viewModel.toggleOverlay()
                            true
                        }
                        KeyEvent.KEYCODE_INFO,
                        KeyEvent.KEYCODE_MENU -> {
                            viewModel.showOverlay()
                            true
                        }
                        KeyEvent.KEYCODE_0 -> { viewModel.onNumberKeyPressed(0); true }
                        KeyEvent.KEYCODE_1 -> { viewModel.onNumberKeyPressed(1); true }
                        KeyEvent.KEYCODE_2 -> { viewModel.onNumberKeyPressed(2); true }
                        KeyEvent.KEYCODE_3 -> { viewModel.onNumberKeyPressed(3); true }
                        KeyEvent.KEYCODE_4 -> { viewModel.onNumberKeyPressed(4); true }
                        KeyEvent.KEYCODE_5 -> { viewModel.onNumberKeyPressed(5); true }
                        KeyEvent.KEYCODE_6 -> { viewModel.onNumberKeyPressed(6); true }
                        KeyEvent.KEYCODE_7 -> { viewModel.onNumberKeyPressed(7); true }
                        KeyEvent.KEYCODE_8 -> { viewModel.onNumberKeyPressed(8); true }
                        KeyEvent.KEYCODE_9 -> { viewModel.onNumberKeyPressed(9); true }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (playerState.isPlaying) viewModel.playerManager.pause()
                            else viewModel.playerManager.resume()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            viewModel.playerManager.resume()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            viewModel.playerManager.pause()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        // Fullscreen ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    keepScreenOn = true
                    isFocusable = false
                    isFocusableInTouchMode = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = viewModel.playerManager.initializePlayer()
                    setOnClickListener {
                        viewModel.toggleOverlay()
                    }
                }
            },
            update = { playerView ->
                val exoPlayer = viewModel.playerManager.initializePlayer()
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Click interceptor for Mouse and Touch on video area (non-focusable so DPAD stays on root)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        viewModel.toggleOverlay()
                    }
                }
        )

        // TV & Mouse Overlay
        PlayerOverlay(
            channel = uiState.currentChannel,
            playerState = playerState,
            isVisible = uiState.isOverlayVisible,
            numberBuffer = uiState.numberBuffer,
            onBack = {
                viewModel.playerManager.stop()
                onBackToLiveTv()
            },
            onZapPrevious = viewModel::zapPrevious,
            onZapNext = viewModel::zapNext,
            onTogglePlayPause = {
                if (playerState.isPlaying) viewModel.playerManager.pause()
                else viewModel.playerManager.resume()
            },
            onToggleFavorite = viewModel::toggleCurrentFavorite
        )
    }
}
