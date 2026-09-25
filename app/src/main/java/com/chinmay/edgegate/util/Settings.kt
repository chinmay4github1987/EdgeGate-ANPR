package com.chinmay.edgegate.util

import android.content.Context
import com.chinmay.edgegate.core.GateMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Every user-changeable setting, as one immutable snapshot for the UI. */
data class GateSettings(
    val gateName: String = "Main Gate",
    val gateMode: GateMode = GateMode.ENTRY_ONLY,
    val preferGpu: Boolean = true,
    val votingFrames: Int = 3,
    val cooldownSec: Int = 60,
    val alarmOnDeny: Boolean = true,
    val beepOnAllow: Boolean = true,
    val supervisorPhone: String = "",
    val retentionDays: Int = 90,
)

/**
 * Persisted settings (SharedPreferences) exposed as a StateFlow, so every
 * screen updates the moment something changes. Survives the gate phone rebooting.
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("edgegate", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GateSettings> = _state.asStateFlow()
    val current: GateSettings get() = _state.value

    fun update(transform: (GateSettings) -> GateSettings) {
        val next = transform(_state.value)
        prefs.edit()
            .putString("gate_name", next.gateName)
            .putString("gate_mode", next.gateMode.name)
            .putBoolean("prefer_gpu", next.preferGpu)
            .putInt("voting_frames", next.votingFrames)
            .putInt("cooldown_sec", next.cooldownSec)
            .putBoolean("alarm_on_deny", next.alarmOnDeny)
            .putBoolean("beep_on_allow", next.beepOnAllow)
            .putString("supervisor_phone", next.supervisorPhone)
            .putInt("retention_days", next.retentionDays)
            .apply()
        _state.value = next
    }

    private fun load(): GateSettings {
        val d = GateSettings()
        return GateSettings(
            gateName = prefs.getString("gate_name", null) ?: d.gateName,
            gateMode = runCatching { GateMode.valueOf(prefs.getString("gate_mode", null)!!) }.getOrDefault(d.gateMode),
            preferGpu = prefs.getBoolean("prefer_gpu", d.preferGpu),
            votingFrames = prefs.getInt("voting_frames", d.votingFrames),
            cooldownSec = prefs.getInt("cooldown_sec", d.cooldownSec),
            alarmOnDeny = prefs.getBoolean("alarm_on_deny", d.alarmOnDeny),
            beepOnAllow = prefs.getBoolean("beep_on_allow", d.beepOnAllow),
            supervisorPhone = prefs.getString("supervisor_phone", null) ?: d.supervisorPhone,
            retentionDays = prefs.getInt("retention_days", d.retentionDays),
        )
    }
}
