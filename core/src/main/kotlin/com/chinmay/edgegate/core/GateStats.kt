package com.chinmay.edgegate.core

/** The minimum an event needs for dashboard maths (mirrors the Room row). */
data class EventLite(
    val timestampMs: Long,
    val direction: Direction,
    val action: GateAction,
    val category: VehicleCategory,
)

/** Everything the dashboard's KPI cards and hourly chart show for one day. */
data class DaySummary(
    val entries: Int,
    val exits: Int,
    val visitors: Int,
    val alerts: Int,
    val lastAlertAtMs: Long?,
    /** 24 buckets, index = hour of day (local), value = vehicles that moved (entries + exits). */
    val hourly: List<Int>,
) {
    val total: Int get() = entries + exits
    val peakHour: Int? get() = hourly.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
}

object GateStats {
    /**
     * @param startOfDayMs local midnight in epoch ms; events before it are ignored.
     * @param zoneOffsetMs offset of local time from UTC for the hour buckets (e.g. +5:30 = 19_800_000).
     */
    fun summarize(events: List<EventLite>, startOfDayMs: Long, zoneOffsetMs: Long): DaySummary {
        val hourly = IntArray(24)
        var entries = 0
        var exits = 0
        var visitors = 0
        var alerts = 0
        var lastAlert: Long? = null
        for (e in events) {
            if (e.timestampMs < startOfDayMs) continue
            if (e.action == GateAction.DENY_BLACKLISTED) {
                alerts++
                if (lastAlert == null || e.timestampMs > lastAlert) lastAlert = e.timestampMs
                continue // a denied vehicle did not move through the gate
            }
            if (e.action == GateAction.DUPLICATE) continue
            when (e.direction) {
                Direction.ENTRY -> entries++
                Direction.EXIT -> exits++
            }
            if (e.action == GateAction.ALLOW_LOG_VISITOR && e.direction == Direction.ENTRY) visitors++
            val hour = (((e.timestampMs + zoneOffsetMs) / 3_600_000L) % 24).toInt()
            hourly[hour]++
        }
        return DaySummary(entries, exits, visitors, alerts, lastAlert, hourly.toList())
    }
}
