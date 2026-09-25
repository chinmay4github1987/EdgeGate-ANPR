package com.chinmay.edgegate.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.edgegate.ai.EngineStatus
import com.chinmay.edgegate.camera.CameraPreview
import com.chinmay.edgegate.camera.PlateAnalyzer
import com.chinmay.edgegate.core.GateAction
import com.chinmay.edgegate.core.GateMode
import com.chinmay.edgegate.core.PlateNormalizer
import com.chinmay.edgegate.ui.components.IconDisc
import com.chinmay.edgegate.ui.components.PlateChip
import com.chinmay.edgegate.ui.components.PlateSize
import com.chinmay.edgegate.ui.components.decisionLook
import com.chinmay.edgegate.ui.theme.EdgeText
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.theme.Palette
import com.chinmay.edgegate.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OverlayBg = Color(0xE00B1220)
private val OnNight = Color(0xFFF1F5F9)

@Composable
fun ScanScreen(vm: ScanViewModel) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    if (!hasPermission) {
        PermissionScreen(onAllow = { launcher.launch(Manifest.permission.CAMERA) })
        return
    }

    val state by vm.state.collectAsStateWithLifecycle()
    val engine by vm.engineStatus.collectAsStateWithLifecycle()
    var camera by remember { mutableStateOf<Camera?>(null) }
    var showManual by remember { mutableStateOf(false) }
    val denied = state.lastDecision?.decision?.action == GateAction.DENY_BLACKLISTED

    LaunchedEffect(camera, state.torchOn) { camera?.cameraControl?.enableTorch(state.torchOn) }

    Box(Modifier.fillMaxSize().background(Palette.Night)) {
        // ---- Camera + overlays (3:4 box so detector coordinates map 1:1)
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).align(Alignment.TopCenter)) {
            val analyzerFactory = remember(vm) {
                { PlateAnalyzer(vm::createPipeline, { vm.paused }, vm::onFrame) }
            }
            CameraPreview(
                analyzerFactory = analyzerFactory,
                modifier = Modifier.fillMaxSize(),
                onCamera = { cam ->
                    camera = cam
                    if (cam != null) vm.onCameraStarted() else vm.onCameraStopped()
                },
            )
            Viewfinder(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 90.dp))
            PlateOverlay(state)
            if (denied) Box(Modifier.fillMaxSize().border(6.dp, EdgeTheme.colors.deny))
        }

        // ---- Top controls
        Column(Modifier.statusBarsPadding().padding(Space.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ModeSegmented(state.mode, vm::setMode)
                Spacer(Modifier.weight(1f))
                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    RoundIconButton(
                        icon = if (state.torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        label = if (state.torchOn) "Turn torch off" else "Turn torch on",
                        onClick = { vm.setTorch(!state.torchOn) },
                    )
                }
            }
            EngineHud(engine, state.paused)
            if (denied) AlarmBanner()
        }

        // ---- Bottom sheet
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AnimatedContent(
                targetState = state.lastDecision,
                transitionSpec = { (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut() },
                label = "decision",
            ) { decision ->
                if (decision == null) {
                    IdleSheet(state, onManual = { showManual = true }, onPause = vm::togglePause)
                } else {
                    DecisionSheet(
                        ui = decision,
                        onManual = { showManual = true },
                        onNext = vm::dismissDecision,
                        onSilence = vm::silenceAlarm,
                        onCallSupervisor = {
                            val uri = Uri.parse("tel:" + state.supervisorPhone)
                            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                        },
                    )
                }
            }
        }
    }

    if (showManual) ManualEntryDialog(onDismiss = { showManual = false }, onSubmit = vm::manualEntry)
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

// ------------------------------------------------------------------ Overlays

@Composable
private fun Viewfinder(modifier: Modifier) {
    Canvas(modifier) {
        val len = 28.dp.toPx()
        val w = 3.dp.toPx()
        val c = OnNight
        val r = size
        // four corner brackets
        drawLine(c, Offset(0f, 0f), Offset(len, 0f), w, StrokeCap.Round); drawLine(c, Offset(0f, 0f), Offset(0f, len), w, StrokeCap.Round)
        drawLine(c, Offset(r.width, 0f), Offset(r.width - len, 0f), w, StrokeCap.Round); drawLine(c, Offset(r.width, 0f), Offset(r.width, len), w, StrokeCap.Round)
        drawLine(c, Offset(0f, r.height), Offset(len, r.height), w, StrokeCap.Round); drawLine(c, Offset(0f, r.height), Offset(0f, r.height - len), w, StrokeCap.Round)
        drawLine(c, Offset(r.width, r.height), Offset(r.width - len, r.height), w, StrokeCap.Round); drawLine(c, Offset(r.width, r.height), Offset(r.width, r.height - len), w, StrokeCap.Round)
    }
}

@Composable
private fun PlateOverlay(state: ScanUiState) {
    val box = state.plateBox ?: return
    val read = state.liveRead
    val color = if (read != null) Color(0xFF22C55E) else Color(0xFFFACC15)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val left = maxWidth * box.left
        val top = maxHeight * box.top
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = color,
                topLeft = Offset(box.left * size.width, box.top * size.height),
                size = Size(box.width * size.width, box.height * size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        if (read != null) {
            Row(
                Modifier.offset(x = left, y = (top - 40.dp).coerceAtLeast(0.dp))
                    .clip(RoundedCornerShape(8.dp)).background(OverlayBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(read, style = EdgeText.plateMedium, color = Color.White)
                Text("${(state.liveConfidence * 100).toInt()}%", style = EdgeText.mono, color = Color(0xFF86EFAC))
            }
        }
    }
}

@Composable
private fun ModeSegmented(mode: GateMode, onSelect: (GateMode) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(OverlayBg)
            .border(1.dp, Palette.NightLine, RoundedCornerShape(999.dp)).padding(4.dp)
            .selectableGroup(),
    ) {
        listOf(GateMode.ENTRY_ONLY to "Entry", GateMode.EXIT_ONLY to "Exit", GateMode.AUTO to "Auto").forEach { (m, label) ->
            val on = m == mode
            Box(
                Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (on) OnNight else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(m) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Palette.Ink else Color(0xFFE2E8F0))
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = OverlayBg,
        border = BorderStroke(1.dp, Palette.NightLine),
        modifier = Modifier.size(Space.touch),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = OnNight) }
    }
}

@Composable
private fun EngineHud(engine: EngineStatus, paused: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(OverlayBg).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val style = EdgeText.mono.copy(fontSize = EdgeText.mono.fontSize * 0.92f)
        Text(
            if (paused) "● Paused" else "● " + engine.runtimeLabel.substringBefore(" + "),
            style = style,
            color = if (paused) Color(0xFFFACC15) else Color(0xFF86EFAC),
        )
        if (engine.usesDetector) Text("det %.0fms".format(engine.detectP50), style = style, color = Color(0xFFCBD5E1))
        Text("ocr %.0fms".format(engine.ocrP50), style = style, color = Color(0xFFCBD5E1))
        Text("%.0f fps".format(engine.fps), style = style, color = Color(0xFFCBD5E1))
    }
}

@Composable
private fun AlarmBanner() {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Palette.Deny).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, null, tint = Color.White)
        Text("Alarm sounding · blacklisted plate", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

// ------------------------------------------------------------------ Sheets

@Composable
private fun SheetSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.md, bottom = Space.lg), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(EdgeTheme.colors.cardLine))
            content()
        }
    }
}

@Composable
private fun IdleSheet(state: ScanUiState, onManual: () -> Unit, onPause: () -> Unit) {
    SheetSurface {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconDisc(Icons.Filled.PhotoCamera, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            Column(Modifier.weight(1f)) {
                Text(if (state.paused) "Scanning paused" else "Ready to scan", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        state.paused -> "Tap resume when the next vehicle arrives"
                        state.liveRead != null -> "Reading ${state.liveRead} · holding for confirmation"
                        else -> "Point the camera at the number plate, 2–5 m away"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = EdgeTheme.colors.textMuted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryAction("Type plate", Icons.Filled.Keyboard, onManual, Modifier.weight(1f))
            SecondaryAction(if (state.paused) "Resume" else "Pause", if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, onPause, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DecisionSheet(
    ui: DecisionUi,
    onManual: () -> Unit,
    onNext: () -> Unit,
    onSilence: () -> Unit,
    onCallSupervisor: () -> Unit,
) {
    val d = ui.decision
    val look = decisionLook(d.action)
    val time = remember(d.timestampMs) { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(d.timestampMs)) }
    val denied = d.action == GateAction.DENY_BLACKLISTED
    val direction = d.direction.name.lowercase().replaceFirstChar { it.uppercase() }

    SheetSurface {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconDisc(look.icon, look.solid, Color.White, contentDescription = look.label)
            Column {
                Text(
                    when (d.action) {
                        GateAction.ALLOW -> "Allow · $direction"
                        GateAction.ALLOW_LOG_VISITOR -> "Visitor · $direction logged"
                        GateAction.DENY_BLACKLISTED -> "Deny · Do not open"
                        GateAction.DUPLICATE -> "Already processed"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = look.ink,
                )
                Text("${d.message} · $time", style = MaterialTheme.typography.bodyMedium, color = EdgeTheme.colors.textMuted)
            }
        }
        PlateChip(ui.display, PlateSize.Large)
        if (denied) {
            Text(
                "Keep the barrier closed and inform the security supervisor.",
                style = MaterialTheme.typography.bodyMedium,
                color = EdgeTheme.colors.denyInk,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(EdgeTheme.colors.denyWash).padding(12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryAction("Silence alarm", Icons.AutoMirrored.Filled.VolumeOff, onSilence, Modifier.weight(1f))
                Button(
                    onClick = onCallSupervisor,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Deny, contentColor = Color.White),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).height(Space.touch),
                ) { Icon(Icons.Filled.Phone, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Call supervisor") }
            }
            TextButton(onClick = onNext, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Done · next vehicle") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Detail("Owner", ui.profile?.ownerName ?: "Not registered", Modifier.weight(1f))
                Detail("Flat / unit", ui.profile?.unit ?: "Ask visitor", Modifier.weight(1f))
                Detail("Confidence", "${(ui.confidence * 100).toInt()}% · ${ui.source}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryAction("Type plate", Icons.Filled.Keyboard, onManual, Modifier.weight(1f))
                Button(
                    onClick = onNext,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).height(Space.touch),
                ) { Text("Next vehicle") }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = EdgeTheme.colors.textMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

@Composable
private fun SecondaryAction(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, EdgeTheme.colors.cardLine),
        modifier = modifier.height(Space.touch),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ------------------------------------------------------------------ Dialogs & permission

@Composable
private fun ManualEntryDialog(onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val preview = remember(text) { PlateNormalizer.parse(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Type plate number") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.uppercase(); error = null },
                    placeholder = { Text("KA 01 AB 1234") },
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    singleLine = true,
                    textStyle = EdgeText.plateMedium,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                preview?.let { PlateChip(it.display, PlateSize.Medium) }
            }
        },
        confirmButton = {
            Button(onClick = { if (onSubmit(text)) onDismiss() else error = "Not a valid Indian registration number" }, shape = MaterialTheme.shapes.small) { Text("Log vehicle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(Space.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconDisc(Icons.Filled.PhotoCamera, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, size = 72.dp)
        Spacer(Modifier.height(Space.xl))
        Text("Let EdgeGate use the camera", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Space.sm))
        Text(
            "The camera reads number plates at the gate so the guard doesn't have to write them down.",
            style = MaterialTheme.typography.bodyLarge,
            color = EdgeTheme.colors.textMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(Space.lg))
        Row(
            Modifier.clip(MaterialTheme.shapes.small).background(EdgeTheme.colors.card).border(1.dp, EdgeTheme.colors.cardLine, MaterialTheme.shapes.small).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
            Text("Video is processed on this phone and never stored or uploaded.", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(Space.xl))
        Button(onClick = onAllow, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Allow camera") }
    }
}
