package com.indianservers.aiexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class AppVisualTreatment {
    Standard,
    NeonGlass,
    SpectralWireframe,
}

/**
 * Presentation-only values. They never alter layout, input, maths, rendering
 * resolution, animation cadence, or workspace state.
 */
@Immutable
internal data class AppVisualEffects(
    val treatment: AppVisualTreatment,
    val backdropAccentAlpha: Float,
    val backdropSecondaryAlpha: Float,
    val surfaceTintAlpha: Float,
    val borderGlowAlpha: Float,
    val activeGlowAlpha: Float,
    val gridGlowAlpha: Float,
    val graphGlowAlpha: Float,
) {
    val enhanced: Boolean get() = treatment != AppVisualTreatment.Standard

    companion object {
        val Standard = AppVisualEffects(
            treatment = AppVisualTreatment.Standard,
            backdropAccentAlpha = 0f,
            backdropSecondaryAlpha = 0f,
            surfaceTintAlpha = 0f,
            borderGlowAlpha = 0f,
            activeGlowAlpha = 0f,
            gridGlowAlpha = 0f,
            graphGlowAlpha = 0f,
        )
    }
}

internal val LocalAppVisualEffects = staticCompositionLocalOf { AppVisualEffects.Standard }

internal fun visualEffectsFor(scheme: AppColorScheme): AppVisualEffects = when (scheme) {
    AppColorScheme.Aurora -> AppVisualEffects(
        treatment = AppVisualTreatment.NeonGlass,
        backdropAccentAlpha = .16f,
        backdropSecondaryAlpha = .10f,
        surfaceTintAlpha = .08f,
        borderGlowAlpha = .48f,
        activeGlowAlpha = .22f,
        gridGlowAlpha = .22f,
        graphGlowAlpha = .32f,
    )
    AppColorScheme.Royal -> AppVisualEffects(
        treatment = AppVisualTreatment.SpectralWireframe,
        backdropAccentAlpha = .18f,
        backdropSecondaryAlpha = .13f,
        surfaceTintAlpha = .09f,
        borderGlowAlpha = .52f,
        activeGlowAlpha = .25f,
        gridGlowAlpha = .28f,
        graphGlowAlpha = .38f,
    )
    else -> AppVisualEffects.Standard
}

internal fun isVisuallyActiveLabel(label: String): Boolean {
    val normalized = label.trim().lowercase()
    return normalized.startsWith("•") ||
        normalized.endsWith(": on") ||
        normalized.endsWith(" active") ||
        normalized.endsWith(" current") ||
        normalized.startsWith("selected:") ||
        normalized == "comparing"
}

@Composable
internal fun ProvideAppVisualEffects(
    scheme: AppColorScheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppVisualEffects provides visualEffectsFor(scheme),
        content = content,
    )
}

/**
 * Adds an in-bounds, cached treatment to visual workspaces. Standard themes
 * return the original modifier unchanged.
 */
@Composable
internal fun Modifier.appWorkspaceTreatment(
    cornerRadius: Dp = 8.dp,
    accent: Color? = null,
    secondary: Color? = null,
): Modifier {
    val effects = LocalAppVisualEffects.current
    if (!effects.enhanced) return this

    val primaryColor = accent ?: MaterialTheme.colorScheme.primary
    val secondaryColor = secondary ?: MaterialTheme.colorScheme.secondary
    return then(
        Modifier.drawWithCache {
            val radiusPx = cornerRadius.toPx()
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = effects.borderGlowAlpha),
                    secondaryColor.copy(alpha = effects.borderGlowAlpha * .78f),
                    primaryColor.copy(alpha = effects.borderGlowAlpha),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val innerBrush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = effects.activeGlowAlpha * .32f),
                    Color.Transparent,
                    secondaryColor.copy(alpha = effects.activeGlowAlpha * .26f),
                ),
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
            )
            val radius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx)
            val cornerLength = minOf(size.width, size.height) * .055f
            val cornerColor = when (effects.treatment) {
                AppVisualTreatment.NeonGlass -> primaryColor.copy(alpha = effects.activeGlowAlpha)
                AppVisualTreatment.SpectralWireframe -> secondaryColor.copy(alpha = effects.activeGlowAlpha)
                AppVisualTreatment.Standard -> Color.Transparent
            }

            onDrawWithContent {
                drawContent()
                drawRoundRect(
                    brush = innerBrush,
                    cornerRadius = radius,
                    style = Stroke(width = 3f),
                )
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = radius,
                    style = Stroke(width = 1f),
                )
                drawLine(cornerColor, Offset(1f, cornerLength), Offset(1f, 1f), 2f)
                drawLine(cornerColor, Offset(1f, 1f), Offset(cornerLength, 1f), 2f)
                drawLine(cornerColor, Offset(size.width - cornerLength, size.height - 1f), Offset(size.width - 1f, size.height - 1f), 2f)
                drawLine(cornerColor, Offset(size.width - 1f, size.height - cornerLength), Offset(size.width - 1f, size.height - 1f), 2f)
            }
        },
    )
}
