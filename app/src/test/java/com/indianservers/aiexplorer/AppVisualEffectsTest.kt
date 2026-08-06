package com.indianservers.aiexplorer

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVisualEffectsTest {
    @Test
    fun existingPaletteSetAndColorValuesRemainUnchanged() {
        assertEquals(
            listOf("Modern", "Aurora", "Ocean", "Forest", "Solar", "Crimson", "Royal", "Mono", "Sunset"),
            AppColorScheme.entries.map { it.name },
        )
        assertEquals(Color(0xFF030507), AppColorScheme.Modern.palette.background)
        assertEquals(Color(0xFF20D9FF), AppColorScheme.Modern.palette.primary)
        assertEquals(Color(0xFF52F5C8), AppColorScheme.Aurora.palette.primary)
        assertEquals(Color(0xFFE66BFF), AppColorScheme.Aurora.palette.secondary)
        assertEquals(Color(0xFF7C83FF), AppColorScheme.Royal.palette.primary)
        assertEquals(Color(0xFFFFC857), AppColorScheme.Royal.palette.secondary)
    }

    @Test
    fun onlyAuroraAndRoyalReceiveEnhancedVisualTreatment() {
        val enhanced = AppColorScheme.entries.filter { visualEffectsFor(it).enhanced }
        assertEquals(listOf(AppColorScheme.Aurora, AppColorScheme.Royal), enhanced)
        assertEquals(AppVisualTreatment.NeonGlass, visualEffectsFor(AppColorScheme.Aurora).treatment)
        assertEquals(AppVisualTreatment.SpectralWireframe, visualEffectsFor(AppColorScheme.Royal).treatment)
        assertFalse(visualEffectsFor(AppColorScheme.Modern).enhanced)
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
        assertEquals(
            setOf(
                "treatment",
                "backdropAccentAlpha",
                "backdropSecondaryAlpha",
                "surfaceTintAlpha",
                "borderGlowAlpha",
                "activeGlowAlpha",
                "gridGlowAlpha",
                "graphGlowAlpha",
            ),
            AppVisualEffects::class.java.declaredFields
                .map { it.name }
                .filterNot { it.startsWith("$") || it in setOf("Companion", "Standard") }
                .toSet(),
        )
    }

    @Test
    fun activeControlDetectionIsExplicitAndDoesNotMisreadInactiveLabels() {
        listOf("• Plot", "Compare: ON", "Selected: Royal", "Trace active", "Method current", "Comparing")
            .forEach { assertTrue(it, isVisuallyActiveLabel(it)) }
        listOf("Plot", "Inactive", "Turn on", "Current value", "Select Royal")
            .forEach { assertFalse(it, isVisuallyActiveLabel(it)) }
    }

    @Test
    fun enhancedEffectsHaveVisibleButSubtleGraphAndGridStrengths() {
        listOf(AppColorScheme.Aurora, AppColorScheme.Royal).map(::visualEffectsFor).forEach { effects ->
            assertTrue(effects.gridGlowAlpha in .15f..45f)
            assertTrue(effects.graphGlowAlpha in .20f..50f)
            assertTrue(effects.activeGlowAlpha < effects.borderGlowAlpha)
        }
        assertEquals(0f, visualEffectsFor(AppColorScheme.Modern).gridGlowAlpha)
        assertEquals(0f, visualEffectsFor(AppColorScheme.Modern).graphGlowAlpha)
    }
}
