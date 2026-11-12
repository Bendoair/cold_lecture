package dev.coldlecture.notes

/** Minimal Markdown subset for notes: headings, bullet/numbered lists, bold, italic, inline code. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

sealed interface MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Bullet(val spans: List<MdSpan>) : MdBlock
    data class Numbered(val number: String, val spans: List<MdSpan>) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data object Blank : MdBlock
}

private val headingRegex = Regex("^(#{1,3})\\s+(.*)$")
private val bulletRegex = Regex("^\\s*[-*+]\\s+(.*)$")
private val numberedRegex = Regex("^\\s*(\\d+)[.)]\\s+(.*)$")

fun parseMarkdown(source: String): List<MdBlock> = source.lines().map { line ->
    headingRegex.matchEntire(line)?.let { return@map MdBlock.Heading(it.groupValues[1].length, parseInline(it.groupValues[2])) }
    bulletRegex.matchEntire(line)?.let { return@map MdBlock.Bullet(parseInline(it.groupValues[1])) }
    numberedRegex.matchEntire(line)?.let { return@map MdBlock.Numbered(it.groupValues[1], parseInline(it.groupValues[2])) }
    if (line.isBlank()) MdBlock.Blank else MdBlock.Paragraph(parseInline(line))
}

fun parseInline(text: String): List<MdSpan> {
    val spans = mutableListOf<MdSpan>()
    val buffer = StringBuilder()
    var bold = false
    var italic = false
    var i = 0

    fun flush() {
        if (buffer.isNotEmpty()) {
            spans += MdSpan(buffer.toString(), bold = bold, italic = italic)
            buffer.clear()
        }
    }

    while (i < text.length) {
        val c = text[i]
        when {
            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    flush()
                    spans += MdSpan(text.substring(i + 1, end), bold = bold, italic = italic, code = true)
                    i = end + 1
                } else {
                    buffer.append(c)
                    i++
                }
            }
            text.startsWith("**", i) && (bold || text.indexOf("**", i + 2) > i + 2) -> {
                flush()
                bold = !bold
                i += 2
            }
            c == '*' && (italic || text.indexOf('*', i + 1) > i + 1) -> {
                flush()
                italic = !italic
                i++
            }
            else -> {
                buffer.append(c)
                i++
            }
        }
    }
    flush()
    return spans
}
