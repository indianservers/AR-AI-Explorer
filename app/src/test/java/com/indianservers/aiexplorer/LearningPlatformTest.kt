package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.learning.ClassroomEngine
import com.indianservers.aiexplorer.learning.LearnerProgress
import com.indianservers.aiexplorer.learning.LearningCatalog
import com.indianservers.aiexplorer.learning.LearningEvaluator
import com.indianservers.aiexplorer.learning.LearningOperation
import com.indianservers.aiexplorer.learning.LearningOperationType
import com.indianservers.aiexplorer.learning.LearningPackage
import com.indianservers.aiexplorer.learning.OfflineLearningQueue
import com.indianservers.aiexplorer.learning.ProgressStatus
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningPlatformTest {
    @Test
    fun multiCheckpointLessonReportsProgressAndMisconception() {
        val lesson = LearningCatalog.lessons.first { it.id == "triangle-angle-sum" }
        val pointsOnly = WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(1.0, 2.0)), shapes = emptyList())
        val partial = LearningEvaluator.evaluate(lesson, pointsOnly)

        assertFalse(partial.passed)
        assertTrue(partial.checkpoints.first().passed)
        assertFalse(partial.checkpoints.last().passed)
        assertTrue(partial.checkpoints.last().misconception!!.contains("points"))

        val completeState = pointsOnly.copy(shapes = listOf(Shape2D("triangle", Shape2DType.Triangle, listOf(0, 1, 2))))
        val complete = LearningEvaluator.evaluate(lesson, completeState)
        assertTrue(complete.passed)
    }

    @Test
    fun progressAccumulatesCheckpointsAndAttempts() {
        val lesson = LearningCatalog.lessons.first { it.id == "triangle-angle-sum" }
        val state = WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(1.0, 2.0)), shapes = emptyList())
        val first = LearningEvaluator.recordAttempt(lesson, null, LearningEvaluator.evaluate(lesson, state), 100L)
        val completedState = state.copy(shapes = listOf(Shape2D("triangle", Shape2DType.Triangle, listOf(0, 1, 2))))
        val second = LearningEvaluator.recordAttempt(lesson, first, LearningEvaluator.evaluate(lesson, completedState), 200L)

        assertEquals(1, first.completedCheckpointIds.size)
        assertEquals(ProgressStatus.Completed, second.status)
        assertEquals(2, second.attempts)
        assertEquals(100, second.percent(lesson))
    }

    @Test
    fun teacherSummaryFlagsRepeatedStruggleWithoutScoringLearners() {
        val assignment = LearningCatalog.defaultAssignments.first()
        val lesson = LearningCatalog.lessons.first()
        val progress = mapOf(
            lesson.id to LearnerProgress(lesson.id, ProgressStatus.InProgress, attempts = 3, hintsUsed = 2),
        )
        val summary = ClassroomEngine.summarize(assignment, LearningCatalog.lessons, progress)

        assertEquals(assignment.lessonIds.size, summary.assignedLessons)
        assertTrue(lesson.title in summary.needsAttention)
        assertEquals(3, summary.attempts)
    }

    @Test
    fun offlineQueueIsOrderedIdempotentAndAcknowledged() {
        val queue = OfflineLearningQueue()
        val later = LearningOperation("b", "lesson", LearningOperationType.HintUsed, 20L)
        val earlier = LearningOperation("a", "lesson", LearningOperationType.Attempt, 10L)
        queue.enqueue(later)
        queue.enqueue(earlier)
        queue.enqueue(earlier)

        assertEquals(listOf("a", "b"), queue.pending().map { it.id })
        queue.acknowledge(setOf("a"))
        assertEquals(listOf("b"), queue.pending().map { it.id })
    }

    @Test
    fun learningPackageRoundTripsThroughSafetyValidation() {
        val source = LearningPackage.export(
            WorkspaceState(name = "Classroom \"A\""),
            LearningCatalog.lessons.first().id,
            emptyMap(),
            LearningCatalog.defaultAssignments,
        )
        val valid = LearningPackage.validate(source)

        assertTrue(valid.valid)
        assertEquals(LearningPackage.schemaVersion, valid.schemaVersion)
        assertTrue(source.contains("Classroom \\\"A\\\""))
        assertFalse(LearningPackage.validate(source.trimEnd().dropLast(1)).valid)
        assertFalse(LearningPackage.validate(source.replace("ai-explorer-learning", "unknown-package")).valid)
    }
}
