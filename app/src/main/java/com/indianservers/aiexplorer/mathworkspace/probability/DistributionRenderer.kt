package com.indianservers.aiexplorer.mathworkspace.probability

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.*
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P

@androidx.compose.runtime.Composable
fun DistributionRenderer(vm: ProbabilityStatisticsViewModel, modifier: Modifier = Modifier) {
    val kind = vm.kind.value; val parameters = vm.parameters(); val viewport = vm.viewport.value
    Canvas(modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(P.snow).pointerInput(kind, parameters) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val base = graphBounds(vm,kind,parameters); val span = (base.second - base.first).coerceAtLeast(1e-12)
            val currentRange=xRange(size.width.toDouble(),kind,parameters,viewport,base);val pxPerUnit = size.width / (currentRange.second-currentRange.first).coerceAtLeast(1e-12)
            val nextScale = (viewport.scaleX * zoom).coerceIn(1e-12, 1e12)
            val sampleY=yMap(0.0,size.height.toDouble(),kind,parameters,viewport,vm.samples.value)
            if(vm.mode.value==ProbabilityMode.Distributions && centroid.y>sampleY-34f && kind !in listOf(DistributionKind.Binomial,DistributionKind.Poisson) && zoom==1f){
                val range=currentRange;val x=range.first+centroid.x/size.width*(range.second-range.first)
                val a=vm.lower.value.toDoubleOrNull()?:x;val b=vm.upper.value.toDoubleOrNull()?:x
                if(abs(x-a)<abs(x-b))vm.lower.value=x.toString()else vm.upper.value=x.toString()
            } else vm.viewport.value = viewport.copy(centerX = viewport.centerX - pan.x / pxPerUnit, scaleX = nextScale,
                centerY = viewport.centerY + pan.y / (size.height / (maxY(kind, parameters, vm.samples.value) * 1.2).coerceAtLeast(1e-12)),
                scaleY = (viewport.scaleY * zoom).coerceIn(1e-8, 1e8))
        }
    }.pointerInput(kind, parameters, viewport, vm.samples.value) {
        detectTapGestures { pos ->
            val (lo, hi) = xRange(size.width.toDouble(), kind, parameters, viewport)
            val x = lo + pos.x / size.width * (hi - lo)
            val axis = yMap(0.0, size.height.toDouble(), kind, parameters, viewport, vm.samples.value)
            if (pos.y > axis - 30 && kind.discrete.not()) {
                val a = vm.lower.value.toDoubleOrNull() ?: x; val b = vm.upper.value.toDoubleOrNull() ?: x
                if (abs(a - x) < abs(b - x)) vm.lower.value = x.toString() else vm.upper.value = x.toString()
            } else vm.inspectedX.value = x
        }
    }) {
        val w = size.width.toDouble(); val h = size.height.toDouble()
        val simulation=vm.simulationMeans.value
        val displayBounds=graphBounds(vm,kind,parameters)
        val (x0, x1) = xRange(w, kind, parameters, viewport,displayBounds)
        val empirical=if(simulation.isNotEmpty())simulation else vm.samples.value
        val theoreticalPeak=if(simulation.isNotEmpty()){val sd=sqrt(ProbabilityMath.variance(kind,parameters)/(vm.sampleSize.value.toIntOrNull()?.coerceIn(1,5000)?:1000));1/(sd*sqrt(2*PI)).coerceAtLeast(1e-12)}else maxY(kind, parameters, emptyList())
        val empiricalPeak=if(empirical.isNotEmpty()){
            val c=IntArray(36);empirical.forEach{v->val i=(((v-x0)/(x1-x0).coerceAtLeast(1e-12))*36).toInt().coerceIn(0,35);c[i]++};(c.maxOrNull()?:0).toDouble()/empirical.size/((x1-x0)/36).coerceAtLeast(1e-12)
        }else 0.0
        val maxY = max(theoreticalPeak,empiricalPeak*1.08).coerceAtLeast(1e-9) * 1.15
        val minY = viewport.centerY
        val spanY = (maxY / viewport.scaleY).coerceAtLeast(1e-12)
        fun sx(x: Double) = ((x - x0) / (x1 - x0) * w).toFloat()
        fun sy(y: Double) = (h - (y - minY) / spanY * h).toFloat()
        val xStep = tickStep(x1 - x0)
        var xTick = ceil(x0 / xStep) * xStep
        while (xTick <= x1 && xStep > 0) {
            val x = sx(xTick); drawLine(P.border.copy(alpha = .65f), Offset(x, 0f), Offset(x, size.height), 1f)
            drawContext.canvas.nativeCanvas.drawText(format(xTick), x + 3f, size.height - 7f, android.graphics.Paint().apply { color = android.graphics.Color.rgb(101,122,155); textSize = 11f })
            xTick += xStep
        }
        val yStep=tickStep(spanY);var yTick=ceil(minY/yStep)*yStep
        while(yTick<=minY+spanY){val y=sy(yTick);drawLine(P.border.copy(alpha=.52f),Offset(0f,y),Offset(size.width,y),.8f);drawContext.canvas.nativeCanvas.drawText(format(yTick),5f,y-3f,android.graphics.Paint().apply{color=android.graphics.Color.rgb(101,122,155);textSize=10f});yTick+=yStep}
        val axisY = sy(0.0).coerceIn(0f, size.height)
        drawLine(P.muted.copy(alpha=.55f), Offset(0f, axisY), Offset(size.width, axisY), 1.2f)
        if (kind.discrete) {
            if(simulation.isEmpty()) {
                val first = max(0.0, floor(x0)); val last = min(if (kind == DistributionKind.Binomial) parameters.trials.toDouble() else ceil(x1), ceil(x1))
                if (last - first <= 5000) for (index in 0..max(0.0,last-first).toInt()) {
                    val k=first+index; if(k>last)continue
                    val y = ProbabilityMath.pmf(kind, k, parameters)
                    val x = sx(k); val yy = sy(y)
                    val a=vm.lower.value.toDoubleOrNull();val b=vm.upper.value.toDoubleOrNull();val selected=a!=null&&b!=null&&k>=ceil(min(a,b))&&k<=floor(max(a,b));val color=if(selected)P.violet else P.blue
                    drawLine(color, Offset(x, axisY), Offset(x, yy), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawCircle(color, 4.2f, Offset(x, yy))
                }
                else {
                    val bins=min(900,max(160,(w/2).roundToInt()));val span=(x1-x0).coerceAtLeast(1e-12)
                    for(i in 0 until bins){val left=x0+span*i/bins;val right=x0+span*(i+1)/bins;val mass=ProbabilityMath.interval(kind,left,right,parameters);if(mass>0){val density=mass/(right-left).coerceAtLeast(1.0);val x=sx((left+right)/2);val yy=sy(density);drawLine(P.blue,Offset(x,axisY),Offset(x,yy),strokeWidth=2.1f);drawCircle(P.blue,2.5f,Offset(x,yy))}}
                }
            }
        } else {
            val path = Path(); val fill = Path(); var started = false; val samples = min(1400, max(220, (w / 1.4).roundToInt()))
            val lo = max(x0, if (kind == DistributionKind.Exponential) 0.0 else x0); val hi = x1
            for (i in 0..samples) {
                val x = lo + (hi - lo) * i / samples
                val y = if(simulation.isNotEmpty()) {val m=ProbabilityMath.mean(kind,parameters);val sd=sqrt(ProbabilityMath.variance(kind,parameters)/(vm.sampleSize.value.toIntOrNull()?.coerceIn(1,5000)?:1000));exp(-.5*((x-m)/sd).pow(2))/(sd*sqrt(2*PI))} else ProbabilityMath.pdf(kind, x, parameters)
                val px = sx(x); val py = sy(y)
                if (!y.isFinite() || !py.isFinite() || py < -size.height * 2 || py > size.height * 3) { started = false; continue }
                if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
            }
            drawPath(path, P.blue, style = Stroke(3f, cap = StrokeCap.Round))
            val a = vm.lower.value.toDoubleOrNull(); val b = vm.upper.value.toDoubleOrNull()
            if (vm.mode.value==ProbabilityMode.Distributions && a != null && b != null && a.isFinite() && b.isFinite()) {
                val left = max(min(a,b), x0); val right = min(max(a,b), x1)
                if (right > left) {
                    fill.moveTo(sx(left), axisY)
                    val n = 160
                    for (i in 0..n) { val x = left + (right-left)*i/n; fill.lineTo(sx(x), sy(ProbabilityMath.pdf(kind,x,parameters))) }
                    fill.lineTo(sx(right), axisY); fill.close()
                    drawPath(fill, P.blue.copy(alpha=.20f)); drawLine(P.blue, Offset(sx(left), sy(0.0)), Offset(sx(left), sy(ProbabilityMath.pdf(kind,left,parameters))), 1.6f)
                    drawLine(P.blue, Offset(sx(right), sy(0.0)), Offset(sx(right), sy(ProbabilityMath.pdf(kind,right,parameters))), 1.6f)
                }
            }
        }
        if(empirical.isNotEmpty()) {
            val bins=36;val counts=IntArray(bins);val width=(x1-x0).coerceAtLeast(1e-12)
            empirical.forEach{v->val i=(((v-x0)/width)*bins).toInt().coerceIn(0,bins-1);counts[i]++}
            counts.forEachIndexed{i,count->if(count>0){val left=i*size.width/bins;val right=(i+1)*size.width/bins;val density=count.toDouble()/empirical.size/(width/bins);val top=sy(density).coerceIn(0f,size.height);drawRect(P.violet.copy(alpha=.45f),Offset(left,top),androidx.compose.ui.geometry.Size(right-left,(sy(0.0)-top).coerceAtLeast(0f)))} }
        }
        vm.inspectedX.value?.let { x -> if (x in x0..x1) {
            val y = if (kind.discrete) ProbabilityMath.pmf(kind, round(x), parameters) else ProbabilityMath.pdf(kind, x, parameters)
            val px = sx(x); val py = sy(y)
            drawLine(P.violet.copy(alpha=.7f), Offset(px, 0f), Offset(px, size.height), 1.2f)
            drawCircle(P.violet, 5f, Offset(px, py.coerceIn(0f,size.height)))
        } }
    }
}

private fun graphBounds(vm:ProbabilityStatisticsViewModel,kind:DistributionKind,p:DistributionParameters):Pair<Double,Double>{
    val simulation=vm.simulationMeans.value
    if(simulation.isEmpty())return ProbabilityMath.support(kind,p)
    val n=vm.sampleSize.value.toIntOrNull()?.coerceIn(1,5000)?:1000;val mean=ProbabilityMath.mean(kind,p);val sd=sqrt(ProbabilityMath.variance(kind,p)/n).coerceAtLeast(1e-12)
    return if(kind==DistributionKind.Exponential)max(0.0,mean-5*sd) to mean+5*sd else mean-5*sd to mean+5*sd
}

private fun xRange(width: Double, kind: DistributionKind, p: DistributionParameters, view: DistributionViewport, bounds:Pair<Double,Double> = ProbabilityMath.support(kind,p)): Pair<Double,Double> {
    val center = if (view.centerX == 0.0) (bounds.first+bounds.second)/2 else view.centerX
    val span = (bounds.second-bounds.first).coerceAtLeast(1e-12) / view.scaleX
    val effectiveCenter = if (view.scaleX == 1.0 && view.centerX == 0.0) (bounds.first+bounds.second)/2 else center
    return effectiveCenter-span/2 to effectiveCenter+span/2
}
private fun maxY(kind: DistributionKind,p: DistributionParameters,samples: List<Double>): Double = when {
    samples.isNotEmpty() -> 1.0
    kind.discrete -> when(kind) { DistributionKind.Binomial -> max(ProbabilityMath.pmf(kind, floor((p.trials+1)*p.probability),p), 1e-6); DistributionKind.Poisson -> max(ProbabilityMath.pmf(kind, floor(p.first),p),1e-6); else -> 1.0 }
    else -> max(ProbabilityMath.pdf(kind, ProbabilityMath.mean(kind,p), p), 1e-9)
}
private fun yMap(y: Double,height: Double,kind: DistributionKind,p: DistributionParameters,v: DistributionViewport,s: List<Double>) = (height * (1 - (y-v.centerY)/(maxY(kind,p,s)*1.15/v.scaleY).coerceAtLeast(1e-12))).toFloat()
private fun tickStep(span: Double): Double { if (!span.isFinite() || span <= 0) return 1.0; val raw = span/7; val base = 10.0.pow(floor(log10(raw))); val fraction = raw/base; return (if(fraction<1.5) 1 else if(fraction<3.5) 2 else if(fraction<7.5) 5 else 10)*base }
private fun format(x: Double) = if (abs(x) >= 1e5 || (abs(x) < 1e-3 && x != 0.0)) "%.1e".format(x) else "%.3g".format(x)
