package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphAdapterResult
import com.indianservers.aiexplorer.ar3dgraph.integration.ARVector3
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineGraphResult
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineVector3
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineAdapter
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEquationRequest
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphGenerationRequest
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import com.indianservers.aiexplorer.spatial.SurfaceDomain
import com.indianservers.aiexplorer.spatial.TypedSurfaceMesher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AR3DGraphEngineParityTest {
    private data class GoldenCase(val category: String, val equation: String, val minimum: Double = -3.0, val maximum: Double = 3.0)

    private val phase2cExplicitGoldenCases = listOf(
        GoldenCase("basic", "z=x"), GoldenCase("basic", "z=y"), GoldenCase("basic", "z=0"),
        GoldenCase("basic", "z=x+y"), GoldenCase("basic", "z=x-y"), GoldenCase("basic", "z=x^2"),
        GoldenCase("basic", "z=y^2"), GoldenCase("basic", "z=x*y"), GoldenCase("basic", "z=sqrt(x^2)+sqrt(y^2)"),
        GoldenCase("basic", "z=sqrt(x^2+y^2)"),
        GoldenCase("trigonometric", "z=sin(x)"), GoldenCase("trigonometric", "z=cos(x)"),
        GoldenCase("trigonometric", "z=sin(y)"), GoldenCase("trigonometric", "z=cos(y)"),
        GoldenCase("trigonometric", "z=sin(x)+cos(y)"), GoldenCase("trigonometric", "z=sin(x*y)"),
        GoldenCase("trigonometric", "z=cos(x^2+y^2)"), GoldenCase("trigonometric", "z=tan(x)"),
        GoldenCase("trigonometric", "z=sin(2*x)"), GoldenCase("trigonometric", "z=cos(3*y)"),
        GoldenCase("exponential-log", "z=exp(x)"), GoldenCase("exponential-log", "z=exp(-(x^2+y^2))"),
        GoldenCase("exponential-log", "z=log(x^2+y^2+1)"), GoldenCase("exponential-log", "z=log(sqrt(x^2)+sqrt(y^2)+1)"),
        GoldenCase("exponential-log", "z=exp(x-y)"),
        GoldenCase("rational", "z=1/(x^2+y^2)"), GoldenCase("rational", "z=x/(1+y^2)"),
        GoldenCase("rational", "z=(x^2-y^2)/(x^2+y^2+1)"), GoldenCase("rational", "z=1/(x-y)"),
        GoldenCase("rational", "z=(x*y)/(1+x^2+y^2)"),
        GoldenCase("invalid-region", "z=sqrt(x-y)"), GoldenCase("invalid-region", "z=sqrt(x+y)"),
        GoldenCase("invalid-region", "z=log(x+y)"), GoldenCase("invalid-region", "z=log(x-y)"),
        GoldenCase("invalid-region", "z=1/sqrt(x^2+y^2)"),
        GoldenCase("orientation", "z=x+2*y"), GoldenCase("orientation", "z=2*x-3*y"),
        GoldenCase("orientation", "z=x^2+y"), GoldenCase("orientation", "z=sin(x)+0.5*y"),
        GoldenCase("orientation", "z=exp(-(x^2))+y"),
        GoldenCase("domain", "z=x+y", -1.0, 1.0), GoldenCase("domain", "z=x^2-y^2", -7.0, 9.0),
        GoldenCase("domain", "z=sin(x*y)", -0.5, 0.5), GoldenCase("domain", "z=exp(-x^2-y^2)", -10.0, 10.0),
        GoldenCase("domain", "z=cos(x)+sin(y)", 2.0, 5.0),
    )
    private val requiredExplicitEquations = listOf(
        "z = x² + y²",
        "z = x² - y²",
        "z = sin(x) + cos(y)",
        "z = sin(sqrt(x² + y²))",
        "z = exp(-(x² + y²))",
        "z = x + 2y",
        "z = x² + y",
        "z = sin(x) + 0.5y",
        "z = 2x - 3y",
        "z = 1 / (x² + y²)",
        "z = sqrt(x - y)",
        "z = log(x + y)",
        "z = tan(x)",
        "z = sin(xy)",
        "z = cos(x² + y²)",
        "z = (x² - y²) / (x² + y² + 1)",
    )

    private val phase3cExplicitCases = buildList {
        listOf(
            "z=0", "z=x", "z=y", "z=x+y", "z=x-y", "z=x*y", "z=x^2", "z=y^2",
            "z=x^2+y^2", "z=x^2-y^2", "z=x^3", "z=y^3", "z=x^3-y", "z=x-y^3",
            "z=2*x+3*y", "z=x^2+2*x*y+y^2", "z=x^3+y^3", "z=x^4-y^2",
            "z=0.5*x^2+2*y", "z=3*x^2-2*y^2+x-y",
        ).forEach { add(GoldenCase("polynomial-algebraic", it)) }
        listOf(
            "z=sin(x)", "z=cos(x)", "z=sin(y)", "z=cos(y)", "z=sin(x)+cos(y)",
            "z=sin(x*y)", "z=cos(x^2+y^2)", "z=tan(x)", "z=sin(2*x)", "z=cos(3*y)",
            "z=sin(x)+sin(y)", "z=cos(x)-cos(y)", "z=sin(x-y)", "z=cos(x+y)",
            "z=sin(x^2)", "z=cos(y^2)", "z=sin(x)*cos(y)", "z=sin(3*x-y)",
            "z=cos(x-2*y)", "z=tan(0.25*y)",
        ).forEach { add(GoldenCase("trigonometric", it, -1.0, 1.0)) }
        listOf(
            "z=exp(x)", "z=exp(y)", "z=exp(x-y)", "z=exp(-(x^2+y^2))", "z=exp(-x^2)",
            "z=log(x^2+y^2+1)", "z=log(sqrt(x^2)+1)", "z=log(sqrt(y^2)+1)",
            "z=log(x^2+2)", "z=exp(-x^2)+log(y^2+1)",
        ).forEach { add(GoldenCase("exponential-logarithmic", it)) }
        listOf(
            "z=1/(x^2+y^2)", "z=x/(1+y^2)", "z=y/(1+x^2)", "z=1/(x-y)",
            "z=(x^2-y^2)/(x^2+y^2+1)", "z=(x*y)/(1+x^2+y^2)", "z=1/(x+y)",
            "z=(x+y)/(1+x^2)", "z=x/(x^2+y^2)", "z=y/(x^2+y^2)",
        ).forEach { add(GoldenCase("rational-discontinuous", it)) }
        listOf(
            "z=sqrt(x-y)", "z=sqrt(x+y)", "z=log(x+y)", "z=log(x-y)",
            "z=1/sqrt(x^2+y^2)", "z=sqrt(x-2*y)", "z=sqrt(y-2*x)",
            "z=log(x^2+y)", "z=log(y^2+x)", "z=sqrt(x*y)",
        ).forEach { add(GoldenCase("invalid-domain", it)) }
        listOf(
            "z=x+2*y", "z=2*x-3*y", "z=x^2+y", "z=x^3+2*y", "z=sin(x)+0.5*y",
            "z=exp(-x^2)+y", "z=x+sin(2*y)", "z=3*x-y^2", "z=x^2+2*x+y", "z=log(x^2+1)-2*y",
        ).forEach { add(GoldenCase("asymmetric-orientation", it)) }
        listOf(
            GoldenCase("boundary-extreme", "z=x+y", -0.000001, 0.000001),
            GoldenCase("boundary-extreme", "z=x^2-y^2", -100.0, 100.0),
            GoldenCase("boundary-extreme", "z=sin(x*y)", -0.5, 0.5),
            GoldenCase("boundary-extreme", "z=exp(-x^2-y^2)", -10.0, 10.0),
            GoldenCase("boundary-extreme", "z=cos(x)+sin(y)", 2.0, 5.0),
            GoldenCase("boundary-extreme", "z=1/(1+x^2+y^2)", -50.0, 50.0),
            GoldenCase("boundary-extreme", "z=0.000001*x+1000*y", -1.0, 1.0),
            GoldenCase("boundary-extreme", "z=x^3-y^3", -20.0, 20.0),
            GoldenCase("boundary-extreme", "z=log(x^2+y^2+1)", -100.0, 100.0),
            GoldenCase("boundary-extreme", "z=sqrt(x^2+y^2)", -1000.0, 1000.0),
        ).forEach(::add)
    }

    private val phase3cCompositeCases = listOf(
        listOf("z=x+y", "z=x-y"),
        listOf("z=x^2+y^2", "z=x^2-y^2"),
        listOf("z=sin(x)+cos(y)", "z=exp(-x^2-y^2)"),
        listOf("z=x+2*y", "z=2*x-3*y", "z=x^2+y"),
        listOf("z=sqrt(x-y)", "z=log(x+y)"),
        listOf("z=1/(x^2+y^2)", "z=(x^2-y^2)/(x^2+y^2+1)"),
        listOf("x=cos(u)*(3+cos(v)); y=sin(u)*(3+cos(v)); z=sin(v)", "z=x+y"),
        listOf("x^2+y^2+z^2=4", "z=0"),
        listOf("z=sin(x)", "z=cos(y)", "z=sin(x*y)"),
        listOf("z=exp(x-y)", "z=log(x^2+y^2+1)", "z=x*y"),
    )

    @Test fun requiredEquationSetPreservesExactExplicitEngineVerticesAndSettings() {
        val bridge = Existing3DGraphEngineBridge()
        requiredExplicitEquations.forEachIndexed { index, equation ->
            val request = request(listOf(equation))
            val engineOutput = bridge.generate(request) as EngineGraphResult.Success
            val interpretation = SurfaceInputInterpreter.explicit(equation).getOrThrow()
            val direct = Graph3D().mesh(interpretation.expression, -3.0, 3.0, 8)
            val snapshot = engineOutput.meshes.single()
            assertEquals("equation $index vertex count", direct.vertices.size, snapshot.vertices.size)
            assertEquals(direct.vertices.map { EngineVector3(it.x, it.y, it.z) }, snapshot.vertices)
            assertEquals(direct.rows, snapshot.rows)
            assertEquals(direct.columns, snapshot.columns)
            assertEquals(direct.triangleIndices, snapshot.triangleIndices)

            val ar = (GraphEngineAdapter(bridge).generate(request) as ARGraphAdapterResult.Success).data
            assertEquals(direct.vertices.map { ARVector3(it.x, it.y, it.z) }, ar.meshes.single().vertices)
            assertEquals(-3.0, ar.domainMinimum, 0.0)
            assertEquals(3.0, ar.domainMaximum, 0.0)
            assertEquals(8, ar.density)
            assertTrue(ar.meshes.single().normals.all(ARVector3::finite))
        }
    }

    @Test fun phase3cOneHundredCaseGoldenCertificationPreservesEngineAdapterParity() {
        assertEquals(90, phase3cExplicitCases.size)
        assertEquals(10, phase3cCompositeCases.size)
        assertEquals(
            mapOf(
                "polynomial-algebraic" to 20, "trigonometric" to 20,
                "exponential-logarithmic" to 10, "rational-discontinuous" to 10,
                "invalid-domain" to 10, "asymmetric-orientation" to 10,
                "boundary-extreme" to 10,
            ),
            phase3cExplicitCases.groupingBy(GoldenCase::category).eachCount(),
        )
        val bridge = Existing3DGraphEngineBridge()
        val requests = phase3cExplicitCases.map { request(listOf(it.equation), it.minimum, it.maximum) } +
            phase3cCompositeCases.map(::request)
        assertEquals(100, requests.size)
        requests.forEachIndexed { caseIndex, generationRequest ->
            val engine = bridge.generate(generationRequest)
            val adapted = GraphEngineAdapter(bridge).generate(generationRequest)
            assertTrue("case $caseIndex engine must succeed: $engine", engine is EngineGraphResult.Success)
            assertTrue("case $caseIndex adapter must succeed: $adapted", adapted is ARGraphAdapterResult.Success)
            engine as EngineGraphResult.Success
            adapted as ARGraphAdapterResult.Success
            val ar = adapted.data
            assertEquals(generationRequest.domainMinimum, ar.domainMinimum, 0.0)
            assertEquals(generationRequest.domainMaximum, ar.domainMaximum, 0.0)
            assertEquals(generationRequest.density, ar.density)
            assertEquals(engine.meshes.map { it.equationId }, ar.meshes.map { it.equationId })
            assertEquals(engine.meshes.map { it.canonicalEquation }, ar.meshes.map { it.canonicalEquation })
            engine.meshes.zip(ar.meshes).forEachIndexed { meshIndex, (source, converted) ->
                assertEquals("case $caseIndex mesh $meshIndex vertices", source.vertices.map { ARVector3(it.x, it.y, it.z) }, converted.vertices)
                val expectedIndices = if (source.triangleIndices.isNotEmpty()) source.triangleIndices else buildList {
                    for (row in 0 until source.rows - 1) for (column in 0 until source.columns - 1) {
                        val a = row * source.columns + column
                        val b = a + 1
                        val c = a + source.columns
                        val d = c + 1
                        add(a); add(c); add(b); add(b); add(c); add(d)
                    }
                }
                assertEquals("case $caseIndex mesh $meshIndex index order", expectedIndices, converted.indices)
                assertEquals(converted.vertices.size, converted.normals.size)
                assertEquals(converted.vertices.size, converted.colors.size)
                assertTrue(converted.normals.all(ARVector3::finite))
                assertTrue(converted.colors.all { it.alpha.isFinite() && it.alpha in 0f..1f })
                assertEquals(source.opacity.toFloat(), converted.opacity, 0f)
                assertEquals(converted.vertices.minOf(ARVector3::x), converted.bounds.minimum.x, 0.0)
                assertEquals(converted.vertices.maxOf(ARVector3::z), converted.bounds.maximum.z, 0.0)
            }
        }
    }

    @Test fun phase2cFortyFiveCategorizedExplicitGoldenCasesPreserveExactGeometryAndRenderIdentity() {
        assertEquals(45, phase2cExplicitGoldenCases.size)
        val bridge = Existing3DGraphEngineBridge()
        phase2cExplicitGoldenCases.forEachIndexed { index, case ->
            val request = request(listOf(case.equation), case.minimum, case.maximum)
            val interpretation = SurfaceInputInterpreter.explicit(case.equation).getOrThrow()
            val direct = Graph3D().mesh(interpretation.expression, case.minimum, case.maximum, 8)
            val engine = bridge.generate(request) as EngineGraphResult.Success
            val snapshot = engine.meshes.single()
            val ar = (GraphEngineAdapter(bridge).generate(request) as ARGraphAdapterResult.Success).data.meshes.single()
            assertEquals("$index ${case.category} vertices", direct.vertices.map { EngineVector3(it.x, it.y, it.z) }, snapshot.vertices)
            assertEquals("$index ${case.category} AR vertices", direct.vertices.map { ARVector3(it.x, it.y, it.z) }, ar.vertices)
            if (snapshot.triangleIndices.isNotEmpty()) assertEquals(snapshot.triangleIndices, ar.indices)
            else assertEquals((snapshot.rows - 1) * (snapshot.columns - 1) * 6, ar.indices.size)
            assertEquals(snapshot.equationId, ar.equationId)
            assertEquals(snapshot.canonicalEquation, ar.canonicalEquation)
            assertTrue(ar.normals.all(ARVector3::finite))
        }
    }

    @Test fun phase2cFiveCompositeGoldenCasesPreserveOrderTopologyAndIdentity() {
        val bridge = Existing3DGraphEngineBridge()
        val composite = listOf(
            listOf("z=x+y", "z=x-y"),
            listOf("z=sin(x)+cos(y)", "z=x^2-y^2", "z=exp(-x^2-y^2)"),
            listOf("z=x+2*y", "z=2*x-3*y"),
            listOf("x^2+y^2+z^2=4"),
            listOf("x=cos(u)*(3+cos(v)); y=sin(u)*(3+cos(v)); z=sin(v)"),
        )
        assertEquals(5, composite.size)
        composite.forEach { equations ->
            val request = request(equations)
            val engine = bridge.generate(request) as EngineGraphResult.Success
            val ar = (GraphEngineAdapter(bridge).generate(request) as ARGraphAdapterResult.Success).data
            assertEquals(engine.meshes.map { it.equationId }, ar.meshes.map { it.equationId })
            assertEquals(engine.meshes.map { it.canonicalEquation }, ar.meshes.map { it.canonicalEquation })
            engine.meshes.zip(ar.meshes).forEach { (source, converted) ->
                assertEquals(source.vertices.map { ARVector3(it.x, it.y, it.z) }, converted.vertices)
                if (source.triangleIndices.isNotEmpty()) assertEquals(source.triangleIndices, converted.indices)
            }
        }
    }

    @Test fun parametricSurfacePreservesTypedMesherGeometryAndTopologyExactly() {
        val equation = "x=cos(u)*(3+cos(v)); y=sin(u)*(3+cos(v)); z=sin(v)"
        val request = request(listOf(equation))
        val snapshot = (Existing3DGraphEngineBridge().generate(request) as EngineGraphResult.Success).meshes.single()
        val domain = SurfaceDomain(-3.0..3.0, -3.0..3.0, -3.0..3.0)
        val direct = TypedSurfaceMesher().mesh(
            SurfaceDefinition3D.Parametric(
                "surface", "cos(u)*(3+cos(v))", "sin(u)*(3+cos(v))", "sin(v)", domain = domain,
            ),
            8,
        ).geometry
        assertEquals(direct.vertices.map { EngineVector3(it.x, it.y, it.z) }, snapshot.vertices)
        assertEquals(direct.triangles, snapshot.triangleIndices)
    }

    @Test fun implicitSurfacePreservesTypedMesherGeometryAndTopologyExactly() {
        val equation = "x^2+y^2+z^2=4"
        val request = request(listOf(equation))
        val snapshot = (Existing3DGraphEngineBridge().generate(request) as EngineGraphResult.Success).meshes.single()
        val domain = SurfaceDomain(-3.0..3.0, -3.0..3.0, -3.0..3.0)
        val direct = TypedSurfaceMesher().mesh(SurfaceDefinition3D.Implicit("surface", equation, domain), 8).geometry
        assertEquals(direct.vertices.map { EngineVector3(it.x, it.y, it.z) }, snapshot.vertices)
        assertEquals(direct.triangles, snapshot.triangleIndices)
    }

    @Test fun multipleEquationsPreserveOrderIdentityColoursAndIndependentMeshes() {
        val request = request(listOf("z=x+2*y", "z=x^2+y", "z=sin(x)+0.5*y"))
        val output = (Existing3DGraphEngineBridge().generate(request) as EngineGraphResult.Success).meshes
        assertEquals(3, output.size)
        assertEquals(listOf("equation-0", "equation-1", "equation-2"), output.map { it.equationId })
        assertEquals(3, output.map { it.lineColor }.distinct().size)
        assertTrue(output.all { it.vertices.isNotEmpty() && it.opacity == 1f })
    }

    @Test fun invalidEmptyAndExtremeSettingsAreControlled() {
        val bridge = Existing3DGraphEngineBridge()
        assertTrue(bridge.generate(request(listOf(""))) is EngineGraphResult.ValidationError)
        assertTrue(bridge.generate(request(listOf("   "))) is EngineGraphResult.ValidationError)
        assertTrue(bridge.generate(request(listOf("z ="))) is EngineGraphResult.ValidationError)
        assertTrue(bridge.generate(request(listOf("z=exp(10000*x)"))) is EngineGraphResult.Success)
        assertTrue(bridge.generate(request(listOf("z=1e-300*x"))) is EngineGraphResult.Success)
        listOf("z=x+unknown", "z=sin((x)", "z=unsupported(x)", "z=1/0").forEach { input ->
            assertTrue(runCatching { bridge.generate(request(listOf(input))) }.isSuccess)
        }
        assertTrue(bridge.generate(request(listOf("z=x"), minimum = 2.0, maximum = -2.0)) is EngineGraphResult.ValidationError)
    }

    @Test fun customDomainResolutionColourTransparencyAxesGridAndBoundsRemainConsistent() {
        val request = GraphGenerationRequest(
            equations = listOf(GraphEquationRequest("custom", "z=x+2*y", colorIndex = 4, opacity = .35)),
            domainMinimum = -7.0,
            domainMaximum = 9.0,
            density = 12,
        )
        val bridge = Existing3DGraphEngineBridge()
        val engine = bridge.generate(request) as EngineGraphResult.Success
        val ar = (GraphEngineAdapter(bridge).generate(request) as ARGraphAdapterResult.Success).data
        assertEquals(request, engine.request)
        assertEquals(-7.0, ar.domainMinimum, 0.0)
        assertEquals(9.0, ar.domainMaximum, 0.0)
        assertEquals(12, ar.density)
        assertEquals(.35f, ar.meshes.single().opacity, 1e-6f)
        assertTrue(ar.meshes.single().colors.all { it.alpha <= .35f })
        assertEquals(6, ar.axes.size)
        assertTrue(ar.grid.isNotEmpty())
        assertEquals(ar.meshes.single().vertices.minOf { it.x }, ar.bounds.minimum.x, 0.0)
        assertEquals(ar.meshes.single().vertices.maxOf { it.z }, ar.bounds.maximum.z, 0.0)
    }

    private fun request(
        equations: List<String>,
        minimum: Double = -3.0,
        maximum: Double = 3.0,
    ) = GraphGenerationRequest(
        equations = equations.mapIndexed { index, value -> GraphEquationRequest("equation-$index", value, colorIndex = index) },
        domainMinimum = minimum,
        domainMaximum = maximum,
        density = 8,
    )
}
