package com.thc.safewords

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.glance.appwidget.updateAll
import com.thc.safewords.widget.SafewordsWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SafewordsApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT ||
                intent.action == Intent.ACTION_SCREEN_ON
            ) {
                scope.launch { SafewordsWidget().updateAll(context.applicationContext) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    companion object {
        lateinit var instance: SafewordsApp
            private set
    }
}
