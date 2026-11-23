package com.zerotwodev.recall

import androidx.compose.ui.window.ComposeUIViewController
import com.zerotwodev.recall.ui.App

/** Created once for the app's lifetime. */
private val container: AppContainer by lazy { createAppContainer() }

// Called from iosApp.swift as ViewControllerKt.ViewController().
fun ViewController() = ComposeUIViewController { App(container) }
