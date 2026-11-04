package org.jameslick.foresight.ui.theme

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Draws a low-resolution class-activation heat map (e.g. 16×16 from the model's feature output) over the
 * participant's fundus image, upsampled smoothly on device (spec §4.3 Explainability, §3.4).
 *
 * Deliberately restrained: a single warm tint from transparent to accent, no "lesion" markers, no outlines,
 * no scanning effects (anti-patterns, §3.10). The caption sentence ("Highlighted areas influenced the
 * result") is rendered by the caller from `strings_medical.xml`; this component only draws.
 *
 * @param values row-major grid in 0..1, length = rows * cols
 * @param visible toggled by the screener; fades in/out
 */
@Composable
fun HeatmapOverlay(
    values: FloatArray,
    rows: Int,
    cols: Int,
    visible: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = ForesightTheme.colors.accent,
    maxAlpha: Float = 0.6f,
) {
    require(values.size == rows * cols) { "heat map size mismatch" }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(Motion.Medium, easing = Motion.Ease),
        label = "heatmap-alpha",
    )
    val bitmap: Bitmap = remember(values, rows, cols, tint, maxAlpha) { heatmapBitmap(values, rows, cols, tint, maxAlpha) }
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }

    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        if (alpha <= 0f) return@Canvas
        paint.alpha = (alpha * 255).toInt()
        drawIntoCanvas { canvas ->
            // Bilinear upsampling by the platform: the tiny bitmap is stretched to the full overlay size.
            canvas.nativeCanvas.drawBitmap(bitmap, null, Rect(0, 0, size.width.toInt(), size.height.toInt()), paint)
        }
    }
}

/** Builds the tiny ARGB bitmap: alpha rises with the activation; hue stays the single accent. */
internal fun heatmapBitmap(values: FloatArray, rows: Int, cols: Int, tint: Color, maxAlpha: Float): Bitmap {
    val pixels = IntArray(rows * cols) { i ->
        val v = values[i].coerceIn(0f, 1f)
        // ease-in so faint activations stay invisible and the strongest areas read clearly
        val a = (v * v * maxAlpha).coerceIn(0f, 1f)
        tint.copy(alpha = a).toArgb()
    }
    return Bitmap.createBitmap(pixels, cols, rows, Bitmap.Config.ARGB_8888)
}
