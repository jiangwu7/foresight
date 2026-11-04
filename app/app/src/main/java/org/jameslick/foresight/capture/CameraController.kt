package org.jameslick.foresight.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import android.util.Size
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The phone camera, locked the way spec §3.2 demands: manual focus at the aerial-image plane, fixed
 * exposure and ISO, torch off, white balance locked. CameraX for the plumbing (preview, analysis stream,
 * still capture), Camera2 interop for the manual controls.
 *
 * Two exposure "programs" are switched at runtime: [applyAlignment] (long, high ISO, for the dim red LED)
 * and [applyCapture] (shutter a little longer than the 40 ms flash). Preview frames are handed to
 * [onFrame] as ARGB pixel arrays at ~5 fps for the quality check.
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private var settings: CaptureSettings,
    private val onFrame: (pixels: IntArray, width: Int, height: Int) -> Unit,
) {
    /** What this phone can do; shown in Settings → Device check. */
    data class Capabilities(
        val manualFocus: Boolean,
        val manualExposure: Boolean,
        val awbLock: Boolean,
        val minFocusDiopters: Float,     // 0 = fixed-focus camera
        val isoRange: IntRange?,
        val exposureNsRange: LongRange?,
    ) {
        val ok: Boolean get() = manualFocus && manualExposure
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var lastFrameAt = 0L
    var capabilities: Capabilities? = null
        private set
    /** While set, every frame goes to [catchSink] at full rate instead of the throttled [onFrame]. */
    private val catching = AtomicBoolean(false)
    @Volatile private var catchSink: ((IntArray, Int, Int) -> Unit)? = null

    suspend fun bind(previewView: PreviewView) {
        val p = awaitProvider()
        provider = p
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            // 1280×960: enough for grading (the model sees 224 px) so a caught flash frame can serve as the still.
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
            .build()
        analysis.setAnalyzer(analysisExecutor) { image -> handleFrame(image) }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
        imageCapture = capture
        p.unbindAll()
        val cam = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis, capture)
        camera = cam
        cam.cameraControl.enableTorch(false)
        capabilities = readCapabilities(cam)
        applyAlignment()
    }

    fun update(newSettings: CaptureSettings) { settings = newSettings; applyAlignment() }

    fun applyAlignment() = applyProgram(settings.alignIso, settings.alignShutterNs)
    fun applyCapture() = applyProgram(settings.captureIso, settings.captureShutterNs)

    private fun applyProgram(iso: Int, shutterNs: Long) {
        val cam = camera ?: return
        val caps = capabilities
        val b = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
        if (caps?.manualFocus == true) {
            val diopters = settings.focusDiopters.coerceIn(0f, caps.minFocusDiopters.takeIf { it > 0f } ?: settings.focusDiopters)
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, diopters)
        }
        if (caps?.manualExposure == true) {
            val isoC = caps.isoRange?.let { iso.coerceIn(it.first, it.last) } ?: iso
            val nsC = caps.exposureNsRange?.let { shutterNs.coerceIn(it.first, it.last) } ?: shutterNs
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoC)
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, nsC)
        }
        Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(b.build())
    }

    /** Full-resolution still, rotated upright. Call [applyCapture] first and [applyAlignment] after. */
    suspend fun takeStill(): Bitmap {
        val cap = imageCapture ?: error("camera not bound")
        return suspendCancellableCoroutine { cont ->
            cap.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bmp = image.toBitmap()
                        val rot = image.imageInfo.rotationDegrees
                        val out = if (rot != 0) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot.toFloat()) }, true) else bmp
                        cont.resume(out)
                    } catch (e: Exception) { cont.resumeWithException(e) } finally { image.close() }
                }
                override fun onError(exception: ImageCaptureException) { cont.resumeWithException(exception) }
            })
        }
    }

    fun unbind() {
        provider?.unbindAll(); camera = null; imageCapture = null
    }

    fun close() { unbind(); analysisExecutor.shutdown() }

    /**
     * Mode A capture: the screener fires the flash at an unknown moment, and this phone cannot expose for
     * longer than its limit (S23 Ultra: 150 ms), so the stream is watched at full rate and the frame in
     * which the flash appears is kept ([FlashCatcher]). Returns null if no flash is seen within [windowMs].
     */
    suspend fun catchFlash(windowMs: Long = FlashCatcher.WINDOW_MS): Bitmap? {
        val catcher = FlashCatcher()
        val result = CompletableDeferred<Bitmap?>()
        var best: IntArray? = null; var bw = 0; var bh = 0
        catchSink = { px, w, h ->
            if (!result.isCompleted) {
                if (catcher.feed(FlashCatcher.luma(px, w, h))) { best = px.copyOf(); bw = w; bh = h }
                if (catcher.state == FlashCatcher.State.Done) {
                    val b = best
                    result.complete(if (b != null) Bitmap.createBitmap(b, bw, bh, Bitmap.Config.ARGB_8888) else null)
                }
            }
        }
        catching.set(true)
        return try {
            withTimeoutOrNull(windowMs) { result.await() } ?: best?.let { Bitmap.createBitmap(it, bw, bh, Bitmap.Config.ARGB_8888) }
        } finally {
            catching.set(false); catchSink = null
        }
    }

    private fun handleFrame(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            val catch = catching.get()
            if (!catch && now - lastFrameAt < AutoCapture.FRAME_INTERVAL_MS) return
            lastFrameAt = now
            val plane = image.planes[0]
            val w = image.width; val h = image.height
            val rowStride = plane.rowStride; val pixelStride = plane.pixelStride
            val buf = plane.buffer
            val px = IntArray(w * h)
            val row = ByteArray(rowStride)
            for (y in 0 until h) {
                buf.position(y * rowStride)
                buf.get(row, 0, minOf(rowStride, buf.remaining()))
                var o = y * w
                var i = 0
                for (x in 0 until w) {
                    val r = row[i].toInt() and 0xFF; val g = row[i + 1].toInt() and 0xFF; val b = row[i + 2].toInt() and 0xFF
                    px[o++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    i += pixelStride
                }
            }
            if (catch) catchSink?.invoke(px, w, h) else onFrame(px, w, h)
        } finally {
            image.close()
        }
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({ try { cont.resume(future.get()) } catch (e: Exception) { cont.resumeWithException(e) } }, ContextCompat.getMainExecutor(context))
    }

    private fun readCapabilities(cam: Camera): Capabilities {
        val info = Camera2CameraInfo.from(cam.cameraInfo)
        fun <T> ch(key: CameraCharacteristics.Key<T>): T? = runCatching { info.getCameraCharacteristic(key) }.getOrNull()
        return capabilitiesFrom(
            afModes = ch(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), aeModes = ch(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
            awbLock = ch(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE), minFocus = ch(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE),
            isoRange = ch(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { it.lower..it.upper },
            expRange = ch(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { it.lower..it.upper },
        )
    }

    companion object {
        /** Device check without opening the camera (Settings). */
        fun probe(context: Context): Capabilities? = runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK } ?: return null
            val c = cm.getCameraCharacteristics(id)
            capabilitiesFrom(
                afModes = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), aeModes = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                awbLock = c.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE), minFocus = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE),
                isoRange = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { it.lower..it.upper },
                expRange = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { it.lower..it.upper },
            )
        }.getOrNull()

        private fun capabilitiesFrom(afModes: IntArray?, aeModes: IntArray?, awbLock: Boolean?, minFocus: Float?, isoRange: IntRange?, expRange: LongRange?) = Capabilities(
            manualFocus = afModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) == true && (minFocus ?: 0f) > 0f,
            manualExposure = aeModes?.contains(CameraCharacteristics.CONTROL_AE_MODE_OFF) == true && isoRange != null && expRange != null,
            awbLock = awbLock == true,
            minFocusDiopters = minFocus ?: 0f,
            isoRange = isoRange,
            exposureNsRange = expRange,
        )
    }
}
