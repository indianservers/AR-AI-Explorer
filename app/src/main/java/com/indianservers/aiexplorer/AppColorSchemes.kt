package com.indianservers.aiexplorer

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

data class AppPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val secondary: Color,
    val success: Color,
    val warning: Color,
)

enum class AppColorScheme(val displayName: String, val description: String, val palette: AppPalette) {
    Modern(
        "Modern",
        "The original cyan and violet AI Explorer look.",
        AppPalette(
            background = Color(0xFF030507), surface = Color(0xFF07101A), surfaceAlt = Color(0xFF0B1017),
            ink = Color(0xFFEAF5FF), muted = Color(0xFFB8C4D8), primary = Color(0xFF20D9FF),
            secondary = Color(0xFF985DFF), success = Color(0xFF48E0A4), warning = Color(0xFFFFC857),
        ),
    ),
    Aurora(
        "Aurora",
        "Mint, electric blue and magenta on deep charcoal.",
        AppPalette(
            background = Color(0xFF05080B), surface = Color(0xFF0A1518), surfaceAlt = Color(0xFF11141D),
            ink = Color(0xFFF1FFFC), muted = Color(0xFFB7CAC7), primary = Color(0xFF52F5C8),
            secondary = Color(0xFFE66BFF), success = Color(0xFF80ED99), warning = Color(0xFFFFD166),
        ),
    ),
    Ocean(
        "Ocean",
        "Marine blue with aqua, coral and clean white.",
        AppPalette(
            background = Color(0xFF031018), surface = Color(0xFF071B26), surfaceAlt = Color(0xFF10212D),
            ink = Color(0xFFF0FAFF), muted = Color(0xFFA8C7D5), primary = Color(0xFF31C7D8),
            secondary = Color(0xFFFF7A72), success = Color(0xFF58D6A9), warning = Color(0xFFFFD166),
        ),
    ),
    Forest(
        "Forest",
        "Deep green with mint, gold and sky accents.",
        AppPalette(
            background = Color(0xFF06100C), surface = Color(0xFF0D1C15), surfaceAlt = Color(0xFF17211B),
            ink = Color(0xFFF1FBF4), muted = Color(0xFFB4C8BA), primary = Color(0xFF63D69B),
            secondary = Color(0xFF67B7E1), success = Color(0xFFA3E635), warning = Color(0xFFF6C453),
        ),
    ),
    Solar(
        "Solar",
        "Graphite with warm gold, cyan and energetic coral.",
        AppPalette(
            background = Color(0xFF0D0C0A), surface = Color(0xFF1A1711), surfaceAlt = Color(0xFF211D18),
            ink = Color(0xFFFFF8E8), muted = Color(0xFFD0C4AA), primary = Color(0xFFFFC247),
            secondary = Color(0xFF42D4D0), success = Color(0xFF72D572), warning = Color(0xFFFF7B54),
        ),
    ),
    Crimson(
        "Crimson",
        "Wine red balanced by ice blue and rose.",
        AppPalette(
            background = Color(0xFF10070B), surface = Color(0xFF211018), surfaceAlt = Color(0xFF281820),
            ink = Color(0xFFFFF2F5), muted = Color(0xFFD2B7BF), primary = Color(0xFFFF5C7A),
            secondary = Color(0xFF70D6E8), success = Color(0xFF66D19E), warning = Color(0xFFFFC857),
        ),
    ),
    Royal(
        "Royal",
        "Indigo, gold and turquoise with high visual depth.",
        AppPalette(
            background = Color(0xFF080817), surface = Color(0xFF12142A), surfaceAlt = Color(0xFF1B1C35),
            ink = Color(0xFFF7F5FF), muted = Color(0xFFBEBCDA), primary = Color(0xFF7C83FF),
            secondary = Color(0xFFFFC857), success = Color(0xFF46D9B0), warning = Color(0xFFFF8A5B),
        ),
    ),
    Mono(
        "Mono",
        "Neutral graphite with crisp white and restrained teal.",
        AppPalette(
            background = Color(0xFF090A0B), surface = Color(0xFF151719), surfaceAlt = Color(0xFF202326),
            ink = Color(0xFFF7F7F7), muted = Color(0xFFBFC3C7), primary = Color(0xFFE8ECEF),
            secondary = Color(0xFF64D8CB), success = Color(0xFF73D69C), warning = Color(0xFFF1C75B),
        ),
    ),
    Sunset(
        "Sunset",
        "Coral, lavender and turquoise over warm black.",
        AppPalette(
            background = Color(0xFF100A0E), surface = Color(0xFF21141B), surfaceAlt = Color(0xFF291C24),
            ink = Color(0xFFFFF5F1), muted = Color(0xFFD7BBB4), primary = Color(0xFFFF8066),
            secondary = Color(0xFFB88CFF), success = Color(0xFF55D6BE), warning = Color(0xFFFFD166),
        ),
    ),
}

internal var Ink by mutableStateOf(AppColorScheme.Modern.palette.ink)
internal var Muted by mutableStateOf(AppColorScheme.Modern.palette.muted)
internal var Background by mutableStateOf(AppColorScheme.Modern.palette.background)
internal var SurfaceA by mutableStateOf(AppColorScheme.Modern.palette.surface.copy(alpha = 0.87f))
internal var SurfaceB by mutableStateOf(AppColorScheme.Modern.palette.surfaceAlt.copy(alpha = 0.73f))
internal var Cyan by mutableStateOf(AppColorScheme.Modern.palette.primary)
internal var Violet by mutableStateOf(AppColorScheme.Modern.palette.secondary)
internal var Green by mutableStateOf(AppColorScheme.Modern.palette.success)
internal var Amber by mutableStateOf(AppColorScheme.Modern.palette.warning)
internal var Grid by mutableStateOf(AppColorScheme.Modern.palette.primary.copy(alpha = 0.20f))

internal fun applyAppPalette(palette: AppPalette) {
    Background = palette.background
    SurfaceA = palette.surface.copy(alpha = 0.87f)
    SurfaceB = palette.surfaceAlt.copy(alpha = 0.73f)
    Ink = palette.ink
    Muted = palette.muted
    Cyan = palette.primary
    Violet = palette.secondary
    Green = palette.success
    Amber = palette.warning
    Grid = palette.primary.copy(alpha = 0.20f)
}

internal fun appBackdrop(scheme: AppColorScheme): Brush {
    val palette = scheme.palette
    val effects = visualEffectsFor(scheme)
    return when (effects.treatment) {
        AppVisualTreatment.Standard -> Brush.radialGradient(
            colors = listOf(
                palette.primary.copy(alpha = 0.20f),
                palette.background,
                palette.background,
            ),
            radius = 1100f,
            center = Offset(420f, 220f),
        )
        AppVisualTreatment.NeonGlass -> Brush.linearGradient(
            colors = listOf(
                palette.primary.copy(alpha = effects.backdropAccentAlpha),
                palette.background,
                palette.surfaceAlt.copy(alpha = .92f),
                palette.secondary.copy(alpha = effects.backdropSecondaryAlpha),
                palette.background,
            ),
            start = Offset.Zero,
            end = Offset(1150f, 1700f),
        )
        AppVisualTreatment.SpectralWireframe -> Brush.linearGradient(
            colors = listOf(
                palette.background,
                palette.primary.copy(alpha = effects.backdropAccentAlpha),
                palette.background,
                palette.secondary.copy(alpha = effects.backdropSecondaryAlpha),
                palette.background,
            ),
            start = Offset(120f, 0f),
            end = Offset(980f, 1500f),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AppAppearanceDialog(
    selected: AppColorScheme,
    onSelect: (AppColorScheme) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .border(1.dp, selected.palette.primary.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
            color = selected.palette.surface.copy(alpha = 0.98f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
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
