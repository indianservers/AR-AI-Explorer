package com.indianservers.aiexplorer.ar3dgraph.integration

data class GraphEquationRequest(
    val id: String,
    val expression: String,
    val enabled: Boolean = true,
    val colorIndex: Int = 0,
    val opacity: Double = 1.0,
)

data class GraphGenerationRequest(
    val equations: List<GraphEquationRequest>,
    val domainMinimum: Double = -3.0,
    val domainMaximum: Double = 3.0,
    val density: Int = 26,
)

data class EngineVector3(val x: Double, val y: Double, val z: Double) {
    val finite: Boolean get() = x.isFinite() && y.isFinite() && z.isFinite()
}

data class EngineColor(val red: Float, val green: Float, val blue: Float, val alpha: Float = 1f)
data class EngineAxisStyle(
    val x: EngineColor = EngineColor(.05f, .49f, 1f),
    val y: EngineColor = EngineColor(.94f, 0f, .85f),
    val z: EngineColor = EngineColor(0f, .9f, 1f),
    val grid: EngineColor = EngineColor(.05f, .49f, 1f, .22f),
)

data class EngineMeshSnapshot(
    val equationId: String,
    val canonicalEquation: String,
    val vertices: List<EngineVector3>,
    val rows: Int,
    val columns: Int,
    val triangleIndices: List<Int>,
    val palette: List<EngineColor>,
    val lineColor: EngineColor,
    val opacity: Float,
)

sealed interface EngineGraphResult {
    data class Success(
        val request: GraphGenerationRequest,
        val meshes: List<EngineMeshSnapshot>,
        val axisStyle: EngineAxisStyle = EngineAxisStyle(),
    ) : EngineGraphResult

    data class ValidationError(val message: String) : EngineGraphResult
    data class GenerationError(val message: String, val cause: Throwable? = null) : EngineGraphResult
}

/**
 * Implemented by the app module so this isolated AR library can consume the existing graph engine
 * without depending on, copying, or mutating engine-owned types.
 */
fun interface GraphEngineContract {
    fun generate(request: GraphGenerationRequest): EngineGraphResult

    val connected: Boolean get() = true
}

object DisconnectedGraphEngineContract : GraphEngineContract {
    override val connected: Boolean = false
    override fun generate(request: GraphGenerationRequest): EngineGraphResult =
        EngineGraphResult.GenerationError("The existing 3D graph engine is not connected.")
}
