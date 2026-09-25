package com.chinmay.edgegate.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.edgegate.core.GateMode
import com.chinmay.edgegate.ui.components.EdgeCard
import com.chinmay.edgegate.ui.components.SectionLabel
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Space
import com.chinmay.edgegate.util.GateSettings
import com.chinmay.edgegate.util.Settings

class SettingsViewModel(private val settings: Settings) : ViewModel() {
    val state = settings.state
    fun update(transform: (GateSettings) -> GateSettings) = settings.update(transform)
}

private sealed interface Editor {
    data object GateName : Editor
    data object Mode : Editor
    data object Frames : Editor
    data object Cooldown : Editor
    data object Phone : Editor
    data object Retention : Editor
}

private val modeLabels = mapOf(GateMode.ENTRY_ONLY to "Entry lane", GateMode.EXIT_ONLY to "Exit lane", GateMode.AUTO to "Auto (shared gate)")

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<Editor?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = Space.md, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Group("Gate") {
                ValueRow("Gate name", s.gateName) { editor = Editor.GateName }
                Divider()
                ValueRow("Default mode", modeLabels.getValue(s.gateMode)) { editor = Editor.Mode }
            }
            Group("AI engine") {
                SwitchRow("Use GPU acceleration", "Falls back to CPU if unsupported. Applies next time Scan opens.", s.preferGpu) { v -> vm.update { it.copy(preferGpu = v) } }
                Divider()
                ValueRow("Frames to confirm", "${s.votingFrames}") { editor = Editor.Frames }
                Divider()
                ValueRow("Repeat cooldown", "${s.cooldownSec} s") { editor = Editor.Cooldown }
            }
            Group("Alerts") {
                SwitchRow("Alarm sound on deny", null, s.alarmOnDeny) { v -> vm.update { it.copy(alarmOnDeny = v) } }
                Divider()
                SwitchRow("Confirmation beep on allow", null, s.beepOnAllow) { v -> vm.update { it.copy(beepOnAllow = v) } }
                Divider()
                ValueRow("Supervisor phone", s.supervisorPhone.ifBlank { "Not set" }) { editor = Editor.Phone }
            }
            Group("Privacy & data") {
                ValueRow("Keep logs for", "${s.retentionDays} days") { editor = Editor.Retention }
                Divider()
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        "Video never leaves this phone. Only plate text, time and decision are stored. The app has no internet permission.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text("EdgeGate ANPR · v1.1", style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    when (editor) {
        Editor.GateName -> TextEditor("Gate name", s.gateName, KeyboardType.Text, onDismiss = { editor = null }) { v -> vm.update { it.copy(gateName = v.ifBlank { "Main Gate" }) } }
        Editor.Phone -> TextEditor("Supervisor phone", s.supervisorPhone, KeyboardType.Phone, onDismiss = { editor = null }) { v -> vm.update { it.copy(supervisorPhone = v.trim()) } }
        Editor.Mode -> ChoiceEditor("Default mode", GateMode.entries.toList(), s.gateMode, { modeLabels.getValue(it) }, onDismiss = { editor = null }) { v -> vm.update { it.copy(gateMode = v) } }
        Editor.Frames -> ChoiceEditor("Frames to confirm", listOf(1, 2, 3, 4, 5), s.votingFrames, { if (it == 3) "3 (recommended)" else "$it" }, onDismiss = { editor = null }) { v -> vm.update { it.copy(votingFrames = v) } }
        Editor.Cooldown -> ChoiceEditor("Repeat cooldown", listOf(30, 60, 120, 300), s.cooldownSec, { "$it seconds" }, onDismiss = { editor = null }) { v -> vm.update { it.copy(cooldownSec = v) } }
        Editor.Retention -> ChoiceEditor("Keep logs for", listOf(30, 90, 180, 365), s.retentionDays, { "$it days" }, onDismiss = { editor = null }) { v -> vm.update { it.copy(retentionDays = v) } }
        null -> Unit
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        EdgeCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = EdgeTheme.colors.divider)

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = EdgeTheme.colors.textMuted)
    }
}

@Composable
private fun SwitchRow(label: String, caption: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TextEditor(title: String, initial: String, keyboard: KeyboardType, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(title) },
        text = {
            OutlinedTextField(text, { text = it }, singleLine = true, shape = MaterialTheme.shapes.small, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
        },
        confirmButton = { Button(onClick = { onSave(text); onDismiss() }, shape = MaterialTheme.shapes.small) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun <T> ChoiceEditor(title: String, options: List<T>, selected: T, label: (T) -> String, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = o == selected, role = Role.RadioButton, onClick = { onPick(o); onDismiss() }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = o == selected, onClick = null)
                        Text(label(o), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
