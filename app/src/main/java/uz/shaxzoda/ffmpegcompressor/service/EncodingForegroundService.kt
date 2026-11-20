package uz.shaxzoda.ffmpegcompressor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uz.shaxzoda.ffmpegcompressor.R
import uz.shaxzoda.ffmpegcompressor.data.repository.CompressionRepository
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger

@AndroidEntryPoint
class EncodingForegroundService : Service() {
    @Inject lateinit var repository: CompressionRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification(getString(R.string.notification_encoding_idle), 0),
                foregroundServiceType()
            )
        }.onFailure {
            AppLogger.error("startForeground failed", it)
            stopSelf()
        }.isSuccess
        if (!started) return
        AppLogger.info("EncodingForegroundService started with type ${foregroundServiceType()}")
        scope.launch {
            EncodingQueue.tasks.collect { task ->
                AppLogger.info("Service received job=${task.jobId}, file=${task.media.displayName}")
                repository.runJob(task.jobId, task.media, task.request, task.plan).collect { progress ->
                    track(progress.jobId, progress.fileName, progress.progress, progress.message)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            AppLogger.info("Cancel requested")
            repository.cancel()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun track(jobId: Long, fileName: String, progress: Float, message: String) {
        val percent = (progress * 100).toInt().coerceIn(0, 100)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification("$fileName: $message", percent))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_encoding),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun notification(text: String, progress: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(getString(R.string.notification_encoding_title))
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(progress in 1..99)
            .setProgress(100, progress, progress == 0)
            .build()

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

    companion object {
        const val ACTION_CANCEL = "uz.shaxzoda.ffmpegcompressor.CANCEL"
        private const val CHANNEL_ID = "encoding"
        private const val NOTIFICATION_ID = 41
    }
}
