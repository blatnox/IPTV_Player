package com.blatnox.tvplayer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.blatnox.tvplayer.data.settings.SettingsDataStore
import com.blatnox.tvplayer.domain.repository.AccountRepository
import com.blatnox.tvplayer.ui.navigation.NavRoute
import com.blatnox.tvplayer.ui.navigation.TvNavHost
import com.blatnox.tvplayer.ui.theme.DarkBackground
import com.blatnox.tvplayer.ui.theme.FocusCyan
import com.blatnox.tvplayer.ui.theme.TvPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            TvPlayerTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val activeAccount = accountRepository.getActiveAccountSync()
                    if (activeAccount != null) {
                        val shouldResume = settingsDataStore.resumeLastChannel.first()
                        val lastWatched = settingsDataStore.lastWatchedStreamId.first()
                        if (shouldResume && lastWatched != null && lastWatched > 0) {
                            startDestination = NavRoute.Player.createRoute(lastWatched)
                        } else {
                            startDestination = NavRoute.LiveTv.route
                        }
                    } else {
                        startDestination = NavRoute.Login.route
                    }
                }

                val currentDestination = startDestination
                if (currentDestination != null) {
                    val navController = rememberNavController()
                    TvNavHost(
                        navController = navController,
                        startDestination = currentDestination,
                        onExitApp = { finish() }
                    )
                } else {
                    LoadingScreen()
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = FocusCyan
        )
    }
}
