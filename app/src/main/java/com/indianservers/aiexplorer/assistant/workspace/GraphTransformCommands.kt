package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Deterministic graph edits, independent of the natural-language audit cases. */
object GraphTransformCommands {
    private const val number = "[-+]?\\d+(?:\\.\\d+)?"
    private fun scalar(s: String) = s.toDoubleOrNull()?.takeIf { it.isFinite() && abs(it)<=10000 } ?: throw IllegalArgumentException("Use a finite number between -10000 and 10000.")
    private fun vector(s: String): Vec3 { val v=s.split(',').map { scalar(it.trim()) }; require(v.size==3) { "Provide three numeric coordinates." }; return Vec3(v[0],v[1],v[2]) }
    fun parse(text: String): GraphAction? {
        Regex("(?:move|place|position|put) (.+?) (?:to|at) (?:the )?(?:origin|\\(([^)]+)\\))").matchEntire(text)?.let { return GraphAction(GraphIntent.MOVE,"absolute",it.groupValues[1],position=if(it.groupValues[2].isEmpty()) Vec3(0.0,0.0,0.0) else vector(it.groupValues[2])) }
        Regex("translate (.+?) by (?:the )?vector \\(([^)]+)\\)").matchEntire(text)?.let { return GraphAction(GraphIntent.MOVE,"relative",it.groupValues[1],position=vector(it.groupValues[2])) }
        Regex("(?:move|shift) (.+?) ($number) units? (right|left|upward|up|higher|downward|down|forward|backward)").matchEntire(text)?.let { return movement(it.groupValues[1],it.groupValues[3],scalar(it.groupValues[2])) }
        Regex("(?:move|shift) (.+?) (right|left|upward|up|higher|downward|down|forward|backward) by ($number) units?").matchEntire(text)?.let { return movement(it.groupValues[1],it.groupValues[2],scalar(it.groupValues[3])) }
        Regex("(?:position|put|place) (.+?) (above|on top of|next to|inside) (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.MOVE,"relation:${it.groupValues[2]}:${it.groupValues[3]}",it.groupValues[1]) }
        Regex("align (?:all )?objects along the ([xyz])-axis").matchEntire(text)?.let { return GraphAction(GraphIntent.MOVE,"align:${it.groupValues[1]}","all") }
        if(text=="move all objects to the positive x region") return GraphAction(GraphIntent.MOVE,"positiveX","all")
        Regex("(?:double (.+?)(?:'s size)|reduce (.+?) to half its size|make (.+?) twice as wide)").matchEntire(text)?.let {
            val g=it.groupValues;return GraphAction(GraphIntent.SCALE,if(g[3].isNotEmpty()) "x" else "uniform",g.drop(1).first(String::isNotEmpty),if(g[2].isNotEmpty()) .5 else 2.0)
        }
        Regex("scale (.+?) by ($number)").matchEntire(text)?.let { val v=scalar(it.groupValues[2]);require(v>0) { "Scale must be positive." };return GraphAction(GraphIntent.SCALE,"uniform",it.groupValues[1],v) }
        Regex("(?:set|change|increase) (.+?)(?:'s)? (side length|height|radius) to ($number)(?: units?)?").matchEntire(text)?.let { val v=scalar(it.groupValues[3]);require(v>0);return GraphAction(GraphIntent.SCALE,"dimension:${it.groupValues[2]}",it.groupValues[1],v) }
        Regex("resize (.+?) to ($number) [x×] ($number) [x×] ($number)").matchEntire(text)?.let { val v=Vec3(scalar(it.groupValues[2]),scalar(it.groupValues[3]),scalar(it.groupValues[4]));require(minOf(v.x,v.y,v.z)>0);return GraphAction(GraphIntent.SCALE,"dimensions",it.groupValues[1],position=v) }
        Regex("restore (.+?)(?:'s)? original size").matchEntire(text)?.let { return GraphAction(GraphIntent.SCALE,"reset",it.groupValues[1]) }
        if(text=="make all cubes the same size") return GraphAction(GraphIntent.SCALE,"same","all cubes")
        Regex("reset (.+?)(?:'s)? rotation").matchEntire(text)?.let { return GraphAction(GraphIntent.ROTATE,"reset",it.groupValues[1]) }
        Regex("align (.+?) with the ([xyz])-axis").matchEntire(text)?.let { return GraphAction(GraphIntent.ROTATE,"align:${it.groupValues[2]}",it.groupValues[1]) }
        Regex("turn (.+?) upside down").matchEntire(text)?.let { return GraphAction(GraphIntent.ROTATE,"x",it.groupValues[1],180.0) }
        Regex("rotate (.+?) (?:around|about) the ([xyz])-axis by ($number) degrees").matchEntire(text)?.let { return rotation(it.groupValues[1],it.groupValues[2],scalar(it.groupValues[3])) }
        Regex("rotate (.+?) clockwise by ($number) degrees").matchEntire(text)?.let { return rotation(it.groupValues[1],"z",-scalar(it.groupValues[2])) }
        Regex("(?:rotate|tilt) (.+?)(?: by)? ($number) degrees(?: (?:around|about) the ([xyz])-axis)?").matchEntire(text)?.let { return rotation(it.groupValues[1],it.groupValues[3].ifEmpty { if(text.startsWith("tilt")) "x" else "z" },scalar(it.groupValues[2])) }
        Regex("rotate (.+?) (?:by )?($number) degrees around (?:the )?(world origin|its own center)").matchEntire(text)?.let { return rotation(it.groupValues[1],if(it.groupValues[3]=="world origin") "world:z" else "z",scalar(it.groupValues[2])) }
        Regex("group (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.GROUP,target=it.groupValues[1]) }
        Regex("ungroup (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.UNGROUP,target=it.groupValues[1]) }
        Regex("mirror (.+?) across the ([xyz][xyz])-plane").matchEntire(text)?.let { return GraphAction(GraphIntent.MIRROR,it.groupValues[2],it.groupValues[1]) }
        if("infinitely" in text) throw IllegalArgumentException("An infinite scale is not drawable. Use a positive finite size.")
        return null
    }
    private fun movement(target:String,direction:String,n:Double) = GraphAction(GraphIntent.MOVE,"relative",target,position=when(direction) { "left"->Vec3(-n,0.0,0.0);"right"->Vec3(n,0.0,0.0);"up","upward","higher"->Vec3(0.0,0.0,n);"down","downward"->Vec3(0.0,0.0,-n);"forward"->Vec3(0.0,-n,0.0);else->Vec3(0.0,n,0.0) })
    private fun rotation(target:String,axis:String,n:Double): GraphAction { require(abs(n)<=360000) { "Rotation is limited to 360000 degrees per command." };return GraphAction(GraphIntent.ROTATE,axis,target,n) }
    val intents = setOf(GraphIntent.MOVE,GraphIntent.SCALE,GraphIntent.ROTATE,GraphIntent.GROUP,GraphIntent.UNGROUP,GraphIntent.MIRROR)
    fun apply(state: WorkspaceState, action: GraphAction, ids: Set<String>, resolve: (String)->Set<String>): WorkspaceState {
        require(state.module==MathModule.Graph3D) { "Object transforms currently require 3D Graph." }
        val chosen=state.surfaceLayers.filter { it.id in ids };require(chosen.isNotEmpty()) { "Select an object first." }
        fun bounds(l:SpatialSurfaceLayer,local:Boolean=false):Pair<Vec3,Vec3> { val v=GraphSurfaceGeometry.mesh(if(local) l.copy(graphTransform=GraphTransform()) else l,16).vertices;require(v.isNotEmpty());return Vec3(v.minOf{it.x},v.minOf{it.y},v.minOf{it.z}) to Vec3(v.maxOf{it.x},v.maxOf{it.y},v.maxOf{it.z}) }
        fun size(l:SpatialSurfaceLayer):Vec3 {
            val d=state.labSessionValues["ruhi.graph.dimension.${l.id}"]?.toDoubleOrNull()
            if(d!=null) when(state.labSessionValues[GraphAssistant.TYPE_PREFIX+l.id]) {
                "cube" -> return Vec3(d,d,d)
                "sphere" -> return Vec3(2*d,2*d,2*d)
                "cylinder","cone","pyramid" -> return Vec3(2.0,2.0,d)
                "rectangular prism" -> return Vec3(2*d,d,d)
            }
            val b=bounds(l,true);return b.second-b.first
        }
        fun center(l:SpatialSurfaceLayer):Vec3 { val b=bounds(l);return (b.first+b.second)*.5 }
        val pivot=chosen.map(::center).reduce(Vec3::plus)*(1.0/chosen.size)
        val group=if(action.intent==GraphIntent.GROUP) generateSequence(1){it+1}.map{"ruhi-group-$it"}.first { g->state.surfaceLayers.none{it.graphGroup==g} } else ""
        val reference=if(action.argument.startsWith("relation:")) state.surfaceLayers.first { it.id in resolve(action.argument.substringAfterLast(':')) } else null
        val firstSize=size(chosen.first());val firstScale=chosen.first().graphTransform.scale
        val layers=state.surfaceLayers.map { layer ->
            if(layer.id !in ids) layer else {
                var t=layer.graphTransform
                val local=size(layer)
                when(action.intent) {
                    GraphIntent.MOVE -> t=when {
                        action.argument=="absolute" -> t.copy(position=t.position+action.position-pivot)
                        action.argument=="positiveX" -> t.copy(position=t.position+Vec3(max(0.0,1-bounds(layer).first.x),0.0,0.0))
                        action.argument.startsWith("align:") -> t.copy(position=when(action.argument.last()) { 'x'->Vec3(t.position.x,0.0,0.0);'y'->Vec3(0.0,t.position.y,0.0);else->Vec3(0.0,0.0,t.position.z) })
                        reference!=null -> {
                            require(reference.id !in ids) { "Choose a different reference object." }
                            val a=bounds(layer);val b=bounds(reference);val c=center(reference)
                            val destination=when(action.argument.substringAfter(':').substringBefore(':')) { "above","on top of"->Vec3(c.x,c.y,b.second.z+(a.second.z-a.first.z)/2+.25);"next to"->Vec3(b.second.x+(a.second.x-a.first.x)/2+.25,c.y,c.z);else->c }
                            t.copy(position=t.position+destination-center(layer))
                        }
                        else -> t.copy(position=t.position+action.position)
                    }
                    GraphIntent.SCALE -> {
                        fun safe(v:Double) = v.takeIf { it.isFinite() && it in .001..1000.0 } ?: throw IllegalArgumentException("Resulting scale must be between 0.001 and 1000.")
                        val old=t.scale
                        val next=when(action.argument) {
                            "reset"->Vec3(1.0,1.0,1.0)
                            "dimensions"->Vec3(action.position.x/local.x,action.position.y/local.y,action.position.z/local.z)
                            "dimension:side length"->Vec3(action.amount!!/local.x,action.amount/local.y,action.amount/local.z)
                            "dimension:height"->old.copy(z=action.amount!!/local.z)
                            "dimension:radius"-> { val sx=action.amount!!*2/local.x;val sy=action.amount*2/local.y;Vec3(sx,sy,if(state.labSessionValues[GraphAssistant.TYPE_PREFIX+layer.id]=="sphere") action.amount*2/local.z else old.z) }
                            "same"->Vec3(firstSize.x*firstScale.x/local.x,firstSize.y*firstScale.y/local.y,firstSize.z*firstScale.z/local.z)
                            "x"->old.copy(x=old.x*action.amount!!)
                            else->old*action.amount!!
                        }
                        val checked=Vec3(safe(next.x),safe(next.y),safe(next.z))
                        // Keep the local object's center fixed while changing its size.
                        val b=bounds(layer,true);val c=(b.first+b.second)*.5
                        t=t.copy(scale=checked,position=t.position+t.direction(Vec3(c.x*(old.x-checked.x),c.y*(old.y-checked.y),c.z*(old.z-checked.z))))
                        if(chosen.size>1 && action.argument=="uniform") { val desired=pivot+(center(layer)-pivot)*action.amount!!; t=t.copy(position=t.position+desired-t.point(c)) }
                    }
                    GraphIntent.ROTATE -> t=when {
                        action.argument=="reset" -> t.copy(orientation=GraphTransform().orientation)
                        action.argument.startsWith("align:") -> t.copy(orientation=GraphTransform().rotated(if(action.argument.endsWith("x")) "y" else "x",if(action.argument.endsWith("z")) 0.0 else 90.0,Vec3(0.0,0.0,0.0)).orientation)
                        else -> t.rotated(action.argument.substringAfter(':'),action.amount!!,if(action.argument.startsWith("world:")) Vec3(0.0,0.0,0.0) else pivot)
                    }
                    GraphIntent.MIRROR -> { val axis="xyz".first { it !in action.argument };val m=GraphTransform(orientation=when(axis){'x'->listOf(-1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0);'y'->listOf(1.0,0.0,0.0,0.0,-1.0,0.0,0.0,0.0,1.0);else->listOf(1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,-1.0)});val o=List(9){i->(0..2).sumOf{k->m.orientation[i/3*3+k]*t.orientation[k*3+i%3]}};t=t.copy(position=m.direction(t.position),orientation=o) }
                    else -> Unit
                }
                if(action.intent==GraphIntent.ROTATE && (action.argument=="reset" || action.argument.startsWith("align:"))) {
                    val b=bounds(layer,true);val c=(b.first+b.second)*.5
                    t=t.copy(position=t.position+center(layer)-t.point(c))
                }
                val output=layer.copy(graphTransform=t,graphGroup=when(action.intent) { GraphIntent.GROUP->group;GraphIntent.UNGROUP->"";else->layer.graphGroup })
                require(listOf(t.position.x,t.position.y,t.position.z).all { abs(it)<=10000 }) { "Position exceeds the graph range budget." }
                output
            }
        }
        val radius = if(action.intent in setOf(GraphIntent.MOVE,GraphIntent.SCALE,GraphIntent.ROTATE,GraphIntent.MIRROR))
            layers.filter { it.visible }.flatMap { GraphSurfaceGeometry.mesh(it,8).vertices }.maxOfOrNull { it.magnitude() } ?: 3.0 else 3.0
        val zoom=minOf(state.graph3DView.zoom,(3.0/radius.coerceAtLeast(3.0)).coerceIn(.1,1.0).toFloat())
        return state.copy(surfaceLayers=layers,graph3DView=state.graph3DView.copy(zoom=zoom))
    }
}
