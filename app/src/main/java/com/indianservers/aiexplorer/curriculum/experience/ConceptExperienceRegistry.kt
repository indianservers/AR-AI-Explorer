package com.indianservers.aiexplorer.curriculum.experience

import com.indianservers.aiexplorer.biology.ncert.experience.BiologyConceptExperiences
import com.indianservers.aiexplorer.chemistry.ncert.experience.ChemistryConceptExperiences
import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.interaction.*
import com.indianservers.aiexplorer.learning.ncert.experience.MathematicsConceptExperiences
import com.indianservers.aiexplorer.physics.ncert.experience.PhysicsConceptExperiences
import java.io.File
import java.time.LocalDate

object ConceptExperienceRegistry {
    val experiences by lazy { MathematicsConceptExperiences.all + PhysicsConceptExperiences.all + ChemistryConceptExperiences.all + BiologyConceptExperiences.all }
    fun get(conceptId: String) = experiences.singleOrNull { it.conceptId == conceptId }
}

data class ExperienceValidationReport(val errors: List<String>) { val valid get()=errors.isEmpty() }
object ConceptExperienceValidator {
    fun validate(experience: InteractiveConceptExperience): ExperienceValidationReport {
        val errors=mutableListOf<String>();val sectionIds=experience.contentSections.map{it.id};val visuals=experience.visualisations.associateBy{it.id}
        if(sectionIds.size!=sectionIds.distinct().size)errors += "${experience.conceptId}: duplicate section id"
        if(experience.learningObjectives.isEmpty())errors += "${experience.conceptId}: missing objectives"
        if(experience.contentSections.none{it.kind==ContentSectionKind.EXPLAIN})errors += "${experience.conceptId}: missing explanation"
        if(experience.contentSections.none{it.kind==ContentSectionKind.PRACTISE})errors += "${experience.conceptId}: missing practice"
        experience.visualisationLinks.forEach{link->if(link.contentSectionId !in sectionIds)errors += "${experience.conceptId}: link section missing ${link.contentSectionId}";if(link.visualisationId !in visuals)errors += "${experience.conceptId}: linked visual missing ${link.visualisationId}"}
        experience.visualisations.forEach{visual->if(experience.visualisationLinks.none{it.visualisationId==visual.id})errors += "${experience.conceptId}: visual ${visual.id} is disconnected";if(visual.accessibilityDescription.isBlank())errors += "${experience.conceptId}: visual accessibility missing"}
        experience.contentSections.flatMap{it.blocks}.forEach{block->when(block){
            is InteractiveContentBlock.FormulaExplorer->{if(block.variables.isEmpty())errors += "${experience.conceptId}: formula has no variable links";block.variables.forEach{if(it.unit.isBlank()&&it.symbol !in setOf("sin θ","cos θ","tan θ","P(E)","Z","A","period","group","part","pH","n"))errors += "${experience.conceptId}: ${it.symbol} unit missing"}}
            is InteractiveContentBlock.ProcessStepper->block.steps.forEach{if(it.inputs.isEmpty()||it.action.isBlank()||it.outputs.isEmpty()||it.explanation.isBlank())errors += "${experience.conceptId}: incomplete process step ${it.id}"}
            is InteractiveContentBlock.TextWithHighlight->if(block.text.isBlank())errors += "${experience.conceptId}: blank text block"
            is InteractiveContentBlock.PredictionPrompt->if(block.prompt.isBlank()||block.revealExplanation.isBlank())errors += "${experience.conceptId}: incomplete prediction"
            is InteractiveContentBlock.ObservationPrompt->if(block.expectedEvidence.isBlank())errors += "${experience.conceptId}: observation lacks evidence"
            is InteractiveContentBlock.MisconceptionTest->if(listOf(block.incorrectIdea,block.testInstruction,block.observedEvidence,block.correction).any{it.isBlank()})errors += "${experience.conceptId}: incomplete misconception test"
            is InteractiveContentBlock.PracticeTask->if(block.prompt.isBlank()||block.expectedAnswer.isBlank()||block.explanation.isBlank())errors += "${experience.conceptId}: incomplete practice"
            else->Unit}}
        if(!sectionIds.containsAll(experience.completionPolicy.requiredSectionIds))errors += "${experience.conceptId}: completion references missing section"
        val activity=ReferenceActivityRegistry.definitions.singleOrNull{it.activityId==experience.activities.singleOrNull()?.activityId}
        if(activity==null)errors += "${experience.conceptId}: activity missing" else {val targets=(activity.controls.map{it.id}+activity.diagramParts.map{it.id}).toSet();experience.visualisationLinks.mapNotNull{it.focusTargetId}.forEach{if(it !in targets)errors += "${experience.conceptId}: focus target $it does not resolve"};experience.contentSections.flatMap{it.blocks}.filterIsInstance<InteractiveContentBlock.FormulaExplorer>().flatMap{it.variables}.forEach{if(it.visualTargetId !in targets)errors += "${experience.conceptId}: formula target ${it.visualTargetId} does not resolve"}}
        return ExperienceValidationReport(errors)
    }
}

enum class ConceptPageCoverageStatus { INTEGRATED_COMPLETE, PARTIALLY_CONNECTED, TEXT_ONLY, STATIC_ONLY, SIMULATION_ONLY, FORMULA_ONLY, QUIZ_ONLY, MISSING, REQUIRES_REVIEW }
data class ConceptPageAuditProfile(val conceptId:String,val subject:SchoolSubject,val classLevels:Set<SchoolClassLevel>,val status:ConceptPageCoverageStatus,val hasText:Boolean,val assetIds:List<String>,val structuredExperienceId:String?,val missingConnections:List<String>)
data class ConceptPageAuditSummary(val totalConcepts:Int,val officialConcepts:Int,val assessableConcepts:Int,val statusCounts:Map<ConceptPageCoverageStatus,Int>,val missingExplanationLinks:Int)
data class ConceptPageAuditReport(val schemaVersion:Int,val generatedOn:LocalDate,val profiles:List<ConceptPageAuditProfile>,val summary:ConceptPageAuditSummary,val limitations:List<String>){
 fun toJson()=buildString{append("{\"schemaVersion\":$schemaVersion,\"generatedOn\":\"$generatedOn\",\"summary\":{\"totalConcepts\":${summary.totalConcepts},\"officialConcepts\":${summary.officialConcepts},\"assessableConcepts\":${summary.assessableConcepts},\"missingExplanationLinks\":${summary.missingExplanationLinks}},\"profiles\":[");append(profiles.joinToString{p->"{\"conceptId\":\"${p.conceptId}\",\"subject\":\"${p.subject}\",\"status\":\"${p.status}\",\"hasText\":${p.hasText},\"experience\":${p.structuredExperienceId?.let{"\"$it\""}?:"null"}}"});append("]}")}
 fun writeTo(file:File):File{file.parentFile?.mkdirs();file.writeText(toJson());return file}
}

object ConceptPageAuditor {
 fun audit():ConceptPageAuditReport{
  val manifests=CbseNcert2026Curriculum.manifests;val inventory=ProjectCurriculumInventory.build();val curriculum=CurriculumCoverageAuditor.audit(manifests,inventory);val mappings=curriculum.mappings.associateBy{it.curriculumNodeId};val interaction=InteractionCoverageAuditor.audit().profiles.associateBy{it.conceptId}
  val official=manifests.flatMap{m->m.units.flatMap{it.chapters}.flatMap{chapter->chapter.topics.map{topic->val assets=mappings[chapter.id]?.appContentIds.orEmpty();val ip=interaction[topic.id];val status=when{topic.currentAssessmentStatus in setOf(AssessmentStatus.REQUIRES_MANUAL_REVIEW,AssessmentStatus.SOURCE_UNCLEAR)->ConceptPageCoverageStatus.REQUIRES_REVIEW;assets.isEmpty()->ConceptPageCoverageStatus.MISSING;ip?.coverageStatus==InteractionCoverageStatus.STATIC_ONLY->ConceptPageCoverageStatus.STATIC_ONLY;ip?.coverageStatus==InteractionCoverageStatus.TEXT_ONLY->ConceptPageCoverageStatus.TEXT_ONLY;ip?.coverageStatus==InteractionCoverageStatus.PARTIAL->ConceptPageCoverageStatus.PARTIALLY_CONNECTED;else->ConceptPageCoverageStatus.TEXT_ONLY};ConceptPageAuditProfile(topic.id,m.subject,setOf(m.classLevel),status,assets.isNotEmpty(),assets,null,listOf("No structured content-to-visual link","No unified concept completion policy"))}}}
  val implemented=ConceptExperienceRegistry.experiences.map{e->ConceptPageAuditProfile(e.conceptId,e.subject,e.classLevels,ConceptPageCoverageStatus.INTEGRATED_COMPLETE,true,e.visualisations.map{it.activityId},e.conceptId,emptyList())};val profiles=official+implemented;val assessable=manifests.flatMap{it.units}.flatMap{it.chapters}.flatMap{it.topics}.count{it.currentAssessmentStatus==AssessmentStatus.INCLUDED_AND_ASSESSABLE};return ConceptPageAuditReport(1,LocalDate.now(),profiles,ConceptPageAuditSummary(profiles.size,official.size,assessable,profiles.groupingBy{it.status}.eachCount(),official.count{it.missingConnections.any{m->"visual link" in m}}),listOf("Official manifests currently expose one core topic per chapter, so integrated sub-concepts are reported separately.","Existing assets without structured link metadata are not assumed to be coordinated merely because text and a diagram both exist."))
 }
}
