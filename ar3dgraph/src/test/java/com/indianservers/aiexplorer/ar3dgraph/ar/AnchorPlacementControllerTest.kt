package com.indianservers.aiexplorer.ar3dgraph.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorPlacementControllerTest {
    private class FakeAnchor : PlacementAnchor {
        var detached = false
        override fun detach() { detached = true }
    }

    private val pose = WorldPlacementPose(
        WorldVector3(0f, 0f, -1.5f),
        WorldQuaternion(0f, 0f, 0f, 1f),
    )

    @Test fun firstPlacementAndOneHundredRepositionsKeepOneAnchor() {
        val anchors = mutableListOf<FakeAnchor>()
        val controller = AnchorPlacementController { FakeAnchor().also(anchors::add) }
        assertEquals(AnchorPlacementResult.Placed, controller.place(pose, tracking = true))
        repeat(100) {
            assertEquals(AnchorPlacementResult.Replaced, controller.place(pose, tracking = true))
            assertEquals(1, controller.activeAnchorCount)
        }
        assertEquals(101, anchors.size)
        assertTrue(anchors.dropLast(1).all(FakeAnchor::detached))
        assertFalse(anchors.last().detached)
    }

    @Test fun pausedTrackingCreatesNoAnchor() {
        var calls = 0
        val controller = AnchorPlacementController { calls++; FakeAnchor() }
        assertEquals(AnchorPlacementResult.TrackingPaused, controller.place(pose, tracking = false))
        assertEquals(0, calls)
        assertEquals(0, controller.activeAnchorCount)
    }

    @Test fun failedReplacementPreservesExistingAnchor() {
        val first = FakeAnchor()
        var calls = 0
        val controller = AnchorPlacementController {
            if (calls++ == 0) first else error("replacement failed")
        }
        controller.place(pose, tracking = true)
        assertTrue(controller.place(pose, tracking = true) is AnchorPlacementResult.Failed)
        assertEquals(1, controller.activeAnchorCount)
        assertFalse(first.detached)
    }

    @Test fun failedGraphAttachmentDetachesReplacementAndPreservesExistingAnchor() {
        val anchors = mutableListOf<FakeAnchor>()
        var attachments = 0
        val controller = AnchorPlacementController(
            factory = { FakeAnchor().also(anchors::add) },
            attach = {
                attachments++
                if (attachments == 2) error("graph attachment failed")
            },
        )
        assertEquals(AnchorPlacementResult.Placed, controller.place(pose, tracking = true))

        val result = controller.place(pose, tracking = true)

        assertTrue(result is AnchorPlacementResult.Failed)
        assertEquals(1, controller.activeAnchorCount)
        assertFalse(anchors.first().detached)
        assertTrue(anchors.last().detached)
    }

    @Test fun resetClearDisposeAndFiveReopenCyclesDetachEverything() {
        val anchors = mutableListOf<FakeAnchor>()
        repeat(5) {
            val controller = AnchorPlacementController { FakeAnchor().also(anchors::add) }
            controller.place(pose, tracking = true)
            controller.reset()
            assertEquals(0, controller.activeAnchorCount)
            controller.place(pose, tracking = true)
            controller.clear()
            controller.close()
        }
        assertTrue(anchors.all(FakeAnchor::detached))
    }

    @Test fun twentyFiveResetAndTwentyFiveClearReplacementCyclesNeverRetainAnAnchor() {
        val anchors = mutableListOf<FakeAnchor>()
        val controller = AnchorPlacementController { FakeAnchor().also(anchors::add) }
        repeat(25) {
            assertEquals(AnchorPlacementResult.Placed, controller.place(pose, tracking = true))
            controller.reset()
            assertEquals(0, controller.activeAnchorCount)
        }
        repeat(25) {
            assertEquals(AnchorPlacementResult.Placed, controller.place(pose, tracking = true))
            controller.clear()
            assertEquals(0, controller.activeAnchorCount)
        }
        assertEquals(50, anchors.size)
        assertTrue(anchors.all(FakeAnchor::detached))
    }
}
