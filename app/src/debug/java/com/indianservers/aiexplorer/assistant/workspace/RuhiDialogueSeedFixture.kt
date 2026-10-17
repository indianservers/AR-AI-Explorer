package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*

/** Source commands remain untouched. Their prerequisite conversations use the production manager. */
object RuhiDialogueSeedFixture {
    const val CASE="qa.ruhi.dialogue.case"
    const val FACTOR="qa.ruhi.dialogue.factor"
    private const val SHAPE="qa.ruhi.dialogue.shape"
    private const val SOLID="qa.ruhi.dialogue.solid"
    private const val FUNCTION="qa.ruhi.dialogue.function"
    fun context(s:WorkspaceState)=RuhiSceneContext(selectedShapeId=s.labSessionValues[SHAPE]?.takeIf{it.isNotBlank()},selectedSolidIndex=s.labSessionValues[SOLID]?.toIntOrNull(),selectedFunctionIds=s.labSessionValues[FUNCTION]?.takeIf{it.isNotBlank()}?.let(::listOf).orEmpty())
    fun state(id:Int,variant:Int=0):WorkspaceState {
        require(id in 476..500);require(variant in 0..7)
        val v=variant.toDouble();val factor=1.25+v/8;val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
        var s=WorkspaceState(id="dialogue-seed-$id-$variant",module=MathModule.Geometry2D);var c=RuhiSceneContext()
        fun say(text:String,status:GraphCommandStatus=GraphCommandStatus.APPLIED) {
            val r=manager.execute(s,text,history,c);check(r.status==status){"Fixture $id prerequisite $text: ${r.status}: ${r.message}"};s=r.state;c=r.context
        }
        fun graph(existing:Boolean=false) {s=s.copy(module=MathModule.Graph2D,functions=if(existing)listOf(FunctionDefinition("existing,/907","Existing_$variant","y=2*x+5","#FF0000"))else emptyList());c=RuhiSceneContext()}
        fun triangle(dependent:Boolean=false) {
            val x=6.0+v;val y=6.0+2*v
            s=s.copy(points=listOf(Vec2(0.0,0.0),Vec2(x,0.0),if(dependent)Vec2(x/2,y/2)else Vec2(0.0,4.0+v),Vec2(0.0,y),Vec2(20.0,30.0)),shapes=listOf(Shape2D("dialogue-shape,/$variant",Shape2DType.Triangle,listOf(0,1,2),"Roof_$variant")),pointDependencies=if(dependent)listOf(PointDependency(2,listOf(1,3),PointDependencyType.Midpoint))else emptyList(),solids=listOf(Solid(SolidType.Cube,2.0)),labSessionValues=(0..4).associate{"ruhi.studio.point.${listOf("Anchor","End","Middle","Parent","Unrelated")[it]}_$variant" to it.toString()})
            c=RuhiSceneContext(selectedShapeId=s.shapes.single().id)
        }
        fun twoSolids(cubes:Boolean=false) {
            val solids=if(cubes)listOf(Solid(SolidType.Cube,2.0+v/4),Solid(SolidType.Cube,3.0+v/4))else listOf(Solid(SolidType.Cube,2.0+v/4),Solid(SolidType.Cone,3.0+v/2))
            s=s.copy(module=MathModule.Geometry3D,solids=if(variant%2==0||cubes)solids else solids.reversed());c=RuhiSceneContext()
        }
        when(id) {
            476->{graph(true)}
            477,478->{s=s.copy(module=MathModule.Geometry3D);say("Create a cube with side length ${2+v/4}");say("Create a sphere with radius ${1+v/4}");say("Create a cube with side length ${3+v/4}");c=RuhiSceneContext()}
            479->{twoSolids();c=RuhiSceneContext(selectedSolidIndex=0);say("Scale the selected object by $factor")}
            480->{s=s.copy(module=MathModule.Geometry3D);say("Create a sphere with radius ${1.75+v/4}");say("Find the volume of a cylinder with height ${4+v}",GraphCommandStatus.CLARIFICATION)}
            481->{s=s.copy(module=MathModule.Geometry3D);say("Find the volume of a cone with radius ${1.5+v/4}",GraphCommandStatus.CLARIFICATION)}
            482->{twoSolids(cubes=true);s=s.copy(labSessionValues=s.labSessionValues+mapOf("ruhi.studio.appearance.0" to "red","ruhi.studio.appearance.1" to "blue"));say("Move the cube to (3, 2, 1)",GraphCommandStatus.CLARIFICATION);c=RuhiSceneContext()}
            483,484->{twoSolids();say("Scale it by $factor",GraphCommandStatus.CLARIFICATION);c=RuhiSceneContext()}
            485,486,487,490->{graph(true);say("Draw a graph",GraphCommandStatus.CLARIFICATION)}
            488->{s=s.copy(solids=listOf(Solid(SolidType.Cube,2.0)))}
            489,492,494->{triangle()}
            491->{graph();say("Compare two functions",GraphCommandStatus.CLARIFICATION)}
            493->{graph();say("Plot y = x^2+${1+v}");val first=s.functions.single().id;say("Plot y = 2*x+${3+2*v}");val second=s.functions.last().id;s=s.copy(functions=s.functions.map{it.copy(name=if(it.id==first)"Earlier_$variant"else"Current_$variant")}.reversed());c=RuhiSceneContext(selectedFunctionIds=listOf(second))}
            495->{triangle();s=s.copy(points=listOf(Vec2(4.0+v,1.0),Vec2(1.0,1.0),Vec2(1.0,6.0+v),Vec2(20.0,30.0),Vec2(1.0,3.5+v/2)),pointDependencies=listOf(PointDependency(4,listOf(1,2),PointDependencyType.Midpoint)),labSessionValues=s.labSessionValues+(RuhiAngleLanguage.PREFIX+"context" to "0,1,2|interior|"))}
            496->{triangle();s=s.copy(labSessionValues=s.labSessionValues+(RuhiSceneEditLanguage.PREFIX+"measure.${s.shapes.single().id}" to "false"))}
            497->{triangle();say("Use meters for coordinate units")}
            498->{twoSolids();val cube=s.solids.indexOfFirst{it.type==SolidType.Cube};s=s.copy(labSessionValues=s.labSessionValues+("ruhi.studio.appearance.$cube" to "blue"));say("Scale it by $factor",GraphCommandStatus.CLARIFICATION);c=RuhiSceneContext()}
            499->{twoSolids();c=RuhiSceneContext(selectedSolidIndex=0);say("Scale the selected object by -1",GraphCommandStatus.INVALID)}
            500->{triangle(dependent=true)}
        }
        // Persistence is part of the prerequisite, not a fabricated pending record.
        s=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state ?: error("Fixture did not restore")
        return s.copy(labSessionValues=s.labSessionValues+mapOf(CASE to id.toString(),FACTOR to factor.toString(),SHAPE to c.selectedShapeId.orEmpty(),SOLID to c.selectedSolidIndex?.toString().orEmpty(),FUNCTION to c.selectedFunctionIds.singleOrNull().orEmpty()))
    }
}
