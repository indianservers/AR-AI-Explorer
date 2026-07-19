package com.indianservers.aiexplorer.curriculum.experience

import com.indianservers.aiexplorer.curriculum.SchoolClassLevel
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.curriculum.interaction.ActivityDefinition
import com.indianservers.aiexplorer.curriculum.interaction.ActivityRunSnapshot
import com.indianservers.aiexplorer.curriculum.production.ScientificReviewStatus

data class ExperienceLearningObjective(val id: String, val statement: String)
enum class ContentSectionKind { INTRODUCTION, EXPLAIN, VISUALISE, OBSERVE, APPLY, PRACTISE, REVIEW }
enum class VisualisationTriggerType { TAP, SECTION_VISIBLE, PRACTICE_ATTEMPT, PREDICTION_LOCKED }

sealed interface ContentVisualAction {
    data class HighlightPart(val partId: String) : ContentVisualAction
    data class SetSimulationPreset(val presetId: String) : ContentVisualAction
    data class FocusGraphRegion(val rangeId: String) : ContentVisualAction
    data class PlayProcessSegment(val segmentId: String) : ContentVisualAction
    data class ShowDiagramLayer(val layerId: String) : ContentVisualAction
    data class SelectVariable(val variableId: String) : ContentVisualAction
    data class ResetVisualisation(val visualisationId: String) : ContentVisualAction
}

sealed interface InteractiveContentBlock {
    val id: String
    data class TextWithHighlight(override val id: String, val text: String, val calloutLabel: String?, val action: ContentVisualAction?) : InteractiveContentBlock
    data class InlineDiagram(override val id: String, val visualisationId: String, val caption: String, val focusTargetIds: List<String>) : InteractiveContentBlock
    data class ManipulableModel(override val id: String, val visualisationId: String, val instruction: String) : InteractiveContentBlock
    data class LiveGraph(override val id: String, val visualisationId: String, val axisDescription: String) : InteractiveContentBlock
    data class ProcessStepper(override val id: String, val visualisationId: String, val steps: List<ProcessStep>) : InteractiveContentBlock
    data class ComparisonExplorer(override val id: String, val visualisationId: String, val leftLabel: String, val rightLabel: String, val synchronisedVariables: List<String>) : InteractiveContentBlock
    data class FormulaExplorer(override val id: String, val visualisationId: String, val formula: String, val variables: List<FormulaVariableLink>) : InteractiveContentBlock
    data class PredictionPrompt(override val id: String, val prompt: String, val revealExplanation: String) : InteractiveContentBlock
    data class ObservationPrompt(override val id: String, val prompt: String, val expectedEvidence: String) : InteractiveContentBlock
    data class MisconceptionTest(override val id: String, val incorrectIdea: String, val testInstruction: String, val observedEvidence: String, val correction: String) : InteractiveContentBlock
    data class PracticeTask(override val id: String, val questionId: String, val prompt: String, val expectedAnswer: String, val explanation: String) : InteractiveContentBlock
    data class ReflectionPrompt(override val id: String, val prompt: String) : InteractiveContentBlock
    data class RevisionStrip(override val id: String, val takeaway: String, val formulaRecap: String?, val relatedConceptIds: List<String>) : InteractiveContentBlock
}

data class ProcessStep(val id: String, val location: String, val inputs: List<String>, val action: String, val outputs: List<String>, val explanation: String, val commonError: String?)
data class FormulaVariableLink(val symbol: String, val meaning: String, val unit: String, val visualTargetId: String, val relationship: String)
data class ConceptContentSection(val id: String, val title: String, val kind: ContentSectionKind, val blocks: List<InteractiveContentBlock>)
data class ConceptVisualisation(val id: String, val activityId: String, val title: String, val accessibilityDescription: String, val onDemand: Boolean = true)
data class ConceptVisualisationLink(val contentSectionId: String, val visualisationId: String, val triggerType: VisualisationTriggerType, val focusTargetId: String?, val instructionalPrompt: String?, val expectedObservation: String?)
data class ConceptActivity(val id: String, val title: String, val activityId: String)
data class QuestionReference(val id: String, val prompt: String, val expectedAnswer: String)
data class RevisionAssetReference(val id: String, val title: String)

data class ConceptCompletionPolicy(
    val requiredSectionIds: Set<String>,
    val minimumControlChanges: Int,
    val requiresPrediction: Boolean,
    val requiresObservation: Boolean,
    val requiresChallenge: Boolean,
    val minimumPracticeAttempts: Int,
    val requiresReview: Boolean,
)

data class InteractiveConceptExperience(
    val conceptId: String,
    val subject: SchoolSubject,
    val classLevels: Set<SchoolClassLevel>,
    val title: String,
    val unitAndChapter: String,
    val summary: String,
    val estimatedMinutes: Int,
    val prerequisites: List<String>,
    val learningObjectives: List<ExperienceLearningObjective>,
    val contentSections: List<ConceptContentSection>,
    val visualisations: List<ConceptVisualisation>,
    val visualisationLinks: List<ConceptVisualisationLink>,
    val activities: List<ConceptActivity>,
    val practiceItems: List<QuestionReference>,
    val revisionAssets: List<RevisionAssetReference>,
    val completionPolicy: ConceptCompletionPolicy,
    val reviewStatus: ScientificReviewStatus,
)

data class ConceptProgressState(
    val conceptId: String,
    val readSectionIds: Set<String> = emptySet(),
    val openedVisualisationIds: Set<String> = emptySet(),
    val controlChanges: Int = 0,
    val predictionAttempts: Int = 0,
    val predictionCorrect: Int = 0,
    val observationCount: Int = 0,
    val completedChallengeIds: Set<String> = emptySet(),
    val practiceAttempts: Int = 0,
    val practiceCorrect: Int = 0,
    val reviewed: Boolean = false,
    val bookmarked: Boolean = false,
) {
    fun isComplete(experience: InteractiveConceptExperience): Boolean { val p=experience.completionPolicy;return readSectionIds.containsAll(p.requiredSectionIds)&&controlChanges>=p.minimumControlChanges&&(!p.requiresPrediction||predictionAttempts>0)&&(!p.requiresObservation||observationCount>0)&&(!p.requiresChallenge||completedChallengeIds.isNotEmpty())&&practiceAttempts>=p.minimumPracticeAttempts&&(!p.requiresReview||reviewed) }
}

data class DynamicExplanation(
    val whatIsHappening: String,
    val whatChanged: String,
    val whyItChanged: String,
    val governingRule: String,
    val notice: String,
    val simplification: String,
)

object DynamicExplanationEngine {
    fun explain(definition: ActivityDefinition, before: ActivityRunSnapshot?, current: ActivityRunSnapshot): DynamicExplanation {
        val changed=if(before==null)"Initial preset is shown." else current.controls.filter{(id,value)->before.controls[id]!=value}.entries.joinToString{(id,value)->"${definition.controls.single{it.id==id}.label} is now $value ${definition.controls.single{it.id==id}.unit}"}.ifBlank{"The selected focus changed; model variables are unchanged."}
        return DynamicExplanation(current.visual.labels.values.joinToString("; "),changed,current.visual.explanation,definition.formulaOrProcess,"Compare the labelled state, graph or process before and after your action.",definition.assumptions.joinToString(" "))
    }
}

data class AuthoredExperienceCopy(
    val unitAndChapter: String,
    val summary: String,
    val prerequisites: List<String>,
    val explanation: String,
    val keyFact: String,
    val realWorldOrWorkedExample: String,
    val predictionPrompt: String,
    val expectedObservation: String,
    val misconception: String,
    val misconceptionTest: String,
    val misconceptionCorrection: String,
    val practicePrompt: String,
    val practiceAnswer: String,
    val practiceExplanation: String,
    val takeaway: String,
    val primaryFocusTargetId: String,
    val formulaVariables: List<FormulaVariableLink>,
    val processSteps: List<ProcessStep> = emptyList(),
    val comparisonLabels: Pair<String,String>? = null,
)

/** Neutral authoring assembler: all teaching claims and subject rules arrive from subject-owned packages. */
object ConceptExperienceAuthoring {
    fun build(definition: ActivityDefinition, copy: AuthoredExperienceCopy): InteractiveConceptExperience {
        val visualId="${definition.activityId}-visual";val intro="${definition.conceptId}-intro";val explain="${definition.conceptId}-explain";val interact="${definition.conceptId}-interact";val apply="${definition.conceptId}-apply";val practice="${definition.conceptId}-practice";val review="${definition.conceptId}-review"
        val sections=listOf(
            ConceptContentSection(intro,"Start here",ContentSectionKind.INTRODUCTION,listOf(InteractiveContentBlock.TextWithHighlight("$intro-summary",copy.summary,null,null),InteractiveContentBlock.TextWithHighlight("$intro-objective",definition.learningObjectives.joinToString(),"Show the model",ContentVisualAction.HighlightPart(copy.primaryFocusTargetId)))),
            ConceptContentSection(explain,"Explain",ContentSectionKind.EXPLAIN,listOf(InteractiveContentBlock.TextWithHighlight("$explain-main",copy.explanation,"Tap to highlight",ContentVisualAction.HighlightPart(copy.primaryFocusTargetId)),InteractiveContentBlock.TextWithHighlight("$explain-fact",copy.keyFact,"Try the guided preset",ContentVisualAction.SetSimulationPreset(definition.presets.first().id)),InteractiveContentBlock.FormulaExplorer("$explain-formula",visualId,definition.formulaOrProcess,copy.formulaVariables))),
            ConceptContentSection(interact,"Visualise, predict and test",ContentSectionKind.VISUALISE,buildList{add(InteractiveContentBlock.ManipulableModel("$interact-model",visualId,definition.guidedInstructions.joinToString(" ")));add(InteractiveContentBlock.InlineDiagram("$interact-diagram",visualId,"Change the controls and use labels, values and the dynamic explanation as evidence.",definition.diagramParts.map{it.id}));add(InteractiveContentBlock.PredictionPrompt("$interact-predict",copy.predictionPrompt,"Compare your prediction with the live state, then read why."));add(InteractiveContentBlock.ObservationPrompt("$interact-observe","What changed, and which evidence supports your conclusion?",copy.expectedObservation));if(copy.processSteps.isNotEmpty())add(InteractiveContentBlock.ProcessStepper("$interact-process",visualId,copy.processSteps));copy.comparisonLabels?.let{add(InteractiveContentBlock.ComparisonExplorer("$interact-compare",visualId,it.first,it.second,definition.controls.map{c->c.id}))};add(InteractiveContentBlock.MisconceptionTest("$interact-misconception",copy.misconception,copy.misconceptionTest,copy.expectedObservation,copy.misconceptionCorrection))}),
            ConceptContentSection(apply,"Apply",ContentSectionKind.APPLY,listOf(InteractiveContentBlock.TextWithHighlight("$apply-example",copy.realWorldOrWorkedExample,"Try this example",ContentVisualAction.SetSimulationPreset(definition.presets.last().id)))),
            ConceptContentSection(practice,"Practise",ContentSectionKind.PRACTISE,listOf(InteractiveContentBlock.PracticeTask("$practice-task","${definition.conceptId}-practice-1",copy.practicePrompt,copy.practiceAnswer,copy.practiceExplanation),InteractiveContentBlock.ReflectionPrompt("$practice-reflect","Explain the relationship using evidence from the visual model."))),
            ConceptContentSection(review,"Review",ContentSectionKind.REVIEW,listOf(InteractiveContentBlock.RevisionStrip("$review-strip",copy.takeaway,definition.formulaOrProcess,listOf()))),
        )
        val requiredChanges=if(definition.controls.size<=2)1 else 2
        return InteractiveConceptExperience(definition.conceptId,definition.subject,definition.classLevels,definition.title,copy.unitAndChapter,copy.summary,18,copy.prerequisites,definition.learningObjectives.mapIndexed{i,s->ExperienceLearningObjective("${definition.conceptId}-objective-${i+1}",s)},sections,listOf(ConceptVisualisation(visualId,definition.activityId,definition.title,definition.accessibility.visualStateTemplate)),listOf(ConceptVisualisationLink(explain,visualId,VisualisationTriggerType.TAP,copy.primaryFocusTargetId,"Use the highlighted quantity while reading.",copy.expectedObservation),ConceptVisualisationLink(interact,visualId,VisualisationTriggerType.SECTION_VISIBLE,copy.primaryFocusTargetId,copy.predictionPrompt,copy.expectedObservation),ConceptVisualisationLink(apply,visualId,VisualisationTriggerType.TAP,null,"Load the worked-example preset.",copy.expectedObservation)),listOf(ConceptActivity("${definition.conceptId}-activity",definition.title,definition.activityId)),listOf(QuestionReference("${definition.conceptId}-practice-1",copy.practicePrompt,copy.practiceAnswer)),listOf(RevisionAssetReference("${definition.conceptId}-revision","Visual recap and misconception correction")),ConceptCompletionPolicy(setOf(intro,explain,interact,practice,review),requiredChanges,true,true,true,1,true),ScientificReviewStatus.REVIEWED)
    }
}
