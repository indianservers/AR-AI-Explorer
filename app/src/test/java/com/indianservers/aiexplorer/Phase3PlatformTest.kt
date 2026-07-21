package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.connectedlearning.ScientificReviewStatus
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.phase3.Phase3Reference
import com.indianservers.aiexplorer.phase3.Phase3ReleaseValidator
import com.indianservers.aiexplorer.phase3.ar.ArLearningRegistry
import com.indianservers.aiexplorer.phase3.assessment.*
import com.indianservers.aiexplorer.phase3.delivery.*
import com.indianservers.aiexplorer.phase3.exam.*
import com.indianservers.aiexplorer.phase3.governance.*
import com.indianservers.aiexplorer.phase3.teacher.ReferenceTeacherFlows
import com.indianservers.aiexplorer.phase3.teacher.TeacherPlatformEngine
import com.indianservers.aiexplorer.phase3.teacher.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class Phase3PlatformTest {
    private val now = Instant.parse("2026-07-20T00:00:00Z")

    @Test fun symbolic_validation_is_restricted_equivalent_and_domain_aware() {
        val engine = SafeSymbolicEquivalenceEngine()
        assertTrue(engine.validate("2(x+3)", "2x+6").equivalent)
        assertFalse(engine.validate("x/x", "1").domainEquivalent)
        assertTrue(engine.validate("[[1,2],[3,4]]", "[[1,2],[3,4]]").equivalent)
        assertEquals("unsupported_symbol", engine.validate("x+1", "exec{x}").issueCode)
    }

    @Test fun partial_credit_preserves_method_and_follow_through_without_awarding_false_result() {
        val rubric = StepScoringRubric("task", listOf(
            StepCriterion("setup", "Setup", 1.0, "setup", emptySet(), LearnerErrorType.FORMULA),
            StepCriterion("method", "Method", 2.0, "method", setOf("setup"), LearnerErrorType.ARITHMETIC)
        ), 3.0, SchoolSubject.MATHEMATICS, ScientificReviewStatus.Verified)
        val engine = PartialCreditAssessmentEngine(mapOf(SchoolSubject.MATHEMATICS to SubjectCriterionValidator { criterion, _, _ ->
            if (criterion.id == "setup") CriterionValidation(false, false, feedback = "Incorrect setup")
            else CriterionValidation(false, methodValid = true, safeConclusion = true, feedback = "Correct method on carried value")
        }))
        val result = engine.score(rubric, listOf(CriterionAttempt("setup", "wrong"), CriterionAttempt("method", "work", "setup")))
        assertEquals(1.5, result.earnedMarks, 0.001)
        assertTrue(result.scores.last().followThrough)
        assertFalse(result.scores.last().valid)
    }

    @Test fun units_dimensions_significant_figures_and_physical_bounds_are_separate() {
        val engine = ScientificUnitEngine()
        val converted = engine.validate(1.0, "m", "100", "cm", significantFigures = 3)
        assertEquals(1.0, converted.numericCorrectness, 0.0)
        assertEquals(.75, converted.unitCorrectness, 0.0)
        assertEquals(1.0, converted.significantFigureCorrectness, 0.0)
        assertFalse(engine.validate(1.0, "m", "1", "s").dimensionalCorrectness)
        assertFalse(engine.validate(0.0, "K", "-1", "K").physicallyPlausible)
    }

    @Test fun exam_runtime_is_curriculum_bound_and_auto_submits_at_zero() {
        val manifest = ReferenceExaminations.manifests.first()
        val runtime = LocalExamRuntime(manifest, ReferenceExaminations.originalQuestions(manifest))
        val session = runtime.start(runtime.newSession("exam-1"), now)
        val submitted = runtime.tick(session, manifest.durationMinutes * 60)
        assertEquals(ExamSessionStatus.AUTO_SUBMITTED, submitted.status)
        assertTrue(runtime.palette(submitted).values.all { it == ExamQuestionStatus.UNANSWERED })
    }

    @Test fun review_lineage_accessibility_and_release_gates_fail_closed() {
        assertNotNull(ContentLineageRegistry.learnerSummary("math-triangles"))
        val record = ReviewRecord("formula", ReviewAssetType.FORMULA, "reviewer", ReviewStage.DRAFT, emptySet(),
            listOf(ReviewComment("c", "scientist", "Dimension mismatch", true, now)), listOf("1.0.0"), mapOf("dimension" to false))
        assertFalse(ScientificReviewConsole().canRelease(record, true))
        val accessibility = AccessibilityReleaseGate.evaluate(AccessibilityDefinition("visual", null, null, emptyList(), false, false, 32, false, false, false, false, false, false))
        assertFalse(accessibility.passed)
        assertTrue(accessibility.missingRequirements.contains("48dp touch targets"))
    }

    @Test fun concept_split_does_not_copy_mastery_to_every_child() {
        val state = LearnerConceptState("old", setOf("node"), SchoolSubject.MATHEMATICS, masteryState = ConceptMasteryState.MASTERED)
        val plan = ContentMigrationPlan(VersionedContentIdentity("old", SemanticVersion(2,0,0), SemanticVersion(1,0,0), ContentMigrationType.CONCEPT_SPLIT),
            listOf("child-a", "child-b"), mapOf("child-a" to .8, "child-b" to .2), "Reviewed split")
        val result = ContentMigrationEngine().migrate(state, plan)
        assertNotEquals(ConceptMasteryState.MASTERED, result.states[0].masteryState)
        assertEquals(ConceptMasteryState.LEARNING, result.states[1].masteryState)
        assertTrue(result.notesPreserved)
    }

    @Test fun scientific_dimension_validation_and_counterexamples_are_reviewed() {
        assertTrue(DimensionalConsistencyValidator().validateVerified(Phase3Reference.formulas).isEmpty())
        val bad = ScientificFormulaDefinition("bad", SchoolSubject.PHYSICS, DimensionExpression.Variable("distance"), DimensionExpression.Variable("time"),
            mapOf("distance" to Dimension(mapOf(BaseDimension.LENGTH to 1)), "time" to Dimension(mapOf(BaseDimension.TIME to 1))), true)
        assertFalse(DimensionalConsistencyValidator().validate(bad).valid)
        assertEquals(8, AuthoredCounterexampleEngine.definitions.size)
        assertTrue(AuthoredCounterexampleEngine.generate("bio-arteries-oxygen").explanation.contains("away"))
    }

    @Test fun adaptive_layout_restores_learning_state_and_translation_preserves_notation() {
        assertEquals(AdaptiveWindowClass.LANDSCAPE_LAB, AdaptiveLayoutPolicy.classify(800, 500))
        val state = RestorableLearningUiState("math-triangles", "proof-2", "state-7", "note-1", "hint-2", "exam-1")
        val restored = AdaptiveLayoutPolicy.restoreAcrossResize(state, AdaptiveWindowClass.EXPANDED)
        assertEquals(state, restored.first)
        assertTrue(restored.second.persistentTools)
        assertTrue(IndianLanguageLayer.preservesNotation("Use F=ma", "F=ma का उपयोग करें", setOf("F=ma")))
        assertFalse(IndianLanguageLayer.releasable(LocalizedLearningText("draft", "hi-IN", "मसौदा", ReviewStatus.IN_REVIEW)))
    }

    @Test fun offline_sync_preserves_versions_deletions_and_rejects_sensitive_payloads() {
        val engine = OfflineFirstSyncEngine()
        engine.enqueue(PendingSyncOperation("op", "exam", "e1", SyncOperationType.UPDATE, 1, now, 0, SyncStatus.PENDING, mapOf("answerHash" to "abc")))
        assertEquals(1, engine.pending().size)
        val bookmarks = engine.mergeBookmarks(BookmarkState(setOf("a"), setOf("b")), BookmarkState(setOf("b","c"), emptySet())).value
        assertEquals(setOf("a","c"), bookmarks.activeIds)
        assertFails { engine.enqueue(PendingSyncOperation("bad", "x", "x", SyncOperationType.CREATE, 1, now, 0, SyncStatus.PENDING, mapOf("apiKey" to "secret"))) }
        val notes = engine.mergeNotes(VersionedNote("n", "local", 2, now), VersionedNote("n", "remote", 1, now.minusSeconds(2)))
        assertEquals("local", notes.value.text)
        assertEquals(2, notes.preservedVersions)
        val notebook = engine.mergeNotebookEntries(mapOf("entry" to "local observation"), mapOf("entry" to "remote observation"))
        assertEquals(2, notebook.value.size)
        assertTrue(notebook.value.values.containsAll(listOf("local observation", "remote observation")))
    }

    @Test fun platform_record_codec_round_trips_unicode_and_delimiters() {
        val record = LocalPlatformRecord("exam|१", PlatformEntityType.EXAM_SESSION, 2, now, "उत्तर|answer\nnext")
        val decoded = Phase3RecordCodec.decode(1, Phase3RecordCodec.encode(listOf(record)))
        assertEquals(listOf(record), decoded.records)
    }

    @Test fun performance_governor_never_changes_scientific_result() {
        val signals = PerformanceSignals(45.0, 20.0, true, true, true, false)
        val (answer, quality) = PerformanceQualityGovernor().preserveScientificOutput({ 9.81 * 2.0 }, signals)
        assertEquals(19.62, answer, 0.0)
        assertEquals(PerformanceQualityLevel.LOW, quality.level)
        assertFalse(quality.glow)
    }

    @Test fun teacher_privacy_and_ar_fallbacks_are_enforced_and_release_is_valid() {
        val classroom = ReferenceTeacherFlows.mathClass
        val teacher = classroom.teacherIds.first()
        val learner = classroom.learnerIds.first()
        val engine = TeacherPlatformEngine()
        assertTrue(engine.canReadLearnerEvidence(teacher, learner, classroom))
        assertFalse(engine.canReadPrivateNote(teacher, learner))
        assertTrue(engine.canReadPrivateNote(learner, learner))
        assertTrue(ArLearningRegistry.validate().isEmpty())
        ArLearningRegistry.assets.forEach { assertEquals(it.fallbackActivityId, ArLearningRegistry.resolve(it, false, false).selectedActivityId) }
        assertTrue(Phase3ReleaseValidator.validate().valid)
    }

    @Test fun released_assets_have_lineage_and_unverified_simulation_is_blocked() {
        assertTrue(ContentLineageRegistry.validateReleased(com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog.conceptIds()).isEmpty())
        val simulation = ReviewRecord("sim", ReviewAssetType.SIMULATION, "physics-reviewer", ReviewStage.DRAFT,
            setOf(ReviewStage.CONTENT_REVIEW, ReviewStage.CURRICULUM_REVIEW, ReviewStage.ACCESSIBILITY_REVIEW, ReviewStage.TECHNICAL_REVIEW),
            emptyList(), listOf("1.0.0"), mapOf("model" to true))
        assertFalse(ScientificReviewConsole().canRelease(simulation, true))
    }

    @Test fun misconception_clusters_hide_small_groups() {
        val classroom = ReferenceTeacherFlows.mathClass
        val misconception = MisconceptionEvidence("slope-height", "math-coordinate-geometry", "q1", 1.0, LearnerConfidence.VERY_SURE, now)
        val states = classroom.learnerIds.take(2).associateWith {
            listOf(LearnerConceptState("math-coordinate-geometry", setOf("node"), SchoolSubject.MATHEMATICS, misconceptionEvidence = listOf(misconception)))
        }
        val cluster = TeacherPlatformEngine(minimumClusterSize = 5).misconceptionClusters(classroom, states).single()
        assertFalse(cluster.visible)
        assertTrue(cluster.explanation.contains("privacy"))
    }

    @Test fun invitation_tokens_are_hashed_expiring_and_role_access_is_explicit() {
        val service = ClassroomInvitationService()
        val issued = service.issue("class-1", now.plusSeconds(60))
        assertNotEquals(issued.plaintextToken, issued.persistedInvite.tokenHash)
        assertEquals(1, service.accept(issued.persistedInvite, issued.plaintextToken, now).used)
        assertFails { service.accept(issued.persistedInvite, issued.plaintextToken, now.plusSeconds(61)) }
        assertTrue(PlatformAccessPolicy.permits(PlatformRole.TEACHER, PlatformPermission.READ_ASSIGNED_EVIDENCE, assigned = true))
        assertFalse(PlatformAccessPolicy.permits(PlatformRole.TEACHER, PlatformPermission.READ_PRIVATE_NOTE, assigned = true))
        assertFalse(PlatformAccessPolicy.permits(PlatformRole.LEARNER, PlatformPermission.EXPORT_CLASS_REPORT, owner = true))
    }

    private fun assertFails(block: () -> Unit) {
        try { block(); fail("Expected failure") } catch (_: IllegalArgumentException) { }
    }
}
