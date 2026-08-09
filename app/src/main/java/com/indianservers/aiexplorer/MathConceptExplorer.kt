package com.indianservers.aiexplorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.presentation.NumberTheoryVisualProofScreen
import com.indianservers.aiexplorer.workspace.MathModule
import kotlin.math.cos
import kotlin.math.sin

private val ConceptSpace = Color(0xFF020612)
private val ConceptPanel = Color(0xEE091426)
private val ConceptInk = Color(0xFFF5F8FF)
private val ConceptMuted = Color(0xFFAAB7D2)
private val ConceptCyan = Color(0xFF29DFFF)
private val ConceptPurple = Color(0xFFA96AFF)
private val ConceptGreen = Color(0xFF4DE5A3)
private val ConceptAmber = Color(0xFFFFB63E)
private val ConceptPink = Color(0xFFFF68A6)

internal enum class MathClassBand(val label: String) {
    Class1("Class 1"), Class2("Class 2"), Class3("Class 3"), Class4("Class 4"), Class5("Class 5"),
    Class6("Class 6"), Class7("Class 7"), Class8("Class 8"), Class9("Class 9"), Class10("Class 10"),
    Class11("Class 11"), Class12("Class 12"), UG("UG"), PG("PG"), PhD("PhD"),
}

internal data class MathConceptDefinition(
    val title: String,
    val icon: String,
    val summary: String,
    val subtopics: List<String>,
    val levels: Set<MathClassBand>,
)

internal object MathConceptCatalog {
    val concepts = listOf(
        concept("Abstract and Advanced Algebra", "AAA", "Abstract and Advanced Algebra lessons covering Rings and Fields, Groups, Advanced Algebraic Geometry.", "Rings and Fields", "Groups", "Advanced Algebraic Geometry", "Classical Algebraic Geometry", "Field and Galois Theory", "Category Theory", levels = setOf(MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Algebra", "A", "Algebra lessons covering Complex Numbers, Expressions Identities Factorisation, Sequences and Series.", "Complex Numbers", "Expressions Identities Factorisation", "Sequences and Series", "Introduction to Algebra", "Polynomials", "Determinants", levels = setOf(MathClassBand.Class4, MathClassBand.Class5, MathClassBand.Class6, MathClassBand.Class7, MathClassBand.Class8, MathClassBand.Class9, MathClassBand.Class10, MathClassBand.Class11, MathClassBand.Class12)),
        concept("Applied and Interdisciplinary Mathematics", "AAI", "Applied and Interdisciplinary Mathematics lessons covering Research Design and Communication, Mathematical Research, Models.", "Research Design and Communication", "Mathematical Research", "Models", "Strategies", "Information", "Machine Learning Mathematics", levels = setOf(MathClassBand.Class12, MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Arithmetic", "A", "Arithmetic lessons covering Four Operations, Factors and Operations, Multiplication.", "Four Operations", "Factors and Operations", "Multiplication", "Addition", "Division", "Subtraction", levels = setOf(MathClassBand.Class2, MathClassBand.Class3, MathClassBand.Class4, MathClassBand.Class5)),
        concept("Calculus", "C", "Calculus lessons covering Continuity and Differentiability, Limits and Derivatives, Applications of Derivatives.", "Continuity and Differentiability", "Limits and Derivatives", "Applications of Derivatives", "Differentiation", "Integrals", "Limits and Continuity", levels = setOf(MathClassBand.Class11, MathClassBand.Class12, MathClassBand.UG)),
        concept("Complex Analysis", "CA", "Complex Analysis lessons covering Complex Integration, Advanced Complex Analysis, Complex Functions.", "Complex Integration", "Advanced Complex Analysis", "Complex Functions", levels = setOf(MathClassBand.UG, MathClassBand.PG)),
        concept("Coordinate Geometry", "CG", "Coordinate Geometry lessons covering Vectors, Conic Sections, Three-Dimensional Geometry.", "Vectors", "Conic Sections", "Three-Dimensional Geometry", "Graphs", "Straight Lines", "Cartesian System", levels = setOf(MathClassBand.Class8, MathClassBand.Class9, MathClassBand.Class10, MathClassBand.Class11, MathClassBand.Class12)),
        concept("Data Handling and Patterns", "DHA", "Data Handling and Patterns lessons covering Statistics and Probability, Data Handling, Patterns.", "Statistics and Probability", "Data Handling", "Patterns", "Statistics", "Measurement", levels = setOf(MathClassBand.Class1, MathClassBand.Class2, MathClassBand.Class3, MathClassBand.Class5, MathClassBand.Class6, MathClassBand.Class7, MathClassBand.Class8)),
        concept("Differential Equations and PDE", "DEA", "Differential Equations and PDE lessons covering Ordinary Differential Equations, Advanced PDE, PDE Foundations.", "Ordinary Differential Equations", "Advanced PDE", "PDE Foundations", "Nonlinear PDE", "Computational Mathematics", "Finite Element Analysis", levels = setOf(MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Differential Geometry", "DG", "Differential Geometry lessons covering Curves and Surfaces, Smooth Manifolds, Riemannian Geometry.", "Curves and Surfaces", "Smooth Manifolds", "Riemannian Geometry", levels = setOf(MathClassBand.UG, MathClassBand.PG)),
        concept("Discrete Mathematics", "DM", "Discrete Mathematics lessons covering Advanced Combinatorics, Graph Theory, Advanced Graph Theory.", "Advanced Combinatorics", "Graph Theory", "Advanced Graph Theory", "Counting Methods", "Theoretical Computer Science", "Combinatorics", levels = setOf(MathClassBand.Class12, MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Dynamical Systems and Control", "DSA", "Dynamical Systems and Control lessons covering Nonlinear Dynamics, Systems and Control, Advanced Control.", "Nonlinear Dynamics", "Systems and Control", "Advanced Control", "Advanced Dynamical Systems", levels = setOf(MathClassBand.PG, MathClassBand.PhD)),
        concept("Financial and Commercial Mathematics", "FAC", "Financial and Commercial Mathematics lessons covering Comparing Quantities, Quantitative Finance, Percent Applications.", "Comparing Quantities", "Quantitative Finance", "Percent Applications", levels = setOf(MathClassBand.Class7, MathClassBand.Class8, MathClassBand.PG)),
        concept("Fractions Decimals Ratio and Percent", "FDR", "Fractions Decimals Ratio and Percent lessons covering Fractions, Decimals, Basic Fractions.", "Fractions", "Decimals", "Basic Fractions", "Ratio and Proportion", "Percent", levels = setOf(MathClassBand.Class2, MathClassBand.Class3, MathClassBand.Class4, MathClassBand.Class5, MathClassBand.Class6)),
        concept("Geometry", "G", "Geometry lessons covering Basic Geometry, Angles and Shapes, Olympiad Geometry.", "Basic Geometry", "Angles and Shapes", "Olympiad Geometry", "Lines and Angles", "Triangles", "Quadrilaterals", levels = setOf(MathClassBand.Class1, MathClassBand.Class2, MathClassBand.Class3, MathClassBand.Class4, MathClassBand.Class5, MathClassBand.Class6, MathClassBand.Class7, MathClassBand.Class8, MathClassBand.Class9, MathClassBand.Class10, MathClassBand.Class12)),
        concept("Linear Algebra", "LA", "Linear Algebra lessons covering Advanced Linear Algebra, Vector Spaces, Systems and Matrices.", "Advanced Linear Algebra", "Vector Spaces", "Systems and Matrices", levels = setOf(MathClassBand.UG)),
        concept("Logic and Foundations", "LAF", "Logic and Foundations lessons covering Set Theory, Logic, Logic and Proof.", "Set Theory", "Logic", "Logic and Proof", "Advanced Set Theory", "Computability", "Model Theory", levels = setOf(MathClassBand.Class11, MathClassBand.UG, MathClassBand.PhD)),
        concept("Measurement", "M", "Measurement lessons covering Length Mass Capacity, Money, Metric Awareness.", "Length Mass Capacity", "Money", "Metric Awareness", "Time and Money", "Mass and Capacity", "Metric Measures", levels = setOf(MathClassBand.Class1, MathClassBand.Class2, MathClassBand.Class3, MathClassBand.Class4)),
        concept("Mensuration", "M", "Mensuration lessons covering Perimeter and Area, Circles and Solids, Heron and Solids.", "Perimeter and Area", "Circles and Solids", "Heron and Solids", "Area Surface Area Volume", "Area and Volume", levels = setOf(MathClassBand.Class4, MathClassBand.Class5, MathClassBand.Class6, MathClassBand.Class7, MathClassBand.Class8, MathClassBand.Class9, MathClassBand.Class10)),
        concept("Multivariable and Vector Calculus", "MAV", "Multivariable and Vector Calculus lessons covering Several Variables, Vector Fields, Multiple Integrals.", "Several Variables", "Vector Fields", "Multiple Integrals", levels = setOf(MathClassBand.UG)),
        concept("Number Theory", "NT", "Number Theory lessons covering Elementary and Algebraic Number Theory, Divisibility and Congruences, Mathematical Cryptography.", "Elementary and Algebraic Number Theory", "Divisibility and Congruences", "Mathematical Cryptography", "Number Fields", "Advanced Algebraic Number Theory", "Advanced Analytic Number Theory", levels = setOf(MathClassBand.Class12, MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Numbers and Number Systems", "NAN", "Numbers and Number Systems lessons covering Large Numbers, Whole Numbers and Integers, Counting and Number Sense.", "Large Numbers", "Whole Numbers and Integers", "Counting and Number Sense", "Real Numbers", "Numbers to 1000", "Addition", levels = setOf(MathClassBand.Class1, MathClassBand.Class2, MathClassBand.Class3, MathClassBand.Class4, MathClassBand.Class5, MathClassBand.Class6, MathClassBand.Class7, MathClassBand.Class8, MathClassBand.Class9, MathClassBand.Class10)),
        concept("Numerical Analysis", "NA", "Numerical Analysis lessons covering Numerical Computation, Advanced Numerical Methods.", "Numerical Computation", "Advanced Numerical Methods", levels = setOf(MathClassBand.UG, MathClassBand.PG)),
        concept("Optimization and Operations Research", "OAO", "Optimization and Operations Research lessons covering Optimization, Nonlinear Optimization, Advanced Convex Optimization.", "Optimization", "Nonlinear Optimization", "Advanced Convex Optimization", "Discrete Optimization", "Stochastic Optimization", levels = setOf(MathClassBand.Class12, MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Probability and Statistics", "PAS", "Probability and Statistics lessons covering Statistics, Advanced Probability, Probability.", "Statistics", "Advanced Probability", "Probability", "Mathematical Statistics", "Probability Foundations", "Probability Distributions", levels = setOf(MathClassBand.Class4, MathClassBand.Class9, MathClassBand.Class10, MathClassBand.Class11, MathClassBand.Class12, MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Real and Functional Analysis", "RAF", "Real and Functional Analysis lessons covering Measure and Integration, Measure Theory, Metric Spaces.", "Measure and Integration", "Measure Theory", "Metric Spaces", "Harmonic Analysis", "Continuity Differentiation Integration", "Functional and Operator Analysis", levels = setOf(MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Sets Relations and Functions", "SRA", "Sets Relations and Functions lessons covering Sets, Relations and Functions, Functions.", "Sets", "Relations and Functions", "Functions", "Inverse Trigonometric Functions", "Relations", levels = setOf(MathClassBand.Class11, MathClassBand.Class12)),
        concept("Topology and Geometry", "TAG", "Topology and Geometry lessons covering General Topology, Advanced Algebraic Topology, Algebraic Topology I.", "General Topology", "Advanced Algebraic Topology", "Algebraic Topology I", "General Topology II", "Advanced Riemannian Geometry", "Differential Topology", levels = setOf(MathClassBand.UG, MathClassBand.PG, MathClassBand.PhD)),
        concept("Trigonometry", "T", "Trigonometry lessons covering Trigonometric Ratios, Applications.", "Trigonometric Ratios", "Applications", levels = setOf(MathClassBand.Class10))
    )

    fun search(query: String, band: MathClassBand?): List<MathConceptDefinition> {
        val normalized = query.trim().lowercase()
        return concepts.filter { definition ->
            (band == null || band in definition.levels) &&
                (normalized.isBlank() || normalized in definition.title.lowercase() ||
                    normalized in definition.summary.lowercase() ||
                    definition.subtopics.any { normalized in it.lowercase() })
        }
    }

    fun find(title: String?): MathConceptDefinition? = concepts.firstOrNull { it.title == title }

    private fun concept(
        title: String,
        icon: String,
        summary: String,
        vararg subtopics: String,
        levels: Set<MathClassBand>,
    ) = MathConceptDefinition(title, icon, summary, subtopics.toList(), levels)
}

@Composable
internal fun MathConceptExplorerScreen(vm: ExplorerViewModel, wide: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    var bandName by rememberSaveable { mutableStateOf<String?>(null) }
    var showNumberTheoryProofs by rememberSaveable { mutableStateOf(false) }
    val selected = MathConceptCatalog.find(vm.selectedMathConcept)
    val selectedSubtopic = vm.selectedMathSubConcept
    val band = bandName?.let { value -> MathClassBand.entries.firstOrNull { it.name == value } }

    if (showNumberTheoryProofs) {
        NumberTheoryVisualProofScreen(onExit = { showNumberTheoryProofs = false })
        return
    }

    BackHandler {
        when {
            selectedSubtopic != null -> vm.selectMathSubConcept(null)
            selected != null -> vm.selectMathConcept(null)
            else -> vm.returnToMathMenu()
        }
    }

    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF18214B), ConceptSpace, Color.Black), center = Offset(260f, 50f), radius = 1200f))
            .verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 30.dp else 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ConceptButton("< BACK", ConceptCyan) {
                when {
                    selectedSubtopic != null -> vm.selectMathSubConcept(null)
                    selected != null -> vm.selectMathConcept(null)
                    else -> vm.returnToMathMenu()
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(selected?.title ?: "Math Concepts", color = ConceptInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(if (selected == null) "CLASS 1 TO PHD" else "DIRECT CONCEPT LESSONS", color = ConceptPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            ConceptButton("HOME", ConceptPurple, vm::returnToMathMenu)
        }

        if (selected == null) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                placeholder = { Text("Search concepts and sub-concepts") },
                leadingIcon = { Text("Q", color = ConceptCyan, fontWeight = FontWeight.Black) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search all mathematics concepts" },
            )
            Text("EXPLORE CLASS-WISE", color = ConceptGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ConceptChip("All", band == null, ConceptGreen) { bandName = null }
                MathClassBand.entries.forEach { item -> ConceptChip(item.label, band == item, ConceptGreen) { bandName = item.name } }
            }
            val results = remember(query, band) { MathConceptCatalog.search(query, band) }
            Text(
                "${results.size} CONCEPTS · ${results.sumOf { it.subtopics.size }} SUB-CONCEPTS",
                color = ConceptCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 760.dp) 3 else 2
                val gap = 8.dp
                val cardWidth = (maxWidth - gap * (columns - 1)) / columns
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                    results.forEachIndexed { index, concept ->
                        val accent = listOf(ConceptCyan, ConceptPurple, ConceptGreen, ConceptAmber, ConceptPink)[index % 5]
                        Column(
                            Modifier.width(cardWidth).heightIn(min = 142.dp).background(Brush.linearGradient(listOf(accent.copy(.18f), ConceptPanel)), RoundedCornerShape(18.dp))
                                .border(1.dp, accent.copy(.55f), RoundedCornerShape(18.dp)).clickable {
                                    vm.selectMathConcept(concept.title)
                                }
                                .semantics { contentDescription = "Open ${concept.title} sub concepts" }.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                MathConceptIconImage(concept.title, Modifier.width(44.dp).height(44.dp), 12.dp)
                                Text("OPEN >", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                            Text(concept.title, color = ConceptInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(concept.summary, color = ConceptMuted, fontSize = 9.sp, maxLines = 2)
                            Text(
                                concept.subtopics.joinToString(" · "),
                                color = accent.copy(alpha = .92f),
                                fontSize = 8.sp,
                                maxLines = 3,
                            )
                        }
                    }
                }
            }
        } else {
            ConceptHero(selected)
            if (selectedSubtopic == null) {
                if (selected.title == "Number Theory") {
                    Row(
                        Modifier.fillMaxWidth().background(ConceptGreen.copy(.12f), RoundedCornerShape(15.dp))
                            .border(1.dp, ConceptGreen.copy(.65f), RoundedCornerShape(15.dp))
                            .clickable { showNumberTheoryProofs = true }
                            .semantics { contentDescription = "Open Number Theory visual proofs" }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ConceptIcon("QED", ConceptGreen)
                        Column(Modifier.weight(1f)) {
                            Text("Visual Proofs", color = ConceptInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Build and test the reason behind number patterns.", color = ConceptMuted, fontSize = 10.sp)
                        }
                        Text("EXPLORE >", color = ConceptGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text("CHOOSE A SUB-CONCEPT", color = ConceptCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                selected.subtopics.forEachIndexed { index, subtopic ->
                    val accent = listOf(ConceptCyan, ConceptPurple, ConceptGreen, ConceptAmber)[index % 4]
                    Row(
                        Modifier.fillMaxWidth().background(accent.copy(.09f), RoundedCornerShape(15.dp)).border(1.dp, accent.copy(.42f), RoundedCornerShape(15.dp))
                            .clickable { vm.selectMathSubConcept(subtopic) }.semantics { contentDescription = "Open $subtopic lesson" }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ConceptIcon("${index + 1}", accent)
                        Column(Modifier.weight(1f)) {
                            Text(subtopic, color = ConceptInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                conceptExplanation(selected.title, subtopic),
                                color = ConceptMuted,
                                fontSize = 10.sp,
                                maxLines = 3,
                            )
                        }
                        Text("LEARN >", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                ConceptLesson(selected, selectedSubtopic, vm)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConceptHero(concept: MathConceptDefinition) {
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(ConceptPurple.copy(.22f), ConceptCyan.copy(.1f), ConceptPanel)), RoundedCornerShape(20.dp))
            .border(1.dp, ConceptPurple.copy(.6f), RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        MathConceptIconImage(concept.title, Modifier.width(58.dp).height(58.dp), 16.dp)
        Column(Modifier.weight(1f)) {
            Text(concept.title, color = ConceptInk, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(concept.summary, color = ConceptMuted, fontSize = 11.sp)
            Text(concept.levels.joinToString(" - ") { it.label }, color = ConceptGreen, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ConceptLesson(concept: MathConceptDefinition, subtopic: String, vm: ExplorerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("${concept.title} > $subtopic", color = ConceptPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(subtopic, color = ConceptInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
        ConceptVisual(concept.title, subtopic, Modifier.fillMaxWidth().height(230.dp))
        Column(Modifier.fillMaxWidth().background(ConceptPanel, RoundedCornerShape(16.dp)).border(1.dp, ConceptCyan.copy(.4f), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CORE IDEA", color = ConceptCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(conceptExplanation(concept.title, subtopic), color = ConceptInk, fontSize = 13.sp)
            Text("Try changing the objects in the visual workspace and observe which relationships remain unchanged.", color = ConceptMuted, fontSize = 10.sp)
        }
        Text("CONTINUE WITH", color = ConceptGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ConceptButton("OPEN VISUAL LAB", ConceptCyan) { openConceptWorkspace(vm, concept.title) }
            ConceptButton("FORMULAS", ConceptPurple) { vm.openKnowledgeHub(KnowledgeSection.Formulas) }
            ConceptButton("VISUAL PROOFS", ConceptGreen) { vm.openKnowledgeHub(KnowledgeSection.Proofs) }
            ConceptButton("THEOREMS", ConceptAmber) { vm.openKnowledgeHub(KnowledgeSection.Theorems) }
        }
    }
}

@Composable
private fun ConceptVisual(concept: String, subtopic: String, modifier: Modifier) {
    Canvas(modifier.background(Color(0xFF030B1D), RoundedCornerShape(18.dp)).border(1.dp, ConceptPurple.copy(.55f), RoundedCornerShape(18.dp)).semantics {
        contentDescription = "Interactive-style visual explanation for $subtopic"
    }) {
        for (i in 1..7) {
            drawLine(ConceptMuted.copy(.1f), Offset(size.width * i / 8, 0f), Offset(size.width * i / 8, size.height))
            drawLine(ConceptMuted.copy(.1f), Offset(0f, size.height * i / 8), Offset(size.width, size.height * i / 8))
        }
        when {
            concept.contains("Geometry") || "Triangle" in subtopic || "Circle" in subtopic -> {
                val a = Offset(size.width * .2f, size.height * .78f)
                val b = Offset(size.width * .78f, size.height * .78f)
                val c = Offset(size.width * .58f, size.height * .2f)
                drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, ConceptCyan, style = Stroke(5f))
                drawCircle(ConceptPink, 9f, a); drawCircle(ConceptGreen, 9f, b); drawCircle(ConceptAmber, 9f, c)
            }
            concept in setOf("Statistics", "Probability", "Combinatorics") -> {
                val heights = listOf(.3f, .58f, .82f, .65f, .38f)
                heights.forEachIndexed { index, value ->
                    val width = size.width / 7
                    val x = width * (index + 1)
                    drawRect(listOf(ConceptCyan, ConceptPurple, ConceptGreen, ConceptAmber, ConceptPink)[index], Offset(x, size.height * (1 - value)), androidx.compose.ui.geometry.Size(width * .7f, size.height * value - 20f))
                }
            }
            concept.contains("Algebra") || concept in setOf("Calculus", "Precalculus", "Differential Equations", "Real Analysis") -> {
                val path = Path()
                repeat(101) { index ->
                    val x = index / 100f
                    val y = .52f - .3f * sin(x * 6.28f)
                    if (index == 0) path.moveTo(x * size.width, y * size.height) else path.lineTo(x * size.width, y * size.height)
                }
                drawPath(path, ConceptCyan, style = Stroke(5f))
                drawLine(ConceptMuted, Offset(0f, size.height * .52f), Offset(size.width, size.height * .52f), 2f)
            }
            else -> {
                val center = Offset(size.width / 2, size.height / 2)
                repeat(8) { index ->
                    val angle = index * Math.PI / 4
                    val edge = Offset(center.x + cos(angle).toFloat() * size.width * .32f, center.y + sin(angle).toFloat() * size.height * .32f)
                    drawLine(ConceptCyan.copy(.65f), center, edge, 3f); drawCircle(if (index % 2 == 0) ConceptPurple else ConceptGreen, 9f, edge)
                }
                drawCircle(ConceptAmber, 13f, center)
            }
        }
    }
}

private fun openConceptWorkspace(vm: ExplorerViewModel, concept: String) {
    when (concept) {
        "Geometry", "Coordinate Geometry" -> vm.open(MathModule.Geometry2D)
        "Trigonometry" -> vm.open(MathModule.Trigonometry)
        "Multivariable Calculus", "Differential Geometry", "Topology" -> vm.open(MathModule.Graph3D)
        "Probability", "Statistics", "Combinatorics" -> vm.openProbabilityLab()
        "Set Theory", "Logic" -> vm.openSetLogicVisualizer()
        "Visual Formulas" -> vm.openKnowledgeHub(KnowledgeSection.Visualize)
        "Arithmetic", "Pre-Algebra", "Algebra", "Linear Algebra", "Number Theory", "Abstract Algebra" -> vm.open(MathModule.Manipulatives)
        else -> vm.open(MathModule.Graph2D)
    }
}

private fun conceptExplanation(concept: String, subtopic: String): String = when {
    concept == "Coordinate Geometry" && subtopic == "Cartesian plane" -> "A point is located by an ordered pair (x, y): move x units horizontally, then y units vertically."
    concept == "Coordinate Geometry" && subtopic == "Distance & midpoint" -> "Distance comes from the Pythagorean theorem. The midpoint is found by averaging the two x-coordinates and the two y-coordinates."
    concept == "Coordinate Geometry" && subtopic == "Straight lines" -> "A line's slope measures vertical change per horizontal change. Its equation connects every point with that constant rate."
    concept == "Coordinate Geometry" && subtopic == "Circles & conics" -> "Conics are loci: collections of points satisfying a fixed geometric distance relationship."
    else -> "$subtopic is a central part of $concept. Study its definition, connect each symbol to the visual, then test the relationship with examples and counterexamples."
}

@Composable
private fun ConceptIcon(text: String, accent: Color, size: Int = 40) {
    Box(Modifier.width(size.dp).height(size.dp).background(accent.copy(.14f), RoundedCornerShape(12.dp)).border(1.dp, accent.copy(.6f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(text, color = accent, fontSize = if (text.length > 4) 9.sp else 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ConceptButton(text: String, accent: Color, onClick: () -> Unit) {
    Text(text, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(accent.copy(.1f), RoundedCornerShape(11.dp)).border(1.dp, accent.copy(.65f), RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 9.dp))
}

@Composable
private fun ConceptChip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(text, color = if (selected) ConceptSpace else accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(if (selected) accent else accent.copy(.08f), RoundedCornerShape(10.dp)).border(1.dp, accent.copy(.5f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp))
}
