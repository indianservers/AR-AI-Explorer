package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.adaptive.adaptiveDialogBounds
import com.indianservers.aiexplorer.adaptive.adaptiveFocusGroup
import com.indianservers.aiexplorer.adaptive.tvRemoteScrollable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Every application-owned colour role. Document and dataset colours remain independent. */
data class AppPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val navigation: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val cyan: Color,
    val violet: Color,
    val amber: Color,
    val coral: Color,
    val green: Color,
    val border: Color,
    val divider: Color,
    val grid: Color,
    val axis: Color,
    val selected: Color,
    val unselected: Color,
    val disabledSurface: Color,
    val disabledContent: Color,
    val shadow: Color,
    val tooltip: Color,
    val onTooltip: Color,
    val focus: Color,
    val input: Color,
    val geometryFill: Color,
    val geometryOutline: Color,
    val matrixHighlight: Color,
    val calculatorKey: Color,
    val calculatorOperator: Color,
    val chartColors: List<Color>,
) {
    val secondary: Color get() = violet
    val success: Color get() = green
    val warning: Color get() = amber
    val error: Color get() = coral
    val graphPrimary: Color get() = primary
    val graphSecondary: Color get() = violet
    val point: Color get() = cyan
    val handle: Color get() = cyan
    val vectorAxis: Color get() = axis
}

private fun currentPalette() = AppPalette(
    background = Color(0xFF030507),
    surface = Color(0xFF07101A),
    surfaceAlt = Color(0xFF0B1017),
    navigation = Color(0xFF07101A),
    ink = Color(0xFFEAF5FF),
    muted = Color(0xFFB8C4D8),
    primary = Color(0xFF20D9FF),
    cyan = Color(0xFF20D9FF),
    violet = Color(0xFF985DFF),
    amber = Color(0xFFFFC857),
    coral = Color(0xFFFF5E73),
    green = Color(0xFF48E0A4),
    border = Color(0xFF20D9FF).copy(alpha = .28f),
    divider = Color(0xFFB8C4D8).copy(alpha = .18f),
    grid = Color(0xFF20D9FF).copy(alpha = .20f),
    axis = Color(0xFFEAF5FF).copy(alpha = .85f),
    selected = Color(0xFF20D9FF),
    unselected = Color(0xFFB8C4D8),
    disabledSurface = Color(0xFF0B1017),
    disabledContent = Color(0xFFB8C4D8).copy(alpha = .38f),
    shadow = Color.Black,
    tooltip = Color(0xFF07101A),
    onTooltip = Color(0xFFEAF5FF),
    focus = Color(0xFF20D9FF),
    input = Color(0xFF0B1017),
    geometryFill = Color(0xFFFFC857),
    geometryOutline = Color(0xFFFFC857),
    matrixHighlight = Color(0xFF985DFF),
    calculatorKey = Color(0xFF0B1017),
    calculatorOperator = Color(0xFFFFC857),
    chartColors = listOf(
        Color(0xFF20D9FF), Color(0xFF985DFF), Color(0xFFFFC857),
        Color(0xFFFF5E73), Color(0xFF48E0A4), Color(0xFF70D6E8),
    ),
)

/** Exact practical colours sampled from the supplied V4 redesign screenshots. */
private fun mathsExplorerVibrantPalette() = AppPalette(
    background = Color(0xFFF4F7FF),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF6F8FE),
    navigation = Color(0xFF121A33),
    ink = Color(0xFF121A33),
    muted = Color(0xFF64708A),
    primary = Color(0xFF5458E8),
    cyan = Color(0xFF08A9B8),
    violet = Color(0xFF8B5CF6),
    amber = Color(0xFFF2A516),
    coral = Color(0xFFEF6375),
    green = Color(0xFF21A66A),
    border = Color(0xFFD9E2F2),
    divider = Color(0xFFE3E9F4),
    grid = Color(0xFFD7DFEF),
    axis = Color(0xFF414960),
    selected = Color(0xFF5458E8),
    unselected = Color(0xFFB6BAC9),
    disabledSurface = Color(0xFFEEF2F9),
    disabledContent = Color(0xFFBBBEC9),
    shadow = Color(0xFF121A33).copy(alpha = .14f),
    tooltip = Color(0xFF121A33),
    onTooltip = Color.White,
    focus = Color(0xFF5458E8),
    input = Color.White,
    geometryFill = Color(0xFFFFF5DA),
    geometryOutline = Color(0xFFF2A516),
    matrixHighlight = Color(0xFFF2ECFF),
    calculatorKey = Color(0xFFF6F8FE),
    calculatorOperator = Color(0xFFF2A516),
    chartColors = listOf(
        Color(0xFF5458E8), Color(0xFF8B5CF6), Color(0xFF08A9B8),
        Color(0xFFEF6375), Color(0xFF21A66A), Color(0xFFF2A516),
    ),
)

enum class AppColorScheme(val displayName: String, val description: String, val palette: AppPalette) {
    Current("Current", "The original Maths Explorer colour theme.", currentPalette()),
    MathsExplorerVibrant(
        "Maths Explorer Vibrant",
        "Indigo, teal and purposeful maths accents on light workspaces.",
        mathsExplorerVibrantPalette(),
    ),
}

internal fun AppColorScheme.materialColorScheme() = palette.let { colors ->
    if (this == AppColorScheme.Current) {
        // Keep the original Material colour mapping untouched for existing users.
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceAlt,
            primary = colors.primary,
            secondary = colors.secondary,
            tertiary = colors.success,
            onBackground = colors.ink,
            onSurface = colors.ink,
            onPrimary = colors.background,
            onSecondary = colors.background,
        )
    } else {
        darkColorScheme(
            background = colors.background,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.muted,
            surfaceTint = colors.primary,
            inverseSurface = colors.navigation,
            inverseOnSurface = colors.onTooltip,
            primary = colors.primary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEEF0FF),
            onPrimaryContainer = colors.ink,
            inversePrimary = Color(0xFFBFC2FF),
            secondary = colors.violet,
            onSecondary = Color(0xFF030507),
            secondaryContainer = colors.matrixHighlight,
            onSecondaryContainer = colors.ink,
            tertiary = colors.cyan,
            onTertiary = colors.ink,
            tertiaryContainer = Color(0xFFE8FAFC),
            onTertiaryContainer = colors.ink,
            error = colors.coral,
            onError = colors.ink,
            errorContainer = Color(0xFFFFF0F2),
            onErrorContainer = colors.ink,
            outline = colors.border,
            outlineVariant = colors.divider,
            scrim = colors.shadow,
        )
    }
}

internal var Ink by mutableStateOf(currentPalette().ink)
internal var Muted by mutableStateOf(currentPalette().muted)
internal var Background by mutableStateOf(currentPalette().background)
internal var SurfaceA by mutableStateOf(currentPalette().surface.copy(alpha = 0.87f))
internal var SurfaceB by mutableStateOf(currentPalette().surfaceAlt.copy(alpha = 0.73f))
internal var Navy by mutableStateOf(currentPalette().navigation)
internal var Cyan by mutableStateOf(currentPalette().cyan)
internal var Violet by mutableStateOf(currentPalette().violet)
internal var Green by mutableStateOf(currentPalette().green)
internal var Amber by mutableStateOf(currentPalette().amber)
internal var Coral by mutableStateOf(currentPalette().coral)
internal var Grid by mutableStateOf(currentPalette().grid)
internal var Axis by mutableStateOf(currentPalette().axis)
internal var AppBorder by mutableStateOf(currentPalette().border)
internal var ActiveAppPalette by mutableStateOf(currentPalette())

/** Preserves a screen's exact legacy colour in Current and supplies its semantic Vibrant colour. */
internal fun themedColor(current: Color, vibrant: Color): Color =
    if (ActiveAppPalette == AppColorScheme.Current.palette) current else vibrant

internal fun applyAppPalette(palette: AppPalette) {
    ActiveAppPalette = palette
    Background = palette.background
    val vibrant = palette == AppColorScheme.MathsExplorerVibrant.palette
    SurfaceA = if (vibrant) palette.surface else palette.surface.copy(alpha = 0.87f)
    SurfaceB = if (vibrant) palette.surfaceAlt else palette.surfaceAlt.copy(alpha = 0.73f)
    Navy = palette.navigation
    Ink = palette.ink
    Muted = palette.muted
    Cyan = palette.cyan
    Violet = palette.violet
    Green = palette.green
    Amber = palette.amber
    Coral = palette.coral
    Grid = palette.grid
    Axis = palette.axis
    AppBorder = palette.border
}

internal fun appBackdrop(scheme: AppColorScheme): Brush {
    val palette = scheme.palette
    return if (scheme == AppColorScheme.Current) {
        Brush.radialGradient(
            colors = listOf(
                palette.primary.copy(alpha = 0.20f),
                palette.background,
                palette.background,
            ),
            radius = 1100f,
            center = Offset(420f, 220f),
        )
    } else {
        Brush.linearGradient(listOf(palette.background, palette.background))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AppAppearanceDialog(
    selected: AppColorScheme,
    onSelect: (AppColorScheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .adaptiveDialogBounds()
                .border(1.dp, selected.palette.primary.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
            color = selected.palette.surface.copy(alpha = 0.98f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .adaptiveFocusGroup()
                    .tvRemoteScrollable(scrollState)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Appearance", color = selected.palette.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Color scheme applies across the entire app", color = selected.palette.muted, fontSize = 12.sp)
                    }
                    GlowButton("Close", icon = "close", iconOnly = true, onClick = onDismiss)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppColorScheme.entries.forEach { scheme ->
                        val active = scheme == selected
                        Column(
                            Modifier
                                .widthIn(min = 132.dp, max = 190.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(scheme.palette.surfaceAlt)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) scheme.palette.primary else scheme.palette.muted.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .clickable { onSelect(scheme) }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(
                                    scheme.palette.primary,
                                    scheme.palette.secondary,
                                    scheme.palette.success,
                                    scheme.palette.warning,
                                ).forEach { color ->
                                    Box(Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)).background(color))
                                }
                            }
                            Text(
                                if (active) "${scheme.displayName} - Selected" else scheme.displayName,
                                color = scheme.palette.ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                            Text(scheme.description, color = scheme.palette.muted, fontSize = 10.sp, lineHeight = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
