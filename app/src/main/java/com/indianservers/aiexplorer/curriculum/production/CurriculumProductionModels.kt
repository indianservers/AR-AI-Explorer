package com.indianservers.aiexplorer.curriculum.production

import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.interactive.*

enum class CurriculumGapPriority { CRITICAL_MISSING_CHAPTER, CRITICAL_MISSING_TOPIC, PARTIAL_CORE_TOPIC, MISSING_PRACTICAL, MISSING_FORMULA, MISSING_DIAGRAM, MISSING_ASSESSMENT, MISSING_REVISION, ENRICHMENT }
enum class ImplementationBatchStatus { PLANNED, IN_PROGRESS, REVIEW_REQUIRED, TESTING, READY, RELEASED }
enum class CurriculumImplementationStatus { NOT_STARTED, CONTENT_IN_PROGRESS, VISUALS_IN_PROGRESS, ASSESSMENT_IN_PROGRESS, REVIEW_REQUIRED, TESTING, READY, RELEASED }
enum class DiagramType { GEOMETRY, GRAPH, CIRCUIT, RAY_DIAGRAM, FREE_BODY, APPARATUS, PARTICLE_MODEL, CHEMICAL_STRUCTURE, REACTION_PATHWAY, BIOLOGICAL_STRUCTURE, PROCESS_FLOW, ECOLOGICAL_NETWORK, TABLE }
enum class DiagramInteractionMode { STATIC_WITH_LABELS, LABEL_HIDE_AND_QUIZ, INTERACTIVE, PRINTABLE }
enum class ScientificReviewStatus { DRAFT, REVIEWED, VERIFIED }
enum class QuestionDifficulty { FOUNDATION, STANDARD, CHALLENGE }
enum class CurriculumQuestionType { MULTIPLE_CHOICE, MULTIPLE_SELECT, TRUE_FALSE, FILL_BLANK, NUMERIC_INPUT, SHORT_ANSWER, MATCH_FOLLOWING, ASSERTION_REASON, CASE_BASED, SOURCE_BASED, DIAGRAM_LABELLING, GRAPH_INTERPRETATION, TABLE_INTERPRETATION, SEQUENCE_ORDERING, EXPERIMENTAL_REASONING, PROOF_DERIVATION, CONSTRUCTION }
enum class PracticalSupportType { PHYSICAL_LAB_GUIDANCE, VIRTUAL_SIMULATION, DEMONSTRATION, DATA_ANALYSIS_ACTIVITY, GUIDED_ACTIVITY }

data class CurriculumGapItem(val curriculumNodeId: String, val subject: SchoolSubject, val classLevel: SchoolClassLevel, val title: String, val priority: CurriculumGapPriority, val missingAssets: Set<RequiredAssetType>, val reason: String)
data class CurriculumImplementationBatch(val id: String, val academicYear: String, val priority: CurriculumGapPriority, val subjectItems: Map<SchoolSubject, List<CurriculumGapItem>>, val estimatedLessonCount: Int, val estimatedAssetCount: Int, val status: ImplementationBatchStatus)
data class CurriculumCompletionRequirements(val requiresLesson: Boolean, val requiresDiagram: Boolean, val requiresFormula: Boolean, val requiresWorkedExamples: Boolean, val requiresActivity: Boolean, val requiresPractical: Boolean, val requiresQuiz: Boolean, val requiresCompetencyQuestions: Boolean, val requiresRevision: Boolean,val requiresInteractiveVisual:Boolean=false,val requiresGuidedMode:Boolean=false,val requiresExploreMode:Boolean=false,val requiresChallengeMode:Boolean=false,val requiresPrediction:Boolean=false,val requiresAccessibleAlternative:Boolean=false)
data class DiagramLabelRequirement(val id: String, val label: String, val purpose: String)
data class InteractiveDiagramPart(val id:String,val label:String,val description:String,val function:String?,val linkedSectionId:String)
data class InteractiveDiagramLayer(val id:String,val label:String,val defaultVisible:Boolean)
data class DiagramProcessStage(val id:String,val title:String,val explanation:String,val inputIds:List<String>,val outputIds:List<String>)
data class CurriculumDiagramSpecification(val id: String, val subject: SchoolSubject, val classLevel: SchoolClassLevel, val conceptId: String, val title: String, val diagramType: DiagramType, val requiredLabels: List<DiagramLabelRequirement>, val learningPurpose: String, val accuracyNotes: List<String>, val interactionMode: DiagramInteractionMode, val accessibilityDescription: String, val verificationStatus: ScientificReviewStatus,val selectableParts:List<InteractiveDiagramPart> = emptyList(),val layers:List<InteractiveDiagramLayer> = emptyList(),val processStages:List<DiagramProcessStage> = emptyList(),val supportsZoom:Boolean=true,val supportsPan:Boolean=true,val supportsLabelQuiz:Boolean=false,val supportsComparison:Boolean=false,val linkedExplanationSectionIds:List<String> = emptyList())
data class CurriculumFormulaLink(val formulaId: String, val relationship: String, val symbolDefinitions: Map<String, String>, val units: Map<String, String>, val assumptions: List<String>, val rearrangedForms: List<String>, val commonMistakes: List<String>, val calculatorRoute: String?)
data class CurriculumPracticalContent(val id: String, val supportType: PracticalSupportType, val objective: String, val apparatus: List<String>, val materials: List<String>, val procedure: List<String>, val observations: List<String>, val calculations: List<String>, val resultTemplate: String, val precautions: List<String>, val safetyNotes: List<String>, val vivaQuestions: List<String>, val physicalLabRequired: Boolean)
data class AnswerDefinition(val expected: String, val acceptedAlternatives: List<String> = emptyList())
data class CurriculumQuestion(val id: String, val subject: SchoolSubject, val classLevel: SchoolClassLevel, val chapterId: String, val topicIds: List<String>, val competency: CompetencyType, val difficulty: QuestionDifficulty, val questionType: CurriculumQuestionType, val prompt: String, val options: List<String>? = null, val answer: AnswerDefinition, val explanation: String, val commonErrorTags: List<String>, val remediationSectionId: String)
data class WorkedExample(val prompt: String, val steps: List<String>, val answer: String, val validation: String)
data class CurriculumRevisionResource(val takeaways: List<String>, val formulaRecap: List<String>, val diagramRecap: List<String>, val confusedPoints: List<String>, val quickSelfTestQuestionIds: List<String>)

data class SubjectOwnedCurriculumChapter(
    val id: String,
    val curriculumNodeId: String,
    val subject: SchoolSubject,
    val classLevel: SchoolClassLevel,
    val officialChapterTitle: String,
    val unitTitle: String,
    val estimatedMinutes: Int,
    val route: String,
    val topicIds: List<String>,
    val learningObjectives: List<String>,
    val prerequisites: List<String>,
    val explanationSections: Map<String, String>,
    val keyTerms: Map<String, String>,
    val workedExamples: List<WorkedExample>,
    val diagrams: List<CurriculumDiagramSpecification>,
    val formulaLinks: List<CurriculumFormulaLink>,
    val activities: List<String>,
    val practicals: List<CurriculumPracticalContent>,
    val misconceptions: Map<String, String>,
    val questions: List<CurriculumQuestion>,
    val revision: CurriculumRevisionResource,
    val relationships: List<String>,
    val requirements: CurriculumCompletionRequirements,
    val reviewStatus: ScientificReviewStatus,
    val interactionProfiles:List<CurriculumInteractionProfile> = emptyList(),
    val interactiveActivities:List<CurriculumInteractiveActivity> = emptyList()
) {
    val completedAssetTypes: Set<RequiredAssetType> get() = buildSet {
        if (explanationSections.isNotEmpty()) add(RequiredAssetType.LESSON)
        if (diagrams.isNotEmpty()) add(RequiredAssetType.DIAGRAM)
        if (formulaLinks.isNotEmpty()) add(RequiredAssetType.FORMULA)
        if (workedExamples.isNotEmpty()) add(RequiredAssetType.WORKED_EXAMPLE)
        if (activities.isNotEmpty()) add(RequiredAssetType.ACTIVITY)
        if (practicals.isNotEmpty()) add(RequiredAssetType.PRACTICAL)
        if (questions.isNotEmpty()) { add(RequiredAssetType.QUIZ); add(RequiredAssetType.EXERCISE) }
        if (questions.any { it.competency in setOf(CompetencyType.APPLYING, CompetencyType.ANALYSING, CompetencyType.EVALUATING, CompetencyType.CREATING) }) add(RequiredAssetType.COMPETENCY_QUESTION)
        if (revision.takeaways.isNotEmpty()) add(RequiredAssetType.REVISION_MATERIAL)
        if (diagrams.any { it.diagramType == DiagramType.GRAPH }) add(RequiredAssetType.GRAPH)
        if (practicals.any { it.supportType == PracticalSupportType.PHYSICAL_LAB_GUIDANCE }) add(RequiredAssetType.EXPERIMENT)
        val valid=interactiveActivities.filter{it.functionalFor(subject)}
        if(valid.isNotEmpty()){add(RequiredAssetType.INTERACTIVE_VISUAL);add(RequiredAssetType.ACTIVITY)}
        if(valid.any{it.visualType==InteractiveVisualType.PROCESS_STEPPER})add(RequiredAssetType.PROCESS_VISUALISER)
        if(valid.any{it.visualType==InteractiveVisualType.FORMULA_VISUALISER||it.measurableOutputs.any{o->o.outputType==InteractiveOutputType.FORMULA_RESULT}})add(RequiredAssetType.FORMULA_VISUALISER)
        if(valid.any{it.predictionPrompts.isNotEmpty()})add(RequiredAssetType.PREDICTION_ACTIVITY)
        if(valid.any{it.challenges.isNotEmpty()})add(RequiredAssetType.CHALLENGE_ACTIVITY)
        if(valid.any{it.accessibility.summary.isNotBlank()&&it.accessibility.dynamicStateTemplate.isNotBlank()})add(RequiredAssetType.ACCESSIBLE_VISUAL_ALTERNATIVE)
    }
}

data class ChapterReadiness(val complete: Boolean, val missing: Set<RequiredAssetType>, val issues: List<String>)

object CurriculumRenderedDiagramRegistry {
    val ids = setOf("math-c9-sequence-table-graph", "physics-c9-position-time-graph", "physics-c9-path-displacement", "chem-c10-reaction-conservation-model", "chem-c10-reaction-apparatus", "bio-c10-life-process-network", "bio-c10-nephron-structure-function", "math-c10-real-factor-exponent-map", "physics-c10-electric-circuit", "physics-c10-vi-graph", "chem-c10-ph-scale", "bio-c10-reflex-arc")
}

object DerivedChapterCompletion {
    fun evaluate(chapter: SubjectOwnedCurriculumChapter, official: CurriculumChapter): ChapterReadiness {
        if (official.assessmentStatus in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION)) return ChapterReadiness(true, emptySet(), emptyList())
        val required = buildSet {
            if (chapter.requirements.requiresLesson) add(RequiredAssetType.LESSON)
            if (chapter.requirements.requiresDiagram) add(RequiredAssetType.DIAGRAM)
            if (chapter.requirements.requiresFormula) add(RequiredAssetType.FORMULA)
            if (chapter.requirements.requiresWorkedExamples) add(RequiredAssetType.WORKED_EXAMPLE)
            if (chapter.requirements.requiresActivity) add(RequiredAssetType.ACTIVITY)
            if (chapter.requirements.requiresPractical) add(RequiredAssetType.PRACTICAL)
            if (chapter.requirements.requiresQuiz) add(RequiredAssetType.QUIZ)
            if (chapter.requirements.requiresCompetencyQuestions) add(RequiredAssetType.COMPETENCY_QUESTION)
            if (chapter.requirements.requiresRevision) add(RequiredAssetType.REVISION_MATERIAL)
            if (chapter.requirements.requiresInteractiveVisual) add(RequiredAssetType.INTERACTIVE_VISUAL)
            if (chapter.requirements.requiresPrediction) add(RequiredAssetType.PREDICTION_ACTIVITY)
            if (chapter.requirements.requiresChallengeMode) add(RequiredAssetType.CHALLENGE_ACTIVITY)
            if (chapter.requirements.requiresAccessibleAlternative) add(RequiredAssetType.ACCESSIBLE_VISUAL_ALTERNATIVE)
        }
        val missing = required - chapter.completedAssetTypes
        val issues = buildList {
            if (chapter.curriculumNodeId != official.id) add("Official curriculum link does not resolve.")
            if (chapter.subject.name.lowercase() !in chapter.route) add("Route is outside the owning subject.")
            if (chapter.questions.map { it.questionType }.distinct().size < 4) add("Assessment diversity requires at least four question types.")
            if (chapter.questions.any { it.remediationSectionId !in chapter.explanationSections }) add("A remediation section is broken.")
            if (chapter.diagrams.any { it.id !in CurriculumRenderedDiagramRegistry.ids }) add("A required diagram has no implemented renderer.")
            if (chapter.reviewStatus != ScientificReviewStatus.VERIFIED) add("Scientific or mathematical verification is incomplete.")
            val activities=chapter.interactiveActivities
            if(chapter.requirements.requiresGuidedMode&&activities.none{InteractionMode.GUIDED in it.supportedModes})add("Required guided mode is missing.")
            if(chapter.requirements.requiresExploreMode&&activities.none{InteractionMode.EXPLORE in it.supportedModes})add("Required explore mode is missing.")
            if(chapter.requirements.requiresChallengeMode&&activities.none{InteractionMode.CHALLENGE in it.supportedModes})add("Required challenge mode is missing.")
            if(chapter.requirements.requiresPrediction&&activities.none{it.predictionPrompts.isNotEmpty()})add("Required prediction is missing.")
            if(chapter.requirements.requiresAccessibleAlternative&&activities.none{it.accessibility.summary.isNotBlank()&&it.accessibility.dynamicStateTemplate.isNotBlank()})add("Accessibility alternative is missing.")
            activities.forEach{activity->
                if(activity.subject!=chapter.subject)add("Activity ${activity.id} is not subject-owned.")
                if(activity.route.isBlank()||"/${chapter.subject.name.lowercase()}/" !in activity.route)add("Activity route is broken: ${activity.id}.")
                if(activity.controls.any{!it.changesModelState||it.affectedVisualIds.isEmpty()})add("Activity has a disconnected control: ${activity.id}.")
                if(activity.contentLinks.isEmpty()||activity.contentLinks.any{it.explanationSectionId !in chapter.explanationSections})add("Activity has a missing explanation-to-visual link: ${activity.id}.")
                if(activity.reviewStatus==ScientificReviewStatus.VERIFIED&&activity.learningObjectives.isEmpty())add("Verified activity lacks a learning objective: ${activity.id}.")
                if(activity.challenges.any{it.instruction.isBlank()||it.successConditionId.isBlank()})add("Activity has an empty or unverifiable challenge: ${activity.id}.")
                if(activity.completionPolicy.requiresReset&&!activity.hasReset)add("Visualisation requires reset but has no reset mechanism: ${activity.id}.")
            }
        }
        return ChapterReadiness(missing.isEmpty() && issues.isEmpty(), missing, issues)
    }
}

data class LearnerCurriculumProgress(val completedContentIds: Set<String>, val notesByContentId: Map<String, String>, val bookmarks: Set<String>, val quizAttemptsByQuestionId: Map<String, Int>, val newlyIntroducedIds: Set<String> = emptySet(), val migrationNotice: String? = null)
object CurriculumProgressMigration {
    fun addNodes(old: LearnerCurriculumProgress, newIds: Set<String>) = old.copy(newlyIntroducedIds = newIds - old.completedContentIds, migrationNotice = if (newIds.isEmpty()) null else "The 2026-27 syllabus added ${newIds.size} learning item(s); previous mastery, notes, bookmarks and quiz history were preserved.")
}

object CurriculumProductionRegistry {
    private val providers = mutableListOf<() -> List<SubjectOwnedCurriculumChapter>>()
    fun register(provider: () -> List<SubjectOwnedCurriculumChapter>) { providers += provider }
    fun chapters(): List<SubjectOwnedCurriculumChapter> = providers.flatMap { it() }.distinctBy { it.id }
}
