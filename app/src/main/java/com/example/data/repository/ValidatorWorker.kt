package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.di.ServiceLocator

class ValidatorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ValidatorWorker", "Starting channel validation background task...")
        try {
            val repository = ServiceLocator.getIptvRepository(applicationContext)
            val profiles = repository.getProfiles()
            for (profile in profiles) {
                val channels = repository.getChannels(profile.id)
                Log.d("ValidatorWorker", "Validating ${channels.size} channels for profile: ${profile.name}")
                for (channel in channels) {
                    if (isStopped) {
                        Log.d("ValidatorWorker", "Worker is stopped, cancelling remaining validation.")
                        return Result.retry()
                    }
                    repository.validateChannelUrl(channel)
                }
            }
            Log.d("ValidatorWorker", "Channel validation completed successfully!")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ValidatorWorker", "Error executing channel validation worker", e)
            return Result.failure()
        }
    }
}
