package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*

/** A reproducible scene, not ground truth for missing engine features. */
object RuhiSeed500Fixture {
    val labels=('A'..'Z').map(Char::toString)+listOf("A1","B2","Alpha7","Beta9")
    val coordinates=labels.indices.map{i->Vec3(i.toDouble(),(i*i%17).toDouble(),(i%5).toDouble())}
    fun module(engine:String)=when(engine){"GEOMETRY_3D"->MathModule.Geometry3D;"GRAPH_3D"->MathModule.Graph3D;"GRAPH_2D"->MathModule.Graph2D;else->MathModule.Geometry2D}
    fun state(engine:String,caseId:Int?=null):WorkspaceState {
        if(caseId in 476..500)return RuhiDialogueSeedFixture.state(caseId!!)
        val activeEngine=if(engine=="ALL")when(caseId){451,454,456,457,471->"GRAPH_2D";470->"GRAPH_3D";else->"GEOMETRY_2D"}else engine
        val base=WorkspaceState(module=module(activeEngine),points=coordinates.map{Vec2(it.x,it.y)},points3D=labels.zip(coordinates).map{(name,position)->Point3D(name,name,position)},
        shapes=listOf(Shape2D("line-ab",Shape2DType.Segment,listOf(0,1),"AB",styleKey="#0000FF"),Shape2D("triangle-abc",Shape2DType.Triangle,listOf(0,1,2),"ABC")),
        functions=listOf(FunctionDefinition("curve-one","f","y = x^2","#0000FF"),FunctionDefinition("curve-two","g","y = x","#FF0000")),solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,6.0,radius=3.0),Solid(SolidType.Cylinder,4.0,8.0,radius=2.0),Solid(SolidType.Cone,6.0,4.0,radius=3.0)),
        labSessionValues=labels.withIndex().associate{"ruhi.studio.point.${it.value}" to it.index.toString()})
        // A requested new label must be free. Its old unlabelled coordinate stays intact.
        return when(caseId){
            454->base.copy(labSessionValues=base.labSessionValues+(RuhiSceneEditLanguage.PREFIX+"axes" to "false"))
            455,463->base.copy(shapes=base.shapes+Shape2D("scene-circle",Shape2DType.Circle,listOf(0,1),"Orbit"))
            457->base.copy(labSessionValues=base.labSessionValues+(GraphAssistant.selectionKey(MathModule.Graph2D) to "curve-one"))
            458->base.copy(labSessionValues=base.labSessionValues+(RuhiSceneEditLanguage.PREFIX+"labels" to "false"))
            466->base.copy(shapes=base.shapes.mapIndexed{i,q->if(i==0)q.copy(styleKey="#FF0000")else q})
            475->base.copy(shapes=base.shapes.map{it.copy(styleKey="#FF0000")})
            436->base.copy(solids=base.solids+Solid(SolidType.Pyramid,4.0,7.0,4.0))
            437->base.copy(solids=base.solids.mapIndexed{i,q->q.copy(position=Vec3(i*3.0,1.0,0.0))},labSessionValues=base.labSessionValues+("ruhi.geometry3d.selection" to "0,1"))
            438->base.copy(solids=base.solids.mapIndexed{i,q->if(i==0)q.copy(position=Vec3(2.0,3.0,4.0),rotation=Vec3(13.0,27.0,41.0))else q})
            439->base.copy(solids=base.solids.mapIndexed{i,q->if(i==0)q.copy(rotation=Vec3(15.0,25.0,35.0))else q})
            440->base.copy(solids=base.solids+Solid(SolidType.Torus,4.0,radius=1.0))
            442->base.copy(solids=base.solids+Solid(SolidType.Cuboid,2.0,3.0,4.0))
            449->base.copy(labSessionValues=base.labSessionValues+(RuhiTransform3DLanguage.PREFIX+"explosion.0" to "1.0"))
            450->base.copy(solids=base.solids.mapIndexed{i,q->if(i==0)q.copy(rotation=Vec3(45.0,0.0,0.0))else q})
            409->base.copy(solids=base.solids+Solid(SolidType.Cuboid,3.0,4.0,12.0))
            311,312,330,335,337,342,345,364,372->base.copy(labSessionValues=base.labSessionValues+(GraphAssistant.selectionKey(MathModule.Graph2D) to "curve-one"))
            314->base.copy(points=base.points.mapIndexed{i,p->when(i){0->Vec2(-1.0,0.0);1->Vec2(1.0,0.0);else->p}}+Vec2(3.0,0.0),shapes=base.shapes+listOf(Shape2D("circle-a",Shape2DType.Circle,listOf(0,1),"C1"),Shape2D("circle-b",Shape2DType.Circle,listOf(1,base.points.size),"C2")))
            315->base.copy(points=base.points.mapIndexed{i,p->when(i){0->Vec2(-3.0,0.0);1->Vec2(3.0,0.0);else->p}}+listOf(Vec2(0.0,0.0),Vec2(2.0,0.0)),shapes=base.shapes+Shape2D("circle-c1",Shape2DType.Circle,listOf(base.points.size,base.points.size+1),"C1"))
            259->base.copy(labSessionValues=base.labSessionValues.filterKeys{it !in listOf("ruhi.studio.point.A","ruhi.studio.point.B","ruhi.studio.point.C")})
            273->base.copy(labSessionValues=base.labSessionValues.filterKeys{it !in listOf("ruhi.studio.point.D","ruhi.studio.point.E","ruhi.studio.point.F")})
            268->base.copy(points=base.points.mapIndexed{i,p->when(i){0->Vec2(0.0,0.0);1->Vec2(6.0,0.0);else->p}})
            168->base.copy(labSessionValues=base.labSessionValues-"ruhi.studio.point.M")
            185->base.copy(points=base.points.mapIndexed{i,p->if(i==1)Vec2(0.0,4.0)else p})
            186->base.copy(shapes=base.shapes+Shape2D("line-cd",Shape2DType.Segment,listOf(2,3),"CD",styleKey="#FF0000"))
            208->base.copy(shapes=base.shapes+Shape2D("triangle-def",Shape2DType.Triangle,listOf(3,4,5),"DEF"))
            205->base.copy(shapes=base.shapes+listOf(Shape2D("vector-u",Shape2DType.Vector,listOf(0,1),"u"),Shape2D("vector-v",Shape2DType.Vector,listOf(0,2),"v")))
            211->base.copy(shapes=base.shapes+listOf(Shape2D("line-l",Shape2DType.Line,listOf(0,1),"l"),Shape2D("line-m",Shape2DType.Line,listOf(2,3),"m")))
            212->base.copy(points=base.points.mapIndexed{i,p->when(i){0->Vec2(4.0,0.0);1->Vec2(0.0,0.0);2->Vec2(0.0,3.0);else->p}})
            218->base.copy(shapes=base.shapes+Shape2D("quad-abcd",Shape2DType.Polygon,listOf(0,1,2,3),"ABCD"))
            219->base.copy(points=base.points.mapIndexed{i,p->when(i){0->Vec2(2.0,0.0);1->Vec2(0.0,3.0);else->p}})
            220->base.copy(module=MathModule.Geometry3D,vectors3D=listOf(Vector3D("vector-907",Vec3(1.0,1.0,1.0),Vec3(4.0,1.0,5.0),"Arrow_907"),Vector3D("vector-811",Vec3(-2.0,1.0,0.0),Vec3(-2.0,5.0,3.0),"Arrow_811")))
            221->base.copy(module=MathModule.Geometry3D,points3D=base.points3D+listOf(Point3D("plane-r","Corner_907",Vec3(1.0,2.0,3.0)),Point3D("plane-s","Edge_811",Vec3(3.0,2.0,3.0)),Point3D("plane-t","Floor_662",Vec3(1.0,4.0,3.0)),Point3D("plane-u","Roof_553",Vec3(1.0,3.0,4.0))),planes3D=listOf(Plane3DObject("plane-floor","Floor",listOf("plane-r","plane-s","plane-t")),Plane3DObject("plane-wall","Wall",listOf("plane-r","plane-s","plane-u"))))
            224->base.copy(shapes=base.shapes+Shape2D("triangle-pqr",Shape2DType.Triangle,listOf(15,16,17),"PQR"))
            225->base.copy(shapes=base.shapes+Shape2D("segment-bc",Shape2DType.Segment,listOf(1,2),"BC"))
            234,239,240,243,248->base.copy(points=base.points+listOf(Vec2(1.0,2.0),Vec2(4.0,2.0),Vec2(-2.0,-1.0),Vec2(4.0,-1.0)),shapes=base.shapes+listOf(Shape2D("circle-c1",Shape2DType.Circle,listOf(base.points.size,base.points.size+1),"C1"),Shape2D("circle-c2",Shape2DType.Circle,listOf(base.points.size+2,base.points.size+3),"C2")))
            else->base
        }
    }
    fun context(caseId:Int?=null,state:WorkspaceState?=null)=if(caseId in 476..500&&state!=null)RuhiDialogueSeedFixture.context(state)else when(caseId){
        452,459->RuhiSceneContext(selectedShapeId="triangle-abc")
        455,463->RuhiSceneContext(selectedShapeId="scene-circle")
        457->RuhiSceneContext(selectedFunctionIds=listOf("curve-one"))
        429,447->RuhiSceneContext(selectedSolidIndex=2)
        433,435->RuhiSceneContext(selectedSolidIndex=1)
        401,412->RuhiSceneContext(selectedSolidIndex=1)
        406,421->RuhiSceneContext(selectedSolidIndex=3)
        409->RuhiSceneContext(selectedSolidIndex=4)
        411,419->RuhiSceneContext(selectedSolidIndex=2)
        335,345->RuhiSceneContext(selectedPointIndex=1,selectedFunctionIds=listOf("curve-one"))
        206,209->RuhiSceneContext(selectedShapeId="triangle-abc",selectedPointIndex=1)
        212->RuhiSceneContext(selectedShapeId="triangle-abc")
        218->RuhiSceneContext(selectedShapeId="quad-abcd")
        220->RuhiSceneContext(selectedVectorIds=listOf("vector-907","vector-811"))
        221->RuhiSceneContext(selectedPlaneIds=listOf("plane-floor","plane-wall"))
        240,248->RuhiSceneContext(selectedShapeId="circle-c1")
        316,317,318,325->RuhiSceneContext(selectedFunctionIds=listOf("curve-one","curve-two"))
        249,269->RuhiSceneContext(selectedShapeId="triangle-abc")
        else->RuhiSceneContext(selectedShapeId="line-ab",selectedSolidIndex=0)
    }
    /** Row 450 explicitly refers to a prior rotation; provide that history without rewriting the command. */
    fun history(caseId:Int,state:WorkspaceState)=WorkspaceHistoryByModule().also{h->
        if(caseId in setOf(466,467)){
            val previous=state.copy(shapes=state.shapes.mapIndexed{i,q->if(i==0)q.copy(styleKey="#0000FF")else q})
            val changed=previous.copy(shapes=previous.shapes.mapIndexed{i,q->if(i==0)q.copy(styleKey="#FF0000")else q})
            h.forModule(state.module).recordApplied(GraphWorkspaceCommand(previous,changed,"Fixture: color line red"))
            if(caseId==467)h.forModule(state.module).undo(changed)
        }
        if(caseId==450){val previous=state.copy(solids=state.solids.mapIndexed{i,q->if(i==0)q.copy(rotation=Vec3(0.0,0.0,0.0))else q});h.forModule(state.module).recordApplied(RuhiSolidTransformCommand(previous,state,RuhiTransform3DLanguage.ROTATION_LABEL+" fixture x45"))}}
}
