package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.WorkspaceState
import java.util.Base64
import kotlin.math.*

enum class ArCadType { Point, Line, Segment, Ray, Vector, Plane, Triangle, Polygon, Circle, Sphere, Cube, Cuboid, Cylinder, Cone, Prism, Pyramid, Curve, ParametricSurface, FunctionSurface, ImplicitSurface, VectorField, Midpoint, Intersection }
data class ArCadNode(val id: String, val type: ArCadType, val parameters: Map<String,String> = emptyMap(),val dependencies: List<String> = emptyList()) {
    fun encode()=listOf(type.name,dependencies.joinToString(","),parameters.entries.sortedBy { it.key }.joinToString(";") { pack(it.key)+":"+pack(it.value) }).joinToString("|")
    companion object {
        fun pack(s: String)=Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
        fun unpack(s: String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        fun decode(id: String,s: String): ArCadNode { val f=s.split('|'); return ArCadNode(id,ArCadType.valueOf(f[0]),f.getOrElse(2){""}.split(';').filter(String::isNotBlank).associate { val p=it.split(':'); unpack(p[0]) to unpack(p[1]) },f.getOrElse(1){""}.split(',').filter(String::isNotBlank)) }
    }
}
fun WorkspaceState.arCadNodes()=labSessionValues.filterKeys { it.startsWith("arCad.node.") }.map { (key,value) -> ArCadNode.decode(key.removePrefix("arCad.node."),value) }.associateBy { it.id }
object ArCadDependencies {
    fun order(nodes: Map<String,ArCadNode>): List<String> {
        val visiting=mutableSetOf<String>(); val done=mutableSetOf<String>(); val result=mutableListOf<String>()
        fun visit(id: String) {
            require(id in nodes) { "Missing dependency $id" }
            if(id in done) return
            require(visiting.add(id)) { "Circular dependency at $id" }
            nodes.getValue(id).dependencies.forEach(::visit)
            visiting.remove(id); done+=id; result+=id
        }
        nodes.keys.forEach(::visit); return result
    }
}

/** Cached analytic primitives and constructions; references resolve in topological order. */
class ArCadSceneCompiler {
    private val cache=mutableMapOf<String,Pair<String,SpatialGeometry>>()
    private val edited=ArGraphGeometryCache()
    private val radii=mutableMapOf<String,Double>()
    @Synchronized fun build(source: SpatialRenderScene, state: WorkspaceState): SpatialRenderScene {
        val nodes=state.arCadNodes(); val centers=mutableMapOf<String,Vec3>(); val primitives=mutableListOf<SpatialPrimitive>()
        ArCadDependencies.order(nodes).forEach { id ->
            val original=nodes.getValue(id)
            var node=original
            val pose=state.arGraphObject(id).transform()
            val equal=original.parameters["radiusReference"]
            if(equal!=null) {
                require(state.labSessionValues["arCad.mesh.$equal"]==null) { "Equal radius reference must be an analytic sphere" }
                require(abs(pose.axisScale.x-pose.axisScale.y)<1e-8 && abs(pose.axisScale.y-pose.axisScale.z)<1e-8) { "Equal radius requires uniform sphere scaling" }
                node=node.copy(parameters=node.parameters+("radius" to ((radii[equal] ?: error("Equal radius needs a sphere reference"))/(pose.uniformScale*pose.axisScale.x)).toString()))
            }
            var parents=node.dependencies.filter { it!=equal && it!=original.parameters["tangentReference"] }.map(centers::getValue)
            val tangent=original.parameters["tangentReference"]
            if(tangent!=null) {
                val c=centers.getValue(tangent); val r=radii[tangent] ?: error("Tangency needs a sphere reference")
                val own=ArCadTopology.number(node.parameters["radius"] ?: "1")
                val currentLocal=parents.firstOrNull() ?: Vec3(ArCadTopology.number(node.parameters["x"] ?: "0"),ArCadTopology.number(node.parameters["y"] ?: "0"),ArCadTopology.number(node.parameters["z"] ?: "0"))
                require(state.labSessionValues["arCad.mesh.$tangent"]==null && abs(pose.axisScale.x-pose.axisScale.y)<1e-8 && abs(pose.axisScale.y-pose.axisScale.z)<1e-8) { "Tangency requires analytic spheres with uniform scale" }
                val currentVector=pose.orientation.rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(currentLocal.x*pose.axisScale.x,currentLocal.y*pose.axisScale.y,currentLocal.z*pose.axisScale.z)*pose.uniformScale)+pose.offsetMeters
                val current=Vec3(currentVector.x,currentVector.y,currentVector.z)
                val direction=if((current-c).magnitude()>1e-8) (current-c).normalized() else Vec3(1.0,0.0,0.0)
                val target=c+direction*(r+own*pose.uniformScale*pose.axisScale.x)
                val local=pose.orientation.conjugate().rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(target.x,target.y,target.z)-pose.offsetMeters)*(1.0/pose.uniformScale)
                parents=listOf(Vec3(local.x/pose.axisScale.x,local.y/pose.axisScale.y,local.z/pose.axisScale.z))+parents.drop(1)
            }
            val signature=node.copy(parameters=node.parameters-"parameterT").encode()+parents.joinToString()
            val g=cache[id]?.takeIf { it.first==signature }?.second ?: geometry(node,parents).also { cache[id]=signature to it }
            require(g.vertices.isNotEmpty() && g.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }) { "${node.type} has no finite geometry" }
            val actual=edited.geometry(id,g,state)
            val center=when(node.type) { ArCadType.Point,ArCadType.Midpoint,ArCadType.Intersection -> actual.vertices.first(); else -> actual.vertices.reduce(Vec3::plus)*(1.0/actual.vertices.size) }
            val moved=pose.orientation.rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(center.x*pose.axisScale.x,center.y*pose.axisScale.y,center.z*pose.axisScale.z)*pose.uniformScale)+pose.offsetMeters
            centers[id]=Vec3(moved.x,moved.y,moved.z)
            if(node.type in setOf(ArCadType.Sphere,ArCadType.Circle)) radii[id]=ArCadTopology.number(node.parameters["radius"] ?: "1")*pose.uniformScale*pose.axisScale.x
            val kind=when(node.type) { ArCadType.Point,ArCadType.Midpoint,ArCadType.Intersection -> SpatialPrimitiveKind.Point; ArCadType.Line,ArCadType.Segment,ArCadType.Ray,ArCadType.Vector,ArCadType.Curve,ArCadType.Circle -> SpatialPrimitiveKind.Curve; ArCadType.ImplicitSurface,ArCadType.FunctionSurface,ArCadType.ParametricSurface,ArCadType.Plane,ArCadType.Triangle,ArCadType.Polygon -> SpatialPrimitiveKind.Surface; ArCadType.VectorField -> SpatialPrimitiveKind.VectorField; else -> SpatialPrimitiveKind.Solid }
            primitives+=SpatialPrimitive(id,kind,g,SpatialMaterial("CAD",listOf(.3f,.85f,1f,runCatching { ArCadTopology.number(node.parameters["opacity"] ?: "0.8").toFloat().coerceIn(0f,1f) }.getOrDefault(.8f)),emissive=.15f,blendMode=SpatialBlendMode.Transparent),node.type.name,dependencyIds=node.dependencies.toSet(),metadata=mapOf("cadType" to node.type.name,"radius" to (node.parameters["radius"] ?: ""),"filled" to (node.parameters["filled"] ?: "true")),visible=!(node.type in setOf(ArCadType.FunctionSurface,ArCadType.ParametricSurface) && node.parameters["filled"]=="false" && node.parameters["wireframe"]=="false"))
            if(node.type==ArCadType.Curve && node.parameters.containsKey("parameterT")) {
                val pointCenter=parents.firstOrNull() ?: Vec3(ArCadTopology.number(node.parameters["x"] ?: "0"),ArCadTopology.number(node.parameters["y"] ?: "0"),ArCadTopology.number(node.parameters["z"] ?: "0"))
                val t=ArCadTopology.number(node.parameters.getValue("parameterT")); val variables=node.parameters.filterKeys { it.startsWith("parameter.") }.mapKeys { it.key.removePrefix("parameter.") }.mapValues { ArCadTopology.number(it.value) }+("t" to t)
                val engine=ExpressionEngine(); fun coordinate(key:String)=engine.compile(node.parameters.getValue(key)).eval(variables)
                primitives+=SpatialPrimitive("$id-parameter-point",SpatialPrimitiveKind.Point,SpatialGeometry(listOf(Vec3(coordinate("expressionX"),coordinate("expressionY"),coordinate("expressionZ"))+pointCenter),pointRadius=.09),SpatialMaterial("Parameter point",listOf(1f,.7f,.2f,1f)),"${node.type} t=$t",localTransform=state.arGraphObject(id).transform(),selectable=false)
            }
        }
        cache.keys.retainAll(nodes.keys)
        return source.copy(primitives=source.primitives+primitives)
    }
    fun validateNode(node:ArCadNode,nodes:Map<String,ArCadNode>) {
        val construction=node.dependencies.filter { it!=node.parameters["radiusReference"] && it!=node.parameters["tangentReference"] }
        val size=construction.size
        when(node.type) {
            ArCadType.Midpoint -> require(size==2) { "Midpoint requires two point dependencies" }
            ArCadType.Intersection -> require(size==5) { "Intersection requires two line points and three plane points" }
            ArCadType.Line,ArCadType.Segment,ArCadType.Ray,ArCadType.Vector -> require(size==0 || size==2) { "Provide two endpoint dependencies" }
            ArCadType.Plane -> require(size==0 || size==3) { "Provide three non-collinear point dependencies" }
            ArCadType.Triangle -> require(size==0 || size==3)
            ArCadType.Polygon -> require(size==0 || size>=3)
            else -> require(size<=1) { "This object accepts at most one center dependency" }
        }
        construction.forEach { id -> require(nodes[id]?.type in setOf(ArCadType.Point,ArCadType.Midpoint,ArCadType.Intersection)) { "Construction dependencies must be points" } }
        fun v(key:String,default:String)=ArCadTopology.number(node.parameters[key] ?: default)
        if(size==0 && node.type in setOf(ArCadType.Line,ArCadType.Segment,ArCadType.Ray,ArCadType.Vector)) require(Vec3(v("end.x","2")-v("x","0"),v("end.y","1")-v("y","0"),v("end.z","0")-v("z","0")).magnitude()>1e-9) { "Endpoints must differ" }
        if(size==0 && node.type in setOf(ArCadType.Plane,ArCadType.Circle)) require(Vec3(v("nx","0"),v("ny","0"),v("nz","1")).magnitude()>1e-9) { "Normal cannot be zero" }
    }
    private fun geometry(n: ArCadNode, parents: List<Vec3>): SpatialGeometry {
        fun v(key: String,default: String)=ArCadTopology.number(n.parameters[key] ?: default)
        fun point(prefix: String="")=Vec3(v(prefix+"x","0"),v(prefix+"y","0"),v(prefix+"z","0"))
        val center=parents.firstOrNull() ?: point()
        val count=v("samples","32").toInt().coerceIn(8,128)
        fun triangulate(points: List<Vec3>)=SpatialGeometry(points,(1 until points.size-1).flatMap { listOf(0,it,it+1) },points.indices.map { it to (it+1)%points.size })
        return when(n.type) {
            ArCadType.Point -> SpatialGeometry(listOf(center),pointRadius=.08)
            ArCadType.Midpoint -> { require(parents.size==2); SpatialGeometry(listOf((parents[0]+parents[1])*.5),pointRadius=.08) }
            ArCadType.Line,ArCadType.Segment,ArCadType.Ray,ArCadType.Vector -> {
                val a=parents.getOrNull(0) ?: point(); val b=parents.getOrNull(1) ?: point("end."); val d=b-a; require(d.magnitude()>1e-9) { "Endpoints must differ" }
                val points=when(n.type) { ArCadType.Line -> listOf(a-d*10.0,b+d*10.0); ArCadType.Ray -> listOf(a,a+d*10.0); else -> listOf(a,b) }
                val vertices=points.toMutableList(); val lines=mutableListOf(0 to 1)
                if(n.type==ArCadType.Vector) { val axis=if(abs(d.normalized().y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0); val side=AnalyticGeometry3D.cross(d,axis).normalized()*.1; vertices+=b-d.normalized()*.2+side; vertices+=b-d.normalized()*.2-side; lines+=listOf(1 to 2,1 to 3) }
                SpatialGeometry(vertices,lines=lines)
            }
            ArCadType.Plane,ArCadType.Circle -> {
                val normal=if(parents.size>=3) AnalyticGeometry3D.cross(parents[1]-parents[0],parents[2]-parents[0]).normalized() else Vec3(v("nx","0"),v("ny","0"),v("nz","1")).normalized()
                require(normal.magnitude()>1e-9)
                val u=AnalyticGeometry3D.cross(normal,if(abs(normal.y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized(); val w=AnalyticGeometry3D.cross(normal,u).normalized()
                if(n.type==ArCadType.Circle) { val r=v("radius","1"); require(r>0); val points=(0 until count).map { center+(u*cos(it*2*PI/count)+w*sin(it*2*PI/count))*r }; SpatialGeometry(points,lines=points.indices.map { it to (it+1)%points.size }) }
                else { val size=v("size","2"); require(size>0); triangulate(listOf(center-u*size-w*size,center+u*size-w*size,center+u*size+w*size,center-u*size+w*size)) }
            }
            ArCadType.Triangle,ArCadType.Polygon -> {
                val points=if(parents.size>=3) parents else (0 until if(n.type==ArCadType.Triangle) 3 else v("sides","5").toInt().coerceIn(3,32)).map { i -> val sides=if(n.type==ArCadType.Triangle) 3 else v("sides","5").toInt(); center+Vec3(cos(i*2*PI/sides),sin(i*2*PI/sides),0.0)*v("radius","1") }
                triangulate(points)
            }
            ArCadType.Curve -> {
                val engine=ExpressionEngine(); val x=engine.compile(MathExpressionNormalizer.normalize(n.parameters["expressionX"] ?: "cos(t)")); val y=engine.compile(MathExpressionNormalizer.normalize(n.parameters["expressionY"] ?: "sin(t)")); val z=engine.compile(MathExpressionNormalizer.normalize(n.parameters["expressionZ"] ?: "t/4"))
                val min=v("tMin","0"); val max=v("tMax","2*pi"); require(min<max)
                val points=(0..count).map { i -> val vars=n.parameters.filterKeys { it.startsWith("parameter.") }.mapKeys { it.key.removePrefix("parameter.") }.mapValues { ArCadTopology.number(it.value) }+mapOf("t" to min+(max-min)*i/count); Vec3(x.eval(vars),y.eval(vars),z.eval(vars))+center }
                SpatialGeometry(points,lines=(0 until count).map { it to it+1 },pointRadius=v("thickness","0.03"))
            }
            ArCadType.FunctionSurface,ArCadType.ParametricSurface,ArCadType.ImplicitSurface -> {
                val domain=SurfaceDomain(v("uMin","-3")..v("uMax","3"),v("vMin","-3")..v("vMax","3"),v("wMin","-3")..v("wMax","3"))
                val variables=n.parameters.filterKeys { it.startsWith("parameter.") }.mapKeys { it.key.removePrefix("parameter.") }.mapValues { ArCadTopology.number(it.value) }
                fun substitute(s: String)=variables.entries.fold(MathExpressionNormalizer.normalize(s)) { text,(key,value) -> text.replace(Regex("\\b${Regex.escape(key)}\\b"),"($value)") }
                val definition=if(n.type==ArCadType.FunctionSurface) SurfaceDefinition3D.Explicit(n.id,substitute(n.parameters["expressionZ"] ?: "x^2+y^2"),domain) else if(n.type==ArCadType.ImplicitSurface) SurfaceDefinition3D.Implicit(n.id,substitute(n.parameters["expressionF"] ?: "x^2+y^2+z^2-1"),domain) else SurfaceDefinition3D.Parametric(n.id,substitute(n.parameters["expressionX"] ?: "u"),substitute(n.parameters["expressionY"] ?: "v"),substitute(n.parameters["expressionZ"] ?: "sin(u)*cos(v)"),domain=domain)
                ArCadTopology.weld(TypedSurfaceMesher().mesh(definition,count.coerceIn(8,if(n.type==ArCadType.ImplicitSurface) 32 else 64)).geometry).let { g -> g.copy(vertices=g.vertices.map { it+center },lines=if(n.parameters["wireframe"] != "false") ArCadTopology.edges(g) else emptyList()) }
            }
            ArCadType.VectorField -> {
                val engine=ExpressionEngine(); val components=listOf("expressionX","expressionY","expressionZ").map { engine.compile(n.parameters[it] ?: when(it) { "expressionX" -> "-y"; "expressionY" -> "x"; else -> "0" }) }
                val density=v("density","5").toInt().coerceIn(2,10); val bound=v("bound","2"); val size=v("arrowSize","0.3"); require(bound>0 && size>0)
                val vertices=mutableListOf<Vec3>(); val lines=mutableListOf<Pair<Int,Int>>()
                val parameters=n.parameters.filterKeys { it.startsWith("parameter.") }.mapKeys { it.key.removePrefix("parameter.") }.mapValues { ArCadTopology.number(it.value) }
                for(i in 0 until density) for(j in 0 until density) for(k in 0 until density) {
                    val p=Vec3(-bound+2*bound*i/(density-1),-bound+2*bound*j/(density-1),-bound+2*bound*k/(density-1)); val variables=parameters+mapOf("x" to p.x,"y" to p.y,"z" to p.z)
                    val d=runCatching { Vec3(components[0].eval(variables),components[1].eval(variables),components[2].eval(variables)) }.getOrNull() ?: continue
                    if(d.magnitude()<1e-10) continue
                    val arrow=(if(n.parameters["normalize"]=="true") d.normalized() else d)*size
                    val a=p+center; val b=a+arrow; val side=AnalyticGeometry3D.cross(arrow,if(abs(arrow.normalized().y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized()*min(.08,arrow.magnitude()*.2)
                    val index=vertices.size; vertices+=listOf(a,b,b-arrow*.2+side,b-arrow*.2-side); lines+=listOf(index to index+1,index+1 to index+2,index+1 to index+3)
                }
                require(vertices.isNotEmpty()) { "Vector field is zero or undefined throughout its bounds" }
                SpatialGeometry(vertices,lines=lines,pointRadius=.025)
            }
            ArCadType.Intersection -> {
                require(parents.size==5) { "Intersection needs two line points and three plane points" }
                val normal=AnalyticGeometry3D.cross(parents[3]-parents[2],parents[4]-parents[2]).normalized()
                require(normal.magnitude()>1e-9)
                val d=parents[1]-parents[0]; val denominator=normal.dot(d); require(abs(denominator)>1e-9) { "Line is parallel to plane" }
                SpatialGeometry(listOf(parents[0]+d*(normal.dot(parents[2]-parents[0])/denominator)),pointRadius=.08)
            }
            else -> {
                val type=when(n.type) { ArCadType.Sphere -> SolidType.Sphere; ArCadType.Cube -> SolidType.Cube; ArCadType.Cuboid -> SolidType.Cuboid; ArCadType.Cylinder -> SolidType.Cylinder; ArCadType.Cone -> SolidType.Cone; ArCadType.Prism -> SolidType.TriangularPrism; else -> SolidType.Pyramid }
                val width=v("width","2"); val height=v("height","2"); val depth=v("depth","2"); val radius=v("radius","1"); require(listOf(width,height,depth,radius).all { it>0 })
                val mesh=SolidMeshFactory.create(Solid(type,width,height,depth,radius,position=center))
                val raw=SpatialGeometry(mesh.vertices.map { it+center },mesh.faces.flatMap { face -> (1 until face.size-1).flatMap { listOf(face[0],face[it],face[it+1]) } },mesh.edges.map { it.first to it.second })
                val signed=raw.triangles.chunked(3).sumOf { f -> raw.vertices[f[0]].dot(AnalyticGeometry3D.cross(raw.vertices[f[1]],raw.vertices[f[2]]))/6.0 }
                ArCadTopology.weld(if(signed<0) raw.copy(triangles=raw.triangles.chunked(3).flatMap { listOf(it[0],it[2],it[1]) }) else raw)
            }
        }
    }
}
