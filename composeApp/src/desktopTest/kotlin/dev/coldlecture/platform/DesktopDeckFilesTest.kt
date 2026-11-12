package dev.coldlecture.platform

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopDeckFilesTest {
    private val sampleBytes: ByteArray =
        File("src/commonMain/composeResources/files/sample_lecture.pdf").readBytes()

    @Test
    fun rendersAndExtractsTextFromBundledDeck() = runTest {
        val files = DesktopDeckFiles(Files.createTempDirectory("decks").toFile()) { null }
        val document = files.openBytes(sampleBytes)
        try {
            assertEquals(9, document.pageCount)
            val bitmap = document.renderPage(6, targetWidthPx = 640)
            assertTrue(bitmap.width in 600..700, "width was ${bitmap.width}")
            assertEquals(16f / 9f, bitmap.width.toFloat() / bitmap.height, 0.02f)
            assertTrue("Stokes" in document.pageText(6))
        } finally {
            document.close()
        }
    }
}
