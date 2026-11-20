package uz.shaxzoda.ffmpegcompressor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EncodeJobDao {
    @Query("SELECT * FROM encode_jobs ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<EncodeJobEntity>>

    @Query("SELECT * FROM encode_jobs WHERE status IN ('QUEUED','RUNNING') ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<EncodeJobEntity>>

    @Insert
    suspend fun insert(entity: EncodeJobEntity): Long

    @Update
    suspend fun update(entity: EncodeJobEntity)

    @Query("SELECT * FROM encode_jobs WHERE id = :id")
    suspend fun get(id: Long): EncodeJobEntity?

    @Query("UPDATE encode_jobs SET status = :status, progress = :progress, error = :error, finishedAt = :finishedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, progress: Float, error: String?, finishedAt: Long?)
}
