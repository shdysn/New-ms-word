package com.example.doccraft.model

import java.util.UUID

/**
 * Core Document AST (Abstract Syntax Tree) and Data Models.
 * Production-ready, fully typed immutable representations.
 */

data class Document(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Document",
    val metadata: DocumentMetadata = DocumentMetadata(),
    val sections: List<Section> = listOf(Section()),
    val settings: DocumentSettings = DocumentSettings(),
    val comments: List<Comment> = emptyList(),
    val freehandStrokes: List<FreehandStroke> = emptyList()
) {
    val totalBlockCount: Int
        get() = sections.sumOf { it.blocks.size }

    fun allBlocks(): List<BlockElement> = sections.flatMap { it.blocks }
}

data class DocumentMetadata(
    val author: String = "Author",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val subject: String = "",
    val keywords: List<String> = emptyList(),
    val revisionNumber: Int = 1
)

data class DocumentSettings(
    val defaultPaperSize: PaperSize = PaperSize.A4,
    val defaultOrientation: Orientation = Orientation.Portrait,
    val defaultMargins: Margins = Margins.Normal,
    val watermark: Watermark? = null,
    val isReadOnly: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null
)

data class Section(
    val id: String = UUID.randomUUID().toString(),
    val pageSetup: PageSetup = PageSetup(),
    val headerFooter: HeaderFooter = HeaderFooter(),
    val blocks: List<BlockElement> = listOf(
        ParagraphBlock(
            runs = listOf(TextRun(text = "Welcome to your new document."))
        )
    )
)

data class PageSetup(
    val paperSize: PaperSize = PaperSize.A4,
    val orientation: Orientation = Orientation.Portrait,
    val margins: Margins = Margins.Normal,
    val columnCount: Int = 1,
    val columnGutterPt: Float = 18f
) {
    val effectiveWidthPt: Float
        get() = if (orientation == Orientation.Portrait) paperSize.widthPt else paperSize.heightPt

    val effectiveHeightPt: Float
        get() = if (orientation == Orientation.Portrait) paperSize.heightPt else paperSize.widthPt

    val printableWidthPt: Float
        get() = effectiveWidthPt - margins.leftPt - margins.rightPt

    val printableHeightPt: Float
        get() = effectiveHeightPt - margins.topPt - margins.bottomPt

    val columnWidthPt: Float
        get() = if (columnCount <= 1) printableWidthPt
        else (printableWidthPt - (columnCount - 1) * columnGutterPt) / columnCount
}

enum class PaperSize(val displayName: String, val widthPt: Float, val heightPt: Float) {
    A4("A4 (210 x 297 mm)", 595.28f, 841.89f),
    Letter("US Letter (8.5 x 11 in)", 612f, 792f),
    Legal("Legal (8.5 x 14 in)", 612f, 1008f),
    Executive("Executive (7.25 x 10.5 in)", 522f, 756f),
    A3("A3 (297 x 420 mm)", 841.89f, 1190.55f),
    A5("A5 (148 x 210 mm)", 419.53f, 595.28f)
}

enum class Orientation {
    Portrait,
    Landscape
}

data class Margins(
    val topPt: Float,
    val bottomPt: Float,
    val leftPt: Float,
    val rightPt: Float
) {
    companion object {
        // Standard 72 points = 1 inch
        val Normal = Margins(72f, 72f, 72f, 72f) // 1 inch all around
        val Narrow = Margins(36f, 36f, 36f, 36f) // 0.5 inch
        val Moderate = Margins(72f, 72f, 54f, 54f) // 1 inch top/bottom, 0.75 left/right
        val Wide = Margins(72f, 72f, 144f, 144f) // 1 inch top/bottom, 2 inch left/right
    }
}

data class HeaderFooter(
    val headerText: String = "",
    val footerText: String = "Page [PAGE] of [NUMPAGES]",
    val showOnFirstPage: Boolean = true,
    val differentOddEven: Boolean = false,
    val heightPt: Float = 40f
)

data class Watermark(
    val text: String = "CONFIDENTIAL",
    val colorArgb: Long = 0x22888888,
    val opacity: Float = 0.15f,
    val fontSizePt: Float = 54f,
    val angleDegrees: Float = -45f
)

sealed class BlockElement {
    abstract val id: String
}

data class ParagraphBlock(
    override val id: String = UUID.randomUUID().toString(),
    val runs: List<TextRun> = listOf(TextRun(text = "")),
    val alignment: TextAlignment = TextAlignment.Left,
    val lineSpacingMultiplier: Float = 1.15f,
    val spaceBeforePt: Float = 0f,
    val spaceAfterPt: Float = 6f,
    val firstLineIndentPt: Float = 0f,
    val hangingIndentPt: Float = 0f,
    val headingStyle: HeadingStyle = HeadingStyle.Normal,
    val listStyle: ListStyle = ListStyle.None,
    val listLevel: Int = 0,
    val isDropCap: Boolean = false
) : BlockElement() {
    val plainText: String
        get() = runs.joinToString(separator = "") { it.text }
}

data class TableBlock(
    override val id: String = UUID.randomUUID().toString(),
    val rows: List<TableRow> = listOf(
        TableRow(cells = listOf(TableCell(text = "Header 1"), TableCell(text = "Header 2"))),
        TableRow(cells = listOf(TableCell(text = "Data 1"), TableCell(text = "Data 2")))
    ),
    val columnWidthsFraction: List<Float> = emptyList(), // fractions summing to 1.0
    val borderColorArgb: Long = 0xFFCCCCCC,
    val borderWidthPt: Float = 1f,
    val borderStyle: TableBorderStyle = TableBorderStyle.Solid,
    val spaceBeforePt: Float = 8f,
    val spaceAfterPt: Float = 8f
) : BlockElement() {
    val columnCount: Int
        get() = rows.maxOfOrNull { it.cells.size } ?: 0
}

data class TableRow(
    val id: String = UUID.randomUUID().toString(),
    val cells: List<TableCell> = emptyList()
)

data class TableCell(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val backgroundColorArgb: Long? = null,
    val textAlignment: TextAlignment = TextAlignment.Left,
    val isHeader: Boolean = false
)

enum class TableBorderStyle {
    Solid,
    Dashed,
    Dotted,
    None
}

data class ImageBlock(
    override val id: String = UUID.randomUUID().toString(),
    val uri: String = "",
    val caption: String = "",
    val widthPt: Float = 240f,
    val heightPt: Float = 160f,
    val rotationDegrees: Float = 0f,
    val wrappingMode: TextWrappingMode = TextWrappingMode.Inline,
    val alignment: TextAlignment = TextAlignment.Center,
    val spaceBeforePt: Float = 6f,
    val spaceAfterPt: Float = 6f
) : BlockElement()

enum class TextWrappingMode {
    Inline,
    Square,
    Tight,
    BehindText,
    InFrontOfText
}

data class EquationBlock(
    override val id: String = UUID.randomUUID().toString(),
    val latex: String = "E = mc^2",
    val fontSizePt: Float = 14f,
    val alignment: TextAlignment = TextAlignment.Center,
    val spaceBeforePt: Float = 8f,
    val spaceAfterPt: Float = 8f
) : BlockElement()

data class PageBreakBlock(
    override val id: String = UUID.randomUUID().toString()
) : BlockElement()

data class SectionBreakBlock(
    override val id: String = UUID.randomUUID().toString(),
    val isContinuous: Boolean = false
) : BlockElement()

data class TableOfContentsBlock(
    override val id: String = UUID.randomUUID().toString(),
    val title: String = "Table of Contents",
    val showPageNumbers: Boolean = true
) : BlockElement()

data class HorizontalRuleBlock(
    override val id: String = UUID.randomUUID().toString(),
    val thicknessPt: Float = 1f,
    val colorArgb: Long = 0xFFCCCCCC
) : BlockElement()

data class TextRun(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val style: TextStyleRun = TextStyleRun(),
    val revision: RevisionTag? = null
)

data class TextStyleRun(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val underline: UnderlineStyle = UnderlineStyle.None,
    val isStrikethrough: Boolean = false,
    val script: ScriptStyle = ScriptStyle.Normal,
    val fontSizePt: Float = 11f,
    val textColorArgb: Long = 0xFF1C1B1F, // Default dark text
    val highlightColorArgb: Long? = null,
    val letterSpacingPt: Float = 0f,
    val baselineShiftPt: Float = 0f,
    val fontFamily: String = "SansSerif"
)

enum class UnderlineStyle {
    None,
    Single,
    Double,
    Dotted,
    Wavy
}

enum class ScriptStyle {
    Normal,
    Superscript,
    Subscript
}

enum class TextAlignment {
    Left,
    Center,
    Right,
    Justify
}

enum class HeadingStyle(val level: Int, val defaultFontSizePt: Float, val isDefaultBold: Boolean) {
    Normal(0, 11f, false),
    Title(1, 24f, true),
    Subtitle(2, 16f, false),
    Heading1(1, 18f, true),
    Heading2(2, 14f, true),
    Heading3(3, 12f, true),
    Heading4(4, 11f, true),
    Heading5(5, 10f, true),
    Heading6(6, 9f, true)
}

enum class ListStyle {
    None,
    Bullet,
    Numbered,
    Roman,
    Alphabetical,
    Checkbox
}

data class RevisionTag(
    val type: RevisionType,
    val author: String,
    val timestamp: Long = System.currentTimeMillis(),
    val originalText: String? = null
)

enum class RevisionType {
    Insertion,
    Deletion
}

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val author: String = "Author",
    val timestamp: Long = System.currentTimeMillis(),
    val targetBlockId: String = "",
    val quoteText: String = "",
    val message: String = "",
    val replies: List<CommentReply> = emptyList(),
    val isResolved: Boolean = false
)

data class CommentReply(
    val id: String = UUID.randomUUID().toString(),
    val author: String = "Author",
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
)

data class FreehandStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint> = emptyList(),
    val colorArgb: Long = 0xFFFF0000,
    val strokeWidthPt: Float = 2f,
    val isEraser: Boolean = false
)

data class StrokePoint(
    val xPt: Float,
    val yPt: Float,
    val pressure: Float = 1.0f
)
