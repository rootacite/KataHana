package com.acite.katahana

import android.app.Application
import com.acite.katahana.settings.installAppContext
import dev.zacsweers.metro.createGraph

class KataHanaApp : Application() {
    val graph: AppGraph by lazy { createGraph<AppGraph>() }

    override fun onCreate() {
        super.onCreate()
        installAppContext(this)
    }
}
