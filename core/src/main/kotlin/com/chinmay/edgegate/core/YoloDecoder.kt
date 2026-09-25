package com.chinmay.edgegate.core

/**
 * Post-processing for a YOLOv8-style single-stage detector exported to TFLite/LiteRT.
 *
 * Ultralytics exports produce one output tensor of shape [1, 4 + numClasses, numAnchors]
 * ("channels first"), where each anchor column holds cx, cy, w, h followed by class scores.
 * For a 320x320 input there are 2100 anchors; for 640x640, 8400.
 *
 * This lives in :core (plain Kotlin) so it can be unit-tested on the JVM without a device.
 */
object YoloDecoder {

    /**
     * @param output         flattened (already de-quantised) output tensor
     * @param numChannels    4 + number of classes (5 for a plate-only model)
     * @param numAnchors     number of candidate boxes
     * @param channelsFirst  true for [1, C, N] (Ultralytics default), false for [1, N, C]
     * @param coordScale     divide coordinates by this to normalise; 1f if model already outputs 0..1,
     *                       or the input size (e.g. 320f) if it outputs pixels
     */
    fun decode(
        output: FloatArray,
        numChannels: Int,
        numAnchors: Int,
        channelsFirst: Boolean = true,
        scoreThreshold: Float = 0.35f,
        coordScale: Float = 1f,
    ): List<Detection> {
        require(output.size >= numChannels * numAnchors) { "output too small for $numChannels x $numAnchors" }
        fun v(c: Int, a: Int) = if (channelsFirst) output[c * numAnchors + a] else output[a * numChannels + c]

        val result = ArrayList<Detection>()
        for (a in 0 until numAnchors) {
            var score = 0f
            for (c in 4 until numChannels) score = maxOf(score, v(c, a))
            if (score < scoreThreshold) continue
            val cx = v(0, a) / coordScale
            val cy = v(1, a) / coordScale
            val w = v(2, a) / coordScale
            val h = v(3, a) / coordScale
            val box = Box(
                (cx - w / 2).coerceIn(0f, 1f),
                (cy - h / 2).coerceIn(0f, 1f),
                (cx + w / 2).coerceIn(0f, 1f),
                (cy + h / 2).coerceIn(0f, 1f),
            )
            if (box.area > 0f) result += Detection(box, score)
        }
        return result
    }

    /** Greedy non-maximum suppression: keep the best box, drop overlapping duplicates. */
    fun nms(detections: List<Detection>, iouThreshold: Float = 0.45f, maxDetections: Int = 5): List<Detection> {
        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val kept = ArrayList<Detection>()
        while (sorted.isNotEmpty() && kept.size < maxDetections) {
            val best = sorted.removeAt(0)
            kept += best
            sorted.removeAll { it.box.iou(best.box) > iouThreshold }
        }
        return kept
    }
}
