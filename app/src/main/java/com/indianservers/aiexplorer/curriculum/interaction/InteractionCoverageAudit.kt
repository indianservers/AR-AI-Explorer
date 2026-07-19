package com.indianservers.aiexplorer.curriculum.interaction

import com.indianservers.aiexplorer.biology.ncert.interaction.BiologyInteractiveReference
import com.indianservers.aiexplorer.chemistry.ncert.interaction.ChemistryInteractiveReference
import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.learning.ncert.interaction.MathematicsInteractiveReference
import com.indianservers.aiexplorer.physics.ncert.interaction.PhysicsInteractiveReference
import java.io.File
import java.time.LocalDate

object ReferenceActivityRegistry {
    val definitions: List<ActivityDefinition> by lazy { MathematicsInteractiveReference.definitions + PhysicsInteractiveReference.definitions + ChemistryInteractiveReference.definitions + BiologyInteractiveReference.definitions }
    fun subject(subject: SchoolSubject) = definitions.filter { it.subject == subject }
    fun create(activityId: String) = FunctionalInteractiveActivity(definitions.single { it.activityId == activityId })
}

data class ExistingInteractionInventory(
    val totalAssets: Int,
    val diagrams: Int,
    val simulations: Int,
    val experimentsAndPracticals: Int,
    val quizzes: Int,
    val graphs: Int,
    val activities: Int,
    val constructionTools: Int,
    val future3dMetadataConcepts: Int,
    val duplicateAssetIds: List<String>,
    val disconnectedOrBrokenControls: List<String>,
    val performanceHeavyActivities: List<String>,
    val activitiesWithoutObjectives: List<String>,
    val activitiesWithoutProgress: List<String>,
)

data class InteractionAuditSummary(
    val totalConcepts: Int,
    val assessableConcepts: Int,
    val tierCounts: Map<InteractionSuitability, Int>,
    val statusCounts: Map<InteractionCoverageStatus, Int>,
    val referenceActivities: Int,
)

data class InteractionCoverageReport(
    val schemaVersion: Int,
    val academicYear: String,
    val generatedOn: LocalDate,
    val profiles: List<ConceptInteractionProfile>,
    val inventory: ExistingInteractionInventory,
    val summary: InteractionAuditSummary,
    val limitations: List<String>,
) {
    fun writeTo(file: File): File { file.parentFile?.mkdirs(); file.writeText(toJson()); return file }
    fun toJson(): String = buildString {
        append("{\"schemaVersion\":").append(schemaVersion).append(",\"academicYear\":\"").append(academicYear).append("\",\"generatedOn\":\"").append(generatedOn).append("\",")
        append("\"summary\":{\"totalConcepts\":").append(summary.totalConcepts).append(",\"assessableConcepts\":").append(summary.assessableConcepts).append(",\"referenceActivities\":").append(summary.referenceActivities).append("},\"profiles\":[")
        append(profiles.joinToString { p -> "{\"conceptId\":\"${p.conceptId}\",\"subject\":\"${p.subject}\",\"classes\":[${p.classLevels.joinToString { it.number.toString() }}],\"tier\":\"${p.interactionSuitability}\",\"status\":\"${p.coverageStatus}\",\"assets\":[${p.existingAssetIds.joinToString { "\"$it\"" }}],\"missing\":[${p.missingCapabilities.joinToString { "\"$it\"" }}],\"priority\":\"${p.priority}\"}" })
        append("],\"limitations\":[").append(limitations.joinToString { "\"${it.replace("\"", "\\\"")}\"" }).append("]}")
    }
}

object InteractionCoverageAuditor {
    private val tierA = listOf("motion", "force", "wave", "electric", "light", "reflection", "refraction", "atom", "period", "bond", "gas", "equilibrium", "solution", "acid", "cell", "circulation", "inheritance", "genetic", "triangle", "coordinate", "trigon", "probability", "linear equation", "quadratic", "graph", "derivative", "integral", "matrix", "determinant")
    private val tierB = listOf("life process", "reproduction", "evolution", "ecosystem", "photosynthesis", "respiration", "classification", "organic", "reaction", "thermodynamic", "magnet", "optics")
    private val tierC = listOf("geometry", "line", "angle", "circle", "number", "polynomial", "statistics", "measurement", "biomolecule", "plant", "animal", "diversity")
    private fun tier(title: String, assessment: AssessmentStatus): InteractionSuitability {
        if (assessment in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION)) return InteractionSuitability.NOT_SUITABLE
        if (assessment in setOf(AssessmentStatus.REQUIRES_MANUAL_REVIEW, AssessmentStatus.SOURCE_UNCLEAR)) return InteractionSuitability.REQUIRES_REVIEW
        val t=title.lowercase(); return when { tierA.any(t::contains)->InteractionSuitability.TIER_A; tierB.any(t::contains)->InteractionSuitability.TIER_B; tierC.any(t::contains)->InteractionSuitability.TIER_C; else->InteractionSuitability.TIER_D }
    }
    private fun selected(subject: SchoolSubject, title: String): List<ActivityDefinition> {
        val t=title.lowercase()
        val keys=when(subject){
            SchoolSubject.MATHEMATICS->mapOf("linear equation" to "math-linear-equation-balance","triangle" to "math-triangle-lab","coordinate" to "math-coordinate-geometry","trigon" to "math-right-triangle-trig","probability" to "math-probability-experiment")
            SchoolSubject.PHYSICS->mapOf("motion" to "physics-motion-graphs","force" to "physics-newton-second-law","wave" to "physics-wave-speed","sound" to "physics-wave-speed","electric" to "physics-electric-circuits","light" to "physics-reflection-refraction","optic" to "physics-reflection-refraction")
            SchoolSubject.CHEMISTRY->mapOf("atom" to "chemistry-atom-builder","period" to "chemistry-periodic-trends","bond" to "chemistry-bonding","solution" to "chemistry-gas-laws","basic concepts" to "chemistry-gas-laws","acid" to "chemistry-titration","equilibrium" to "chemistry-titration")
            SchoolSubject.BIOLOGY->mapOf("cell: the unit" to "biology-cell-explorer","fundamental unit" to "biology-cell-explorer","cell cycle" to "biology-cell-division","circulation" to "biology-circulation","life processes" to "biology-circulation","inheritance" to "biology-mendelian-genetics","heredity" to "biology-mendelian-genetics")
        }
        return keys.filterKeys(t::contains).values.distinct().mapNotNull { id -> ReferenceActivityRegistry.definitions.find { it.activityId==id } }
    }
    private fun requiredVisuals(tier: InteractionSuitability, subject: SchoolSubject, title:String):Set<VisualLearningType>{val t=title.lowercase();return when(tier){InteractionSuitability.TIER_A->buildSet{add(VisualLearningType.VARIABLE_SIMULATION);add(VisualLearningType.PREDICTION_CHALLENGE);if(subject==SchoolSubject.MATHEMATICS||"graph" in t||"motion" in t)add(VisualLearningType.INTERACTIVE_GRAPH);if(subject==SchoolSubject.BIOLOGY)add(VisualLearningType.PROCESS_VISUALISER)};InteractionSuitability.TIER_B->setOf(VisualLearningType.PROCESS_VISUALISER,VisualLearningType.COMPARISON_VIEW);InteractionSuitability.TIER_C->setOf(VisualLearningType.DRAG_DROP_ACTIVITY,VisualLearningType.LABEL_ACTIVITY);InteractionSuitability.TIER_D->setOf(VisualLearningType.STRUCTURE_EXPLORER,VisualLearningType.CLASSIFICATION_TREE);else->emptySet()}}

    fun audit(manifests:List<OfficialCurriculum> = CbseNcert2026Curriculum.manifests, project:ProjectContentInventory = ProjectCurriculumInventory.build()):InteractionCoverageReport {
        val curriculumAudit=CurriculumCoverageAuditor.audit(manifests,project);val mappings=curriculumAudit.mappings.associateBy{it.curriculumNodeId}
        val chapterProfiles=manifests.flatMap{m->m.units.flatMap{it.chapters}.flatMap{chapter->chapter.topics.map{topic->
            val assess=topic.currentAssessmentStatus;val suitability=tier(chapter.officialTitle,assess);val activity=selected(m.subject,chapter.officialTitle);val existing=mappings[chapter.id]?.appContentIds.orEmpty();val status=when{ suitability==InteractionSuitability.NOT_SUITABLE->InteractionCoverageStatus.NOT_APPLICABLE;suitability==InteractionSuitability.REQUIRES_REVIEW->InteractionCoverageStatus.REQUIRES_REVIEW;activity.isNotEmpty()->InteractionCoverageStatus.PARTIAL;existing.isEmpty()->InteractionCoverageStatus.MISSING;topic.requiredAssets.any{it in setOf(RequiredAssetType.DIAGRAM,RequiredAssetType.GRAPH)}->InteractionCoverageStatus.STATIC_ONLY;else->InteractionCoverageStatus.TEXT_ONLY}
            val missing=when(status){InteractionCoverageStatus.COMPLETE,InteractionCoverageStatus.NOT_APPLICABLE->emptyList();InteractionCoverageStatus.STATIC_ONLY->listOf(InteractionCapability.MANIPULATION,InteractionCapability.CHALLENGE,InteractionCapability.PREDICTION,InteractionCapability.PROGRESS);InteractionCoverageStatus.REQUIRES_REVIEW->listOf(InteractionCapability.OBJECTIVE);else->listOf(InteractionCapability.LIVE_VISUAL,InteractionCapability.MANIPULATION,InteractionCapability.CHALLENGE,InteractionCapability.ACCESSIBLE_ALTERNATIVE,InteractionCapability.PROGRESS)}
            ConceptInteractionProfile(topic.id,m.subject,setOf(m.classLevel),suitability,requiredVisuals(suitability,m.subject,chapter.officialTitle),existing+activity.map{it.activityId},status,missing,when{status==InteractionCoverageStatus.COMPLETE||status==InteractionCoverageStatus.NOT_APPLICABLE->InteractionPriority.NONE;suitability==InteractionSuitability.TIER_A->InteractionPriority.CRITICAL;suitability==InteractionSuitability.TIER_B->InteractionPriority.HIGH;suitability==InteractionSuitability.TIER_C->InteractionPriority.MEDIUM;else->InteractionPriority.LOW})}}}
        val implementedProfiles=ReferenceActivityRegistry.definitions.map{definition->ConceptInteractionProfile(definition.conceptId,definition.subject,definition.classLevels,InteractionSuitability.TIER_A,buildSet{add(VisualLearningType.MANIPULABLE_DIAGRAM);add(VisualLearningType.PREDICTION_CHALLENGE);if(definition.compute(definition.controls.associate{it.id to it.initial}).points.isNotEmpty())add(VisualLearningType.INTERACTIVE_GRAPH);if(definition.formulaOrProcess.contains("=") )add(VisualLearningType.FORMULA_VISUALISER)},listOf(definition.activityId),InteractionCoverageStatus.COMPLETE,emptyList(),InteractionPriority.NONE)}
        val profiles=chapterProfiles+implementedProfiles
        val types=project.assets.flatMap{a->a.assetTypes.map{it to a.id}};val inventory=ExistingInteractionInventory(project.assets.size,types.count{it.first==RequiredAssetType.DIAGRAM},types.count{it.first==RequiredAssetType.SIMULATION},types.count{it.first in setOf(RequiredAssetType.EXPERIMENT,RequiredAssetType.PRACTICAL)},types.count{it.first==RequiredAssetType.QUIZ},types.count{it.first==RequiredAssetType.GRAPH},types.count{it.first==RequiredAssetType.ACTIVITY},0,1,project.assets.groupBy{it.id}.filterValues{it.size>1}.keys.sorted(),emptyList(),emptyList(),project.assets.filter{RequiredAssetType.ACTIVITY in it.assetTypes}.map{it.id},project.assets.filter{RequiredAssetType.ACTIVITY in it.assetTypes}.map{it.id})
        val assessable=manifests.flatMap{it.units}.flatMap{it.chapters}.flatMap{it.topics}.count{it.currentAssessmentStatus==AssessmentStatus.INCLUDED_AND_ASSESSABLE}
        val summary=InteractionAuditSummary(profiles.size,assessable,profiles.groupingBy{it.interactionSuitability}.eachCount(),profiles.groupingBy{it.coverageStatus}.eachCount(),ReferenceActivityRegistry.definitions.size)
        return InteractionCoverageReport(1,"2026-27",LocalDate.now(),profiles,inventory,summary,listOf("Curriculum manifests currently represent one core topic per official chapter; this report does not claim finer textbook-section coverage.","Class 7–8 source-review nodes remain REQUIRES_REVIEW and are not promoted to complete by title matching.","An existing asset is not called interactive unless the reference engine or an audited simulation connects controls to model output."))
    }
}
