package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*

enum class RuhiEngine(val module: MathModule) {
    GRAPH_2D(MathModule.Graph2D), GRAPH_3D(MathModule.Graph3D), GEOMETRY_2D(MathModule.Geometry2D), GEOMETRY_3D(MathModule.Geometry3D);
    companion object { fun from(module:MathModule)=entries.firstOrNull{it.module==module} }
}
enum class RuhiInputModality { TEXT, VOICE_TRANSCRIPT }
data class RuhiEnginePrediction(val engine: RuhiEngine?, val confidence: Float, val margin: Float, val contextual: Boolean = false)
fun interface RuhiEngineClassifier { fun predict(text:String):RuhiEnginePrediction }
object RuhiLanguageServices { @Volatile var engineClassifier:RuhiEngineClassifier?=null; @Volatile var dialogueClassifier:GraphIntentClassifier?=null }
data class RuhiSceneContext(val selectedShapeId:String?=null,val selectedSolidIndex:Int?=null)
enum class RuhiMeasure { AREA, PERIMETER, VOLUME, SURFACE_AREA, EXPLAIN }
sealed interface RuhiOperation {
    data object ShowVertex:RuhiOperation
    data class GraphProgram(val actions:List<GraphAction>):RuhiOperation
    data class CreatePoint(val name:String,val position:Vec2):RuhiOperation
    data class CreateShape(val type:Shape2DType,val dimensions:List<Double>,val center:Vec2=Vec2(0.0,0.0)):RuhiOperation
    data class Distance(val first:String,val second:String):RuhiOperation
    data class Midpoint(val first:String,val second:String):RuhiOperation
    data class ConnectPoints(val first:String,val second:String):RuhiOperation
    data class CreateSolid(val solid:Solid,val animation:RuhiAngularMotion?=null):RuhiOperation
    data class SetPosition(val center:Vec3):RuhiOperation
    data class Translate(val delta:Vec3):RuhiOperation
    data class Scale(val factor:Double):RuhiOperation
    data class Rotate(val axis:String,val degrees:Double):RuhiOperation
    data class Measure(val kind:RuhiMeasure):RuhiOperation
    data class SetColor(val hex:String):RuhiOperation
    data class History(val redo:Boolean,val count:Int):RuhiOperation
    data object DeleteSelected:RuhiOperation
    data object StopMotion:RuhiOperation
}
data class RuhiCommand(
    val schemaVersion:Int=1,val engine:RuhiEngine,val operation:RuhiOperation,
    val modality:RuhiInputModality=RuhiInputModality.TEXT,val targetObjectId:String?=null,
    val engineProposal:RuhiEnginePrediction?=null,val intentProposal:GraphIntentPrediction?=null,val routingReason:String="active scene context",
) { init { require(schemaVersion==1) { "Unsupported Ruhi command schema." } } }
data class RuhiExecutionResult(val state:WorkspaceState,val status:GraphCommandStatus,val message:String,val command:RuhiCommand?=null,val context:RuhiSceneContext=RuhiSceneContext(),val dialogue:RuhiDialogueView?=null)

/** Time and angular velocity determine pose. No learned pixels or per-frame history entries. */
data class RuhiAngularMotion(val axis:String="y",val degreesPerSecond:Double=30.0,val startMillis:Long=0L) {
    init { require(axis in setOf("x","y","z") && degreesPerSecond.isFinite() && kotlin.math.abs(degreesPerSecond)<=360) }
    fun encode()="$axis,$degreesPerSecond,$startMillis"
    fun angle(nowMillis:Long)=((nowMillis-startMillis).coerceAtLeast(0)/1000.0*degreesPerSecond)%360
    companion object { fun decode(s:String):RuhiAngularMotion { val v=s.split(',');require(v.size==3);return RuhiAngularMotion(v[0],v[1].toDouble(),v[2].toLong()) } }
}
object RuhiMotionEngine {
    const val PREFIX="ruhi.studio.motion.solid."
    fun solids(state:WorkspaceState,nowMillis:Long):List<Solid> = state.solids.mapIndexed { index,solid ->
        val motion=state.labSessionValues[PREFIX+index]?.let { runCatching { RuhiAngularMotion.decode(it) }.getOrNull() } ?: return@mapIndexed solid
        val angle=motion.angle(nowMillis);solid.copy(rotation=solid.rotation+when(motion.axis){"x"->Vec3(angle,0.0,0.0);"y"->Vec3(0.0,angle,0.0);else->Vec3(0.0,0.0,angle)})
    }
}
