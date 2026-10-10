package com.example.doccraft.engine

import com.example.doccraft.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class CalculatedPage(
    val pageIndex: Int,
    val sectionIndex: Int,
    val pageSetup: PageSetup,
    val headerText: String,
    val footerText: String,
    val watermark: Watermark?,
    val items: List<PageItem>
)

data class PageItem(
    val block: BlockElement,
    val topPt: Float,
    val heightPt: Float,
    val columnIndex: Int = 0,
    val columnWidthPt: Float
)

data class TocEntry(
    val title: String,
    val pageNumber: Int,
    val level: Int,
    val blockId: String
)

object PaginationEngine {

    fun paginate(document: Document): List<CalculatedPage> {
        val totalPagesList = mutableListOf<CalculatedPage>()
        val tocEntries = mutableListOf<TocEntry>()

        // Pass 1: Paginate and collect headings with their page locations
        var currentPageNumber = 1

        for ((sectionIndex, section) in document.sections.withIndex()) {
            val pageSetup = section.pageSetup
            val printableHeight = pageSetup.printableHeightPt
            val columnWidth = pageSetup.columnWidthPt
            val columnCount = pageSetup.columnCount

            var currentColumnIndex = 0
            var currentY = 0f
            var currentPageItems = mutableListOf<PageItem>()

            fun finishCurrentPage() {
                val page = CalculatedPage(
                    pageIndex = totalPagesList.size,
                    sectionIndex = sectionIndex,
                    pageSetup = pageSetup,
                    headerText = "", // Filled in Pass 2
                    footerText = "", // Filled in Pass 2
                    watermark = document.settings.watermark,
                    items = currentPageItems.toList()
                )
                totalPagesList.add(page)
                currentPageItems = mutableListOf()
                currentColumnIndex = 0
                currentY = 0f
                currentPageNumber++
            }

            for (block in section.blocks) {
                when (block) {
                    is PageBreakBlock -> {
                        finishCurrentPage()
                    }
                    is SectionBreakBlock -> {
                        if (!block.isContinuous) {
                            finishCurrentPage()
                        }
                    }
                    is TableOfContentsBlock -> {
                        val tocHeight = 90f
                        if (currentY + tocHeight > printableHeight && currentPageItems.isNotEmpty()) {
                            if (currentColumnIndex < columnCount - 1) {
                                currentColumnIndex++
                                currentY = 0f
                            } else {
                                finishCurrentPage()
                            }
                        }
                        currentPageItems.add(
                            PageItem(
                                block = block,
                                topPt = currentY,
                                heightPt = tocHeight,
                                columnIndex = currentColumnIndex,
                                columnWidthPt = columnWidth
                            )
                        )
                        currentY += tocHeight
                    }
                    is ParagraphBlock -> {
                        // Check if heading to register in TOC
                        if (block.headingStyle != HeadingStyle.Normal) {
                            val headingText = block.plainText.trim()
                            if (headingText.isNotEmpty()) {
                                tocEntries.add(
                                    TocEntry(
                                        title = headingText,
                                        pageNumber = currentPageNumber,
                                        level = block.headingStyle.level,
                                        blockId = block.id
                                    )
                                )
                            }
                        }

                        val paraHeight = estimateParagraphHeight(block, columnWidth)
                        if (currentY + paraHeight > printableHeight && currentPageItems.isNotEmpty()) {
                            if (currentColumnIndex < columnCount - 1) {
                                currentColumnIndex++
                                currentY = 0f
                            } else {
                                finishCurrentPage()
                            }
                        }
                        currentPageItems.add(
                            PageItem(
                                block = block,
                                topPt = currentY,
                                heightPt = paraHeight,
                                columnIndex = currentColumnIndex,
                                columnWidthPt = columnWidth
                            )
                        )
                        currentY += paraHeight
                    }
                    is TableBlock -> {
                        val tableHeight = estimateTableHeight(block, columnWidth)
                        if (currentY + tableHeight > printableHeight && currentPageItems.isNotEmpty()) {
                            if (currentColumnIndex < columnCount - 1) {
                                currentColumnIndex++
                                currentY = 0f
                            } else {
                                finishCurrentPage()
                            }
                        }
                        currentPageItems.add(
                            PageItem(
                                block = block,
                                topPt = currentY,
                                heightPt = tableHeight,
                                columnIndex = currentColumnIndex,
                                columnWidthPt = columnWidth
                            )
                        )
                        currentY += tableHeight
                    }
                    is ImageBlock -> {
                        val imgHeight = block.heightPt + block.spaceBeforePt + block.spaceAfterPt
                        if (currentY + imgHeight > printableHeight && currentPageItems.isNotEmpty()) {
                            if (currentColumnIndex < columnCount - 1) {
                                currentColumnIndex++
                                currentY = 0f
                            } else {
                                finishCurrentPage()
                            }
                        }
                        currentPageItems.add(
                            PageItem(
                                block = block,
                                topPt = currentY,
                                heightPt = imgHeight,
                                columnIndex = currentColumnIndex,
                                columnWidthPt = columnWidth
                            )
                        )
                        currentY += imgHeight
                    }
                    is EquationBlock -> {
                        val eqHeight = max(36f, block.fontSizePt * 2.5f) + block.spaceBeforePt + block.spaceAfterPt
                        if (currentY + eqHeight > printableHeight && currentPageItems.isNotEmpty()) {
                            if (currentColumnIndex < columnCount - 1) {
                                currentColumnIndex++
                                currentY = 0f
                            } else {
                                finishCurrentPage()
                            }
                        }
                        currentPageItems.add(
                            PageItem(
                                block = block,
                                topPt = currentY,
                                heightPt = eqHeight,
                                columnIndex = currentColumnIndex,
                                columnWidthPt = columnWidth
                            )
                        )
                        currentY += eqHeight
                    }
                    is HorizontalRuleBlock -> {
                        val hrHeight = 12f
                        if (currentY + hrHeight > printableHeight && currentPageItems.isNotEmpty()) {
                            if (currentColumnIndex < columnCount - 1) {
                                currentColumnIndex++
                                currentY = 0f
                            } else {
                                finishCurrentPage()
                            }
                        }
                        currentPageItems.add(
                            PageItem(
                                block = block,
                                topPt = currentY,
                                heightPt = hrHeight,
                                columnIndex = currentColumnIndex,
                                columnWidthPt = columnWidth
                            )
                        )
                        currentY += hrHeight
                    }
                }
            }

            // Flush remaining items
            if (currentPageItems.isNotEmpty() || totalPagesList.isEmpty()) {
                finishCurrentPage()
            }
        }

        // Pass 2: Evaluate dynamic Header and Footer tokens (Page X of Y, Title, Date)
        val totalCount = totalPagesList.size
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

        return totalPagesList.map { page ->
            val section = document.sections.getOrNull(page.sectionIndex) ?: Section()
            val hf = section.headerFooter
            val pNum = page.pageIndex + 1

            val rawHeader = if (pNum == 1 && !hf.showOnFirstPage) "" else hf.headerText
            val rawFooter = if (pNum == 1 && !hf.showOnFirstPage) "" else hf.footerText

            val evaluatedHeader = evaluateTokens(rawHeader, pNum, totalCount, document.title, dateStr)
            val evaluatedFooter = evaluateTokens(rawFooter, pNum, totalCount, document.title, dateStr)

            page.copy(
                headerText = evaluatedHeader,
                footerText = evaluatedFooter
            )
        }
    }

    private fun evaluateTokens(template: String, page: Int, totalPages: Int, title: String, date: String): String {
        return template
            .replace("[PAGE]", page.toString())
            .replace("[NUMPAGES]", totalPages.toString())
            .replace("[TITLE]", title)
            .replace("[DATE]", date)
    }

    fun estimateParagraphHeight(block: ParagraphBlock, availableWidthPt: Float): Float {
        val maxFontSize = block.runs.maxOfOrNull { it.style.fontSizePt } ?: block.headingStyle.defaultFontSizePt
        val lineSpacing = block.lineSpacingMultiplier * maxFontSize * 1.25f

        val text = block.plainText
        if (text.isEmpty()) {
            return block.spaceBeforePt + lineSpacing + block.spaceAfterPt
        }

        // Approximate line count via words
        val effectiveWidth = max(50f, availableWidthPt - block.firstLineIndentPt)
        val avgCharWidth = maxFontSize * 0.48f
        val charsPerLine = max(10, (effectiveWidth / avgCharWidth).toInt())

        var lines = 0
        val paragraphs = text.split("\n")
        for (para in paragraphs) {
            if (para.isEmpty()) {
                lines++
            } else {
                val words = para.split(Regex("\\s+"))
                var currentLineLen = 0
                for (w in words) {
                    if (currentLineLen + w.length > charsPerLine) {
                        lines++
                        currentLineLen = w.length
                    } else {
                        currentLineLen += w.length + 1
                    }
                }
                lines++ // last line of this newline paragraph
            }
        }

        val totalLines = max(1, lines)
        return block.spaceBeforePt + (totalLines * lineSpacing) + block.spaceAfterPt
    }

    fun estimateTableHeight(block: TableBlock, availableWidthPt: Float): Float {
        var totalHeight = block.spaceBeforePt + block.spaceAfterPt
        val colCount = max(1, block.columnCount)
        val cellWidth = availableWidthPt / colCount

        for (row in block.rows) {
            var maxRowHeight = 26f
            for (cell in row.cells) {
                val charsPerLine = max(8, (cellWidth / 5.5f).toInt())
                val lines = max(1, (cell.text.length / charsPerLine) + 1)
                val cellHeight = 12f + (lines * 14f)
                if (cellHeight > maxRowHeight) {
                    maxRowHeight = cellHeight
                }
            }
            totalHeight += maxRowHeight
        }
        return totalHeight
    }
}
