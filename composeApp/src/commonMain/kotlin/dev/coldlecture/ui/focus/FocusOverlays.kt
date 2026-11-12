package dev.coldlecture.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coldlecture.state.AwayReason
import dev.coldlecture.state.FocusPhase
import dev.coldlecture.state.FocusSession
import dev.coldlecture.state.formatClock
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.PanelShape
import dev.coldlecture.ui.components.Pill
import dev.coldlecture.ui.theme.Lecture

@Composable
fun FocusOverlays(session: FocusSession) {
    val phase = session.phase
    val visible = phase == FocusPhase.Away || phase == FocusPhase.ExitChallenge
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xCC070B14))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.animateEnterExit(
                    enter = scaleIn(tween(220), initialScale = 0.94f),
                    exit = scaleOut(tween(150), targetScale = 0.96f),
                ),
            ) {
                when (phase) {
                    FocusPhase.ExitChallenge -> ExitChallengeCard(session)
                    FocusPhase.Away -> AwayCard(session)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun OverlayCard(content: @Composable () -> Unit) {
    val colors = Lecture.colors
    Column(
        Modifier
            .width(440.dp)
            .clip(PanelShape)
            .background(colors.panel)
            .border(1.dp, colors.hairline, PanelShape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun AwayCard(session: FocusSession) {
    val colors = Lecture.colors
    OverlayCard {
        MonoLabel("Focus session · ${session.strength.label}", color = colors.streak)
        Text(
            if (session.awayReason == AwayReason.FullscreenExited) "Fullscreen was closed" else "You stepped away",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            formatClock(session.awayMillis),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = Lecture.mono, fontSize = 34.sp),
            color = colors.streak,
        )
        Text(
            "Your streak is paused while you're away. It starts over when you return — " +
                "the session timer keeps running.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
        Stats(session)
        Pill(
            "Back to the lecture",
            icon = Icons.Default.Lock,
            container = colors.accent,
            contentColor = Color.White,
            borderColor = null,
            onClick = session::resume,
        )
        Text(
            "End session…",
            style = MaterialTheme.typography.labelLarge,
            color = colors.muted,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = session::requestExit).padding(6.dp),
        )
    }
}

@Composable
private fun ExitChallengeCard(session: FocusSession) {
    val colors = Lecture.colors
    var input by remember(session.exitCode) { mutableStateOf("") }
    var mismatch by remember(session.exitCode) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(session.exitCode) { focusRequester.requestFocus() }

    fun submit() {
        mismatch = !session.submitExitCode(input)
    }

    OverlayCard {
        MonoLabel("Leaving focus mode", color = colors.streak)
        Text("Sure you want to unlock?", style = MaterialTheme.typography.titleLarge)
        Stats(session)
        Text("Type this code to unlock", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        Text(
            session.exitCode.toList().joinToString(" "),
            style = TextStyle(fontFamily = Lecture.mono, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        val fieldStyle = TextStyle(
            fontFamily = Lecture.mono,
            fontSize = 22.sp,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        BasicTextField(
            value = input,
            onValueChange = {
                input = it.uppercase().filter(Char::isLetterOrDigit).take(session.exitCode.length)
                mismatch = false
            },
            singleLine = true,
            textStyle = fieldStyle,
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.panelRaised)
                .border(1.dp, if (mismatch) MaterialTheme.colorScheme.error else colors.hairline, RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            submit(); true
                        }
                        Key.Escape -> {
                            session.cancelExit(); true
                        }
                        else -> false
                    }
                },
        )
        if (mismatch) {
            Text("That code doesn't match.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(
                "Stay focused",
                icon = Icons.Default.Lock,
                container = colors.accent,
                contentColor = Color.White,
                borderColor = null,
                onClick = session::cancelExit,
            )
            Pill("Unlock", enabled = input.length == session.exitCode.length, onClick = ::submit)
        }
    }
}

@Composable
private fun Stats(session: FocusSession) {
    val colors = Lecture.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.panelRaised)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stat("Session", formatClock(session.sessionMillis))
        Stat("Best streak", formatClock(maxOf(session.bestStreakMillis, session.streakMillis)))
        Stat("Times left", session.leaveCount.toString())
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Lecture.mono))
        MonoLabel(label)
    }
}
