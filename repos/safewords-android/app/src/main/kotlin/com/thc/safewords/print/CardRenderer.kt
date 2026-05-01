package com.thc.safewords.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.print.PrintAttributes
import androidx.print.PrintHelper

/**
 * Native, offline card-to-print pipeline. v1.3 renders cards using Android's
 * 2D Canvas API (no webview, no external libraries) and routes them to the
 * platform PrintHelper. Bitmaps are produced at letter-card pixel densities
 * suitable for 300dpi printing.
 *
 * No network calls. The whole pipeline is on-device.
 */
object CardRenderer {

    /** Wallet card dimensions at 300dpi: 3.5" x 2.0". */
    private const val WALLET_W = 1050
    private const val WALLET_H = 600

    /** Letter dimensions at 300dpi: 8.5" x 11". */
    private const val LETTER_W = 2550
    private const val LETTER_H = 3300

    fun renderProtocolCard(
        context: Context,
        groupName: String,
        rules: List<String>,
        title: String = "Safewords Protocol",
        subtitle: String = "Ask. Listen. Hang up if it's wrong.",
        footer: String = "safewords.io · Pre-agreed proof of identity",
    ): Bitmap = renderLetterCard {
        drawHeader(it, title, subtitle)
        drawRules(it, rules, topY = 600f)
        drawFooter(it, footer)
    }

    fun renderOverrideCard(
        groupName: String,
        word: String,
        warningHeading: String,
        warningBody: String,
        footer: String,
    ): Bitmap = renderLetterCard {
        drawHeader(it, "Safewords Override", "$groupName — emergency override word")
        drawHero(it, word)
        drawWarning(it, warningHeading, warningBody, topY = 1900f)
        drawFooter(it, footer)
    }

    fun renderRecoveryCard(
        groupName: String,
        words: List<String>,
        warningHeading: String,
        warningBody: String,
        footer: String,
    ): Bitmap = renderLetterCard {
        drawHeader(it, "Safewords Recovery", "$groupName — 24-word recovery phrase")
        drawWordGrid(it, words, columns = 4, topY = 600f)
        drawWarning(it, warningHeading, warningBody, topY = 2200f)
        drawFooter(it, footer)
    }

    fun renderChallengeAnswerCard(
        groupName: String,
        rows: List<Pair<String, String>>,
        title: String,
        subtitle: String,
        warningHeading: String,
        warningBody: String,
    ): Bitmap = renderLetterCard {
        drawHeader(it, title, subtitle)
        drawTable(it, rows, topY = 480f)
        drawWarning(it, warningHeading, warningBody, topY = 3050f)
    }

    fun renderInviteCard(
        groupName: String,
        qrBitmap: Bitmap,
        title: String,
        subtitle: String,
        warningHeading: String,
        warningBody: String,
        footer: String,
    ): Bitmap = renderLetterCard {
        drawHeader(it, title, subtitle)
        drawQr(it, qrBitmap)
        drawWarning(it, warningHeading, warningBody, topY = 2400f)
        drawFooter(it, footer)
    }

    /** Send a rendered bitmap to the system print dialog. */
    fun print(context: Context, bitmap: Bitmap, jobName: String = "Safewords card") {
        val helper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
            colorMode = PrintHelper.COLOR_MODE_MONOCHROME
            orientation = PrintAttributes.MediaSize.NA_LETTER.let {
                if (bitmap.width > bitmap.height) PrintHelper.ORIENTATION_LANDSCAPE
                else PrintHelper.ORIENTATION_PORTRAIT
            }
        }
        helper.printBitmap(jobName, bitmap)
    }

    // ─── Internals ──────────────────────────────────────────────────────

    private fun renderLetterCard(draw: (Canvas) -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(LETTER_W, LETTER_H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(AColor.WHITE)
        draw(c)
        return bmp
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.BLACK
        textSize = 96f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.DKGRAY
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.BLACK
        textSize = 56f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.BLACK
        textSize = 280f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val warningHeadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.rgb(180, 0, 0)
        textSize = 56f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val warningBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.DKGRAY
        textSize = 40f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.GRAY
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private fun drawHeader(c: Canvas, title: String, subtitle: String) {
        c.drawText(title, 180f, 240f, titlePaint)
        c.drawText(subtitle, 180f, 340f, subtitlePaint)
        c.drawLine(180f, 400f, (LETTER_W - 180).toFloat(), 400f, Paint().apply {
            color = AColor.BLACK
            strokeWidth = 4f
        })
    }

    private fun drawHero(c: Canvas, text: String) {
        // Placed in the upper-middle band of a letter card.
        c.drawText(text, (LETTER_W / 2).toFloat(), 1200f, heroPaint)
    }

    private fun drawRules(c: Canvas, rules: List<String>, topY: Float) {
        var y = topY
        rules.forEachIndexed { i, rule ->
            c.drawText("${i + 1}. $rule", 180f, y, bodyPaint)
            y += 96f
        }
    }

    private fun drawWordGrid(c: Canvas, words: List<String>, columns: Int, topY: Float) {
        val gridX = 180f
        val cellW = (LETTER_W - 360) / columns.toFloat()
        val cellH = 130f
        words.forEachIndexed { idx, w ->
            val col = idx % columns
            val row = idx / columns
            val x = gridX + col * cellW
            val y = topY + row * cellH
            c.drawText("${idx + 1}. $w", x, y, bodyPaint)
        }
    }

    private fun drawTable(c: Canvas, rows: List<Pair<String, String>>, topY: Float) {
        // Two-column table: index/ask | expect.
        val rowH = 60f
        val colWidth = (LETTER_W - 360) / 2f
        // Header
        c.drawText("# / I ask", 180f, topY, warningHeadingPaint)
        c.drawText("They answer", 180f + colWidth + 60f, topY, warningHeadingPaint)
        var y = topY + 80f
        rows.forEachIndexed { i, (ask, expect) ->
            c.drawText("${i + 1}. $ask", 180f, y, footerPaint.apply { textSize = 36f })
            c.drawText(expect, 180f + colWidth + 60f, y, footerPaint)
            y += rowH
        }
    }

    private fun drawQr(c: Canvas, qr: Bitmap) {
        val size = 1500
        val left = (LETTER_W - size) / 2f
        val top = 600f
        val src = Rect(0, 0, qr.width, qr.height)
        val dst = Rect(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
        c.drawBitmap(qr, src, dst, null)
    }

    private fun drawWarning(c: Canvas, heading: String, body: String, topY: Float) {
        c.drawText(heading, 180f, topY, warningHeadingPaint)
        // Wrap body to ~80 chars; simple word-wrap.
        val maxWidth = LETTER_W - 360
        var y = topY + 80f
        var line = ""
        body.split(' ').forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            val width = warningBodyPaint.measureText(candidate)
            if (width > maxWidth) {
                c.drawText(line, 180f, y, warningBodyPaint)
                line = word
                y += 60f
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) c.drawText(line, 180f, y, warningBodyPaint)
    }

    private fun drawFooter(c: Canvas, text: String) {
        c.drawText(text, 180f, (LETTER_H - 180).toFloat(), footerPaint)
    }
}
