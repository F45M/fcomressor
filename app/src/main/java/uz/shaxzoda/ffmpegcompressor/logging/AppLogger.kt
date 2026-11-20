package uz.shaxzoda.ffmpegcompressor.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "FComressor"
    private const val MAX_BYTES = 512_000L
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        info("Logger initialized")
    }

    fun info(message: String) {
        Log.i(TAG, message)
        write("I", message, null)
    }

    fun error(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        write("E", message, throwable)
    }

    fun read(): String {
        val file = logFileOrNull() ?: return "Лог пока недоступен."
        return if (file.exists()) file.readText() else "Лог пока пуст."
    }

    fun clear() {
        logFileOrNull()?.writeText("")
        info("Log cleared")
    }

    private fun write(level: String, message: String, throwable: Throwable?) {
        val file = logFileOrNull() ?: return
        runCatching {
            file.parentFile?.mkdirs()
            if (file.exists() && file.length() > MAX_BYTES) {
                file.writeText("")
            }
            val line = buildString {
                append(timeFormat.format(Date()))
                append(' ')
                append(level)
                append(' ')
                append(message)
                append('\n')
                throwable?.let {
                    append(Log.getStackTraceString(it))
                    append('\n')
                }
            }
            file.appendText(line)
        }
    }

    private fun logFileOrNull(): File? =
        if (::appContext.isInitialized) File(appContext.filesDir, "logs/fcompressor.log") else null
}
