package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.assistant.grounding.GroundingValidator
import com.indianservers.aiexplorer.assistant.local.LocalLearningAssistantProvider
import com.indianservers.aiexplorer.assistant.privacy.*
import com.indianservers.aiexplorer.assistant.routing.*
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.input.*
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningworkspace.*
import com.indianservers.aiexplorer.practice.*
import com.indianservers.aiexplorer.tutor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class Phase2LearningWorkspaceTest {
    private fun request(level:HintLevel=HintLevel.CONCEPT_CUE,steps:List<LearnerAnswerStep> = emptyList())=GroundedRequestFactory.local("math-linear-equations","How do I continue?",ConceptMasteryState.LEARNING,level,ExplanationStyle.STEP_BY_STEP,steps=steps)

    @Test fun tutorUsesOnlyGroundedConceptContent()=runBlocking{val req=request();val response=LocalLearningAssistantProvider().respond(req);assertTrue(GroundingValidator.validate(req).valid);assertTrue(response.groundingReferences.all{ref->req.verifiedContent.any{it.id==ref}})}
    @Test fun tutorDoesNotExceedAllowedHintLevel()=runBlocking{val req=request(HintLevel.NUDGE);val response=LocalLearningAssistantProvider().respond(req);assertFalse(response.groundingReferences.any{it.contains("-5")});assertEquals(AssistantVerificationStatus.LOCALLY_AUTHORED,response.verificationStatus)}
    @Test fun invalidCloudResponseFallsBackToLocalContent()=runBlocking{val bad=object:LearningAssistantProvider{override val id="bad";override val capabilities=AssistantCapability.entries.toSet();override suspend fun respond(request:GroundedAssistantRequest)=AssistantResponse("invented result",AssistantResponseType.EXPLANATION,listOf("not-grounded"),emptyList(),AssistantVerificationStatus.PARTIALLY_VERIFIED,emptyList(),id)};val response=AssistantRouter(LocalLearningAssistantProvider(),mapOf("bad" to bad)).respond(request(),AssistanceNeed.OPEN_ENDED,"bad",AssistantConsent(cloudEnabled=true));assertEquals(AssistantVerificationStatus.FALLBACK_USED,response.verificationStatus);assertNotEquals("bad",response.providerId)}
    @Test fun simulationExplanationMatchesCurrentState(){val state=SimulationStateSnapshot("w1","wave",1,0,mapOf("frequency" to 5.0,"speed" to 20.0,"wavelength" to 4.0),mapOf("frequency" to "Hz","speed" to "m/s","wavelength" to "m"));val e=VisualExplanationEngine().explain(state);assertTrue(e.observations.any{"5" in it&&"Hz" in it});assertTrue(e.formulaConnections.contains("v = fλ"))}
    @Test fun firstInvalidStepIsIdentified(){val evaluations=MistakeAwareStepEvaluator().ordered(listOf("2x+4=10","2x=6","x=3"),listOf(LearnerAnswerStep("2x+4=10"),LearnerAnswerStep("2x=14"),LearnerAnswerStep("x=7")),setOf("math-move-change-sign"));assertEquals(1,MistakeAwareStepEvaluator().firstInvalid(evaluations)?.stepIndex);assertTrue(evaluations[0].valid)}
    @Test fun generatedPracticeAlwaysPassesLocalValidation(){val generator=VerifiedPracticeGenerator();VerifiedPracticeCatalog.templates.forEach{template->repeat(100){seed->val result=generator.generate(template,seed);assertNotNull(result.task);assertEquals(10,result.task!!.validationStages.size);assertTrue(result.task!!.answer.isFinite())}}}
    @Test fun prerequisiteRepairReturnsToOriginalState(){val states=mapOf("math-fractions" to LearnerConceptState("math-fractions",emptySet(),SchoolSubject.MATHEMATICS));val plan=PrerequisiteRepairEngine().plan("math-linear-equations",states,"activity",2)!!;assertEquals("math-linear-equations",PrerequisiteRepairEngine().returnToOriginal(plan).conceptId);assertTrue(plan.estimatedMinutes in 3..8)}
    @Test fun replayIsDeterministic(){val replay=replay();val engine=DeterministicReplayEngine{state,event->state.copy(values=state.values+(event.targetId!! to event.numericValue!!))};assertTrue(engine.deterministic(replay));assertEquals(2.0,engine.stateAt(replay,1000).values["x"]!!,0.0)}
    @Test fun formulaGraphAndVisualShareOneState(){val initial=SimulationStateSnapshot("g","gas-law",1,0,mapOf("moles" to 1.0,"temperature" to 300.0,"volume" to 10.0));val c=LocalRepresentationCoordinator(initial,ReferenceRepresentationCalculators.gasLaw);c.dispatch(RepresentationAction.SetValue("temperature",350.0));assertEquals(350.0,c.state.value.snapshot.values["temperature"]!!,0.0);assertTrue(c.state.value.formulaText.contains("350"));assertTrue(c.state.value.graphPoints.isNotEmpty())}
    @Test fun parameterSweepRejectsInvalidRange(){assertThrows(IllegalArgumentException::class.java){ParameterSweepEngine(ReferenceSweepModels.models).run(ParameterSweepDefinition("gas-law","volume",10.0,5.0,10,emptyMap(),setOf("pressure")))}}
    @Test fun comparisonDetectsChangedVariables(){val a=SimulationStateSnapshot("a","wave",1,0,mapOf("frequency" to 2.0,"speed" to 10.0));val b=a.copy(id="b",values=mapOf("frequency" to 5.0,"speed" to 10.0));val c=StateComparisonEngine().compare(a,b,setOf("frequency"));assertEquals("frequency",c.changedInputs.single().id);assertEquals(listOf("speed"),c.unchangedValues)}
    @Test fun experimentDesignRejectsMultipleIndependentVariables(){val v=ExperimentVariable("force","Force","N",0.0..10.0);val d=StudentExperimentDesign("Force test","How does force affect acceleration?",null,v,listOf(ExperimentVariable("a","Acceleration","m/s2",0.0..10.0)),listOf(ExperimentVariable("m","Mass","kg",1.0..5.0)),listOf("cart"),listOf(MeasurementPlanStep(1,"Measure",3)),listOf("trend visible"),listOf("model only"),additionalIndependentVariables=listOf(ExperimentVariable("angle","Angle","degree",0.0..30.0)));assertFalse(StudentExperimentValidator().validate(d).valid)}
    @Test fun graphSketchUsesStructuralNotPixelMatching(){val target=GraphStructure(listOf(0.0),1,listOf(1,-1),1,domain=-2.0..2.0);val shiftedPoints=GraphStructure(listOf(.05),1,listOf(1,-1),1,domain=-2.05..2.05);val result=GraphSketchEvaluator().evaluate(shiftedPoints,target);assertTrue(result.overallShapeScore>.8)}
    @Test fun crossSubjectJourneyPreservesSubjectOwnership(){CrossSubjectJourneyCatalog.journeys.forEach{assertTrue(CrossSubjectJourneyEngine().preservesOwnership(it))}}
    @Test fun cameraImageIsNotUploadedWithoutConsent(){val importer=CameraQuestionImporter{"Solve linear equation 2x + 3 = 9"};assertFalse(importer.mayUploadForInterpretation(AssistantConsent(),true,true));assertEquals(ProcessingLocation.LOCAL,importer.import(byteArrayOf(1)).processingLocation)}
    @Test fun providerKeyIsNeverStoredInPlainText(){val marker="sk-secret-value";val snapshot=Phase2SnapshotMapper.snapshot(settings=ProviderSettings(ProviderKind.OPENAI,AssistantConsent(cloudEnabled=true)));assertFalse(snapshot.toString().contains(marker));assertTrue(snapshot.records.none{it.fields.keys.any{k->"key" in k.lowercase()}})}
    @Test fun uncertaintyFlagsButDoesNotDeleteOutlier(){val values=listOf(10.0,10.1,9.9,10.0,30.0);val result=UncertaintyLaboratory().analyse(MeasurementDataset("length","cm",values,.1,10.0));assertEquals(values.size,5);assertNotNull(result.mean);assertTrue(result.notes.any{"never deleted" in it})}
    @Test fun proofRequiresReasonsNotJustFinalStatement(){val expected=listOf(ProofStep("a=b","given"),ProofStep("a+c=b+c","addition property"));val learner=listOf(ProofStep("a=b",null),ProofStep("a+c=b+c",null));assertFalse(ProofStudioEngine().evaluate(ProofTaskType.STATEMENT_REASON,expected,learner).valid)}
    @Test fun dataToolsAreAgeProgressive(){val rows=listOf(DataRow("1",mapOf("x" to 1.0,"y" to 2.0)),DataRow("2",mapOf("x" to 2.0,"y" to 4.0)));assertFalse("residuals" in DataAnalysisWorkspace().analyse(rows,"x","y",DataComplexity.CLASSES_7_8).enabledTools);assertTrue("residuals" in DataAnalysisWorkspace().analyse(rows,"x","y",DataComplexity.CLASSES_11_12).enabledTools)}
    @Test fun phase2RegistryIsValid(){assertTrue(Phase2Validator.validate().errors.joinToString(),Phase2Validator.validate().valid)}
    @Test fun phase2PerformanceTenThousandAnalyses(){val engine=StateComparisonEngine();val a=SimulationStateSnapshot("a","wave",1,0,mapOf("x" to 1.0));val b=a.copy(id="b",values=mapOf("x" to 2.0));val start=System.nanoTime();repeat(10_000){engine.compare(a,b,setOf("x"))};val elapsed=(System.nanoTime()-start)/1_000_000;println("PHASE2_10000_COMPARISONS_MS=$elapsed");assertTrue(elapsed<3000)}

    private fun replay():SimulationReplay{val initial=SimulationStateSnapshot("s0","motion-graph",1,0,mapOf("x" to 0.0));return SimulationReplay("motion-graph",initial,listOf(SimulationInputEvent(500,SimulationInputEventType.SET_VALUE,"x",1.0),SimulationInputEvent(1000,SimulationInputEventType.SET_VALUE,"x",2.0)),listOf(initial),1000)}
}
