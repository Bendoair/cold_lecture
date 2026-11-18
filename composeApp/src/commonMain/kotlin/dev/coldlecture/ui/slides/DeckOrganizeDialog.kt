package dev.coldlecture.ui.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.coldlecture.data.LectureDeck
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.Pill
import dev.coldlecture.ui.theme.Lecture

data class DeckOrganizeDraft(
    val title: String,
    val courseCode: String,
    val courseTitle: String,
)

@Composable
fun DeckOrganizeDialog(
    heading: String,
    draft: DeckOrganizeDraft,
    knownCourses: List<Pair<String, String>>,
    confirmLabel: String,
    onConfirm: (DeckOrganizeDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Lecture.colors
    var title by remember(draft) { mutableStateOf(draft.title) }
    var courseCode by remember(draft) { mutableStateOf(draft.courseCode) }
    var courseTitle by remember(draft) { mutableStateOf(draft.courseTitle) }
    val canSave = title.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(min = 360.dp, max = 440.dp)
                .shadow(24.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(colors.panel)
                .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                heading,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Course code groups decks in the picker. Reuse a course to keep lectures together.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Field(
                label = "Deck title",
                value = title,
                onValueChange = { title = it },
                placeholder = "Lecture title",
            )
            Field(
                label = "Course code",
                value = courseCode,
                onValueChange = { courseCode = it },
                placeholder = "e.g. MAT-304",
            )
            Field(
                label = "Course title",
                value = courseTitle,
                onValueChange = { courseTitle = it },
                placeholder = "e.g. Vector Calculus",
            )
            if (knownCourses.isNotEmpty()) {
                MonoLabel("Existing courses")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    knownCourses.take(8).forEach { (code, name) ->
                        Text(
                            if (name.isBlank()) code else "$code · $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    courseCode = code
                                    if (name.isNotBlank()) courseTitle = name
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Pill(
                    text = "Cancel",
                    container = colors.panelRaised,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onDismiss,
                )
                Pill(
                    text = confirmLabel,
                    container = if (canSave) colors.accent else colors.hairline,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    borderColor = null,
                    enabled = canSave,
                    onClick = {
                        onConfirm(
                            DeckOrganizeDraft(
                                title = title.trim(),
                                courseCode = courseCode.trim(),
                                courseTitle = courseTitle.trim(),
                            ),
                        )
                    },
                )
            }
        }
    }
}

fun knownCoursesFrom(decks: List<LectureDeck>): List<Pair<String, String>> =
    decks
        .map { it.courseCode.trim() to it.courseTitle.trim() }
        .filter { it.first.isNotEmpty() }
        .distinctBy { it.first.lowercase() }
        .sortedBy { it.first.lowercase() }

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = Lecture.colors
    val style = TextStyle(
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MonoLabel(label)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.panelRaised)
                .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, style = style.copy(color = colors.muted))
                    }
                    inner()
                },
            )
        }
    }
}
