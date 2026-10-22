package com.antigravity.tvplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "categories",
    primaryKeys = ["accountId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"])
    ]
)
data class CategoryEntity(
    val accountId: Long,
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0
)
