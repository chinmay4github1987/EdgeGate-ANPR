package com.chinmay.edgegate.core

/** A plate that several consecutive frames agreed on. */
data class Consensus(val plate: String, val votes: Int, val avgConfidence: Float)

/**
 * Multi-frame consensus ("temporal voting").
 *
 * A single frame can be blurred, glared or half-occluded, so a single OCR read
 * is not trustworthy. The camera, however, gives us ~10-30 frames per second
 * for free. We only confirm a plate when it has been read at least [minVotes]
 * times within [windowMs] and holds at least [minShare] of all reads in that
 * window (confidence-weighted). This removes most false reads without a larger
 * model – accuracy is bought with time instead of compute.
 *
 * After confirming, the same plate is not emitted again while it stays in view;
 * it re-arms once it has been unseen for [rearmMs].
 *
 * Not thread-safe: call from the single camera-analysis thread.
 */
class TemporalVoter(
    private val windowMs: Long = 2_000,
    private val minVotes: Int = 3,
    private val minShare: Float = 0.6f,
    private val rearmMs: Long = 3_000,
) {
    private data class Read(val plate: String, val confidence: Float, val t: Long)

    private val reads = ArrayDeque<Read>()
    private var lastEmitted: String? = null
    private var lastEmittedSeenAt = 0L

    /**
     * Feed one frame's result ([plate] may be null when nothing was read).
     * Returns a [Consensus] exactly once per vehicle pass, otherwise null.
     */
    fun offer(plate: String?, confidence: Float, nowMs: Long): Consensus? {
        while (reads.isNotEmpty() && nowMs - reads.first().t > windowMs) reads.removeFirst()
        if (plate != null) reads.addLast(Read(plate, confidence.coerceIn(0.05f, 1f), nowMs))

        lastEmitted?.let { emitted ->
            if (plate == emitted) lastEmittedSeenAt = nowMs
            if (nowMs - lastEmittedSeenAt > rearmMs) lastEmitted = null
        }
        if (reads.isEmpty()) return null

        val totalWeight = reads.sumOf { it.confidence.toDouble() }
        val (best, votes) = reads.groupBy { it.plate }
            .maxByOrNull { (_, rs) -> rs.sumOf { it.confidence.toDouble() } } ?: return null
        val weight = votes.sumOf { it.confidence.toDouble() }
        val share = (weight / totalWeight).toFloat()

        if (votes.size >= minVotes && share >= minShare && best != lastEmitted) {
            lastEmitted = best
            lastEmittedSeenAt = nowMs
            reads.clear()
            return Consensus(best, votes.size, (weight / votes.size).toFloat())
        }
        return null
    }

    fun reset() {
        reads.clear()
        lastEmitted = null
    }
}
