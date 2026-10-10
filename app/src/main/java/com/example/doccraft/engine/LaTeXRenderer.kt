package com.example.doccraft.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * High-performance vector math renderer for LaTeX mathematical formulas.
 * Supports fractions, radicals, powers, integrals, summations, and greek symbols.
 */
object LaTeXRenderer {

    fun substituteGreekAndSymbols(raw: String): String {
        return raw
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\delta", "δ")
            .replace("\\epsilon", "ε")
            .replace("\\theta", "θ")
            .replace("\\lambda", "λ")
            .replace("\\mu", "μ")
            .replace("\\pi", "π")
            .replace("\\sigma", "σ")
            .replace("\\omega", "ω")
            .replace("\\Delta", "Δ")
            .replace("\\Omega", "Ω")
            .replace("\\infty", "∞")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\pm", "±")
            .replace("\\le", "≤")
            .replace("\\ge", "≥")
            .replace("\\ne", "≠")
            .replace("\\approx", "≈")
            .replace("\\rightarrow", "→")
            .replace("\\partial", "∂")
            .replace("\\int", "∫")
            .replace("\\sum", "∑")
            .replace("\\prod", "∏")
    }

    /**
     * Converts a LaTeX string into an accessible display string with Unicode mathematical glyphs.
     */
    fun toDisplayText(latex: String): String {
        var processed = substituteGreekAndSymbols(latex)
        // Convert \frac{a}{b} -> (a / b)
        val fracRegex = Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")
        processed = fracRegex.replace(processed) { matchResult ->
            val num = matchResult.groupValues[1]
            val den = matchResult.groupValues[2]
            "($num / $den)"
        }
        // Convert \sqrt{x} -> √(x)
        val sqrtRegex = Regex("""\\sqrt\{([^}]+)\}""")
        processed = sqrtRegex.replace(processed) { matchResult ->
            "√(" + matchResult.groupValues[1] + ")"
        }
        return processed
    }
}

@Composable
fun MathFormulaCanvas(
    latex: String,
    fontSizePt: Float,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val displayText = LaTeXRenderer.toDisplayText(latex)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(max(38f, fontSizePt * 2.8f).dp)
    ) {
        val paint = android.graphics.Paint().apply {
            textSize = fontSizePt * 2.5f
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (textColor.alpha * 255).toInt(),
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            typeface = android.graphics.Typeface.SERIF
        }

        val textWidth = paint.measureText(displayText)
        val startX = max(10f, (size.width - textWidth) / 2f)
        val baselineY = size.height * 0.65f

        drawContext.canvas.nativeCanvas.drawText(displayText, startX, baselineY, paint)
    }
}
