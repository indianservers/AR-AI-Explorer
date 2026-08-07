package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.learning.FormulaCard
import com.indianservers.aiexplorer.learning.FormulaCategory
import com.indianservers.aiexplorer.learning.FormulaDimension
import com.indianservers.aiexplorer.learning.FormulaExperienceEngine
import com.indianservers.aiexplorer.learning.FormulaFilterSet
import com.indianservers.aiexplorer.learning.FormulaPurpose
import com.indianservers.aiexplorer.learning.KnowledgeLevel
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import com.indianservers.aiexplorer.learning.icon

@Composable
internal fun FormulaCategoryChip(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Cyan.copy(alpha = .18f) else Color.Transparent)
            .border(1.dp, if (selected) Cyan.copy(alpha = .72f) else Cyan.copy(alpha = .22f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Cyan.copy(alpha = if (selected) .20f else .08f))
                .border(1.dp, Cyan.copy(alpha = .38f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(label, color = if (selected) Cyan else Ink, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
internal fun FormulaCategoryLibrary(formulas: List<FormulaCard>) {
    var formulaScale by rememberFormulaFontScalePreference("math_formulas")
    if (formulas.isEmpty()) {
        Text("No formulas match the selected category or sub category.", color = Amber)
        return
    }
    FormulaFontControls(formulaScale) { formulaScale = it }
    formulas
        .groupBy { it.category }
        .toList()
        .sortedBy { it.first.ordinal }
        .forEach { (category, categoryFormulas) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceB.copy(alpha = 0.20f))
                    .border(1.dp, Cyan.copy(alpha = .24f), RoundedCornerShape(14.dp))
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FormulaCategoryChip(category.label, category.icon(), selected = true, onClick = {})
                    Text("${categoryFormulas.size}", color = Muted, fontSize = 10.sp)
                }
                categoryFormulas.groupBy { it.subcategory }.forEach { (subcategory, subcategoryFormulas) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(subcategory, color = Violet, fontSize = scaledSp(12, formulaScale), fontWeight = FontWeight.Bold)
                            Text("${subcategoryFormulas.size} formulas", color = Muted, fontSize = scaledSp(9, formulaScale))
                        }
                        subcategoryFormulas.forEach { formula -> FormulaItemCard(formula, formulaScale) }
                    }
                }
            }
        }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FormulaCategoryGallery(
    formulas: List<FormulaCard>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenCategory: (FormulaCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF10112D), Color(0xFF071A31), Color(0xFF170A2D)),
                    ),
                )
                .border(1.dp, Violet.copy(alpha = .42f), RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Formulas", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("${FormulaCategory.entries.size} categories", color = Muted, fontSize = 11.sp)
                    Text("${formulas.size} formulas", color = Muted, fontSize = 11.sp)
                }
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Violet.copy(alpha = .42f), Cyan.copy(alpha = .12f), Color.Transparent),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("π", color = Violet, fontSize = 54.sp)
                }
            }
            KnowledgeSearchField(query, "Search formulas or tags", onQueryChange)
        }
        Text("Choose a formula category", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Open a main category to see its formulas directly.", color = Muted, fontSize = 11.sp)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            FormulaCategory.entries.forEach { category ->
                val count = formulas.count { it.category == category }
                val accent = formulaCategoryAccent(category)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(accent.copy(alpha = .14f), SurfaceB.copy(alpha = .52f))))
                        .border(1.dp, accent.copy(alpha = .50f), RoundedCornerShape(8.dp))
                        .clickable(enabled = count > 0) { onOpenCategory(category) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = .18f))
                                .border(1.dp, accent.copy(alpha = .46f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(category.icon(), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("$count", color = Ink, fontSize = 9.sp)
                    }
                    Text(category.label, color = if (count > 0) Ink else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Explore →", color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FormulaDirectCategoryLibrary(
    category: FormulaCategory,
    formulas: List<FormulaCard>,
    selectedTag: String?,
    onTag: (String?) -> Unit,
    onBack: () -> Unit,
    onOpenWorkspace: (FormulaCard) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { FormulaLibraryStore(context) }
    var libraryState by remember { mutableStateOf(store.load()) }
    var formulaScale by rememberFormulaFontScalePreference("math_formulas")
    var selectedFormulaId by remember(category) { mutableStateOf<String?>(null) }
    var filtersOpen by remember(category) { mutableStateOf(false) }
    var purpose by remember(category) { mutableStateOf<FormulaPurpose?>(null) }
    var dimension by remember(category) { mutableStateOf<FormulaDimension?>(null) }
    var level by remember(category) { mutableStateOf<KnowledgeLevel?>(null) }
    var requiredVariable by remember(category) { mutableStateOf<String?>(null) }
    var outputSymbol by remember(category) { mutableStateOf<String?>(null) }
    var shelf by remember(category) { mutableStateOf<String?>(null) }

    selectedFormulaId?.let { formulaId ->
        MathKnowledgeCatalog.formulas.firstOrNull { it.id == formulaId }?.let { formula ->
            FormulaWorkbench(
                formula = formula,
                allFormulas = MathKnowledgeCatalog.formulas,
                libraryState = libraryState,
                onLibraryState = { libraryState = it },
                store = store,
                onBack = { selectedFormulaId = null },
                onOpenWorkspace = onOpenWorkspace,
            )
            return
        }
    }

    val tags = formulas.flatMap { it.tags }.distinct().sortedByFormulaFilter()
    val variables = formulas.flatMap { it.variables }.distinct().sorted().take(16)
    val outputs = formulas.mapNotNull { FormulaExperienceEngine.details(it, formulas).outputSymbol }.distinct().sorted().take(12)
    val filtered = FormulaExperienceEngine.filter(
        formulas,
        query = "",
        filters = FormulaFilterSet(
            purpose = purpose,
            outputSymbol = outputSymbol,
            requiredVariable = requiredVariable,
            dimension = dimension,
            level = level,
        ),
    )
    val visible = filtered.filter { formula ->
        (selectedTag == null || selectedTag in formula.tags) &&
            when {
                shelf == "favorites" -> formula.id in libraryState.favorites
                shelf == "recent" -> formula.id in libraryState.recent
                shelf?.startsWith("collection:") == true ->
                    formula.id in libraryState.collections[shelf!!.substringAfter(':')].orEmpty()
                else -> true
            }
    }.sortedBy { formula ->
        libraryState.recent.indexOf(formula.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back to categories", icon = "back", iconOnly = true, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(category.label, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${visible.size} formulas · choose a tag to filter", color = Muted, fontSize = 10.sp)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormulaCategoryChip("All", "A", shelf == null) { shelf = null }
            FormulaCategoryChip("Favorites", "☆", shelf == "favorites") {
                shelf = if (shelf == "favorites") null else "favorites"
            }
            FormulaCategoryChip("Recent", "R", shelf == "recent") {
                shelf = if (shelf == "recent") null else "recent"
            }
            libraryState.collections.keys.sorted().take(4).forEach { name ->
                val key = "collection:$name"
                FormulaCategoryChip(name, "C", shelf == key) { shelf = if (shelf == key) null else key }
            }
            FormulaCategoryChip(if (filtersOpen) "Close filters" else "More filters", "F", filtersOpen) {
                filtersOpen = !filtersOpen
            }
        }
        if (filtersOpen) {
            Column(
                Modifier.fillMaxWidth()
                    .background(SurfaceB.copy(alpha = .24f), RoundedCornerShape(8.dp))
                    .border(1.dp, Violet.copy(alpha = .28f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Purpose", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    FormulaCategoryChip("Any", "·", purpose == null) { purpose = null }
                    FormulaPurpose.entries.forEach { item ->
                        FormulaCategoryChip(item.label, "=", purpose == item) {
                            purpose = if (purpose == item) null else item
                        }
                    }
                }
                Text("Output and level", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    FormulaDimension.entries.forEach { item ->
                        FormulaCategoryChip(item.label, "u", dimension == item) {
                            dimension = if (dimension == item) null else item
                        }
                    }
                    KnowledgeLevel.entries.forEach { item ->
                        FormulaCategoryChip(item.label, "L", level == item) {
                            level = if (level == item) null else item
                        }
                    }
                }
                if (outputs.isNotEmpty()) {
                    Text("Find output", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        outputs.forEach { symbol ->
                            FormulaCategoryChip(symbol, "=", outputSymbol == symbol) {
                                outputSymbol = if (outputSymbol == symbol) null else symbol
                            }
                        }
                    }
                }
                if (variables.isNotEmpty()) {
                    Text("Contains variable", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        variables.forEach { symbol ->
                            FormulaCategoryChip(symbol, "v", requiredVariable == symbol) {
                                requiredVariable = if (requiredVariable == symbol) null else symbol
                            }
                        }
                    }
                }
            }
        }
        if (tags.isNotEmpty()) {
            Text("Filters", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormulaCategoryChip("All", "•", selectedTag == null) { onTag(null) }
                tags.forEach { tag ->
                    FormulaCategoryChip(tag.replaceFirstChar(Char::uppercase), "#", selectedTag == tag) {
                        onTag(if (selectedTag == tag) null else tag)
                    }
                }
            }
        }
        FormulaFontControls(formulaScale) { formulaScale = it }
        if (visible.isEmpty()) {
            Text("No formulas match this tag and search.", color = Amber)
        } else {
            visible.forEach { formula ->
                FormulaItemCard(
                    formula = formula,
                    scale = formulaScale,
                    favorite = formula.id in libraryState.favorites,
                    onFavorite = { libraryState = store.toggleFavorite(libraryState, formula.id) },
                    onClick = {
                        libraryState = store.viewed(libraryState, formula.id)
                        selectedFormulaId = formula.id
                    },
                )
            }
        }
    }
}

@Composable
internal fun FormulaSubcategoryGallery(
    category: FormulaCategory,
    formulas: List<FormulaCard>,
    onBack: () -> Unit,
    onOpenSubcategory: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back to categories", icon = "back", iconOnly = true, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(category.label, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Choose a subcategory", color = Muted, fontSize = 11.sp)
            }
        }
        category.subcategories.forEach { subcategory ->
            val count = formulas.count { it.category == category && it.subcategory == subcategory }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceB.copy(alpha = 0.38f))
                    .border(1.dp, Violet.copy(alpha = .34f), RoundedCornerShape(8.dp))
                    .clickable(enabled = count > 0) { onOpenSubcategory(subcategory) }
                    .padding(11.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Violet.copy(alpha = .14f))
                        .border(1.dp, Violet.copy(alpha = .42f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text((category.subcategories.indexOf(subcategory) + 1).toString(), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(subcategory, color = if (count > 0) Ink else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("$count formulas", color = Muted, fontSize = 10.sp)
                }
                Text(">", color = if (count > 0) Cyan else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun FormulaSubcategoryLibrary(
    category: FormulaCategory,
    subcategory: String,
    formulas: List<FormulaCard>,
    onBack: () -> Unit,
) {
    var formulaScale by rememberFormulaFontScalePreference("math_formulas")
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back to subcategories", icon = "back", iconOnly = true, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(subcategory, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${category.label} · ${formulas.size} formulas", color = Muted, fontSize = 10.sp)
            }
        }
        FormulaFontControls(formulaScale) { formulaScale = it }
        if (formulas.isEmpty()) {
            Text("No formulas match the current search and filters.", color = Amber)
        } else {
            formulas.forEach { formula -> FormulaItemCard(formula, formulaScale) }
        }
    }
}

@Composable
private fun FormulaItemCard(
    formula: FormulaCard,
    scale: Float,
    favorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val accent = formulaCategoryAccent(formula.category)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = .08f), SurfaceB.copy(alpha = .34f))))
            .border(1.dp, accent.copy(alpha = .34f), RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(formula.category.icon(), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    formula.title,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = scaledSp(14, scale),
                )
            }
            onFavorite?.let {
                FormulaCategoryChip(if (favorite) "Saved" else "Save", if (favorite) "★" else "☆", favorite, it)
            }
        }
        FormulaLatexText(
            formula = formula.expression,
            color = accent,
            fontSize = scaledSp(21, scale),
            fontWeight = FontWeight.SemiBold,
        )
        Text("${formula.category.label} · ${formula.subcategory} · ${formula.level.label}", color = Violet, fontSize = scaledSp(10, scale))
        Text(formula.introduction, color = Ink.copy(alpha = .86f), fontSize = scaledSp(10, scale), lineHeight = scaledSp(13, scale))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(Amber.copy(alpha = .07f))
                .border(1.dp, Amber.copy(alpha = .22f), RoundedCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("!", color = Amber, fontSize = scaledSp(10, scale), fontWeight = FontWeight.Bold)
            Text(formula.useCase, color = Ink, fontSize = scaledSp(10, scale))
        }
        if (formula.tags.isNotEmpty()) {
            Text(formula.tags.joinToString("  ") { "#$it" }, color = Green, fontSize = scaledSp(10, scale))
        }
        if (formula.variables.isNotEmpty()) {
            Text("Variables: ${formula.variables.joinToString()}", color = Muted, fontSize = scaledSp(11, scale))
        }
        if (onClick != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FormulaCategoryChip("Details", "≡", false, onClick)
                FormulaCategoryChip("Examples", "E", false, onClick)
                FormulaCategoryChip("Practice", "P", false, onClick)
            }
        }
    }
}

@Composable
private fun FormulaFontControls(scale: Float, onScale: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        FormulaCategoryChip("-", "-", selected = false) { onScale((scale - .1f).coerceAtLeast(.75f)) }
        Text("${(scale * 100).toInt()}%", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp))
        FormulaCategoryChip("+", "+", selected = false) { onScale((scale + .1f).coerceAtMost(1.45f)) }
        FormulaCategoryChip("Reset", "R", selected = false) { onScale(1f) }
    }
}

private fun scaledSp(base: Int, scale: Float) = (base * scale).sp

private fun formulaCategoryAccent(category: FormulaCategory): Color = when (category) {
    FormulaCategory.AlgebraFunctions -> Violet
    FormulaCategory.GeometryMensuration -> Cyan
    FormulaCategory.Trigonometry -> Color(0xFFFF4BAE)
    FormulaCategory.CalculusAnalysis -> Green
    FormulaCategory.DifferentialEquations -> Color(0xFFFF8A3D)
    FormulaCategory.LinearAlgebraVectors -> Color(0xFF46A6FF)
    FormulaCategory.CoordinateGeometry3D -> Color(0xFF35D6B4)
    FormulaCategory.ProbabilityCombinatorics -> Color(0xFFFFD166)
    FormulaCategory.StatisticsDistributions -> Color(0xFF7BE495)
    FormulaCategory.NumberTheory -> Color(0xFFFF6B6B)
    FormulaCategory.ComplexNumbers -> Color(0xFFA78BFA)
    FormulaCategory.NumericalMethods -> Color(0xFF38BDF8)
}

private fun List<String>.sortedByFormulaFilter(): List<String> {
    val priority = listOf(
        "area", "perimeter", "volume", "distance", "angle", "triangle", "circle",
        "identity", "equation", "derivative", "integral", "matrix", "vector",
        "probability", "statistics", "counting",
    )
    return distinct().sortedWith(
        compareBy<String> {
            val index = priority.indexOf(it)
            if (index == -1) priority.size else index
        }.thenBy { it },
    )
}
