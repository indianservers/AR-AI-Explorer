package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64

/** Validated operation roles, never numbers recovered from a previous explanation. */
object RuhiActionContext {
    const val KEY="ruhi.dialogue.action.v1"
    data class Task(val module:String,val target:String,val kind:String,val values:List<Double>,val qualifier:String,val guard:String)
    data class Resolution(val command:String?=null,val context:RuhiSceneContext=RuhiSceneContext(),val message:String="",val choices:List<RuhiAnswerChip> = emptyList(),val targets:List<String> = emptyList())
    fun sceneGuard(s:WorkspaceState)="${s.id}:${s.module}:${listOf(s.solids,s.shapes.map{it.id}).hashCode()}"
    private fun encode(t:Task):String {
        val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use{o->o.writeInt(1);listOf(t.module,t.target,t.kind,t.qualifier,t.guard).forEach(o::writeUTF);o.writeInt(t.values.size);t.values.forEach(o::writeDouble)}
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }
    fun read(s:WorkspaceState):Task?=runCatching {
        val raw=s.labSessionValues[KEY] ?: return null;require(raw.length<=8000)
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(raw))).use{i->require(i.readInt()==1);val v=List(5){i.readUTF()};val n=i.readInt();require(n in 0..3);val numbers=List(n){i.readDouble()};require(numbers.all{it.isFinite()});Task(v[0],v[1],v[2],numbers,v[3],v[4])}
    }.getOrNull()
    private fun ids(s:WorkspaceState)=when(s.module){MathModule.Geometry2D->s.shapes.map{"s:${it.id}"};MathModule.Geometry3D->s.solids.indices.map{"o:$it"};else->emptyList()}
    private fun context(id:String)=if(id.startsWith("s:"))RuhiSceneContext(selectedShapeId=id.drop(2))else RuhiSceneContext(selectedSolidIndex=id.drop(2).toInt())
    fun record(before:WorkspaceState,result:RuhiExecutionResult):WorkspaceState {
        val s=result.state
        if(result.status!=GraphCommandStatus.APPLIED)return s
        if(result.semanticFrame?.parameters?.get("action")=="display_units")return s
        var target=when(s.module){MathModule.Geometry2D->result.context.selectedShapeId?.let{"s:$it"};MathModule.Geometry3D->result.context.selectedSolidIndex?.let{"o:$it"};else->null}
        var kind="";var values=emptyList<Double>();var qualifier=""
        fun use(k:String,v:List<Double> = emptyList(),q:String=""){kind=k;values=v;qualifier=q}
        when(val op=result.command?.operation) {
            is RuhiOperation.Scale->use("scale",listOf(op.factor))
            is RuhiOperation.Rotate->use("rotate",listOf(op.degrees),op.axis)
            is RuhiOperation.Measure->use("measure",q=op.kind.name)
            is RuhiOperation.SetColor->use("color",q=op.hex)
            else->Unit
        }
        @Suppress("UNCHECKED_CAST")
        val frame=result.semanticFrame?.let{if(it.parameters["constraint"]=="preserve_cube")RuhiSemanticFrame(it.parameters["applied_intent"].toString(),it.engine,it.parameters["applied_parameters"] as? Map<String,Any> ?: emptyMap())else it}
        if(frame?.intent=="TRANSFORM_3D") {
            val p=frame.parameters
            fun number(k:String)=(p[k] as? Number)?.toDouble()
            fun vector(k:String)=(p[k] as? List<*>)?.mapNotNull{(it as? Number)?.toDouble()}?.takeIf{it.size==3}
            // Exclude multi-object/pivot/duplication operations rather than dropping their roles.
            if(before.solids.size==s.solids.size&&p.keys.all{it in setOf("target","scale","degrees","axis","position","translation")}) {
                val changed=s.solids.indices.filter{before.solids[it]!=s.solids[it]}
                if(changed.size==1)target="o:${changed.single()}"
                if(changed.size<=1)when {
                    number("scale")!=null->use("scale",listOf(number("scale")!!))
                    number("degrees")!=null&&p["axis"] in setOf("x","y","z")->use("rotate",listOf(number("degrees")!!),p["axis"].toString())
                    vector("position")!=null->use("position",vector("position")!!)
                    vector("translation")!=null->use("translate",vector("translation")!!)
                }
            }
        }
        if(frame?.intent=="MEASURE_SOLID")runCatching {
            val q=RuhiSolidMeasureLanguage.decode(s.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK))
            if(q.shape==null&&!q.compare&&q.indices.size==1){target="o:${q.indices.single()}";use("solid_measure",q=q.measures.joinToString(","))}
        }
        if(frame?.parameters?.get("action")=="explain") {
            (frame.parameters["resolved_object_id"] as? String)?.let{target=it;use("explain")}
        }
        val changedShapes=s.shapes.count{shape->val old=before.shapes.firstOrNull{it.id==shape.id};old!=shape||old.pointIndices.map{before.points.getOrNull(it)}!=shape.pointIndices.map{s.points.getOrNull(it)}}
        val singleSolid=s.module!=MathModule.Geometry3D||before.solids.size==s.solids.size&&s.solids.indices.count{before.solids[it]!=s.solids[it]}<=1
        val task=if(kind.isNotEmpty()&&target in ids(s)&&values.all{it.isFinite()}&&(s.module!=MathModule.Geometry2D||changedShapes<=1)&&singleSolid)Task(s.module.name,target!!,kind,values,qualifier,sceneGuard(s))else null
        // A newly applied unrecordable action invalidates an older action, avoiding accidental replay.
        return s.copy(labSessionValues=if(task==null)s.labSessionValues-KEY else s.labSessionValues+(KEY to encode(task)))
    }
    internal fun command(t:Task):String? {
        val selected="the selected object"
        return when(t.kind) {
            "scale"->t.values.singleOrNull()?.let{"scale $selected by $it"}
            "rotate"->t.values.singleOrNull()?.let{"rotate $selected $it degrees around the ${t.qualifier.uppercase()}-axis"}
            "position"->t.values.takeIf{it.size==3}?.let{"move $selected to (${it.joinToString(",")})"}
            "translate"->t.values.takeIf{it.size==3}?.let{"translate $selected by vector (${it.joinToString(",")})"}
            "explain"->"Explain the shape we are viewing"
            "measure"->when(t.qualifier){"AREA"->"calculate the area";"PERIMETER"->"calculate the perimeter";"VOLUME"->"Find the volume of the selected solid";"SURFACE_AREA"->"Find the surface area of the selected solid";"EXPLAIN"->"Explain the shape we are viewing";else->null}
            "solid_measure"->when(t.qualifier){"surface_area,volume"->"Find surface area and volume of the selected solid";"volume","surface_area","base_area","space_diagonal","height","radius","diameter","total_edge_length","slant_height"->"Find ${t.qualifier.replace('_',' ')} of the selected solid";else->null}
            "color"->mapOf("#FF0000" to "red","#0000FF" to "blue","#008000" to "green","#800080" to "purple","#FFFF00" to "yellow")[t.qualifier]?.let{"make $selected $it"}
            else->null
        }
    }
    fun accepts(source:String)=RuhiMathLanguage.normalize(source) in setOf("what about the other 1","what about the other object")||source.startsWith("Use other object ")
    fun resolve(s:WorkspaceState,source:String,c:RuhiSceneContext,p:RuhiPending?):Resolution {
        if(p!=null&&p.kind!="other_object")return Resolution(message="Complete or cancel the pending request before applying the previous action to another object.")
        val task=read(s) ?: return Resolution(message="Which action should I apply to the other object?")
        if(task.module!=s.module.name||!task.guard.startsWith("${s.id}:${s.module}:")||task.target !in ids(s)||(s.module==MathModule.Geometry3D&&task.guard!=sceneGuard(s)))return Resolution(message="The scene changed. State the action and target again for the current objects.")
        val command=command(task) ?: return Resolution(message="Which action should I apply to the other object?")
        val others=ids(s).filter{it!=task.target}
        val explicit=when(s.module){MathModule.Geometry2D->c.selectedShapeId?.let{"s:$it"};MathModule.Geometry3D->c.selectedSolidIndex?.let{"o:$it"};else->null}
        val picked=if(source.startsWith("Use other object "))runCatching{String(Base64.getDecoder().decode(source.removePrefix("Use other object ")),Charsets.UTF_8)}.getOrNull()?.takeIf{p?.kind=="other_object"&&it in p.targets&&p.scene==sceneGuard(s)}else explicit?.takeIf{it in others} ?: others.singleOrNull()
        if(picked!=null&&picked in others)return Resolution(command,context(picked))
        val chips=others.take(5).map{id->val label=if(id.startsWith("s:"))s.shapes.first{it.id==id.drop(2)}.name else "${s.solids[id.drop(2).toInt()].type} ${id.drop(2).toInt()+1}";RuhiAnswerChip(label,"Use other object "+Base64.getEncoder().encodeToString(id.toByteArray(Charsets.UTF_8)))}
        return Resolution(message=if(others.isEmpty())"Create another object, then state the action again."else"Which other object should receive the same action?",choices=chips,targets=others)
    }
}
