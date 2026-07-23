package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraCommandCatalog
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraFilter
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraKeyboardAction
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraKeyboardController
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraEquationAction
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraEquationActions
import com.indianservers.aiexplorer.phase2.mathstudio.AlgebraSortMode
import com.indianservers.aiexplorer.phase2.mathstudio.UnifiedMathStudioEngine
import com.indianservers.aiexplorer.phase2.mathstudio.UniversalAlgebraProjection
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalAlgebraExperienceTest {
    private val engine = UnifiedMathStudioEngine()
    private val base = WorkspaceState(functions = listOf(FunctionDefinition("f", "f(x)", "x^2", "cyan")))

    @Test fun typedInputCreatesFirstClassObjectsInsteadOfFlatteningToFunctions() {
        var session = engine.fromWorkspace(base)
        listOf("a = 3/4", "A = (2, 3)", "v = (4, -1)", "L = {1, 4, 9}", "M = {{1,2},{3,4}}", "eq: x+y=5", "region: x+y<=5", "g(x)=f(x)+a").forEach {
            session = engine.add(session, it)
        }
        val kinds = session.document.objects.values.associate { it.name to it.kind }
        assertEquals(UniversalMathKind.Scalar, kinds["a"])
        assertEquals(UniversalMathKind.Point2D, kinds["A"])
        assertEquals(UniversalMathKind.Vector, kinds["v"])
        assertEquals(UniversalMathKind.DataList, kinds["L"])
        assertEquals(UniversalMathKind.Matrix, kinds["M"])
        assertEquals(UniversalMathKind.Line, kinds["eq"])
        assertEquals(UniversalMathKind.Inequality, kinds["region"])
        assertEquals(UniversalMathKind.Function, kinds["g(x)"])
        assertTrue(session.document.objects.values.first { it.name == "g(x)" }.dependencies.isNotEmpty())
    }

    @Test fun projectionIncludesGeometryThreeDimensionalAndAlgebraObjects() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "A=(2,3)")
        val entries = UniversalAlgebraProjection.entries(session)
        assertTrue(entries.any { it.kind == UniversalMathKind.Point2D && it.name == "A" })
        assertTrue(entries.any { it.kind == UniversalMathKind.Segment })
        assertTrue(entries.any { it.kind == UniversalMathKind.Solid })
        assertTrue(entries.any { it.kind == UniversalMathKind.Surface })
        assertTrue(entries.any { it.kind == UniversalMathKind.SpatialScene })
    }

    @Test fun redefinitionPreviewReportsImpactAndRejectsCycles() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "g(x)=f(x)+1")
        val accepted = engine.redefinitionPreview(session, "f", "x^3")
        assertTrue(accepted.accepted)
        assertTrue(session.document.objects.values.first { it.name == "g(x)" }.id in accepted.affectedObjects)
        val rejected = engine.redefinitionPreview(session, "f", "g(x)+1")
        assertFalse(rejected.accepted)
        assertTrue(rejected.message.contains("circular"))
    }

    @Test fun sortingFilteringStylingSelectionAndBulkConstructionAreLive() {
        var session = engine.add(engine.fromWorkspace(base), "A=(1,2)")
        session = engine.add(session, "B=(3,4)")
        val points = session.document.objects.values.filter { it.kind == UniversalMathKind.Point2D && it.name in setOf("A", "B") }
        session = engine.selectAlgebra(session, points[0].id)
        session = engine.selectAlgebra(session, points[1].id, additive = true)
        session = engine.constructFromSelection(session, "Line")
        assertTrue(session.document.objects.values.any { it.kind == UniversalMathKind.Line && it.name.startsWith("line") })
        val pointId = points[0].id
        session = engine.setAlgebraStyle(session, pointId) { it.copy(auxiliary = true, locked = true, layer = 4) }
        session = engine.configureAlgebra(session, AlgebraSortMode.Layer, AlgebraDisplayMode.Value, AlgebraFilter.Auxiliary).copy(showAuxiliary = true)
        val visible = UniversalAlgebraProjection.visible(session)
        assertEquals(listOf(pointId), visible.map { it.id })
        assertTrue(visible.single().style.locked)
    }

    @Test fun objectAwareCommandSuggestionsExposeSignaturesAndCompatibility() {
        val session = engine.add(engine.fromWorkspace(base), "A=(1,2)")
        val point = UniversalAlgebraProjection.entries(session).first { it.name == "A" }
        val suggestions = AlgebraCommandCatalog.suggest("Li", listOf(point))
        assertEquals("Line", suggestions.first().name)
        assertTrue(point.id in suggestions.first().compatibleObjectIds)
        assertTrue(suggestions.first().signature.contains("A, B"))
    }

    @Test fun universalAlgebraObjectsSurviveWorkspaceProjectRoundTrip() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "A=(2,3)")
        session = engine.add(session, "M={{1,2},{3,4}}")
        val encoded = WorkspaceProjectCodec.encode(engine.toWorkspace(session))
        val restored = WorkspaceProjectCodec.decode(encoded, recover = false).state!!
        val names = engine.fromWorkspace(restored).document.objects.values.map { it.name }.toSet()
        assertTrue("A" in names)
        assertTrue("M" in names)
    }

    @Test fun keyboardAndBulkObjectOperationsAreDeterministic() {
        var session = engine.add(engine.fromWorkspace(base), "A=(2,3)")
        val entries = UniversalAlgebraProjection.entries(session)
        session = engine.selectAllAlgebra(session, entries)
        assertEquals(entries.size, session.algebraSelection.size)
        assertEquals(AlgebraKeyboardAction.SelectAll, AlgebraKeyboardController.action("a", ctrl = true))
        assertEquals(AlgebraKeyboardAction.ToggleVisibility, AlgebraKeyboardController.action("space"))
        assertEquals(entries[1].id, AlgebraKeyboardController.move(entries, entries[0].id, 1))
        session = engine.styleSelection(session) { it.copy(trace = true, folder = "Investigation") }
        assertTrue(session.algebraSelection.all { session.algebraStyles.getValue(it).trace })
    }

    @Test fun universalListHasExplicitKindsForEveryRequestedAlgebraFamily() {
        var session = engine.fromWorkspace(base)
        listOf(
            "a=3/4", "speed=Parameter(2,0,10)", "A=(1,2)", "P=(1,2,3)", "v=(3,4)",
            "eq=t=5", "coordinateLine=x+y=5", "region=x+y<=5", "line1=Line(A,A)", "ray1=Ray(A,A)", "seg1=Segment(A,A)",
            "circle1=Circle(A,A)", "conic1=Ellipse(A,A,A)", "f(x)=x^2", "absf(x)=If(x<0,-x,x)",
            "L={1,2,3}", "seq=Sequence(n^2,n,1,5)", "M={{1,2},{3,4}}", "d=Distance(A,A)",
            "theta=Angle(A,A,A)", "ready=true", "note=\"construction complete\"", "plane1=Plane(P,P,P)", "cas1=CAS(x^2+1)",
        ).forEach { session = engine.add(session, it) }
        val kinds = session.document.objects.values.map { it.kind }.toSet()
        assertTrue(kinds.containsAll(setOf(
            UniversalMathKind.Scalar, UniversalMathKind.Parameter, UniversalMathKind.Point2D, UniversalMathKind.Point3D,
            UniversalMathKind.Vector, UniversalMathKind.Equation, UniversalMathKind.Inequality, UniversalMathKind.Line,
            UniversalMathKind.Ray, UniversalMathKind.Segment, UniversalMathKind.Circle, UniversalMathKind.Conic,
            UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction, UniversalMathKind.DataList, UniversalMathKind.Sequence,
            UniversalMathKind.Matrix, UniversalMathKind.Measurement, UniversalMathKind.Angle, UniversalMathKind.Boolean,
            UniversalMathKind.Text, UniversalMathKind.Plane, UniversalMathKind.Surface, UniversalMathKind.Solid, UniversalMathKind.NotebookCell,
        )))
        val projectedKinds = UniversalAlgebraProjection.entries(session).map { it.kind }.toSet()
        assertTrue(projectedKinds.containsAll(kinds))
    }

    @Test fun casTransformsBecomeDependentAlgebraObjects() {
        var session = engine.fromWorkspace(base)
        session = engine.selectAlgebra(session, "f")
        session = engine.transform(session, com.indianservers.aiexplorer.phase2.mathstudio.StudioTransform.Derivative)
        val result = session.document.objects.values.first { it.kind == UniversalMathKind.NotebookCell && it.sourceView == "CAS" }
        assertEquals(setOf("f"), result.dependencies)
        assertTrue(UniversalAlgebraProjection.entries(session).any { it.id == result.id })
    }

    @Test fun requestedTypedSyntaxInfersLineCircleBooleanAndUnicodePowers() {
        var session = engine.fromWorkspace(base)
        listOf("a = 4", "A = (2, 3)", "v = (3, -1)", "x + 2y = 7", "c: (x - 1)² + (y + 2)² = 9",
            "L = {1, 4, 9, 16}", "M = {{1, 2}, {3, 4}}", "s = Sequence(n², n, 1, 20)", "b = x > 0").forEach {
            session = engine.add(session, it)
        }
        val objects = session.document.objects.values.associateBy { it.name }
        assertEquals(UniversalMathKind.Scalar, objects.getValue("a").kind)
        assertEquals(UniversalMathKind.Point2D, objects.getValue("A").kind)
        assertEquals(UniversalMathKind.Vector, objects.getValue("v").kind)
        assertTrue(objects.values.any { it.kind == UniversalMathKind.Line && it.name.startsWith("eq") })
        assertEquals(UniversalMathKind.Circle, objects.getValue("c").kind)
        assertEquals(UniversalMathKind.DataList, objects.getValue("L").kind)
        assertEquals(UniversalMathKind.Matrix, objects.getValue("M").kind)
        assertEquals(UniversalMathKind.Sequence, objects.getValue("s").kind)
        assertEquals(UniversalMathKind.Boolean, objects.getValue("b").kind)
        assertTrue((objects.getValue("c").payload as com.indianservers.aiexplorer.workspace.UniversalMathPayload.Properties).entries.getValue("definition").contains("^2"))
    }

    @Test fun dependencyRedefinitionCanDetachReplaceAndDuplicateAsFreeWithoutLosingStyle() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "a=4")
        session = engine.add(session, "g(x)=f(x)+a")
        session = engine.add(session, "h(x)=x+1")
        val g = session.document.objects.values.first { it.name == "g(x)" }
        val h = session.document.objects.values.first { it.name == "h(x)" }
        session = engine.setAlgebraStyle(session, g.id) { it.copy(layer = 7, caption = "dependent curve") }
        session = engine.selectAlgebra(session, g.id)
        val revision = session.document.objects.getValue(g.id).objectRevision
        val preview = engine.redefinitionPreview(session, g.id, "f(x)-a")
        assertTrue(preview.accepted)
        assertTrue("f" in preview.newDependencies)
        session = engine.redefine(session, g.id, "f(x)-a")
        assertTrue(session.document.objects.getValue(g.id).objectRevision > revision)
        assertEquals(7, session.algebraStyles.getValue(g.id).layer)
        assertTrue(g.id in session.algebraSelection)

        session = engine.replaceReferences(session, "f", h.id)
        val replaced = session.document.objects.getValue(g.id)
        assertTrue(h.id in replaced.dependencies)
        assertFalse("f" in replaced.dependencies)

        session = engine.duplicateAsFree(session, g.id)
        val freeCopy = session.document.objects.getValue(session.selectedId!!)
        assertTrue(freeCopy.dependencies.isEmpty())
        assertEquals(7, session.algebraStyles.getValue(freeCopy.id).layer)

        session = engine.makeIndependent(session, g.id)
        assertTrue(session.document.objects.getValue(g.id).dependencies.isEmpty())
        assertEquals(7, session.algebraStyles.getValue(g.id).layer)
    }

    @Test fun typedRedefinitionRebuildsPayloadRejectsSelfCyclesAndPreservesIdentity() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "a=4")
        session = engine.add(session, "L={1,2,3}")
        session = engine.add(session, "M={{1,2},{3,4}}")
        val a = session.document.objects.values.first { it.name == "a" }
        val list = session.document.objects.values.first { it.name == "L" }
        val matrix = session.document.objects.values.first { it.name == "M" }
        session = engine.setAlgebraStyle(session, a.id) { it.copy(layer = 6, caption = "control") }
        session = engine.selectAlgebra(session, a.id)

        session = engine.redefine(session, a.id, "9/2")
        session = engine.redefine(session, list.id, "{5, 8, 13}")
        session = engine.redefine(session, matrix.id, "{{2,0},{0,2}}")
        val entries = UniversalAlgebraProjection.entries(session).associateBy { it.id }
        assertEquals("9/2", entries.getValue(a.id).value)
        assertEquals("{5, 8, 13}", entries.getValue(list.id).value)
        assertEquals("{{2.0, 0.0}, {0.0, 2.0}}", entries.getValue(matrix.id).definition)
        assertEquals(6, session.algebraStyles.getValue(a.id).layer)
        assertTrue(a.id in session.algebraSelection)

        val selfCycle = engine.redefinitionPreview(session, "f", "f(x)+1")
        assertFalse(selfCycle.accepted)
        assertTrue(selfCycle.message.contains("circular"))
    }

    @Test fun boundFunctionAndSequenceVariablesDoNotBecomeDocumentDependencies() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "x=7")
        session = engine.add(session, "n=5")
        session = engine.add(session, "a=4")
        session = engine.add(session, "g(x)=x^2+a")
        session = engine.add(session, "s=Sequence(a*n,n,1,20)")
        val byName = session.document.objects.values.associateBy { it.name }
        assertEquals(setOf(byName.getValue("a").id), byName.getValue("g(x)").dependencies)
        assertEquals(setOf(byName.getValue("a").id), byName.getValue("s").dependencies)
    }

    @Test fun geometryConstructionsImmediatelyBecomeEditableAlgebraRowsAndStaySynchronized() {
        var session = engine.fromWorkspace(WorkspaceState(points = emptyList(), shapes = emptyList(), functions = emptyList(), solids = emptyList(), vectors3D = emptyList()))
        session = engine.construct(session, "point2d(A,2,3)")
        session = engine.construct(session, "point2d(O,0,0)")
        session = engine.construct(session, "circle(c,O,A)")
        session = engine.construct(session, "pointon(B,c,0.25)")
        session = engine.construct(session, "line2d(g,A,B)")
        session = engine.construct(session, "intersection(C,g,c,1)")

        val entries = UniversalAlgebraProjection.entries(session).associateBy { it.name }
        assertEquals("(2, 3)", entries.getValue("A").value)
        assertTrue(entries.getValue("B").definition.startsWith("Point(c"))
        assertEquals("Line(A, B)", entries.getValue("g").definition)
        assertEquals("Intersect(g, c, 1)", entries.getValue("C").definition)

        val before = entries.getValue("C").value
        session = engine.moveGeometryPoint(session, "A", com.indianservers.aiexplorer.core.Vec2(4.0, 3.0))
        assertEquals("(4, 3)", UniversalAlgebraProjection.entries(session).first { it.name == "A" }.value)
        assertFalse(before == UniversalAlgebraProjection.entries(session).first { it.name == "C" }.value)

        session = engine.redefine(session, "A", "(5, 3)")
        assertEquals(com.indianservers.aiexplorer.core.Vec2(5.0, 3.0), com.indianservers.aiexplorer.core.DynamicGeometryEngine().resolve(session.construction.geometry).getValue("A"))

        session = engine.select(session, "C")
        assertEquals(setOf("C"), session.algebraSelection)
        assertTrue(session.experience.selection.contextCanonicalIds.containsAll(setOf("g", "c")))
        assertTrue(session.experience.selection.representationIds.any { it == "C@algebra" })
        assertTrue(session.experience.selection.representationIds.any { it == "C@geometry2d" })

        val workspace = engine.toWorkspace(session)
        assertTrue(workspace.points.contains(com.indianservers.aiexplorer.core.Vec2(5.0, 3.0)))
        assertTrue(workspace.shapes.any { it.id == "g" })
    }

    @Test fun equationsSystemsParametricFormsAndRelationsArePersistentAndActionable() {
        var session = engine.fromWorkspace(base)
        listOf("line=x+y=5", "circle=x²+y²=9", "region=x+y≤5", "linear=2*x-3=0",
            "sys=System(x+y=5,x-y=1)", "(x,y)=(cos(t),sin(t))", "A=(1,2)", "c=Circle(A,A)", "belongs=A ∈ c", "orth=line ⟂ linear").forEach {
            session = engine.add(session, it)
        }
        val entries = UniversalAlgebraProjection.entries(session)
        assertTrue(entries.any { it.kind == UniversalMathKind.Line })
        assertTrue(entries.any { it.kind == UniversalMathKind.Circle && it.definition.contains("^2") })
        assertTrue(entries.any { it.kind == UniversalMathKind.Inequality })
        assertTrue(entries.any { it.kind == UniversalMathKind.EquationSystem })
        assertTrue(entries.any { it.kind == UniversalMathKind.ParametricEquation })
        assertEquals(2, entries.count { it.kind == UniversalMathKind.Relation })
        val line = entries.first { it.name == "line" }
        assertTrue(AlgebraEquationActions.available(line).containsAll(listOf(AlgebraEquationAction.Graph, AlgebraEquationAction.Rearrange, AlgebraEquationAction.InspectVariables, AlgebraEquationAction.Substitute, AlgebraEquationAction.CreateGeometry)))
        session = engine.applyEquationAction(session, line.id, AlgebraEquationAction.InspectVariables)
        assertTrue(session.message.contains("x") && session.message.contains("y"))
        session = engine.applyEquationAction(session, line.id, AlgebraEquationAction.Rearrange)
        assertEquals("5 = x+y", UniversalAlgebraProjection.entries(session).first { it.id == line.id }.definition)
        session = engine.applyEquationAction(session, line.id, AlgebraEquationAction.CreateGeometry)
        assertEquals("algebra/geometry", session.document.objects.getValue(line.id).sourceView)
    }

    @Test fun displayModesIncludeValueDefinitionCommandExactDecimalMixedAndSpokenMath() {
        var session = engine.add(engine.fromWorkspace(base), "a=1/2")
        session = engine.construct(session, "point2d(A,2,3)")
        val scalar = UniversalAlgebraProjection.entries(session).first { it.name == "a" }
        val point = UniversalAlgebraProjection.entries(session).first { it.name == "A" }
        assertEquals("1/2", scalar.rendered(com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode.Exact))
        assertTrue(scalar.rendered(com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode.Decimal).contains("0.5"))
        assertTrue(scalar.rendered(com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode.Mixed).contains("≈"))
        assertTrue(point.rendered(com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode.Spoken).contains("point 2 d", ignoreCase = true))
        assertEquals("(2, 3)", point.rendered(com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode.Value))
        assertEquals("(2, 3)", point.rendered(com.indianservers.aiexplorer.phase2.mathstudio.AlgebraDisplayMode.Command))
    }
}
