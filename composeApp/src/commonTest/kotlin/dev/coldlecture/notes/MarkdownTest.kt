package dev.coldlecture.notes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarkdownTest {
    @Test
    fun parsesBlockKinds() {
        val blocks = parseMarkdown("# Title\n- item\n2. second\n\nplain")
        assertEquals(MdBlock.Heading(1, listOf(MdSpan("Title"))), blocks[0])
        assertEquals(MdBlock.Bullet(listOf(MdSpan("item"))), blocks[1])
        assertEquals(MdBlock.Numbered("2", listOf(MdSpan("second"))), blocks[2])
        assertIs<MdBlock.Blank>(blocks[3])
        assertEquals(MdBlock.Paragraph(listOf(MdSpan("plain"))), blocks[4])
    }

    @Test
    fun parsesInlineStyles() {
        assertEquals(
            listOf(
                MdSpan("a "),
                MdSpan("bold", bold = true),
                MdSpan(" and "),
                MdSpan("it", italic = true),
                MdSpan(" "),
                MdSpan("x+1", code = true),
            ),
            parseInline("a **bold** and *it* `x+1`"),
        )
    }

    @Test
    fun unmatchedMarkersStayLiteral() {
        assertEquals(listOf(MdSpan("2 * 3 = 6")), parseInline("2 * 3 = 6"))
        assertEquals(listOf(MdSpan("a ** b")), parseInline("a ** b"))
        assertEquals(listOf(MdSpan("tick ` alone")), parseInline("tick ` alone"))
    }
}
