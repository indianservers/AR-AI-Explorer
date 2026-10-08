package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.arengine.analysis.*
import com.indianservers.aiexplorer.arengine.contract.*
import kotlin.math.*

data class ArCadAnalysisResult(val scene: SpatialRenderScene, val values: Map<String,String>)

/** Read-only adapters keep the existing equation editor authoritative. */
fun WorkspaceState.arExplicitAnalysisNodes():Map<String,ArCadNode> {
    val layers=surfaceLayers.ifEmpty { listOf(SpatialSurfaceLayer("surface-main",surfaceExpression)) }
    val native=layers.filter { it.kind==SpatialSurfaceKind.Explicit }.associate { layer -> layer.id to ArCadNode(layer.id,ArCadType.FunctionSurface,mapOf("expressionZ" to layer.expression,"uMin" to layer.domain.uMin.toString(),"uMax" to layer.domain.uMax.toString(),"vMin" to layer.domain.vMin.toString(),"vMax" to layer.domain.vMax.toString())) }
    return native+arCadNodes().filterValues { it.type==ArCadType.FunctionSurface }
}

/** Analysis uses the same edited, transformed geometry as picking. No AR depth enters the mathematics. */
object ArCadAnalysis {
    fun enrich(source: SpatialRenderScene, state: WorkspaceState): ArCadAnalysisResult {
        val settings=state.labSessionValues
        val overlays=mutableListOf<SpatialPrimitive>(); val values=linkedMapOf<String,String>()
        val plane=source.primitives.firstOrNull { it.id==settings["arAnalysis.slice"] && it.visible }
        if(plane!=null && plane.geometry.vertices.size>=3) {
            val g=world(plane); val p=g.vertices[0]
            val n=AnalyticGeometry3D.cross(g.vertices[1]-p,g.vertices[2]-p).normalized()
            if(n.magnitude()>1e-10 && g.vertices.all { abs((it-p).dot(n))<1e-6 }) {
                values["Slice plane"]="${n.x}x + ${n.y}y + ${n.z}z = ${n.dot(p)}"
                source.primitives.filter { it.visible && it.id!=plane.id && it.selectable && it.geometry.triangles.isNotEmpty() }.forEach { obj ->
                    val mesh=world(obj)
                    val loops=ArCrossSectionEngine.section(ArMesh(mesh.vertices.map { ArVector3(it.x,it.y,it.z) },mesh.triangles),ArPlane(ArVector3(p.x,p.y,p.z),ArVector3(n.x,n.y,n.z)))
                    loops.forEachIndexed { index,loop ->
                        val pts=loop.pointsUnits.map { Vec3(it.x,it.y,it.z) }
                        if(pts.size>1) overlays+=line("section-${obj.id}-$index",pts+if(loop.closed) listOf(pts.first()) else emptyList())
                    }
                    val node=state.arCadNodes()[obj.id]
                    if(node?.type==ArCadType.Cone && state.labSessionValues["arCad.mesh.${obj.id}"]==null && settings.keys.none { it.startsWith("arGraph.vertex.${obj.id}.") }) {
                        val pose=obj.localTransform
                        val radius=ArCadTopology.number(node.parameters["radius"] ?: "1"); val height=ArCadTopology.number(node.parameters["height"] ?: "2")
                        val localApex=obj.geometry.vertices.maxBy { it.y }
                        val apex=arCadWorldGeometry(SpatialGeometry(listOf(localApex)),state.arGraphObject(obj.id)).vertices.single()
                        values["Cone conic (infinite extension)"]=if(abs((apex-p).dot(n))<1e-8) "Degenerate: plane passes through apex" else transformedConic(n,pose,radius/height)
                    }
                    if(loops.isNotEmpty()) {
                        val u=AnalyticGeometry3D.cross(n,if(abs(n.y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized(); val v=AnalyticGeometry3D.cross(n,u)
                        val points=loops.flatMap { it.pointsUnits }.map { Vec3(it.x,it.y,it.z) }
                        val spanU=points.maxOf { it.dot(u) }-points.minOf { it.dot(u) }; val spanV=points.maxOf { it.dot(v) }-points.minOf { it.dot(v) }
                        values["${obj.label} section"]="${loops.size} contour(s), mesh perimeter ${loops.sumOf { it.perimeterUnits }} units; in-plane spans $spanU x $spanV units"
                    }
                }
            } else values["Slice plane"]="Use an undeformed planar object"
        }
        val id=settings["arAnalysis.surface"]
        val node=state.arExplicitAnalysisNodes()[id]
        val obj=source.primitives.firstOrNull { it.id==id && it.visible }
        if(node?.type==ArCadType.FunctionSurface && obj!=null) runCatching {
            require(settings["arCad.mesh.$id"]==null && settings.keys.none { it.startsWith("arGraph.vertex.$id.") }) { "Rebuild the analytic surface before derivative analysis" }
            fun number(key:String,default:String)=ArCadTopology.number(settings[key] ?: default)
            val x=number("arAnalysis.x","0"); val y=number("arAnalysis.y","0")
            require(x in ArCadTopology.number(node.parameters["uMin"] ?: "-3")..ArCadTopology.number(node.parameters["uMax"] ?: "3") && y in ArCadTopology.number(node.parameters["vMin"] ?: "-3")..ArCadTopology.number(node.parameters["vMax"] ?: "3")) { "Point lies outside surface domain" }
            var expression=node.parameters["expressionZ"] ?: "x^2+y^2"
            node.parameters.filterKeys { it.startsWith("parameter.") }.forEach { (key,value) -> expression=expression.replace(Regex("\\b${Regex.escape(key.removePrefix("parameter."))}\\b"),"(${ArCadTopology.number(value)})") }
            val compiled=ExpressionEngine().compile(if('=' in expression) expression.substringAfter('=') else expression)
            fun f(a:Double,b:Double)=compiled.eval(mapOf("x" to a,"y" to b))
            val value=f(x,y)
            val hx=max(1e-6,abs(x)*1e-5); val hy=max(1e-6,abs(y)*1e-5)
            fun smooth(left:Double,right:Double)=abs(left-right)<=1e-3*maxOf(1.0,abs(left),abs(right))
            require(smooth((value-f(x-hx,y))/hx,(f(x+hx,y)-value)/hx) && smooth((value-f(x,y-hy))/hy,(f(x,y+hy)-value)/hy)) { "A reliable derivative is unavailable at this point; check for a corner or discontinuity" }
            val diff=SurfaceCalculus().analyze(expression,x,y)
            val pose=state.arGraphObject(node.id); val transform=pose.transform()
            val center=Vec3(ArCadTopology.number(node.parameters["x"] ?: "0"),ArCadTopology.number(node.parameters["y"] ?: "0"),ArCadTopology.number(node.parameters["z"] ?: "0"))
            require(node.dependencies.isEmpty()) { "Derivative analysis currently requires a surface without a center dependency" }
            fun vector(v:Vec3)=transform.orientation.rotate(ArVector3(v.x*transform.axisScale.x,v.y*transform.axisScale.y,v.z*transform.axisScale.z)*transform.uniformScale).let { Vec3(it.x,it.y,it.z) }
            val p=arCadWorldGeometry(SpatialGeometry(listOf(diff.point+center)),pose).vertices.single()
            val tangentU=vector(Vec3(1.0,0.0,diff.gradient.x)); val tangentV=vector(Vec3(0.0,1.0,diff.gradient.y))
            val normal=AnalyticGeometry3D.cross(tangentU,tangentV).normalized()
            values["Point P (world)"]="${p.x}, ${p.y}, ${p.z}"
            values["f(x,y)"]=diff.point.z.toString(); values["Partial derivatives"]="fx=${diff.gradient.x}, fy=${diff.gradient.y} (numerical; local mathematical coordinates)"
            values["Gradient"]=diff.gradient.toString(); values["Unit normal (world)"]=normal.toString()
            values["Tangent plane"]="${normal.x}x + ${normal.y}y + ${normal.z}z = ${normal.dot(p)}"
            val dx=number("arAnalysis.dx","1"); val dy=number("arAnalysis.dy","0"); val dz=number("arAnalysis.dz","0"); val mag=sqrt(dx*dx+dy*dy+dz*dz); require(mag>1e-10) { "Direction cannot be zero" }
            values["Directional derivative"]=(diff.gradient.x*dx/mag+diff.gradient.y*dy/mag).toString()
            if(diff.gradient.magnitude()>1e-10) values["Direction / gradient angle"]=(acos(((diff.gradient.x*dx+diff.gradient.y*dy)/(diff.gradient.magnitude()*mag)).coerceIn(-1.0,1.0))*180/PI).toString()+" degrees"
            values["Local direction"]="$dx, $dy, $dz"
            if(settings["arAnalysis.direction"]!="false") overlays+=line("analysis-direction",listOf(p,p+vector(Vec3(dx/mag,dy/mag,dz/mag))*.8))
            overlays+=SpatialPrimitive("analysis-P",SpatialPrimitiveKind.Point,SpatialGeometry(listOf(p),pointRadius=.08),SpatialMaterial("P",listOf(1f,1f,1f,1f)),"Surface point P",selectable=false)
            if(settings["arAnalysis.gradient"]=="true") {
                val grad=if(settings["arAnalysis.normalize"]=="true") diff.gradient.normalized() else diff.gradient
                overlays+=line("analysis-gradient",listOf(p,p+vector(grad)*number("arAnalysis.scale","0.5")))
            }
            if(settings["arAnalysis.normal"]=="true") overlays+=line("analysis-normal",listOf(p,p+normal*.8))
            if(settings["arAnalysis.tangent"]=="true") {
                val u=tangentU.normalized(); val v=AnalyticGeometry3D.cross(normal,u).normalized()
                overlays+=SpatialPrimitive("analysis-tangent",SpatialPrimitiveKind.Surface,SpatialGeometry(listOf(p-u-v,p+u-v,p+u+v,p-u+v),listOf(0,1,2,0,2,3)),SpatialMaterial("Tangent",listOf(.2f,.7f,1f,.3f),blendMode=SpatialBlendMode.Transparent),"Tangent plane",selectable=false)
            }
        }.onFailure { values["Surface analysis"]=it.message ?: "Undefined differential" }
        if(settings["arAnalysis.scalar"]=="true") runCatching {
            val scalar=ArScalarSlice.build(settings["arAnalysis.scalarExpression"] ?: "x^2+y^2+z^2",ArCadTopology.number(settings["arAnalysis.scalarZ"] ?: "0"),ArCadTopology.number(settings["arAnalysis.scalarBound"] ?: "2"),contour=ArCadTopology.number(settings["arAnalysis.scalarContour"] ?: "1"))
            overlays.addAll(scalar.primitives)
            values["Scalar slice range"]="${scalar.minimum} to ${scalar.maximum} (blue to red)"
        }.onFailure { values["Scalar field"]=it.message ?: "Undefined scalar slice" }
        return ArCadAnalysisResult(source.copy(primitives=source.primitives+overlays),values)
    }
    fun conic(normal:Vec3,radiusHeightRatio:Double)=transformedConic(normal,ArLocalTransform(),radiusHeightRatio)
    fun transformedConic(normal:Vec3,transform:ArLocalTransform,radiusHeightRatio:Double):String {
        require(normal.magnitude()>1e-10 && radiusHeightRatio>0)
        val n=normal.normalized()
        val u=AnalyticGeometry3D.cross(n,if(abs(n.y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized()
        val v=AnalyticGeometry3D.cross(n,u).normalized()
        fun local(p:Vec3):Vec3 { val r=transform.orientation.conjugate().rotate(ArVector3(p.x,p.y,p.z)); return Vec3(r.x/transform.axisScale.x,r.y/transform.axisScale.y,r.z/transform.axisScale.z) }
        val a=local(u); val b=local(v); val k=radiusHeightRatio*radiusHeightRatio
        fun q(x:Vec3,y:Vec3)=x.x*y.x+x.z*y.z-k*x.y*y.y
        val aa=q(a,a); val bb=q(a,b); val cc=q(b,b); val scale=maxOf(abs(aa),abs(bb),abs(cc),1e-12)
        val discriminant=(bb*bb-aa*cc)/(scale*scale)
        return when {
            abs(discriminant)<1e-8 -> "Parabola"
            discriminant>0 -> "Hyperbola"
            abs(aa-cc)/scale<1e-8 && abs(bb)/scale<1e-8 -> "Circle"
            else -> "Ellipse"
        }
    }
    private fun world(p:SpatialPrimitive):SpatialGeometry {
        val t=p.localTransform
        return p.geometry.copy(vertices=p.geometry.vertices.map { v -> (t.orientation.rotate(ArVector3(v.x*t.axisScale.x,v.y*t.axisScale.y,v.z*t.axisScale.z)*t.uniformScale)+t.offsetMeters).let { Vec3(it.x,it.y,it.z) } })
    }
    private fun line(id:String,points:List<Vec3>):SpatialPrimitive {
        val vertices=points.toMutableList(); val edges=(0 until points.lastIndex).map { it to it+1 }.toMutableList()
        if(id in setOf("analysis-gradient","analysis-normal","analysis-direction") && points.size==2) {
            val direction=points[1]-points[0]
            if(direction.magnitude()>1e-10) {
                val length=min(.16,direction.magnitude()*.2)
                val unit=direction.normalized(); val side=AnalyticGeometry3D.cross(unit,if(abs(unit.y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized()*length*.4
                vertices+=listOf(points[1]-unit*length+side,points[1]-unit*length-side); edges+=listOf(1 to 2,1 to 3)
            }
        }
        return SpatialPrimitive(id,SpatialPrimitiveKind.Curve,SpatialGeometry(vertices,lines=edges,pointRadius=.04),SpatialMaterial("Analysis",listOf(1f,.8f,.2f,1f),emissive=.4f),id,selectable=false)
    }
}
