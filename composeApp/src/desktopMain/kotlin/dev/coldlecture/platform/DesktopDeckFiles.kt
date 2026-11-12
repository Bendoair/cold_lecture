package dev.coldlecture.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class DesktopDeckFiles(
    private val decksDir: File,
    private val parentWindow: () -> Frame?,
) : DeckFiles {

    override suspend fun openBytes(bytes: ByteArray): PdfDocumentHandle =
        withContext(Dispatchers.IO) { PdfBoxDocument(Loader.loadPDF(bytes)) }

    override suspend fun openStored(deckId: String): PdfDocumentHandle =
        withContext(Dispatchers.IO) { PdfBoxDocument(Loader.loadPDF(deckFile(deckId))) }

    override suspend fun pickAndStore(deckId: String): ImportedDeck? {
        val picked = withContext(Dispatchers.Main) {
            val dialog = FileDialog(parentWindow(), "Import lecture PDF", FileDialog.LOAD).apply {
                setFilenameFilter { _, name -> name.endsWith(".pdf", ignoreCase = true) }
                file = "*.pdf"
                isVisible = true
            }
            val name = dialog.file ?: return@withContext null
            File(dialog.directory, name)
        } ?: return null

        return withContext(Dispatchers.IO) {
            val target = deckFile(deckId)
            picked.copyTo(target, overwrite = true)
            val pageCount = Loader.loadPDF(target).use { it.numberOfPages }
            ImportedDeck(fileName = picked.name, pageCount = pageCount)
        }
    }

    private fun deckFile(deckId: String): File = File(decksDir.apply { mkdirs() }, "$deckId.pdf")
}

private class PdfBoxDocument(private val document: PDDocument) : PdfDocumentHandle {
    private val renderer = PDFRenderer(document)
    private val mutex = Mutex()

    override val pageCount: Int = document.numberOfPages

    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pageWidthPt = document.getPage(pageIndex).mediaBox.width
            val scale = (targetWidthPx / pageWidthPt).coerceIn(0.5f, 6f)
            renderer.renderImage(pageIndex, scale, ImageType.RGB).toComposeImageBitmap()
        }
    }

    override suspend fun pageText(pageIndex: Int): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            PDFTextStripper().apply {
                startPage = pageIndex + 1
                endPage = pageIndex + 1
            }.getText(document).trim()
        }
    }

    override fun close() {
        document.close()
    }
}
