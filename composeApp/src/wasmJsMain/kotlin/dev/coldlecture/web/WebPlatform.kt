package dev.coldlecture.web

import androidx.compose.ui.graphics.ImageBitmap
import dev.coldlecture.platform.DeckFiles
import dev.coldlecture.platform.FocusPlatform
import dev.coldlecture.platform.ImportedDeck
import dev.coldlecture.platform.LockStrength
import dev.coldlecture.platform.PdfDocumentHandle
import dev.coldlecture.platform.PresenceEvent
import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

class WebDeckFiles : DeckFiles {
    override suspend fun openBytes(bytes: ByteArray): PdfDocumentHandle =
        PdfJsDocument(pdfOpenBase64(Base64.encode(bytes)).await<JsNumber>().toInt())

    override suspend fun openStored(deckId: String): PdfDocumentHandle =
        PdfJsDocument(pdfOpenStored(deckId).await<JsNumber>().toInt())

    override suspend fun pickAndStore(deckId: String): ImportedDeck? {
        val fileName = pickPdfFile().await<JsString?>()?.toString() ?: return null
        val pageCount = importPickedFile(deckId).await<JsNumber>().toInt()
        return ImportedDeck(fileName = fileName, pageCount = pageCount)
    }
}

private class PdfJsDocument(private val handle: Int) : PdfDocumentHandle {
    private val mutex = Mutex()

    override val pageCount: Int = pdfPageCount(handle)

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): ImageBitmap = mutex.withLock {
        val dataUrl = pdfRenderPage(handle, pageIndex, targetWidthPx).await<JsString>().toString()
        Base64.decode(dataUrl.substringAfter(",")).decodeToImageBitmap()
    }

    override suspend fun pageText(pageIndex: Int): String =
        pdfPageText(handle, pageIndex).await<JsString>().toString()

    override fun close() = pdfClose(handle)
}

/** Fullscreen + leave detection only: a browser tab cannot stop the user from switching away. */
class WebFocusPlatform : FocusPlatform {
    override val strength = LockStrength.SoftBrowser

    private var locked = false
    private var presenceListener: (PresenceEvent) -> Unit = {}

    init {
        watchPresence { event ->
            if (!locked) return@watchPresence
            when (event) {
                "left" -> presenceListener(PresenceEvent.Left)
                "returned" -> presenceListener(PresenceEvent.Returned)
                "fullscreen-exited" -> presenceListener(PresenceEvent.FullscreenExited)
            }
        }
    }

    override fun engage() {
        locked = true
        setLockActive(true)
        requestFullscreen()
    }

    override fun release() {
        locked = false
        setLockActive(false)
        exitFullscreen()
    }

    override fun setPresenceListener(listener: (PresenceEvent) -> Unit) {
        presenceListener = listener
    }

    // Closing a tab can only be confirmed by the browser's own beforeunload prompt.
    override fun setExitAttemptListener(listener: () -> Unit) = Unit
}
