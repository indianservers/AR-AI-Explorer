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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.learning.FormulaCard
import com.indianservers.aiexplorer.learning.FormulaCategory
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
    onOpenCategory: (FormulaCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Choose a formula category", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Open a main category to see its formulas directly.", color = Muted, fontSize = 11.sp)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormulaCategory.entries.forEach { category ->
                val count = formulas.count { it.category == category }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceB.copy(alpha = 0.38f))
                        .border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(8.dp))
                        .clickable(enabled = count > 0) { onOpenCategory(category) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TransparentIcon(category.icon(), if (count > 0) Cyan else Muted)
                        Text("$count", color = Muted, fontSize = 10.sp)
                    }
                    Text(category.label, color = if (count > 0) Ink else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to browse formulas", color = Violet, fontSize = 10.sp)
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
) {
    var formulaScale by rememberFormulaFontScalePreference("math_formulas")
    val tags = formulas.flatMap { it.tags }.distinct().sortedByFormulaFilter()
    val visible = formulas.filter { selectedTag == null || selectedTag in it.tags }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back to categories", icon = "back", iconOnly = true, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(category.label, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${visible.size} formulas · choose a tag to filter", color = Muted, fontSize = 10.sp)
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
            visible.forEach { formula -> FormulaItemCard(formula, formulaScale) }
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
private fun FormulaItemCard(formula: FormulaCard, scale: Float) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceB.copy(alpha = 0.28f))
            .border(1.dp, Cyan.copy(alpha = .22f), RoundedCornerShape(12.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(formula.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = scaledSp(13, scale))
        Text(displayLatexFormula(formula.expression), color = Cyan, fontSize = scaledSp(18, scale), fontWeight = FontWeight.Bold)
        Text("${formula.category.label} · ${formula.subcategory} · ${formula.level.label}", color = Violet, fontSize = scaledSp(10, scale))
        Text(formula.introduction, color = Ink.copy(alpha = .86f), fontSize = scaledSp(10, scale), lineHeight = scaledSp(13, scale))
        Text(formula.useCase, color = Muted, fontSize = scaledSp(11, scale))
        if (formula.tags.isNotEmpty()) {
            Text(formula.tags.joinToString("  ") { "#$it" }, color = Green, fontSize = scaledSp(10, scale))
        }
        if (formula.variables.isNotEmpty()) {
            Text("Variables: ${formula.variables.joinToString()}", color = Muted, fontSize = scaledSp(11, scale))
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
