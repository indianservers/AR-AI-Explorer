package com.indianservers.aiexplorer.mathworkspace

import com.indianservers.aiexplorer.mathworkspace.discrete.*
import com.indianservers.aiexplorer.mathworkspace.spreadsheet.*
import org.junit.Assert.*
import org.junit.Test

class PhaseSixEnginesTest {
    @Test fun spreadsheetReferencesRangesAndDependenciesRecalculate() {
        val cells = mutableMapOf(CellAddress(1,1) to "10", CellAddress(2,1) to "20", CellAddress(3,1) to "30", CellAddress(1,2) to "=SUM(A1:A3)", CellAddress(2,2) to "=AVERAGE(A1:A3)", CellAddress(3,2) to "=MAX(A1:A3)", CellAddress(1,3) to "=B1+3")
        assertEquals("60",FormulaEngine(cells).value(CellAddress(1,2)))
        assertEquals("20",FormulaEngine(cells).value(CellAddress(2,2)))
        assertEquals("30",FormulaEngine(cells).value(CellAddress(3,2)))
        cells[CellAddress(3,1)]="90"
        val engine=FormulaEngine(cells)
        assertEquals("120",engine.value(CellAddress(1,2)))
        assertEquals("40",engine.value(CellAddress(2,2)))
        assertEquals("90",engine.value(CellAddress(3,2)))
        assertEquals("123",engine.value(CellAddress(1,3)))
    }
    @Test fun spreadsheetCyclesAndErrorsAreContained() {
        val e=FormulaEngine(mapOf(CellAddress(1,1) to "=B1",CellAddress(1,2) to "=A1",CellAddress(1,3) to "=1/0"))
        assertEquals("#CIRCULAR!",e.value(CellAddress(1,1)))
        assertEquals("#CIRCULAR!",e.value(CellAddress(1,2)))
        assertEquals("#DIV/0!",e.value(CellAddress(1,3)))
    }
    @Test fun spreadsheetFunctionsAndThousandCellRangeWork() {
        val cells=buildMap { (1..1000).forEach { put(CellAddress(it,1),it.toString()) };put(CellAddress(1,2),"=IF(A1>10,1,0)");put(CellAddress(1,3),"=SUM(A1:A1000)");put(CellAddress(1,4),"=MEDIAN(A1:A5)") }
        val engine=FormulaEngine(cells)
        assertEquals("0",engine.value(CellAddress(1,2)))
        assertEquals("500500",engine.value(CellAddress(1,3)))
        assertEquals("3",engine.value(CellAddress(1,4)))
    }
    @Test fun a1AddressesExpandPastZ() { assertEquals(27,CellAddress.parse("AA4")?.column);assertEquals("AB",CellAddress.columnName(28));assertEquals("Z20",CellAddress(20,26).toString()) }
    @Test fun graphAlgorithmsProduceExpectedResults() {
        val triangle=GraphModel(listOf(GraphVertex(0,0.0,0.0,"A"),GraphVertex(1,0.0,0.0,"B"),GraphVertex(2,0.0,0.0,"C")),listOf(GraphEdge(0,1),GraphEdge(1,2),GraphEdge(2,0)))
        assertTrue(GraphAlgorithms.hasCycle(triangle));assertEquals(1,GraphAlgorithms.components(triangle).size);assertEquals(2,GraphAlgorithms.degrees(triangle)[0]?.first)
        val g=GraphModel((0..3).map{GraphVertex(it,0.0,0.0,"${'A'+it}")},listOf(GraphEdge(0,1,2.0),GraphEdge(0,2,5.0),GraphEdge(1,2,1.0),GraphEdge(1,3,4.0),GraphEdge(2,3,1.0)),weighted=true)
        assertEquals("A → B → C → D · 4.0",GraphAlgorithms.dijkstra(g,0,3).result)
        assertEquals(4.0,GraphAlgorithms.mst(g).result.substringAfterLast(' ').toDouble(),0.0)
        assertEquals("A → B → C → D",GraphAlgorithms.bfs(g,0).result)
    }
    @Test fun setsRelationsCountingAndLogicAreExact() {
        val a=SetEngine.parse("1,2,3,4");val b=SetEngine.parse("3,4,5")
        assertEquals(setOf("1","2","3","4","5"),SetEngine.union(a,b));assertEquals(setOf("3","4"),SetEngine.intersection(a,b));assertEquals(setOf("1","2","5"),SetEngine.symmetricDifference(a,b))
        val elems=setOf("1","2","3");val pairs=RelationEngine.parsePairs("(1,1),(2,2),(3,3),(1,2),(2,1)");val props=RelationEngine.properties(elems,pairs)
        assertTrue(props.reflexive);assertTrue(props.symmetric);assertFalse(props.antisymmetric);assertTrue(props.transitive)
        assertEquals("10",CombinatoricsEngine.choose(5,2).toString());assertEquals("20",CombinatoricsEngine.permute(5,2).toString());assertEquals("3628800",CombinatoricsEngine.factorial(10).toString());assertEquals("100891344545564193334812497256",CombinatoricsEngine.choose(100,50).toString())
        assertEquals("Tautology",LogicEngine.table("P OR NOT P").classification);assertEquals("Contradiction",LogicEngine.table("P AND NOT P").classification)
        val implication=LogicEngine.table("P -> Q");assertEquals(listOf(true,true,false,true),implication.rows.map{it.result})
    }
}
