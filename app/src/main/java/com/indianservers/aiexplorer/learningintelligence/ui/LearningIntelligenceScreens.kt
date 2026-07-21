package com.indianservers.aiexplorer.learningintelligence.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.engine.*
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.learningintelligence.persistence.*
import java.time.Instant
import kotlinx.coroutines.launch
import com.indianservers.aiexplorer.learningworkspace.ui.Phase2LearningWorkspace
import com.indianservers.aiexplorer.phase3.ui.Phase3PlatformWorkspace

@Composable fun DiagnosticOnboardingScreen(subjects:Set<SchoolSubject> = SchoolSubject.entries.toSet(),question:DiagnosticQuestion?,questionNumber:Int,responseCount:Int,onSelectSubject:(SchoolSubject)->Unit,onAnswer:(Int,LearnerConfidence?)->Unit,onSkip:()->Unit,result:DiagnosticResult?,onOpenRecommended:(String)->Unit){
 var selected by remember{mutableStateOf<SchoolSubject?>(null)};var answer by remember(question?.id){mutableStateOf<Int?>(null)};var confidence by remember(question?.id){mutableStateOf<LearnerConfidence?>(null)}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Five-minute learning diagnostic",style=MaterialTheme.typography.headlineSmall);Text("This estimates a useful starting point. It is a recommendation, never a permanent label.")}
  if(selected==null)item{subjects.forEach{s->Button({selected=s;onSelectSubject(s)},Modifier.fillMaxWidth()){Text(s.name.lowercase().replaceFirstChar{it.uppercase()})}}}
  else if(result==null&&question!=null)item{LinearProgressIndicator({responseCount/10f},Modifier.fillMaxWidth());Text("Question $questionNumber · ${question.cluster}");Text(question.prompt);question.options.forEachIndexed{i,label->FilterChip(answer==i,{answer=i},label={Text(label)},modifier=Modifier.fillMaxWidth())};if(question.asksConfidence){Text("How confident are you?");LearnerConfidence.entries.forEach{c->FilterChip(confidence==c,{confidence=c},label={Text(c.name.replace('_',' ').lowercase())})}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onSkip){Text("Skip")};Button({answer?.let{onAnswer(it,confidence)}},enabled=answer!=null){Text("Submit")}}}
  else if(result!=null)item{Text("Recommended starting point",style=MaterialTheme.typography.titleLarge);Text(result.recommendedStartConceptId);result.evidenceSummary.forEach{Text(it)};if(result.recommendedBridgeConceptIds.isNotEmpty())Text("Bridge first: ${result.recommendedBridgeConceptIds.joinToString()}");Button({onOpenRecommended(result.recommendedStartConceptId)}){Text("Open recommended lesson")}}
 }
}

@Composable fun LearningIntelligenceDashboard(recommendation:LearningRecommendation?,states:List<LearnerConceptState>,reviews:List<ScheduledReview>,misconceptions:List<MisconceptionAssessment>,onOpenConcept:(String)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("Your learning",style=MaterialTheme.typography.headlineSmall)}
  recommendation?.let{r->item{IntelligenceCard("Continue learning","${r.conceptId}\n${r.explanation}\nAbout ${r.expectedDurationMinutes} min"){onOpenConcept(r.conceptId)}}}
  if(reviews.isNotEmpty())item{Text("Due for review",style=MaterialTheme.typography.titleMedium)};items(reviews){r->IntelligenceCard(r.conceptId,r.reason){onOpenConcept(r.conceptId)}}
  if(misconceptions.isNotEmpty())item{Text("Patterns to repair",style=MaterialTheme.typography.titleMedium)};items(misconceptions){m->Text("${m.definition.title} · ${m.confidence.name.lowercase()}")}
  item{Text("Recent progress",style=MaterialTheme.typography.titleMedium)};items(states.sortedByDescending{it.lastInteractionAt}){s->Text("${s.conceptId}: ${s.masteryState.name.replace('_',' ').lowercase()}")}
 }
}

@Composable fun ConceptIntelligencePanel(state:LearnerConceptState,review:ScheduledReview?,onHint:()->Unit,onOpenErrorBook:()->Unit){Card(Modifier.fillMaxWidth().semantics{contentDescription="Mastery ${state.masteryState}; evidence ${state.evidenceQuality}"}){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Learning evidence",style=MaterialTheme.typography.titleMedium);Text("Mastery: ${state.masteryState.name.replace('_',' ')}");Text("Independent evidence: ${state.masteryEvidence.count{it.independence>=.7}}");Text(review?.reason?:"Review is not currently due.");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onHint){Text("Hint")};OutlinedButton(onOpenErrorBook){Text("Error book")}}}}}
@Composable fun HintLadderPanel(selection:HintSelection?,onRequestHint:()->Unit){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text("Hint ladder",style=MaterialTheme.typography.titleMedium);selection?.let{Text(it.hint.level.name.replace('_',' '));Text(it.hint.text);Text(it.reason)}?:Text("Start with the least revealing useful hint.");Button(onRequestHint){Text(if(selection==null)"Show a hint" else "Need a deeper hint")}}}}
@Composable fun ReviewQueuePanel(reviews:List<ScheduledReview>,onOpen:(String)->Unit){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Review queue",style=MaterialTheme.typography.titleMedium);reviews.forEach{r->IntelligenceCard(r.conceptId,"${r.urgency}: ${r.reason}"){onOpen(r.conceptId)}}}}
@Composable fun PersonalErrorBookScreen(entries:List<ErrorBookEntry>,onRetry:(ErrorBookEntry)->Unit,onResolve:(String)->Unit){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Personal error book",style=MaterialTheme.typography.headlineSmall);Text("Mistakes become scheduled repair tasks; raw internal scores stay hidden.")};items(entries){e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(e.conceptId,style=MaterialTheme.typography.titleMedium);Text("${e.errorType.name.replace('_',' ')} · ${e.status.name.replace('_',' ')}");e.misconceptionId?.let{Text("Pattern: $it")};Text("Corrected method:");e.correctedMethod.forEach{Text("• $it")};Row{Button({onRetry(e)}){Text("Retry")};Spacer(Modifier.width(8.dp));OutlinedButton({onResolve(e.id)}){Text("Mark resolved")}}}}}}
}
@Composable private fun IntelligenceCard(title:String,body:String,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(body)}}}

private enum class IntelligenceDestination { HOME, DIAGNOSTIC, CONCEPT, ERRORS, WORKSPACE, PLATFORM }
@Composable fun LearningIntelligenceFeatureRoot(onExit:()->Unit){
 val service=remember{LocalLearningIntelligenceService()};val diagnostic=remember{DeterministicDiagnosticEngine()};val confidenceEngine=remember{DeterministicConfidenceCalibrationEngine()};val index=remember{KnowledgeGraphIndex(LearningIntelligenceCatalog.graph)}
 val context=LocalContext.current.applicationContext;val persistence=remember(context){DataStoreLearningPersistence(context)};val scope=rememberCoroutineScope()
 var destination by remember{mutableStateOf(IntelligenceDestination.HOME)};var selectedConcept by remember{mutableStateOf<String?>(null)};var session by remember{mutableStateOf<DiagnosticSession?>(null)};var question by remember{mutableStateOf<DiagnosticQuestion?>(null)};var result by remember{mutableStateOf<DiagnosticResult?>(null)};var hint by remember{mutableStateOf<HintSelection?>(null)}
 fun persist(){scope.launch{val states=service.repository.all();val confidence=session?.responses.orEmpty().mapNotNull{r->r.confidence?.let{ConfidenceObservation(r.questionId,it,r.correct,r.occurredAt)}};persistence.save(LearningSnapshotMapper.snapshot(states,listOfNotNull(session),listOfNotNull(result),states.flatMap{it.masteryEvidence},states.flatMap{it.misconceptionEvidence},confidence,states.mapNotNull{service.repository.review(it.conceptId)},service.repository.current(),service.repository.entries()))}}
 LaunchedEffect(Unit){LearningSnapshotMapper.restoreStates(persistence.load()).forEach(service.repository::save)}
 fun finishOrAdvance(){val s=session?:return;val next=diagnostic.nextQuestion(s,LearningIntelligenceCatalog.diagnosticQuestions);question=next;if(next==null){val observations=s.responses.mapNotNull{r->r.confidence?.let{ConfidenceObservation(r.questionId,it,r.correct,r.occurredAt)}};result=diagnostic.result(s,LearningIntelligenceCatalog.diagnosticQuestions,confidenceEngine.calibrate(observations),index::mandatoryPrerequisites);service.repository.saveResult(result!!)}}
 Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({if(destination==IntelligenceDestination.HOME)onExit()else destination=IntelligenceDestination.HOME}){Text("Back")};Text("LOCAL LEARNING INTELLIGENCE",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp))}}
 when(destination){
  IntelligenceDestination.HOME->{val recommendation=service.recommend();Column{Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({destination=IntelligenceDestination.DIAGNOSTIC}){Text("Start diagnostic")};OutlinedButton({destination=IntelligenceDestination.ERRORS}){Text("Error book")}};Button({destination=IntelligenceDestination.WORKSPACE},Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp)){Text("Open local Tutor & Experiment Studio")};OutlinedButton({destination=IntelligenceDestination.PLATFORM},Modifier.fillMaxWidth().padding(horizontal=16.dp)){Text("Open Exam, Reports & Teacher Platform")};LearningIntelligenceDashboard(recommendation,service.repository.all(),service.repository.due(),emptyList()){selectedConcept=it;destination=IntelligenceDestination.CONCEPT}}}
  IntelligenceDestination.DIAGNOSTIC->DiagnosticOnboardingScreen(question=question,questionNumber=(session?.responses?.size?:0)+1,responseCount=session?.responses?.size?:0,onSelectSubject={subject->session=DiagnosticSession("diagnostic-${subject.name}-${System.currentTimeMillis()}",subject,com.indianservers.aiexplorer.curriculum.SchoolClassLevel.CLASS_9,Instant.now());service.repository.saveSession(session!!);finishOrAdvance();persist()},onAnswer={selected,confidence->question?.let{q->val response=DiagnosticResponse(q.id,selected,selected==q.correctIndex,confidence,0,Instant.now());session=session!!.copy(responses=session!!.responses+response);service.repository.saveSession(session!!);finishOrAdvance();persist()}},onSkip={question?.let{q->session=session!!.copy(skippedQuestionIds=session!!.skippedQuestionIds+q.id);finishOrAdvance();persist()}},result=result,onOpenRecommended={selectedConcept=it;destination=IntelligenceDestination.CONCEPT})
  IntelligenceDestination.CONCEPT->selectedConcept?.let{id->Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(id,style=MaterialTheme.typography.headlineSmall);ConceptIntelligencePanel(service.state(id),service.repository.review(id),{hint=service.hint(id,(hint?.hint?.level?.ordinal?:-1)+1);persist()},{destination=IntelligenceDestination.ERRORS});HintLadderPanel(hint){hint=service.hint(id,(hint?.hint?.level?.ordinal?:-1)+1);persist()}}}?:Text("Choose a concept.")
  IntelligenceDestination.ERRORS->PersonalErrorBookScreen(service.repository.entries(),{selectedConcept=it.conceptId;destination=IntelligenceDestination.CONCEPT},{service.resolveError(it);persist()})
  IntelligenceDestination.WORKSPACE->Phase2LearningWorkspace{destination=IntelligenceDestination.HOME}
  IntelligenceDestination.PLATFORM->Phase3PlatformWorkspace{destination=IntelligenceDestination.HOME}
 }
}
