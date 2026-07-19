package com.indianservers.aiexplorer.chemistry.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.chemistry.model.ChemicalElement

private val ChemistryBackground = Color(0xFF050C12)
private val ChemistryPanel = Color(0xE80C1B24)
private val ChemistryCyan = Color(0xFF55DDE0)
private val ChemistryGreen = Color(0xFF72E6A8)
private val ChemistryAmber = Color(0xFFFFC86B)
private val ChemistryViolet = Color(0xFFB49CFF)
private val ChemistryInk = Color(0xFFF1F8FA)
private val ChemistryMuted = Color(0xFF9CB2B8)

@Composable
fun ChemistryHomeScreen(
    onBack: () -> Unit,
    onOpenFormulas: () -> Unit = {},
    onOpenConnectedJourney: () -> Unit = {},
    vm: ChemistryHomeViewModel = viewModel(),
) {
    val state = vm.state
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF12313A), ChemistryBackground), radius = 1300f)),
    ) {
        val wide = maxWidth >= 760.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 34.dp else 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CHEMISTRY LAB", color = ChemistryCyan, fontSize = if (wide) 34.sp else 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Explore matter, atoms, elements, molecules and reactions interactively", color = ChemistryMuted, fontSize = 12.sp)
                }
                ChemistryButton("Subjects", onBack)
            }
            ValidationBanner(state.validation.valid, state.validation.elementCount, state.datasetVersion)
            ChemistryFormulaEntry(onOpenFormulas)
            ChemistryJourneyEntry(onOpenConnectedJourney)
            ChemistryPanel(title = "Offline element repository") {
                Text("Search the validated Phase 1 dataset by element name, symbol, or atomic number.", color = ChemistryMuted, fontSize = 11.sp)
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::search,
                    label = { Text("Search 118 elements") },
                    placeholder = { Text("Oxygen, O, or 8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Chemistry element search by name, symbol, or atomic number" },
                )
                if (state.results.isEmpty()) Text("No elements match this search.", color = ChemistryAmber, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.results.forEach { element -> ElementPreviewTile(element, state.selectedElement?.atomicNumber == element.atomicNumber) { vm.select(element) } }
                }
            }
            state.selectedElement?.let { SelectedElementPanel(it, vm::clearSelection) }
            Text("Interactive Chemistry roadmap", color = ChemistryViolet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChemistryModuleCard("Periodic Table", "Pan, zoom, filter and compare all 118 elements", "Phase 2", ChemistryCyan, Modifier.weight(1.4f))
                    ChemistryModuleCard("Explore Elements", "Rich element details and atomic structure", "Phase 3", ChemistryGreen, Modifier.weight(1f))
                    ChemistryModuleCard("Electron Configuration", "Orbitals, Aufbau and build mode", "Phase 4", ChemistryViolet, Modifier.weight(1f))
                }
            } else {
                ChemistryModuleCard("Periodic Table", "Pan, zoom, filter and compare all 118 elements", "Phase 2", ChemistryCyan)
                ChemistryModuleCard("Explore Elements", "Rich element details and atomic structure", "Phase 3", ChemistryGreen)
                ChemistryModuleCard("Electron Configuration", "Orbitals, Aufbau and build mode", "Phase 4", ChemistryViolet)
            }
            ChemistryPanel(title = "Coming later · intentionally inactive") {
                Text("Compare Elements · Periodic Trends · Element Challenges · Molecular Builder · Chemical Bonding · Reaction Simulator · Acids and Bases · Virtual Laboratory", color = ChemistryMuted, fontSize = 11.sp)
                Text("These destinations do not open empty screens. They will activate only when their implementation phase is complete.", color = ChemistryAmber, fontSize = 10.sp)
            }
            Text("Core Chemistry data works fully offline. Optional values that are not yet curated remain unavailable—not zero.", color = ChemistryMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ChemistryJourneyEntry(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 92.dp).clip(RoundedCornerShape(18.dp)).background(ChemistryPanel).border(1.dp, ChemistryViolet.copy(.65f), RoundedCornerShape(18.dp)).clickable(onClick = onClick).semantics { contentDescription = "Open connected Chemistry journey from Atomic Structure to Molecular Geometry" }.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("CONNECTED LEARNING · ATOMS TO SHAPE", color = ChemistryViolet, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Learn → Explore → Test with prerequisites, diagrams and chemistry tools", color = ChemistryInk, fontSize = 11.sp)
        }
        Text("START  →", color = ChemistryGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChemistryFormulaEntry(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(ChemistryCyan.copy(.16f), ChemistryViolet.copy(.12f)))).border(1.dp, ChemistryCyan.copy(.65f), RoundedCornerShape(18.dp)).clickable(onClick = onClick).semantics { contentDescription = "Open Chemistry Formulas: chemical relationships, calculations, equations and applications" }.padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("CHEMISTRY FORMULAS", color = ChemistryCyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("Chemical relationships, calculations, equations and applications", color = ChemistryInk, fontSize = 11.sp)
            Text("26 categories · offline · separate Chemistry repository", color = ChemistryGreen, fontSize = 9.sp)
        }
        Text("OPEN  →", color = ChemistryAmber, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ValidationBanner(valid: Boolean, count: Int, version: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (valid) ChemistryGreen.copy(.11f) else ChemistryAmber.copy(.12f)).border(1.dp, if (valid) ChemistryGreen else ChemistryAmber, RoundedCornerShape(14.dp)).padding(11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(if (valid) "✓ $count elements validated" else "Dataset validation needs attention", color = if (valid) ChemistryGreen else ChemistryAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(version, color = ChemistryMuted, fontSize = 9.sp)
    }
}

@Composable
private fun ElementPreviewTile(element: ChemicalElement, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(112.dp).heightIn(min = 124.dp).clip(RoundedCornerShape(15.dp)).background(if (selected) ChemistryCyan.copy(.18f) else ChemistryPanel).border(if (selected) 3.dp else 1.dp, if (selected) ChemistryCyan else ChemistryCyan.copy(.3f), RoundedCornerShape(15.dp)).clickable(onClick = onClick).semantics {
            contentDescription = "${element.name}, symbol ${element.symbol}, atomic number ${element.atomicNumber}, ${element.category.label}, period ${element.period}, ${element.group?.let { "group $it" } ?: "f-block"}"
        }.padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(element.atomicNumber.toString(), color = ChemistryMuted, fontSize = 10.sp)
        Text(element.symbol, color = ChemistryCyan, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text(element.name, color = ChemistryInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(element.atomicWeightDisplay, color = ChemistryMuted, fontSize = 9.sp)
        Text(element.category.label, color = ChemistryGreen, fontSize = 8.sp, maxLines = 2)
    }
}

@Composable
private fun SelectedElementPanel(element: ChemicalElement, onClose: () -> Unit) {
    ChemistryPanel(title = "Selected element · ${element.name}") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)).background(ChemistryCyan.copy(.14f)).border(2.dp, ChemistryCyan, RoundedCornerShape(18.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(element.atomicNumber.toString(), color = ChemistryMuted, fontSize = 10.sp)
                Text(element.symbol, color = ChemistryCyan, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(element.atomicWeightDisplay, color = ChemistryInk, fontSize = 9.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(element.description, color = ChemistryInk, fontSize = 11.sp)
                Text("Period ${element.period} · ${element.group?.let { "Group $it" } ?: "Separated f-block"} · ${element.block.name.lowercase()} block · ${element.standardState.name}", color = ChemistryGreen, fontSize = 10.sp)
                Text("Shells: ${element.electronConfiguration.shellDistribution.joinToString("–")}", color = ChemistryViolet, fontSize = 10.sp)
                Text(element.electronConfiguration.nobleGasShorthand, color = ChemistryAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Representative isotope: ${element.symbol}-${element.representativeIsotope.massNumber}; ${element.representativeNeutronCount} representative neutrons", color = ChemistryMuted, fontSize = 9.sp)
            }
            ChemistryButton("Close", onClose)
        }
    }
}

@Composable
private fun ChemistryModuleCard(title: String, description: String, phase: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ChemistryPanel).border(1.dp, accent.copy(.48f), RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(description, color = ChemistryInk, fontSize = 11.sp)
        Text("PLANNED · $phase", color = ChemistryMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChemistryPanel(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xED102832), ChemistryPanel))).border(1.dp, ChemistryCyan.copy(.25f), RoundedCornerShape(18.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, color = ChemistryCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun ChemistryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17343E), contentColor = ChemistryInk), shape = RoundedCornerShape(14.dp), modifier = Modifier.heightIn(min = 44.dp)) { Text(label, fontSize = 11.sp) }
}
