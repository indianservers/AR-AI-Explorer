package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Source-parameter checks shared by JVM and device; UNKNOWN never means a passing behavior. */
object RuhiSeedBehaviorOracle {
    data class Check(val verified:Boolean,val diagnostics:List<String>)
    fun check(intent:String,parameters:Map<String,Any?>,before:WorkspaceState,result:RuhiExecutionResult,context:RuhiSceneContext):Check {
        if(intent=="DIALOGUE_CONTEXT")return RuhiDialogueSeedOracle.check(parameters,before,result,context)
        if(intent=="EDIT_SCENE")return RuhiSceneEditOracle.check(parameters,before,result,context)
        if(intent=="TRANSFORM_3D")return RuhiTransform3DOracle.check(parameters,before,result,context)
        if(intent=="MEASURE_SOLID")return RuhiSolidMeasureOracle.check(parameters,before,result,context)
        if(intent=="PLOT_SURFACE_3D")return RuhiSurfaceOracle.check(parameters,before,result,context)
        if(intent=="INTEGRATE")return RuhiIntegralOracle.check(parameters,before,result,context)
        if(intent=="DIFFERENTIATE")return RuhiDerivativeOracle.check(parameters,before,result,context)
        if(intent=="FIND_INTERSECTION")return RuhiIntersectionOracle.check(parameters,before,result,context)
        if(intent=="PLOT_FUNCTION_2D")return RuhiFunctionOracle.check(parameters,before,result,context)
        if(intent=="CREATE_TRIANGLE")return RuhiTriangleOracle.check(parameters,before,result,context)
        if(intent=="CREATE_CIRCLE")return RuhiCircleOracle.check(parameters,before,result,context)
        if(intent=="MEASURE_ANGLE")return angleCheck(parameters,before,result,context)
        if(intent !in setOf("MEASURE_DISTANCE","FIND_MIDPOINT","CALCULATE_SLOPE"))return Check(false,listOf("No complete behavior oracle yet for $intent."))
        val errors=mutableListOf<String>()
        fun need(ok:Boolean,message:String){if(!ok)errors+=message}
        val frame=result.semanticFrame
        need(result.status==GraphCommandStatus.APPLIED,"Status ${result.status}: ${result.message}")
        need(frame?.intent==intent,"Missing/wrong semantic intent.")
        parameters.forEach{(key,value)->need(matches(value,frame?.parameters?.get(key)),"Slot $key expected $value, got ${frame?.parameters?.get(key)}")}
        fun point(name:String):Vec2?=before.labSessionValues["ruhi.studio.point.$name"]?.toIntOrNull()?.let{before.points.getOrNull(it)}
        fun coords(value:Any?):Vec2?=(value as? List<*>)?.map{(it as? Number)?.toDouble()}?.let{if(it.size==2&&it.all{v->v!=null})Vec2(it[0]!!,it[1]!!) else null}
        fun target():Shape2D? {
            val lines=before.shapes.filter{it.type in setOf(Shape2DType.Segment,Shape2DType.Line,Shape2DType.Ray)&&it.pointIndices.size==2}
            return when(parameters["target"]){"last_segment","last_line"->lines.lastOrNull();"blue_segment"->lines.singleOrNull{it.styleKey.equals("#0000FF",true)||it.styleKey=="blue"};"red_line"->lines.singleOrNull{it.styleKey.equals("#FF0000",true)||it.styleKey=="red"};else->lines.singleOrNull{it.id==context.selectedShapeId}}
        }
        if(intent=="CALCULATE_SLOPE") {
            val pairs=parameters["pairs"] as? List<*>
            val supplied=parameters["points"] as? List<*>
            val expectedPairs=when {
                pairs!=null->pairs.map{value->val names=value as? List<*>;names?.getOrNull(0)?.let{point(it.toString())} to names?.getOrNull(1)?.let{point(it.toString())}}
                supplied!=null->listOf(coords(supplied.getOrNull(0)) to coords(supplied.getOrNull(1)))
                parameters["expression"]!=null->emptyList()
                else->listOf((parameters["a"]?.let{point(it.toString())} ?: target()?.pointIndices?.firstOrNull()?.let{before.points.getOrNull(it)}) to (parameters["b"]?.let{point(it.toString())} ?: target()?.pointIndices?.getOrNull(1)?.let{before.points.getOrNull(it)}))
            }
            val slopes=Regex("(?i)slope = (undefined|[-+]?[0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?)").findAll(result.message).map{it.groupValues[1]}.toList()
            if(parameters["expression"]!=null) {
                val f=ExpressionEngine().compile(parameters["expression"].toString());val expected=f.eval(mapOf("x" to 1.0))-f.eval(mapOf("x" to 0.0))
                need(slopes.firstOrNull()?.toDoubleOrNull()?.let{abs(it-expected)<1e-9}==true,"Wrong line-expression slope.")
            } else {
                need(expectedPairs.size==slopes.size,"Wrong number of compared slopes.")
                expectedPairs.forEachIndexed{i,(a,b)->
                    need(a!=null&&b!=null,"Slope oracle lacks endpoints.")
                    if(a!=null&&b!=null){val dx=b.x-a.x;val dy=b.y-a.y;if(dx==0.0)need(slopes.getOrNull(i)=="undefined","Vertical line needs undefined slope.")else need(slopes.getOrNull(i)?.toDoubleOrNull()?.let{abs(it-dy/dx)<1e-9}==true,"Wrong rise/run slope.")}
                }
            }
            need(before.points==result.state.points&&before.shapes==result.state.shapes&&before.solids==result.state.solids&&before.functions==result.state.functions,"Slope changed source geometry.")
            if(parameters["format"]=="fraction")need(Regex("Fraction = -?[0-9]+/[0-9]+").containsMatchIn(result.message),"Fraction representation missing.")
            if(parameters["visualize"]==true||parameters["annotate"]==true) {
                val marks=RuhiSlopeLanguage.marks(result.state);need(marks.isNotEmpty(),"Visible slope annotation was not stored.")
                if(parameters["visualize"]==true)need(marks.any{it.triangle},"Slope triangle not requested in renderer state.")
                val a=parameters["a"]?.let{before.labSessionValues["ruhi.studio.point.$it"]?.toIntOrNull()} ?: target()?.pointIndices?.firstOrNull()
                val b=parameters["b"]?.let{before.labSessionValues["ruhi.studio.point.$it"]?.toIntOrNull()} ?: target()?.pointIndices?.getOrNull(1)
                need(marks.any{it.first==a&&it.second==b},"Annotation references the wrong endpoints.")
            }
            return Check(errors.isEmpty(),errors)
        }
        val a=if(parameters["aCoords"]!=null)coords(parameters["aCoords"])else (parameters["point1"] ?: parameters["a"])?.let{point(it.toString())} ?: target()?.pointIndices?.firstOrNull()?.let{before.points.getOrNull(it)}
        val b=if(parameters["bCoords"]!=null)coords(parameters["bCoords"])else (parameters["point2"] ?: parameters["b"])?.let{point(it.toString())} ?: target()?.pointIndices?.getOrNull(1)?.let{before.points.getOrNull(it)}
        need(a!=null&&b!=null,"Oracle fixture lacks endpoints.")
        if(a!=null&&b!=null)when(intent){
            "MEASURE_DISTANCE"->{val value=Regex("= ([0-9.eE+-]+) units").find(result.message)?.groupValues?.get(1)?.toDoubleOrNull();need(value!=null&&abs(value-a.distanceTo(b))<1e-9,"Wrong Euclidean distance.");need(before.points==result.state.points&&before.shapes==result.state.shapes&&before.solids==result.state.solids,"Read-only distance changed geometry.")}
            "FIND_MIDPOINT"->{
                val expected=Vec2((a.x+b.x)/2,(a.y+b.y)/2);val state=result.state
                need(state.points.size==before.points.size+1,"Midpoint must add one marker only.");need(state.points.take(before.points.size)==before.points,"Existing coordinates were overwritten.")
                need(state.points.lastOrNull()?.distanceTo(expected)?.let{it<1e-9}==true,"Wrong midpoint coordinates.")
                val index=state.points.lastIndex;val labels=state.labSessionValues.filterKeys{it.startsWith("ruhi.studio.point.")}.filterValues{it==index.toString()}.keys.map{it.removePrefix("ruhi.studio.point.")}
                need(labels.size==1,"Marker must have one persistent name.");parameters["label"]?.let{need(labels.singleOrNull()==it,"Custom marker name was lost.")}
                if(parameters["aCoords"]==null)need(state.pointDependencies.any{it.outputIndex==index&&it.type==PointDependencyType.Midpoint&&it.inputIndices.size==2},"Named midpoint must follow both endpoints.")
                need(state.solids==before.solids&&state.functions==before.functions&&state.surfaceLayers==before.surfaceLayers,"Construction changed another studio.")
                if(parameters["show_segment"]==false)need(state.shapes==before.shapes,"Unexpected full line was drawn.")
                if(frame?.parameters?.get("split")==true){val pieces=state.shapes.drop(before.shapes.size);need(pieces.size==2&&pieces.all{it.type==Shape2DType.Segment&&index in it.pointIndices},"Split must create two segments joined at the midpoint.")}
            }
        }
        return Check(errors.isEmpty(),errors)
    }
    private fun angleCheck(parameters:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,context:RuhiSceneContext):Check {
        if(parameters["planes"]=="selected_pair"&&parameters["kind"]=="dihedral")return RuhiPlaneAngleOracle.check(parameters,before,r,context)
        if(parameters["vectors"]=="selected_3d_pair")return RuhiSpatialAngleOracle.check(parameters,before,r,context)
        if(parameters["lines"]!=null||parameters["reference"]!=null||parameters["vectors"] is List<*>||parameters["target"]=="last_two_segments"||(parameters["points"] as? List<*>)?.size==2)return RuhiReferenceAngleOracle.check(parameters,before,r)
        val names=parameters["points"] as? List<*>
        val triangle=parameters["triangle"] as? String
        val rays=parameters["rays"] as? List<*>
        val selected=parameters["target"] in setOf("selected_vertex","selected_angle","selected_triangle","selected_quadrilateral")
        if(names?.size!=3&&triangle==null&&rays==null&&parameters["vertex"]==null&&!selected)return Check(false,listOf("Angle context/construction oracle not implemented for this request."))
        val errors=mutableListOf<String>();fun need(ok:Boolean,message:String){if(!ok)errors+=message}
        need(r.status==GraphCommandStatus.APPLIED,"Angle status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="MEASURE_ANGLE","Missing angle semantic frame.")
        parameters.forEach{(k,v)->need(matches(v,r.semanticFrame?.parameters?.get(k)),"Angle slot $k differs from source.")}
        val registry=before.labSessionValues.filterKeys{it.startsWith(StudioReferenceMetadata.POINT_PREFIX)}.mapKeys{it.key.removePrefix(StudioReferenceMetadata.POINT_PREFIX)}.mapValues{it.value.toIntOrNull()}
        fun ray(token:String):List<Int>? {
            val candidates=registry.keys.flatMap{a->registry.keys.mapNotNull{b->if(a+b==token)listOfNotNull(registry[a],registry[b]).takeIf{it.size==2}else null}}.distinct()
            return candidates.singleOrNull()
        }
        val vertex=parameters["vertex"]?.toString()?.let{registry[it]} ?: if(parameters["target"] in setOf("selected_vertex","selected_angle"))context.selectedPointIndex else null
        val shape=if(triangle!=null)before.shapes.singleOrNull{it.name==triangle&&it.pointIndices.size==3}
            else if(selected)before.shapes.singleOrNull{it.id==context.selectedShapeId}
            else if(vertex!=null)before.shapes.singleOrNull{it.type==Shape2DType.Triangle&&vertex in it.pointIndices}else null
        fun corner(ids:List<Int>,i:Int)=listOf(ids[(i+ids.size-1)%ids.size],ids[i],ids[(i+1)%ids.size])
        fun interior(ids:List<Int>,i:Int):Double {
            val p=corner(ids,i).map{before.points[it]};val u=p[0]-p[1];val v=p[2]-p[1]
            val minor=Math.toDegrees(acos(((u.x*v.x+u.y*v.y)/(hypot(u.x,u.y)*hypot(v.x,v.y))).coerceIn(-1.0,1.0)))
            val area=ids.indices.sumOf{j->val a=before.points[ids[j]];val b=before.points[ids[(j+1)%ids.size]];a.x*b.y-a.y*b.x}
            return if((u.x*v.y-u.y*v.x)*area>0)360-minor else minor
        }
        val expected=if(names?.size==3)names.map{registry[it.toString()]} else if(rays?.size==2) {
            val a=ray(rays[0].toString());val b=ray(rays[1].toString());if(a!=null&&b!=null&&a[0]==b[0])listOf(a[1],a[0],b[1])else emptyList()
        } else {
            if(shape==null){need(false,"Oracle triangle missing.");emptyList()}else {
                val ids=shape.pointIndices
                if(vertex!=null){val i=ids.indexOf(vertex);if(i<0)emptyList()else corner(ids,i)}
                else if(parameters["right_angle"]==true) {
                    ids.indices.singleOrNull{abs(interior(ids,it)-90)<1e-7}?.let{corner(ids,it)} ?: emptyList()
                }else if(parameters["select"] in setOf("largest","smallest")) {
                    val i=if(parameters["select"]=="largest")ids.indices.maxBy{interior(ids,it)}else ids.indices.minBy{interior(ids,it)}
                    corner(ids,i)
                }else emptyList()
            }
        }
        val triples=if(parameters["all"]==true&&shape!=null)shape.pointIndices.let{ids->ids.indices.map{i->listOf(ids[(i+ids.size-1)%ids.size],ids[i],ids[(i+1)%ids.size])}}else listOf(expected)
        need(triples.isNotEmpty()&&triples.all{it.size==3&&it.all{v->v!=null}},"Oracle needs three uniquely named points per angle.")
        val marks=RuhiAngleLanguage.marks(r.state)
        need(marks.map{it.indices}==triples,"Wrong angle marker endpoints/vertex.")
        val answers=Regex("= ([0-9.eE+-]+) degrees").findAll(r.message).map{it.groupValues[1].toDouble()}.toList()
        need(answers.size==triples.size,"Wrong number of angle answers.")
        triples.forEachIndexed{index,ids->if(ids.size==3&&ids.all{it!=null}) {
            val p=ids.map{before.points[it!!]};val u=p[0]-p[1];val v=p[2]-p[1];val denominator=hypot(u.x,u.y)*hypot(v.x,v.y)
            val minor=if(shape!=null)interior(shape.pointIndices,shape.pointIndices.indexOf(ids[1]))else Math.toDegrees(acos(((u.x*v.x+u.y*v.y)/denominator).coerceIn(-1.0,1.0)))
            val degrees=when{parameters["exterior"]==true->180-minor;parameters["oriented"]==true&&(u.x*v.y-u.y*v.x)<0->360-minor;else->minor}
            val answer=answers.getOrNull(index)
            need(answer!=null&&abs(answer-degrees)<1e-7,"Wrong independently calculated angle.")
            need(marks.getOrNull(index)?.exterior==(parameters["exterior"]==true),"Wrong exterior arc mode.")
            need(marks.getOrNull(index)?.oriented==(parameters["oriented"]==true),"Wrong oriented arc mode.")
            val compare=(parameters["compare"] as? Number)?.toDouble()
            if(compare!=null)need(r.message.contains(if(abs(degrees-compare)<1e-7)"(equals " else "(does not equal "),"Wrong angle comparison.")
            if(parameters["classify"]==true)need(r.message.contains("(${when{abs(degrees-90)<1e-7->"right";abs(degrees-180)<1e-7->"straight";degrees>180->"reflex";degrees>90->"obtuse";degrees>0->"acute";else->"zero"}})"),"Wrong angle classification.")
            if(shape!=null)need(marks.getOrNull(index)?.polygon==shape.pointIndices,"Live polygon context was lost.")
        }}
        if(parameters["construction"]=="bisector") {
            val added=r.state.shapes.drop(before.shapes.size)
            need(added.size==1&&added[0].type==Shape2DType.AngleBisector&&added[0].pointIndices==expected,"Missing or incorrect native angle-bisector construction.")
            need(r.state.shapes.take(before.shapes.size)==before.shapes,"Bisector altered existing shapes.")
        }else need(before.shapes==r.state.shapes,"Angle measurement changed shapes.")
        need(before.points==r.state.points&&before.solids==r.state.solids&&before.points3D==r.state.points3D,"Angle measurement changed geometry.")
        return Check(errors.isEmpty(),errors)
    }
    private fun matches(a:Any?,b:Any?):Boolean=when {
        a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9
        a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{matches(it.first,it.second)}
        a is Map<*,*>&&b is Map<*,*>->a.all{(k,v)->matches(v,b[k])}
        else->a==b
    }
}

/** Strict JSON reader for debug seed data; no Android stub methods or new runtime dependency. */
object RuhiSeedJson {
    fun objectValue(text:String):Map<String,Any?> {
        val reader=Reader(text);val value=reader.value();reader.ws();require(reader.i==text.length)
        @Suppress("UNCHECKED_CAST") return value as? Map<String,Any?> ?: error("Expected an object.")
    }
    private class Reader(val s:String) {
        var i=0
        fun ws(){while(i<s.length&&s[i].isWhitespace())i++}
        fun value():Any? {ws();require(i<s.length);return when(s[i]){
            '{'->{i++;val m=linkedMapOf<String,Any?>();ws();if(i<s.length&&s[i]=='}'){i++;m}else{while(true){ws();val k=string();ws();require(s[i++]==':');require(!m.containsKey(k));m[k]=value();ws();val c=s[i++];if(c=='}')break;require(c==',')};m}}
            '['->{i++;val a=mutableListOf<Any?>();ws();if(i<s.length&&s[i]==']'){i++;a}else{while(true){a+=value();ws();val c=s[i++];if(c==']')break;require(c==',')};a}}
            '"'->string()
            't'->{require(s.startsWith("true",i));i+=4;true}
            'f'->{require(s.startsWith("false",i));i+=5;false}
            'n'->{require(s.startsWith("null",i));i+=4;null}
            else->{val m=Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?").find(s,i);require(m!=null&&m.range.first==i);i=m.range.last+1;m.value.toDouble().also{require(it.isFinite())}}
        }}
        fun string():String {require(s[i++]=='"');val out=StringBuilder();while(i<s.length){val c=s[i++];if(c=='"')return out.toString();if(c=='\\'){val e=s[i++];out.append(when(e){'"','\\','/'->e;'b'->'\b';'f'->'\u000c';'n'->'\n';'r'->'\r';'t'->'\t';'u'->s.substring(i,i+4).toInt(16).toChar().also{i+=4};else->error("Bad JSON escape")})}else{require(c.code>=32);out.append(c)}};error("Unterminated JSON string")}
    }
}
