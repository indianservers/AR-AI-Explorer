package com.indianservers.aiexplorer.mathworkspace.numbertheory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceAction
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSegmentedControl
import kotlinx.coroutines.delay
import java.math.BigInteger

@Composable
fun NumberTheoryWorkspace(vm: NumberTheoryViewModel, onBack: () -> Unit) {
    val mode = vm.mode.value
    MathWorkspaceShell("Number Theory Lab", "Primes · factors · congruences · integer solutions", onBack, mode.title, vm.sheet.value, { vm.sheet.value = it },
        canvas = { NumberTheoryCanvas(vm) },
        tools = {
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(P.snow.copy(alpha = .96f)).padding(horizontal = 5.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
                    WorkspaceSegmentedControl(NumberTheoryMode.entries.map { it.title }, mode.ordinal) { vm.cancel(); vm.mode.value = NumberTheoryMode.entries[it]; vm.sheet.value = com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop.Peek }
                }
            }
        },
        sheet = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Inspector(vm) } }
    )
}

@Composable private fun Inspector(vm: NumberTheoryViewModel) {
    val mode = vm.mode.value
    when (mode) {
        NumberTheoryMode.Prime -> {
            Input("Integer n", vm.input.value, { vm.input.value = it }, signed = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { WorkspaceAction("Test primality", onClick = vm::testPrime); WorkspaceAction("Factorize", onClick = vm::factor) }
            vm.primality.value?.let { Text("${vm.input.value} → ${if (it.isPrime) "Prime" else "Composite"} · ${it.certainty}", color = if (it.isPrime) P.green else P.red, fontWeight = FontWeight.Bold) }
            vm.factorization.value?.let { Text("${it.display()} · ${it.classification}", color = P.ink) }
            Text("Generate prime range", color = P.ink, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Input("From", vm.from.value, { vm.from.value = it }); Input("To", vm.to.value, { vm.to.value = it }); WorkspaceAction("Generate", onClick = vm::generatePrimes) }
            if (vm.primes.value.isNotEmpty()) { Text("${vm.primes.value.size} primes · gaps ${NumberTheoryEngine.primeGaps(vm.primes.value).take(16).joinToString()}", color = P.muted, fontSize = 11.sp); Text(vm.primes.value.joinToString(), color = P.blue, fontSize = 11.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Number line", "Ulam spiral").forEach { WorkspaceAction(it, onClick = { vm.visual.value = it }, selected = vm.visual.value == it) } }
        }
        NumberTheoryMode.Factors -> {
            Input("Integer n", vm.input.value, { vm.input.value = it }, signed = true); WorkspaceAction("Factorize", onClick = vm::factor)
            vm.factorization.value?.let { f ->
                Text("${f.display()}   ${f.classification}", color = P.blue, fontWeight = FontWeight.Bold)
                Text("Divisors: ${f.divisorCount} · sum ${f.divisorSum} · proper sum ${f.properDivisorSum}", color = P.ink, fontSize = 12.sp)
                runCatching { NumberTheoryEngine.divisors(f).joinToString() }.onSuccess { Text("Divisors: $it", color = P.muted, fontSize = 11.sp) }.onFailure { Text(it.message.orEmpty(), color = P.muted, fontSize = 10.sp) }
            }
        }
        NumberTheoryMode.Euclidean -> {
            InputsAB(vm)
            val result = runCatching { vm.euclidean() }.getOrNull()
            result?.let { r ->
                Text("gcd(${vm.a.value}, ${vm.b.value}) = ${r.gcd}   ·   lcm = ${r.lcm}", color = P.blue, fontWeight = FontWeight.Bold)
                Text("Extended: ${vm.a.value}(${r.x}) + ${vm.b.value}(${r.y}) = ${r.gcd}", color = P.ink, fontSize = 12.sp)
                var playing by remember { mutableStateOf(false) }
                LaunchedEffect(playing, vm.a.value, vm.b.value) { while (playing) { delay(850); if (vm.step.intValue + 1 >= r.steps.size) playing = false else vm.step.intValue++ } }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { WorkspaceAction("←", "Previous Euclidean step", { playing = false; vm.step.intValue = (vm.step.intValue - 1).coerceAtLeast(0) }); WorkspaceAction("Next →", "Next Euclidean step", { playing = false; vm.step.intValue = (vm.step.intValue + 1).coerceAtMost((r.steps.size - 1).coerceAtLeast(0)) }); WorkspaceAction(if (playing) "Pause" else "Play", "Play Euclidean steps", { playing = !playing }, selected = playing); WorkspaceAction("Reset", "Reset Euclidean steps", { playing = false; vm.step.intValue = 0 }) }
                r.steps.getOrNull(vm.step.intValue)?.let { Text("${it.dividend} = ${it.divisor} × ${it.quotient} + ${it.remainder}   (${vm.step.intValue + 1}/${r.steps.size})", color = P.violet) }
            }
        }
        NumberTheoryMode.Modular -> {
            InputsAB(vm); Input("Modulus m", vm.modulus.value, { vm.modulus.value = it })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Add", "Subtract", "Multiply", "Power").forEach { WorkspaceAction(it, "Use modular $it", { vm.modularOperation.value = it }, vm.modularOperation.value == it) } }
            if (vm.modularOperation.value == "Power") Input("Exponent k", vm.exponent.value, { vm.exponent.value = it })
            val result = runCatching { vm.modularResult() }.getOrNull(); result?.let { Text(when(vm.modularOperation.value) { "Add" -> "${vm.a.value} + ${vm.b.value} ≡ $it"; "Subtract" -> "${vm.a.value} − ${vm.b.value} ≡ $it"; "Multiply" -> "${vm.a.value} × ${vm.b.value} ≡ $it"; else -> "${vm.a.value}^${vm.exponent.value} ≡ $it" } + " (mod ${vm.modulus.value})", color = P.blue, fontWeight = FontWeight.Bold) }
            val inverse = runCatching { ModularArithmeticEngine.inverse(vm.a.value.toBigInteger(), vm.modulus.value.toBigInteger()) }.getOrNull()
            Text("Inverse: ${inverse?.toString() ?: "does not exist (gcd ≠ 1)"}", color = P.ink, fontSize = 12.sp)
        }
        NumberTheoryMode.Congruences -> {
            InputsAB(vm); Input("Modulus m", vm.modulus.value, { vm.modulus.value = it })
            val solved = runCatching { vm.congruence() }.getOrNull(); solved?.let { Text(if (it.hasSolution) "Solutions mod ${it.modulus}: ${it.residues.joinToString()}" else "No solution (gcd ${it.gcd} does not divide ${vm.b.value})", color = if (it.hasSolution) P.blue else P.red, fontWeight = FontWeight.Bold) }
            Text("Generalized Chinese remainder", color = P.ink, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Input("r₁", vm.crt1.value, { vm.crt1.value = it }); Input("m₁", vm.crtM1.value, { vm.crtM1.value = it }); Input("r₂", vm.crt2.value, { vm.crt2.value = it }); Input("m₂", vm.crtM2.value, { vm.crtM2.value = it }) }
            val crt = runCatching { vm.crt() }.getOrNull(); Text(crt?.let { "x ≡ ${it.residue} (mod ${it.modulus})" } ?: "No compatible CRT solution", color = P.violet)
        }
        NumberTheoryMode.Diophantine -> {
            InputsAB(vm); Input("Right side c", vm.c.value, { vm.c.value = it })
            runCatching { vm.diophantine() }.getOrNull()?.let { r -> Text(if (r.hasSolution) "${vm.a.value}x + ${vm.b.value}y = ${vm.c.value}" else "No integer solutions (gcd ${r.gcd} does not divide c)", color = if (r.hasSolution) P.blue else P.red, fontWeight = FontWeight.Bold); if (r.allIntegerPairs) Text("Every integer pair (x, y) is a solution.", color = P.ink, fontSize = 12.sp) else if (r.hasSolution) Text("(x₀, y₀) = (${r.x}, ${r.y})   ·   all: x=x₀+${r.stepX}t, y=y₀+(${r.stepY})t", color = P.ink, fontSize = 12.sp) }
        }
    }
    if (vm.loading.value) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp); Text("Working…", color=P.muted,fontSize=11.sp); WorkspaceAction("Cancel", "Cancel number theory calculation", vm::cancel) }
    if (vm.error.value.isNotBlank()) Text(vm.error.value, color = P.red, fontSize = 11.sp)
}

@Composable private fun InputsAB(vm: NumberTheoryViewModel) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Input("a", vm.a.value, { vm.a.value = it }, true); Input("b", vm.b.value, { vm.b.value = it }, true) } }

@Composable private fun Input(label: String, value: String, onChange: (String) -> Unit, signed: Boolean = false) {
    Column(Modifier.widthIn(min = 70.dp, max = 180.dp).clip(RoundedCornerShape(9.dp)).background(P.panelBlue).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(label, color = P.muted, fontSize = 9.sp)
        BasicTextField(value, onChange, Modifier.fillMaxWidth().heightIn(min = 25.dp).semantics { contentDescription = label }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (signed) KeyboardType.Number else KeyboardType.Number),
            textStyle = androidx.compose.ui.text.TextStyle(color = P.ink, fontSize = 12.sp))
    }
}

@Composable private fun NumberTheoryCanvas(vm: NumberTheoryViewModel) {
    val mode = vm.mode.value
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        Column(Modifier.fillMaxSize().padding(top = 48.dp, bottom = h * .40f, start = 10.dp, end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (mode) {
                NumberTheoryMode.Prime -> PrimeVisual(vm)
                NumberTheoryMode.Factors -> FactorVisual(vm)
                NumberTheoryMode.Euclidean -> EuclidVisual(vm)
                NumberTheoryMode.Modular -> ModClock(vm)
                NumberTheoryMode.Congruences -> CongruenceVisual(vm)
                NumberTheoryMode.Diophantine -> DiophantineVisual(vm)
            }
        }
    }
}

@Composable private fun PrimeVisual(vm: NumberTheoryViewModel) {
    val primes = vm.primes.value
    if (vm.visual.value == "Ulam spiral" && primes.isNotEmpty()) {
        val max = primes.last().toInt().coerceAtMost(625); val set = primes.filter { it <= BigInteger.valueOf(max.toLong()) }.toSet()
        var zoom by remember { mutableFloatStateOf(1f) }; var pan by remember { mutableStateOf(Offset.Zero) }
        Canvas(Modifier.fillMaxWidth().height(220.dp).pointerInput(Unit) { detectTransformGestures { _, drag, scale, _ -> zoom = (zoom * scale).coerceIn(.6f,2.5f); pan += drag } }.semantics { contentDescription = "Ulam spiral prime visualization; pinch to zoom and drag to pan" }) {
            val cols = 25; val cell = minOf(size.width, size.height) / cols
            var x = 12; var y = 12; var dx = 1; var dy = 0; var run = 1; var count = 0; var turns = 0
            for (n in 1..max) {
                val v = BigInteger.valueOf(n.toLong())
                drawCircle(if (v in set) P.blue else P.border.copy(alpha = .48f), if (v in set) cell * .32f * zoom else cell * .12f * zoom, Offset((x + 12) * cell + pan.x, (y + 12) * cell + pan.y))
                x += dx; y += dy; count++
                if (count == run) { count = 0; val t = dx; dx = -dy; dy = t; turns++; if (turns % 2 == 0) run++ }
            }
        }
    } else {
        Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = "Prime number line" }) {
            val lo = vm.from.value.toIntOrNull() ?: 0; val hi = vm.to.value.toIntOrNull() ?: 1
            val range = (hi - lo).coerceAtLeast(1)
            val y = size.height * .58f; drawLine(P.border, Offset(18f,y),Offset(size.width-18f,y),3f)
            primes.take(250).forEach { p -> val px = 18f + (p.toDouble() - lo) / range * (size.width - 36f); drawCircle(P.blue,6f,Offset(px.toFloat(),y)); drawContext.canvas.nativeCanvas.drawText(p.toString(),px.toFloat()-8,y-13,android.graphics.Paint().apply { color=android.graphics.Color.rgb(40,120,240);textSize=10f }) }
            drawContext.canvas.nativeCanvas.drawText("Prime gaps: ${NumberTheoryEngine.primeGaps(primes).take(12).joinToString()}",18f,size.height-15,android.graphics.Paint().apply{color=android.graphics.Color.DKGRAY;textSize=11f})
        }
    }
    Text(if (primes.isEmpty()) "Test a number or generate a range" else "${primes.size} primes in [${vm.from.value}, ${vm.to.value}]", color=P.muted, fontSize=11.sp)
}

@Composable private fun FactorVisual(vm: NumberTheoryViewModel) {
    val factors = vm.factorization.value?.factors.orEmpty()
    Text(vm.factorization.value?.display() ?: "Enter an integer and factorize", color=P.blue, fontSize=20.sp, fontWeight=FontWeight.Bold)
    if (factors.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { factors.forEach { (p,e) -> Column(horizontalAlignment=Alignment.CenterHorizontally) { Text(p.toString(),Modifier.clip(RoundedCornerShape(12.dp)).background(P.panelBlue).padding(12.dp),color=P.violet,fontWeight=FontWeight.Bold);Text("× $e",color=P.muted,fontSize=10.sp) } } }
}

@Composable private fun EuclidVisual(vm: NumberTheoryViewModel) {
    val r = runCatching { vm.euclidean() }.getOrNull() ?: return
    Text("Euclidean divisions", color=P.muted,fontSize=11.sp)
    r.steps.take(5).forEachIndexed { i,s -> Text("${s.dividend} = ${s.divisor} × ${s.quotient} + ${s.remainder}", color=if(i==vm.step.intValue)P.blue else P.ink,fontWeight=if(i==vm.step.intValue)FontWeight.Bold else FontWeight.Normal,fontSize=13.sp) }
    Text("gcd = ${r.gcd}",color=P.violet,fontSize=20.sp,fontWeight=FontWeight.Bold)
}

@Composable private fun ModClock(vm: NumberTheoryViewModel) {
    val m = vm.modulus.value.toIntOrNull()?.takeIf { it in 2..36 } ?: run { Text("Enter modulus from 2 to 36 to view the clock", color=P.muted); return }
    val residue = runCatching { vm.modularResult().toInt() }.getOrDefault(0)
    Canvas(Modifier.size(210.dp).semantics { contentDescription = "Modular clock modulo $m; result $residue" }) {
        val center=Offset(size.width/2,size.height/2);val radius=size.minDimension*.40f
        drawCircle(P.border,2f,center,radius)
        for (i in 0 until m) { val angle=(i.toDouble()/m*2*Math.PI-Math.PI/2);val point=Offset(center.x+radius*kotlin.math.cos(angle).toFloat(),center.y+radius*kotlin.math.sin(angle).toFloat());drawCircle(if(i==residue)P.violet else P.blue.copy(alpha=.45f),if(i==residue)10f else 5f,point);drawContext.canvas.nativeCanvas.drawText(i.toString(),point.x-4,point.y+4,android.graphics.Paint().apply{color=android.graphics.Color.DKGRAY;textSize=11f}) }
    }
    Text("Result residue: $residue (mod $m)",color=P.blue,fontWeight=FontWeight.Bold)
}

@Composable private fun CongruenceVisual(vm: NumberTheoryViewModel) {
    val result=runCatching{vm.congruence()}.getOrNull() ?: return
    val m=result.modulus.toInt().takeIf{it in 1..60} ?: 0
    if(m==0){Text("Residue display supports moduli through 60; the exact solution is below.",color=P.muted);return}
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.Center){(0 until m).forEach{n->Text(n.toString(),Modifier.padding(2.dp).clip(RoundedCornerShape(7.dp)).background(if(BigInteger.valueOf(n.toLong()) in result.residues)P.green else P.panelBlue).padding(5.dp),color=if(BigInteger.valueOf(n.toLong()) in result.residues)androidx.compose.ui.graphics.Color.White else P.ink,fontSize=10.sp)}}
}

@Composable private fun DiophantineVisual(vm: NumberTheoryViewModel) {
    val result=runCatching{vm.diophantine()}.getOrNull() ?: return
    if(!result.hasSolution){Text("No integer lattice points satisfy the equation",color=P.red);return}
    Canvas(Modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = "Integer lattice solutions to the Diophantine equation" }) {
        val a=vm.a.value.toFloatOrNull()?:0f;val b=vm.b.value.toFloatOrNull()?:0f;val c=vm.c.value.toFloatOrNull()?:0f
        val scale= minOf(size.width,size.height)*.42f/10f;val origin=Offset(size.width/2,size.height/2)
        for(i in -10..10){drawLine(P.border.copy(alpha=.7f),Offset(origin.x+i*scale,0f),Offset(origin.x+i*scale,size.height),1f);drawLine(P.border.copy(alpha=.7f),Offset(0f,origin.y+i*scale),Offset(size.width,origin.y+i*scale),1f)}
        drawLine(P.muted,Offset(0f,origin.y),Offset(size.width,origin.y),2f);drawLine(P.muted,Offset(origin.x,0f),Offset(origin.x,size.height),2f)
        if(b!=0f){for(i in 0..100){val x=-10f+i*.2f;val y=(c-a*x)/b;if(y in -10f..10f)drawCircle(P.blue,2.6f,Offset(origin.x+x*scale,origin.y-y*scale))}}
        val sx=result.stepX.toFloat();val sy=result.stepY.toFloat();for(t in -5..5){val x=result.x.toFloat()+sx*t;val y=result.y.toFloat()+sy*t;if(x in -10f..10f&&y in -10f..10f)drawCircle(P.violet,5f,Offset(origin.x+x*scale,origin.y-y*scale))}
    }
}
