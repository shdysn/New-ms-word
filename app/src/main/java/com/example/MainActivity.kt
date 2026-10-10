package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.doccraft.ui.*
import com.example.doccraft.viewmodel.DocumentViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DocCraftApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocCraftApp(
    viewModel: DocumentViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // SAF Activity Result Launchers
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    viewModel.exportPdfToStream(stream)
                }
                Toast.makeText(context, "PDF Exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportDocxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    viewModel.exportDocxToStream(stream)
                }
                Toast.makeText(context, "DOCX Exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importDocxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val filename = uri.lastPathSegment ?: "Imported.docx"
                    viewModel.importDocxFromStream(stream, filename)
                }
                Toast.makeText(context, "Document opened!", Toast.LENGTH_SHORT).show()
                viewModel.setShowDocumentManagerDialog(false)
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.insertImage(uri.toString())
        }
    }

    // Status Message Reaction
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                // Top Action Bar
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Document Center (Folder icon)
                        IconButton(
                            onClick = { viewModel.setShowDocumentManagerDialog(true) },
                            modifier = Modifier.testTag("btn_document_center")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Document Center", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Document Title (clickable to rename)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    renameInput = state.document.title
                                    showRenameDialog = true
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = state.document.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }

                        // Quick Actions: Search, Save, Export, Print, Overflow
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Find & Replace
                            IconButton(
                                onClick = { viewModel.openSearch() },
                                modifier = Modifier.testTag("btn_toggle_search")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Find & Replace")
                            }

                            // Auto/Manual Save
                            IconButton(
                                onClick = { viewModel.saveCurrentDocument() },
                                modifier = Modifier.testTag("btn_save_document")
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = "Save Document")
                                }
                            }

                            // Export Dropdown (PDF / DOCX / Print)
                            Box {
                                IconButton(
                                    onClick = { showExportMenu = true },
                                    modifier = Modifier.testTag("btn_export_menu")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export & Share")
                                }

                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F)) },
                                        text = { Text("Export as PDF") },
                                        onClick = {
                                            showExportMenu = false
                                            exportPdfLauncher.launch("${state.document.title}.pdf")
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF1976D2)) },
                                        text = { Text("Export as Word (.docx)") },
                                        onClick = {
                                            showExportMenu = false
                                            exportDocxLauncher.launch("${state.document.title}.docx")
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                        text = { Text("Print Document") },
                                        onClick = {
                                            showExportMenu = false
                                            viewModel.printDocument(context)
                                        }
                                    )
                                }
                            }

                            // Overflow Menu
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                                        text = { Text("Document Statistics") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setShowStatisticsDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null) },
                                        text = { Text(if (state.isStylusMode) "Disable S-Pen" else "Enable S-Pen Inking") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleStylusMode()
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null) },
                                        text = { Text(if (state.isTrackingChanges) "Stop Tracking Changes" else "Track Changes (Suggestions)") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleTrackChanges()
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null) },
                                        text = { Text("Comments (${state.document.comments.size})") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setShowCommentDrawer(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                                        text = { Text(if (state.isCanvasInverted) "Light Canvas" else "Dark Canvas (Comfort)") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleCanvasInvert()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Subtitle Telemetry Pill & Suggestion Mode indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${state.telemetry.pageCount} • ${state.telemetry.wordCount} words • ${state.telemetry.readingTimeMinutes} min read",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.isTrackingChanges) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Suggesting Mode",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Ribbon Categorized Toolbar
                RibbonToolbar(
                    viewModel = viewModel,
                    onPickImage = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // Animated Find & Replace Bar
                FindReplaceBar(viewModel = viewModel)
            }
        }
    ) { innerPadding ->
        // Main Paginated Document Canvas
        PaginatedCanvas(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    // --- Dialogs & Drawers ---
    DocumentManagerDialog(
        viewModel = viewModel,
        onImportDocx = {
            importDocxLauncher.launch(
                arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword")
            )
        }
    )

    CommentDrawerModal(viewModel = viewModel)
    ReviewPanelDialog(viewModel = viewModel)
    TableInsertDialog(viewModel = viewModel)
    EquationEditorDialog(viewModel = viewModel)
    WatermarkDialog(viewModel = viewModel)
    StatisticsDialog(viewModel = viewModel)

    // Rename Document Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_rename_doc")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.updateDocumentTitle(renameInput.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
