package dev.coldlecture.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        LectureDeck::class,
        SlideNote::class,
        DictEntry::class,
        ConceptLink::class,
        TodoItem::class,
        AppSetting::class,
    ],
    version = 1,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun noteDao(): NoteDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun todoDao(): TodoDao
    abstract fun settingsDao(): SettingsDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

const val DATABASE_FILE_NAME = "cold_lecture.db"
