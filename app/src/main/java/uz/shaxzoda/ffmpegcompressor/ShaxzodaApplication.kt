package uz.shaxzoda.ffmpegcompressor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger

@HiltAndroidApp
class ShaxzodaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.error("Uncaught crash on thread ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable) ?: exitProcess(2)
        }
    }
}
