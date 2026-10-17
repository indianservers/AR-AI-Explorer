package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiFunctionLanguageTest {
    @Test fun all25UnchangedFunctionRowsHaveIndependentIdentityDomainAndDiscontinuityProofAndPersist() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="PLOT_FUNCTION_2D"};assertEquals(25,rows.size)
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager();val r=manager.execute(s,decode(row[1]),h,c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c);assertTrue("${row[0]} ${check.diagnostics}",check.verified);assertEquals(1,h.forModule(s.module).protocol.size)
            val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false);assertNotNull(saved.state);assertEquals(r.state.functions,saved.state!!.functions);assertEquals(r.state.graph2DView,saved.state!!.graph2DView)
            val undo=manager.execute(r.state,"Undo.",h,r.context);assertEquals(s.functions,undo.state.functions);val redo=manager.execute(undo.state,"Redo.",h,undo.context);assertEquals(r.state.functions,redo.state.functions)
        }
    }
    @Test fun generalNumericExpressionsAndArbitraryFunctionNamesUseCurrentInputs() {
        val manager=RuhiDialogueManager();var s=WorkspaceState(module=MathModule.Graph2D)
        for(i in 1..15){val slope=i*.375;val offset=i-8.0;val name=if(i==1)"One" else "Speed_$i";val r=manager.execute(s,"Graph $name(x) = ${slope}x + $offset",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val f=r.state.functions.last();assertEquals(name,f.name);val typed=TypedGraphExpressionParser.parse(f.expression);listOf(-3.2,.75,4.6).forEach{x->assertEquals(slope*x+offset,TypedGraphEngine().evaluate(typed,x)!!.y,1e-8)};s=r.state}
        val tiny=manager.execute(WorkspaceState(module=MathModule.Graph2D),"Plot Momentum_907(x) = 1e-3x + 2",WorkspaceHistoryByModule());assertEquals(tiny.message,GraphCommandStatus.APPLIED,tiny.status);assertEquals(2.004,TypedGraphEngine().evaluate(TypedGraphExpressionParser.parse(tiny.state.functions.single().expression),4.0)!!.y,1e-10)
    }
    @Test fun namedRedefinitionKeepsStableIdAppearanceOtherCurvesAndInteractiveView() {
        val s=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("stable-907","Speed_907","y=x^2","#FF0000",appearance=GraphAppearance(width=7f)),FunctionDefinition("other-811","Other_811","y=sin(x)","cyan")),graph2DView=Graph2DViewState(centerX=2.0,centerY=-1.0,zoom=1.7f))
        val r=RuhiDialogueManager().execute(s,"Plot the function Speed_907(x) = 4x - 3",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(2,r.state.functions.size);assertEquals("stable-907",r.state.functions.first().id);assertEquals(s.functions.first().appearance,r.state.functions.first().appearance);assertEquals(s.functions.first().colorKey,r.state.functions.first().colorKey);assertEquals(s.functions.last(),r.state.functions.last());assertEquals(s.graph2DView,r.state.graph2DView)
    }
    @Test fun missingEquationCanResumeRetryCancelAndRestoreItsSuggestions() {
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val s=WorkspaceState(module=MathModule.Graph2D)
        val q=manager.execute(s,"Draw a function",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending);assertTrue(q.dialogue!!.suggestions.isNotEmpty())
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state),recover=false).state!!;assertEquals(q.dialogue!!.suggestions,manager.restoredView(loaded)!!.suggestions)
        val invalid=manager.execute(loaded,"y=sin(",h);assertEquals(GraphCommandStatus.INVALID,invalid.status);assertTrue(invalid.state.functions.isEmpty());assertNotNull(RuhiDialogueMemory.read(invalid.state).pending)
        val r=manager.execute(invalid.state,"y = x cubed",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertNull(RuhiDialogueMemory.read(r.state).pending);assertEquals("x^3",r.semanticFrame!!.parameters["expression"]);assertEquals(1,h.forModule(s.module).protocol.size)
        val q2=manager.execute(r.state,"Plot a graph",h);val cancelled=manager.execute(q2.state,"Never mind",h);assertNull(RuhiDialogueMemory.read(cancelled.state).pending);assertEquals(r.state.functions,cancelled.state.functions)
    }
    @Test fun invalidExpressionsRangesAndUnspecifiedParametersNeverMutateOrExecuteCode() {
        val s=RuhiSeed500Fixture.state("GRAPH_2D")
        for(text in listOf("Plot y = sin(","Plot y = 1/0","Plot y = sqrt(-1)","Plot y = x only for x between 2 and -2","Plot y = x only for x between abc and 2","Plot y = Runtime.exec(1)","Plot y = "+"(".repeat(65)+"x"+")".repeat(65))) {val h=WorkspaceHistoryByModule();val r=RuhiDialogueManager().execute(s,text,h);assertTrue("$text ${r.status} ${r.message}",r.status in setOf(GraphCommandStatus.INVALID,GraphCommandStatus.CLARIFICATION));assertEquals(s.functions,r.state.functions);assertEquals(s.points,r.state.points);assertTrue(h.forModule(s.module).protocol.isEmpty())}
        val q=RuhiDialogueManager().execute(s,"Plot y = a*x + 1",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.message.contains("a"));assertEquals(s.functions,q.state.functions)
    }
    @Test fun nativeSamplesSeparateAsymptotesFloorStepsAndClosedDomainsAndUseRealPiecewiseBranches() {
        val engine=TypedGraphEngine();val floorSample=engine.sample(TypedGraphExpressionParser.parse("y=floor(x)"),GraphDomain(95.0,105.0));assertTrue(floorSample.curves.size>=9);assertTrue(floorSample.curves.all{seg->seg.points.zipWithNext().all{it.first.y==it.second.y}})
        val shifted=RuhiDialogueManager().execute(WorkspaceState(module=MathModule.Graph2D),"Draw y = 1/(x-3)",WorkspaceHistoryByModule());assertEquals(shifted.message,GraphCommandStatus.APPLIED,shifted.status);val pole=engine.sample(TypedGraphExpressionParser.parse(shifted.state.functions.single().expression));assertTrue(pole.curves.none{seg->seg.points.zipWithNext().any{it.first.x<3&&it.second.x>3}})
        val r=RuhiDialogueManager().execute(WorkspaceState(module=MathModule.Graph2D),"Plot y = piecewise(x>=1,2*x,-3*x)",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val definition=TypedGraphExpressionParser.parse(r.state.functions.single().expression);assertTrue(definition is TypedGraphExpression.Piecewise);assertEquals(4.0,engine.evaluate(definition,2.0)!!.y,1e-9);assertEquals(6.0,engine.evaluate(definition,-2.0)!!.y,1e-9);assertTrue(engine.sample(definition).curves.none{seg->seg.points.zipWithNext().any{it.first.x<1&&it.second.x>=1}})
        val largeJump=engine.sample(TypedGraphExpressionParser.parse("piecewise{x>=1:10000;1:10001}"));assertTrue(largeJump.curves.none{seg->seg.points.zipWithNext().any{it.first.y!=it.second.y}})
        val restrictedPole=engine.sample(TypedGraphExpressionParser.parse("y=1/(x-3) {x>=-2&&x<=5}"));assertTrue(restrictedPole.curves.size>1);assertTrue(restrictedPole.curves.none{seg->seg.points.zipWithNext().any{it.first.x<3&&it.second.x>3}});assertTrue(restrictedPole.curves.flatMap{it.points}.all{it.x in -2.0..5.0})
        val restricted=RuhiDialogueManager().execute(WorkspaceState(module=MathModule.Graph2D),"Draw y = x squared only for x between -3 and 4",WorkspaceHistoryByModule());val typed=TypedGraphExpressionParser.parse(restricted.state.functions.single().expression);assertNull(engine.evaluate(typed,-3.1));assertNotNull(engine.evaluate(typed,-3.0));assertNotNull(engine.evaluate(typed,4.0));assertNull(engine.evaluate(typed,4.1));val bounded=engine.sample(typed).curves.flatMap{it.points};assertEquals(-3.0,bounded.minOf{it.x},1e-8);assertEquals(4.0,bounded.maxOf{it.x},1e-8)
    }
}
