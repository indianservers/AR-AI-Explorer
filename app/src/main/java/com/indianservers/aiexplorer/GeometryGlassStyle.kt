package com.indianservers.aiexplorer

import androidx.compose.runtime.staticCompositionLocalOf

// Scoped to the geometry workspace so shared controls keep their existing style elsewhere.
internal val LocalGeometryGlass = staticCompositionLocalOf { false }

internal val GeometryGlassEffects = AppVisualEffects(
    treatment = AppVisualTreatment.NeonGlass,
    backdropAccentAlpha = .025f,
    backdropSecondaryAlpha = .025f,
    surfaceTintAlpha = .08f,
    borderGlowAlpha = .65f,
    activeGlowAlpha = .35f,
    gridGlowAlpha = .10f,
    graphGlowAlpha = .12f,
)
