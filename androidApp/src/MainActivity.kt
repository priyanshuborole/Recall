package com.zerotwodev.recall

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.zerotwodev.recall.domain.AppTheme
import com.zerotwodev.recall.domain.ThemeMode
import com.zerotwodev.recall.ui.App
import com.zerotwodev.recall.ui.Navigator
import com.zerotwodev.recall.ui.theme.isDark
import com.zerotwodev.recall.ui.theme.windowBackgroundColor

class MainActivity : ComponentActivity() {
    private val micPermission: AndroidMicPermission
        get() = (application as RecallApp).container.micPermission as AndroidMicPermission

    private val micPrompt = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micPermission.onResult(granted)
    }

    override fun onDestroy() {
        micPermission.launcher = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as RecallApp).container
        micPermission.launcher = { micPrompt.launch(Manifest.permission.RECORD_AUDIO) }
        setContent {
            val navigator = remember { Navigator() }
            // System back walks the app's own back stack before leaving the app.
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }

            // Status/navigation bar icons and the window follow the app's choice, not just the phone's.
            val settings by container.settingsRepository.settings.collectAsState()
            val dark = settings.themeMode.isDark()
            val windowColor = windowBackgroundColor(settings.theme, dark).toArgb()
            DisposableEffect(dark, windowColor) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                window.setBackgroundDrawable(ColorDrawable(windowColor))
                onDispose {}
            }

            // Android 12+ shows this on the next launch, before any app code runs.
            LaunchedEffect(settings.theme, settings.themeMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    splashScreen.setSplashScreenTheme(splashStyle(settings.theme, settings.themeMode))
                }
            }

            App(container, navigator)
        }
    }
}

private fun splashStyle(theme: AppTheme, mode: ThemeMode): Int = when (theme) {
    AppTheme.WARM -> when (mode) {
        ThemeMode.SYSTEM -> R.style.Theme_Recall_Splash_WarmSystem
        ThemeMode.LIGHT -> R.style.Theme_Recall_Splash_WarmLight
        ThemeMode.DARK -> R.style.Theme_Recall_Splash_WarmDark
    }
    AppTheme.FRESH -> when (mode) {
        ThemeMode.SYSTEM -> R.style.Theme_Recall_Splash_FreshSystem
        ThemeMode.LIGHT -> R.style.Theme_Recall_Splash_FreshLight
        ThemeMode.DARK -> R.style.Theme_Recall_Splash_FreshDark
    }
}
