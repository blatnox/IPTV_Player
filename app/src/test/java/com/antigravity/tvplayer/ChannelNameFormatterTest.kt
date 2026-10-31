package com.antigravity.tvplayer

import com.antigravity.tvplayer.domain.model.ChannelNameFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelNameFormatterTest {

    @Test
    fun parse_detectsSectionSeparators() {
        val hashSeparator = ChannelNameFormatter.parse("#### CRIANCAS ####")
        assertTrue(hashSeparator.isSeparator)
        assertEquals("CRIANCAS", hashSeparator.displayName)
        assertNull(hashSeparator.qualityBadge)

        val dashSeparator = ChannelNameFormatter.parse("--- DESPORTO ---")
        assertTrue(dashSeparator.isSeparator)
        assertEquals("DESPORTO", dashSeparator.displayName)
        assertNull(dashSeparator.qualityBadge)

        val emptySymbols = ChannelNameFormatter.parse("########")
        assertTrue(emptySymbols.isSeparator)
        assertEquals("Section", emptySymbols.displayName)
    }

    @Test
    fun parse_extractsHighestPriorityQualityBadge() {
        val uhd = ChannelNameFormatter.parse("SPORT TV 1 4K UHD")
        assertFalse(uhd.isSeparator)
        assertEquals("4K", uhd.qualityBadge)

        val fhd = ChannelNameFormatter.parse("RTP 1 FHD")
        assertFalse(fhd.isSeparator)
        assertEquals("FHD", fhd.qualityBadge)

        val raw = ChannelNameFormatter.parse("SIC RAW")
        assertFalse(raw.isSeparator)
        assertEquals("RAW", raw.qualityBadge)

        val hevc = ChannelNameFormatter.parse("TVI HEVC")
        assertFalse(hevc.isSeparator)
        assertEquals("HEVC", hevc.qualityBadge)

        val hd = ChannelNameFormatter.parse("CMTV HD")
        assertFalse(hd.isSeparator)
        assertEquals("HD", hd.qualityBadge)

        val standard = ChannelNameFormatter.parse("RTP MEMORIA")
        assertFalse(standard.isSeparator)
        assertNull(standard.qualityBadge)
    }
}
