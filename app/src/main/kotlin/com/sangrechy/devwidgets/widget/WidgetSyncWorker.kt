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

        return if (result.isSuccess) {
            Result.success()
        } else {
            // Even if network fails, refresh with latest fallback cache
            val prefs = WidgetPreferences(applicationContext)
            WidgetManagerHelper.updateAll(applicationContext, prefs.getDashboardData())
            if (runAttemptCount > 3) Result.failure() else Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            AutoRefreshScheduler.schedule(context)
        }
    }
}
