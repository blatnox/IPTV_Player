package com.antigravity.tvplayer.data.api

import com.antigravity.tvplayer.domain.model.StreamFormat

object XtreamUrlBuilder {

    fun normalizeServerUrl(rawUrl: String): String {
        var trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        // If scheme is missing, default to http://
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed = "http://$trimmed"
        }

        // Remove trailing slashes
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        return trimmed
    }

    fun buildPlayerApiUrl(serverUrl: String): String {
        val normalized = normalizeServerUrl(serverUrl)
        return "$normalized/player_api.php"
    }

    fun buildStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        format: StreamFormat = StreamFormat.M3U8
    ): String {
        val normalized = normalizeServerUrl(serverUrl)
        return "$normalized/live/$username/$password/$streamId.${format.extension}"
    }

    fun buildFallbackStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        currentFormat: StreamFormat
    ): String {
        val fallbackFormat = if (currentFormat == StreamFormat.M3U8) StreamFormat.TS else StreamFormat.M3U8
        return buildStreamUrl(serverUrl, username, password, streamId, fallbackFormat)
    }
}
