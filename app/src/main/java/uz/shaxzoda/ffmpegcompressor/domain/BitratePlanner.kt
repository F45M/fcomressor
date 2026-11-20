package uz.shaxzoda.ffmpegcompressor.domain

import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BitratePlanner @Inject constructor() {
    fun plan(media: SelectedMedia, request: CompressionRequest): BitratePlan {
        if (request.mode == WorkMode.MANUAL) {
            val audio = if (request.manual.removeAudio) 0 else request.manual.audioBitrateKbps
            val predicted = predictBytes(media.durationMs, request.manual.videoBitrateKbps, audio)
            return BitratePlan(
                videoBitrateKbps = request.manual.videoBitrateKbps,
                audioBitrateKbps = audio,
                containerOverheadBytes = overhead(media.durationMs),
                predictedBytes = predicted,
                qualityWarning = warningFor(
                    width = media.width,
                    height = media.height,
                    videoKbps = request.manual.videoBitrateKbps,
                    keepOriginalResolution = request.manual.keepOriginalResolution
                ),
                suggestedWidth = if (request.manual.keepOriginalResolution) null else request.manual.width,
                suggestedHeight = if (request.manual.keepOriginalResolution) null else request.manual.height,
                suggestedFps = request.manual.fps,
                useTwoPass = request.manual.passes > 1
            )
        }

        val durationSeconds = max(1.0, media.durationMs / 1000.0)
        val targetBytes = when (request.sizeUnit) {
            SizeUnit.MB -> request.targetSize * 1024.0 * 1024.0
            SizeUnit.GB -> request.targetSize * 1024.0 * 1024.0 * 1024.0
        }.toLong()
        val audioKbps = chooseAudioBitrate(request.priority, media.audioTracks, request.mode)
        val overheadBytes = overhead(media.durationMs)
        val reservedAudioBits = audioKbps * 1000.0 * durationSeconds
        val availableVideoBits = max(
            96_000.0 * durationSeconds,
            targetBytes * 8.0 - reservedAudioBits - overheadBytes * 8.0
        )
        val videoKbps = max(96, (availableVideoBits / durationSeconds / 1000.0).roundToInt())
        val allowDownscale = !request.manual.keepOriginalResolution
        val suggestions = if (allowDownscale) scaleSuggestion(media, videoKbps) else Triple(null, null, null)
        val predicted = predictBytes(media.durationMs, videoKbps, audioKbps)

        return BitratePlan(
            videoBitrateKbps = videoKbps,
            audioBitrateKbps = audioKbps,
            containerOverheadBytes = overheadBytes,
            predictedBytes = predicted,
            qualityWarning = warningFor(
                width = media.width,
                height = media.height,
                videoKbps = videoKbps,
                keepOriginalResolution = request.manual.keepOriginalResolution
            ),
            suggestedWidth = suggestions.first,
            suggestedHeight = suggestions.second,
            suggestedFps = suggestions.third,
            useTwoPass = request.mode == WorkMode.TARGET_SIZE
        )
    }

    fun autoTarget(media: SelectedMedia): Double {
        val current = media.sizeBytes.takeIf { it > 0 } ?: return 64.0
        val factor = when {
            media.width >= 3840 -> 0.32
            media.width >= 1920 -> 0.22
            else -> 0.35
        }
        return max(12.0, current * factor / 1024.0 / 1024.0)
    }

    private fun chooseAudioBitrate(priority: QualityPriority, tracks: Int, mode: WorkMode): Int {
        if (mode == WorkMode.AUDIO) return if (priority == QualityPriority.MAX_COMPRESSION) 96 else 160
        val base = when (priority) {
            QualityPriority.MAX_QUALITY -> 160
            QualityPriority.BALANCED -> 128
            QualityPriority.MAX_COMPRESSION -> 96
        }
        return if (tracks > 1) min(256, base * tracks) else base
    }

    private fun overhead(durationMs: Long): Long {
        val seconds = max(1L, durationMs / 1000L)
        return 384_000L + seconds * 320L
    }

    private fun predictBytes(durationMs: Long, videoKbps: Int, audioKbps: Int): Long {
        val seconds = max(1.0, durationMs / 1000.0)
        return (((videoKbps + audioKbps) * 1000.0 * seconds) / 8.0).toLong() + overhead(durationMs)
    }

    private fun warningFor(width: Int, height: Int, videoKbps: Int, keepOriginalResolution: Boolean): String? {
        val pixels = max(1, width * height)
        val bitsPerPixelSecond = videoKbps * 1000.0 / pixels / 30.0
        return when {
            bitsPerPixelSecond < 0.025 && keepOriginalResolution ->
                "Размер очень мал для исходного разрешения. Разрешение сохранится, но качество может сильно просесть."
            bitsPerPixelSecond < 0.025 ->
                "Размер очень мал: приложение уменьшит картинку, чтобы сохранить видимое качество."
            bitsPerPixelSecond < 0.045 && keepOriginalResolution ->
                "Сжатие агрессивное: разрешение сохранится, но для лучшего качества стоит увеличить целевой размер."
            bitsPerPixelSecond < 0.045 ->
                "Сжатие агрессивное: лучше увеличить целевой размер, если картинка станет мыльной."
            else -> null
        }
    }

    private fun scaleSuggestion(media: SelectedMedia, videoKbps: Int): Triple<Int?, Int?, Int?> {
        val currentPixels = media.width * media.height
        if (currentPixels <= 0) return Triple(null, null, null)
        val targetPixels = when {
            videoKbps < 260 -> 640 * 360
            videoKbps < 900 -> 854 * 480
            videoKbps < 1800 -> 1280 * 720
            videoKbps < 4200 -> 1920 * 1080
            else -> currentPixels
        }
        if (targetPixels >= currentPixels) return Triple(null, null, null)
        val ratio = media.width.toDouble() / media.height.toDouble()
        val height = max(144, (sqrt(targetPixels / ratio).roundToInt() / 2) * 2)
        val width = max(256, ((height * ratio).roundToInt() / 2) * 2)
        val fps = media.frameRate?.takeIf { it > 30f }?.let { 30 }
        return Triple(width, height, fps)
    }
}
