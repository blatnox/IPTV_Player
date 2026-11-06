package com.blatnox.tvplayer.domain.model

object ChannelNameFormatter {

    private val separatorRegex = Regex("""^[\s#\-=*_•~><|+]{2,}\s*(.*?)\s*[\s#\-=*_•~><|+]{2,}$""")
    private val leadingSymbolsRegex = Regex("""^[\s#\-=*_•~]{3,}\s*(.*)$""")
    private val qualityRegex = Regex("""\b(4K|UHD|FHD|1080P|HD|720P|RAW|HEVC|H265|VIP)\b""", RegexOption.IGNORE_CASE)

    data class ParsedChannelName(
        val displayName: String,
        val qualityBadge: String?,
        val isSeparator: Boolean
    )

    fun parse(rawName: String): ParsedChannelName {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            return ParsedChannelName(displayName = "Unnamed Channel", qualityBadge = null, isSeparator = false)
        }

        separatorRegex.matchEntire(trimmed)?.let { match ->
            val inner = match.groupValues[1].trim()
                .trim('#', '-', '=', '*', '_', '•', '~', ' ')
            return ParsedChannelName(
                displayName = inner.ifEmpty { "Section" },
                qualityBadge = null,
                isSeparator = true
            )
        }

        if (trimmed.startsWith("###") || trimmed.startsWith("---") || trimmed.startsWith("===") || trimmed.startsWith("***")) {
            val cleaned = leadingSymbolsRegex.find(trimmed)?.groupValues?.getOrNull(1)
                ?.trim('#', '-', '=', '*', '_', '•', '~', ' ')
                .orEmpty()
            return ParsedChannelName(
                displayName = cleaned.ifEmpty { "Section" },
                qualityBadge = null,
                isSeparator = true
            )
        }

        val matches = qualityRegex.findAll(trimmed).map { it.value.uppercase() }.toList()
        val badge = when {
            "4K" in matches || "UHD" in matches -> "4K"
            "RAW" in matches -> "RAW"
            "FHD" in matches || "1080P" in matches -> "FHD"
            "HEVC" in matches || "H265" in matches -> "HEVC"
            "HD" in matches || "720P" in matches -> "HD"
            "VIP" in matches -> "VIP"
            else -> null
        }

        val cleanedSpaces = trimmed.replace(Regex("""\s{2,}"""), " ")
        return ParsedChannelName(
            displayName = cleanedSpaces,
            qualityBadge = badge,
            isSeparator = false
        )
    }
}
