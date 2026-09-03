package com.indianservers.aiexplorer

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVisualEffectsTest {
    @Test
    fun exactlyTwoBuiltInThemesAreAvailable() {
        assertEquals(
            listOf("Current", "Maths Explorer Vibrant"),
            AppColorScheme.entries.map { it.displayName },
        )
    }

    @Test
    fun currentPaletteRetainsTheOriginalCoreColors() {
        val current = AppColorScheme.Current.palette
        assertEquals(Color(0xFF030507), current.background)
        assertEquals(Color(0xFF07101A), current.surface)
        assertEquals(Color(0xFF0B1017), current.surfaceAlt)
        assertEquals(Color(0xFFEAF5FF), current.ink)
        assertEquals(Color(0xFFB8C4D8), current.muted)
        assertEquals(Color(0xFF20D9FF), current.primary)
        assertEquals(Color(0xFF985DFF), current.secondary)
        assertEquals(Color(0xFF48E0A4), current.success)
        assertEquals(Color(0xFFFFC857), current.warning)
    }

    @Test
    fun vibrantPaletteUsesTheSampledV4ColorSystem() {
        val vibrant = AppColorScheme.MathsExplorerVibrant.palette
        assertEquals(Color(0xFFF4F7FF), vibrant.background)
        assertEquals(Color.White, vibrant.surface)
        assertEquals(Color(0xFF121A33), vibrant.navigation)
        assertEquals(Color(0xFF121A33), vibrant.ink)
        assertEquals(Color(0xFF64708A), vibrant.muted)
        assertEquals(Color(0xFFD9E2F2), vibrant.border)
        assertEquals(Color(0xFF5458E8), vibrant.primary)
        assertEquals(Color(0xFF08A9B8), vibrant.cyan)
        assertEquals(Color(0xFF8B5CF6), vibrant.violet)
        assertEquals(Color(0xFFF2A516), vibrant.amber)
        assertEquals(Color(0xFFEF6375), vibrant.coral)
        assertEquals(Color(0xFF21A66A), vibrant.green)
        assertEquals(6, vibrant.chartColors.size)
    }

    @Test
    fun themesOnlyUseTheStandardColorTreatment() {
        AppColorScheme.entries.forEach { scheme ->
            assertFalse(visualEffectsFor(scheme).enhanced)
            assertEquals(0f, visualEffectsFor(scheme).gridGlowAlpha)
            assertEquals(0f, visualEffectsFor(scheme).graphGlowAlpha)
        }
    }

    @Test
    fun visualEffectsAreBoundedAndContainNoLayoutConfiguration() {
        AppColorScheme.entries.map(::visualEffectsFor).forEach { effects ->
            listOf(
                effects.backdropAccentAlpha,
                effects.backdropSecondaryAlpha,
                effects.surfaceTintAlpha,
                effects.borderGlowAlpha,
                effects.activeGlowAlpha,
                effects.gridGlowAlpha,
                effects.graphGlowAlpha,
            ).forEach { value -> assertTrue(value in 0f..1f) }
        }
    }

    @Test
    fun activeControlDetectionIsExplicitAndDoesNotMisreadInactiveLabels() {
        listOf("• Plot", "Compare: ON", "Selected: Vibrant", "Trace active", "Method current", "Comparing")
            .forEach { assertTrue(it, isVisuallyActiveLabel(it)) }
        listOf("Plot", "Inactive", "Turn on", "Current value", "Select Vibrant")
            .forEach { assertFalse(it, isVisuallyActiveLabel(it)) }
    }
}
