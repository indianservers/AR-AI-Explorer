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
    Class6("Class 6"), Class7("Class 7"), Class8("Class 8"), Class9("Class 9"), Class10("Class 10"),
    Class11("Class 11"), Class12("Class 12"), UG("UG"), PG("PG"),
}

internal data class MathConceptDefinition(
    val title: String,
    val icon: String,
    val summary: String,
    val subtopics: List<String>,
    val levels: Set<MathClassBand>,
)

internal object MathConceptCatalog {
    private val school = MathClassBand.entries.take(7).toSet()
    private val senior = MathClassBand.entries.drop(5).toSet()
    private val university = setOf(MathClassBand.UG, MathClassBand.PG)

    val concepts = listOf(
        concept("Arithmetic", "+-", "Numbers and operations", "Whole numbers", "Fractions & decimals", "Ratio & proportion", "Percentages", levels = school),
        concept("Pre-Algebra", "x?", "Patterns before symbolic algebra", "Number patterns", "Variables", "Simple equations", "Coordinates", levels = MathClassBand.entries.take(5).toSet()),
        concept("Algebra", "x", "Expressions, equations and functions", "Expressions", "Linear equations", "Quadratics", "Polynomials", levels = MathClassBand.entries.drop(2).toSet()),
        concept("Linear Algebra", "Ax", "Vectors, matrices and transformations", "Matrices", "Determinants", "Vector spaces", "Eigenvalues", levels = senior),
        concept("Geometry", "tri", "Shape, measurement and reasoning", "Lines & angles", "Triangles", "Circles", "Mensuration", levels = school + university),
        concept("Coordinate Geometry", "xy", "Geometry placed on coordinate axes", "Cartesian plane", "Distance & midpoint", "Straight lines", "Circles & conics", levels = MathClassBand.entries.drop(2).toSet()),
        concept("Trigonometry", "sin", "Angles, triangles and periodic functions", "Right triangles", "Unit circle", "Identities", "Graphs", levels = MathClassBand.entries.drop(3).toSet()),
        concept("Precalculus", "f", "Functions leading into calculus", "Functions", "Transformations", "Sequences", "Limits preview", levels = senior),
        concept("Calculus", "int", "Change, accumulation and limits", "Limits", "Derivatives", "Integrals", "Series", levels = senior),
        concept("Multivariable Calculus", "dz", "Calculus with several variables", "Partial derivatives", "Multiple integrals", "Vector fields", "Optimization", levels = university),
        concept("Differential Equations", "dy", "Equations involving rates of change", "First-order ODEs", "Second-order ODEs", "Systems", "Numerical solutions", levels = university),
        concept("Discrete Mathematics", "nodes", "Finite structures and algorithms", "Relations", "Recurrences", "Trees", "Discrete probability", levels = senior),
        concept("Number Theory", "123", "Patterns and properties of integers", "Divisibility", "Primes", "Congruences", "Diophantine equations", levels = MathClassBand.entries.toSet()),
        concept("Set Theory", "sets", "Collections, relations and operations", "Set notation", "Venn operations", "Relations", "Cardinality", levels = MathClassBand.entries.toSet()),
        concept("Logic", "logic", "Valid reasoning and symbolic statements", "Propositions", "Truth tables", "Implication", "Proof methods", levels = MathClassBand.entries.drop(1).toSet()),
        concept("Combinatorics", "nCr", "Systematic counting of possibilities", "Counting principles", "Permutations", "Combinations", "Generating functions", levels = MathClassBand.entries.drop(4).toSet()),
        concept("Probability", "dice", "Quantifying uncertainty", "Sample spaces", "Probability rules", "Conditional probability", "Random variables", levels = MathClassBand.entries.drop(1).toSet()),
        concept("Statistics", "bar", "Learning from data", "Descriptive statistics", "Charts", "Sampling", "Statistical inference", levels = MathClassBand.entries.drop(1).toSet()),
        concept("Numerical Methods", "grid", "Approximate mathematical computation", "Root finding", "Interpolation", "Numerical integration", "Error analysis", levels = university),
        concept("Optimization", "target", "Finding best feasible solutions", "Linear programming", "Gradient methods", "Constraints", "Convexity", levels = senior),
        concept("Graph Theory", "graph", "Networks of vertices and edges", "Graphs & degree", "Paths & cycles", "Trees", "Network algorithms", levels = senior),
        concept("Topology", "torus", "Properties preserved by continuous change", "Open sets", "Continuity", "Compactness", "Surfaces", levels = university),
        concept("Complex Analysis", "i", "Functions of complex variables", "Complex plane", "Analytic functions", "Contour integration", "Residues", levels = university),
        concept("Real Analysis", "R", "Rigorous foundations of calculus", "Sequences", "Continuity", "Differentiation", "Measure", levels = university),
        concept("Abstract Algebra", "G", "Groups, rings and algebraic structure", "Groups", "Rings", "Fields", "Homomorphisms", levels = university),
        concept("Financial Mathematics", "fin", "Mathematics of money and risk", "Simple interest", "Compound interest", "Annuities", "Risk models", levels = MathClassBand.entries.drop(3).toSet()),
        concept("Mathematical Modelling", "model", "Representing real systems mathematically", "Model design", "Parameter fitting", "Simulation", "Validation", levels = MathClassBand.entries.drop(4).toSet()),
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
                Text(if (selected == null) "CLASS 6 TO POSTGRADUATE" else "DIRECT CONCEPT LESSONS", color = ConceptPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ConceptIcon(concept.icon, accent); Text("OPEN >", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
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
                ConceptLesson(selected, selectedSubtopic!!, vm)
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
        ConceptIcon(concept.icon, ConceptPurple, 58)
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
