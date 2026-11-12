package dev.coldlecture.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.coldlecture.ui.theme.Lecture

val PanelShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(8.dp)

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(PanelShape)
            .background(Lecture.colors.panel)
            .border(1.dp, Lecture.colors.hairline, PanelShape),
        content = content,
    )
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = Lecture.colors.panelRaised,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color? = Lecture.colors.hairline,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    var m = modifier
        .clip(PillShape)
        .background(container)
    if (borderColor != null) m = m.border(1.dp, borderColor, PillShape)
    if (onClick != null) m = m.clickable(enabled = enabled, onClick = onClick)
    Row(
        m.alpha(if (enabled) 1f else 0.45f).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, maxLines = 1)
        trailing()
    }
}

@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Lecture.colors.muted,
    size: Dp = 30.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(7.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.58f))
    }
}

@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color = Lecture.colors.muted) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier, maxLines = 1)
}

@Composable
fun Badge(count: Int, modifier: Modifier = Modifier, color: Color = Lecture.colors.accent) {
    if (count <= 0) return
    Box(
        modifier.height(16.dp).clip(CircleShape).background(color).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 7.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    fillHeight: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Lecture.colors.accent, modifier = Modifier.size(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Lecture.colors.muted,
                    modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.weight(1f))
            actions()
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = Lecture.colors.muted,
                modifier = Modifier.padding(start = 2.dp).size(20.dp),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120)),
        ) {
            Column(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Lecture.colors.hairline))
                content()
            }
        }
    }
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).height(18.dp).background(Lecture.colors.hairline))
}
