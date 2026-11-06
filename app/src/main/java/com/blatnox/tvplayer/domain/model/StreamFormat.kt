package com.blatnox.tvplayer.domain.model

enum class StreamFormat(val extension: String, val displayName: String) {
    M3U8("m3u8", "HLS (.m3u8)"),
    TS("ts", "MPEG-TS (.ts)");

    companion object {
        fun fromExtension(ext: String?): StreamFormat =
            if (ext?.lowercase() == "ts") TS else M3U8
    }
}
