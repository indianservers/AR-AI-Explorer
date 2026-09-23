package com.indianservers.aiexplorer.mathworkspace.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MathWorkspacePalette {
    val snow = Color(0xFFF7FAFF)
    val panel = Color(0xFFFFFFFF)
    val panelBlue = Color(0xFFEDF4FF)
    val ink = Color(0xFF172B4D)
    val muted = Color(0xFF657A9B)
    val border = Color(0xFFD8E4F3)
    val blue = Color(0xFF2878F0)
    val green = Color(0xFF17976D)
    val red = Color(0xFFD94B5B)
    val violet = Color(0xFF7552D9)
    val amber = Color(0xFFB87800)
}

enum class WorkspaceSheetStop { Collapsed, Peek, Expanded }

@Composable
fun WorkspaceTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().background(MathWorkspacePalette.snow).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkspaceAction("←", "Back to Maths", onBack)
        Column(Modifier.weight(1f)) {
            Text(title, color = MathWorkspacePalette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MathWorkspacePalette.muted, fontSize = 10.sp, maxLines = 1)
        }
        trailing(this)
    }
}

@Composable
fun WorkspaceSegmentedControl(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(MathWorkspacePalette.panelBlue).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            Text(
                label,
                Modifier.clip(RoundedCornerShape(9.dp))
                    .background(if (selected == index) MathWorkspacePalette.blue else Color.Transparent)
                    .clickable { onSelect(index) }.padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { contentDescription = "${if (selected == index) "Selected " else ""}$label" },
                color = if (selected == index) Color.White else MathWorkspacePalette.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun WorkspaceAction(label: String, description: String = label, onClick: () -> Unit, selected: Boolean = false, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MathWorkspacePalette.blue else MathWorkspacePalette.panel,
            contentColor = if (selected) Color.White else MathWorkspacePalette.ink,
        ),
        shape = RoundedCornerShape(11.dp),
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

data class FloatingWorkspaceTool(val label: String, val icon: String, val selected: Boolean = false, val action: () -> Unit)

@Composable
fun WorkspaceFloatingTools(tools: List<FloatingWorkspaceTool>, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(MathWorkspacePalette.panel.copy(alpha = .96f))
            .border(1.dp, MathWorkspacePalette.border, RoundedCornerShape(16.dp))
            .horizontalScroll(rememberScrollState()).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tools.forEach { tool -> WorkspaceAction(tool.icon, tool.label, tool.action, tool.selected) }
    }
}

@Composable
fun WorkspaceBottomSheet(
    title: String,
    stop: WorkspaceSheetStop,
    onStopChange: (WorkspaceSheetStop) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier.fillMaxWidth().wrapContentHeight(Alignment.Bottom).animateContentSize()) {
        val sheetHeight = (maxHeight * when (stop) {
            WorkspaceSheetStop.Collapsed -> .10f
            WorkspaceSheetStop.Peek -> .36f
            WorkspaceSheetStop.Expanded -> .70f
        }).coerceAtLeast(68.dp)
        Column(
            Modifier.fillMaxWidth().height(sheetHeight).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(MathWorkspacePalette.panel)
                .border(1.dp, MathWorkspacePalette.border, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
        ) {
            Column(
                Modifier.fillMaxWidth().pointerInput(stop) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, drag ->
                            change.consume()
                            dragDistance += drag
                        },
                        onDragEnd = {
                            when {
                                dragDistance < -36f -> onStopChange(if (stop == WorkspaceSheetStop.Collapsed) WorkspaceSheetStop.Peek else WorkspaceSheetStop.Expanded)
                                dragDistance > 36f -> onStopChange(if (stop == WorkspaceSheetStop.Expanded) WorkspaceSheetStop.Peek else WorkspaceSheetStop.Collapsed)
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                }.clickable {
                    onStopChange(when (stop) {
                        WorkspaceSheetStop.Collapsed -> WorkspaceSheetStop.Peek
                        WorkspaceSheetStop.Peek -> WorkspaceSheetStop.Expanded
                        WorkspaceSheetStop.Expanded -> WorkspaceSheetStop.Collapsed
                    })
                }.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(MathWorkspacePalette.border))
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), color = MathWorkspacePalette.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(if (stop == WorkspaceSheetStop.Expanded) "⌄" else "⌃", color = MathWorkspacePalette.blue, fontSize = 20.sp)
                }
            }
            if (stop != WorkspaceSheetStop.Collapsed) {
                Column(Modifier.weight(1f, fill = false).fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), content = content)
            }
        }
    }
}

@Composable
fun NumericField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).border(1.dp, MathWorkspacePalette.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp).semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(label, color = MathWorkspacePalette.muted, fontSize = 9.sp, maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(24.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = androidx.compose.ui.text.TextStyle(color = MathWorkspacePalette.ink, fontSize = 13.sp, textAlign = TextAlign.Center),
            decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { if (value.isEmpty()) Text("0", color = MathWorkspacePalette.muted, fontSize = 12.sp); inner() } },
        )
    }
}

@Composable
fun WorkspaceSectionTitle(title: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = MathWorkspacePalette.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        trailing(this)
    }
}
