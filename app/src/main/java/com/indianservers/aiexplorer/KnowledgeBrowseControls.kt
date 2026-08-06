package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofLab
import com.indianservers.aiexplorer.core.CompareModeEngine

@Composable
internal fun KnowledgeSearchField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun VisualProofCategoryPicker(query: String, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Choose a Visual Proof category", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            VisualProofCatalog.categories.forEach { category ->
                val count = VisualProofCatalog.labsFor(category).count { it.matchesProofQuery(query) }
                GlowButton("$category ($count)", enabled = count > 0) { onOpen(category) }
            }
        }
    }
}

@Composable
internal fun VisualProofList(
    category: String,
    query: String,
    compareMode: Boolean,
    compareIds: List<String>,
    onCategories: () -> Unit,
    onCompareMode: (Boolean) -> Unit,
    onCompareSelection: (String) -> Unit,
    onOpen: (VisualProofLab) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(category, color = Violet, fontWeight = FontWeight.Bold)
                Text("Select a Visual Proof to open its interactive workspace", color = Muted, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton(if (compareMode) "Compare: ON" else "Compare") { onCompareMode(!compareMode) }
                GlowButton("Categories", onClick = onCategories)
            }
        }
        if (compareMode) {
            Text(
                if (compareIds.size < 2) "Choose ${2 - compareIds.size} proof${if (compareIds.size == 1) "" else "s"}"
                else "Two proofs selected. Tap another proof to replace the oldest.",
                color = Green,
                fontSize = 11.sp,
            )
        }
        val labs = VisualProofCatalog.labsFor(category).filter { it.matchesProofQuery(query) }
        if (labs.isEmpty()) Text("No Visual Proofs match this search.", color = Amber)
        labs.forEach { lab ->
            val selected = lab.id in compareIds
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) Violet.copy(alpha = .18f) else SurfaceB.copy(alpha = .55f))
                    .border(1.dp, if (selected) Green else Violet.copy(alpha = .42f), RoundedCornerShape(13.dp))
                    .clickable { if (compareMode) onCompareSelection(lab.id) else onOpen(lab) }
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MathFormulaText(lab.formalResult, color = Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(lab.title, color = Ink, fontSize = 12.sp)
                Text(
                    if (selected) "Selected for comparison" else "#${lab.topic.lowercase().replace(' ', '-')}  ·  ${if (compareMode) "Tap to compare" else "Tap to open Visual Proof"}",
                    color = Green,
                    fontSize = 10.sp,
                )
            }
        }
        if (compareIds.size == 2) {
            val selected = compareIds.mapNotNull { id -> VisualProofCatalog.labs.firstOrNull { it.id == id } }
            if (selected.size == 2) {
                SideBySideComparePanel(
                    CompareModeEngine.compare(
                        CompareModeEngine.proof(selected[0]),
                        CompareModeEngine.proof(selected[1]),
                    ),
                )
            }
        }
    }
}

internal fun VisualProofLab.matchesProofQuery(query: String): Boolean =
    query.isBlank() || listOf(title, topic, formalResult, changesPrompt, invariantPrompt).any { it.contains(query, true) }
