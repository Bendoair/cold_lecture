package dev.coldlecture.ui.mindmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dk.kuiver.KuiverViewerState
import com.dk.kuiver.SelectionMode
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edge
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverInteractionCallbacks
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.renderer.KuiverViewerConfig
import com.dk.kuiver.ui.EdgeShape
import com.dk.kuiver.ui.EdgeStyle
import dev.coldlecture.data.ConceptLink
import dev.coldlecture.data.DictEntry
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.components.MonoLabel
import dev.coldlecture.ui.components.Pill
import dev.coldlecture.ui.components.VerticalDivider
import dev.coldlecture.ui.theme.Lecture

private const val NeighborAlpha = 0.4f
private const val MinZoom = 0.1f
private const val MaxZoom = 5f

@Composable
fun MindmapView(
    entries: List<DictEntry>,
    links: List<ConceptLink>,
    onSelectTerm: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    val tags = remember(entries) { distinctTags(entries) }
    val visible = remember(entries, links, selectedTags) {
        visibleGraph(entries, links, selectedTags)
    }
    val byId = remember(visible.entries) { visible.entries.associateBy { it.id } }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.panel),
    ) {
        TagFilterBar(
            tags = tags,
            selected = selectedTags,
            onToggle = { tag ->
                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
            },
            onClear = { selectedTags = emptySet() },
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            entries.isEmpty() -> EmptyMindmap(
                "Add terms in the dictionary to build the map.",
                Modifier.weight(1f).fillMaxWidth(),
            )
            visible.entries.isEmpty() -> EmptyMindmap(
                "No terms match the selected tags.",
                Modifier.weight(1f).fillMaxWidth(),
            )
            else -> {
                val graph = remember(visible) {
                    buildKuiver {
                        nodes(visible.entries.map { it.id.toString() })
                        visible.links.forEach { link ->
                            edge(link.fromId.toString(), link.toId.toString())
                        }
                    }
                }
                val layoutConfig = remember {
                    LayoutConfig.ForceDirected(
                        iterations = 220,
                        repulsionStrength = 520f,
                        attractionStrength = 0.025f,
                        damping = 0.86f,
                    )
                }
                val viewerState = rememberKuiverViewerState(
                    initialKuiver = graph,
                    layoutConfig = layoutConfig,
                )
                LaunchedEffect(graph) {
                    if (viewerState.kuiver != graph) {
                        viewerState.updateKuiver(graph)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    KuiverViewer(
                        state = viewerState,
                        modifier = Modifier.fillMaxSize(),
                        config = KuiverViewerConfig(
                            selectionMode = SelectionMode.SINGLE,
                            hoverEnabled = true,
                            fitToContent = true,
                            contentPadding = 0.85f,
                            nodeDragEnabled = true,
                            minScale = MinZoom,
                            maxScale = MaxZoom,
                        ),
                        callbacks = KuiverInteractionCallbacks(
                            onNodeClick = { node ->
                                node.id.toLongOrNull()?.let(onSelectTerm)
                            },
                        ),
                        nodeContent = { node ->
                            val id = node.id.toLongOrNull()
                            val entry = id?.let { byId[it] }
                            val role = id?.let { visible.roles[it] } ?: MindmapNodeRole.Primary
                            val selected = isSelected
                            val hovered = isHovered
                            MindmapNodeChip(
                                term = entry?.term ?: node.id,
                                tag = entry?.tag.orEmpty(),
                                role = role,
                                selected = selected,
                                hovered = hovered,
                            )
                        },
                        edgeStyle = { edge ->
                            val fromId = edge.fromId.toLongOrNull()
                            val toId = edge.toId.toLongOrNull()
                            val fromNeighbor = fromId != null && visible.roles[fromId] == MindmapNodeRole.Neighbor
                            val toNeighbor = toId != null && visible.roles[toId] == MindmapNodeRole.Neighbor
                            val dimmed = fromNeighbor || toNeighbor
                            EdgeStyle(
                                color = colors.hairline.copy(alpha = if (dimmed) NeighborAlpha else 0.9f),
                                strokeWidth = 1.5.dp,
                                showArrow = false,
                                shape = EdgeShape.STRAIGHT,
                            )
                        },
                    )
                    MindmapZoomControls(
                        state = viewerState,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MindmapZoomControls(
    state: KuiverViewerState,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    val scale = state.scale
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.panelRaised)
            .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ZoomButton("−", "Zoom out", enabled = scale > MinZoom + 0.001f, onClick = state::zoomOut)
        Text(
            "${(scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 34.dp),
        )
        ZoomButton("+", "Zoom in", enabled = scale < MaxZoom - 0.001f, onClick = state::zoomIn)
        VerticalDivider()
        IconAction(Icons.Default.Refresh, "Fit to screen", onClick = { state.centerGraph() }, size = 26.dp)
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
private fun TagFilterBar(
    tags: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Lecture.colors
    val allActive = selected.isEmpty()
    Column(
        modifier
            .background(colors.panelRaised)
            .border(1.dp, colors.hairline)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoLabel("Filter by tag")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = "All",
                container = if (allActive) colors.accent else colors.panel,
                contentColor = if (allActive) Color.White else MaterialTheme.colorScheme.onSurface,
                borderColor = if (allActive) null else colors.hairline,
                onClick = onClear,
            )
            tags.forEach { tag ->
                val on = tag in selected
                Pill(
                    text = tag,
                    container = if (on) colors.accentSoft else colors.panel,
                    contentColor = if (on) colors.accent else MaterialTheme.colorScheme.onSurface,
                    borderColor = if (on) colors.accent.copy(alpha = 0.35f) else colors.hairline,
                    onClick = { onToggle(tag) },
                )
            }
        }
    }
}

@Composable
private fun MindmapNodeChip(
    term: String,
    tag: String,
    role: MindmapNodeRole,
    selected: Boolean,
    hovered: Boolean,
) {
    val colors = Lecture.colors
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        selected -> colors.accent
        hovered -> colors.accent.copy(alpha = 0.55f)
        else -> colors.hairline
    }
    Column(
        Modifier
            .alpha(if (role == MindmapNodeRole.Neighbor) NeighborAlpha else 1f)
            .clip(shape)
            .background(colors.panelRaised)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            term,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (tag.isNotBlank()) {
            Text(
                tag,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyMindmap(message: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Lecture.colors.muted,
        )
    }
}
