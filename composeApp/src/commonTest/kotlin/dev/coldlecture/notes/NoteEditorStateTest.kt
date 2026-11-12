@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.coldlecture.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class MemoryNoteStore : NoteStore {
    val notes = mutableMapOf<Pair<String, Int>, String>()
    var saves = 0

    override suspend fun load(deckId: String, pageIndex: Int) = notes[deckId to pageIndex].orEmpty()

    override suspend fun save(deckId: String, pageIndex: Int, text: String) {
        saves++
        notes[deckId to pageIndex] = text
    }
}

class NoteEditorStateTest {
    @Test
    fun loadsNoteForBoundSlide() = runTest {
        val store = MemoryNoteStore().apply { notes["d" to 2] = "hello" }
        val editor = NoteEditorState(store, this)
        editor.bind("d", 2)
        runCurrent()
        assertEquals("hello", editor.value.text)
        assertEquals(SaveStatus.Saved, editor.status)
    }

    @Test
    fun debouncesSaves() = runTest {
        val store = MemoryNoteStore()
        val editor = NoteEditorState(store, this, debounceMillis = 600)
        editor.bind("d", 0)
        runCurrent()

        editor.edit(TextFieldValue("a"))
        advanceTimeBy(300)
        editor.edit(TextFieldValue("ab"))
        advanceTimeBy(300)
        assertEquals(0, store.saves)
        assertEquals(SaveStatus.Pending, editor.status)

        advanceUntilIdle()
        assertEquals(1, store.saves)
        assertEquals("ab", store.notes["d" to 0])
        assertEquals(SaveStatus.Saved, editor.status)
    }

    @Test
    fun switchingSlidesFlushesPendingEdit() = runTest {
        val store = MemoryNoteStore().apply { notes["d" to 1] = "next" }
        val editor = NoteEditorState(store, this, debounceMillis = 10_000)
        editor.bind("d", 0)
        runCurrent()
        editor.edit(TextFieldValue("unsaved"))

        editor.bind("d", 1)
        runCurrent()
        assertEquals("unsaved", store.notes["d" to 0])
        assertEquals("next", editor.value.text)
    }

    @Test
    fun formattingHelpers() = runTest {
        val editor = NoteEditorState(MemoryNoteStore(), this)
        editor.bind("d", 0)
        runCurrent()

        editor.edit(TextFieldValue("word", TextRange(0, 4)))
        editor.toggleBold()
        assertEquals("**word**", editor.value.text)

        editor.edit(TextFieldValue("one\ntwo", TextRange(5)))
        editor.toggleBullet()
        assertEquals("one\n- two", editor.value.text)
        editor.toggleBullet()
        assertEquals("one\ntwo", editor.value.text)
    }
}
