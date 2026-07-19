package com.indianservers.aiexplorer.curriculum

import com.indianservers.aiexplorer.biology.data.BundledBiologyCatalogue
import com.indianservers.aiexplorer.chemistry.formulas.data.BundledChemistryFormulaData
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import com.indianservers.aiexplorer.physics.formulas.data.BundledPhysicsFormulaData
import com.indianservers.aiexplorer.curriculum.production.CurriculumProductionBatchPlanner
import java.time.LocalDate

data class AppContentAsset(
    val id: String,
    val subject: SchoolSubject,
    val title: String,
    val searchableText: String,
    val assetTypes: Set<RequiredAssetType>,
    val classNumbers: Set<Int> = emptySet(),
    val substantive: Boolean = true,
    val sourceArea: String,
    val curriculumNodeIds: Set<String> = emptySet()
)

data class ProjectContentInventory(
    val schemaVersion: Int,
    val generatedOn: LocalDate,
    val assets: List<AppContentAsset>,
    val architectureNotes: List<String>
)

data class CriticalCurriculumContent(
    val asset: AppContentAsset,
    val learningObjectives: List<String>,
    val explanation: String,
    val visualOrProcedure: List<String>,
    val workedExampleOrObservation: String,
    val practicePrompt: String,
    val reviewStatus: CurriculumVerificationStatus
)

/** Small, independently written and reviewed Phase-2 batch. These are resolvable content records, not learner routes. */
object CriticalGapAssets {
    private fun content(asset: AppContentAsset, objectives: List<String>, explanation: String, visual: List<String>, example: String, prompt: String) =
        CriticalCurriculumContent(asset, objectives, explanation, visual, example, prompt, CurriculumVerificationStatus.PARTIALLY_VERIFIED)

    val content = listOf(
        content(AppContentAsset("ncert-c9-math-sequences-core", SchoolSubject.MATHEMATICS, "Sequences and Progressions", "sequence term pattern arithmetic progression common difference visual number pattern", setOf(RequiredAssetType.LESSON, RequiredAssetType.FORMULA, RequiredAssetType.WORKED_EXAMPLE, RequiredAssetType.COMPETENCY_QUESTION, RequiredAssetType.REVISION_MATERIAL), setOf(9), true, "curriculum-critical-gap"), listOf("Recognise a rule connecting term number and term value.", "Represent an arithmetic progression using first term and common difference."), "A sequence is an ordered list, so position matters. In an arithmetic progression the change between consecutive terms is constant; starting at a and moving by d gives the nth term a + (n - 1)d.", listOf("Plot term number horizontally and term value vertically.", "Join successive points only to expose the constant vertical change; the sequence itself remains discrete."), "For 5, 8, 11, …, a = 5 and d = 3, so term 10 is 5 + 9×3 = 32.", "A pattern starts 14, 10, 6, … . Explain whether it is arithmetic and find term 20."),
        content(AppContentAsset("ncert-c9-math-identities-area-model", SchoolSubject.MATHEMATICS, "Exploring Algebraic Identities", "algebraic identity square binomial area model expansion factorisation", setOf(RequiredAssetType.LESSON, RequiredAssetType.DIAGRAM, RequiredAssetType.WORKED_EXAMPLE), setOf(9), true, "curriculum-critical-gap"), listOf("Connect multiplication of binomials to area.", "Justify rather than memorise the square identity."), "A square of side a + b can be partitioned into an a-by-a square, two a-by-b rectangles, and a b-by-b square. Since the pieces exactly tile the same outer square, (a+b)² = a² + 2ab + b².", listOf("Draw the outer square and one vertical and one horizontal cut at distance a.", "Use equal-area labels on the two rectangles and mark 'area unchanged'."), "With a = 3 and b = 2, the outer area 25 equals 9 + 6 + 6 + 4.", "Explain visually why the middle term cannot be ab."),
        content(AppContentAsset("ncert-c9-physics-motion-graph-practical", SchoolSubject.PHYSICS, "Describing Motion Around Us", "motion distance displacement speed velocity acceleration motion graphs observation table", setOf(RequiredAssetType.LESSON, RequiredAssetType.GRAPH, RequiredAssetType.PRACTICAL, RequiredAssetType.WORKED_EXAMPLE), setOf(9), true, "curriculum-critical-gap"), listOf("Read slope as rate of change on a position–time graph.", "Record time and position with units and discuss uncertainty."), "A position–time graph records where an object is at successive times. Its slope is change in position divided by change in time; a steeper straight segment represents a larger constant velocity.", listOf("Mark a straight walking path and equal position intervals.", "Record clock time at each mark in a table, plot points, and draw a best-fit trend.", "Repeat; do not invent missing readings. This guided activity does not replace a required physical practical."), "A position change from 2 m to 8 m between 1 s and 4 s gives average velocity (8−2)/(4−1) = 2 m/s.", "Compare two plotted walks and justify which was faster using slope, including units."),
        content(AppContentAsset("ncert-c9-biology-earth-system-visual", SchoolSubject.BIOLOGY, "Earth as a System: Energy, Matter, and Life", "earth system biosphere atmosphere hydrosphere geosphere energy matter cycle interactions", setOf(RequiredAssetType.LESSON, RequiredAssetType.DIAGRAM, RequiredAssetType.ACTIVITY, RequiredAssetType.COMPETENCY_QUESTION), setOf(9), true, "curriculum-critical-gap"), listOf("Trace matter between Earth-system spheres.", "Distinguish cycling matter from mostly one-way incoming and outgoing energy."), "The atmosphere, hydrosphere, geosphere and biosphere exchange matter and energy. A change in one sphere can propagate: vegetation loss can alter water movement, soil stability and local energy balance.", listOf("Place the four spheres around a central boundary and draw labelled two-way matter arrows.", "Use a distinct one-way sunlight arrow and an outgoing heat arrow; provide the same relationships as text."), "In a forested catchment, roots help hold soil and vegetation slows runoff; clearing can therefore increase erosion and change stream flow.", "Assume tree cover in a catchment falls sharply. Construct a cause chain involving at least three Earth-system spheres."),
        content(AppContentAsset("ncert-c11-physics-measurement-practical", SchoolSubject.PHYSICS, "Units and Measurements", "units measurement significant figures uncertainty instrument observations graph practical", setOf(RequiredAssetType.LESSON, RequiredAssetType.PRACTICAL, RequiredAssetType.GRAPH, RequiredAssetType.WORKED_EXAMPLE), setOf(11), true, "curriculum-critical-gap"), listOf("Record instrument resolution and repeated observations.", "Report a measured result with a justified uncertainty and unit."), "A measurement is incomplete without a unit and an account of precision. Repeated readings reveal spread, while the least count constrains the resolution that the instrument can support.", listOf("Inspect zero error and least count before measuring.", "Take repeated readings without discarding inconvenient values, tabulate them with units, calculate a mean, and report uncertainty.", "Follow the laboratory's instrument-specific safety instructions."), "Readings 12.4, 12.5 and 12.4 cm have mean 12.43 cm; the reported digits must still respect instrument resolution.", "Explain why writing 12.433333 cm from a millimetre-scale ruler is unjustified."),
        content(AppContentAsset("ncert-c12-chemistry-volumetric-practical", SchoolSubject.CHEMISTRY, "Solutions volumetric analysis", "solutions concentration molarity titration volumetric analysis observation calculation safety", setOf(RequiredAssetType.PRACTICAL, RequiredAssetType.ACTIVITY, RequiredAssetType.WORKED_EXAMPLE), setOf(12), true, "curriculum-critical-gap"), listOf("Use a balanced equation to relate reacting amounts.", "Record burette readings and concordant titres with correct units."), "Volumetric analysis finds an unknown concentration from a measured reacting volume and a known standard. The balanced equation supplies the mole ratio; the endpoint is an observed indicator change, not a reason to add excess reagent.", listOf("Wear eye protection and follow the laboratory's reagent-specific hazard guidance.", "Rinse each apparatus with the appropriate liquid, remove the funnel before reading, read the meniscus at eye level, record initial and final readings, and repeat to obtain concordant titres.", "This guide does not substitute for supervised laboratory work."), "For a 1:1 reaction, 20.00 mL of 0.100 mol L⁻¹ standard reacting with 25.00 mL sample implies 0.0800 mol L⁻¹ sample.", "Identify two procedural errors that would bias the titre and predict the direction of each bias."),
        content(AppContentAsset("ncert-c12-biology-inheritance-cross-diagram", SchoolSubject.BIOLOGY, "Principles of Inheritance and Variation", "inheritance variation monohybrid dihybrid cross pedigree probability genetics", setOf(RequiredAssetType.LESSON, RequiredAssetType.DIAGRAM, RequiredAssetType.ACTIVITY, RequiredAssetType.COMPETENCY_QUESTION), setOf(12), true, "curriculum-critical-gap"), listOf("Represent allele segregation with a probability model.", "Separate genotype from phenotype and state assumptions."), "A Punnett grid organises possible gamete combinations. It predicts probabilities for many independently formed offspring; it does not prescribe the outcome of a particular birth.", listOf("Put one parent's gametes on rows and the other's on columns.", "Combine one allele from each parent in each cell and label both genotype and phenotype under the stated dominance model.", "Provide a text table equivalent for accessibility."), "For Aa × Aa, the genotype probabilities are 1/4 AA, 1/2 Aa and 1/4 aa under simple Mendelian assumptions.", "Explain why four offspring from Aa × Aa need not show exactly a 3:1 phenotype count.")
    )

    val assets: List<AppContentAsset> = content.map { it.asset }
}

object ProjectCurriculumInventory {
    fun build(includeProductionBatch: Boolean = true, productionCurriculumNodeIds: Set<String>? = null): ProjectContentInventory {
        val assets = mutableListOf<AppContentAsset>()
        MathKnowledgeCatalog.formulas.forEach { f -> assets += AppContentAsset(f.id, SchoolSubject.MATHEMATICS, f.title, listOf(f.title, f.category.label, f.useCase, f.relatedTerms.joinToString()).joinToString(" "), setOf(RequiredAssetType.FORMULA, RequiredAssetType.WORKED_EXAMPLE), sourceArea = "learning/MathKnowledge") }
        MathKnowledgeCatalog.theorems.forEach { t -> assets += AppContentAsset(t.id, SchoolSubject.MATHEMATICS, t.title, "${t.title} ${t.statement} ${t.applications.joinToString()}", setOf(RequiredAssetType.LESSON, RequiredAssetType.DERIVATION), sourceArea = "learning/MathKnowledge") }
        MathKnowledgeCatalog.visualProofs.forEach { v -> assets += AppContentAsset(v.id, SchoolSubject.MATHEMATICS, v.title, "${v.title} ${v.invariant} ${v.constructionSteps.joinToString()}", setOf(RequiredAssetType.DIAGRAM, RequiredAssetType.ACTIVITY), sourceArea = "learning/MathKnowledge") }
        MathKnowledgeCatalog.mcqs.forEach { q -> assets += AppContentAsset(q.id, SchoolSubject.MATHEMATICS, q.prompt, "${q.prompt} ${q.explanation}", setOf(RequiredAssetType.QUIZ), sourceArea = "learning/MathKnowledge") }

        BundledPhysicsFormulaData.catalogue.formulas.forEach { f -> assets += AppContentAsset(f.id, SchoolSubject.PHYSICS, f.title, "${f.title} ${f.description} ${f.keywords.joinToString()} ${f.spokenEquation}", buildSet { add(RequiredAssetType.FORMULA); if (f.workedExamples.isNotEmpty()) add(RequiredAssetType.WORKED_EXAMPLE); if (f.derivationSteps.isNotEmpty()) add(RequiredAssetType.DERIVATION) }, sourceArea = "physics/formulas") }
        assets += AppContentAsset("physics-mechanical-wave-lab", SchoolSubject.PHYSICS, "Mechanical wave laboratory", "waves sound transverse longitudinal wavelength frequency speed amplitude", setOf(RequiredAssetType.LESSON, RequiredAssetType.SIMULATION, RequiredAssetType.DIAGRAM, RequiredAssetType.GRAPH, RequiredAssetType.ACTIVITY), setOf(9, 11), sourceArea = "physics/mechanicalwaves")

        BundledChemistryFormulaData.catalogue.formulas.forEach { f -> assets += AppContentAsset(f.id, SchoolSubject.CHEMISTRY, f.title, "${f.title} ${f.description} ${f.keywords.joinToString()} ${f.spokenEquation}", buildSet { add(RequiredAssetType.FORMULA); if (f.workedExamples.isNotEmpty()) add(RequiredAssetType.WORKED_EXAMPLE); if (f.derivationSteps.isNotEmpty()) add(RequiredAssetType.DERIVATION) }, sourceArea = "chemistry/formulas") }

        val bio = BundledBiologyCatalogue.catalogue
        bio.concepts.forEach { c ->
            val types = buildSet { add(RequiredAssetType.LESSON); if (c.diagramIds.isNotEmpty()) add(RequiredAssetType.DIAGRAM); if (c.quizQuestionIds.isNotEmpty()) add(RequiredAssetType.QUIZ) }
            assets += AppContentAsset(c.id, SchoolSubject.BIOLOGY, c.title, "${c.title} ${c.summary} ${c.keywords.joinToString()}", types, substantive = c.blocks.isNotEmpty(), sourceArea = "biology/catalogue")
        }
        val criticalNodeIds = mapOf(
            "ncert-c9-math-sequences-core" to "cbse-2026-c9-mathematics-c3",
            "ncert-c9-math-identities-area-model" to "cbse-2026-c9-mathematics-c4",
            "ncert-c9-physics-motion-graph-practical" to "cbse-2026-c9-physics-c1",
            "ncert-c9-biology-earth-system-visual" to "cbse-2026-c9-biology-c5",
            "ncert-c11-physics-measurement-practical" to "cbse-2026-c11-physics-c1",
            "ncert-c12-chemistry-volumetric-practical" to "cbse-2026-c12-chemistry-c1",
            "ncert-c12-biology-inheritance-cross-diagram" to "cbse-2026-c12-biology-c4"
        )
        assets += CriticalGapAssets.assets.map { it.copy(curriculumNodeIds = setOf(criticalNodeIds.getValue(it.id))) }
        if (includeProductionBatch) CurriculumProductionBatchPlanner.chapters.filter { productionCurriculumNodeIds == null || it.curriculumNodeId in productionCurriculumNodeIds }.forEach { chapter ->
            assets += AppContentAsset(chapter.id, chapter.subject, chapter.officialChapterTitle, buildString {
                append(chapter.officialChapterTitle); append(' '); append(chapter.explanationSections.values.joinToString(" "))
                append(' '); append(chapter.keyTerms.keys.joinToString(" ")); append(' '); append(chapter.relationships.joinToString(" "))
            }, chapter.completedAssetTypes, setOf(chapter.classLevel.number), substantive = true, sourceArea = "${chapter.subject.name.lowercase()}/ncert", curriculumNodeIds = setOf(chapter.curriculumNodeId))
        }
        return ProjectContentInventory(1, LocalDate.now(), assets.distinctBy { it.id }, listOf(
            "One Compose application with separate Mathematics, Physics, Chemistry and Biology packages/features.",
            "Existing formula and concept catalogues are domain-oriented; most assets do not carry NCERT edition, class, chapter or assessment identity.",
            "Biology has concept, diagram, glossary and quiz metadata; Physics has a wave simulation; Mathematics has formulas, proofs and tools; Chemistry has formula and element catalogues.",
            "Existing progress state is feature-local and is not duplicated by this inventory."
        ))
    }
}

object CurriculumCoverageAuditor {
    private val stopWords = setOf("and", "the", "of", "in", "to", "a", "an", "with", "introduction", "some", "their", "our", "how", "do", "its", "as", "around")
    private fun tokens(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), " ").split(' ').filter { it.length > 2 && it !in stopWords }.toSet()
    private fun matches(chapter: CurriculumChapter, asset: AppContentAsset, classNumber: Int): Boolean {
        if (asset.curriculumNodeIds.isNotEmpty()) return chapter.id in asset.curriculumNodeIds
        if (asset.classNumbers.isNotEmpty() && classNumber !in asset.classNumbers) return false
        val expected = tokens(chapter.officialTitle)
        val actual = tokens(asset.searchableText + " " + asset.title)
        val overlap = expected.intersect(actual)
        return overlap.size >= 2 || (expected.size == 1 && overlap.isNotEmpty()) || overlap.any { it.length >= 8 }
    }

    fun audit(manifests: List<OfficialCurriculum> = CbseNcert2026Curriculum.manifests, inventory: ProjectContentInventory = ProjectCurriculumInventory.build()): CurriculumAuditReport {
        val mappings = manifests.flatMap { manifest -> manifest.units.flatMap { it.chapters }.map { chapter ->
            val excluded = chapter.assessmentStatus in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION)
            val found = inventory.assets.filter { it.subject == manifest.subject && matches(chapter, it, manifest.classLevel.number) }
            val provided = found.filter { it.substantive }.flatMap { it.assetTypes }.toSet()
            val required = chapter.topics.flatMap { it.requiredAssets }.toSet()
            val missing = (required - provided).map { MissingRequirement(it, "${chapter.officialTitle} requires ${it.name.lowercase().replace('_', ' ')} evidence.", priority(it)) }
            val uncertain = manifest.verificationStatus == CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW || chapter.assessmentStatus in setOf(AssessmentStatus.SOURCE_UNCLEAR, AssessmentStatus.REQUIRES_MANUAL_REVIEW)
            val percent = if (uncertain) null else if (required.isEmpty()) 0 else (((provided.intersect(required).size * 100.0 / required.size) / 25).toInt() * 25).coerceIn(0, 100)
            val status = when {
                excluded && found.isNotEmpty() -> CoverageStatus.EXCLUDED_BUT_ACTIVE
                excluded -> CoverageStatus.REQUIRES_REVIEW
                uncertain -> CoverageStatus.REQUIRES_REVIEW
                found.isEmpty() -> CoverageStatus.MISSING
                found.none { it.substantive } -> CoverageStatus.TITLE_ONLY
                missing.isEmpty() -> CoverageStatus.COMPLETE
                else -> CoverageStatus.PARTIAL
            }
            CurriculumCoverageMapping(chapter.id, found.map { it.id }, status, percent, missing, reviewNotes = buildList {
                if (uncertain) add("Official current-edition chapter scope requires manual source confirmation; no numeric coverage is reported.")
                if (found.isNotEmpty()) add("Matched by conservative token evidence; human curriculum review remains required before VERIFIED.")
            })
        }}
        val chapterIndex = manifests.flatMap { m -> m.units.flatMap { u -> u.chapters.map { it.id to (m to it) } } }.toMap()
        val summaries = SchoolSubject.entries.map { subject ->
            val ids = chapterIndex.filterValues { it.first.subject == subject }.keys
            val ms = mappings.filter { it.curriculumNodeId in ids }
            val chapters = ids.mapNotNull(chapterIndex::get).map { it.second }
            val excludedCount = chapters.count { it.assessmentStatus in setOf(AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, AssessmentStatus.DELETED_FROM_CURRENT_EDITION) }
            val assessableIds = ids.filter { chapterIndex.getValue(it).second.assessmentStatus == AssessmentStatus.INCLUDED_AND_ASSESSABLE }.toSet()
            val assessableMappings = ms.filter { it.curriculumNodeId in assessableIds }
            val numeric = assessableMappings.mapNotNull { it.coveragePercentage }
            CurriculumAuditSummary(subject, assessableIds.size, assessableMappings.count { it.coverageStatus == CoverageStatus.COMPLETE }, assessableMappings.count { it.coverageStatus == CoverageStatus.PARTIAL || it.coverageStatus == CoverageStatus.TITLE_ONLY }, assessableMappings.count { it.coverageStatus == CoverageStatus.MISSING }, excludedCount, ms.count { it.coverageStatus == CoverageStatus.REQUIRES_REVIEW }, assessableMappings.flatMap { it.missingRequirements }.groupingBy { it.assetType }.eachCount(), if (numeric.isEmpty()) null else (numeric.average() / 5).toInt() * 5)
        }
        val ownership = chapterIndex.mapValues { (_, pair) -> pair.first.classLevel.number to pair.first.subject }
        val ambiguousReuse = mappings.flatMap { m -> m.appContentIds.map { assetId -> Triple(assetId, m.curriculumNodeId, ownership.getValue(m.curriculumNodeId)) } }
            .groupBy { "${it.first}@class${it.third.first}-${it.third.second}" }
            .mapValues { (_, uses) -> uses.map { it.second }.distinct() }
            .filterValues { it.size > 1 }
        val duplicateMappings = mappings.filter { it.appContentIds.size != it.appContentIds.distinct().size }.associate { it.curriculumNodeId to it.appContentIds }
        return CurriculumAuditReport(CbseNcert2026Curriculum.edition, mappings, summaries, duplicateMappings, ambiguousReuse, emptyList(), LocalDate.now(), listOf(
            "Coverage values are evidence bands rounded down to 25-point chapter increments and 5-point subject increments, not claims of pedagogical verification.",
            "Grade 7 Science and the transitioning Grade 8 Science source require manual verification against the physical/current portal edition.",
            "Practical schemes are represented at scheme level in Phase 1; experiment-by-experiment ingestion remains a high-priority gap.",
            "A keyword match only discovers candidates; COMPLETE requires every declared required asset type."
        ))
    }

    private fun priority(type: RequiredAssetType) = when (type) {
        RequiredAssetType.LESSON -> 1; RequiredAssetType.PRACTICAL, RequiredAssetType.EXPERIMENT -> 2; RequiredAssetType.FORMULA -> 3
        RequiredAssetType.DIAGRAM, RequiredAssetType.GRAPH -> 4; RequiredAssetType.WORKED_EXAMPLE -> 5; RequiredAssetType.COMPETENCY_QUESTION -> 6
        else -> 7
    }

    fun toCsv(report: CurriculumAuditReport): String = buildString {
        appendLine("curriculumNodeId,coverageStatus,coverageBand,appContentIds,missingAssets,reviewNotes")
        report.mappings.forEach { m ->
            fun csv(v: String) = "\"${v.replace("\"", "\"\"")}\""
            appendLine(listOf(m.curriculumNodeId, m.coverageStatus.name, m.coveragePercentage?.toString().orEmpty(), m.appContentIds.joinToString("|"), m.missingRequirements.joinToString("|") { it.assetType.name }, m.reviewNotes.joinToString("|")).joinToString(",", transform = ::csv))
        }
    }
}
