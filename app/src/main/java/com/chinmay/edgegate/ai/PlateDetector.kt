package com.chinmay.edgegate.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.chinmay.edgegate.core.Detection
import com.chinmay.edgegate.core.YoloDecoder
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Stage 1 of the pipeline: where in the frame is the number plate? */
interface PlateDetector : Closeable {
    /** Human-readable runtime, e.g. "LiteRT · GPU". Shown on the scan HUD. */
    val runtimeLabel: String
    fun detect(bitmap: Bitmap): List<Detection>
}

/**
 * YOLOv8n plate detector running on LiteRT (formerly TensorFlow Lite).
 *
 * Edge-AI points implemented here:
 *  1. The model is memory-mapped from the APK (no copy into the Java heap).
 *  2. Hardware acceleration with graceful fallback: GPU delegate if the device's
 *     GPU is on the compatibility list, otherwise multi-threaded CPU via XNNPACK.
 *  3. Works with float32, float16 and int8-quantised exports: it reads the tensor
 *     type and quantisation (scale, zero-point) at runtime instead of assuming.
 *  4. Input/output buffers are allocated once and reused every frame, so the
 *     camera loop creates no garbage (no GC pauses = stable latency).
 *
 * Must be created, used and closed on the same thread (the camera analysis
 * thread) because the GPU delegate is bound to the thread that created it.
 */
class LiteRtPlateDetector private constructor(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    override val runtimeLabel: String,
    private val scoreThreshold: Float,
) : PlateDetector {

    private val inputTensor = interpreter.getInputTensor(0)
    private val inShape = inputTensor.shape()
    // Legacy TF-based exports are NHWC [1,H,W,3]; Ultralytics' newer direct LiteRT
    // export (format="litert") traces PyTorch and keeps NCHW [1,3,H,W].
    private val nchw = inShape[1] == 3 && inShape[3] != 3
    private val inH = if (nchw) inShape[2] else inShape[1]
    private val inW = if (nchw) inShape[3] else inShape[2]
    private val inType = inputTensor.dataType()
    private val inScale = inputTensor.quantizationParams().scale
    private val inZeroPoint = inputTensor.quantizationParams().zeroPoint

    private val outputTensor = interpreter.getOutputTensor(0)
    private val outShape = outputTensor.shape() // [1, C, N] or [1, N, C]
    private val channelsFirst = outShape[1] < outShape[2]
    private val numChannels = minOf(outShape[1], outShape[2])
    private val numAnchors = maxOf(outShape[1], outShape[2])
    private val outType = outputTensor.dataType()
    private val outScale = outputTensor.quantizationParams().scale
    private val outZeroPoint = outputTensor.quantizationParams().zeroPoint

    private val bytesPerChannel = if (inType == DataType.FLOAT32) 4 else 1
    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(inH * inW * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
    private val pixels = IntArray(inW * inH)
    private val output = FloatArray(numChannels * numAnchors)

    override fun detect(bitmap: Bitmap): List<Detection> {
        // Stretch-resize to model input. Plates are wide and small, so for production
        // letterboxing (keep aspect, pad) usually gives +1-3% recall – see docs.
        val scaled = Bitmap.createScaledBitmap(bitmap, inW, inH, true)
        scaled.getPixels(pixels, 0, inW, 0, 0, inW, inH)
        if (scaled !== bitmap) scaled.recycle()

        inputBuffer.rewind()
        if (nchw) {
            // Planar: all R, then all G, then all B.
            for (shift in intArrayOf(16, 8, 0)) for (p in pixels) putChannel((p shr shift) and 0xFF)
        } else {
            // Interleaved: RGB RGB RGB ...
            for (p in pixels) {
                putChannel((p shr 16) and 0xFF)
                putChannel((p shr 8) and 0xFF)
                putChannel(p and 0xFF)
            }
        }
        inputBuffer.rewind()
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        readOutput()

        // Ultralytics TFLite exports emit normalised coords; some exports emit pixels.
        var maxCoord = 0f
        for (a in 0 until numAnchors) for (c in 0 until 4) {
            val v = if (channelsFirst) output[c * numAnchors + a] else output[a * numChannels + c]
            if (v > maxCoord) maxCoord = v
        }
        val coordScale = if (maxCoord > 1.5f) inW.toFloat() else 1f

        val raw = YoloDecoder.decode(output, numChannels, numAnchors, channelsFirst, scoreThreshold, coordScale)
        return YoloDecoder.nms(raw, iouThreshold = 0.45f, maxDetections = 3)
    }

    private fun putChannel(value: Int) {
        when (inType) {
            DataType.FLOAT32 -> inputBuffer.putFloat(value / 255f)
            DataType.UINT8 -> inputBuffer.put(value.toByte())
            DataType.INT8 -> {
                val q = Math.round((value / 255f) / inScale + inZeroPoint).coerceIn(-128, 127)
                inputBuffer.put(q.toByte())
            }
            else -> error("Unsupported input type $inType")
        }
    }

    private fun readOutput() {
        when (outType) {
            DataType.FLOAT32 -> outputBuffer.asFloatBuffer().get(output)
            DataType.INT8 -> for (i in output.indices) output[i] = (outputBuffer.get(i) - outZeroPoint) * outScale
            DataType.UINT8 -> for (i in output.indices) {
                output[i] = ((outputBuffer.get(i).toInt() and 0xFF) - outZeroPoint) * outScale
            }
            else -> error("Unsupported output type $outType")
        }
    }

    override fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }

    companion object {
        private const val TAG = "LiteRtPlateDetector"
        const val MODEL_ASSET = "plate_detector.tflite"

        /**
         * Returns null when no model is bundled; the pipeline then falls back to
         * ML Kit on the full frame so the app still works out of the box.
         */
        fun createOrNull(context: Context, preferGpu: Boolean = true, scoreThreshold: Float = 0.35f): LiteRtPlateDetector? {
            val model = try {
                mapAsset(context, MODEL_ASSET)
            } catch (e: IOException) {
                Log.i(TAG, "No $MODEL_ASSET in assets – using full-frame OCR fallback")
                return null
            }

            if (preferGpu) {
                val compat = CompatibilityList()
                try {
                    if (compat.isDelegateSupportedOnThisDevice) {
                        val delegate = GpuDelegate(compat.bestOptionsForThisDevice)
                        try {
                            val interpreter = Interpreter(model, Interpreter.Options().addDelegate(delegate))
                            return LiteRtPlateDetector(interpreter, delegate, "LiteRT · GPU", scoreThreshold)
                        } catch (t: Throwable) {
                            Log.w(TAG, "GPU delegate failed, falling back to CPU", t)
                            delegate.close()
                        }
                    }
                } finally {
                    compat.close()
                }
            }

            val cores = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
            val options = Interpreter.Options().setNumThreads(cores).setUseXNNPACK(true)
            return LiteRtPlateDetector(Interpreter(model, options), null, "LiteRT · CPU×$cores", scoreThreshold)
        }

        private fun mapAsset(context: Context, name: String): MappedByteBuffer {
            context.assets.openFd(name).use { fd ->
                FileInputStream(fd.fileDescriptor).use { stream ->
                    return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        }
    }
}
