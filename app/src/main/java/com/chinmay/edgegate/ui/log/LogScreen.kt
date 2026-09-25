package com.chinmay.edgegate.ui.log

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chinmay.edgegate.core.GateAction
import com.chinmay.edgegate.core.PlateNormalizer
import com.chinmay.edgegate.data.GateRepository
import com.chinmay.edgegate.data.db.GateEventEntity
import com.chinmay.edgegate.ui.components.ActionBadge
import com.chinmay.edgegate.ui.components.EmptyState
import com.chinmay.edgegate.ui.components.FilterPills
import com.chinmay.edgegate.ui.components.ScreenHeader
import com.chinmay.edgegate.ui.components.SearchField
import com.chinmay.edgegate.ui.components.SectionLabel
import com.chinmay.edgegate.ui.theme.EdgeText
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class LogFilter { ALL, ENTRIES, EXITS, VISITORS, ALERTS }

class LogViewModel(private val repo: GateRepository) : ViewModel() {
    private val startOfDay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val events: StateFlow<List<GateEventEntity>> =
        repo.recentEvents(limit = 1_000).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Writes today's register to a CSV in cache and opens the share sheet (WhatsApp, email, Drive…). */
    fun exportToday(context: Context) = viewModelScope.launch {
        val csv = repo.exportCsv(startOfDay)
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val name = "gate-log-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + ".csv"
            File(dir, name).apply { writeText(csv) }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Export gate register"))
    }
}

private fun GateEventEntity.matches(filter: LogFilter) = when (filter) {
    LogFilter.ALL -> true
    LogFilter.ENTRIES -> direction == "ENTRY" && action != GateAction.DENY_BLACKLISTED.name
    LogFilter.EXITS -> direction == "EXIT"
    LogFilter.VISITORS -> action == GateAction.ALLOW_LOG_VISITOR.name
    LogFilter.ALERTS -> action == GateAction.DENY_BLACKLISTED.name
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogScreen(vm: LogViewModel) {
    val context = LocalContext.current
    val events by vm.events.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }

    val clean = PlateNormalizer.clean(query)
    val filtered = events.filter { it.matches(filter) && (clean.isEmpty() || it.plate.contains(clean)) }
    val dayFmt = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val today = remember { dayFmt.format(Date()) }
    val groups = filtered.groupBy { dayFmt.format(Date(it.timestamp)) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        ScreenHeader("Gate log") {
            OutlinedButton(
                onClick = { vm.exportToday(context) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, EdgeTheme.colors.cardLine),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text("Export CSV", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        SearchField(query, { query = it }, "Search plate, e.g. KA01", Modifier.padding(horizontal = Space.gutter))
        FilterPills(
            options = listOf(
                LogFilter.ALL to "All ${events.size}",
                LogFilter.ENTRIES to "Entries",
                LogFilter.EXITS to "Exits",
                LogFilter.VISITORS to "Visitors",
                LogFilter.ALERTS to "Alerts",
            ),
            selected = filter,
            onSelect = { filter = it },
        )

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                if (events.isEmpty()) "The register is empty" else "No matching entries",
                if (events.isEmpty()) "Every vehicle the gate reads will be listed here." else "Try another plate or filter.",
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.xl)) {
            groups.forEach { (day, rows) ->
                stickyHeader(key = "h-$day") {
                    SectionLabel(
                        (if (day == today) "Today · " else "") + day,
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = Space.sm, bottom = Space.sm),
                    )
                }
                itemsIndexed(rows, key = { _, e -> e.id }) { i, e ->
                    val shape = when {
                        rows.size == 1 -> RoundedCornerShape(16.dp)
                        i == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        i == rows.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    Column(Modifier.clip(shape).background(EdgeTheme.colors.card)) {
                        LogRow(e, timeFmt.format(Date(e.timestamp)))
                        if (i != rows.lastIndex) HorizontalDivider(color = EdgeTheme.colors.divider)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(e: GateEventEntity, time: String) {
    val entry = e.direction == "ENTRY"
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (entry) MaterialTheme.colorScheme.primaryContainer else EdgeTheme.colors.divider),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (entry) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                if (entry) "Entry" else "Exit",
                tint = if (entry) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(e.display, style = EdgeText.plateMedium.copy(fontSize = EdgeText.plateMedium.fontSize * 0.94f))
            val source = if (e.source == "AI") "AI ${(e.confidence * 100).toInt()}%" else "Manual"
            Text(
                listOfNotNull(e.note, source).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted, maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(time, style = EdgeText.mono)
            ActionBadge(runCatching { GateAction.valueOf(e.action) }.getOrDefault(GateAction.ALLOW))
        }
    }
}
