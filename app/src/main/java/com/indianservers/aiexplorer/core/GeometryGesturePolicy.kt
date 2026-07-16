package com.indianservers.aiexplorer.core

enum class GeometryGestureTarget { JunctionHandle, ShapeBody, EmptyCanvas }
enum class GeometryGestureAction { ResizeFromHandle, MoveShape, PanViewport, ZoomViewport }

/** Keeps object manipulation and viewport manipulation mutually exclusive. */
object GeometryGesturePolicy {
    fun action(target: GeometryGestureTarget, pointerCount: Int): GeometryGestureAction {
        require(pointerCount >= 1)
        return when (target) {
            GeometryGestureTarget.JunctionHandle -> GeometryGestureAction.ResizeFromHandle
            GeometryGestureTarget.ShapeBody -> GeometryGestureAction.MoveShape
            GeometryGestureTarget.EmptyCanvas -> if (pointerCount >= 2) GeometryGestureAction.ZoomViewport else GeometryGestureAction.PanViewport
        }
    }

    fun allowsViewportTransform(target: GeometryGestureTarget): Boolean = target == GeometryGestureTarget.EmptyCanvas
}
