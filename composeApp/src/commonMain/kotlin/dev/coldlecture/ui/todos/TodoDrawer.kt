package dev.coldlecture.ui.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coldlecture.data.TodoItem
import dev.coldlecture.data.TodoRepository
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.PanelShape
import dev.coldlecture.ui.theme.Lecture
import kotlinx.coroutines.launch

@Composable
fun TodoDrawer(
    todos: List<TodoItem>,
    repository: TodoRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    val open = todos.count { !it.done }
    val doneCount = todos.size - open

    fun submit() {
        val text = draft
        if (text.isBlank()) return
        draft = ""
        scope.launch { repository.add(text) }
    }

    Column(
        modifier
            .width(380.dp)
            .shadow(18.dp, PanelShape)
            .clip(PanelShape)
            .background(colors.panel)
            .border(1.dp, colors.hairline, PanelShape),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "After class",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                MonoLabel("$open open · $doneCount done")
            }
            if (doneCount > 0) {
                Text(
                    "Clear done",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { scope.launch { repository.clearCompleted() } }.padding(6.dp),
                )
            }
            IconAction(Icons.Default.Close, "Close todos", onClick = onClose)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.panelRaised)
                .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily)
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 9.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                            submit()
                            true
                        } else {
                            false
                        }
                    },
                decorationBox = { inner ->
                    if (draft.isEmpty()) Text("Add a task for after class…", style = style.copy(color = colors.muted))
                    inner()
                },
            )
            IconAction(Icons.Default.Add, "Add todo", onClick = ::submit, enabled = draft.isNotBlank(), tint = colors.accent)
        }
        if (todos.isEmpty()) {
            Text(
                "Nothing yet. Jot down what to revisit once the lecture ends.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                modifier = Modifier.padding(14.dp),
            )
        } else {
            LazyColumn(Modifier.heightIn(max = 340.dp).padding(bottom = 8.dp)) {
                items(todos, key = { it.id }) { item ->
                    TodoRow(
                        item = item,
                        onToggle = { scope.launch { repository.setDone(item.id, !item.done) } },
                        onDelete = { scope.launch { repository.delete(item.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoRow(item: TodoItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    val colors = Lecture.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (item.done) colors.ok else Color.Transparent)
                .border(1.5.dp, if (item.done) colors.ok else colors.muted, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.done) Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Text(
            item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.done) colors.muted else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        IconAction(Icons.Default.Delete, "Delete todo", onClick = onDelete, size = 26.dp)
    }
}
