package uz.shaxzoda.ffmpegcompressor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import uz.shaxzoda.ffmpegcompressor.domain.JobStatus

@Entity(tableName = "encode_jobs")
data class EncodeJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inputName: String,
    val inputUri: String,
    val outputUri: String?,
    val sourceBytes: Long,
    val resultBytes: Long?,
    val mode: String,
    val codec: String,
    val videoBitrateKbps: Int,
    val audioBitrateKbps: Int,
    val status: JobStatus,
    val progress: Float,
    val error: String?,
    val createdAt: Long,
    val finishedAt: Long?,
)
