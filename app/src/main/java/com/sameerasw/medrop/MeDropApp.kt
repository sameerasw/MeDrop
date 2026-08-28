package com.sameerasw.medrop

import android.app.Application
import android.content.Context

class MeDropApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}

