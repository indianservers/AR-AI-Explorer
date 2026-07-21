package com.indianservers.aiexplorer.learningintelligence.engine

import com.indianservers.aiexplorer.connectedlearning.ScientificReviewStatus
import com.indianservers.aiexplorer.curriculum.AssessmentStatus
import com.indianservers.aiexplorer.curriculum.CbseNcert2026Curriculum
import com.indianservers.aiexplorer.learningintelligence.model.*
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

data class GraphValidationReport(val errors: List<String>) { val valid get() = errors.isEmpty() }
class KnowledgeGraphIndex(val graph: KnowledgeGraph) {
    val nodeById = graph.nodes.associateBy { it.id }
    private val outgoing = graph.edges.groupBy { it.fromId }
    private val incoming = graph.edges.groupBy { it.toId }
    fun outgoing(id: String, relation: KnowledgeRelation? = null) = outgoing[id].orEmpty().filter { relation == null || it.relation == relation }
    fun incoming(id: String, relation: KnowledgeRelation? = null) = incoming[id].orEmpty().filter { relation == null || it.relation == relation }
    fun mandatoryPrerequisites(conceptId: String) = incoming(conceptId, KnowledgeRelation.PREREQUISITE_OF).filter { it.mandatory }.map { it.fromId }
}

object KnowledgeGraphValidator {
    fun validate(graph: KnowledgeGraph): GraphValidationReport {
        val errors = mutableListOf<String>(); val nodes = graph.nodes.associateBy { it.id }
        if (nodes.size != graph.nodes.size) errors += "Duplicate knowledge node ID."
        graph.edges.forEach { edge ->
            if (edge.fromId !in nodes || edge.toId !in nodes) errors += "Unresolved edge ${edge.fromId} -> ${edge.toId}."
            val from = nodes[edge.fromId]; val to = nodes[edge.toId]
            if (from?.subject != null && to?.subject != null && from.subject != to.subject && edge.relation != KnowledgeRelation.CROSS_SUBJECT_LINK) errors += "Subject ownership violation on ${edge.fromId} -> ${edge.toId}."
            if (edge.mandatory && (from?.reviewStatus != ScientificReviewStatus.Verified || to?.reviewStatus != ScientificReviewStatus.Verified)) errors += "Unverified node in mandatory logic: ${edge.fromId} -> ${edge.toId}."
        }
        val officialStatus = CbseNcert2026Curriculum.manifests.flatMap { it.units }.flatMap { it.chapters }.flatMap { chapter -> chapter.topics.map { it.id to it.currentAssessmentStatus } }.toMap()
        graph.edges.filter { it.mandatory }.forEach { edge -> if (officialStatus[edge.fromId] in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION)) errors += "Excluded topic is mandatory: ${edge.fromId}." }
        val prereq = graph.edges.filter { it.mandatory && it.relation == KnowledgeRelation.PREREQUISITE_OF }.groupBy { it.fromId }
        val visiting = mutableSetOf<String>(); val visited = mutableSetOf<String>()
        fun cycle(id: String): Boolean { if (id in visiting) return true; if (id in visited) return false; visiting += id; val found = prereq[id].orEmpty().any { cycle(it.toId) }; visiting -= id; visited += id; return found }
        if (graph.nodes.any { cycle(it.id) }) errors += "Mandatory prerequisite cycle."
        graph.nodes.filter { it.type == KnowledgeNodeType.CONCEPT }.forEach { concept -> if (graph.edges.none { it.fromId == concept.id && it.relation == KnowledgeRelation.TESTED_BY_QUESTION }) errors += "Concept ${concept.id} has no assessment link." }
        graph.nodes.filter { it.type == KnowledgeNodeType.MISCONCEPTION }.forEach { misconception -> if (graph.edges.none { it.fromId == misconception.id && it.relation == KnowledgeRelation.REMEDIATES_MISCONCEPTION }) errors += "Misconception ${misconception.id} has no remediation link." }
        return GraphValidationReport(errors.distinct())
    }
}

interface MasteryEngine { fun process(state: LearnerConceptState, event: LearnerEvent, policy: MasteryPolicy): LearnerConceptState; fun derive(state: LearnerConceptState, policy: MasteryPolicy): ConceptMasteryState }
class DeterministicMasteryEngine : MasteryEngine {
    override fun process(state: LearnerConceptState, event: LearnerEvent, policy: MasteryPolicy): LearnerConceptState {
        require(event.conceptId == state.conceptId)
        var s = state.copy(lastInteractionAt = event.occurredAt)
        when (event) {
            is LearnerEvent.LessonStarted -> s = s.copy(exposureCount = s.exposureCount + 1, lessonStarted = true)
            is LearnerEvent.LessonCompleted -> s = s.copy(lessonCompletionCount = s.lessonCompletionCount + 1)
            is LearnerEvent.VisualisationInteracted -> s = s.copy(visualisationOpenCount = s.visualisationOpenCount + 1, meaningfulInteractionCount = s.meaningfulInteractionCount + if (event.meaningful) 1 else 0, guidedActivityCount = s.guidedActivityCount + if (event.guided) 1 else 0, exploreModeCount = s.exploreModeCount + if (event.explore) 1 else 0)
            is LearnerEvent.HintRequested -> { val h=s.hintUsage; s=s.copy(hintUsage=h.copy(requests=h.requests+1,maximumLevel=maxOf(h.maximumLevel?:event.level,event.level),fullSolutionCount=h.fullSolutionCount+if(event.level==HintLevel.FULL_EXPLANATION)1 else 0)) }
            is LearnerEvent.ChallengeCompleted -> s = s.copy(challengeAttemptCount=s.challengeAttemptCount+1,challengeCompletionCount=s.challengeCompletionCount+1,masteryEvidence=s.masteryEvidence+evidence(s,event.occurredAt,MasteryEvidenceType.APPLICATION_TASK,1.0,if(event.independent)1.0 else .5,null,event.activityId))
            is LearnerEvent.PredictionSubmitted -> s = s.copy(masteryEvidence=s.masteryEvidence+evidence(s,event.occurredAt,MasteryEvidenceType.SIMULATION_PREDICTION,if(event.correct)1.0 else 0.0,1.0,event.confidence,event.activityId))
            is LearnerEvent.PracticeAnswered -> {
                val first=if(s.practiceAttemptCount==0)if(event.result.valid)1.0 else 0.0 else s.firstAttemptAccuracy
                val ev=evidence(s,event.occurredAt,if(event.result.errorType==LearnerErrorType.GRAPH_INTERPRETATION)MasteryEvidenceType.DATA_INTERPRETATION else MasteryEvidenceType.CONCEPT_QUESTION,event.result.score,event.independence.score,event.confidence,event.questionId)
                val all=(s.masteryEvidence+ev).takeLast(10);val avg=all.map{it.score}.average();val oldN=s.practiceAttemptCount
                s=s.copy(practiceAttemptCount=oldN+1,firstAttemptAccuracy=first,rollingAccuracy=avg,averageResponseTimeMs=if(s.averageResponseTimeMs==null)event.responseTimeMs else ((s.averageResponseTimeMs*oldN+event.responseTimeMs)/(oldN+1)),masteryEvidence=s.masteryEvidence+ev)
            }
            is LearnerEvent.ReviewCompleted -> {
                val ev=evidence(s,event.occurredAt,MasteryEvidenceType.DELAYED_RETRIEVAL,if(event.correct)1.0 else 0.0,event.independence.score,event.confidence,"review")
                val r=s.retentionState;s=s.copy(successfulRetrievalCount=s.successfulRetrievalCount+if(event.correct)1 else 0,masteryEvidence=s.masteryEvidence+ev,lastSuccessfulRetrievalAt=if(event.correct)event.occurredAt else s.lastSuccessfulRetrievalAt,retentionState=r.copy(successfulDelayedRetrievals=r.successfulDelayedRetrievals+if(event.correct)1 else 0,failedDelayedRetrievals=r.failedDelayedRetrievals+if(event.correct)0 else 1))
            }
            is LearnerEvent.ConfidenceSubmitted -> s=s.copy(confidenceSummary=DeterministicConfidenceCalibrationEngine.update(s.confidenceSummary,event.observation))
        }
        val quality=when{s.masteryEvidence.count{it.effectiveScore>=.7}>=3->EvidenceQuality.HIGH;s.masteryEvidence.isNotEmpty()->EvidenceQuality.MODERATE;else->EvidenceQuality.LOW}
        return s.copy(evidenceQuality=quality,masteryState=derive(s,policy))
    }
    private fun evidence(s:LearnerConceptState,at:Instant,type:MasteryEvidenceType,score:Double,independence:Double,confidence:LearnerConfidence?,source:String?)=MasteryEvidence("${s.conceptId}-${at.toEpochMilli()}-${s.masteryEvidence.size}",s.conceptId,type,score,independence,1.0-independence,confidence,at,source)
    override fun derive(state: LearnerConceptState, policy: MasteryPolicy): ConceptMasteryState {
        if(state.masteryState==ConceptMasteryState.MASTERED && (state.retentionState.failedDelayedRetrievals>0||state.retentionState.forgettingRisk>=.8))return ConceptMasteryState.NEEDS_REVIEW
        val independent=state.masteryEvidence.filter{it.independence>=.7&&it.score>=.7};val types=independent.map{it.type}.toSet();val delayed=independent.count{it.type==MasteryEvidenceType.DELAYED_RETRIEVAL};val application=independent.any{it.type in setOf(MasteryEvidenceType.APPLICATION_TASK,MasteryEvidenceType.DATA_INTERPRETATION,MasteryEvidenceType.EXPERIMENT_RESULT,MasteryEvidenceType.CONSTRUCTION_TASK)}
        val fullHintDependence=if(state.practiceAttemptCount==0)0.0 else state.hintUsage.fullSolutionCount.toDouble()/state.practiceAttemptCount
        val policyMet=independent.size>=policy.minimumIndependentEvidence&&types.containsAll(policy.requiredEvidenceTypes)&&delayed>=policy.minimumDelayedRetrievals&&(state.rollingAccuracy?:0.0)>=policy.minimumRollingAccuracy&&fullHintDependence<=policy.maximumFullHintDependence&&(!policy.requiresApplicationEvidence||application)
        if(policyMet)return ConceptMasteryState.MASTERED
        if(independent.size>=policy.minimumIndependentEvidence&&(state.rollingAccuracy?:0.0)>=policy.minimumRollingAccuracy)return ConceptMasteryState.PROFICIENT
        if(state.practiceAttemptCount>=2)return ConceptMasteryState.PRACTISING
        if(state.practiceAttemptCount>0||state.masteryEvidence.isNotEmpty())return ConceptMasteryState.LEARNING
        if(state.exposureCount>0||state.lessonCompletionCount>0||state.visualisationOpenCount>0)return ConceptMasteryState.INTRODUCED
        return ConceptMasteryState.NOT_STARTED
    }
}

interface ConfidenceCalibrationEngine { fun calibrate(observations: List<ConfidenceObservation>): ConfidenceCalibrationResult }
class DeterministicConfidenceCalibrationEngine : ConfidenceCalibrationEngine {
    override fun calibrate(observations:List<ConfidenceObservation>):ConfidenceCalibrationResult{if(observations.isEmpty())return ConfidenceCalibrationResult(0,0.0,0.0,0.0,"No confidence evidence yet.");val over=observations.count{!it.correct&&it.confidence==LearnerConfidence.VERY_SURE};val under=observations.count{it.correct&&it.confidence in setOf(LearnerConfidence.GUESSING,LearnerConfidence.UNSURE)};val accuracy=observations.count{it.correct}.toDouble()/observations.size;return ConfidenceCalibrationResult(observations.size,accuracy,over.toDouble()/observations.size,under.toDouble()/observations.size,"${observations.size} confidence checks: ${(accuracy*100).roundToInt()}% correct; $over confidently wrong and $under correct but unsure.")}
    companion object { fun update(summary:ConfidenceSummary,o:ConfidenceObservation):ConfidenceSummary{val value=o.confidence.ordinal/3.0;val n=summary.observations;val mean=((summary.meanConfidence?:0.0)*n+value)/(n+1);val calibrated=(o.correct&&o.confidence.ordinal>=2)||(!o.correct&&o.confidence.ordinal<=1);return summary.copy(observations=n+1,calibrated=summary.calibrated+if(calibrated)1 else 0,confidentlyWrong=summary.confidentlyWrong+if(!o.correct&&o.confidence==LearnerConfidence.VERY_SURE)1 else 0,meanConfidence=mean)} }
}

interface DiagnosticEngine { fun nextQuestion(session:DiagnosticSession,questions:List<DiagnosticQuestion>):DiagnosticQuestion?; fun result(session:DiagnosticSession,questions:List<DiagnosticQuestion>,calibration:ConfidenceCalibrationResult,prerequisites:(String)->List<String>):DiagnosticResult }
class DeterministicDiagnosticEngine(private val maximumQuestions:Int=10,private val duration:Duration=Duration.ofMinutes(5)):DiagnosticEngine{
 override fun nextQuestion(session:DiagnosticSession,questions:List<DiagnosticQuestion>):DiagnosticQuestion?{val stablePlacement=session.responses.size>=4&&kotlin.math.abs(session.responses.count{it.correct}.toDouble()/session.responses.size-.5)>=.4;if(session.responses.size>=maximumQuestions||stablePlacement||Duration.between(session.startedAt,Instant.now())>=duration)return null;val answered=session.responses.map{it.questionId}.toSet()+session.skippedQuestionIds;val recent=session.responses.takeLast(2);val shift=when{recent.size==2&&recent.all{it.correct}->1;recent.size==2&&recent.none{it.correct}->-1;else->0};val target=(session.selectedLevel.number+shift).coerceIn(7,12);return questions.filter{it.subject==session.subject&&it.id !in answered}.maxByOrNull{it.informationWeight-(kotlin.math.abs(it.classLevel.number-target)*.2)}}
 override fun result(session:DiagnosticSession,questions:List<DiagnosticQuestion>,calibration:ConfidenceCalibrationResult,prerequisites:(String)->List<String>):DiagnosticResult{val byId=questions.associateBy{it.id};val grouped=session.responses.groupBy{byId[it.questionId]?.conceptId};val strong=grouped.filterValues{r->r.size>=1&&r.count{it.correct}.toDouble()/r.size>=.7}.keys.filterNotNull().toSet();val weak=grouped.filterValues{r->r.any{!it.correct}}.keys.filterNotNull().toSet();val misconceptions=session.responses.flatMap{r->if(r.correct)emptyList() else listOfNotNull(r.selectedIndex?.let{byId[r.questionId]?.misconceptionByWrongIndex?.get(it)})}.toSet();val correctLevels=session.responses.filter{it.correct}.mapNotNull{byId[it.questionId]?.classLevel?.number};val estimate=((correctLevels.average().takeUnless{it.isNaN()}?:session.selectedLevel.number.toDouble()).roundToInt()).coerceIn(7,12);val level=com.indianservers.aiexplorer.curriculum.SchoolClassLevel.entries.single{it.number==estimate};val start=weak.firstOrNull()?:questions.firstOrNull{it.subject==session.subject&&it.classLevel==level}?.conceptId?:strong.first();val bridges=prerequisites(start).filterNot(strong::contains);val reason=if(misconceptions.isNotEmpty())"Start with $start because the diagnostic found ${misconceptions.joinToString()} despite successful responses in ${strong.joinToString().ifBlank{"other areas"}}." else "Start with $start because it is the earliest weak or unevidenced concept at the estimated Class $estimate operating level.";return DiagnosticResult(session.subject,level,strong,weak,misconceptions,start,bridges,calibration,listOf(reason,"Placement uses ${session.responses.size} responses and remains a recommendation, not a permanent label."))}
}

interface MisconceptionEngine { fun assess(definition:MisconceptionDefinition,evidence:List<MisconceptionEvidence>):MisconceptionAssessment }
class DeterministicMisconceptionEngine:MisconceptionEngine{override fun assess(definition:MisconceptionDefinition,evidence:List<MisconceptionEvidence>):MisconceptionAssessment{val active=evidence.filter{it.misconceptionId==definition.id&&!it.resolved};val weight=active.sumOf{it.weight*(if(it.confidence==LearnerConfidence.VERY_SURE)1.5 else 1.0)};val status=when{evidence.any{it.misconceptionId==definition.id&&it.resolved}->MisconceptionConfidence.RESOLVED;active.size<2->MisconceptionConfidence.POSSIBLE;weight>=2.5->MisconceptionConfidence.CONFIRMED;else->MisconceptionConfidence.LIKELY};return MisconceptionAssessment(definition,status,active.size,weight)}}
interface HintSelectionEngine{fun select(hints:List<HintDefinition>,state:LearnerConceptState,failedAttempts:Int,assessments:List<MisconceptionAssessment>,previousHintIds:Set<String>):HintSelection?}
class DeterministicHintSelectionEngine:HintSelectionEngine{override fun select(hints:List<HintDefinition>,state:LearnerConceptState,failedAttempts:Int,assessments:List<MisconceptionAssessment>,previousHintIds:Set<String>):HintSelection?{val suspected=assessments.filter{it.confidence in setOf(MisconceptionConfidence.LIKELY,MisconceptionConfidence.CONFIRMED)}.map{it.definition.id}.toSet();val desired=HintLevel.entries[(failedAttempts.coerceAtMost(4))];val hint=hints.filter{it.id !in previousHintIds}.sortedWith(compareByDescending<HintDefinition>{it.misconceptionIds.any(suspected::contains)}.thenBy{it.level.ordinal}).firstOrNull{it.level.ordinal>=desired.ordinal}?:hints.firstOrNull{it.id !in previousHintIds}?:return null;val independence=when(hint.level){HintLevel.NUDGE->EvidenceIndependence.SMALL_HINT;HintLevel.CONCEPT_CUE,HintLevel.VISUAL_FOCUS->EvidenceIndependence.VISUAL_GUIDANCE;HintLevel.PARTIAL_STEP->EvidenceIndependence.PARTIAL_WORKING;HintLevel.NEAR_COMPLETE_WORKING->EvidenceIndependence.NEAR_COMPLETE_WORKING;HintLevel.FULL_EXPLANATION->EvidenceIndependence.FULL_SOLUTION};return HintSelection(hint,independence,if(hint.misconceptionIds.any(suspected::contains))"Targets the learner's repeated error pattern." else "Least revealing unused hint appropriate after $failedAttempts failed attempt(s).")}}
interface WorkedExampleFadingEngine{fun select(example:FadedWorkedExample,state:LearnerConceptState,daysSincePractice:Long):FadeStage}
class DeterministicWorkedExampleFadingEngine:WorkedExampleFadingEngine{override fun select(example:FadedWorkedExample,state:LearnerConceptState,daysSincePractice:Long):FadeStage{val target=when{daysSincePractice>21||state.masteryState==ConceptMasteryState.NEEDS_REVIEW->FadeStage.ONE_STEP_MISSING;state.hintUsage.fullSolutionCount>0->FadeStage.ONE_STEP_MISSING;state.masteryState==ConceptMasteryState.MASTERED->FadeStage.INDEPENDENT;state.masteryState==ConceptMasteryState.PROFICIENT->FadeStage.METHOD_SELECTION;(state.rollingAccuracy?:0.0)>=.7->FadeStage.MULTIPLE_STEPS_MISSING;else->example.defaultStage};return if(target in example.supportedFadeStages)target else example.supportedFadeStages.minBy{ kotlin.math.abs(it.ordinal-target.ordinal)}}}

interface SpacedReviewEngine{fun schedule(state:LearnerConceptState,now:Instant,reviewCorrect:Boolean?=null,independent:Boolean=true,confidentlyWrong:Boolean=false):ScheduledReview}
class DeterministicSpacedReviewEngine:SpacedReviewEngine{override fun schedule(state:LearnerConceptState,now:Instant,reviewCorrect:Boolean?,independent:Boolean,confidentlyWrong:Boolean):ScheduledReview{val prior=state.retentionState.intervalDays.coerceAtLeast(1);val interval=when{confidentlyWrong->1;reviewCorrect==false->maxOf(1,prior/2);reviewCorrect==true&&independent->minOf(90,prior*2);reviewCorrect==true->minOf(30,prior+1);state.masteryState==ConceptMasteryState.MASTERED->maxOf(7,prior);else->prior};val at=now.plus(Duration.ofDays(interval.toLong()));val urgency=if(confidentlyWrong)ReviewUrgency.URGENT_REPAIR else ReviewUrgency.NOT_DUE;val reason=when{confidentlyWrong->"Confidently wrong retrieval requires rapid repair.";reviewCorrect==false->"Failed retrieval shortened the previous $prior-day interval to $interval day(s).";reviewCorrect==true&&independent->"Independent retrieval expanded the interval from $prior to $interval day(s).";else->"Scheduled from current mastery and evidence quality."};return ScheduledReview(state.conceptId,at,urgency,reason,MasteryEvidenceType.DELAYED_RETRIEVAL)}}

interface NextLessonEngine{fun recommend(states:Map<String,LearnerConceptState>,index:KnowledgeGraphIndex,activities:Map<String,String>,timeAvailableMinutes:Int,recentActivityIds:List<String>,now:Instant):LearningRecommendation?}
class DeterministicNextLessonEngine:NextLessonEngine{override fun recommend(states:Map<String,LearnerConceptState>,index:KnowledgeGraphIndex,activities:Map<String,String>,timeAvailableMinutes:Int,recentActivityIds:List<String>,now:Instant):LearningRecommendation?{data class Candidate(val id:String,val reason:RecommendationReason,val score:Double,val why:String);val candidates=states.values.flatMap{s->buildList{val missing=index.mandatoryPrerequisites(s.conceptId).filter{states[it]?.masteryState !in setOf(ConceptMasteryState.PROFICIENT,ConceptMasteryState.MASTERED)};if(missing.isNotEmpty()){val bridge=missing.first();add(Candidate(bridge,RecommendationReason.MISSING_PREREQUISITE,100.0,"$bridge is a mandatory prerequisite for ${s.conceptId}."))}else{if(s.nextReviewAt?.isBefore(now)==true)add(Candidate(s.conceptId,RecommendationReason.REVIEW_DUE,90.0,"Its evidence-based review date has passed."));if(s.confidenceSummary.confidentlyWrong>0)add(Candidate(s.conceptId,RecommendationReason.CONFIDENTLY_WRONG,95.0,"A confident incorrect response needs misconception repair."));if(s.misconceptionEvidence.count{!it.resolved}>=2)add(Candidate(s.conceptId,RecommendationReason.MISCONCEPTION_REPAIR,92.0,"Repeated evidence indicates a specific misconception pattern."));if(s.retentionState.forgettingRisk>=.75)add(Candidate(s.conceptId,RecommendationReason.LOW_RETENTION,80.0,"Forgetting risk is high despite earlier learning."));if(s.masteryEvidence.none{it.type in setOf(MasteryEvidenceType.APPLICATION_TASK,MasteryEvidenceType.DATA_INTERPRETATION)})add(Candidate(s.conceptId,RecommendationReason.LOW_APPLICATION_EVIDENCE,60.0,"Understanding is not yet supported by independent application evidence."));if(s.masteryState !in setOf(ConceptMasteryState.MASTERED,ConceptMasteryState.PROFICIENT))add(Candidate(s.conceptId,RecommendationReason.CONTINUE_LEARNING_PATH,50.0,"This is the next unresolved concept with satisfied prerequisites."))}}}.sortedByDescending{it.score};val chosen=candidates.firstOrNull()?:return null;val activity=activities[chosen.id]?.takeUnless{it in recentActivityIds.takeLast(2)};return LearningRecommendation(chosen.id,activity,chosen.reason,chosen.why,timeAvailableMinutes.coerceIn(3,20),chosen.score)}}
