package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Circle recipes use native controls/dependencies. NLP never supplies an approximate formula or pixels. */
object RuhiCircleLanguage {
    data class Circle(val center:Vec2,val radius:Double)
    fun isCircle(shape:Shape2D)=shape.type in setOf(Shape2DType.Circle,Shape2DType.CircleThreePoints)
    fun describe(s:WorkspaceState,shape:Shape2D):Circle {
        require(isCircle(shape)&&shape.pointIndices.all{it in s.points.indices}){"Choose a valid circle."}
        val p=shape.pointIndices.map{s.points[it]}
        val center=if(shape.type==Shape2DType.Circle){require(p.size==2);p[0]}else {require(p.size==3);Geometry2D.circumcenter(p[0],p[1],p[2]) ?: throw IllegalArgumentException("Three non-collinear points are required for a circle.")}
        val radius=if(shape.type==Shape2DType.Circle)center.distanceTo(p[1])else center.distanceTo(p[0])
        require(listOf(center.x,center.y,radius).all{it.isFinite()}&&radius>0&&radius<=500){"Use a positive circle radius no greater than 500."}
        return Circle(center,radius)
    }
    fun accepts(text:String)=text.length<=2000&&Regex("(?i)\\b(?:circle|circles|circumcircle|incircle)\\b").containsMatchIn(text)
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry2D||!accepts(source))return null
        val text=source.trim().trimEnd('.','?').replace('−','-').replace("²","^2");val low=text.lowercase()
        // Preserve generic measurement, appearance and transformation workflows.
        if(Regex("(?i)^(?:calculate|measure|explain|select|delete|rotate|scale|move|color|paint)\\b").containsMatchIn(text))return null
        if(!Regex("(?i)^(?:draw|create|make|place|add|put|construct|replace|circle)\\b").containsMatchIn(text)&&!low.startsWith("show the circumcircle"))return null
        if(Regex("(?i)\\b(?:bigger|smaller|larger|twice as large)\\b").containsMatchIn(text))return null
        if(Regex("(?i)\\b(?:then|after that)\\b|\\band (?:make|color|move|rotate|delete|scale|create|draw) (?:it|the|a|this)\\b").containsMatchIn(text))return null
        val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
        var frame=RuhiSemanticFrame("CREATE_CIRCLE",RuhiEngine.GEOMETRY_2D,emptyMap())
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        try {
            val resolver=RuhiEndpointResolver(s,c);val circles=s.shapes.filter(::isCircle)
            val points=s.points.toMutableList();val dependencies=s.pointDependencies.toMutableList();val shapes=s.shapes.toMutableList();val created=mutableListOf<Shape2D>()
            var params:Map<String,Any> = emptyMap();var note="";var modified:Shape2D?=null
            fun point(p:Vec2,inputs:List<Int> = emptyList(),type:PointDependencyType?=null,args:List<Double> = emptyList()):Int {
                require(points.size<200&&listOf(p.x,p.y).all{it.isFinite()&&abs(it)<=10000}){"Circle controls exceed the 200-point or coordinate budget."}
                val i=points.size;points+=p;if(type!=null)dependencies+=PointDependency(i,inputs,type,"P${i+1}",args);return i
            }
            fun freshName():String=generateSequence(1){it+1}.map{"C$it"}.first{candidate->(shapes+created).none{it.name.equals(candidate,true)}}
            fun add(center:Int,rim:Int,name:String=freshName()):Shape2D {require(shapes.size+created.size<200){"The shape budget is full."};return Shape2D("ruhi-circle-${java.util.UUID.randomUUID()}",Shape2DType.Circle,listOf(center,rim),name).also{created+=it}}
            fun fixed(center:Vec2,radius:Double,name:String=freshName()):Shape2D {require(radius.isFinite()&&radius>0&&radius<=500){"Use a positive circle radius no greater than 500."};val i=point(center);return add(i,point(center+Vec2(radius,0.0)),name)}
            fun selected():Shape2D=circles.singleOrNull{it.id==c.selectedShapeId} ?: circles.singleOrNull() ?: throw RuhiReferenceQuestion("Select one circle first.")
            fun controls(shape:Shape2D):Pair<Int,Int> {
                val d=describe(s,shape)
                return if(shape.type==Shape2DType.Circle)shape.pointIndices[0] to shape.pointIndices[1]else point(d.center,shape.pointIndices,PointDependencyType.Circumcenter) to shape.pointIndices[0]
            }
            fun triangle(token:String?):Shape2D {
                if(token==null)return s.shapes.singleOrNull{it.id==c.selectedShapeId&&it.type==Shape2DType.Triangle&&it.pointIndices.size==3} ?: throw RuhiReferenceQuestion("Select a triangle first.")
                val byName=s.shapes.filter{it.type==Shape2DType.Triangle&&it.pointIndices.size==3&&it.name.equals(token,true)}
                if(byName.size==1)return byName.single()
                val refs=RuhiEndpointResolver.references(s)
                val sets=refs.flatMap{a->refs.flatMap{b->refs.filter{d->(a.first+b.first+d.first).equals(token,true)}.map{d->setOf(a.second,b.second,d.second)}}}.filter{it.size==3}.distinct()
                return s.shapes.filter{it.type==Shape2DType.Triangle&&it.pointIndices.size==3&&it.pointIndices.toSet() in sets}.singleOrNull() ?: throw RuhiReferenceQuestion("Create or uniquely select triangle $token first.")
            }
            fun scalar(value:String):Double=(value.toDoubleOrNull() ?: runCatching{RuhiMathLanguage.normalize(value).toDoubleOrNull()}.getOrNull() ?: throw IllegalArgumentException("Use a numeric circle dimension.")).also{require(it.isFinite()){"Use a finite circle dimension."}}
            val radiusToken=Regex("(?i)\\bradius(?:\\s+(?:of|with|=))?\\s+([^\\s,;]+)").find(text)?.groupValues?.get(1)
            val diameterToken=Regex("(?i)\\bdiameter\\s+([^\\s,;]+)").find(text)?.groupValues?.get(1)
            val named=Regex("(?i)\\bcircle ($label) (?:at|with|centered)\\b").find(text)?.groupValues?.get(1)?.takeUnless{it.lowercase() in setOf("centered","centred","tangent","passing","with","at","of")}
            val tuples=Regex("\\(([^()]*)\\)").findAll(text).toList();require(tuples.size<=1){"Provide one center coordinate pair, or name the three points the circle passes through."}
            val tuple=tuples.firstOrNull()?.groupValues?.get(1)?.let{t->val values=t.split(',').map{it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Use two numeric center coordinates.")};require(values.size==2&&values.all{it.isFinite()&&abs(it)<=10000});Vec2(values[0],values[1])}
            val centerMatch=if(tuple==null)Regex("(?i)\\bcentered (?:on|at) (point )?($label)\\b").find(text)else null
            val centerName=centerMatch?.groupValues?.get(2)?.takeUnless{centerMatch.groupValues[1].isEmpty()&&(it.equals("the",true)||it.equals("origin",true))}
            val through3=Regex("(?i)\\bthrough ($label)\\s*,\\s*($label)\\s*,?\\s*(?:and\\s+)?($label)$").find(text)
            val triangleToken=Regex("(?i)\\btriangle ($label)$").find(text)?.groupValues?.get(1)
            val equation=Regex("(?i)\\bcircle\\s+(x\\s*\\^2\\s*\\+\\s*y\\s*\\^2\\s*=\\s*([^\\s]+))$").find(text)
            when {
                low.contains("selected circle")&&low.startsWith("replace ")->{
                    val old=selected();val d=describe(s,old);val radius=scalar(radiusToken ?: throw IllegalArgumentException("Specify the replacement radius."));require(radius>0&&radius<=500)
                    val center=if(old.type==Shape2DType.Circle)old.pointIndices[0]else point(d.center,old.pointIndices,PointDependencyType.Circumcenter)
                    val rim=point(d.center+Vec2(radius,0.0),listOf(center),PointDependencyType.Translate,listOf(radius,0.0))
                    modified=old.copy(type=Shape2DType.Circle,pointIndices=listOf(center,rim));shapes[shapes.indexOfFirst{it.id==old.id}]=modified!!;params=mapOf("target" to "selected_circle","radius" to radius,"action" to "update")
                }
                Regex("\\b(?:two|2) (?:externally tangent )?circles\\b").containsMatchIn(low)->{
                    val tangent=low.contains("externally tangent");require(tangent||low.contains("intersect")){"Specify how the two circles should relate."}
                    val first=fixed(Vec2(0.0,0.0),1.0);val i=first.pointIndices[0];val j=first.pointIndices[1];val factor=if(tangent)2.0 else 1.0
                    val k=point(Vec2(factor,0.0),listOf(i,j),PointDependencyType.CircleOffset,listOf(factor));val l=point(Vec2(factor+1,0.0),listOf(i,j,k),PointDependencyType.CircleRadius,listOf(1.0));add(k,l)
                    params=mapOf("count" to 2,"relation" to if(tangent)"externally_tangent"else "intersect");note="Using unit radii; the second circle follows the first circle's controls."
                }
                low.contains("inside the big")||low.contains("concentric")||low.contains("radius equal to the first")->{
                    val inside=low.contains("inside");val copy=low.contains("first");val reference=when{inside->{val sizes=circles.map{it to describe(s,it).radius};val largest=sizes.maxOfOrNull{it.second} ?: throw RuhiReferenceQuestion("Create a larger circle first.");sizes.filter{abs(it.second-largest)<1e-10}.singleOrNull()?.first ?: throw RuhiReferenceQuestion("Select a unique largest circle first.")};copy->circles.firstOrNull() ?: throw RuhiReferenceQuestion("Create the first circle before copying its radius.");else->selected()}
                    val d=describe(s,reference);val (i,j)=controls(reference)
                    val requested=if(!copy&&radiusToken!=null)scalar(radiusToken)else null
                    if(!inside&&!copy&&!low.contains("twice")&&requested==null)throw RuhiReferenceQuestion("What radius should the concentric circle have? Give a positive radius or ask for twice the radius.")
                    if(requested!=null){require(requested>0&&requested<=500);if(inside)require(requested<d.radius){"The inner circle radius must be smaller than the outer radius."};if(low.contains("twice"))require(abs(requested-d.radius*2)<1e-9){"The explicit radius conflicts with twice the reference radius."}}
                    val factor=when{inside->.5;copy->1.0;else->2.0};val radius=requested ?: d.radius*factor;require(radius<=500)
                    val center=if(copy)point(Vec2(0.0,0.0))else i
                    val rim=if(requested==null)point(points[center]+Vec2(radius,0.0),listOf(i,j,center),PointDependencyType.CircleRadius,listOf(factor))else point(points[center]+Vec2(radius,0.0),listOf(center),PointDependencyType.Translate,listOf(radius,0.0));add(center,rim)
                    params=when{inside->mapOf("relation" to "inside","target" to "largest_circle");copy->mapOf("radius_ref" to "first_circle");else->mapOf("relation" to "concentric","scale" to 2)};note=if(inside)"Using half the largest circle's radius and the same center."else if(copy)"Using default center (0,0) and the first circle's live radius."else "Using the selected circle's center and twice its live radius."
                    if(requested!=null){params=params.filterKeys{it!="scale"}+mapOf("radius" to requested);note="Using the reference circle's center and explicit radius $requested."}
                }
                through3!=null||low.contains("circumcircle")||low.contains("incircle")||low.contains("around the selected triangle")->{
                    val incircle=low.contains("incircle")
                    val refs=if(through3!=null)through3.groupValues.drop(1).map{resolver.point(it)}else triangle(triangleToken).pointIndices.map{i->val name=RuhiEndpointResolver.references(s).firstOrNull{it.second==i}?.first ?: "P${i+1}";RuhiEndpoint(name,s.points[i],i)}
                    val ids=refs.map{it.index!!};require(ids.distinct().size==3);require(Geometry2D.circumcenter(refs[0].position,refs[1].position,refs[2].position)!=null){"Three non-collinear points are required for this circle."}
                    if(incircle) {
                        val center=Geometry2D.incenter(refs[0].position,refs[1].position,refs[2].position) ?: throw IllegalArgumentException("Triangle is degenerate.");val i=point(center,ids,PointDependencyType.Incenter)
                        val foot=resolvePointDependency(points,listOf(ids[0],ids[1],i),PointDependencyType.PointOnObject) ?: throw IllegalArgumentException("Triangle side is degenerate.")
                        add(i,point(foot,listOf(ids[0],ids[1],i),PointDependencyType.PointOnObject))
                    }else {val shape=Shape2D("ruhi-circle-${java.util.UUID.randomUUID()}",Shape2DType.CircleThreePoints,ids,freshName());created+=shape}
                    params=if(through3!=null)mapOf("through" to refs.map{it.name})else (if(triangleToken==null)mapOf("target" to "selected_triangle")else mapOf("triangle" to triangleToken))+mapOf("kind" to if(incircle)"incircle"else "circumcircle")
                }
                equation!=null->{val rhs=scalar(equation.groupValues[2]);require(rhs>0){"The circle equation requires a positive right-hand side."};fixed(Vec2(0.0,0.0),sqrt(rhs));params=mapOf("equation" to equation.groupValues[1].replace(" ",""))}
                else->{
                    require(!Regex("\\bcircles\\b").containsMatchIn(low)){"Specify two intersecting or externally tangent circles for this construction."}
                    require(!text.contains('=')){"Use the circle equation x^2+y^2 = a positive constant."}
                    val diameterNumber=diameterToken?.let{runCatching{scalar(it)}.getOrNull()}
                    if(radiusToken!=null&&diameterNumber!=null)require(abs(scalar(radiusToken)*2-diameterNumber)<1e-9){"The radius and diameter describe different circles."}
                    if(low.contains("unit circle")&&radiusToken!=null)require(scalar(radiusToken)==1.0){"A unit circle has radius 1."}
                    if(diameterToken!=null&&diameterNumber==null) {
                        val pair=resolver.segment(diameterToken);val a=pair.first;val b=pair.second;val center=(a.position+b.position)*.5;require(a.position.distanceTo(b.position)>0)
                        val i=point(center,listOf(a.index!!,b.index!!),PointDependencyType.Midpoint);add(i,a.index);params=mapOf("diameter" to listOf(a.name,b.name))
                    }else {
                        val centerPoint=centerName?.let{resolver.point(it)}
                        val through=Regex("(?i)\\bthrough (?:point )?($label)\\b").find(text)?.groupValues?.get(1)?.let{resolver.point(it)}
                        val bare=Regex("(?i)^(?:draw|create|make|add) (?:a |the )?(?:(?:red|blue|green|yellow|purple|orange|pink) )?circle$").matches(text)
                        require(bare||tuple!=null||centerPoint!=null||through!=null||radiusToken!=null||diameterToken!=null||named!=null||low.contains("origin")||low.contains("unit circle")){"Specify a circle center, radius or supported geometric construction."}
                        val center=tuple ?: centerPoint?.position ?: Vec2(0.0,0.0)
                        val existing=named?.let{name->circles.filter{it.name.equals(name,true)}.also{if(it.size>1)throw RuhiReferenceQuestion("Circle $name is ambiguous.")}.singleOrNull()}
                        val radius=when{low.contains("unit circle")->1.0;diameterNumber!=null->diameterNumber/2;radiusToken!=null->scalar(radiusToken);through!=null->center.distanceTo(through.position);low.contains("tangent to the x-axis")->abs(center.y);existing!=null->describe(s,existing).radius;else->1.0}
                        require(radius.isFinite()&&radius>0&&radius<=500){"Use a positive circle radius no greater than 500."}
                        val i=centerPoint?.index ?: point(center)
                        val axisTangent=low.contains("tangent to the x-axis")
                        if(axisTangent&&radiusToken!=null)require(abs(radius-abs(center.y))<1e-9){"The supplied radius does not make this circle tangent to the X-axis."}
                        val j=through?.index ?: point(center+Vec2(radius,0.0),if(centerPoint!=null||axisTangent)listOf(i)else emptyList(),when{axisTangent->PointDependencyType.CircleAxisTangent;centerPoint!=null->PointDependencyType.Translate;else->null},when{axisTangent->listOf(0.0);centerPoint!=null->listOf(radius,0.0);else->emptyList()})
                        if(existing!=null){modified=existing.copy(type=Shape2DType.Circle,pointIndices=listOf(i,j));shapes[shapes.indexOfFirst{it.id==existing.id}]=modified!!}else add(i,j,named ?: freshName())
                        params=(tuple?.let{mapOf("center" to listOf(it.x,it.y))} ?: centerPoint?.let{mapOf("center" to it.name)} ?: if(low.contains("origin")||low.contains("unit circle"))mapOf("center" to listOf(0.0,0.0))else emptyMap())+
                            (if(low.contains("unit circle"))mapOf("radius" to 1)else radiusToken?.let{mapOf("radius" to scalar(it))}.orEmpty())+
                            diameterNumber?.let{mapOf("diameter" to it)}.orEmpty()+through?.let{mapOf("through" to it.name)}.orEmpty()+named?.let{mapOf("label" to it)}.orEmpty()+if(low.contains("tangent to the x-axis"))mapOf("tangent" to "x_axis")else emptyMap()
                        if(centerPoint==null&&tuple==null&&!low.contains("origin"))note="Using default center (0,0)."
                        if(radiusToken==null&&diameterToken==null&&through==null&&!low.contains("tangent"))note+=" ${if(existing!=null)"Keeping radius $radius."else "Using default radius $radius."}"
                    }
                }
            }
            val color=Regex("(?i)\\b(red|blue|green|yellow|purple|orange|pink) circle\\b").find(text)?.groupValues?.get(1)?.lowercase()
            if(color!=null){params=params+mapOf("color" to color);created.indices.forEach{i->created[i]=created[i].copy(styleKey=when(color){"red"->"#FF0000";"blue"->"#0000FF";"green"->"#008000";"yellow"->"#FFFF00";"purple"->"#800080";"orange"->"#FFA500";else->"#FFC0CB"})}}
            frame=frame.copy(parameters=params)
            val next=s.copy(points=points,pointDependencies=dependencies,shapes=shapes+created).recomputed();val outputs=created+listOfNotNull(modified)
            require(outputs.isNotEmpty()&&next.shapes.size<=200)
            val summary=outputs.joinToString("; "){shape->val d=describe(next,shape);"${shape.name}: center (${d.center.x}, ${d.center.y}), radius ${d.radius}"}
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: circle construction"))
            return RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"$summary. ${note.trim()}",RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c.copy(selectedShapeId=outputs.last().id),semanticFrame=frame)
        }catch(e:RuhiReferenceQuestion){return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Choose the circle references.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,e.missing.map{RuhiAnswerChip("Create point $it","Create point $it at ")}),semanticFrame=frame)}
        catch(e:IllegalArgumentException){return RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid circle input.",context=c,semanticFrame=frame)}
    }
}
