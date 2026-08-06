package com.indianservers.aiexplorer.features.numbertheory.visualproofs

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation.NumberTheoryVisualProofEngine
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.data.NumberTheoryVisualProofCatalog
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofAction
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberTheoryProofStateMachineTest {
    private val engine = NumberTheoryVisualProofEngine()

    @Test
    fun `every completed proof has valid evidence at minimum initial and maximum values`() {
        NumberTheoryVisualProofCatalog.completedTopics.forEach { topic ->
            val valueSets = listOf(
                topic.parameters.associate { it.key to it.minimum },
                topic.parameters.associate { it.key to it.initial },
                topic.parameters.associate { it.key to it.maximum },
            )
            valueSets.forEach { values ->
                val evidence = engine.evidence(topic.id, values)
                assertTrue("${topic.id} failed for $values", evidence.holds)
                assertTrue(evidence.accessibilityDescription.isNotBlank())
            }
        }
    }

    @Test
    fun `navigation completes and resets a proof`() {
        var state = engine.start("natural-sum") as NumberTheoryProofState.Ready
        repeat(state.topic.steps.size + 2) {
            state = engine.reduce(state, NumberTheoryProofAction.Next) as NumberTheoryProofState.Ready
        }
        assertEquals(state.topic.steps.lastIndex, state.stepIndex)
        assertTrue(state.completed)
        state = engine.reduce(state, NumberTheoryProofAction.Reset) as NumberTheoryProofState.Ready
        assertEquals(0, state.stepIndex)
        assertFalse(state.completed)
        assertFalse(state.formulaRevealed)
    }

    @Test
    fun `parameter changes clamp and recalculate evidence`() {
        var state = engine.start("modular-clock") as NumberTheoryProofState.Ready
        state = engine.reduce(state, NumberTheoryProofAction.UpdateParameter("modulus", 999)) as NumberTheoryProofState.Ready
        assertEquals(16, state.parameters.getValue("modulus"))
        assertTrue(state.evidence.holds)
        state = engine.reduce(state, NumberTheoryProofAction.UpdateParameter("value", -999)) as NumberTheoryProofState.Ready
        assertEquals(-30, state.parameters.getValue("value"))
        assertTrue(state.evidence.holds)
    }

    @Test
    fun `formula reveal and autoplay are explicit state transitions`() {
        var state = engine.start("odd-sum") as NumberTheoryProofState.Ready
        state = engine.reduce(state, NumberTheoryProofAction.RevealFormula) as NumberTheoryProofState.Ready
        assertTrue(state.formulaRevealed)
        state = engine.reduce(state, NumberTheoryProofAction.TogglePlaying) as NumberTheoryProofState.Ready
        assertTrue(state.playing)
        state = engine.reduce(state, NumberTheoryProofAction.Next) as NumberTheoryProofState.Ready
        assertTrue(state.playing)
    }

    @Test
    fun `unknown topics cannot enter a workspace`() {
        val result = engine.start("not-a-number-theory-proof")
        assertTrue(result is NumberTheoryProofState.Error)
    }

    @Test
    fun `replay reconstructs the current transition and reduced motion disables it`() {
        var state = engine.start("natural-sum") as NumberTheoryProofState.Ready
        state = engine.reduce(state, NumberTheoryProofAction.Next) as NumberTheoryProofState.Ready
        val current = state.stepIndex
        state = engine.reduce(state, NumberTheoryProofAction.ReplayStep) as NumberTheoryProofState.Ready
        assertEquals(current - 1, state.stepIndex)
        assertEquals(current, state.replayTarget)
        assertTrue(state.playing)
        state = engine.reduce(state, NumberTheoryProofAction.Next) as NumberTheoryProofState.Ready
        assertEquals(current, state.stepIndex)
        assertFalse(state.playing)

        state = engine.reduce(state, NumberTheoryProofAction.SetReducedMotion(true)) as NumberTheoryProofState.Ready
        state = engine.reduce(state, NumberTheoryProofAction.TogglePlaying) as NumberTheoryProofState.Ready
        assertFalse(state.playing)
    }
}
