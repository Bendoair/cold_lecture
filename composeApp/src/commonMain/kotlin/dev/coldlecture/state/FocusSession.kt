package dev.coldlecture.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.coldlecture.data.nowMillis
import dev.coldlecture.platform.FocusPlatform
import dev.coldlecture.platform.LockStrength
import dev.coldlecture.platform.PresenceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class FocusPhase { Idle, Locked, Away, ExitChallenge }

enum class AwayReason { LeftApp, FullscreenExited }

/**
 * Lock-in session shared by all platforms. The platform decides how strong the lock is;
 * this class owns the friction (exit code), the streak and the "you left" bookkeeping.
 */
class FocusSession(
    private val platform: FocusPlatform,
    scope: CoroutineScope,
) {
    val strength: LockStrength get() = platform.strength

    var phase by mutableStateOf(FocusPhase.Idle)
        private set
    var now by mutableLongStateOf(nowMillis())
        private set
    var exitCode by mutableStateOf("")
        private set
    var awayReason by mutableStateOf(AwayReason.LeftApp)
        private set
    var leaveCount by mutableIntStateOf(0)
        private set
    var lastAwayMillis by mutableLongStateOf(0L)
        private set
    var bestStreakMillis by mutableLongStateOf(0L)
        private set

    private var sessionStartedAt by mutableLongStateOf(0L)
    private var streakStartedAt by mutableLongStateOf(0L)
    private var awayStartedAt = 0L
    private var phaseBeforeChallenge = FocusPhase.Locked

    /** Platform dialogs (file picker) steal window focus; that should not count as leaving. */
    var suppressPresence = false

    val isActive: Boolean get() = phase != FocusPhase.Idle
    val sessionMillis: Long get() = if (isActive) now - sessionStartedAt else 0L
    val streakMillis: Long get() = if (phase == FocusPhase.Locked || phase == FocusPhase.ExitChallenge) now - streakStartedAt else 0L
    val awayMillis: Long get() = if (phase == FocusPhase.Away) now - awayStartedAt else 0L

    init {
        platform.setPresenceListener(::onPresence)
        platform.setExitAttemptListener(::requestExit)
        scope.launch {
            while (true) {
                now = nowMillis()
                delay(1_000)
            }
        }
    }

    fun lockIn() {
        if (isActive) return
        now = nowMillis()
        sessionStartedAt = now
        streakStartedAt = now
        leaveCount = 0
        bestStreakMillis = 0
        platform.engage()
        phase = FocusPhase.Locked
    }

    fun requestExit() {
        if (phase != FocusPhase.Locked && phase != FocusPhase.Away) return
        phaseBeforeChallenge = phase
        exitCode = generateCode()
        phase = FocusPhase.ExitChallenge
    }

    fun cancelExit() {
        if (phase == FocusPhase.ExitChallenge) phase = phaseBeforeChallenge
    }

    /** Returns true when the typed code matched and the session ended. */
    fun submitExitCode(input: String): Boolean {
        if (phase != FocusPhase.ExitChallenge) return false
        if (!input.trim().equals(exitCode, ignoreCase = true)) return false
        recordStreak()
        platform.release()
        phase = FocusPhase.Idle
        return true
    }

    fun resume() {
        if (phase != FocusPhase.Away) return
        lastAwayMillis = nowMillis() - awayStartedAt
        now = nowMillis()
        streakStartedAt = now
        platform.engage()
        phase = FocusPhase.Locked
    }

    private fun onPresence(event: PresenceEvent) {
        if (suppressPresence) return
        when (event) {
            PresenceEvent.Left -> goAway(AwayReason.LeftApp)
            PresenceEvent.FullscreenExited -> goAway(AwayReason.FullscreenExited)
            PresenceEvent.Returned -> Unit
        }
    }

    private fun goAway(reason: AwayReason) {
        if (phase != FocusPhase.Locked) return
        recordStreak()
        now = nowMillis()
        awayStartedAt = now
        awayReason = reason
        leaveCount++
        phase = FocusPhase.Away
    }

    private fun recordStreak() {
        bestStreakMillis = maxOf(bestStreakMillis, nowMillis() - streakStartedAt)
    }

    private fun generateCode(): String = buildString {
        repeat(EXIT_CODE_LENGTH) { append(CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)]) }
    }

    private companion object {
        const val EXIT_CODE_LENGTH = 6
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

fun formatClock(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}

fun formatMinutes(millis: Long): String {
    val minutes = millis / 60_000
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
