package uz.shaxzoda.ffmpegcompressor.data.ffmpeg

import java.io.File
import javax.inject.Inject
import uz.shaxzoda.ffmpegcompressor.domain.BitratePlan
import uz.shaxzoda.ffmpegcompressor.domain.CompressionRequest
import uz.shaxzoda.ffmpegcompressor.domain.EncoderPreference
import uz.shaxzoda.ffmpegcompressor.domain.WorkMode

class FFmpegCommandBuilder @Inject constructor() {
    fun build(
        input: File,
        output: File,
        request: CompressionRequest,
        plan: BitratePlan,
        passLog: File,
    ): List<List<String>> {
        val settings = request.manual
        val codec = when {
            settings.copyVideo -> "copy"
            settings.encoderPreference == EncoderPreference.HARDWARE -> "h264_mediacodec"
            settings.videoCodec.isNotBlank() -> settings.videoCodec
            settings.encoderPreference == EncoderPreference.AUTO -> "libx264"
            else -> "libx264"
        }
        val audioCodec = when {
            settings.removeAudio -> null
            settings.copyAudio -> "copy"
            settings.audioCodec.isNotBlank() -> settings.audioCodec
            else -> "aac"
        }
        val filters = mutableListOf<String>()
        val width = if (settings.keepOriginalResolution) null else settings.width ?: plan.suggestedWidth
        val height = if (settings.keepOriginalResolution) null else settings.height ?: plan.suggestedHeight
        if (width != null && height != null) filters += "scale=$width:$height:flags=lanczos"
        val fps = settings.fps ?: plan.suggestedFps
        if (fps != null) filters += "fps=$fps"

        fun base(pass: Int?): MutableList<String> {
            val firstPass = pass == 1
            val args = mutableListOf("-hide_banner", "-y", "-i", input.absolutePath)
            if (!settings.keepMetadata) args += listOf("-map_metadata", "-1")
            if (settings.keepSubtitles && !firstPass) args += listOf("-map", "0")
            if (settings.removeAudio || firstPass) args += "-an"
            if (firstPass) args += "-sn"
            if (filters.isNotEmpty()) args += listOf("-vf", filters.joinToString(","))

            args += listOf("-c:v", codec)
            if (codec != "copy") {
                val bitrateMode = request.mode != WorkMode.MANUAL || settings.passes > 1 || codec == "h264_mediacodec"
                if (bitrateMode) {
                    args += listOf("-b:v", "${plan.videoBitrateKbps}k")
                    args += listOf("-maxrate", "${(plan.videoBitrateKbps * 1.45).toInt()}k")
                    args += listOf("-bufsize", "${(plan.videoBitrateKbps * 2).coerceAtLeast(512)}k")
                } else if (settings.crf > 0) {
                    args += listOf("-crf", settings.crf.toString())
                }

                if (settings.preset.isNotBlank() && codec.startsWith("lib")) args += listOf("-preset", settings.preset)
                if (settings.profile.isNotBlank() && codec == "libx264") args += listOf("-profile:v", settings.profile)
                if (settings.pixelFormat.isNotBlank() && codec != "h264_mediacodec") args += listOf("-pix_fmt", settings.pixelFormat)
                if (codec == "h264_mediacodec") args += listOf("-g", "60")
            }

            if (audioCodec != null && !firstPass) {
                args += listOf("-c:a", audioCodec)
                if (audioCodec != "copy") args += listOf("-b:a", "${plan.audioBitrateKbps}k")
            }
            if (pass != null) {
                args += listOf("-pass", pass.toString(), "-passlogfile", passLog.absolutePath)
            }
            if (!firstPass && output.extension.lowercase() == "mp4") args += listOf("-movflags", "+faststart")
            return args
        }

        val supportsTwoPass = codec.startsWith("lib") || codec == "mpeg4"
        return if ((plan.useTwoPass || settings.passes > 1) && supportsTwoPass) {
            listOf(
                base(1).apply { addAll(listOf("-f", "null", "-")) },
                base(2).apply { add(output.absolutePath) }
            )
        } else {
            listOf(base(null).apply { add(output.absolutePath) })
        }
    }
}
