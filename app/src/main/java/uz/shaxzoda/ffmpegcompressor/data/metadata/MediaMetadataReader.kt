package uz.shaxzoda.ffmpegcompressor.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import uz.shaxzoda.ffmpegcompressor.domain.SelectedMedia
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger

class MediaMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun read(uri: Uri): SelectedMedia {
        AppLogger.info("Read metadata uri=$uri")
        val resolver = context.contentResolver
        val nameAndSize = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else uri.lastPathSegment ?: "media"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                name to size
            } else {
                (uri.lastPathSegment ?: "media") to -1L
            }
        } ?: ((uri.lastPathSegment ?: "media") to -1L)

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.long(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val width = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val bitrate = retriever.long(MediaMetadataRetriever.METADATA_KEY_BITRATE).takeIf { it > 0 }
            val fps = retriever.string(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            val hasAudio = retriever.string(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            val hasVideo = retriever.string(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            val media = SelectedMedia(
                uri = uri,
                displayName = nameAndSize.first,
                sizeBytes = nameAndSize.second,
                durationMs = duration,
                width = width,
                height = height,
                frameRate = fps,
                bitrate = bitrate,
                videoCodec = if (hasVideo) "video" else null,
                audioCodec = if (hasAudio) "audio" else null,
                audioBitrate = null,
                audioTracks = if (hasAudio) 1 else 0
            )
            AppLogger.info("Metadata read: name=${media.displayName}, size=${media.sizeBytes}, duration=${media.durationMs}, ${media.width}x${media.height}")
            media
        } catch (throwable: Throwable) {
            AppLogger.error("Metadata read failed for uri=$uri", throwable)
            throw throwable
        } finally {
            retriever.release()
        }
    }

    private fun MediaMetadataRetriever.string(keyCode: Int): String? = extractMetadata(keyCode)
    private fun MediaMetadataRetriever.long(keyCode: Int): Long = string(keyCode)?.toLongOrNull() ?: 0L
    private fun MediaMetadataRetriever.int(keyCode: Int): Int = string(keyCode)?.toIntOrNull() ?: 0
}
