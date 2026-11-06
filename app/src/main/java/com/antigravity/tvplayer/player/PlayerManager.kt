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
    private var streamCandidates: List<String> = emptyList()
    private var candidateIndex = 0
    private var retryCountOnCandidate = 0
    private val maxRetriesPerCandidate = 2
    var onStreamSuccess: ((successfulUrl: String) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
                    startStallWatchdog()
                }
                Player.STATE_READY -> {
                    cancelWatchdogAndReconnect()
                    retryCountOnCandidate = 0
                    val currentUrl = _uiState.value.currentStreamUrl.ifEmpty { primaryStreamUrl }
                    if (currentUrl.isNotEmpty()) {
                        onStreamSuccess?.invoke(currentUrl)
                    }
                    val isBackup = candidateIndex > 0
                    val host = extractHost(currentUrl)
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            retryAttempt = 0,
                            isPlaying = exoPlayer?.isPlaying == true,
                            isUsingBackupServer = isBackup,
                            activeServerHost = host,
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

            // 1. Retry current candidate if attempts remain (up to 2 retries)
            if (retryCountOnCandidate < maxRetriesPerCandidate && currentUrl.isNotEmpty()) {
                retryCountOnCandidate++
                val backoffMs = retryCountOnCandidate * 1000L
                _uiState.update {
                    it.copy(
                        isBuffering = true,
                        retryAttempt = retryCountOnCandidate,
                        errorMessage = null
                    )
                }
                reconnectJob = scope.launch {
                    delay(backoffMs)
                    playInternal(currentUrl)
                }
                return
            }

            // 2. Failover to next candidate URL (alternate format or backup server)
            if (candidateIndex + 1 < streamCandidates.size) {
                candidateIndex++
                retryCountOnCandidate = 0
                val nextUrl = streamCandidates[candidateIndex]
                val isBackup = candidateIndex > 0
                val host = extractHost(nextUrl)
                val msg = if (isBackup) {
                    "Switching to backup mirror (${candidateIndex + 1}/${streamCandidates.size})…"
                } else {
                    "Trying stream fallback…"
                }

                _uiState.update {
                    it.copy(
                        currentStreamUrl = nextUrl,
                        isUsingFallback = nextUrl.endsWith(".ts", ignoreCase = true),
                        isUsingBackupServer = isBackup,
                        activeServerHost = host,
                        isBuffering = true,
                        retryAttempt = candidateIndex + 1,
                        errorMessage = msg
                    )
                }
                reconnectJob = scope.launch {
                    delay(600L)
                    playInternal(nextUrl)
                }
                return
            }

            // 3. All candidates exhausted
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
                    errorMessage = if (streamCandidates.size > 1) "All servers/mirrors failed: $friendlyMsg" else friendlyMsg
                )
            }
        }
    }

    private fun startStallWatchdog() {
        stallWatchdogJob?.cancel()
        stallWatchdogJob = scope.launch {
            delay(12_000L)
            // If still stuck buffering after 12 seconds, trigger failover
            if (_uiState.value.isBuffering && exoPlayer?.playbackState == Player.STATE_BUFFERING) {
                if (candidateIndex + 1 < streamCandidates.size) {
                    candidateIndex++
                    retryCountOnCandidate = 0
                    val nextUrl = streamCandidates[candidateIndex]
                    val isBackup = candidateIndex > 0
                    val host = extractHost(nextUrl)
                    _uiState.update {
                        it.copy(
                            currentStreamUrl = nextUrl,
                            isUsingFallback = nextUrl.endsWith(".ts", ignoreCase = true),
                            isUsingBackupServer = isBackup,
                            activeServerHost = host,
                            retryAttempt = candidateIndex + 1,
                            errorMessage = "Buffering stalled. Switching server…"
                        )
                    }
                    playInternal(nextUrl)
                } else if (retryCountOnCandidate < maxRetriesPerCandidate) {
                    retryCountOnCandidate++
                    val activeUrl = _uiState.value.currentStreamUrl.ifEmpty { primaryStreamUrl }
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

    fun initializePlayer(userAgent: String = "TVPlayer/1.0 (Linux; Android TV)"): ExoPlayer {
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

    fun play(candidates: List<String>) {
        cancelWatchdogAndReconnect()
        streamCandidates = candidates.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (streamCandidates.isEmpty()) return

        candidateIndex = 0
        retryCountOnCandidate = 0
        val firstUrl = streamCandidates.first()
        primaryStreamUrl = firstUrl

        val isBackup = candidateIndex > 0
        val host = extractHost(firstUrl)

        _uiState.update {
            it.copy(
                currentStreamUrl = firstUrl,
                isUsingFallback = firstUrl.endsWith(".ts", ignoreCase = true),
                isUsingBackupServer = isBackup,
                activeServerHost = host,
                isBuffering = true,
                retryAttempt = 0,
                errorMessage = null
            )
        }
        playInternal(firstUrl)
    }

    fun play(url: String, fallbackProvider: ((String) -> String)? = null) {
        val candidates = mutableListOf(url)
        val fb = fallbackProvider?.invoke(url)
        if (!fb.isNullOrBlank() && fb != url) {
            candidates.add(fb)
        }
        play(candidates)
    }

    fun retry() {
        if (streamCandidates.isNotEmpty()) {
            play(streamCandidates)
        } else {
            val url = primaryStreamUrl.ifEmpty { _uiState.value.currentStreamUrl }
            if (url.isNotEmpty()) {
                play(listOf(url))
            }
        }
    }

    private fun extractHost(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: ""
        } catch (_: Exception) {
            ""
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
