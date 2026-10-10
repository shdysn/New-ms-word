package com.example.doccraft.io

import android.util.Xml
import com.example.doccraft.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DocxPipeline {

    /**
     * Exports a Document AST into an OpenXML (.docx) ZIP package.
     */
    fun writeDocx(document: Document, outputStream: OutputStream) {
        val zip = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(getContentTypesXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(getRelsXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 3. word/_rels/document.xml.rels
        zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        zip.write(getDocumentRelsXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 4. word/document.xml
        zip.putNextEntry(ZipEntry("word/document.xml"))
        val docXml = generateDocumentXml(document)
        zip.write(docXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.finish()
    }

    /**
     * Reads an OpenXML (.docx) ZIP stream and parses word/document.xml into a Document AST.
     */
    fun readDocx(inputStream: InputStream, filename: String = "Imported Document"): Document {
        val zip = ZipInputStream(inputStream)
        var documentXmlContent: String? = null

        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val bytes = zip.readBytes()
                documentXmlContent = String(bytes, StandardCharsets.UTF_8)
                break
            }
            entry = zip.nextEntry
        }
        zip.close()

        if (documentXmlContent.isNullOrEmpty()) {
            return Document(title = filename)
        }

        return parseDocumentXml(documentXmlContent, filename)
    }

    private fun generateDocumentXml(document: Document): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        for (section in document.sections) {
            for (block in section.blocks) {
                when (block) {
                    is ParagraphBlock -> {
                        sb.append("<w:p>")
                        sb.append("<w:pPr>")
                        // Alignment
                        val jcVal = when (block.alignment) {
                            TextAlignment.Left -> "left"
                            TextAlignment.Center -> "center"
                            TextAlignment.Right -> "right"
                            TextAlignment.Justify -> "both"
                        }
                        sb.append("""<w:jc w:val="$jcVal"/>""")

                        // Spacing
                        val spaceBeforeDxa = (block.spaceBeforePt * 20).toInt()
                        val spaceAfterDxa = (block.spaceAfterPt * 20).toInt()
                        val lineDxa = (block.lineSpacingMultiplier * 240).toInt()
                        sb.append("""<w:spacing w:before="$spaceBeforeDxa" w:after="$spaceAfterDxa" w:line="$lineDxa" w:lineRule="auto"/>""")

                        // Indent
                        if (block.firstLineIndentPt > 0) {
                            val indentDxa = (block.firstLineIndentPt * 20).toInt()
                            sb.append("""<w:ind w:firstLine="$indentDxa"/>""")
                        }
                        sb.append("</w:pPr>")

                        // Runs
                        for (run in block.runs) {
                            sb.append("<w:r>")
                            sb.append("<w:rPr>")
                            if (run.style.isBold) sb.append("<w:b/>")
                            if (run.style.isItalic) sb.append("<w:i/>")
                            if (run.style.isStrikethrough) sb.append("<w:strike/>")
                            if (run.style.underline != UnderlineStyle.None) sb.append("""<w:u w:val="single"/>""")
                            val halfPts = (run.style.fontSizePt * 2).toInt()
                            sb.append("""<w:sz w:val="$halfPts"/>""")
                            val hexColor = String.format("%06X", (run.style.textColorArgb and 0x00FFFFFF))
                            sb.append("""<w:color w:val="$hexColor"/>""")
                            sb.append("</w:rPr>")
                            sb.append("""<w:t xml:space="preserve">""").append(escapeXml(run.text)).append("</w:t>")
                            sb.append("</w:r>")
                        }
                        sb.append("</w:p>")
                    }
                    is TableBlock -> {
                        sb.append("<w:tbl>")
                        for (row in block.rows) {
                            sb.append("<w:tr>")
                            for (cell in row.cells) {
                                sb.append("<w:tc>")
                                sb.append("<w:p><w:r><w:t>").append(escapeXml(cell.text)).append("</w:t></w:r></w:p>")
                                sb.append("</w:tc>")
                            }
                            sb.append("</w:tr>")
                        }
                        sb.append("</w:tbl>")
                    }
                    is PageBreakBlock -> {
                        sb.append("""<w:p><w:r><w:br w:type="page"/></w:r></w:p>""")
                    }
                    is EquationBlock -> {
                        sb.append("""<w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:t>""").append(escapeXml(block.latex)).append("</w:t></w:r></w:p>")
                    }
                    is HorizontalRuleBlock -> {
                        sb.append("""<w:p><w:pBdr><w:bottom w:val="single" w:sz="12" w:space="1" w:color="CCCCCC"/></w:pBdr></w:p>""")
                    }
                    else -> {}
                }
            }

            // Section properties (A4 or Letter dimensions)
            val wDxa = (section.pageSetup.effectiveWidthPt * 20).toInt()
            val hDxa = (section.pageSetup.effectiveHeightPt * 20).toInt()
            val topDxa = (section.pageSetup.margins.topPt * 20).toInt()
            val bottomDxa = (section.pageSetup.margins.bottomPt * 20).toInt()
            val leftDxa = (section.pageSetup.margins.leftPt * 20).toInt()
            val rightDxa = (section.pageSetup.margins.rightPt * 20).toInt()

            sb.append("<w:sectPr>")
            sb.append("""<w:pgSz w:w="$wDxa" w:h="$hDxa"/>""")
            sb.append("""<w:pgMar w:top="$topDxa" w:right="$rightDxa" w:bottom="$bottomDxa" w:left="$leftDxa"/>""")
            sb.append("</w:sectPr>")
        }

        sb.append("</w:body>")
        sb.append("</w:document>")
        return sb.toString()
    }

    private fun parseDocumentXml(xml: String, filename: String): Document {
        val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val blocks = mutableListOf<BlockElement>()
        var eventType = parser.eventType

        var inParagraph = false
        var inRun = false
        var currentRuns = mutableListOf<TextRun>()
        var isBold = false
        var isItalic = false
        var fontSizePt = 11f
        var currentText = StringBuilder()

        var inTable = false
        var currentRows = mutableListOf<TableRow>()
        var currentCells = mutableListOf<TableCell>()
        var currentCellText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val localName = parser.name?.substringAfter(":") ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (localName) {
                        "tbl" -> {
                            inTable = true
                            currentRows = mutableListOf()
                        }
                        "tr" -> {
                            currentCells = mutableListOf()
                        }
                        "tc" -> {
                            currentCellText = StringBuilder()
                        }
                        "p" -> {
                            if (!inTable) {
                                inParagraph = true
                                currentRuns = mutableListOf()
                            }
                        }
                        "r" -> {
                            inRun = true
                            isBold = false
                            isItalic = false
                            fontSizePt = 11f
                            currentText = StringBuilder()
                        }
                        "b" -> isBold = true
                        "i" -> isItalic = true
                        "sz" -> {
                            val valAttr = parser.getAttributeValue(null, "val") 
                                ?: parser.getAttributeValue(null, "w:val")
                            valAttr?.toIntOrNull()?.let { halfPts ->
                                fontSizePt = halfPts / 2f
                            }
                        }
                        "t" -> {
                            val text = parser.nextText()
                            if (inTable) {
                                currentCellText.append(text)
                            } else {
                                currentText.append(text)
                            }
                        }
                        "br" -> {
                            val typeAttr = parser.getAttributeValue(null, "type")
                                ?: parser.getAttributeValue(null, "w:type")
                            if (typeAttr == "page") {
                                blocks.add(PageBreakBlock())
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localName) {
                        "r" -> {
                            if (inRun && !inTable) {
                                currentRuns.add(
                                    TextRun(
                                        text = currentText.toString(),
                                        style = TextStyleRun(isBold = isBold, isItalic = isItalic, fontSizePt = fontSizePt)
                                    )
                                )
                            }
                            inRun = false
                        }
                        "p" -> {
                            if (inParagraph && !inTable) {
                                blocks.add(ParagraphBlock(runs = if (currentRuns.isEmpty()) listOf(TextRun()) else currentRuns))
                            }
                            inParagraph = false
                        }
                        "tc" -> {
                            currentCells.add(TableCell(text = currentCellText.toString()))
                        }
                        "tr" -> {
                            currentRows.add(TableRow(cells = currentCells.toList()))
                        }
                        "tbl" -> {
                            blocks.add(TableBlock(rows = currentRows.toList()))
                            inTable = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val section = Section(
            blocks = if (blocks.isEmpty()) listOf(ParagraphBlock()) else blocks
        )

        return Document(
            id = UUID.randomUUID().toString(),
            title = filename.removeSuffix(".docx"),
            sections = listOf(section)
        )
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
    }

    private fun getRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun getDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""
    }
}
