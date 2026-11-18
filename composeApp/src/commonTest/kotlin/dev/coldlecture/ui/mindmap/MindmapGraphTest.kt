package dev.coldlecture.ui.mindmap

import dev.coldlecture.data.ConceptLink
import dev.coldlecture.data.DictEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindmapGraphTest {
    private fun entry(id: Long, term: String, tag: String = "") =
        DictEntry(id = id, term = term, definition = "", tag = tag, createdAt = 0)

    @Test
    fun distinctTagsIgnoresBlankAndSortsCaseInsensitive() {
        val tags = distinctTags(
            listOf(
                entry(1, "a", "Zeta"),
                entry(2, "b", " alpha "),
                entry(3, "c", ""),
                entry(4, "d", "Alpha"),
                entry(5, "e", "   "),
                entry(6, "f", "alpha"),
            ),
        )
        // Case-insensitive distinct keeps first casing ("alpha" from " alpha ").
        assertEquals(listOf("alpha", "Zeta"), tags)
    }

    @Test
    fun emptySelectionShowsAllAsPrimary() {
        val entries = listOf(entry(1, "A", "Calc"), entry(2, "B"), entry(3, "C", "LinAlg"))
        val links = listOf(ConceptLink(1, 2))
        val graph = visibleGraph(entries, links, emptySet())
        assertEquals(entries, graph.entries)
        assertEquals(links, graph.links)
        assertEquals(
            mapOf(1L to MindmapNodeRole.Primary, 2L to MindmapNodeRole.Primary, 3L to MindmapNodeRole.Primary),
            graph.roles,
        )
    }

    @Test
    fun multiTagUnionKeepsOneHopNeighborsDimmed() {
        // 1,2 Calculus; 3 LinAlg linked to 1; 4 unlinked other tag
        val entries = listOf(
            entry(1, "Limit", "Calculus"),
            entry(2, "Derivative", "Calculus"),
            entry(3, "Matrix", "LinAlg"),
            entry(4, "Group", "Algebra"),
        )
        val links = listOf(
            ConceptLink(1, 2),
            ConceptLink(1, 3),
        )
        val graph = visibleGraph(entries, links, setOf("Calculus"))
        assertEquals(setOf(1L, 2L, 3L), graph.entries.map { it.id }.toSet())
        assertEquals(MindmapNodeRole.Primary, graph.roles[1])
        assertEquals(MindmapNodeRole.Primary, graph.roles[2])
        assertEquals(MindmapNodeRole.Neighbor, graph.roles[3])
        assertTrue(4L !in graph.roles)
        assertEquals(2, graph.links.size)
    }

    @Test
    fun multiSelectUnionsTags() {
        val entries = listOf(
            entry(1, "A", "T1"),
            entry(2, "B", "T2"),
            entry(3, "C", "T3"),
        )
        val graph = visibleGraph(entries, emptyList(), setOf("T1", "T2"))
        assertEquals(setOf(1L, 2L), graph.entries.map { it.id }.toSet())
        assertEquals(MindmapNodeRole.Primary, graph.roles[1])
        assertEquals(MindmapNodeRole.Primary, graph.roles[2])
    }

    @Test
    fun isolatesRemainWhenShowingAll() {
        val entries = listOf(entry(1, "Lonely"), entry(2, "AlsoAlone"))
        val graph = visibleGraph(entries, emptyList(), emptySet())
        assertEquals(2, graph.entries.size)
        assertTrue(graph.links.isEmpty())
        assertTrue(graph.roles.values.all { it == MindmapNodeRole.Primary })
    }

    @Test
    fun filteredIsolateWithoutMatchingTagIsHidden() {
        val entries = listOf(entry(1, "Match", "X"), entry(2, "Isolate", "Y"))
        val graph = visibleGraph(entries, emptyList(), setOf("X"))
        assertEquals(listOf(1L), graph.entries.map { it.id })
    }
}
