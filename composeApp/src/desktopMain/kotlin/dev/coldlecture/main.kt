package dev.coldlecture

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.coldlecture.data.AppDatabase
import dev.coldlecture.data.DATABASE_FILE_NAME
import dev.coldlecture.platform.AppPlatform
import dev.coldlecture.platform.DesktopDeckFiles
import dev.coldlecture.platform.DesktopFocusPlatform
import dev.coldlecture.resources.Res
import dev.coldlecture.resources.app_icon
import dev.coldlecture.ui.App
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.painterResource
import java.awt.Frame
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowStateListener
import java.io.File

fun main() {
    val dataDir = appDataDir().apply { mkdirs() }
    val database = Room.databaseBuilder<AppDatabase>(name = File(dataDir, DATABASE_FILE_NAME).absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    application {
        val windowState = rememberWindowState(size = DpSize(1440.dp, 900.dp))
        val focus = remember { DesktopFocusPlatform(windowState) }
        val frameRef = remember { arrayOfNulls<Frame>(1) }
        val platform = remember {
            AppPlatform(
                database = database,
                deckFiles = DesktopDeckFiles(File(dataDir, "decks")) { frameRef[0] },
                focus = focus,
            )
        }

        Window(
            onCloseRequest = { if (!focus.interceptClose()) exitApplication() },
            state = windowState,
            title = "Cold Lecture",
            icon = painterResource(Res.drawable.app_icon),
            alwaysOnTop = focus.locked,
        ) {
            frameRef[0] = window
            focus.window = window
            DisposableEffect(window) {
                val focusListener = object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent) = focus.onWindowFocusChanged(true)
                    override fun windowLostFocus(e: WindowEvent) = focus.onWindowFocusChanged(false)
                }
                val stateListener = WindowStateListener {
                    focus.onPlacementChanged(window.placement)
                }
                val componentListener = object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        focus.onPlacementChanged(window.placement)
                    }
                }
                window.addWindowFocusListener(focusListener)
                window.addWindowStateListener(stateListener)
                window.addComponentListener(componentListener)
                onDispose {
                    focus.window = null
                    window.removeWindowFocusListener(focusListener)
                    window.removeWindowStateListener(stateListener)
                    window.removeComponentListener(componentListener)
                }
            }
            App(platform)
        }
    }
}

private fun appDataDir(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> File(System.getenv("APPDATA") ?: home, "ColdLecture")
        os.contains("mac") -> File(home, "Library/Application Support/ColdLecture")
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "cold-lecture")
    }
}
