package com.chinmay.edgegate.ai

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Stage 2: read the characters.
 *
 * Uses ML Kit's *bundled* Latin text recogniser – the model ships inside the APK,
 * runs fully on-device and needs no network, not even on first launch.
 *
 * Runs synchronously (Tasks.await) because it is called from the single camera
 * analysis thread; the camera's KEEP_ONLY_LATEST strategy provides back-pressure.
 */
class PlateOcr : Closeable {

    data class Result(val lines: List<String>, val confidence: Float)

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun read(bitmap: Bitmap): Result {
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 2, TimeUnit.SECONDS)
        // Top-to-bottom so two-row plates (bikes, trucks) are joined in reading order.
        val lines = text.textBlocks
            .flatMap { it.lines }
            .sortedBy { it.boundingBox?.top ?: 0 }
        if (lines.isEmpty()) return Result(emptyList(), 0f)
        val confidence = lines.map { it.confidence }.average().toFloat()
        return Result(lines.map { it.text }, confidence)
    }

    override fun close() = recognizer.close()
}
