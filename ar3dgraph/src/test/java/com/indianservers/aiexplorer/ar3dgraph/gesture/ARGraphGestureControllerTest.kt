package com.indianservers.aiexplorer.ar3dgraph.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ARGraphGestureControllerTest {
    private val available = ARGestureAvailability(graphReady = true, graphPlaced = true, trackingUsable = true)
    private fun pointer(id: Int = 0, x: Float = 10f, y: Float = 10f) = ARGesturePointer(id, x, y)
    private fun event(action: ARGestureAction, vararg pointers: ARGesturePointer, time: Long = 0L, inside: Boolean = true) =
        ARGestureEvent(action, pointers.toList(), time, inside)
    private fun down(controller: ARGraphGestureController) =
        controller.onEvent(event(ARGestureAction.Down, pointer(), time = 10L), available)

    @Test fun shortTapRepositions() {
        val controller = ARGraphGestureController(8f); down(controller)
        assertTrue(controller.onEvent(event(ARGestureAction.Up, pointer(), time = 100L), available) is ARGestureOutcome.RepositionTap)
    }

    @Test fun jitterBelowSlopRemainsTap() {
        val controller = ARGraphGestureController(8f); down(controller)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 14f, y = 13f), time = 40L), available)
        assertTrue(controller.onEvent(event(ARGestureAction.Up, pointer(x = 14f, y = 13f), time = 80L), available) is ARGestureOutcome.RepositionTap)
    }

    @Test fun longPressDoesNotReposition() {
        val controller = ARGraphGestureController(8f); down(controller)
        assertEquals(ARGestureOutcome.None, controller.onEvent(event(ARGestureAction.Up, pointer(), time = 511L), available))
    }

    @Test fun dragRotatesWithoutTap() {
        val controller = ARGraphGestureController(8f); down(controller)
        val change = controller.onEvent(event(ARGestureAction.Move, pointer(x = 30f)), available) as ARGestureOutcome.TransformChanged
        assertEquals(5f, change.transform.yawDegrees, 0.001f)
        assertEquals(ARGestureOutcome.None, controller.onEvent(event(ARGestureAction.Up, pointer(x = 30f)), available))
    }

    @Test fun verticalRotationIsClamped() {
        val controller = ARGraphGestureController(1f); down(controller)
        val change = controller.onEvent(event(ARGestureAction.Move, pointer(y = 1000f)), available) as ARGestureOutcome.TransformChanged
        assertEquals(80f, change.transform.pitchDegrees, 0f)
    }

    @Test fun yawRemainsBoundedAcrossManyDrags() {
        val controller = ARGraphGestureController(1f)
        repeat(100) {
            down(controller); controller.onEvent(event(ARGestureAction.Move, pointer(x = 1010f)), available)
            controller.onEvent(event(ARGestureAction.Up, pointer(x = 1010f)), available)
        }
        assertTrue(controller.transform.yawDegrees in -180f..180f)
    }

    @Test fun pinchScalesUniformly() {
        val controller = ARGraphGestureController(8f); down(controller)
        controller.onEvent(event(ARGestureAction.PointerDown, pointer(), pointer(1, 20f, 10f)), available)
        val change = controller.onEvent(event(ARGestureAction.Move, pointer(), pointer(1, 30f, 10f)), available) as ARGestureOutcome.TransformChanged
        assertEquals(2f, change.transform.uniformScale, 0.001f)
    }

    @Test fun pinchScaleClampsAtBothLimits() {
        val controller = ARGraphGestureController(8f); down(controller)
        controller.onEvent(event(ARGestureAction.PointerDown, pointer(), pointer(1, 20f, 10f)), available)
        var change = controller.onEvent(event(ARGestureAction.Move, pointer(), pointer(1, 1000f, 10f)), available) as ARGestureOutcome.TransformChanged
        assertEquals(3f, change.transform.uniformScale, 0f)
        controller.onEvent(event(ARGestureAction.PointerUp, pointer()), available); down(controller)
        controller.onEvent(event(ARGestureAction.PointerDown, pointer(), pointer(1, 1010f, 10f)), available)
        change = controller.onEvent(event(ARGestureAction.Move, pointer(), pointer(1, 11f, 10f)), available) as ARGestureOutcome.TransformChanged
        assertEquals(0.35f, change.transform.uniformScale, 0f)
    }

    @Test fun secondFingerPromotesDragToPinch() {
        val controller = ARGraphGestureController(1f); down(controller)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 20f)), available)
        controller.onEvent(event(ARGestureAction.PointerDown, pointer(x = 20f), pointer(1, 30f, 10f)), available)
        assertEquals(ARGestureMode.Scaling, controller.mode)
    }

    @Test fun pointerUpNeverBecomesTap() {
        val controller = ARGraphGestureController(8f); down(controller)
        controller.onEvent(event(ARGestureAction.PointerDown, pointer(), pointer(1, 20f, 10f)), available)
        controller.onEvent(event(ARGestureAction.PointerUp, pointer()), available)
        assertEquals(ARGestureOutcome.None, controller.onEvent(event(ARGestureAction.Up, pointer()), available))
    }

    @Test fun thirdPointerCancels() {
        val controller = ARGraphGestureController(8f); down(controller)
        controller.onEvent(event(ARGestureAction.PointerDown, pointer(), pointer(1), pointer(2)), available)
        assertEquals(ARGestureMode.Cancelled, controller.mode)
    }

    @Test fun leavingViewportCancels() {
        val controller = ARGraphGestureController(8f); down(controller)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = -1f), inside = false), available)
        assertEquals(ARGestureMode.Cancelled, controller.mode)
    }

    @Test fun trackingLossCancelsWithoutMutation() {
        val controller = ARGraphGestureController(1f); down(controller)
        val paused = available.copy(trackingUsable = false)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 50f)), paused)
        assertEquals(ARGraphTransformState(), controller.transform)
    }

    @Test fun graphMustBePlacedToRotateButTapStillPlacesReadyGraph() {
        val controller = ARGraphGestureController(1f)
        val ready = available.copy(graphPlaced = false)
        controller.onEvent(event(ARGestureAction.Down, pointer(), time = 10L), ready)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 50f)), ready)
        assertEquals(ARGraphTransformState(), controller.transform)
        controller.onEvent(event(ARGestureAction.Down, pointer(), time = 100L), ready)
        assertTrue(controller.onEvent(event(ARGestureAction.Up, pointer(), time = 150L), ready) is ARGestureOutcome.RepositionTap)
    }

    @Test fun resetViewRestoresIdentity() {
        val controller = ARGraphGestureController(1f); down(controller)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 50f)), available)
        assertEquals(ARGraphTransformState(), controller.resetView())
    }

    @Test fun clearGraphRestoresIdentityAndIdle() {
        val controller = ARGraphGestureController(1f); down(controller)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 50f)), available)
        controller.clearGraph()
        assertEquals(ARGraphTransformState(), controller.transform)
        assertEquals(ARGestureMode.Idle, controller.mode)
    }

    @Test fun quaternionStaysNormalizedAfterExtremeInput() {
        val controller = ARGraphGestureController(1f); down(controller)
        controller.onEvent(event(ARGestureAction.Move, pointer(x = 100000f, y = 100000f)), available)
        assertEquals(1f, controller.transform.rotation.magnitudeSquared, 0.0001f)
    }

    @Test fun disposeRejectsLateEvents() {
        val controller = ARGraphGestureController(1f); controller.dispose()
        assertEquals(ARGestureOutcome.None, controller.onEvent(event(ARGestureAction.Down, pointer()), available))
        assertEquals(ARGestureMode.Cancelled, controller.mode)
    }

    @Test fun seventyFiveCertifiedDragsRotateWithoutEverRepositioning() {
        val deltas = buildList {
            repeat(25) { add((20f + it) to 0f) }
            repeat(25) { add(0f to (20f + it)) }
            repeat(25) { add((20f + it) to (20f + it)) }
        }
        deltas.forEachIndexed { index, (dx, dy) ->
            val controller = ARGraphGestureController(8f)
            down(controller)
            val changed = controller.onEvent(
                event(ARGestureAction.Move, pointer(x = 10f + dx, y = 10f + dy), time = 20L + index),
                available,
            )
            assertTrue("drag $index must change the transform", changed is ARGestureOutcome.TransformChanged)
            val up = controller.onEvent(
                event(ARGestureAction.Up, pointer(x = 10f + dx, y = 10f + dy), time = 100L + index),
                available,
            )
            assertEquals("drag $index must not reposition", ARGestureOutcome.None, up)
            assertTrue(controller.transform.yawDegrees in -180f..180f)
            assertTrue(controller.transform.pitchDegrees in -80f..80f)
            assertEquals(1f, controller.transform.rotation.magnitudeSquared, 0.0001f)
        }
    }

    @Test fun fiftyCertifiedPinchesScaleWithinLimitsWithoutEverRepositioning() {
        repeat(50) { index ->
            val controller = ARGraphGestureController(8f)
            down(controller)
            controller.onEvent(
                event(ARGestureAction.PointerDown, pointer(), pointer(1, 30f, 10f), time = 20L),
                available,
            )
            val targetX = if (index < 25) 15f else 50f
            val changed = controller.onEvent(
                event(ARGestureAction.Move, pointer(), pointer(1, targetX, 10f), time = 40L),
                available,
            )
            assertTrue("pinch $index must change the transform", changed is ARGestureOutcome.TransformChanged)
            assertTrue(controller.transform.uniformScale in 0.35f..3f)
            controller.onEvent(event(ARGestureAction.PointerUp, pointer(), time = 60L), available)
            assertEquals(
                "pinch $index must not reposition",
                ARGestureOutcome.None,
                controller.onEvent(event(ARGestureAction.Up, pointer(), time = 80L), available),
            )
        }
    }
}
