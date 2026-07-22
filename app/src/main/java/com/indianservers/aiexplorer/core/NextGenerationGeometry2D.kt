package com.indianservers.aiexplorer.core

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

data class GeometryProofClaim(val id:String,val statement:String,val dependencies:Set<String>,val reason:String,val verified:Boolean)
data class ConstructionProof(val claims:List<GeometryProofClaim>,val valid:Boolean,val diagnostics:List<String>)
object ConstructionProofEngine { fun build(steps:List<ProtocolStep>):ConstructionProof{val known=mutableSetOf<String>();val claims=steps.map{s->val valid=s.dependencies.all{it in known};known+=s.id;GeometryProofClaim(s.id,s.title,s.dependencies.toSet(),s.reason,valid)};return ConstructionProof(claims,claims.all{it.verified},claims.filterNot{it.verified}.map{"${it.id} uses an unavailable prerequisite."})} }

data class DegreeOfFreedom(val objectId:String,val freeDimensions:Int,val directions:List<Vec2>,val restrictingConstraints:List<String>,val explanation:String)
object GeometryFreedomEngine { fun analyse(pointId:String,incidentDirections:List<Vec2>,constraints:List<GeometryConstraint>):DegreeOfFreedom{val related=constraints.filter{pointId in it.dependencies};val independent=related.map{it::class.simpleName}.distinct().size;val dimensions=(2-independent).coerceIn(0,2);val directions=when(dimensions){0->emptyList();1->listOf(incidentDirections.firstOrNull()?.let{Vec2(-it.y,it.x)}?:Vec2(1.0,0.0));else->listOf(Vec2(1.0,0.0),Vec2(0.0,1.0))};return DegreeOfFreedom(pointId,dimensions,directions,related.map{it.id},"$pointId has $dimensions free dimension(s) after ${related.size} active constraints.")} }

enum class GeometryIntent { ExistingPoint, Midpoint, Incidence, Tangent, Perpendicular, Parallel, Symmetry, EqualLength }
data class GeometryIntentPreview(val intent:GeometryIntent,val point:Vec2,val score:Double,val references:List<String>,val explanation:String)
object IntentAwareGeometrySnap { fun infer(raw:Vec2,points:Map<String,Vec2>,segments:Map<String,Pair<Vec2,Vec2>>,tolerance:Double=.2):List<GeometryIntentPreview>{val out=mutableListOf<GeometryIntentPreview>();points.forEach{(id,p)->val d=raw.distanceTo(p);if(d<=tolerance)out+=GeometryIntentPreview(GeometryIntent.ExistingPoint,p,1-d/tolerance,listOf(id),"Coincide with $id")};segments.forEach{(id,s)->val mid=(s.first+s.second)*.5;val dm=raw.distanceTo(mid);if(dm<=tolerance)out+=GeometryIntentPreview(GeometryIntent.Midpoint,mid,1-dm/tolerance,listOf(id),"Use midpoint of $id");val d=s.second-s.first;val t=if(d.dot(d)<1e-15)0.0 else ((raw-s.first).dot(d)/d.dot(d)).coerceIn(0.0,1.0);val foot=s.first+d*t;val df=raw.distanceTo(foot);if(df<=tolerance)out+=GeometryIntentPreview(GeometryIntent.Incidence,foot,1-df/tolerance,listOf(id),"Place point on $id")};return out.sortedByDescending{it.score}} }

enum class ExactOrientation { Clockwise, Collinear, CounterClockwise }
object ExactGeometryPredicates { private val mc=MathContext(40);private fun bd(x:Double)=BigDecimal.valueOf(x);fun orientation(a:Vec2,b:Vec2,c:Vec2):ExactOrientation{val value=(bd(b.x)-bd(a.x)).multiply(bd(c.y)-bd(a.y),mc).subtract((bd(b.y)-bd(a.y)).multiply(bd(c.x)-bd(a.x),mc),mc);return when(value.signum()){1->ExactOrientation.CounterClockwise;-1->ExactOrientation.Clockwise;else->ExactOrientation.Collinear}};fun incident(point:Vec2,a:Vec2,b:Vec2)=orientation(a,b,point)==ExactOrientation.Collinear&&point.x in minOf(a.x,b.x)..maxOf(a.x,b.x)&&point.y in minOf(a.y,b.y)..maxOf(a.y,b.y);fun tangent(lineA:Vec2,lineB:Vec2,center:Vec2,point:Vec2)=incident(point,lineA,lineB)&&abs((point-center).dot(lineB-lineA))<1e-12 }

data class GeometryInvariant(val name:String,val baseline:Double,val maximumDeviation:Double,val stable:Boolean,val evidence:Int)
object GeometryInvariantDiscovery { fun discover(frames:List<Map<String,Double>>,relativeTolerance:Double=1e-7):List<GeometryInvariant>{if(frames.isEmpty())return emptyList();return frames.flatMap{it.keys}.distinct().mapNotNull{name->val values=frames.mapNotNull{it[name]}.filter{it.isFinite()};if(values.size!=frames.size)null else{val base=values.first();val deviation=values.maxOf{abs(it-base)};GeometryInvariant(name,base,deviation,deviation<=relativeTolerance*max(1.0,abs(base)),values.size)}}.sortedByDescending{it.stable}} }

enum class GeometryCanvasModel { Euclidean, Spherical, Hyperbolic, Projective, Affine }
data class ModelPoint(val coordinates:List<Double>)
object NonEuclideanGeometryEngine { fun distance(model:GeometryCanvasModel,a:ModelPoint,b:ModelPoint):Double=when(model){GeometryCanvasModel.Euclidean,GeometryCanvasModel.Affine->hypot(a.coordinates[0]-b.coordinates[0],a.coordinates[1]-b.coordinates[1]);GeometryCanvasModel.Spherical->{val dot=a.coordinates.zip(b.coordinates).sumOf{it.first*it.second};acos(dot.coerceIn(-1.0,1.0))};GeometryCanvasModel.Hyperbolic->{val dx=a.coordinates[0]-b.coordinates[0];val dy=a.coordinates[1]-b.coordinates[1];val aa=1-a.coordinates[0]*a.coordinates[0]-a.coordinates[1]*a.coordinates[1];val bb=1-b.coordinates[0]*b.coordinates[0]-b.coordinates[1]*b.coordinates[1];acoshSafe(1+2*(dx*dx+dy*dy)/(aa*bb))};GeometryCanvasModel.Projective->{val cross=abs(a.coordinates[0]*b.coordinates[1]-a.coordinates[1]*b.coordinates[0]);atan2(cross,a.coordinates.zip(b.coordinates).sumOf{it.first*it.second})}};fun geodesic(model:GeometryCanvasModel,a:ModelPoint,b:ModelPoint,samples:Int=64)=when(model){GeometryCanvasModel.Spherical->(0..samples).map{i->val t=i.toDouble()/samples;val angle=distance(model,a,b);val s=sin(angle).takeIf{abs(it)>1e-12}?:1.0;ModelPoint(a.coordinates.indices.map{j->sin((1-t)*angle)/s*a.coordinates[j]+sin(t*angle)/s*b.coordinates[j]})};else->(0..samples).map{i->val t=i.toDouble()/samples;ModelPoint(a.coordinates.indices.map{j->a.coordinates[j]*(1-t)+b.coordinates[j]*t})}};private fun acoshSafe(x:Double)=ln(x+sqrt(x*x-1)) }

data class Matrix3(val values:List<Double>){init{require(values.size==9)};operator fun times(other:Matrix3)=Matrix3(List(9){i->val r=i/3;val c=i%3;(0..2).sumOf{k->values[r*3+k]*other.values[k*3+c]}});fun apply(p:Vec2):Vec2{val x=values[0]*p.x+values[1]*p.y+values[2];val y=values[3]*p.x+values[4]*p.y+values[5];val w=values[6]*p.x+values[7]*p.y+values[8];return Vec2(x/w,y/w)};companion object{fun identity()=Matrix3(listOf(1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0));fun translation(x:Double,y:Double)=Matrix3(listOf(1.0,0.0,x,0.0,1.0,y,0.0,0.0,1.0));fun rotation(a:Double)=Matrix3(listOf(cos(a),-sin(a),0.0,sin(a),cos(a),0.0,0.0,0.0,1.0));fun scale(x:Double,y:Double)=Matrix3(listOf(x,0.0,0.0,0.0,y,0.0,0.0,0.0,1.0))}}
data class TransformationLink(val matrix:Matrix3,val handlePoints:List<Vec2>,val formula:String)
object CoordinateMatrixLinkEngine { fun link(matrix:Matrix3,handles:List<Vec2>)=TransformationLink(matrix,handles.map(matrix::apply),"[x',y',1]^T = M[x,y,1]^T") }

data class TransformationCompositionResult(val matrix:Matrix3,val transformed:List<Vec2>,val commutesWithReverse:Boolean,val fixedPoints:List<Vec2>)
object TransformationCompositionLab { fun compose(transformations:List<Matrix3>,points:List<Vec2>):TransformationCompositionResult{val composed=transformations.fold(Matrix3.identity()){a,b->b*a};val reverse=transformations.asReversed().fold(Matrix3.identity()){a,b->b*a};val fixed=points.filter{composed.apply(it).distanceTo(it)<1e-8};return TransformationCompositionResult(composed,points.map(composed::apply),composed.values.zip(reverse.values).all{abs(it.first-it.second)<1e-10},fixed)} }

data class ConstructionFamilyFrame(val parameter:Double,val points:Map<String,Vec2>)
data class ConstructionFamily(val frames:List<ConstructionFamilyFrame>,val envelopes:Map<String,List<Vec2>>)
object ParametricConstructionFamilyEngine { fun sweep(range:ClosedFloatingPointRange<Double>,samples:Int,constructor:(Double)->Map<String,Vec2>):ConstructionFamily{val frames=(0..samples).map{i->val t=range.start+(range.endInclusive-range.start)*i/samples;ConstructionFamilyFrame(t,constructor(t))};return ConstructionFamily(frames,frames.flatMap{it.points.keys}.distinct().associateWith{id->frames.mapNotNull{it.points[id]}})} }

data class GeometryOptimizationResult(val parameter:Double,val value:Double,val points:Map<String,Vec2>,val trace:List<Vec2>,val maximizing:Boolean)
object GeometryOptimizationEngine { fun optimize(range:ClosedFloatingPointRange<Double>,samples:Int=1000,maximize:Boolean=true,construction:(Double)->Map<String,Vec2>,objective:(Map<String,Vec2>)->Double):GeometryOptimizationResult{val states=(0..samples).map{i->val t=range.start+(range.endInclusive-range.start)*i/samples;Triple(t,construction(t),0.0)}.map{Triple(it.first,it.second,objective(it.second))};val best=if(maximize)states.maxBy{it.third}else states.minBy{it.third};return GeometryOptimizationResult(best.first,best.third,best.second,states.map{Vec2(it.first,it.third)},maximize)} }

private fun Vec2.dot(other:Vec2)=x*other.x+y*other.y
