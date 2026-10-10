package com.example.doccraft.io

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.doccraft.engine.CalculatedPage
import com.example.doccraft.engine.LaTeXRenderer
import com.example.doccraft.engine.PaginationEngine
import com.example.doccraft.model.*
import java.io.OutputStream
import kotlin.math.max

object PdfExporter {

    fun exportToPdf(document: Document, outputStream: OutputStream) {
        val pages = PaginationEngine.paginate(document)
        val pdfDocument = PdfDocument()

        for (page in pages) {
            val pageInfo = PdfDocument.PageInfo.Builder(
                page.pageSetup.effectiveWidthPt.toInt(),
                page.pageSetup.effectiveHeightPt.toInt(),
                page.pageIndex + 1
            ).create()

            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas = pdfPage.canvas

            // 1. Background fill
            val bgPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, page.pageSetup.effectiveWidthPt, page.pageSetup.effectiveHeightPt, bgPaint)

            // 2. Watermark
            page.watermark?.let { wm ->
                canvas.save()
                canvas.rotate(
                    wm.angleDegrees,
                    page.pageSetup.effectiveWidthPt / 2f,
                    page.pageSetup.effectiveHeightPt / 2f
                )
                val wmPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = wm.fontSizePt
                    color = wm.colorArgb.toInt()
                    alpha = (wm.opacity * 255).toInt()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    wm.text,
                    page.pageSetup.effectiveWidthPt / 2f,
                    page.pageSetup.effectiveHeightPt / 2f,
                    wmPaint
                )
                canvas.restore()
            }

            // 3. Header
            if (page.headerText.isNotEmpty()) {
                val headerPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 9f
                    color = Color.DKGRAY
                    typeface = Typeface.DEFAULT
                }
                val headerY = page.pageSetup.margins.topPt * 0.6f
                canvas.drawText(page.headerText, page.pageSetup.margins.leftPt, headerY, headerPaint)
            }

            // 4. Footer
            if (page.footerText.isNotEmpty()) {
                val footerPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 9f
                    color = Color.DKGRAY
                    typeface = Typeface.DEFAULT
                    textAlign = Paint.Align.RIGHT
                }
                val footerX = page.pageSetup.effectiveWidthPt - page.pageSetup.margins.rightPt
                val footerY = page.pageSetup.effectiveHeightPt - (page.pageSetup.margins.bottomPt * 0.4f)
                canvas.drawText(page.footerText, footerX, footerY, footerPaint)
            }

            // 5. Page Content Items
            for (item in page.items) {
                val itemX = page.pageSetup.margins.leftPt + item.columnIndex * (item.columnWidthPt + page.pageSetup.columnGutterPt)
                val itemY = page.pageSetup.margins.topPt + item.topPt

                when (val block = item.block) {
                    is ParagraphBlock -> {
                        drawParagraph(canvas, block, itemX, itemY, item.columnWidthPt)
                    }
                    is TableBlock -> {
                        drawTable(canvas, block, itemX, itemY, item.columnWidthPt)
                    }
                    is EquationBlock -> {
                        drawEquation(canvas, block, itemX, itemY, item.columnWidthPt)
                    }
                    is HorizontalRuleBlock -> {
                        val hrPaint = Paint().apply {
                            color = block.colorArgb.toInt()
                            strokeWidth = block.thicknessPt
                        }
                        val lineY = itemY + 6f
                        canvas.drawLine(itemX, lineY, itemX + item.columnWidthPt, lineY, hrPaint)
                    }
                    is TableOfContentsBlock -> {
                        drawTableOfContentsPlaceholder(canvas, block, itemX, itemY, item.columnWidthPt)
                    }
                    else -> {}
                }
            }

            pdfDocument.finishPage(pdfPage)
        }

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private fun drawParagraph(
        canvas: android.graphics.Canvas,
        block: ParagraphBlock,
        startX: Float,
        startY: Float,
        widthPt: Float
    ) {
        var currentY = startY + block.spaceBeforePt
        val isList = block.listStyle != ListStyle.None
        val indent = block.firstLineIndentPt + (if (isList) 18f else 0f)

        if (isList) {
            val bulletPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.BLACK
            }
            val bulletSymbol = when (block.listStyle) {
                ListStyle.Bullet -> "•"
                ListStyle.Numbered -> "1."
                ListStyle.Roman -> "I."
                ListStyle.Alphabetical -> "a."
                ListStyle.Checkbox -> "☐"
                else -> ""
            }
            canvas.drawText(bulletSymbol, startX, currentY + 12f, bulletPaint)
        }

        var lineX = startX + indent
        var maxLineHeight = 14f

        for (run in block.runs) {
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = run.style.fontSizePt
                color = run.style.textColorArgb.toInt()
                val styleFlag = when {
                    run.style.isBold && run.style.isItalic -> Typeface.BOLD_ITALIC
                    run.style.isBold -> Typeface.BOLD
                    run.style.isItalic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                val family = if (run.style.fontFamily == "Serif") Typeface.SERIF else Typeface.SANS_SERIF
                typeface = Typeface.create(family, styleFlag)
                isStrikeThruText = run.style.isStrikethrough
                isUnderlineText = run.style.underline != UnderlineStyle.None
            }

            maxLineHeight = max(maxLineHeight, run.style.fontSizePt * block.lineSpacingMultiplier * 1.2f)

            // Split into words for simple wrapping
            val words = run.text.split(Regex("((?<=\\s)|(?=\\s))"))
            for (word in words) {
                if (word == "\n") {
                    currentY += maxLineHeight
                    lineX = startX
                    continue
                }
                val wordWidth = textPaint.measureText(word)
                if (lineX + wordWidth > startX + widthPt && lineX > startX) {
                    currentY += maxLineHeight
                    lineX = startX
                }

                // Highlight background
                run.style.highlightColorArgb?.let { hlColor ->
                    val hlPaint = Paint().apply { color = hlColor.toInt() }
                    canvas.drawRect(lineX, currentY, lineX + wordWidth, currentY + run.style.fontSizePt * 1.1f, hlPaint)
                }

                canvas.drawText(word, lineX, currentY + run.style.fontSizePt, textPaint)
                lineX += wordWidth
            }
        }
    }

    private fun drawTable(
        canvas: android.graphics.Canvas,
        block: TableBlock,
        startX: Float,
        startY: Float,
        widthPt: Float
    ) {
        val colCount = max(1, block.columnCount)
        val colWidth = widthPt / colCount
        var currentY = startY + block.spaceBeforePt

        val borderPaint = Paint().apply {
            color = block.borderColorArgb.toInt()
            strokeWidth = block.borderWidthPt
            style = Paint.Style.STROKE
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.BLACK
            typeface = Typeface.DEFAULT
        }

        for (row in block.rows) {
            val rowHeight = 24f
            for ((colIndex, cell) in row.cells.withIndex()) {
                val cellLeft = startX + (colIndex * colWidth)
                val cellRight = cellLeft + (colWidth * cell.colSpan)
                val cellTop = currentY
                val cellBottom = currentY + rowHeight

                // Background
                cell.backgroundColorArgb?.let { bgColor ->
                    val bgPaint = Paint().apply { color = bgColor.toInt(); style = Paint.Style.FILL }
                    canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, bgPaint)
                }

                // Border
                canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, borderPaint)

                // Text
                val cellTextPaint = if (cell.isHeader) {
                    Paint(textPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                } else textPaint
                canvas.drawText(cell.text, cellLeft + 4f, cellTop + 16f, cellTextPaint)
            }
            currentY += rowHeight
        }
    }

    private fun drawEquation(
        canvas: android.graphics.Canvas,
        block: EquationBlock,
        startX: Float,
        startY: Float,
        widthPt: Float
    ) {
        val displayText = LaTeXRenderer.toDisplayText(block.latex)
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = block.fontSizePt * 1.2f
            color = Color.DKGRAY
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        val centerX = startX + (widthPt / 2f)
        val eqY = startY + block.spaceBeforePt + block.fontSizePt * 1.2f
        canvas.drawText(displayText, centerX, eqY, paint)
    }

    private fun drawTableOfContentsPlaceholder(
        canvas: android.graphics.Canvas,
        block: TableOfContentsBlock,
        startX: Float,
        startY: Float,
        widthPt: Float
    ) {
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(block.title, startX, startY + 16f, titlePaint)
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.GRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Headings will automatically populate page references here.", startX, startY + 34f, subPaint)
    }
}
