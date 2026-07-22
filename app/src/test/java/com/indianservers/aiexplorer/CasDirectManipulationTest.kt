package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test

class CasDirectManipulationTest {
    private val engine = CasDirectManipulationEngine()

    @Test fun transformationsPreviewBeforeCommitAndRemainUndoable() {
        var state = CasManipulationState("(x+1)^2")
        state = engine.select(state, engine.targets(state).first { it.text == "x+1" })
        state = engine.preview(state, CasTargetAction.Differentiate)
        assertEquals("(x+1)^2", state.expression)
        assertNotEquals(state.preview!!.before, state.preview!!.after)
        state = engine.commit(state)
        assertTrue(state.canUndo)
        assertEquals("(x+1)^2", engine.undo(state).expression)
        assertEquals(state.expression, engine.redo(engine.undo(state)).expression)
    }

    @Test fun cancellingPreviewDoesNotCreateHistory() {
        var state = CasManipulationState("x^2+1")
        state = engine.select(state, engine.targets(state).first { it.text == "x^2" })
        state = engine.cancel(engine.preview(state, CasTargetAction.Differentiate))
        assertEquals(1, state.history.size)
        assertNull(state.preview)
    }

    @Test fun parameterHandlesScrubWithoutFlatteningOriginalExpression() {
        val handles = engine.handles("a*x^2+b")
        assertEquals(listOf("a", "x", "b"), handles.map { it.variable })
        val scrub = engine.scrub("a*x^2+b", "x", 3.0)
        assertEquals("a*x^2+b", scrub.expression)
        assertTrue(scrub.substituted.contains("9*a") || scrub.substituted.contains("a*9"))
    }

    @Test fun rowPayloadCanReplaceASelectedSubexpressionOrAppend() {
        var state = CasManipulationState("x+1")
        state = engine.select(state, engine.targets(state).first { it.text == "x" })
        val replaced = engine.drop(state, CasExpressionTransfer("r1", "y^2"), replaceSelection = true)
        assertEquals("(y^2)+1", replaced.expression)
        val appended = engine.drop(CasManipulationState("x"), CasExpressionTransfer("r1", "y"), replaceSelection = false)
        assertEquals("x + (y)", appended.expression)
    }
}
