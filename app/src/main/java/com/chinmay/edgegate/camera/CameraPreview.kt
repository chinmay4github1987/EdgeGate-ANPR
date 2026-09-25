package com.chinmay.edgegate.camera

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * Binds CameraX Preview + ImageAnalysis to the lifecycle.
 *
 * Both use cases use a 4:3 aspect ratio and the preview is shown in a 3:4 box,
 * so normalised detector coordinates map 1:1 onto the preview overlay.
 * Analysis runs at ~1280×960 (plenty for plates up to ~8 m away) with
 * KEEP_ONLY_LATEST: if inference is slower than the camera, stale frames are
 * dropped instead of queuing up – latency stays bounded.
 *
 * [onCamera] hands back the bound Camera (for the torch) or null when unbound.
 */
@Composable
fun CameraPreview(
    analyzerFactory: () -> PlateAnalyzer,
    modifier: Modifier = Modifier,
    onCamera: (Camera?) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCamera = rememberUpdatedState(onCamera)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var analyzer: PlateAnalyzer? = null

        providerFuture.addListener({
            val provider = providerFuture.get()
            val aspect = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            val preview = Preview.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspect).build())
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(aspect)
                        .setResolutionStrategy(
                            ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analyzer = analyzerFactory().also { analysis.setAnalyzer(executor, it) }

            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            currentOnCamera.value(camera)
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            currentOnCamera.value(null)
            runCatching { providerFuture.get().unbindAll() }
            val a = analyzer
            executor.execute { a?.close() } // close on the same thread that created the GPU delegate
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
