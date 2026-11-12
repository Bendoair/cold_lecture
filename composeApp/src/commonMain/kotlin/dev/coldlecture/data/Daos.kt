package dev.coldlecture.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LectureDeck>>

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(deck: LectureDeck)

    @Query("UPDATE decks SET lastPage = :page WHERE id = :deckId")
    suspend fun setLastPage(deckId: String, page: Int)

    @Query("UPDATE decks SET pageCount = :pageCount WHERE id = :deckId")
    suspend fun setPageCount(deckId: String, pageCount: Int)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM slide_notes WHERE deckId = :deckId AND pageIndex = :pageIndex")
    suspend fun get(deckId: String, pageIndex: Int): SlideNote?

    @Query("SELECT pageIndex FROM slide_notes WHERE deckId = :deckId AND text != ''")
    fun observePagesWithNotes(deckId: String): Flow<List<Int>>

    @Upsert
    suspend fun upsert(note: SlideNote)

    @Query("DELETE FROM slide_notes WHERE deckId = :deckId AND pageIndex = :pageIndex")
    suspend fun delete(deckId: String, pageIndex: Int)

    @Query("SELECT * FROM slide_notes WHERE text LIKE '%' || :query || '%' ORDER BY deckId, pageIndex LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<SlideNote>
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dict_entries ORDER BY term COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<DictEntry>>

    @Query("SELECT COUNT(*) FROM dict_entries")
    suspend fun count(): Int

    @Query("SELECT * FROM dict_entries WHERE term = :term COLLATE NOCASE LIMIT 1")
    suspend fun findByTerm(term: String): DictEntry?

    @Insert
    suspend fun insert(entry: DictEntry): Long

    @Query("UPDATE dict_entries SET term = :term, definition = :definition, tag = :tag WHERE id = :id")
    suspend fun update(id: Long, term: String, definition: String, tag: String)

    @Query("DELETE FROM dict_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM concept_links")
    fun observeLinks(): Flow<List<ConceptLink>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: ConceptLink)

    @Delete
    suspend fun deleteLink(link: ConceptLink)

    @Query(
        "SELECT * FROM dict_entries WHERE term LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%' " +
            "ORDER BY term COLLATE NOCASE LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int): List<DictEntry>
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done ASC, createdAt DESC")
    fun observeAll(): Flow<List<TodoItem>>

    @Query("SELECT COUNT(*) FROM todos")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: TodoItem): Long

    @Query("UPDATE todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearCompleted()
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun put(setting: AppSetting)
}
