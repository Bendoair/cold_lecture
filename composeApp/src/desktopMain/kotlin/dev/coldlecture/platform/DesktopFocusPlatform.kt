package dev.coldlecture.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.awt.Frame

/**
 * Fullscreen + always-on-top window while locked; Alt-Tab is still possible, so leaving is
 * detected, not prevented.
 *
 * Compose Desktop can leave Skiko/native fullscreen active while [WindowState.placement] already
 * reads as [WindowPlacement.Floating], so state-only restores become no-ops. Release therefore
 * also writes placement on the live [ComposeWindow].
 */
class DesktopFocusPlatform(private val windowState: WindowState) : FocusPlatform {
    override val strength = LockStrength.Desktop

    var locked by mutableStateOf(false)
        private set

    /** Set from the Window composition so release can poke the real AWT/Skiko window. */
    var window: ComposeWindow? = null

    private var presenceListener: (PresenceEvent) -> Unit = {}
    private var exitAttemptListener: () -> Unit = {}
    private var placementBeforeLock = WindowPlacement.Floating

    override fun engage() {
        if (locked) return
        val current = window?.placement ?: windowState.placement
        placementBeforeLock = when (current) {
            WindowPlacement.Fullscreen -> WindowPlacement.Floating
            else -> current
        }
        applyPlacement(WindowPlacement.Fullscreen)
        locked = true
    }

    override fun release() {
        if (!locked) return
        locked = false
        // Always leave fullscreen first so decorations return even if WindowState is already Floating.
        applyPlacement(WindowPlacement.Floating)
        val restore = placementBeforeLock
        if (restore == WindowPlacement.Maximized) {
            applyPlacement(WindowPlacement.Maximized)
        }
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

    /** Native/OS left fullscreen while we still think we are locked. */
    fun onPlacementChanged(placement: WindowPlacement) {
        if (locked && placement != WindowPlacement.Fullscreen) {
            presenceListener(PresenceEvent.FullscreenExited)
        }
    }

    /** Returns true when the close request was swallowed because a focus session is active. */
    fun interceptClose(): Boolean {
        if (!locked) return false
        exitAttemptListener()
        return true
    }

    private fun applyPlacement(placement: WindowPlacement) {
        windowState.placement = placement
        val frame = window
        if (frame != null) {
            // Direct apply bypasses Compose's appliedState equality check.
            frame.placement = placement
            if (placement == WindowPlacement.Floating) {
                // Some WMs keep MAXIMIZED_BOTH after exiting exclusive fullscreen.
                frame.extendedState = frame.extendedState and Frame.MAXIMIZED_BOTH.inv()
            }
        }
    }
}
