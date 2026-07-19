package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.curriculum.experience.*
import com.indianservers.aiexplorer.curriculum.interaction.FunctionalInteractiveActivity
import com.indianservers.aiexplorer.curriculum.interaction.ReferenceActivityRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

class IntegratedConceptExperienceTest {
 private val experiences=ConceptExperienceRegistry.experiences

 @Test fun auditCoversOfficialPagesAndIntegratedSubconcepts(){val report=ConceptPageAuditor.audit();assertEquals(223,report.profiles.size);assertEquals(203,report.summary.officialConcepts);assertEquals(175,report.summary.assessableConcepts);assertEquals(20,report.summary.statusCounts[ConceptPageCoverageStatus.INTEGRATED_COMPLETE]);val file=report.writeTo(File("build/reports/concept-experience/concept-page-audit.json"));assertTrue(file.length()>10_000)}

 @Test fun fiveCompleteExperiencesExistPerSubject(){assertEquals(20,experiences.size);SchoolSubject.entries.forEach{subject->assertEquals(subject.name,5,experiences.count{it.subject==subject})}}

 @Test fun everyIntegratedExperiencePassesSchemaValidation(){val errors=experiences.flatMap{ConceptExperienceValidator.validate(it).errors};assertTrue(errors.joinToString("\n"),errors.isEmpty())}

 @Test fun everyExperienceHasReadObserveManipulatePredictTestApplyPracticeReviewCycle(){experiences.forEach{e->val blocks=e.contentSections.flatMap{it.blocks};assertTrue(e.summary.isNotBlank());assertTrue(e.contentSections.any{it.kind==ContentSectionKind.EXPLAIN});assertTrue(blocks.any{it is InteractiveContentBlock.ManipulableModel});assertTrue(blocks.any{it is InteractiveContentBlock.PredictionPrompt});assertTrue(blocks.any{it is InteractiveContentBlock.ObservationPrompt});assertTrue(blocks.any{it is InteractiveContentBlock.MisconceptionTest});assertTrue(blocks.any{it is InteractiveContentBlock.PracticeTask});assertTrue(blocks.any{it is InteractiveContentBlock.RevisionStrip})}}

 @Test fun visualisationIsLinkedToConceptSection(){experiences.forEach{e->e.visualisations.forEach{visual->val links=e.visualisationLinks.filter{it.visualisationId==visual.id};assertTrue("${e.conceptId}/${visual.id}",links.isNotEmpty());assertTrue(links.all{link->e.contentSections.any{it.id==link.contentSectionId}})}}}

 @Test fun everyControlUpdatesScientificStateAndDynamicExplanation(){ReferenceActivityRegistry.definitions.forEach{d->d.controls.forEach{control->val activity=FunctionalInteractiveActivity(d);val before=activity.snapshot;activity.setControl(control.id,if(control.initial==control.maximum)control.minimum else control.maximum);val after=activity.snapshot;assertNotEquals("${d.activityId}/${control.id}",before.visual.values,after.visual.values);val explanation=DynamicExplanationEngine.explain(d,before,after);assertTrue(explanation.whatChanged.contains(control.label));assertTrue(explanation.whyItChanged.isNotBlank())}}}

 @Test fun formulaVariablesAndDiagramPartsResolveToVisualQuantities(){experiences.forEach{e->val d=ReferenceActivityRegistry.definitions.single{it.activityId==e.activities.single().activityId};val targets=(d.controls.map{it.id}+d.diagramParts.map{it.id}).toSet();e.contentSections.flatMap{it.blocks}.filterIsInstance<InteractiveContentBlock.FormulaExplorer>().flatMap{it.variables}.forEach{assertTrue("${e.conceptId}/${it.visualTargetId}",it.visualTargetId in targets)};val inline=e.contentSections.flatMap{it.blocks}.filterIsInstance<InteractiveContentBlock.InlineDiagram>().single();assertTrue(inline.focusTargetIds.containsAll(d.diagramParts.map{it.id}))}}

 @Test fun everyProcessStepHasInputActionOutputAndExplanation(){experiences.flatMap{it.contentSections}.flatMap{it.blocks}.filterIsInstance<InteractiveContentBlock.ProcessStepper>().flatMap{it.steps}.forEach{step->assertTrue(step.inputs.isNotEmpty());assertTrue(step.action.isNotBlank());assertTrue(step.outputs.isNotEmpty());assertTrue(step.explanation.isNotBlank())}}

 @Test fun everyVisualisationHasAccessibleStateDescription(){experiences.flatMap{it.visualisations}.forEach{assertTrue(it.accessibilityDescription.isNotBlank())};ReferenceActivityRegistry.definitions.forEach{d->assertTrue(d.compute(d.controls.associate{it.id to it.initial}).textAlternative.isNotBlank())}}

 @Test fun textFocusAndPresetActionsChangeTheCoordinatedVisualState(){experiences.forEach{e->val d=ReferenceActivityRegistry.definitions.single{it.activityId==e.activities.single().activityId};val activity=FunctionalInteractiveActivity(d);val focus=e.visualisationLinks.firstNotNullOf{it.focusTargetId};activity.focus(focus);assertEquals(focus,activity.snapshot.focusTargetId);val preset=d.presets.last();activity.applyPreset(preset.id);preset.values.forEach{(id,value)->assertEquals(value,activity.snapshot.controls[id]!!,1e-9)}}}

 @Test fun conceptCannotCompleteFromOpeningOrSimulationUseAlone(){experiences.forEach{e->var p=ConceptProgressState(e.conceptId);assertFalse(p.isComplete(e));p=p.copy(openedVisualisationIds=e.visualisations.map{it.id}.toSet(),controlChanges=10);assertFalse(p.isComplete(e));p=p.copy(readSectionIds=e.completionPolicy.requiredSectionIds,predictionAttempts=1,observationCount=1,completedChallengeIds=setOf("challenge"),practiceAttempts=1,reviewed=true);assertTrue(p.isComplete(e))}}

 @Test fun predictionPracticeAndReviewEvidenceRemainSeparate(){val e=experiences.first();val p=ConceptProgressState(e.conceptId,predictionAttempts=2,predictionCorrect=1,practiceAttempts=3,practiceCorrect=2,reviewed=true);assertEquals(2,p.predictionAttempts);assertEquals(3,p.practiceAttempts);assertTrue(p.reviewed);assertFalse(p.isComplete(e))}

 @Test fun noPlaceholderOrDisconnectedRouteIsMarkedComplete(){val report=ConceptPageAuditor.audit();report.profiles.filter{it.status==ConceptPageCoverageStatus.INTEGRATED_COMPLETE}.forEach{profile->val experience=ConceptExperienceRegistry.get(profile.conceptId);assertNotNull(experience);assertTrue(ConceptExperienceValidator.validate(experience!!).valid);assertTrue(experience.visualisations.all{visual->visual.activityId in ReferenceActivityRegistry.definitions.map{it.activityId}})}}

 @Test fun referenceModelsMeetControlDrivenCpuBudget(){val elapsed=measureTimeMillis{ReferenceActivityRegistry.definitions.forEach{d->val activity=FunctionalInteractiveActivity(d);repeat(200){activity.update(.016f)}}};println("CONCEPT_MODEL_4000_UPDATES_MS=$elapsed");assertTrue("4000 model updates took ${elapsed}ms",elapsed<5_000)}
}
