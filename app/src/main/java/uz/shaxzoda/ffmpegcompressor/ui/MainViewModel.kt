package uz.shaxzoda.ffmpegcompressor.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.shaxzoda.ffmpegcompressor.data.db.EncodeJobEntity
import uz.shaxzoda.ffmpegcompressor.data.metadata.MediaMetadataReader
import uz.shaxzoda.ffmpegcompressor.data.repository.CompressionRepository
import uz.shaxzoda.ffmpegcompressor.domain.BitratePlan
import uz.shaxzoda.ffmpegcompressor.domain.BitratePlanner
import uz.shaxzoda.ffmpegcompressor.domain.CompressionRequest
import uz.shaxzoda.ffmpegcompressor.domain.ManualSettings
import uz.shaxzoda.ffmpegcompressor.domain.QualityPriority
import uz.shaxzoda.ffmpegcompressor.domain.SelectedMedia
import uz.shaxzoda.ffmpegcompressor.domain.SizeUnit
import uz.shaxzoda.ffmpegcompressor.domain.WorkMode
import uz.shaxzoda.ffmpegcompressor.logging.AppLogger
import uz.shaxzoda.ffmpegcompressor.service.EncodingForegroundService
import uz.shaxzoda.ffmpegcompressor.service.EncodingQueue
import uz.shaxzoda.ffmpegcompressor.service.EncodingTask
import uz.shaxzoda.ffmpegcompressor.ui.theme.FCompressorTheme

data class MainUiState(
    val selected: List<SelectedMedia> = emptyList(),
    val outputTree: Uri? = null,
    val mode: WorkMode = WorkMode.TARGET_SIZE,
    val targetSize: String = "40",
    val sizeUnit: SizeUnit = SizeUnit.MB,
    val priority: QualityPriority = QualityPriority.BALANCED,
    val manual: ManualSettings = ManualSettings(),
    val plans: Map<Uri, BitratePlan> = emptyMap(),
    val history: List<EncodeJobEntity> = emptyList(),
    val active: List<EncodeJobEntity> = emptyList(),
    val message: String? = null,
    val logs: String = "",
    val theme: FCompressorTheme = FCompressorTheme.LEMON_COOKIE,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val metadataReader: MediaMetadataReader,
    private val planner: BitratePlanner,
    private val repository: CompressionRepository,
) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("fcompressor_settings", Application.MODE_PRIVATE)
    private val editor = MutableStateFlow(
        MainUiState(
            theme = runCatching {
                FCompressorTheme.valueOf(prefs.getString("theme", FCompressorTheme.LEMON_COOKIE.name) ?: FCompressorTheme.LEMON_COOKIE.name)
            }.getOrDefault(FCompressorTheme.LEMON_COOKIE)
        )
    )

    val uiState: StateFlow<MainUiState> =
        combine(editor, repository.history, repository.active) { state, history, active ->
            state.copy(history = history, active = active)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), editor.value)

    fun selectFiles(uris: List<Uri>) {
        viewModelScope.launch {
            runCatching {
                AppLogger.info("selectFiles count=${uris.size}")
                val context = getApplication<Application>()
                uris.forEach { uri ->
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }.onFailure { AppLogger.error("takePersistableUriPermission failed for $uri", it) }
                }
                val media = uris.map { metadataReader.read(it) }
                editor.update { it.copy(selected = media, message = null) }
                recalculate()
            }.onFailure { throwable ->
                AppLogger.error("selectFiles failed", throwable)
                editor.update { it.copy(message = "Ошибка выбора файла: ${throwable.message}") }
            }
        }
    }

    fun selectOutput(uri: Uri?) {
        if (uri == null) return
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.onFailure { AppLogger.error("takePersistableUriPermission failed for output=$uri", it) }
        AppLogger.info("Output folder selected uri=$uri")
        editor.update { it.copy(outputTree = uri, message = null) }
    }

    fun setMode(mode: WorkMode) {
        editor.update { state ->
            val target = if (mode == WorkMode.AUTO && state.selected.isNotEmpty()) {
                planner.autoTarget(state.selected.first()).toInt().toString()
            } else {
                state.targetSize
            }
            state.copy(mode = mode, targetSize = target)
        }
        recalculate()
    }

    fun setTargetSize(value: String) {
        editor.update { it.copy(targetSize = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) }
        recalculate()
    }

    fun setSizeUnit(unit: SizeUnit) {
        editor.update { it.copy(sizeUnit = unit) }
        recalculate()
    }

    fun setPriority(priority: QualityPriority) {
        editor.update { it.copy(priority = priority) }
        recalculate()
    }

    fun updateManual(manual: ManualSettings) {
        editor.update { it.copy(manual = manual) }
        recalculate()
    }

    fun startCompression() {
        viewModelScope.launch {
            runCatching {
                val state = editor.value
                AppLogger.info("startCompression clicked, files=${state.selected.size}, target=${state.targetSize}${state.sizeUnit}, output=${state.outputTree}")
                if (state.selected.isEmpty()) {
                    editor.update { it.copy(message = "Выберите один или несколько файлов.") }
                    return@launch
                }
                if (state.outputTree == null) {
                    editor.update { it.copy(message = "Выберите папку сохранения.") }
                    return@launch
                }
                val request = requestFromState(editor.value)
                var enqueued = 0
                var failedToStart = false
                state.selected.forEach { media ->
                    val plan = state.plans[media.uri] ?: planner.plan(media, request)
                    val jobId = repository.createJob(media, request, plan)
                    val serviceStarted = runCatching {
                        ContextCompat.startForegroundService(
                            getApplication(),
                            Intent(getApplication(), EncodingForegroundService::class.java)
                        )
                    }.onFailure { AppLogger.error("startForegroundService failed", it) }.isSuccess
                    if (serviceStarted) {
                        EncodingQueue.enqueue(EncodingTask(jobId, media, request, plan))
                        enqueued++
                    } else {
                        failedToStart = true
                        editor.update { it.copy(message = "Android не разрешил запустить foreground service. Откройте приложение и повторите запуск.") }
                    }
                }
                if (!failedToStart) {
                    editor.update { it.copy(message = "Задачи добавлены в очередь: $enqueued.") }
                }
            }.onFailure { throwable ->
                AppLogger.error("startCompression failed", throwable)
                editor.update { it.copy(message = "Ошибка запуска: ${throwable.message}") }
            }
        }
    }

    fun cancel() {
        repository.cancel()
        getApplication<Application>().startService(
            Intent(getApplication(), EncodingForegroundService::class.java).setAction(EncodingForegroundService.ACTION_CANCEL)
        )
    }

    fun refreshLogs() {
        editor.update { it.copy(logs = AppLogger.read()) }
    }

    fun clearLogs() {
        AppLogger.clear()
        refreshLogs()
    }

    fun setTheme(theme: FCompressorTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        editor.update { it.copy(theme = theme) }
    }

    private fun recalculate() {
        val state = editor.value
        val request = requestFromState(state)
        val plans = state.selected.associate { it.uri to planner.plan(it, request) }
        editor.update { it.copy(plans = plans) }
    }

    private fun requestFromState(state: MainUiState): CompressionRequest =
        CompressionRequest(
            mode = state.mode,
            targetSize = state.targetSize.replace(',', '.').toDoubleOrNull() ?: 1.0,
            sizeUnit = state.sizeUnit,
            priority = state.priority,
            outputTree = state.outputTree,
            manual = state.manual
        )
}
