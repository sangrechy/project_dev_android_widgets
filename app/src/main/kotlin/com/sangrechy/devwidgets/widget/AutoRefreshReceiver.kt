package com.sangrechy.devwidgets.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sangrechy.devwidgets.data.DefaultDataRepository
import com.sangrechy.devwidgets.data.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = WidgetPreferences(appContext)
                val repo = DefaultDataRepository(appContext)
                
                // Perform sync
                repo.syncData()

                // Update all widgets
                val updatedData = prefs.getDashboardData()
                WidgetManagerHelper.updateAll(appContext, updatedData)

                // Reschedule if interval is under 15 minutes
                val interval = prefs.autoRefreshInterval
                val millis = AutoRefreshScheduler.parseIntervalToMillis(interval)
                if (millis < 15 * 60_000L) {
                    AutoRefreshScheduler.scheduleNextAlarm(appContext, millis)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
