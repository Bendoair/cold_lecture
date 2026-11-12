package dev.coldlecture.state

import dev.coldlecture.platform.FocusPlatform
import dev.coldlecture.platform.LockStrength
import dev.coldlecture.platform.PresenceEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeFocusPlatform : FocusPlatform {
    override val strength = LockStrength.SoftBrowser
    var engaged = 0
    var released = 0
    lateinit var presence: (PresenceEvent) -> Unit
    lateinit var exitAttempt: () -> Unit

    override fun engage() {
        engaged++
    }

    override fun release() {
        released++
    }

    override fun setPresenceListener(listener: (PresenceEvent) -> Unit) {
        presence = listener
    }

    override fun setExitAttemptListener(listener: () -> Unit) {
        exitAttempt = listener
    }
}

class FocusSessionTest {
    @Test
    fun lockInEngagesPlatform() = runTest {
        val platform = FakeFocusPlatform()
        val session = FocusSession(platform, backgroundScope)
        session.lockIn()
        assertEquals(FocusPhase.Locked, session.phase)
        assertEquals(1, platform.engaged)
    }

    @Test
    fun exitRequiresMatchingCode() = runTest {
        val platform = FakeFocusPlatform()
        val session = FocusSession(platform, backgroundScope)
        session.lockIn()
        session.requestExit()
        assertEquals(FocusPhase.ExitChallenge, session.phase)
        assertEquals(6, session.exitCode.length)

        assertFalse(session.submitExitCode("WRONG1"))
        assertEquals(FocusPhase.ExitChallenge, session.phase)
        assertEquals(0, platform.released)

        assertTrue(session.submitExitCode(session.exitCode.lowercase()))
        assertEquals(FocusPhase.Idle, session.phase)
        assertEquals(1, platform.released)
    }

    @Test
    fun cancelExitReturnsToPreviousPhase() = runTest {
        val session = FocusSession(FakeFocusPlatform(), backgroundScope)
        session.lockIn()
        session.requestExit()
        session.cancelExit()
        assertEquals(FocusPhase.Locked, session.phase)
    }

    @Test
    fun leavingCountsAndResumeReengages() = runTest {
        val platform = FakeFocusPlatform()
        val session = FocusSession(platform, backgroundScope)
        session.lockIn()

        platform.presence(PresenceEvent.Left)
        assertEquals(FocusPhase.Away, session.phase)
        assertEquals(AwayReason.LeftApp, session.awayReason)
        assertEquals(1, session.leaveCount)

        platform.presence(PresenceEvent.Returned)
        assertEquals(FocusPhase.Away, session.phase, "returning still needs an explicit resume")

        session.resume()
        assertEquals(FocusPhase.Locked, session.phase)
        assertEquals(2, platform.engaged)

        platform.presence(PresenceEvent.FullscreenExited)
        assertEquals(AwayReason.FullscreenExited, session.awayReason)
        assertEquals(2, session.leaveCount)
    }

    @Test
    fun presenceIgnoredWhenIdleOrSuppressed() = runTest {
        val platform = FakeFocusPlatform()
        val session = FocusSession(platform, backgroundScope)
        platform.presence(PresenceEvent.Left)
        assertEquals(FocusPhase.Idle, session.phase)

        session.lockIn()
        session.suppressPresence = true
        platform.presence(PresenceEvent.Left)
        assertEquals(FocusPhase.Locked, session.phase)
    }

    @Test
    fun platformExitAttemptOpensChallenge() = runTest {
        val platform = FakeFocusPlatform()
        val session = FocusSession(platform, backgroundScope)
        platform.exitAttempt()
        assertEquals(FocusPhase.Idle, session.phase)
        session.lockIn()
        platform.exitAttempt()
        assertEquals(FocusPhase.ExitChallenge, session.phase)
    }

    @Test
    fun clockFormatting() {
        assertEquals("00:05", formatClock(5_000))
        assertEquals("24:18", formatClock((24 * 60 + 18) * 1000L))
        assertEquals("1:02:03", formatClock((3600 + 2 * 60 + 3) * 1000L))
        assertEquals("48m", formatMinutes(48 * 60_000L))
        assertEquals("1h 5m", formatMinutes(65 * 60_000L))
    }
}
