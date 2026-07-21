package com.indianservers.aiexplorer.phase3.delivery

import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.phase3.governance.ReviewStatus
import java.time.Instant

enum class AdaptiveWindowClass{COMPACT,MEDIUM,EXPANDED,LANDSCAPE_LAB}
data class AdaptiveLearningLayout(val windowClass:AdaptiveWindowClass,val panes:List<String>,val bottomSheetControls:Boolean,val persistentTools:Boolean)
data class RestorableLearningUiState(val selectedConceptId:String?,val scrollAnchor:String?,val simulationStateId:String?,val notebookEntryId:String?,val tutorState:String?,val examSessionId:String?)
object AdaptiveLayoutPolicy{fun classify(widthDp:Int,heightDp:Int)=when{widthDp>=840->AdaptiveWindowClass.EXPANDED;widthDp>heightDp&&widthDp>=700->AdaptiveWindowClass.LANDSCAPE_LAB;widthDp>=600->AdaptiveWindowClass.MEDIUM;else->AdaptiveWindowClass.COMPACT};fun layout(c:AdaptiveWindowClass)=when(c){AdaptiveWindowClass.COMPACT->AdaptiveLearningLayout(c,listOf("active"),true,false);AdaptiveWindowClass.MEDIUM->AdaptiveLearningLayout(c,listOf("lesson","visualisation"),false,false);AdaptiveWindowClass.EXPANDED->AdaptiveLearningLayout(c,listOf("curriculum","lesson","visualisation","support"),false,true);AdaptiveWindowClass.LANDSCAPE_LAB->AdaptiveLearningLayout(c,listOf("simulation","formula-graph","notebook","controls"),false,true)};fun restoreAcrossResize(state:RestorableLearningUiState,newClass:AdaptiveWindowClass)=state to layout(newClass)}

enum class TermDisplayMode{REGIONAL,ENGLISH,BOTH}
data class ScientificTermTranslation(val termId:String,val locale:String,val translatedTerm:String,val transliteration:String?,val englishFallback:String,val contextNotes:List<String>,val reviewStatus:ReviewStatus)
data class LocalizedLearningText(val contentId:String,val locale:String,val text:String,val reviewStatus:ReviewStatus,val formulaTokens:Set<String> = emptySet())
object IndianLanguageLayer{
 val supportedLocales=setOf("en-IN","hi-IN","te-IN")
 val terms=listOf(ScientificTermTranslation("velocity","hi-IN","वेग","veg","velocity",listOf("Physics vector quantity"),ReviewStatus.APPROVED),ScientificTermTranslation("velocity","te-IN","వేగం","vēgaṁ","velocity",listOf("Physics vector quantity"),ReviewStatus.APPROVED),ScientificTermTranslation("equation","hi-IN","समीकरण","sameekaran","equation",emptyList(),ReviewStatus.APPROVED),ScientificTermTranslation("equation","te-IN","సమీకరణం","samīkaraṇaṁ","equation",emptyList(),ReviewStatus.APPROVED),ScientificTermTranslation("cell","hi-IN","कोशिका","koshika","cell",listOf("Biological cell"),ReviewStatus.APPROVED),ScientificTermTranslation("cell","te-IN","కణం","kaṇaṁ","cell",listOf("Biological cell"),ReviewStatus.APPROVED))
 fun display(termId:String,locale:String,mode:TermDisplayMode):String{val t=terms.singleOrNull{it.termId==termId&&it.locale==locale&&it.reviewStatus==ReviewStatus.APPROVED}?:return terms.firstOrNull{it.termId==termId}?.englishFallback?:termId;return when(mode){TermDisplayMode.REGIONAL->t.translatedTerm;TermDisplayMode.ENGLISH->t.englishFallback;TermDisplayMode.BOTH->"${t.translatedTerm} (${t.englishFallback})"}}
 fun releasable(text:LocalizedLearningText)=text.locale in supportedLocales&&text.reviewStatus==ReviewStatus.APPROVED
 fun preservesNotation(source:String,translated:String,tokens:Set<String>)=tokens.all{it in source&&it in translated}
}

enum class SyncOperationType{CREATE,UPDATE,DELETE_TOMBSTONE,APPEND}
enum class SyncStatus{PENDING,IN_FLIGHT,RETRY,COMPLETED,PERMANENT_FAILURE}
data class PendingSyncOperation(val id:String,val entityType:String,val entityId:String,val operation:SyncOperationType,val payloadVersion:Int,val createdAt:Instant,val retryCount:Int,val status:SyncStatus,val minimizedPayload:Map<String,String>)
data class VersionedNote(val id:String,val text:String,val version:Int,val editedAt:Instant)
data class BookmarkState(val activeIds:Set<String>,val tombstones:Set<String>)
data class ImmutableSubmission(val id:String,val version:Int,val payloadHash:String,val createdAt:Instant)
data class SyncConflictResult<T>(val value:T,val preservedVersions:Int,val explanation:String)
class OfflineFirstSyncEngine{
 private val queue=mutableListOf<PendingSyncOperation>();fun enqueue(op:PendingSyncOperation){require(op.minimizedPayload.keys.none{it.contains("api",true)||it.contains("audio",true)||it.contains("image",true)});queue+=op};fun pending()=queue.filter{it.status in setOf(SyncStatus.PENDING,SyncStatus.RETRY)}
 fun mergeEvidence(local:List<String>,remote:List<String>)=SyncConflictResult((local+remote).distinct(),local.size+remote.size,"Evidence is merged by stable ID; neither side is discarded.")
 fun mergeNotes(local:VersionedNote,remote:VersionedNote)=SyncConflictResult(if(local.editedAt>=remote.editedAt)local else remote,2,"Latest edit is active and both versions remain in history.")
 fun mergeBookmarks(local:BookmarkState,remote:BookmarkState):SyncConflictResult<BookmarkState>{val tomb=local.tombstones+remote.tombstones;return SyncConflictResult(BookmarkState((local.activeIds+remote.activeIds)-tomb,tomb),2,"Union with explicit deletion tombstones.")}
 fun mergeNotebookEntries(local:Map<String,String>,remote:Map<String,String>):SyncConflictResult<Map<String,String>>{val merged=local.toMutableMap();remote.forEach{(id,value)->when{!merged.containsKey(id)->merged[id]=value;merged[id]==value->Unit;else->{var suffix=1;var preservedId="$id@remote-$suffix";while(merged.containsKey(preservedId)){suffix++;preservedId="$id@remote-$suffix"};merged[preservedId]=value}}};return SyncConflictResult(merged,local.size+remote.size,"Entries merge by ID; local conflicts remain and remote versions are appended separately.")}
 fun submissionVersions(local:List<ImmutableSubmission>,remote:List<ImmutableSubmission>)=SyncConflictResult((local+remote).distinctBy{it.id to it.version}.sortedBy{it.version},local.size+remote.size,"Submission versions are immutable.")
}

enum class PerformanceQualityLevel{HIGH,BALANCED,LOW,ACCESSIBILITY_STABLE}
data class PerformanceSignals(val frameTimeMs:Double,val jankPercent:Double,val lowMemory:Boolean,val thermalSevere:Boolean,val batteryLow:Boolean,val accessibilityStableRequested:Boolean)
data class RenderQuality(val level:PerformanceQualityLevel,val particleCount:Int,val trailLength:Int,val shadow:Boolean,val blur:Boolean,val glow:Boolean,val samplingDensity:Int,val animationHz:Int,val graphRefreshHz:Int)
class PerformanceQualityGovernor{
 fun decide(s:PerformanceSignals)=when{ s.accessibilityStableRequested->PerformanceQualityLevel.ACCESSIBILITY_STABLE;s.lowMemory||s.thermalSevere||s.frameTimeMs>28||s.jankPercent>10->PerformanceQualityLevel.LOW;s.batteryLow||s.frameTimeMs>18||s.jankPercent>4->PerformanceQualityLevel.BALANCED;else->PerformanceQualityLevel.HIGH}
 fun quality(level:PerformanceQualityLevel)=when(level){PerformanceQualityLevel.HIGH->RenderQuality(level,240,80,true,true,true,160,60,30);PerformanceQualityLevel.BALANCED->RenderQuality(level,120,40,true,false,false,100,45,20);PerformanceQualityLevel.LOW->RenderQuality(level,50,12,false,false,false,60,24,12);PerformanceQualityLevel.ACCESSIBILITY_STABLE->RenderQuality(level,70,0,false,false,false,80,0,10)}
 fun <T> preserveScientificOutput(calculation:()->T,signals:PerformanceSignals)=calculation() to quality(decide(signals))
}

enum class BenchmarkJourney{COLD_LAUNCH,MAIN_DASHBOARD,OPEN_SUBJECT,OPEN_NCERT,OPEN_CONCEPT,OPEN_FORMULA_LIBRARY,OPEN_PERIODIC_TABLE,LAUNCH_SIMULATION,OPEN_NOTEBOOK,OPEN_EXAM,OPEN_TEACHER_DASHBOARD,NAVIGATION,SCROLLING,GRAPH_INTERACTION,QUESTION_NAVIGATION,EXAM_SUBMISSION,LARGE_DATASET}
data class BenchmarkMeasurement(val journey:BenchmarkJourney,val timeToInitialDisplayMs:Double?,val timeToFullDisplayMs:Double?,val frameP95Ms:Double?,val jankPercent:Double?,val memoryMb:Double?,val measuredOn:String,val measuredAt:Instant)
object PerformanceBenchmarkRegistry{val required=BenchmarkJourney.entries.toSet();fun report(measurements:List<BenchmarkMeasurement>)=required.associateWith{journey->measurements.filter{it.journey==journey}}}
