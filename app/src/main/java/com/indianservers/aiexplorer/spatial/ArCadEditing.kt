package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.arengine.interaction.ArSubObjectKind
import java.util.Base64
import kotlin.math.*

/** Indexed topology is shared by picking, connected editing, inspector and rendering. */
object ArCadTopology {
    private data class Topology(val geometry:SpatialGeometry,val edges:List<Pair<Int,Int>>,val faces:List<List<Int>>,val adjacency:List<Set<Int>>,val regions:MutableMap<Int,Set<Int>> = mutableMapOf())
    private val topologyCache=java.util.ArrayDeque<Topology>()
    @Synchronized private fun topology(g:SpatialGeometry):Topology {
        topologyCache.firstOrNull { it.geometry===g }?.let { return it }
        val faces=g.triangles.chunked(3); val incident=mutableMapOf<Pair<Int,Int>,MutableList<Int>>()
        faces.forEachIndexed { i,f -> f.indices.forEach { j -> val a=f[j]; val b=f[(j+1)%3]; incident.getOrPut(minOf(a,b) to maxOf(a,b)) { mutableListOf() }.add(i) } }
        val adjacency=List(faces.size) { mutableSetOf<Int>() }; incident.values.forEach { list -> list.forEach { i -> adjacency[i].addAll(list.filter { it!=i }) } }
        val value=Topology(g,if(g.lines.isNotEmpty() || faces.isEmpty()) g.lines else incident.filter { (_,fs) -> fs.size!=2 || run { fun normal(i:Int):Vec3 { val f=faces[i]; return AnalyticGeometry3D.cross(g.vertices[f[1]]-g.vertices[f[0]],g.vertices[f[2]]-g.vertices[f[0]]).normalized() }
                normal(fs[0]).dot(normal(fs[1]))<.999999 } }.keys.toList(),faces,adjacency)
        if(topologyCache.size>=48) topologyCache.removeFirst(); topologyCache.addLast(value); return value
    }
    fun edges(g: SpatialGeometry): List<Pair<Int,Int>> = topology(g).edges
    @Synchronized fun face(g: SpatialGeometry, triangle: Int): Set<Int> {
        val topology=topology(g); val faces=topology.faces
        require(triangle in faces.indices)
        topology.regions[triangle]?.let { return it }
        fun normal(f:List<Int>)=AnalyticGeometry3D.cross(g.vertices[f[1]]-g.vertices[f[0]],g.vertices[f[2]]-g.vertices[f[0]]).normalized()
        val n=normal(faces[triangle]); val origin=g.vertices[faces[triangle][0]]
        val selected=mutableSetOf(triangle); val queue=java.util.ArrayDeque<Int>(); queue.add(triangle)
        while(queue.isNotEmpty()) topology.adjacency[queue.removeFirst()].forEach { i ->
            val f=faces[i]
            if(i !in selected && normal(f).dot(n)>.999999 && f.all { abs((g.vertices[it]-origin).dot(n))<1e-7 }) { selected+=i; queue.add(i) }
        }
        val region=selected.toSet(); selected.forEach { topology.regions[it]=region }; return region
    }
    fun vertices(g: SpatialGeometry, kind: ArSubObjectKind, index: Int): Set<Int> = when(kind) {
        ArSubObjectKind.Vertex -> setOf(index).also { require(index in g.vertices.indices) }
        ArSubObjectKind.Edge -> edges(g).getOrNull(index)?.let { setOf(it.first,it.second) } ?: error("Edge no longer exists")
        ArSubObjectKind.Face -> face(g,index).flatMap { g.triangles.drop(it*3).take(3) }.toSet()
        ArSubObjectKind.Whole -> g.vertices.indices.toSet()
    }
    fun divide(g:SpatialGeometry,index:Int,ratio:Double):SpatialGeometry {
        require(ratio>0 && ratio<1) { "Division ratio must be between 0 and 1" }
        val edge=edges(g)[index]
        if(g.triangles.isEmpty()) {
            val next=g.vertices.size; val point=g.vertices[edge.first]+(g.vertices[edge.second]-g.vertices[edge.first])*ratio
            return g.copy(vertices=g.vertices+point,lines=g.lines.filterIndexed { i,_ -> i!=index }+listOf(edge.first to next,next to edge.second))
        }
        val mesh=EditableSpatialMesh(g.vertices,g.triangles); val meshIndex=SpatialMeshEditor.edges(mesh).indexOfFirst { (a,b) -> setOf(a,b)==setOf(edge.first,edge.second) }
        val result=SpatialMeshEditor.splitEdge(mesh,meshIndex,ratio)
        val next=result.mesh.vertices.size-1
        return SpatialGeometry(result.mesh.vertices,result.mesh.triangles,edges(g).filterIndexed { i,_ -> i!=index }+listOf(edge.first to next,next to edge.second))
    }
    fun weld(g: SpatialGeometry): SpatialGeometry {
        val indices = linkedMapOf<Vec3,Int>(); val remap = g.vertices.map { p -> val key=Vec3(round(p.x*1e9)/1e9,round(p.y*1e9)/1e9,round(p.z*1e9)/1e9); indices.getOrPut(key) { indices.size } }
        val points=indices.keys.toList(); val triangles=g.triangles.chunked(3).map { f -> f.map { remap[it] } }.filter { f -> f.distinct().size==3 && AnalyticGeometry3D.cross(points[f[1]]-points[f[0]],points[f[2]]-points[f[0]]).magnitude()>1e-10 }.flatten()
        return g.copy(vertices=points,triangles=triangles,lines=g.lines.map { remap[it.first] to remap[it.second] }.distinct())
    }
    fun move(g: SpatialGeometry, kind: ArSubObjectKind, index: Int, delta: Vec3, angle: Vec3=Vec3(0.0,0.0,0.0), scale: Double=1.0): SpatialGeometry {
        val selected = vertices(g,kind,index)
        val center = selected.map(g.vertices::get).reduce(Vec3::plus)*(1.0/selected.size)
        val q = com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(angle.x,angle.y,angle.z)
        return g.copy(vertices=g.vertices.mapIndexed { i,p -> if (i !in selected) p else {
            val v=(p-center)*scale
            val turned=q.rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(v.x,v.y,v.z))
            center+Vec3(turned.x,turned.y,turned.z)+delta
        } }).also(::validate)
    }
    fun validate(g: SpatialGeometry) {
        require(g.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }) { "Coordinates must be finite" }
        require(g.triangles.chunked(3).all { f -> f.size == 3 && f.all { it in g.vertices.indices } && AnalyticGeometry3D.cross(g.vertices[f[1]]-g.vertices[f[0]],g.vertices[f[2]]-g.vertices[f[0]]).magnitude() > 1e-10 }) { "Edit would collapse a face" }
    }
    fun encode(g: SpatialGeometry) = listOf(g.vertices.joinToString(";") { "${it.x},${it.y},${it.z}" },g.triangles.joinToString(","),g.lines.joinToString(";") { "${it.first},${it.second}" }).joinToString("|")
    fun decode(s: String): SpatialGeometry {
        val f=s.split('|')
        return SpatialGeometry(f[0].split(';').filter(String::isNotBlank).map { row -> val p=row.split(',').map(String::toDouble); Vec3(p[0],p[1],p[2]) },f[1].split(',').filter(String::isNotBlank).map(String::toInt),f.getOrElse(2){""}.split(';').filter(String::isNotBlank).map { val p=it.split(','); p[0].toInt() to p[1].toInt() }).also { require(it.triangles.all { index -> index in it.vertices.indices }) }
    }
    fun inspector(g: SpatialGeometry, kind: ArSubObjectKind, index: Int): Map<String,String> {
        val ids=vertices(g,kind,index); val center=ids.map(g.vertices::get).reduce(Vec3::plus)*(1.0/ids.size)
        val values=linkedMapOf("Coordinates" to "${center.x}, ${center.y}, ${center.z}","Connected vertices" to ids.size.toString())
        if (kind == ArSubObjectKind.Edge) { val e=edges(g)[index]; values["Length"]=(g.vertices[e.second]-g.vertices[e.first]).magnitude().toString() }
        if (kind == ArSubObjectKind.Face) {
            val triangles=face(g,index); val f=g.triangles.drop(index*3).take(3); val n=AnalyticGeometry3D.cross(g.vertices[f[1]]-g.vertices[f[0]],g.vertices[f[2]]-g.vertices[f[0]]).normalized()
            values["Area"]=triangles.sumOf { t -> val v=g.triangles.drop(t*3).take(3); AnalyticGeometry3D.cross(g.vertices[v[1]]-g.vertices[v[0]],g.vertices[v[2]]-g.vertices[v[0]]).magnitude()*.5 }.toString()
            values["Normal"]="${n.x}, ${n.y}, ${n.z}"; values["Plane equation"]="${n.x}x + ${n.y}y + ${n.z}z = ${n.dot(center)}"
        }
        return values
    }
    fun number(source: String): Double = ExpressionEngine().compile(MathExpressionNormalizer.normalize(source)).eval(emptyMap()).also { require(it.isFinite()) { "Enter a finite mathematical expression" } }
}

enum class ArCadSnapKind { Grid, Point, Vertex, Midpoint, Edge, Axis, Plane, Intersection, Surface }
data class ArCadSnap(val point: Vec3,val kind: ArCadSnapKind,val label: String)
object ArCadSnapping {
    fun snap(point: Vec3, geometry: List<SpatialGeometry>, tolerance: Double=.15, grid: Double=.25): ArCadSnap? {
        require(tolerance > 0 && grid > 0)
        val candidates=mutableListOf<ArCadSnap>()
        candidates+=ArCadSnap(Vec3(round(point.x/grid)*grid,round(point.y/grid)*grid,round(point.z/grid)*grid),ArCadSnapKind.Grid,"GRID")
        listOf(Vec3(point.x,0.0,0.0),Vec3(0.0,point.y,0.0),Vec3(0.0,0.0,point.z)).forEachIndexed { i,p -> candidates+=ArCadSnap(p,ArCadSnapKind.Axis,"${"XYZ"[i]} AXIS") }
        geometry.forEach { g ->
            g.vertices.forEach { candidates+=ArCadSnap(it,if(g.vertices.size==1) ArCadSnapKind.Point else ArCadSnapKind.Vertex,if(g.vertices.size==1) "POINT" else "VERTEX") }
            ArCadTopology.edges(g).forEach { (a,b) ->
                val first=g.vertices[a]; val last=g.vertices[b]; val d=last-first
                candidates+=ArCadSnap((first+last)*.5,ArCadSnapKind.Midpoint,"MIDPOINT")
                if(d.dot(d)>1e-12) candidates+=ArCadSnap(first+d*((point-first).dot(d)/d.dot(d)).coerceIn(0.0,1.0),ArCadSnapKind.Edge,"EDGE")
            }
            if(g.triangles.isNotEmpty()) {
                val face=g.triangles.take(3); val origin=g.vertices[face[0]]; val normal=AnalyticGeometry3D.cross(g.vertices[face[1]]-origin,g.vertices[face[2]]-origin).normalized()
                if(normal.magnitude()>1e-9 && g.vertices.all { abs((it-origin).dot(normal))<1e-7 }) candidates+=ArCadSnap(point-normal*(point-origin).dot(normal),ArCadSnapKind.Plane,"PLANE")
            }
            if(g.triangles.isNotEmpty()) SpatialAnalysisTools3D.project(point,g)?.let { candidates+=ArCadSnap(it,ArCadSnapKind.Surface,"SURFACE") }
        }
        val segments=geometry.filter { it.triangles.isEmpty() && it.lines.size==1 }.map { g -> g.vertices[g.lines[0].first] to g.vertices[g.lines[0].second] }
        segments.forEachIndexed { i,(a,b) -> segments.drop(i+1).forEach { (c,d) ->
            val u=b-a; val v=d-c; val w=a-c; val denom=u.dot(u)*v.dot(v)-u.dot(v)*u.dot(v)
            if(abs(denom)>1e-10) {
                val t=(u.dot(v)*v.dot(w)-v.dot(v)*u.dot(w))/denom
                val s=(u.dot(u)*v.dot(w)-u.dot(v)*u.dot(w))/denom
                val first=a+u*t; val second=c+v*s
                if(t in 0.0..1.0 && s in 0.0..1.0 && (first-second).magnitude()<1e-7) candidates+=ArCadSnap((first+second)*.5,ArCadSnapKind.Intersection,"INTERSECTION")
            }
        } }
        val planes=geometry.mapNotNull { g ->
            if(g.triangles.size<3) null else {
                val f=g.triangles.take(3); val origin=g.vertices[f[0]]; val normal=AnalyticGeometry3D.cross(g.vertices[f[1]]-origin,g.vertices[f[2]]-origin).normalized()
                if(normal.magnitude()>1e-9 && g.vertices.all { abs((it-origin).dot(normal))<1e-7 }) Triple(g,origin,normal) else null
            }
        }
        planes.forEachIndexed { i,(g,a,n) ->
            segments.forEach { (start,end) ->
                val direction=end-start; val denominator=direction.dot(n)
                if(abs(denominator)>1e-10) {
                    val t=(a-start).dot(n)/denominator; val hit=start+direction*t
                    if(t in 0.0..1.0 && SpatialAnalysisTools3D.project(hit,g)?.let { (it-hit).magnitude()<1e-6 }==true) candidates+=ArCadSnap(hit,ArCadSnapKind.Intersection,"LINE / PLANE INTERSECTION")
                }
            }
            planes.drop(i+1).forEach { (other,b,m) ->
                val direction=AnalyticGeometry3D.cross(n,m); val square=direction.dot(direction)
                if(square>1e-10) {
                    val origin=(AnalyticGeometry3D.cross(m,direction)*n.dot(a)+AnalyticGeometry3D.cross(direction,n)*m.dot(b))*(1.0/square)
                    val hit=origin+direction*((point-origin).dot(direction)/square)
                    if(SpatialAnalysisTools3D.project(hit,g)?.let { (it-hit).magnitude()<1e-6 }==true && SpatialAnalysisTools3D.project(hit,other)?.let { (it-hit).magnitude()<1e-6 }==true) candidates+=ArCadSnap(hit,ArCadSnapKind.Intersection,"PLANE / PLANE INTERSECTION")
                }
            }
        }
        val nearby=candidates.filter { (it.point-point).magnitude() <= tolerance }
        val discrete=nearby.filter { it.kind in setOf(ArCadSnapKind.Point,ArCadSnapKind.Vertex,ArCadSnapKind.Midpoint,ArCadSnapKind.Intersection) }
        return (discrete.ifEmpty { nearby }).minWithOrNull(compareBy<ArCadSnap> { (it.point-point).magnitude() }.thenBy { it.kind.ordinal })
    }
}
