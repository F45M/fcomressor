package uz.shaxzoda.ffmpegcompressor.domain

import android.net.Uri

enum class WorkMode { TARGET_SIZE, AUTO, MANUAL, AUDIO }
enum class QualityPriority { MAX_QUALITY, BALANCED, MAX_COMPRESSION }
enum class SizeUnit { MB, GB }
enum class EncoderPreference { AUTO, HARDWARE, SOFTWARE }
enum class JobStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELED }

data class SelectedMedia(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float?,
    val bitrate: Long?,
    val videoCodec: String?,
    val audioCodec: String?,
    val audioBitrate: Long?,
    val audioTracks: Int,
)

data class ManualSettings(
    val container: String = "mp4",
    val videoCodec: String = "libx264",
    val audioCodec: String = "aac",
    val videoBitrateKbps: Int = 1600,
    val audioBitrateKbps: Int = 128,
    val crf: Int = 23,
    val preset: String = "medium",
    val width: Int? = null,
    val height: Int? = null,
    val keepOriginalResolution: Boolean = false,
    val fps: Int? = null,
    val profile: String = "baseline",
    val pixelFormat: String = "yuv420p",
    val passes: Int = 1,
    val keepMetadata: Boolean = false,
    val keepSubtitles: Boolean = true,
    val keepAllAudioTracks: Boolean = false,
    val removeAudio: Boolean = false,
    val copyVideo: Boolean = false,
    val copyAudio: Boolean = false,
    val encoderPreference: EncoderPreference = EncoderPreference.SOFTWARE,
)

data class CompressionRequest(
    val mode: WorkMode,
    val targetSize: Double,
    val sizeUnit: SizeUnit,
    val priority: QualityPriority,
    val outputTree: Uri?,
    val manual: ManualSettings,
)

data class BitratePlan(
    val videoBitrateKbps: Int,
    val audioBitrateKbps: Int,
    val containerOverheadBytes: Long,
    val predictedBytes: Long,
    val qualityWarning: String?,
    val suggestedWidth: Int?,
    val suggestedHeight: Int?,
    val suggestedFps: Int?,
    val useTwoPass: Boolean,
)

data class EncodingProgress(
    val jobId: Long,
    val fileName: String,
    val progress: Float,
    val status: JobStatus,
    val message: String,
)
