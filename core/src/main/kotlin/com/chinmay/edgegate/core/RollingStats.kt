package com.chinmay.edgegate.core

/**
 * Fixed-size window of latency samples with mean and percentiles.
 *
 * Edge AI is judged on tail latency (p90/p99), not just the average: a gate
 * camera that is usually 40 ms but sometimes 400 ms feels broken. The app shows
 * p50/p90 live so you can compare CPU vs GPU delegates on a real device.
 */
class RollingStats(private val capacity: Int = 60) {
    private val samples = ArrayDeque<Double>(capacity)

    @Synchronized
    fun add(value: Double) {
        if (samples.size == capacity) samples.removeFirst()
        samples.addLast(value)
    }

    @get:Synchronized
    val count: Int get() = samples.size

    @Synchronized
    fun mean(): Double = if (samples.isEmpty()) 0.0 else samples.average()

    /** Nearest-rank percentile, p in 0..100. */
    @Synchronized
    fun percentile(p: Double): Double {
        if (samples.isEmpty()) return 0.0
        val sorted = samples.sorted()
        val rank = kotlin.math.ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }
}
