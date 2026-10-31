package com.antigravity.tvplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "recent_channels",
    primaryKeys = ["accountId", "streamId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "watchedAt"])
    ]
)
data class RecentChannelEntity(
    val accountId: Long,
    val streamId: Int,
    val watchedAt: Long = System.currentTimeMillis()
)
