package com.example.doccraft.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccraft.engine.MathFormulaCanvas
import com.example.doccraft.viewmodel.DocumentViewModel

@Composable
fun TableInsertDialog(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var rowCount by remember { mutableStateOf(3) }
    var colCount by remember { mutableStateOf(3) }

    if (state.showTableInsertDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowTableInsertDialog(false) },
            title = { Text("Insert Table Grid", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select dimensions for your new table:")

                    // Rows Slider
                    Column {
                        Text("Rows: $rowCount", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Slider(
                            value = rowCount.toFloat(),
                            onValueChange = { rowCount = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }

                    // Columns Slider
                    Column {
                        Text("Columns: $colCount", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Slider(
                            value = colCount.toFloat(),
                            onValueChange = { colCount = it.toInt() },
                            valueRange = 1f..6f,
                            steps = 4
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.insertTable(rowCount, colCount) },
                    modifier = Modifier.testTag("btn_confirm_insert_table")
                ) {
                    Text("Insert Table")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowTableInsertDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EquationEditorDialog(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var latexInput by remember { mutableStateOf("\\frac{a}{b} + \\sqrt{x} = \\pi") }

    if (state.showEquationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowEquationDialog(false) },
            title = { Text("Mathematical Formula (LaTeX)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = latexInput,
                        onValueChange = { latexInput = it },
                        label = { Text("LaTeX Expression") },
                        modifier = Modifier.fillMaxWidth().testTag("input_latex")
                    )

                    // Quick Symbol / Formula Chips
                    Text("Quick Insert:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "\\frac{a}{b}" to "Fraction",
                            "\\sqrt{x}" to "Sqrt",
                            "x^2" to "Power",
                            "\\int" to "Integral",
                            "\\sum" to "Sum",
                            "\\pi" to "π",
                            "\\alpha" to "α",
                            "\\beta" to "β",
                            "\\infty" to "∞"
                        ).forEach { (symbol, label) ->
                            SuggestionChip(
                                onClick = { latexInput += " $symbol" },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Live Vector Math Rendering Preview
                    Text("Formula Preview:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MathFormulaCanvas(
                                latex = latexInput,
                                fontSizePt = 16f,
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.insertEquation(latexInput) },
                    modifier = Modifier.testTag("btn_confirm_insert_equation")
                ) {
                    Text("Insert Equation")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowEquationDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WatermarkDialog(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var watermarkText by remember(state.document.settings.watermark?.text) {
        mutableStateOf(state.document.settings.watermark?.text ?: "CONFIDENTIAL")
    }

    if (state.showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowWatermarkDialog(false) },
            title = { Text("Document Watermark", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set a faint diagonal watermark text across all pages:")

                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("Watermark Text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CONFIDENTIAL", "DRAFT", "SAMPLE", "DO NOT COPY").forEach { preset ->
                            SuggestionChip(
                                onClick = { watermarkText = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setWatermark(watermarkText)
                        viewModel.setShowWatermarkDialog(false)
                    }
                ) {
                    Text("Apply Watermark")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setWatermark(null)
                        viewModel.setShowWatermarkDialog(false)
                    }
                ) {
                    Text("Remove")
                }
            }
        )
    }
}

@Composable
fun StatisticsDialog(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tele = state.telemetry

    if (state.showStatisticsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowStatisticsDialog(false) },
            title = { Text("Document Statistics & Telemetry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatRow("Words", "${tele.wordCount}")
                    StatRow("Characters", "${tele.charCount}")
                    StatRow("Paragraphs", "${tele.paragraphCount}")
                    StatRow("Calculated Pages", "${tele.pageCount}")
                    StatRow("Estimated Reading Time", "${tele.readingTimeMinutes} min")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.setShowStatisticsDialog(false) }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
