package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Ground truth comes from source roles plus current controls and independent arithmetic. */
object RuhiDialogueSeedOracle {
    private fun same(a:WorkspaceState,b:WorkspaceState)=a.points==b.points&&a.shapes==b.shapes&&a.solids==b.solids&&a.functions==b.functions&&a.surfaceLayers==b.surfaceLayers&&a.points3D==b.points3D&&a.vectors3D==b.vectors3D&&a.planes3D==b.planes3D&&a.pointDependencies==b.pointDependencies
    private fun area(p:List<Vec2>)=abs(p.indices.sumOf{i->p[i].x*p[(i+1)%p.size].y-p[i].y*p[(i+1)%p.size].x})/2
    private fun perimeter(p:List<Vec2>)=p.indices.sumOf{i->hypot(p[i].x-p[(i+1)%p.size].x,p[i].y-p[(i+1)%p.size].y)}
    private fun matches(a:Any?,b:Any?):Boolean=when {a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.indices.all{matches(a[it],b[it])};else->a==b}
    fun check(parameters:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,message:String){if(!ok)errors+=message}
        fun close(a:Double,b:Double)=a.isFinite()&&abs(a-b)<=1e-7*max(1.0,abs(b))
        val id=before.labSessionValues[RuhiDialogueSeedFixture.CASE]?.toIntOrNull() ?: return RuhiSeedBehaviorOracle.Check(false,listOf("Missing actual dialogue prerequisite fixture."))
        if(id !in 476..500)return RuhiSeedBehaviorOracle.Check(false,listOf("Missing actual dialogue prerequisite fixture."))
        val after=r.state;val old=RuhiDialogueMemory.read(before);val memory=RuhiDialogueMemory.read(after)
        need(r.semanticFrame?.intent=="DIALOGUE_CONTEXT","Wrong contextual intent")
        parameters.forEach{(k,v)->need(matches(v,r.semanticFrame?.parameters?.get(k)),"Context slot $k expected $v, got ${r.semanticFrame?.parameters?.get(k)}")}
        val question=id in setOf(476,486,487,489)
        need(r.status==if(question)GraphCommandStatus.CLARIFICATION else GraphCommandStatus.APPLIED,"Unexpected ${r.status}: ${r.message}")
        if(question) {
            need(same(before,after),"Question changed native geometry")
            need(memory.pending!=null,"Essential question has no saved pending task")
            need(r.dialogue?.suggestions?.size in 1..5,"Question lacks bounded actual answer choices")
            need(r.message.count{it=='?'}<=1,"More than one essential question")
        }else if(r.dialogue?.pending!=true)need(r.dialogue?.suggestions.isNullOrEmpty(),"Successful contextual action showed unnecessary answer examples")
        fun scaled(i:Int,factor:Double) {
            val a=before.solids[i];val b=after.solids[i]
            need(close(b.width,a.width*factor)&&close(b.height,a.height*factor)&&close(b.depth,a.depth*factor)&&close(b.radius,a.radius*factor)&&close(b.topRadius,a.topRadius*factor),"Wrong inherited scale on solid $i")
            need(a.copy(width=b.width,height=b.height,depth=b.depth,radius=b.radius,topRadius=b.topRadius)==b,"Scaling altered unrelated pose/type on $i")
        }
        fun unchangedExceptSolids(indices:Set<Int>) {
            need(before.solids.size==after.solids.size,"Unexpected solid count")
            before.solids.indices.filter{it !in indices}.forEach{need(before.solids[it]==after.solids.getOrNull(it),"Changed unrelated solid $it")}
            need(before.points==after.points&&before.shapes==after.shapes&&before.functions==after.functions&&before.surfaceLayers==after.surfaceLayers,"Changed an unrelated engine scene")
        }
        val number="[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?"
        fun mentions(label:String,value:Double)=Regex("${Regex.escape(label)}\\s+($number)(?:\\s|$)").find(r.message)?.groupValues?.get(1)?.toDoubleOrNull()?.let{close(it,value)}==true
        try {when(id) {
            476,486,487->{if(id!=476)need(memory.pending==old.pending,"Did not retain the original task");need(memory.pending?.kind=="equation","Wrong pending graph slot")
                if(id==486)need(r.message==old.pendingPrompt,"Resume changed the actual saved question")
                if(id!=476)need(r.dialogue?.suggestions==old.lastChoices,"Displayed answer bindings changed")
                val chip=r.dialogue!!.suggestions.first();val resumed=RuhiDialogueManager().execute(after,chip.answer,WorkspaceHistoryByModule(),c)
                need(resumed.status==GraphCommandStatus.APPLIED,"Graph answer did not resume")
                need(resumed.state.functions.size==before.functions.size+1&&resumed.state.functions.take(before.functions.size)==before.functions,"Resume replaced an existing graph")
                need(RuhiDialogueMemory.read(resumed.state).pending==null,"Completed graph stayed pending")
            }
            477->{scaled(before.solids.lastIndex,1.25);unchangedExceptSolids(setOf(before.solids.lastIndex))}
            478->{val i=before.solids.lastIndex;need(before.solids[i].copy(rotation=before.solids[i].rotation+Vec3(0.0,45.0,0.0))==after.solids[i],"Wrong default Y rotation/topic");unchangedExceptSolids(setOf(i))}
            479->{val factor=before.labSessionValues.getValue(RuhiDialogueSeedFixture.FACTOR).toDouble();scaled(1,factor);unchangedExceptSolids(setOf(1))}
            480,481->{val q=RuhiSolidMeasureLanguage.decode(after.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK));val radius=if(id==480)before.solids.single().radius else RuhiSolidMeasureLanguage.parse(old.pending!!.source).query.dimensions.getValue("radius");val height=if(id==480)RuhiSolidMeasureLanguage.parse(old.pending!!.source).query.dimensions.getValue("height")else 7.0
                need(q.dimensions["radius"]?.let{close(it,radius)}==true&&q.dimensions["height"]?.let{close(it,height)}==true,"Correction lost a typed dimension role")
                val expected=PI*radius*radius*height/(if(id==481)3 else 1);val drawing=RuhiSolidMeasureLanguage.compute(after,q)
                need(close(drawing.entities.single().quantities.single().value,expected),"Wrong independent corrected volume")
                need(same(before,after),"Literal measurement changed native objects");need(memory.pending==null,"Corrected dimension remained pending")
            }
            482->{need(after.solids[1]==before.solids[1].copy(position=Vec3(3.0,2.0,1.0)),"Did not move the actual blue pending target");unchangedExceptSolids(setOf(1));need(memory.pending==null,"Blue correction remained pending")}
            483,484->{val factor=before.labSessionValues.getValue(RuhiDialogueSeedFixture.FACTOR).toDouble();val ids=if(id==483)setOf(0,1)else setOf(0);ids.forEach{scaled(it,factor)};unchangedExceptSolids(ids);need(memory.pending==null,"Candidate action remained pending")}
            485->{need(old.pending!=null&&memory.pending==null,"Cancel did not clear the actual task");need(same(before,after),"Cancel changed geometry")}
            488->{need(after.shapes.size==1&&after.points.size==3,"Default triangle did not create one native triangle");val shape=after.shapes.single();val p=shape.pointIndices.map{after.points[it]};val lengths=p.indices.map{p[it].distanceTo(p[(it+1)%p.size])};need(shape.type==Shape2DType.Triangle&&area(p)>0&&lengths.all{close(it,2.0)},"Default triangle is degenerate or wrong standard size");need(before.solids==after.solids,"Triangle changed inactive solids")}
            489->{need(memory.pending?.kind=="construction_place","Deictic placement did not ask for actual location")
                val chip=r.dialogue!!.suggestions[1];val requested=chip.label.removeSurrounding("(",")").split(',').map{it.trim().toDouble()};val resumed=RuhiDialogueManager().execute(after,chip.answer,WorkspaceHistoryByModule(),r.context)
                need(resumed.status==GraphCommandStatus.APPLIED,"Location did not resume drawing")
                val s=resumed.state;need(s.shapes.size==before.shapes.size+1&&s.points.take(before.points.size)==before.points&&s.shapes.take(before.shapes.size)==before.shapes,"Placed copy changed original construction")
                val clone=s.shapes.last();val p=clone.pointIndices.map{s.points[it]};need(clone.id!=before.shapes.single().id&&clone.pointIndices.all{it>=before.points.size},"Placed copy shares native controls")
                need(close(p.sumOf{it.x}/p.size,requested[0])&&close(p.sumOf{it.y}/p.size,requested[1]),"Wrong clarified center")
                need(close(area(p),area(before.shapes.single().pointIndices.map{before.points[it]})),"Copy changed native area")
            }
            490->{need(after.functions.size==before.functions.size+1&&after.functions.take(before.functions.size)==before.functions,"Equation answer replaced an existing graph");val expression=after.functions.last().expression.substringAfter('=');val f=ExpressionEngine().compile(expression);listOf(0.0,.5,2.0).forEach{need(close(f.eval(mapOf("x" to it)),it*it),"Wrong completed x-squared function")};need(memory.pending==null,"Equation completion stayed pending")}
            491->{need(old.lastChoices.size>=2&&after.functions.size==2,"Did not execute the actual second composite answer");val values=after.functions.map{ExpressionEngine().compile(it.expression.substringAfter('=')).eval(mapOf("x" to 0.0))}.sorted();need(values.size==2&&close(values[0],0.0)&&close(values[1],1.0),"Second answer did not create sin/cos");need(after.functions.any{it.expression.contains("sin")}&&after.functions.any{it.expression.contains("cos")},"Wrong displayed composite choice")}
            492->{val p=before.shapes.single().pointIndices.map{before.points[it]};need(mentions("area",area(p))&&mentions("perimeter",perimeter(p)),"Explanation does not use current control quantities");need(same(before,after),"Explanation changed geometry")}
            493->{need(same(before,after),"Existing-graph comparison changed the scene");val ids=r.semanticFrame?.parameters?.get("resolved_graph_ids") as? List<*>;val timeline=old.graphTimeline;need(ids==listOf(timeline.last(),timeline[timeline.lastIndex-1]),"Wrong stable current/previous graph IDs")
                val graphs=ids!!.map{id->before.functions.single{it.id==id}};val v=graphs.map{ExpressionEngine().compile(it.expression.substringAfter('=')).eval(mapOf("x" to 0.0))};val actual=Regex("first minus second at x = 0 is ($number)").find(r.message)?.groupValues?.get(1)?.toDoubleOrNull();need(actual?.let{close(it,v[0]-v[1])}==true,"Wrong independently evaluated graph difference")
            }
            494->{need(after.points.isEmpty()&&after.shapes.isEmpty(),"Blank-canvas restart did not clear active construction");need(before.solids==after.solids,"Restart cleared inactive solids")}
            495->{val a=after.points[0]-after.points[1];val b=after.points[2]-after.points[1];val angle=Math.toDegrees(acos(((a.x*b.x+a.y*b.y)/(hypot(a.x,a.y)*hypot(b.x,b.y))).coerceIn(-1.0,1.0)));need(close(angle,60.0),"Native points do not form 60 degrees");need(before.points.take(2)==after.points.take(2)&&before.points[3]==after.points[3],"Angle edit moved anchors/unrelated point");need(close(before.points[2].distanceTo(before.points[1]),after.points[2].distanceTo(after.points[1])),"Angle edit changed arm length");need(after.points[4]==(after.points[1]+after.points[2])*.5,"Angle edit detached midpoint dependency");need(before.shapes==after.shapes,"Angle edit replaced native construction")}
            496->{need(same(before,after),"Measurement visibility changed geometry");need(!RuhiSceneEditLanguage.measurements(before,before.shapes.single().id)&&RuhiSceneEditLanguage.measurements(after,before.shapes.single().id),"Measurements did not become visible")}
            497->{need(same(before,after),"Display units changed native coordinates");val u=GeometryMeasurementUnits.read(after);need(u.base==GeometryMeasurementUnits.Length.METER&&u.display==GeometryMeasurementUnits.Length.CENTIMETER,"Base/display units were conflated");val p=before.shapes.single().pointIndices.map{before.points[it]};need(mentions("area",area(p)*10000)&&mentions("perimeter",perimeter(p)*100),"Wrong dimensional conversion")}
            498->{val cube=before.solids.indexOfFirst{it.type==SolidType.Cube};val other=before.solids.indices.single{it!=cube};need(before.solids[cube]==after.solids[cube],"Constraint changed protected native cube");need(before.labSessionValues["ruhi.studio.appearance.$cube"]==after.labSessionValues["ruhi.studio.appearance.$cube"],"Constraint changed protected appearance");scaled(other,before.labSessionValues.getValue(RuhiDialogueSeedFixture.FACTOR).toDouble());unchangedExceptSolids(setOf(other));need(memory.pending==null,"Safe constrained edit remained pending")}
            499->{need(old.lastError.isNotBlank()&&r.message.contains(old.lastError),"Did not retain actual failure reason");need(same(before,after),"Error explanation changed geometry")}
            500->{need(same(before,after),"Construction trace changed geometry");val names=RuhiEndpointResolver.references(before).associate{it.second to it.first};val lines=r.message.lines();val child=lines.indexOfFirst{it.contains("midpoint of")};val parentLines=listOf(1,3).map{i->lines.indexOfFirst{it.contains(names.getValue(i))}};need(child>=0&&parentLines.all{it>=0&&it<child},"Trace did not order actual midpoint parents before dependent control");need(r.message.contains("(${before.points[2].x}, ${before.points[2].y})"),"Trace lacks current dependent coordinates");need(!r.message.contains(names.getValue(4)),"Trace included unrelated point")}
        }}catch(e:Exception){errors+="Context oracle could not verify row $id: ${e.message}"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
