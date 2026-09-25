package com.chinmay.edgegate.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GateStatsTest {
    private val ist = 19_800_000L // +05:30
    // A whole number of days since the epoch, shifted back by the offset = local (IST) midnight.
    private val midnightIst = 20_721L * 86_400_000L - ist

    private fun at(hour: Int, min: Int = 0) = midnightIst + hour * 3_600_000L + min * 60_000L

    @Test fun countsEntriesExitsVisitorsAndAlerts() {
        val events = listOf(
            EventLite(at(9, 5), Direction.ENTRY, GateAction.ALLOW, VehicleCategory.RESIDENT),
            EventLite(at(9, 40), Direction.ENTRY, GateAction.ALLOW_LOG_VISITOR, VehicleCategory.UNKNOWN),
            EventLite(at(10), Direction.EXIT, GateAction.ALLOW, VehicleCategory.RESIDENT),
            EventLite(at(11), Direction.ENTRY, GateAction.DENY_BLACKLISTED, VehicleCategory.UNKNOWN),
            EventLite(at(0) - 1, Direction.ENTRY, GateAction.ALLOW, VehicleCategory.RESIDENT), // yesterday
        )
        val s = GateStats.summarize(events, midnightIst, ist)
        assertEquals(2, s.entries)
        assertEquals(1, s.exits)
        assertEquals(1, s.visitors)
        assertEquals(1, s.alerts)
        assertEquals(at(11), s.lastAlertAtMs)
        assertEquals(2, s.hourly[9])
        assertEquals(1, s.hourly[10])
        assertEquals(0, s.hourly[11]) // denied vehicles did not move
        assertEquals(9, s.peakHour)
        assertEquals(3, s.total)
    }

    @Test fun emptyDayHasNoPeak() {
        val s = GateStats.summarize(emptyList(), midnightIst, ist)
        assertNull(s.peakHour)
        assertNull(s.lastAlertAtMs)
    }

    @Test fun stateNames() {
        assertEquals("Karnataka", RtoStates.describe(PlateNormalizer.parse("KA01AB1234")!!))
        assertEquals("Bharat series", RtoStates.describe(PlateNormalizer.parse("22BH1234AA")!!))
    }
}
