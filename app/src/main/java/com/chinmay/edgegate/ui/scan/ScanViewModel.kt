package com.chinmay.edgegate.ui.scan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.edgegate.AppContainer
import com.chinmay.edgegate.ai.FrameResult
import com.chinmay.edgegate.core.Box
import com.chinmay.edgegate.core.GateAction
import com.chinmay.edgegate.core.GateDecision
import com.chinmay.edgegate.core.GateMode
import com.chinmay.edgegate.core.PlateNormalizer
import com.chinmay.edgegate.core.RollingStats
import com.chinmay.edgegate.core.VehicleProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DecisionUi(
    val decision: GateDecision,
    val display: String,
    val confidence: Float,
    val source: String,
    val profile: VehicleProfile?,
)

data class ScanUiState(
    val mode: GateMode = GateMode.ENTRY_ONLY,
    val liveRead: String? = null,
    val liveConfidence: Float = 0f,
    val plateBox: Box? = null,
    val lastDecision: DecisionUi? = null,
    val paused: Boolean = false,
    val torchOn: Boolean = false,
    val supervisorPhone: String = "",
)

class ScanViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository
    private val engine = container.decisionEngine
    private val monitor = container.engineMonitor

    private val _state = MutableStateFlow(
        ScanUiState(mode = container.settings.current.gateMode, supervisorPhone = container.settings.current.supervisorPhone)
    )
    val state: StateFlow<ScanUiState> = _state.asStateFlow()
    val engineStatus = monitor.status

    private val detectStats = RollingStats()
    private val ocrStats = RollingStats()
    private val totalStats = RollingStats()
    private val frameIntervals = RollingStats()
    private var lastFrameAt = 0L

    /** Read from the camera thread; @Volatile so it sees UI changes immediately. */
    @Volatile var paused: Boolean = false
        private set

    init {
        // Reflect changes made on the Settings screen (default mode, supervisor phone).
        viewModelScope.launch {
            container.settings.state.collect { s ->
                _state.update { it.copy(mode = s.gateMode, supervisorPhone = s.supervisorPhone) }
            }
        }
    }

    fun createPipeline() = container.createPipeline()

    fun onCameraStarted() = monitor.update { it.copy(cameraLive = true) }
    fun onCameraStopped() = monitor.update { it.copy(cameraLive = false) }

    /** Called on the camera analysis thread for every processed frame. */
    fun onFrame(result: FrameResult) {
        val now = SystemClock.elapsedRealtime()
        if (lastFrameAt != 0L) frameIntervals.add((now - lastFrameAt).toDouble())
        lastFrameAt = now
        if (result.detectMs > 0) detectStats.add(result.detectMs)
        if (result.ocrMs > 0) ocrStats.add(result.ocrMs)
        totalStats.add(result.totalMs)

        val interval = frameIntervals.mean()
        monitor.update {
            it.copy(
                cameraLive = true,
                runtimeLabel = result.runtimeLabel,
                usesDetector = result.usesDetector,
                usesGpu = result.usesGpu,
                detectP50 = detectStats.percentile(50.0),
                ocrP50 = ocrStats.percentile(50.0),
                frameP50 = totalStats.percentile(50.0),
                frameP90 = totalStats.percentile(90.0),
                fps = if (interval > 0) 1000.0 / interval else 0.0,
            )
        }
        _state.update {
            it.copy(liveRead = result.candidate?.display, liveConfidence = result.confidence, plateBox = result.plateBox)
        }

        result.consensus?.let { c ->
            val display = PlateNormalizer.parse(c.plate)?.display ?: c.plate
            viewModelScope.launch { confirm(c.plate, display, c.avgConfidence, "AI", result.totalMs) }
        }
    }

    /** Human-in-the-loop fallback: guard types the plate when the camera can't read it. */
    fun manualEntry(raw: String): Boolean {
        val candidate = PlateNormalizer.parse(raw) ?: return false
        engine.clearCooldown(candidate.plate)
        viewModelScope.launch { confirm(candidate.plate, candidate.display, 1f, "MANUAL", null) }
        return true
    }

    private suspend fun confirm(plate: String, display: String, confidence: Float, source: String, latencyMs: Double?) {
        val profile = repo.profile(plate)
        val decision = engine.decide(
            plate = plate,
            nowMs = System.currentTimeMillis(),
            mode = _state.value.mode,
            profile = profile,
            lastDirection = repo.lastDirection(plate),
        )
        if (decision.action == GateAction.DUPLICATE) return
        repo.record(decision, display, confidence, source, latencyMs)
        if (decision.action == GateAction.DENY_BLACKLISTED) container.alerter.alarm() else container.alerter.ok()
        _state.update { it.copy(lastDecision = DecisionUi(decision, display, confidence, source, profile)) }
    }

    fun setMode(mode: GateMode) {
        container.settings.update { it.copy(gateMode = mode) }
        _state.update { it.copy(mode = mode) }
    }

    fun togglePause() {
        paused = !paused
        _state.update { it.copy(paused = paused, liveRead = null, plateBox = null) }
    }

    fun setTorch(on: Boolean) = _state.update { it.copy(torchOn = on) }

    fun silenceAlarm() = container.alerter.silence()

    /** "Next vehicle": clear the decision sheet and go back to scanning. */
    fun dismissDecision() {
        container.alerter.silence()
        _state.update { it.copy(lastDecision = null) }
    }

    override fun onCleared() {
        monitor.update { it.copy(cameraLive = false) }
    }
}
