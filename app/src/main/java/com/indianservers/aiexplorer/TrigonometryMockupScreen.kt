package com.indianservers.aiexplorer

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import kotlin.random.Random

private val TrigBg = Color(0xFF02070B)
private val TrigPanel = Color(0xFF071119)
private val TrigPanel2 = Color(0xFF0B151D)
private val TrigStroke = Color(0xFF17313B)
private val TrigGrid = Color(0xFF0C2730)
private val TrigText = Color(0xFFF1F5F7)
private val TrigMuted = Color(0xFFB1BEC5)
private val TrigCyan = Color(0xFF25D5D5)
private val TrigGreen = Color(0xFF4BE0B2)
private val TrigAmber = Color(0xFFFFB848)
private val TrigOrange = Color(0xFFFF7C46)
private val TrigViolet = Color(0xFF9D62F4)

private enum class TrigDestination(val title: String, val subtitle: String, val tab: String, val icon: String) {
    UnitCircle("AI Maths Explorer", "Trigonometry Lab", "Unit Circle", "⊕"),
    Graphs("Trig Graphs", "Live Sync with Unit Circle", "Graphs", "⌁"),
    Transform("Transformations Lab", "Explore Trig Functions", "Transform", "⇄"),
    Identities("Identities Lab", "Visual Proofs", "Identities", "▣"),
    RealWorld("Real World", "Trigonometry in Action", "Real World", "◉"),
    AiTutor("AI Tutor", "Ask anything about Trigonometry", "AI Tutor", "◌"),
}

@Composable
internal fun TrigonometryMockupScreen(
    onBack: () -> Unit,
    onMenu: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(TrigDestination.UnitCircle) }
    var angle by rememberSaveable { mutableFloatStateOf(-135f) }
    var animating by rememberSaveable { mutableStateOf(false) }
    var useRadians by rememberSaveable { mutableStateOf(false) }
    var graphFunction by rememberSaveable { mutableStateOf("sin") }
    var graphSync by rememberSaveable { mutableStateOf(true) }
    var graphAngle by rememberSaveable { mutableFloatStateOf(-135f) }
    var showSin by rememberSaveable { mutableStateOf(true) }
    var showCos by rememberSaveable { mutableStateOf(false) }
    var showTan by rememberSaveable { mutableStateOf(false) }
    var transformFunction by rememberSaveable { mutableStateOf("sin") }
    var transformA by rememberSaveable { mutableFloatStateOf(1.5f) }
    var transformB by rememberSaveable { mutableFloatStateOf(1f) }
    var transformH by rememberSaveable { mutableFloatStateOf(0f) }
    var transformK by rememberSaveable { mutableFloatStateOf(.5f) }
    var identity by rememberSaveable { mutableIntStateOf(0) }
    var application by rememberSaveable { mutableStateOf("Ferris Wheel") }
    var appTime by rememberSaveable { mutableFloatStateOf(2.2f) }
    var appPlaying by rememberSaveable { mutableStateOf(false) }
    var tutorQuestion by rememberSaveable { mutableStateOf("Why is tan 90° undefined?") }

    LaunchedEffect(animating) {
        while (animating) {
            delay(32)
            angle += .8f
            if (angle > 180f) angle = -180f
        }
    }
    LaunchedEffect(appPlaying, destination) {
        while (appPlaying && destination == TrigDestination.RealWorld) {
            delay(32)
            appTime = (appTime + .025f).let { if (it > 6.28f) 0f else it }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(TrigBg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TrigHeader(destination, onBack, onMenu)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (destination) {
                TrigDestination.UnitCircle -> UnitCircleDestination(angle, animating, useRadians, { angle = it }, { animating = !animating }, { useRadians = it })
                TrigDestination.Graphs -> TrigGraphsDestination(
                    angle = if (graphSync) angle else graphAngle,
                    selected = graphFunction,
                    sync = graphSync,
                    overlays = mapOf("sin" to showSin, "cos" to showCos, "tan" to showTan),
                    onSelected = { graphFunction = it },
                    onSync = {
                        graphSync = !graphSync
                        if (!graphSync) graphAngle = angle
                    },
                    onAngle = {
                        graphAngle = it
                        if (graphSync) angle = normalizeDegrees(it)
                    },
                    onOverlay = { function ->
                        when (function) {
                            "sin" -> showSin = !showSin
                            "cos" -> showCos = !showCos
                            else -> showTan = !showTan
                        }
                    },
                )
                TrigDestination.Transform -> TransformDestination(transformFunction, transformA, transformB, transformH, transformK, { transformFunction = it }, { transformA = it }, { transformB = it }, { transformH = it }, { transformK = it })
                TrigDestination.Identities -> IdentitiesDestination(angle, identity, { identity = it }, { angle = it })
                TrigDestination.RealWorld -> RealWorldDestination(
                    application,
                    appTime,
                    appPlaying,
                    {
                        application = it
                        appTime = 0f
                        appPlaying = false
                    },
                    { appTime = it },
                    { appPlaying = !appPlaying },
                    {
                        appTime = 0f
                        appPlaying = false
                    },
                )
                TrigDestination.AiTutor -> AiTutorDestination(tutorQuestion) { tutorQuestion = it }
            }
        }
        TrigBottomNavigation(destination) {
            animating = false
            if (it != TrigDestination.RealWorld) appPlaying = false
            destination = it
        }
    }
}

@Composable
private fun TrigHeader(destination: TrigDestination, onBack: () -> Unit, onMenu: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .background(TrigPanel, RoundedCornerShape(20.dp)),
    ) {
        MiniIconButton("‹", onBack, Modifier.align(Alignment.CenterStart))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(destination.title, color = TrigText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(destination.subtitle, color = TrigMuted, fontSize = 11.sp)
        }
        MiniIconButton(if (destination == TrigDestination.UnitCircle) "≡" else "⌾", onMenu, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun TrigBottomNavigation(selected: TrigDestination, onSelect: (TrigDestination) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(TrigPanel)
            .border(1.dp, TrigStroke)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrigDestination.entries.forEach { item ->
            val active = item == selected
            Column(
                Modifier
                    .width(57.dp)
                    .clickable { onSelect(item) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(item.icon, color = if (active) TrigCyan else TrigMuted, fontSize = 22.sp)
                Text(item.tab, color = if (active) TrigCyan else TrigMuted, fontSize = 8.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

@Composable
private fun UnitCircleDestination(angle: Float, animating: Boolean, useRadians: Boolean, onAngle: (Float) -> Unit, onAnimate: () -> Unit, onUnit: (Boolean) -> Unit) {
    val rad = Math.toRadians(angle.toDouble())
    val s = sin(rad)
    val c = cos(rad)
    val t = if (abs(c) < .00001) Double.NaN else s / c
    val q = quadrant(angle)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricCard("Angle", if (useRadians) radiansLabel(angle) else "${angle.roundToInt()}°", TrigCyan, Modifier.weight(1f))
            MetricCard("Radians", radiansLabel(angle), TrigGreen, Modifier.weight(1f))
            MetricCard("Quadrant", q, TrigOrange, Modifier.weight(1f))
            ActionCard(if (animating) "Ⅱ  Pause" else "▶  Animate", onAnimate, Modifier.weight(1.15f))
        }
        Row(Modifier.fillMaxWidth().height(38.dp).panel(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TabButton("Degrees", !useRadians, TrigCyan, { onUnit(false) }, Modifier.weight(1f))
            TabButton("Radians", useRadians, TrigGreen, { onUnit(true) }, Modifier.weight(1f))
            Text("Reference: ${angleDisplay(referenceAngle(angle), useRadians)}", color = TrigAmber, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 10.dp))
        }
        Row(Modifier.fillMaxWidth().height(350.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitCircleCanvas(angle, onAngle, Modifier.weight(1f).fillMaxHeight().panel())
            Column(Modifier.width(112.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ValuesCard("Function Values", listOf("sin θ" to safeTrig(s), "cos θ" to safeTrig(c), "tan θ" to safeRatio(s, c), "csc θ" to safeRatio(1.0, s), "sec θ" to safeRatio(1.0, c), "cot θ" to safeRatio(c, s)), TrigCyan, Modifier.weight(1f))
                ValuesCard("Exact Values", listOf("sin θ" to exactValue(angle, true), "cos θ" to exactValue(angle, false), "tan θ" to safeRatio(s, c)), TrigGreen, Modifier.weight(.72f))
            }
        }
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValuesCard("Quadrant $q", listOf("sin" to signText(s), "cos" to signText(c), "tan" to signText(t)), TrigAmber, Modifier.weight(.9f), horizontal = true)
            RightTriangleCard(s, c, Modifier.weight(1.1f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (useRadians) "−π" else "-180°", color = TrigMuted, fontSize = 10.sp)
            Slider(angle, onAngle, valueRange = -180f..180f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = TrigText, activeTrackColor = TrigCyan, inactiveTrackColor = TrigStroke))
            Text(if (useRadians) "π" else "180°", color = TrigMuted, fontSize = 10.sp)
        }
        Text("Quick Angles", color = TrigMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(0, 30, 45, 60, 90, 120, 135, 180, 270, 360).forEach { value ->
                val normalized = if (value > 180) value - 360 else value
                SmallPill(if (useRadians) radiansLabel(normalized.toFloat()) else "$value°", normalized == angle.roundToInt()) { onAngle(normalized.toFloat()) }
            }
        }
    }
}

@Composable
private fun TrigGraphsDestination(
    angle: Float,
    selected: String,
    sync: Boolean,
    overlays: Map<String, Boolean>,
    onSelected: (String) -> Unit,
    onSync: () -> Unit,
    onAngle: (Float) -> Unit,
    onOverlay: (String) -> Unit,
) {
    val r = Math.toRadians(angle.toDouble())
    val value = when (selected) { "cos" -> cos(r); "tan" -> tan(r); else -> sin(r) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MetricCard("Angle", "${angle.roundToInt()}°", TrigCyan, Modifier.width(105.dp))
            ActionCard("⟳  Sync ${if (sync) "ON" else "OFF"}", onSync, Modifier.width(130.dp))
        }
        Row(Modifier.fillMaxWidth().height(48.dp).panel(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("sin", "cos", "tan").forEach { f ->
                TabButton("$f(x)", selected == f, colorForFunction(f), onClick = { onSelected(f) })
            }
        }
        FunctionGraphCanvas(selected, overlays.filterValues { it }.keys, angle, onAngle, Modifier.fillMaxWidth().weight(1f).panel())
        Text("●  x = ${angle.roundToInt()}° = ${radiansLabel(angle)}     $selected(x) = ${if (selected == "tan") safeRatio(sin(r), cos(r)) else safeTrig(value)}", color = TrigText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.panel().padding(10.dp))
        Text("Overlay functions", color = TrigMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(46.dp).panel(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("sin", "cos", "tan").forEach { function ->
                TabButton("${if (overlays[function] == true) "●" else "○"} $function(x)", overlays[function] == true, colorForFunction(function), { onOverlay(function) }, Modifier.weight(1f))
            }
        }
        Text("Drag inside the graph to inspect any angle. Dashed lines mark tan(x) asymptotes.", color = TrigMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TransformDestination(function: String, a: Float, b: Float, h: Float, k: Float, onFunction: (String) -> Unit, onA: (Float) -> Unit, onB: (Float) -> Unit, onH: (Float) -> Unit, onK: (Float) -> Unit) {
    val functionName = when (function) { "cos" -> "cos"; "tan" -> "tan"; else -> "sin" }
    fun applyPreset(values: List<Float>) { onA(values[0]); onB(values[1]); onH(values[2]); onK(values[3]) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("y = A $functionName( B (x - h) ) + k", color = TrigText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(38.dp).panel()) {
            listOf("sin", "cos", "tan").forEach { item -> TabButton(item, function == item, colorForFunction(item), { onFunction(item) }, Modifier.weight(1f)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ParameterCard("A", a, -3f..3f, TrigCyan, onA, Modifier.weight(1f))
            ParameterCard("B", b, .25f..3f, TrigAmber, onB, Modifier.weight(1f))
            ParameterCard("h", h, -3.14f..3.14f, TrigViolet, onH, Modifier.weight(1f))
            ParameterCard("k", k, -2f..2f, TrigGreen, onK, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().height(38.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionCard("Reset", { applyPreset(listOf(1f, 1f, 0f, 0f)) }, Modifier.weight(1f))
            ActionCard("2× amplitude", { applyPreset(listOf(2f, 1f, 0f, 0f)) }, Modifier.weight(1f))
            ActionCard("Shift π/2", { applyPreset(listOf(1f, 1f, (PI / 2).toFloat(), 0f)) }, Modifier.weight(1f))
            ActionCard("Random", { applyPreset(listOf(Random.nextInt(-3, 4).toFloat().let { if (it == 0f) 1f else it }, Random.nextInt(1, 4).toFloat(), Random.nextInt(-3, 4) * .5f, Random.nextInt(-2, 3) * .5f)) }, Modifier.weight(1f))
        }
        TransformGraphCanvas(function, a, b, h, k, Modifier.fillMaxWidth().weight(1f).panel())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ResultCard("Amplitude", if (function == "tan") "not bounded" else "|A| = ${oneDecimal(abs(a))}", Modifier.weight(1f))
            ResultCard("Period", if (function == "tan") "π/B = ${oneDecimal((PI / abs(b)).toFloat())}" else "2π/B = ${oneDecimal((2 * PI / abs(b)).toFloat())}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ResultCard("Phase Shift", "h = ${oneDecimal(h)}", Modifier.weight(1f))
            ResultCard("Vertical Shift", "k = ${oneDecimal(k)}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun IdentitiesDestination(angle: Float, identity: Int, onIdentity: (Int) -> Unit, onAngle: (Float) -> Unit) {
    val identities = identityProofs()
    val proof = identities[identity.coerceIn(0, identities.lastIndex)]
    var step by rememberSaveable(identity) { mutableIntStateOf(0) }
    var playing by rememberSaveable(identity) { mutableStateOf(false) }
    LaunchedEffect(playing, identity) {
        while (playing) {
            delay(900)
            if (step < proof.steps.lastIndex) step++ else playing = false
        }
    }
    val check = identityCheck(identity, angle)
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(46.dp).panel().horizontalScroll(rememberScrollState())) {
            identities.forEachIndexed { index, item -> TabButton(item.shortName, identity == index, item.color, { onIdentity(index) }) }
        }
        IdentityCanvas(angle, identity, step, Modifier.fillMaxWidth().weight(1f).panel())
        Text(
            proof.equation,
            color = proof.color,
            fontSize = if (proof.equation.length > 30) 16.sp else 22.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Step ${step + 1}/${proof.steps.size}: ${proof.steps[step]}", color = TrigMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().panel().padding(8.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ActionCard("‹ Previous", { step = (step - 1).coerceAtLeast(0); playing = false }, Modifier.weight(1f))
            ActionCard(if (playing) "Ⅱ Pause" else "▶ Play", { playing = !playing }, Modifier.weight(1f))
            ActionCard("Next ›", { step = (step + 1).coerceAtMost(proof.steps.lastIndex); playing = false }, Modifier.weight(1f))
        }
        Text("✓  LHS ${check.first}   =   RHS ${check.second}", color = TrigGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.panel().padding(horizontal = 14.dp, vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("−180°", color = TrigMuted, fontSize = 10.sp)
            Slider(angle, onAngle, valueRange = -180f..180f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = proof.color, activeTrackColor = proof.color, inactiveTrackColor = TrigStroke))
            Text("180°", color = TrigMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RealWorldDestination(
    application: String,
    time: Float,
    playing: Boolean,
    onApplication: (String) -> Unit,
    onTime: (Float) -> Unit,
    onPlay: () -> Unit,
    onReset: () -> Unit,
) {
    val apps = listOf("Ferris Wheel", "Pendulum", "Sound Wave", "Satellite Orbit")
    var ferrisRadius by rememberSaveable { mutableFloatStateOf(20f) }
    var ferrisCenter by rememberSaveable { mutableFloatStateOf(25f) }
    var pendulumLength by rememberSaveable { mutableFloatStateOf(2f) }
    var pendulumRelease by rememberSaveable { mutableFloatStateOf(35f) }
    var soundAmplitude by rememberSaveable { mutableFloatStateOf(1f) }
    var soundFrequency by rememberSaveable { mutableFloatStateOf(2f) }
    var orbitEccentricity by rememberSaveable { mutableFloatStateOf(.35f) }
    var orbitSpeed by rememberSaveable { mutableFloatStateOf(1f) }
    val height = ferrisRadius * sin(time.toDouble()) + ferrisCenter
    val pendulumOmega = kotlin.math.sqrt(9.81 / pendulumLength)
    val pendulumAngle = pendulumRelease * cos(pendulumOmega * time)
    val soundDisplacement = soundAmplitude * sin(2 * PI * soundFrequency * time)
    val orbitRadius = 7000f * (1f - orbitEccentricity * orbitEccentricity) / (1f + orbitEccentricity * cos(time * orbitSpeed))
    val equationTitle: String
    val equation: String
    val liveValues: List<Pair<String, String>>
    when (application) {
        "Pendulum" -> {
            equationTitle = "Angular Motion"
            equation = "θ(t) = θ₀ cos(√g/L · t)"
            liveValues = listOf("Angle" to "${oneDecimal(pendulumAngle.toFloat())}°", "Period" to "${oneDecimal((2 * PI / pendulumOmega).toFloat())} s", "Time" to "${oneDecimal(time)} s")
        }
        "Sound Wave" -> {
            equationTitle = "Wave Equation"
            equation = "y = A sin(2πft − kx)"
            liveValues = listOf("Displacement" to oneDecimal(soundDisplacement.toFloat()), "Wavelength" to "${oneDecimal(343f / soundFrequency)} m", "Frequency" to "${oneDecimal(soundFrequency)} Hz")
        }
        "Satellite Orbit" -> {
            equationTitle = "Polar Orbit"
            equation = "r = p / (1 + e cos θ)"
            liveValues = listOf("Radius" to "${orbitRadius.roundToInt()} km", "Angle" to "${oneDecimal(Math.toDegrees((time * orbitSpeed).toDouble()).toFloat() % 360f)}°", "Speed" to "${oneDecimal(orbitSpeed)}×")
        }
        else -> {
            equationTitle = "Height Equation"
            equation = "h(t) = ${oneDecimal(ferrisRadius)} sin(t) + ${oneDecimal(ferrisCenter)}"
            liveValues = listOf("Height" to "${oneDecimal(height.toFloat())} m", "Time" to "${oneDecimal(time)} s", "Period" to "6.3 s")
        }
    }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp).panel().horizontalScroll(rememberScrollState())) {
            apps.forEach { item ->
                TabButton(item, application == item, TrigCyan, onClick = { onApplication(item) })
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).panel()) {
            when (application) {
                "Ferris Wheel" -> FerrisWheelCanvas(time, ferrisRadius, ferrisCenter, Modifier.fillMaxSize())
                "Pendulum" -> PendulumCanvas(time, pendulumLength, pendulumRelease, Modifier.fillMaxSize())
                "Sound Wave" -> SoundWaveCanvas(time, soundAmplitude, soundFrequency, Modifier.fillMaxSize())
                else -> SatelliteOrbitCanvas(time, orbitEccentricity, orbitSpeed, Modifier.fillMaxSize())
            }
            Column(Modifier.align(Alignment.TopEnd).padding(10.dp).width(150.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ValuesCard(equationTitle, listOf("model" to equation), TrigGreen, Modifier.fillMaxWidth())
                ValuesCard("Live Values", liveValues, TrigMuted, Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            when (application) {
                "Ferris Wheel" -> {
                    SimulationParameterCard("Radius", ferrisRadius, 8f..35f, "m", TrigCyan, { ferrisRadius = it }, Modifier.weight(1f))
                    SimulationParameterCard("Center", ferrisCenter, 12f..45f, "m", TrigGreen, { ferrisCenter = it }, Modifier.weight(1f))
                }
                "Pendulum" -> {
                    SimulationParameterCard("Length", pendulumLength, .5f..5f, "m", TrigCyan, { pendulumLength = it }, Modifier.weight(1f))
                    SimulationParameterCard("Release", pendulumRelease, 5f..70f, "°", TrigAmber, { pendulumRelease = it }, Modifier.weight(1f))
                }
                "Sound Wave" -> {
                    SimulationParameterCard("Amplitude", soundAmplitude, .2f..2f, "", TrigCyan, { soundAmplitude = it }, Modifier.weight(1f))
                    SimulationParameterCard("Frequency", soundFrequency, .5f..6f, "Hz", TrigViolet, { soundFrequency = it }, Modifier.weight(1f))
                }
                else -> {
                    SimulationParameterCard("Eccentricity", orbitEccentricity, 0f..0.75f, "", TrigAmber, { orbitEccentricity = it }, Modifier.weight(1f))
                    SimulationParameterCard("Orbit speed", orbitSpeed, .25f..3f, "×", TrigCyan, { orbitSpeed = it }, Modifier.weight(1f))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(62.dp).panel().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniIconButton(if (playing) "Ⅱ" else "▶", onPlay)
            Slider(time, onTime, valueRange = 0f..6.28f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = TrigCyan, activeTrackColor = TrigCyan, inactiveTrackColor = TrigStroke))
            ActionCard("Reset", onReset, Modifier.width(72.dp).height(42.dp))
        }
    }
}

@Composable
private fun AiTutorDestination(question: String, onQuestion: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(question, color = TrigText, fontSize = 15.sp, modifier = Modifier.background(Color(0xFF07575E), RoundedCornerShape(18.dp, 18.dp, 3.dp, 18.dp)).padding(14.dp))
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Text("Great question!\n\ntan θ = sin θ / cos θ\n\nAt 90°, cos 90° = 0.\nDivision by zero is undefined.\n\nThat’s why tan 90° is undefined.\n\nSimilarly, tan 270°\nis also undefined.", color = TrigText, fontSize = 15.sp, lineHeight = 23.sp, modifier = Modifier.weight(1f).panel().padding(14.dp))
            TutorDiagram(Modifier.width(155.dp).align(Alignment.CenterVertically))
        }
        Row(Modifier.fillMaxWidth().height(58.dp).panel().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ask more questions...", color = TrigMuted, fontSize = 14.sp, modifier = Modifier.weight(1f).clickable { onQuestion("How do radians work?") }.padding(10.dp))
            MiniIconButton("➤", {})
        }
    }
}

@Composable
private fun UnitCircleCanvas(angle: Float, onAngle: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .pointerInput(onAngle) {
                detectDragGestures { change, _ ->
                    val dx = change.position.x - size.width / 2f
                    val dy = change.position.y - size.height / 2f
                    onAngle(normalizeDegrees(Math.toDegrees(atan2((-dy).toDouble(), dx.toDouble())).toFloat()))
                    change.consume()
                }
            }
            .clipToBounds()
            .padding(8.dp),
    ) {
        drawGrid()
        val center = Offset(size.width * .5f, size.height * .49f)
        val radius = minOf(size.width, size.height) * .38f
        drawLine(Color(0xFFCAD4D8), Offset(center.x - radius - 18, center.y), Offset(center.x + radius + 18, center.y), 1.2f)
        drawLine(Color(0xFFCAD4D8), Offset(center.x, center.y - radius - 18), Offset(center.x, center.y + radius + 18), 1.2f)
        drawCircle(TrigAmber, radius, center, style = Stroke(2.5f))
        val r = Math.toRadians(angle.toDouble())
        val s = sin(r)
        val c = cos(r)
        val point = Offset(center.x + radius * c.toFloat(), center.y - radius * s.toFloat())
        drawLine(TrigCyan, center, point, 4f, StrokeCap.Round)
        drawLine(TrigGreen, point, Offset(point.x, center.y), 3f)
        drawLine(TrigOrange, Offset(center.x, center.y), Offset(point.x, center.y), 3f)
        drawCircle(TrigCyan, 8f, point)
        listOf(0.0 to "(1, 0)", PI / 2 to "(0, 1)", PI to "(-1, 0)", 3 * PI / 2 to "(0, -1)").forEach { (a, _) ->
            drawCircle(TrigAmber, 6f, Offset(center.x + radius * cos(a).toFloat(), center.y - radius * sin(a).toFloat()))
        }
        drawArc(
            color = TrigCyan,
            startAngle = 0f,
            sweepAngle = -angle,
            useCenter = false,
            topLeft = center - Offset(radius * .3f, radius * .3f),
            size = Size(radius * .6f, radius * .6f),
            style = Stroke(2f),
        )
        drawLabel("(0, 1)", center.x - 28f, center.y - radius - 10f, TrigText)
        drawLabel("(1, 0)", center.x + radius + 7f, center.y - 6f, TrigText)
        drawLabel("(-1, 0)", center.x - radius - 58f, center.y - 6f, TrigText)
        drawLabel("(0, -1)", center.x - 32f, center.y + radius + 25f, TrigText)
        drawLabel("O", center.x - 21f, center.y - 10f, TrigText, 14f)
        drawLabel("P(${decimal(c)}, ${decimal(s)})", point.x + 10f, point.y + 18f, TrigGreen, 11f)
        drawLabel("${angle.roundToInt()}°", center.x + 30f, center.y + 44f, TrigCyan, 13f)
        drawLabel("cos θ = ${decimal(c)}", (center.x + point.x) / 2f - 35f, center.y + 25f, TrigOrange, 9f)
        drawLabel("sin θ = ${decimal(s)}", point.x + 8f, (point.y + center.y) / 2f, TrigGreen, 9f)
        drawLabel("ref ${oneDecimal(referenceAngle(angle))}°", center.x - 45f, center.y + radius * .55f, TrigAmber, 9f)
    }
}

@Composable
private fun FunctionGraphCanvas(function: String, overlays: Set<String>, angle: Float, onAngle: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .pointerInput(onAngle) {
                detectDragGestures { change, _ ->
                    onAngle((change.position.x / size.width * 720f - 360f).coerceIn(-360f, 360f))
                    change.consume()
                }
            }
            .clipToBounds()
            .padding(8.dp),
    ) {
        drawGrid()
        val midY = size.height / 2f
        drawLine(TrigMuted, Offset(0f, midY), Offset(size.width, midY), 2f)
        val ticks = listOf(-2 to "−2π", -1 to "−π", 0 to "0", 1 to "π", 2 to "2π")
        ticks.forEach { (multiple, label) ->
            val x = (multiple + 2) / 4f * size.width
            drawLine(TrigMuted.copy(.6f), Offset(x, midY - 7f), Offset(x, midY + 7f), 2f)
            drawLabel(label, x - 10f, midY + 24f, TrigMuted, 9f)
        }
        val intercepts = if (function == "cos") listOf(-1.5, -.5, .5, 1.5) else listOf(-2.0, -1.0, 0.0, 1.0, 2.0)
        intercepts.forEach { multiple ->
            val x = ((multiple + 2) / 4 * size.width).toFloat()
            drawCircle(colorForFunction(function), 5f, Offset(x, midY))
        }
        listOf(-1f, 1f).forEach { value ->
            val y = midY - value * size.height * .26f
            drawLabel(if (value > 0) "1" else "−1", 5f, y - 4f, TrigMuted, 8f)
        }
        if ("tan" in overlays || function == "tan") {
            listOf(-1.5, -.5, .5, 1.5).forEach { multiple ->
                val x = ((multiple + 2) / 4 * size.width).toFloat()
                drawDashedVertical(x, TrigAmber.copy(.65f))
            }
        }
        val functions = (overlays + function).distinct()
        functions.forEach { item ->
            val path = Path()
            var started = false
            for (i in 0..size.width.toInt()) {
                val x = i / size.width * 4 * PI - 2 * PI
                val raw = trigValue(item, x)
                val valid = item != "tan" || abs(cos(x)) > .075
                val y = midY - raw.toFloat() * size.height * .26f
                if (valid && y in -40f..size.height + 40f) {
                    if (!started) { path.moveTo(i.toFloat(), y); started = true } else path.lineTo(i.toFloat(), y)
                } else started = false
            }
            drawPath(path, colorForFunction(item).copy(if (item == function) 1f else .62f), style = Stroke(if (item == function) 4f else 2.5f, cap = StrokeCap.Round))
        }
        val px = ((angle + 360f) / 720f * size.width).coerceIn(0f, size.width)
        drawLine(TrigText.copy(.5f), Offset(px, 0f), Offset(px, size.height), 2f)
        functions.forEach { item ->
            val raw = trigValue(item, Math.toRadians(angle.toDouble()))
            if (item != "tan" || abs(cos(Math.toRadians(angle.toDouble()))) > .075) {
                val py = midY - raw.toFloat().coerceIn(-2f, 2f) * size.height * .26f
                drawCircle(colorForFunction(item), if (item == function) 9f else 6f, Offset(px, py))
                drawLabel("${item}=${safeTrig(raw)}", (px + 12f).coerceAtMost(size.width - 85f), (py - 10f).coerceIn(20f, size.height - 10f), colorForFunction(item), 9f)
            }
        }
    }
}

@Composable
private fun MiniWaveCanvas(function: String, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(6.dp)) {
        drawGrid()
        val path = Path()
        var active = false
        for (i in 0..size.width.toInt()) {
            val x = i / size.width * 4 * PI
            val yv = if (function == "tan") tan(x) else cos(x)
            val okay = function != "tan" || abs(cos(x)) > .1
            val y = size.height / 2 - yv.toFloat() * size.height * .32f
            if (okay && y in -20f..size.height + 20f) { if (!active) { path.moveTo(i.toFloat(), y); active = true } else path.lineTo(i.toFloat(), y) } else active = false
        }
        drawPath(path, if (function == "tan") TrigAmber else TrigOrange, style = Stroke(3f))
    }
}

@Composable
private fun TransformGraphCanvas(function: String, a: Float, b: Float, h: Float, k: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds().padding(8.dp)) {
        drawGraphAxes()
        val reference = Path()
        for (i in 0..size.width.toInt()) {
            val x = i / size.width * 5 * PI - 2.5 * PI
            val y = size.height / 2 - trigValue(function, x).toFloat().coerceIn(-3f, 3f) * size.height / 6f
            if (function == "tan" && abs(cos(x)) < .08) reference.moveTo(i.toFloat(), y) else if (i == 0) reference.moveTo(i.toFloat(), y) else reference.lineTo(i.toFloat(), y)
        }
        drawPath(reference, TrigMuted.copy(.45f), style = Stroke(2f))
        val path = Path()
        var started = false
        for (i in 0..size.width.toInt()) {
            val x = i / size.width * 5 * PI - 2.5 * PI
            val input = b * (x - h)
            val yv = a * trigValue(function, input).toFloat() + k
            val y = size.height / 2 - yv * size.height / 6f
            val valid = function != "tan" || abs(cos(input)) > .075
            if (valid && y in -40f..size.height + 40f) { if (!started) { path.moveTo(i.toFloat(), y); started = true } else path.lineTo(i.toFloat(), y) } else started = false
        }
        drawPath(path, TrigCyan, style = Stroke(4f, cap = StrokeCap.Round))
        val midlineY = size.height / 2 - k * size.height / 6f
        drawDashedHorizontal(midlineY, TrigGreen.copy(.7f))
        drawLabel("midline y=${oneDecimal(k)}", 12f, midlineY - 9f, TrigGreen, 9f)
        if (function != "tan") {
            val maxY = size.height / 2 - (k + abs(a)) * size.height / 6f
            val minY = size.height / 2 - (k - abs(a)) * size.height / 6f
            drawDashedHorizontal(maxY, TrigCyan.copy(.35f)); drawDashedHorizontal(minY, TrigCyan.copy(.35f))
            drawLabel("max ${oneDecimal(k + abs(a))}", size.width - 92f, maxY - 7f, TrigCyan, 8f)
            drawLabel("min ${oneDecimal(k - abs(a))}", size.width - 92f, minY - 7f, TrigCyan, 8f)
        }
        val period = if (function == "tan") PI / abs(b) else 2 * PI / abs(b)
        drawLabel("period ${oneDecimal(period.toFloat())}   shift ${oneDecimal(h)}", 12f, size.height - 12f, TrigAmber, 9f)
    }
}

@Composable
private fun IdentityCanvas(angle: Float, identity: Int, step: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(20.dp)) {
        val base = minOf(size.width, size.height) * .28f
        val rad = Math.toRadians(angle.toDouble())
        val sinSide = (base * abs(sin(rad))).toFloat()
        val cosSide = (base * abs(cos(rad))).toFloat()
        val origin = Offset(size.width * .48f, size.height * .55f)
        if (identity == 0) {
            if (step >= 0) {
                drawRect(TrigCyan.copy(.27f), Offset(origin.x - sinSide, origin.y - sinSide), Size(sinSide, sinSide))
                drawRect(TrigCyan, Offset(origin.x - sinSide, origin.y - sinSide), Size(sinSide, sinSide), style = Stroke(2f))
            }
            if (step >= 1) {
                drawRect(TrigOrange.copy(.32f), origin, Size(cosSide, cosSide))
                drawRect(TrigOrange, origin, Size(cosSide, cosSide), style = Stroke(2f))
            }
            val diamond = Path().apply { moveTo(origin.x, origin.y); lineTo(origin.x + base, origin.y - base); lineTo(origin.x + 2 * base, origin.y); lineTo(origin.x + base, origin.y + base); close() }
            if (step >= 2) { drawPath(diamond, TrigAmber.copy(.35f)); drawPath(diamond, TrigAmber, style = Stroke(3f)) }
            drawLabel("sin²θ", origin.x - sinSide + 8f, origin.y - sinSide / 2f, TrigCyan, 14f)
            drawLabel("cos²θ", origin.x + 8f, origin.y + cosSide / 2f, TrigOrange, 14f)
            if (step >= 2) drawLabel("= 1", origin.x + base * .78f, origin.y - 10f, TrigText, 22f)
        } else {
            val left = Offset(size.width * .18f, size.height * .72f)
            val right = Offset(size.width * .78f, size.height * .72f)
            val top = Offset(size.width * .68f, size.height * .25f)
            drawLine(TrigOrange, left, right, 4f)
            if (step >= 1) drawLine(TrigGreen, right, top, 4f)
            if (step >= 2) drawLine(TrigCyan, left, top, 5f)
            drawLabel("cos θ", (left.x + right.x) / 2f, right.y + 34f, TrigOrange, 13f)
            drawLabel("sin θ", right.x + 12f, (right.y + top.y) / 2f, TrigGreen, 13f)
            drawLabel("1", (left.x + top.x) / 2f - 15f, (left.y + top.y) / 2f - 12f, TrigCyan, 15f)
            drawLabel(identityProofs()[identity.coerceIn(0, identityProofs().lastIndex)].shortName, size.width * .28f, size.height * .16f, identityProofs()[identity.coerceIn(0, identityProofs().lastIndex)].color, 15f)
        }
    }
}

@Composable
private fun FerrisWheelCanvas(time: Float, wheelRadius: Float, centerHeight: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(10.dp)) {
        val radiusScale = .68f + ((wheelRadius - 8f) / 27f) * .32f
        val radius = minOf(size.width * .25f, size.height * .34f) * radiusScale
        val centerY = size.height * (.62f - ((centerHeight - 12f) / 33f) * .18f)
        val center = Offset(size.width * .28f, centerY)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF10153A), Color(0xFF321326), TrigBg)), Offset.Zero, size)
        drawCircle(TrigAmber, radius, center, style = Stroke(3f))
        for (i in 0 until 12) {
            val a = i * PI / 6
            val p = Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat())
            drawLine(TrigStroke, center, p, 2f); drawCircle(TrigOrange, 5f, p)
        }
        val rider = Offset(center.x + radius * cos(time).toFloat(), center.y - radius * sin(time).toFloat())
        drawCircle(TrigCyan, 7f, rider)
        val wave = Path().apply {
            for (i in 0..size.width.toInt()) {
                val y = center.y - radius * sin(i / size.width * 2 * PI + time).toFloat()
                if (i == 0) moveTo(i.toFloat(), y) else lineTo(i.toFloat(), y)
            }
        }
        drawPath(wave, TrigGreen, style = Stroke(3f))
    }
}

@Composable
private fun PendulumCanvas(time: Float, length: Float, release: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(12.dp)) {
        drawGrid()
        val pivot = Offset(size.width * .36f, size.height * .18f)
        val omega = kotlin.math.sqrt(9.81 / length)
        val theta = Math.toRadians((release * cos(omega * time)).toDouble())
        val rope = minOf(size.height * .58f, size.width * .32f)
        val bob = Offset(pivot.x + rope * sin(theta).toFloat(), pivot.y + rope * cos(theta).toFloat())
        drawLine(TrigMuted, Offset(pivot.x - 85f, pivot.y), Offset(pivot.x + 85f, pivot.y), 7f, StrokeCap.Round)
        drawLine(TrigMuted.copy(.35f), pivot, Offset(pivot.x, pivot.y + rope), 2f)
        drawLine(TrigCyan, pivot, bob, 5f, StrokeCap.Round)
        drawCircle(TrigAmber, 22f, bob)
        drawCircle(TrigOrange.copy(.25f), 34f, bob)
        drawArc(TrigViolet, -90f, Math.toDegrees(theta).toFloat(), false, topLeft = Offset(pivot.x - 65f, pivot.y - 65f), size = Size(130f, 130f), style = Stroke(3f))
        drawLabel("θ = ${oneDecimal(Math.toDegrees(theta).toFloat())}°", bob.x + 25f, bob.y, TrigCyan, 13f)
        val energyY = size.height * .82f
        drawLabel("Potential", size.width * .05f, energyY - 20f, TrigAmber, 10f)
        drawLine(TrigStroke, Offset(size.width * .05f, energyY), Offset(size.width * .55f, energyY), 14f, StrokeCap.Round)
        drawLine(TrigAmber, Offset(size.width * .05f, energyY), Offset(size.width * (.05f + .5f * abs(sin(theta)).toFloat()), energyY), 14f, StrokeCap.Round)
    }
}

@Composable
private fun SoundWaveCanvas(time: Float, amplitude: Float, frequency: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(12.dp)) {
        drawGrid()
        val mid = size.height * .52f
        val speakerX = size.width * .12f
        drawRect(TrigPanel2, Offset(speakerX - 45f, mid - 75f), Size(65f, 150f))
        val cone = Path().apply { moveTo(speakerX + 20f, mid - 52f); lineTo(speakerX + 75f, mid - 92f); lineTo(speakerX + 75f, mid + 92f); lineTo(speakerX + 20f, mid + 52f); close() }
        drawPath(cone, TrigViolet.copy(.35f)); drawPath(cone, TrigViolet, style = Stroke(3f))
        val startX = speakerX + 85f
        repeat(3) { band -> drawArc(TrigCyan.copy(1f - band * .22f), -70f, 140f, false, topLeft = Offset(startX - 28f + band * 32f, mid - 65f - band * 15f), size = Size(75f + band * 30f, 130f + band * 30f), style = Stroke(3f)) }
        val wave = Path()
        for (i in startX.roundToInt()..size.width.toInt()) {
            val x = (i - startX) / size.width * 4 * PI
            val y = mid - amplitude * sin(frequency * x - 2 * PI * time).toFloat() * size.height * .18f
            if (i == startX.roundToInt()) wave.moveTo(i.toFloat(), y) else wave.lineTo(i.toFloat(), y)
        }
        drawPath(wave, TrigCyan, style = Stroke(4f, cap = StrokeCap.Round))
        drawLabel("Compression", size.width * .38f, size.height * .16f, TrigGreen, 11f)
        drawLabel("Rarefaction", size.width * .63f, size.height * .82f, TrigViolet, 11f)
    }
}

@Composable
private fun SatelliteOrbitCanvas(time: Float, eccentricity: Float, speed: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(12.dp)) {
        drawGrid()
        val center = Offset(size.width * .38f, size.height * .53f)
        val rx = minOf(size.width * .31f, size.height * .33f)
        val ry = rx * kotlin.math.sqrt(1f - eccentricity * eccentricity)
        val focus = Offset(center.x - eccentricity * rx, center.y)
        drawOval(TrigViolet.copy(.28f), Offset(center.x - rx, center.y - ry), Size(rx * 2, ry * 2))
        drawOval(TrigCyan, Offset(center.x - rx, center.y - ry), Size(rx * 2, ry * 2), style = Stroke(3f))
        drawCircle(TrigAmber.copy(.25f), 38f, focus)
        drawCircle(TrigAmber, 20f, focus)
        val theta = time * speed
        val satellite = Offset(center.x + rx * cos(theta), center.y + ry * sin(theta))
        drawLine(TrigStroke, focus, satellite, 2f)
        drawCircle(TrigCyan.copy(.25f), 25f, satellite)
        drawCircle(TrigCyan, 9f, satellite)
        drawLabel("Earth (focus)", focus.x - 58f, focus.y + 52f, TrigAmber, 11f)
        drawLabel("Satellite", satellite.x + 16f, satellite.y - 12f, TrigCyan, 11f)
        drawLabel("periapsis", center.x - rx, center.y + ry + 34f, TrigMuted, 10f)
        drawLabel("apoapsis", center.x + rx - 35f, center.y + ry + 34f, TrigMuted, 10f)
    }
}

@Composable
private fun TutorDiagram(modifier: Modifier = Modifier) {
    Canvas(modifier.height(190.dp).panel().padding(8.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val r = minOf(size.width, size.height) * .36f
        drawCircle(TrigMuted, r, center, style = Stroke(2f))
        drawLine(TrigMuted, Offset(center.x - r - 10, center.y), Offset(center.x + r + 10, center.y), 2f)
        drawLine(TrigMuted, Offset(center.x, center.y - r - 10), Offset(center.x, center.y + r + 10), 2f)
        drawLine(TrigCyan, center, Offset(center.x, center.y - r), 4f)
        drawCircle(TrigGreen, 6f, Offset(center.x, center.y - r))
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.panel().padding(horizontal = 8.dp, vertical = 7.dp)) {
        Text(label, color = TrigMuted, fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SimulationParameterCard(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    color: Color,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.panel().padding(horizontal = 9.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TrigMuted, fontSize = 11.sp, maxLines = 1)
            Text("${oneDecimal(value)}$suffix", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = TrigStroke),
        )
    }
}

@Composable
private fun ActionCard(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.panel().clickable(onClick = onClick).padding(8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = TrigText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ValuesCard(title: String, values: List<Pair<String, String>>, accent: Color, modifier: Modifier = Modifier, horizontal: Boolean = false) {
    Column(modifier.panel().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (horizontal) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { values.forEach { (k, v) -> Text("$k $v", color = TrigText, fontSize = 10.sp) } }
        else values.forEach { (k, v) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(k, color = TrigMuted, fontSize = 9.sp); Text(v, color = TrigText, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun RightTriangleCard(s: Double, c: Double, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxHeight().panel().padding(8.dp)) {
        val a = Offset(size.width * .15f, size.height * .78f); val b = Offset(size.width * .85f, size.height * .78f); val d = Offset(size.width * .85f, size.height * .22f)
        drawLine(TrigOrange, a, b, 3f); drawLine(TrigGreen, b, d, 3f); drawLine(TrigAmber, a, d, 3f)
        drawLabel("Hypotenuse = 1", size.width * .38f, size.height * .16f, TrigText, 10f)
        drawLabel("Adjacent = ${decimal(c)}", size.width * .18f, size.height * .95f, TrigOrange, 9f)
        drawLabel("Opposite = ${decimal(s)}", size.width * .48f, size.height * .54f, TrigGreen, 9f)
    }
}

@Composable
private fun ParameterCard(label: String, value: Float, range: ClosedFloatingPointRange<Float>, color: Color, onValue: (Float) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.panel().padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(oneDecimal(value), color = color, fontSize = 18.sp)
        Slider(value, onValue, valueRange = range, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = TrigStroke))
    }
}

@Composable
private fun ResultCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.panel().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = TrigMuted, fontSize = 11.sp); Text(value, color = TrigText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TabButton(label: String, active: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().clickable(onClick = onClick).background(if (active) color.copy(.15f) else Color.Transparent).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (active) color else TrigMuted, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun SmallPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.background(if (active) TrigCyan.copy(.18f) else TrigPanel2, RoundedCornerShape(14.dp)).border(1.dp, if (active) TrigCyan else TrigStroke, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp)) { Text(label, color = if (active) TrigCyan else TrigText, fontSize = 10.sp) }
}

@Composable
private fun MiniIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(46.dp).background(TrigPanel2, RoundedCornerShape(16.dp)).border(1.dp, TrigStroke, RoundedCornerShape(16.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(label, color = TrigCyan, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
}

private fun Modifier.panel() = background(TrigPanel, RoundedCornerShape(14.dp)).border(1.dp, TrigStroke, RoundedCornerShape(14.dp))

private fun DrawScope.drawGrid() {
    val step = 24f
    var x = 0f; while (x < size.width) { drawLine(TrigGrid, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f; while (y < size.height) { drawLine(TrigGrid, Offset(0f, y), Offset(size.width, y), 1f); y += step }
}

private fun DrawScope.drawGraphAxes() {
    drawGrid(); drawLine(TrigMuted, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f); drawLine(TrigMuted, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f)
}

private fun DrawScope.drawDashedVertical(x: Float, color: Color) {
    var y = 0f
    while (y < size.height) { drawLine(color, Offset(x, y), Offset(x, (y + 12f).coerceAtMost(size.height)), 2f); y += 22f }
}

private fun DrawScope.drawDashedHorizontal(y: Float, color: Color) {
    if (y !in 0f..size.height) return
    var x = 0f
    while (x < size.width) { drawLine(color, Offset(x, y), Offset((x + 12f).coerceAtMost(size.width), y), 2f); x += 22f }
}

private fun DrawScope.drawLabel(text: String, x: Float, y: Float, color: Color, size: Float = 12f) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb((color.alpha * 255).roundToInt(), (color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt())
            textSize = size * density
        },
    )
}

private fun radiansLabel(angle: Float): String = when (angle.roundToInt()) {
    -180 -> "-π"; -135 -> "-3π/4"; -90 -> "-π/2"; -60 -> "-π/3"; -45 -> "-π/4"; -30 -> "-π/6"; 0 -> "0"; 30 -> "π/6"; 45 -> "π/4"; 60 -> "π/3"; 90 -> "π/2"; 120 -> "2π/3"; 135 -> "3π/4"; 180 -> "π"; else -> "${oneDecimal((angle / 180f))}π"
}

private data class IdentityProof(val shortName: String, val equation: String, val steps: List<String>, val color: Color)

private fun identityProofs() = listOf(
    IdentityProof("Pythagorean", "sin²θ + cos²θ = 1", listOf("Build squares on the sine and cosine legs.", "Their areas are sin²θ and cos²θ.", "Together they equal the unit hypotenuse square."), TrigCyan),
    IdentityProof("Tan–Sec", "1 + tan²θ = sec²θ", listOf("Begin with sin²θ + cos²θ = 1.", "Divide every term by cos²θ.", "Simplify to tan²θ + 1 = sec²θ."), TrigAmber),
    IdentityProof("Reciprocal", "sin θ · csc θ = 1", listOf("Read sin θ as opposite over hypotenuse.", "Invert it to obtain csc θ.", "The reciprocal factors multiply to 1."), TrigGreen),
    IdentityProof("Quotient", "tan θ = sin θ / cos θ", listOf("Write sin θ = opposite/hypotenuse.", "Write cos θ = adjacent/hypotenuse.", "Dividing cancels the hypotenuse, leaving opposite/adjacent."), TrigViolet),
    IdentityProof("Cofunction", "sin θ = cos(90° − θ)", listOf("The acute angles of a right triangle are complementary.", "The side opposite θ is adjacent to 90°−θ.", "Their sine and cosine ratios are therefore equal."), TrigOrange),
    IdentityProof("Double Angle", "sin 2θ = 2 sin θ cos θ", listOf("Use sin(α+β) with α = β = θ.", "Expand sinθ cosθ + cosθ sinθ.", "Combine equal terms to obtain 2sinθcosθ."), TrigCyan),
    IdentityProof("Angle Sum", "sin(θ+30°) = sinθ cos30° + cosθ sin30°", listOf("Rotate a unit vector by θ and then 30°.", "Resolve the vertical components after both rotations.", "Add the components to obtain the angle-sum identity."), TrigAmber),
)

private fun identityCheck(identity: Int, angle: Float): Pair<String, String> {
    val theta = Math.toRadians(angle.toDouble())
    val s = sin(theta); val c = cos(theta)
    val values = when (identity) {
        1 -> if (abs(c) < 1e-5) Double.NaN to Double.NaN else (1 + (s / c) * (s / c)) to (1 / (c * c))
        2 -> if (abs(s) < 1e-5) Double.NaN to Double.NaN else (s * (1 / s)) to 1.0
        3 -> if (abs(c) < 1e-5) Double.NaN to Double.NaN else (s / c) to (s / c)
        4 -> s to cos(Math.toRadians(90.0 - angle))
        5 -> sin(2 * theta) to (2 * s * c)
        6 -> sin(theta + PI / 6) to (s * cos(PI / 6) + c * sin(PI / 6))
        else -> (s * s + c * c) to 1.0
    }
    return safeTrig(values.first) to safeTrig(values.second)
}

private fun trigValue(function: String, x: Double): Double = when (function) { "cos" -> cos(x); "tan" -> tan(x); else -> sin(x) }
private fun safeTrig(value: Double): String = decimal(if (abs(value) < 1e-10) 0.0 else value)
private fun safeRatio(numerator: Double, denominator: Double): String = if (abs(denominator) < 1e-5) "undefined" else safeTrig(numerator / denominator)
private fun normalizeDegrees(value: Float): Float = ((value + 180f) % 360f + 360f) % 360f - 180f
private fun referenceAngle(angle: Float): Float {
    val normalized = ((angle % 360f) + 360f) % 360f
    return when { normalized <= 90f -> normalized; normalized <= 180f -> 180f - normalized; normalized <= 270f -> normalized - 180f; else -> 360f - normalized }
}
private fun angleDisplay(angle: Float, radians: Boolean): String = if (radians) radiansLabel(angle) else "${oneDecimal(angle)}°"

private fun quadrant(angle: Float): String { val n = ((angle % 360) + 360) % 360; return when { n == 0f || n == 90f || n == 180f || n == 270f -> "Axis"; n < 90 -> "I"; n < 180 -> "II"; n < 270 -> "III"; else -> "IV" } }
private fun exactValue(angle: Float, sine: Boolean): String { val a = angle.roundToInt(); return when (a) { -135 -> if (sine) "−√2/2" else "−√2/2"; -90 -> if (sine) "−1" else "0"; -45 -> if (sine) "−√2/2" else "√2/2"; 0 -> if (sine) "0" else "1"; 30 -> if (sine) "1/2" else "√3/2"; 45 -> "√2/2"; 60 -> if (sine) "√3/2" else "1/2"; 90 -> if (sine) "1" else "0"; 135 -> if (sine) "√2/2" else "−√2/2"; 180 -> if (sine) "0" else "−1"; else -> decimal(if (sine) sin(Math.toRadians(angle.toDouble())) else cos(Math.toRadians(angle.toDouble()))) } }
private fun decimal(v: Double): String = if (!v.isFinite() || abs(v) > 9999) "undefined" else String.format(java.util.Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
private fun oneDecimal(v: Float): String = if (abs(v - v.roundToInt()) < .001f) v.roundToInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
private fun signText(v: Double): String = if (v >= 0) "+" else "−"
private fun colorForFunction(f: String): Color = when (f) { "cos" -> TrigOrange; "tan" -> TrigAmber; else -> TrigCyan }
