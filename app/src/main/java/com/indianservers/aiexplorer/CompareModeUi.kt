package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.ComparisonItem
import com.indianservers.aiexplorer.core.ComparisonReport

@Composable
internal fun SideBySideComparePanel(
    report: ComparisonReport,
    leftAccent: Color = Cyan,
    rightAccent: Color = Violet,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xE609121D), RoundedCornerShape(8.dp))
            .border(1.dp, leftAccent.copy(alpha = .35f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CompareItemPane(report.left, report, true, leftAccent, Modifier.weight(1f))
            CompareItemPane(report.right, report, false, rightAccent, Modifier.weight(1f))
        }
        Text(
            "${report.sharedCount} shared properties  |  ${report.differenceCount} differences",
            color = if (report.differenceCount == 0) Green else Amber,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CompareItemPane(
    item: ComparisonItem,
    report: ComparisonReport,
    left: Boolean,
    accent: Color,
    modifier: Modifier,
) {
    Column(
        modifier
            .background(accent.copy(alpha = .08f), RoundedCornerShape(6.dp))
            .border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(item.title, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        MathFormulaText(item.primary, color = Ink, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        report.rows.forEach { row ->
            Text(row.label, color = Muted, fontSize = 9.sp, maxLines = 1)
            Text(
                if (left) row.left else row.right,
                color = if (row.matches) Green else Ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
