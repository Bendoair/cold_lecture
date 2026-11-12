package dev.coldlecture.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.coldlecture.data.DictionaryRepository
import dev.coldlecture.data.LectureDeck
import dev.coldlecture.data.NoteRepository

enum class SearchScope(val label: String) {
    All("All"),
    Slides("Slides"),
    Notes("Notes"),
    Dictionary("Dictionary"),
    ;

    fun next(): SearchScope = entries[(ordinal + 1) % entries.size]
}

sealed interface SearchHit {
    data class Slide(val pageIndex: Int, val snippet: String) : SearchHit
    data class Note(val deckId: String, val deckTitle: String, val pageIndex: Int, val snippet: String) : SearchHit
    data class Term(val entryId: Long, val term: String, val snippet: String) : SearchHit
}

/** Short excerpt of [text] around the first match of [query], collapsed to one line. */
fun snippet(text: String, query: String, radius: Int = 48): String {
    val flat = text.replace(Regex("\\s+"), " ").trim()
    val index = flat.indexOf(query, ignoreCase = true)
    if (index < 0) return flat.take(radius * 2).let { if (flat.length > it.length) "$it…" else it }
    val start = (index - radius).coerceAtLeast(0)
    val end = (index + query.length + radius).coerceAtMost(flat.length)
    return buildString {
        if (start > 0) append('…')
        append(flat, start, end)
        if (end < flat.length) append('…')
    }
}

fun searchSlides(pageTexts: Map<Int, String>, query: String, limit: Int = 12): List<SearchHit.Slide> =
    pageTexts.entries
        .sortedBy { it.key }
        .filter { it.value.contains(query, ignoreCase = true) }
        .take(limit)
        .map { SearchHit.Slide(it.key, snippet(it.value, query)) }

class LocalSearch(
    private val notes: NoteRepository,
    private val dictionary: DictionaryRepository,
) {
    var query by mutableStateOf("")
    var scope by mutableStateOf(SearchScope.All)
    var results by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var open by mutableStateOf(false)

    suspend fun run(pageTexts: Map<Int, String>, decks: List<LectureDeck>) {
        val q = query.trim()
        if (q.length < MIN_QUERY) {
            results = emptyList()
            return
        }
        val titles = decks.associate { it.id to it.title }
        val hits = mutableListOf<SearchHit>()
        if (scope == SearchScope.All || scope == SearchScope.Slides) {
            hits += searchSlides(pageTexts, q)
        }
        if (scope == SearchScope.All || scope == SearchScope.Notes) {
            hits += notes.search(q).map { SearchHit.Note(it.deckId, titles[it.deckId] ?: "Unknown deck", it.pageIndex, snippet(it.text, q)) }
        }
        if (scope == SearchScope.All || scope == SearchScope.Dictionary) {
            hits += dictionary.search(q).map { SearchHit.Term(it.id, it.term, snippet(it.definition, q)) }
        }
        results = hits
    }

    fun clear() {
        query = ""
        results = emptyList()
        open = false
    }

    companion object {
        const val MIN_QUERY = 2
    }
}
