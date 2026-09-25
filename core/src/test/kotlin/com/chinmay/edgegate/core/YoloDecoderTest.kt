package com.chinmay.edgegate.core

import org.junit.Assert.assertEquals
import org.junit.Test

class YoloDecoderTest {

    /** Builds a channels-first [5, n] tensor from (cx, cy, w, h, score) tuples. */
    private fun tensor(vararg anchors: FloatArray): FloatArray {
        val n = anchors.size
        val out = FloatArray(5 * n)
        anchors.forEachIndexed { a, v -> for (c in 0 until 5) out[c * n + a] = v[c] }
        return out
    }

    @Test fun decodesAndThresholds() {
        val out = tensor(
            floatArrayOf(0.5f, 0.5f, 0.2f, 0.1f, 0.9f),
            floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f), // below threshold
        )
        val d = YoloDecoder.decode(out, numChannels = 5, numAnchors = 2)
        assertEquals(1, d.size)
        assertEquals(0.4f, d[0].box.left, 1e-5f)
        assertEquals(0.55f, d[0].box.bottom, 1e-5f)
    }

    @Test fun handlesPixelCoordinatesAndTransposedLayout() {
        val out = floatArrayOf(160f, 160f, 64f, 32f, 0.8f) // [1, 1, 5]
        val d = YoloDecoder.decode(out, 5, 1, channelsFirst = false, coordScale = 320f)
        assertEquals(0.4f, d[0].box.left, 1e-5f)
    }

    @Test fun nmsRemovesOverlappingDuplicates() {
        val a = Detection(Box(0.1f, 0.1f, 0.5f, 0.3f), 0.9f)
        val b = Detection(Box(0.12f, 0.1f, 0.52f, 0.3f), 0.8f) // same plate, shifted
        val c = Detection(Box(0.6f, 0.6f, 0.9f, 0.8f), 0.7f)   // different vehicle
        val kept = YoloDecoder.nms(listOf(b, c, a))
        assertEquals(listOf(a, c), kept)
    }
}
