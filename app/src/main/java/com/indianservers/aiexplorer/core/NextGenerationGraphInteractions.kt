package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class CurveSculptCandidate(val parameter: String, val oldValue: Double, val newValue: Double, val predictedY: Double, val error: Double)
data class CurveSculptPreview(val anchor: Vec2, val target: Vec2, val candidates: List<CurveSculptCandidate>, val preferred: CurveSculptCandidate?)

class CurveSculptEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun preview(expression: String, parameters: Map<String, Double>, anchorX: Double, targetY: Double): CurveSculptPreview {
        val compiled = expressions.compile(expression); val base = compiled.eval(parameters + ("x" to anchorX)); val h = 1e-5
        val candidates = parameters.mapNotNull { (name, value) ->
            val derivative = (compiled.eval(parameters + (name to value + h) + ("x" to anchorX)) - compiled.eval(parameters + (name to value - h) + ("x" to anchorX))) / (2 * h)
            if (!derivative.isFinite() || abs(derivative) < 1e-10) null else {
                val next = value + (targetY - base) / derivative
                val predicted = compiled.eval(parameters + (name to next) + ("x" to anchorX))
                CurveSculptCandidate(name, value, next, predicted, abs(predicted - targetY))
            }
        }.sortedWith(compareBy<CurveSculptCandidate> { it.error }.thenBy { abs(it.newValue - it.oldValue) })
        return CurveSculptPreview(Vec2(anchorX, base), Vec2(anchorX, targetY), candidates, candidates.firstOrNull())
    }
    fun commit(parameters: Map<String, Double>, candidate: CurveSculptCandidate) = parameters + (candidate.parameter to candidate.newValue)
}

data class DragFitPreview(val result: ProfessionalRegressionResult, val coefficientChanges: Map<String, Pair<Double, Double>>, val residualDelta: Double)
class DragToFitEngine(private val regression: ProfessionalRegressionEngine = ProfessionalRegressionEngine()) {
    fun drag(points: List<Vec2>, kind: GraphRegressionKind, handleX: Double, handleY: Double, influence: Double = .35): DragFitPreview {
        val before = regression.fit(points, kind); val nearest = points.minBy { abs(it.x - handleX) }
        val adjusted = points.map { if (it === nearest) Vec2(it.x, it.y + (handleY - nearest.y) * influence.coerceIn(0.0, 1.0)) else it }
        val after = regression.fit(adjusted, kind)
        val changes = (before.coefficients.keys + after.coefficients.keys).associateWith { (before.coefficients[it] ?: 0.0) to (after.coefficients[it] ?: 0.0) }
        return DragFitPreview(after, changes, after.rmse - before.rmse)
    }
}

data class EditableInequality(val variable: String, val lower: Double?, val upper: Double?, val lowerClosed: Boolean, val upperClosed: Boolean) {
    fun contains(value: Double) = (lower == null || if (lowerClosed) value >= lower else value > lower) && (upper == null || if (upperClosed) value <= upper else value < upper)
    fun dragLower(value: Double) = copy(lower = upper?.let { value.coerceAtMost(it) } ?: value)
    fun dragUpper(value: Double) = copy(upper = lower?.let { value.coerceAtLeast(it) } ?: value)
    fun toggleLower() = copy(lowerClosed = !lowerClosed); fun toggleUpper() = copy(upperClosed = !upperClosed)
    fun expression() = buildString { lower?.let { append("$it ${if (lowerClosed) "<=" else "<"} ") }; append(variable); upper?.let { append(" ${if (upperClosed) "<=" else "<"} $it") } }
}

data class OdeTrajectory(val initial: Vec2, val points: List<Vec2>)
object OdePhasePortraitEngine {
    fun seed(initial: Vec2, derivative: (Double, Double) -> Double, xSpan: ClosedFloatingPointRange<Double>, step: Double = .04): OdeTrajectory {
        require(step > 0); fun integrate(direction: Double): List<Vec2> { val out = mutableListOf(initial); var x=initial.x; var y=initial.y
            while ((direction > 0 && x < xSpan.endInclusive) || (direction < 0 && x > xSpan.start)) { val h=step*direction; val k1=derivative(x,y); val k2=derivative(x+h/2,y+h*k1/2); val k3=derivative(x+h/2,y+h*k2/2); val k4=derivative(x+h,y+h*k3); y += h*(k1+2*k2+2*k3+k4)/6; x += h; if(!y.isFinite()) break; out += Vec2(x,y) }; return out }
        return OdeTrajectory(initial, integrate(-1.0).asReversed().dropLast(1) + integrate(1.0))
    }
    fun directionField(derivative: (Double, Double)->Double, xs: ClosedFloatingPointRange<Double>, ys: ClosedFloatingPointRange<Double>, grid: Int=20) = (0..grid).flatMap { i -> (0..grid).map { j -> val x=xs.start+(xs.endInclusive-xs.start)*i/grid; val y=ys.start+(ys.endInclusive-ys.start)*j/grid; Vec2(x,y) to derivative(x,y) } }
}

data class VectorFieldProbe(val point: Vec2, val vector: Vec2, val magnitude: Double, val directionRadians: Double, val divergence: Double, val curl: Double)
object VectorFieldProbeEngine {
    fun probe(point: Vec2, field: (Double,Double)->Vec2, h: Double=1e-4): VectorFieldProbe { val v=field(point.x,point.y); val px=(field(point.x+h,point.y).x-field(point.x-h,point.y).x)/(2*h); val qy=(field(point.x,point.y+h).y-field(point.x,point.y-h).y)/(2*h); val qx=(field(point.x+h,point.y).y-field(point.x-h,point.y).y)/(2*h); val py=(field(point.x,point.y+h).x-field(point.x,point.y-h).x)/(2*h); return VectorFieldProbe(point,v,hypot(v.x,v.y),atan2(v.y,v.x),px+qy,qx-py) }
    fun streamline(seed:Vec2,field:(Double,Double)->Vec2,step:Double=.04,count:Int=500):List<Vec2>{val out=mutableListOf(seed);repeat(count){val p=out.last();val v=field(p.x,p.y);val m=hypot(v.x,v.y);if(m<1e-12||!m.isFinite())return out;out+=Vec2(p.x+step*v.x/m,p.y+step*v.y/m)};return out}
}

data class ComplexValue(val re:Double,val im:Double){val modulus get()=hypot(re,im);val argument get()=atan2(im,re)}
data class ComplexProbe(val input:ComplexValue,val output:ComplexValue,val hue:Double,val brightness:Double,val zero:Boolean,val pole:Boolean)
object ComplexFunctionExplorer {
    fun probe(z:ComplexValue,function:(ComplexValue)->ComplexValue):ComplexProbe{val w=function(z);val pole=!w.re.isFinite()||!w.im.isFinite()||w.modulus>1e12;val zero=w.modulus<1e-8;return ComplexProbe(z,w,(w.argument+PI)/(2*PI),(ln(1+w.modulus)/6).coerceIn(0.0,1.0),zero,pole)}
    fun conformalGrid(function:(ComplexValue)->ComplexValue, range:ClosedFloatingPointRange<Double>, lines:Int=12,samples:Int=100)= (0..lines).flatMap { i -> val c=range.start+(range.endInclusive-range.start)*i/lines; listOf((0..samples).map{j->function(ComplexValue(c,range.start+(range.endInclusive-range.start)*j/samples))},(0..samples).map{j->function(ComplexValue(range.start+(range.endInclusive-range.start)*j/samples,c))}) }
}

data class FourierComponent(val frequency:Int,val cosine:Double,val sine:Double,val amplitude:Double,val phase:Double)
data class FourierWorkspace(val samples:List<Double>,val components:List<FourierComponent>,val reconstruction:List<Double>,val rmse:Double)
object LinkedFourierEngine {
    fun analyse(samples:List<Double>,harmonics:Int=samples.size/2):FourierWorkspace{require(samples.size>=4);val n=samples.size;val parts=(0..harmonics.coerceAtMost(n/2)).map{k->val a=2.0/n*samples.indices.sumOf{i->samples[i]*cos(2*PI*k*i/n)};val b=2.0/n*samples.indices.sumOf{i->samples[i]*sin(2*PI*k*i/n)};FourierComponent(k,a,b,hypot(a,b),atan2(-b,a))};val reconstruction=samples.indices.map{i->parts.sumOf{p->(if(p.frequency==0).5 else 1.0)*(p.cosine*cos(2*PI*p.frequency*i/n)+p.sine*sin(2*PI*p.frequency*i/n))}};return FourierWorkspace(samples,parts,reconstruction,sqrt(samples.indices.sumOf{(samples[it]-reconstruction[it]).pow(2)}/n))}
}

data class UncertaintyPoint(val x:Double,val estimate:Double,val lower:Double,val upper:Double)
object GraphUncertaintyEngine { fun propagate(expression:String,parameters:Map<String,Double>,sigma:Map<String,Double>,xs:List<Double>,confidence:Double=1.96):List<UncertaintyPoint>{val c=ExpressionEngine().compile(expression);return xs.map{x->val mean=c.eval(parameters+("x" to x));val variance=sigma.entries.sumOf{(p,s)->val v=parameters[p]?:0.0;val h=max(1e-6,abs(v)*1e-5);val d=(c.eval(parameters+(p to v+h)+("x" to x))-c.eval(parameters+(p to v-h)+("x" to x)))/(2*h);d*d*s*s};val band=confidence*sqrt(variance);UncertaintyPoint(x,mean,mean-band,mean+band)}} }

enum class DiscontinuityKind { Hole, Jump, VerticalAsymptote, BranchBoundary }
data class DiscontinuityFinding(val x:Double,val kind:DiscontinuityKind,val left:Double?,val right:Double?,val explanation:String)
object GraphDiscontinuityInspector { fun inspect(function:(Double)->Double,domain:ClosedFloatingPointRange<Double>,samples:Int=2000):List<DiscontinuityFinding>{val dx=(domain.endInclusive-domain.start)/samples;return (1 until samples).mapNotNull{i->val x=domain.start+i*dx;val l=runCatching{function(x-dx*.1)}.getOrNull();val c=runCatching{function(x)}.getOrNull();val r=runCatching{function(x+dx*.1)}.getOrNull();when{l==null||r==null||l?.isFinite()!=true||r?.isFinite()!=true->DiscontinuityFinding(x,DiscontinuityKind.VerticalAsymptote,l,r,"Function becomes unbounded or undefined.");abs(l-r)>max(1.0,abs(l)+abs(r))*.25->DiscontinuityFinding(x,DiscontinuityKind.Jump,l,r,"One-sided values disagree.");c==null||!c.isFinite()->DiscontinuityFinding(x,DiscontinuityKind.Hole,l,r,"Finite neighbouring values but undefined centre.");else->null}}.distinctBy{(it.x/dx).toInt()/3}} }

data class LodBucket(val first:Vec2,val minimum:Vec2,val maximum:Vec2,val last:Vec2,val sourceCount:Int)
object LargeGraphDataPipeline { fun aggregate(points:List<Vec2>,pixelWidth:Int):List<LodBucket>{require(pixelWidth>0);if(points.size<=pixelWidth*4)return points.map{LodBucket(it,it,it,it,1)};val size=(points.size+pixelWidth-1)/pixelWidth;return points.chunked(size).map{bucket->LodBucket(bucket.first(),bucket.minBy{it.y},bucket.maxBy{it.y},bucket.last(),bucket.size)}} }

enum class GraphEasing { Linear, EaseInOut }
data class GraphKeyframe(val time:Double,val values:Map<String,Double>,val easing:GraphEasing=GraphEasing.Linear)
data class GraphAnimationFrame(val time:Double,val values:Map<String,Double>,val triggeredEvents:List<String>)
class GraphAnimationTimeline(private val keyframes:List<GraphKeyframe>,private val events:Map<Double,String> = emptyMap()) { init{require(keyframes.size>=2)};fun frame(time:Double):GraphAnimationFrame{val a=keyframes.lastOrNull{it.time<=time}?:keyframes.first();val b=keyframes.firstOrNull{it.time>=time}?:keyframes.last();val raw=if(a.time==b.time)0.0 else ((time-a.time)/(b.time-a.time)).coerceIn(0.0,1.0);val t=if(b.easing==GraphEasing.EaseInOut)raw*raw*(3-2*raw)else raw;val names=a.values.keys+b.values.keys;return GraphAnimationFrame(time,names.associateWith{(a.values[it]?:b.values[it]?:0.0)*(1-t)+(b.values[it]?:a.values[it]?:0.0)*t},events.filterKeys{abs(it-time)<1e-6}.values.toList())} }

data class SonificationVoice(val graphId:String,val frequencyHz:Double,val pan:Double,val gain:Double,val pulseMillis:Int,val hapticStrength:Double)
object MultigraphSonificationEngine { fun voices(values:Map<String,Double>,x:Double,yRange:ClosedFloatingPointRange<Double>):List<SonificationVoice>{val span=(yRange.endInclusive-yRange.start).coerceAtLeast(1e-9);return values.entries.sortedBy{it.key}.mapIndexed{i,(id,y)->val n=((y-yRange.start)/span).coerceIn(0.0,1.0);SonificationVoice(id,110*2.0.pow(n*4),((i%5)/2.0-1).coerceIn(-1.0,1.0),(1-abs(n-.5)).coerceIn(.15,1.0),(240-x.toInt().mod(160)).coerceAtLeast(60),(abs(n-.5)*2).coerceIn(0.0,1.0))}} }
