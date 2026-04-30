package com.thc.safewords.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Three update paths feed the widget:
 *   1. AppWidgetManager periodic poke (every 30 min via safewords_widget_info.xml)
 *   2. WorkManager periodic worker (30 min, redundant fallback for OEMs that throttle path 1)
 *   3. Screen-on / user-present broadcasts — refresh whenever the user actually looks
 *      at the phone. Closest we can get to "realtime" without a foreground service;
 *      Android caps periodic widget updates at 30 min by design.
 */
class SafewordsWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SafewordsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_USER_PRESENT ||
            intent.action == Intent.ACTION_SCREEN_ON ||
            intent.action == Intent.ACTION_TIME_TICK
        ) {
            runBlocking { SafewordsWidget().updateAll(context) }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_WORK_NAME)
    }

    companion object {
        private const val WIDGET_WORK_NAME = "safewords_widget_update"

        fun scheduleWidgetUpdates(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

/**
 * Forces a widget redraw on the WorkManager schedule. Without explicit updateAll,
 * the periodic worker is decorative — Glance doesn't redraw on its own.
 */
class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            SafewordsWidget().updateAll(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
