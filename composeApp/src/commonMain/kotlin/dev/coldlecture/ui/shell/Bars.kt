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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.coldlecture.data.LectureDeck
import dev.coldlecture.data.ThemePreference
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
import kotlin.math.roundToInt

@Composable
fun TopBar(
    session: FocusSession,
    deck: LectureDeck?,
    compact: Boolean,
    sidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    animals: Boolean,
    theme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    interactivity: Int,
    onInteractivityChange: (Int) -> Unit,
    onAnimalsChange: (Boolean) -> Unit,
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
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (animals) HeaderCompanion()
        }
        StreakChip(session)
        Box(Modifier.weight(1f))
        if (!compact) ViewToggle()
        IconAction(
            Icons.Default.Menu,
            if (sidebarVisible) "Hide side panel" else "Show side panel",
            onClick = onToggleSidebar,
            tint = if (sidebarVisible) colors.accent else colors.muted,
        )
        SettingsMenu(
            theme = theme,
            onThemeChange = onThemeChange,
            interactivity = interactivity,
            onInteractivityChange = onInteractivityChange,
            animals = animals,
            onAnimalsChange = onAnimalsChange,
        )
    }
}

@Composable
private fun HeaderCompanion() {
    val bounce = rememberInfiniteTransition(label = "companion")
    val bob by bounce.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "bob",
    )
    Text(
        "🐱",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.offset(y = bob.dp),
    )
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
private fun SettingsMenu(
    theme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    interactivity: Int,
    onInteractivityChange: (Int) -> Unit,
    animals: Boolean,
    onAnimalsChange: (Boolean) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val colors = Lecture.colors
    Box {
        IconAction(Icons.Default.Settings, "Settings", onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.width(300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MonoLabel("Appearance")
                ThemeSwitch(theme = theme, onThemeChange = onThemeChange)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Interactivity", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "$interactivity%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (interactivity >= 50) colors.accent else colors.muted,
                        )
                    }
                    Slider(
                        value = interactivity.toFloat(),
                        onValueChange = { onInteractivityChange(it.roundToInt()) },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.hairline,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                    )
                    Box(Modifier.fillMaxWidth().height(14.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .width(1.dp)
                                .height(10.dp)
                                .background(colors.hairline),
                        )
                        Text(
                            "50% checkpoint",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Animals", style = MaterialTheme.typography.bodyMedium)
                        Text("Colab-style companions", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                    }
                    Switch(
                        checked = animals,
                        onCheckedChange = onAnimalsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = colors.hairline,
                            uncheckedBorderColor = colors.hairline,
                        ),
                    )
                }

                Text(
                    "Split & Mindmap views arrive later",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun ThemeSwitch(theme: ThemePreference, onThemeChange: (ThemePreference) -> Unit) {
    val colors = Lecture.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.panelRaised)
            .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ThemeOption(
            selected = theme == ThemePreference.System,
            label = "System",
            icon = ThemeIcons.System,
            onClick = { onThemeChange(ThemePreference.System) },
            modifier = Modifier.weight(1f),
        )
        ThemeOption(
            selected = theme == ThemePreference.Light,
            label = "Light",
            icon = ThemeIcons.Light,
            onClick = { onThemeChange(ThemePreference.Light) },
            modifier = Modifier.weight(1f),
        )
        ThemeOption(
            selected = theme == ThemePreference.Dark,
            label = "Dark",
            icon = ThemeIcons.Dark,
            onClick = { onThemeChange(ThemePreference.Dark) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeOption(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Color.White else colors.muted,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else colors.muted,
        )
    }
}

private object ThemeIcons {
    val System: ImageVector by lazy {
        ImageVector.Builder(
            name = "ThemeSystem",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColorBlack) {
                moveTo(4f, 5f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(11f)
                horizontalLineToRelative(-16f)
                close()
                moveTo(6f, 7f)
                verticalLineToRelative(7f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(-7f)
                close()
                moveTo(9f, 18f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-6f)
                close()
            }
        }.build()
    }

    val Light: ImageVector by lazy {
        ImageVector.Builder(
            name = "ThemeLight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColorBlack) {
                moveTo(12f, 8f)
                arcToRelative(4f, 4f, 0f, true, true, 0f, 8f)
                arcToRelative(4f, 4f, 0f, true, true, 0f, -8f)
                close()
                moveTo(11.25f, 2.5f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(2.5f)
                horizontalLineToRelative(-1.5f)
                close()
                moveTo(11.25f, 19f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(2.5f)
                horizontalLineToRelative(-1.5f)
                close()
                moveTo(2.5f, 11.25f)
                horizontalLineToRelative(2.5f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-2.5f)
                close()
                moveTo(19f, 11.25f)
                horizontalLineToRelative(2.5f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-2.5f)
                close()
                moveTo(5.05f, 4.4f)
                lineTo(6.8f, 6.15f)
                lineTo(5.74f, 7.21f)
                lineTo(3.99f, 5.46f)
                close()
                moveTo(17.2f, 16.85f)
                lineTo(18.95f, 18.6f)
                lineTo(17.89f, 19.66f)
                lineTo(16.14f, 17.91f)
                close()
                moveTo(18.95f, 5.46f)
                lineTo(17.2f, 7.21f)
                lineTo(16.14f, 6.15f)
                lineTo(17.89f, 4.4f)
                close()
                moveTo(6.8f, 17.91f)
                lineTo(5.05f, 19.66f)
                lineTo(3.99f, 18.6f)
                lineTo(5.74f, 16.85f)
                close()
            }
        }.build()
    }

    val Dark: ImageVector by lazy {
        ImageVector.Builder(
            name = "ThemeDark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColorBlack) {
                moveTo(12.5f, 3.1f)
                arcToRelative(8.5f, 8.5f, 0f, true, false, 8.4f, 10.7f)
                arcToRelative(7f, 7f, 0f, true, true, -8.4f, -10.7f)
                close()
            }
        }.build()
    }
}

private val SolidColorBlack = SolidColor(Color.Black)

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
