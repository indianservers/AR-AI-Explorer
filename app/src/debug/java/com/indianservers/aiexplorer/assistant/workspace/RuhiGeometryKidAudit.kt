package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Shared device/JVM fixture. Each numbered request starts in a declared, reproducible scene. */
object RuhiGeometryKidAudit {
    data class Case(val id:Int,val text:String,val expected:String)
    data class Record(val id:Int,val status:String,val message:String,val errors:List<String>)
    fun base()=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(
        Solid(SolidType.Cube,2.0,position=Vec3(1.0,2.0,3.0)),
        Solid(SolidType.Cuboid,6.0,2.0,4.0),Solid(SolidType.Sphere,6.0,6.0,6.0,3.0,position=Vec3(3.0,2.0,1.0)),
        Solid(SolidType.Cylinder,4.0,5.0,4.0,2.0),Solid(SolidType.Cone,6.0,6.0,6.0,3.0),
        Solid(SolidType.Pyramid,4.0,7.0,4.0),Solid(SolidType.TriangularPrism,2.0,3.0,5.0),Solid(SolidType.Tetrahedron,2.0)),
        points3D=listOf(Point3D("a","A",Vec3(2.0,0.0,0.0)),Point3D("b","B",Vec3(0.0,3.0,0.0)),Point3D("c","C",Vec3(0.0,0.0,4.0))),
        vectors3D=listOf(Vector3D("ab",Vec3(2.0,0.0,0.0),Vec3(0.0,3.0,0.0)),Vector3D("ac",Vec3(2.0,0.0,0.0),Vec3(0.0,0.0,4.0))),
        labSessionValues=mapOf("ruhi.studio.appearance.4" to "red"))
    fun fixture(id:Int):Pair<WorkspaceState,RuhiSceneContext> {
        var s=base();var c=RuhiSceneContext(selectedSolidIndex=0)
        if(id in 1..29 || id in setOf(39,40))s=s.copy(solids=emptyList(),points3D=emptyList(),vectors3D=emptyList(),labSessionValues=emptyMap()).also{c=RuhiSceneContext()}
        if(id==22 || id==16) c=RuhiSceneContext()
        if(id in setOf(135,153))s=s.copy(solids=s.solids+Solid(SolidType.Cube,3.0))
        if(id in setOf(134,176,205))s=s.copy(solids=s.solids+Solid(SolidType.Sphere,2.0,radius=1.0))
        if(id==208)s=s.copy(solids=s.solids+Solid(SolidType.Cuboid,3.0))
        if(id in setOf(213,214,215))s=s.copy(vectors3D=s.vectors3D.take(1))
        if(id==286)s=s.copy(solids=s.solids.filter{it.type!=SolidType.Cone})
        if(id==287)c=RuhiSceneContext()
        if(id==185)c=RuhiSceneContext(selectedSolidIndex=4)
        if(id==210)c=RuhiSceneContext(selectedSolidIndex=5)
        if(id==186)s=s.copy(labSessionValues=s.labSessionValues+("ruhi.studio.appearance.2" to "blue"))
        if(id==296)c=RuhiSceneContext(selectedSolidIndex=2)
        return s to c
    }
    fun run(case:Case):Record {
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule();var(s,c)=fixture(case.id)
        // History and scene restoration requests need a prior change, not an empty undo stack.
        if(case.id in setOf(293,294,299)) {
            val seed=manager.execute(s,if(case.id==299)"Save this 3D scene." else "Color the cube red.",h,c);s=seed.state;c=seed.context
            if(case.id==294){val r=manager.execute(s,"Undo.",h,c);s=r.state;c=r.context}
            if(case.id==299){val r=manager.execute(s,"Move the cube 2 units to the right.",h,c);s=r.state;c=r.context}
        }
        val before=s;val r=manager.execute(s,case.text,h,c);val errors=mutableListOf<String>()
        fun verify(ok:Boolean,reason:String){if(!ok)errors+=reason}
        fun near(a:Double,b:Double)=abs(a-b)<1e-7
        fun contains(value:Double)=Regex("[-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?").findAll(r.message).mapNotNull{it.value.toDoubleOrNull()}.any{near(it,value)}
        verify(r.status.name==case.expected,"expected ${case.expected}, got ${r.status}: ${r.message}")
        verify(r.state.module==MathModule.Geometry3D,"Changed workspace")
        verify(r.state.functions==before.functions && r.state.shapes==before.shapes && r.state.surfaceLayers==before.surfaceLayers,"Changed unrelated workspace data")
        if(r.status!=GraphCommandStatus.APPLIED) verify(r.state.solids==before.solids && r.state.points3D==before.points3D && r.state.planes3D==before.planes3D && r.state.linears3D==before.linears3D && r.state.vectors3D==before.vectors3D,"Failed request mutated scene")
        if(r.status==GraphCommandStatus.APPLIED) {
            val q=r.state.solids.lastOrNull()
            val kinds=mapOf(1 to SolidType.Cube,2 to SolidType.Sphere,3 to SolidType.Cone,4 to SolidType.Cylinder,5 to SolidType.Cuboid,6 to SolidType.Pyramid,7 to SolidType.TriangularPyramid,8 to SolidType.TriangularPrism,9 to SolidType.Torus,10 to SolidType.Hemisphere,11 to SolidType.Ellipsoid,12 to SolidType.Capsule,14 to SolidType.Frustum,15 to SolidType.Cube,16 to SolidType.Sphere,17 to SolidType.Cylinder,18 to SolidType.Cone,19 to SolidType.Cuboid,20 to SolidType.Pyramid,21 to SolidType.TriangularPrism,22 to SolidType.Sphere,23 to SolidType.Cube,24 to SolidType.Cylinder)
            kinds[case.id]?.let{verify(q?.type==it,"Wrong created solid type; expected $it")}
            if(q!=null)when(case.id){
                15->verify(q.width==4.0,"Wrong cube side")
                16->verify(q.radius==3.0,"Wrong sphere radius")
                17->verify(q.radius==2.0&&q.height==5.0,"Wrong cylinder dimensions")
                18->verify(q.radius==3.0&&q.height==6.0,"Wrong cone dimensions")
                19->verify(q.width==6.0&&q.depth==4.0&&q.height==2.0,"Wrong box dimensions")
                20->verify(q.width==4.0&&q.depth==4.0&&q.height==7.0,"Wrong pyramid dimensions")
                21->verify(q.depth==5.0,"Wrong prism length")
                22->verify(q.radius==5.0,"Diameter must convert to radius")
                23->verify(q.width==2.5,"Decimal side incorrect")
                24->verify(q.height==8.0,"Wrong cylinder height")
            }
            when(case.id){
                in 1..24->{verify(r.state.solids.size==1,"Expected one created solid");verify(q!=null && SolidMeshFactory.create(q).vertices.isNotEmpty(),"Missing native mesh")}
                in 25..29->verify(r.state.points3D.size==1,"Point was not created")
                30,31,32->{
                    val kind=when(case.id){30->Linear3DKind.Line;31->Linear3DKind.Segment;else->Linear3DKind.Ray}
                    val refs=when(case.id){30->listOf("a","b");31->listOf("b","c");else->listOf("a","c")}
                    val obj=r.state.linears3D.singleOrNull()
                    verify(obj?.kind==kind&&obj.pointIds==refs,"Linear construction must retain the exact kind and ordered point IDs")
                    verify(r.state.points3D==before.points3D&&r.state.vectors3D==before.vectors3D&&r.state.solids==before.solids,"Linear construction changed other geometry")
                    val linear=obj?.let{runCatching{NativeLinearGeometry.evaluate(r.state,it)}.getOrNull()}
                    verify(linear!=null,"Missing current linear evaluation")
                    if(linear!=null) {
                        verify(linear.containsParameter(-1.0)==(case.id==30)&&linear.containsParameter(2.0)==(case.id!=31),"Wrong line/segment/ray mathematical domain")
                        val expected=when(case.id){30->Vec3(-2.0,6.0,0.0);31->Vec3(0.0,1.5,2.0);else->Vec3(-2.0,0.0,8.0)}
                        verify(linear.at(if(case.id==31).5 else 2.0)==expected,"Incorrect independently calculated linear coordinates")
                    }
                }
                33->verify(r.state.vectors3D.size==before.vectors3D.size+1 && r.state.vectors3D.last().end==Vec3(2.0,3.0,4.0),"Wrong vector endpoint")
                34,234->{
                    verify(r.state.planes3D.size==1&&r.state.planes3D.single().pointIds==listOf("a","b","c"),"Plane must reference A/B/C without adding helper points")
                    verify(r.state.points3D==before.points3D&&r.state.solids==before.solids,"Plane construction changed other geometry")
                    val plane=r.state.planes3D.singleOrNull()?.let{runCatching{NativePlaneGeometry.evaluate(r.state,it)}.getOrNull()}
                    verify(plane!=null&&plane.patch.size==4&&plane.patch.all{abs(it.x/2+it.y/3+it.z/4-1)<1e-8},"Plane patch does not satisfy x/2 + y/3 + z/4 = 1")
                }
                39->verify(r.state.solids.size==2 && r.state.solids[0].position!=r.state.solids[1].position,"Two cubes overlap at identical centers")
                40->verify(r.state.solids.map{it.radius}==listOf(1.0,2.0,3.0),"Different sphere sizes incorrect")
                41->verify(r.message.contains("8"),"Wrong solid count")
                42,43->verify(r.message.contains("1"),"Wrong shape count")
                51,54->verify(r.message.contains("6"),"Wrong faces")
                52->verify(r.message.contains("12"),"Wrong edges")
                53->verify(r.message.contains("8"),"Wrong vertices")
                55->verify(r.message.contains("9"),"Wrong prism edges")
                56->verify(r.message.contains("5"),"Wrong pyramid vertices")
                57->verify(r.message.contains("4"),"Wrong tetrahedron faces")
                58,59->verify(r.message.contains("0"),"Wrong zero vertices")
                71,211->verify(contains(sqrt(13.0)),"AB length should be sqrt(13)")
                72->verify(contains(4.0),"C distance should be 4")
                75->verify(r.state.points3D.last().position==Vec3(1.0,0.0,2.0),"Wrong midpoint")
                91->verify(contains(8.0),"Cube volume incorrect")
                92->verify(contains(48.0),"Box volume incorrect")
                93->verify(contains(36*PI),"Sphere volume incorrect")
                94->verify(contains(20*PI),"Cylinder volume incorrect")
                95->verify(contains(18*PI),"Cone volume incorrect")
                98->verify(contains(24.0),"Cube surface area incorrect")
                99->verify(contains(88.0),"Box surface area incorrect")
                102->verify(contains(20*PI),"Cylinder curved area incorrect")
                108->verify(contains(2.0),"Wrong cube edge")
                126->verify(r.state.solids[0].position==Vec3(3.0,2.0,3.0),"Wrong right translation")
                127->verify(r.state.solids[2].position==Vec3(3.0,2.0,4.0),"Z-up translation incorrect")
                128->verify(r.state.solids[4].position==Vec3(0.0,4.0,0.0),"Wrong Y translation")
                129->verify(r.state.solids[3].position==Vec3(-2.0,0.0,0.0),"Wrong negative X translation")
                130->verify(r.state.solids[2].position==Vec3(3.0,2.0,1.0),"Wrong absolute sphere center")
                131->verify(r.state.points3D[0].position==Vec3(4.0,5.0,6.0),"Wrong named point movement")
                133->verify(r.state.solids[0].position==Vec3(0.0,0.0,0.0),"Cube origin placement incorrect")
                137->verify(r.state.solids[1].position==Vec3(1.0,2.0,3.0),"Wrong translation vector")
                145->verify(r.state.solids.zip(before.solids).all{(a,b)->a.position==b.position+Vec3(0.0,0.0,5.0)},"Move everything omitted objects")
                146->verify(r.state.solids[0].rotation.z==90.0,"Wrong cube Z rotation")
                147->verify(r.state.solids[1].rotation.x==45.0,"Wrong box X rotation")
                148->verify(r.state.solids[4].rotation.y==180.0,"Wrong cone Y rotation")
                152->verify((r.state.points3D[0].position-Vec3(0.0,2.0,0.0)).magnitude()<1e-9,"Wrong point Z rotation")
                154->verify(r.state.solids[3].rotation.z==90.0,"Cylinder must turn from Y to X")
                155->verify(r.state.solids[4].rotation.x==180.0,"Downward cone must reverse its Y-directed axis")
                156->verify(r.state.solids[5].rotation.x==180.0,"Upside-down pyramid must reverse its Y-directed axis")
                162->verify(r.state.solids[0].position.x== -1.0,"Wrong YZ reflection")
                166->verify(near(Geometry3D.measure(r.state.solids[0]).volume,64.0),"Doubling sides must yield 8x volume")
                167->verify(near(r.state.solids[2].radius,1.5),"Wrong half-size sphere")
                168->verify(near(r.state.solids[3].height,10.0),"Wrong doubled cylinder height")
                170->verify(near(r.state.solids[0].width,5.0),"Wrong cube side")
                171->verify(near(r.state.solids[2].radius,4.0),"Wrong sphere radius")
                172->verify(near(r.state.solids[3].radius,3.0),"Diameter must convert to radius")
                173->verify(near(r.state.solids[4].height,10.0),"Wrong cone height")
                174->verify(r.state.solids[1].let{it.width==8.0&&it.depth==5.0&&it.height==3.0},"Box dimension ordering incorrect")
                178->verify(r.state.solids[3].let{it.height==2.5&&it.radius==2.0},"Halving height changed radius")
                181->verify(r.state.labSessionValues["ruhi.studio.appearance.0"]=="red","Wrong cube color")
                182->verify(r.state.labSessionValues["ruhi.studio.appearance.2"]=="blue","Wrong sphere color")
                188->verify(r.state.labSessionValues["ruhi.studio.appearance.3"]=="glass","Glass material missing")
                209->verify(r.context.selectedSolidIndex==null,"Selection not cleared")
                210,266->verify(r.state.solids.size==before.solids.size+1,"Copy not created")
                215->verify(near(r.state.vectors3D.last().magnitude,1.0),"Unit vector length incorrect")
                221->verify(contains(4.0),"AB dot AC should be 4")
                293->verify(r.state.labSessionValues["ruhi.studio.appearance.0"]==null,"Undo did not restore color")
                294->verify(r.state.labSessionValues["ruhi.studio.appearance.0"]=="red","Redo did not restore color")
                296->verify(r.state.solids.none{it.type==SolidType.Sphere},"Sphere not deleted")
                297->verify(r.state.solids.isEmpty()&&r.state.points3D==before.points3D&&r.state.vectors3D==before.vectors3D,"Clear did not preserve points/vectors")
                299->verify(r.state.solids==base().solids,"Saved solids not restored")
            }
            // Every generated mesh and every numeric result must remain finite.
            r.state.solids.forEach{solid->verify(SolidMeshFactory.create(solid).vertices.all{it.x.isFinite()&&it.y.isFinite()&&it.z.isFinite()},"Nonfinite mesh")}
        }
        return Record(case.id,r.status.name,r.message,errors)
    }
}
