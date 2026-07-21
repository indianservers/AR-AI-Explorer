package com.indianservers.aiexplorer.curriculum

import com.indianservers.aiexplorer.curriculum.experience.ConceptExperienceRegistry
import com.indianservers.aiexplorer.curriculum.interaction.ReferenceActivityRegistry
import com.indianservers.aiexplorer.curriculum.interactive.*
import com.indianservers.aiexplorer.learningintelligence.testing.LearningIntelligenceValidator
import com.indianservers.aiexplorer.learningworkspace.Phase2Validator
import com.indianservers.aiexplorer.phase3.Phase3ReleaseValidator

data class CurriculumValidationReport(val errors: List<String>, val warnings: List<String>) { val valid: Boolean get() = errors.isEmpty() }

object CurriculumValidator {
    private val nonAssessable = setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION)

    fun validate(manifests: List<OfficialCurriculum>, inventory: ProjectContentInventory): CurriculumValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val expected = buildSet {
            SchoolClassLevel.entries.forEach { level ->
                add(level to SchoolSubject.MATHEMATICS)
                if (level.number <= 10) addAll(listOf(SchoolSubject.PHYSICS, SchoolSubject.CHEMISTRY, SchoolSubject.BIOLOGY).map { level to it })
                else addAll(listOf(SchoolSubject.PHYSICS, SchoolSubject.CHEMISTRY, SchoolSubject.BIOLOGY).map { level to it })
            }
        }
        val actual = manifests.map { it.classLevel to it.subject }.toSet()
        (expected - actual).forEach { errors += "Missing class-subject manifest: ${it.first.number} ${it.second}" }
        (actual - expected).forEach { errors += "Unsupported class-subject manifest: ${it.first.number} ${it.second}" }
        val ids = manifests.flatMap { m -> listOf(m.id) + m.units.flatMap { u -> listOf(u.id) + u.chapters.flatMap { c -> listOf(c.id) + c.topics.flatMap { t -> listOf(t.id) + t.subtopics.map { it.id } } } } }
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "Duplicate curriculum id: $it" }
        val sourceIds = CbseNcert2026Curriculum.edition.sourceDocuments.map { it.id }.toSet()
        manifests.forEach { manifest ->
            if (manifest.units.isEmpty()) errors += "Empty manifest: ${manifest.id}"
            if (manifest.sourceDocuments.any { it.id !in sourceIds }) errors += "Unregistered source in ${manifest.id}"
            manifest.units.forEach { unit ->
                if (unit.chapters.isEmpty()) errors += "Empty unit: ${unit.id}"
                if (unit.chapters.map { it.sequence } != (1..unit.chapters.size).toList()) errors += "Non-contiguous chapter order: ${unit.id}"
                unit.chapters.forEach { chapter ->
                    if (chapter.sourceReference.sourceId !in sourceIds) errors += "Broken source reference: ${chapter.id}"
                    if (chapter.topics.isEmpty()) errors += "Chapter has no topics: ${chapter.id}"
                    if (manifest.classLevel.number <= 10 && manifest.subject != SchoolSubject.MATHEMATICS && chapter.integratedScienceChapterId == null) warnings += "Integrated Science relationship absent: ${chapter.id}"
                    chapter.topics.forEach { topic ->
                        if (topic.currentAssessmentStatus in nonAssessable && topic.contentState == CurriculumContentState.VERIFIED) errors += "Excluded topic marked verified: ${topic.id}"
                        if (topic.contentState in setOf(CurriculumContentState.FULLY_READY, CurriculumContentState.VERIFIED) && topic.requiredAssets.isEmpty()) errors += "Ready topic lacks completion requirements: ${topic.id}"
                    }
                }
            }
        }
        inventory.assets.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "Duplicate app content id: $it" }
        inventory.assets.filter { it.classNumbers.any { c -> c !in 7..12 } }.forEach { errors += "Incorrect school class mapping: ${it.id}" }
        val interaction=validateInteractions(manifests,CurriculumInteractionBridge.activities)
        errors += interaction.errors;warnings += interaction.warnings
        errors += LearningIntelligenceValidator.validate().errors
        errors += Phase2Validator.validate().errors
        errors += Phase3ReleaseValidator.validate().errors
        return CurriculumValidationReport(errors, warnings)
    }

    fun validateInteractions(manifests:List<OfficialCurriculum>,activities:List<CurriculumInteractiveActivity>):CurriculumValidationReport{
        val errors=mutableListOf<String>();val warnings=mutableListOf<String>();val officialIds=manifests.flatMap{it.units}.flatMap{it.chapters}.flatMap{it.topics}.map{it.id}.toSet();val experienceIds=ConceptExperienceRegistry.experiences.map{it.conceptId}.toSet();val validConceptIds=officialIds+experienceIds
        activities.groupingBy{it.id}.eachCount().filterValues{it>1}.keys.forEach{errors += "Duplicate interaction id: $it"};activities.groupingBy{it.route}.eachCount().filterValues{it>1}.keys.forEach{errors += "Duplicate interaction route: $it"}
        activities.forEach{activity->
            if(activity.conceptId !in validConceptIds)errors += "Invalid interaction concept id: ${activity.id}/${activity.conceptId}"
            if(activity.route.isBlank()||"/${activity.subject.name.lowercase()}/" !in activity.route)errors += "Invalid interaction route: ${activity.id}"
            if(activity.supportedModes.isEmpty())errors += "Interaction has no mode: ${activity.id}"
            if(activity.learningObjectives.isEmpty())errors += "Interaction has no learning objective: ${activity.id}"
            activity.controls.groupingBy{it.id}.eachCount().filterValues{it>1}.keys.forEach{errors += "Duplicate control id in ${activity.id}: $it"}
            if(activity.controls.any{!it.changesModelState||it.affectedVisualIds.isEmpty()})errors += "Disconnected interaction control: ${activity.id}"
            val controls=activity.controls.map{it.id}.toSet();activity.presets.forEach{preset->if(preset.values.keys.any{it !in controls})errors += "Invalid preset ${preset.id} in ${activity.id}"}
            if(activity.challenges.any{it.instruction.isBlank()||it.successConditionId.isBlank()})errors += "Challenge lacks success criteria: ${activity.id}"
            if(activity.accessibility.summary.isBlank()||activity.accessibility.dynamicStateTemplate.isBlank()||activity.accessibility.nonAnimatedSteps.isEmpty())errors += "Interaction lacks accessible state: ${activity.id}"
            val experience=ConceptExperienceRegistry.get(activity.conceptId);val sections=experience?.contentSections?.map{it.id}.orEmpty().toSet();if(experience!=null&&activity.contentLinks.any{it.explanationSectionId !in sections})errors += "Broken explanation link: ${activity.id}"
            val source=ReferenceActivityRegistry.definitions.singleOrNull{it.activityId==activity.id};val targets=source?.let{d->(d.controls.map{it.id}+d.diagramParts.map{it.id}).toSet()}.orEmpty();if(activity.contentLinks.mapNotNull{it.visualTargetId}.any{it !in targets})errors += "Broken visual target: ${activity.id}"
            if(activity.reviewStatus==com.indianservers.aiexplorer.curriculum.production.ScientificReviewStatus.VERIFIED&&!activity.functionalFor(activity.subject))errors += "Verified interaction is not functional: ${activity.id}"
        }
        return CurriculumValidationReport(errors,warnings)
    }

    fun completionDenominator(manifest: OfficialCurriculum): Int = manifest.units.sumOf { unit -> unit.chapters.count { it.assessmentStatus == AssessmentStatus.INCLUDED_AND_ASSESSABLE } }
}

object CurriculumDeveloperDashboard {
    data class Row(
        val board: EducationBoard,
        val academicYear: String,
        val classNumber: Int,
        val subject: SchoolSubject,
        val totalOfficialChapters: Int,
        val complete: Int,
        val partial: Int,
        val missing: Int,
        val excluded: Int,
        val requiresReview: Int,
        val missingDiagrams: Int,
        val missingPracticals: Int,
        val missingAssessments: Int,
        val brokenLinks: Int,
        val lastVerificationDate: String
    )

    fun rows(report: CurriculumAuditReport, manifests: List<OfficialCurriculum> = CbseNcert2026Curriculum.manifests): List<Row> {
        val byId = report.mappings.associateBy { it.curriculumNodeId }
        return manifests.map { m ->
            val chapters = m.units.flatMap { it.chapters }
            val mappings = chapters.mapNotNull { byId[it.id] }
            Row(m.board, m.academicYear, m.classLevel.number, m.subject, chapters.size,
                mappings.count { it.coverageStatus == CoverageStatus.COMPLETE },
                mappings.count { it.coverageStatus in setOf(CoverageStatus.PARTIAL, CoverageStatus.TITLE_ONLY) },
                mappings.count { it.coverageStatus == CoverageStatus.MISSING },
                chapters.count { it.assessmentStatus in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION) },
                mappings.count { it.coverageStatus == CoverageStatus.REQUIRES_REVIEW },
                mappings.sumOf { it.missingRequirements.count { missing -> missing.assetType == RequiredAssetType.DIAGRAM } },
                mappings.sumOf { it.missingRequirements.count { missing -> missing.assetType == RequiredAssetType.PRACTICAL } },
                mappings.sumOf { it.missingRequirements.count { missing -> missing.assetType in setOf(RequiredAssetType.QUIZ, RequiredAssetType.COMPETENCY_QUESTION) } },
                0, report.generatedOn.toString())
        }
    }
}
