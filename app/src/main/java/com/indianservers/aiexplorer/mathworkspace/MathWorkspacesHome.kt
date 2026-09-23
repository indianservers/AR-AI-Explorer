package com.indianservers.aiexplorer.mathworkspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceTopBar

@Composable
fun MathWorkspacesHome(onBack: () -> Unit, onOpenVectorLab: () -> Unit, onOpenMathTiles: () -> Unit, onOpenProbabilityLab: () -> Unit = {}, onOpenCalculusLab: () -> Unit = {}, onOpenPhysicsMath: () -> Unit = {}, onOpenMathematicalArt: () -> Unit = {}, onOpenLinearTransform: () -> Unit = {}, onOpenSpreadsheet: () -> Unit = {}, onOpenDiscreteMath: () -> Unit = {}, onOpenNumberTheory: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(P.snow).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        WorkspaceTopBar("Math Workspaces", "Interactive mathematical models", onBack)
        Text("Choose a workspace", Modifier.padding(horizontal = 4.dp), color = P.muted, fontSize = 11.sp)
        WorkspaceEntry("Vector Lab", "Explore 2D and 3D vectors with direct numeric controls and a pan-and-zoom canvas", "Vector", onOpenVectorLab)
        WorkspaceEntry("Math Tiles", "Build polynomial expressions by arranging and simplifying algebra tiles", "Tiles", onOpenMathTiles)
        WorkspaceEntry("Probability & Statistics Lab", "Explore distributions, sampling, and probability regions", "P(X)", onOpenProbabilityLab)
        WorkspaceEntry("Calculus Lab", "Visualize functions, derivatives, integrals, and limits", "∫", onOpenCalculusLab)
        WorkspaceEntry("Physics–Math Workspace", "Simulate motion and oscillations with live mathematical graphs", "∿", onOpenPhysicsMath)
        WorkspaceEntry("Mathematical Art", "Create colorful curves from mathematical equations", "𝑓", onOpenMathematicalArt)
        WorkspaceEntry("Matrices & Linear Transformations", "Explore 2D matrix transformations, composition, and eigenvectors", "A·x", onOpenLinearTransform)
        WorkspaceEntry("Data Table & Spreadsheet", "Edit cells, calculate formulas, and explore data", "Σ", onOpenSpreadsheet)
        WorkspaceEntry("Discrete Mathematics Lab", "Explore graphs, sets, relations, counting, and logic", "G=(V,E)", onOpenDiscreteMath)
        WorkspaceEntry("Number Theory Lab", "Explore primes, factors, congruences, and integer solutions", "ℤ", onOpenNumberTheory)
    }
}

@Composable
private fun WorkspaceEntry(title: String, description: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(P.panel).border(1.dp, P.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).semantics { contentDescription = "Open $title. $description" }.padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(tag, Modifier.clip(RoundedCornerShape(10.dp)).background(P.panelBlue).padding(horizontal = 10.dp, vertical = 8.dp), color = P.blue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = P.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(description, color = P.muted, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Text("→", color = P.blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
