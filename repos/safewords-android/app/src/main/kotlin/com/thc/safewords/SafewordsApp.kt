package com.thc.safewords

import android.app.Application

class SafewordsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SafewordsApp
            private set
    }
}
