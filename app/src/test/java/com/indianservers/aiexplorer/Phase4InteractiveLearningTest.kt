package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.learning.*
import com.indianservers.aiexplorer.workspace.MathModule
import org.junit.Assert.*
import org.junit.Test

class Phase4InteractiveLearningTest {
    @Test fun authoredActivityIsReachableValidatedAndChecksummed() {
        val document = InteractiveActivityCatalog.unitCircle
        val validation = InteractiveActivityAuthoring.validate(document)
        assertTrue(validation.errors.joinToString(), validation.valid)
        assertEquals(document.blocks.map { it.id }.toSet(), validation.reachableBlockIds)
        val exported = InteractiveActivityAuthoring.serialize(document)
        assertTrue(exported.contains("\"checksum\""))
        assertTrue(exported.contains("interactive-trig-mastery"))
    }

    @Test fun activityRunsHintFreeChecksAndRecordsSkillMastery() {
        val document = InteractiveActivityCatalog.unitCircle
        val engine = InteractiveActivityEngine()
        var state = engine.start(document)
        state = engine.submit(document, state, ActivityAnswer.Continue)
        assertEquals("identity", state.currentBlockId)
        state = engine.submit(document, state, ActivityAnswer.Text("sin(x)^2+cos(x)^2"), ActivityEvaluationContext(now = 1_000))
        assertTrue(state.results.last().passed)
        assertEquals(1, state.mastery.getValue("trig-identities").evidenceCount)
        state = engine.submit(document, state, ActivityAnswer.Choice(0), ActivityEvaluationContext(now = 2_000))
        state = engine.submit(document, state, ActivityAnswer.Text("Because the coordinates stay on the unit circle, their squared lengths always sum to one."), ActivityEvaluationContext(now = 3_000))
        assertTrue(state.completed)
        assertEquals(100, state.score)
    }

    @Test fun failedBlockBranchesToRetryAndDoesNotRevealAnswer() {
        val document = InteractiveActivityCatalog.unitCircle
        val engine = InteractiveActivityEngine()
        var state = engine.submit(document, engine.start(document), ActivityAnswer.Continue)
        state = engine.submit(document, state, ActivityAnswer.Text("0"))
        assertFalse(state.results.last().passed)
        assertEquals("identity", state.currentBlockId)
        assertFalse(state.results.last().feedback.contains("expected", true))
    }

    @Test fun adaptiveBranchUsesAccumulatedMasteryEvidence() {
        val document = InteractiveActivityDocument("branch", "Branch", "Adaptive", MathModule.Graph2D, "check", listOf(
            ActivityBlock.MathResponse("check", "Check", "Enter x.", "x", setOf("algebra"), nextOnPass = "branch"),
            ActivityBlock.Branch("branch", "Route", "algebra", .6, "secure", "practice"),
            ActivityBlock.Instruction("secure", "Secure", "Extension"),
            ActivityBlock.Instruction("practice", "Practice", "Review"),
        ))
        val engine = InteractiveActivityEngine(); var state = engine.start(document)
        state = engine.submit(document, state, ActivityAnswer.Text("x"))
        assertEquals("branch", state.currentBlockId)
        state = engine.submit(document, state, ActivityAnswer.Continue)
        assertEquals("secure", state.currentBlockId)
    }

    @Test fun manipulativeBlockConsumesFormalMathLink() {
        val document = InteractiveActivityDocument("tiles", "Tiles", "Link visual to formal", MathModule.Manipulatives, "tiles", listOf(
            ActivityBlock.ManipulativeCheck("tiles", "Tiles", FormalMathDestination.Equation, "2x + 3 = 0", setOf("algebra-tiles")),
        ))
        val manip = ManipulativeScene(items = listOf(
            ManipulativeItem("x", ManipulativeKind.AlgebraX, Vec2(0.0, 0.0), value = 2.0),
            ManipulativeItem("u", ManipulativeKind.AlgebraUnit, Vec2(1.0, 0.0), value = 3.0),
        ))
        val state = InteractiveActivityEngine().submit(document, InteractiveActivityEngine().start(document), ActivityAnswer.Manipulative, ActivityEvaluationContext(manipulatives = manip))
        assertTrue(state.completed)
    }

    @Test fun classroomSummaryIsDeterministicPrivateAndIdempotent() {
        val assignment = ActivityAssignment("a", "Trig", listOf("trig"), setOf("Learner-01", "Learner-02"))
        val events = listOf(
            ClassroomEvent("1", "a", "Learner-01", "trig", "identity", ClassroomEventKind.Attempted, 1, 50),
            ClassroomEvent("1", "a", "Learner-01", "trig", "identity", ClassroomEventKind.Attempted, 1, 50),
            ClassroomEvent("2", "a", "Learner-01", "trig", null, ClassroomEventKind.Completed, 2, 100),
            ClassroomEvent("3", "a", "Learner-02", "trig", "identity", ClassroomEventKind.Attempted, 3, 20),
            ClassroomEvent("4", "a", "Learner-02", "trig", "identity", ClassroomEventKind.Attempted, 4, 30),
            ClassroomEvent("5", "a", "Learner-02", "trig", "identity", ClassroomEventKind.HintUsed, 5),
        )
        val summary = ClassroomActivityEngine.summarize(assignment, events)
        assertEquals(.5, summary.completionRate, 1e-9)
        assertEquals(1, summary.learners.first().attempts)
        assertEquals(listOf("identity"), summary.commonStruggleBlocks)
        assertTrue(summary.learners.all { it.learnerAlias.startsWith("Learner-") })
    }

    @Test fun examGuardEnforcesPolicyAndMaintainsTamperEvidentAudit() {
        val policy = ExamPolicy("exam", "Algebra", setOf(MathModule.Graph2D), setOf("Plot", "Trace"), 60)
        val guard = ExamSessionGuard(policy, 1_000)
        assertTrue(guard.check(ExamAction.OpenModule, "Graph2D", 2_000).allowed)
        assertFalse(guard.check(ExamAction.OpenNetwork, "internet", 3_000).allowed)
        assertFalse(guard.check(ExamAction.RequestHint, "hint", 4_000).allowed)
        assertFalse(guard.check(ExamAction.UseTool, "CAS", 5_000).allowed)
        assertTrue(guard.verifyAuditChain())
        assertEquals(4, guard.auditTrail().size)
    }

    @Test fun graphSonificationPreservesSegmentsAndAnnouncesLandmarks() {
        val graph = listOf(
            GraphSegment(listOf(Vec2(-2.0, 2.0), Vec2(-1.0, 0.0), Vec2(0.0, -1.0))),
            GraphSegment(listOf(Vec2(1.0, 0.0), Vec2(2.0, 3.0))),
        )
        val accessible = GraphAccessibilityEngine.sonify(graph)
        assertTrue(accessible.description.contains("2 continuous segments"))
        assertEquals(5, accessible.notes.size)
        assertTrue(accessible.notes.all { it.pitchHz in 220.0..880.0 && it.pan in -1.0..1.0 })
        assertTrue(accessible.landmarks.any { it.contains("Root") })
    }

    @Test fun keyboardNavigatorMovesAcrossSemanticLandmarks() {
        val navigator = AccessibleMathNavigator(listOf(
            AccessibleMathLandmark("h", AccessibleMathLandmarkKind.Heading, "Graph"),
            AccessibleMathLandmark("i", AccessibleMathLandmarkKind.Input, "Expression", keyboardAction = "Enter"),
            AccessibleMathLandmark("r", AccessibleMathLandmarkKind.Result, "Roots"),
        ))
        assertEquals("h", navigator.first()?.id)
        assertEquals("i", navigator.next("h")?.id)
        assertEquals("r", navigator.next("i")?.id)
        assertEquals("i", navigator.previous("r")?.id)
    }
}
