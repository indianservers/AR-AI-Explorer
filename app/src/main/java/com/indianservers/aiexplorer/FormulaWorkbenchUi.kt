package com.indianservers.aiexplorer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.learning.FormulaCard
import com.indianservers.aiexplorer.learning.FormulaCalculation
import com.indianservers.aiexplorer.learning.FormulaExperience
import com.indianservers.aiexplorer.learning.FormulaExperienceEngine
import com.indianservers.aiexplorer.learning.FormulaVariableSpec
import kotlin.math.abs

private enum class FormulaWorkbenchTab(val label: String) {
    Overview("Overview"),
    Calculate("Calculate"),
    Learn("Learn"),
    Compare("Compare"),
    Save("Save"),
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FormulaWorkbench(
    formula: FormulaCard,
    allFormulas: List<FormulaCard>,
    libraryState: FormulaLibraryState,
    onLibraryState: (FormulaLibraryState) -> Unit,
    store: FormulaLibraryStore,
    onBack: () -> Unit,
    onOpenWorkspace: (FormulaCard) -> Unit,
) {
    val detail = remember(formula.id, allFormulas) { FormulaExperienceEngine.details(formula, allFormulas) }
    var tab by remember(formula.id) { mutableStateOf(FormulaWorkbenchTab.Overview) }
    var compareId by remember(formula.id) { mutableStateOf(detail.relatedFormulaIds.firstOrNull()) }
    val favorite = formula.id in libraryState.favorites

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back", icon = "back", iconOnly = true, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(formula.title, color = Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${detail.purpose.label} · ${formula.subcategory} · ${formula.level.label}", color = Muted, fontSize = 10.sp)
            }
            GlowButton(if (favorite) "Saved" else "Favorite", icon = if (favorite) "star" else "☆", iconOnly = true) {
                onLibraryState(store.toggleFavorite(libraryState, formula.id))
            }
        }
        MathFormulaText(formula.expression, color = Cyan, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormulaWorkbenchTab.entries.forEach { item ->
                FormulaCategoryChip(item.label, tabIcon(item), tab == item) { tab = item }
            }
        }
        when (tab) {
            FormulaWorkbenchTab.Overview -> FormulaOverview(detail, onOpenWorkspace)
            FormulaWorkbenchTab.Calculate -> FormulaCalculator(detail)
            FormulaWorkbenchTab.Learn -> FormulaLearning(detail)
            FormulaWorkbenchTab.Compare -> FormulaComparison(detail, allFormulas, compareId) { compareId = it }
            FormulaWorkbenchTab.Save -> FormulaSaveExport(detail, libraryState, onLibraryState, store)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FormulaOverview(detail: FormulaExperience, onOpenWorkspace: (FormulaCard) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        FormulaPanel("Introduction", Cyan) {
            Text(detail.formula.introduction, color = Ink, fontSize = 12.sp)
            Text(detail.whenToUse, color = Green, fontSize = 12.sp)
        }
        FormulaVisual(detail)
        GlowButton("Open linked ${detail.workspace.label}") { onOpenWorkspace(detail.formula) }
        FormulaPanel("Variables and units", Violet) {
            if (detail.variables.isEmpty()) Text("This is a symbolic identity with no direct numeric inputs.", color = Muted, fontSize = 11.sp)
            detail.variables.forEach { variable ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(variable.symbol, color = Cyan, fontWeight = FontWeight.Bold)
                    Text("${variable.label} · ${variable.dimension.label}${variable.dimension.baseUnit.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", color = Muted, fontSize = 11.sp)
                }
            }
        }
        FormulaPanel("Conditions", Amber) {
            detail.assumptions.forEach { Text("✓ $it", color = Ink, fontSize = 11.sp) }
            detail.limitations.forEach { Text("! $it", color = Amber, fontSize = 11.sp) }
        }
        FormulaPanel("Connections", Green) {
            Text("Related theorem and proof topics: ${detail.formula.relatedTerms.take(6).joinToString()}", color = Ink, fontSize = 11.sp)
            Text("${detail.relatedFormulaIds.size} related formulas · ${detail.workspace.label} visualization · local practice available", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FormulaCalculator(detail: FormulaExperience) {
    var inputs by remember(detail.formula.id) {
        mutableStateOf(detail.variables.associate { it.symbol to it.defaultInput })
    }
    var calculation by remember(detail.formula.id) { mutableStateOf<FormulaCalculation?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enter mixed units such as 25 cm, 2 m, 30 deg, or 1.5 min.", color = Muted, fontSize = 11.sp)
        if (detail.variables.isEmpty()) {
            Text("Use Learn for this symbolic identity or open its linked workspace.", color = Amber, fontSize = 12.sp)
        }
        detail.variables.take(8).forEach { spec ->
            FormulaVariableInput(spec, inputs[spec.symbol].orEmpty()) { value ->
                inputs = inputs + (spec.symbol to value)
                calculation = FormulaExperienceEngine.calculate(detail, inputs)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            GlowButton("Calculate", enabled = detail.variables.isNotEmpty()) {
                calculation = FormulaExperienceEngine.calculate(detail, inputs)
            }
            GlowButton("Reset") {
                inputs = detail.variables.associate { it.symbol to it.defaultInput }
                calculation = null
            }
        }
        calculation?.let { result ->
            FormulaPanel(if (result.valid) "Result" else "Check inputs", if (result.valid) Green else Amber) {
                if (result.valid) {
                    Text("${result.substitution} ${result.resultUnit}", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    val magnitude = abs(result.result ?: 0.0).toFloat().coerceIn(0f, 100f)
                    Canvas(Modifier.fillMaxWidth().height(42.dp)) {
                        drawLine(Muted.copy(alpha = .35f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 7f)
                        drawLine(Green, Offset(0f, size.height / 2), Offset(size.width * (magnitude / 100f), size.height / 2), 7f)
                    }
                }
                result.messages.forEach { Text(it, color = if (result.valid) Muted else Amber, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun FormulaVariableInput(spec: FormulaVariableSpec, value: String, onValue: (String) -> Unit) {
    val numeric = Regex("""[-+]?(?:\d+(?:\.\d*)?|\.\d+)""").find(value)?.value?.toFloatOrNull() ?: 0f
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("${spec.symbol} · ${spec.label}") },
            supportingText = { Text("Expected ${spec.dimension.label.lowercase()}${spec.dimension.baseUnit.takeIf { it.isNotBlank() }?.let { "; base $it" }.orEmpty()}") },
            singleLine = true,
        )
        Slider(
            value = numeric.coerceIn(spec.minimum.toFloat(), spec.maximum.toFloat()),
            onValueChange = { slider ->
                val unit = spec.dimension.baseUnit
                onValue("${slider.toInt()}${if (unit.isBlank()) "" else " $unit"}")
            },
            valueRange = spec.minimum.toFloat()..spec.maximum.toFloat(),
        )
    }
}

@Composable
private fun FormulaLearning(detail: FormulaExperience) {
    var revealPractice by remember(detail.formula.id) { mutableStateOf(false) }
    var practiceSeed by remember(detail.formula.id) { mutableIntStateOf(3) }
    val practice = remember(detail.formula.id, practiceSeed) { FormulaExperienceEngine.practice(detail, practiceSeed) }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        FormulaPanel("Derivation", Violet) {
            detail.derivation.forEachIndexed { index, step ->
                Text("${index + 1}. $step", color = Ink, fontSize = 11.sp)
            }
        }
        FormulaPanel("Rearrange and equivalent forms", Cyan) {
            detail.rearrangements.forEach { MathFormulaText(it, color = Cyan, fontSize = 14.sp) }
            detail.equivalentForms.forEach { (label, value) ->
                Text(label, color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = Ink, fontSize = 11.sp)
            }
        }
        FormulaPanel("Worked examples", Green) {
            detail.examples.forEach { example ->
                Text(example.prompt, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(example.substitution, color = Muted, fontSize = 10.sp)
                Text(example.result, color = Green, fontSize = 11.sp)
            }
        }
        FormulaPanel("Practice", Amber) {
            Text(practice.prompt, color = Ink, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton(if (revealPractice) "Hide answer" else "Hint / answer") { revealPractice = !revealPractice }
                GlowButton("New question") { practiceSeed += 7; revealPractice = false }
            }
            if (revealPractice) {
                Text(practice.substitution, color = Muted, fontSize = 10.sp)
                Text(practice.result, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FormulaComparison(
    detail: FormulaExperience,
    allFormulas: List<FormulaCard>,
    compareId: String?,
    onCompare: (String) -> Unit,
) {
    val related = detail.relatedFormulaIds.mapNotNull { id -> allFormulas.firstOrNull { it.id == id } }
    val compared = allFormulas.firstOrNull { it.id == compareId } ?: related.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Choose a related formula", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            related.take(8).forEach { formula ->
                FormulaCategoryChip(formula.title.take(18), "=", compared?.id == formula.id) { onCompare(formula.id) }
            }
        }
        if (compared == null) {
            Text("No close match was found in this category.", color = Amber)
        } else {
            val other = remember(compared.id) { FormulaExperienceEngine.details(compared, allFormulas) }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormulaCompareCard(detail, Modifier.width(260.dp))
                FormulaCompareCard(other, Modifier.width(260.dp))
            }
            FormulaPanel("Key difference", Violet) {
                Text(
                    "${detail.formula.title} is mainly used for ${detail.purpose.label.lowercase()}, while ${other.formula.title} is used for ${other.purpose.label.lowercase()}. Check their variable lists and assumptions before choosing.",
                    color = Ink,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun FormulaCompareCard(detail: FormulaExperience, modifier: Modifier = Modifier) {
    FormulaPanel(detail.formula.title, Cyan, modifier) {
        MathFormulaText(detail.formula.expression, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(detail.whenToUse, color = Ink, fontSize = 10.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Text("Variables: ${detail.formula.variables.joinToString()}", color = Muted, fontSize = 10.sp)
        Text("Output: ${detail.outputDimension.label}", color = Green, fontSize = 10.sp)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FormulaSaveExport(
    detail: FormulaExperience,
    state: FormulaLibraryState,
    onState: (FormulaLibraryState) -> Unit,
    store: FormulaLibraryStore,
) {
    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var collectionName by remember { mutableStateOf("Exam revision") }
    var message by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        FormulaPanel("Collections", Green) {
            OutlinedTextField(collectionName, { collectionName = it.take(32) }, label = { Text("Collection name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (state.collections.keys + collectionName.trim()).filter(String::isNotBlank).distinct().sorted().forEach { name ->
                    val selected = detail.formula.id in state.collections[name].orEmpty()
                    FormulaCategoryChip(name, if (selected) "✓" else "+", selected) {
                        onState(store.toggleCollection(state, name, detail.formula.id))
                    }
                }
            }
        }
        FormulaPanel("Copy and export", Cyan) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Readable") {
                    clipboard.setPrimaryClip(ClipData.newPlainText(detail.formula.title, FormulaExperienceEngine.exportText(detail)))
                    message = "Formula notes copied."
                }
                GlowButton("LaTeX") {
                    clipboard.setPrimaryClip(ClipData.newPlainText("${detail.formula.title} LaTeX", detail.formula.expression))
                    message = "LaTeX copied."
                }
                GlowButton("MathML") {
                    clipboard.setPrimaryClip(ClipData.newPlainText("${detail.formula.title} MathML", FormulaExperienceEngine.exportMathMl(detail.formula)))
                    message = "MathML copied."
                }
                GlowButton("Image") {
                    FormulaImageExporter.share(context, detail)
                }
                GlowButton("Share") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, detail.formula.title)
                        putExtra(Intent.EXTRA_TEXT, FormulaExperienceEngine.exportText(detail))
                    }
                    context.startActivity(Intent.createChooser(intent, "Share formula"))
                }
            }
            if (message.isNotBlank()) Text(message, color = Green, fontSize = 11.sp)
            Text("The complete formula catalogue and these notes remain available offline.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FormulaVisual(detail: FormulaExperience) {
    FormulaPanel("Visual model", Violet) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val count = detail.variables.size.coerceIn(1, 6)
            val step = size.width / (count + 1)
            repeat(count) { index ->
                val x = step * (index + 1)
                val height = size.height * (.25f + .11f * index)
                drawLine(Cyan.copy(alpha = .35f), Offset(size.width / 2, size.height * .88f), Offset(x, size.height - height), 3f)
                drawCircle(listOf(Cyan, Green, Violet, Amber)[index % 4], 9f, Offset(x, size.height - height))
            }
            drawLine(Muted.copy(alpha = .45f), Offset(0f, size.height * .88f), Offset(size.width, size.height * .88f), 2f)
        }
        Text("Each point represents an input variable; Calculate updates the numeric relationship and the linked workspace provides the full domain visualization.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun FormulaPanel(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(SurfaceB.copy(alpha = .3f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = .34f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

private fun tabIcon(tab: FormulaWorkbenchTab) = when (tab) {
    FormulaWorkbenchTab.Overview -> "i"
    FormulaWorkbenchTab.Calculate -> "="
    FormulaWorkbenchTab.Learn -> "?"
    FormulaWorkbenchTab.Compare -> "⇄"
    FormulaWorkbenchTab.Save -> "☆"
}
