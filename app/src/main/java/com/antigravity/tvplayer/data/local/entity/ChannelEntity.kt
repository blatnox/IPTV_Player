package com.antigravity.tvplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "channels",
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
        Index(value = ["accountId", "categoryId"]),
        Index(value = ["accountId", "name"])
    ]
)
data class ChannelEntity(
    val accountId: Long,
    val streamId: Int,
    val num: Int = 0,
    val name: String,
    val streamIcon: String? = null,
    val epgChannelId: String? = null,
    val categoryId: String = ""
)
