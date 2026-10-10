package com.example.doccraft.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.model.*
import com.example.doccraft.viewmodel.DocumentViewModel
import com.example.doccraft.viewmodel.RibbonTab
import com.example.doccraft.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RibbonToolbar(
    viewModel: DocumentViewModel,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Ribbon Tab Headers (Home, Insert, Layout, Review, View, Draw)
            ScrollableTabRow(
                selectedTabIndex = state.activeRibbonTab.ordinal,
                edgePadding = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ribbon_tab_row"),
                divider = {}
            ) {
                RibbonTab.values().forEach { tab ->
                    Tab(
                        selected = state.activeRibbonTab == tab,
                        onClick = { viewModel.setRibbonTab(tab) },
                        text = {
                            Text(
                                text = tab.name.uppercase(),
                                fontWeight = if (state.activeRibbonTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2. Active Tab Ribbon Action Items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (state.activeRibbonTab) {
                    RibbonTab.Home -> HomeTabActions(viewModel, state)
                    RibbonTab.Insert -> InsertTabActions(viewModel, onPickImage)
                    RibbonTab.Layout -> LayoutTabActions(viewModel, state)
                    RibbonTab.Review -> ReviewTabActions(viewModel, state)
                    RibbonTab.View -> ViewTabActions(viewModel, state)
                    RibbonTab.Draw -> DrawTabActions(viewModel, state)
                }
            }
        }
    }
}

@Composable
private fun HomeTabActions(viewModel: DocumentViewModel, state: com.example.doccraft.viewmodel.DocumentEditorState) {
    // Undo / Redo
    IconButton(
        onClick = { viewModel.undo() },
        enabled = state.canUndo,
        modifier = Modifier.testTag("btn_undo")
    ) {
        Icon(Icons.Default.Undo, contentDescription = "Undo")
    }

    IconButton(
        onClick = { viewModel.redo() },
        enabled = state.canRedo,
        modifier = Modifier.testTag("btn_redo")
    ) {
        Icon(Icons.Default.Redo, contentDescription = "Redo")
    }

    VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

    // Font Size Controls (- / +)
    var showFontSizeMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showFontSizeMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp).testTag("btn_font_size_menu")
        ) {
            val currentSize = 11f
            Text("${currentSize.toInt()} pt", fontSize = 12.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Font size list", modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = showFontSizeMenu,
            onDismissRequest = { showFontSizeMenu = false }
        ) {
            listOf(9f, 10f, 11f, 12f, 14f, 16f, 18f, 20f, 24f, 28f, 32f, 40f).forEach { size ->
                DropdownMenuItem(
                    text = { Text("${size.toInt()} pt") },
                    onClick = {
                        viewModel.setFontSize(size)
                        showFontSizeMenu = false
                    }
                )
            }
        }
    }

    // Font Family selector
    var showFontMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showFontMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp).testTag("btn_font_family_menu")
        ) {
            Text("Font", fontSize = 12.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = showFontMenu,
            onDismissRequest = { showFontMenu = false }
        ) {
            listOf("SansSerif", "Serif", "Monospace", "Cursive").forEach { font ->
                DropdownMenuItem(
                    text = { Text(font) },
                    onClick = {
                        viewModel.setFontFamily(font)
                        showFontMenu = false
                    }
                )
            }
        }
    }

    VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

    // Bold, Italic, Underline, Strikethrough
    IconButton(
        onClick = { viewModel.toggleBold() },
        modifier = Modifier.testTag("btn_bold")
    ) {
        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
    }

    IconButton(
        onClick = { viewModel.toggleItalic() },
        modifier = Modifier.testTag("btn_italic")
    ) {
        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
    }

    IconButton(
        onClick = { viewModel.toggleUnderline() },
        modifier = Modifier.testTag("btn_underline")
    ) {
        Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
    }

    IconButton(
        onClick = { viewModel.toggleStrikethrough() },
        modifier = Modifier.testTag("btn_strikethrough")
    ) {
        Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough")
    }

    // Text Color Picker
    var showColorMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { showColorMenu = true },
            modifier = Modifier.testTag("btn_text_color")
        ) {
            Icon(Icons.Default.FormatColorText, contentDescription = "Text Color")
        }

        DropdownMenu(
            expanded = showColorMenu,
            onDismissRequest = { showColorMenu = false }
        ) {
            Text("Text Color", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            listOf(
                0xFF1C1B1F to "Dark",
                0xFFD32F2F to "Red",
                0xFF1976D2 to "Blue",
                0xFF388E3C to "Green",
                0xFFF57C00 to "Orange",
                0xFF7B1FA2 to "Purple"
            ).forEach { (colorArgb, label) ->
                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb))
                        )
                    },
                    text = { Text(label) },
                    onClick = {
                        viewModel.setTextColor(colorArgb)
                        showColorMenu = false
                    }
                )
            }
        }
    }

    // Highlight Color Picker
    var showHlMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { showHlMenu = true },
            modifier = Modifier.testTag("btn_highlight_color")
        ) {
            Icon(Icons.Default.BorderColor, contentDescription = "Highlight")
        }

        DropdownMenu(
            expanded = showHlMenu,
            onDismissRequest = { showHlMenu = false }
        ) {
            Text("Highlight Color", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    viewModel.setHighlightColor(null)
                    showHlMenu = false
                }
            )
            listOf(
                0xFFFFF9C4 to "Yellow",
                0xFFC8E6C9 to "Light Green",
                0xFFBBDEFB to "Light Blue",
                0xFFFFCCBC to "Light Peach",
                0xFFF8BBD0 to "Light Pink"
            ).forEach { (colorArgb, label) ->
                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(colorArgb))
                        )
                    },
                    text = { Text(label) },
                    onClick = {
                        viewModel.setHighlightColor(colorArgb)
                        showHlMenu = false
                    }
                )
            }
        }
    }

    VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

    // Alignments
    IconButton(onClick = { viewModel.setAlignment(TextAlignment.Left) }) {
        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left")
    }
    IconButton(onClick = { viewModel.setAlignment(TextAlignment.Center) }) {
        Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center")
    }
    IconButton(onClick = { viewModel.setAlignment(TextAlignment.Right) }) {
        Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right")
    }
    IconButton(onClick = { viewModel.setAlignment(TextAlignment.Justify) }) {
        Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify")
    }

    VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

    // Heading styles
    var showHeadingMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showHeadingMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Styles", fontSize = 12.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = showHeadingMenu,
            onDismissRequest = { showHeadingMenu = false }
        ) {
            HeadingStyle.values().forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.name) },
                    onClick = {
                        viewModel.setHeadingStyle(style)
                        showHeadingMenu = false
                    }
                )
            }
        }
    }

    // Lists (Bullet & Numbered)
    IconButton(onClick = { viewModel.setListStyle(ListStyle.Bullet) }) {
        Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List")
    }
    IconButton(onClick = { viewModel.setListStyle(ListStyle.Numbered) }) {
        Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List")
    }

    // Line Spacing
    var showSpacingMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showSpacingMenu = true }) {
            Icon(Icons.Default.FormatLineSpacing, contentDescription = "Line Spacing")
        }

        DropdownMenu(
            expanded = showSpacingMenu,
            onDismissRequest = { showSpacingMenu = false }
        ) {
            listOf(1.0f, 1.15f, 1.5f, 2.0f).forEach { spacing ->
                DropdownMenuItem(
                    text = { Text("${spacing}x") },
                    onClick = {
                        viewModel.setLineSpacing(spacing)
                        showSpacingMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InsertTabActions(viewModel: DocumentViewModel, onPickImage: () -> Unit) {
    // Insert Table
    OutlinedButton(
        onClick = { viewModel.setShowTableInsertDialog(true) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).testTag("btn_insert_table")
    ) {
        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Table", fontSize = 12.sp)
    }

    // Insert Image
    OutlinedButton(
        onClick = onPickImage,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).testTag("btn_insert_image")
    ) {
        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Image", fontSize = 12.sp)
    }

    // Insert Math Equation (LaTeX)
    OutlinedButton(
        onClick = { viewModel.setShowEquationDialog(true) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).testTag("btn_insert_equation")
    ) {
        Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Equation", fontSize = 12.sp)
    }

    // Insert Page Break
    OutlinedButton(
        onClick = { viewModel.insertPageBreak() },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(Icons.Default.InsertPageBreak, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Page Break", fontSize = 12.sp)
    }

    // Insert Horizontal Divider
    OutlinedButton(
        onClick = { viewModel.insertHorizontalRule() },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Divider", fontSize = 12.sp)
    }

    // Insert Table of Contents
    OutlinedButton(
        onClick = { viewModel.insertTableOfContents() },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(Icons.Default.Toc, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("TOC", fontSize = 12.sp)
    }
}

@Composable
private fun LayoutTabActions(viewModel: DocumentViewModel, state: com.example.doccraft.viewmodel.DocumentEditorState) {
    // Paper Size
    var showPaperMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showPaperMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Paper Size", fontSize = 12.sp)
        }

        DropdownMenu(expanded = showPaperMenu, onDismissRequest = { showPaperMenu = false }) {
            PaperSize.values().forEach { size ->
                DropdownMenuItem(
                    text = { Text(size.displayName) },
                    onClick = {
                        viewModel.setPaperSize(size)
                        showPaperMenu = false
                    }
                )
            }
        }
    }

    // Orientation (Portrait / Landscape)
    var showOrientMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showOrientMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Orientation", fontSize = 12.sp)
        }

        DropdownMenu(expanded = showOrientMenu, onDismissRequest = { showOrientMenu = false }) {
            DropdownMenuItem(
                text = { Text("Portrait") },
                onClick = {
                    viewModel.setOrientation(Orientation.Portrait)
                    showOrientMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Landscape") },
                onClick = {
                    viewModel.setOrientation(Orientation.Landscape)
                    showOrientMenu = false
                }
            )
        }
    }

    // Margins (Normal, Narrow, Wide)
    var showMarginMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showMarginMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.SpaceBar, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Margins", fontSize = 12.sp)
        }

        DropdownMenu(expanded = showMarginMenu, onDismissRequest = { showMarginMenu = false }) {
            DropdownMenuItem(text = { Text("Normal (1 in)") }, onClick = { viewModel.setMargins(Margins.Normal); showMarginMenu = false })
            DropdownMenuItem(text = { Text("Narrow (0.5 in)") }, onClick = { viewModel.setMargins(Margins.Narrow); showMarginMenu = false })
            DropdownMenuItem(text = { Text("Moderate") }, onClick = { viewModel.setMargins(Margins.Moderate); showMarginMenu = false })
            DropdownMenuItem(text = { Text("Wide (2 in)") }, onClick = { viewModel.setMargins(Margins.Wide); showMarginMenu = false })
        }
    }

    // Columns (1, 2, 3)
    var showColMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { showColMenu = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Columns", fontSize = 12.sp)
        }

        DropdownMenu(expanded = showColMenu, onDismissRequest = { showColMenu = false }) {
            DropdownMenuItem(text = { Text("1 Column") }, onClick = { viewModel.setColumnCount(1); showColMenu = false })
            DropdownMenuItem(text = { Text("2 Columns") }, onClick = { viewModel.setColumnCount(2); showColMenu = false })
            DropdownMenuItem(text = { Text("3 Columns") }, onClick = { viewModel.setColumnCount(3); showColMenu = false })
        }
    }

    // Watermark
    OutlinedButton(
        onClick = { viewModel.setShowWatermarkDialog(true) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(Icons.Default.BrandingWatermark, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Watermark", fontSize = 12.sp)
    }
}

@Composable
private fun ReviewTabActions(viewModel: DocumentViewModel, state: com.example.doccraft.viewmodel.DocumentEditorState) {
    // Track Changes Toggle
    FilterChip(
        selected = state.isTrackingChanges,
        onClick = { viewModel.toggleTrackChanges() },
        label = { Text("Track Changes", fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                if (state.isTrackingChanges) Icons.Default.CheckCircle else Icons.Default.EditNote,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = Modifier.testTag("btn_track_changes")
    )

    // Review Revisions panel
    OutlinedButton(
        onClick = { viewModel.setShowReviewPanel(true) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).testTag("btn_review_panel")
    ) {
        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Review Panel", fontSize = 12.sp)
    }

    // Comments drawer
    OutlinedButton(
        onClick = { viewModel.setShowCommentDrawer(true) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).testTag("btn_comments")
    ) {
        Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Comments (${state.document.comments.size})", fontSize = 12.sp)
    }

    // Add Comment on selection
    IconButton(
        onClick = { viewModel.addComment("Needs review") },
        modifier = Modifier.testTag("btn_add_comment")
    ) {
        Icon(Icons.Default.AddComment, contentDescription = "Add Comment")
    }

    // Word Count & Stats
    OutlinedButton(
        onClick = { viewModel.setShowStatisticsDialog(true) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).testTag("btn_statistics")
    ) {
        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Word Count", fontSize = 12.sp)
    }
}

@Composable
private fun ViewTabActions(viewModel: DocumentViewModel, state: com.example.doccraft.viewmodel.DocumentEditorState) {
    // Print Layout vs Continuous View toggle
    FilterChip(
        selected = state.viewMode == ViewMode.PrintLayout,
        onClick = { viewModel.toggleViewMode() },
        label = {
            Text(
                if (state.viewMode == ViewMode.PrintLayout) "Print Layout" else "Mobile Continuous",
                fontSize = 12.sp
            )
        },
        leadingIcon = {
            Icon(
                if (state.viewMode == ViewMode.PrintLayout) Icons.Default.Description else Icons.Default.ViewStream,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = Modifier.testTag("btn_view_mode")
    )

    // Eye Comfort Dark Canvas Invert
    FilterChip(
        selected = state.isCanvasInverted,
        onClick = { viewModel.toggleCanvasInvert() },
        label = { Text("Dark Canvas", fontSize = 12.sp) },
        leadingIcon = {
            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        modifier = Modifier.testTag("btn_canvas_invert")
    )

    // Zoom Controls
    IconButton(onClick = { viewModel.setZoom(state.zoomPercent - 15) }) {
        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
    }

    Text("${state.zoomPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)

    IconButton(onClick = { viewModel.setZoom(state.zoomPercent + 15) }) {
        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
    }

    OutlinedButton(
        onClick = { viewModel.setZoom(100) },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text("100%", fontSize = 11.sp)
    }
}

@Composable
private fun DrawTabActions(viewModel: DocumentViewModel, state: com.example.doccraft.viewmodel.DocumentEditorState) {
    // Stylus Mode Toggle
    FilterChip(
        selected = state.isStylusMode,
        onClick = { viewModel.toggleStylusMode() },
        label = { Text("S-Pen / Stylus", fontSize = 12.sp) },
        leadingIcon = {
            Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        modifier = Modifier.testTag("btn_stylus_mode")
    )

    // Eraser Toggle
    FilterChip(
        selected = state.isEraser,
        onClick = { viewModel.toggleEraser() },
        label = { Text("Eraser", fontSize = 12.sp) },
        leadingIcon = {
            Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    )

    // Color swatches for drawing ink
    listOf(
        0xFFFF1744 to "Red",
        0xFF2979FF to "Blue",
        0xFF00E676 to "Green",
        0xFF212121 to "Black",
        0xFFFFD600 to "Yellow"
    ).forEach { (colorArgb, label) ->
        IconButton(
            onClick = { viewModel.setDrawingColor(colorArgb) },
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (state.currentDrawingColor == colorArgb && !state.isEraser) 22.dp else 16.dp)
                    .clip(CircleShape)
                    .background(Color(colorArgb))
            )
        }
    }

    // Stroke width
    listOf(1.5f, 3.0f, 6.0f).forEach { width ->
        OutlinedButton(
            onClick = { viewModel.setStrokeWidth(width) },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("${width.toInt()}pt", fontSize = 11.sp)
        }
    }

    // Clear Inks
    IconButton(onClick = { viewModel.clearAllStrokes() }) {
        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Inks")
    }
}
