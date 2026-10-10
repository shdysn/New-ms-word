package com.example.doccraft.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.templates.DocumentTemplates
import com.example.doccraft.viewmodel.DocumentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentManagerDialog(
    viewModel: DocumentViewModel,
    onImportDocx: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val savedDocs by viewModel.savedDocuments.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Templates, 1: Saved Documents

    if (state.showDocumentManagerDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDocumentManagerDialog(false) },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Document Center", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.setShowDocumentManagerDialog(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action: Import DOCX
                    OutlinedButton(
                        onClick = onImportDocx,
                        modifier = Modifier.fillMaxWidth().testTag("btn_import_docx")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open / Import .DOCX File")
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Starter Templates") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("My Saved Docs (${savedDocs.size})") }
                        )
                    }

                    if (selectedTab == 0) {
                        // Templates List
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                TemplateCard(
                                    title = "Blank Document",
                                    description = "Clean empty canvas ready for your words.",
                                    onClick = { viewModel.createNewBlankDocument() }
                                )
                            }
                            item {
                                TemplateCard(
                                    title = "Modern Professional Resume",
                                    description = "Clean hierarchy with skills table and executive summary.",
                                    onClick = {
                                        viewModel.loadDocument(DocumentTemplates.createResumeTemplate())
                                        viewModel.setShowDocumentManagerDialog(false)
                                    }
                                )
                            }
                            item {
                                TemplateCard(
                                    title = "Executive Business Proposal",
                                    description = "Formatted with multi-page roadmap, timeline matrix, and TOC.",
                                    onClick = {
                                        viewModel.loadDocument(DocumentTemplates.createBusinessProposalTemplate())
                                        viewModel.setShowDocumentManagerDialog(false)
                                    }
                                )
                            }
                            item {
                                TemplateCard(
                                    title = "Academic Research Paper (APA)",
                                    description = "Standard academic layout with abstract, equations, and serif type.",
                                    onClick = {
                                        viewModel.loadDocument(DocumentTemplates.createAcademicEssayTemplate())
                                        viewModel.setShowDocumentManagerDialog(false)
                                    }
                                )
                            }
                            item {
                                TemplateCard(
                                    title = "Clean Invoice & Billing Statement",
                                    description = "Itemized billing rows, payment terms, and total calculation.",
                                    onClick = {
                                        viewModel.loadDocument(DocumentTemplates.createInvoiceTemplate())
                                        viewModel.setShowDocumentManagerDialog(false)
                                    }
                                )
                            }
                        }
                    } else {
                        // Saved Docs List
                        if (savedDocs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No saved documents yet.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(savedDocs, key = { it.id }) { docEntity ->
                                    val dateStr = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                                        .format(Date(docEntity.lastModified))
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.loadDocumentById(docEntity.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(docEntity.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(docEntity.previewSnippet, maxLines = 1, fontSize = 11.sp, color = Color.Gray)
                                                Text(
                                                    "$dateStr • ${docEntity.wordCount} words",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Row {
                                                IconButton(onClick = { viewModel.toggleFavorite(docEntity.id) }) {
                                                    Icon(
                                                        if (docEntity.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = "Favorite",
                                                        tint = if (docEntity.isFavorite) Color(0xFFFFB300) else Color.Gray
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.deleteDocument(docEntity.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun TemplateCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(description, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
