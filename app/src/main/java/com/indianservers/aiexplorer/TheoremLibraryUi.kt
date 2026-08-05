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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.learning.TheoremCard

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TheoremCategoryGallery(
    categories: List<String>,
    theorems: List<TheoremCard>,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Theorem categories", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("${categories.size} main categories · search by theorem name, application, or tag", color = Muted, fontSize = 11.sp)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEachIndexed { index, category ->
                val count = theorems.count { it.category == category }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceB.copy(alpha = .38f))
                        .border(1.dp, Violet.copy(alpha = .34f), RoundedCornerShape(10.dp))
                        .clickable(enabled = count > 0) { onOpen(category) }
                        .padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text((index + 1).toString().padStart(2, '0'), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(category, color = if (count > 0) Ink else Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("$count theorems", color = Green, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TheoremCategoryLibrary(
    category: String,
    theorems: List<TheoremCard>,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back to theorem categories", icon = "back", iconOnly = true, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(category, color = Violet, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${theorems.size} matching theorems", color = Muted, fontSize = 10.sp)
            }
        }
        if (theorems.isEmpty()) {
            Text("No theorems match this search.", color = Amber)
        }
        theorems.forEach { theorem ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(SurfaceB.copy(alpha = .32f))
                    .border(1.dp, Violet.copy(alpha = .36f), RoundedCornerShape(13.dp))
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(theorem.title, color = Violet, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                MathFormulaText(theorem.statement, color = Ink, fontSize = 15.sp)
                Text("${theorem.band.label} · ${theorem.level.label} · Conditions: ${theorem.conditions.joinToString()}", color = Muted, fontSize = 10.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    theorem.tags.forEach { tag ->
                        Text(
                            "#${tag.lowercase().replace(' ', '-')}",
                            color = Green,
                            fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(9.dp))
                                .background(Green.copy(alpha = .10f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
                Text("Uses: ${theorem.applications.joinToString()}", color = Cyan, fontSize = 11.sp)
                Text("Proof sketch: ${theorem.proofSketch.joinToString(" → ")}", color = Muted, fontSize = 11.sp)
            }
        }
    }
}
