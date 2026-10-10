package com.example.doccraft.viewmodel

import android.app.Application
import android.content.Context
import android.print.PrintManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.doccraft.data.DocCraftDatabase
import com.example.doccraft.data.DocumentEntity
import com.example.doccraft.data.DocumentRepository
import com.example.doccraft.engine.CalculatedPage
import com.example.doccraft.engine.PaginationEngine
import com.example.doccraft.io.DocxPipeline
import com.example.doccraft.io.DocumentPrintAdapter
import com.example.doccraft.io.PdfExporter
import com.example.doccraft.model.*
import com.example.doccraft.templates.DocumentTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class RibbonTab {
    Home,
    Insert,
    Layout,
    Review,
    View,
    Draw
}

enum class ViewMode {
    PrintLayout,
    ContinuousFlow
}

data class DocumentTelemetry(
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val paragraphCount: Int = 0,
    val pageCount: Int = 1,
    val readingTimeMinutes: Int = 1
)

data class SearchMatch(
    val blockId: String,
    val runIndex: Int,
    val startIndex: Int,
    val endIndex: Int,
    val matchedText: String
)

data class DocumentEditorState(
    val document: Document = Document(),
    val activeBlockId: String? = null,
    val activeSectionIndex: Int = 0,
    val activeRibbonTab: RibbonTab = RibbonTab.Home,
    val viewMode: ViewMode = ViewMode.PrintLayout,
    val isCanvasInverted: Boolean = false,
    val zoomPercent: Int = 100,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isTrackingChanges: Boolean = false,
    val activeAuthor: String = "Author",
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchResults: List<SearchMatch> = emptyList(),
    val currentSearchMatchIndex: Int = 0,
    val isSearchOpen: Boolean = false,
    val matchCase: Boolean = false,
    val matchWholeWord: Boolean = false,
    val useRegex: Boolean = false,
    val isStylusMode: Boolean = false,
    val currentDrawingColor: Long = 0xFFFF1744, // Vibrant Red
    val currentStrokeWidthPt: Float = 2.5f,
    val isEraser: Boolean = false,
    val calculatedPages: List<CalculatedPage> = emptyList(),
    val telemetry: DocumentTelemetry = DocumentTelemetry(),
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val isReadOnly: Boolean = false,
    val showTableInsertDialog: Boolean = false,
    val showEquationDialog: Boolean = false,
    val showWatermarkDialog: Boolean = false,
    val showPageSetupDialog: Boolean = false,
    val showDocumentManagerDialog: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val showStatisticsDialog: Boolean = false,
    val showCommentDrawer: Boolean = false,
    val showReviewPanel: Boolean = false
)

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    private val undoStack = mutableListOf<Document>()
    private val redoStack = mutableListOf<Document>()
    private val maxUndoHistory = 40

    private val _uiState = MutableStateFlow(DocumentEditorState())
    val uiState: StateFlow<DocumentEditorState> = _uiState.asStateFlow()

    val savedDocuments: StateFlow<List<DocumentEntity>>

    init {
        val database = DocCraftDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao())
        savedDocuments = repository.allDocuments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Initial setup: Seed templates if first launch, then load resume or default
        viewModelScope.launch {
            repository.allDocuments.firstOrNull()?.let { list ->
                if (list.isEmpty()) {
                    repository.seedInitialTemplatesIfEmpty()
                }
            }
            // Load first document or starter template
            val initialDoc = DocumentTemplates.createResumeTemplate()
            loadDocument(initialDoc, saveInitial = false)
        }
    }

    private fun pushUndoSnapshot() {
        val current = _uiState.value.document
        if (undoStack.size >= maxUndoHistory) {
            undoStack.removeAt(0)
        }
        undoStack.add(current)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(_uiState.value.document)
        applyDocumentChange(previous, recordUndo = false)
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = true) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(_uiState.value.document)
        applyDocumentChange(next, recordUndo = false)
        _uiState.update { it.copy(canUndo = true, canRedo = redoStack.isNotEmpty()) }
    }

    private fun applyDocumentChange(newDoc: Document, recordUndo: Boolean = true) {
        if (recordUndo) {
            pushUndoSnapshot()
        }
        _uiState.update { it.copy(document = newDoc) }
        recalculatePaginationAndTelemetry(newDoc)
    }

    private fun recalculatePaginationAndTelemetry(document: Document) {
        viewModelScope.launch(Dispatchers.Default) {
            val pages = PaginationEngine.paginate(document)

            var words = 0
            var chars = 0
            var paragraphs = 0

            for (sec in document.sections) {
                for (b in sec.blocks) {
                    if (b is ParagraphBlock) {
                        paragraphs++
                        val pText = b.plainText
                        chars += pText.length
                        val pWords = pText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                        words += pWords
                    } else if (b is TableBlock) {
                        for (r in b.rows) {
                            for (c in r.cells) {
                                chars += c.text.length
                                words += c.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                            }
                        }
                    }
                }
            }

            val readTime = kotlin.math.max(1, (words / 200.0).toInt())
            val tele = DocumentTelemetry(
                wordCount = words,
                charCount = chars,
                paragraphCount = paragraphs,
                pageCount = kotlin.math.max(1, pages.size),
                readingTimeMinutes = readTime
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        calculatedPages = pages,
                        telemetry = tele
                    )
                }
            }
        }
    }

    // --- Tab and Navigation ---
    fun setRibbonTab(tab: RibbonTab) {
        _uiState.update { it.copy(activeRibbonTab = tab) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = if (it.viewMode == ViewMode.PrintLayout) ViewMode.ContinuousFlow else ViewMode.PrintLayout
            )
        }
    }

    fun toggleCanvasInvert() {
        _uiState.update { it.copy(isCanvasInverted = !it.isCanvasInverted) }
    }

    fun setZoom(percent: Int) {
        _uiState.update { it.copy(zoomPercent = percent.coerceIn(50, 200)) }
    }

    fun setActiveBlock(blockId: String?) {
        _uiState.update { it.copy(activeBlockId = blockId) }
    }

    // --- Text Editing & Formatting ---
    fun updateParagraphText(blockId: String, newText: String) {
        val currentDoc = _uiState.value.document
        val tracking = _uiState.value.isTrackingChanges
        val author = _uiState.value.activeAuthor

        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == blockId && block is ParagraphBlock) {
                    val existingStyle = block.runs.firstOrNull()?.style ?: TextStyleRun()
                    val revision = if (tracking) RevisionTag(RevisionType.Insertion, author) else null
                    block.copy(
                        runs = listOf(TextRun(text = newText, style = existingStyle, revision = revision))
                    )
                } else block
            }
            section.copy(blocks = newBlocks)
        }

        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun updateDocumentTitle(newTitle: String) {
        val currentDoc = _uiState.value.document
        applyDocumentChange(currentDoc.copy(title = newTitle))
    }

    fun toggleBold() {
        formatActiveParagraph { style -> style.copy(isBold = !style.isBold) }
    }

    fun toggleItalic() {
        formatActiveParagraph { style -> style.copy(isItalic = !style.isItalic) }
    }

    fun toggleUnderline(underlineStyle: UnderlineStyle = UnderlineStyle.Single) {
        formatActiveParagraph { style ->
            val next = if (style.underline != UnderlineStyle.None) UnderlineStyle.None else underlineStyle
            style.copy(underline = next)
        }
    }

    fun toggleStrikethrough() {
        formatActiveParagraph { style -> style.copy(isStrikethrough = !style.isStrikethrough) }
    }

    fun toggleScript(script: ScriptStyle) {
        formatActiveParagraph { style ->
            val next = if (style.script == script) ScriptStyle.Normal else script
            style.copy(script = next)
        }
    }

    fun setFontSize(sizePt: Float) {
        formatActiveParagraph { style -> style.copy(fontSizePt = sizePt) }
    }

    fun setTextColor(colorArgb: Long) {
        formatActiveParagraph { style -> style.copy(textColorArgb = colorArgb) }
    }

    fun setHighlightColor(colorArgb: Long?) {
        formatActiveParagraph { style -> style.copy(highlightColorArgb = colorArgb) }
    }

    fun setFontFamily(family: String) {
        formatActiveParagraph { style -> style.copy(fontFamily = family) }
    }

    fun setAlignment(alignment: TextAlignment) {
        val currentDoc = _uiState.value.document
        val targetId = _uiState.value.activeBlockId ?: currentDoc.sections.firstOrNull()?.blocks?.firstOrNull()?.id ?: return

        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == targetId && block is ParagraphBlock) {
                    block.copy(alignment = alignment)
                } else block
            }
            section.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setLineSpacing(multiplier: Float) {
        val currentDoc = _uiState.value.document
        val targetId = _uiState.value.activeBlockId ?: return
        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == targetId && block is ParagraphBlock) {
                    block.copy(lineSpacingMultiplier = multiplier)
                } else block
            }
            section.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setHeadingStyle(heading: HeadingStyle) {
        val currentDoc = _uiState.value.document
        val targetId = _uiState.value.activeBlockId ?: return
        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == targetId && block is ParagraphBlock) {
                    val updatedRuns = block.runs.map { run ->
                        run.copy(
                            style = run.style.copy(
                                fontSizePt = heading.defaultFontSizePt,
                                isBold = heading.isDefaultBold
                            )
                        )
                    }
                    block.copy(headingStyle = heading, runs = updatedRuns)
                } else block
            }
            section.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setListStyle(listStyle: ListStyle) {
        val currentDoc = _uiState.value.document
        val targetId = _uiState.value.activeBlockId ?: return
        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == targetId && block is ParagraphBlock) {
                    val nextStyle = if (block.listStyle == listStyle) ListStyle.None else listStyle
                    block.copy(listStyle = nextStyle)
                } else block
            }
            section.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    private fun formatActiveParagraph(transformStyle: (TextStyleRun) -> TextStyleRun) {
        val currentDoc = _uiState.value.document
        val targetId = _uiState.value.activeBlockId ?: currentDoc.sections.firstOrNull()?.blocks?.firstOrNull()?.id ?: return

        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == targetId && block is ParagraphBlock) {
                    val updatedRuns = block.runs.map { run ->
                        run.copy(style = transformStyle(run.style))
                    }
                    block.copy(runs = updatedRuns)
                } else block
            }
            section.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    // --- Block Insertion ---
    fun insertParagraph(afterBlockId: String? = null) {
        val currentDoc = _uiState.value.document
        val newBlock = ParagraphBlock(runs = listOf(TextRun(text = "")))
        insertBlockInternal(newBlock, afterBlockId)
        _uiState.update { it.copy(activeBlockId = newBlock.id) }
    }

    fun insertTable(rows: Int, cols: Int) {
        val tableRows = (0 until rows).map { r ->
            TableRow(
                cells = (0 until cols).map { c ->
                    TableCell(
                        text = if (r == 0) "Column ${c + 1}" else "Data",
                        isHeader = (r == 0),
                        backgroundColorArgb = if (r == 0) 0xFFEEEEEE else null
                    )
                }
            )
        }
        val tableBlock = TableBlock(rows = tableRows)
        insertBlockInternal(tableBlock)
        _uiState.update { it.copy(showTableInsertDialog = false, activeBlockId = tableBlock.id) }
    }

    fun updateTableCell(tableBlockId: String, rowIdx: Int, colIdx: Int, newText: String) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { section ->
            val newBlocks = section.blocks.map { block ->
                if (block.id == tableBlockId && block is TableBlock) {
                    val updatedRows = block.rows.mapIndexed { r, row ->
                        if (r == rowIdx) {
                            val updatedCells = row.cells.mapIndexed { c, cell ->
                                if (c == colIdx) cell.copy(text = newText) else cell
                            }
                            row.copy(cells = updatedCells)
                        } else row
                    }
                    block.copy(rows = updatedRows)
                } else block
            }
            section.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun insertEquation(latex: String) {
        val eqBlock = EquationBlock(latex = latex)
        insertBlockInternal(eqBlock)
        _uiState.update { it.copy(showEquationDialog = false, activeBlockId = eqBlock.id) }
    }

    fun insertImage(uri: String) {
        val imgBlock = ImageBlock(uri = uri)
        insertBlockInternal(imgBlock)
        _uiState.update { it.copy(activeBlockId = imgBlock.id) }
    }

    fun insertPageBreak() {
        insertBlockInternal(PageBreakBlock())
    }

    fun insertHorizontalRule() {
        insertBlockInternal(HorizontalRuleBlock())
    }

    fun insertTableOfContents() {
        insertBlockInternal(TableOfContentsBlock())
    }

    private fun insertBlockInternal(newBlock: BlockElement, afterBlockId: String? = null) {
        val currentDoc = _uiState.value.document
        val secIndex = _uiState.value.activeSectionIndex.coerceIn(0, currentDoc.sections.lastIndex)
        val section = currentDoc.sections[secIndex]

        val targetIndex = if (afterBlockId != null) {
            val idx = section.blocks.indexOfFirst { it.id == afterBlockId }
            if (idx != -1) idx + 1 else section.blocks.size
        } else {
            val activeIdx = section.blocks.indexOfFirst { it.id == _uiState.value.activeBlockId }
            if (activeIdx != -1) activeIdx + 1 else section.blocks.size
        }

        val updatedBlocks = section.blocks.toMutableList().apply {
            add(targetIndex, newBlock)
        }

        val updatedSections = currentDoc.sections.toMutableList().apply {
            set(secIndex, section.copy(blocks = updatedBlocks))
        }

        applyDocumentChange(currentDoc.copy(sections = updatedSections))
    }

    fun deleteBlock(blockId: String) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { section ->
            val filtered = section.blocks.filter { it.id != blockId }
            section.copy(blocks = if (filtered.isEmpty()) listOf(ParagraphBlock()) else filtered)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
        if (_uiState.value.activeBlockId == blockId) {
            _uiState.update { it.copy(activeBlockId = null) }
        }
    }

    // --- Page Setup & Layout ---
    fun setPaperSize(size: PaperSize) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            sec.copy(pageSetup = sec.pageSetup.copy(paperSize = size))
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setOrientation(orientation: Orientation) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            sec.copy(pageSetup = sec.pageSetup.copy(orientation = orientation))
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setMargins(margins: Margins) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            sec.copy(pageSetup = sec.pageSetup.copy(margins = margins))
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setColumnCount(columns: Int) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            sec.copy(pageSetup = sec.pageSetup.copy(columnCount = columns))
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun setWatermark(text: String?) {
        val currentDoc = _uiState.value.document
        val wm = if (text.isNullOrBlank()) null else Watermark(text = text.trim())
        applyDocumentChange(currentDoc.copy(settings = currentDoc.settings.copy(watermark = wm)))
    }

    // --- Review, Track Changes & Comments ---
    fun toggleTrackChanges() {
        _uiState.update { it.copy(isTrackingChanges = !it.isTrackingChanges) }
    }

    fun setAuthor(author: String) {
        _uiState.update { it.copy(activeAuthor = author.ifBlank { "Author" }) }
    }

    fun acceptRevision(blockId: String, runId: String) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            val newBlocks = sec.blocks.map { b ->
                if (b.id == blockId && b is ParagraphBlock) {
                    val updatedRuns = b.runs.mapNotNull { r ->
                        if (r.id == runId) {
                            if (r.revision?.type == RevisionType.Deletion) null
                            else r.copy(revision = null)
                        } else r
                    }
                    b.copy(runs = if (updatedRuns.isEmpty()) listOf(TextRun()) else updatedRuns)
                } else b
            }
            sec.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun rejectRevision(blockId: String, runId: String) {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            val newBlocks = sec.blocks.map { b ->
                if (b.id == blockId && b is ParagraphBlock) {
                    val updatedRuns = b.runs.mapNotNull { r ->
                        if (r.id == runId) {
                            if (r.revision?.type == RevisionType.Insertion) null
                            else r.copy(
                                text = r.revision?.originalText ?: r.text,
                                revision = null,
                                style = r.style.copy(isStrikethrough = false)
                            )
                        } else r
                    }
                    b.copy(runs = if (updatedRuns.isEmpty()) listOf(TextRun()) else updatedRuns)
                } else b
            }
            sec.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun acceptAllRevisions() {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            val newBlocks = sec.blocks.map { b ->
                if (b is ParagraphBlock) {
                    val updatedRuns = b.runs.mapNotNull { r ->
                        if (r.revision?.type == RevisionType.Deletion) null
                        else r.copy(revision = null)
                    }
                    b.copy(runs = if (updatedRuns.isEmpty()) listOf(TextRun()) else updatedRuns)
                } else b
            }
            sec.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun rejectAllRevisions() {
        val currentDoc = _uiState.value.document
        val newSections = currentDoc.sections.map { sec ->
            val newBlocks = sec.blocks.map { b ->
                if (b is ParagraphBlock) {
                    val updatedRuns = b.runs.mapNotNull { r ->
                        if (r.revision?.type == RevisionType.Insertion) null
                        else r.copy(
                            text = r.revision?.originalText ?: r.text,
                            revision = null,
                            style = r.style.copy(isStrikethrough = false)
                        )
                    }
                    b.copy(runs = if (updatedRuns.isEmpty()) listOf(TextRun()) else updatedRuns)
                } else b
            }
            sec.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
    }

    fun addComment(message: String) {
        val blockId = _uiState.value.activeBlockId ?: return
        val currentDoc = _uiState.value.document
        val block = currentDoc.allBlocks().find { it.id == blockId }
        val quote = if (block is ParagraphBlock) block.plainText.take(40) else "Selection"

        val comment = Comment(
            author = _uiState.value.activeAuthor,
            targetBlockId = blockId,
            quoteText = quote,
            message = message
        )
        val updatedComments = currentDoc.comments + comment
        applyDocumentChange(currentDoc.copy(comments = updatedComments))
    }

    fun replyComment(commentId: String, replyMsg: String) {
        val currentDoc = _uiState.value.document
        val updatedComments = currentDoc.comments.map { c ->
            if (c.id == commentId) {
                c.copy(
                    replies = c.replies + CommentReply(
                        author = _uiState.value.activeAuthor,
                        message = replyMsg
                    )
                )
            } else c
        }
        applyDocumentChange(currentDoc.copy(comments = updatedComments))
    }

    fun resolveComment(commentId: String) {
        val currentDoc = _uiState.value.document
        val updatedComments = currentDoc.comments.map { c ->
            if (c.id == commentId) c.copy(isResolved = !c.isResolved) else c
        }
        applyDocumentChange(currentDoc.copy(comments = updatedComments))
    }

    fun deleteComment(commentId: String) {
        val currentDoc = _uiState.value.document
        val updatedComments = currentDoc.comments.filter { it.id != commentId }
        applyDocumentChange(currentDoc.copy(comments = updatedComments))
    }

    // --- Stylus & Freehand Inking ---
    fun toggleStylusMode() {
        _uiState.update { it.copy(isStylusMode = !it.isStylusMode) }
    }

    fun setDrawingColor(color: Long) {
        _uiState.update { it.copy(currentDrawingColor = color, isEraser = false) }
    }

    fun setStrokeWidth(widthPt: Float) {
        _uiState.update { it.copy(currentStrokeWidthPt = widthPt) }
    }

    fun toggleEraser() {
        _uiState.update { it.copy(isEraser = !it.isEraser) }
    }

    fun addFreehandStroke(stroke: FreehandStroke) {
        val currentDoc = _uiState.value.document
        val updated = currentDoc.freehandStrokes + stroke
        applyDocumentChange(currentDoc.copy(freehandStrokes = updated), recordUndo = false)
    }

    fun clearAllStrokes() {
        val currentDoc = _uiState.value.document
        applyDocumentChange(currentDoc.copy(freehandStrokes = emptyList()))
    }

    // --- Search & Replace Engine ---
    fun openSearch() {
        _uiState.update { it.copy(isSearchOpen = true) }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchOpen = false, searchResults = emptyList(), searchQuery = "", replaceQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        performSearch()
    }

    fun setReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun toggleMatchCase() {
        _uiState.update { it.copy(matchCase = !it.matchCase) }
        performSearch()
    }

    fun toggleMatchWholeWord() {
        _uiState.update { it.copy(matchWholeWord = !it.matchWholeWord) }
        performSearch()
    }

    fun toggleUseRegex() {
        _uiState.update { it.copy(useRegex = !it.useRegex) }
        performSearch()
    }

    private fun performSearch() {
        val state = _uiState.value
        val query = state.searchQuery
        if (query.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList(), currentSearchMatchIndex = 0) }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val results = mutableListOf<SearchMatch>()
            val pattern = try {
                if (state.useRegex) {
                    val flags = if (state.matchCase) setOf() else setOf(RegexOption.IGNORE_CASE)
                    Regex(query, flags)
                } else {
                    val escaped = Regex.escape(query)
                    val fullPattern = if (state.matchWholeWord) "\\b$escaped\\b" else escaped
                    val flags = if (state.matchCase) setOf() else setOf(RegexOption.IGNORE_CASE)
                    Regex(fullPattern, flags)
                }
            } catch (e: Exception) {
                null
            }

            if (pattern != null) {
                for (sec in state.document.sections) {
                    for (block in sec.blocks) {
                        if (block is ParagraphBlock) {
                            for ((runIdx, run) in block.runs.withIndex()) {
                                pattern.findAll(run.text).forEach { match ->
                                    results.add(
                                        SearchMatch(
                                            blockId = block.id,
                                            runIndex = runIdx,
                                            startIndex = match.range.first,
                                            endIndex = match.range.last + 1,
                                            matchedText = match.value
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        currentSearchMatchIndex = if (results.isNotEmpty()) 0 else 0
                    )
                }
            }
        }
    }

    fun nextSearchMatch() {
        val results = _uiState.value.searchResults
        if (results.isEmpty()) return
        val nextIdx = (_uiState.value.currentSearchMatchIndex + 1) % results.size
        _uiState.update { it.copy(currentSearchMatchIndex = nextIdx) }
    }

    fun prevSearchMatch() {
        val results = _uiState.value.searchResults
        if (results.isEmpty()) return
        val prevIdx = if (_uiState.value.currentSearchMatchIndex - 1 < 0) results.size - 1 else _uiState.value.currentSearchMatchIndex - 1
        _uiState.update { it.copy(currentSearchMatchIndex = prevIdx) }
    }

    fun replaceCurrentMatch() {
        val state = _uiState.value
        val results = state.searchResults
        if (results.isEmpty()) return
        val currentMatch = results.getOrNull(state.currentSearchMatchIndex) ?: return

        val currentDoc = state.document
        val newSections = currentDoc.sections.map { sec ->
            val newBlocks = sec.blocks.map { b ->
                if (b.id == currentMatch.blockId && b is ParagraphBlock) {
                    val updatedRuns = b.runs.mapIndexed { rIdx, run ->
                        if (rIdx == currentMatch.runIndex) {
                            val original = run.text
                            val replaced = original.substring(0, currentMatch.startIndex) +
                                    state.replaceQuery +
                                    original.substring(currentMatch.endIndex)
                            run.copy(text = replaced)
                        } else run
                    }
                    b.copy(runs = updatedRuns)
                } else b
            }
            sec.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
        performSearch()
    }

    fun replaceAllMatches() {
        val state = _uiState.value
        val query = state.searchQuery
        val replaceWith = state.replaceQuery
        if (query.isEmpty()) return

        val currentDoc = state.document
        val pattern = try {
            val escaped = if (state.useRegex) query else Regex.escape(query)
            val fullPattern = if (state.matchWholeWord) "\\b$escaped\\b" else escaped
            val flags = if (state.matchCase) setOf() else setOf(RegexOption.IGNORE_CASE)
            Regex(fullPattern, flags)
        } catch (e: Exception) {
            null
        } ?: return

        val newSections = currentDoc.sections.map { sec ->
            val newBlocks = sec.blocks.map { b ->
                if (b is ParagraphBlock) {
                    val updatedRuns = b.runs.map { run ->
                        run.copy(text = pattern.replace(run.text, replaceWith))
                    }
                    b.copy(runs = updatedRuns)
                } else b
            }
            sec.copy(blocks = newBlocks)
        }
        applyDocumentChange(currentDoc.copy(sections = newSections))
        performSearch()
    }

    // --- Document Persistence & Management ---
    fun saveCurrentDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.saveDocument(_uiState.value.document)
            _uiState.update { it.copy(isSaving = false, statusMessage = "Saved successfully") }
        }
    }

    fun loadDocument(document: Document, saveInitial: Boolean = true) {
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                document = document,
                canUndo = false,
                canRedo = false,
                activeBlockId = document.sections.firstOrNull()?.blocks?.firstOrNull()?.id,
                activeSectionIndex = 0
            )
        }
        recalculatePaginationAndTelemetry(document)
        if (saveInitial) {
            viewModelScope.launch {
                repository.saveDocument(document)
            }
        }
    }

    fun loadDocumentById(id: String) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(id)
            if (doc != null) {
                loadDocument(doc, saveInitial = false)
                _uiState.update { it.copy(showDocumentManagerDialog = false) }
            }
        }
    }

    fun createNewBlankDocument() {
        val blank = DocumentTemplates.createBlankDocument()
        loadDocument(blank, saveInitial = true)
        _uiState.update { it.copy(showDocumentManagerDialog = false) }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    // --- Export & Print ---
    fun printDocument(context: Context) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = DocumentPrintAdapter(context, _uiState.value.document)
        printManager.print("${_uiState.value.document.title}_Print", printAdapter, null)
    }

    fun exportPdfToStream(outputStream: OutputStream) {
        PdfExporter.exportToPdf(_uiState.value.document, outputStream)
    }

    fun exportDocxToStream(outputStream: OutputStream) {
        DocxPipeline.writeDocx(_uiState.value.document, outputStream)
    }

    fun importDocxFromStream(inputStream: InputStream, filename: String) {
        val doc = DocxPipeline.readDocx(inputStream, filename)
        loadDocument(doc, saveInitial = true)
    }

    // Dialog state toggles
    fun setShowTableInsertDialog(show: Boolean) = _uiState.update { it.copy(showTableInsertDialog = show) }
    fun setShowEquationDialog(show: Boolean) = _uiState.update { it.copy(showEquationDialog = show) }
    fun setShowWatermarkDialog(show: Boolean) = _uiState.update { it.copy(showWatermarkDialog = show) }
    fun setShowPageSetupDialog(show: Boolean) = _uiState.update { it.copy(showPageSetupDialog = show) }
    fun setShowDocumentManagerDialog(show: Boolean) = _uiState.update { it.copy(showDocumentManagerDialog = show) }
    fun setShowPasswordDialog(show: Boolean) = _uiState.update { it.copy(showPasswordDialog = show) }
    fun setShowStatisticsDialog(show: Boolean) = _uiState.update { it.copy(showStatisticsDialog = show) }
    fun setShowCommentDrawer(show: Boolean) = _uiState.update { it.copy(showCommentDrawer = show) }
    fun setShowReviewPanel(show: Boolean) = _uiState.update { it.copy(showReviewPanel = show) }
    fun clearStatusMessage() = _uiState.update { it.copy(statusMessage = null) }
}
