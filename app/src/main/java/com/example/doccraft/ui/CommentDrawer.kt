package com.example.doccraft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.model.Comment
import com.example.doccraft.viewmodel.DocumentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentDrawerModal(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var newCommentText by remember { mutableStateOf("") }

    if (state.showCommentDrawer) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowCommentDrawer(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = modifier.testTag("modal_comments")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxHeight(0.8f)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Comments (${state.document.comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.setShowCommentDrawer(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // New Comment Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add comment to current section...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                viewModel.addComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank()
                    ) {
                        Text("Post")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Comments List
                if (state.document.comments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No comments yet.\nSelect text or a block to leave contextual feedback.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.document.comments, key = { it.id }) { comment ->
                            CommentCard(comment = comment, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: Comment, viewModel: DocumentViewModel) {
    var replyText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (comment.isResolved) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header with Author & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comment.author.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(text = comment.author, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.resolveComment(comment.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (comment.isResolved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Resolve",
                            tint = if (comment.isResolved) Color(0xFF2E7D32) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.deleteComment(comment.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (comment.quoteText.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${comment.quoteText}”",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Text(text = comment.message, fontSize = 13.sp)

            // Threaded Replies
            for (reply in comment.replies) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "${reply.author}:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(text = reply.message, fontSize = 12.sp)
                }
            }

            // Reply Box
            if (isReplying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                viewModel.replyComment(comment.id, replyText)
                                replyText = ""
                                isReplying = false
                            }
                        },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Send", fontSize = 11.sp)
                    }
                }
            } else {
                TextButton(
                    onClick = { isReplying = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Reply", fontSize = 11.sp)
                }
            }
        }
    }
}
