package com.antigravity.tvplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.antigravity.tvplayer.ui.screens.live.LiveTvScreen
import com.antigravity.tvplayer.ui.screens.live.LiveTvViewModel
import com.antigravity.tvplayer.ui.screens.login.LoginScreen
import com.antigravity.tvplayer.ui.screens.login.LoginViewModel
import com.antigravity.tvplayer.ui.screens.player.PlayerScreen
import com.antigravity.tvplayer.ui.screens.player.PlayerViewModel
import com.antigravity.tvplayer.ui.screens.settings.SettingsScreen
import com.antigravity.tvplayer.ui.screens.settings.SettingsViewModel

@Composable
fun TvNavHost(
    navController: NavHostController,
    startDestination: String,
    onExitApp: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoute.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoute.LiveTv.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.LiveTv.route) {
            val viewModel: LiveTvViewModel = hiltViewModel()
            LiveTvScreen(
                viewModel = viewModel,
                onChannelSelected = { channel ->
                    navController.navigate(NavRoute.Player.createRoute(channel.streamId))
                },
                onOpenSettings = {
                    navController.navigate(NavRoute.Settings.route)
                },
                onExitApp = onExitApp
            )
        }

        composable(
            route = NavRoute.Player.route,
            arguments = listOf(
                navArgument("streamId") { type = NavType.StringType }
            )
        ) {
            val viewModel: PlayerViewModel = hiltViewModel()
            PlayerScreen(
                viewModel = viewModel,
                onBackToLiveTv = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoute.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onAddNewAccount = {
                    navController.navigate(NavRoute.Login.route)
                }
            )
        }
    }
}
