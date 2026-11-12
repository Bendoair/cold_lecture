package dev.coldlecture.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/** Fullscreen + always-on-top window while locked; Alt-Tab is still possible, so leaving is detected, not prevented. */
class DesktopFocusPlatform(private val windowState: WindowState) : FocusPlatform {
    override val strength = LockStrength.Desktop

    var locked by mutableStateOf(false)
        private set

    private var presenceListener: (PresenceEvent) -> Unit = {}
    private var exitAttemptListener: () -> Unit = {}
    private var placementBeforeLock = WindowPlacement.Floating

    override fun engage() {
        if (locked) return
        placementBeforeLock = windowState.placement
        windowState.placement = WindowPlacement.Fullscreen
        locked = true
    }

    override fun release() {
        if (!locked) return
        locked = false
        windowState.placement = placementBeforeLock
    }

    override fun setPresenceListener(listener: (PresenceEvent) -> Unit) {
        presenceListener = listener
    }

    override fun setExitAttemptListener(listener: () -> Unit) {
        exitAttemptListener = listener
    }

    fun onWindowFocusChanged(focused: Boolean) {
        if (locked) presenceListener(if (focused) PresenceEvent.Returned else PresenceEvent.Left)
    }

    /** Returns true when the close request was swallowed because a focus session is active. */
    fun interceptClose(): Boolean {
        if (!locked) return false
        exitAttemptListener()
        return true
    }
}
