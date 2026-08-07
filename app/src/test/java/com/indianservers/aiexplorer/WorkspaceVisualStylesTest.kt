package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.SpatialMaterial
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceVisualStylesTest {
    @Test
    fun spectralDefaultUsesOnlyReferenceColors() {
        val allowed = setOf(
            WorkspaceVisualStyles.ReferenceCyan,
            WorkspaceVisualStyles.ReferenceBlue,
            WorkspaceVisualStyles.ReferenceViolet,
            WorkspaceVisualStyles.ReferenceMagenta,
            WorkspaceVisualStyles.ReferenceCoral,
            WorkspaceVisualStyles.ReferenceOrange,
            WorkspaceVisualStyles.ReferenceYellow,
        )

        assertEquals(WorkspacePaletteId.Spectral, WorkspaceAppearance().paletteId)
        assertTrue(WorkspaceVisualStyles.Spectral.colors.isNotEmpty())
        assertTrue(WorkspaceVisualStyles.Spectral.colors.all(allowed::contains))
        assertTrue(WorkspaceVisualStyles.Spectral.axes.x in allowed)
        assertTrue(WorkspaceVisualStyles.Spectral.axes.y in allowed)
        assertTrue(WorkspaceVisualStyles.Spectral.axes.z in allowed)
    }

    @Test
    fun surfaceAppearanceRoundTripsAllEditableProperties() {
        val edited = WorkspaceAppearance(
            paletteId = WorkspacePaletteId.Ember,
            colorIndex = 2,
            material = SpatialMaterial.Glass,
            texture = WorkspaceTexture.Contour,
            glow = false,
        )
        val layer = SpatialSurfaceLayer("surface", "x^2+y^2").withWorkspaceAppearance(edited)

        assertEquals(edited, layer.workspaceAppearance())
    }

    @Test
    fun everyPaletteHasDistinctAxisAndObjectColors() {
        assertEquals(9, WorkspaceVisualStyles.palettes.size)
        WorkspaceVisualStyles.palettes.forEach { palette ->
            assertTrue(palette.colors.size >= 4)
            assertEquals(palette.colors.size, palette.colors.distinct().size)
            assertTrue(palette.axes.x != palette.axes.y || palette.axes.y != palette.axes.z)
        }
    }

    @Test
    fun referenceThemesCarryTheirOwnMaterialTextureAndGlowDefaults() {
        assertEquals(SpatialMaterial.Gloss, WorkspaceVisualStyles.CyberNeon.defaultMaterial)
        assertEquals(WorkspaceTexture.Mesh, WorkspaceVisualStyles.CyberNeon.defaultTexture)
        assertTrue(WorkspaceVisualStyles.CyberNeon.defaultGlow)

        assertEquals(SpatialMaterial.Glass, WorkspaceVisualStyles.Abyss.defaultMaterial)
        assertEquals(SpatialMaterial.Metal, WorkspaceVisualStyles.EmeraldGold.defaultMaterial)
        assertEquals(WorkspaceTexture.Faceted, WorkspaceVisualStyles.EmeraldGold.defaultTexture)

        assertEquals(SpatialMaterial.Glass, WorkspaceVisualStyles.Porcelain.defaultMaterial)
        assertEquals(WorkspaceTexture.Smooth, WorkspaceVisualStyles.Porcelain.defaultTexture)
        assertEquals(false, WorkspaceVisualStyles.Porcelain.defaultGlow)

        assertEquals(SpatialMaterial.Matte, WorkspaceVisualStyles.Topographic.defaultMaterial)
        assertEquals(false, WorkspaceVisualStyles.Topographic.defaultGlow)
    }

    @Test
    fun everyPaletteRoundTripsThroughSurfaceStorage() {
        WorkspaceVisualStyles.palettes.forEachIndexed { index, palette ->
            val appearance = WorkspaceAppearance().copy(colorIndex = index).switchPalette(palette)
            val layer = SpatialSurfaceLayer("surface-$index", "x+y").withWorkspaceAppearance(appearance)
            assertEquals(appearance, layer.workspaceAppearance())
        }
    }
}
