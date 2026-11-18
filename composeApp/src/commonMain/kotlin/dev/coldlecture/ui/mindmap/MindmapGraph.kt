package dev.coldlecture.ui.mindmap

import dev.coldlecture.data.ConceptLink
import dev.coldlecture.data.DictEntry

enum class MindmapNodeRole {
    Primary,
    Neighbor,
}

data class VisibleMindmap(
    val entries: List<DictEntry>,
    val links: List<ConceptLink>,
    val roles: Map<Long, MindmapNodeRole>,
)

/** Distinct non-blank tags (case-insensitive), sorted case-insensitively. Keeps first casing. */
fun distinctTags(entries: List<DictEntry>): List<String> {
    val seen = linkedMapOf<String, String>()
    for (entry in entries) {
        val trimmed = entry.tag.trim()
        if (trimmed.isEmpty()) continue
        val key = trimmed.lowercase()
        if (key !in seen) seen[key] = trimmed
    }
    return seen.values.sortedWith(String.CASE_INSENSITIVE_ORDER)
}

/**
 * Visible subgraph for the mindmap.
 *
 * Empty [selectedTags] → every entry is primary.
 * Otherwise primary = tag in the selected set; neighbor = linked to ≥1 primary; rest hidden.
 */
fun visibleGraph(
    entries: List<DictEntry>,
    links: List<ConceptLink>,
    selectedTags: Set<String>,
): VisibleMindmap {
    if (entries.isEmpty()) {
        return VisibleMindmap(emptyList(), emptyList(), emptyMap())
    }
    if (selectedTags.isEmpty()) {
        val roles = entries.associate { it.id to MindmapNodeRole.Primary }
        return VisibleMindmap(entries, links, roles)
    }

    val selectedLower = selectedTags.map { it.lowercase() }.toSet()
    val byId = entries.associateBy { it.id }
    val primaryIds = entries
        .filter { it.tag.trim().lowercase() in selectedLower }
        .map { it.id }
        .toSet()

    val neighborIds = mutableSetOf<Long>()
    for (link in links) {
        val fromPrimary = link.fromId in primaryIds
        val toPrimary = link.toId in primaryIds
        when {
            fromPrimary && !toPrimary && link.toId in byId -> neighborIds += link.toId
            toPrimary && !fromPrimary && link.fromId in byId -> neighborIds += link.fromId
        }
    }

    val visibleIds = primaryIds + neighborIds
    val visibleEntries = entries.filter { it.id in visibleIds }
    val visibleLinks = links.filter { it.fromId in visibleIds && it.toId in visibleIds }
    val roles = buildMap {
        primaryIds.forEach { put(it, MindmapNodeRole.Primary) }
        neighborIds.forEach { put(it, MindmapNodeRole.Neighbor) }
    }
    return VisibleMindmap(visibleEntries, visibleLinks, roles)
}
