package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.interaction.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class InteractiveReferenceBatchTest {
    private val definitions = ReferenceActivityRegistry.definitions

    @Test fun everyAssessableConceptHasInteractionProfile() {
        val report = InteractionCoverageAuditor.audit()
        val assessable = CbseNcert2026Curriculum.manifests.flatMap { manifest -> manifest.units.flatMap { it.chapters }.flatMap { it.topics }.filter { it.currentAssessmentStatus == AssessmentStatus.INCLUDED_AND_ASSESSABLE }.map { it.id } }.toSet()
        assertEquals(assessable, report.profiles.filter { it.conceptId in assessable }.map { it.conceptId }.toSet())
        assertEquals(assessable.size, report.summary.assessableConcepts)
        assertTrue(report.toJson().contains("\"profiles\""))
        val artifact = report.writeTo(File("build/reports/interaction/interaction-coverage.json"))
        assertTrue(artifact.isFile && artifact.length() > 1_000)
        println("INTERACTION_AUDIT=" + report.summary)
        println("INTERACTION_INVENTORY=" + report.inventory)
    }

    @Test fun referenceBatchHasFiveFunctionalSubjectOwnedActivitiesPerSubject() {
        assertEquals(20, definitions.size)
        SchoolSubject.entries.forEach { subject -> assertEquals(subject.name, 5, definitions.count { it.subject == subject }) }
        assertEquals(20, definitions.map { it.activityId }.distinct().size)
        assertTrue(definitions.all { it.supportedModes == ActivityMode.entries.toSet() && it.challenges.size >= 3 })
    }

    @Test fun noInteractiveControlIsDisconnected() {
        definitions.forEach { definition ->
            definition.controls.forEach { control ->
                val activity = FunctionalInteractiveActivity(definition)
                val before = activity.snapshot.visual
                val changed = if (control.initial != control.maximum) control.maximum else control.minimum
                activity.setControl(control.id, changed)
                assertNotEquals("${definition.activityId}/${control.id}", before.values, activity.snapshot.visual.values)
                assertEquals(changed, activity.snapshot.controls.getValue(control.id), 1e-9)
            }
        }
    }

    @Test fun everySimulationCanResetAndUsePresets() {
        definitions.forEach { definition ->
            val activity = FunctionalInteractiveActivity(definition); val initial = activity.snapshot
            activity.setControl(definition.controls.first().id, definition.controls.first().maximum)
            activity.applyPreset(definition.presets.first().id)
            activity.reset()
            assertEquals(definition.activityId, initial.controls, activity.snapshot.controls)
            assertEquals(0, activity.snapshot.interactionCount)
            assertFalse(activity.snapshot.completed)
        }
    }

    @Test fun everyActivityHasAccessibleAlternativeObjectivesUnitsAndValidLabels() {
        definitions.forEach { definition ->
            assertTrue(definition.learningObjectives.all { it.isNotBlank() })
            assertTrue(definition.accessibility.stepByStepTextAvailable)
            assertTrue(definition.controls.all { it.unit.isNotBlank() && it.spokenLabel.isNotBlank() })
            val visual = definition.compute(definition.controls.associate { it.id to it.initial })
            assertTrue(visual.textAlternative.isNotBlank())
            assertEquals(visual.values.keys, visual.valueUnits.keys)
            visual.highlightedPartId?.let { id -> assertTrue("${definition.activityId}: $id", definition.diagramParts.any { it.id == id }) }
        }
    }

    @Test fun everyChallengeHasExecutableVerificationCondition() {
        definitions.forEach { definition ->
            val visual = definition.compute(definition.controls.associate { it.id to it.initial })
            definition.challenges.forEach { challenge ->
                assertTrue(challenge.instruction.isNotBlank() && challenge.targetDescription.isNotBlank())
                challenge.isSatisfied(visual) // must be deterministic and side-effect free
            }
        }
    }

    @Test fun activityCompletionRequiresMeaningfulInteraction() {
        val activity = ReferenceActivityRegistry.create("math-linear-equation-balance")
        assertFalse(activity.snapshot.completed)
        activity.predict("x will be four")
        activity.observe("Both pans remain balanced")
        assertTrue(activity.checkChallenge("x4"))
        assertFalse("Opening, predicting, observing and passing without manipulation must not complete", activity.snapshot.completed)
        activity.setControl("b", 2.0); activity.setControl("c", 10.0)
        assertTrue(activity.checkChallenge("x4"))
        assertTrue(activity.snapshot.completed)
        assertNull("Prediction accuracy is independent until explicitly assessed", activity.snapshot.predictionCorrect)
    }

    @Test fun persistenceStoresCompactEvidenceNotFrameHistory() {
        val store = InMemoryActivityProgressStore()
        store.save(ActivityProgressRecord("a", mapOf("x" to 2.0), true, 2, 3, 2, (1..30).map { "observation $it" }, false, .7))
        val saved = store.load("a")!!
        assertEquals(20, saved.observations.size)
        assertEquals(3, saved.predictionAttempts)
        assertEquals(2, saved.predictionCorrect)
    }

    @Test fun excludedConceptsDoNotBlockInteractionCoverage() {
        val report = InteractionCoverageAuditor.audit()
        val excluded = CbseNcert2026Curriculum.manifests.flatMap { it.units }.flatMap { it.chapters }.flatMap { it.topics }.filter { it.currentAssessmentStatus in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION) }.map { it.id }.toSet()
        assertTrue(report.profiles.filter { it.conceptId in excluded }.all { it.coverageStatus == InteractionCoverageStatus.NOT_APPLICABLE })
    }
}
