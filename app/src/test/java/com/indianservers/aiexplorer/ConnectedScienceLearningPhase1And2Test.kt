package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.biology.learning.BiologyConnectedLearningRepository
import com.indianservers.aiexplorer.biology.repository.OfflineBiologyRepository
import com.indianservers.aiexplorer.chemistry.formulas.repository.OfflineChemistryFormulaRepository
import com.indianservers.aiexplorer.chemistry.learning.ChemistryConnectedLearningRepository
import com.indianservers.aiexplorer.connectedlearning.*
import com.indianservers.aiexplorer.physics.formulas.repository.OfflinePhysicsFormulaRepository
import com.indianservers.aiexplorer.physics.learning.PhysicsConnectedLearningRepository
import org.junit.Assert.*
import org.junit.Test

class ConnectedScienceLearningPhase1And2Test {
    private val journeys = listOf(PhysicsConnectedLearningRepository.journey, ChemistryConnectedLearningRepository.journey, BiologyConnectedLearningRepository.journey)

    @Test fun referenceJourneysHaveRequiredOrderAndStableIds() {
        assertEquals(listOf("motion","velocity","acceleration","newton-second-law","work-energy"), PhysicsConnectedLearningRepository.journey.conceptIds)
        assertEquals(listOf("atomic-structure","electron-configuration","periodic-trends","chemical-bonding","molecular-geometry"), ChemistryConnectedLearningRepository.journey.conceptIds)
        assertEquals(listOf("cell-structure","cell-membrane","diffusion-osmosis","active-transport","cell-homeostasis"), BiologyConnectedLearningRepository.journey.conceptIds)
        journeys.forEach { assertEquals(5, it.concepts.size); assertEquals(5, it.concepts.map { concept -> concept.id }.toSet().size) }
    }

    @Test fun prerequisitesFormExplicitNonCircularChains() {
        journeys.forEach { journey -> journey.concepts.forEachIndexed { index, concept -> assertEquals(if(index==0) emptyList<String>() else listOf(journey.concepts[index-1].id), concept.prerequisiteIds) } }
        assertTrue(PhysicsConnectedLearningRepository.validate().valid); assertTrue(ChemistryConnectedLearningRepository.validate().valid); assertTrue(BiologyConnectedLearningRepository.validate().valid)
    }

    @Test fun everyConceptHasObjectivesCriteriaAndReviewedContent() {
        journeys.flatMap { it.concepts }.forEach { concept ->
            assertTrue(concept.learningObjectives.size >= 3); assertTrue(concept.coreExplanation.isNotBlank()); assertTrue(concept.whyItMatters.isNotBlank())
            assertTrue(concept.completionCriteria.practiceAttempts > 0); assertTrue(concept.completionCriteria.quizMinimumPercent in 1..100)
            assertNotEquals(ScientificReviewStatus.Draft, concept.reviewStatus)
        }
    }

    @Test fun physicsJourneyConnectsAllRequiredActivityTypes() {
        val required=setOf(LearningActivityKind.Lesson,LearningActivityKind.Diagram,LearningActivityKind.Formula,LearningActivityKind.Calculator,LearningActivityKind.Interactive,LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision)
        PhysicsConnectedLearningRepository.journey.concepts.forEach { assertTrue(it.activities.map { activity -> activity.kind }.containsAll(required)) }
    }

    @Test fun chemistryJourneyConnectsLearningExplorationAndAssessment() {
        val required=setOf(LearningActivityKind.Lesson,LearningActivityKind.Diagram,LearningActivityKind.Interactive,LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision)
        ChemistryConnectedLearningRepository.journey.concepts.forEach { assertTrue(it.activities.map { activity -> activity.kind }.containsAll(required)) }
    }

    @Test fun biologyJourneyHasProcessDiagramInteractionAndFuture3DForEveryConcept() {
        val required=setOf(LearningActivityKind.Lesson,LearningActivityKind.Diagram,LearningActivityKind.ProcessView,LearningActivityKind.Interactive,LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision,LearningActivityKind.Future3D)
        BiologyConnectedLearningRepository.journey.concepts.forEach { concept -> assertTrue(concept.activities.map { it.kind }.containsAll(required)); assertTrue(concept.activities.first { it.kind==LearningActivityKind.Future3D }.available.not()) }
    }

    @Test fun physicsFormulaReferencesResolveInPhysicsOnly() {
        val formulas=OfflinePhysicsFormulaRepository();PhysicsConnectedLearningRepository.journey.concepts.flatMap { it.activities }.filter { it.kind==LearningActivityKind.Formula }.forEach { assertNotNull(formulas.getFormula(it.id)) }
    }

    @Test fun chemistryFormulaReferencesResolveWhenScientificallyApplicable() {
        val formulas=OfflineChemistryFormulaRepository();val refs=ChemistryConnectedLearningRepository.journey.concepts.flatMap { it.activities }.filter { it.kind==LearningActivityKind.Formula };assertEquals(4,refs.size);refs.forEach { assertNotNull(formulas.getFormula(it.id)) }
    }

    @Test fun biologyLessonReferencesResolveAgainstExistingCatalogue() {
        val biology=OfflineBiologyRepository();BiologyConnectedLearningRepository.journey.concepts.flatMap { it.activities }.filter { it.kind==LearningActivityKind.Lesson }.forEach { assertNotNull("Missing ${it.id}",biology.getConcept(it.id)) }
    }

    @Test fun recommendedNextExplainsPrerequisiteProgression() {
        val journey=PhysicsConnectedLearningRepository.journey
        val first=RecommendedNextEngine.recommend(journey,emptySet(),ConnectedLearningLevel.Class12)!!;assertEquals("motion",first.concept.id);assertTrue(first.reason.contains("foundation"))
        val second=RecommendedNextEngine.recommend(journey,setOf("motion"),ConnectedLearningLevel.Class12)!!;assertEquals("velocity",second.concept.id);assertTrue(second.reason.contains("Motion"))
    }

    @Test fun recommendationDoesNotSkipMissingPrerequisites() {
        val journey=BiologyConnectedLearningRepository.journey
        assertEquals("cell-membrane",RecommendedNextEngine.recommend(journey,setOf("cell-structure"),ConnectedLearningLevel.Class12)?.concept?.id)
        assertEquals("cell-structure",RecommendedNextEngine.recommend(journey,setOf("active-transport"),ConnectedLearningLevel.Class12)?.concept?.id)
    }

    @Test fun validatorRejectsBrokenPrerequisiteAndExposedDraft() {
        val journey=PhysicsConnectedLearningRepository.journey;val broken=journey.copy(concepts=journey.concepts.mapIndexed { i,c -> if(i==0)c.copy(prerequisiteIds=listOf("missing"),reviewStatus=ScientificReviewStatus.Draft)else c })
        val report=ConnectedLearningValidator.validate(broken,setOf(LearningActivityKind.Lesson));assertFalse(report.valid);assertTrue(report.errors.any { it.contains("broken") });assertTrue(report.errors.any { it.contains("draft") })
    }

    @Test fun subjectOwnershipAndActivityNamespacesRemainSeparate() {
        assertEquals(setOf("Physics","Chemistry","Biology"),journeys.map { it.subject }.toSet())
        assertEquals(3,journeys.map { it.id }.toSet().size)
        journeys.forEach { journey -> journey.concepts.forEach { concept -> assertEquals(concept.activities.size,concept.activities.map { it.id }.toSet().size) } }
    }
}

