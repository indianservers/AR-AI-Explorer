package com.indianservers.aiexplorer.ar3dgraph.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ARGestureAccessibilityPolicyTest {
    @Test fun touchExplorationCannotManipulateGraph() {
        assertFalse(ARGestureAccessibilityPolicy.shouldHandleViewportGesture(touchExplorationEnabled = true))
    }

    @Test fun ordinaryDirectTouchRemainsEnabled() {
        assertTrue(ARGestureAccessibilityPolicy.shouldHandleViewportGesture(touchExplorationEnabled = false))
    }
}
