package com.example.doccraft.data

import android.content.Context
import com.example.doccraft.model.Document
import com.example.doccraft.templates.DocumentTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(id: String): Document? = withContext(Dispatchers.IO) {
        val entity = documentDao.getDocumentById(id) ?: return@withContext null
        try {
            DocumentJsonAdapter.fromJson(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDocument(document: Document) = withContext(Dispatchers.IO) {
        val json = DocumentJsonAdapter.toJson(document)
        val plainText = document.allBlocks()
            .filterIsInstance<com.example.doccraft.model.ParagraphBlock>()
            .joinToString(separator = " ") { it.plainText }

        val words = plainText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val snippet = plainText.take(120).trim()

        val entity = DocumentEntity(
            id = document.id,
            title = document.title,
            jsonContent = json,
            lastModified = System.currentTimeMillis(),
            wordCount = words,
            pageCount = 1, // updated dynamically by editor
            previewSnippet = if (snippet.isEmpty()) "Empty document" else snippet
        )
        documentDao.insertOrUpdateDocument(entity)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        documentDao.toggleFavorite(id)
    }

    suspend fun seedInitialTemplatesIfEmpty() = withContext(Dispatchers.IO) {
        val templates = listOf(
            DocumentTemplates.createResumeTemplate(),
            DocumentTemplates.createBusinessProposalTemplate(),
            DocumentTemplates.createAcademicEssayTemplate(),
            DocumentTemplates.createInvoiceTemplate()
        )
        for (doc in templates) {
            saveDocument(doc)
        }
    }
}
