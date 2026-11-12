package dev.coldlecture.ui.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coldlecture.data.ConceptLink
import dev.coldlecture.data.DictEntry
import dev.coldlecture.data.DictSaveResult
import dev.coldlecture.data.DictionaryRepository
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.Pill
import dev.coldlecture.ui.theme.Lecture
import kotlinx.coroutines.launch

/** Selection and editing state; lives in the shell so search results can open an entry. */
class DictionaryUiState {
    var selectedId by mutableStateOf<Long?>(null)
    var editing by mutableStateOf<DictEntry?>(null)
    var creating by mutableStateOf(false)
    var filter by mutableStateOf("")

    fun startCreate(prefillTerm: String = "") {
        editing = null
        creating = true
        draftTerm = prefillTerm
    }

    var draftTerm by mutableStateOf("")
}

fun linkedIds(entryId: Long, links: List<ConceptLink>): Set<Long> =
    links.mapNotNullTo(mutableSetOf()) { link ->
        when (entryId) {
            link.fromId -> link.toId
            link.toId -> link.fromId
            else -> null
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryBody(
    state: DictionaryUiState,
    entries: List<DictEntry>,
    links: List<ConceptLink>,
    repository: DictionaryRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val selected = entries.firstOrNull { it.id == state.selectedId }
    Column(modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            state.creating || state.editing != null -> EntryForm(
                initial = state.editing,
                prefillTerm = state.draftTerm,
                onCancel = {
                    state.creating = false
                    state.editing = null
                },
                onSave = { term, tag, definition, onError ->
                    scope.launch {
                        val existing = state.editing
                        val result = if (existing != null) {
                            repository.update(existing.id, term, definition, tag)
                        } else {
                            repository.create(term, definition, tag)
                        }
                        when (result) {
                            is DictSaveResult.Saved -> {
                                state.selectedId = result.id
                                state.creating = false
                                state.editing = null
                            }
                            DictSaveResult.DuplicateTerm -> onError("\"${term.trim()}\" is already in your dictionary.")
                            DictSaveResult.EmptyTerm -> onError("A term needs a name.")
                        }
                    }
                },
            )
            selected != null -> EntryDetail(
                entry = selected,
                entries = entries,
                linked = linkedIds(selected.id, links),
                onBack = { state.selectedId = null },
                onOpen = { state.selectedId = it },
                onEdit = { state.editing = selected },
                onDelete = {
                    scope.launch {
                        repository.delete(selected.id)
                        state.selectedId = null
                    }
                },
                onLink = { other -> scope.launch { repository.link(selected.id, other) } },
                onUnlink = { other -> scope.launch { repository.unlink(selected.id, other) } },
            )
        }

        FilterField(state.filter, onChange = { state.filter = it })
        val query = state.filter.trim()
        val visible = if (query.isEmpty()) entries else entries.filter {
            it.term.contains(query, ignoreCase = true) || it.tag.contains(query, ignoreCase = true)
        }
        if (visible.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (entries.isEmpty()) "Your dictionary is empty." else "No term matches \"$query\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = Lecture.colors.muted,
                )
                if (query.isNotEmpty()) {
                    Pill("Add \"$query\"", icon = Icons.Default.Add, onClick = { state.startCreate(query) })
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 520.dp)) {
                items(visible, key = { it.id }) { entry ->
                    TermRow(
                        entry = entry,
                        linkCount = linkedIds(entry.id, links).size,
                        selected = entry.id == state.selectedId,
                        onClick = {
                            state.creating = false
                            state.editing = null
                            state.selectedId = entry.id
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryDetail(
    entry: DictEntry,
    entries: List<DictEntry>,
    linked: Set<Long>,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLink: (Long) -> Unit,
    onUnlink: (Long) -> Unit,
) {
    val colors = Lecture.colors
    var confirmDelete by remember(entry.id) { mutableStateOf(false) }
    var linkMenu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.panelRaised)
            .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back to all terms", onClick = onBack, size = 24.dp)
            Text(
                entry.term,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Lecture.mono),
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.tag.isNotBlank()) {
                Pill(entry.tag, container = colors.accentSoft, contentColor = colors.accent, borderColor = null)
            }
        }
        Text(
            entry.definition.ifBlank { "No definition yet." },
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.definition.isBlank()) colors.muted else MaterialTheme.colorScheme.onSurface,
        )
        MonoLabel("Linked concepts")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.filter { it.id in linked }.forEach { other ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.panel)
                        .border(1.dp, colors.hairline, RoundedCornerShape(6.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        other.term,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { onOpen(other.id) }.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    )
                    IconAction(Icons.Default.Close, "Unlink ${other.term}", onClick = { onUnlink(other.id) }, size = 20.dp)
                }
            }
            Box {
                Pill("Link", icon = Icons.Default.Add, onClick = { linkMenu = true })
                DropdownMenu(expanded = linkMenu, onDismissRequest = { linkMenu = false }) {
                    val candidates = entries.filter { it.id != entry.id && it.id !in linked }
                    if (candidates.isEmpty()) {
                        DropdownMenuItem(text = { Text("Nothing left to link", color = colors.muted) }, onClick = { linkMenu = false }, enabled = false)
                    }
                    candidates.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.term, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                linkMenu = false
                                onLink(candidate.id)
                            },
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Edit", icon = Icons.Default.Edit, onClick = onEdit)
            if (confirmDelete) {
                Pill(
                    "Confirm delete",
                    icon = Icons.Default.Delete,
                    container = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    borderColor = null,
                    onClick = onDelete,
                )
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = colors.muted, modifier = Modifier.clickable { confirmDelete = false }.padding(4.dp))
            } else {
                Pill("Delete", icon = Icons.Default.Delete, onClick = { confirmDelete = true })
            }
        }
    }
}

@Composable
private fun EntryForm(
    initial: DictEntry?,
    prefillTerm: String,
    onCancel: () -> Unit,
    onSave: (term: String, tag: String, definition: String, onError: (String) -> Unit) -> Unit,
) {
    val colors = Lecture.colors
    var term by remember(initial, prefillTerm) { mutableStateOf(initial?.term ?: prefillTerm) }
    var tag by remember(initial) { mutableStateOf(initial?.tag ?: "") }
    var definition by remember(initial) { mutableStateOf(initial?.definition ?: "") }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.panelRaised)
            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoLabel(if (initial == null) "New term" else "Edit term", color = colors.accent)
        FormField("Term", term, onChange = { term = it; error = null })
        FormField("Tag (e.g. Calculus)", tag, onChange = { tag = it })
        FormField("Definition", definition, onChange = { definition = it }, singleLine = false, minHeight = 64)
        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(
                "Save",
                container = colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                borderColor = null,
                onClick = { onSave(term, tag, definition) { error = it } },
            )
            Pill("Cancel", onClick = onCancel)
        }
    }
}

@Composable
private fun FormField(placeholder: String, value: String, onChange: (String) -> Unit, singleLine: Boolean = true, minHeight: Int = 0) {
    val colors = Lecture.colors
    val style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.panel)
            .border(1.dp, colors.hairline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, style = style.copy(color = colors.muted))
            inner()
        },
    )
}

@Composable
private fun FilterField(value: String, onChange: (String) -> Unit) {
    val colors = Lecture.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.panelRaised)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(14.dp))
        val style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Filter terms or tags", style = style.copy(color = colors.muted))
                inner()
            },
        )
        if (value.isNotEmpty()) IconAction(Icons.Default.Close, "Clear filter", onClick = { onChange("") }, size = 18.dp)
    }
}

@Composable
private fun TermRow(entry: DictEntry, linkCount: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = Lecture.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.term,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.tag.isNotBlank()) {
            Text(entry.tag, style = MaterialTheme.typography.labelSmall, color = colors.muted, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
        }
        if (linkCount > 0) {
            Text("·$linkCount", style = MaterialTheme.typography.labelSmall, color = colors.accent, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
