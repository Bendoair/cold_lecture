package dev.coldlecture.ui.shell

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.coldlecture.data.LectureDeck
import dev.coldlecture.state.FocusPhase
import dev.coldlecture.state.FocusSession
import dev.coldlecture.state.formatClock
import dev.coldlecture.state.formatMinutes
import dev.coldlecture.ui.components.Badge
import dev.coldlecture.ui.components.Dot
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.Pill
import dev.coldlecture.ui.components.VerticalDivider
import dev.coldlecture.ui.theme.Lecture

@Composable
fun TopBar(
    session: FocusSession,
    deck: LectureDeck?,
    compact: Boolean,
    sidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    dark: Boolean,
    onToggleDark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (session.isActive) {
            Pill(
                "Locked",
                icon = Icons.Default.Lock,
                container = colors.streakSoft,
                contentColor = colors.streak,
                borderColor = null,
                onClick = session::requestExit,
            )
        } else {
            Pill(
                "Lock in",
                icon = Icons.Default.Lock,
                container = colors.accent,
                contentColor = Color.White,
                borderColor = null,
                onClick = session::lockIn,
            )
        }
        if (deck != null) {
            Text(
                deck.courseCode,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (!compact) {
                Text(
                    deck.courseTitle.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
            }
        }
        Box(Modifier.weight(1f))
        StreakChip(session)
        Box(Modifier.weight(1f))
        if (!compact) ViewToggle()
        IconAction(
            Icons.Default.Menu,
            if (sidebarVisible) "Hide side panel" else "Show side panel",
            onClick = onToggleSidebar,
            tint = if (sidebarVisible) colors.accent else colors.muted,
        )
        SettingsMenu(dark = dark, onToggleDark = onToggleDark)
    }
}

@Composable
private fun StreakChip(session: FocusSession) {
    val colors = Lecture.colors
    val locked = session.phase == FocusPhase.Locked
    val pulse = rememberInfiniteTransition(label = "streak")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (locked) 1.35f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "flame",
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (session.isActive) colors.streakSoft else colors.panelRaised)
            .border(1.dp, if (session.isActive) colors.streak.copy(alpha = 0.35f) else colors.hairline, RoundedCornerShape(20.dp))
            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Dot(if (session.isActive) colors.streak else colors.muted, Modifier.scale(scale), size = 8.dp)
        Text(
            "Focus streak ${formatMinutes(session.streakMillis)}",
            style = MaterialTheme.typography.labelMedium,
            color = if (session.isActive) colors.streak else colors.muted,
        )
        VerticalDivider()
        Text(
            formatClock(session.sessionMillis),
            style = MaterialTheme.typography.labelMedium,
            color = if (session.isActive) MaterialTheme.colorScheme.onSurface else colors.muted,
        )
        if (!session.isActive) {
            IconAction(Icons.Default.PlayArrow, "Start focus session", onClick = session::lockIn, tint = colors.accent, size = 24.dp)
        } else {
            Box(Modifier.size(6.dp))
        }
    }
}

@Composable
private fun ViewToggle() {
    val colors = Lecture.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.panelRaised)
            .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
            .padding(2.dp),
    ) {
        listOf("Slides" to true, "Split" to false, "Mindmap" to false).forEach { (label, available) ->
            val active = label == "Slides"
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    active -> Color.White
                    else -> colors.muted
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) colors.accent else Color.Transparent)
                    .alpha(if (available) 1f else 0.5f)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsMenu(dark: Boolean, onToggleDark: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconAction(Icons.Default.Settings, "Settings", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (dark) "Switch to light theme" else "Switch to dark theme", style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    open = false
                    onToggleDark()
                },
            )
            DropdownMenuItem(
                text = { Text("Split & Mindmap views arrive later", style = MaterialTheme.typography.bodySmall, color = Lecture.colors.muted) },
                onClick = { open = false },
                enabled = false,
            )
        }
    }
}

@Composable
fun BottomBar(
    session: FocusSession,
    openTodos: Int,
    todosOpen: Boolean,
    onToggleTodos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.panel)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(
            "TODOS",
            icon = Icons.AutoMirrored.Filled.List,
            container = if (todosOpen) colors.accentSoft else colors.panelRaised,
            contentColor = if (todosOpen) colors.accent else MaterialTheme.colorScheme.onSurface,
            onClick = onToggleTodos,
        ) { Badge(openTodos) }
        Pill("msgs", icon = Icons.Default.Email, enabled = false) { MonoLabel("soon") }
        Pill("games", icon = Icons.Default.Star, enabled = false) { MonoLabel("break") }
        Box(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dot(if (session.isActive) colors.streak else colors.muted)
            Text(
                if (session.isActive) "Focus: ${session.strength.label}" else "Focus: off · ${session.strength.label} available",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
        }
        VerticalDivider()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.ok, modifier = Modifier.size(14.dp))
            Text("Saved locally", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        }
    }
}
