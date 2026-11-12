package dev.coldlecture.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import dev.coldlecture.data.DeckSource
import dev.coldlecture.data.LectureDeck
import dev.coldlecture.data.Repositories
import dev.coldlecture.data.SAMPLE_DECK_RESOURCE
import dev.coldlecture.data.SettingsRepository
import dev.coldlecture.data.nowMillis
import dev.coldlecture.platform.DeckFiles
import dev.coldlecture.platform.PdfDocumentHandle
import dev.coldlecture.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Active deck, current page, rendered page cache and per-page text used by local search. */
class LectureController(
    private val repos: Repositories,
    private val deckFiles: DeckFiles,
    private val focus: FocusSession,
    private val scope: CoroutineScope,
) {
    var activeDeck by mutableStateOf<LectureDeck?>(null)
        private set
    var document by mutableStateOf<PdfDocumentHandle?>(null)
        private set
    var page by mutableIntStateOf(0)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var importing by mutableStateOf(false)
        private set

    val pageCount: Int get() = document?.pageCount ?: activeDeck?.pageCount ?: 0

    /** Extracted text per page of the active deck (filled in the background after opening). */
    val pageTexts = mutableStateMapOf<Int, String>()

    private val renderCache = LinkedHashMap<String, ImageBitmap>()
    private var textJob: Job? = null

    fun openDeck(deck: LectureDeck, startPage: Int? = null) {
        if (deck.id == activeDeck?.id && document != null) {
            if (startPage != null) goTo(startPage)
            return
        }
        scope.launch {
            loading = true
            error = null
            document?.close()
            document = null
            renderCache.clear()
            pageTexts.clear()
            textJob?.cancel()
            activeDeck = deck
            try {
                val handle = if (deck.source == DeckSource.BUNDLED) {
                    deckFiles.openBytes(Res.readBytes(SAMPLE_DECK_RESOURCE))
                } else {
                    deckFiles.openStored(deck.id)
                }
                document = handle
                page = (startPage ?: deck.lastPage).coerceIn(0, (handle.pageCount - 1).coerceAtLeast(0))
                if (startPage != null) repos.decks.setLastPage(deck.id, page)
                if (handle.pageCount != deck.pageCount) repos.decks.setPageCount(deck.id, handle.pageCount)
                repos.settings.put(SettingsRepository.ACTIVE_DECK, deck.id)
                textJob = scope.launch { extractTexts(handle) }
            } catch (t: Throwable) {
                error = "Could not open \"${deck.title}\": ${t.message ?: t::class.simpleName}"
            } finally {
                loading = false
            }
        }
    }

    fun goTo(target: Int) {
        val count = pageCount
        if (count == 0) return
        val clamped = target.coerceIn(0, count - 1)
        if (clamped == page) return
        page = clamped
        activeDeck?.let { deck -> scope.launch { repos.decks.setLastPage(deck.id, clamped) } }
    }

    fun next() = goTo(page + 1)
    fun previous() = goTo(page - 1)

    suspend fun render(pageIndex: Int, widthPx: Int): ImageBitmap? {
        val doc = document ?: return null
        val key = "$pageIndex@$widthPx"
        renderCache[key]?.let { return it }
        val bitmap = doc.renderPage(pageIndex, widthPx)
        if (document !== doc) return null
        renderCache[key] = bitmap
        while (renderCache.size > RENDER_CACHE_SIZE) renderCache.remove(renderCache.keys.first())
        return bitmap
    }

    fun importDeck(onImported: (LectureDeck) -> Unit = {}) {
        if (importing) return
        scope.launch {
            importing = true
            error = null
            focus.suppressPresence = true
            try {
                val id = "deck-" + nowMillis().toString(36) + "-" + Random.nextInt(0, 1 shl 20).toString(36)
                val imported = deckFiles.pickAndStore(id) ?: return@launch
                val deck = LectureDeck(
                    id = id,
                    title = imported.fileName.removeSuffix(".pdf").removeSuffix(".PDF"),
                    courseCode = activeDeck?.courseCode ?: "COURSE",
                    courseTitle = activeDeck?.courseTitle ?: "Imported lecture",
                    source = DeckSource.IMPORTED,
                    pageCount = imported.pageCount,
                    createdAt = nowMillis(),
                )
                repos.decks.add(deck)
                openDeck(deck)
                onImported(deck)
            } catch (t: Throwable) {
                error = "Import failed: ${t.message ?: t::class.simpleName}"
            } finally {
                focus.suppressPresence = false
                importing = false
            }
        }
    }

    fun removeDeck(deck: LectureDeck, remaining: List<LectureDeck>) {
        scope.launch {
            val wasActive = activeDeck?.id == deck.id
            if (wasActive) {
                textJob?.cancel()
                document?.close()
                document = null
                renderCache.clear()
                pageTexts.clear()
                activeDeck = null
                page = 0
                error = null
            }
            runCatching { deckFiles.deleteStored(deck.id) }
            repos.decks.delete(deck.id)
            if (wasActive) {
                val next = remaining.firstOrNull { it.id != deck.id }
                if (next != null) openDeck(next) else repos.settings.put(SettingsRepository.ACTIVE_DECK, "")
            }
        }
    }

    private suspend fun extractTexts(handle: PdfDocumentHandle) {
        for (i in 0 until handle.pageCount) {
            if (document !== handle) return
            pageTexts[i] = runCatching { handle.pageText(i) }.getOrDefault("")
        }
    }

    private companion object {
        const val RENDER_CACHE_SIZE = 8
    }
}
