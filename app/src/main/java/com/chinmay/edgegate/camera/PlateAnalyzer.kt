package com.chinmay.edgegate.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.chinmay.edgegate.ai.AnprPipeline
import com.chinmay.edgegate.ai.FrameResult
import java.io.Closeable

/**
 * CameraX analyzer that feeds frames into the ANPR pipeline.
 *
 * The pipeline is created lazily *on the analysis thread* so the LiteRT GPU
 * delegate is created, used and closed on one thread (a GPU delegate requirement).
 * Call [close] via the same executor.
 */
class PlateAnalyzer(
    private val pipelineFactory: () -> AnprPipeline,
    private val isPaused: () -> Boolean,
    private val onResult: (FrameResult) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {

    private var pipeline: AnprPipeline? = null

    override fun analyze(image: ImageProxy) {
        image.use { proxy ->
            if (isPaused()) return
            try {
                val p = pipeline ?: pipelineFactory().also { pipeline = it }
                val frame = proxy.toBitmap().rotate(proxy.imageInfo.rotationDegrees)
                val result = p.process(frame)
                frame.recycle()
                onResult(result)
            } catch (t: Throwable) {
                // Never let one bad frame kill the camera loop.
                Log.e("PlateAnalyzer", "Frame failed", t)
            }
        }
    }

    override fun close() {
        pipeline?.close()
        pipeline = null
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(this, 0, 0, width, height, m, true)
        if (rotated !== this) recycle()
        return rotated
    }
}
