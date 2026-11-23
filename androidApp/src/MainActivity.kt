package com.zerotwodev.recall

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.zerotwodev.recall.ui.App
import com.zerotwodev.recall.ui.Navigator
import com.zerotwodev.recall.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as RecallApp).container
        setContent {
            val navigator = remember { Navigator() }
            // System back walks the app's own back stack before leaving the app.
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }

            // Status/navigation bar icons follow the app's Light/Dark choice, not just the phone's.
            val settings by container.settingsRepository.settings.collectAsState()
            val dark = settings.themeMode.isDark()
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            App(container, navigator)
        }
    }
}
