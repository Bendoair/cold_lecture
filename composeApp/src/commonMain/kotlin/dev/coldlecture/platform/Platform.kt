package dev.coldlecture.platform

import androidx.compose.ui.graphics.ImageBitmap
import dev.coldlecture.data.AppDatabase

/** An opened PDF. Implementations serialize access internally; callers may render from any coroutine. */
interface PdfDocumentHandle {
    val pageCount: Int
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): ImageBitmap
    suspend fun pageText(pageIndex: Int): String
    fun close()
}

data class ImportedDeck(val fileName: String, val pageCount: Int)

/** Opens bundled/imported PDFs and owns where imported PDF bytes are stored. */
interface DeckFiles {
    suspend fun openBytes(bytes: ByteArray): PdfDocumentHandle
    suspend fun openStored(deckId: String): PdfDocumentHandle

    /** Shows the platform picker and stores the chosen PDF under [deckId]; null when cancelled. */
    suspend fun pickAndStore(deckId: String): ImportedDeck?
}

enum class LockStrength(val label: String) {
    Desktop("Desktop lock"),
    SoftBrowser("Soft lock (browser)"),
}

enum class PresenceEvent { Left, Returned, FullscreenExited }

interface FocusPlatform {
    val strength: LockStrength
    fun engage()
    fun release()
    fun setPresenceListener(listener: (PresenceEvent) -> Unit)

    /** Called by the platform when the user tries to close/leave the app while locked. */
    fun setExitAttemptListener(listener: () -> Unit)
}

class AppPlatform(
    val database: AppDatabase,
    val deckFiles: DeckFiles,
    val focus: FocusPlatform,
)
