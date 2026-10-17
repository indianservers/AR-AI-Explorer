package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.util.Base64
import kotlin.math.*

/** Stable vector IDs provide live 3D angle references independently of array order and display names. */
object RuhiSpatialAngleLanguage {
    const val PREFIX="ruhi.geometry3d.angle."
    const val SELECTION=PREFIX+"selection"
    private const val MARK=PREFIX+"mark."
    data class Drawing(val center:Vec3,val first:Vec3,val second:Vec3,val arc:List<Vec3>,val degrees:Double)
    private fun encode(ids:List<String>)=ids.joinToString(","){Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(Charsets.UTF_8))}
    private fun decode(value:String):List<String> = runCatching{require(value.length<=12000);value.split(',').map{String(Base64.getUrlDecoder().decode(it),Charsets.UTF_8)}.also{require(it.size==2&&it.distinct().size==2)}}.getOrDefault(emptyList())
    fun selected(s:WorkspaceState)=decode(s.labSessionValues[SELECTION].orEmpty())
    fun marks(s:WorkspaceState)=s.labSessionValues.filterKeys{it.startsWith(MARK)}.values.map(::decode).filter{it.size==2}
    private fun resolve(s:WorkspaceState,ids:List<String>):List<Vector3D> {
        require(ids.size==2&&ids.distinct().size==2){"Choose two different vectors."}
        return ids.map{id->s.vectors3D.singleOrNull{it.id==id} ?: throw IllegalArgumentException("A referenced vector was removed or its ID is ambiguous. Select the pair again.")}
    }
    private fun length(v:Vec3)=hypot(hypot(v.x,v.y),v.z)
    private fun unit(v:Vec3):Vec3 {val n=length(v);require(n.isFinite()&&n>0){"The angle of a zero or non-finite vector is undefined."};return v*(1/n)}
    fun drawing(s:WorkspaceState,ids:List<String>):Drawing {
        val pair=resolve(s,ids);val u=unit(pair[0].components);val v=unit(pair[1].components)
        val cosine=u.dot(v).coerceIn(-1.0,1.0);val residual=v-u*cosine
        val sine=length(residual);val theta=atan2(sine,cosine)
        val tangent=if(sine>1e-12)residual*(1/sine)else {
            val axis=when{abs(u.x)<=abs(u.y)&&abs(u.x)<=abs(u.z)->Vec3(1.0,0.0,0.0);abs(u.y)<=abs(u.z)->Vec3(0.0,1.0,0.0);else->Vec3(0.0,0.0,1.0)}
            unit(axis-u*u.dot(axis))
        }
        val center=pair[0].start;require(listOf(center.x,center.y,center.z).all{it.isFinite()&&abs(it)<=10000}){"The vector origin is outside the supported range."}
        val radius=minOf(length(pair[0].components),length(pair[1].components),3.0)*.35
        val arc=(0..64).map{i->val a=theta*i/64;center+(u*cos(a)+tangent*sin(a))*radius}
        return Drawing(center,center+u*(radius*1.35),center+v*(radius*1.35),arc,Math.toDegrees(theta))
    }
    fun drawings(s:WorkspaceState)=marks(s).mapNotNull{runCatching{drawing(s,it)}.getOrNull()}
    fun accepts(text:String)=text.length<=2000&&(Regex("(?i)\\b(?:select|use) vectors? .+ and .+$").containsMatchIn(text.trim().trimEnd('.','?'))||Regex("(?i)\\bangle between (?:(?:these|the) two (?:3d )?vectors|(?:3d )?vectors .+ and .+)").containsMatchIn(text))
    private fun chips(s:WorkspaceState,ids:List<String>):List<RuhiAnswerChip> {
        val v=ids.mapNotNull{id->s.vectors3D.singleOrNull{it.id==id}}.filter{a->s.vectors3D.count{it.name.equals(a.name,true)}==1}
        return v.indices.flatMap{i->(i+1 until v.size).map{j->RuhiAnswerChip("${v[i].name} + ${v[j].name}","Use vectors ${v[i].name} and ${v[j].name}")}}.take(4)
    }
    fun restored(s:WorkspaceState,p:RuhiPending)=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips(s,p.targets),true)
    private fun scene(s:WorkspaceState)=java.security.MessageDigest.getInstance("SHA-256").digest((s.id+"|"+s.module.name+"|"+s.vectors3D.toString()).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,p:RuhiPending?):RuhiPointConversation.Outcome? {
        if(p!=null&&p.kind!="spatial_vector_pair")return null
        if(s.module!=MathModule.Geometry3D) {
            if(p==null)return null
            return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The studio changed. Ask the vector-angle question again.",context=c),null)
        }
        if(source.length>2000)return null
        val text=source.trim().trimEnd('.','?');val low=text.lowercase()
        if(low in setOf("cancel","cancel that"))return null
        if(p==null&&!accepts(text))return null
        val frame=RuhiSemanticFrame("MEASURE_ANGLE",RuhiEngine.GEOMETRY_3D,mapOf("vectors" to "selected_3d_pair"))
        fun ask(message:String,pending:RuhiPending?):RuhiPointConversation.Outcome=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,message,context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,pending?.let{chips(s,it.targets)}.orEmpty(),pending!=null),semanticFrame=frame),pending)
        if(p!=null&&(p.module!=s.module.name||p.scene!=scene(s)))return ask("The vector scene changed. Ask the angle question again so I can use the current vectors.",null)
        if(p!=null&&low in setOf("resume","continue","resume pending"))return ask("Which two vectors should I use?",p)
        val body=Regex("(?i)\\b(?:select|use) vectors? (.+)$").find(text)?.groupValues?.get(1)
            ?: Regex("(?i)\\bangle between (?:3d )?vectors (.+)$").find(text)?.groupValues?.get(1)
            ?: if(p!=null)text else null
        val named=body?.let{b->s.vectors3D.flatMap{a->s.vectors3D.filter{v->v.id!=a.id&&b.equals("${a.name} and ${v.name}",true)}.map{v->listOf(a.id,v.id)}}.distinct().singleOrNull()}
        val ids=named ?: if(body==null)c.selectedVectorIds.takeIf{it.size==2} ?: selected(s)else emptyList()
        if(ids.size!=2) {
            val pending=if(s.vectors3D.size>=2)RuhiPending("spatial_vector_pair",p?.source ?: source,s.module.name,scene(s),s.vectors3D.map{it.id})else null
            return ask(if(s.vectors3D.size<2)"Create two nonzero 3D vectors first." else "Which two 3D vectors should I use? Choose a pair or enter their unique names.",pending)
        }
        if(ids.any{id->s.vectors3D.count{it.id==id}!=1})return ask("A selected vector was removed or is ambiguous. Select the current pair again.",null)
        try {
            val pair=resolve(s,ids)
            if(p==null&&Regex("(?i)^(?:select|use) vectors? ").containsMatchIn(text)) {
                val next=s.copy(labSessionValues=s.labSessionValues+(SELECTION to encode(ids)))
                return RuhiPointConversation.Outcome(RuhiExecutionResult(next,GraphCommandStatus.APPLIED,"Selected vectors ${pair.joinToString(" and "){it.name}}.",context=c.copy(selectedVectorIds=ids)),null)
            }
            val drawing=drawing(s,ids);val params=if(p!=null||body==null)frame.parameters else mapOf("vectors" to pair.map{it.name})
            val actual=frame.copy(parameters=params);val key=MARK+encode(ids)
            require(marks(s).size<32||s.labSessionValues.containsKey(key)){"Remove a 3D angle annotation before adding more."}
            val next=s.copy(labSessionValues=s.labSessionValues+mapOf(SELECTION to encode(ids),key to encode(ids)))
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: 3D vector angle"))
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Angle between ${pair.joinToString(" and "){it.name}} = ${drawing.degrees} degrees. Marked in 3D; follows both vectors. Free-vector directions are compared at ${pair[0].name}'s start.",RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.PointQuery(actual),modality=m),c.copy(selectedVectorIds=ids),semanticFrame=actual),null)
        }catch(e:IllegalArgumentException){return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid vector angle.",context=c,semanticFrame=frame),null)}
    }
}
