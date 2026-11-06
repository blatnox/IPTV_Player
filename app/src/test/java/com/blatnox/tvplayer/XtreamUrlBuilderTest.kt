package com.blatnox.tvplayer

import com.blatnox.tvplayer.data.api.XtreamUrlBuilder
import com.blatnox.tvplayer.domain.model.StreamFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamUrlBuilderTest {

    @Test
    fun normalizeServerUrl_addsHttpIfMissing() {
        val input = "example.com:8080"
        val expected = "http://example.com:8080"
        assertEquals(expected, XtreamUrlBuilder.normalizeServerUrl(input))
    }

    @Test
    fun normalizeServerUrl_preservesHttps() {
        val input = "https://secure.example.com"
        val expected = "https://secure.example.com"
        assertEquals(expected, XtreamUrlBuilder.normalizeServerUrl(input))
    }

    @Test
    fun normalizeServerUrl_stripsTrailingSlashesAndWhitespace() {
        val input = "  http://example.com:8080///  "
        val expected = "http://example.com:8080"
        assertEquals(expected, XtreamUrlBuilder.normalizeServerUrl(input))
    }

    @Test
    fun normalizeServerUrl_returnsEmptyForBlank() {
        assertEquals("", XtreamUrlBuilder.normalizeServerUrl("   "))
    }

    @Test
    fun buildPlayerApiUrl_correctlyAppendsPlayerApi() {
        val server = "http://iptv.server.net:8080"
        val expected = "http://iptv.server.net:8080/player_api.php"
        assertEquals(expected, XtreamUrlBuilder.buildPlayerApiUrl(server))
    }

    @Test
    fun buildStreamUrl_generatesHlsM3u8Url() {
        val url = XtreamUrlBuilder.buildStreamUrl(
            serverUrl = "http://iptv.server.net:8080/",
            username = "myuser",
            password = "mypassword",
            streamId = 12345,
            format = StreamFormat.M3U8
        )
        val expected = "http://iptv.server.net:8080/live/myuser/mypassword/12345.m3u8"
        assertEquals(expected, url)
    }

    @Test
    fun buildStreamUrl_generatesMpegTsUrl() {
        val url = XtreamUrlBuilder.buildStreamUrl(
            serverUrl = "http://iptv.server.net:8080",
            username = "myuser",
            password = "mypassword",
            streamId = 12345,
            format = StreamFormat.TS
        )
        val expected = "http://iptv.server.net:8080/live/myuser/mypassword/12345.ts"
        assertEquals(expected, url)
    }

    @Test
    fun buildFallbackStreamUrl_swapsFormat() {
        val fallbackFromM3u8 = XtreamUrlBuilder.buildFallbackStreamUrl(
            serverUrl = "http://iptv.server.net:8080",
            username = "u",
            password = "p",
            streamId = 99,
            currentFormat = StreamFormat.M3U8
        )
        assertEquals("http://iptv.server.net:8080/live/u/p/99.ts", fallbackFromM3u8)

        val fallbackFromTs = XtreamUrlBuilder.buildFallbackStreamUrl(
            serverUrl = "http://iptv.server.net:8080",
            username = "u",
            password = "p",
            streamId = 99,
            currentFormat = StreamFormat.TS
        )
        assertEquals("http://iptv.server.net:8080/live/u/p/99.m3u8", fallbackFromTs)
    }
}
