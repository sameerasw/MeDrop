package com.sameerasw.medrop

import android.app.Application
import android.content.Context
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MeDropApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        try {
            HiddenApiBypass.setHiddenApiExemptions("")
        } catch (_: Throwable) {
        }
    }
}
