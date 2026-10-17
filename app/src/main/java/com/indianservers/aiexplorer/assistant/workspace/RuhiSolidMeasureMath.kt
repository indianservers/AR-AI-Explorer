package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Measurements use native solid dimensions; topology uses logical mesh faces, not triangulation faces. */
object RuhiSolidMeasureMath {
    data class Quantity(val name:String,val value:Double,val exact:String,val power:Int)
    data class Topology(val vertices:Int,val edges:Int,val faces:Int){val euler:Int get()=vertices-edges+faces}
    val polyhedra=setOf(SolidType.Cube,SolidType.Cuboid,SolidType.Pyramid,SolidType.Tetrahedron,SolidType.TriangularPyramid,SolidType.Octahedron,SolidType.TriangularPrism,SolidType.PentagonalPrism,SolidType.HexagonalPrism,SolidType.OctagonalPrism,SolidType.Wedge)
    fun topology(s:Solid):Topology {
        require(s.type in polyhedra){"Euler's polyhedron query needs a supported convex polyhedron with flat faces."}
        val mesh=SolidMeshFactory.create(s);require(mesh.faces.isNotEmpty())
        val edges=mutableMapOf<Pair<Int,Int>,Int>()
        mesh.faces.forEach{face->require(face.size>=3&&face.all{it in mesh.vertices.indices});face.indices.forEach{i->val a=face[i];val b=face[(i+1)%face.size];require(a!=b);val edge=min(a,b) to max(a,b);edges[edge]=(edges[edge] ?: 0)+1}}
        require(edges.values.all{it==2}){"The native mesh is not a closed two-face-per-edge polyhedron."}
        return Topology(mesh.vertices.size,edges.size,mesh.faces.size)
    }
    fun quantity(s:Solid,measure:String,baseArea:Double?=null):Quantity {
        require(listOf(s.width,s.height,s.depth,s.radius).all{it.isFinite()&&it>0&&it<=10000}){"Use positive finite dimensions within 10000 units."}
        baseArea?.let{require(it.isFinite()&&it>0&&it<=1e8)}
        val w=s.width;val h=s.height;val d=s.depth;val r=s.radius
        fun q(formula:String,power:Int):Quantity {
            val value=ExpressionEngine().compile(formula).eval(emptyMap());require(value.isFinite())
            val exact=runCatching{SymbolicCasEngine().simplify(formula).exact}.getOrNull()?.takeIf{runCatching{val actual=ExpressionEngine().compile(it).eval(emptyMap());actual.isFinite()&&abs(actual-value)<1e-8*max(1.0,abs(value))}.getOrDefault(false)} ?: formula
            return Quantity(measure,value,exact,power)
        }
        return when(measure) {
            "volume"->q(when(s.type){SolidType.Cube,SolidType.Cuboid->"$w*$h*$d";SolidType.Sphere->"4*pi*$r^3/3";SolidType.Hemisphere->"2*pi*$r^3/3";SolidType.Cylinder->"pi*$r^2*$h";SolidType.Cone->"pi*$r^2*$h/3";SolidType.Pyramid->"(${baseArea ?: w*d})*$h/3";else->Geometry3D.measure(s).volume.toString()},3)
            "surface_area"->q(when(s.type){SolidType.Cube,SolidType.Cuboid->"2*($w*$h+$w*$d+$h*$d)";SolidType.Sphere->"4*pi*$r^2";SolidType.Hemisphere->"3*pi*$r^2";SolidType.Cylinder->"2*pi*$r*($r+$h)";SolidType.Cone->"pi*$r*($r+sqrt($r^2+$h^2))";else->Geometry3D.measure(s).surfaceArea.toString()},2)
            "lateral_area"->q(when(s.type){SolidType.Cone->"pi*$r*sqrt($r^2+$h^2)";SolidType.Cylinder->"2*pi*$r*$h";else->error("Lateral area needs a supported cone or cylinder.")},2)
            "space_diagonal"->{require(s.type in setOf(SolidType.Cube,SolidType.Cuboid));q("sqrt($w^2+$h^2+$d^2)",1)}
            "total_edge_length"->{require(s.type in setOf(SolidType.Cube,SolidType.Cuboid));q("4*($w+$h+$d)",1)}
            "radius"->{require(s.type in setOf(SolidType.Sphere,SolidType.Hemisphere,SolidType.Cone,SolidType.Cylinder));q("$r",1)}
            "diameter"->{require(s.type in setOf(SolidType.Sphere,SolidType.Hemisphere,SolidType.Cone,SolidType.Cylinder));q("2*$r",1)}
            "height"->q("$h",1)
            "slant_height"->{require(s.type==SolidType.Cone);q("sqrt($r^2+$h^2)",1)}
            "base_area"->{require(s.type==SolidType.Cylinder||s.type==SolidType.Cone);q("pi*$r^2",2)}
            "topology"->{val t=topology(s);Quantity(measure,t.faces.toDouble(),"F=${t.faces}; E=${t.edges}; V=${t.vertices}",0)}
            "euler_characteristic"->{val t=topology(s);Quantity(measure,t.euler.toDouble(),"${t.vertices} - ${t.edges} + ${t.faces} = ${t.euler}",0)}
            else->error("Choose volume, surface/lateral/base area, a dimension, diagonal, edge total or topology.")
        }
    }
}
