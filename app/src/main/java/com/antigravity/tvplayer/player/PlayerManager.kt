package com.antigravity.tvplayer.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@androidx.annotation.OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private var sharedDataSourceFactory: DefaultDataSource.Factory? = null
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var fallbackUrlProvider: ((String) -> String)? = null
    private var hasAttemptedFallback = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
                }
                Player.STATE_READY -> {
                    _uiState.update { it.copy(isBuffering = false, isPlaying = exoPlayer?.isPlaying == true, errorMessage = null) }
                }
                Player.STATE_ENDED -> {
                    _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                }
                Player.STATE_IDLE -> {
                    _uiState.update { it.copy(isPlaying = false) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _uiState.update {
                it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val currentUrl = _uiState.value.currentStreamUrl
            if (!hasAttemptedFallback && fallbackUrlProvider != null && currentUrl.isNotEmpty()) {
                val fallbackUrl = fallbackUrlProvider?.invoke(currentUrl)
                if (fallbackUrl != null && fallbackUrl != currentUrl) {
                    hasAttemptedFallback = true
                    _uiState.update { it.copy(isUsingFallback = true, isBuffering = true) }
                    playInternal(fallbackUrl)
                    return
                }
            }

            val friendlyMsg = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Server returned HTTP stream error"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Stream connection failed"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Stream connection timed out"
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Invalid stream format"
                else -> error.localizedMessage ?: "Playback error"
            }
            _uiState.update { it.copy(isBuffering = false, isPlaying = false, errorMessage = friendlyMsg) }
        }
    }

    fun initializePlayer(userAgent: String = "IPTVSmartersPro/3.1.5 (Linux; Android TV)"): ExoPlayer {
        exoPlayer?.let { return it }

        // Configure hardware acceleration renderers
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        // Fast channel-zapping TV load control with controlled memory footprint
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        sharedDataSourceFactory = dataSourceFactory

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(playerListener)
        player.playWhenReady = true
        exoPlayer = player
        return player
    }

    fun play(url: String, fallbackProvider: ((String) -> String)? = null) {
        hasAttemptedFallback = false
        fallbackUrlProvider = fallbackProvider
        _uiState.update {
            it.copy(
                currentStreamUrl = url,
                isUsingFallback = false,
                isBuffering = true,
                errorMessage = null
            )
        }
        playInternal(url)
    }

    private fun playInternal(url: String) {
        val player = exoPlayer ?: initializePlayer()
        val mediaItem = MediaItem.fromUri(url)

        val dataSourceFactory = sharedDataSourceFactory ?: DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(12_000)
                .setReadTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true)
        ).also { sharedDataSourceFactory = it }

        val mediaSource: MediaSource = if (url.endsWith(".m3u8", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun stop() {
        exoPlayer?.stop()
        _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _uiState.update { PlayerUiState() }
    }
}
