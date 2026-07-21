package com.indianservers.aiexplorer.learningintelligence.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.model.*

private val Context.learningIntelligenceDataStore by preferencesDataStore("learning_intelligence")
data class PersistenceRecord(val type:String,val id:String,val fields:Map<String,String>)
data class LocalLearningSnapshot(val schemaVersion:Int=2,val records:List<PersistenceRecord>)
interface LearningPersistence { suspend fun load():LocalLearningSnapshot;suspend fun save(snapshot:LocalLearningSnapshot) }

class DataStoreLearningPersistence(private val context:Context):LearningPersistence{
 private val version=intPreferencesKey("schema_version");private val payload=stringPreferencesKey("records")
 override suspend fun load():LocalLearningSnapshot{val p=context.learningIntelligenceDataStore.data.first();val raw=p[payload].orEmpty();val snapshot=LocalLearningSnapshot(p[version]?:1,raw.lineSequence().filter{it.isNotBlank()}.map(::decode).toList());return LearningPersistenceMigration.migrate(snapshot)}
 override suspend fun save(snapshot:LocalLearningSnapshot){context.learningIntelligenceDataStore.edit{it[version]=snapshot.schemaVersion;it[payload]=snapshot.records.joinToString("\n",transform=::encode)}}
 private fun encode(r:PersistenceRecord)=listOf(r.type,r.id,r.fields.entries.joinToString("&"){"${escape(it.key)}=${escape(it.value)}"}).joinToString("|"){escape(it)}
 private fun decode(raw:String):PersistenceRecord{val p=raw.split('|').map(::unescape);val fields=p.getOrElse(2){""}.split('&').filter{it.contains('=')}.associate{val i=it.indexOf('=');unescape(it.substring(0,i)) to unescape(it.substring(i+1))};return PersistenceRecord(p[0],p[1],fields)}
 private fun escape(s:String)=URLEncoder.encode(s,StandardCharsets.UTF_8.name())
 private fun unescape(s:String)=URLDecoder.decode(s,StandardCharsets.UTF_8.name())
}
object LearningPersistenceMigration{
 const val CURRENT_VERSION=2
 fun migrate(old:LocalLearningSnapshot):LocalLearningSnapshot{if(old.schemaVersion>=CURRENT_VERSION)return old;val migrated=old.records.map{record->if(record.type=="concept-state")record.copy(fields=mapOf("evidenceQuality" to "LOW","masteryState" to "INTRODUCED")+record.fields) else record};return LocalLearningSnapshot(CURRENT_VERSION,migrated)}
}

object LearningSnapshotMapper{
 fun snapshot(states:List<LearnerConceptState>,sessions:List<DiagnosticSession>,results:List<DiagnosticResult>,mastery:List<MasteryEvidence>,misconceptions:List<MisconceptionEvidence>,confidence:List<ConfidenceObservation>,reviews:List<ScheduledReview>,recommendations:List<LearningRecommendation>,errors:List<ErrorBookEntry>):LocalLearningSnapshot{
  val records=buildList{
   states.forEach{s->add(PersistenceRecord("concept-state",s.conceptId,mapOf("subject" to s.subject.name,"curriculum" to s.curriculumNodeIds.joinToString(","),"exposure" to s.exposureCount.toString(),"lessonStarted" to s.lessonStarted.toString(),"lessons" to s.lessonCompletionCount.toString(),"visuals" to s.visualisationOpenCount.toString(),"interactions" to s.meaningfulInteractionCount.toString(),"guided" to s.guidedActivityCount.toString(),"explore" to s.exploreModeCount.toString(),"challengeAttempts" to s.challengeAttemptCount.toString(),"challengeCompleted" to s.challengeCompletionCount.toString(),"practice" to s.practiceAttemptCount.toString(),"retrievals" to s.successfulRetrievalCount.toString(),"firstAccuracy" to s.firstAttemptAccuracy?.toString().orEmpty(),"accuracy" to s.rollingAccuracy?.toString().orEmpty(),"responseTime" to s.averageResponseTimeMs?.toString().orEmpty(),"confidenceObservations" to s.confidenceSummary.observations.toString(),"confidenceCalibrated" to s.confidenceSummary.calibrated.toString(),"confidentlyWrong" to s.confidenceSummary.confidentlyWrong.toString(),"meanConfidence" to s.confidenceSummary.meanConfidence?.toString().orEmpty(),"masteryState" to s.masteryState.name,"evidenceQuality" to s.evidenceQuality.name,"successfulDelayed" to s.retentionState.successfulDelayedRetrievals.toString(),"failedDelayed" to s.retentionState.failedDelayedRetrievals.toString(),"intervalDays" to s.retentionState.intervalDays.toString(),"forgettingRisk" to s.retentionState.forgettingRisk.toString(),"lastRetrieval" to s.lastSuccessfulRetrievalAt?.toString().orEmpty(),"lastInteraction" to s.lastInteractionAt?.toString().orEmpty(),"nextReview" to s.nextReviewAt?.toString().orEmpty())));add(PersistenceRecord("hint-usage",s.conceptId,mapOf("requests" to s.hintUsage.requests.toString(),"maximum" to s.hintUsage.maximumLevel?.name.orEmpty(),"full" to s.hintUsage.fullSolutionCount.toString())))}
   sessions.forEach{s->add(PersistenceRecord("diagnostic-session",s.id,mapOf("subject" to s.subject.name,"level" to s.selectedLevel.name,"started" to s.startedAt.toString(),"responses" to s.responses.joinToString("\u001e"){listOf(it.questionId,it.selectedIndex?.toString().orEmpty(),it.correct.toString(),it.confidence?.name.orEmpty(),it.responseTimeMs.toString(),it.occurredAt.toString()).joinToString("\u001f")},"skipped" to s.skippedQuestionIds.joinToString(","))))}
   results.forEach{r->add(PersistenceRecord("diagnostic-result",r.subject.name,mapOf("level" to r.estimatedLevel.name,"strong" to r.strongConceptIds.joinToString(","),"weak" to r.weakConceptIds.joinToString(","),"misconceptions" to r.suspectedMisconceptionIds.joinToString(","),"start" to r.recommendedStartConceptId,"bridges" to r.recommendedBridgeConceptIds.joinToString(","),"calibration" to listOf(r.confidenceCalibration.observations,r.confidenceCalibration.accuracy,r.confidenceCalibration.overconfidenceRate,r.confidenceCalibration.underconfidenceRate,r.confidenceCalibration.explanation).joinToString("\u001f"),"summary" to r.evidenceSummary.joinToString("\u001f"))))}
   mastery.forEach{e->add(PersistenceRecord("mastery-evidence",e.id,mapOf("concept" to e.conceptId,"type" to e.type.name,"score" to e.score.toString(),"independence" to e.independence.toString(),"hintPenalty" to e.hintPenalty.toString(),"confidence" to e.confidence?.name.orEmpty(),"source" to e.sourceActivityId.orEmpty(),"at" to e.occurredAt.toString())))}
   misconceptions.forEach{e->add(PersistenceRecord("misconception-evidence","${e.misconceptionId}-${e.occurredAt.toEpochMilli()}",mapOf("misconception" to e.misconceptionId,"concept" to e.conceptId,"weight" to e.weight.toString(),"confidence" to (e.confidence?.name.orEmpty()),"resolved" to e.resolved.toString(),"at" to e.occurredAt.toString())))}
   confidence.forEach{e->add(PersistenceRecord("confidence-observation","${e.questionId}-${e.occurredAt.toEpochMilli()}",mapOf("question" to e.questionId,"confidence" to e.confidence.name,"correct" to e.correct.toString(),"at" to e.occurredAt.toString())))}
   reviews.forEach{r->add(PersistenceRecord("scheduled-review",r.conceptId,mapOf("at" to r.scheduledAt.toString(),"urgency" to r.urgency.name,"reason" to r.reason,"type" to r.preferredEvidenceType.name)))}
   recommendations.forEach{r->add(PersistenceRecord("recommendation",r.conceptId,mapOf("activity" to r.activityId.orEmpty(),"reason" to r.reason.name,"explanation" to r.explanation,"minutes" to r.expectedDurationMinutes.toString(),"score" to r.priorityScore.toString())))}
   errors.forEach{e->add(PersistenceRecord("error-book",e.id,mapOf("concept" to e.conceptId,"curriculum" to e.curriculumNodeId,"question" to e.questionId.orEmpty(),"answer" to e.learnerAnswer,"steps" to e.learnerSteps.joinToString("\u001e"){it.expression+"\u001f"+it.explanation.orEmpty()},"firstInvalid" to e.firstInvalidStepIndex?.toString().orEmpty(),"error" to e.errorType.name,"misconception" to e.misconceptionId.orEmpty(),"confidence" to e.confidence?.name.orEmpty(),"hintDepth" to e.hintDepth?.name.orEmpty(),"status" to e.status.name,"retry" to e.retryAt?.toString().orEmpty(),"remediation" to e.remediationActivityId.orEmpty(),"corrected" to e.correctedMethod.joinToString("\u001f"))))}
  };return LocalLearningSnapshot(LearningPersistenceMigration.CURRENT_VERSION,records)
 }
 fun restoreStates(snapshot:LocalLearningSnapshot):List<LearnerConceptState>{val hints=snapshot.records.filter{it.type=="hint-usage"}.associateBy{it.id};return snapshot.records.filter{it.type=="concept-state"}.map{r->val f=r.fields;val h=hints[r.id]?.fields.orEmpty();LearnerConceptState(r.id,f["curriculum"].orEmpty().split(',').filter{it.isNotBlank()}.toSet(),SchoolSubject.valueOf(f.getValue("subject")),exposureCount=f["exposure"]?.toIntOrNull()?:0,lessonCompletionCount=f["lessons"]?.toIntOrNull()?:0,practiceAttemptCount=f["practice"]?.toIntOrNull()?:0,rollingAccuracy=f["accuracy"]?.toDoubleOrNull(),hintUsage=HintUsageSummary(h["requests"]?.toIntOrNull()?:0,h["maximum"]?.takeIf{it.isNotBlank()}?.let(HintLevel::valueOf),h["full"]?.toIntOrNull()?:0),masteryState=f["masteryState"]?.let(ConceptMasteryState::valueOf)?:ConceptMasteryState.NOT_STARTED,evidenceQuality=f["evidenceQuality"]?.let(EvidenceQuality::valueOf)?:EvidenceQuality.LOW,lastInteractionAt=f["lastInteraction"]?.takeIf{it.isNotBlank()}?.let(java.time.Instant::parse),nextReviewAt=f["nextReview"]?.takeIf{it.isNotBlank()}?.let(java.time.Instant::parse))}}
}
