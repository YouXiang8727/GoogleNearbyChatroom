package com.youxiang8727.googlenearbychatroom.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.youxiang8727.googlenearbychatroom.domain.repository.ChatRepository
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class MediaCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chatRepository: ChatRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("MediaCleanupWorker", "Starting media cleanup task...")
        val sevenDaysInMillis = TimeUnit.DAYS.toMillis(7)
        val expiryTime = System.currentTimeMillis() - sevenDaysInMillis

        try {
            val expiredMessages = chatRepository.getExpiredVideos(expiryTime)
            Log.d("MediaCleanupWorker", "Found ${expiredMessages.size} expired videos")

            expiredMessages.forEach { message ->
                message.mediaUri?.let { uriString ->
                    val uri = Uri.parse(uriString)
                    if (uri.scheme == "file") {
                        val file = File(uri.path!!)
                        if (file.exists()) {
                            if (file.delete()) {
                                Log.d("MediaCleanupWorker", "Deleted expired video: ${file.name}")
                            }
                        }
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("MediaCleanupWorker", "Error during media cleanup", e)
            return Result.failure()
        }
    }
}
