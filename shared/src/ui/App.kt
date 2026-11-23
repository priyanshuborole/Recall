package com.zerotwodev.recall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwodev.recall.AppContainer
import com.zerotwodev.recall.presentation.HomeViewModel
import com.zerotwodev.recall.presentation.MemoryDetailViewModel
import com.zerotwodev.recall.presentation.SettingsViewModel
import com.zerotwodev.recall.presentation.TimelineViewModel
import com.zerotwodev.recall.ui.detail.MemoryDetailScreen
import com.zerotwodev.recall.ui.home.HomeScreen
import com.zerotwodev.recall.ui.settings.AppearanceScreen
import com.zerotwodev.recall.ui.settings.SettingsScreen
import com.zerotwodev.recall.ui.theme.RecallTheme
import com.zerotwodev.recall.ui.theme.isDark
import com.zerotwodev.recall.ui.timeline.TimelineScreen
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object Timeline : Screen
    data object Settings : Screen
    data object Appearance : Screen
    data class Detail(val memoryId: String) : Screen
}

/**
 * A small back stack that works the same on every platform.
 * Home is always the root; tabs sit on top of it so Back returns to Home.
 */
class Navigator {
    var stack: List<Screen> by mutableStateOf(listOf(Screen.Home))
        private set

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun open(screen: Screen) {
        stack = stack + screen
    }

    fun switchTab(tab: Screen) {
        stack = if (tab == Screen.Home) listOf(Screen.Home) else listOf(Screen.Home, tab)
    }

    /** Returns false when already at Home, so the platform can close the app. */
    fun back(): Boolean {
        if (!canGoBack) return false
        stack = stack.dropLast(1)
        return true
    }
}

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, "Home", RecallIcons.Home),
    Tab(Screen.Timeline, "Timeline", RecallIcons.Timeline),
    Tab(Screen.Settings, "Settings", RecallIcons.Settings),
)

/** The whole app. Each platform calls this with its own [AppContainer]. */
@Composable
fun App(
    container: AppContainer,
    navigator: Navigator = remember { Navigator() },
) {
    // The saved theme and mode drive the whole app; changing them on Appearance recolours it instantly.
    val settings by container.settingsRepository.settings.collectAsState()

    RecallTheme(theme = settings.theme, darkTheme = settings.themeMode.isDark()) {
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val current = navigator.current
        val isTab = current == Screen.Home || current == Screen.Timeline || current == Screen.Settings

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (isTab) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = current == tab.screen,
                                onClick = { navigator.switchTab(tab.screen) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (current) {
                    Screen.Home -> HomeScreen(
                        viewModel = viewModel { HomeViewModel(container.memoryRepository, container.clock) },
                        onOpenMemory = { navigator.open(Screen.Detail(it)) },
                        onSeeAll = { navigator.switchTab(Screen.Timeline) },
                        onComingSoon = { message -> scope.launch { snackbar.showSnackbar(message) } },
                    )
                    Screen.Timeline -> TimelineScreen(
                        viewModel = viewModel { TimelineViewModel(container.memoryRepository) },
                        onOpenMemory = { navigator.open(Screen.Detail(it)) },
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel { SettingsViewModel(container.settingsRepository) },
                        onOpenAppearance = { navigator.open(Screen.Appearance) },
                    )
                    Screen.Appearance -> AppearanceScreen(
                        // Same ViewModel instance as Settings (same type, no key).
                        viewModel = viewModel { SettingsViewModel(container.settingsRepository) },
                        onBack = { navigator.back() },
                    )
                    is Screen.Detail -> MemoryDetailScreen(
                        viewModel = viewModel(key = "detail-${current.memoryId}") {
                            MemoryDetailViewModel(current.memoryId, container.memoryRepository)
                        },
                        onBack = { navigator.back() },
                    )
                }
            }
        }
    }
}
