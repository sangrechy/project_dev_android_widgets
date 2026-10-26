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

        WidgetManagerHelper.updateAll(applicationContext, data)

        return if (result.isSuccess) {
            Result.success()
        } else {
            // Even if network fails, widget was refreshed with latest cache or fallback
            if (runAttemptCount > 3) Result.failure() else Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            AutoRefreshScheduler.schedule(context)
        }
    }
}
