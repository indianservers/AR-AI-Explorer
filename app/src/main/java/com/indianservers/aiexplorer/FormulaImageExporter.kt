package com.indianservers.aiexplorer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import com.indianservers.aiexplorer.learning.FormulaExperience
import java.io.File
import java.io.FileOutputStream

internal object FormulaImageExporter {
    fun share(context: Context, detail: FormulaExperience) {
        val bitmap = Bitmap.createBitmap(1200, 630, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(3, 12, 18))
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            isFakeBoldText = true
        }
        val formulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(38, 211, 238)
            textSize = 62f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 207, 216)
            textSize = 31f
        }
        canvas.drawText(detail.formula.title.take(38), 64f, 100f, titlePaint)
        drawWrapped(canvas, displayLatexFormula(detail.formula.expression), formulaPaint, 64f, 205f, 1070f, 72f, 2)
        drawWrapped(canvas, detail.whenToUse, bodyPaint, 64f, 410f, 1070f, 42f, 4)
        bodyPaint.color = Color.rgb(88, 220, 165)
        canvas.drawText("AI Maths Explorer · ${detail.formula.category.label}", 64f, 585f, bodyPaint)

        val directory = File(context.cacheDir, "shared-maths").apply { mkdirs() }
        val file = File(directory, "formula-${detail.formula.id}.png")
        FileOutputStream(file).use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, detail.formula.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share formula image"))
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Float,
        lineHeight: Float,
        maxLines: Int,
    ) {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var line = ""
        words.forEach { word ->
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) line = candidate
            else {
                if (line.isNotBlank()) lines += line
                line = word
            }
        }
        if (line.isNotBlank()) lines += line
        lines.take(maxLines).forEachIndexed { index, value ->
            val suffix = if (index == maxLines - 1 && lines.size > maxLines) "..." else ""
            canvas.drawText(value + suffix, x, y + lineHeight * index, paint)
        }
    }
}
