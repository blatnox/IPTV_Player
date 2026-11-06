package com.antigravity.tvplayer.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Account(
    val id: Long = 0,
    val serverUrl: String,
    val username: String,
    val password: String = "",
    val serverName: String = "",
    val status: String = "Active",
    val expDate: String? = null,
    val maxConnections: String? = null,
    val activeConnections: String? = null,
    val isActive: Boolean = false,
    val lastLoginAt: Long = 0L,
    val backupServers: List<String> = emptyList()
)

@Immutable
data class Category(
    val id: String,
    val name: String,
    val parentId: Int = 0,
    val isFavoritesCategory: Boolean = false,
    val isRecentCategory: Boolean = false,
    val isHidden: Boolean = false
) {
    companion object {
        const val FAVORITES_ID = "FAVORITES_SPECIAL_CATEGORY_ID"
        const val RECENT_ID = "RECENT_SPECIAL_CATEGORY_ID"

        fun favorites() = Category(
            id = FAVORITES_ID,
            name = "★ Favorites",
            isFavoritesCategory = true
        )

        fun recent() = Category(
            id = RECENT_ID,
            name = "⟳ Recent",
            isRecentCategory = true
        )
    }
}

@Immutable
data class Channel(
    val streamId: Int,
    val num: Int,
    val name: String,
    val streamIcon: String?,
    val epgChannelId: String?,
    val categoryId: String,
    val isFavorite: Boolean = false,
    val displayName: String = name,
    val qualityBadge: String? = null,
    val isSeparator: Boolean = false
)
