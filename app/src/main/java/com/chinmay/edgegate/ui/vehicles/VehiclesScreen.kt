package com.chinmay.edgegate.ui.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chinmay.edgegate.core.VehicleCategory
import com.chinmay.edgegate.data.GateRepository
import com.chinmay.edgegate.data.db.VehicleEntity
import com.chinmay.edgegate.ui.components.CategoryBadge
import com.chinmay.edgegate.ui.components.EdgeCard
import com.chinmay.edgegate.ui.components.EmptyState
import com.chinmay.edgegate.ui.components.FilterPills
import com.chinmay.edgegate.ui.components.ScreenHeader
import com.chinmay.edgegate.ui.components.SearchField
import com.chinmay.edgegate.ui.theme.EdgeText
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Space
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VehicleFilter { ALL, RESIDENTS, STAFF, VISITORS, BLOCKED }

class VehiclesViewModel(private val repo: GateRepository) : ViewModel() {
    val vehicles = repo.vehicles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setBlacklisted(plate: String, value: Boolean) = viewModelScope.launch { repo.setBlacklisted(plate, value) }
    fun delete(plate: String) = viewModelScope.launch { repo.deleteVehicle(plate) }
    fun save(vehicle: VehicleEntity) = viewModelScope.launch { repo.saveVehicle(vehicle) }
}

private fun VehicleEntity.matches(f: VehicleFilter) = when (f) {
    VehicleFilter.ALL -> true
    VehicleFilter.RESIDENTS -> category == VehicleCategory.RESIDENT.name && !blacklisted
    VehicleFilter.STAFF -> category == VehicleCategory.STAFF.name && !blacklisted
    VehicleFilter.VISITORS -> category == VehicleCategory.VISITOR.name && !blacklisted
    VehicleFilter.BLOCKED -> blacklisted
}

@Composable
fun VehiclesScreen(vm: VehiclesViewModel, onAdd: () -> Unit) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(VehicleFilter.ALL) }
    val q = query.trim().uppercase().replace(" ", "")
    val shown = vehicles.filter {
        it.matches(filter) && (q.isEmpty() || it.plate.contains(q) ||
            it.ownerName?.uppercase()?.replace(" ", "")?.contains(q) == true ||
            it.unit?.uppercase()?.replace(" ", "")?.contains(q) == true)
    }
    val blocked = vehicles.count { it.blacklisted }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader("Vehicles", subtitle = "${vehicles.size - blocked} registered · $blocked blocked")
            SearchField(query, { query = it }, "Search plate, name or flat", Modifier.padding(horizontal = Space.gutter))
            FilterPills(
                options = listOf(
                    VehicleFilter.ALL to "All",
                    VehicleFilter.RESIDENTS to "Residents",
                    VehicleFilter.STAFF to "Staff",
                    VehicleFilter.VISITORS to "Visitors",
                    VehicleFilter.BLOCKED to "Blocked $blocked",
                ),
                selected = filter,
                onSelect = { filter = it },
            )
            if (shown.isEmpty()) {
                EmptyState(
                    Icons.Outlined.DirectionsCar,
                    if (vehicles.isEmpty()) "No vehicles registered" else "No matches",
                    if (vehicles.isEmpty()) "Add residents and staff so the gate recognises them." else "Try another name, flat or filter.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.plate }) { v ->
                        VehicleCard(v, onBlock = { vm.setBlacklisted(v.plate, it) }, onDelete = { vm.delete(v.plate) })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Add vehicle") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Space.gutter),
        )
    }
}

@Composable
private fun VehicleCard(v: VehicleEntity, onBlock: (Boolean) -> Unit, onDelete: () -> Unit) {
    val c = EdgeTheme.colors
    val category = runCatching { VehicleCategory.valueOf(v.category) }.getOrDefault(VehicleCategory.UNKNOWN)
    var menu by remember { mutableStateOf(false) }
    val (avBg, avFg) = when {
        v.blacklisted -> c.denyWash to c.denyInk
        category == VehicleCategory.STAFF -> c.staffWash to c.staffInk
        category == VehicleCategory.VISITOR -> c.visitorWash to c.visitorInk
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val initials = v.ownerName?.split(" ")?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.first().uppercase() }
        ?.ifEmpty { null } ?: "–"

    EdgeCard(Modifier.fillMaxWidth(), borderColor = if (v.blacklisted) c.denyLine else c.cardLine) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(avBg), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.titleSmall, color = avFg)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(v.display, style = EdgeText.plateMedium.copy(fontSize = EdgeText.plateMedium.fontSize * 0.94f))
                Text(
                    listOfNotNull(v.ownerName, v.unit, v.phone).joinToString(" · ").ifEmpty { "No owner details" },
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1,
                )
                CategoryBadge(category, v.blacklisted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Block", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                Switch(
                    checked = v.blacklisted,
                    onCheckedChange = onBlock,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.deny, checkedThumbColor = Color.White),
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More actions for ${v.display}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Delete vehicle") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
