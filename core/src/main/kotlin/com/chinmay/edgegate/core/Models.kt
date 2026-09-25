package com.chinmay.edgegate.core

/** Who a vehicle belongs to. Drives what the guard sees and what gets logged. */
enum class VehicleCategory { RESIDENT, STAFF, VISITOR, UNKNOWN }

/** Physical direction of travel through the gate. */
enum class Direction { ENTRY, EXIT }

/**
 * How this phone/camera is mounted.
 * ENTRY_ONLY / EXIT_ONLY  -> one device per lane (most reliable).
 * AUTO                    -> a single device at a shared gate infers direction
 *                            from the vehicle's last known state (inside -> EXIT).
 */
enum class GateMode { ENTRY_ONLY, EXIT_ONLY, AUTO }

/** What the app tells the guard to do. */
enum class GateAction {
    /** Registered resident/staff vehicle – open the boom. */
    ALLOW,
    /** Unknown or visitor vehicle – allowed but logged; guard may ask for purpose / flat no. */
    ALLOW_LOG_VISITOR,
    /** Blacklisted plate – do not open, raise alert. */
    DENY_BLACKLISTED,
    /** Same plate confirmed again inside the cooldown window – ignored (vehicle still at the gate). */
    DUPLICATE,
}

/** Registry record the decision engine needs; mirrors the Room entity in :app. */
data class VehicleProfile(
    val plate: String,
    val category: VehicleCategory,
    val blacklisted: Boolean,
    val ownerName: String? = null,
    val unit: String? = null,
)

data class GateDecision(
    val plate: String,
    val action: GateAction,
    val direction: Direction,
    val category: VehicleCategory,
    val timestampMs: Long,
    val message: String,
)

/** Axis-aligned box in normalised [0,1] image coordinates. */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = (right - left).coerceAtLeast(0f)
    val height get() = (bottom - top).coerceAtLeast(0f)
    val area get() = width * height

    fun iou(other: Box): Float {
        val l = maxOf(left, other.left)
        val t = maxOf(top, other.top)
        val r = minOf(right, other.right)
        val b = minOf(bottom, other.bottom)
        val inter = (r - l).coerceAtLeast(0f) * (b - t).coerceAtLeast(0f)
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }
}

data class Detection(val box: Box, val score: Float)
