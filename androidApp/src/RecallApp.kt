package com.zerotwodev.recall

import android.app.Application

class RecallApp : Application() {
    /** Created once per process; the database and settings live as long as the app. */
    val container: AppContainer by lazy { createAppContainer(this) }
}
