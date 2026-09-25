package com.chinmay.edgegate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinmay.edgegate.core.GateAction
import com.chinmay.edgegate.core.VehicleCategory
import com.chinmay.edgegate.ui.theme.EdgeText
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Palette
import com.chinmay.edgegate.ui.theme.Space

// ---------------------------------------------------------------- Plate chip

enum class PlateSize(val border: Dp, val strip: Dp, val radius: Dp, val padH: Dp, val padV: Dp) {
    Small(1.5.dp, 10.dp, 5.dp, 8.dp, 3.dp),
    Medium(2.dp, 12.dp, 6.dp, 10.dp, 4.dp),
    Large(2.5.dp, 18.dp, 8.dp, 16.dp, 8.dp),
}

/**
 * A number plate drawn like an Indian high-security plate (white, black border, blue IND strip),
 * so a guard recognises it at a glance. Always light, even in dark theme, like a real plate.
 */
@Composable
fun PlateChip(text: String, size: PlateSize = PlateSize.Medium, modifier: Modifier = Modifier) {
    val style = when (size) {
        PlateSize.Small -> EdgeText.plateSmall
        PlateSize.Medium -> EdgeText.plateMedium
        PlateSize.Large -> EdgeText.plateLarge
    }
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(size.radius))
            .background(Color.White)
            .border(size.border, Palette.Ink, RoundedCornerShape(size.radius))
            .semantics { contentDescription = "Plate $text" },
    ) {
        Box(
            Modifier.width(size.strip).fillMaxHeight().background(Palette.PlateStrip),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (size == PlateSize.Large) {
                Text("IND", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        Text(text, style = style, color = Palette.Ink, modifier = Modifier.padding(horizontal = size.padH, vertical = size.padV))
    }
}

// ---------------------------------------------------------------- Badges

@Composable
fun Badge(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = foreground,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(background).padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Visual language of one gate decision: colours, icon and a word. */
data class DecisionLook(val label: String, val solid: Color, val ink: Color, val wash: Color, val icon: ImageVector)

@Composable
fun decisionLook(action: GateAction): DecisionLook {
    val c = EdgeTheme.colors
    return when (action) {
        GateAction.ALLOW -> DecisionLook("Allow", c.allow, c.allowInk, c.allowWash, Icons.Filled.Check)
        GateAction.ALLOW_LOG_VISITOR -> DecisionLook("Visitor", c.visitor, c.visitorInk, c.visitorWash, Icons.Filled.PersonOutline)
        GateAction.DENY_BLACKLISTED -> DecisionLook("Deny", c.deny, c.denyInk, c.denyWash, Icons.Filled.Close)
        GateAction.DUPLICATE -> DecisionLook("Repeat", c.textMuted, c.textMuted, c.divider, Icons.Filled.Check)
    }
}

@Composable
fun ActionBadge(action: GateAction, modifier: Modifier = Modifier) {
    val look = decisionLook(action)
    Badge(look.label, look.wash, look.ink, modifier)
}

@Composable
fun CategoryBadge(category: VehicleCategory, blacklisted: Boolean, modifier: Modifier = Modifier) {
    val c = EdgeTheme.colors
    when {
        blacklisted -> Badge("Blocked", c.denyWash, c.denyInk, modifier)
        category == VehicleCategory.RESIDENT -> Badge("Resident", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, modifier)
        category == VehicleCategory.STAFF -> Badge("Staff", c.staffWash, c.staffInk, modifier)
        category == VehicleCategory.VISITOR -> Badge("Visitor", c.visitorWash, c.visitorInk, modifier)
        else -> Badge("Unknown", c.divider, c.textMuted, modifier)
    }
}

@Composable
fun IconDisc(icon: ImageVector, background: Color, tint: Color, size: Dp = 36.dp, contentDescription: String? = null) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

// ---------------------------------------------------------------- Layout

@Composable
fun EdgeCard(
    modifier: Modifier = Modifier,
    color: Color = EdgeTheme.colors.card,
    borderColor: Color = EdgeTheme.colors.cardLine,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = BorderStroke(1.dp, borderColor),
    ) { Column(content = content) }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    overline: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.gutter, bottom = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overline?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = EdgeTheme.colors.textMuted) }
            Text(title, style = MaterialTheme.typography.headlineMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = EdgeTheme.colors.textMuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = EdgeText.overline, color = EdgeTheme.colors.textMuted, modifier = modifier)
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = EdgeTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Space.touch)
            .clip(MaterialTheme.shapes.small)
            .background(c.card)
            .border(1.dp, c.cardLine, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Search, null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
    }
}

/** Horizontally scrolling single-select pills (filters). */
@Composable
fun <T> FilterPills(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) MaterialTheme.colorScheme.secondaryContainer else EdgeTheme.colors.card)
                    .border(1.dp, if (on) Color.Transparent else EdgeTheme.colors.cardLine, RoundedCornerShape(18.dp))
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(value) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium),
                    color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        IconDisc(icon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, size = 56.dp)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = EdgeTheme.colors.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Status pill used on the dashboard ("Camera live", "On-device · LiteRT GPU"). */
@Composable
fun StatusPill(text: String, dotColor: Color? = null, highlighted: Boolean = false) {
    val c = EdgeTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlighted) c.allowWash else c.card)
            .border(1.dp, if (highlighted) Color.Transparent else c.cardLine, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        dotColor?.let { Box(Modifier.size(8.dp).clip(CircleShape).background(it)) }
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = if (highlighted) c.allowInk else MaterialTheme.colorScheme.onSurface)
    }
}
