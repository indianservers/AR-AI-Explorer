package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.production.*
import org.junit.Assert.*
import org.junit.Test

class NcertProductionBatch02Test {
    private val manifests=CbseNcert2026Curriculum.manifests
    private val firstIds=setOf("cbse-2026-c9-mathematics-c3","cbse-2026-c9-physics-c1","cbse-2026-c10-chemistry-c1","cbse-2026-c10-biology-c1")
    private val secondIds=CurriculumProductionBatchPlanner.batch02Ids.toSet()
    private val before=CurriculumCoverageAuditor.audit(manifests,ProjectCurriculumInventory.build(true,firstIds))
    private val after=CurriculumCoverageAuditor.audit(manifests,ProjectCurriculumInventory.build())
    private val official=manifests.flatMap{it.units}.flatMap{it.chapters}.associateBy{it.id}
    private val chapters=CurriculumProductionBatchPlanner.chapters.filter{it.curriculumNodeId in secondIds}

    @Test fun secondBatchSelectionIsDeterministicAndSubjectBalanced(){val b=CurriculumProductionBatchPlanner.selectSecondProductionBatch(before);assertEquals("ncert-2026-27-production-02",b.id);assertEquals(4,b.estimatedLessonCount);assertEquals(SchoolSubject.entries.toSet(),b.subjectItems.keys)}
    @Test fun fourMoreExactChaptersBecomeComplete(){assertEquals(4,chapters.size);chapters.forEach{assertTrue(it.id,DerivedChapterCompletion.evaluate(it,official.getValue(it.curriculumNodeId)).complete)};assertEquals(4,after.mappings.count{it.coverageStatus==CoverageStatus.COMPLETE}-before.mappings.count{it.coverageStatus==CoverageStatus.COMPLETE})}
    @Test fun newDiagramsAreRenderedAndAccessible(){chapters.flatMap{it.diagrams}.forEach{assertTrue(it.id in CurriculumRenderedDiagramRegistry.ids);assertTrue(it.accessibilityDescription.length>80);assertEquals(ScientificReviewStatus.VERIFIED,it.verificationStatus)}}
    @Test fun numericalPhysicsHasUnitsFormulaGraphAndPractical(){val p=chapters.single{it.subject==SchoolSubject.PHYSICS};assertTrue(RequiredAssetType.FORMULA in p.completedAssetTypes);assertTrue(RequiredAssetType.GRAPH in p.completedAssetTypes);assertTrue(p.formulaLinks.all{it.units.isNotEmpty()&&it.assumptions.isNotEmpty()});assertTrue(p.practicals.single().physicalLabRequired)}
    @Test fun chemistryAndBiologyDoNotInventFormulas(){assertTrue(chapters.filter{it.subject in setOf(SchoolSubject.CHEMISTRY,SchoolSubject.BIOLOGY)}.all{it.formulaLinks.isEmpty()})}
    @Test fun questionsAreDiverseAndRemediationResolves(){chapters.forEach{c->assertTrue(c.questions.map{it.questionType}.distinct().size>=4);assertTrue(c.questions.all{it.remediationSectionId in c.explanationSections})}}
    @Test fun batch02RoutesAndDashboardAreReady(){chapters.forEach{assertTrue(CurriculumProductionBatchPlanner.routeResolves(it.route))};val rows=CurriculumProductionDashboard.rows().filter{it.batchId=="ncert-2026-27-production-02"};assertEquals(4,rows.size);assertTrue(rows.all{it.releaseReady})}
    @Test fun batch02ImprovesCoverageWithoutDuplicateMappings(){assertTrue(after.duplicateMappings.isEmpty());val bp=before.mappings.sumOf{it.coveragePercentage?:0};val ap=after.mappings.sumOf{it.coveragePercentage?:0};assertTrue(ap>bp);println("BATCH02_BEFORE="+before.summaries.joinToString(";"){"${it.subject}:${it.evidenceWeightedCoveragePercent}:${it.complete}:${it.partial}:${it.missing}"});println("BATCH02_AFTER="+after.summaries.joinToString(";"){"${it.subject}:${it.evidenceWeightedCoveragePercent}:${it.complete}:${it.partial}:${it.missing}"});println("BATCH02_EVIDENCE_POINTS=$bp->$ap");println("BATCH02_QUESTION_TYPES="+chapters.flatMap{it.questions}.groupingBy{it.questionType}.eachCount())}
}
