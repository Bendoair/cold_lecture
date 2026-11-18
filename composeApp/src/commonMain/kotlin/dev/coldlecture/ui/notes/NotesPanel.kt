package dev.coldlecture.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coldlecture.notes.MdBlock
import dev.coldlecture.notes.MdSpan
import dev.coldlecture.notes.NoteEditorState
import dev.coldlecture.notes.SaveStatus
import dev.coldlecture.notes.parseMarkdown
import dev.coldlecture.ui.components.Dot
import dev.coldlecture.ui.theme.Lecture

@Composable
fun NotesBody(editor: NoteEditorState, modifier: Modifier = Modifier) {
    var preview by remember { mutableStateOf(false) }
    val mono = Lecture.mono
    val noteStyle = TextStyle(
        fontFamily = mono,
        fontSize = 12.5.sp,
        lineHeight = 19.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Column(modifier.padding(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Lecture.colors.panelRaised)
                .border(1.dp, Lecture.colors.hairline, RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            if (preview) {
                MarkdownPreview(editor.value.text, Modifier.fillMaxSize().verticalScroll(rememberScrollState()))
            } else {
                BasicTextField(
                    value = editor.value,
                    onValueChange = editor::edit,
                    textStyle = noteStyle,
                    cursorBrush = SolidColor(Lecture.colors.accent),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (editor.value.text.isEmpty() && editor.status != SaveStatus.Loading) {
                            Text(
                                "Notes for this slide… (markdown: **bold**, *italic*, - list)",
                                style = noteStyle.copy(color = Lecture.colors.muted),
                            )
                        }
                        inner()
                    },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SaveIndicator(editor.status)
            Box(Modifier.weight(1f))
            ToolbarToggle("B", "Bold", enabled = !preview, fontWeight = FontWeight.Bold) { editor.toggleBold() }
            ToolbarToggle("I", "Italic", enabled = !preview, fontStyle = FontStyle.Italic) { editor.toggleItalic() }
            ToolbarToggle("•—", "Bullet list", enabled = !preview) { editor.toggleBullet() }
            Box(Modifier.width(4.dp))
            ToolbarToggle(if (preview) "Edit" else "Preview", if (preview) "Edit note" else "Preview markdown", selected = preview) {
                preview = !preview
            }
        }
    }
}

@Composable
private fun SaveIndicator(status: SaveStatus) {
    val colors = Lecture.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        when (status) {
            SaveStatus.Saved -> Icon(Icons.Default.Check, contentDescription = null, tint = colors.ok, modifier = Modifier.size(13.dp))
            SaveStatus.Failed -> Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
            else -> Dot(colors.streak, size = 6.dp)
        }
        Text(
            when (status) {
                SaveStatus.Loading -> "Loading…"
                SaveStatus.Saved -> "Auto-saved"
                SaveStatus.Pending -> "Editing…"
                SaveStatus.Saving -> "Saving…"
                SaveStatus.Failed -> "Save failed — keep typing to retry"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (status == SaveStatus.Saved) colors.ok else colors.muted,
        )
    }
}

@Composable
private fun ToolbarToggle(
    label: String,
    description: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle = FontStyle.Normal,
    onClick: () -> Unit,
) {
    val colors = Lecture.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.accentSoft else colors.panelRaised)
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = fontWeight, fontStyle = fontStyle),
            color = when {
                !enabled -> colors.hairline
                selected -> colors.accent
                else -> colors.muted
            },
        )
    }
}

@Composable
fun MarkdownPreview(text: String, modifier: Modifier = Modifier) {
    val colors = Lecture.colors
    val mono = Lecture.mono
    if (text.isBlank()) {
        Text("Nothing to preview yet.", style = MaterialTheme.typography.bodySmall, color = colors.muted, modifier = modifier)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        parseMarkdown(text).forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    block.spans.toAnnotated(mono, colors.accentSoft),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                is MdBlock.Bullet -> Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = colors.accent, modifier = Modifier.width(14.dp))
                    Text(
                        block.spans.toAnnotated(mono, colors.accentSoft),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                is MdBlock.Numbered -> Row {
                    Text("${block.number}.", style = MaterialTheme.typography.bodyMedium, color = colors.accent, modifier = Modifier.width(22.dp))
                    Text(
                        block.spans.toAnnotated(mono, colors.accentSoft),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                is MdBlock.Paragraph -> Text(
                    block.spans.toAnnotated(mono, colors.accentSoft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                MdBlock.Blank -> Box(Modifier.size(6.dp))
            }
        }
    }
}

private fun List<MdSpan>.toAnnotated(mono: FontFamily, codeBackground: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        for (span in this@toAnnotated) {
            withStyle(
                SpanStyle(
                    fontWeight = if (span.bold) FontWeight.Bold else null,
                    fontStyle = if (span.italic) FontStyle.Italic else null,
                    fontFamily = if (span.code) mono else null,
                    background = if (span.code) codeBackground else androidx.compose.ui.graphics.Color.Unspecified,
                ),
            ) { append(span.text) }
        }
    }
