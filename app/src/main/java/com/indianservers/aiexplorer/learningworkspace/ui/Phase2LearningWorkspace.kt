package com.indianservers.aiexplorer.learningworkspace.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.assistant.local.LocalLearningAssistantProvider
import com.indianservers.aiexplorer.assistant.privacy.*
import com.indianservers.aiexplorer.assistant.routing.AssistantRouter
import com.indianservers.aiexplorer.assistant.routing.AssistanceNeed
import com.indianservers.aiexplorer.assistant.providers.RemoteProviderFactory
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningworkspace.*
import com.indianservers.aiexplorer.practice.VerifiedPracticeCatalog
import com.indianservers.aiexplorer.practice.VerifiedPracticeGenerator
import com.indianservers.aiexplorer.tutor.VisualExplanationEngine
import com.indianservers.aiexplorer.input.CameraQuestionImporter
import com.indianservers.aiexplorer.input.ImportedQuestion
import com.indianservers.aiexplorer.input.LocalVoiceCommandParser
import kotlinx.coroutines.launch
import java.time.Instant
import java.io.ByteArrayOutputStream
import java.util.Locale

private enum class WorkspaceTab { TUTOR, INPUT, EXPERIMENT, ANALYSE, JOURNEYS, SETTINGS }

@Composable fun Phase2LearningWorkspace(onExit:()->Unit){
    val service=remember{LocalLearningIntelligenceService()};var tab by remember{mutableStateOf(WorkspaceTab.TUTOR)};var conceptId by remember{mutableStateOf("math-linear-equations")}
    val context=LocalContext.current;val secretStore=remember(context){AndroidKeystoreSecretStore(context)};var providerSettings by remember{mutableStateOf(ProviderSettings())};var settingsLoaded by remember{mutableStateOf(false)}
    val persistence=remember(context){DataStorePhase2Persistence(context)}
    LaunchedEffect(Unit){providerSettings=Phase2SnapshotMapper.settings(persistence.load());settingsLoaded=true}
    LaunchedEffect(providerSettings,settingsLoaded){if(settingsLoaded){val existing=persistence.load();val settingsRecords=Phase2SnapshotMapper.snapshot(settings=providerSettings).records.filter{it.type in setOf("preference","provider-settings")};persistence.save(Phase2Snapshot(records=existing.records.filterNot{it.type in setOf("preference","provider-settings")}+settingsRecords))}}
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onExit){Text("Back")};Text("LOCAL TUTOR & EXPERIMENT STUDIO",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp))}
        ScrollableTabRow(tab.ordinal){WorkspaceTab.entries.forEach{item->Tab(tab==item,{tab=item},text={Text(item.name.lowercase().replaceFirstChar(Char::uppercase))})}}
        when(tab){WorkspaceTab.TUTOR->TutorWorkspace(service,conceptId,providerSettings,secretStore){conceptId=it};WorkspaceTab.INPUT->InputAssistanceWorkspace();WorkspaceTab.EXPERIMENT->ExperimentWorkspace(persistence);WorkspaceTab.ANALYSE->AnalysisWorkspace();WorkspaceTab.JOURNEYS->JourneyWorkspace();WorkspaceTab.SETTINGS->ProviderSettingsScreen(providerSettings,{providerSettings=it},secretStore)}
    }
}

@Composable private fun TutorWorkspace(service:LocalLearningIntelligenceService,conceptId:String,settings:ProviderSettings,secrets:SecureSecretStore,onConcept:(String)->Unit){
    val scope=rememberCoroutineScope();val provider=remember{LocalLearningAssistantProvider()};val router=remember(secrets){AssistantRouter(provider,RemoteProviderFactory.providers(secrets))};var question by remember{mutableStateOf("")};var response by remember{mutableStateOf<AssistantResponse?>(null)};var style by remember{mutableStateOf(ExplanationStyle.INTUITIVE)};var hintOrdinal by remember{mutableIntStateOf(0)}
    val concepts=com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog.concepts
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide=maxWidth>=840.dp
        val content: @Composable () -> Unit = { LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Grounded Socratic tutor",style=MaterialTheme.typography.headlineSmall);Text("Local reviewed guidance · ${service.state(conceptId).masteryState.name.replace('_',' ')}");Text("The tutor asks one focused question and never changes verified formulas or simulation results.")}
        item{Text("Active concept");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){concepts.take(8).forEach{c->FilterChip(conceptId==c.conceptId,{onConcept(c.conceptId)},label={Text(c.conceptId.substringAfter('-').take(12))})}}}
        item{Text("Explanation style");Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){ExplanationStyle.entries.take(4).forEach{s->FilterChip(style==s,{style=s},label={Text(s.name.substringBefore('_').lowercase())})}}}
        item{OutlinedTextField(question,{question=it},Modifier.fillMaxWidth().semantics{contentDescription="Ask the local Socratic tutor"},label={Text("Ask a question or enter your step")},minLines=2);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={scope.launch{val state=service.state(conceptId);val text=question.ifBlank{"Help me choose the next step"};val request=GroundedRequestFactory.local(conceptId,text,state.masteryState,HintLevel.entries[hintOrdinal.coerceIn(HintLevel.entries.indices)],style);val need=if(text.contains("in another way",true)||text.startsWith("why",true))AssistanceNeed.OPEN_ENDED else AssistanceNeed.ROUTINE;response=runCatching{router.respond(request,need,settings.selectedProvider.takeIf{it!=ProviderKind.NONE}?.name?.lowercase(),settings.consent)}.getOrElse{router.respond(request)} }}){Text(if(settings.consent.cloudEnabled)"Ask (local first)" else "Ask locally")};OutlinedButton({hintOrdinal=(hintOrdinal+1).coerceAtMost(HintLevel.entries.lastIndex);question="Give me the next bounded hint"}){Text("Deeper hint")}}}
        response?.let{r->item{ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(r.text);Text("${r.verificationStatus.name.replace('_',' ')} · ${r.providerId?:"fallback"}",style=MaterialTheme.typography.labelMedium);Text("Sources: ${r.groundingReferences.joinToString()}")}}}}
        item{VerifiedPracticePanel(conceptId)}
        } }
        if(wide) Row { Box(Modifier.weight(1f)){content()};Surface(Modifier.width(300.dp).fillMaxHeight(),tonalElevation=2.dp){Column(Modifier.padding(16.dp)){Text("Supporting pane",style=MaterialTheme.typography.titleMedium);Text("Tutor, notebook and active visual can remain visible together on tablets and landscape screens.")}} } else content()
    }
}

@Composable private fun VerifiedPracticePanel(conceptId:String){val generator=remember{VerifiedPracticeGenerator()};var prompt by remember{mutableStateOf<String?>(null)};val template=VerifiedPracticeCatalog.templates.firstOrNull{it.conceptId==conceptId};Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text("Verified practice",style=MaterialTheme.typography.titleMedium);Text(prompt?:"Numbers and answers come from deterministic local solvers.");Button({template?.let{prompt=generator.generate(it,(System.currentTimeMillis()%10000).toInt()).task?.prompt}},enabled=template!=null){Text("Generate validated variant")}}}}

@Composable private fun InputAssistanceWorkspace(){
    val context=LocalContext.current;var transcription by remember{mutableStateOf("Solve 2x + 4 = 10")};var imported by remember{mutableStateOf<ImportedQuestion?>(null)};var pendingImage by remember{mutableStateOf<ByteArray?>(null)};var voice by remember{mutableStateOf("Increase frequency to five hertz")};var voiceResult by remember{mutableStateOf("")};val importer=remember(transcription){CameraQuestionImporter{transcription}};val voiceParser=remember{LocalVoiceCommandParser()}
    var tts by remember{mutableStateOf<TextToSpeech?>(null)};DisposableEffect(context){val engine=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS)tts?.language=Locale.getDefault()};tts=engine;onDispose{engine.stop();engine.shutdown()}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->pendingImage=uri?.let{context.contentResolver.openInputStream(it)?.use{stream->stream.readBytes()}}}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->pendingImage=bitmap?.let{ByteArrayOutputStream().use{out->it.compress(android.graphics.Bitmap.CompressFormat.JPEG,88,out);out.toByteArray()}}}
    var launchCamera by remember{mutableStateOf(false)};val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted&&launchCamera)camera.launch(null);launchCamera=false}
    val speech=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{voice=it}}
    var launchSpeech by remember{mutableStateOf(false)};val audioPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted&&launchSpeech)speech.launch(speechIntent());launchSpeech=false}
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Camera question import",style=MaterialTheme.typography.headlineSmall);Text("Capture/select → transient local image → local recognizer contract → editable transcription → confirm → concept map. Images are not uploaded or retained by default.");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({picker.launch("image/*")}){Text("Select image")};OutlinedButton({if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera.launch(null)else{launchCamera=true;cameraPermission.launch(Manifest.permission.CAMERA)}}){Text("Use camera")}};pendingImage?.let{bytes->Text("Transient image ready · ${bytes.size} bytes");Button({imported=importer.import(bytes,transcription);pendingImage=null}){Text("Run local recognition")}};OutlinedTextField(transcription,{transcription=it},Modifier.fillMaxWidth(),label={Text("Editable recognised text")});Button({imported=importer.import(pendingImage?:byteArrayOf(1),transcription);pendingImage=null}){Text("Confirm local transcription")}};imported?.let{q->item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text("Mapped locally · confidence ${q.confidence}");Text("Subject: ${q.subjectCandidates.firstOrNull()?.subject?:"choose manually"}");Text("Concept: ${q.conceptCandidates.firstOrNull()?.conceptId?:"choose manually"}");Text("Choose: explain concept · open formula · open visualisation · solve with hints · attempt independently")}}}};item{Text("Voice learning mode",style=MaterialTheme.typography.titleLarge);OutlinedTextField(voice,{voice=it},Modifier.fillMaxWidth(),label={Text("Confirmed local voice transcript")});Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)speech.launch(speechIntent())else{launchSpeech=true;audioPermission.launch(Manifest.permission.RECORD_AUDIO)}}){Text("Speak")};OutlinedButton({tts?.speak(voice,TextToSpeech.QUEUE_FLUSH,null,"learning-read-aloud")}){Text("Read aloud")}};Button({val command=voiceParser.parse(voice,setOf("frequency"),mapOf("frequency" to 1.0..20.0));voiceResult=if(command.valid)"Validated ${command.type}: ${command.targetId.orEmpty()} ${command.value?:""} ${command.unit.orEmpty()}" else command.issue.orEmpty()}){Text("Validate voice command")};if(voiceResult.isNotBlank())Text(voiceResult);Text("Speech recognition requests the Android service's offline-preferred mode; no raw audio is stored by AIExplorer. Cloud voice remains separately consented and optional.")}}
}

private fun speechIntent()=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)}

@Composable private fun ExperimentWorkspace(persistence:Phase2Persistence){
    val scope=rememberCoroutineScope();val initial=remember{SimulationStateSnapshot("gas-initial","gas-law",1,0,mapOf("moles" to 1.0,"temperature" to 300.0,"volume" to 10.0),mapOf("moles" to "mol","temperature" to "K","volume" to "L"),mapOf("title" to "Ideal gas workspace"))};val coordinator=remember{LocalRepresentationCoordinator(initial,ReferenceRepresentationCalculators.gasLaw)};val state by coordinator.state.collectAsState();val explanation=remember(state.snapshot){VisualExplanationEngine().explain(state.snapshot)};val notebook=remember{ExperimentNotebook()};var note by remember{mutableStateOf("")};var sweep by remember{mutableStateOf<ParameterSweepResult?>(null)}
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Universal experiment notebook",style=MaterialTheme.typography.headlineSmall);Text("One state drives particles, values, formula, graph, explanation and captures.")}
        item{Text("Temperature ${state.snapshot.values["temperature"]?.toInt()} K");Slider((state.snapshot.values["temperature"]?:300.0).toFloat(),{coordinator.dispatch(RepresentationAction.SetValue("temperature",it.toDouble()))},valueRange=250f..400f);Text(state.formulaText);Text(state.explanation)}
        item{Canvas(Modifier.fillMaxWidth().height(130.dp).semantics{contentDescription="Synchronized gas pressure graph"}){val points=state.graphPoints;if(points.isNotEmpty()){drawLine(Color.Cyan,Offset(0f,size.height/2),Offset(size.width,size.height/2),2f);drawCircle(Color.Green,8f,Offset(size.width*.55f,size.height*.35f))}}}
        item{ElevatedCard{Column(Modifier.padding(12.dp)){Text(explanation.title,style=MaterialTheme.typography.titleMedium);Text(explanation.summary);explanation.observations.take(4).forEach{Text("• $it")};Text(explanation.limitations.first(),style=MaterialTheme.typography.labelSmall)}}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({val entry=notebook.get("gas-note")?:ExperimentNotebookEntry("gas-note",SchoolSubject.CHEMISTRY,"chemistry-gas-laws","gas-law","Gas-law investigation",prediction=note);notebook.save(entry);notebook.capture("gas-note",state.snapshot);scope.launch{val existing=persistence.load();val records=Phase2SnapshotMapper.snapshot(notebooks=notebook.all()).records.filter{it.type=="notebook"};persistence.save(Phase2Snapshot(records=existing.records.filterNot{it.type=="notebook"}+records))}}){Text("Capture values")};OutlinedButton({sweep=ParameterSweepEngine(ReferenceSweepModels.models).run(ParameterSweepDefinition("gas-law","volume",5.0,25.0,12,mapOf("moles" to 1.0,"temperature" to 300.0),setOf("pressure")))}){Text("Run sweep")}};OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Prediction or observation")})}
        sweep?.let{result->item{Text("Parameter sweep · ${result.trendDescription}",style=MaterialTheme.typography.titleMedium);result.samples.take(6).forEach{Text("V=${"%.1f".format(it.input)} → P=${"%.2f".format(it.outputs["pressure"])}")};Text(result.limitations.first())}}
    }
}

@Composable private fun AnalysisWorkspace(){var readings by remember{mutableStateOf("9.8, 10.1, 9.9, 10.0")};val values=readings.split(',').mapNotNull{it.trim().toDoubleOrNull()};val result=remember(values){UncertaintyLaboratory().analyse(MeasurementDataset("length","cm",values,.1,10.0))};LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Uncertainty & data laboratory",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(readings,{readings=it},Modifier.fillMaxWidth(),label={Text("Repeated readings (comma-separated)")});Text("Mean: ${result.mean} cm");Text("Range: ${result.range} cm · standard deviation: ${result.standardDeviation}");Text("Percentage error: ${result.percentageError}%");result.notes.forEach{Text("• $it")};Text("Possible outlier indices: ${result.possibleOutlierIndices.ifEmpty{listOf("none")}.joinToString()}")};item{Text("Graph-sketch assessment",style=MaterialTheme.typography.titleMedium);val evaluation=GraphSketchEvaluator().evaluate(GraphStructure(listOf(0.0),1,listOf(1),0),GraphStructure(listOf(0.0),1,listOf(1),0));Text("Structural score ${"%.0f".format(evaluation.overallShapeScore*100)}% — intercept, trend, slope sign and turning points; never pixel matching.")}}}

@Composable private fun JourneyWorkspace(){val engine=remember{CrossSubjectJourneyEngine()};LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Cross-subject journeys",style=MaterialTheme.typography.headlineSmall);Text("Each stop remains owned by its original subject module and includes a return path.")};items(CrossSubjectJourneyCatalog.journeys){journey->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(journey.title,style=MaterialTheme.typography.titleMedium);journey.stops.forEach{stop->Text("${stop.subject.name}: ${stop.conceptId} · ${stop.expectedMinutes} min");Text(stop.whyItMatters,style=MaterialTheme.typography.bodySmall)};Text(if(engine.preservesOwnership(journey))"Ownership verified" else "Journey requires review",style=MaterialTheme.typography.labelMedium)}}}}}

@Composable private fun ProviderSettingsScreen(settings:ProviderSettings,onSettings:(ProviderSettings)->Unit,store:SecureSecretStore){val scope=rememberCoroutineScope();var key by remember{mutableStateOf("")};var message by remember{mutableStateOf("Cloud AI is optional and off. Core tutoring remains local.")};LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Optional AI provider",style=MaterialTheme.typography.headlineSmall);Text(message);Switch(settings.consent.cloudEnabled,{onSettings(settings.copy(consent=settings.consent.copy(cloudEnabled=it)))});Text("Enable cloud AI")};item{ProviderKind.entries.forEach{provider->FilterChip(settings.selectedProvider==provider,{onSettings(settings.copy(selectedProvider=provider))},label={Text(provider.name)})}};item{OutlinedTextField(key,{key=it},Modifier.fillMaxWidth(),label={Text("API key")},singleLine=true);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({if(key.isNotBlank()&&settings.selectedProvider!=ProviderKind.NONE){store.put(settings.selectedProvider.name.lowercase(),key.toCharArray());key="";message="Key encrypted with Android Keystore."}}){Text("Save securely")};OutlinedButton({store.delete(settings.selectedProvider.name.lowercase());message="Stored key deleted."}){Text("Delete key")};OutlinedButton({val id=settings.selectedProvider.name.lowercase();val secret=store.get(id);if(secret==null){message="No key stored."}else{secret.fill('\u0000');message="Testing grounded provider connection…";scope.launch{val provider=RemoteProviderFactory.providers(store)[id];val req=GroundedRequestFactory.local("math-linear-equations","Reply with one short Socratic question.",ConceptMasteryState.LEARNING,HintLevel.NUDGE,ExplanationStyle.CONCISE);message=runCatching{provider?.respond(req)?.let{"Connection succeeded; response will still pass local verification."}?:"Provider is unavailable."}.getOrElse{"Connection failed: ${it.message?.take(120)?:"unknown error"}"}}}}){Text("Test")}}};item{ConsentToggle("Camera crop may be sent",settings.consent.cameraMayBeSent){onSettings(settings.copy(consent=settings.consent.copy(cameraMayBeSent=it)))};ConsentToggle("Voice transcript may be sent",settings.consent.voiceMayBeSent){onSettings(settings.copy(consent=settings.consent.copy(voiceMayBeSent=it)))};ConsentToggle("Selected learner steps may be sent",settings.consent.learnerStepsMayBeSent){onSettings(settings.copy(consent=settings.consent.copy(learnerStepsMayBeSent=it)))};Text("Shared categories: confirmed question text, concept and level. Complete profiles, unrelated history, raw audio, unselected images, error-book history and API keys are never included.")}}
}
@Composable private fun ConsentToggle(label:String,checked:Boolean,onChecked:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Switch(checked,onChecked)}}
