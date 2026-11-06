package com.blatnox.tvplayer

import com.blatnox.tvplayer.data.api.XtreamApiService
import com.blatnox.tvplayer.data.api.dto.AuthResponseDto
import com.blatnox.tvplayer.data.api.dto.CategoryDto
import com.blatnox.tvplayer.data.api.dto.StreamDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class XtreamApiParsingTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: XtreamApiService

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        apiService = retrofit.create(XtreamApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun parseAuthResponse_success() = runBlocking {
        val jsonPayload = """
            {
              "user_info": {
                "username": "tester",
                "password": "secretpassword",
                "message": "Auth success",
                "auth": 1,
                "status": "Active",
                "exp_date": "1735689600",
                "is_trial": "0",
                "active_cons": "0",
                "created_at": "1609459200",
                "max_connections": "2",
                "allowed_output_formats": ["m3u8", "ts"]
              },
              "server_info": {
                "url": "http://mock.iptv.com",
                "port": "8080",
                "server_protocol": "http",
                "timezone": "UTC"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonPayload))

        val response: AuthResponseDto = apiService.authenticate(
            url = mockWebServer.url("/player_api.php").toString(),
            username = "tester",
            password = "secretpassword"
        )

        assertNotNull(response.userInfo)
        assertEquals("tester", response.userInfo?.username)
        assertEquals(1, response.userInfo?.auth)
        assertEquals("Active", response.userInfo?.status)
        assertEquals("1735689600", response.userInfo?.expDate)
        assertEquals("2", response.userInfo?.maxConnections)
        assertEquals("http://mock.iptv.com", response.serverInfo?.url)
    }

    @Test
    fun parseCategoriesResponse_handlesStringAndIntIds() = runBlocking {
        val jsonPayload = """
            [
              {
                "category_id": "10",
                "category_name": "Sports HD",
                "parent_id": 0
              },
              {
                "category_id": 25,
                "category_name": "News USA",
                "parent_id": 0
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonPayload))

        val categories: List<CategoryDto> = apiService.getLiveCategories(
            url = mockWebServer.url("/player_api.php").toString(),
            username = "user",
            password = "pwd"
        )

        assertEquals(2, categories.size)
        assertEquals("10", categories[0].categoryId)
        assertEquals("Sports HD", categories[0].categoryName)
        assertEquals("25", categories[1].categoryId)
        assertEquals("News USA", categories[1].categoryName)
    }

    @Test
    fun parseLiveStreamsResponse_handlesVariousFields() = runBlocking {
        val jsonPayload = """
            [
              {
                "num": 1,
                "name": "Sky Sports Main Event",
                "stream_type": "live",
                "stream_id": 9876,
                "stream_icon": "http://img.example.com/sky.png",
                "epg_channel_id": "sky.sports.uk",
                "category_id": "10",
                "tv_archive": 1
              },
              {
                "num": 2,
                "name": "BBC One HD",
                "stream_type": "live",
                "stream_id": "54321",
                "stream_icon": null,
                "epg_channel_id": null,
                "category_id": 12,
                "tv_archive": 0
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonPayload))

        val streams: List<StreamDto> = apiService.getLiveStreams(
            url = mockWebServer.url("/player_api.php").toString(),
            username = "user",
            password = "pwd"
        )

        assertEquals(2, streams.size)
        assertEquals(9876, streams[0].streamId)
        assertEquals("Sky Sports Main Event", streams[0].name)
        assertEquals("http://img.example.com/sky.png", streams[0].streamIcon)
        assertEquals("10", streams[0].categoryId)

        // Verify string streamId and integer categoryId in second item
        assertEquals(54321, streams[1].streamId)
        assertEquals("BBC One HD", streams[1].name)
        assertEquals("12", streams[1].categoryId)
    }
}
