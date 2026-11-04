package org.jameslick.foresight.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.jameslick.foresight.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * The printed/shared referral card (spec §3.1 step 8, mockup 509). A5-ish page on cream paper with the
 * mark, the approved outcome line, the next step, a QR code of the card code (no name, no result data in
 * the QR: the clinic matches the code to its own records), the date and the §1.3 disclaimer.
 * Output goes to cacheDir/cards and is shared through the FileProvider; never to shared storage.
 */
object ReferralCardPdf {
    data class Content(
        val cardCode: String,
        val dateText: String,
        val outcomeLine: String,
        val nextStepLine: String,
        val clinicName: String,
        val clinicPhone: String,
        val disclaimer: String,
        val codeLabel: String,
        val dateLabel: String,
    )

    private const val PAPER = 0xFFF5F1E8.toInt()
    private const val INK = 0xFF0B0D10.toInt()
    private const val MUTED = 0xFF5B5750.toInt()
    private const val PAGE_W = 420   // A5 at 72 dpi
    private const val PAGE_H = 595

    fun create(context: Context, content: Content): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas
        c.drawColor(PAPER)
        val margin = 36f
        val mark = BitmapFactory.decodeResource(context.resources, R.drawable.foresight_mark_ink)
        mark?.let { c.drawBitmap(Bitmap.createScaledBitmap(it, 56, 56, true), margin, margin, null) }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; textSize = 20f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; textSize = 13f; typeface = Typeface.SANS_SERIF }
        val micro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 9f; typeface = Typeface.MONOSPACE; letterSpacing = 0.12f }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 9f; typeface = Typeface.SANS_SERIF }
        c.drawText("FORESIGHT", margin + 68f, margin + 36f, micro)

        var y = margin + 110f
        y = drawWrapped(c, content.outcomeLine, margin, y, PAGE_W - 2 * margin - 120f, title, 26f)
        y += 10f
        y = drawWrapped(c, content.nextStepLine, margin, y, PAGE_W - 2 * margin - 120f, body, 18f)
        y += 18f
        c.drawText(content.clinicName, margin, y, body); y += 18f
        c.drawText(content.clinicPhone, margin, y, body); y += 30f

        // QR: the card code only.
        val qr = qrBitmap(content.cardCode, 110)
        c.drawBitmap(qr, PAGE_W - margin - 110f, margin + 100f, null)
        c.drawText("${content.codeLabel.uppercase()}  ${content.cardCode}", margin, y, micro); y += 16f
        c.drawText("${content.dateLabel.uppercase()}  ${content.dateText}", margin, y, micro); y += 30f

        val rule = Paint().apply { color = MUTED; strokeWidth = 0.5f }
        c.drawLine(margin, y, PAGE_W - margin, y, rule); y += 16f
        drawWrapped(c, content.disclaimer, margin, y, PAGE_W - 2 * margin, small, 12f)

        doc.finishPage(page)
        val dir = File(context.cacheDir, "cards").apply { mkdirs() }
        val out = File(dir, "foresight_card_${content.cardCode.replace("-", "")}.pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return out
    }

    fun shareUri(context: Context, file: File) = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Sends the PDF to the system print service (any printer the phone can reach, incl. Bluetooth via a print plugin). */
    fun print(context: Context, file: File, jobName: String) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(jobName, PdfAdapter(file, jobName), PrintAttributes.Builder().build())
    }

    private class PdfAdapter(private val file: File, private val name: String) : PrintDocumentAdapter() {
        override fun onLayout(old: PrintAttributes?, new: PrintAttributes, cancel: android.os.CancellationSignal?,
                              callback: LayoutResultCallback, extras: android.os.Bundle?) {
            callback.onLayoutFinished(PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(), true)
        }
        override fun onWrite(pages: Array<out PageRange>?, dest: ParcelFileDescriptor, cancel: android.os.CancellationSignal?, callback: WriteResultCallback) {
            FileInputStream(file).use { i -> FileOutputStream(dest.fileDescriptor).use { o -> i.copyTo(o) } }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }
    }

    fun qrBitmap(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val px = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) INK else PAPER }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun drawWrapped(c: android.graphics.Canvas, text: String, x: Float, startY: Float, width: Float, paint: Paint, lineHeight: Float): Float {
        var y = startY
        var line = StringBuilder()
        for (word in text.split(" ")) {
            val trial = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(trial) > width && line.isNotEmpty()) {
                c.drawText(line.toString(), x, y, paint); y += lineHeight; line = StringBuilder(word)
            } else line = StringBuilder(trial)
        }
        if (line.isNotEmpty()) { c.drawText(line.toString(), x, y, paint); y += lineHeight }
        return y
    }

}
