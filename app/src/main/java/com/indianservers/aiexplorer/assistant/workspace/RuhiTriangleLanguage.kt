package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Validated triangle constraints construct native points; the NLP head only proposes intents. */
object RuhiTriangleLanguage {
    fun accepts(t:String)=t.length<=2000&&Regex("(?i)\\btriangle\\b").containsMatchIn(t)
    fun measures(p:List<Vec2>):Pair<List<Double>,List<Double>> {
        require(p.size==3);val sides=p.indices.map{p[it].distanceTo(p[(it+1)%3])}
        require(sides.all{it.isFinite()&&it>1e-9&&it<=1000}){"Use three distinct points and sides no greater than 1000."}
        val cross=(p[1].x-p[0].x)*(p[2].y-p[0].y)-(p[1].y-p[0].y)*(p[2].x-p[0].x)
        require(abs(cross)>1e-10*sides.max().pow(2)){"Three non-collinear points are required."}
        val angles=p.indices.map{i->val u=p[(i+1)%3]-p[i];val v=p[(i+2)%3]-p[i];Math.toDegrees(acos(((u.x*v.x+u.y*v.y)/(hypot(u.x,u.y)*hypot(v.x,v.y))).coerceIn(-1.0,1.0)))}
        return sides to angles
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry2D||!accepts(source))return null
        val t=source.trim().trimEnd('.','?').replace('−','-');val low=t.lowercase()
        if(low.startsWith("show ")&&!Regex("^show (?:a|an) triangle\\b").containsMatchIn(low))return null
        if(!Regex("(?i)^(draw|make|construct|create|build|show|place)\\b").containsMatchIn(t))return null
        if(Regex("(?i)\\b(?:circumcircle|incircle|bisector|midpoint|slope|angle of|angle in|then|after that)\\b").containsMatchIn(t))return null
        if(Regex("(?i)\\b(?:bigger|smaller|rotate the|move the|and (?:move|rotate|color|delete))\\b").containsMatchIn(t))return null
        var frame=RuhiSemanticFrame("CREATE_TRIANGLE",RuhiEngine.GEOMETRY_2D,emptyMap())
        try {
            val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}";val num="[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?"
            val points=s.points.toMutableList();val deps=s.pointDependencies.toMutableList();var values=s.labSessionValues
            val resolver=RuhiEndpointResolver(s,c);var params:Map<String,Any> = emptyMap();var note="";var modified:Shape2D?=null
            fun number(v:String):Double=(v.toDoubleOrNull() ?: RuhiMathLanguage.normalize(v).toDoubleOrNull() ?: throw IllegalArgumentException("Use numeric triangle dimensions.")).also{require(it.isFinite()){"Use finite triangle dimensions."}}
            fun dimension(v:Double)=v.also{require(it>0&&it<=500){"Use positive triangle dimensions no greater than 500."}}
            fun scalar(key:String):Double?=Regex("(?i)\\b$key\\s+(?:of\\s+)?([^\\s,;]+)").find(t)?.groupValues?.get(1)?.let(::number)
            fun add(p:Vec2,inputs:List<Int> = emptyList(),kind:PointDependencyType?=null,args:List<Double> = emptyList(),name:String?=null):Int {
                require(points.size<200&&listOf(p.x,p.y).all{it.isFinite()&&abs(it)<=10000}){"Triangle point budget exceeded."}
                if(name!=null)require(RuhiEndpointResolver.references(s).none{it.first.equals(name,true)}){"Point $name already exists; use it by name or choose a new label."}
                val i=points.size;points+=p;if(kind!=null)deps+=PointDependency(i,inputs,kind,name ?: "P${i+1}",args)
                val actual=name ?: generateSequence(1){it+1}.map{"T${it}"}.first{n->values.keys.none{it.equals("ruhi.studio.point.$n",true)}}
                values=values+("ruhi.studio.point.$actual" to i.toString());return i
            }
            fun existing(token:String):Shape2D {
                val triangles=s.shapes.filter{it.type==Shape2DType.Triangle&&it.pointIndices.size==3}
                return triangles.singleOrNull{it.name.equals(token,true)} ?: throw RuhiReferenceQuestion("Choose an existing triangle named $token.")
            }
            fun vertex(a:Int,b:Int,u:Double,v:Double):Int {val edge=points[b]-points[a];return add(points[a]+edge*u+Vec2(-edge.y,edge.x)*v,listOf(a,b),PointDependencyType.TriangleVertex,listOf(u,v))}
            fun names(token:String):List<String> {
                val refs=RuhiEndpointResolver.references(s)
                val sets=refs.flatMap{a->refs.flatMap{b->refs.filter{d->(a.first+b.first+d.first).equals(token,true)}.map{d->listOf(a.first,b.first,d.first)}}}.filter{it.distinct().size==3}.distinct()
                if(sets.size==1)return sets.single()
                if(token.length==3&&token.all{it.isLetter()})return token.map(Char::toString)
                throw RuhiReferenceQuestion("Give three point names separated by commas.")
            }
            val coords=Regex("($label)\\s*=?\\s*\\(([^()]*)\\)").findAll(t).toList()
            val explicit=Regex("(?i)(?:defined by points|through points|through the points)\\s+($label)\\s*,\\s*($label)\\s*,?\\s*(?:and\\s+)?($label)$").find(t)
            var indices:List<Int> = emptyList()
            if(coords.isNotEmpty()) {
                require(coords.size==3){"Supply exactly three named coordinate pairs."}
                val labels=coords.map{it.groupValues[1]};require(labels.distinctBy{it.lowercase()}.size==3){"Use distinct vertex names."}
                val p=coords.map{match->val xy=match.groupValues[2].split(',').map{number(it.trim())};require(xy.size==2);Vec2(xy[0],xy[1])};measures(p)
                params=mapOf("coords" to p.map{listOf(it.x,it.y)})+if(low.contains("triangle "))mapOf("labels" to labels)else emptyMap()
                indices=p.mapIndexed{i,v->add(v,name=labels[i])}
            } else if(explicit!=null||low.contains("through the existing points")) {
                val labels=explicit?.groupValues?.drop(1) ?: names(Regex("(?i)\\btriangle ($label)\\b").find(t)?.groupValues?.get(1) ?: throw IllegalArgumentException("Name the triangle vertices."))
                indices=labels.map{requireNotNull(resolver.point(it).index)};params=mapOf("vertices" to labels)
            } else if(low.contains("current triangle")||low.contains("selected triangle")) {
                require(low.contains("equilateral")){"Specify the triangle modification."}
                modified=s.shapes.singleOrNull{it.id==c.selectedShapeId&&it.type==Shape2DType.Triangle} ?: throw RuhiReferenceQuestion("Select one triangle first.")
                require(modified.pointIndices.size==3);val a=modified.pointIndices[0];val b=modified.pointIndices[1];measures(modified.pointIndices.map{s.points[it]})
                indices=listOf(a,b,vertex(a,b,.5,sqrt(3.0)/2));params=mapOf("target" to "selected_triangle","kind" to "equilateral","action" to "modify");note="Kept the first side and original parent points."
            } else if(low.contains("congruent")||low.contains("similar to")) {
                val congruent=low.contains("congruent");val ref=if(congruent)s.shapes.firstOrNull{it.type==Shape2DType.Triangle} ?: throw RuhiReferenceQuestion("Create a triangle first.")else existing(Regex("(?i)similar to ($label)").find(t)!!.groupValues[1])
                val p=ref.pointIndices.map{s.points[it]};measures(p);val scale=if(congruent)1.0 else scalar("scale") ?: throw RuhiReferenceQuestion("What positive similarity scale should I use?")
                dimension(scale);val offset=Vec2(p.maxOf{it.x}-p.minOf{it.x}+2,0.0)
                indices=ref.pointIndices.map{i->if(congruent)add(s.points[i]+offset,listOf(i),PointDependencyType.Translate,listOf(offset.x,offset.y))else add(s.points[i]*scale,listOf(i),PointDependencyType.Dilate,listOf(scale,0.0,0.0))}
                params=if(congruent)mapOf("source" to "first_triangle","kind" to "congruent")else mapOf("source" to ref.name,"scale" to scale);note=if(congruent)"Translated the copy to the right; it follows its parent vertices."else"Scaled about the origin; the copy follows its parent vertices."
            } else if(Regex("(?i)side ($label) fixed").containsMatchIn(t)) {
                val match=Regex("(?i)side ($label) fixed at ([^\\s]+)").find(t) ?: throw IllegalArgumentException("Supply a positive fixed side length.")
                val pair=resolver.segment(match.groupValues[1]);val length=dimension(number(match.groupValues[2]));require(abs(pair.first.position.distanceTo(pair.second.position)-length)<1e-8){"The existing side has a different length. Resize it to $length first, or use new point names."}
                val a=requireNotNull(pair.first.index);val b=requireNotNull(pair.second.index);indices=listOf(a,b,vertex(a,b,.5,sqrt(3.0)/2));params=mapOf("fixed_side" to match.groupValues[1],"length" to length);note="Used the existing side; default equilateral third vertex."
            } else {
                var kind=listOf("equilateral","isosceles","acute","obtuse","scalene","right").firstOrNull{Regex("\\b$it\\b").containsMatchIn(low)}
                val pyth=low.contains("pythagoras")||low.contains("pythagorean");if(pyth)kind="right"
                val sides=Regex("(?i)\\bsides ($num)\\s*,\\s*($num)\\s*,?\\s*(?:and\\s+)?($num)").find(t)?.groupValues?.drop(1)?.map{dimension(number(it))}
                val legs=Regex("(?i)\\blegs ([^\\s,]+) (?:and|,) ([^\\s,]+)").find(t)?.groupValues?.drop(1)?.map{dimension(number(it))}
                val angles=Regex("(?i)interior angles ($num)\\s*,\\s*($num)\\s*,?\\s*(?:and\\s+)?($num)(?: degrees)?$").find(t)?.groupValues?.drop(1)?.map(::number)
                val atAngle=Regex("(?i)([^\\s]+)-degree angle at ($label)").find(t)
                val base=scalar("base")?.let(::dimension);val height=scalar("height")?.let(::dimension);val side=scalar("side")?.let(::dimension);val equal=scalar("equal sides")?.let(::dimension);val hyp=scalar("hypotenuse")?.let(::dimension);val leg=scalar("one leg")?.let(::dimension)
                require(!low.contains("sides")||equal!=null||sides!=null){"Supply three numeric side lengths, or a positive equal-side length."}
                require(listOf(sides!=null,angles!=null,atAngle!=null,hyp!=null||leg!=null,legs!=null,base!=null||height!=null,side!=null||equal!=null).count{it}<=1){"Use one consistent triangle constraint family at a time."}
                require(kind!="equilateral"||base==null||height==null||abs(height-sqrt(3.0)*base/2)<1e-8){"The supplied height conflicts with an equilateral triangle."}
                params=kind?.let{mapOf("kind" to it)}.orEmpty()
                require(side==null||kind=="equilateral"){"Name which side to constrain, or request an equilateral triangle."}
                var p:List<Vec2>
                when {
                    sides!=null->{val a=sides[0];val b=sides[1];val cc=sides[2];require(a+b>cc&&a+cc>b&&b+cc>a){"Triangle sides must satisfy the strict triangle inequality."};val x=(a*a+cc*cc-b*b)/(2*a);p=listOf(Vec2(0.0,0.0),Vec2(a,0.0),Vec2(x,sqrt(max(0.0,cc*cc-x*x))));params=params+mapOf("sides" to sides)}
                    angles!=null->{require(angles.all{it>0&&it<180}&&abs(angles.sum()-180)<1e-7){"Three positive interior angles must total 180 degrees."};val a=Math.toRadians(angles[0]);val b=Math.toRadians(angles[1]);val len=4*sin(b)/sin(a+b);p=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(len*cos(a),len*sin(a)));params=params+mapOf("angles" to angles);note="Using default baseline length 4."}
                    atAngle!=null->{val angle=number(atAngle.groupValues[1]);require(angle>0&&angle<180){"Use an interior angle between 0 and 180 degrees."};val name=atAngle.groupValues[2];val anchor=runCatching{resolver.point(name)}.getOrElse{if(it is RuhiReferenceQuestion&&it.missing.isNotEmpty())throw it else throw it};val a=Math.toRadians(angle);val ai=requireNotNull(anchor.index);val bi=add(anchor.position+Vec2(4.0,0.0));indices=listOf(ai,bi,vertex(ai,bi,cos(a),sin(a)));params=params+mapOf("angle_at_$name" to angle);p=emptyList();note="Using two default sides of length 4."}
                    hyp!=null||leg!=null->{require(kind=="right"&&hyp!=null&&leg!=null&&hyp>leg){"The hypotenuse must be longer than the positive leg."};p=listOf(Vec2(0.0,0.0),Vec2(leg,0.0),Vec2(0.0,sqrt(hyp*hyp-leg*leg)));params=params+mapOf("hypotenuse" to hyp,"leg" to leg)}
                    legs!=null||kind=="right"->{val v=legs ?: listOf(3.0,4.0);p=listOf(Vec2(0.0,0.0),Vec2(v[0],0.0),Vec2(0.0,v[1]));if(legs!=null)params=params+mapOf("legs" to legs)else note="Using default legs 3 and 4.";if(pyth)params=params+mapOf("demo" to "pythagorean")}
                    base!=null||height!=null->{require(base!=null&&height!=null){"Supply both the base and perpendicular height."};p=listOf(Vec2(0.0,0.0),Vec2(base,0.0),Vec2(base/2,height));params=params+mapOf("base" to base,"height" to height)}
                    kind=="equilateral"||equal!=null->{val len=equal ?: side ?: 4.0;dimension(len);p=listOf(Vec2(0.0,0.0),Vec2(len,0.0),Vec2(len/2,sqrt(3.0)*len/2));params=params+if(equal!=null)mapOf("equal_sides" to equal)else if(side!=null)mapOf("side" to side)else emptyMap();note=if(equal!=null)"Using a default base equal to the specified equal sides."else if(side==null)"Using default side length 4."else""}
                    kind=="obtuse"-> {p=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(1.0,1.0));note="Using default vertices (0,0), (4,0), (1,1)."}
                    kind=="scalene"->{p=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(0.0,3.0));note="Using default sides 4, 5 and 3."}
                    else->{p=if(kind=="acute")listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(2.0,sqrt(12.0)))else listOf(Vec2(0.0,0.0),Vec2(2.0,0.0),Vec2(1.0,sqrt(3.0)));note="Using default vertices ${p.joinToString{v->"(${v.x},${v.y})"}}."}
                }
                if(atAngle==null) {
                    if(low.contains("centered on the origin")){val center=p.reduce{a,b->a+b}*(1.0/3.0);p=p.map{it-center};params=params+mapOf("center" to listOf(0,0))}
                    val rotation=Regex("(?i)rotated ([^\\s]+) degrees").find(t)?.groupValues?.get(1)?.let(::number)
                    if(rotation!=null){val center=p.reduce{a,b->a+b}*(1.0/3.0);val a=Math.toRadians(rotation%360);p=p.map{v->val d=v-center;center+Vec2(d.x*cos(a)-d.y*sin(a),d.x*sin(a)+d.y*cos(a))};params=params+mapOf("rotation_degrees" to rotation);note+=" Rotated around its centroid."}
                    if(low.contains("above the x-axis")){val dy=1-p.minOf{it.y};p=p.map{it+Vec2(0.0,dy)};params=params+mapOf("placement" to "above_x");note+=" Lowest vertex is one unit above the X-axis."}
                    measures(p);indices=p.map{add(it)}
                }
            }
            val shapeName=generateSequence(1){it+1}.map{"Triangle$it"}.first{n->s.shapes.none{it.name.equals(n,true)}}
            val output=modified?.copy(pointIndices=indices) ?: Shape2D("ruhi-triangle-${java.util.UUID.randomUUID()}",Shape2DType.Triangle,indices,shapeName)
            val next=s.copy(points=points,pointDependencies=deps,shapes=if(modified==null)s.shapes+output else s.shapes.map{if(it.id==modified.id)output else it},labSessionValues=values).recomputed();require(next.shapes.size<=200)
            val (lengths,angles)=measures(indices.map{next.points[it]})
            val kind=params["kind"];val equal={a:Double,b:Double->abs(a-b)<1e-8*max(1.0,a)}
            require(when(kind){"equilateral"->lengths.all{equal(it,lengths[0])};"isosceles"->equal(lengths[0],lengths[1])||equal(lengths[1],lengths[2])||equal(lengths[0],lengths[2]);"right"->angles.any{abs(it-90)<1e-7};"acute"->angles.all{it<90-1e-7};"obtuse"->angles.any{it>90+1e-7};"scalene"->!equal(lengths[0],lengths[1])&&!equal(lengths[1],lengths[2])&&!equal(lengths[0],lengths[2]);else->true}){"Triangle constraints conflict with the requested kind."}
            frame=frame.copy(parameters=params)
            val labels=RuhiEndpointResolver.references(next).associate{it.second to it.first}
            val summary="${output.name}: vertices ${indices.joinToString{labels[it] ?: "P${it+1}"}}; lengths ${lengths.joinToString()}; angles ${angles.joinToString()} degrees. $note"
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: triangle construction"))
            val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            return RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,summary,RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c.copy(selectedShapeId=output.id),semanticFrame=frame)
        }catch(e:RuhiReferenceQuestion){return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Choose triangle references.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,e.missing.map{RuhiAnswerChip("Create point $it","Create point $it at ")}),semanticFrame=frame)}
        catch(e:IllegalArgumentException){return RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid triangle constraints.",context=c,semanticFrame=frame)}
    }
}
