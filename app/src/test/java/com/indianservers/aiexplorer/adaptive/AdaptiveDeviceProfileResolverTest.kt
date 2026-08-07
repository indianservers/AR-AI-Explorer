package com.indianservers.aiexplorer.adaptive

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveDeviceProfileResolverTest {
    @Test
    fun phonePortraitKeepsExistingMobileBreakpointsAndSpacing() {
        val profile = profile(width = 411, height = 891)

        assertEquals(AppFormFactor.Phone, profile.formFactor)
        assertEquals(AdaptiveWidthClass.Compact, profile.widthClass)
        assertEquals(AdaptiveOrientation.Portrait, profile.orientation)
        assertTrue(profile.legacyCompact)
        assertFalse(profile.legacyWide)
        assertEquals(8f, profile.contentPadding.value, 0f)
        assertEquals(0f, profile.overscanPadding.value, 0f)
        assertEquals(1, profile.preferredPaneCount)
    }

    @Test
    fun phoneLandscapeUsesSidePanelPolicyWithoutBecomingTablet() {
        val profile = profile(width = 891, height = 411)

        assertEquals(AppFormFactor.Phone, profile.formFactor)
        assertEquals(AdaptiveOrientation.Landscape, profile.orientation)
        assertTrue(profile.legacyWide)
        assertEquals(AdaptiveWorkspaceArrangement.CanvasWithSidePanel, profile.workspaceArrangement)
        assertEquals(AdaptivePanelPresentation.Side, profile.workspacePolicy.panelPresentation)
        assertEquals(68f, profile.workspacePolicy.topChromeClearance.value, 0f)
        assertTrue(profile.workspacePolicy.collapseSecondaryPanelsInitially)
    }

    @Test
    fun legacyPhoneThresholdsRemainExact() {
        assertTrue(profile(width = 519, height = 900).legacyCompact)
        assertFalse(profile(width = 520, height = 900).legacyCompact)
        assertFalse(profile(width = 759, height = 900).legacyWide)
        assertTrue(profile(width = 760, height = 900).legacyWide)
    }

    @Test
    fun tabletAndFoldableSignalsAreDistinct() {
        val tablet = profile(width = 800, height = 1280)
        val foldable = profile(width = 800, height = 1280, isFoldable = true)

        assertEquals(AppFormFactor.Tablet, tablet.formFactor)
        assertEquals(AppFormFactor.Foldable, foldable.formFactor)
        assertEquals(AdaptiveWidthClass.Medium, tablet.widthClass)
        assertEquals(2, tablet.preferredPaneCount)
    }

    @Test
    fun televisionForcesDirectionalInputOverscanAndThreePanes() {
        val profile = profile(
            width = 1920,
            height = 1080,
            uiModeType = Configuration.UI_MODE_TYPE_TELEVISION,
            inputMode = AdaptiveInputMode.Touch,
        )

        assertEquals(AppFormFactor.Television, profile.formFactor)
        assertEquals(AdaptiveInputMode.Directional, profile.inputMode)
        assertTrue(profile.legacyWide)
        assertEquals(24f, profile.overscanPadding.value, 0f)
        assertEquals(56f, profile.minimumTargetSize.value, 0f)
        assertEquals(3, profile.preferredPaneCount)
        assertEquals(AdaptiveNavigationMode.NavigationRail, profile.navigationPolicy.mode)
        assertEquals(AdaptiveOverlayEdge.Start, profile.navigationPolicy.overlayEdge)
        assertTrue(profile.navigationPolicy.showCommandLabels)
        assertEquals(4, profile.navigationPolicy.hubColumnCount)
        assertEquals(216f, profile.workspacePolicy.reservedNavigationWidth.value, 0f)
        assertEquals(8f, profile.workspacePolicy.topChromeClearance.value, 0f)
        assertEquals(360f, profile.workspacePolicy.sidePanelWidth.value, 0f)
        assertEquals(.72f, profile.workspacePolicy.minimumCanvasFraction, 0f)
        assertEquals(AdaptivePanelPresentation.Side, profile.workspacePolicy.panelPresentation)
        assertTrue(profile.workspacePolicy.collapseSecondaryPanelsInitially)
        assertTrue(profile.interactionPolicy.requestInitialNavigationFocus)
        assertTrue(profile.interactionPolicy.restoreFocusAfterOverlay)
        assertTrue(profile.interactionPolicy.remotePageScrolling)
        assertEquals(720f, profile.interactionPolicy.dialogMaximumWidth.value, 0f)
        assertEquals(.82f, profile.interactionPolicy.dialogMaximumHeightFraction, 0f)
        assertTrue(profile.interactionPolicy.showRemoteHints)
    }

    @Test
    fun desktopKeyboardProfileIsRecognized() {
        val profile = profile(
            width = 1440,
            height = 900,
            uiModeType = Configuration.UI_MODE_TYPE_DESK,
            inputMode = AdaptiveInputMode.KeyboardMouse,
        )

        assertEquals(AppFormFactor.Desktop, profile.formFactor)
        assertEquals(AdaptiveInputMode.KeyboardMouse, profile.inputMode)
        assertEquals(AdaptiveWidthClass.Expanded, profile.widthClass)
        assertEquals(AdaptiveNavigationMode.ExpandedTopBar, profile.navigationPolicy.mode)
        assertEquals(0f, profile.workspacePolicy.reservedNavigationWidth.value, 0f)
        assertEquals(AdaptivePanelPresentation.Side, profile.workspacePolicy.panelPresentation)
        assertTrue(profile.interactionPolicy.remotePageScrolling)
        assertFalse(profile.interactionPolicy.requestInitialNavigationFocus)
    }

    @Test
    fun portraitPhoneKeepsCompactCenteredNavigation() {
        val policy = profile(width = 411, height = 891).navigationPolicy

        assertEquals(AdaptiveNavigationMode.CompactTopBar, policy.mode)
        assertEquals(AdaptiveOverlayEdge.Center, policy.overlayEdge)
        assertFalse(policy.showCommandLabels)
        assertEquals(390f, policy.launcherWidth.value, 0f)
        val workspace = profile(width = 411, height = 891).workspacePolicy
        assertEquals(AdaptivePanelPresentation.Overlay, workspace.panelPresentation)
        assertEquals(68f, workspace.topChromeClearance.value, 0f)
        assertFalse(profile(width = 411, height = 891).interactionPolicy.remotePageScrolling)
        assertEquals(480f, profile(width = 411, height = 891).interactionPolicy.dialogMaximumWidth.value, 0f)
    }

    private fun profile(
        width: Int,
        height: Int,
        uiModeType: Int = Configuration.UI_MODE_TYPE_NORMAL,
        inputMode: AdaptiveInputMode = AdaptiveInputMode.Touch,
        isFoldable: Boolean = false,
    ): AdaptiveDeviceProfile = AdaptiveDeviceProfileResolver.resolve(
        AdaptiveDeviceSignals(
            widthDp = width,
            heightDp = height,
            density = 2f,
            uiModeType = uiModeType,
            inputMode = inputMode,
            isFoldable = isFoldable,
        ),
    )
}
