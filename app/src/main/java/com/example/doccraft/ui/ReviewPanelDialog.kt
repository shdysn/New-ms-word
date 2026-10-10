package com.example.doccraft.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.model.ParagraphBlock
import com.example.doccraft.model.RevisionTag
import com.example.doccraft.model.RevisionType
import com.example.doccraft.viewmodel.DocumentViewModel

data class PendingRevisionItem(
    val blockId: String,
    val runId: String,
    val text: String,
    val revision: RevisionTag
)

@Composable
fun ReviewPanelDialog(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (state.showReviewPanel) {
        val pendingRevisions = mutableListOf<PendingRevisionItem>()
        for (sec in state.document.sections) {
            for (b in sec.blocks) {
                if (b is ParagraphBlock) {
                    for (r in b.runs) {
                        r.revision?.let { rev ->
                            pendingRevisions.add(
                                PendingRevisionItem(
                                    blockId = b.id,
                                    runId = r.id,
                                    text = r.text,
                                    revision = rev
                                )
                            )
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.setShowReviewPanel(false) },
            title = {
                Text(
                    text = "Track Changes Review (${pendingRevisions.size})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bulk Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.acceptAllRevisions() },
                            enabled = pendingRevisions.isNotEmpty(),
                            modifier = Modifier.weight(1f).testTag("btn_accept_all_revisions")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Accept All", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.rejectAllRevisions() },
                            enabled = pendingRevisions.isNotEmpty(),
                            modifier = Modifier.weight(1f).testTag("btn_reject_all_revisions")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject All", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider()

                    if (pendingRevisions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All revisions have been resolved.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(pendingRevisions, key = { it.runId }) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val isInsert = item.revision.type == RevisionType.Insertion
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isInsert) "Inserted by ${item.revision.author}" else "Deleted by ${item.revision.author}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isInsert) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                            )
                                        }

                                        Text(
                                            text = "“${item.text}”",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.rejectRevision(item.blockId, item.runId) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red)
                                            }
                                            IconButton(
                                                onClick = { viewModel.acceptRevision(item.blockId, item.runId) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF2E7D32))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowReviewPanel(false) }) {
                    Text("Close")
                }
            }
        )
    }
}
