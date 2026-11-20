package uz.shaxzoda.ffmpegcompressor.data.ffmpeg

import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger

sealed interface FFmpegEvent {
    data class Progress(val progress: Float, val line: String) : FFmpegEvent
    data class Log(val line: String) : FFmpegEvent
    data class Finished(val exitCode: Int) : FFmpegEvent
}

class FFmpegExecutor @Inject constructor(
    private val binaryProvider: FFmpegBinaryProvider,
) {
    private val canceled = AtomicBoolean(false)
    private val timePattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.?\\d*)")

    fun cancel() {
        canceled.set(true)
    }

    fun execute(commands: List<List<String>>, durationMs: Long): Flow<FFmpegEvent> = channelFlow {
        canceled.set(false)
        if (!binaryProvider.hasFFmpeg()) {
            AppLogger.error("FFmpeg binary not found at ${binaryProvider.ffmpeg().absolutePath}")
            send(FFmpegEvent.Finished(127))
            close()
            return@channelFlow
        }
        val runner = launch(Dispatchers.IO) {
            commands.forEachIndexed { index, args ->
                val command = listOf(binaryProvider.ffmpeg().absolutePath) + args
                AppLogger.info("FFmpeg pass ${index + 1}/${commands.size}: ${command.joinToString(" ")}")
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                process.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (canceled.get()) {
                            process.destroy()
                            break
                        }
                        val progress = parseProgress(line, durationMs)
                        val event = if (progress != null) {
                            val normalized = (index + progress) / commands.size
                            FFmpegEvent.Progress(normalized.coerceIn(0f, 1f), line)
                        } else {
                            FFmpegEvent.Log(line)
                        }
                        send(event)
                    }
                }
                val code = process.waitFor()
                AppLogger.info("FFmpeg pass ${index + 1} finished with code $code")
                if (code != 0) {
                    send(FFmpegEvent.Finished(code))
                    close()
                    return@launch
                }
            }
            send(FFmpegEvent.Finished(0))
            close()
        }
        awaitClose { runner.cancel() }
    }

    private fun parseProgress(line: String, durationMs: Long): Float? {
        val matcher = timePattern.matcher(line)
        if (!matcher.find() || durationMs <= 0) return null
        val hours = matcher.group(1)?.toLongOrNull() ?: 0L
        val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
        val seconds = matcher.group(3)?.toDoubleOrNull() ?: 0.0
        val currentMs = ((hours * 3600 + minutes * 60) * 1000 + seconds * 1000).toLong()
        return (currentMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }
}
