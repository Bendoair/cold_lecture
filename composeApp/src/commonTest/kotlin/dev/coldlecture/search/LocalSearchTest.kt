package dev.coldlecture.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalSearchTest {
    @Test
    fun snippetCentersOnMatchAndCollapsesWhitespace() {
        val text = "intro ".repeat(30) + "the  Stokes\ntheorem relates" + " outro".repeat(30)
        val result = snippet(text, "stokes", radius = 10)
        assertTrue(result.startsWith("…"))
        assertTrue(result.endsWith("…"))
        assertTrue("Stokes theorem" in result)
    }

    @Test
    fun snippetWithoutMatchFallsBackToStart() {
        assertEquals("short text", snippet("short   text", "zzz"))
    }

    @Test
    fun searchSlidesIsCaseInsensitiveAndOrderedByPage() {
        val texts = mapOf(3 to "Green's theorem", 0 to "Agenda: Green", 1 to "Flux")
        val hits = searchSlides(texts, "green")
        assertEquals(listOf(0, 3), hits.map { it.pageIndex })
    }

    @Test
    fun scopeCyclesThroughAllValues() {
        var scope = SearchScope.All
        val seen = mutableListOf<SearchScope>()
        repeat(SearchScope.entries.size) {
            seen += scope
            scope = scope.next()
        }
        assertEquals(SearchScope.entries.toList(), seen)
        assertEquals(SearchScope.All, scope)
    }
}
