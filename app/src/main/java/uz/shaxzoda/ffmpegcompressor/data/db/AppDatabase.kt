package uz.shaxzoda.ffmpegcompressor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import uz.shaxzoda.ffmpegcompressor.domain.JobStatus

class JobConverters {
    @TypeConverter fun fromStatus(status: JobStatus): String = status.name
    @TypeConverter fun toStatus(value: String): JobStatus = JobStatus.valueOf(value)
}

@Database(entities = [EncodeJobEntity::class], version = 1, exportSchema = false)
@TypeConverters(JobConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun encodeJobDao(): EncodeJobDao
}
