package com.example

import com.example.doccraft.data.DocumentJsonAdapter
import com.example.doccraft.engine.LaTeXRenderer
import com.example.doccraft.engine.PaginationEngine
import com.example.doccraft.io.DocxPipeline
import com.example.doccraft.model.*
import com.example.doccraft.templates.DocumentTemplates
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocCraftEngineTest {

    @Test
    fun testDocumentJsonRoundtrip() {
        val originalDoc = DocumentTemplates.createResumeTemplate()
        val json = DocumentJsonAdapter.toJson(originalDoc)
        assertNotNull(json)
        assertTrue(json.contains("ALEXANDRA MORGAN"))

        val parsedDoc = DocumentJsonAdapter.fromJson(json)
        assertEquals(originalDoc.title, parsedDoc.title)
        assertEquals(originalDoc.sections.size, parsedDoc.sections.size)
        assertEquals(originalDoc.sections[0].blocks.size, parsedDoc.sections[0].blocks.size)
    }

    @Test
    fun testPaginationEngineCalculatesPagesAndTokens() {
        val doc = DocumentTemplates.createBusinessProposalTemplate()
        val pages = PaginationEngine.paginate(doc)

        assertTrue(pages.isNotEmpty())
        for (page in pages) {
            assertTrue(page.items.isNotEmpty())
            assertTrue(page.footerText.contains("${page.pageIndex + 1} of ${pages.size}"))
        }
    }

    @Test
    fun testDocxOpenXmlWriteAndRead() {
        val doc = Document(
            title = "Test Proposal",
            sections = listOf(
                Section(
                    blocks = listOf(
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            runs = listOf(TextRun(text = "Executive Summary", style = TextStyleRun(isBold = true)))
                        ),
                        TableBlock(
                            rows = listOf(
                                TableRow(cells = listOf(TableCell(text = "Metric"), TableCell(text = "Score"))),
                                TableRow(cells = listOf(TableCell(text = "Speed"), TableCell(text = "99.8%")))
                            )
                        )
                    )
                )
            )
        )

        val outputStream = ByteArrayOutputStream()
        DocxPipeline.writeDocx(doc, outputStream)
        val docxBytes = outputStream.toByteArray()

        assertTrue(docxBytes.isNotEmpty())

        val inputStream = ByteArrayInputStream(docxBytes)
        val readDoc = DocxPipeline.readDocx(inputStream, "Test Proposal.docx")

        assertEquals("Test Proposal", readDoc.title)
        val readBlocks = readDoc.allBlocks()
        assertTrue(readBlocks.any { it is ParagraphBlock && it.plainText.contains("Executive Summary") })
        assertTrue(readBlocks.any { it is TableBlock })
    }

    @Test
    fun testLaTeXRendererSymbolSubstitution() {
        val raw = "\\alpha + \\beta = \\pi"
        val display = LaTeXRenderer.toDisplayText(raw)
        assertEquals("α + β = π", display)

        val fracRaw = "\\frac{x}{y}"
        val fracDisplay = LaTeXRenderer.toDisplayText(fracRaw)
        assertEquals("(x / y)", fracDisplay)

        val sqrtRaw = "\\sqrt{16}"
        val sqrtDisplay = LaTeXRenderer.toDisplayText(sqrtRaw)
        assertEquals("√(16)", sqrtDisplay)
    }

    @Test
    fun testStarterTemplatesIntegrity() {
        val blank = DocumentTemplates.createBlankDocument()
        assertEquals("Untitled Document", blank.title)
        assertEquals(1, blank.sections.size)

        val resume = DocumentTemplates.createResumeTemplate()
        assertTrue(resume.allBlocks().size > 5)

        val invoice = DocumentTemplates.createInvoiceTemplate()
        assertTrue(invoice.allBlocks().any { it is TableBlock })

        val paper = DocumentTemplates.createAcademicEssayTemplate()
        assertTrue(paper.allBlocks().any { it is EquationBlock })
    }
}
