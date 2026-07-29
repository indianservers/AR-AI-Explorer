package com.indianservers.aiexplorer.chemistry.formulas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.displayLatexFormula
import com.indianservers.aiexplorer.rememberFormulaFontScalePreference
import com.indianservers.aiexplorer.chemistry.formulas.model.ChemistryFormula
import com.indianservers.aiexplorer.chemistry.formulas.model.ChemistryFormulaFilters
import com.indianservers.aiexplorer.chemistry.formulas.model.ChemistryFormulaLevel
import com.indianservers.aiexplorer.chemistry.formulas.navigation.ChemistryFormulaRoute
import com.indianservers.aiexplorer.chemistry.formulas.navigation.ChemistryFormulaViewModel

private val Bg = Color(0xFF050C12)
private val Panel = Color(0xE80C1B24)
private val Cyan = Color(0xFF55DDE0)
private val Green = Color(0xFF72E6A8)
private val Violet = Color(0xFFB49CFF)
private val Amber = Color(0xFFFFC86B)
private val Ink = Color(0xFFF1F8FA)
private val Muted = Color(0xFF9CB2B8)

@Composable
fun ChemistryFormulaHome(vm: ChemistryFormulaViewModel, onExit: () -> Unit) {
    val state = vm.state
    ChemPage { scale ->
        Header("CHEMISTRY FORMULAS", "Chemical relationships, calculations, equations and applications", onExit, "Chemistry")
        LevelRow(state.filters.level, vm::setLevel)
        SearchBox(state.query, vm::search)
        FilterRow(state.filters, vm)
        if (state.query.isNotBlank()) FormulaList(state.results, vm, scale)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Bookmarks", "${state.bookmarks.size} saved", Amber, Modifier.weight(1f)) { vm.navigate(ChemistryFormulaRoute.Bookmarks) }
            Action("Recently viewed", "${state.recentlyViewed.size} formulas", Green, Modifier.weight(1f)) {
                state.recentlyViewed.firstOrNull()?.let { vm.navigate(ChemistryFormulaRoute.Detail(it)) }
            }
            Action("Revision", "Flashcards planned", Violet, Modifier.weight(1f)) { vm.navigate(ChemistryFormulaRoute.Revision) }
        }
        PanelBox("Validated offline catalogue", scale) {
            val report = vm.repository.validate()
            Text("${vm.repository.getCategories().size} categories · ${if (report.valid) "hierarchy validated" else "validation issue"}", color = if (report.valid) Green else Amber, fontSize = scaledSp(12, scale))
        }
        vm.repository.getCategories().forEach { category ->
            val count = vm.repository.getSubcategories(category.id).sumOf { vm.repository.getFormulas(it.id).size }
            Node(category.title, category.description, "${category.subcategoryIds.size} subcategories · $count verified formulas", Cyan, scale) {
                vm.navigate(ChemistryFormulaRoute.Category(category.id))
            }
        }
    }
}

@Composable
fun ChemistryFormulaCategoryPage(vm: ChemistryFormulaViewModel, id: String) {
    val category = vm.repository.getCategory(id) ?: return ChemistryFormulaPlannedPage(vm, "Unknown Chemistry formula category.")
    ChemPage { scale ->
        PageHeader(vm, category.title, category.description)
        vm.repository.getSubcategories(id).forEach { sub ->
            val count = vm.repository.getFormulas(sub.id).size
            Node(sub.title, sub.description, if (count > 0) "$count formula entries" else "Qualitative topic · no formula forced", Violet, scale) {
                vm.navigate(ChemistryFormulaRoute.Subcategory(sub.id))
            }
        }
    }
}

@Composable
fun ChemistryFormulaSubcategoryPage(vm: ChemistryFormulaViewModel, id: String) {
    val sub = vm.repository.getSubcategory(id) ?: return ChemistryFormulaPlannedPage(vm, "Unknown Chemistry formula subcategory.")
    val formulas = vm.repository.getFormulas(id)
    ChemPage { scale ->
        PageHeader(vm, sub.title, sub.description)
        if (formulas.isEmpty()) {
            PanelBox("Scientifically honest coverage", scale) {
                Text("This hierarchy topic remains visible, but no quantitative expression is invented for a primarily qualitative chemical concept.", color = Muted, fontSize = scaledSp(12, scale))
            }
        } else {
            FormulaList(formulas, vm, scale)
        }
    }
}

@Composable
fun ChemistryFormulaDetailPage(vm: ChemistryFormulaViewModel, id: String) {
    val formula = vm.repository.getFormula(id) ?: return ChemistryFormulaPlannedPage(vm, "Unknown Chemistry formula.")
    ChemPage { scale ->
        PageHeader(vm, formula.title, "${formula.minimumLevel.label} · ${vm.repository.getSubcategory(formula.subcategoryId)?.title}")
        Equation(formula, scale)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton(if (id in vm.state.bookmarks) "Bookmarked ✓" else "Bookmark") { vm.toggleBookmark(id) }
            if (formula.calculator != null) SmallButton("Calculator") { vm.navigate(ChemistryFormulaRoute.Calculator(id)) }
        }
        PanelBox("Meaning", scale) {
            Text(formula.description, color = Ink, fontSize = scaledSp(13, scale))
            Text(formula.spokenEquation, color = Muted, fontSize = scaledSp(11, scale))
        }
        PanelBox("Symbols and accepted units", scale) {
            formula.variables.forEach { variable ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${variable.symbol} · ${variable.spokenName}", color = Cyan, fontWeight = FontWeight.Bold, fontSize = scaledSp(12, scale))
                    Text(variable.siUnit ?: "dimensionless", color = Green, fontSize = scaledSp(11, scale))
                }
                Text(variable.meaning, color = Muted, fontSize = scaledSp(10, scale))
            }
        }
        PanelBox("Assumptions and limitations", scale) {
            formula.assumptions.forEach { Text("Assumption · $it", color = Ink, fontSize = scaledSp(11, scale)) }
            formula.limitations.forEach { Text("Limit · $it", color = Amber, fontSize = scaledSp(11, scale)) }
        }
        PanelBox("Worked example", scale) {
            formula.workedExamples.forEach {
                Text(it.question, color = Ink, fontSize = scaledSp(12, scale))
                Text(it.substitution, color = Muted, fontSize = scaledSp(11, scale))
                Text(it.answer, color = Green, fontSize = scaledSp(12, scale))
                Text(it.unitCheck, color = Cyan, fontSize = scaledSp(10, scale))
            }
        }
        PanelBox("Unit and notation check", scale) { Text(formula.unitCheck, color = Green, fontSize = scaledSp(12, scale)) }
        if (formula.derivationSteps.isNotEmpty()) {
            PanelBox("Derivation foundation", scale) {
                formula.derivationSteps.forEachIndexed { i, step ->
                    Text("${i + 1}. ${displayLatexFormula(step.equation)}", color = Cyan, fontFamily = FontFamily.Serif, fontSize = scaledSp(14, scale))
                    Text(step.explanation, color = Muted, fontSize = scaledSp(10, scale))
                }
            }
        }
    }
}

@Composable
fun ChemistryFormulaSearchPage(vm: ChemistryFormulaViewModel) {
    ChemPage { scale ->
        PageHeader(vm, "Chemistry formula search", "Search equations without requiring exact symbols")
        SearchBox(vm.state.query, vm::search)
        FilterRow(vm.state.filters, vm)
        FormulaList(vm.state.results, vm, scale)
    }
}

@Composable
fun ChemistryFormulaBookmarksPage(vm: ChemistryFormulaViewModel) {
    ChemPage { scale ->
        PageHeader(vm, "Chemistry formula bookmarks", "Never mixed with Physics bookmarks")
        val items = vm.state.bookmarks.mapNotNull(vm.repository::getFormula)
        if (items.isEmpty()) Text("No Chemistry formula bookmarks in this session.", color = Muted, fontSize = scaledSp(12, scale)) else FormulaList(items, vm, scale)
    }
}

@Composable
fun ChemistryFormulaPlannedPage(vm: ChemistryFormulaViewModel, message: String) {
    ChemPage { scale ->
        PageHeader(vm, "Chemistry Formulas", "Route registered safely")
        PanelBox("Planned destination", scale) {
            Text(message, color = Amber, fontSize = scaledSp(12, scale))
            Text("No fake calculator or assessment controls are shown.", color = Muted, fontSize = scaledSp(12, scale))
        }
    }
}

@Composable
private fun FormulaList(items: List<ChemistryFormula>, vm: ChemistryFormulaViewModel, scale: Float) {
    if (items.isEmpty()) Text("No formulas match the current level and filters.", color = Amber, fontSize = scaledSp(12, scale))
    items.forEach { formula ->
        Node(
            formula.title,
            displayLatexFormula(formula.equation),
            "${formula.minimumLevel.label}${if (formula.calculator != null) " · calculator-ready" else ""}${if (formula.derivationSteps.isNotEmpty()) " · derivation" else ""}",
            Cyan,
            scale,
        ) { vm.navigate(ChemistryFormulaRoute.Detail(formula.id)) }
    }
}

@Composable
private fun Equation(formula: ChemistryFormula, scale: Float) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF10333B), Color(0xFF211B38))))
            .border(1.dp, Cyan, RoundedCornerShape(18.dp))
            .semantics { contentDescription = formula.spokenEquation }
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(displayLatexFormula(formula.equation), color = Ink, fontSize = scaledSp(26, scale), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        Text(formula.spokenEquation, color = Muted, fontSize = scaledSp(10, scale))
    }
}

@Composable
private fun SearchBox(value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text("Search Chemistry formulas") }, placeholder = { Text("pH, ideal gas, PV = nRT, molarity, Nernst") }, singleLine = true)
}

@Composable
private fun FilterRow(filters: ChemistryFormulaFilters, vm: ChemistryFormulaViewModel) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Toggle("Calculators", filters.calculatorOnly, vm::toggleCalculator)
        Toggle("Derivations", filters.derivationOnly, vm::toggleDerivation)
        Toggle("Bookmarked", filters.bookmarkedOnly, vm::toggleBookmarksFilter)
        SmallButton("Full search") { vm.navigate(ChemistryFormulaRoute.Search) }
    }
}

@Composable
private fun LevelRow(level: ChemistryFormulaLevel, onSelect: (ChemistryFormulaLevel) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ChemistryFormulaLevel.entries.forEach { item -> Toggle(item.label, item == level) { onSelect(item) } }
    }
}

@Composable
private fun FormulaFontControls(scale: Float, onScale: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        SmallButton("-") { onScale((scale - .1f).coerceAtLeast(.75f)) }
        Text("${(scale * 100).toInt()}%", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp))
        SmallButton("+") { onScale((scale + .1f).coerceAtMost(1.45f)) }
        SmallButton("Reset") { onScale(1f) }
    }
}

@Composable
private fun Toggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) Bg else Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected) Cyan else Panel).border(1.dp, Cyan.copy(.5f), RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 9.dp))
}

@Composable
private fun Header(title: String, subtitle: String, onBack: () -> Unit, backLabel: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Cyan, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        SmallButton(backLabel, onBack)
    }
}

@Composable
private fun PageHeader(vm: ChemistryFormulaViewModel, title: String, subtitle: String) {
    Header(title, subtitle, { vm.back() }, "Back")
    SmallButton("Formula home", vm::home)
}

@Composable
private fun Node(title: String, description: String, meta: String, accent: Color, scale: Float, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, accent.copy(.4f), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp)) {
        Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = scaledSp(15, scale))
        Text(description, color = Ink, fontSize = scaledSp(12, scale))
        Text(meta, color = Muted, fontSize = scaledSp(9, scale))
    }
}

@Composable
private fun Action(title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 86.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(.1f)).border(1.dp, accent.copy(.45f), RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(10.dp)) {
        Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(subtitle, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun PanelBox(title: String, scale: Float, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Cyan.copy(.25f), RoundedCornerShape(17.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Cyan, fontWeight = FontWeight.Bold, fontSize = scaledSp(13, scale))
        content()
    }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    Button(onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16353D), contentColor = Ink), modifier = Modifier.heightIn(min = 40.dp)) {
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun ChemPage(content: @Composable ColumnScope.(Float) -> Unit) {
    var formulaScale by rememberFormulaFontScalePreference("chemistry_formulas")
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF12313A), Bg), radius = 1300f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        FormulaFontControls(formulaScale) { formulaScale = it }
        content(formulaScale)
    }
}

private fun scaledSp(base: Int, scale: Float) = (base * scale).sp
