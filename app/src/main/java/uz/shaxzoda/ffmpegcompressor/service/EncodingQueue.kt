package uz.shaxzoda.ffmpegcompressor.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import uz.shaxzoda.ffmpegcompressor.domain.BitratePlan
import uz.shaxzoda.ffmpegcompressor.domain.CompressionRequest
import uz.shaxzoda.ffmpegcompressor.domain.SelectedMedia
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger

data class EncodingTask(
    val jobId: Long,
    val media: SelectedMedia,
    val request: CompressionRequest,
    val plan: BitratePlan,
)

object EncodingQueue {
    private val channel = Channel<EncodingTask>(Channel.UNLIMITED)
    val tasks = channel.receiveAsFlow()

    fun enqueue(task: EncodingTask) {
        AppLogger.info("Queue enqueue job=${task.jobId}, file=${task.media.displayName}")
        channel.trySend(task)
    }
}
