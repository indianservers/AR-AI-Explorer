package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.curriculum.*
import org.junit.Assert.*
import org.junit.Test

class NcertCurriculumPhase1And2Test {
    private val manifests = CbseNcert2026Curriculum.manifests
    private val inventory = ProjectCurriculumInventory.build()
    private val audit = CurriculumCoverageAuditor.audit(manifests, inventory)

    @Test fun all24ClassSubjectManifestsExist() {
        assertEquals(24, manifests.size)
        assertEquals(24, manifests.map { it.classLevel to it.subject }.distinct().size)
        assertEquals((7..12).toSet(), manifests.filter { it.subject == SchoolSubject.MATHEMATICS }.map { it.classLevel.number }.toSet())
    }

    @Test fun activeEditionUsesOfficial2026_27Sources() {
        val edition = CbseNcert2026Curriculum.edition
        assertEquals("2026-27", edition.academicYear)
        assertTrue(edition.sourceDocuments.all { it.url.startsWith("https://") })
        assertTrue(edition.sourceDocuments.all { "ncert.nic.in" in it.url || "cbseacademic.nic.in" in it.url })
    }

    @Test fun manifestsAndSourceReferencesValidate() {
        val result = CurriculumValidator.validate(manifests, inventory)
        assertTrue(result.errors.joinToString("\n"), result.valid)
    }

    @Test fun integratedScienceIsOwnedBySeparateSubjectModules() {
        val integrated = manifests.filter { it.classLevel.number <= 10 && it.subject != SchoolSubject.MATHEMATICS }
        assertEquals(12, integrated.size)
        assertTrue(integrated.flatMap { it.units }.flatMap { it.chapters }.all { it.integratedScienceChapterId != null && it.disciplines.isNotEmpty() })
    }

    @Test fun everyOfficialChapterHasAnAuditMapping() {
        val chapterIds = manifests.flatMap { it.units }.flatMap { it.chapters }.map { it.id }.toSet()
        assertEquals(chapterIds, audit.mappings.map { it.curriculumNodeId }.toSet())
        assertTrue(audit.mappings.all { it.coverageStatus != CoverageStatus.COMPLETE || it.missingRequirements.isEmpty() })
    }

    @Test fun titleOnlyOrAmbiguousEvidenceIsNeverComplete() {
        assertTrue(audit.mappings.filter { it.coveragePercentage == null }.all { it.coverageStatus != CoverageStatus.COMPLETE })
        assertTrue(audit.limitations.any { "keyword" in it.lowercase() })
    }

    @Test fun excludedAndFormativeTopicsDoNotInflateCompletion() {
        val chemistry10 = manifests.single { it.classLevel == SchoolClassLevel.CLASS_10 && it.subject == SchoolSubject.CHEMISTRY }
        val periodic = chemistry10.units.flatMap { it.chapters }.single { it.officialTitle == "Periodic Classification of Elements" }
        assertEquals(AssessmentStatus.FORMATIVE_ONLY, periodic.assessmentStatus)
        assertEquals(4, CurriculumValidator.completionDenominator(chemistry10))
        assertTrue(CbseNcert2026Curriculum.currentSessionOverrides.any { it.status == AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION })
    }

    @Test fun criticalGapBatchIsClassAndSubjectScoped() {
        assertEquals(7, CriticalGapAssets.assets.size)
        assertEquals(7, CriticalGapAssets.content.size)
        assertTrue(CriticalGapAssets.content.all { it.explanation.length > 100 && it.learningObjectives.isNotEmpty() && it.practicePrompt.isNotBlank() })
        assertTrue(CriticalGapAssets.assets.all { it.classNumbers.size == 1 && it.substantive })
        assertTrue(CriticalGapAssets.assets.any { it.id.contains("practical") })
        assertTrue(CriticalGapAssets.assets.any { RequiredAssetType.DIAGRAM in it.assetTypes })
    }

    @Test fun developerDashboardAndCsvAreExportable() {
        assertEquals(24, CurriculumDeveloperDashboard.rows(audit).size)
        val csv = CurriculumCoverageAuditor.toCsv(audit)
        assertTrue(csv.startsWith("curriculumNodeId,"))
        assertTrue(csv.lineSequence().count() > 100)
        println("CURRICULUM_SUMMARIES=" + audit.summaries.joinToString(";") { "${it.subject}:${it.evidenceWeightedCoveragePercent}:${it.assessableChapters}:${it.complete}:${it.partial}:${it.missing}:${it.excluded}:${it.requiresReview}" })
        println("CURRICULUM_MISSING_ASSETS=" + audit.summaries.joinToString(";") { "${it.subject}:${it.missingAssets}" })
        println("CURRICULUM_DUPLICATES=${audit.duplicateMappings.size}")
        println("CURRICULUM_AMBIGUOUS_REUSE=${audit.ambiguousCandidateReuse.size}")
    }

    @Test fun advancedAssetsAreNotAutomaticallyLabelledNcert() {
        assertTrue(inventory.assets.none { "undergraduate" in it.id && it.classNumbers.isNotEmpty() })
        assertTrue(audit.mappings.all { mapping -> mapping.appContentIds.all { id -> inventory.assets.single { it.id == id }.subject == manifests.flatMap { m -> m.units.flatMap { u -> u.chapters.map { it.id to m.subject } } }.toMap()[mapping.curriculumNodeId] } })
    }
}
