package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HomeSearchSuggestion(
    val label: String,
    val supportingText: String,
    val concept: Boolean,
    val lessonId: String? = null,
    val conceptTitle: String? = null,
    val dictionaryTermKey: String? = null,
    val query: String = label,
    val kindLabel: String = if (concept) "Concept" else "Tool",
)

@Composable
fun HomeSearchSuggestions(
    query: String,
    suggestions: List<HomeSearchSuggestion>,
    onSample: (String) -> Unit,
    onSuggestion: (HomeSearchSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cyan = Color(0xFF20D9FF)
    val violet = Color(0xFF9B6CFF)
    val ink = Color(0xFFEAF7FF)
    val muted = Color(0xFF91A4B5)
    if (query.isBlank()) {
        Column(modifier.fillMaxWidth()) {
            Text("TRY A SEARCH", color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            FlowRow {
                listOf("quadratic equations", "circle area", "matrix", "probability").forEach { sample ->
                    Text(
                        sample,
                        color = cyan,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 5.dp, end = 6.dp)
                            .background(cyan.copy(.08f), RoundedCornerShape(10.dp))
                            .border(1.dp, cyan.copy(.35f), RoundedCornerShape(10.dp))
                            .clickable { onSample(sample) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    } else if (suggestions.isNotEmpty()) {
        Column(
            modifier.fillMaxWidth().background(Color(0xF20A1522), RoundedCornerShape(14.dp))
                .border(1.dp, violet.copy(.38f), RoundedCornerShape(14.dp)).padding(6.dp),
        ) {
            Text("SUGGESTIONS", color = violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            suggestions.take(8).forEach { suggestion ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSuggestion(suggestion) }
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                ) {
                    Text(
                        when {
                            suggestion.dictionaryTermKey != null -> "A-Z"
                            suggestion.lessonId != null -> "L"
                            suggestion.concept -> "Fx"
                            else -> "S"
                        },
                        color = if (suggestion.concept) violet else cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(suggestion.label, color = ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("${suggestion.kindLabel} · ${suggestion.supportingText}", color = muted, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
