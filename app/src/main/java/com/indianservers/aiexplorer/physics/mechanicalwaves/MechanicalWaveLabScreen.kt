package com.indianservers.aiexplorer.physics.mechanicalwaves

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.physics.core.PhysicsCalculations
import com.indianservers.aiexplorer.physics.simulation.LaboratorySection
import com.indianservers.aiexplorer.physics.simulation.MeasurementValue
import com.indianservers.aiexplorer.physics.simulation.SimulationPlayback
import com.indianservers.aiexplorer.physics.ui.ChoiceChip
import com.indianservers.aiexplorer.physics.ui.LaboratoryPanel
import com.indianservers.aiexplorer.physics.ui.LiveGraph
import com.indianservers.aiexplorer.physics.ui.MeasurementPanel
import com.indianservers.aiexplorer.physics.ui.ParameterSlider
import com.indianservers.aiexplorer.physics.ui.PhysicsButton
import com.indianservers.aiexplorer.physics.ui.PhysicsColors
import com.indianservers.aiexplorer.physics.ui.SectionSelector
import com.indianservers.aiexplorer.physics.ui.SimulationTransport
import com.indianservers.aiexplorer.physics.ui.ToggleSetting
import com.indianservers.aiexplorer.physics.ui.format
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MechanicalWaveLabScreen(
    onBack: () -> Unit,
    vm: MechanicalWaveViewModel = viewModel(),
) {
    val state = vm.state
    var fullScreen by remember { mutableStateOf(false) }
    BackHandler { if (fullScreen) fullScreen = false else onBack() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) vm.pause() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.playback, state.reducedMotion) {
        if (state.playback != SimulationPlayback.Playing) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            vm.advanceFrame((now - previous) / 1_000_000_000.0)
            previous = now
            if (state.reducedMotion) kotlinx.coroutines.delay(32)
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF10243A), PhysicsColors.Background), radius = 1400f),
        ).padding(8.dp),
    ) {
        val wide = maxWidth >= 840.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabHeader(onBack = onBack, fullScreen = fullScreen, onFullScreen = { fullScreen = !fullScreen })
            if (!fullScreen) SectionSelector(state.selectedSection, vm::setSection)
            if (wide && !fullScreen) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1.55f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WaveCanvas(state, vm, Modifier.fillMaxWidth().height(390.dp))
                        MeasurementPanel(measurements(state))
                        EducationalSection(state, vm)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SimulationTransport(state.playback, state.parameters.simulationSpeed, vm::togglePlayback, vm::stepForward, vm::reset, vm::setSimulationSpeed, vm::createPulse)
                        ModeControls(state, vm)
                        ParameterControls(state, vm)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WaveCanvas(state, vm, Modifier.fillMaxWidth().height(if (fullScreen) 560.dp else 320.dp))
                    SimulationTransport(state.playback, state.parameters.simulationSpeed, vm::togglePlayback, vm::stepForward, vm::reset, vm::setSimulationSpeed, vm::createPulse)
                    if (!fullScreen) {
                        MeasurementPanel(measurements(state))
                        ModeControls(state, vm)
                        if (state.selectedSection == LaboratorySection.Explore) ParameterControls(state, vm)
                        EducationalSection(state, vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabHeader(onBack: () -> Unit, fullScreen: Boolean, onFullScreen: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PhysicsButton("Back", onBack)
            Column {
                Text("Mechanical Waves Laboratory", color = PhysicsColors.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Manipulate the medium · observe particles · measure the wave", color = PhysicsColors.Muted, fontSize = 10.sp)
            }
        }
        PhysicsButton(if (fullScreen) "Exit full screen" else "Full screen", onFullScreen)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ModeControls(state: MechanicalWaveUiState, vm: MechanicalWaveViewModel) {
    LaboratoryPanel(title = "Wave model") {
        Text("Particle motion", color = PhysicsColors.Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            WaveMotion.entries.forEach { ChoiceChip(it.label, state.motion == it) { vm.setMotion(it) } }
        }
        Text("Source", color = PhysicsColors.Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            WaveDrive.entries.forEach { ChoiceChip(it.label, state.drive == it) { vm.setDrive(it) } }
        }
        Text("Representation", color = PhysicsColors.Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            WaveMedium.entries.forEach { ChoiceChip(it.label, state.medium == it) { vm.setMedium(it) } }
        }
        Text("Boundary", color = PhysicsColors.Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            EndCondition.entries.forEach { ChoiceChip(it.label, state.endCondition == it) { vm.setEndCondition(it) } }
        }
        Text("Presets", color = PhysicsColors.Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Gentle string", "Fixed reflection", "Compression", "Water").forEach { preset -> PhysicsButton(preset, onClick = { vm.applyPreset(preset) }) }
        }
        ToggleSetting("Show particles", state.showParticles, vm::setParticles)
        ToggleSetting("Particle trails", state.showTrails, vm::setTrails)
        ToggleSetting("High contrast", state.highContrast, vm::setHighContrast)
        ToggleSetting("Reduced motion", state.reducedMotion, vm::setReducedMotion)
    }
}

@Composable
private fun ParameterControls(state: MechanicalWaveUiState, vm: MechanicalWaveViewModel) {
    val p = state.parameters
    LaboratoryPanel(title = "Parameters") {
        ParameterSlider("Amplitude A", p.amplitudeMetres, 0.05..1.0, "m", vm::setAmplitude)
        ParameterSlider("Frequency f", p.frequencyHz, 0.25..4.0, "Hz", vm::setFrequency)
        ParameterSlider("Wavelength λ", p.wavelengthMetres, 0.4..5.0, "m", vm::setWavelength)
        ParameterSlider("Wave speed v", p.speedMetresPerSecond, 0.1..20.0, "m/s", vm::setSpeed)
        ParameterSlider("Phase", p.phaseRadians, 0.0..(2.0 * PI), "rad", vm::setPhase)
        ParameterSlider("Damping", p.dampingPerMetre, 0.0..0.6, "m⁻¹", vm::setDamping)
        ParameterSlider("Tension", p.tensionNewtons, 1.0..60.0, "N", vm::setTension)
        ParameterSlider("Linear density", p.linearDensityKgPerMetre, 0.25..5.0, "kg/m", vm::setLinearDensity)
        ParameterSlider("Pulse width", p.pulseWidthMetres, 0.15..1.5, "m", vm::setPulseWidth)
    }
}

@Composable
private fun EducationalSection(state: MechanicalWaveUiState, vm: MechanicalWaveViewModel) {
    val p = state.parameters
    when (state.selectedSection) {
        LaboratorySection.Explore -> LaboratoryPanel(title = "Explore") {
            Text("Drag the cyan source at the left edge. Tap the field to launch a pulse. Drag the numbered markers to compare two points.", color = PhysicsColors.Ink, fontSize = 12.sp)
            Text("Particles move around equilibrium; the disturbance and energy travel through the medium.", color = PhysicsColors.Muted, fontSize = 11.sp)
        }
        LaboratorySection.Experiment -> LaboratoryPanel(title = "Guided experiment · Measure wavelength") {
            InfoLine("Objective", "Verify v = fλ using two crest markers.")
            InfoLine("Apparatus", "Virtual string, oscillator, markers and live clock.")
            InfoLine("Steps", "Pause near a crest. Drag marker 1 to that crest and marker 2 to the next crest. Compare Δx with λ, then change f.")
            InfoLine("Observation", "Marker spacing is ${format(abs(state.markerTwoMetres - state.markerOneMetres))} m; calculated λ is ${format(p.wavelengthMetres)} m.")
            InfoLine("Result", if (abs(abs(state.markerTwoMetres - state.markerOneMetres) - p.wavelengthMetres) < .12) "Your markers match one wavelength." else "Move the markers until their spacing matches one wavelength.")
            PhysicsButton("Reset experiment", vm::reset)
        }
        LaboratorySection.Formula -> LaboratoryPanel(title = "Formula and units") {
            FormulaCard("v = fλ", "${format(p.speedMetresPerSecond)} m/s = ${format(p.frequencyHz)} Hz × ${format(p.wavelengthMetres)} m")
            FormulaCard("T = 1/f", "${format(p.periodSeconds)} s = 1 / ${format(p.frequencyHz)} Hz")
            FormulaCard("vstring = √(Fₜ/μ)", "${format(PhysicsCalculations.stringWaveSpeed(p.tensionNewtons, p.linearDensityKgPerMetre))} m/s for ${format(p.tensionNewtons)} N and ${format(p.linearDensityKgPerMetre)} kg/m")
            Text("Symbols: v speed (m/s), f frequency (Hz), λ wavelength (m), T period (s), Fₜ tension (N), μ linear density (kg/m).", color = PhysicsColors.Muted, fontSize = 11.sp)
            Text("Rearranged: f = v/λ · λ = v/f", color = PhysicsColors.Ink, fontSize = 12.sp)
            Text("Unit check: Hz × m = s⁻¹ × m = m/s", color = PhysicsColors.Green, fontSize = 11.sp)
        }
        LaboratorySection.Graph -> LaboratoryPanel(title = null) {
            ToggleSetting("Pause graph updates", state.graphPaused, vm::setGraphPaused)
            LiveGraph(state.graphSamples, listOf("displacement", "velocity"), "m / m·s⁻¹")
        }
        LaboratorySection.Explanation -> LaboratoryPanel(title = "What is happening?") {
            Text(if (state.motion == WaveMotion.Transverse) "Each particle moves across the direction the wave travels. The curve is a snapshot of many particles, not a piece of material moving forward." else "Particles oscillate parallel to propagation. Crowded regions are compressions; spread-out regions are rarefactions.", color = PhysicsColors.Ink, fontSize = 12.sp)
            Text(if (state.endCondition == EndCondition.Fixed) "At the fixed end the reflected displacement is inverted because the endpoint cannot move." else if (state.endCondition == EndCondition.Free) "At the free end the reflected displacement is not inverted because the endpoint can move." else "With an open model boundary, the wave leaves the visible region without a reflected component." , color = PhysicsColors.Muted, fontSize = 11.sp)
            Text("This is an ideal one-dimensional educational model. Damping is exponential and the water view is illustrative, not a fluid-dynamics solution.", color = PhysicsColors.Amber, fontSize = 11.sp)
        }
        LaboratorySection.Application -> LaboratoryPanel(title = "Real-world application") {
            InfoLine("Strings", "Tension and mass per unit length set wave speed in musical instruments and cables.")
            InfoLine("Earth science", "Seismic P waves are longitudinal; S waves are transverse.")
            InfoLine("Water", "Surface waves combine vertical and horizontal motion; this view emphasizes vertical displacement.")
        }
        LaboratorySection.Challenge -> LaboratoryPanel(title = "Challenge · Match the target") {
            Text("Set wavelength to ${format(state.challengeTargetWavelength)} m while keeping frequency above 1 Hz.", color = PhysicsColors.Ink, fontSize = 12.sp)
            val error = abs(p.wavelengthMetres - state.challengeTargetWavelength)
            Text(when { error < .03 && p.frequencyHz > 1.0 -> "Success — target matched."; error < .2 -> "Very close. Use a smaller wavelength adjustment."; p.wavelengthMetres < state.challengeTargetWavelength -> "Hint: increase wavelength."; else -> "Hint: decrease wavelength." }, color = if (error < .03 && p.frequencyHz > 1.0) PhysicsColors.Green else PhysicsColors.Amber, fontWeight = FontWeight.Bold)
        }
        LaboratorySection.Quiz -> LaboratoryPanel(title = "State-aware quiz") {
            Text("At the current f = ${format(p.frequencyHz)} Hz and λ = ${format(p.wavelengthMetres)} m, what happens to speed if frequency doubles while wavelength stays fixed?", color = PhysicsColors.Ink, fontSize = 12.sp)
            listOf("It halves", "It doubles", "It is unchanged").forEachIndexed { index, answer -> ChoiceChip(answer, state.quizChoice == index) { vm.answerQuiz(index) } }
            state.quizChoice?.let { Text(if (it == 1) "Correct. Since v = fλ, doubling f doubles v when λ is held fixed." else "Try again: apply v = fλ with λ unchanged.", color = if (it == 1) PhysicsColors.Green else PhysicsColors.Amber, fontSize = 11.sp) }
        }
    }
}

private enum class CanvasDragTarget { Source, MarkerOne, MarkerTwo }

@Composable
private fun WaveCanvas(state: MechanicalWaveUiState, vm: MechanicalWaveViewModel, modifier: Modifier) {
    var dragTarget by remember { mutableStateOf<CanvasDragTarget?>(null) }
    val p = state.parameters
    val waveColor = if (state.highContrast) Color.White else PhysicsColors.Cyan
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(PhysicsColors.Background).border(1.dp, waveColor.copy(.5f), RoundedCornerShape(20.dp)),
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(state.markerOneMetres, state.markerTwoMetres) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val usableWidth = size.width * .88f
                            val x1 = size.width * .06f + usableWidth * (state.markerOneMetres / MechanicalWaveEngine.MEDIUM_LENGTH_METRES).toFloat()
                            val x2 = size.width * .06f + usableWidth * (state.markerTwoMetres / MechanicalWaveEngine.MEDIUM_LENGTH_METRES).toFloat()
                            dragTarget = when {
                                abs(start.x - x1) < 40f -> CanvasDragTarget.MarkerOne
                                abs(start.x - x2) < 40f -> CanvasDragTarget.MarkerTwo
                                start.x < size.width * .16f -> CanvasDragTarget.Source
                                else -> null
                            }
                        },
                        onDragEnd = { vm.releaseManualSource(); dragTarget = null },
                        onDragCancel = { vm.releaseManualSource(); dragTarget = null },
                    ) { change, _ ->
                        change.consume()
                        when (dragTarget) {
                            CanvasDragTarget.Source -> vm.setManualSource(((size.height / 2f - change.position.y) / (size.height * .34f)).toDouble())
                            CanvasDragTarget.MarkerOne -> vm.setMarkerOne((change.position.x - size.width * .06f) / (size.width * .88f) * MechanicalWaveEngine.MEDIUM_LENGTH_METRES)
                            CanvasDragTarget.MarkerTwo -> vm.setMarkerTwo((change.position.x - size.width * .06f) / (size.width * .88f) * MechanicalWaveEngine.MEDIUM_LENGTH_METRES)
                            null -> Unit
                        }
                    }
                }
                .pointerInput(Unit) { detectTapGestures { vm.createPulse() } }
                .semantics { contentDescription = "Interactive ${state.motion.label.lowercase()} wave. Drag the left source, drag measurement markers, or tap to create a pulse." },
        ) {
            val left = size.width * .06f
            val right = size.width * .94f
            val centre = size.height * .52f
            val xScale = (right - left) / MechanicalWaveEngine.MEDIUM_LENGTH_METRES.toFloat()
            val yScale = size.height * .30f
            drawLine(PhysicsColors.Muted.copy(.45f), Offset(left, centre), Offset(right, centre), 2f)
            repeat(7) { tick ->
                val x = left + tick * (right - left) / 6f
                drawLine(PhysicsColors.Muted.copy(.35f), Offset(x, centre - 7f), Offset(x, centre + 7f), 2f)
            }
            if (state.showTrails && !state.reducedMotion) {
                repeat(3) { trailIndex ->
                    drawWavePath(state, state.timeSeconds - (trailIndex + 1) * .045, left, centre, xScale, yScale, waveColor.copy(alpha = .14f + trailIndex * .07f), 2f)
                }
            }
            if (state.medium == WaveMedium.Water && state.motion == WaveMotion.Transverse) {
                repeat(4) { band ->
                    drawWavePath(state, state.timeSeconds - band * .035, left, centre + band * 10f, xScale, yScale, waveColor.copy(alpha = .28f), 2.5f)
                }
            }
            drawWavePath(state, state.timeSeconds, left, centre, xScale, yScale, waveColor, if (state.highContrast) 6f else 4f)
            if (state.showParticles) drawParticles(state, left, centre, xScale, yScale, waveColor)
            drawMarkers(state, left, centre, xScale, yScale)
            val sourceY = centre - state.manualSourceMetres.toFloat() * yScale
            drawCircle(PhysicsColors.Cyan.copy(.20f), 22f, Offset(left, sourceY))
            drawCircle(PhysicsColors.Cyan, 11f, Offset(left, sourceY))
            if (state.endCondition != EndCondition.Open) {
                drawLine(if (state.endCondition == EndCondition.Fixed) PhysicsColors.Red else PhysicsColors.Green, Offset(right, centre - yScale), Offset(right, centre + yScale), 5f)
            }
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 27f }
                drawText("drag source", left + 8f, 28f, paint)
                drawText("tap: pulse", right - 125f, 28f, paint)
                if (state.motion == WaveMotion.Longitudinal) {
                    paint.color = android.graphics.Color.rgb(255, 199, 102)
                    drawText("compression", size.width * .38f, size.height - 14f, paint)
                    drawText("rarefaction", size.width * .68f, size.height - 14f, paint)
                }
            }
        }
        Text("${if (state.playback == SimulationPlayback.Playing) "RUNNING" else "PAUSED"} · t ${format(state.timeSeconds)} s", color = if (state.playback == SimulationPlayback.Playing) PhysicsColors.Green else PhysicsColors.Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWavePath(
    state: MechanicalWaveUiState,
    time: Double,
    left: Float,
    centre: Float,
    xScale: Float,
    yScale: Float,
    color: Color,
    stroke: Float,
) {
    if (state.motion == WaveMotion.Longitudinal) return
    val path = Path()
    val count = 160
    repeat(count) { index ->
        val xMetres = MechanicalWaveEngine.MEDIUM_LENGTH_METRES * index / (count - 1.0)
        val displacement = MechanicalWaveEngine.displacement(xMetres, time, state.parameters, state.drive, state.endCondition, state.manualSourceMetres, state.pulseStartedAtSeconds)
        val x = left + xMetres.toFloat() * xScale
        val y = centre - displacement.toFloat() * yScale
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawParticles(
    state: MechanicalWaveUiState,
    left: Float,
    centre: Float,
    xScale: Float,
    yScale: Float,
    color: Color,
) {
    val count = if (state.reducedMotion) 25 else 45
    repeat(count) { index ->
        val equilibrium = MechanicalWaveEngine.MEDIUM_LENGTH_METRES * index / (count - 1.0)
        val displacement = MechanicalWaveEngine.displacement(equilibrium, state.timeSeconds, state.parameters, state.drive, state.endCondition, state.manualSourceMetres, state.pulseStartedAtSeconds)
        val point = if (state.motion == WaveMotion.Transverse) {
            Offset(left + equilibrium.toFloat() * xScale, centre - displacement.toFloat() * yScale)
        } else {
            Offset(left + equilibrium.toFloat() * xScale + displacement.toFloat() * yScale * .7f, centre)
        }
        drawCircle(color.copy(alpha = .22f), if (state.motion == WaveMotion.Longitudinal) 9f else 7f, point)
        drawCircle(color, if (state.motion == WaveMotion.Longitudinal) 4.5f else 3.5f, point)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarkers(
    state: MechanicalWaveUiState,
    left: Float,
    centre: Float,
    xScale: Float,
    yScale: Float,
) {
    listOf(state.markerOneMetres to PhysicsColors.Violet, state.markerTwoMetres to PhysicsColors.Amber).forEachIndexed { index, (position, color) ->
        val x = left + position.toFloat() * xScale
        drawLine(color.copy(.75f), Offset(x, centre - yScale * 1.05f), Offset(x, centre + yScale * 1.05f), 3f)
        drawCircle(color, 12f, Offset(x, centre - yScale * 1.05f))
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE; textSize = 23f; textAlign = Paint.Align.CENTER }
            drawText("${index + 1}", x, centre - yScale * 1.05f + 8f, paint)
        }
    }
}

private fun measurements(state: MechanicalWaveUiState): List<MeasurementValue> {
    val p = state.parameters
    val markerDistance = abs(state.markerTwoMetres - state.markerOneMetres)
    val phase = PhysicsCalculations.phaseDifferenceRadians(markerDistance, p.wavelengthMetres)
    val energy = PhysicsCalculations.relativeWaveEnergy(p.amplitudeMetres, p.frequencyHz)
    return listOf(
        MeasurementValue("Frequency f", format(p.frequencyHz), "Hz"),
        MeasurementValue("Period T", format(p.periodSeconds), "s"),
        MeasurementValue("Wavelength λ", format(p.wavelengthMetres), "m"),
        MeasurementValue("Amplitude A", format(p.amplitudeMetres), "m"),
        MeasurementValue("Wave speed v", format(p.speedMetresPerSecond), "m/s"),
        MeasurementValue("Marker distance Δx", format(markerDistance), "m"),
        MeasurementValue("Phase difference", format(phase), "rad"),
        MeasurementValue("Relative energy", format(energy), "A²f²"),
    )
}

@Composable
private fun FormulaCard(formula: String, current: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PhysicsColors.Background.copy(.65f)).padding(11.dp)) {
        Text(formula, color = PhysicsColors.Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(current, color = PhysicsColors.Ink, fontSize = 12.sp)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = PhysicsColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
        Text(value, color = PhysicsColors.Ink, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}
