package com.example.doccraft.io

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.example.doccraft.engine.PaginationEngine
import com.example.doccraft.model.Document
import java.io.FileOutputStream

class DocumentPrintAdapter(
    private val context: Context,
    private val document: Document
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val pages = PaginationEngine.paginate(document)
        val pInfo = PrintDocumentInfo.Builder("${document.title}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pages.size)
            .build()

        callback?.onLayoutFinished(pInfo, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) {
            callback?.onWriteFailed("Destination file descriptor is null")
            return
        }

        try {
            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                PdfExporter.exportToPdf(document, outputStream)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}
