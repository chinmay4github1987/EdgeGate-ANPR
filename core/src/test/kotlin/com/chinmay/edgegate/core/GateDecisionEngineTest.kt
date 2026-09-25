package com.chinmay.edgegate.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GateDecisionEngineTest {

    private val resident = VehicleProfile("KA01AB1234", VehicleCategory.RESIDENT, false, "Asha", "B-402")
    private val banned = VehicleProfile("KA02ZZ0001", VehicleCategory.UNKNOWN, true)

    @Test fun residentIsAllowed() {
        val d = GateDecisionEngine().decide(resident.plate, 0, GateMode.ENTRY_ONLY, resident, null)
        assertEquals(GateAction.ALLOW, d.action)
        assertEquals(Direction.ENTRY, d.direction)
        assertEquals("Resident · Asha · B-402", d.message)
    }

    @Test fun blacklistBeatsEverything() {
        val d = GateDecisionEngine().decide(banned.plate, 0, GateMode.ENTRY_ONLY, banned, null)
        assertEquals(GateAction.DENY_BLACKLISTED, d.action)
    }

    @Test fun unknownVehicleIsLoggedAsVisitor() {
        val d = GateDecisionEngine().decide("TN09CK4321", 0, GateMode.ENTRY_ONLY, null, null)
        assertEquals(GateAction.ALLOW_LOG_VISITOR, d.action)
        assertEquals(VehicleCategory.UNKNOWN, d.category)
    }

    @Test fun autoModeInfersDirectionFromLastEvent() {
        val e = GateDecisionEngine()
        assertEquals(Direction.EXIT, e.decide("A", 0, GateMode.AUTO, null, Direction.ENTRY).direction)
        assertEquals(Direction.ENTRY, e.decide("B", 0, GateMode.AUTO, null, Direction.EXIT).direction)
        assertEquals(Direction.ENTRY, e.decide("C", 0, GateMode.AUTO, null, null).direction)
    }

    @Test fun duplicateWithinCooldownIsIgnored() {
        val e = GateDecisionEngine(cooldownMs = 60_000)
        e.decide(resident.plate, 0, GateMode.ENTRY_ONLY, resident, null)
        assertEquals(GateAction.DUPLICATE, e.decide(resident.plate, 30_000, GateMode.ENTRY_ONLY, resident, null).action)
        assertEquals(GateAction.ALLOW, e.decide(resident.plate, 61_000, GateMode.ENTRY_ONLY, resident, null).action)
    }
}
