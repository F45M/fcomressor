package uz.shaxzoda.ffmpegcompressor.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import uz.shaxzoda.ffmpegcompressor.data.db.AppDatabase
import uz.shaxzoda.ffmpegcompressor.data.db.EncodeJobDao

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "shaxzoda-encoding.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideEncodeJobDao(database: AppDatabase): EncodeJobDao = database.encodeJobDao()
}
