package com.indianservers.aiexplorer.curriculum

import java.time.LocalDate

/** Official-name metadata only. It intentionally contains no copyrighted textbook prose. */
object CbseNcert2026Curriculum {
    private val accessed = LocalDate.of(2026, 7, 19)
    private fun source(id: String, title: String, url: String, type: CurriculumSourceType, status: CurriculumVerificationStatus = CurriculumVerificationStatus.VERIFIED) =
        CurriculumSource(id, title, url, type, "2026-27", accessed, status)

    val cbseIndex = source("cbse-2026-index", "CBSE Curriculum 2026-27", "https://cbseacademic.nic.in/curriculum_2027.html", CurriculumSourceType.CBSE_CURRICULUM)
    val mathSecondary = source("cbse-ix-math", "CBSE Mathematics IX 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart1/Maths_SecP1IX_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val scienceSecondary = source("cbse-ix-science", "CBSE Science IX 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart1/ScienceSt_SecP1_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val mathSecondary10 = source("cbse-x-math", "CBSE Mathematics X 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart1/Maths_SecP1X_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val scienceSecondary10 = source("cbse-x-science", "CBSE Science X 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart1/Science_SecP1_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val mathSenior = source("cbse-xi-xii-math", "CBSE Mathematics XI-XII 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart2/Maths_SecP2_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val physicsSenior = source("cbse-xi-xii-physics", "CBSE Physics XI-XII 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart2/Physics_SecP2_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val chemistrySenior = source("cbse-xi-xii-chemistry", "CBSE Chemistry XI-XII 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart2/Chemistry_SecP2_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val biologySenior = source("cbse-xi-xii-biology", "CBSE Biology XI-XII 2026-27", "https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart2/Biology_SecP2_2026-27.pdf", CurriculumSourceType.CBSE_CURRICULUM)
    val grade7Math = source("ncert-vii-ganita", "NCERT Ganita Prakash Grade 7, Part I, reprint 2026-27", "https://ncert.nic.in/textbook/pdf/gegp1ps.pdf", CurriculumSourceType.NCERT_TEXTBOOK, CurriculumVerificationStatus.PARTIALLY_VERIFIED)
    val grade7Science = source("ncert-vii-science", "NCERT Science Grade 7 portal edition", "https://ncert.nic.in/textbook.php?gesc1=0-13", CurriculumSourceType.NCERT_TEXTBOOK, CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW)
    val grade8Math = source("ncert-viii-ganita", "NCERT Ganita Prakash Grade 8, Part I, reprint 2026-27", "https://ncert.nic.in/textbook.php?hegp1=0-7", CurriculumSourceType.NCERT_TEXTBOOK, CurriculumVerificationStatus.PARTIALLY_VERIFIED)
    val grade8Bridge = source("ncert-viii-bridge", "NCERT Grade 8 Science Bridge Programme", "https://www.ncert.nic.in/pdf/Bridge_Programme/Grade8/Bridge_Programme-Science-Grade_8.pdf", CurriculumSourceType.NCERT_ADVISORY, CurriculumVerificationStatus.PARTIALLY_VERIFIED)

    val edition = CurriculumEdition(
        EducationBoard.CBSE_NCERT, "2026-27", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
        listOf(cbseIndex, mathSecondary, scienceSecondary, mathSecondary10, scienceSecondary10, mathSenior, physicsSenior, chemistrySenior, biologySenior, grade7Math, grade7Science, grade8Math, grade8Bridge),
        CurriculumVerificationStatus.PARTIALLY_VERIFIED
    )

    /** Kept separately from assessable nodes so it cannot inflate current-syllabus completion. */
    val currentSessionOverrides = listOf(
        CurriculumStatusOverride("cbse-2026-c10-chem-periodic-formative", SchoolClassLevel.CLASS_10, SchoolSubject.CHEMISTRY, "Periodic Classification of Elements", AssessmentStatus.FORMATIVE_ONLY, CurriculumSourceReference(scienceSecondary10.id, "Class X course structure/status note"), "Retained with formative status and excluded from the assessable completion denominator."),
        CurriculumStatusOverride("cbse-2026-c11-bio-biomolecules-exclusions", SchoolClassLevel.CLASS_11, SchoolSubject.BIOLOGY, "Biomolecules textbook subsections explicitly excluded in the syllabus", AssessmentStatus.EXCLUDED_FOR_CURRENT_SESSION, CurriculumSourceReference(biologySenior.id, "Class XI, Unit III, Biomolecules exclusion note"), "Preserve as previous-edition/enrichment material and exclude from current completion."),
        CurriculumStatusOverride("cbse-2026-physics-textbook-exclusions", SchoolClassLevel.CLASS_12, SchoolSubject.PHYSICS, "NCERT textbook content explicitly excluded for 2026-27", AssessmentStatus.REQUIRES_MANUAL_REVIEW, CurriculumSourceReference(physicsSenior.id, "general assessment note on NCERT exclusions"), "The syllabus delegates exact scope to NCERT exclusion markings; ingest line items before claiming topic-level verification.")
    )

    private val commonCompetencies = setOf(CompetencyType.UNDERSTANDING, CompetencyType.APPLYING, CompetencyType.ANALYSING)
    private fun assets(subject: SchoolSubject): Set<RequiredAssetType> = when (subject) {
        SchoolSubject.MATHEMATICS -> setOf(RequiredAssetType.LESSON, RequiredAssetType.WORKED_EXAMPLE, RequiredAssetType.EXERCISE, RequiredAssetType.COMPETENCY_QUESTION, RequiredAssetType.REVISION_MATERIAL)
        SchoolSubject.PHYSICS -> setOf(RequiredAssetType.LESSON, RequiredAssetType.FORMULA, RequiredAssetType.DIAGRAM, RequiredAssetType.GRAPH, RequiredAssetType.WORKED_EXAMPLE, RequiredAssetType.PRACTICAL, RequiredAssetType.COMPETENCY_QUESTION)
        SchoolSubject.CHEMISTRY -> setOf(RequiredAssetType.LESSON, RequiredAssetType.DIAGRAM, RequiredAssetType.ACTIVITY, RequiredAssetType.PRACTICAL, RequiredAssetType.COMPETENCY_QUESTION)
        SchoolSubject.BIOLOGY -> setOf(RequiredAssetType.LESSON, RequiredAssetType.DIAGRAM, RequiredAssetType.ACTIVITY, RequiredAssetType.PRACTICAL, RequiredAssetType.COMPETENCY_QUESTION)
    }

    private fun manifest(
        level: SchoolClassLevel,
        subject: SchoolSubject,
        source: CurriculumSource,
        chapters: List<String>,
        verification: CurriculumVerificationStatus = if (source.verificationStatus == CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW) CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW else CurriculumVerificationStatus.PARTIALLY_VERIFIED,
        excluded: Map<String, AssessmentStatus> = emptyMap(),
        integratedPrefix: String? = null,
        discipline: ScienceDiscipline? = null,
        practical: Boolean = false
    ): OfficialCurriculum {
        val slugSubject = subject.name.lowercase()
        val chapterModels = chapters.mapIndexed { index, title ->
            val chapterId = "cbse-2026-c${level.number}-$slugSubject-c${index + 1}"
            val status = excluded[title] ?: if (verification == CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW) AssessmentStatus.REQUIRES_MANUAL_REVIEW else AssessmentStatus.INCLUDED_AND_ASSESSABLE
            val lowerTitle = title.lowercase()
            val mathDiagram = subject == SchoolSubject.MATHEMATICS && listOf("geometry", "trigon", "coordinate", "graph", "mensuration", "triangle", "circle", "quadrilateral", "area", "three-dimensional").any(lowerTitle::contains)
            val mathFormula = subject == SchoolSubject.MATHEMATICS && listOf("algebra", "polynomial", "sequence", "progression", "mensuration", "area", "volume", "trigon", "statistics", "probability", "quadratic", "binomial", "complex", "line", "conic", "limit", "derivative", "integral", "matrix", "determinant", "vector", "differential").any(lowerTitle::contains)
            val chemistryFormula = subject == SchoolSubject.CHEMISTRY && listOf("basic concepts", "atom", "thermodynamics", "equilibrium", "redox", "solutions", "electrochemistry", "kinetics").any(lowerTitle::contains)
            val interactive = when(subject){
                SchoolSubject.MATHEMATICS -> listOf("linear equation","triangle","coordinate geometry","trigonometry","probability").any(lowerTitle::contains)
                SchoolSubject.PHYSICS -> listOf("motion","force","wave","sound","electricity","light","optics").any(lowerTitle::contains)
                SchoolSubject.CHEMISTRY -> listOf("atom","periodicity","periodic classification","bonding","gas law","acid","equilibrium").any(lowerTitle::contains)
                SchoolSubject.BIOLOGY -> listOf("fundamental unit","cell: the unit","cell cycle","circulation","life processes","inheritance","heredity").any(lowerTitle::contains)
            }
            val interactiveAssets=if(interactive)setOf(RequiredAssetType.INTERACTIVE_VISUAL,RequiredAssetType.PREDICTION_ACTIVITY,RequiredAssetType.CHALLENGE_ACTIVITY,RequiredAssetType.ACCESSIBLE_VISUAL_ALTERNATIVE) else emptySet()
            val required = assets(subject) + (if (mathDiagram) setOf(RequiredAssetType.DIAGRAM) else emptySet()) + (if (mathFormula || chemistryFormula) setOf(RequiredAssetType.FORMULA) else emptySet()) + interactiveAssets
            val effectiveDiscipline = discipline ?: when (subject) {
                SchoolSubject.PHYSICS -> ScienceDiscipline.PHYSICS
                SchoolSubject.CHEMISTRY -> ScienceDiscipline.CHEMISTRY
                SchoolSubject.BIOLOGY -> ScienceDiscipline.BIOLOGY
                SchoolSubject.MATHEMATICS -> null
            }
            val sciencePrefix = integratedPrefix ?: if (level.number <= 10 && subject != SchoolSubject.MATHEMATICS) "ncert-c${level.number}-science" else null
            CurriculumChapter(
                chapterId, title, sequence = index + 1,
                topics = listOf(CurriculumTopic("$chapterId-core", "Core concepts and prescribed scope", requiredCompetencies = commonCompetencies, requiredAssets = required, currentAssessmentStatus = status)),
                practicalReferences = if (practical) listOf("$chapterId-practical-scheme") else emptyList(),
                assessmentStatus = status,
                sourceReference = CurriculumSourceReference(source.id, "chapter/unit ${index + 1}"),
                integratedScienceChapterId = sciencePrefix?.let { prefix -> "$prefix-${title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}" },
                disciplines = effectiveDiscipline?.let(::setOf) ?: emptySet()
            )
        }
        val unit = CurriculumUnit("cbse-2026-c${level.number}-$slugSubject-u1", "Prescribed course", 1, chapterModels)
        val practicalScheme = if (practical) PracticalScheme(CurriculumSourceReference(source.id, "practical syllabus"), listOf("record work", "experiments/activities", "project", "viva", "safety and observations")) else null
        return OfficialCurriculum("cbse-2026-c${level.number}-$slugSubject", EducationBoard.CBSE_NCERT, "2026-27", level, subject, listOf(unit), practicalScheme,
            AssessmentScheme(CurriculumSourceReference(source.id, "question paper design"), setOf("MCQ", "short answer", "long answer", "case-based", "competency-based", "data/diagram interpretation")), listOf(source), verification)
    }

    private val math7 = listOf("Large Numbers Around Us", "Arithmetic Expressions", "A Peek Beyond the Point", "Expressions using Letter-Numbers", "Parallel and Intersecting Lines", "Number Play", "A Tale of Three Intersecting Lines", "Working with Fractions")
    private val math8 = listOf("A Square and A Cube", "Power Play", "A Story of Numbers", "Quadrilaterals", "Number Play", "We Distribute, Yet Things Multiply", "Proportional Reasoning-1")
    private val math9 = listOf("Number Systems", "Introduction to Polynomials", "Sequences and Progressions", "Exploring Algebraic Identities", "Linear Equations in Two Variables", "Coordinate Geometry", "Introduction to Euclid's Geometry", "Lines and Angles", "Triangles", "Quadrilaterals", "Circles", "Area and Perimeter", "Surface Areas and Volumes", "Statistics", "Introduction to Probability")
    private val math10 = listOf("Real Numbers", "Polynomials", "Pair of Linear Equations in Two Variables", "Quadratic Equations", "Arithmetic Progressions", "Triangles", "Coordinate Geometry", "Introduction to Trigonometry", "Applications of Trigonometry", "Circles", "Areas Related to Circles", "Surface Areas and Volumes", "Statistics", "Probability")
    private val math11 = listOf("Sets", "Relations and Functions", "Trigonometric Functions", "Complex Numbers and Quadratic Equations", "Linear Inequalities", "Permutations and Combinations", "Binomial Theorem", "Sequences and Series", "Straight Lines", "Conic Sections", "Introduction to Three-dimensional Geometry", "Limits and Derivatives", "Statistics", "Probability")
    private val math12 = listOf("Relations and Functions", "Inverse Trigonometric Functions", "Matrices", "Determinants", "Continuity and Differentiability", "Applications of Derivatives", "Integrals", "Applications of Integrals", "Differential Equations", "Vector Algebra", "Three-dimensional Geometry", "Linear Programming", "Probability")

    private val science7 = listOf("Nutrition in Plants", "Nutrition in Animals", "Heat", "Acids, Bases and Salts", "Physical and Chemical Changes", "Respiration in Organisms", "Transportation in Animals and Plants", "Reproduction in Plants", "Motion and Time", "Electric Current and its Effects", "Light", "Forests: Our Lifeline", "Wastewater Story")
    private val science8 = listOf("Crop Production and Management", "Microorganisms: Friend and Foe", "Coal and Petroleum", "Combustion and Flame", "Conservation of Plants and Animals", "Reproduction in Animals", "Reaching the Age of Adolescence", "Force and Pressure", "Friction", "Sound", "Chemical Effects of Electric Current", "Some Natural Phenomena", "Light")
    private val science9Physics = listOf("Describing Motion Around Us", "How Forces Affect Motion", "Work, Energy, and Simple Machines", "Sound Waves: Characteristics and Applications")
    private val science9Chemistry = listOf("Exploring Mixtures and their Separation", "Journey Inside the Atom", "Atomic Foundations of Matter")
    private val science9Biology = listOf("The Fundamental Unit of Life", "Tissues in Action", "Reproduction: How Life Continues", "Diversity and Adaptation in Living Organisms", "Earth as a System: Energy, Matter, and Life")
    private val science10Physics = listOf("Light – Reflection and Refraction", "The Human Eye and the Colourful World", "Electricity", "Magnetic Effects of Electric Current")
    private val science10Chemistry = listOf("Chemical Reactions and Equations", "Acids, Bases and Salts", "Metals and Non-metals", "Carbon and its Compounds", "Periodic Classification of Elements")
    private val science10Biology = listOf("Life Processes", "Control and Coordination", "How do Organisms Reproduce?", "Heredity", "Our Environment")

    private val physics11 = listOf("Units and Measurements", "Motion in a Straight Line", "Motion in a Plane", "Laws of Motion", "Work, Energy and Power", "System of Particles and Rotational Motion", "Gravitation", "Mechanical Properties of Solids", "Mechanical Properties of Fluids", "Thermal Properties of Matter", "Thermodynamics", "Kinetic Theory", "Oscillations", "Waves")
    private val physics12 = listOf("Electric Charges and Fields", "Electrostatic Potential and Capacitance", "Current Electricity", "Moving Charges and Magnetism", "Magnetism and Matter", "Electromagnetic Induction", "Alternating Current", "Electromagnetic Waves", "Ray Optics and Optical Instruments", "Wave Optics", "Dual Nature of Radiation and Matter", "Atoms", "Nuclei", "Semiconductor Electronics")
    private val chemistry11 = listOf("Some Basic Concepts of Chemistry", "Structure of Atom", "Classification of Elements and Periodicity in Properties", "Chemical Bonding and Molecular Structure", "Chemical Thermodynamics", "Equilibrium", "Redox Reactions", "Organic Chemistry: Some Basic Principles and Techniques", "Hydrocarbons")
    private val chemistry12 = listOf("Solutions", "Electrochemistry", "Chemical Kinetics", "The d- and f-Block Elements", "Coordination Compounds", "Haloalkanes and Haloarenes", "Alcohols, Phenols and Ethers", "Aldehydes, Ketones and Carboxylic Acids", "Amines", "Biomolecules")
    private val biology11 = listOf("The Living World", "Biological Classification", "Plant Kingdom", "Animal Kingdom", "Morphology of Flowering Plants", "Anatomy of Flowering Plants", "Structural Organisation in Animals", "Cell: The Unit of Life", "Biomolecules", "Cell Cycle and Cell Division", "Photosynthesis in Higher Plants", "Respiration in Plants", "Plant Growth and Development", "Breathing and Exchange of Gases", "Body Fluids and Circulation", "Excretory Products and their Elimination", "Locomotion and Movement", "Neural Control and Coordination", "Chemical Coordination and Integration")
    private val biology12 = listOf("Sexual Reproduction in Flowering Plants", "Human Reproduction", "Reproductive Health", "Principles of Inheritance and Variation", "Molecular Basis of Inheritance", "Evolution", "Human Health and Disease", "Microbes in Human Welfare", "Biotechnology: Principles and Processes", "Biotechnology and its Applications", "Organisms and Populations", "Ecosystem", "Biodiversity and Conservation")

    private fun integrated(level: SchoolClassLevel, subject: SchoolSubject, source: CurriculumSource, all: List<String>, selected: (String) -> Boolean, discipline: ScienceDiscipline, verification: CurriculumVerificationStatus = source.verificationStatus, excluded: Map<String, AssessmentStatus> = emptyMap()) =
        manifest(level, subject, source, all.filter(selected), verification, excluded, "ncert-c${level.number}-science", discipline)

    val manifests: List<OfficialCurriculum> by lazy {
        val s7p = setOf("Heat", "Motion and Time", "Electric Current and its Effects", "Light")
        val s7c = setOf("Acids, Bases and Salts", "Physical and Chemical Changes", "Wastewater Story")
        val s7b = science7.toSet() - s7p - s7c
        val s8p = setOf("Force and Pressure", "Friction", "Sound", "Chemical Effects of Electric Current", "Some Natural Phenomena", "Light")
        val s8c = setOf("Coal and Petroleum", "Combustion and Flame", "Chemical Effects of Electric Current")
        val s8b = science8.toSet() - s8p - s8c
        listOf(
            manifest(SchoolClassLevel.CLASS_7, SchoolSubject.MATHEMATICS, grade7Math, math7),
            integrated(SchoolClassLevel.CLASS_7, SchoolSubject.PHYSICS, grade7Science, science7, { it in s7p }, ScienceDiscipline.PHYSICS),
            integrated(SchoolClassLevel.CLASS_7, SchoolSubject.CHEMISTRY, grade7Science, science7, { it in s7c }, ScienceDiscipline.CHEMISTRY),
            integrated(SchoolClassLevel.CLASS_7, SchoolSubject.BIOLOGY, grade7Science, science7, { it in s7b }, ScienceDiscipline.BIOLOGY),
            manifest(SchoolClassLevel.CLASS_8, SchoolSubject.MATHEMATICS, grade8Math, math8),
            integrated(SchoolClassLevel.CLASS_8, SchoolSubject.PHYSICS, grade8Bridge, science8, { it in s8p }, ScienceDiscipline.PHYSICS, CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW),
            integrated(SchoolClassLevel.CLASS_8, SchoolSubject.CHEMISTRY, grade8Bridge, science8, { it in s8c }, ScienceDiscipline.CHEMISTRY, CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW),
            integrated(SchoolClassLevel.CLASS_8, SchoolSubject.BIOLOGY, grade8Bridge, science8, { it in s8b }, ScienceDiscipline.BIOLOGY, CurriculumVerificationStatus.REQUIRES_MANUAL_REVIEW),
            manifest(SchoolClassLevel.CLASS_9, SchoolSubject.MATHEMATICS, mathSecondary, math9),
            manifest(SchoolClassLevel.CLASS_9, SchoolSubject.PHYSICS, scienceSecondary, science9Physics, practical = true),
            manifest(SchoolClassLevel.CLASS_9, SchoolSubject.CHEMISTRY, scienceSecondary, science9Chemistry, practical = true),
            manifest(SchoolClassLevel.CLASS_9, SchoolSubject.BIOLOGY, scienceSecondary, science9Biology, practical = true),
            manifest(SchoolClassLevel.CLASS_10, SchoolSubject.MATHEMATICS, mathSecondary10, math10),
            manifest(SchoolClassLevel.CLASS_10, SchoolSubject.PHYSICS, scienceSecondary10, science10Physics, practical = true),
            manifest(SchoolClassLevel.CLASS_10, SchoolSubject.CHEMISTRY, scienceSecondary10, science10Chemistry, excluded = mapOf("Periodic Classification of Elements" to AssessmentStatus.FORMATIVE_ONLY), practical = true),
            manifest(SchoolClassLevel.CLASS_10, SchoolSubject.BIOLOGY, scienceSecondary10, science10Biology, practical = true),
            manifest(SchoolClassLevel.CLASS_11, SchoolSubject.MATHEMATICS, mathSenior, math11),
            manifest(SchoolClassLevel.CLASS_11, SchoolSubject.PHYSICS, physicsSenior, physics11, practical = true),
            manifest(SchoolClassLevel.CLASS_11, SchoolSubject.CHEMISTRY, chemistrySenior, chemistry11, practical = true),
            manifest(SchoolClassLevel.CLASS_11, SchoolSubject.BIOLOGY, biologySenior, biology11, practical = true),
            manifest(SchoolClassLevel.CLASS_12, SchoolSubject.MATHEMATICS, mathSenior, math12),
            manifest(SchoolClassLevel.CLASS_12, SchoolSubject.PHYSICS, physicsSenior, physics12, practical = true),
            manifest(SchoolClassLevel.CLASS_12, SchoolSubject.CHEMISTRY, chemistrySenior, chemistry12, practical = true),
            manifest(SchoolClassLevel.CLASS_12, SchoolSubject.BIOLOGY, biologySenior, biology12, practical = true)
        )
    }
}
