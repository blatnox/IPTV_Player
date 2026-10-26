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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconnectJob: Job? = null
    private var stallWatchdogJob: Job? = null

    private var primaryStreamUrl: String = ""
    private var fallbackUrlProvider: ((String) -> String)? = null
    private var hasAttemptedFallback = false
    private var retryCount = 0
    private val maxAutoRetries = 3

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
                    startStallWatchdog()
                }
                Player.STATE_READY -> {
                    cancelWatchdogAndReconnect()
                    retryCount = 0
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            retryAttempt = 0,
                            isPlaying = exoPlayer?.isPlaying == true,
                            errorMessage = null
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    // Live TV streams send EOS on segment/resolution changes or brief server drops;
                    // automatically reconnect to keep live playback continuous.
                    cancelWatchdogAndReconnect()
                    val activeUrl = _uiState.value.currentStreamUrl.ifEmpty { primaryStreamUrl }
                    if (activeUrl.isNotEmpty()) {
                        reconnectJob = scope.launch {
                            _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
                            delay(400L)
                            playInternal(activeUrl)
                        }
                    } else {
                        _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                    }
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
            cancelWatchdogAndReconnect()

            // Recover immediately if live window drifted
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                exoPlayer?.let { player ->
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.playWhenReady = true
                    return
                }
            }

            val currentUrl = _uiState.value.currentStreamUrl.ifEmpty { primaryStreamUrl }

            // Auto-reconnect up to maxAutoRetries (3 attempts) with short backoff
            if (retryCount < maxAutoRetries && currentUrl.isNotEmpty()) {
                retryCount++
                val nextUrl = if (!hasAttemptedFallback && retryCount >= 2 && fallbackUrlProvider != null) {
                    val fb = fallbackUrlProvider?.invoke(primaryStreamUrl)
                    if (!fb.isNullOrEmpty() && fb != currentUrl) {
                        hasAttemptedFallback = true
                        fb
                    } else {
                        currentUrl
                    }
                } else {
                    currentUrl
                }

                val backoffMs = retryCount * 1200L
                _uiState.update {
                    it.copy(
                        currentStreamUrl = nextUrl,
                        isUsingFallback = hasAttemptedFallback,
                        isBuffering = true,
                        retryAttempt = retryCount,
                        errorMessage = null
                    )
                }
                reconnectJob = scope.launch {
                    delay(backoffMs)
                    playInternal(nextUrl)
                }
                return
            }

            val friendlyMsg = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Server returned HTTP stream error"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Stream connection failed"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Stream connection timed out"
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Invalid stream format"
                else -> error.localizedMessage ?: "Playback error"
            }
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    retryAttempt = 0,
                    errorMessage = friendlyMsg
                )
            }
        }
    }

    private fun startStallWatchdog() {
        stallWatchdogJob?.cancel()
        stallWatchdogJob = scope.launch {
            delay(12_000L)
            // If still stuck buffering after 12 seconds, trigger auto-reconnect
            if (_uiState.value.isBuffering && exoPlayer?.playbackState == Player.STATE_BUFFERING) {
                val activeUrl = _uiState.value.currentStreamUrl.ifEmpty { primaryStreamUrl }
                if (activeUrl.isNotEmpty() && retryCount < maxAutoRetries) {
                    retryCount++
                    _uiState.update { it.copy(retryAttempt = retryCount, errorMessage = null) }
                    playInternal(activeUrl)
                }
            }
        }
    }

    private fun cancelWatchdogAndReconnect() {
        stallWatchdogJob?.cancel()
        stallWatchdogJob = null
        reconnectJob?.cancel()
        reconnectJob = null
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
        cancelWatchdogAndReconnect()
        primaryStreamUrl = url
        retryCount = 0
        hasAttemptedFallback = false
        fallbackUrlProvider = fallbackProvider
        _uiState.update {
            it.copy(
                currentStreamUrl = url,
                isUsingFallback = false,
                isBuffering = true,
                retryAttempt = 0,
                errorMessage = null
            )
        }
        playInternal(url)
    }

    fun retry() {
        val url = primaryStreamUrl.ifEmpty { _uiState.value.currentStreamUrl }
        if (url.isNotEmpty()) {
            play(url, fallbackUrlProvider)
        }
    }

    private fun playInternal(url: String) {
        val player = exoPlayer ?: initializePlayer()
        // Close any active stream socket first so 1-connection servers never see 2 open sockets
        player.stop()
        player.clearMediaItems()

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
        cancelWatchdogAndReconnect()
        primaryStreamUrl = ""
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _uiState.update { it.copy(isPlaying = false, isBuffering = false, retryAttempt = 0) }
    }

    fun release() {
        cancelWatchdogAndReconnect()
        primaryStreamUrl = ""
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _uiState.update { PlayerUiState() }
    }
}
