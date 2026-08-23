package com.indianservers.aiexplorer.ar3dgraph.gesture

/** Touch exploration owns viewport input so accessibility navigation can never manipulate AR. */
object ARGestureAccessibilityPolicy {
    fun shouldHandleViewportGesture(touchExplorationEnabled: Boolean): Boolean = !touchExplorationEnabled
}
