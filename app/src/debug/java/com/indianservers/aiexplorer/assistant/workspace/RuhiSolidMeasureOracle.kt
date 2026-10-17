package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Independent source-slot formulas; never reads expected values from the produced annotation. */
object RuhiSolidMeasureOracle {
    fun check(p:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(v:Boolean,message:String){if(!v)errors+=message}
        fun same(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{same(it.first,it.second)};else->a==b}
        need(r.status==GraphCommandStatus.APPLIED,"Status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="MEASURE_SOLID","Missing measurement intent")
        p.forEach{(k,v)->need(same(v,r.semanticFrame?.parameters?.get(k)),"Original slot $k differs: expected $v got ${r.semanticFrame?.parameters?.get(k)}")}
        need(before.solids==r.state.solids&&before.points==r.state.points&&before.points3D==r.state.points3D&&before.functions==r.state.functions&&before.surfaceLayers==r.state.surfaceLayers&&before.graph3DView==r.state.graph3DView,"Measurement mutated geometry or camera")
        runCatching {
            val d=requireNotNull(RuhiSolidMeasureLanguage.drawing(r.state));need(d.diagnostic.isEmpty(),d.diagnostic)
            val names=(p["targets"] as? List<*>)?.map{it.toString()} ?: listOf((p["shape"] ?: p["target"].toString().removePrefix("selected_")).toString())
            val measures=(p["measures"] as? List<*>)?.map{it.toString()} ?: listOf(p["measure"].toString())
            need(names.size==d.entities.size,"Wrong number of measured solids")
            val expectedVolumes=mutableListOf<Double>()
            names.zip(d.entities).forEach{(kind,e)->
                val literal=p["shape"]!=null
                val target=if(literal)null else before.solids.indices.filter{kind=="convex_polyhedron"||before.solids[it].type.name.lowercase()==kind}.let{candidates->c.selectedSolidIndex?.takeIf{it in candidates} ?: candidates.single()}
                need(e.index==target,"Wrong scene reference")
                val native=target?.let{before.solids[it]}
                fun n(key:String,default:Double)= (p[key] as? Number)?.toDouble() ?: default
                val dims=(p["dimensions"] as? List<*>)?.map{(it as Number).toDouble()}
                val w=dims?.get(0) ?: n("side",native?.width ?: 1.0);val h=dims?.get(1) ?: n("height",native?.height ?: w);val depth=dims?.get(2) ?: (native?.depth ?: w);val radius=n("radius",native?.radius ?: .5)
                need(e.quantities.map{it.name}==measures,"Wrong requested quantities")
                e.quantities.zip(measures).forEach{(q,m)->
                    val expected=when(m){
                        "volume"->when(kind){"cube","cuboid"->w*h*depth;"sphere"->4*PI*radius*radius*radius/3;"hemisphere"->2*PI*radius*radius*radius/3;"cylinder"->PI*radius*radius*h;"cone"->PI*radius*radius*h/3;"pyramid"->n("base_area",w*depth)*h/3;else->error(kind)}
                        "surface_area"->when(kind){"cube","cuboid"->2*(w*h+w*depth+h*depth);"sphere"->4*PI*radius*radius;"cylinder"->2*PI*radius*(radius+h);"cone"->PI*radius*(radius+hypot(radius,h));else->error(kind)}
                        "lateral_area"->PI*radius*hypot(radius,h)
                        "space_diagonal"->sqrt(w*w+h*h+depth*depth)
                        "total_edge_length"->4*(w+h+depth)
                        "height"->h;"radius"->radius;"diameter"->2*radius;"slant_height"->hypot(radius,h);"base_area"->PI*radius*radius
                        "topology"->6.0;"euler_characteristic"->2.0;else->error(m)
                    }
                    need(abs(q.value-expected)<1e-8*max(1.0,abs(expected)),"Independent $m expected $expected got ${q.value}")
                    val power=when(m){"volume"->3;"surface_area","lateral_area","base_area"->2;"topology","euler_characteristic"->0;else->1}
                    need(q.power==power,"Incorrect dimensional units")
                    if(power>0){val exact=ExpressionEngine().compile(q.exact).eval(emptyMap());need(abs(exact-expected)<1e-7*max(1.0,abs(expected)),"Exact formula disagrees with source dimensions");need(r.message.contains(RuhiSolidMeasureLanguage.units(power)),"Missing units")}
                    if(m=="topology")need(q.exact=="F=6; E=12; V=8","Logical cube topology differs")
                    if(m=="euler_characteristic")need(q.exact=="8 - 12 + 6 = 2","Wrong native Euler counts")
                    if(m=="volume")expectedVolumes+=expected
                }
                need(e.segments.isNotEmpty(),"No dimension annotations")
                need(e.segments.all{seg->listOf(seg.a,seg.b).all{it.x.isFinite()&&it.y.isFinite()&&it.z.isFinite()}},"Invalid dimension coordinates")
            }
            if(p["compare"]==true){need(abs(requireNotNull(d.difference)-(expectedVolumes[1]-expectedVolumes[0]))<1e-8,"Wrong comparison difference");need(abs(requireNotNull(d.ratio)-expectedVolumes[1]/expectedVolumes[0])<1e-8,"Wrong comparison ratio")}
        }.onFailure{errors+=it.message ?: "Measurement oracle failed"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
