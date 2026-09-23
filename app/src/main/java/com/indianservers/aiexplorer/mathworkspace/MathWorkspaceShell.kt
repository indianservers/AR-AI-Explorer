package com.indianservers.aiexplorer.mathworkspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceBottomSheet
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceTopBar

@Composable
fun MathWorkspaceShell(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    sheetTitle: String,
    sheetStop: WorkspaceSheetStop,
    onSheetStopChange: (WorkspaceSheetStop) -> Unit,
    topBarTrailing: @Composable RowScope.() -> Unit = {},
    canvas: @Composable BoxScope.() -> Unit,
    tools: @Composable BoxScope.() -> Unit,
    sheet: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MathWorkspacePalette.snow), verticalArrangement = Arrangement.Top) {
        WorkspaceTopBar(title, subtitle, onBack, topBarTrailing)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            canvas()
            tools()
            WorkspaceBottomSheet(
                title = sheetTitle,
                stop = sheetStop,
                onStopChange = onSheetStopChange,
                modifier = Modifier.align(Alignment.BottomCenter),
                content = sheet,
            )
        }
    }
}
