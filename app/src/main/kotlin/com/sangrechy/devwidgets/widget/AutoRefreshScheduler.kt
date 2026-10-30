package com.sangrechy.devwidgets.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sangrechy.devwidgets.data.WidgetPreferences
import java.util.concurrent.TimeUnit

object AutoRefreshScheduler {

    const val ACTION_AUTO_REFRESH = "com.sangrechy.devwidgets.ACTION_AUTO_REFRESH_TICK"
    private const val PERIODIC_WORK_TAG = "dev_widget_periodic_sync"
    private const val ALARM_REQUEST_CODE = 9021

    fun parseIntervalToMillis(interval: String): Long {
        return when (interval.trim().lowercase()) {
            "1s" -> 1_000L
            "5s" -> 5_000L
            "15s" -> 15_000L
            "30s" -> 30_000L
            "1m" -> 60_000L
            "2m" -> 120_000L
            "5m" -> 300_000L
            "10m" -> 600_000L
            "15m" -> 15 * 60_000L
            "30m" -> 30 * 60_000L
            "1h" -> 60 * 60_000L
            "2h" -> 2 * 60 * 60_000L
            "4h" -> 4 * 60 * 60_000L
            "6h" -> 6 * 60 * 60_000L
            "12h" -> 12 * 60 * 60_000L
            "1d" -> 24 * 60 * 60_000L
            else -> 60 * 60_000L
        }
    }

    fun schedule(context: Context, interval: String? = null) {
        val appContext = context.applicationContext
        val prefs = WidgetPreferences(appContext)
        val selected = interval ?: prefs.autoRefreshInterval
        val millis = parseIntervalToMillis(selected)

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(appContext, AutoRefreshReceiver::class.java).apply {
            action = ACTION_AUTO_REFRESH
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(appContext, ALARM_REQUEST_CODE, intent, flags)

        val workManager = WorkManager.getInstance(appContext)

        if (millis >= 15 * 60_000L) {
            // Cancel sub-15m alarm
            alarmManager?.cancel(pendingIntent)

            // WorkManager Periodic Work (minimum supported interval is 15 minutes)
            val minutes = millis / 60_000L
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WidgetSyncWorker>(
                repeatInterval = minutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        } else {
            // Cancel WorkManager periodic work
            workManager.cancelUniqueWork(PERIODIC_WORK_TAG)

            // Schedule via AlarmManager
            scheduleNextAlarm(appContext, millis, alarmManager, pendingIntent)
        }
    }

    fun scheduleNextAlarm(
        context: Context,
        delayMillis: Long,
        alarmMgr: AlarmManager? = null,
        pIntent: PendingIntent? = null
    ) {
        val appContext = context.applicationContext
        val alarmManager = alarmMgr ?: (appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
            ?: return
        val pendingIntent = pIntent ?: run {
            val intent = Intent(appContext, AutoRefreshReceiver::class.java).apply {
                action = ACTION_AUTO_REFRESH
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getBroadcast(appContext, ALARM_REQUEST_CODE, intent, flags)
        }

        val triggerAt = System.currentTimeMillis() + delayMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: Exception) {
            // Fallback for strict exact alarm policies
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (_: Exception) {}
        }
    }
}
