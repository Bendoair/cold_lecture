package dev.coldlecture.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RepositoriesTest {
    private lateinit var database: AppDatabase
    private lateinit var repos: Repositories

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        repos = Repositories(database)
    }

    @AfterTest
    fun tearDown() = database.close()

    private suspend fun insertSampleDeck() {
        repos.decks.add(
            LectureDeck(
                id = SAMPLE_DECK_ID,
                title = "Sample",
                courseCode = "MAT-304",
                courseTitle = "Calc",
                source = DeckSource.BUNDLED,
                pageCount = 9,
                createdAt = 1L,
            ),
        )
        repos.notes.save(SAMPLE_DECK_ID, 6, "Laplace note")
    }

    @Test
    fun notesUpsertAndBlankDeletes() = runTest {
        repos.notes.save("deck", 0, "first")
        repos.notes.save("deck", 0, "second")
        assertEquals("second", repos.notes.load("deck", 0))
        assertEquals(listOf(0), repos.notes.pagesWithNotes("deck").first())

        repos.notes.save("deck", 0, "   ")
        assertEquals("", repos.notes.load("deck", 0))
        assertEquals(emptyList(), repos.notes.pagesWithNotes("deck").first())
    }

    @Test
    fun notesSearchMatchesSubstring() = runTest {
        repos.notes.save("deck", 3, "Divergence theorem trick")
        repos.notes.save("deck", 4, "unrelated")
        val hits = repos.notes.search("theorem")
        assertEquals(listOf(3), hits.map { it.pageIndex })
    }

    @Test
    fun dictionaryRejectsDuplicateAndEmptyTerms() = runTest {
        assertIs<DictSaveResult.Saved>(repos.dictionary.create("Limit", "def", "Calc"))
        assertEquals(DictSaveResult.DuplicateTerm, repos.dictionary.create("limit", "other", ""))
        assertEquals(DictSaveResult.EmptyTerm, repos.dictionary.create("   ", "x", ""))
    }

    @Test
    fun dictionaryUpdateKeepsOwnTermButBlocksClash() = runTest {
        val a = (repos.dictionary.create("Kernel", "", "") as DictSaveResult.Saved).id
        repos.dictionary.create("Image", "", "")
        assertIs<DictSaveResult.Saved>(repos.dictionary.update(a, "Kernel", "null space", "LinAlg"))
        assertEquals(DictSaveResult.DuplicateTerm, repos.dictionary.update(a, "image", "", ""))
        assertEquals("null space", repos.dictionary.entries.first().first { it.id == a }.definition)
    }

    @Test
    fun linksAreUndirectedAndCascadeOnDelete() = runTest {
        val a = (repos.dictionary.create("A", "", "") as DictSaveResult.Saved).id
        val b = (repos.dictionary.create("B", "", "") as DictSaveResult.Saved).id
        repos.dictionary.link(b, a)
        repos.dictionary.link(a, b)
        repos.dictionary.link(a, a)
        assertEquals(listOf(ConceptLink(minOf(a, b), maxOf(a, b))), repos.dictionary.links.first())

        repos.dictionary.unlink(b, a)
        assertTrue(repos.dictionary.links.first().isEmpty())

        repos.dictionary.link(a, b)
        repos.dictionary.delete(a)
        assertTrue(repos.dictionary.links.first().isEmpty())
        assertEquals(listOf("B"), repos.dictionary.entries.first().map { it.term })
    }

    @Test
    fun todosLifecycle() = runTest {
        repos.todos.add("  review flux  ")
        repos.todos.add("")
        val item = repos.todos.todos.first().single()
        assertEquals("review flux", item.text)

        repos.todos.setDone(item.id, true)
        assertTrue(repos.todos.todos.first().single().done)

        repos.todos.clearCompleted()
        assertTrue(repos.todos.todos.first().isEmpty())
    }

    @Test
    fun settingsRoundTrip() = runTest {
        repos.settings.put(SettingsRepository.DARK_THEME, "true")
        repos.settings.put(SettingsRepository.DARK_THEME, "false")
        assertEquals("false", repos.settings.get(SettingsRepository.DARK_THEME))
    }

    @Test
    fun themePreferenceMigratesLegacyDarkFlag() = runTest {
        assertEquals(ThemePreference.System, repos.settings.themePreference())
        repos.settings.put(SettingsRepository.DARK_THEME, "true")
        assertEquals(ThemePreference.Dark, repos.settings.themePreference())
        repos.settings.put(SettingsRepository.THEME_PREFERENCE, ThemePreference.Light.storage)
        assertEquals(ThemePreference.Light, repos.settings.themePreference())
    }

    @Test
    fun deckDeleteRemovesNotes() = runTest {
        insertSampleDeck()
        assertTrue(repos.notes.load(SAMPLE_DECK_ID, 6).isNotBlank())
        repos.decks.delete(SAMPLE_DECK_ID)
        assertTrue(repos.decks.decks.first().isEmpty())
        assertEquals("", repos.notes.load(SAMPLE_DECK_ID, 6))
    }

    @Test
    fun deckLastPagePersists() = runTest {
        insertSampleDeck()
        repos.decks.setLastPage(SAMPLE_DECK_ID, 4)
        assertEquals(4, repos.decks.decks.first().single().lastPage)
    }

    @Test
    fun deckUpdateDetailsRenamesAndRegroups() = runTest {
        insertSampleDeck()
        val deck = repos.decks.decks.first().single()
        val updated = repos.decks.updateDetails(
            deck,
            title = " Stokes review ",
            courseCode = " mat-401 ",
            courseTitle = " Advanced calculus ",
        )
        assertEquals("Stokes review", updated.title)
        assertEquals("mat-401", updated.courseCode)
        assertEquals("Advanced calculus", updated.courseTitle)
        assertEquals(updated, repos.decks.decks.first().single())
    }
}
