package uz.shaxzoda.ffmpegcompressor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.shaxzoda.ffmpegcompressor.data.db.EncodeJobEntity
import uz.shaxzoda.ffmpegcompressor.domain.BitratePlan
import uz.shaxzoda.ffmpegcompressor.domain.ManualSettings
import uz.shaxzoda.ffmpegcompressor.domain.QualityPriority
import uz.shaxzoda.ffmpegcompressor.domain.SelectedMedia
import uz.shaxzoda.ffmpegcompressor.domain.SizeUnit
import uz.shaxzoda.ffmpegcompressor.domain.WorkMode
import uz.shaxzoda.ffmpegcompressor.ui.theme.FCompressorTheme
import uz.shaxzoda.ffmpegcompressor.ui.theme.ShaxzodaTheme

private val CardShape = RoundedCornerShape(5.dp)
private val InnerShape = RoundedCornerShape(5.dp)

private data class BottomDestination(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.selectFiles(uris)
    }
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.selectOutput(uri)
    }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        BottomDestination("Сжатие", Icons.Outlined.VideoFile),
        BottomDestination("Очередь", Icons.Outlined.Queue),
        BottomDestination("История", Icons.Outlined.History),
        BottomDestination("Настройки", Icons.Outlined.Settings),
    )

    ShaxzodaTheme(theme = state.theme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, destination ->
                            val selected = tab == index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(InnerShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { tab = index }
                                    )
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(InnerShape)
                                        .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                when (tab) {
                    0 -> HomeTab(
                        state = state,
                        onPickFiles = { filePicker.launch(arrayOf("video/*", "audio/*", "application/octet-stream")) },
                        onPickOutput = { treePicker.launch(null) },
                        onMode = viewModel::setMode,
                        onTarget = viewModel::setTargetSize,
                        onUnit = viewModel::setSizeUnit,
                        onPriority = viewModel::setPriority,
                        onManual = viewModel::updateManual,
                        onStart = viewModel::startCompression,
                        padding = padding
                    )

                    1 -> JobsTab(state.active, viewModel::cancel, padding)
                    2 -> HistoryTab(state.history, padding)
                    else -> SettingsTab(
                        state = state,
                        onThemeSelected = viewModel::setTheme,
                        onRefreshLogs = viewModel::refreshLogs,
                        onClearLogs = viewModel::clearLogs,
                        padding = padding
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun FilterRow(options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.68f),
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.52f),
                    selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeTab(
    state: MainUiState,
    onPickFiles: () -> Unit,
    onPickOutput: () -> Unit,
    onMode: (WorkMode) -> Unit,
    onTarget: (String) -> Unit,
    onUnit: (SizeUnit) -> Unit,
    onPriority: (QualityPriority) -> Unit,
    onManual: (ManualSettings) -> Unit,
    onStart: () -> Unit,
    padding: PaddingValues,
) {
    ScreenColumn(padding) {
        Text("FComressor", style = MaterialTheme.typography.headlineMedium)
        SummaryCard("Быстрые действия") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPickFiles,
                    modifier = Modifier.weight(1f),
                    shape = InnerShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Outlined.VideoFile, null)
                    Text("Файлы", Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onPickOutput,
                    modifier = Modifier.weight(1f),
                    shape = InnerShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Rounded.Folder, null)
                    Text(if (state.outputTree == null) "Папка" else "Выбрана", Modifier.padding(start = 8.dp))
                }
            }
        }
        SummaryCard("Режим") {
            FilterRow(
                options = listOf("size" to "По размеру", "auto" to "Авто", "manual" to "Ручной", "audio" to "Аудио"),
                selected = when (state.mode) {
                    WorkMode.TARGET_SIZE -> "size"
                    WorkMode.AUTO -> "auto"
                    WorkMode.MANUAL -> "manual"
                    WorkMode.AUDIO -> "audio"
                },
                onSelected = {
                    onMode(
                        when (it) {
                            "auto" -> WorkMode.AUTO
                            "manual" -> WorkMode.MANUAL
                            "audio" -> WorkMode.AUDIO
                            else -> WorkMode.TARGET_SIZE
                        }
                    )
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.targetSize,
                    onValueChange = onTarget,
                    label = { Text("Желаемый размер") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilterChip(selected = state.sizeUnit == SizeUnit.MB, onClick = { onUnit(SizeUnit.MB) }, label = { Text("МБ") })
                FilterChip(selected = state.sizeUnit == SizeUnit.GB, onClick = { onUnit(SizeUnit.GB) }, label = { Text("ГБ") })
            }
            FilterRow(
                options = listOf("quality" to "Качество", "balance" to "Баланс", "compression" to "Сжатие"),
                selected = when (state.priority) {
                    QualityPriority.MAX_QUALITY -> "quality"
                    QualityPriority.BALANCED -> "balance"
                    QualityPriority.MAX_COMPRESSION -> "compression"
                },
                onSelected = {
                    onPriority(
                        when (it) {
                            "quality" -> QualityPriority.MAX_QUALITY
                            "compression" -> QualityPriority.MAX_COMPRESSION
                            else -> QualityPriority.BALANCED
                        }
                    )
                }
            )
            ToggleRow("Сохранять исходное разрешение", state.manual.keepOriginalResolution) {
                onManual(state.manual.copy(keepOriginalResolution = it))
            }
        }
        if (state.mode == WorkMode.MANUAL || state.mode == WorkMode.AUDIO) {
            ManualControls(state.manual, onManual)
        }
        state.selected.forEach { media -> MediaCard(media, state.plans[media.uri]) }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = InnerShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Rounded.PlayArrow, null)
            Text("Сжать", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ManualControls(settings: ManualSettings, onManual: (ManualSettings) -> Unit) {
    SummaryCard("Ручные параметры") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(settings.container, { onManual(settings.copy(container = it)) }, label = { Text("Контейнер") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(settings.videoCodec, { onManual(settings.copy(videoCodec = it)) }, label = { Text("Видео кодек") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(settings.videoBitrateKbps.toString(), { onManual(settings.copy(videoBitrateKbps = it.toIntOrNull() ?: settings.videoBitrateKbps)) }, label = { Text("Видео Kbps") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(settings.audioBitrateKbps.toString(), { onManual(settings.copy(audioBitrateKbps = it.toIntOrNull() ?: settings.audioBitrateKbps)) }, label = { Text("Аудио Kbps") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Text("CRF: ${settings.crf}")
        Slider(value = settings.crf.toFloat(), onValueChange = { onManual(settings.copy(crf = it.toInt())) }, valueRange = 0f..35f, steps = 34)
        ToggleRow("Удалить аудио", settings.removeAudio) { onManual(settings.copy(removeAudio = it)) }
        ToggleRow("Копировать видео", settings.copyVideo) { onManual(settings.copy(copyVideo = it)) }
        ToggleRow("Копировать аудио", settings.copyAudio) { onManual(settings.copy(copyAudio = it)) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MediaCard(media: SelectedMedia, plan: BitratePlan?) {
    SummaryCard(media.displayName) {
        MetricLine("Исходный размер", size(media.sizeBytes))
        MetricLine("Длительность", duration(media.durationMs))
        if (media.width > 0) MetricLine("Разрешение", "${media.width}x${media.height}")
        plan?.let {
            MetricLine("Прогноз", size(it.predictedBytes))
            MetricLine("Видео", "${it.videoBitrateKbps} Kbps")
            MetricLine("Аудио", "${it.audioBitrateKbps} Kbps")
            if (it.suggestedWidth != null && it.suggestedHeight != null) {
                MetricLine("Будет уменьшено до", "${it.suggestedWidth}x${it.suggestedHeight}")
            }
            it.qualityWarning?.let { warning -> Text(warning, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun JobsTab(active: List<EncodeJobEntity>, onCancel: () -> Unit, padding: PaddingValues) {
    ScreenColumn(padding) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Очередь задач", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onCancel) { Icon(Icons.Rounded.Cancel, "Отмена") }
        }
        active.forEach { job ->
            SummaryCard(job.inputName) {
                LinearProgressIndicator(progress = { job.progress }, modifier = Modifier.fillMaxWidth())
                MetricLine("Статус", job.status.name)
                MetricLine("Прогресс", "${(job.progress * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<EncodeJobEntity>, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("История", style = MaterialTheme.typography.headlineSmall) }
        items(history, key = { it.id }) { job ->
            SummaryCard(job.inputName) {
                MetricLine("Статус", job.status.name)
                MetricLine("Размер", "${size(job.sourceBytes)} -> ${size(job.resultBytes ?: 0)}")
                job.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    state: MainUiState,
    onThemeSelected: (FCompressorTheme) -> Unit,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    padding: PaddingValues,
) {
    ScreenColumn(padding) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        SummaryCard("Тема") {
            Text("Выберите оформление приложения.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FCompressorTheme.values().forEach { theme ->
                val selected = state.theme == theme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(InnerShape)
                        .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f))
                        .clickable { onThemeSelected(theme) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(theme.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    Text(if (selected) "Выбрано" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SummaryCard("Логи") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefreshLogs, shape = InnerShape) { Text("Обновить") }
                OutlinedButton(onClick = onClearLogs, shape = InnerShape) { Text("Очистить") }
            }
            Text(
                text = state.logs.ifBlank { "Нажмите «Обновить» после сбоя." },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 18,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun size(bytes: Long): String {
    if (bytes <= 0) return "неизвестно"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) "%.2f ГБ".format(mb / 1024.0) else "%.1f МБ".format(mb)
}

private fun duration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
