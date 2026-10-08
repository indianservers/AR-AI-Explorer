package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.ArVector2
import org.junit.Assert.*
import org.junit.Test

class ArGestureFoundationTest {
    private fun hand(confidence: Float = 1f, x: Float = .5f) = ArTrackedHand("Left",MutableList(21) { ArVector2(x,.5f) }.apply { this[0]=ArVector2(x,.7f); this[9]=ArVector2(x,.3f); this[4]=ArVector2(x-.01f,.5f); this[8]=ArVector2(x+.01f,.5f) },confidence)
    @Test fun lowConfidenceCannotStartAGrab() {
        val c = ArHandGestureController()
        c.update(ArHandFrame(100,listOf(hand(.4f))),ArHandTool.Move,true)
        assertEquals(ArHandPhase.Idle,c.update(ArHandFrame(300,listOf(hand(.4f))),ArHandTool.Move,true).phase)
    }
    @Test fun hoverNeedsDwellAndChangingTargetsClearsHoverImmediately() {
        val hover = ArStableHover()
        assertNull(hover.update("a",100)); assertNull(hover.update("a",279)); assertEquals("a",hover.update("a",280))
        assertNull(hover.update("b",290)); assertNull(hover.update(null,500))
    }
    @Test fun filteringReducesRawMovementAndPreservesBaseline() {
        val c = ArHandGestureController(.4f,.003f)
        c.update(ArHandFrame(100,listOf(hand())),ArHandTool.Move,true)
        assertEquals(ArHandPhase.Begin,c.update(ArHandFrame(220,listOf(hand())),ArHandTool.Move,true).phase)
        val update = c.update(ArHandFrame(250,listOf(hand(x=.7f))),ArHandTool.Move,true)
        assertEquals(.08f,update.pan.x,.0001f)
    }
    @Test fun releaseIsAnEventAndConfidenceLossStopsGesture() {
        val c = ArGestureRecognizer()
        assertEquals(ArRecognizedGesture.PINCH_START,c.update(ArHandFrame(100,listOf(hand())),ArHandTool.Move).gesture)
        assertEquals(ArRecognizedGesture.PINCH_RELEASE,c.update(ArHandFrame(140,emptyList()),ArHandTool.Move).gesture)
        assertEquals(ArRecognizedGesture.NONE,c.update(ArHandFrame(180,emptyList()),ArHandTool.Move).gesture)
    }
    @Test fun handednessDuplicatesAndDetectorReorderingRetainDistinctIds() {
        val tracker = ArHandIdentityTracker()
        val first = tracker.assign(listOf(hand(x=.2f),hand(x=.8f)),100)
        assertNotEquals(first[0].id,first[1].id)
        val second = tracker.assign(listOf(hand(x=.81f),hand(x=.21f)),130)
        assertEquals(first[1].id,second[0].id)
        assertEquals(first[0].id,second[1].id)
    }
}
