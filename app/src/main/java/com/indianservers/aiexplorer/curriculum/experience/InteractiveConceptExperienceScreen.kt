package com.indianservers.aiexplorer.curriculum.experience

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.curriculum.interaction.ActivityRunSnapshot
import com.indianservers.aiexplorer.curriculum.interaction.InteractiveActivityScreen

@Composable
fun InteractiveConceptExperienceScreen(conceptId: String, onProgress: (ConceptProgressState) -> Unit = {}) {
    val experience=remember(conceptId){ConceptExperienceRegistry.get(conceptId)?:error("No integrated concept experience for $conceptId")}
    var progress by remember(conceptId){mutableStateOf(ConceptProgressState(conceptId))};var focus by remember{mutableStateOf<String?>(null)};var preset by remember{mutableStateOf<String?>(null)};var presetNonce by remember{mutableIntStateOf(0)};var previousRun by remember{mutableStateOf<ActivityRunSnapshot?>(null)};var currentRun by remember{mutableStateOf<ActivityRunSnapshot?>(null)}
    fun publish(value:ConceptProgressState){progress=value;onProgress(value)}
    fun action(a:ContentVisualAction?){when(a){is ContentVisualAction.HighlightPart->{focus=a.partId};is ContentVisualAction.SelectVariable->{focus=a.variableId};is ContentVisualAction.SetSimulationPreset->{preset=a.presetId;presetNonce++};is ContentVisualAction.ResetVisualisation->{focus=null;preset=null;presetNonce++};is ContentVisualAction.FocusGraphRegion->{focus=a.rangeId};is ContentVisualAction.PlayProcessSegment->{focus=a.segmentId};is ContentVisualAction.ShowDiagramLayer->{focus=a.layerId};null->Unit}}
    val activity: @Composable (Boolean)->Unit={embedded->InteractiveActivityScreen(experience.visualisations.single().activityId,focus,preset,presetNonce,embedded){run->previousRun=currentRun;currentRun=run;publish(progress.copy(openedVisualisationIds=progress.openedVisualisationIds+experience.visualisations.single().id,controlChanges=run.interactionCount,predictionAttempts=if(run.prediction==null)progress.predictionAttempts else maxOf(1,progress.predictionAttempts),predictionCorrect=if(run.predictionCorrect==true)maxOf(1,progress.predictionCorrect) else progress.predictionCorrect,observationCount=if(run.observation==null)progress.observationCount else maxOf(1,progress.observationCount),completedChallengeIds=run.completedChallengeIds))}}
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if(maxWidth>=840.dp) Row(Modifier.fillMaxSize()){ConceptReadingPane(experience,progress,::publish,::action,Modifier.weight(.48f).fillMaxHeight());Column(Modifier.weight(.52f).fillMaxHeight()){activity(false);currentRun?.let{DynamicExplanationPanel(DynamicExplanationEngine.explain(com.indianservers.aiexplorer.curriculum.interaction.ReferenceActivityRegistry.definitions.single{d->d.activityId==experience.activities.single().activityId},previousRun,it))}}}
        else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ConceptHeader(experience,progress){publish(progress.copy(bookmarked=!progress.bookmarked))};experience.contentSections.forEach{section->ConceptSection(experience,section,progress,{publish(progress.copy(readSectionIds=progress.readSectionIds+section.id))},::action,{correct->publish(progress.copy(practiceAttempts=progress.practiceAttempts+1,practiceCorrect=progress.practiceCorrect+if(correct)1 else 0))});if(section.kind==ContentSectionKind.VISUALISE){activity(true);currentRun?.let{DynamicExplanationPanel(DynamicExplanationEngine.explain(com.indianservers.aiexplorer.curriculum.interaction.ReferenceActivityRegistry.definitions.single{d->d.activityId==experience.activities.single().activityId},previousRun,it))}}};ReviewCompletion(experience,progress){publish(progress.copy(reviewed=true))}}
    }
}

@Composable private fun ConceptReadingPane(e:InteractiveConceptExperience,p:ConceptProgressState,publish:(ConceptProgressState)->Unit,action:(ContentVisualAction?)->Unit,modifier:Modifier){Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ConceptHeader(e,p){publish(p.copy(bookmarked=!p.bookmarked))};e.contentSections.forEach{s->ConceptSection(e,s,p,{publish(p.copy(readSectionIds=p.readSectionIds+s.id))},action,{correct->publish(p.copy(practiceAttempts=p.practiceAttempts+1,practiceCorrect=p.practiceCorrect+if(correct)1 else 0))})};ReviewCompletion(e,p){publish(p.copy(reviewed=true))}}}

@Composable private fun ConceptHeader(e:InteractiveConceptExperience,p:ConceptProgressState,onBookmark:()->Unit){Text(e.title,style=MaterialTheme.typography.headlineMedium);Text("${e.subject.name.lowercase().replaceFirstChar{it.uppercase()}} · Classes ${e.classLevels.joinToString{it.number.toString()}} · ${e.unitAndChapter}");Text("About ${e.estimatedMinutes} minutes");Text(e.summary);Text("Objectives",style=MaterialTheme.typography.titleMedium);e.learningObjectives.forEach{Text("• ${it.statement}")};Text("Prerequisites: ${e.prerequisites.joinToString()}");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onBookmark){Text(if(p.bookmarked)"Bookmarked" else "Bookmark")};Text("${p.readSectionIds.size}/${e.contentSections.size} sections reviewed")}}

@Composable private fun ConceptSection(e:InteractiveConceptExperience,s:ConceptContentSection,p:ConceptProgressState,onRead:()->Unit,onAction:(ContentVisualAction?)->Unit,onPractice:(Boolean)->Unit){ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(s.title,style=MaterialTheme.typography.titleLarge);s.blocks.forEach{block->ContentBlock(block,onAction,onPractice)};TextButton(onClick=onRead,enabled=s.id !in p.readSectionIds){Text(if(s.id in p.readSectionIds)"✓ Read" else "Mark section read")}}}}

@Composable private fun ContentBlock(block:InteractiveContentBlock,onAction:(ContentVisualAction?)->Unit,onPractice:(Boolean)->Unit){when(block){
 is InteractiveContentBlock.TextWithHighlight->Column{Text(block.text);block.calloutLabel?.let{TextButton(onClick={onAction(block.action)}){Text(it)}}}
 is InteractiveContentBlock.InlineDiagram->Text("Interactive diagram: ${block.caption}")
 is InteractiveContentBlock.ManipulableModel->Text("Guided interaction: ${block.instruction}")
 is InteractiveContentBlock.LiveGraph->Text("Live graph: ${block.axisDescription}")
 is InteractiveContentBlock.ProcessStepper->Column{Text("Process steps",style=MaterialTheme.typography.titleMedium);block.steps.forEachIndexed{i,step->Text("${i+1}. ${step.action} → ${step.outputs.joinToString()}. ${step.explanation}")}}
 is InteractiveContentBlock.ComparisonExplorer->Text("Compare ${block.leftLabel} with ${block.rightLabel} using synchronized controls.")
 is InteractiveContentBlock.FormulaExplorer->Column{Text(block.formula,style=MaterialTheme.typography.titleMedium);block.variables.forEach{variable->AssistChip(onClick={onAction(ContentVisualAction.SelectVariable(variable.visualTargetId))},label={Text("${variable.symbol}: ${variable.meaning} ${variable.unit}")})}}
 is InteractiveContentBlock.PredictionPrompt->Text("Predict first: ${block.prompt}",style=MaterialTheme.typography.titleMedium)
 is InteractiveContentBlock.ObservationPrompt->Text("Observe: ${block.prompt}")
 is InteractiveContentBlock.MisconceptionTest->Column{Text("Misconception test",style=MaterialTheme.typography.titleMedium);Text("Tempting idea: ${block.incorrectIdea}");Text("Test: ${block.testInstruction}");Text("Evidence: ${block.observedEvidence}");Text("Correction: ${block.correction}")}
 is InteractiveContentBlock.PracticeTask->{var answer by remember(block.id){mutableStateOf("")};var feedback by remember(block.id){mutableStateOf<String?>(null)};Column{Text("Practice: ${block.prompt}");OutlinedTextField(answer,{answer=it},label={Text("Your answer")});Button(onClick={val correct=answer.trim().equals(block.expectedAnswer.trim(),true);feedback=if(correct)"Correct. ${block.explanation}" else "Review the model. Expected: ${block.expectedAnswer}. ${block.explanation}";onPractice(correct)},enabled=answer.isNotBlank()){Text("Check")};feedback?.let{Text(it)}}}
 is InteractiveContentBlock.ReflectionPrompt->Text("Reflect: ${block.prompt}")
 is InteractiveContentBlock.RevisionStrip->Text("Takeaway: ${block.takeaway}${block.formulaRecap?.let{" · $it"}.orEmpty()}")}}
@Composable private fun DynamicExplanationPanel(e:DynamicExplanation){ElevatedCard(Modifier.fillMaxWidth().padding(12.dp).semantics{contentDescription="Dynamic explanation. ${e.whatIsHappening}. ${e.whatChanged}. ${e.whyItChanged}"}){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("What is happening?",style=MaterialTheme.typography.titleMedium);Text(e.whatIsHappening);Text("What changed? ${e.whatChanged}");Text("Why? ${e.whyItChanged}");Text("Rule: ${e.governingRule}");Text("Notice: ${e.notice}");Text("Model limit: ${e.simplification}")}}}
@Composable private fun ReviewCompletion(e:InteractiveConceptExperience,p:ConceptProgressState,onReview:()->Unit){Button(onClick=onReview){Text(if(p.reviewed)"✓ Review recorded" else "Record review")};Text(if(p.isComplete(e))"Concept complete: reading, interaction, prediction, observation, challenge, practice and review evidence are present." else "Progress requires the policy-defined learning evidence; opening this page is not enough.")}
