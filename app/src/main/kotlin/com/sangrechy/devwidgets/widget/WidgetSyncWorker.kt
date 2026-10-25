package com.sangrechy.devwidgets.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sangrechy.devwidgets.data.DefaultDataRepository
import com.sangrechy.devwidgets.data.WidgetPreferences
import java.util.concurrent.TimeUnit

class WidgetSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = DefaultDataRepository(applicationContext)
        val result = repo.syncData()

        val prefs = WidgetPreferences(applicationContext)
        val data = prefs.getDashboardData()

        DevActivityWidgetProvider.updateAllWidgets(applicationContext, data)

        return if (result.isSuccess) {
            Result.success()
        } else {
            // Even if network fails, widget was refreshed with latest cache or fallback
            if (runAttemptCount > 3) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "dev_widget_periodic_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WidgetSyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
