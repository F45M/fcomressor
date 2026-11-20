package uz.shaxzoda.ffmpegcompressor.data.ffmpeg

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class FFmpegBinaryProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ffmpeg(): File = resolve("libffmpeg.so", "ffmpeg")
    fun ffprobe(): File = resolve("libffprobe.so", "ffprobe")

    private fun resolve(nativeName: String, plainName: String): File {
        val native = File(context.applicationInfo.nativeLibraryDir, nativeName)
        if (native.exists() && native.canExecute()) return native
        val plain = File(context.applicationInfo.nativeLibraryDir, plainName)
        if (plain.exists() && plain.canExecute()) return plain
        return native
    }

    fun hasFFmpeg(): Boolean = ffmpeg().exists() && ffmpeg().canExecute()
}
