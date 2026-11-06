package com.antigravity.tvplayer

import com.antigravity.tvplayer.data.api.XtreamUrlBuilder
import com.antigravity.tvplayer.data.local.entity.AccountEntity
import com.antigravity.tvplayer.domain.model.Account
import com.antigravity.tvplayer.domain.model.StreamFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerFailoverTest {

    @Test
    fun accountEntity_backupServersSerialization_roundTrip() {
        val backups = listOf("http://pro.uhd8.top", "http://vip.wd.prouhd.top", "http://pro.vpn8k.top")
        val serialized = backups.joinToString("|")

        val entity = AccountEntity(
            id = 1L,
            serverUrl = "http://vip.wd.uhd8.top",
            username = "user123",
            backupServers = serialized
        )

        val parsedList = entity.backupServers.split('|', ',', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        assertEquals(3, parsedList.size)
        assertEquals("http://pro.uhd8.top", parsedList[0])
        assertEquals("http://vip.wd.prouhd.top", parsedList[1])
        assertEquals("http://pro.vpn8k.top", parsedList[2])
    }

    @Test
    fun candidateUrls_generateCorrectHierarchy() {
        val account = Account(
            id = 1L,
            serverUrl = "http://primary.com",
            username = "user",
            password = "pwd",
            backupServers = listOf("http://backup1.com", "http://backup2.com")
        )

        val streamId = 999
        val format = StreamFormat.M3U8
        val fallbackFormat = StreamFormat.TS

        val allServers = (listOf(account.serverUrl) + account.backupServers).distinct()
        val candidates = mutableListOf<String>()
        for (server in allServers) {
            candidates.add(
                XtreamUrlBuilder.buildStreamUrl(
                    serverUrl = server,
                    username = account.username,
                    password = account.password,
                    streamId = streamId,
                    format = format
                )
            )
            candidates.add(
                XtreamUrlBuilder.buildStreamUrl(
                    serverUrl = server,
                    username = account.username,
                    password = account.password,
                    streamId = streamId,
                    format = fallbackFormat
                )
            )
        }

        // 3 servers * 2 formats = 6 candidates
        assertEquals(6, candidates.size)
        // Primary format first
        assertTrue(candidates[0].startsWith("http://primary.com/live/user/pwd/999.m3u8"))
        // Primary fallback second
        assertTrue(candidates[1].startsWith("http://primary.com/live/user/pwd/999.ts"))
        // Backup 1 format third
        assertTrue(candidates[2].startsWith("http://backup1.com/live/user/pwd/999.m3u8"))
        // Backup 1 fallback fourth
        assertTrue(candidates[3].startsWith("http://backup1.com/live/user/pwd/999.ts"))
        // Backup 2 format fifth
        assertTrue(candidates[4].startsWith("http://backup2.com/live/user/pwd/999.m3u8"))
        // Backup 2 fallback sixth
        assertTrue(candidates[5].startsWith("http://backup2.com/live/user/pwd/999.ts"))
    }
}
