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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.zerotwodev.recall.domain.AppTheme
import com.zerotwodev.recall.domain.MAX_PHOTOS_AT_ONCE
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

    private val photoPicker: AndroidPhotoPicker
        get() = (application as RecallApp).container.photoPicker as AndroidPhotoPicker

    private val photoPrompt = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS_AT_ONCE),
    ) { uris -> photoPicker.onResult(uris) }

    private val notificationPermission: AndroidNotificationPermission
        get() = (application as RecallApp).container.notificationPermission as AndroidNotificationPermission

    private val notificationPrompt = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermission.onResult(granted)
    }

    private val appLock: AndroidAppLock
        get() = (application as RecallApp).container.appLock as AndroidAppLock

    override fun onDestroy() {
        appLock.attach(null)
        micPermission.launcher = null
        notificationPermission.launcher = null
        photoPicker.launcher = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as RecallApp).container
        appLock.attach(this)
        micPermission.launcher = { micPrompt.launch(Manifest.permission.RECORD_AUDIO) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launcher = { notificationPrompt.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        photoPicker.launcher = {
            photoPrompt.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
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
