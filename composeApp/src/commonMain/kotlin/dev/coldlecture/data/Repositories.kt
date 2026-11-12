package dev.coldlecture.data

import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

class DeckRepository(private val dao: DeckDao) {
    val decks: Flow<List<LectureDeck>> = dao.observeAll()

    suspend fun add(deck: LectureDeck) = dao.upsert(deck)
    suspend fun setLastPage(deckId: String, page: Int) = dao.setLastPage(deckId, page)
    suspend fun setPageCount(deckId: String, pageCount: Int) = dao.setPageCount(deckId, pageCount)
}

class NoteRepository(private val dao: NoteDao) {
    suspend fun load(deckId: String, pageIndex: Int): String = dao.get(deckId, pageIndex)?.text.orEmpty()

    fun pagesWithNotes(deckId: String): Flow<List<Int>> = dao.observePagesWithNotes(deckId)

    suspend fun save(deckId: String, pageIndex: Int, text: String) {
        if (text.isBlank()) {
            dao.delete(deckId, pageIndex)
        } else {
            dao.upsert(SlideNote(deckId, pageIndex, text, nowMillis()))
        }
    }

    suspend fun search(query: String, limit: Int = 20): List<SlideNote> = dao.search(query, limit)
}

sealed interface DictSaveResult {
    data class Saved(val id: Long) : DictSaveResult
    data object DuplicateTerm : DictSaveResult
    data object EmptyTerm : DictSaveResult
}

class DictionaryRepository(private val dao: DictionaryDao) {
    val entries: Flow<List<DictEntry>> = dao.observeAll()
    val links: Flow<List<ConceptLink>> = dao.observeLinks()

    suspend fun create(term: String, definition: String, tag: String): DictSaveResult {
        val cleanTerm = term.trim()
        if (cleanTerm.isEmpty()) return DictSaveResult.EmptyTerm
        if (dao.findByTerm(cleanTerm) != null) return DictSaveResult.DuplicateTerm
        val id = dao.insert(DictEntry(term = cleanTerm, definition = definition.trim(), tag = tag.trim(), createdAt = nowMillis()))
        return DictSaveResult.Saved(id)
    }

    suspend fun update(id: Long, term: String, definition: String, tag: String): DictSaveResult {
        val cleanTerm = term.trim()
        if (cleanTerm.isEmpty()) return DictSaveResult.EmptyTerm
        val clash = dao.findByTerm(cleanTerm)
        if (clash != null && clash.id != id) return DictSaveResult.DuplicateTerm
        dao.update(id, cleanTerm, definition.trim(), tag.trim())
        return DictSaveResult.Saved(id)
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun link(a: Long, b: Long) {
        if (a != b) dao.insertLink(edge(a, b))
    }

    suspend fun unlink(a: Long, b: Long) = dao.deleteLink(edge(a, b))

    suspend fun search(query: String, limit: Int = 20): List<DictEntry> = dao.search(query, limit)

    private fun edge(a: Long, b: Long) = ConceptLink(fromId = minOf(a, b), toId = maxOf(a, b))
}

class TodoRepository(private val dao: TodoDao) {
    val todos: Flow<List<TodoItem>> = dao.observeAll()

    suspend fun add(text: String) {
        val clean = text.trim()
        if (clean.isNotEmpty()) dao.insert(TodoItem(text = clean, createdAt = nowMillis()))
    }

    suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clearCompleted() = dao.clearCompleted()
}

class SettingsRepository(private val dao: SettingsDao) {
    suspend fun get(key: String): String? = dao.get(key)
    suspend fun put(key: String, value: String) = dao.put(AppSetting(key, value))

    companion object {
        const val DARK_THEME = "dark_theme"
        const val ACTIVE_DECK = "active_deck"
    }
}

class Repositories(database: AppDatabase) {
    val decks = DeckRepository(database.deckDao())
    val notes = NoteRepository(database.noteDao())
    val dictionary = DictionaryRepository(database.dictionaryDao())
    val todos = TodoRepository(database.todoDao())
    val settings = SettingsRepository(database.settingsDao())
}
