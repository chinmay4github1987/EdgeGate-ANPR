package com.chinmay.edgegate.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chinmay.edgegate.AppContainer
import com.chinmay.edgegate.ai.EngineStatus
import com.chinmay.edgegate.core.DaySummary
import com.chinmay.edgegate.core.Direction
import com.chinmay.edgegate.core.EventLite
import com.chinmay.edgegate.core.GateAction
import com.chinmay.edgegate.core.GateStats
import com.chinmay.edgegate.core.VehicleCategory
import com.chinmay.edgegate.data.db.GateEventEntity
import com.chinmay.edgegate.ui.components.ActionBadge
import com.chinmay.edgegate.ui.components.EdgeCard
import com.chinmay.edgegate.ui.components.EmptyState
import com.chinmay.edgegate.ui.components.PlateChip
import com.chinmay.edgegate.ui.components.PlateSize
import com.chinmay.edgegate.ui.components.StatusPill
import com.chinmay.edgegate.ui.theme.EdgeText
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Palette
import com.chinmay.edgegate.ui.theme.Space
import com.chinmay.edgegate.util.GateSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DashboardUi(
    val summary: DaySummary = GateStats.summarize(emptyList(), 0, 0),
    val insideNow: Int = 0,
    val registered: Int = 0,
    val recent: List<GateEventEntity> = emptyList(),
    val settings: GateSettings = GateSettings(),
)

class DashboardViewModel(container: AppContainer) : ViewModel() {
    private val startOfDay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val zoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
    private val repo = container.repository

    val engine: StateFlow<EngineStatus> = container.engineMonitor.status

    val ui: StateFlow<DashboardUi> = combine(
        repo.eventsSince(startOfDay),
        repo.insideCount,
        repo.vehicles,
        container.settings.state,
    ) { events, inside, vehicles, settings ->
        DashboardUi(
            summary = GateStats.summarize(events.map { it.toLite() }, startOfDay, zoneOffset),
            insideNow = inside,
            registered = vehicles.count { !it.blacklisted },
            recent = events.take(5),
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUi())

    private fun GateEventEntity.toLite() = EventLite(
        timestampMs = timestamp,
        direction = runCatching { Direction.valueOf(direction) }.getOrDefault(Direction.ENTRY),
        action = runCatching { GateAction.valueOf(action) }.getOrDefault(GateAction.ALLOW),
        category = runCatching { VehicleCategory.valueOf(category) }.getOrDefault(VehicleCategory.UNKNOWN),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(vm: DashboardViewModel, onOpenSettings: () -> Unit, onOpenLog: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val s = ui.summary
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(bottom = Space.xl),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.gutter, bottom = Space.md), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(ui.settings.gateName, style = MaterialTheme.typography.labelMedium, color = EdgeTheme.colors.textMuted)
                Text("Today", style = MaterialTheme.typography.headlineMedium)
            }
            OutlinedIconButton(
                onClick = onOpenSettings,
                border = BorderStroke(1.dp, EdgeTheme.colors.cardLine),
                colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = EdgeTheme.colors.card),
                modifier = Modifier.padding(top = 2.dp),
            ) { Icon(Icons.Outlined.Settings, "Settings") }
        }

        // Status pills
        FlowRow(Modifier.padding(horizontal = Space.gutter), horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (engine.cameraLive) StatusPill("Camera live", dotColor = EdgeTheme.colors.allow, highlighted = true)
            else StatusPill("Camera idle", dotColor = EdgeTheme.colors.textMuted)
            StatusPill(
                when {
                    engine.runtimeLabel == "Not started" -> "AI engine loads on first scan"
                    engine.usesGpu -> "On-device · LiteRT GPU"
                    engine.usesDetector -> "On-device · LiteRT CPU"
                    else -> "On-device · OCR only"
                },
            )
            StatusPill("Offline ready")
        }

        // KPI grid
        Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Kpi("Entries", s.entries.toString(), "${s.visitors} visitors", Modifier.weight(1f))
                Kpi("Exits", s.exits.toString(), "since 00:00", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Kpi("Inside now", ui.insideNow.toString(), "of ${ui.registered} registered", Modifier.weight(1f), inverted = true)
                Kpi(
                    "Alerts", s.alerts.toString(),
                    s.lastAlertAtMs?.let { "Blacklisted · " + timeFmt.format(Date(it)) } ?: "None today",
                    Modifier.weight(1f), alert = s.alerts > 0,
                )
            }
        }

        // Hourly chart
        EdgeCard(Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.lg)) {
            Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Traffic by hour", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    s.peakHour?.let {
                        Text("Peak %02d:00 · %d vehicles".format(it, s.hourly[it]), style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted)
                    }
                }
                HourlyBars(s.hourly)
            }
        }

        // Recent activity
        EdgeCard(Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.lg)) {
            Row(Modifier.fillMaxWidth().padding(start = Space.lg, end = Space.sm, top = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenLog) { Text("View log") }
            }
            if (ui.recent.isEmpty()) {
                EmptyState(Icons.Outlined.DirectionsCar, "No vehicles yet today", "Open Scan to start reading plates at the gate.")
            }
            ui.recent.forEach { e ->
                HorizontalDivider(color = EdgeTheme.colors.divider)
                Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    PlateChip(e.display, PlateSize.Small)
                    Column(Modifier.weight(1f)) {
                        Text(e.note ?: e.category.lowercase(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(
                            "${e.direction.lowercase().replaceFirstChar { it.uppercase() }} · ${timeFmt.format(Date(e.timestamp))}",
                            style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted,
                        )
                    }
                    ActionBadge(runCatching { GateAction.valueOf(e.action) }.getOrDefault(GateAction.ALLOW))
                }
            }
        }

        // Engine health
        EdgeCard(Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.lg)) {
            Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("AI engine", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(engine.runtimeLabel.substringBefore(" + "), style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted)
                }
                Row {
                    Metric(if (engine.usesDetector) "%.0f".format(engine.detectP50) else "–", "ms", "detect p50", Modifier.weight(1f))
                    Metric("%.0f".format(engine.ocrP50), "ms", "OCR p50", Modifier.weight(1f))
                    Metric("%.0f".format(engine.frameP90), "ms", "frame p90", Modifier.weight(1f))
                    Metric("%.0f".format(engine.fps), "", "fps", Modifier.weight(1f))
                }
                if (!engine.cameraLive) {
                    Text("Live figures appear while the Scan screen is open.", style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, caption: String, modifier: Modifier, inverted: Boolean = false, alert: Boolean = false) {
    val c = EdgeTheme.colors
    val bg = when { inverted -> MaterialTheme.colorScheme.secondaryContainer; alert -> c.denyWash; else -> c.card }
    val fg = when { inverted -> MaterialTheme.colorScheme.onSecondaryContainer; alert -> c.denyInk; else -> MaterialTheme.colorScheme.onSurface }
    val sub = when { inverted -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f); alert -> c.denyInk; else -> c.textMuted }
    EdgeCard(modifier.semantics(mergeDescendants = true) {}, color = bg, borderColor = if (inverted) Color.Transparent else if (alert) c.denyLine else c.cardLine) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = sub)
            Text(value, style = EdgeText.kpi, color = fg)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = sub, maxLines = 1)
        }
    }
}

@Composable
private fun Metric(value: String, unit: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = EdgeText.metric)
            if (unit.isNotEmpty()) Text(" $unit", style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted)
    }
}

/** 06:00–22:59 bars; the peak hour is drawn in full teal, others in tint. */
@Composable
private fun HourlyBars(hourly: List<Int>) {
    val hours = 6..22
    val values = hours.map { hourly.getOrElse(it) { 0 } }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val peak = values.indexOf(values.maxOrNull() ?: 0).takeIf { (values.maxOrNull() ?: 0) > 0 }
    val strong = Palette.Teal
    val soft = Color(0xFF9FD3CC)
    val axis = EdgeTheme.colors.cardLine
    val description = "Vehicles per hour: " + hours.zip(values).filter { it.second > 0 }.joinToString { "%02d:00 %d".format(it.first, it.second) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = description }) {
            val gap = 4.dp.toPx()
            val barW = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { i, v ->
                val h = if (v == 0) 2.dp.toPx() else (v.toFloat() / max) * (size.height - 4.dp.toPx())
                drawRoundRect(
                    color = if (i == peak) strong else soft,
                    topLeft = Offset(i * (barW + gap), size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
            drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("06", "10", "14", "18", "22").forEach { Text(it, style = EdgeText.mono, color = EdgeTheme.colors.textMuted) }
        }
    }
}
