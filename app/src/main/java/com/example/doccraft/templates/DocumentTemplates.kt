package com.example.doccraft.templates

import com.example.doccraft.model.*
import java.util.UUID

object DocumentTemplates {

    fun createBlankDocument(title: String = "Untitled Document"): Document {
        return Document(
            id = UUID.randomUUID().toString(),
            title = title,
            sections = listOf(
                Section(
                    blocks = listOf(
                        ParagraphBlock(
                            runs = listOf(TextRun(text = "Start typing your document here..."))
                        )
                    )
                )
            )
        )
    }

    fun createResumeTemplate(): Document {
        return Document(
            id = UUID.randomUUID().toString(),
            title = "Modern Professional Resume",
            sections = listOf(
                Section(
                    pageSetup = PageSetup(
                        paperSize = PaperSize.A4,
                        margins = Margins.Moderate
                    ),
                    blocks = listOf(
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Title,
                            alignment = TextAlignment.Center,
                            spaceAfterPt = 2f,
                            runs = listOf(
                                TextRun(
                                    text = "ALEXANDRA MORGAN",
                                    style = TextStyleRun(
                                        fontSizePt = 22f,
                                        isBold = true,
                                        textColorArgb = 0xFF1A237E
                                    )
                                )
                            )
                        ),
                        ParagraphBlock(
                            alignment = TextAlignment.Center,
                            spaceAfterPt = 8f,
                            runs = listOf(
                                TextRun(
                                    text = "Principal Software Architect | Cloud & Mobile Platforms\n",
                                    style = TextStyleRun(fontSizePt = 11f, isItalic = true, textColorArgb = 0xFF455A64)
                                ),
                                TextRun(
                                    text = "alexandra.morgan@example.com  •  +1 (555) 019-2834  •  San Francisco, CA",
                                    style = TextStyleRun(fontSizePt = 9.5f, textColorArgb = 0xFF607D8B)
                                )
                            )
                        ),
                        HorizontalRuleBlock(thicknessPt = 1.5f, colorArgb = 0xFF1A237E),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 10f,
                            spaceAfterPt = 4f,
                            runs = listOf(
                                TextRun(
                                    text = "EXECUTIVE SUMMARY",
                                    style = TextStyleRun(fontSizePt = 13f, isBold = true, textColorArgb = 0xFF1A237E)
                                )
                            )
                        ),
                        ParagraphBlock(
                            spaceAfterPt = 8f,
                            lineSpacingMultiplier = 1.2f,
                            runs = listOf(
                                TextRun(
                                    text = "Visionary software architect with 12+ years designing mission-critical distributed systems and enterprise mobile applications. Proven track record leading high-velocity engineering organizations, establishing scalable architectures, and delivering frictionless user experiences.",
                                    style = TextStyleRun(fontSizePt = 10f, textColorArgb = 0xFF263238)
                                )
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 10f,
                            spaceAfterPt = 4f,
                            runs = listOf(
                                TextRun(
                                    text = "CORE COMPETENCIES & TECHNICAL SKILLS",
                                    style = TextStyleRun(fontSizePt = 13f, isBold = true, textColorArgb = 0xFF1A237E)
                                )
                            )
                        ),
                        TableBlock(
                            rows = listOf(
                                TableRow(cells = listOf(
                                    TableCell(text = "Languages & Runtimes", isHeader = true, backgroundColorArgb = 0xFFE8EAF6),
                                    TableCell(text = "Kotlin, Java, Swift, Rust, TypeScript, C++")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Frameworks & Architecture", isHeader = true, backgroundColorArgb = 0xFFE8EAF6),
                                    TableCell(text = "Jetpack Compose, Clean Architecture, MVI, Coroutines, Flow")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Cloud & Infrastructure", isHeader = true, backgroundColorArgb = 0xFFE8EAF6),
                                    TableCell(text = "Google Cloud, Kubernetes, Terraform, gRPC, CI/CD Pipelines")
                                ))
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 12f,
                            spaceAfterPt = 4f,
                            runs = listOf(
                                TextRun(
                                    text = "PROFESSIONAL EXPERIENCE",
                                    style = TextStyleRun(fontSizePt = 13f, isBold = true, textColorArgb = 0xFF1A237E)
                                )
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading2,
                            spaceBeforePt = 6f,
                            spaceAfterPt = 2f,
                            runs = listOf(
                                TextRun(text = "Lead Systems Architect  |  Apex Technologies  ", style = TextStyleRun(fontSizePt = 11f, isBold = true)),
                                TextRun(text = "(2021 – Present)", style = TextStyleRun(fontSizePt = 10f, isItalic = true, textColorArgb = 0xFF78909C))
                            )
                        ),
                        ParagraphBlock(
                            listStyle = ListStyle.Bullet,
                            spaceAfterPt = 2f,
                            runs = listOf(
                                TextRun(text = "Architected next-generation document processing suite serving 4.5M active daily enterprise users.")
                            )
                        ),
                        ParagraphBlock(
                            listStyle = ListStyle.Bullet,
                            spaceAfterPt = 2f,
                            runs = listOf(
                                TextRun(text = "Reduced layout engine memory footprint by 42% via lazy virtualized page streaming.")
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading2,
                            spaceBeforePt = 6f,
                            spaceAfterPt = 2f,
                            runs = listOf(
                                TextRun(text = "Senior Android Engineer  |  CloudScale Labs  ", style = TextStyleRun(fontSizePt = 11f, isBold = true)),
                                TextRun(text = "(2017 – 2021)", style = TextStyleRun(fontSizePt = 10f, isItalic = true, textColorArgb = 0xFF78909C))
                            )
                        ),
                        ParagraphBlock(
                            listStyle = ListStyle.Bullet,
                            spaceAfterPt = 2f,
                            runs = listOf(
                                TextRun(text = "Implemented high-performance OpenXML file parser handling multi-megabyte docs without UI stutters.")
                            )
                        )
                    )
                )
            )
        )
    }

    fun createBusinessProposalTemplate(): Document {
        return Document(
            id = UUID.randomUUID().toString(),
            title = "Executive Business Proposal",
            sections = listOf(
                Section(
                    pageSetup = PageSetup(paperSize = PaperSize.A4, margins = Margins.Normal),
                    headerFooter = HeaderFooter(
                        headerText = "Confidential Commercial Proposal",
                        footerText = "DocCraft Enterprise Solutions  |  Page [PAGE] of [NUMPAGES]"
                    ),
                    blocks = listOf(
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Title,
                            spaceBeforePt = 16f,
                            spaceAfterPt = 6f,
                            runs = listOf(
                                TextRun(
                                    text = "BUSINESS PROPOSAL & ROADMAP",
                                    style = TextStyleRun(fontSizePt = 24f, isBold = true, textColorArgb = 0xFF0D47A1)
                                )
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Subtitle,
                            spaceAfterPt = 16f,
                            runs = listOf(
                                TextRun(
                                    text = "Modernization of Mobile Document Workflows and Security Infrastructure",
                                    style = TextStyleRun(fontSizePt = 14f, textColorArgb = 0xFF546E7A)
                                )
                            )
                        ),
                        HorizontalRuleBlock(thicknessPt = 2f, colorArgb = 0xFF0D47A1),
                        TableOfContentsBlock(),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 14f,
                            spaceAfterPt = 6f,
                            runs = listOf(
                                TextRun(text = "1. Executive Summary", style = TextStyleRun(fontSizePt = 16f, isBold = true, textColorArgb = 0xFF0D47A1))
                            )
                        ),
                        ParagraphBlock(
                            lineSpacingMultiplier = 1.25f,
                            spaceAfterPt = 8f,
                            runs = listOf(
                                TextRun(
                                    text = "In an increasingly mobile-first work environment, enterprise knowledge workers require desktop-class document authoring, formatting, and proofing tools directly on Android tablets, foldables, and smartphones. This proposal outlines the phased deployment of DocCraft Enterprise."
                                )
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 14f,
                            spaceAfterPt = 6f,
                            runs = listOf(
                                TextRun(text = "2. Investment & Timeline Matrix", style = TextStyleRun(fontSizePt = 16f, isBold = true, textColorArgb = 0xFF0D47A1))
                            )
                        ),
                        TableBlock(
                            rows = listOf(
                                TableRow(cells = listOf(
                                    TableCell(text = "Phase", isHeader = true, backgroundColorArgb = 0xFFBBDEFB),
                                    TableCell(text = "Deliverables", isHeader = true, backgroundColorArgb = 0xFFBBDEFB),
                                    TableCell(text = "Timeline", isHeader = true, backgroundColorArgb = 0xFFBBDEFB),
                                    TableCell(text = "Investment", isHeader = true, backgroundColorArgb = 0xFFBBDEFB)
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Phase 1"),
                                    TableCell(text = "Core AST Engine & Virtual Pagination"),
                                    TableCell(text = "Q1 2026"),
                                    TableCell(text = "$45,000")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Phase 2"),
                                    TableCell(text = "DOCX OpenXML & Vector PDF Pipeline"),
                                    TableCell(text = "Q2 2026"),
                                    TableCell(text = "$38,000")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Phase 3"),
                                    TableCell(text = "Collaboration, Track Changes & S-Pen"),
                                    TableCell(text = "Q3 2026"),
                                    TableCell(text = "$52,000")
                                ))
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 14f,
                            spaceAfterPt = 6f,
                            runs = listOf(
                                TextRun(text = "3. Technical Formula & Metrics", style = TextStyleRun(fontSizePt = 16f, isBold = true, textColorArgb = 0xFF0D47A1))
                            )
                        ),
                        ParagraphBlock(
                            runs = listOf(
                                TextRun(text = "Target throughput performance metric calculation:")
                            )
                        ),
                        EquationBlock(latex = "Throughput = \\frac{Pages \\times FPS}{Memory_{MB}} \\ge 99.8\\%")
                    )
                )
            )
        )
    }

    fun createAcademicEssayTemplate(): Document {
        return Document(
            id = UUID.randomUUID().toString(),
            title = "Academic Research Paper (APA)",
            sections = listOf(
                Section(
                    pageSetup = PageSetup(
                        paperSize = PaperSize.Letter,
                        margins = Margins.Normal
                    ),
                    headerFooter = HeaderFooter(
                        headerText = "QUANTUM ARCHITECTURE IN MOBILE OPERATING SYSTEMS",
                        footerText = "[PAGE]"
                    ),
                    blocks = listOf(
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Title,
                            alignment = TextAlignment.Center,
                            spaceBeforePt = 36f,
                            spaceAfterPt = 12f,
                            runs = listOf(
                                TextRun(
                                    text = "Asynchronous Virtual Memory Management for Large-Scale Document Synthesis",
                                    style = TextStyleRun(fontSizePt = 18f, isBold = true, fontFamily = "Serif")
                                )
                            )
                        ),
                        ParagraphBlock(
                            alignment = TextAlignment.Center,
                            spaceAfterPt = 24f,
                            runs = listOf(
                                TextRun(
                                    text = "Dr. Katherine Vance, Department of Computer Science\nUniversity Institute of Technology\nOctober 2026",
                                    style = TextStyleRun(fontSizePt = 11f, fontFamily = "Serif")
                                )
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            alignment = TextAlignment.Center,
                            spaceBeforePt = 12f,
                            spaceAfterPt = 6f,
                            runs = listOf(
                                TextRun(text = "Abstract", style = TextStyleRun(fontSizePt = 13f, isBold = true, fontFamily = "Serif"))
                            )
                        ),
                        ParagraphBlock(
                            alignment = TextAlignment.Justify,
                            lineSpacingMultiplier = 1.5f,
                            firstLineIndentPt = 36f,
                            spaceAfterPt = 12f,
                            runs = listOf(
                                TextRun(
                                    text = "This paper presents a formal evaluation of virtualized layout pagination under constrained mobile execution environments. By decomposing rich documents into an immutable Abstract Syntax Tree (AST) coupled with lazy viewport projection, we demonstrate deterministic O(1) frame rendering latency regardless of overall page volume.",
                                    style = TextStyleRun(fontSizePt = 11f, fontFamily = "Serif")
                                )
                            )
                        ),
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Heading1,
                            spaceBeforePt = 14f,
                            spaceAfterPt = 6f,
                            runs = listOf(
                                TextRun(text = "1. Introduction and Problem Definition", style = TextStyleRun(fontSizePt = 13f, isBold = true, fontFamily = "Serif"))
                            )
                        ),
                        ParagraphBlock(
                            alignment = TextAlignment.Justify,
                            lineSpacingMultiplier = 1.5f,
                            firstLineIndentPt = 36f,
                            spaceAfterPt = 12f,
                            runs = listOf(
                                TextRun(
                                    text = "Modern mobile devices possess hardware acceleration capabilities exceeding desktop workstations of the prior decade. However, arbitrary text layout and glyph measurement remains a compute-intensive CPU task. When parsing complex multi-level tables and vector equations, standard reflow pipelines cause noticeable UI stutter.",
                                    style = TextStyleRun(fontSizePt = 11f, fontFamily = "Serif")
                                )
                            )
                        ),
                        EquationBlock(
                            latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}"
                        )
                    )
                )
            )
        )
    }

    fun createInvoiceTemplate(): Document {
        return Document(
            id = UUID.randomUUID().toString(),
            title = "Clean Invoice & Billing Statement",
            sections = listOf(
                Section(
                    pageSetup = PageSetup(paperSize = PaperSize.A4, margins = Margins.Moderate),
                    blocks = listOf(
                        ParagraphBlock(
                            headingStyle = HeadingStyle.Title,
                            spaceAfterPt = 4f,
                            runs = listOf(
                                TextRun(
                                    text = "INVOICE",
                                    style = TextStyleRun(fontSizePt = 24f, isBold = true, textColorArgb = 0xFF2E7D32)
                                )
                            )
                        ),
                        ParagraphBlock(
                            spaceAfterPt = 12f,
                            runs = listOf(
                                TextRun(
                                    text = "Invoice Number: #INV-2026-0891\nDate: October 10, 2026\nPayment Due: Net 30 Days",
                                    style = TextStyleRun(fontSizePt = 10f, textColorArgb = 0xFF555555)
                                )
                            )
                        ),
                        TableBlock(
                            rows = listOf(
                                TableRow(cells = listOf(
                                    TableCell(text = "Billed From:", isHeader = true, backgroundColorArgb = 0xFFE8F5E9),
                                    TableCell(text = "Billed To:", isHeader = true, backgroundColorArgb = 0xFFE8F5E9)
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "DocCraft Studio Labs Inc.\n100 Innovation Way\nSan Francisco, CA 94105"),
                                    TableCell(text = "Global Tech Enterprises Ltd.\n500 Corporate Blvd\nNew York, NY 10001")
                                ))
                            )
                        ),
                        ParagraphBlock(spaceBeforePt = 10f, runs = listOf(TextRun(text = "Itemized Services", style = TextStyleRun(isBold = true, fontSizePt = 12f)))),
                        TableBlock(
                            rows = listOf(
                                TableRow(cells = listOf(
                                    TableCell(text = "Description", isHeader = true, backgroundColorArgb = 0xFFE8F5E9),
                                    TableCell(text = "Hours", isHeader = true, backgroundColorArgb = 0xFFE8F5E9),
                                    TableCell(text = "Rate ($/hr)", isHeader = true, backgroundColorArgb = 0xFFE8F5E9),
                                    TableCell(text = "Total ($)", isHeader = true, backgroundColorArgb = 0xFFE8F5E9)
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Mobile Typography Engine Development"),
                                    TableCell(text = "40"),
                                    TableCell(text = "$150.00"),
                                    TableCell(text = "$6,000.00")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "High-Fidelity PDF & DOCX File Pipeline"),
                                    TableCell(text = "25"),
                                    TableCell(text = "$150.00"),
                                    TableCell(text = "$3,750.00")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Interactive S-Pen & Canvas Integration"),
                                    TableCell(text = "15"),
                                    TableCell(text = "$150.00"),
                                    TableCell(text = "$2,250.00")
                                )),
                                TableRow(cells = listOf(
                                    TableCell(text = "Total Amount Due", colSpan = 3, isHeader = true, backgroundColorArgb = 0xFFC8E6C9),
                                    TableCell(text = "$12,000.00", isHeader = true, backgroundColorArgb = 0xFFC8E6C9)
                                ))
                            )
                        ),
                        ParagraphBlock(
                            spaceBeforePt = 16f,
                            runs = listOf(
                                TextRun(
                                    text = "Thank you for your business! Please remit wire transfers to First National Bank (Routing: 121000358, Acct: 987654321).",
                                    style = TextStyleRun(fontSizePt = 9.5f, isItalic = true, textColorArgb = 0xFF666666)
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
