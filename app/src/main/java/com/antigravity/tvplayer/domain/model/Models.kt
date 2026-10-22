package com.antigravity.tvplayer.domain.model

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
    val lastLoginAt: Long = 0L
)

data class Category(
    val id: String,
    val name: String,
    val parentId: Int = 0,
    val isFavoritesCategory: Boolean = false
) {
    companion object {
        const val FAVORITES_ID = "FAVORITES_SPECIAL_CATEGORY_ID"
        fun favorites() = Category(
            id = FAVORITES_ID,
            name = "★ Favorites",
            isFavoritesCategory = true
        )
    }
}

data class Channel(
    val streamId: Int,
    val num: Int,
    val name: String,
    val streamIcon: String?,
    val epgChannelId: String?,
    val categoryId: String,
    val isFavorite: Boolean = false
)
