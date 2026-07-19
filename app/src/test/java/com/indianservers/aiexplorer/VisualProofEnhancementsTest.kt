package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ProofEnhancement
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualProofEnhancementsTest {
    @Test
    fun allFiftyRequestedEnhancementsAreRepresented() {
        assertEquals(50, ProofEnhancement.entries.size)
        assertEquals(ProofEnhancement.entries.toSet(), VisualProofCatalog.enhancementCoverage)
    }

    @Test
    fun everyProofHasCompleteReasoningProfile() {
        VisualProofCatalog.labs.forEach { lab ->
            val profile = VisualProofCatalog.profileFor(lab.id)
            assertTrue("${lab.id} has staged visual grammar", ProofEnhancement.ColorCodedStages in profile.features)
            assertTrue("${lab.id} has before/after comparison", ProofEnhancement.BeforeAfter in profile.features)
            assertTrue("${lab.id} lists assumptions", profile.assumptions.isNotEmpty())
            assertTrue("${lab.id} marks an invariant", profile.invariant.isNotBlank())
            assertTrue("${lab.id} explains validity", profile.validityNotes.size >= 3)
            assertTrue("${lab.id} has definitions", profile.definitions.isNotEmpty())
            assertTrue("${lab.id} has a counterexample", profile.counterexample.isNotBlank())
            assertTrue("${lab.id} has an error trap", profile.errorTrap.isNotBlank())
            assertTrue("${lab.id} has a simple case", profile.simpleCase.isNotBlank())
            assertTrue("${lab.id} ends with a takeaway", profile.takeaway.startsWith("The picture proves this because"))
        }
    }

    @Test
    fun targetedProofTypesUseTheirSpecializedVisuals() {
        val expected = mapOf(
            "absolute-inequality" to ProofEnhancement.NumberLine,
            "equation-balance" to ProofEnhancement.BalanceScale,
            "set-de-morgan" to ProofEnhancement.VennDiagram,
            "epsilon-delta" to ProofEnhancement.EpsilonDeltaBands,
            "slope-triangle" to ProofEnhancement.SlopeTriangle,
            "eigenvector-direction" to ProofEnhancement.EigenvectorDirection,
            "counting-paths" to ProofEnhancement.CountingTree,
            "modular-clock" to ProofEnhancement.ModularClock,
        )
        expected.forEach { (labId, feature) ->
            assertTrue(feature in VisualProofCatalog.profileFor(labId).features)
            assertTrue(labId in InteractiveVisualProofSceneIds)
        }
    }

    @Test
    fun newProofCalculationsAreInteractiveAndVerified() {
        val engine = VisualProofEngine()
        val newIds = listOf(
            "absolute-inequality", "equation-balance", "set-de-morgan", "epsilon-delta",
            "slope-triangle", "eigenvector-direction", "counting-paths", "modular-clock",
        )
        newIds.forEach { id ->
            val playback = engine.start(id)
            assertTrue("$id has live measurements", playback.frame.measurements.isNotEmpty())
            assertTrue("$id starts in a valid state", playback.frame.holds)
            assertTrue(playback.frame.residual.isFinite())
        }
        val epsilon = engine.start("epsilon-delta")
        val invalid = engine.setParameter(engine.setParameter(epsilon, "epsilon", .2), "delta", 1.0)
        assertFalse("too-wide delta band is visibly rejected", invalid.frame.holds)
        assertTrue(invalid.frame.residual > 0.0)
    }
}
