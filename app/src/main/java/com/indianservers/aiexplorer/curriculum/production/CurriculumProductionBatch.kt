package com.indianservers.aiexplorer.curriculum.production

import com.indianservers.aiexplorer.biology.ncert.BiologyNcertProductionContent
import com.indianservers.aiexplorer.biology.ncert.BiologyNcertBatch02Content
import com.indianservers.aiexplorer.chemistry.ncert.ChemistryNcertProductionContent
import com.indianservers.aiexplorer.chemistry.ncert.ChemistryNcertBatch02Content
import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.interactive.*
import com.indianservers.aiexplorer.learning.ncert.MathematicsNcertProductionContent
import com.indianservers.aiexplorer.learning.ncert.MathematicsNcertBatch02Content
import com.indianservers.aiexplorer.physics.ncert.PhysicsNcertProductionContent
import com.indianservers.aiexplorer.physics.ncert.PhysicsNcertBatch02Content

object CurriculumProductionBatchPlanner {
    val chapters: List<SubjectOwnedCurriculumChapter> by lazy {
        (MathematicsNcertProductionContent.chapters + PhysicsNcertProductionContent.chapters + ChemistryNcertProductionContent.chapters + BiologyNcertProductionContent.chapters + MathematicsNcertBatch02Content.chapters + PhysicsNcertBatch02Content.chapters + ChemistryNcertBatch02Content.chapters + BiologyNcertBatch02Content.chapters).map(::attachVerifiedInteraction)
    }
    private val selectedIds = listOf("cbse-2026-c9-mathematics-c3", "cbse-2026-c9-physics-c1", "cbse-2026-c10-chemistry-c1", "cbse-2026-c10-biology-c1")
    val batch02Ids = listOf("cbse-2026-c10-mathematics-c1", "cbse-2026-c10-physics-c3", "cbse-2026-c10-chemistry-c2", "cbse-2026-c10-biology-c2")

    private fun attachVerifiedInteraction(chapter:SubjectOwnedCurriculumChapter):SubjectOwnedCurriculumChapter{
        val activityId=when(chapter.curriculumNodeId){"cbse-2026-c9-physics-c1"->"physics-motion-graphs";"cbse-2026-c10-physics-c3"->"physics-electric-circuits";"cbse-2026-c10-chemistry-c2"->"chemistry-titration";"cbse-2026-c10-biology-c1"->"biology-circulation";else->null}?:return chapter
        val source=requireNotNull(CurriculumInteractionBridge.activity(activityId));val topicId=chapter.topicIds.first();val sectionId=chapter.explanationSections.keys.first();val activity=source.copy(conceptId=topicId,contentLinks=source.contentLinks.map{it.copy(explanationSectionId=sectionId)},route="/${chapter.subject.name.lowercase()}/interactive/$activityId")
        val sourceProfile=CurriculumInteractionBridge.profiles.single{activityId in it.activityIds};val profile=sourceProfile.copy(conceptId=topicId,classLevels=setOf(chapter.classLevel),explanationSectionIds=listOf(sectionId))
        return chapter.copy(interactionProfiles=listOf(profile),interactiveActivities=listOf(activity),requirements=chapter.requirements.copy(requiresInteractiveVisual=true,requiresGuidedMode=true,requiresExploreMode=true,requiresChallengeMode=true,requiresPrediction=true,requiresAccessibleAlternative=true))
    }

    fun prioritizedGaps(report: CurriculumAuditReport, manifests: List<OfficialCurriculum> = CbseNcert2026Curriculum.manifests, interactionReport:InteractionAuditReport = CurriculumInteractionAuditor.audit(manifests)): List<CurriculumGapItem> {
        val chaptersById = manifests.flatMap { manifest -> manifest.units.flatMap { unit -> unit.chapters.map { it.id to (manifest to it) } } }.toMap()
        val interactionByTopic=interactionReport.mappings.associateBy{it.conceptId}
        return report.mappings.mapNotNull { mapping ->
            val (manifest, chapter) = chaptersById[mapping.curriculumNodeId] ?: return@mapNotNull null
            val interactions=chapter.topics.mapNotNull{interactionByTopic[it.id]};val interactionGap=interactions.any{it.status !in setOf(InteractionCoverageStatus.COMPLETE,InteractionCoverageStatus.NOT_APPLICABLE)}
            if (chapter.assessmentStatus != AssessmentStatus.INCLUDED_AND_ASSESSABLE || (mapping.coverageStatus == CoverageStatus.COMPLETE&&!interactionGap)) return@mapNotNull null
            val priority = when (mapping.coverageStatus) {
                CoverageStatus.MISSING -> CurriculumGapPriority.CRITICAL_MISSING_CHAPTER
                CoverageStatus.TITLE_ONLY -> CurriculumGapPriority.CRITICAL_MISSING_TOPIC
                CoverageStatus.PARTIAL -> CurriculumGapPriority.PARTIAL_CORE_TOPIC
                else -> CurriculumGapPriority.CRITICAL_MISSING_TOPIC
            }
            val interactionAssets=buildSet{interactions.flatMap{it.missingCapabilities}.forEach{cap->when(cap){InteractionCapability.PREDICTION->add(RequiredAssetType.PREDICTION_ACTIVITY);InteractionCapability.CHALLENGE_MODE->add(RequiredAssetType.CHALLENGE_ACTIVITY);InteractionCapability.ACCESSIBLE_ALTERNATIVE->add(RequiredAssetType.ACCESSIBLE_VISUAL_ALTERNATIVE);else->add(RequiredAssetType.INTERACTIVE_VISUAL)}}}
            CurriculumGapItem(chapter.id, manifest.subject, manifest.classLevel, chapter.officialTitle, priority, mapping.missingRequirements.map { it.assetType }.toSet()+interactionAssets, when {
                mapping.coverageStatus == CoverageStatus.MISSING -> "No substantive app asset currently satisfies this assessable chapter."
                interactionGap -> "Written coverage exists, but required interactive learning capabilities are incomplete."
                RequiredAssetType.PRACTICAL in mapping.missingRequirements.map { it.assetType } -> "Core content is incomplete and a required practical connection is absent."
                else -> "Existing candidates cover only part of the required lesson, visual, practice and assessment bundle."
            })
        }.sortedWith(compareBy<CurriculumGapItem> { it.priority.ordinal }.thenByDescending { classImpact(it.classLevel) }.thenByDescending { it.missingAssets.size }.thenBy { it.curriculumNodeId })
    }

    fun selectFirstProductionBatch(report: CurriculumAuditReport): CurriculumImplementationBatch {
        val index = prioritizedGaps(report).associateBy { it.curriculumNodeId }
        val selected = selectedIds.map { id -> requireNotNull(index[id]) { "Selected production node $id is no longer an active gap." } }
        return CurriculumImplementationBatch("ncert-2026-27-production-01", "2026-27", selected.minBy { it.priority.ordinal }.priority, selected.groupBy { it.subject }, selected.size, chapters.filter { it.curriculumNodeId in selectedIds }.sumOf { it.completedAssetTypes.size }, ImplementationBatchStatus.READY)
    }

    fun selectSecondProductionBatch(report: CurriculumAuditReport): CurriculumImplementationBatch {
        val index = prioritizedGaps(report).associateBy { it.curriculumNodeId }
        val selected = batch02Ids.map { id -> requireNotNull(index[id]) { "Selected Batch 02 node $id is no longer an active gap." } }
        return CurriculumImplementationBatch("ncert-2026-27-production-02", "2026-27", selected.minBy { it.priority.ordinal }.priority, selected.groupBy { it.subject }, selected.size, chapters.filter { it.curriculumNodeId in batch02Ids }.sumOf { it.completedAssetTypes.size }, ImplementationBatchStatus.READY)
    }

    fun selectInteractionProductionBatch(curriculumReport:CurriculumAuditReport,interactionReport:InteractionAuditReport,maxItemsPerSubject:Int=5):CurriculumImplementationBatch{
        require(maxItemsPerSubject>0);val gaps=prioritizedGaps(curriculumReport,interactionReport=interactionReport);val interactionById=interactionReport.mappings.associateBy{it.conceptId};val official=CbseNcert2026Curriculum.manifests.flatMap{it.units}.flatMap{it.chapters};val chapterRank=official.associate{chapter->chapter.id to (chapter.topics.mapNotNull{interactionById[it.id]}.minOfOrNull{m->when(m.status){InteractionCoverageStatus.MISSING->1;InteractionCoverageStatus.TEXT_ONLY->2;InteractionCoverageStatus.STATIC_ONLY->3;InteractionCoverageStatus.BROKEN->4;InteractionCoverageStatus.PARTIAL->5;InteractionCoverageStatus.REQUIRES_REVIEW->9;else->10}}?:10)}
        val selected=gaps.groupBy{it.subject}.flatMap{(_,items)->items.sortedWith(compareBy<CurriculumGapItem>{chapterRank[it.curriculumNodeId]?:10}.thenBy{it.curriculumNodeId}).take(maxItemsPerSubject)};return CurriculumImplementationBatch("ncert-2026-27-interaction-generated","2026-27",selected.minOfOrNull{it.priority}?:CurriculumGapPriority.ENRICHMENT,selected.groupBy{it.subject},selected.size,selected.sumOf{it.missingAssets.size},ImplementationBatchStatus.PLANNED)
    }

    fun routeResolves(route: String): Boolean = chapters.any { it.route == route && it.subject.name.lowercase() in route }
    fun chapterForRoute(route: String): SubjectOwnedCurriculumChapter? = chapters.singleOrNull { it.route == route }
    private fun classImpact(level: SchoolClassLevel) = when (level) { SchoolClassLevel.CLASS_10 -> 6; SchoolClassLevel.CLASS_12 -> 5; SchoolClassLevel.CLASS_9 -> 4; SchoolClassLevel.CLASS_11 -> 3; SchoolClassLevel.CLASS_8 -> 2; SchoolClassLevel.CLASS_7 -> 1 }
}

data class ProductionDashboardRow(val batchId: String, val contentOwner: SchoolSubject, val curriculumNodeId: String, val status: CurriculumImplementationStatus, val requiredAssets: Set<RequiredAssetType>, val completedAssets: Set<RequiredAssetType>, val verificationStatus: ScientificReviewStatus, val blockingIssues: List<String>, val testStatus: String, val navigationStatus: String, val releaseReady: Boolean,val interactionTier:InteractionTier?=null,val interactionCoverageStatus:InteractionCoverageStatus=InteractionCoverageStatus.MISSING,val interactiveAssetCount:Int=0,val guidedModeAvailable:Boolean=false,val exploreModeAvailable:Boolean=false,val challengeModeAvailable:Boolean=false,val predictionAvailable:Boolean=false,val accessibilityAvailable:Boolean=false,val brokenControls:Int=0,val brokenVisualLinks:Int=0,val interactionReleaseReady:Boolean=false)
object CurriculumProductionDashboard {
    fun rows(manifests: List<OfficialCurriculum> = CbseNcert2026Curriculum.manifests): List<ProductionDashboardRow> {
        val official = manifests.flatMap { it.units }.flatMap { it.chapters }.associateBy { it.id }
        return CurriculumProductionBatchPlanner.chapters.map { chapter ->
            val readiness = DerivedChapterCompletion.evaluate(chapter, requireNotNull(official[chapter.curriculumNodeId]))
            val activity=chapter.interactiveActivities.firstOrNull();val profile=chapter.interactionProfiles.firstOrNull();val brokenControls=chapter.interactiveActivities.sumOf{it.controls.count{c->!c.changesModelState||c.affectedVisualIds.isEmpty()}};val brokenLinks=chapter.interactiveActivities.sumOf{it.contentLinks.count{link->link.explanationSectionId !in chapter.explanationSections}};val interactionReady=!chapter.requirements.requiresInteractiveVisual||chapter.interactiveActivities.any{it.functionalFor(chapter.subject)}
            ProductionDashboardRow(if (chapter.curriculumNodeId in CurriculumProductionBatchPlanner.batch02Ids) "ncert-2026-27-production-02" else "ncert-2026-27-production-01", chapter.subject, chapter.curriculumNodeId, if (readiness.complete) CurriculumImplementationStatus.READY else CurriculumImplementationStatus.REVIEW_REQUIRED, chapter.completedAssetTypes + readiness.missing, chapter.completedAssetTypes, chapter.reviewStatus, readiness.issues, "unit and route tests required", if (CurriculumProductionBatchPlanner.routeResolves(chapter.route)) "RESOLVED" else "BROKEN", readiness.complete,profile?.tier,if(interactionReady&&activity!=null)InteractionCoverageStatus.COMPLETE else if(activity!=null)InteractionCoverageStatus.BROKEN else InteractionCoverageStatus.MISSING,chapter.interactiveActivities.size,activity?.supportedModes?.contains(InteractionMode.GUIDED)==true,activity?.supportedModes?.contains(InteractionMode.EXPLORE)==true,activity?.supportedModes?.contains(InteractionMode.CHALLENGE)==true,activity?.predictionPrompts?.isNotEmpty()==true,activity?.accessibility?.summary?.isNotBlank()==true,brokenControls,brokenLinks,interactionReady&&brokenControls==0&&brokenLinks==0)
        }
    }
}
