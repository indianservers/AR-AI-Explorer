package com.indianservers.aiexplorer.curriculum

import java.time.LocalDate

enum class EducationBoard { CBSE_NCERT }
enum class SchoolClassLevel(val number: Int) { CLASS_7(7), CLASS_8(8), CLASS_9(9), CLASS_10(10), CLASS_11(11), CLASS_12(12) }
enum class SchoolSubject { MATHEMATICS, PHYSICS, CHEMISTRY, BIOLOGY }
enum class ScienceDiscipline { PHYSICS, CHEMISTRY, BIOLOGY, EARTH_AND_ENVIRONMENTAL_SCIENCE, INTERDISCIPLINARY }
enum class CurriculumVerificationStatus { VERIFIED, PARTIALLY_VERIFIED, REQUIRES_MANUAL_REVIEW }

enum class AssessmentStatus {
    INCLUDED_AND_ASSESSABLE, INCLUDED_FOR_ENRICHMENT, PRACTICAL_ONLY, FORMATIVE_ONLY,
    EXCLUDED_FOR_CURRENT_SESSION, DELETED_FROM_CURRENT_EDITION, SOURCE_UNCLEAR, REQUIRES_MANUAL_REVIEW
}

enum class CoverageStatus { COMPLETE, PARTIAL, TITLE_ONLY, INCORRECTLY_MAPPED, DUPLICATED, MISSING, EXCLUDED_BUT_ACTIVE, REQUIRES_REVIEW }
enum class CurriculumContentState { METADATA_ONLY, OUTLINE_READY, LESSON_READY, VISUALS_READY, PRACTICE_READY, ASSESSMENT_READY, PRACTICAL_READY, FULLY_READY, VERIFIED }
enum class RequiredAssetType { LESSON, FORMULA, DERIVATION, DIAGRAM, ACTIVITY, SIMULATION, PRACTICAL, EXPERIMENT, WORKED_EXAMPLE, EXERCISE, QUIZ, COMPETENCY_QUESTION, CASE_STUDY, GRAPH, DATA_INTERPRETATION, REVISION_MATERIAL, INTERACTIVE_VISUAL, PROCESS_VISUALISER, FORMULA_VISUALISER, PREDICTION_ACTIVITY, CHALLENGE_ACTIVITY, ACCESSIBLE_VISUAL_ALTERNATIVE }
enum class CompetencyType { REMEMBERING, UNDERSTANDING, APPLYING, ANALYSING, EVALUATING, CREATING }
enum class CurriculumSourceType { CBSE_CURRICULUM, NCERT_TEXTBOOK, NCERT_ADVISORY, CBSE_PRACTICAL_SYLLABUS, CBSE_ASSESSMENT_SCHEME }

data class CurriculumSource(
    val id: String,
    val title: String,
    val url: String,
    val type: CurriculumSourceType,
    val academicYear: String,
    val accessedOn: LocalDate,
    val verificationStatus: CurriculumVerificationStatus
)

data class CurriculumSourceReference(val sourceId: String, val locator: String, val note: String? = null)

data class CurriculumEdition(
    val board: EducationBoard,
    val academicYear: String,
    val effectiveFrom: LocalDate?,
    val effectiveTo: LocalDate?,
    val sourceDocuments: List<CurriculumSource>,
    val verificationStatus: CurriculumVerificationStatus
)

data class CurriculumSubtopic(val id: String, val officialTitle: String, val assessmentStatus: AssessmentStatus)
data class LearningOutcome(val id: String, val statement: String)

data class CurriculumTopic(
    val id: String,
    val officialTitle: String,
    val subtopics: List<CurriculumSubtopic> = emptyList(),
    val learningOutcomes: List<LearningOutcome> = emptyList(),
    val requiredCompetencies: Set<CompetencyType>,
    val requiredAssets: Set<RequiredAssetType>,
    val currentAssessmentStatus: AssessmentStatus,
    val contentState: CurriculumContentState = CurriculumContentState.METADATA_ONLY
)

data class CurriculumChapter(
    val id: String,
    val officialTitle: String,
    val textbookPart: String? = null,
    val sequence: Int,
    val topics: List<CurriculumTopic>,
    val practicalReferences: List<String> = emptyList(),
    val assessmentStatus: AssessmentStatus,
    val sourceReference: CurriculumSourceReference,
    val integratedScienceChapterId: String? = null,
    val disciplines: Set<ScienceDiscipline> = emptySet()
)

data class CurriculumUnit(
    val id: String,
    val officialTitle: String,
    val sequence: Int,
    val chapters: List<CurriculumChapter>,
    val marksWeightage: Double? = null,
    val periods: Int? = null
)

data class PracticalScheme(val sourceReference: CurriculumSourceReference, val requirements: List<String>)
data class AssessmentScheme(val sourceReference: CurriculumSourceReference, val questionTypes: Set<String>)

data class OfficialCurriculum(
    val id: String,
    val board: EducationBoard,
    val academicYear: String,
    val classLevel: SchoolClassLevel,
    val subject: SchoolSubject,
    val units: List<CurriculumUnit>,
    val practicalScheme: PracticalScheme? = null,
    val assessmentScheme: AssessmentScheme? = null,
    val sourceDocuments: List<CurriculumSource>,
    val verificationStatus: CurriculumVerificationStatus
)

data class MissingRequirement(val assetType: RequiredAssetType, val detail: String, val priority: Int)
data class CurriculumCoverageMapping(
    val curriculumNodeId: String,
    val appContentIds: List<String>,
    val coverageStatus: CoverageStatus,
    val coveragePercentage: Int?,
    val missingRequirements: List<MissingRequirement>,
    val duplicateContentIds: List<String> = emptyList(),
    val reviewNotes: List<String> = emptyList()
)

data class CurriculumAuditSummary(
    val subject: SchoolSubject,
    val assessableChapters: Int,
    val complete: Int,
    val partial: Int,
    val missing: Int,
    val excluded: Int,
    val requiresReview: Int,
    val missingAssets: Map<RequiredAssetType, Int>,
    val evidenceWeightedCoveragePercent: Int?
)

data class CurriculumAuditReport(
    val edition: CurriculumEdition,
    val mappings: List<CurriculumCoverageMapping>,
    val summaries: List<CurriculumAuditSummary>,
    val duplicateMappings: Map<String, List<String>>,
    val ambiguousCandidateReuse: Map<String, List<String>>,
    val incorrectClassMappings: List<String>,
    val generatedOn: LocalDate,
    val limitations: List<String>
)

data class CurriculumStatusOverride(
    val id: String,
    val classLevel: SchoolClassLevel,
    val subject: SchoolSubject,
    val officialScope: String,
    val status: AssessmentStatus,
    val sourceReference: CurriculumSourceReference,
    val note: String
)
