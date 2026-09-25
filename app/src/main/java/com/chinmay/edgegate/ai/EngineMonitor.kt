package com.chinmay.edgegate.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Live health of the on-device AI engine, shared by the Scan HUD and the Dashboard card. */
data class EngineStatus(
    val cameraLive: Boolean = false,
    val runtimeLabel: String = "Not started",
    val usesDetector: Boolean = false,
    val usesGpu: Boolean = false,
    val detectP50: Double = 0.0,
    val ocrP50: Double = 0.0,
    val frameP50: Double = 0.0,
    val frameP90: Double = 0.0,
    val fps: Double = 0.0,
)

class EngineMonitor {
    private val _status = MutableStateFlow(EngineStatus())
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    fun update(transform: (EngineStatus) -> EngineStatus) = _status.update(transform)
}
