package com.example.fittrack

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class DailyRefreshWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d("DailyRefreshWorker", "Running daily refresh work")
        // Send broadcast
        val intent = android.content.Intent("com.example.fittrack.REFRESH_SUMMARY")
        applicationContext.sendBroadcast(intent)
        return Result.success()
    }
    }
