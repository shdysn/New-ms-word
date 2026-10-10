package com.example.doccraft.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.engine.CalculatedPage
import com.example.doccraft.engine.MathFormulaCanvas
import com.example.doccraft.model.*
import com.example.doccraft.viewmodel.DocumentViewModel
import com.example.doccraft.viewmodel.ViewMode
import kotlin.math.max

@Composable
fun PaginatedCanvas(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val zoomFactor = state.zoomPercent / 100f

    // Eye Comfort Dark Canvas inversion: Inverts canvas background to deep dark grey and text to light
    val canvasBgColor = if (state.isCanvasInverted) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val surroundingBgColor = if (state.isCanvasInverted) Color(0xFF121212) else Color(0xFFE0E0E0)
    val defaultTextColor = if (state.isCanvasInverted) Color(0xFFE0E0E0) else Color(0xFF1C1B1F)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surroundingBgColor)
            .testTag("document_canvas_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state.viewMode == ViewMode.PrintLayout) {
            // Virtualized Lazy Column of Pages
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("virtualized_page_list"),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.calculatedPages, key = { it.pageIndex }) { page ->
                    SingleSheetPage(
                        page = page,
                        state = state,
                        viewModel = viewModel,
                        zoomFactor = zoomFactor,
                        canvasBgColor = canvasBgColor,
                        defaultTextColor = defaultTextColor
                    )
                }
            }
        } else {
            // Mobile Continuous Flow View
            ContinuousFlowView(
                state = state,
                viewModel = viewModel,
                canvasBgColor = canvasBgColor,
                defaultTextColor = defaultTextColor
            )
        }
    }
}

@Composable
private fun SingleSheetPage(
    page: CalculatedPage,
    state: com.example.doccraft.viewmodel.DocumentEditorState,
    viewModel: DocumentViewModel,
    zoomFactor: Float,
    canvasBgColor: Color,
    defaultTextColor: Color
) {
    val sheetWidthDp = (page.pageSetup.effectiveWidthPt * 0.72f * zoomFactor).dp
    val sheetHeightDp = (page.pageSetup.effectiveHeightPt * 0.72f * zoomFactor).dp

    Box(
        modifier = Modifier
            .width(sheetWidthDp)
            .height(sheetHeightDp)
            .shadow(6.dp, RoundedCornerShape(2.dp))
            .background(canvasBgColor, RoundedCornerShape(2.dp))
            .border(0.5.dp, Color(0xFFBDBDBD), RoundedCornerShape(2.dp))
            .padding(
                top = (page.pageSetup.margins.topPt * 0.6f * zoomFactor).dp,
                bottom = (page.pageSetup.margins.bottomPt * 0.6f * zoomFactor).dp,
                start = (page.pageSetup.margins.leftPt * 0.6f * zoomFactor).dp,
                end = (page.pageSetup.margins.rightPt * 0.6f * zoomFactor).dp
            )
    ) {
        // Watermark layer
        page.watermark?.let { wm ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = wm.text,
                    fontSize = (wm.fontSizePt * zoomFactor * 0.8f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(wm.colorArgb).copy(alpha = wm.opacity),
                    modifier = Modifier.rotate(wm.angleDegrees)
                )
            }
        }

        // Header
        if (page.headerText.isNotEmpty()) {
            Text(
                text = page.headerText,
                fontSize = (9f * zoomFactor).sp,
                color = defaultTextColor.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // Footer
        if (page.footerText.isNotEmpty()) {
            Text(
                text = page.footerText,
                fontSize = (9f * zoomFactor).sp,
                color = defaultTextColor.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = (24f * zoomFactor).dp),
            verticalArrangement = Arrangement.spacedBy((6f * zoomFactor).dp)
        ) {
            for (item in page.items) {
                PageItemView(
                    item = item,
                    document = state.document,
                    activeBlockId = state.activeBlockId,
                    onSelectBlock = { viewModel.setActiveBlock(item.block.id) },
                    onUpdateText = { newText -> viewModel.updateParagraphText(item.block.id, newText) },
                    onUpdateCell = { r, c, txt -> viewModel.updateTableCell(item.block.id, r, c, txt) },
                    defaultTextColor = defaultTextColor,
                    zoomFactor = zoomFactor
                )
            }
        }

        // Stylus / Freehand Inking Overlay
        FreehandInkingLayer(
            strokes = state.document.freehandStrokes,
            isStylusMode = state.isStylusMode,
            currentColor = state.currentDrawingColor,
            strokeWidth = state.currentStrokeWidthPt,
            isEraser = state.isEraser,
            onAddStroke = { viewModel.addFreehandStroke(it) }
        )
    }
}

@Composable
private fun ContinuousFlowView(
    state: com.example.doccraft.viewmodel.DocumentEditorState,
    viewModel: DocumentViewModel,
    canvasBgColor: Color,
    defaultTextColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(canvasBgColor, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = state.document.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = defaultTextColor
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        items(state.document.allBlocks(), key = { it.id }) { block ->
            BlockElementRow(
                block = block,
                document = state.document,
                isSelected = state.activeBlockId == block.id,
                onSelect = { viewModel.setActiveBlock(block.id) },
                onUpdateText = { txt -> viewModel.updateParagraphText(block.id, txt) },
                onUpdateCell = { r, c, txt -> viewModel.updateTableCell(block.id, r, c, txt) },
                onDelete = { viewModel.deleteBlock(block.id) },
                defaultTextColor = defaultTextColor,
                zoomFactor = 1.0f
            )
        }

        item {
            OutlinedButton(
                onClick = { viewModel.insertParagraph() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("btn_append_paragraph")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Paragraph")
            }
        }
    }
}

@Composable
private fun PageItemView(
    item: com.example.doccraft.engine.PageItem,
    document: Document,
    activeBlockId: String?,
    onSelectBlock: () -> Unit,
    onUpdateText: (String) -> Unit,
    onUpdateCell: (Int, Int, String) -> Unit,
    defaultTextColor: Color,
    zoomFactor: Float
) {
    val block = item.block
    val isSelected = activeBlockId == block.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectBlock() }
            .then(
                if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                else Modifier
            )
            .padding(2.dp)
    ) {
        when (block) {
            is ParagraphBlock -> {
                ParagraphBlockView(block, onUpdateText, isSelected, defaultTextColor, zoomFactor)
            }
            is TableBlock -> {
                TableBlockView(block, onUpdateCell, defaultTextColor, zoomFactor)
            }
            is ImageBlock -> {
                ImageBlockPlaceholderView(block, defaultTextColor, zoomFactor)
            }
            is EquationBlock -> {
                MathFormulaCanvas(
                    latex = block.latex,
                    fontSizePt = block.fontSizePt * zoomFactor,
                    textColor = defaultTextColor
                )
            }
            is HorizontalRuleBlock -> {
                HorizontalDivider(
                    thickness = (block.thicknessPt * zoomFactor).dp,
                    color = Color(block.colorArgb)
                )
            }
            is TableOfContentsBlock -> {
                TableOfContentsView(document, defaultTextColor, zoomFactor)
            }
            else -> {}
        }
    }
}

@Composable
private fun BlockElementRow(
    block: BlockElement,
    document: Document,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdateText: (String) -> Unit,
    onUpdateCell: (Int, Int, String) -> Unit,
    onDelete: () -> Unit,
    defaultTextColor: Color,
    zoomFactor: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            when (block) {
                is ParagraphBlock -> ParagraphBlockView(block, onUpdateText, isSelected, defaultTextColor, zoomFactor)
                is TableBlock -> TableBlockView(block, onUpdateCell, defaultTextColor, zoomFactor)
                is ImageBlock -> ImageBlockPlaceholderView(block, defaultTextColor, zoomFactor)
                is EquationBlock -> MathFormulaCanvas(latex = block.latex, fontSizePt = block.fontSizePt, textColor = defaultTextColor)
                is HorizontalRuleBlock -> HorizontalDivider(thickness = block.thicknessPt.dp, color = Color(block.colorArgb))
                is TableOfContentsBlock -> TableOfContentsView(document, defaultTextColor, zoomFactor)
                else -> {}
            }
        }
    }
}

@Composable
private fun ParagraphBlockView(
    block: ParagraphBlock,
    onUpdateText: (String) -> Unit,
    isSelected: Boolean,
    defaultTextColor: Color,
    zoomFactor: Float
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(block.plainText) { mutableStateOf(block.plainText) }

    val textAlign = when (block.alignment) {
        TextAlignment.Left -> TextAlign.Start
        TextAlignment.Center -> TextAlign.Center
        TextAlignment.Right -> TextAlign.End
        TextAlignment.Justify -> TextAlign.Justify
    }

    if (isEditing) {
        OutlinedTextField(
            value = editText,
            onValueChange = {
                editText = it
                onUpdateText(it)
            },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { isEditing = false }) {
                    Icon(Icons.Default.Edit, contentDescription = "Done Editing")
                }
            }
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isEditing = true },
            verticalAlignment = Alignment.Top
        ) {
            // Bullet / Number icon if list
            if (block.listStyle != ListStyle.None) {
                val bullet = when (block.listStyle) {
                    ListStyle.Bullet -> "• "
                    ListStyle.Numbered -> "1. "
                    ListStyle.Roman -> "I. "
                    ListStyle.Alphabetical -> "a. "
                    ListStyle.Checkbox -> "☐ "
                    else -> ""
                }
                Text(
                    text = bullet,
                    fontWeight = FontWeight.Bold,
                    color = defaultTextColor,
                    fontSize = (11f * zoomFactor).sp
                )
            }

            val annotatedString = buildAnnotatedString {
                for (run in block.runs) {
                    val isHeading = block.headingStyle != HeadingStyle.Normal
                    val fs = if (isHeading) block.headingStyle.defaultFontSizePt else run.style.fontSizePt
                    val isBold = run.style.isBold || block.headingStyle.isDefaultBold

                    val decoration = when {
                        run.revision?.type == RevisionType.Deletion -> TextDecoration.LineThrough
                        run.revision?.type == RevisionType.Insertion -> TextDecoration.Underline
                        run.style.isStrikethrough && run.style.underline != UnderlineStyle.None ->
                            TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
                        run.style.isStrikethrough -> TextDecoration.LineThrough
                        run.style.underline != UnderlineStyle.None -> TextDecoration.Underline
                        else -> TextDecoration.None
                    }

                    val color = when {
                        run.revision?.type == RevisionType.Deletion -> Color(0xFFD32F2F)
                        run.revision?.type == RevisionType.Insertion -> Color(0xFF2E7D32)
                        run.style.textColorArgb != 0xFF1C1B1F -> Color(run.style.textColorArgb)
                        else -> defaultTextColor
                    }

                    val fFamily = when (run.style.fontFamily) {
                        "Serif" -> FontFamily.Serif
                        "Monospace" -> FontFamily.Monospace
                        "Cursive" -> FontFamily.Cursive
                        else -> FontFamily.Default
                    }

                    val bg = run.style.highlightColorArgb?.let { Color(it) } ?: Color.Transparent

                    withStyle(
                        SpanStyle(
                            fontSize = (fs * zoomFactor).sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (run.style.isItalic) FontStyle.Italic else FontStyle.Normal,
                            fontFamily = fFamily,
                            textDecoration = decoration,
                            color = color,
                            background = bg
                        )
                    ) {
                        append(run.text.ifEmpty { " " })
                    }
                }
            }

            Text(
                text = annotatedString,
                textAlign = textAlign,
                lineHeight = (14f * block.lineSpacingMultiplier * zoomFactor).sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TableBlockView(
    block: TableBlock,
    onUpdateCell: (Int, Int, String) -> Unit,
    defaultTextColor: Color,
    zoomFactor: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border((block.borderWidthPt * zoomFactor).dp, Color(block.borderColorArgb), RoundedCornerShape(2.dp))
    ) {
        block.rows.forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (rowIdx < block.rows.size - 1)
                            Modifier.border(0.5.dp, Color(block.borderColorArgb).copy(alpha = 0.5f))
                        else Modifier
                    )
            ) {
                row.cells.forEachIndexed { colIdx, cell ->
                    var isEditingCell by remember { mutableStateOf(false) }
                    var cellText by remember(cell.text) { mutableStateOf(cell.text) }

                    val cellBg = cell.backgroundColorArgb?.let { Color(it) }
                        ?: if (cell.isHeader) Color(0xFFECEFF1) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f * cell.colSpan)
                            .background(cellBg)
                            .border(0.5.dp, Color(block.borderColorArgb).copy(alpha = 0.3f))
                            .clickable { isEditingCell = true }
                            .padding((4f * zoomFactor).dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isEditingCell) {
                            TextField(
                                value = cellText,
                                onValueChange = {
                                    cellText = it
                                    onUpdateCell(rowIdx, colIdx, it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Text(
                                text = cell.text,
                                fontSize = (9.5f * zoomFactor).sp,
                                fontWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = defaultTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageBlockPlaceholderView(block: ImageBlock, defaultTextColor: Color, zoomFactor: Float) {
    Box(
        modifier = Modifier
            .width((block.widthPt * 0.72f * zoomFactor).dp)
            .height((block.heightPt * 0.72f * zoomFactor).dp)
            .background(Color(0xFFECEFF1), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "[ Embedded Document Image ]",
                fontSize = (11f * zoomFactor).sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            if (block.caption.isNotEmpty()) {
                Text(
                    text = block.caption,
                    fontSize = (9f * zoomFactor).sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
private fun TableOfContentsView(document: Document, defaultTextColor: Color, zoomFactor: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Table of Contents",
                fontSize = (14f * zoomFactor).sp,
                fontWeight = FontWeight.Bold,
                color = defaultTextColor
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val headings = document.allBlocks()
                .filterIsInstance<ParagraphBlock>()
                .filter { it.headingStyle != HeadingStyle.Normal }

            if (headings.isEmpty()) {
                Text(
                    text = "Add headings (H1, H2, Title) to generate dynamic TOC entries.",
                    fontSize = (10f * zoomFactor).sp,
                    color = Color.Gray
                )
            } else {
                headings.forEachIndexed { idx, h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${h.headingStyle.name}: ${h.plainText.take(40)}",
                            fontSize = (10f * zoomFactor).sp,
                            color = defaultTextColor
                        )
                        Text(
                            text = "p. ${idx + 1}",
                            fontSize = (10f * zoomFactor).sp,
                            fontWeight = FontWeight.Bold,
                            color = defaultTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreehandInkingLayer(
    strokes: List<FreehandStroke>,
    isStylusMode: Boolean,
    currentColor: Long,
    strokeWidth: Float,
    isEraser: Boolean,
    onAddStroke: (FreehandStroke) -> Unit
) {
    var activePoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isStylusMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints = listOf(StrokePoint(offset.x, offset.y, 1f))
                            },
                            onDrag = { change, _ ->
                                val pt = StrokePoint(change.position.x, change.position.y, 1f)
                                activePoints = activePoints + pt
                            },
                            onDragEnd = {
                                if (activePoints.isNotEmpty()) {
                                    onAddStroke(
                                        FreehandStroke(
                                            points = activePoints,
                                            colorArgb = currentColor,
                                            strokeWidthPt = strokeWidth,
                                            isEraser = isEraser
                                        )
                                    )
                                    activePoints = emptyList()
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // Draw existing strokes
        for (stroke in strokes) {
            if (stroke.points.size > 1) {
                val path = Path()
                path.moveTo(stroke.points[0].xPt, stroke.points[0].yPt)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].xPt, stroke.points[i].yPt)
                }
                drawPath(
                    path = path,
                    color = if (stroke.isEraser) Color.White else Color(stroke.colorArgb),
                    style = Stroke(width = stroke.strokeWidthPt * 2f)
                )
            }
        }

        // Draw current active in-flight stroke
        if (activePoints.size > 1) {
            val path = Path()
            path.moveTo(activePoints[0].xPt, activePoints[0].yPt)
            for (i in 1 until activePoints.size) {
                path.lineTo(activePoints[i].xPt, activePoints[i].yPt)
            }
            drawPath(
                path = path,
                color = if (isEraser) Color.White else Color(currentColor),
                style = Stroke(width = strokeWidth * 2f)
            )
        }
    }
}
