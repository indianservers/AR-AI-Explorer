package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Surface language constructs ordinary native layers; the model never creates mesh vertices. */
object RuhiSurfaceLanguage {
    private class MissingParameters(names:Set<String>):IllegalArgumentException("What numeric values should I use for ${names.joinToString()}? Supply the complete z equation with those values.")
    data class Parsed(val expressions:List<String>,val domain:SurfaceDomain3D,val wireframe:Boolean,val parameters:Map<String,Any>,val type:String="surface")
    private fun text(v:String)=MathExpressionNormalizer.normalize(RuhiMathLanguage.normalize(v))
    fun accepts(v:String):Boolean {
        if(v.length>2000)return false
        val t=text(v)
        if(';' in t||Regex("\\bthen\\b|\\band (?:move|rotate|delete|scale|color|group)\\b").containsMatchIn(t))return false
        return Regex("^(?:plot|graph|draw|show|display|render|create|make)\\b").containsMatchIn(t)&&(Regex("\\bz\\s*=").containsMatchIn(t)||Regex("^(?:render|draw|show|create) (?:a )?gaussian hill$").matches(t))
    }
    fun expression(v:String):String {
        var source=text(v).replace(Regex("\\|([^|]+)\\|")){"abs(${it.groupValues[1]})"}
        require('|' !in source){"Use balanced absolute-value bars."}
        source=source.replace(Regex("(?<![A-Za-z_])[xy]{2,8}(?![A-Za-z_0-9])")){it.value.toList().joinToString("*")}
        val tokens=Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?|[A-Za-z_][A-Za-z_0-9]*|[^\\s]").findAll(source).map{it.value}.toList()
        val variables=setOf("x","y","pi","e");fun number(t:String)=t.toDoubleOrNull()!=null
        return buildString{tokens.forEachIndexed{i,t->val previous=tokens.getOrNull(i-1);if(previous!=null&&(number(previous)||previous==")"||previous in variables)&&(t=="("||t.firstOrNull()?.isLetter()==true||number(t)))append('*');append(t)}}
    }
    fun parse(source:String):Parsed {
        var t=text(source);val params=linkedMapOf<String,Any>();val wire=t.endsWith(" as wireframe")
        if(wire){params["style"]="wireframe";t=t.removeSuffix(" as wireframe")}
        t=t.removeSuffix(" where defined").removeSuffix(" without connecting the singularity").removeSuffix(" together")
        var domain=SurfaceDomain3D()
        val square=Regex("\\s+on the square \\[([^,]+),([^]]+)\\]\\s*(?:\\*|x|by)\\s*\\[([^,]+),([^]]+)\\]$").find(t)
        if(square!=null){val bounds=square.groupValues.drop(1).map{ExpressionEngine().compile(expression(it)).eval(emptyMap())};require(bounds.all{it.isFinite()&&abs(it)<=10000}&&bounds[0]<bounds[1]&&bounds[2]<bounds[3]){"Use finite increasing x/y domains within 10000 units."};domain=SurfaceDomain3D(bounds[0],bounds[1],bounds[2],bounds[3]);params["x_domain"]=bounds.take(2);params["y_domain"]=bounds.drop(2);t=t.substring(0,square.range.first)}
        val body=Regex("\\bz\\s*=\\s*(.+)$").find(t)?.groupValues?.get(1)
        val formulas=if(body==null){require(Regex("^(?:render|draw|show|create) (?:a )?gaussian hill$").matches(t)){"Supply an explicit z=f(x,y) equation."};listOf("exp(-(x^2+y^2))")}else body.split(Regex("\\s+and\\s+z\\s*=\\s*")).map(::expression)
        require(formulas.size in 1..2){"Create one or two explicit surfaces per instruction."}
        if(formulas.size==1)params["expression"]=formulas.single()else params["expressions"]=formulas
        val hemisphere=if(t.contains("hemisphere"))Regex("^sqrt\\(([-+0-9.]+)-x\\^2-y\\^2\\)$").matchEntire(formulas.single())else null
        if(hemisphere!=null){val squared=hemisphere.groupValues[1].toDouble();require(squared.isFinite()&&squared>0&&squared<=1e8);params["domain"]="x^2+y^2<=${hemisphere.groupValues[1]}";if(square==null){val radius=sqrt(squared);domain=SurfaceDomain3D(-radius,radius,-radius,radius)}}
        val type=when{t.contains("hemisphere")->"hemisphere";t.contains("cone")->"cone";t.contains("paraboloid")->"paraboloid";t.contains("plane")->"plane";else->"surface"}
        return Parsed(formulas,domain,wire,params,type)
    }
    private fun validate(formula:String) {
        require(formula.length<=1200){"Keep surface formulas within 1200 characters."};var depth=0
        formula.forEach{if(it=='(')depth++;if(it==')')depth--;require(depth in 0..64){"Use balanced formulas with at most 64 nesting levels."}};require(depth==0)
        val known=setOf("x","y","pi","e","infinity")
        val withoutNumbers=formula.replace(Regex("(?<![A-Za-z_0-9])(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?")," ")
        val unknown=Regex("[A-Za-z_][A-Za-z_0-9]*").findAll(withoutNumbers).filter{it.value !in known&&withoutNumbers.getOrNull(it.range.last+1)!='('}.map{it.value}.toSet()
        if(unknown.isNotEmpty())throw MissingParameters(unknown)
        ExpressionEngine().compile(formula)
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality):RuhiExecutionResult? {
        if(s.module!=MathModule.Graph3D||!accepts(source))return null
        var frame=RuhiSemanticFrame("PLOT_SURFACE_3D",RuhiEngine.GRAPH_3D,emptyMap())
        return try {
            val p=parse(source);if(p.expressions.any{Regex("\\bz\\b").containsMatchIn(it)})return null;frame=frame.copy(parameters=p.parameters);p.expressions.forEach(::validate)
            require(s.surfaceLayers.size+p.expressions.size<=24){"The graph layer budget is 24; delete layers before creating more."}
            val taken=(s.surfaceLayers.map{it.id}+s.functions.map{it.id}+s.universalMathDocument?.objects.orEmpty().keys).toMutableSet()
            val layers=p.expressions.mapIndexed{i,formula->val id=generateSequence(1){it+1}.map{"ruhi-surface-$it"}.first{taken.add(it)};SpatialSurfaceLayer(id,formula,domain=p.domain,colorIndex=s.surfaceLayers.size+i,renderMode=if(p.wireframe)SpatialSurfaceRenderMode.Wireframe else SpatialSurfaceRenderMode.SurfaceMesh)}
            layers.forEach{layer->val mesh=GraphSurfaceGeometry.mesh(layer,GraphSurfaceGeometry.sceneDensity(24,s.surfaceLayers.count{it.visible}+layers.size));require(mesh.vertices.isNotEmpty()&&(mesh.triangleIndices.isNotEmpty()||mesh.rows>1)){"This equation has no drawable finite surface in the requested domain."};require(mesh.vertices.all{it.x.isFinite()&&it.y.isFinite()&&it.z.isFinite()&&abs(it.z)<=1e9}){"The surface exceeds the numeric display range; narrow its domain."}}
            val values=s.labSessionValues+layers.associate{GraphAssistant.TYPE_PREFIX+it.id to p.type}+(GraphAssistant.SELECTION to layers.last().id)
            val next=s.copy(surfaceLayers=s.surfaceLayers+layers,surfaceExpression=s.surfaceLayers.firstOrNull()?.expression ?: layers.first().expression,labSessionValues=values)
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: create explicit surfaces"))
            val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Plotted ${layers.size} native 3D surface${if(layers.size==1)""else"s"}: ${p.expressions.joinToString{"z=$it"}}. Undefined cells and zero-crossing denominator cells are omitted; existing layers are preserved.",RuhiCommand(engine=RuhiEngine.GRAPH_3D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c,semanticFrame=frame)
        }catch(e:MissingParameters){RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Supply numeric parameter values.",context=c,semanticFrame=frame)}
        catch(e:RuntimeException){RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid surface request.",context=c,semanticFrame=frame)}
    }
}
