package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.ArVector2
import org.junit.Assert.*
import org.junit.Test

class ArHandGestureControllerTest {
    @Test fun portraitRotationUsesPhysicalPixelAspect() {
        val c = ArHandGestureController()
        val initial = listOf(hand("Left", .3f), hand("Right", .7f))
        c.update(ArHandFrame(100, initial), ArHandTool.Rotate, true, .5f)
        assertEquals(ArHandPhase.Begin, c.update(ArHandFrame(220, initial), ArHandTool.Rotate, true, .5f).phase)
        val changed = listOf(hand("Left", .4f, .45f), hand("Right", .6f, .55f))
        assertEquals(45f, c.update(ArHandFrame(250, changed), ArHandTool.Rotate, true, .5f).rotationDegrees, .001f)
        assertEquals(ArHandPhase.Cancel, c.update(ArHandFrame(280, changed), ArHandTool.Rotate, true, 2f).phase)
    }
    private fun hand(id: String = "Left", x: Float = .5f, y: Float = .5f, gap: Float = .02f) =
        ArTrackedHand(id, MutableList(21) { ArVector2(x, y) }.apply {
            this[0] = ArVector2(x, y + .1f); this[9] = ArVector2(x, y - .1f)
            this[4] = ArVector2(x - gap / 2, y); this[8] = ArVector2(x + gap / 2, y)
        })
    private fun start(c: ArHandGestureController, tool: ArHandTool = ArHandTool.Move, hands: List<ArTrackedHand> = listOf(hand())) {
        assertEquals(ArHandPhase.Idle, c.update(ArHandFrame(100, hands), tool, true).phase)
        assertEquals(ArHandPhase.Begin, c.update(ArHandFrame(220, hands), tool, true).phase)
    }
    @Test fun pinchNeedsStableHold() {
        val c = ArHandGestureController()
        assertEquals(ArHandPhase.Idle, c.update(ArHandFrame(100, listOf(hand())), ArHandTool.Move, true).phase)
        assertEquals(ArHandPhase.Idle, c.update(ArHandFrame(219, listOf(hand())), ArHandTool.Move, true).phase)
        assertEquals(ArHandPhase.Begin, c.update(ArHandFrame(220, listOf(hand())), ArHandTool.Move, true).phase)
    }
    @Test fun moveUsesCapturedBaseline() {
        val c = ArHandGestureController(); start(c)
        val a = c.update(ArHandFrame(250, listOf(hand(x = .65f, y = .4f))), ArHandTool.Move, true)
        assertEquals(ArHandPhase.Update, a.phase); assertEquals(.15f, a.pan.x, .0001f)
        assertEquals(-.1f, a.pan.y, .0001f); assertEquals(1f, a.scale, 0f)
    }
    @Test fun releaseCommitsOnce() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.End, c.update(ArHandFrame(250, emptyList()), ArHandTool.Move, true).phase)
        assertEquals(ArHandPhase.Idle, c.update(ArHandFrame(260, emptyList()), ArHandTool.Move, true).phase)
    }
    @Test fun pinchHysteresisAvoidsFlicker() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.Update, c.update(ArHandFrame(250, listOf(hand(gap = .08f))), ArHandTool.Move, true).phase)
        assertEquals(ArHandPhase.End, c.update(ArHandFrame(280, listOf(hand(gap = .12f))), ArHandTool.Move, true).phase)
    }
    @Test fun trackingLossCancels() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.Cancel, c.update(ArHandFrame(250, listOf(hand())), ArHandTool.Move, false).phase)
    }
    @Test fun staleFrameCancels() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.Cancel, c.update(ArHandFrame(571, listOf(hand())), ArHandTool.Move, true).phase)
    }
    @Test fun duplicateAndOldFramesDoNotTransform() {
        val c = ArHandGestureController(); start(c)
        for (t in listOf(220L, 200L)) assertEquals(ArHandPhase.Idle, c.update(ArHandFrame(t, listOf(hand(x = .9f))), ArHandTool.Move, true).phase)
    }
    @Test fun toolSwitchEndsPriorGesture() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.End, c.update(ArHandFrame(250, listOf(hand())), ArHandTool.Scale, true).phase)
    }
    @Test fun oneHandRotationOnlyRotates() {
        val c = ArHandGestureController(); start(c, ArHandTool.Rotate)
        val a = c.update(ArHandFrame(250, listOf(hand(x = .7f))), ArHandTool.Rotate, true)
        assertEquals(36f, a.rotationDegrees, .001f); assertEquals(0f, a.pan.x, 0f); assertEquals(1f, a.scale, 0f)
    }
    @Test fun twoHandsScaleIndependentlyOfOrdering() {
        val c = ArHandGestureController()
        start(c, ArHandTool.Scale, listOf(hand("Left", .4f), hand("Right", .6f)))
        val a = c.update(ArHandFrame(250, listOf(hand("Right", .7f), hand("Left", .3f))), ArHandTool.Scale, true)
        assertEquals(2f, a.scale, .001f); assertEquals(0f, a.rotationDegrees, 0f)
    }
    @Test fun twoHandsRotate() {
        val c = ArHandGestureController()
        start(c, ArHandTool.Rotate, listOf(hand("Left", .4f), hand("Right", .6f)))
        val a = c.update(ArHandFrame(250, listOf(hand("Left", .5f, .4f), hand("Right", .5f, .6f))), ArHandTool.Rotate, true)
        assertEquals(90f, a.rotationDegrees, .001f)
    }
    @Test fun scaleIsBounded() {
        val c = ArHandGestureController()
        start(c, ArHandTool.Scale, listOf(hand("Left", .48f), hand("Right", .52f)))
        assertEquals(4f, c.update(ArHandFrame(250, listOf(hand("Left", .1f), hand("Right", .9f))), ArHandTool.Scale, true).scale, 0f)
    }
    @Test fun malformedHandsIgnored() {
        val c = ArHandGestureController()
        assertEquals(ArHandPhase.Idle, c.update(ArHandFrame(100, listOf(ArTrackedHand("Left", emptyList()))), ArHandTool.Move, true).phase)
    }
    @Test fun ambiguousIdentityCancelsSelection() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.End, c.update(ArHandFrame(250, listOf(hand(), hand(x = .8f))), ArHandTool.Move, true).phase)
    }
    @Test fun leavingViewportEndsGesture() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.End, c.update(ArHandFrame(250, listOf(hand(x = 1.1f))), ArHandTool.Move, true).phase)
    }
    @Test fun resetAllowsNewSessionClock() {
        val c = ArHandGestureController(); start(c)
        assertEquals(ArHandPhase.Cancel, c.reset().phase); start(c)
    }
}
