package com.indianservers.aiexplorer
import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.learningintelligence.engine.*
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class LearningIntelligenceIntegrationTest{
 private val now=Instant.parse("2026-07-19T12:00:00Z")
 @Test fun diagnosticToRecommendedStart(){val qs=LearningIntelligenceCatalog.diagnosticQuestions.filter{it.subject==SchoolSubject.MATHEMATICS};val session=DiagnosticSession("math",SchoolSubject.MATHEMATICS,SchoolClassLevel.CLASS_9,now,qs.take(3).mapIndexed{i,q->DiagnosticResponse(q.id,if(i==0)1 else 0,i!=0,LearnerConfidence.FAIRLY_SURE,800,now)});val r=DeterministicDiagnosticEngine().result(session,qs,DeterministicConfidenceCalibrationEngine().calibrate(session.responses.map{ConfidenceObservation(it.questionId,it.confidence!!,it.correct,it.occurredAt)})){id->KnowledgeGraphIndex(LearningIntelligenceCatalog.graph).mandatoryPrerequisites(id)};assertTrue(r.recommendedStartConceptId.isNotBlank());assertTrue(r.evidenceSummary.isNotEmpty())}
 @Test fun practiceEventUpdatesMasteryAndCreatesErrorRepairFlow(){val service=LocalLearningIntelligenceService();val id="math-linear-equations";val result=ValidationResult(false,0.0,0,LearnerErrorType.ALGEBRAIC_TRANSFORMATION,"bad",setOf("math-move-change-sign"));val updated=service.emit(LearnerEvent.PracticeAnswered(id,"q",result,1200,EvidenceIndependence.INDEPENDENT,LearnerConfidence.VERY_SURE,now));assertEquals(1,updated.practiceAttemptCount);assertEquals(1,service.repository.misconceptionEvidence(id).size);val hint=service.hint(id,1)!!;assertTrue("math-move-change-sign" in hint.hint.misconceptionIds);val c=LearningIntelligenceCatalog.concepts.single{it.conceptId==id};val error=service.recordError(id,c.curriculumNodeId,"q","moved term",listOf(LearnerAnswerStep("x+2=5 -> x=5-2")),result,LearnerConfidence.VERY_SURE,listOf("Subtract 2 from both sides."),c.activityId,now);assertEquals(ErrorBookStatus.RETRY_SCHEDULED,error.status);assertEquals(ErrorBookStatus.RESOLVED,service.resolveError(error.id).status)}
 @Test fun confidentlyWrongReviewSchedulesRepairAndUpdatesRecommendation(){val service=LocalLearningIntelligenceService();val id="physics-velocity-acceleration";service.emit(LearnerEvent.ReviewCompleted(id,false,EvidenceIndependence.INDEPENDENT,LearnerConfidence.VERY_SURE,now));assertEquals(ReviewUrgency.URGENT_REPAIR,service.repository.review(id)!!.urgency);val recommendation=service.recommend(now=now)!!;assertTrue(recommendation.reason in setOf(RecommendationReason.MISSING_PREREQUISITE,RecommendationReason.CONFIDENTLY_WRONG,RecommendationReason.REVIEW_DUE))}
 @Test fun completedReviewCanRestoreEvidencePath(){val service=LocalLearningIntelligenceService();val id="biology-cell-structure";val before=service.state(id);val after=service.emit(LearnerEvent.ReviewCompleted(id,true,EvidenceIndependence.INDEPENDENT,LearnerConfidence.VERY_SURE,now));assertEquals(before.successfulRetrievalCount+1,after.successfulRetrievalCount);assertTrue(after.nextReviewAt!!.isAfter(now))}
}
