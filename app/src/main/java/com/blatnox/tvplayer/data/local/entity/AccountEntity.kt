package com.blatnox.tvplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverUrl: String,
    val username: String,
    val serverName: String = "",
    val status: String = "Unknown",
    val expDate: String? = null,
    val maxConnections: String? = null,
    val activeConnections: String? = null,
    val isActive: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "backup_servers", defaultValue = "")
    val backupServers: String = ""
)
