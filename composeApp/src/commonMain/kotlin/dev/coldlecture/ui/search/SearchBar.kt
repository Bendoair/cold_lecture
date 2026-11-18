package dev.coldlecture.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coldlecture.search.LocalSearch
import dev.coldlecture.search.SearchHit
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.PanelShape
import dev.coldlecture.ui.components.Pill
import dev.coldlecture.ui.theme.Lecture

@Composable
fun SearchBar(
    search: LocalSearch,
    focusRequester: FocusRequester,
    shortcutLabel: String,
    onSubmitFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    Row(
        modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.panel)
                .border(1.dp, if (search.open) colors.accent else colors.hairline, RoundedCornerShape(10.dp))
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
            val style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily)
            BasicTextField(
                value = search.query,
                onValueChange = {
                    search.query = it
                    search.open = true
                },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused && search.query.isNotBlank()) search.open = true }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Escape -> {
                                search.clear(); true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                onSubmitFirst(); true
                            }
                            else -> false
                        }
                    },
                decorationBox = { inner ->
                    if (search.query.isEmpty()) {
                        Text(
                            "insta-search slides, notes and dictionary ($shortcutLabel)",
                            style = style.copy(color = colors.muted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                },
            )
            if (search.query.isNotEmpty()) IconAction(Icons.Default.Close, "Clear search", onClick = search::clear, size = 24.dp)
            Text(
                shortcutLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(colors.panelRaised).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Pill(
            "Scope: ${search.scope.label}",
            onClick = {
                search.scope = search.scope.next()
                search.open = search.query.isNotBlank()
            },
        )
    }
}

@Composable
fun SearchResults(
    search: LocalSearch,
    onSelect: (SearchHit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    val q = search.query.trim()
    Column(
        modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .shadow(16.dp, PanelShape)
            .clip(PanelShape)
            .background(colors.panel)
            .border(1.dp, colors.hairline, PanelShape)
            .padding(vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
            MonoLabel("Local results · ${search.scope.label}", modifier = Modifier.weight(1f))
            MonoLabel("Enter opens first · Esc closes")
        }
        when {
            q.length < LocalSearch.MIN_QUERY -> Hint("Type at least ${LocalSearch.MIN_QUERY} characters.")
            search.results.isEmpty() -> Hint("Nothing in this deck's slides, your notes or dictionary matches \"$q\".")
            else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(search.results) { hit -> ResultRow(hit, onClick = { onSelect(hit) }) }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Lecture.colors.muted, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
}

@Composable
private fun ResultRow(hit: SearchHit, onClick: () -> Unit) {
    val colors = Lecture.colors
    val (kind, title, snippet) = when (hit) {
        is SearchHit.Slide -> Triple("Slide", "Slide ${hit.pageIndex + 1}", hit.snippet)
        is SearchHit.Note -> Triple("Note", "${hit.deckTitle} · slide ${hit.pageIndex + 1}", hit.snippet)
        is SearchHit.Term -> Triple("Term", hit.term, hit.snippet)
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            kind.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent,
            modifier = Modifier.widthIn(min = 44.dp).padding(top = 2.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (snippet.isNotBlank()) {
                Text(snippet, style = MaterialTheme.typography.bodySmall, color = colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
