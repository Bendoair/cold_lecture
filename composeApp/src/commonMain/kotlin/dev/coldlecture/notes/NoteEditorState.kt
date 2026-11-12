package dev.coldlecture.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SaveStatus { Loading, Saved, Pending, Saving, Failed }

/** Store abstraction so the editor can be tested without a database. */
interface NoteStore {
    suspend fun load(deckId: String, pageIndex: Int): String
    suspend fun save(deckId: String, pageIndex: Int, text: String)
}

/**
 * Text of the note for the slide currently shown. Edits are saved after [debounceMillis] of
 * inactivity; switching slides saves the previous slide's pending edit immediately.
 */
class NoteEditorState(
    private val store: NoteStore,
    private val scope: CoroutineScope,
    private val debounceMillis: Long = 600,
) {
    var value by mutableStateOf(TextFieldValue(""))
        private set
    var status by mutableStateOf(SaveStatus.Loading)
        private set

    private var key: Pair<String, Int>? = null
    private var saveJob: Job? = null
    private var loadJob: Job? = null

    fun bind(deckId: String, pageIndex: Int) {
        val newKey = deckId to pageIndex
        if (newKey == key) return
        flush()
        key = newKey
        status = SaveStatus.Loading
        value = TextFieldValue("")
        loadJob?.cancel()
        loadJob = scope.launch {
            val text = runCatching { store.load(deckId, pageIndex) }.getOrDefault("")
            if (key == newKey) {
                value = TextFieldValue(text, TextRange(text.length))
                status = SaveStatus.Saved
            }
        }
    }

    fun edit(newValue: TextFieldValue) {
        val changed = newValue.text != value.text
        value = newValue
        if (!changed || status == SaveStatus.Loading) return
        status = SaveStatus.Pending
        saveJob?.cancel()
        val target = key ?: return
        val text = newValue.text
        saveJob = scope.launch {
            delay(debounceMillis)
            persist(target, text)
        }
    }

    /** Saves a pending edit right away (slide change, app shutdown). */
    fun flush() {
        if (status != SaveStatus.Pending) return
        saveJob?.cancel()
        val target = key ?: return
        val text = value.text
        scope.launch { persist(target, text) }
    }

    fun toggleBold() = wrapSelection("**")
    fun toggleItalic() = wrapSelection("*")

    fun toggleBullet() {
        val text = value.text
        val cursor = value.selection.start
        val lineStart = if (cursor == 0) 0 else text.lastIndexOf('\n', cursor - 1) + 1
        val updated: String
        val shift: Int
        if (text.startsWith("- ", lineStart)) {
            updated = text.removeRange(lineStart, lineStart + 2)
            shift = -2
        } else {
            updated = text.substring(0, lineStart) + "- " + text.substring(lineStart)
            shift = 2
        }
        val newCursor = (cursor + shift).coerceIn(lineStart, updated.length)
        edit(TextFieldValue(updated, TextRange(newCursor)))
    }

    private fun wrapSelection(marker: String) {
        val text = value.text
        val selection = value.selection
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)
        val updated = text.substring(0, start) + marker + text.substring(start, end) + marker + text.substring(end)
        val newSelection = if (start == end) TextRange(start + marker.length) else TextRange(start + marker.length, end + marker.length)
        edit(TextFieldValue(updated, newSelection))
    }

    private suspend fun persist(target: Pair<String, Int>, text: String) {
        if (key == target) status = SaveStatus.Saving
        val ok = runCatching { store.save(target.first, target.second, text) }.isSuccess
        if (key == target && value.text == text) status = if (ok) SaveStatus.Saved else SaveStatus.Failed
    }
}
