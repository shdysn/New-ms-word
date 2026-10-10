package com.example.doccraft.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.viewmodel.DocumentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindReplaceBar(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    AnimatedVisibility(
        visible = state.isSearchOpen,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            modifier = Modifier.fillMaxWidth().testTag("find_replace_bar")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Find Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Find text...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("input_search_query")
                    )

                    // Match Count indicator
                    val total = state.searchResults.size
                    val current = if (total > 0) state.currentSearchMatchIndex + 1 else 0
                    Text(
                        text = "$current of $total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { viewModel.prevSearchMatch() },
                        enabled = total > 0,
                        modifier = Modifier.testTag("btn_prev_match")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Match")
                    }

                    IconButton(
                        onClick = { viewModel.nextSearchMatch() },
                        enabled = total > 0,
                        modifier = Modifier.testTag("btn_next_match")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Match")
                    }

                    IconButton(
                        onClick = { viewModel.closeSearch() },
                        modifier = Modifier.testTag("btn_close_search")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Find")
                    }
                }

                // Replace Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.replaceQuery,
                        onValueChange = { viewModel.setReplaceQuery(it) },
                        placeholder = { Text("Replace with...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.FindReplace, contentDescription = null) },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("input_replace_query")
                    )

                    Button(
                        onClick = { viewModel.replaceCurrentMatch() },
                        enabled = state.searchResults.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp).testTag("btn_replace_one")
                    ) {
                        Text("Replace", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.replaceAllMatches() },
                        enabled = state.searchResults.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp).testTag("btn_replace_all")
                    ) {
                        Text("Replace All", fontSize = 12.sp)
                    }
                }

                // Options Row: Match Case, Match Whole Word, Regex
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.matchCase,
                        onClick = { viewModel.toggleMatchCase() },
                        label = { Text("Match Case", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = state.matchWholeWord,
                        onClick = { viewModel.toggleMatchWholeWord() },
                        label = { Text("Whole Word", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = state.useRegex,
                        onClick = { viewModel.toggleUseRegex() },
                        label = { Text("Regex", fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}
