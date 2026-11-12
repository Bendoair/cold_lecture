package dev.coldlecture.ui.slides

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.coldlecture.data.LectureDeck
import dev.coldlecture.state.LectureController
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.VerticalDivider
import dev.coldlecture.ui.theme.Lecture
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.min

const val MIN_ZOOM = 1f
const val MAX_ZOOM = 3f

@Composable
fun SlideHeader(
    controller: LectureController,
    decks: List<LectureDeck>,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val deck = controller.activeDeck
    val colors = Lecture.colors
    Row(
        modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoLabel("Slide deck")
        Box(Modifier.weight(1f, fill = false)) {
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                    filter = ""
                    menuOpen = true
                }.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    deck?.title ?: "No deck",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Switch deck",
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.widthIn(min = 320.dp, max = 420.dp),
            ) {
                DeckPickerContent(
                    decks = decks,
                    activeId = deck?.id,
                    filter = filter,
                    onFilterChange = { filter = it },
                    importing = controller.importing,
                    onSelect = {
                        menuOpen = false
                        controller.openDeck(it)
                    },
                    onRemove = {
                        controller.removeDeck(it, decks)
                        if (decks.size <= 1) menuOpen = false
                    },
                    onImport = {
                        menuOpen = false
                        controller.importDeck()
                    },
                )
            }
        }
        Box(Modifier.weight(1f))
        IconAction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous slide", onClick = controller::previous, enabled = controller.page > 0)
        Text(
            if (controller.pageCount > 0) "${controller.page + 1} / ${controller.pageCount}" else "– / –",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconAction(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "Next slide",
            onClick = controller::next,
            enabled = controller.page < controller.pageCount - 1,
        )
        VerticalDivider()
        ZoomButton("−", "Zoom out", enabled = zoom > MIN_ZOOM) { onZoomChange((zoom - 0.25f).coerceAtLeast(MIN_ZOOM)) }
        Text(
            "${(zoom * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.widthIn(min = 34.dp),
        )
        ZoomButton("+", "Zoom in", enabled = zoom < MAX_ZOOM) { onZoomChange((zoom + 0.25f).coerceAtMost(MAX_ZOOM)) }
        IconAction(Icons.Default.Refresh, "Fit to screen", onClick = { onZoomChange(MIN_ZOOM) }, enabled = zoom != MIN_ZOOM)
        IconAction(Icons.Default.Create, "Annotations (coming later)", onClick = {}, enabled = false)
    }
}

@Composable
private fun DeckPickerContent(
    decks: List<LectureDeck>,
    activeId: String?,
    filter: String,
    onFilterChange: (String) -> Unit,
    importing: Boolean,
    onSelect: (LectureDeck) -> Unit,
    onRemove: (LectureDeck) -> Unit,
    onImport: () -> Unit,
) {
    val colors = Lecture.colors
    val q = filter.trim()
    val filtered = remember(decks, q) {
        if (q.isEmpty()) decks
        else decks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.courseCode.contains(q, ignoreCase = true) ||
                it.courseTitle.contains(q, ignoreCase = true)
        }
    }
    val groups = remember(filtered) {
        filtered.groupBy { it.courseCode.ifBlank { "Uncategorized" } }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    Column(Modifier.width(360.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.panelRaised)
                .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(15.dp))
            val style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = filter,
                onValueChange = onFilterChange,
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (filter.isEmpty()) {
                        Text("Search decks…", style = style.copy(color = colors.muted))
                    }
                    inner()
                },
            )
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(top = 6.dp)) {
            if (groups.isEmpty()) {
                Text(
                    if (decks.isEmpty()) "No decks yet." else "No decks match \"$q\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            } else {
                groups.forEach { (course, items) ->
                    val courseTitle = items.firstOrNull()?.courseTitle.orEmpty()
                    MonoLabel(
                        if (courseTitle.isBlank()) course else "$course · $courseTitle",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                    items.forEach { item ->
                        DeckPickerRow(
                            item = item,
                            selected = item.id == activeId,
                            onSelect = { onSelect(item) },
                            onRemove = { onRemove(item) },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = colors.hairline, modifier = Modifier.padding(vertical = 6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !importing, onClick = onImport)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Text(
                if (importing) "Importing…" else "Import PDF…",
                style = MaterialTheme.typography.bodyMedium,
                color = if (importing) colors.muted else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DeckPickerRow(
    item: LectureDeck,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = Lecture.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.pageCount} slides",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
        IconAction(Icons.Default.Delete, "Remove ${item.title}", onClick = onRemove, size = 28.dp)
    }
}

@Composable
private fun ZoomButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Lecture.colors.muted else Lecture.colors.hairline,
        )
    }
}

@Composable
fun SlideViewer(
    controller: LectureController,
    zoom: Float,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val document = controller.document
    val page = controller.page
    BoxWithConstraints(
        modifier
            .background(Lecture.colors.slideStage)
            .focusRequester(focusRequester)
            .focusable()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focusRequester.requestFocus()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight, Key.DirectionDown, Key.PageDown, Key.Spacebar -> {
                        controller.next(); true
                    }
                    Key.DirectionLeft, Key.DirectionUp, Key.PageUp -> {
                        controller.previous(); true
                    }
                    Key.MoveHome -> {
                        controller.goTo(0); true
                    }
                    Key.MoveEnd -> {
                        controller.goTo(controller.pageCount - 1); true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val padding = 28.dp
        val availableWidth = with(density) { (maxWidth - padding * 2).toPx() }.coerceAtLeast(1f)
        val availableHeight = with(density) { (maxHeight - padding * 2).toPx() }.coerceAtLeast(1f)
        var aspect by remember(document) { mutableFloatStateOf(16f / 9f) }
        val fitWidth = min(availableWidth, availableHeight * aspect)
        val displayWidth = fitWidth * zoom
        val renderWidth = (ceil(displayWidth / RENDER_BUCKET_PX) * RENDER_BUCKET_PX).toInt().coerceAtLeast(RENDER_BUCKET_PX.toInt())
        var shown by remember(document) { mutableStateOf<ShownPage?>(null) }

        LaunchedEffect(document, page, renderWidth) {
            if (document == null) return@LaunchedEffect
            if (shown?.page == page) delay(120)
            val bitmap = controller.render(page, renderWidth) ?: return@LaunchedEffect
            aspect = bitmap.width.toFloat() / bitmap.height
            shown = ShownPage(page, bitmap)
            if (page + 1 < controller.pageCount) controller.render(page + 1, renderWidth)
        }

        when {
            controller.error != null -> StageMessage(controller.error!!, isError = true)
            document == null && controller.loading -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            document == null -> StageMessage("Pick a deck or import a PDF from the deck menu above.")
            else -> {
                val current = shown
                val widthDp = with(density) { displayWidth.toDp() }
                val heightDp = with(density) { (displayWidth / aspect).toDp() }
                val scrollV = rememberScrollState()
                val scrollH = rememberScrollState()
                Box(
                    Modifier.fillMaxSize().verticalScroll(scrollV, enabled = zoom > MIN_ZOOM).horizontalScroll(scrollH, enabled = zoom > MIN_ZOOM),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .padding(padding)
                            .size(widthDp, heightDp)
                            .shadow(10.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(androidx.compose.ui.graphics.Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (current == null) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Crossfade(targetState = current, animationSpec = tween(160), label = "slide") { shownPage ->
                                Image(
                                    bitmap = shownPage.bitmap,
                                    contentDescription = "Slide ${shownPage.page + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ShownPage(val page: Int, val bitmap: ImageBitmap)

@Composable
private fun StageMessage(text: String, isError: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else Lecture.colors.muted,
        modifier = Modifier.padding(32.dp).widthIn(max = 420.dp),
    )
}

private const val RENDER_BUCKET_PX = 160f
