package uz.shaxzoda.ffmpegcompressor.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uz.shaxzoda.ffmpegcompressor.data.db.EncodeJobDao
import uz.shaxzoda.ffmpegcompressor.data.db.EncodeJobEntity
import uz.shaxzoda.ffmpegcompressor.data.ffmpeg.FFmpegCommandBuilder
import uz.shaxzoda.ffmpegcompressor.data.ffmpeg.FFmpegEvent
import uz.shaxzoda.ffmpegcompressor.data.ffmpeg.FFmpegExecutor
import uz.shaxzoda.ffmpegcompressor.domain.BitratePlan
import uz.shaxzoda.ffmpegcompressor.domain.CompressionRequest
import uz.shaxzoda.ffmpegcompressor.domain.EncodingProgress
import uz.shaxzoda.ffmpegcompressor.domain.JobStatus
import uz.shaxzoda.ffmpegcompressor.domain.SelectedMedia
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger

class CompressionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: EncodeJobDao,
    private val builder: FFmpegCommandBuilder,
    private val executor: FFmpegExecutor,
) {
    val history: Flow<List<EncodeJobEntity>> = dao.observeHistory()
    val active: Flow<List<EncodeJobEntity>> = dao.observeActive()

    suspend fun createJob(media: SelectedMedia, request: CompressionRequest, plan: BitratePlan): Long {
        val jobId = dao.insert(
            EncodeJobEntity(
                inputName = media.displayName,
                inputUri = media.uri.toString(),
                outputUri = null,
                sourceBytes = media.sizeBytes,
                resultBytes = null,
                mode = request.mode.name,
                codec = request.manual.videoCodec,
                videoBitrateKbps = plan.videoBitrateKbps,
                audioBitrateKbps = plan.audioBitrateKbps,
                status = JobStatus.QUEUED,
                progress = 0f,
                error = null,
                createdAt = System.currentTimeMillis(),
                finishedAt = null
            )
        )
        val plannedScale = if (plan.suggestedWidth != null && plan.suggestedHeight != null) {
            "${plan.suggestedWidth}x${plan.suggestedHeight}"
        } else {
            "original"
        }
        AppLogger.info(
            "Created job=$jobId, file=${media.displayName}, mode=${request.mode}, " +
                "target=${request.targetSize}${request.sizeUnit}, video=${plan.videoBitrateKbps}k, " +
                "audio=${plan.audioBitrateKbps}k, scale=$plannedScale, keepOriginal=${request.manual.keepOriginalResolution}"
        )
        return jobId
    }

    fun runJob(
        jobId: Long,
        media: SelectedMedia,
        request: CompressionRequest,
        plan: BitratePlan,
    ): Flow<EncodingProgress> = flow {
        val inputFile = File(context.cacheDir, "input-$jobId-${safe(media.displayName)}")
        val extension = request.manual.container.ifBlank { "mp4" }
        val outputFile = File(context.cacheDir, "output-$jobId.$extension")
        val passLog = File(context.cacheDir, "ffmpeg-pass-$jobId")

        try {
            AppLogger.info("Run job=$jobId started")
            dao.updateStatus(jobId, JobStatus.RUNNING.name, 0f, null, null)

            AppLogger.info("Copy input job=$jobId from uri=${media.uri}")
            context.contentResolver.openInputStream(media.uri).use { input ->
                requireNotNull(input) { "Не удалось открыть исходный файл." }
                inputFile.outputStream().use { output -> input.copyTo(output) }
            }
            AppLogger.info("Input copied job=$jobId, bytes=${inputFile.length()}")

            val commands = builder.build(inputFile, outputFile, request, plan, passLog)
            executor.execute(commands, media.durationMs).collect { event ->
                when (event) {
                    is FFmpegEvent.Progress -> {
                        dao.updateStatus(jobId, JobStatus.RUNNING.name, event.progress, null, null)
                        emit(
                            EncodingProgress(
                                jobId = jobId,
                                fileName = media.displayName,
                                progress = event.progress,
                                status = JobStatus.RUNNING,
                                message = "FFmpeg: ${(event.progress * 100).toInt()}%"
                            )
                        )
                    }

                    is FFmpegEvent.Log -> Unit

                    is FFmpegEvent.Finished -> {
                        if (event.exitCode != 0) {
                            val message = if (event.exitCode == 127) {
                                "FFmpeg не найден внутри APK."
                            } else {
                                "FFmpeg завершился с кодом ${event.exitCode}."
                            }
                            AppLogger.error("Job=$jobId failed: $message")
                            dao.updateStatus(jobId, JobStatus.FAILED.name, 0f, message, System.currentTimeMillis())
                            emit(EncodingProgress(jobId, media.displayName, 0f, JobStatus.FAILED, message))
                        } else {
                            AppLogger.info("Write result job=$jobId, tempBytes=${outputFile.length()}")
                            val outputUri = writeResult(request.outputTree, outputFile, media.displayName, extension)
                            dao.get(jobId)?.let { entity ->
                                dao.update(
                                    entity.copy(
                                        outputUri = outputUri.toString(),
                                        resultBytes = outputFile.length(),
                                        status = JobStatus.SUCCEEDED,
                                        progress = 1f,
                                        finishedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                            AppLogger.info("Job=$jobId succeeded, outputUri=$outputUri")
                            emit(EncodingProgress(jobId, media.displayName, 1f, JobStatus.SUCCEEDED, "Готово"))
                        }
                    }
                }
            }
        } catch (throwable: Throwable) {
            val message = throwable.message ?: "Ошибка обработки"
            AppLogger.error("Job=$jobId crashed inside runJob: $message", throwable)
            dao.updateStatus(jobId, JobStatus.FAILED.name, 0f, message, System.currentTimeMillis())
            emit(EncodingProgress(jobId, media.displayName, 0f, JobStatus.FAILED, message))
        } finally {
            inputFile.delete()
            outputFile.delete()
            passLog.delete()
            File("${passLog.absolutePath}-0.log").delete()
        }
    }.flowOn(Dispatchers.IO)

    fun cancel() = executor.cancel()

    private fun writeResult(treeUri: Uri?, file: File, inputName: String, extension: String): Uri {
        val resolver = context.contentResolver
        val targetUri = if (treeUri != null) {
            val tree = DocumentFile.fromTreeUri(context, treeUri)
            val base = inputName.substringBeforeLast('.').ifBlank { "result" }
            tree?.createFile(mime(extension), "$base-compressed.$extension")?.uri
        } else {
            null
        }
        val uri = requireNotNull(targetUri) { "Выберите папку сохранения результата." }
        resolver.openOutputStream(uri, "w").use { out ->
            requireNotNull(out) { "Не удалось открыть файл результата." }
            file.inputStream().use { it.copyTo(out) }
        }
        return uri
    }

    private fun safe(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun mime(extension: String): String = when (extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        else -> "video/mp4"
    }
}
