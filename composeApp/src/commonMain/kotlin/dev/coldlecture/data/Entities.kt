package dev.coldlecture.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

object DeckSource {
    const val BUNDLED = "bundled"
    const val IMPORTED = "imported"
}

@Entity(tableName = "decks")
data class LectureDeck(
    @PrimaryKey val id: String,
    val title: String,
    val courseCode: String,
    val courseTitle: String,
    val source: String,
    val pageCount: Int,
    val lastPage: Int = 0,
    val createdAt: Long,
)

@Entity(tableName = "slide_notes", primaryKeys = ["deckId", "pageIndex"])
data class SlideNote(
    val deckId: String,
    val pageIndex: Int,
    val text: String,
    val updatedAt: Long,
)

@Entity(tableName = "dict_entries", indices = [Index(value = ["term"])])
data class DictEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val definition: String,
    val tag: String = "",
    val createdAt: Long,
)

/** Undirected edge between two dictionary entries, stored with `fromId < toId`. */
@Entity(
    tableName = "concept_links",
    primaryKeys = ["fromId", "toId"],
    indices = [Index(value = ["toId"])],
    foreignKeys = [
        ForeignKey(entity = DictEntry::class, parentColumns = ["id"], childColumns = ["fromId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DictEntry::class, parentColumns = ["id"], childColumns = ["toId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ConceptLink(
    val fromId: Long,
    val toId: Long,
)

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val done: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String,
)
