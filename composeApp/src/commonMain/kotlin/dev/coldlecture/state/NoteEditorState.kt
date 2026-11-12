package dev.coldlecture.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.coldlecture.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SaveStatus { Idle, Pending, Saving, Saved, Failed }

/** Note text for the current (deck, slide); edits are written back after a short pause. */
class NoteEditorState(
    private val repo: NoteRepository,
    private val scope: CoroutineScope,
    private val debounceMillis: Long = 600,
) {
    var value by mutableStateOf(TextFieldValue(""))
        private set
    var status by mutableStateOf(SaveStatus.Idle)
        private set
    var loaded by mutableStateOf(false)
        private set

    private var deckId: String? = null
    private var pageIndex: Int = -1
    private var saveJob: Job? = null
    private var loadJob: Job? = null

    fun bind(deckId: String, pageIndex: Int) {
        if (deckId == this.deckId && pageIndex == this.pageIndex) return
        flush()
        this.deckId = deckId
        this.pageIndex = pageIndex
        loaded = false
        loadJob?.cancel()
        loadJob = scope.launch {
            val text = repo.load(deckId, pageIndex)
            value = TextFieldValue(text, TextRange(text.length))
            status = SaveStatus.Idle
            loaded = true
        }
    }

    fun edit(newValue: TextFieldValue) {
        val changed = newValue.text != value.text
        value = newValue
        if (!changed || !loaded) return
        status = SaveStatus.Pending
        saveJob?.cancel()
        val key = deckId ?: return
        val page = pageIndex
        saveJob = scope.launch {
            delay(debounceMillis)
            persist(key, page, newValue.text)
        }
    }

    /** Writes a pending edit immediately, e.g. before switching slides. */
    fun flush() {
        if (status != SaveStatus.Pending) return
        saveJob?.cancel()
        val key = deckId ?: return
        val page = pageIndex
        val text = value.text
        scope.launch { persist(key, page, text) }
    }

    fun wrapSelection(marker: String) {
        val v = value
        val start = v.selection.min
        val end = v.selection.max
        val selected = v.text.substring(start, end)
        val text = v.text.replaceRange(start, end, marker + selected + marker)
        val cursor = if (selected.isEmpty()) start + marker.length else end + marker.length * 2
        edit(TextFieldValue(text, TextRange(cursor)))
    }

    fun toggleLinePrefix(prefix: String) {
        val v = value
        val lineStart = v.text.lastIndexOf('\n', (v.selection.min - 1).coerceAtLeast(0)).let { if (v.selection.min == 0) 0 else it + 1 }
        val hasPrefix = v.text.startsWith(prefix, lineStart)
        val text = if (hasPrefix) {
            v.text.removeRange(lineStart, lineStart + prefix.length)
        } else {
            v.text.substring(0, lineStart) + prefix + v.text.substring(lineStart)
        }
        val shift = if (hasPrefix) -prefix.length else prefix.length
        val cursor = (v.selection.max + shift).coerceIn(0, text.length)
        edit(TextFieldValue(text, TextRange(cursor)))
    }

    private suspend fun persist(deckId: String, pageIndex: Int, text: String) {
        val isCurrent = deckId == this.deckId && pageIndex == this.pageIndex
        if (isCurrent) status = SaveStatus.Saving
        val result = runCatching { repo.save(deckId, pageIndex, text) }
        if (deckId == this.deckId && pageIndex == this.pageIndex && status == SaveStatus.Saving) {
            status = if (result.isSuccess) SaveStatus.Saved else SaveStatus.Failed
        }
    }
}
