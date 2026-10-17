package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64
import kotlin.math.*

/** Literal dimensions and live selected-solid measurements share the same mathematical engine. */
object RuhiSolidMeasureLanguage {
    const val MARK="ruhi.geometry3d.measure.v1"
    const val UNIT="ruhi.measure.lengthUnit"
    data class Query(val shape:String?,val dimensions:Map<String,Double>,val targets:List<String>,val measures:List<String>,val compare:Boolean=false,val indices:List<Int> = emptyList(),val guard:List<String> = emptyList())
    data class Parsed(val query:Query,val parameters:Map<String,Any>)
    data class Segment(val a:Vec3,val b:Vec3,val text:String,val value:Double?=null,val power:Int=1)
    data class Entity(val solid:Solid,val index:Int?,val quantities:List<RuhiSolidMeasureMath.Quantity>,val segments:List<Segment>,val baseArea:Double?=null)
    data class Drawing(val entities:List<Entity> = emptyList(),val difference:Double?=null,val ratio:Double?=null,val diagnostic:String="")
    private class Missing(val candidates:List<Int>,val picked:Map<String,Int>,val kind:String,val prompt:String):IllegalArgumentException(prompt)
    private class Dimension(val field:String):IllegalArgumentException("What ${field.replace('_',' ')} should I use?")
    private val kinds=linkedMapOf("rectangular prism" to "cuboid","cuboid" to "cuboid","hemisphere" to "hemisphere","sphere" to "sphere","cube" to "cube","cylinder" to "cylinder","cone" to "cone","pyramid" to "pyramid","convex polyhedron" to "polyhedron")
    private val num="[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?"
    private fun text(s:String)=MathExpressionNormalizer.normalize(RuhiMathLanguage.normalize(s)).replace(Regex("\\bradius-([0-9])"),"radius $1")
    fun accepts(s:String):Boolean {
        if(s.length>2000)return false;val t=text(s)
        if(t in setOf("what's its volume","now show its volume"))return false
        if(Regex("\\b(?:slice|cross section|cut)\\b").containsMatchIn(t))return false
        if("faces" in t && !listOf("faces","edges","vertices").all(t::contains))return false
        if(t.startsWith("give ")&&!t.startsWith("give the "))return false
        return Regex("^(?:what|calculate|find|volume|measure|how|give|compute|show|compare|verify)\\b").containsMatchIn(t)&&Regex("\\b(?:volume|volumes|surface area|total area|lateral area|base area|diagonal|height|radius|diameter|edge total|faces|euler)\\b").containsMatchIn(t)
    }
    fun parse(source:String,extra:Map<String,Double> = emptyMap()):Parsed {
        val t=text(source);val compare=t.startsWith("compare the volumes")||t.startsWith("compare volumes")
        val measures=when {
            "euler" in t->listOf("euler_characteristic")
            listOf("faces","edges","vertices").all(t::contains)->listOf("topology")
            "surface area and volume" in t->listOf("surface_area","volume")
            "slant height" in t->listOf("slant_height")
            "lateral area" in t||"curved surface area" in t->listOf("lateral_area")
            "surface area" in t||"total area" in t->listOf("surface_area")
            "base area" in t&&!t.contains("volume")->listOf("base_area")
            "diagonal" in t->listOf("space_diagonal")
            "edge total" in t->listOf("total_edge_length")
            "diameter" in t->listOf("diameter")
            "volume" in t->listOf("volume")
            "height" in t->listOf("height")
            "radius" in t->listOf("radius")
            else->throw IllegalArgumentException("Specify the solid measurement.")
        }
        val names=kinds.mapNotNull{(word,kind)->Regex("\\b${Regex.escape(word)}\\b").find(t)?.let{it.range.first to kind}}.sortedBy{it.first}.map{it.second}.distinct()
        val shape=names.firstOrNull() ?: "solid"
        val dims=linkedMapOf<String,Double>()
        for((field,label) in listOf("radius" to "radius","side" to "side","height" to "height","base_area" to "base area")){
            Regex("\\b$label\\s+(?:is\\s+|of\\s+|=\\s*)?($num)(?![A-Za-z_0-9.])").find(t)?.let{dims[field]=it.groupValues[1].toDouble()}
        }
        val dimensions=Regex("($num)\\s*(?:by|\\*|x)\\s*($num)\\s*(?:by|\\*|x)\\s*($num)(?![A-Za-z_0-9.])").find(t)?.groupValues?.drop(1)?.map(String::toDouble)
        if(dimensions!=null){dims["width"]=dimensions[0];dims["height"]=dimensions[1];dims["depth"]=dimensions[2]}
        dims.putAll(extra);require(dims.values.all{it.isFinite()&&it>0&&it<=10000}){"Use positive finite dimensions within 10000 units."}
        val literal=dims.isNotEmpty()||Regex("\\b(?:a|an) (?:cube|sphere|hemisphere|cone|cylinder|cuboid|rectangular prism)\\b").containsMatchIn(t)||!t.contains("this")&&!t.contains("selected")&&listOf("radius","side","height","base area").any{word->word in t&&measures.none{it==word.replace(' ','_')}}
        val params=linkedMapOf<String,Any>()
        val targets=if(compare){require(names.size==2){"Name two supported solids to compare."};params["targets"]=names;params["compare"]=true;names}else if(literal){require(shape!="solid"&&shape!="polyhedron"){"Name the shape for those dimensions."};params["shape"]=shape;for((k,v) in dims)if(k !in setOf("width","depth")&&!(k=="height"&&dimensions!=null))params[k]=v;if(dimensions!=null||dims.keys.containsAll(listOf("width","height","depth")))params["dimensions"]=listOf(dims.getValue("width"),dims.getValue("height"),dims.getValue("depth"));emptyList()}else{val target=if(shape=="polyhedron")"selected_convex_polyhedron"else if(shape=="solid")"selected_solid"else "selected_$shape";params["target"]=target;listOf(shape)}
        if(measures.size==1)params["measure"]=measures.single()else params["measures"]=measures
        return Parsed(Query(if(literal&&!compare)shape else null,dims,targets,measures,compare),params)
    }
    fun type(name:String):SolidType=when(name){"cube"->SolidType.Cube;"cuboid"->SolidType.Cuboid;"sphere"->SolidType.Sphere;"hemisphere"->SolidType.Hemisphere;"cylinder"->SolidType.Cylinder;"cone"->SolidType.Cone;"pyramid"->SolidType.Pyramid;else->error("Unsupported literal shape $name")}
    fun literal(q:Query):Solid {
        fun need(field:String)=q.dimensions[field] ?: throw Dimension(field)
        return when(val kind=type(requireNotNull(q.shape))){
            SolidType.Cube->need("side").let{Solid(kind,it)}
            SolidType.Cuboid->Solid(kind,need("width"),need("height"),need("depth"))
            SolidType.Sphere,SolidType.Hemisphere->need("radius").let{Solid(kind,2*it,radius=it)}
            SolidType.Cylinder,SolidType.Cone->{val r=need("radius");val h=if(q.measures.all{it in setOf("base_area","radius","diameter")})q.dimensions["height"] ?: 1.0 else need("height");Solid(kind,2*r,h,2*r,radius=r)}
            SolidType.Pyramid->{val a=need("base_area");Solid(kind,sqrt(a),need("height"),sqrt(a))}
            else->error("Unsupported literal shape")
        }
    }
    private fun scene(s:WorkspaceState)=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.solids).toByteArray(Charsets.UTF_8)))
    private fun matches(s:Solid,kind:String)=when(kind){"solid"->true;"polyhedron"->s.type in RuhiSolidMeasureMath.polyhedra;else->s.type==type(kind)}
    private fun bind(s:WorkspaceState,c:RuhiSceneContext,q:Query,pickedInput:Map<String,Int>):Query {
        if(q.shape!=null){literal(q);return q}
        val picked=pickedInput.toMutableMap();val indices=q.targets.map{kind->val candidates=s.solids.indices.filter{matches(s.solids[it],kind)}
            val selected=picked[kind]?.takeIf{it in candidates} ?: c.selectedSolidIndex?.takeIf{it in candidates} ?: candidates.singleOrNull() ?: throw Missing(candidates,picked,kind,"${if(candidates.isEmpty())"Create or select"else"Which"} $kind should I measure?")
            picked[kind]=selected;selected}
        return q.copy(indices=indices,guard=s.solids.mapIndexed{i,solid->if(i in indices)"@${solid.type.name}"else solid.toString()})
    }
    fun encode(q:Query):String {val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use{o->o.writeInt(1);o.writeUTF(q.shape ?: "");o.writeInt(q.dimensions.size);q.dimensions.forEach{(k,v)->o.writeUTF(k);o.writeDouble(v)};o.writeInt(q.targets.size);q.targets.forEach(o::writeUTF);o.writeInt(q.measures.size);q.measures.forEach(o::writeUTF);o.writeBoolean(q.compare);o.writeInt(q.indices.size);q.indices.forEach(o::writeInt);o.writeInt(q.guard.size);q.guard.forEach(o::writeUTF)};return Base64.getEncoder().encodeToString(bytes.toByteArray())}
    fun decode(record:String):Query {require(record.length<=50000);return DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(record))).use{i->require(i.readInt()==1);val shape=i.readUTF().ifBlank{null};val n=i.readInt();require(n in 0..6);val dims=(0 until n).associate{i.readUTF() to i.readDouble()};val nt=i.readInt();require(nt in 0..2);val targets=List(nt){i.readUTF()};val nm=i.readInt();require(nm in 1..2);val measures=List(nm){i.readUTF()};val compare=i.readBoolean();val ni=i.readInt();require(ni in 0..2);val indices=List(ni){i.readInt()};val ng=i.readInt();require(ng in 0..100);Query(shape,dims,targets,measures,compare,indices,List(ng){i.readUTF()})}}
    fun compute(s:WorkspaceState,q:Query):Drawing {
        val solids=if(q.shape!=null)listOf(literal(q))else{require(q.guard.size==s.solids.size&&q.guard.indices.all{q.guard[it]=="@${s.solids[it].type.name}"||q.guard[it]==s.solids[it].toString()}){"The solid arrangement changed. Ask the measurement again to confirm its target."};require(q.indices.all{it in s.solids.indices});q.indices.map{s.solids[it]}}
        val entities=solids.mapIndexed{i,solid->val measures=q.measures.map{RuhiSolidMeasureMath.quantity(solid,it,q.dimensions["base_area"])};Entity(solid,if(q.shape==null)q.indices[i]else null,measures,segments(solid,q),q.dimensions["base_area"])}
        val values=entities.mapNotNull{it.quantities.firstOrNull{it.name=="volume"}?.value};return Drawing(entities,if(q.compare&&values.size==2)values[1]-values[0]else null,if(q.compare&&values.size==2)values[1]/values[0]else null)
    }
    private fun segments(s:Solid,q:Query):List<Segment> {
        val out=mutableListOf<Segment>();val w=s.width/2;val h=s.height/2;val d=s.depth/2
        when(s.type){
            SolidType.Sphere,SolidType.Hemisphere->out+=Segment(Vec3(0.0,0.0,0.0),Vec3(s.radius,0.0,0.0),"r = ${trim(s.radius)}",s.radius)
            SolidType.Cylinder,SolidType.Cone->{val y=if(q.measures.all{it=="base_area"})0.0 else -h;out+=Segment(Vec3(0.0,y,0.0),Vec3(s.radius,y,0.0),"r = ${trim(s.radius)}",s.radius);if(q.measures.any{it !in setOf("base_area","radius","diameter")})out+=Segment(Vec3(0.0,-h,0.0),Vec3(0.0,h,0.0),"h = ${trim(s.height)}",s.height);if("slant_height" in q.measures||"lateral_area" in q.measures)out+=Segment(Vec3(s.radius,-h,0.0),Vec3(0.0,h,0.0),"s = ${trim(hypot(s.radius,s.height))}",hypot(s.radius,s.height))}
            SolidType.Cube,SolidType.Cuboid->{out+=Segment(Vec3(-w,-h,d),Vec3(w,-h,d),"w = ${trim(s.width)}",s.width);out+=Segment(Vec3(w,-h,d),Vec3(w,h,d),"h = ${trim(s.height)}",s.height);out+=Segment(Vec3(w,-h,-d),Vec3(w,-h,d),"d = ${trim(s.depth)}",s.depth);if("space_diagonal" in q.measures)out+=Segment(Vec3(-w,-h,-d),Vec3(w,h,d),"diagonal",sqrt(s.width*s.width+s.height*s.height+s.depth*s.depth))}
            SolidType.Pyramid->{out+=Segment(Vec3(0.0,-h,0.0),Vec3(0.0,h,0.0),"h = ${trim(s.height)}",s.height);val a=q.dimensions["base_area"] ?: s.width*s.depth;out+=Segment(Vec3(-w,-h,d),Vec3(w,-h,d),"base A = ${trim(a)}",a,2)}
            else->Unit
        };return out
    }
    private data class Key(val record:String,val solids:List<Solid>)
    private val cache=object:LinkedHashMap<Key,Drawing>(8,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<Key,Drawing>?)=size>8}
    fun drawing(s:WorkspaceState):Drawing?{val record=s.labSessionValues[MARK] ?: return null;val key=Key(record,s.solids);synchronized(cache){cache[key]?.let{return it}};val d=runCatching{compute(s,decode(record))}.getOrElse{Drawing(diagnostic=it.message ?: "Measurement unavailable")};synchronized(cache){cache[key]=d};return d}
    fun segmentLabel(segment:Segment,unit:GeometryMeasurementUnits.Context):String=if(!unit.known||segment.value==null)segment.text else segment.text.substringBefore('=').trim()+" = "+unit.text(segment.value,segment.power,::trim)
    fun quantityLabel(solid:Solid,q:RuhiSolidMeasureMath.Quantity,unit:GeometryMeasurementUnits.Context):String=if(q.power==0)q.exact else "${solid.type} ${q.name.replace('_',' ')} = ${unit.text(q.value,q.power){"%.7g".format(java.util.Locale.US,it)}}"
    fun drawingLabel(s:WorkspaceState):String {
        val d=drawing(s) ?: return "";val u=GeometryMeasurementUnits.read(s)
        return d.diagnostic.ifEmpty{d.entities.flatMap{e->e.quantities.map{quantityLabel(e.solid,it,u)}+e.segments.map{segmentLabel(it,u)}}.joinToString("; ")}
    }
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        val chips=if(p.kind=="solid_measure_dimension")listOf(2,5).map{RuhiAnswerChip(it.toString(),"Use measurement value $it")}else p.targets.mapNotNull{it.toIntOrNull()}.filter{it in s.solids.indices}.take(5).map{i->val solid=s.solids[i];RuhiAnswerChip("${solid.type} ${i+1} (${trim(solid.width)} x ${trim(solid.height)} x ${trim(solid.depth)})","Use measurement solid $i")}
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    fun units(power:Int,unit:String="unit"):String=if(power==0)""else if(unit=="unit")when(power){1->"units";2->"square units";else->"cubic units"}else unit+when(power){2->"²";3->"³";else->""}
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        if(s.module!=MathModule.Geometry3D)return null;val pending=pendingInput?.takeIf{it.kind in setOf("solid_measure","solid_measure_dimension")};val accepted=accepts(source)
        val numericReply=pending?.kind=="solid_measure_dimension"&&Regex(num).matches(source.trim())
        if(!accepted&&(pending==null||!source.startsWith("Use measurement ")&&!numericReply&&text(source) !in setOf("resume","continue")))return null
        var frame=RuhiSemanticFrame("MEASURE_SOLID",RuhiEngine.GEOMETRY_3D,emptyMap());val picked=mutableMapOf<String,Int>();val extra=mutableMapOf<String,Double>()
        fun ask(prompt:String,p:RuhiPending)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,prompt,context=c,semanticFrame=frame,dialogue=restored(s,p)),p)
        try {
            if(pending!=null&&!accepted&&pending.scene!=scene(s))return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The solid scene changed. Ask the measurement again.",context=c,semanticFrame=frame),null)
            if(pending!=null&&!accepted){pending.targets.filter{it.startsWith("picked:")}.forEach{val v=it.removePrefix("picked:").split('=');picked[v[0]]=v[1].toInt()};pending.targets.filter{it.startsWith("dim:")}.forEach{val v=it.removePrefix("dim:").split('=');extra[v[0]]=v[1].toDouble()};if(pending.kind=="solid_measure_dimension"){val value=source.removePrefix("Use measurement value ").toDoubleOrNull() ?: return ask("Enter a positive numeric ${pending.targets.first()}.",pending);extra[pending.targets.first()]=value}else{val index=source.removePrefix("Use measurement solid ").toIntOrNull() ?: return ask("Choose a current solid.",pending);require(index.toString() in pending.targets&&index in s.solids.indices);val kind=pending.targets.first{it.startsWith("kind:")}.removePrefix("kind:");picked[kind]=index}}
            val p=parse(if(pending!=null&&!accepted)pending.source else source,extra);frame=frame.copy(parameters=p.parameters);val q=bind(s,c,p.query,picked);val d=compute(s,q);val unit=GeometryMeasurementUnits.read(s)
            val message=d.entities.joinToString("; "){e->e.quantities.joinToString("; "){v->
                val factor=unit.factor(v.power);val exact=if(factor==1.0)v.exact else "(${v.exact}) * ${unit.factorText(v.power)}"
                "${e.solid.type} ${v.name.replace('_',' ')} = $exact"+if(v.power==0)""else" ≈ ${unit.text(v.value,v.power){"%.12g".format(java.util.Locale.US,it)}}"
            }}+(if(q.compare)"; second minus first = ${unit.text(requireNotNull(d.difference),3,::trim)}; second / first = ${trim(requireNotNull(d.ratio))}."else".")+(if(q.shape=="pyramid")" Reference drawing uses a square base of the supplied area."else"")
            val next=s.copy(labSessionValues=s.labSessionValues+(MARK to encode(q)));val reconciled=GraphWorkspaceCommand.reconcileDocument(s,next);val committed=if(q.shape==null)reconciled else h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,reconciled,"Ruhi: solid measurement"));val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,message,RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c,semanticFrame=frame),null)
        }catch(e:Missing){val targets=listOf("kind:${e.kind}")+e.picked.map{(k,v)->"picked:$k=$v"}+e.candidates.map{it.toString()};return ask(e.prompt,RuhiPending("solid_measure",pending?.source ?: source,s.module.name,scene(s),targets))}
        catch(e:Dimension){return ask(e.message ?: "Supply the dimension.",RuhiPending("solid_measure_dimension",pending?.source ?: source,s.module.name,scene(s),listOf(e.field)+extra.map{(k,v)->"dim:$k=$v"}))}
        catch(e:RuntimeException){return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid solid measurement.",context=c,semanticFrame=frame),pending)}
    }
}
