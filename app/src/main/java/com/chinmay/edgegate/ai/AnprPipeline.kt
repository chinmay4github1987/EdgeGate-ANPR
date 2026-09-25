package com.chinmay.edgegate.ai

import android.graphics.Bitmap
import android.os.SystemClock
import com.chinmay.edgegate.core.Box
import com.chinmay.edgegate.core.Consensus
import com.chinmay.edgegate.core.PlateCandidate
import com.chinmay.edgegate.core.PlateNormalizer
import com.chinmay.edgegate.core.TemporalVoter
import java.io.Closeable

/** Everything the UI needs to know about one processed camera frame. */
data class FrameResult(
    val candidate: PlateCandidate?,
    val confidence: Float,
    val plateBox: Box?,
    val detectMs: Double,
    val ocrMs: Double,
    val totalMs: Double,
    val consensus: Consensus?,
    val runtimeLabel: String,
    val usesDetector: Boolean,
    val usesGpu: Boolean,
)

/**
 * The on-device ANPR (Automatic Number Plate Recognition) pipeline:
 *
 *   camera frame
 *     └─▶ [1] LiteRT YOLOv8n plate detector   (GPU / CPU)      ~10-40 ms
 *           └─▶ crop + upscale plate region
 *                 └─▶ [2] ML Kit OCR                           ~20-60 ms
 *                       └─▶ [3] PlateNormalizer (grammar fix)  <0.1 ms
 *                             └─▶ [4] TemporalVoter (N frames agree)
 *                                   └─▶ confirmed plate ▶ GateDecisionEngine
 *
 * If no detector model is bundled, stage 1 is skipped and OCR runs on the whole
 * frame (slower, noisier, but works with zero setup).
 */
class AnprPipeline(
    private val detector: PlateDetector?,
    private val ocr: PlateOcr,
    private val voter: TemporalVoter = TemporalVoter(),
) : Closeable {

    val runtimeLabel: String = detector?.let { "${it.runtimeLabel} + ML Kit OCR" } ?: "ML Kit OCR · full frame (no detector)"
    private val usesDetector = detector != null
    private val usesGpu = detector?.runtimeLabel?.contains("GPU") == true

    fun process(frame: Bitmap): FrameResult {
        val t0 = SystemClock.elapsedRealtimeNanos()
        var box: Box? = null
        var detScore = 1f
        var region = frame
        var detectMs = 0.0

        if (detector != null) {
            val detections = detector.detect(frame)
            detectMs = msSince(t0)
            val best = detections.firstOrNull()
            if (best == null) {
                val consensus = voter.offer(null, 0f, SystemClock.elapsedRealtime())
                return FrameResult(null, 0f, null, detectMs, 0.0, msSince(t0), consensus, runtimeLabel, usesDetector, usesGpu)
            }
            box = best.box
            detScore = best.score
            region = cropForOcr(frame, best.box)
        }

        val t1 = SystemClock.elapsedRealtimeNanos()
        val ocrResult = ocr.read(region)
        val ocrMs = msSince(t1)
        if (region !== frame) region.recycle()

        val candidate = PlateNormalizer.bestFromLines(ocrResult.lines)
        val confidence = if (candidate == null) 0f else
            (ocrResult.confidence * detScore * (1f - 0.15f * candidate.corrections)).coerceIn(0.05f, 1f)

        val consensus = voter.offer(candidate?.plate, confidence, SystemClock.elapsedRealtime())
        return FrameResult(candidate, confidence, box, detectMs, ocrMs, msSince(t0), consensus, runtimeLabel, usesDetector, usesGpu)
    }

    /**
     * Pads the detected box (detectors crop tight and clip the first/last char)
     * and upscales small plates: ML Kit needs roughly 16 px per character height.
     */
    private fun cropForOcr(frame: Bitmap, box: Box): Bitmap {
        val padX = box.width * 0.08f
        val padY = box.height * 0.15f
        val l = ((box.left - padX).coerceIn(0f, 1f) * frame.width).toInt()
        val t = ((box.top - padY).coerceIn(0f, 1f) * frame.height).toInt()
        val r = ((box.right + padX).coerceIn(0f, 1f) * frame.width).toInt()
        val b = ((box.bottom + padY).coerceIn(0f, 1f) * frame.height).toInt()
        val w = (r - l).coerceAtLeast(1)
        val h = (b - t).coerceAtLeast(1)
        val crop = Bitmap.createBitmap(frame, l, t, w, h)
        if (h >= MIN_OCR_HEIGHT) return crop
        val scale = MIN_OCR_HEIGHT.toFloat() / h
        val up = Bitmap.createScaledBitmap(crop, (w * scale).toInt(), MIN_OCR_HEIGHT, true)
        if (up !== crop) crop.recycle()
        return up
    }

    fun resetVoting() = voter.reset()

    override fun close() {
        detector?.close()
        ocr.close()
    }

    private fun msSince(startNs: Long) = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0

    private companion object {
        const val MIN_OCR_HEIGHT = 96
    }
}
