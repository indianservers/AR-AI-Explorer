package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.production.*
import org.junit.Assert.*
import org.junit.Test

class NcertProductionBatchTest {
    private val manifests = CbseNcert2026Curriculum.manifests
    private val before = CurriculumCoverageAuditor.audit(manifests, ProjectCurriculumInventory.build(includeProductionBatch = false))
    private val batch01Ids = setOf("cbse-2026-c9-mathematics-c3", "cbse-2026-c9-physics-c1", "cbse-2026-c10-chemistry-c1", "cbse-2026-c10-biology-c1")
    private val after = CurriculumCoverageAuditor.audit(manifests, ProjectCurriculumInventory.build(includeProductionBatch = true, productionCurriculumNodeIds = batch01Ids))
    private val official = manifests.flatMap { it.units }.flatMap { it.chapters }.associateBy { it.id }
    private val chapters = CurriculumProductionBatchPlanner.chapters.filter { it.curriculumNodeId in batch01Ids }

    @Test fun highestPriorityCurriculumGapsAreSelectedFirst() {
        val list = CurriculumProductionBatchPlanner.prioritizedGaps(before)
        assertTrue(list.zipWithNext().all { (a, b) -> a.priority.ordinal <= b.priority.ordinal })
        val batch = CurriculumProductionBatchPlanner.selectFirstProductionBatch(before)
        assertEquals("2026-27", batch.academicYear)
        assertEquals(SchoolSubject.entries.toSet(), batch.subjectItems.keys)
        assertEquals(4, batch.estimatedLessonCount)
    }

    @Test fun selectedChaptersBecomeCompleteOnlyWithAllRequirements() {
        assertEquals(4, chapters.size)
        chapters.forEach { chapter -> assertTrue(chapter.id, DerivedChapterCompletion.evaluate(chapter, official.getValue(chapter.curriculumNodeId)).complete) }
        val beforeComplete = before.mappings.count { it.coverageStatus == CoverageStatus.COMPLETE }
        val afterComplete = after.mappings.count { it.coverageStatus == CoverageStatus.COMPLETE }
        assertEquals(4, afterComplete - beforeComplete)
    }

    @Test fun chapterCannotBeCompleteWithoutRequiredDiagram() {
        val chapter = chapters.first()
        val stripped = chapter.copy(diagrams = emptyList())
        val result = DerivedChapterCompletion.evaluate(stripped, official.getValue(chapter.curriculumNodeId))
        assertFalse(result.complete); assertTrue(RequiredAssetType.DIAGRAM in result.missing)
    }

    @Test fun chapterCannotBeCompleteWithoutRequiredPractical() {
        val chapter = chapters.single { it.subject == SchoolSubject.PHYSICS }
        val result = DerivedChapterCompletion.evaluate(chapter.copy(practicals = emptyList()), official.getValue(chapter.curriculumNodeId))
        assertFalse(result.complete); assertTrue(RequiredAssetType.PRACTICAL in result.missing)
    }

    @Test fun excludedTopicDoesNotBlockCompletion() {
        val chapter = chapters.first()
        val excluded = official.getValue(chapter.curriculumNodeId).copy(assessmentStatus = AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION)
        val empty = chapter.copy(explanationSections = emptyMap(), diagrams = emptyList(), questions = emptyList())
        assertTrue(DerivedChapterCompletion.evaluate(empty, excluded).complete)
    }

    @Test fun newlyAddedTopicPreservesExistingProgress() {
        val old = LearnerCurriculumProgress(setOf("old-lesson"), mapOf("old-lesson" to "my note"), setOf("old-lesson"), mapOf("old-q" to 3))
        val migrated = CurriculumProgressMigration.addNodes(old, chapters.map { it.id }.toSet())
        assertEquals(old.completedContentIds, migrated.completedContentIds)
        assertEquals(old.notesByContentId, migrated.notesByContentId)
        assertEquals(old.bookmarks, migrated.bookmarks)
        assertEquals(old.quizAttemptsByQuestionId, migrated.quizAttemptsByQuestionId)
        assertEquals(4, migrated.newlyIntroducedIds.size)
    }

    @Test fun subjectAssetLinksRemainInsideTheirOwners() {
        val physics = chapters.single { it.subject == SchoolSubject.PHYSICS }
        val chemistry = chapters.single { it.subject == SchoolSubject.CHEMISTRY }
        val biology = chapters.single { it.subject == SchoolSubject.BIOLOGY }
        val maths = chapters.single { it.subject == SchoolSubject.MATHEMATICS }
        assertTrue(physics.formulaLinks.all { it.formulaId.startsWith("physics-") })
        assertTrue(chemistry.formulaLinks.isEmpty())
        assertTrue(biology.diagrams.all { it.subject == SchoolSubject.BIOLOGY })
        assertTrue(maths.formulaLinks.all { it.calculatorRoute?.startsWith("mathematics/") == true })
    }

    @Test fun assessmentDiversityAndRemediationLinksValidate() {
        chapters.forEach { chapter ->
            assertTrue(chapter.questions.map { it.questionType }.distinct().size >= 4)
            assertTrue(chapter.questions.any { it.competency == CompetencyType.ANALYSING })
            assertTrue(chapter.questions.all { it.remediationSectionId in chapter.explanationSections })
        }
    }

    @Test fun diagramsAndPracticalsMeetProductionMinimums() {
        chapters.forEach { chapter ->
            assertTrue(chapter.diagrams.all { it.requiredLabels.isNotEmpty() && it.accessibilityDescription.length > 80 && it.verificationStatus == ScientificReviewStatus.VERIFIED })
            assertTrue(chapter.diagrams.all { it.id in CurriculumRenderedDiagramRegistry.ids })
        }
        chapters.filter { it.requirements.requiresPractical }.forEach { chapter ->
            assertTrue(chapter.practicals.all { it.procedure.size >= 5 && it.safetyNotes.isNotEmpty() && it.vivaQuestions.isNotEmpty() })
        }
    }

    @Test fun allProductionRoutesResolveWithoutCrossSubjectOwnership() {
        chapters.forEach { chapter ->
            assertTrue(CurriculumProductionBatchPlanner.routeResolves(chapter.route))
            assertSame(chapter, CurriculumProductionBatchPlanner.chapterForRoute(chapter.route))
            assertTrue(chapter.subject.name.lowercase() in chapter.route)
        }
    }

    @Test fun productionMappingsAreExactAndDuplicateFree() {
        assertTrue(after.duplicateMappings.isEmpty())
        chapters.forEach { chapter ->
            val mapping = after.mappings.single { it.curriculumNodeId == chapter.curriculumNodeId }
            assertTrue(chapter.id in mapping.appContentIds)
            assertTrue(after.mappings.filter { it.curriculumNodeId != chapter.curriculumNodeId }.none { chapter.id in it.appContentIds })
        }
    }

    @Test fun developerRowsAreReleaseReadyAndCoverageImproves() {
        val rows = CurriculumProductionDashboard.rows(manifests).filter { it.batchId == "ncert-2026-27-production-01" }
        assertEquals(4, rows.size); assertTrue(rows.all { it.releaseReady && it.navigationStatus == "RESOLVED" })
        val beforePoints = before.mappings.sumOf { it.coveragePercentage ?: 0 }
        val afterPoints = after.mappings.sumOf { it.coveragePercentage ?: 0 }
        assertTrue("before=$beforePoints after=$afterPoints", afterPoints > beforePoints)
        println("PRODUCTION_BEFORE=" + before.summaries.joinToString(";") { "${it.subject}:${it.evidenceWeightedCoveragePercent}:${it.complete}:${it.partial}:${it.missing}" })
        println("PRODUCTION_AFTER=" + after.summaries.joinToString(";") { "${it.subject}:${it.evidenceWeightedCoveragePercent}:${it.complete}:${it.partial}:${it.missing}" })
        println("PRODUCTION_EVIDENCE_POINTS=$beforePoints->$afterPoints")
        println("PRODUCTION_QUESTION_TYPES=" + chapters.flatMap { it.questions }.groupingBy { it.questionType }.eachCount())
    }
}
