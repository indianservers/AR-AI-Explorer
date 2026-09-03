package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.physics.core.ForceEnergyResult
import com.indianservers.aiexplorer.physics.core.CollisionType
import com.indianservers.aiexplorer.physics.core.KinematicsResult
import com.indianservers.aiexplorer.physics.core.OscillationResult
import com.indianservers.aiexplorer.physics.core.PhysicsMathWorkspaceEngine
import com.indianservers.aiexplorer.physics.core.PhysicsSeries
import com.indianservers.aiexplorer.physics.core.ProjectileResult
import com.indianservers.aiexplorer.workspace.MathModule
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private val PhysicsBlue = Color(0xFF31D7FF)
private val PhysicsPurple = Color(0xFFA878FF)
private val PhysicsGreen = Color(0xFF55E6A5)
private val PhysicsOrange = Color(0xFFFFC857)

private enum class PhysicsMathMode(val label: String) { Motion("Motion"), Projectile("Projectile"), ForceEnergy("Force & Energy"), Oscillation("Oscillation"), Collision("Collisions") }

@Composable
internal fun PhysicsMathWorkspace(vm: ExplorerViewModel) {
    var mode by remember { mutableStateOf(PhysicsMathMode.Motion) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Physics–Math Workspace", color = PhysicsBlue, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("Turn physical laws into vectors, functions, graphs and verified numerical predictions.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhysicsMathMode.entries.forEach { choice -> Button(onClick = { mode = choice }, enabled = choice != mode) { Text(choice.label) } }
        }
        when (mode) {
            PhysicsMathMode.Motion -> MotionPanel(vm)
            PhysicsMathMode.Projectile -> ProjectilePanel(vm)
            PhysicsMathMode.ForceEnergy -> ForceEnergyPanel(vm)
            PhysicsMathMode.Oscillation -> OscillationPanel(vm)
            PhysicsMathMode.Collision -> CollisionPanel(vm)
        }
        PhysicsUnitConverter(vm)
    }
}

@Composable
private fun MotionPanel(vm: ExplorerViewModel) {
    var x0 by rememberLabText(vm, MathModule.PhysicsMath, "motion.x0", "0"); var v0 by rememberLabText(vm, MathModule.PhysicsMath, "motion.v0", "5")
    var acceleration by rememberLabText(vm, MathModule.PhysicsMath, "motion.acceleration", "2"); var duration by rememberLabText(vm, MathModule.PhysicsMath, "motion.duration", "5")
    var seriesChoice by remember { mutableStateOf(0) }
    val result = remember(x0, v0, acceleration, duration) { runCatching {
        PhysicsMathWorkspaceEngine.kinematics(x0.toDouble(), v0.toDouble(), acceleration.toDouble(), duration.toDouble())
    } }
    Text("CONSTANT-ACCELERATION MOTION", color = PhysicsBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    PhysicsFields(listOf(
        FieldState("x₀ (m)", x0) { x0 = it }, FieldState("v₀ (m/s)", v0) { v0 = it },
        FieldState("a (m/s²)", acceleration) { acceleration = it }, FieldState("t (s)", duration) { duration = it },
    ))
    val value = result.getOrNull()
    if (value == null) return PhysicsError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhysicsMetric("Final position", "${physicsNumber(value.finalPosition)} m", PhysicsBlue, Modifier.weight(1f))
        PhysicsMetric("Final velocity", "${physicsNumber(value.finalVelocity)} m/s", PhysicsGreen, Modifier.weight(1f))
        PhysicsMetric("Average velocity", "${physicsNumber(value.averageVelocity)} m/s", PhysicsOrange, Modifier.weight(1f))
    }
    val series = listOf(value.position, value.velocity, value.acceleration)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { series.forEachIndexed { index, item -> Button(onClick = { seriesChoice = index }, enabled = index != seriesChoice) { Text(item.label) } } }
    PhysicsGraph(series[seriesChoice])
    PhysicsEquations(value.equations)
    Button(onClick = {
        vm.addFunction("${x0.toDouble()}+${v0.toDouble()}*x+0.5*${acceleration.toDouble()}*x^2")
        vm.open(MathModule.Graph2D)
    }) { Text("Open x(t) in Graph") }
}

@Composable
private fun ProjectilePanel(vm: ExplorerViewModel) {
    var speed by rememberLabText(vm, MathModule.PhysicsMath, "projectile.speed", "20"); var angle by rememberLabText(vm, MathModule.PhysicsMath, "projectile.angle", "45")
    var height by rememberLabText(vm, MathModule.PhysicsMath, "projectile.height", "0"); var gravity by rememberLabText(vm, MathModule.PhysicsMath, "projectile.gravity", "9.80665")
    val result = remember(speed, angle, height, gravity) { runCatching {
        PhysicsMathWorkspaceEngine.projectile(speed.toDouble(), angle.toDouble(), height.toDouble(), gravity.toDouble())
    } }
    Text("PROJECTILE MOTION", color = PhysicsBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    PhysicsFields(listOf(
        FieldState("Speed (m/s)", speed) { speed = it }, FieldState("Angle (°)", angle) { angle = it },
        FieldState("Height (m)", height) { height = it }, FieldState("g (m/s²)", gravity) { gravity = it },
    ))
    val value = result.getOrNull()
    if (value == null) return PhysicsError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhysicsMetric("Flight time", "${physicsNumber(value.flightTime)} s", PhysicsPurple, Modifier.weight(1f))
        PhysicsMetric("Range", "${physicsNumber(value.horizontalRange)} m", PhysicsGreen, Modifier.weight(1f))
        PhysicsMetric("Maximum height", "${physicsNumber(value.maximumHeight)} m", PhysicsOrange, Modifier.weight(1f))
    }
    PhysicsGraph(value.trajectory, horizontalLabel = "horizontal distance (m)")
    Text("v₀ = ${physicsVector(value.initialVelocity)} m/s · impact v = ${physicsVector(value.impactVelocity)} m/s", color = MaterialTheme.colorScheme.onSurfaceVariant)
    PhysicsEquations(value.equations)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val vx = value.initialVelocity.x
            if (abs(vx) < 1e-8) vm.reportStatus("A vertical launch cannot be written as y(x).") else {
                val slope = value.initialVelocity.y / vx
                val quadratic = gravity.toDouble() / (2.0 * vx * vx)
                vm.addFunction("${height.toDouble()}+$slope*x-$quadratic*x^2")
                vm.open(MathModule.Graph2D)
            }
        }) { Text("Open trajectory in Graph") }
        Button(onClick = {
            vm.addVector3D("v", Vec3(0.0, 0.0, 0.0), Vec3(value.initialVelocity.x, value.initialVelocity.y, 0.0), "projectile velocity")
            vm.open(MathModule.VectorLab)
        }) { Text("Send velocity to Vector Lab") }
    }
}

@Composable
private fun ForceEnergyPanel(vm: ExplorerViewModel) {
    var mass by rememberLabText(vm, MathModule.PhysicsMath, "force.mass", "5"); var fx by rememberLabText(vm, MathModule.PhysicsMath, "force.fx", "20"); var fy by rememberLabText(vm, MathModule.PhysicsMath, "force.fy", "0")
    var vx by rememberLabText(vm, MathModule.PhysicsMath, "force.vx", "0"); var vy by rememberLabText(vm, MathModule.PhysicsMath, "force.vy", "0"); var duration by rememberLabText(vm, MathModule.PhysicsMath, "force.duration", "3")
    val result = remember(mass, fx, fy, vx, vy, duration) { runCatching {
        PhysicsMathWorkspaceEngine.forceEnergy(mass.toDouble(), Vec2(fx.toDouble(), fy.toDouble()), Vec2(vx.toDouble(), vy.toDouble()), duration.toDouble())
    } }
    Text("VECTOR FORCE & WORK–ENERGY", color = PhysicsBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    PhysicsFields(listOf(
        FieldState("Mass (kg)", mass) { mass = it }, FieldState("Fₓ (N)", fx) { fx = it }, FieldState("Fᵧ (N)", fy) { fy = it },
        FieldState("v₀x (m/s)", vx) { vx = it }, FieldState("v₀y (m/s)", vy) { vy = it }, FieldState("Δt (s)", duration) { duration = it },
    ), columns = 3)
    val value = result.getOrNull()
    if (value == null) return PhysicsError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhysicsMetric("Acceleration", "${physicsVector(value.acceleration)} m/s²", PhysicsBlue, Modifier.weight(1f))
        PhysicsMetric("Work", "${physicsNumber(value.work)} J", PhysicsGreen, Modifier.weight(1f))
        PhysicsMetric("Δ kinetic energy", "${physicsNumber(value.kineticEnergyChange)} J", PhysicsOrange, Modifier.weight(1f))
    }
    ForceVectorCanvas(value)
    Text("Δr = ${physicsVector(value.displacement)} m · v final = ${physicsVector(value.finalVelocity)} m/s · Δp = ${physicsVector(value.momentumChange)} kg·m/s", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Work–energy residual: ${physicsNumber(value.work - value.kineticEnergyChange)} J", color = PhysicsGreen, fontWeight = FontWeight.Bold)
    PhysicsEquations(value.equations)
    Button(onClick = {
        vm.addVector3D("F", Vec3(0.0, 0.0, 0.0), Vec3(value.netForce.x, value.netForce.y, 0.0), "net force")
        vm.addVector3D("a", Vec3(0.0, 0.0, 0.0), Vec3(value.acceleration.x, value.acceleration.y, 0.0), "acceleration")
        vm.open(MathModule.VectorLab)
    }) { Text("Send F and a to Vector Lab") }
}

@Composable
private fun OscillationPanel(vm: ExplorerViewModel) {
    var amplitude by rememberLabText(vm, MathModule.PhysicsMath, "oscillation.amplitude", "1"); var frequency by rememberLabText(vm, MathModule.PhysicsMath, "oscillation.frequency", "1")
    var mass by rememberLabText(vm, MathModule.PhysicsMath, "oscillation.mass", "1"); var phase by rememberLabText(vm, MathModule.PhysicsMath, "oscillation.phase", "0"); var duration by rememberLabText(vm, MathModule.PhysicsMath, "oscillation.duration", "3")
    var seriesChoice by remember { mutableStateOf(0) }
    val result = remember(amplitude, frequency, mass, phase, duration) { runCatching {
        PhysicsMathWorkspaceEngine.oscillation(amplitude.toDouble(), frequency.toDouble(), mass.toDouble(), phase.toDouble(), duration.toDouble())
    } }
    Text("SIMPLE HARMONIC MOTION", color = PhysicsBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    PhysicsFields(listOf(
        FieldState("A (m)", amplitude) { amplitude = it }, FieldState("f (Hz)", frequency) { frequency = it },
        FieldState("Mass (kg)", mass) { mass = it }, FieldState("Phase (°)", phase) { phase = it }, FieldState("Duration (s)", duration) { duration = it },
    ))
    val value = result.getOrNull()
    if (value == null) return PhysicsError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhysicsMetric("Period", "${physicsNumber(value.period)} s", PhysicsPurple, Modifier.weight(1f))
        PhysicsMetric("Spring k", "${physicsNumber(value.springConstant)} N/m", PhysicsGreen, Modifier.weight(1f))
        PhysicsMetric("Total energy", "${physicsNumber(value.totalEnergy)} J", PhysicsOrange, Modifier.weight(1f))
    }
    val series = listOf(value.position, value.velocity, value.acceleration)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { series.forEachIndexed { index, item -> Button(onClick = { seriesChoice = index }, enabled = index != seriesChoice) { Text(item.label) } } }
    PhysicsGraph(series[seriesChoice])
    PhysicsEquations(value.equations)
    Button(onClick = {
        val omega = value.angularFrequency
        val phaseRad = phase.toDouble() * kotlin.math.PI / 180.0
        vm.addFunction("${amplitude.toDouble()}*cos($omega*x+$phaseRad)")
        vm.open(MathModule.Graph2D)
    }) { Text("Open x(t) in Graph") }
}

@Composable
private fun CollisionPanel(vm: ExplorerViewModel) {
    var firstMass by rememberLabText(vm, MathModule.PhysicsMath, "collision.m1", "2")
    var firstVelocity by rememberLabText(vm, MathModule.PhysicsMath, "collision.u1", "5")
    var secondMass by rememberLabText(vm, MathModule.PhysicsMath, "collision.m2", "3")
    var secondVelocity by rememberLabText(vm, MathModule.PhysicsMath, "collision.u2", "0")
    var type by remember { mutableStateOf(CollisionType.Elastic) }
    val result = remember(firstMass, firstVelocity, secondMass, secondVelocity, type) { runCatching {
        PhysicsMathWorkspaceEngine.collision1D(firstMass.toDouble(), firstVelocity.toDouble(), secondMass.toDouble(), secondVelocity.toDouble(), type)
    } }
    Text("ONE-DIMENSIONAL COLLISIONS", color = PhysicsBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    PhysicsFields(listOf(
        FieldState("m₁ (kg)", firstMass) { firstMass = it }, FieldState("u₁ (m/s)", firstVelocity) { firstVelocity = it },
        FieldState("m₂ (kg)", secondMass) { secondMass = it }, FieldState("u₂ (m/s)", secondVelocity) { secondVelocity = it },
    ))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CollisionType.entries.forEach { choice -> Button(onClick = { type = choice }, enabled = type != choice) { Text(if (choice == CollisionType.Elastic) "Elastic" else "Stick together") } }
    }
    val value = result.getOrNull() ?: return PhysicsError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhysicsMetric("v₁", "${physicsNumber(value.firstFinalVelocity)} m/s", PhysicsBlue, Modifier.weight(1f))
        PhysicsMetric("v₂", "${physicsNumber(value.secondFinalVelocity)} m/s", PhysicsPurple, Modifier.weight(1f))
        PhysicsMetric("Restitution e", physicsNumber(value.coefficientOfRestitution), PhysicsGreen, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhysicsMetric("Momentum residual", physicsNumber(value.momentumResidual), PhysicsGreen, Modifier.weight(1f))
        PhysicsMetric("Δ kinetic energy", "${physicsNumber(value.kineticEnergyChange)} J", PhysicsOrange, Modifier.weight(1f))
    }
    Text("Initial p = ${physicsNumber(value.initialMomentum)} · final p = ${physicsNumber(value.finalMomentum)} kg·m/s", color = MaterialTheme.colorScheme.onSurfaceVariant)
    PhysicsEquations(value.equations)
    Button(onClick = {
        vm.addVector3D("v1", Vec3(0.0, 0.0, 0.0), Vec3(value.firstFinalVelocity, 0.0, 0.0), "post-collision velocity")
        vm.addVector3D("v2", Vec3(0.0, 0.0, 0.0), Vec3(value.secondFinalVelocity, 0.0, 0.0), "post-collision velocity")
        vm.open(MathModule.VectorLab)
    }) { Text("Send final velocities to Vector Lab") }
}

private data class FieldState(val label: String, val value: String, val update: (String) -> Unit)

@Composable
private fun PhysicsFields(fields: List<FieldState>, columns: Int = 2) {
    fields.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { field -> OutlinedTextField(field.value, field.update, label = { Text(field.label) }, singleLine = true, modifier = Modifier.weight(1f)) }
            repeat(columns - row.size) { Column(Modifier.weight(1f)) {} }
        }
    }
}

@Composable
private fun PhysicsMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.border(1.dp, accent.copy(.5f), RoundedCornerShape(13.dp)).background(accent.copy(.08f), RoundedCornerShape(13.dp)).padding(10.dp)) {
        Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun PhysicsGraph(series: PhysicsSeries, horizontalLabel: String = "time (s)") {
    Text("${series.label.uppercase()} · ${series.unit} against $horizontalLabel", color = PhysicsPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    Canvas(Modifier.fillMaxWidth().height(270.dp).border(1.dp, PhysicsBlue.copy(.4f), RoundedCornerShape(16.dp)).padding(8.dp)) {
        val points = series.points
        if (points.size < 2) return@Canvas
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
        val rawMinY = minOf(0.0, points.minOf { it.y }); val rawMaxY = maxOf(0.0, points.maxOf { it.y })
        val ySpan = max(1e-9, rawMaxY - rawMinY); val paddingY = ySpan * .08
        val minY = rawMinY - paddingY; val maxY = rawMaxY + paddingY
        fun map(point: Vec2) = Offset(
            ((point.x - minX) / max(1e-12, maxX - minX) * size.width).toFloat(),
            (size.height - (point.y - minY) / (maxY - minY) * size.height).toFloat(),
        )
        if (0.0 in minX..maxX) drawLine(Color.Gray.copy(.45f), map(Vec2(0.0, minY)), map(Vec2(0.0, maxY)), 1.5f)
        if (0.0 in minY..maxY) drawLine(Color.Gray.copy(.45f), map(Vec2(minX, 0.0)), map(Vec2(maxX, 0.0)), 1.5f)
        val path = Path(); points.forEachIndexed { index, point -> val p = map(point); if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawPath(path, PhysicsBlue, style = Stroke(4f))
    }
}

@Composable
private fun ForceVectorCanvas(value: ForceEnergyResult) {
    Canvas(Modifier.fillMaxWidth().height(230.dp).border(1.dp, PhysicsPurple.copy(.4f), RoundedCornerShape(16.dp)).padding(8.dp)) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        fun end(vector: Vec2, fraction: Float): Offset {
            val magnitude = PhysicsMathWorkspaceEngine.magnitude(vector).coerceAtLeast(1e-12)
            val scale = size.minDimension * fraction / magnitude.toFloat()
            return Offset(centre.x + vector.x.toFloat() * scale, centre.y - vector.y.toFloat() * scale)
        }
        drawLine(Color.Gray.copy(.5f), Offset(0f, centre.y), Offset(size.width, centre.y), 1.5f)
        drawLine(Color.Gray.copy(.5f), Offset(centre.x, 0f), Offset(centre.x, size.height), 1.5f)
        drawLine(PhysicsOrange, centre, end(value.netForce, .38f), 8f)
        drawCircle(PhysicsOrange, 8f, end(value.netForce, .38f))
        drawLine(PhysicsGreen, centre, end(value.acceleration, .27f), 6f)
        drawCircle(PhysicsGreen, 7f, end(value.acceleration, .27f))
    }
    Text("Orange: net force · Green: acceleration (direction shared; independent display scaling)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
}

@Composable
private fun PhysicsEquations(equations: List<String>) {
    Text("MATHEMATICAL MODEL", color = PhysicsPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    equations.forEach { Text(it, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun PhysicsError(result: Result<*>) {
    Text(result.exceptionOrNull()?.message ?: "Enter valid values.", color = MaterialTheme.colorScheme.error)
}

@Composable
private fun PhysicsUnitConverter(vm: ExplorerViewModel) {
    val pairs = remember { listOf("km/h" to "m/s", "cm" to "m", "h" to "s", "°C" to "K") }
    var pairIndex by remember { mutableStateOf(0) }; var input by rememberLabText(vm, MathModule.PhysicsMath, "units.input", "36")
    val pair = pairs[pairIndex]
    val converted = runCatching { PhysicsMathWorkspaceEngine.convert(input.toDouble(), pair.first, pair.second) }
    Column(Modifier.fillMaxWidth().border(1.dp, PhysicsGreen.copy(.45f), RoundedCornerShape(15.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SI UNIT CONVERTER", color = PhysicsGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, label = { Text(pair.first) }, singleLine = true, modifier = Modifier.weight(1f))
            PhysicsMetric(pair.second, converted.fold({ physicsNumber(it) }, { "—" }), PhysicsGreen, Modifier.weight(1f))
        }
        Button(onClick = { pairIndex = (pairIndex + 1) % pairs.size }) { Text("Next conversion") }
    }
}

private fun physicsVector(vector: Vec2) = "(${physicsNumber(vector.x)}, ${physicsNumber(vector.y)})"
private fun physicsNumber(value: Double): String = when {
    !value.isFinite() -> "—"
    abs(value - value.toLong()) < 1e-8 -> value.toLong().toString()
    else -> "%.5f".format(Locale.US, value).trimEnd('0').trimEnd('.')
}
