package dev.coldlecture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.coldlecture.resources.Res
import dev.coldlecture.resources.ibm_plex_mono_medium
import dev.coldlecture.resources.ibm_plex_mono_regular
import dev.coldlecture.resources.ibm_plex_mono_semibold
import dev.coldlecture.resources.ibm_plex_sans_bold
import dev.coldlecture.resources.ibm_plex_sans_medium
import dev.coldlecture.resources.ibm_plex_sans_regular
import dev.coldlecture.resources.ibm_plex_sans_semibold
import org.jetbrains.compose.resources.Font

/** App-specific colors that Material's scheme has no slot for. */
@Immutable
data class LectureColors(
    val backdrop: Color,
    val backdropEdge: Color,
    val panel: Color,
    val panelRaised: Color,
    val hairline: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color,
    val streak: Color,
    val streakSoft: Color,
    val ok: Color,
    val slideStage: Color,
    val isDark: Boolean,
)

private val LightLecture = LectureColors(
    backdrop = Color(0xFFEDF0F6),
    backdropEdge = Color(0xFFE2E7F1),
    panel = Color(0xFFFFFFFF),
    panelRaised = Color(0xFFF5F7FB),
    hairline = Color(0xFFDCE1EC),
    muted = Color(0xFF5E6781),
    accent = Color(0xFF2F5BEA),
    accentSoft = Color(0xFFE3EAFE),
    streak = Color(0xFFE2671F),
    streakSoft = Color(0xFFFFEEDF),
    ok = Color(0xFF1F9D63),
    slideStage = Color(0xFFE6EAF2),
    isDark = false,
)

private val DarkLecture = LectureColors(
    backdrop = Color(0xFF0C111E),
    backdropEdge = Color(0xFF111829),
    panel = Color(0xFF141B2C),
    panelRaised = Color(0xFF1A2236),
    hairline = Color(0xFF263049),
    muted = Color(0xFF8A94AE),
    accent = Color(0xFF5B85FF),
    accentSoft = Color(0xFF1C2A52),
    streak = Color(0xFFFF8A3D),
    streakSoft = Color(0xFF3A2516),
    ok = Color(0xFF3DD68C),
    slideStage = Color(0xFF0F1524),
    isDark = true,
)

val LocalLectureColors = staticCompositionLocalOf { LightLecture }
val LocalMonoFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

object Lecture {
    val colors: LectureColors
        @Composable get() = LocalLectureColors.current
    val mono: FontFamily
        @Composable get() = LocalMonoFamily.current
}

@Composable
fun ColdLectureTheme(dark: Boolean, content: @Composable () -> Unit) {
    val sans = FontFamily(
        Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
        Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
        Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold),
    )
    val mono = FontFamily(
        Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
        Font(Res.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    )
    val lecture = if (dark) DarkLecture else LightLecture
    val scheme = if (dark) {
        darkColorScheme(
            primary = lecture.accent,
            onPrimary = Color.White,
            primaryContainer = lecture.accentSoft,
            onPrimaryContainer = Color(0xFFC9D6FF),
            background = lecture.backdrop,
            onBackground = Color(0xFFE6EAF5),
            surface = lecture.panel,
            onSurface = Color(0xFFE6EAF5),
            surfaceVariant = lecture.panelRaised,
            onSurfaceVariant = lecture.muted,
            surfaceContainer = lecture.panelRaised,
            surfaceContainerHigh = Color(0xFF202A41),
            outline = lecture.hairline,
            outlineVariant = lecture.hairline,
            error = Color(0xFFFF6B6B),
        )
    } else {
        lightColorScheme(
            primary = lecture.accent,
            onPrimary = Color.White,
            primaryContainer = lecture.accentSoft,
            onPrimaryContainer = Color(0xFF15307F),
            background = lecture.backdrop,
            onBackground = Color(0xFF141A2A),
            surface = lecture.panel,
            onSurface = Color(0xFF141A2A),
            surfaceVariant = lecture.panelRaised,
            onSurfaceVariant = lecture.muted,
            surfaceContainer = lecture.panelRaised,
            surfaceContainerHigh = Color(0xFFEAEEF6),
            outline = lecture.hairline,
            outlineVariant = lecture.hairline,
            error = Color(0xFFC73A3A),
        )
    }
    val base = TextStyle(fontFamily = sans)
    val typography = Typography(
        titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleMedium = base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = base.copy(fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = base.copy(fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontFamily = mono, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
        labelSmall = TextStyle(fontFamily = mono, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    )
    // Panels use raw backgrounds (not Material Surface), so LocalContentColor would
    // otherwise stay at its default black and vanish on dark panels.
    CompositionLocalProvider(
        LocalLectureColors provides lecture,
        LocalMonoFamily provides mono,
        LocalContentColor provides scheme.onSurface,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
