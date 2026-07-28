package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.physics.formulas.navigation.PhysicsFormulaFeatureRoot
import com.indianservers.aiexplorer.physics.learning.PhysicsConnectedLearningFeature
import com.indianservers.aiexplorer.physics.mechanicalwaves.MechanicalWaveLabScreen

private data class PhysicsModuleOption(val title: String, val description: String, val icon: String)

private val PhysicsModules = listOf(
    PhysicsModuleOption("Measurement", "Units, dimensions, errors and significant figures", "SI"),
    PhysicsModuleOption("Mechanics", "Particles, rigid bodies, equilibrium and dynamics", "M"),
    PhysicsModuleOption("Motion and Kinematics", "Position, velocity, acceleration and projectiles", "v"),
    PhysicsModuleOption("Force and Newton's Laws", "Forces, free-body diagrams and Newton's three laws", "F"),
    PhysicsModuleOption("Work, Energy and Power", "Energy transfer, conservation, work and efficiency", "W"),
    PhysicsModuleOption("Gravitation", "Orbits, gravitational fields, potential and satellites", "g"),
    PhysicsModuleOption("Oscillations", "Simple harmonic motion, damping and resonance", "SHM"),
    PhysicsModuleOption("Waves and Sound", "Wave motion, interference, standing waves and acoustics", "lambda"),
    PhysicsModuleOption("Optics", "Reflection, refraction, lenses, diffraction and instruments", "Ray"),
    PhysicsModuleOption("Electricity", "Charge, fields, potential, current and circuits", "E"),
    PhysicsModuleOption("Magnetism", "Magnetic fields, forces, induction and electromagnetism", "B"),
    PhysicsModuleOption("Electronics", "Semiconductors, diodes, transistors and digital logic", "IC"),
    PhysicsModuleOption("Thermodynamics", "Heat, temperature, gases and thermodynamic laws", "T"),
    PhysicsModuleOption("Fluid Mechanics", "Pressure, buoyancy, continuity and fluid flow", "rho"),
    PhysicsModuleOption("Modern Physics", "Relativity, quantum physics, atoms and nuclei", "Q"),
    PhysicsModuleOption("Astronomy and Astrophysics", "Stars, galaxies, cosmology and observational physics", "Star"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhysicsHubScreen(vm: ExplorerViewModel, wide: Boolean) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PhysicsModuleOption?>(null) }
    var showMechanicalWaves by rememberSaveable { mutableStateOf(false) }
    var showFormulaLibrary by rememberSaveable { mutableStateOf(false) }
    var showConnectedJourney by rememberSaveable { mutableStateOf(false) }
    if (showConnectedJourney) {
        PhysicsConnectedLearningFeature(onExit = { showConnectedJourney = false })
        return
    }
    if (showFormulaLibrary) {
        PhysicsFormulaFeatureRoot(onExit = { showFormulaLibrary = false })
        return
    }
    if (showMechanicalWaves) {
        MechanicalWaveLabScreen(onBack = { showMechanicalWaves = false })
        return
    }
    val visible = PhysicsModules.filter { it.title.contains(query, true) || it.description.contains(query, true) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 34.dp else 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransparentIcon("F", Cyan)
                Column {
                    Text("Physics Explorer", color = Cyan, fontSize = if (wide) 28.sp else 22.sp, fontWeight = FontWeight.Bold)
                    Text("16 physics learning modules", color = Muted, fontSize = 11.sp)
                }
            }
            GlowButton("Home", icon = "H", onClick = vm::openSubjectHub)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Cyan.copy(.16f), Violet.copy(.12f)))).border(1.dp, Cyan.copy(.65f), RoundedCornerShape(18.dp)).clickable { showFormulaLibrary = true }.semantics { contentDescription = "Open Physics Formulas: equations, derivations, units, calculators and applications" }.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PHYSICS FORMULAS", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Equations, derivations, units, calculators and applications", color = Ink, fontSize = 11.sp)
                Text("25 categories - offline - separate Physics repository", color = Green, fontSize = 9.sp)
            }
            Text("OPEN  >", color = Amber, fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xCC0B1420)).border(1.dp, Violet.copy(.65f), RoundedCornerShape(18.dp)).clickable { showConnectedJourney = true }.semantics { contentDescription = "Open connected Physics journey from Motion through Work and Energy" }.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("CONNECTED LEARNING - MOTION TO ENERGY", color = Violet, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Learn > Explore > Test across prerequisites, formulas and activities", color = Ink, fontSize = 11.sp)
            }
            Text("START  >", color = Green, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Physics") },
            placeholder = { Text("Try waves, energy, electricity or astronomy") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            visible.forEach { module ->
                Column(
                    Modifier
                        .width(if (wide) 225.dp else 165.dp)
                        .heightIn(min = 130.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected == module) Violet.copy(.18f) else Color(0xCC0B1420))
                        .border(1.dp, if (selected == module) Violet else Cyan.copy(.35f), RoundedCornerShape(18.dp))
                        .clickable {
                            if (module.title == "Waves and Sound") showMechanicalWaves = true
                            else selected = module
                        }
                        .focusable()
                        .semantics { contentDescription = "Open Physics module ${module.title}" }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TransparentIcon(module.icon, if (selected == module) Violet else Cyan)
                        Text(if (selected == module) "SELECTED" else "OPEN", color = if (selected == module) Violet else Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(module.title, color = Ink, fontSize = if (wide) 16.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(module.description, color = Muted, fontSize = 10.sp, maxLines = 3)
                }
            }
        }
        selected?.let { module ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Color(0xDD10233A), Color(0xDD21183A)))).border(1.dp, Violet.copy(.55f), RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TransparentIcon(module.icon, Violet)
                    Text(module.title, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(module.description, color = Ink, fontSize = 12.sp)
                Text("Module selected. Interactive lessons, formulas, simulations, visual proofs and practice activities can be added inside this destination.", color = Muted, fontSize = 11.sp)
            }
        }
        if (visible.isEmpty()) Text("No Physics modules match this search.", color = Amber, fontWeight = FontWeight.SemiBold)
    }
}
