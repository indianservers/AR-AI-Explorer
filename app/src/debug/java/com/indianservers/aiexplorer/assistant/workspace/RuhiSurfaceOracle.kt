package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Independent coordinate identities and topology gates, not formula-string-only success. */
object RuhiSurfaceOracle {
    fun expected(f:String,x:Double,y:Double):Double=when(f){
        "x^2+y^2"->x*x+y*y;"x^2-y^2"->x*x-y*y;"sin(x)*cos(y)"->sin(x)*cos(y);"2*x+y"->2*x+y
        "exp(-(x^2+y^2))"->exp(-(x*x+y*y));"sqrt(x^2+y^2)"->hypot(x,y);"x*y"->x*y
        "sin(sqrt(x^2+y^2))"->sin(hypot(x,y));"x^2/4+y^2/9"->x*x/4+y*y/9;"1/(1+x^2+y^2)"->1/(1+x*x+y*y)
        "exp(-x^2)*sin(y)"->exp(-x*x)*sin(y);"x^3-3*x*y^2"->x*x*x-3*x*y*y;"ln(x^2+y^2)"->ln(x*x+y*y)
        "cos(x)+cos(y)"->cos(x)+cos(y);"x^2-2*y^2"->x*x-2*y*y;"sqrt(9-x^2-y^2)"->sqrt(9-x*x-y*y)
        "abs(x)+abs(y)"->abs(x)+abs(y);"sin(x*y)"->sin(x*y);"x/y"->x/y;"sin(x)*sin(y)"->sin(x)*sin(y)
        "-sqrt(x^2+y^2)"->-hypot(x,y);"(x^2-y^2)/(x^2+y^2)"->(x*x-y*y)/(x*x+y*y);"5"->5.0;"4"->4.0
        else->error("No independent surface identity for $f")
    }
    fun triangles(m:SurfaceMesh):List<List<Vec3>> {
        val indices=if(m.triangleIndices.isNotEmpty())m.triangleIndices else buildList{for(r in 0 until m.rows-1)for(c in 0 until m.columns-1){val i=r*m.columns+c;addAll(listOf(i,i+m.columns,i+1,i+1,i+m.columns,i+m.columns+1))}}
        require(indices.all{it in m.vertices.indices});return indices.chunked(3).filter{it.size==3}.map{it.map(m.vertices::get)}
    }
    fun check(p:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,why:String){if(!ok)errors+=why}
        fun same(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{same(it.first,it.second)};else->a==b}
        need(r.status==GraphCommandStatus.APPLIED,"Status ${r.status}: ${r.message}");need(r.semanticFrame?.intent=="PLOT_SURFACE_3D","Missing surface intent")
        p.forEach{(k,v)->need(same(v,r.semanticFrame?.parameters?.get(k)),"Slot $k expected $v got ${r.semanticFrame?.parameters?.get(k)}")}
        val original=before.surfaceLayers.associateBy{it.id};val layers=r.state.surfaceLayers.filter{it.id !in original};val formulas=(p["expressions"] as? List<*>)?.map{it.toString()} ?: listOf(p["expression"].toString())
        need(layers.size==formulas.size,"Wrong number of created layers");need(before.surfaceLayers.all{old->r.state.surfaceLayers.singleOrNull{it.id==old.id}==old},"Existing surface changed")
        need(before.functions==r.state.functions&&before.solids==r.state.solids&&before.points==r.state.points&&before.shapes==r.state.shapes&&before.points3D==r.state.points3D&&before.graph3DView==r.state.graph3DView,"Unrelated objects or camera changed")
        layers.zip(formulas).forEach{(layer,f)->runCatching {
            need(layer.visible&&layer.kind==SpatialSurfaceKind.Explicit,"Wrong native layer kind/visibility");need(layer.expression==f,"Native expression differs from original source slots")
            need(layer.renderMode==if(p["style"]=="wireframe")SpatialSurfaceRenderMode.Wireframe else SpatialSurfaceRenderMode.SurfaceMesh,"Wrong render style")
            (p["x_domain"] as? List<*>)?.let{need(same(it,listOf(layer.domain.uMin,layer.domain.uMax)),"Wrong x domain")};(p["y_domain"] as? List<*>)?.let{need(same(it,listOf(layer.domain.vMin,layer.domain.vMax)),"Wrong y domain")}
            for(density in listOf(13,24)){
                val m=GraphSurfaceGeometry.mesh(layer,density);need(m.vertices.isNotEmpty(),"No drawable vertices");val faces=triangles(m);need(faces.isNotEmpty(),"No actual surface topology")
                m.vertices.forEach{v->val wanted=expected(f,v.x,v.y);need(v.x.isFinite()&&v.y.isFinite()&&v.z.isFinite()&&wanted.isFinite()&&abs(v.z-wanted)<=1e-7*max(1.0,abs(wanted)),"Vertex does not satisfy original equation: $v");need(v.x>=layer.domain.uMin-1e-9&&v.x<=layer.domain.uMax+1e-9&&v.y>=layer.domain.vMin-1e-9&&v.y<=layer.domain.vMax+1e-9,"Vertex escaped native domain")}
                if(f=="x/y")faces.forEach{tri->need(!(tri.minOf{it.y}<0&&tri.maxOf{it.y}>0),"Triangle bridges the y=0 singularity")}
                if(f in setOf("ln(x^2+y^2)","(x^2-y^2)/(x^2+y^2)"))faces.forEach{tri->need(!(tri.minOf{it.x}<0&&tri.maxOf{it.x}>0&&tri.minOf{it.y}<0&&tri.maxOf{it.y}>0),"Triangle covers the undefined origin")}
                if(p["domain"]!=null){need(m.vertices.all{it.x*it.x+it.y*it.y<=9+1e-8&&it.z>=0},"Hemisphere has invented outside-disk geometry");need(m.vertices.filter{it.z<1e-5&&abs(it.x*it.x+it.y*it.y-9)<1e-7}.distinctBy{round(it.x*1e6).toLong() to round(it.y*1e6).toLong()}.size>=12,"Hemisphere rim is truncated instead of following its circular boundary")}
                if(p["x_domain"]!=null)need(abs(m.vertices.minOf{it.x}-layer.domain.uMin)<1e-8&&abs(m.vertices.maxOf{it.x}-layer.domain.uMax)<1e-8&&abs(m.vertices.minOf{it.y}-layer.domain.vMin)<1e-8&&abs(m.vertices.maxOf{it.y}-layer.domain.vMax)<1e-8,"Requested rectangle is incomplete")
            }
        }.onFailure{errors+=it.message ?: "Surface oracle failed"}}
        need(r.state.labSessionValues[GraphAssistant.SELECTION] in layers.map{it.id},"Created surface is not selected")
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
