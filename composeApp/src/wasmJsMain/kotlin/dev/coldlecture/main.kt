package dev.coldlecture

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import dev.coldlecture.data.AppDatabase
import dev.coldlecture.data.DATABASE_FILE_NAME
import dev.coldlecture.platform.AppPlatform
import dev.coldlecture.ui.App
import dev.coldlecture.web.WebDeckFiles
import dev.coldlecture.web.WebFocusPlatform
import kotlinx.browser.document
import org.w3c.dom.Worker

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val database = Room.databaseBuilder<AppDatabase>(name = DATABASE_FILE_NAME)
        .setDriver(WebWorkerSQLiteDriver(sqliteWorker()))
        .build()
    val platform = AppPlatform(
        database = database,
        deckFiles = WebDeckFiles(),
        focus = WebFocusPlatform(),
    )
    ComposeViewport {
        App(platform)
    }
    document.getElementById("boot")?.remove()
}

private fun sqliteWorker(): Worker =
    js("""new Worker(new URL("cold-lecture-web/sqlite-worker.mjs", import.meta.url), { type: "module" })""")
