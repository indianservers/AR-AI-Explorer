package com.indianservers.aiexplorer.adaptive

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class AppFormFactor {
    Phone,
    Tablet,
    Foldable,
    Television,
    Desktop,
}

enum class AdaptiveWidthClass { Compact, Medium, Expanded }

enum class AdaptiveHeightClass { Compact, Medium, Expanded }

enum class AdaptiveOrientation { Portrait, Landscape }

enum class AdaptiveInputMode { Touch, Directional, KeyboardMouse }

enum class AdaptiveWorkspaceArrangement {
    CanvasFirst,
    CanvasWithSidePanel,
    ThreePane,
}

enum class AdaptiveNavigationMode {
    CompactTopBar,
    ExpandedTopBar,
    NavigationRail,
}

enum class AdaptiveOverlayEdge {
    Center,
    Start,
    End,
}

enum class AdaptivePanelPresentation {
    Overlay,
    Side,
    BottomSheet,
}

@Immutable
data class AdaptiveWorkspacePolicy(
    val reservedNavigationWidth: Dp,
    val topChromeClearance: Dp,
    val sidePanelWidth: Dp,
    val panelPresentation: AdaptivePanelPresentation,
    val minimumCanvasFraction: Float,
    val collapseSecondaryPanelsInitially: Boolean,
)

@Immutable
data class AdaptiveInteractionPolicy(
    val requestInitialNavigationFocus: Boolean,
    val restoreFocusAfterOverlay: Boolean,
    val remotePageScrolling: Boolean,
    val remotePageDistance: Dp,
    val dialogMaximumWidth: Dp,
    val dialogMaximumHeightFraction: Float,
    val showRemoteHints: Boolean,
)

@Immutable
data class AdaptiveNavigationPolicy(
    val mode: AdaptiveNavigationMode,
    val overlayEdge: AdaptiveOverlayEdge,
    val showCommandLabels: Boolean,
    val hubColumnCount: Int,
    val maximumContentWidth: Dp,
    val launcherWidth: Dp,
)

@Immutable
data class AdaptiveDeviceProfile(
    val formFactor: AppFormFactor,
    val widthClass: AdaptiveWidthClass,
    val heightClass: AdaptiveHeightClass,
    val orientation: AdaptiveOrientation,
    val inputMode: AdaptiveInputMode,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val legacyCompact: Boolean,
    val legacyWide: Boolean,
    val contentPadding: Dp,
    val overscanPadding: Dp,
    val minimumTargetSize: Dp,
    val preferredPaneCount: Int,
) {
    val isTelevision: Boolean get() = formFactor == AppFormFactor.Television
    val isDirectionalInput: Boolean get() = inputMode == AdaptiveInputMode.Directional
    val isLandscape: Boolean get() = orientation == AdaptiveOrientation.Landscape

    val workspaceArrangement: AdaptiveWorkspaceArrangement
        get() = when {
            preferredPaneCount >= 3 -> AdaptiveWorkspaceArrangement.ThreePane
            preferredPaneCount == 2 -> AdaptiveWorkspaceArrangement.CanvasWithSidePanel
            else -> AdaptiveWorkspaceArrangement.CanvasFirst
        }

    val navigationPolicy: AdaptiveNavigationPolicy
        get() = AdaptiveNavigationPolicyResolver.resolve(this)

    val workspacePolicy: AdaptiveWorkspacePolicy
        get() = AdaptiveWorkspacePolicyResolver.resolve(this)

    val interactionPolicy: AdaptiveInteractionPolicy
        get() = AdaptiveInteractionPolicyResolver.resolve(this)
}

@Immutable
data class AdaptiveDeviceSignals(
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val uiModeType: Int,
    val inputMode: AdaptiveInputMode,
    val isFoldable: Boolean = false,
)

object AdaptiveDeviceProfileResolver {
    fun resolve(signals: AdaptiveDeviceSignals): AdaptiveDeviceProfile {
        val shortestSide = minOf(signals.widthDp, signals.heightDp)
        val orientation = if (signals.widthDp >= signals.heightDp) {
            AdaptiveOrientation.Landscape
        } else {
            AdaptiveOrientation.Portrait
        }
        val formFactor = when {
            signals.uiModeType == Configuration.UI_MODE_TYPE_TELEVISION -> AppFormFactor.Television
            signals.isFoldable -> AppFormFactor.Foldable
            signals.uiModeType == Configuration.UI_MODE_TYPE_DESK -> AppFormFactor.Desktop
            shortestSide >= 600 -> AppFormFactor.Tablet
            else -> AppFormFactor.Phone
        }
        val widthClass = when {
            signals.widthDp < 600 -> AdaptiveWidthClass.Compact
            signals.widthDp < 840 -> AdaptiveWidthClass.Medium
            else -> AdaptiveWidthClass.Expanded
        }
        val heightClass = when {
            signals.heightDp < 480 -> AdaptiveHeightClass.Compact
            signals.heightDp < 900 -> AdaptiveHeightClass.Medium
            else -> AdaptiveHeightClass.Expanded
        }
        val paneCount = when {
            formFactor == AppFormFactor.Television -> 3
            formFactor == AppFormFactor.Phone && orientation == AdaptiveOrientation.Landscape -> 2
            widthClass == AdaptiveWidthClass.Expanded -> 3
            widthClass == AdaptiveWidthClass.Medium || orientation == AdaptiveOrientation.Landscape -> 2
            else -> 1
        }

        return AdaptiveDeviceProfile(
            formFactor = formFactor,
            widthClass = widthClass,
            heightClass = heightClass,
            orientation = orientation,
            inputMode = if (formFactor == AppFormFactor.Television) {
                AdaptiveInputMode.Directional
            } else {
                signals.inputMode
            },
            widthDp = signals.widthDp,
            heightDp = signals.heightDp,
            density = signals.density,
            legacyCompact = signals.widthDp < 520,
            legacyWide = signals.widthDp >= 760,
            contentPadding = 8.dp,
            overscanPadding = if (formFactor == AppFormFactor.Television) 24.dp else 0.dp,
            minimumTargetSize = if (formFactor == AppFormFactor.Television) 56.dp else 48.dp,
            preferredPaneCount = paneCount,
        )
    }
}

object AdaptiveNavigationPolicyResolver {
    fun resolve(profile: AdaptiveDeviceProfile): AdaptiveNavigationPolicy = when {
        profile.isTelevision -> AdaptiveNavigationPolicy(
            mode = AdaptiveNavigationMode.NavigationRail,
            overlayEdge = AdaptiveOverlayEdge.Start,
            showCommandLabels = true,
            hubColumnCount = 4,
            maximumContentWidth = 1440.dp,
            launcherWidth = 520.dp,
        )

        profile.formFactor == AppFormFactor.Phone && profile.isLandscape -> AdaptiveNavigationPolicy(
            mode = AdaptiveNavigationMode.ExpandedTopBar,
            overlayEdge = AdaptiveOverlayEdge.Start,
            showCommandLabels = false,
            hubColumnCount = 3,
            maximumContentWidth = 960.dp,
            launcherWidth = 420.dp,
        )

        profile.widthClass == AdaptiveWidthClass.Expanded -> AdaptiveNavigationPolicy(
            mode = AdaptiveNavigationMode.ExpandedTopBar,
            overlayEdge = AdaptiveOverlayEdge.Start,
            showCommandLabels = true,
            hubColumnCount = 5,
            maximumContentWidth = 1280.dp,
            launcherWidth = 460.dp,
        )

        else -> AdaptiveNavigationPolicy(
            mode = AdaptiveNavigationMode.CompactTopBar,
            overlayEdge = AdaptiveOverlayEdge.Center,
            showCommandLabels = false,
            hubColumnCount = 2,
            maximumContentWidth = 720.dp,
            launcherWidth = 390.dp,
        )
    }
}

object AdaptiveWorkspacePolicyResolver {
    fun resolve(profile: AdaptiveDeviceProfile): AdaptiveWorkspacePolicy = when {
        profile.isTelevision -> AdaptiveWorkspacePolicy(
            reservedNavigationWidth = 216.dp,
            topChromeClearance = 8.dp,
            sidePanelWidth = 360.dp,
            panelPresentation = AdaptivePanelPresentation.Side,
            minimumCanvasFraction = .72f,
            collapseSecondaryPanelsInitially = true,
        )

        profile.formFactor == AppFormFactor.Phone && profile.isLandscape -> AdaptiveWorkspacePolicy(
            reservedNavigationWidth = 0.dp,
            topChromeClearance = 68.dp,
            sidePanelWidth = 300.dp,
            panelPresentation = AdaptivePanelPresentation.Side,
            minimumCanvasFraction = .62f,
            collapseSecondaryPanelsInitially = true,
        )

        profile.widthClass == AdaptiveWidthClass.Expanded -> AdaptiveWorkspacePolicy(
            reservedNavigationWidth = 0.dp,
            topChromeClearance = 78.dp,
            sidePanelWidth = 320.dp,
            panelPresentation = AdaptivePanelPresentation.Side,
            minimumCanvasFraction = .66f,
            collapseSecondaryPanelsInitially = false,
        )

        else -> AdaptiveWorkspacePolicy(
            reservedNavigationWidth = 0.dp,
            topChromeClearance = 68.dp,
            sidePanelWidth = 280.dp,
            panelPresentation = AdaptivePanelPresentation.Overlay,
            minimumCanvasFraction = .58f,
            collapseSecondaryPanelsInitially = true,
        )
    }
}

object AdaptiveInteractionPolicyResolver {
    fun resolve(profile: AdaptiveDeviceProfile): AdaptiveInteractionPolicy = when {
        profile.isTelevision -> AdaptiveInteractionPolicy(
            requestInitialNavigationFocus = true,
            restoreFocusAfterOverlay = true,
            remotePageScrolling = true,
            remotePageDistance = 420.dp,
            dialogMaximumWidth = 720.dp,
            dialogMaximumHeightFraction = .82f,
            showRemoteHints = true,
        )

        profile.inputMode == AdaptiveInputMode.KeyboardMouse -> AdaptiveInteractionPolicy(
            requestInitialNavigationFocus = false,
            restoreFocusAfterOverlay = true,
            remotePageScrolling = true,
            remotePageDistance = 520.dp,
            dialogMaximumWidth = 640.dp,
            dialogMaximumHeightFraction = .88f,
            showRemoteHints = false,
        )

        else -> AdaptiveInteractionPolicy(
            requestInitialNavigationFocus = false,
            restoreFocusAfterOverlay = false,
            remotePageScrolling = false,
            remotePageDistance = 360.dp,
            dialogMaximumWidth = 480.dp,
            dialogMaximumHeightFraction = .9f,
            showRemoteHints = false,
        )
    }
}

private val DefaultAdaptiveDeviceProfile = AdaptiveDeviceProfileResolver.resolve(
    AdaptiveDeviceSignals(
        widthDp = 411,
        heightDp = 891,
        density = 1f,
        uiModeType = Configuration.UI_MODE_TYPE_NORMAL,
        inputMode = AdaptiveInputMode.Touch,
    ),
)

val LocalAdaptiveDeviceProfile = compositionLocalOf { DefaultAdaptiveDeviceProfile }

@Composable
fun rememberAdaptiveDeviceProfile(isFoldable: Boolean = false): AdaptiveDeviceProfile {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val composeInputMode = LocalInputModeManager.current.inputMode
    val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    val inputMode = when {
        uiModeType == Configuration.UI_MODE_TYPE_TELEVISION -> AdaptiveInputMode.Directional
        composeInputMode == InputMode.Keyboard -> AdaptiveInputMode.KeyboardMouse
        else -> AdaptiveInputMode.Touch
    }

    return remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density,
        uiModeType,
        inputMode,
        isFoldable,
    ) {
        AdaptiveDeviceProfileResolver.resolve(
            AdaptiveDeviceSignals(
                widthDp = configuration.screenWidthDp,
                heightDp = configuration.screenHeightDp,
                density = density,
                uiModeType = uiModeType,
                inputMode = inputMode,
                isFoldable = isFoldable,
            ),
        )
    }
}

@Composable
fun AdaptiveAppScaffold(
    profile: AdaptiveDeviceProfile,
    modifier: Modifier = Modifier,
    backdrop: Brush? = null,
    content: @Composable BoxScope.(AdaptiveDeviceProfile) -> Unit,
) {
    CompositionLocalProvider(LocalAdaptiveDeviceProfile provides profile) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.background(backdrop) else Modifier)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(profile.contentPadding)
                .padding(profile.overscanPadding),
        ) {
            content(profile)
        }
    }
}

@Composable
fun AdaptiveWorkspaceScaffold(
    modifier: Modifier = Modifier,
    profile: AdaptiveDeviceProfile = LocalAdaptiveDeviceProfile.current,
    primaryPanel: (@Composable () -> Unit)? = null,
    secondaryPanel: (@Composable () -> Unit)? = null,
    canvas: @Composable () -> Unit,
) {
    when (profile.workspaceArrangement) {
        AdaptiveWorkspaceArrangement.CanvasFirst -> Box(modifier.fillMaxSize()) {
            canvas()
            primaryPanel?.invoke()
            secondaryPanel?.invoke()
        }

        AdaptiveWorkspaceArrangement.CanvasWithSidePanel -> Row(modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize()) { canvas() }
            primaryPanel?.invoke()
            secondaryPanel?.invoke()
        }

        AdaptiveWorkspaceArrangement.ThreePane -> Row(modifier.fillMaxSize()) {
            primaryPanel?.invoke()
            Box(Modifier.weight(1f).fillMaxSize()) { canvas() }
            secondaryPanel?.invoke()
        }
    }
}

@Composable
fun AdaptivePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, content = content)
}

@Composable
fun TvFocusContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .then(if (LocalAdaptiveDeviceProfile.current.isDirectionalInput) Modifier.focusGroup() else Modifier)
            .then(
            if (LocalAdaptiveDeviceProfile.current.isDirectionalInput) {
                Modifier.tvFocusTarget()
            } else {
                Modifier
            },
        ),
        content = content,
    )
}

@Composable
fun OrientationAwareDock(
    modifier: Modifier = Modifier,
    profile: AdaptiveDeviceProfile = LocalAdaptiveDeviceProfile.current,
    content: @Composable () -> Unit,
) {
    if (profile.isLandscape || profile.isTelevision) {
        Row(modifier) { content() }
    } else {
        Column(modifier) { content() }
    }
}

@Composable
fun CanvasViewport(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().clipToBounds(), content = content)
}

@Stable
fun Modifier.tvFocusTarget(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    focusColor: Color = Color(0xFF42E8FF),
    onActivate: (() -> Unit)? = null,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(2.dp, focusColor, shape) else Modifier)
        .onPreviewKeyEvent { event ->
            val activates = event.type == KeyEventType.KeyUp &&
                (event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter)
            if (enabled && activates && onActivate != null) {
                onActivate()
                true
            } else {
                false
            }
        }
        .focusable(enabled)
}

@Stable
fun Modifier.adaptiveFocusRing(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    focusColor: Color = Color(0xFF42E8FF),
): Modifier = composed {
    val profile = LocalAdaptiveDeviceProfile.current
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .then(
            if (enabled && focused && profile.inputMode != AdaptiveInputMode.Touch) {
                Modifier.border(3.dp, focusColor, shape)
            } else {
                Modifier
            },
        )
}

@Stable
fun Modifier.adaptiveFocusGroup(): Modifier = composed {
    if (LocalAdaptiveDeviceProfile.current.isDirectionalInput) focusGroup() else this
}

@Stable
fun Modifier.tvRemoteScrollable(
    state: ScrollState,
    enabled: Boolean = true,
): Modifier = composed {
    val profile = LocalAdaptiveDeviceProfile.current
    val policy = profile.interactionPolicy
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pageDistancePx = with(density) { policy.remotePageDistance.roundToPx() }
    if (!enabled || !policy.remotePageScrolling) {
        this
    } else {
        onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val target = when (event.key) {
                Key.PageDown -> (state.value + pageDistancePx).coerceAtMost(state.maxValue)
                Key.PageUp -> (state.value - pageDistancePx).coerceAtLeast(0)
                else -> return@onPreviewKeyEvent false
            }
            scope.launch { state.animateScrollTo(target) }
            true
        }
    }
}

@Stable
fun Modifier.adaptiveDialogBounds(): Modifier = composed {
    val policy = LocalAdaptiveDeviceProfile.current.interactionPolicy
    fillMaxWidth()
        .fillMaxHeight(policy.dialogMaximumHeightFraction)
        .widthIn(max = policy.dialogMaximumWidth)
}

@Stable
fun Modifier.adaptiveDialogWidth(): Modifier = composed {
    val policy = LocalAdaptiveDeviceProfile.current.interactionPolicy
    fillMaxWidth().widthIn(max = policy.dialogMaximumWidth)
}
