package com.blatnox.tvplayer.data.local.entity

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
        Index(value = ["accountId"]),
        Index(value = ["accountId", "isHidden"])
    ]
)
data class CategoryEntity(
    val accountId: Long,
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0,
    val isHidden: Boolean = false
)
