package com.indianservers.aiexplorer.assistant.grounding

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog

data class GroundingValidation(val valid: Boolean, val issues: List<String>, val allowedReferenceIds: Set<String>)

object GroundingValidator {
    fun validate(request: GroundedAssistantRequest): GroundingValidation {
        val issues = mutableListOf<String>()
        val concept = LearningIntelligenceCatalog.concepts.singleOrNull { it.conceptId == request.conceptId }
        if (concept == null) issues += "Concept is not in the verified local knowledge graph."
        if (concept != null && concept.subject != request.subject) issues += "Subject does not own this concept."
        if (request.curriculumNodeId != null && concept?.curriculumNodeId != request.curriculumNodeId) issues += "Curriculum node is not linked to this concept."
        if (request.verifiedContent.isEmpty()) issues += "No grounded content was supplied."
        if (request.verifiedContent.any { !it.reviewed }) issues += "Unreviewed content cannot be used as a factual source."
        if (request.learnerQuestion.isBlank()) issues += "Learner question is empty."
        val references = buildSet {
            request.verifiedContent.filter { it.reviewed }.mapTo(this) { it.id }
            addAll(request.verifiedFormulaIds)
            request.currentSimulationState?.let { add(it.id) }
        }
        return GroundingValidation(issues.isEmpty(), issues, references)
    }
}

object GroundedRequestFactory {
    fun local(
        conceptId: String,
        question: String,
        mastery: com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState,
        hintLevel: com.indianservers.aiexplorer.learningintelligence.model.HintLevel,
        style: ExplanationStyle,
        state: SimulationStateSnapshot? = null,
        steps: List<com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep> = emptyList(),
        misconceptions: Set<String> = emptySet(),
    ): GroundedAssistantRequest {
        val concept = LearningIntelligenceCatalog.concepts.single { it.conceptId == conceptId }
        val blocks = buildList {
            add(GroundedContentBlock("definition-$conceptId", GroundedContentKind.DEFINITION, concept.diagnosticQuestions.first().options.first()))
            concept.hints.filter { it.level.ordinal <= hintLevel.ordinal }.forEach { add(GroundedContentBlock(it.id, GroundedContentKind.HINT, it.text)) }
            add(GroundedContentBlock(concept.misconceptions.first().explanationSectionId, GroundedContentKind.EXPLANATION, concept.misconceptions.first().description))
        }
        return GroundedAssistantRequest(conceptId, concept.curriculumNodeId, concept.subject, concept.classLevels.minByOrNull { it.number }, question, blocks, emptyList(), state, emptySet(), steps, misconceptions, mastery, hintLevel, style)
    }
}
