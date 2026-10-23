package com.antigravity.tvplayer.ui.navigation

sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login")
    data object LiveTv : NavRoute("live_tv")
    data object Player : NavRoute("player/{streamId}") {
        fun createRoute(streamId: Int): String = "player/$streamId"
    }
    data object Settings : NavRoute("settings")
    data object ManageCategories : NavRoute("manage_categories")
}
