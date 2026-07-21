package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.input.*
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningworkspace.*
import com.indianservers.aiexplorer.practice.*
import com.indianservers.aiexplorer.tutor.*
import org.junit.Assert.*
import org.junit.Test

class Phase2IntegrationTest {
 @Test fun learnerMistakeFlowsToFocusedTutorResponse(){val steps=listOf(LearnerAnswerStep("2x+4=10"),LearnerAnswerStep("2x=14"));val eval=MistakeAwareStepEvaluator().ordered(listOf("2x+4=10","2x=6"),steps,setOf("math-move-change-sign"));val req=GroundedRequestFactory.local("math-linear-equations","Check my work",ConceptMasteryState.LEARNING,HintLevel.PARTIAL_STEP,ExplanationStyle.STEP_BY_STEP,steps=steps);val prompt=SocraticTutorEngine().next(req,eval);assertEquals(SocraticTutorState.EXECUTE_STEP,prompt.state);assertTrue(prompt.text.contains("step 2"))}
 @Test fun misconceptionFlowsToSocraticRemediation(){val req=GroundedRequestFactory.local("physics-electric-circuits","Is current used up?",ConceptMasteryState.PRACTISING,HintLevel.CONCEPT_CUE,ExplanationStyle.INTUITIVE,misconceptions=setOf("physics-current-consumed"));assertEquals(SocraticTutorState.CHECK_UNDERSTANDING,SocraticTutorEngine().next(req).state)}
 @Test fun cameraConfirmationMapsConceptWithoutSolving(){val imported=CameraQuestionImporter{"In a circuit calculate current using V and R"}.import(byteArrayOf(7),"In an electric circuit calculate current from voltage and resistance");assertEquals(SchoolSubject.PHYSICS,imported.subjectCandidates.first().subject);assertTrue(imported.conceptCandidates.any{it.conceptId=="physics-electric-circuits"});assertFalse(imported.confirmedText.contains("answer is",true))}
 @Test fun voiceCommandIsValidatedBeforeSimulationAction(){val command=LocalVoiceCommandParser().parse("Increase frequency to five hertz",setOf("frequency"),mapOf("frequency" to 1.0..10.0));assertTrue(command.valid);assertEquals(5.0,command.value!!,0.0);assertFalse(LocalVoiceCommandParser().parse("Set frequency to 500 hertz",setOf("frequency"),mapOf("frequency" to 1.0..10.0)).valid)}
 @Test fun practiceSolverAndValidatorPipelineCompletes(){val result=VerifiedPracticeGenerator().generate(VerifiedPracticeCatalog.templates.first(),42);assertNotNull(result.task);assertEquals("answer_verified",result.task!!.validationStages[8])}
 @Test fun visualStateProducesGroundedExplanation(){val s=SimulationStateSnapshot("c1","electric-circuit",1,20,mapOf("voltage" to 12.0,"resistance" to 6.0,"current" to 2.0),mapOf("current" to "A"));assertTrue(VisualExplanationEngine().explain(s).causalExplanation.any{"Charge is not consumed" in it})}
 @Test fun sweepProducesTableAndTrend(){val result=ParameterSweepEngine(ReferenceSweepModels.models).run(ParameterSweepDefinition("wave","frequency",1.0,5.0,5,mapOf("speed" to 20.0),setOf("wavelength")));assertEquals(5,result.samples.size);assertTrue("decreases" in result.trendDescription)}
 @Test fun experimentValidationCanFlowIntoNotebook(){val d=StudentExperimentDesign("Circuit","How does resistance affect current?","Current falls",ExperimentVariable("r","Resistance","ohm",1.0..20.0),listOf(ExperimentVariable("i","Current","A",0.0..12.0)),listOf(ExperimentVariable("v","Voltage","V",12.0..12.0)),listOf("supply","resistor"),listOf(MeasurementPlanStep(1,"Measure current",5)),listOf("negative trend"),listOf("Use simulated circuit"));assertTrue(StudentExperimentValidator().validate(d).valid);val notebook=ExperimentNotebook();notebook.save(ExperimentNotebookEntry("n",SchoolSubject.PHYSICS,"physics-electric-circuits","circuit","Circuit",question=d.question,hypothesis=d.hypothesis));notebook.addObservation("n","Current decreased.");assertEquals(1,notebook.get("n")!!.observations.size)}
 @Test fun journeyMovesAndReturnsWithoutOwnershipChange(){val journey=CrossSubjectJourneyCatalog.journeys.first();val engine=CrossSubjectJourneyEngine();val moved=engine.move(journey,engine.open(journey),1);assertEquals(0,engine.returnToPrevious(moved).stopIndex);assertTrue(engine.preservesOwnership(journey))}
}
