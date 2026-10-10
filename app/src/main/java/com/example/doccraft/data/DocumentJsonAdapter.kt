package com.example.doccraft.data

import com.example.doccraft.model.*
import org.json.JSONArray
import org.json.JSONObject

object DocumentJsonAdapter {

    fun toJson(document: Document): String {
        val root = JSONObject()
        root.put("id", document.id)
        root.put("title", document.title)

        val meta = JSONObject()
        meta.put("author", document.metadata.author)
        meta.put("createdTimestamp", document.metadata.createdTimestamp)
        meta.put("lastModifiedTimestamp", document.metadata.lastModifiedTimestamp)
        meta.put("subject", document.metadata.subject)
        meta.put("revisionNumber", document.metadata.revisionNumber)
        root.put("metadata", meta)

        val settings = JSONObject()
        settings.put("defaultPaperSize", document.settings.defaultPaperSize.name)
        settings.put("defaultOrientation", document.settings.defaultOrientation.name)
        settings.put("isReadOnly", document.settings.isReadOnly)
        settings.put("isPasswordProtected", document.settings.isPasswordProtected)
        settings.put("passwordHash", document.settings.passwordHash ?: "")
        document.settings.watermark?.let { wm ->
            val wmObj = JSONObject()
            wmObj.put("text", wm.text)
            wmObj.put("colorArgb", wm.colorArgb)
            wmObj.put("opacity", wm.opacity.toDouble())
            wmObj.put("fontSizePt", wm.fontSizePt.toDouble())
            wmObj.put("angleDegrees", wm.angleDegrees.toDouble())
            settings.put("watermark", wmObj)
        }
        root.put("settings", settings)

        val sectionsArray = JSONArray()
        for (sec in document.sections) {
            val sObj = JSONObject()
            sObj.put("id", sec.id)

            val ps = JSONObject()
            ps.put("paperSize", sec.pageSetup.paperSize.name)
            ps.put("orientation", sec.pageSetup.orientation.name)
            ps.put("columnCount", sec.pageSetup.columnCount)
            ps.put("columnGutterPt", sec.pageSetup.columnGutterPt.toDouble())
            val mObj = JSONObject()
            mObj.put("topPt", sec.pageSetup.margins.topPt.toDouble())
            mObj.put("bottomPt", sec.pageSetup.margins.bottomPt.toDouble())
            mObj.put("leftPt", sec.pageSetup.margins.leftPt.toDouble())
            mObj.put("rightPt", sec.pageSetup.margins.rightPt.toDouble())
            ps.put("margins", mObj)
            sObj.put("pageSetup", ps)

            val hf = JSONObject()
            hf.put("headerText", sec.headerFooter.headerText)
            hf.put("footerText", sec.headerFooter.footerText)
            hf.put("showOnFirstPage", sec.headerFooter.showOnFirstPage)
            hf.put("differentOddEven", sec.headerFooter.differentOddEven)
            sObj.put("headerFooter", hf)

            val blocksArr = JSONArray()
            for (b in sec.blocks) {
                blocksArr.put(blockToJson(b))
            }
            sObj.put("blocks", blocksArr)
            sectionsArray.put(sObj)
        }
        root.put("sections", sectionsArray)

        // Comments
        val commentsArr = JSONArray()
        for (c in document.comments) {
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("author", c.author)
            cObj.put("timestamp", c.timestamp)
            cObj.put("targetBlockId", c.targetBlockId)
            cObj.put("quoteText", c.quoteText)
            cObj.put("message", c.message)
            cObj.put("isResolved", c.isResolved)
            val repArr = JSONArray()
            for (r in c.replies) {
                val rObj = JSONObject()
                rObj.put("id", r.id)
                rObj.put("author", r.author)
                rObj.put("timestamp", r.timestamp)
                rObj.put("message", r.message)
                repArr.put(rObj)
            }
            cObj.put("replies", repArr)
            commentsArr.put(cObj)
        }
        root.put("comments", commentsArr)

        // Freehand strokes
        val strokesArr = JSONArray()
        for (s in document.freehandStrokes) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("colorArgb", s.colorArgb)
            sObj.put("strokeWidthPt", s.strokeWidthPt.toDouble())
            sObj.put("isEraser", s.isEraser)
            val ptsArr = JSONArray()
            for (pt in s.points) {
                val ptObj = JSONObject()
                ptObj.put("x", pt.xPt.toDouble())
                ptObj.put("y", pt.yPt.toDouble())
                ptObj.put("p", pt.pressure.toDouble())
                ptsArr.put(ptObj)
            }
            sObj.put("points", ptsArr)
            strokesArr.put(sObj)
        }
        root.put("freehandStrokes", strokesArr)

        return root.toString(2)
    }

    private fun blockToJson(block: BlockElement): JSONObject {
        val obj = JSONObject()
        obj.put("id", block.id)
        when (block) {
            is ParagraphBlock -> {
                obj.put("type", "paragraph")
                obj.put("alignment", block.alignment.name)
                obj.put("lineSpacingMultiplier", block.lineSpacingMultiplier.toDouble())
                obj.put("spaceBeforePt", block.spaceBeforePt.toDouble())
                obj.put("spaceAfterPt", block.spaceAfterPt.toDouble())
                obj.put("firstLineIndentPt", block.firstLineIndentPt.toDouble())
                obj.put("hangingIndentPt", block.hangingIndentPt.toDouble())
                obj.put("headingStyle", block.headingStyle.name)
                obj.put("listStyle", block.listStyle.name)
                obj.put("listLevel", block.listLevel)
                obj.put("isDropCap", block.isDropCap)

                val runsArr = JSONArray()
                for (r in block.runs) {
                    val rObj = JSONObject()
                    rObj.put("id", r.id)
                    rObj.put("text", r.text)

                    val stObj = JSONObject()
                    stObj.put("isBold", r.style.isBold)
                    stObj.put("isItalic", r.style.isItalic)
                    stObj.put("underline", r.style.underline.name)
                    stObj.put("isStrikethrough", r.style.isStrikethrough)
                    stObj.put("script", r.style.script.name)
                    stObj.put("fontSizePt", r.style.fontSizePt.toDouble())
                    stObj.put("textColorArgb", r.style.textColorArgb)
                    if (r.style.highlightColorArgb != null) {
                        stObj.put("highlightColorArgb", r.style.highlightColorArgb)
                    }
                    stObj.put("letterSpacingPt", r.style.letterSpacingPt.toDouble())
                    stObj.put("baselineShiftPt", r.style.baselineShiftPt.toDouble())
                    stObj.put("fontFamily", r.style.fontFamily)
                    rObj.put("style", stObj)

                    r.revision?.let { rev ->
                        val revObj = JSONObject()
                        revObj.put("type", rev.type.name)
                        revObj.put("author", rev.author)
                        revObj.put("timestamp", rev.timestamp)
                        rev.originalText?.let { revObj.put("originalText", it) }
                        rObj.put("revision", revObj)
                    }
                    runsArr.put(rObj)
                }
                obj.put("runs", runsArr)
            }
            is TableBlock -> {
                obj.put("type", "table")
                obj.put("borderColorArgb", block.borderColorArgb)
                obj.put("borderWidthPt", block.borderWidthPt.toDouble())
                obj.put("borderStyle", block.borderStyle.name)
                obj.put("spaceBeforePt", block.spaceBeforePt.toDouble())
                obj.put("spaceAfterPt", block.spaceAfterPt.toDouble())
                val rowsArr = JSONArray()
                for (row in block.rows) {
                    val rowObj = JSONObject()
                    rowObj.put("id", row.id)
                    val cellsArr = JSONArray()
                    for (c in row.cells) {
                        val cObj = JSONObject()
                        cObj.put("id", c.id)
                        cObj.put("text", c.text)
                        cObj.put("colSpan", c.colSpan)
                        cObj.put("rowSpan", c.rowSpan)
                        if (c.backgroundColorArgb != null) cObj.put("backgroundColorArgb", c.backgroundColorArgb)
                        cObj.put("textAlignment", c.textAlignment.name)
                        cObj.put("isHeader", c.isHeader)
                        cellsArr.put(cObj)
                    }
                    rowObj.put("cells", cellsArr)
                    rowsArr.put(rowObj)
                }
                obj.put("rows", rowsArr)
            }
            is ImageBlock -> {
                obj.put("type", "image")
                obj.put("uri", block.uri)
                obj.put("caption", block.caption)
                obj.put("widthPt", block.widthPt.toDouble())
                obj.put("heightPt", block.heightPt.toDouble())
                obj.put("rotationDegrees", block.rotationDegrees.toDouble())
                obj.put("wrappingMode", block.wrappingMode.name)
                obj.put("alignment", block.alignment.name)
                obj.put("spaceBeforePt", block.spaceBeforePt.toDouble())
                obj.put("spaceAfterPt", block.spaceAfterPt.toDouble())
            }
            is EquationBlock -> {
                obj.put("type", "equation")
                obj.put("latex", block.latex)
                obj.put("fontSizePt", block.fontSizePt.toDouble())
                obj.put("alignment", block.alignment.name)
                obj.put("spaceBeforePt", block.spaceBeforePt.toDouble())
                obj.put("spaceAfterPt", block.spaceAfterPt.toDouble())
            }
            is PageBreakBlock -> {
                obj.put("type", "pageBreak")
            }
            is SectionBreakBlock -> {
                obj.put("type", "sectionBreak")
                obj.put("isContinuous", block.isContinuous)
            }
            is TableOfContentsBlock -> {
                obj.put("type", "tableOfContents")
                obj.put("title", block.title)
                obj.put("showPageNumbers", block.showPageNumbers)
            }
            is HorizontalRuleBlock -> {
                obj.put("type", "horizontalRule")
                obj.put("thicknessPt", block.thicknessPt.toDouble())
                obj.put("colorArgb", block.colorArgb)
            }
        }
        return obj
    }

    fun fromJson(jsonStr: String): Document {
        val root = JSONObject(jsonStr)
        val id = root.optString("id")
        val title = root.optString("title", "Untitled Document")

        val metaObj = root.optJSONObject("metadata")
        val metadata = if (metaObj != null) {
            DocumentMetadata(
                author = metaObj.optString("author", "Author"),
                createdTimestamp = metaObj.optLong("createdTimestamp", System.currentTimeMillis()),
                lastModifiedTimestamp = metaObj.optLong("lastModifiedTimestamp", System.currentTimeMillis()),
                subject = metaObj.optString("subject", ""),
                revisionNumber = metaObj.optInt("revisionNumber", 1)
            )
        } else DocumentMetadata()

        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj != null) {
            val wmObj = settingsObj.optJSONObject("watermark")
            val wm = if (wmObj != null) {
                Watermark(
                    text = wmObj.optString("text", "CONFIDENTIAL"),
                    colorArgb = wmObj.optLong("colorArgb", 0x22888888),
                    opacity = wmObj.optDouble("opacity", 0.15).toFloat(),
                    fontSizePt = wmObj.optDouble("fontSizePt", 54.0).toFloat(),
                    angleDegrees = wmObj.optDouble("angleDegrees", -45.0).toFloat()
                )
            } else null

            DocumentSettings(
                defaultPaperSize = try {
                    PaperSize.valueOf(settingsObj.optString("defaultPaperSize", "A4"))
                } catch (e: Exception) { PaperSize.A4 },
                defaultOrientation = try {
                    Orientation.valueOf(settingsObj.optString("defaultOrientation", "Portrait"))
                } catch (e: Exception) { Orientation.Portrait },
                watermark = wm,
                isReadOnly = settingsObj.optBoolean("isReadOnly", false),
                isPasswordProtected = settingsObj.optBoolean("isPasswordProtected", false),
                passwordHash = settingsObj.optString("passwordHash").ifEmpty { null }
            )
        } else DocumentSettings()

        val sectionsArr = root.optJSONArray("sections")
        val sections = mutableListOf<Section>()
        if (sectionsArr != null) {
            for (i in 0 until sectionsArr.length()) {
                val sObj = sectionsArr.getJSONObject(i)
                val sId = sObj.optString("id")

                val psObj = sObj.optJSONObject("pageSetup")
                val pageSetup = if (psObj != null) {
                    val mObj = psObj.optJSONObject("margins")
                    val margins = if (mObj != null) {
                        Margins(
                            topPt = mObj.optDouble("topPt", 72.0).toFloat(),
                            bottomPt = mObj.optDouble("bottomPt", 72.0).toFloat(),
                            leftPt = mObj.optDouble("leftPt", 72.0).toFloat(),
                            rightPt = mObj.optDouble("rightPt", 72.0).toFloat()
                        )
                    } else Margins.Normal

                    PageSetup(
                        paperSize = try {
                            PaperSize.valueOf(psObj.optString("paperSize", "A4"))
                        } catch (e: Exception) { PaperSize.A4 },
                        orientation = try {
                            Orientation.valueOf(psObj.optString("orientation", "Portrait"))
                        } catch (e: Exception) { Orientation.Portrait },
                        margins = margins,
                        columnCount = psObj.optInt("columnCount", 1),
                        columnGutterPt = psObj.optDouble("columnGutterPt", 18.0).toFloat()
                    )
                } else PageSetup()

                val hfObj = sObj.optJSONObject("headerFooter")
                val headerFooter = if (hfObj != null) {
                    HeaderFooter(
                        headerText = hfObj.optString("headerText", ""),
                        footerText = hfObj.optString("footerText", "Page [PAGE] of [NUMPAGES]"),
                        showOnFirstPage = hfObj.optBoolean("showOnFirstPage", true),
                        differentOddEven = hfObj.optBoolean("differentOddEven", false)
                    )
                } else HeaderFooter()

                val blocksArr = sObj.optJSONArray("blocks")
                val blocks = mutableListOf<BlockElement>()
                if (blocksArr != null) {
                    for (j in 0 until blocksArr.length()) {
                        blocks.add(blockFromJson(blocksArr.getJSONObject(j)))
                    }
                }
                sections.add(Section(id = sId, pageSetup = pageSetup, headerFooter = headerFooter, blocks = blocks))
            }
        }
        if (sections.isEmpty()) sections.add(Section())

        // Comments
        val commentsArr = root.optJSONArray("comments")
        val comments = mutableListOf<Comment>()
        if (commentsArr != null) {
            for (i in 0 until commentsArr.length()) {
                val cObj = commentsArr.getJSONObject(i)
                val repliesArr = cObj.optJSONArray("replies")
                val replies = mutableListOf<CommentReply>()
                if (repliesArr != null) {
                    for (r in 0 until repliesArr.length()) {
                        val rObj = repliesArr.getJSONObject(r)
                        replies.add(
                            CommentReply(
                                id = rObj.optString("id"),
                                author = rObj.optString("author", "Author"),
                                timestamp = rObj.optLong("timestamp", System.currentTimeMillis()),
                                message = rObj.optString("message", "")
                            )
                        )
                    }
                }
                comments.add(
                    Comment(
                        id = cObj.optString("id"),
                        author = cObj.optString("author", "Author"),
                        timestamp = cObj.optLong("timestamp", System.currentTimeMillis()),
                        targetBlockId = cObj.optString("targetBlockId", ""),
                        quoteText = cObj.optString("quoteText", ""),
                        message = cObj.optString("message", ""),
                        replies = replies,
                        isResolved = cObj.optBoolean("isResolved", false)
                    )
                )
            }
        }

        // Strokes
        val strokesArr = root.optJSONArray("freehandStrokes")
        val strokes = mutableListOf<FreehandStroke>()
        if (strokesArr != null) {
            for (i in 0 until strokesArr.length()) {
                val sObj = strokesArr.getJSONObject(i)
                val ptsArr = sObj.optJSONArray("points")
                val pts = mutableListOf<StrokePoint>()
                if (ptsArr != null) {
                    for (p in 0 until ptsArr.length()) {
                        val pObj = ptsArr.getJSONObject(p)
                        pts.add(
                            StrokePoint(
                                xPt = pObj.optDouble("x", 0.0).toFloat(),
                                yPt = pObj.optDouble("y", 0.0).toFloat(),
                                pressure = pObj.optDouble("p", 1.0).toFloat()
                            )
                        )
                    }
                }
                strokes.add(
                    FreehandStroke(
                        id = sObj.optString("id"),
                        points = pts,
                        colorArgb = sObj.optLong("colorArgb", 0xFFFF0000),
                        strokeWidthPt = sObj.optDouble("strokeWidthPt", 2.0).toFloat(),
                        isEraser = sObj.optBoolean("isEraser", false)
                    )
                )
            }
        }

        return Document(
            id = id,
            title = title,
            metadata = metadata,
            sections = sections,
            settings = settings,
            comments = comments,
            freehandStrokes = strokes
        )
    }

    private fun blockFromJson(obj: JSONObject): BlockElement {
        val id = obj.optString("id")
        return when (obj.optString("type")) {
            "paragraph" -> {
                val runsArr = obj.optJSONArray("runs")
                val runs = mutableListOf<TextRun>()
                if (runsArr != null) {
                    for (i in 0 until runsArr.length()) {
                        val rObj = runsArr.getJSONObject(i)
                        val stObj = rObj.optJSONObject("style")
                        val style = if (stObj != null) {
                            TextStyleRun(
                                isBold = stObj.optBoolean("isBold", false),
                                isItalic = stObj.optBoolean("isItalic", false),
                                underline = try {
                                    UnderlineStyle.valueOf(stObj.optString("underline", "None"))
                                } catch (e: Exception) { UnderlineStyle.None },
                                isStrikethrough = stObj.optBoolean("isStrikethrough", false),
                                script = try {
                                    ScriptStyle.valueOf(stObj.optString("script", "Normal"))
                                } catch (e: Exception) { ScriptStyle.Normal },
                                fontSizePt = stObj.optDouble("fontSizePt", 11.0).toFloat(),
                                textColorArgb = stObj.optLong("textColorArgb", 0xFF1C1B1F),
                                highlightColorArgb = if (stObj.has("highlightColorArgb")) stObj.getLong("highlightColorArgb") else null,
                                letterSpacingPt = stObj.optDouble("letterSpacingPt", 0.0).toFloat(),
                                baselineShiftPt = stObj.optDouble("baselineShiftPt", 0.0).toFloat(),
                                fontFamily = stObj.optString("fontFamily", "SansSerif")
                            )
                        } else TextStyleRun()

                        val revObj = rObj.optJSONObject("revision")
                        val revision = if (revObj != null) {
                            RevisionTag(
                                type = try {
                                    RevisionType.valueOf(revObj.optString("type", "Insertion"))
                                } catch (e: Exception) { RevisionType.Insertion },
                                author = revObj.optString("author", "Author"),
                                timestamp = revObj.optLong("timestamp", System.currentTimeMillis()),
                                originalText = revObj.optString("originalText").ifEmpty { null }
                            )
                        } else null

                        runs.add(
                            TextRun(
                                id = rObj.optString("id"),
                                text = rObj.optString("text", ""),
                                style = style,
                                revision = revision
                            )
                        )
                    }
                }
                ParagraphBlock(
                    id = id,
                    runs = if (runs.isEmpty()) listOf(TextRun(text = "")) else runs,
                    alignment = try {
                        TextAlignment.valueOf(obj.optString("alignment", "Left"))
                    } catch (e: Exception) { TextAlignment.Left },
                    lineSpacingMultiplier = obj.optDouble("lineSpacingMultiplier", 1.15).toFloat(),
                    spaceBeforePt = obj.optDouble("spaceBeforePt", 0.0).toFloat(),
                    spaceAfterPt = obj.optDouble("spaceAfterPt", 6.0).toFloat(),
                    firstLineIndentPt = obj.optDouble("firstLineIndentPt", 0.0).toFloat(),
                    hangingIndentPt = obj.optDouble("hangingIndentPt", 0.0).toFloat(),
                    headingStyle = try {
                        HeadingStyle.valueOf(obj.optString("headingStyle", "Normal"))
                    } catch (e: Exception) { HeadingStyle.Normal },
                    listStyle = try {
                        ListStyle.valueOf(obj.optString("listStyle", "None"))
                    } catch (e: Exception) { ListStyle.None },
                    listLevel = obj.optInt("listLevel", 0),
                    isDropCap = obj.optBoolean("isDropCap", false)
                )
            }
            "table" -> {
                val rowsArr = obj.optJSONArray("rows")
                val rows = mutableListOf<TableRow>()
                if (rowsArr != null) {
                    for (r in 0 until rowsArr.length()) {
                        val rowObj = rowsArr.getJSONObject(r)
                        val cellsArr = rowObj.optJSONArray("cells")
                        val cells = mutableListOf<TableCell>()
                        if (cellsArr != null) {
                            for (c in 0 until cellsArr.length()) {
                                val cObj = cellsArr.getJSONObject(c)
                                cells.add(
                                    TableCell(
                                        id = cObj.optString("id"),
                                        text = cObj.optString("text", ""),
                                        colSpan = cObj.optInt("colSpan", 1),
                                        rowSpan = cObj.optInt("rowSpan", 1),
                                        backgroundColorArgb = if (cObj.has("backgroundColorArgb")) cObj.getLong("backgroundColorArgb") else null,
                                        textAlignment = try {
                                            TextAlignment.valueOf(cObj.optString("textAlignment", "Left"))
                                        } catch (e: Exception) { TextAlignment.Left },
                                        isHeader = cObj.optBoolean("isHeader", false)
                                    )
                                )
                            }
                        }
                        rows.add(TableRow(id = rowObj.optString("id"), cells = cells))
                    }
                }
                TableBlock(
                    id = id,
                    rows = rows,
                    borderColorArgb = obj.optLong("borderColorArgb", 0xFFCCCCCC),
                    borderWidthPt = obj.optDouble("borderWidthPt", 1.0).toFloat(),
                    borderStyle = try {
                        TableBorderStyle.valueOf(obj.optString("borderStyle", "Solid"))
                    } catch (e: Exception) { TableBorderStyle.Solid },
                    spaceBeforePt = obj.optDouble("spaceBeforePt", 8.0).toFloat(),
                    spaceAfterPt = obj.optDouble("spaceAfterPt", 8.0).toFloat()
                )
            }
            "image" -> {
                ImageBlock(
                    id = id,
                    uri = obj.optString("uri", ""),
                    caption = obj.optString("caption", ""),
                    widthPt = obj.optDouble("widthPt", 240.0).toFloat(),
                    heightPt = obj.optDouble("heightPt", 160.0).toFloat(),
                    rotationDegrees = obj.optDouble("rotationDegrees", 0.0).toFloat(),
                    wrappingMode = try {
                        TextWrappingMode.valueOf(obj.optString("wrappingMode", "Inline"))
                    } catch (e: Exception) { TextWrappingMode.Inline },
                    alignment = try {
                        TextAlignment.valueOf(obj.optString("alignment", "Center"))
                    } catch (e: Exception) { TextAlignment.Center },
                    spaceBeforePt = obj.optDouble("spaceBeforePt", 6.0).toFloat(),
                    spaceAfterPt = obj.optDouble("spaceAfterPt", 6.0).toFloat()
                )
            }
            "equation" -> {
                EquationBlock(
                    id = id,
                    latex = obj.optString("latex", "E = mc^2"),
                    fontSizePt = obj.optDouble("fontSizePt", 14.0).toFloat(),
                    alignment = try {
                        TextAlignment.valueOf(obj.optString("alignment", "Center"))
                    } catch (e: Exception) { TextAlignment.Center },
                    spaceBeforePt = obj.optDouble("spaceBeforePt", 8.0).toFloat(),
                    spaceAfterPt = obj.optDouble("spaceAfterPt", 8.0).toFloat()
                )
            }
            "pageBreak" -> PageBreakBlock(id = id)
            "sectionBreak" -> SectionBreakBlock(id = id, isContinuous = obj.optBoolean("isContinuous", false))
            "tableOfContents" -> TableOfContentsBlock(
                id = id,
                title = obj.optString("title", "Table of Contents"),
                showPageNumbers = obj.optBoolean("showPageNumbers", true)
            )
            "horizontalRule" -> HorizontalRuleBlock(
                id = id,
                thicknessPt = obj.optDouble("thicknessPt", 1.0).toFloat(),
                colorArgb = obj.optLong("colorArgb", 0xFFCCCCCC)
            )
            else -> ParagraphBlock(id = id)
        }
    }
}
