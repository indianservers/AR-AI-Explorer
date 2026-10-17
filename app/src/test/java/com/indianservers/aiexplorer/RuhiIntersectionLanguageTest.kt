package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiIntersectionLanguageTest {
    @Test fun implicitEquationsIntersectCurrentExplicitCurvesWithoutCreatingObjects() {
        for(radius in listOf(1.25,2.0,3.7)) {
            val s=empty().copy(functions=listOf(FunctionDefinition("implicit,/$radius","Orbit_907","x^2+y^2=$radius^2","cyan"),FunctionDefinition("explicit,/$radius","Diagonal_811","y=x","red")))
            val r=query(s,"Where do Orbit_907 and Diagonal_811 intersect?")
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            val wanted=radius/sqrt(2.0);val p=points(r.state);assertEquals(2,p.size)
            assertTrue(p.any{it.distanceTo(Vec2(wanted,wanted))<1e-6});assertTrue(p.any{it.distanceTo(Vec2(-wanted,-wanted))<1e-6})
            assertEquals(s.functions,r.state.functions);assertEquals(s.points,r.state.points)
            val changed=r.state.copy(functions=r.state.functions.map{if(it.id.startsWith("explicit"))it.copy(expression="y=0")else it})
            assertTrue(points(changed).all{abs(abs(it.x)-radius)<1e-6&&abs(it.y)<1e-6})
            val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(changed),recover=false).state!!
            assertEquals(points(changed),points(loaded))
        }
    }
    @Test fun implicitVerticalIntersectionsUseTheVisibleYRangeAndDeduplicateTangency() {
        val circle=RuhiIntersectionMath.native("x^2+y^2=4")
        val crossing=RuhiIntersectionMath.solve(circle,RuhiIntersectionMath.Curve.Vertical(1.0),-7.0,7.0,-4.0,4.0)
        assertEquals(2,crossing.points.size);assertTrue(crossing.points.all{abs(it.x-1)<1e-8&&abs(abs(it.y)-sqrt(3.0))<1e-6})
        assertEquals(listOf(Vec2(2.0,0.0)),RuhiIntersectionMath.solve(circle,RuhiIntersectionMath.Curve.Vertical(2.0),-7.0,7.0,-4.0,4.0).points)
        assertTrue(RuhiIntersectionMath.solve(circle,RuhiIntersectionMath.Curve.Vertical(3.0),-7.0,7.0,-4.0,4.0).points.isEmpty())
        val upper=RuhiIntersectionMath.solve(circle,RuhiIntersectionMath.Curve.Vertical(1.0),-7.0,7.0,1.0,3.0)
        assertEquals(1,upper.points.size);assertEquals(sqrt(3.0),upper.points.single().y,1e-6)
        val restricted=RuhiIntersectionMath.solve(circle,RuhiIntersectionMath.Curve.Scalar("0","x>0"),-4.0,4.0)
        assertEquals(listOf(Vec2(2.0,0.0)),restricted.points)
    }
    @Test fun implicitCandidatesMustSatisfyOriginalEquationsAndContinuousDomains() {
        fun solve(a:String,b:String)=RuhiIntersectionMath.solve(RuhiIntersectionMath.native(a),RuhiIntersectionMath.native(b),-4.0,4.0,-4.0,4.0)
        assertTrue(solve("y=1/(x-1)","y=0").points.isEmpty())
        assertTrue(solve("y/(x-1)=1/(x-1)","y=0").points.isEmpty())
        assertTrue(solve("x^2+y^2=-1","y=x").points.isEmpty())
        assertEquals(listOf(Vec2(4.0,2.0)),solve("y^2=x","y=x-2").points.filter{it.y>=0})
        assertTrue(runCatching{solve("x*y=0","y=0")}.isFailure)
        // A predicate name containing y must not be replaced as if it were the variable y.
        assertTrue(solve("abs(y)=x^2+1","y=0").points.isEmpty())
        assertTrue(solve("1e-10*(x^2+y^2+1)=0","y=0").points.isEmpty())
        val scaled=solve("1e-10*(x^2+y^2-4)=0","y=x").points
        assertEquals(2,scaled.size);assertTrue(scaled.all{abs(abs(it.x)-sqrt(2.0))<1e-6&&abs(it.y-it.x)<1e-8})
        assertTrue(runCatching{solve("x^2+y^2=4 {x>0}","y=0")}.isFailure)
    }
    private fun empty()=WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList())
    private fun curves()=empty().copy(functions=listOf(FunctionDefinition("first,/907","First_907","y=x^2","#0000FF"),FunctionDefinition("second,/811","One","y=x","#FF0000")))
    private fun points(s:WorkspaceState)=requireNotNull(RuhiIntersectionLanguage.drawing(s)).points
    private fun query(s:WorkspaceState,text:String,c:RuhiSceneContext=RuhiSceneContext())=RuhiDialogueManager().execute(s,text,WorkspaceHistoryByModule(),c)
    @Test fun all25OriginalRowsMeetUnchangedSlotsCoordinatesAndMarkerPoliciesWithPersistenceAndHistory() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="FIND_INTERSECTION"};assertEquals(25,rows.size)
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager();val r=manager.execute(s,decode(row[1]),h,c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c);assertTrue("${row[0]} ${check.diagnostics}",check.verified);assertEquals(1,h.forModule(s.module).protocol.size)
            val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false);assertNotNull(saved.state);assertEquals(points(r.state),points(saved.state!!));assertEquals(r.state.functions,saved.state!!.functions);assertEquals(r.state.points,saved.state!!.points)
            val undo=manager.execute(r.state,"Undo.",h,r.context);assertNull(RuhiIntersectionLanguage.drawing(undo.state));assertEquals(s.functions,undo.state.functions);val redo=manager.execute(undo.state,"Redo.",h,undo.context);assertEquals(points(r.state),points(redo.state))
        }
    }
    @Test fun renamedCurveIdsAndNewNumericCoefficientsGiveNewComputedIntersections() {
        for(i in 1..12){val slope=i*.375;val wanted=i*.2-1;val offset=.5;val s=empty().copy(functions=listOf(FunctionDefinition("curve,/$i","Speed_$i","y=$slope*x+$offset","cyan"),FunctionDefinition("other,/$i","One","y=${slope*wanted+offset}","red")))
            val manager=RuhiDialogueManager();val selected=manager.execute(s,"Ruhi, could you select curves Speed_$i and One",WorkspaceHistoryByModule());assertEquals(selected.message,GraphCommandStatus.APPLIED,selected.status);val result=manager.execute(selected.state,"Ruhi, could you give intersection coordinates to 4 decimals",WorkspaceHistoryByModule(),selected.context);assertEquals(result.message,GraphCommandStatus.APPLIED,result.status);val p=points(result.state).single();assertEquals(wanted,p.x,1e-6);assertEquals(slope*wanted+offset,p.y,1e-6);assertEquals(s.functions,result.state.functions)
        }
    }
    @Test fun missingPairRestoresRealChoicesResumesStableIdsAndRejectsAStaleReply() {
        val s=curves().copy(functions=curves().functions+FunctionDefinition("third,/662","Other_662","y=x+2","green"));val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val q=manager.execute(s,"Do the selected curves intersect at all?",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending);assertEquals(3,q.dialogue!!.suggestions.size);assertEquals(s.functions,q.state.functions)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state),recover=false).state!!;assertEquals(q.dialogue!!.suggestions,manager.restoredView(saved)!!.suggestions)
        val answer=q.dialogue!!.suggestions.first().answer;val r=manager.execute(saved,answer,h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertNull(RuhiDialogueMemory.read(r.state).pending);assertEquals(2,points(r.state).size);assertEquals(1,h.forModule(s.module).protocol.size)
        val q2=manager.execute(s,"Do the selected curves intersect at all?",WorkspaceHistoryByModule());val changed=q2.state.copy(functions=q2.state.functions.mapIndexed{i,f->if(i==0)f.copy(expression="y=x^2+1")else f});val stale=manager.execute(changed,answer,WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertTrue(stale.message.contains("scene changed"));assertNull(RuhiIntersectionLanguage.drawing(stale.state))
        val cancel=manager.execute(q2.state,"Never mind",WorkspaceHistoryByModule());assertNull(RuhiDialogueMemory.read(cancel.state).pending);assertEquals(s.functions,cancel.state.functions)
        val plot=manager.execute(q2.state,"Plot y = x cubed",WorkspaceHistoryByModule());assertEquals(plot.message,GraphCommandStatus.APPLIED,plot.status);assertNull(RuhiDialogueMemory.read(plot.state).pending);assertEquals(4,plot.state.functions.size)
    }
    @Test fun liveQueriesFollowCurrentParentsViewportVisibilityAndAnExpandingAllGraphScope() {
        val s=curves();val r=query(s,"Mark the crossing points of the two graphs");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(listOf(Vec2(0.0,0.0),Vec2(1.0,1.0)),points(r.state))
        val edited=r.state.copy(functions=r.state.functions.mapIndexed{i,f->if(i==1)f.copy(expression="y=2*x")else f});assertTrue(points(edited).any{it.distanceTo(Vec2(2.0,4.0))<1e-6})
        val removed=edited.copy(functions=edited.functions.take(1));assertTrue(points(removed).isEmpty());assertTrue(RuhiIntersectionLanguage.drawing(removed)!!.diagnostic.isNotEmpty())
        val panned=r.state.copy(graph2DView=r.state.graph2DView.copy(centerX=10.0));assertTrue(points(panned).isEmpty());assertTrue(points(r.state.copy(graph2DView=r.state.graph2DView.copy(centerY=100.0))).isEmpty())
        val short=edited.copy(labSessionValues=edited.labSessionValues+(RuhiIntersectionLanguage.ASPECT to ".5"));assertEquals(listOf(Vec2(0.0,0.0)),points(short));assertEquals(2,points(edited).size)
        val boundary=r.state.copy(graph2DView=r.state.graph2DView.copy(zoom=7f));assertEquals(2,points(boundary).size)
        val all=query(s,"Mark all intersections in the current viewport");val more=all.state.copy(functions=all.state.functions+FunctionDefinition("new-662","Third_662","y=x+2","green"));assertEquals(4,points(more).size);assertEquals(2,points(more.copy(functions=more.functions.map{if(it.id=="new-662")it.copy(visible=false)else it})).size)
    }
    @Test fun namedNativeCirclesAndFullLinesFollowParentCoordinatesWithoutDuplicatingObjects() {
        val s=empty().copy(points=listOf(Vec2(-1.0,0.0),Vec2(1.0,0.0),Vec2(1.0,0.0),Vec2(3.0,0.0)),shapes=listOf(Shape2D("orb-a",Shape2DType.Circle,listOf(0,2),"Orb_907"),Shape2D("orb-b",Shape2DType.Circle,listOf(1,3),"Orb_811")),labSessionValues=mapOf("ruhi.studio.point.Anchor_907" to "0","ruhi.studio.point.East_811" to "1"))
        val r=query(s,"Find intersection of circles centered at Anchor_907 and East_811");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(2,points(r.state).size);assertTrue(points(r.state).all{abs(it.x)<1e-8&&abs(abs(it.y)-sqrt(3.0))<1e-8});assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes)
        val edited=r.state.copy(points=listOf(Vec2(-1.0,0.0),Vec2(1.0,0.0),Vec2(2.0,0.0),Vec2(4.0,0.0)));assertTrue(points(edited).all{abs(it.x)<1e-8&&abs(abs(it.y)-sqrt(8.0))<1e-8})
        val line=empty().copy(points=listOf(Vec2(0.0,0.0),Vec2(2.0,0.0),Vec2(-.5,0.0),Vec2(.5,0.0)),shapes=listOf(Shape2D("orb",Shape2DType.Circle,listOf(0,1),"Orb_811"),Shape2D("edge",Shape2DType.Segment,listOf(2,3),"Edge_907")))
        val crossing=query(line,"Mark intersections of line Edge_907 with circle Orb_811");assertEquals(crossing.message,GraphCommandStatus.APPLIED,crossing.status);assertEquals(listOf(Vec2(-2.0,0.0),Vec2(2.0,0.0)),points(crossing.state));assertEquals(line.shapes,crossing.state.shapes)
    }
    @Test fun tangenciesPolesEmptySetsAndCoincidentCurvesAreNotInventedCrossings() {
        val tangent=query(empty(),"Where do y = (x-1.234)^2 and y = 0 meet?");assertEquals(tangent.message,GraphCommandStatus.APPLIED,tangent.status);assertEquals(1,points(tangent.state).size);assertEquals(1.234,points(tangent.state).single().x,1e-6)
        for(expression in listOf("1/x","1/(x-2)","x^2+1")){val r=query(empty(),"Where do y = $expression and y = 0 meet?");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue("$expression ${points(r.state)}",points(r.state).isEmpty())}
        val circles=RuhiIntersectionMath.solve(RuhiIntersectionMath.Curve.Circle(Vec2(0.0,0.0),1.0),RuhiIntersectionMath.Curve.Circle(Vec2(2.0,0.0),1.0),-7.0,7.0);assertEquals(listOf(Vec2(1.0,0.0)),circles.points)
        val same=query(empty(),"Where do y = x and y = x meet?");assertEquals(same.message,GraphCommandStatus.APPLIED,same.status);assertTrue(RuhiIntersectionLanguage.drawing(same.state)!!.infinite);assertTrue(points(same.state).isEmpty())
        val extraneous=query(empty(),"Intersect y = sqrt(x) with y = x - 2");assertEquals(1,points(extraneous.state).size);assertTrue(points(extraneous.state).single().distanceTo(Vec2(4.0,2.0))<1e-6)
    }
    @Test fun aMissingSingleGraphAndMissingGeometryAskOnlyRelevantQuestionsWithoutMutation() {
        val manager=RuhiDialogueManager();val s=curves();val q=manager.execute(s,"Show the x-intercepts of the selected graph",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(2,q.dialogue!!.suggestions.size);val r=manager.execute(q.state,q.dialogue!!.suggestions.first().answer,WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(listOf(Vec2(0.0,0.0)),points(r.state))
        val legacy=query(r.state,"Ruhi, could you show its roots?");assertEquals(legacy.message,GraphCommandStatus.APPLIED,legacy.status);assertTrue(legacy.state.labSessionValues.containsKey("ruhi.dialogue.graph.markers"));val legacyCleared=query(legacy.state,"Clear all intersection markers");assertNull(RuhiIntersectionLanguage.drawing(legacyCleared.state));assertFalse(legacyCleared.state.labSessionValues.containsKey("ruhi.dialogue.graph.markers"))
        val count=query(s,"Ruhi, please count the intersections of these plotted curves");assertEquals(count.message,GraphCommandStatus.APPLIED,count.status);assertEquals("Intersection count: 2.",count.message)
        val alias=query(s,"Ruhi, how many intersections do these plotted curves have?");assertEquals(alias.message,GraphCommandStatus.APPLIED,alias.status);assertEquals("Intersection count: 2.",alias.message)
        val cleared=query(count.state,"Ruhi, please delete intersection markers");assertEquals(cleared.message,GraphCommandStatus.APPLIED,cleared.status);assertNull(RuhiIntersectionLanguage.drawing(cleared.state));assertEquals(s.functions,cleared.state.functions)
        val move=query(count.state,"Move intersection 2 units right");assertNotEquals("FIND_INTERSECTION",move.semanticFrame?.intent);assertEquals(count.state.labSessionValues[RuhiIntersectionLanguage.MARK],move.state.labSessionValues[RuhiIntersectionLanguage.MARK])
        val missing=query(s,"Mark intersections of line Missing_907 with circle Missing_811");assertEquals(GraphCommandStatus.CLARIFICATION,missing.status);assertTrue(missing.dialogue!!.suggestions.isEmpty());assertEquals(s.functions,missing.state.functions);assertEquals(s.shapes,missing.state.shapes)
    }
}
