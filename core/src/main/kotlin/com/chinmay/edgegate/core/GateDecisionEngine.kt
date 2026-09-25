package com.chinmay.edgegate.core

/**
 * Business rules for a society / campus / parking gate. Pure logic, no I/O.
 *
 * Input : a plate the AI has *confirmed* (after temporal voting), the registry
 *         profile for that plate (or null if unregistered) and the vehicle's
 *         last logged direction (or null if never seen).
 * Output: what the guard should do and which direction to log.
 *
 * Keeping this separate from the AI means the rules can change (e.g. "visitors
 * need approval after 10 pm") without touching or re-validating the models.
 */
class GateDecisionEngine(
    /** Mutable so the Settings screen can change it without restarting. */
    @Volatile var cooldownMs: Long = 60_000,
) {

    private val lastDecisionAt = HashMap<String, Long>()

    fun decide(
        plate: String,
        nowMs: Long,
        mode: GateMode,
        profile: VehicleProfile?,
        lastDirection: Direction?,
    ): GateDecision {
        val category = profile?.category ?: VehicleCategory.UNKNOWN
        val direction = when (mode) {
            GateMode.ENTRY_ONLY -> Direction.ENTRY
            GateMode.EXIT_ONLY -> Direction.EXIT
            GateMode.AUTO -> if (lastDirection == Direction.ENTRY) Direction.EXIT else Direction.ENTRY
        }

        val previous = lastDecisionAt[plate]
        if (previous != null && nowMs - previous < cooldownMs) {
            return GateDecision(plate, GateAction.DUPLICATE, direction, category, nowMs,
                "Already processed ${(nowMs - previous) / 1000}s ago")
        }
        lastDecisionAt[plate] = nowMs
        prune(nowMs)

        val (action, message) = when {
            profile?.blacklisted == true ->
                GateAction.DENY_BLACKLISTED to "BLACKLISTED – do not open. Inform security."
            category == VehicleCategory.RESIDENT || category == VehicleCategory.STAFF ->
                GateAction.ALLOW to listOfNotNull(
                    category.name.lowercase().replaceFirstChar { it.uppercase() },
                    profile?.ownerName,
                    profile?.unit,
                ).joinToString(" · ")
            category == VehicleCategory.VISITOR ->
                GateAction.ALLOW_LOG_VISITOR to "Pre-registered visitor${profile?.unit?.let { " for $it" } ?: ""}"
            else ->
                GateAction.ALLOW_LOG_VISITOR to "Unregistered vehicle – ask purpose / flat number"
        }
        return GateDecision(plate, action, direction, category, nowMs, message)
    }

    /** Forget a plate's cooldown (e.g. guard manually re-scans). */
    fun clearCooldown(plate: String) {
        lastDecisionAt.remove(plate)
    }

    private fun prune(nowMs: Long) {
        if (lastDecisionAt.size < 512) return
        lastDecisionAt.entries.removeAll { nowMs - it.value > cooldownMs }
    }
}
