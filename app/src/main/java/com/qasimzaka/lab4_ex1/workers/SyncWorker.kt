package com.qasimzaka.lab4_ex1.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import android.util.Log

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // Simulate data synchronization
            Log.d("SyncWorker", "Starting data sync...")
            delay(3000) // Simulate work with a delay
            Log.d("SyncWorker", "Data sync completed successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error during data sync", e)
            return Result.retry()
        }
    }
}