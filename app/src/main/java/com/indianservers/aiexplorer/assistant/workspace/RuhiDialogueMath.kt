package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

object RuhiDialogueMath {
    private val num="[-+]?\\d+(?:\\.\\d+)?"
    fun edit(s:WorkspaceState,source:String,c:RuhiSceneContext):Pair<WorkspaceState,String>? {
        val text=source.removePrefix("actually, ").removePrefix("actually ").removePrefix("and ").removePrefix("now ")
        val dimension=Regex("(?:make (?:the )?|set (?:the )?|the )?(height|radius|side length|base)(?: should be| should equal| to| is)? ($num)").matchEntire(text)
        if(text=="duplicate the selected object" && s.module==MathModule.Geometry3D) {
            require(s.solids.size<24);val i=c.selectedSolidIndex?.takeIf{it in s.solids.indices} ?: throw IllegalArgumentException("Select one solid to duplicate.")
            val source=s.solids[i];val index=s.solids.size
            val solid=source.copy(position=source.position+Vec3(source.width+1,0.0,0.0));require(abs(solid.position.x)<=10000)
            return s.copy(solids=s.solids+solid,labSessionValues=s.labSessionValues+("ruhi.studio.selected.solid" to index.toString())+s.labSessionValues["ruhi.studio.appearance.$i"]?.let{mapOf("ruhi.studio.appearance.$index" to it)}.orEmpty()) to "Created an independent copy with the same dimensions and appearance, placed beside the original."
        }
        if(text=="duplicate the selected object" && s.module==MathModule.Geometry2D) {
            val shape=s.shapes.firstOrNull{it.id==c.selectedShapeId} ?: throw IllegalArgumentException("Select one shape to duplicate.")
            val points=shape.pointIndices.map(s.points::get);require(s.shapes.size<80&&s.points.size+points.size<=400)
            val shift=(points.maxOf{it.x}-points.minOf{it.x})+1
            val id=generateSequence(1){it+1}.map{"ruhi-shape-$it"}.first{id->s.shapes.none{it.id==id}}
            val newPoints=points.map{it+Vec2(shift,0.0)};require(newPoints.all{abs(it.x)<=10000&&abs(it.y)<=10000})
            return s.copy(points=s.points+newPoints,shapes=s.shapes+shape.copy(id=id,pointIndices=newPoints.indices.map{s.points.size+it},locked=false),labSessionValues=s.labSessionValues+("ruhi.studio.selected.shape" to id)) to "Created an independent copy beside the original, preserving its shape and appearance."
        }
        if(s.module==MathModule.Geometry3D) {
            if(text=="calculate the volume") {
                val solid=c.selectedSolidIndex?.takeIf{it in s.solids.indices}?.let{s.solids[it]} ?: return null
                val volume=Geometry3D.measure(solid).volume
                val formula=when(solid.type){SolidType.Cylinder->"πr²h = ${solid.radius*solid.radius*solid.height}π";SolidType.Cone->"πr²h/3 = ${solid.radius*solid.radius*solid.height/3}π";SolidType.Pyramid->"base area × height / 3";else->"the native ${solid.type.name.lowercase()} formula"}
                return s to "Volume = $formula ≈ ${"%.2f".format(java.util.Locale.US,volume)} cubic units."
            }
            if(dimension==null && text !in setOf("make it taller","make it transparent","make it solid","make it red","make it blue","make it green") && !text.startsWith("the base should be ") && !text.contains("same height as")) return null
            val index=c.selectedSolidIndex?.takeIf{it in s.solids.indices} ?: throw IllegalArgumentException("Select one solid first.")
            val old=s.solids[index];var solid=old
            if(text in setOf("make it transparent","make it solid") || Regex("make it (red|blue|green)").matches(text)) {
                val key="ruhi.studio.appearance.$index"
                val value=if(text.endsWith("transparent")) "glass" else if(text.endsWith("solid")) "solid" else text.substringAfterLast(' ')
                return s.copy(labSessionValues=s.labSessionValues+(key to value)) to "Updated the selected solid appearance."
            }
            val matches=if(text.startsWith("the base should be ")) Regex("the base should be ($num),? and make it taller").matchEntire(text) else null
            if(matches!=null) solid=solid.copy(width=matches.groupValues[1].toDouble(),depth=matches.groupValues[1].toDouble(),height=old.height+1)
            else if(text=="make it taller") solid=solid.copy(height=old.height+1)
            else if(text.contains("same height as")) return null
            else if(dimension!=null) {
                val value=dimension.groupValues[2].toDouble()
                solid=when(dimension.groupValues[1]) {
                    "height"->solid.copy(height=value)
                    "radius"->{require(old.type in setOf(SolidType.Cylinder,SolidType.Cone,SolidType.Sphere));solid.copy(radius=value,width=2*value,depth=2*value,height=if(old.type==SolidType.Sphere)2*value else solid.height)}
                    "base"->solid.copy(width=value,depth=value)
                    else->{require(old.type==SolidType.Cube);solid.copy(width=value,height=value,depth=value,radius=value/2)}
                }
            } else return null
            require(listOf(solid.width,solid.height,solid.depth,solid.radius).all{it.isFinite()&&it>0&&it<=50}) { "Use positive finite dimensions no larger than 50 units." }
            return s.copy(solids=s.solids.mapIndexed{i,v->if(i==index) solid else v}) to "Updated the existing ${solid.type.name.lowercase()}; height ${solid.height}, width ${solid.width}, radius ${solid.radius}. Taller means +1 unit."
        }
        if(s.module!=MathModule.Geometry2D) return null
        if(dimension==null && text !in setOf("make it equilateral","equilateral","make it isosceles","isosceles","make it a right triangle","right triangle","show the angles","show its angles")) return null
        val shape=s.shapes.firstOrNull{it.id==c.selectedShapeId} ?: throw IllegalArgumentException("Select one shape first.")
        val p=shape.pointIndices.map{s.points[it]}
        if(text in setOf("show the angles","show its angles")) {
            require(shape.type!=Shape2DType.Circle && p.size>=3)
            val angles=p.indices.map{i->val a=p[(i+p.size-1)%p.size]-p[i];val b=p[(i+1)%p.size]-p[i];Math.toDegrees(acos(((a.x*b.x+a.y*b.y)/(hypot(a.x,a.y)*hypot(b.x,b.y))).coerceIn(-1.0,1.0)))}
            require(angles.all(Double::isFinite));return s to "Interior angles: ${angles.joinToString { "%.2f°".format(java.util.Locale.US,it) }}."
        }
        require(!shape.locked && s.geometryConstraints.none{shape.id in it.shapeIds || it.pointIndices.any{n->n in shape.pointIndices}} && shape.pointIndices.none{n->s.pointDependencies.any{it.outputIndex==n}}) { "Use the construction tools for locked, constrained or dependent shapes." }
        val center=if(shape.type==Shape2DType.Circle) p.first() else p.reduce(Vec2::plus)*(1.0/p.size)
        val updated=if(text.contains("equilateral") || text.contains("isosceles") || text=="right triangle" || text.contains("right triangle")) {
            require(shape.type==Shape2DType.Triangle && p.size==3)
            val side=hypot(p[1].x-p[0].x,p[1].y-p[0].y)
            val height=if(text.contains("equilateral"))sqrt(3.0)*side/2 else side
            if(text.contains("right")) listOf(center+Vec2(-side/3,-side/3),center+Vec2(2*side/3,-side/3),center+Vec2(-side/3,2*side/3)) else listOf(center+Vec2(-side/2,-height/3),center+Vec2(side/2,-height/3),center+Vec2(0.0,2*height/3))
        } else {
            val value=dimension!!.groupValues[2].toDouble();require(value.isFinite()&&value>0&&value<=50)
            val old=if(shape.type==Shape2DType.Circle) hypot(p[1].x-p[0].x,p[1].y-p[0].y) else hypot(p[1].x-p[0].x,p[1].y-p[0].y)
            require(old>1e-9);p.map{center+(it-center)*(value/old)}
        }
        val points=s.points.toMutableList();shape.pointIndices.zip(updated).forEach{(i,v)->points[i]=v}
        return s.copy(points=points).recomputed() to "Updated the existing ${shape.type.name.lowercase()} without creating a duplicate."
    }
    fun analyze(s:WorkspaceState,intersection:Boolean,target:String?):Pair<WorkspaceState,String> {
        require(s.module==MathModule.Graph2D && !s.graph2DView.xLogarithmic && !s.graph2DView.yLogarithmic) { "Use the 2D linear graph for this analysis." }
        val rows=if(intersection) s.functions.takeLast(2) else listOfNotNull(s.functions.firstOrNull{it.id==target} ?: s.functions.singleOrNull())
        require(rows.size==if(intersection)2 else 1) { "Select one curve for roots, or create two curves for intersections." }
        val radius=7.0/s.graph2DView.zoom;val min=s.graph2DView.centerX-radius;val max=s.graph2DView.centerX+radius
        require(min.isFinite()&&max.isFinite()&&min<max&&max-min<=20000)
        val engine=RobustGraphAnalysisEngine()
        val points=if(intersection) engine.intersections(rows[0].expression,rows[1].expression,min,max) else engine.roots(rows[0].expression,min,max)
        val real=points.map{it.point}.filter{it.x.isFinite()&&it.y.isFinite()}.distinct().take(64)
        val encoded=s.functions.joinToString("\t"){it.expression}+"\n"+real.joinToString(";"){"${it.x},${it.y}"}
        return s.copy(labSessionValues=s.labSessionValues+("ruhi.dialogue.graph.markers" to encoded)) to "${if(intersection) "Intersections" else "Roots"} in the visible X domain [$min, $max]: ${real.joinToString { "(%.5f, %.5f)".format(java.util.Locale.US,it.x,it.y) }.ifEmpty{"none detected"}}. Numerical results are marked on the graph."
    }
    fun markers(s:WorkspaceState):List<Vec2> {
        val record=s.labSessionValues["ruhi.dialogue.graph.markers"]?.split('\n') ?: return emptyList()
        if(record.size!=2 || record[0]!=s.functions.joinToString("\t"){it.expression}) return emptyList()
        return record[1].split(';').mapNotNull{val v=it.split(',').mapNotNull(String::toDoubleOrNull);if(v.size==2&&v.all(Double::isFinite))Vec2(v[0],v[1]) else null}.take(64)
    }
}
