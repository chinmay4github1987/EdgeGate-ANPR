package com.chinmay.edgegate.ui.vehicles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chinmay.edgegate.core.PlateNormalizer
import com.chinmay.edgegate.core.RtoStates
import com.chinmay.edgegate.core.VehicleCategory
import com.chinmay.edgegate.data.db.VehicleEntity
import com.chinmay.edgegate.ui.components.PlateChip
import com.chinmay.edgegate.ui.components.PlateSize
import com.chinmay.edgegate.ui.theme.EdgeText
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Space

@Composable
fun AddVehicleScreen(onSave: (VehicleEntity) -> Unit, onBack: () -> Unit) {
    var plate by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(VehicleCategory.RESIDENT) }
    var blacklisted by rememberSaveable { mutableStateOf(false) }
    var triedSave by rememberSaveable { mutableStateOf(false) }

    val parsed = remember(plate) { PlateNormalizer.parse(plate) }
    val c = EdgeTheme.colors

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = Space.md, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Register vehicle", style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.sm),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Plate with live validation preview
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Plate number")
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    placeholder = { Text("KA 01 AB 1234", style = EdgeText.plateMedium) },
                    singleLine = true,
                    textStyle = EdgeText.plateMedium.copy(fontSize = EdgeText.plateMedium.fontSize * 1.1f),
                    isError = triedSave && parsed == null,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Plate number" },
                )
                if (plate.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(c.card)
                            .border(1.dp, c.cardLine, MaterialTheme.shapes.small).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (parsed != null) {
                            PlateChip(parsed.display, PlateSize.Medium)
                            Icon(Icons.Filled.CheckCircle, null, tint = c.allow, modifier = Modifier.size(18.dp))
                            Text("Valid" + (RtoStates.describe(parsed)?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelMedium, color = c.allowInk)
                        } else {
                            Icon(Icons.Filled.ErrorOutline, null, tint = c.deny, modifier = Modifier.size(18.dp))
                            Text("Not a valid Indian plate yet", style = MaterialTheme.typography.labelMedium, color = c.denyInk)
                        }
                    }
                }
            }

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Category")
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(VehicleCategory.RESIDENT to "Resident", VehicleCategory.STAFF to "Staff", VehicleCategory.VISITOR to "Visitor").forEach { (value, label) ->
                        val on = category == value
                        Box(
                            Modifier.weight(1f).heightIn(min = 44.dp).clip(MaterialTheme.shapes.small)
                                .background(if (on) MaterialTheme.colorScheme.secondaryContainer else c.card)
                                .border(1.dp, if (on) Color.Transparent else c.cardLine, MaterialTheme.shapes.small)
                                .selectable(selected = on, role = Role.RadioButton, onClick = { category = value }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            LabeledField("Owner name", owner, { owner = it }, KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Flat / unit", unit, { unit = it.uppercase() }, KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next), Modifier.weight(1f))
                LabeledField("Phone (optional)", phone, { phone = it }, KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done), Modifier.weight(1f))
            }

            // Blacklist
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(c.card)
                    .border(1.dp, if (blacklisted) c.denyLine else c.cardLine, MaterialTheme.shapes.small)
                    .toggleable(value = blacklisted, role = Role.Switch, onValueChange = { blacklisted = it })
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Blacklist this plate", style = MaterialTheme.typography.titleSmall)
                    Text("The gate will deny it and sound the alarm", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                Switch(checked = blacklisted, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = c.deny, checkedThumbColor = Color.White))
            }
        }

        Row(Modifier.navigationBarsPadding().padding(Space.gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, c.cardLine),
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            Button(
                onClick = {
                    triedSave = true
                    val p = parsed ?: return@Button
                    onSave(
                        VehicleEntity(
                            plate = p.plate, display = p.display, category = category.name, blacklisted = blacklisted,
                            ownerName = owner.trim().ifBlank { null }, unit = unit.trim().ifBlank { null },
                            phone = phone.trim().ifBlank { null }, createdAt = System.currentTimeMillis(),
                        )
                    )
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(2f).height(52.dp),
            ) { Text("Save vehicle") }
        }
    }
}

@Composable
private fun FieldLabel(text: String) = Text(text, style = MaterialTheme.typography.labelLarge)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = EdgeTheme.colors.card,
    focusedContainerColor = EdgeTheme.colors.card,
    unfocusedBorderColor = EdgeTheme.colors.cardLine,
)

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            shape = MaterialTheme.shapes.small, colors = fieldColors(), keyboardOptions = keyboard,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}
