package com.example.doccraft.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val jsonContent: String,
    val lastModified: Long,
    val wordCount: Int,
    val pageCount: Int,
    val previewSnippet: String,
    val isFavorite: Boolean = false
)
