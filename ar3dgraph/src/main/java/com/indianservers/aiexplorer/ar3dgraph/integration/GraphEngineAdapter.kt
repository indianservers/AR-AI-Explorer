package com.indianservers.aiexplorer.ar3dgraph.integration

import kotlin.math.sqrt

data class ARVector3(val x: Double, val y: Double, val z: Double) {
    val finite: Boolean get() = x.isFinite() && y.isFinite() && z.isFinite()
}

data class ARColor(val red: Float, val green: Float, val blue: Float, val alpha: Float)
data class ARLineRenderData(val start: ARVector3, val end: ARVector3, val color: ARColor)

data class ARGraphBounds(val minimum: ARVector3, val maximum: ARVector3) {
    val centre: ARVector3 get() = ARVector3(
        (minimum.x + maximum.x) / 2.0,
        (minimum.y + maximum.y) / 2.0,
        (minimum.z + maximum.z) / 2.0,
    )
}

data class ARMeshRenderData(
    val equationId: String,
    val canonicalEquation: String,
    val vertices: List<ARVector3>,
    val indices: List<Int>,
    val normals: List<ARVector3>,
    val colors: List<ARColor>,
    val bounds: ARGraphBounds,
    val opacity: Float,
)

data class ARGraphRenderData(
    val meshes: List<ARMeshRenderData>,
    val axes: List<ARLineRenderData>,
    val grid: List<ARLineRenderData>,
    val bounds: ARGraphBounds,
    val domainMinimum: Double,
    val domainMaximum: Double,
    val density: Int,
)

sealed interface ARGraphAdapterResult {
    data class Success(val data: ARGraphRenderData) : ARGraphAdapterResult
    data class ValidationError(val message: String) : ARGraphAdapterResult
    data class GenerationError(val message: String) : ARGraphAdapterResult
}

/**
 * Thin representation adapter. Mathematical parsing, sampling, invalid-region handling, and mesh
 * generation remain exclusively owned by the existing engine behind [GraphEngineContract].
 */
class GraphEngineAdapter(
    private val contract: GraphEngineContract = DisconnectedGraphEngineContract,
) {
    fun isConnected(): Boolean = contract.connected

    fun generate(request: GraphGenerationRequest): ARGraphAdapterResult {
        val result = runCatching { contract.generate(request) }.getOrElse { error ->
            return ARGraphAdapterResult.GenerationError(
                error.message ?: "The existing 3D graph engine failed safely.",
            )
        }
        return when (result) {
            is EngineGraphResult.ValidationError -> ARGraphAdapterResult.ValidationError(result.message)
            is EngineGraphResult.GenerationError -> ARGraphAdapterResult.GenerationError(result.message)
            is EngineGraphResult.Success -> convert(result)
        }
    }

    private fun convert(result: EngineGraphResult.Success): ARGraphAdapterResult {
        if (result.meshes.isEmpty()) return ARGraphAdapterResult.ValidationError("No enabled equation produced graph geometry.")
        return runCatching {
            val vertexCount = result.meshes.sumOf { it.vertices.size.toLong() }
            val indexCount = result.meshes.sumOf { mesh ->
                if (mesh.triangleIndices.isNotEmpty()) mesh.triangleIndices.size.toLong()
                else (mesh.rows - 1L).coerceAtLeast(0L) * (mesh.columns - 1L).coerceAtLeast(0L) * 6L
            }
            require(vertexCount <= MAX_AR_VERTICES && indexCount <= MAX_AR_INDICES) {
                "This graph is too large for safe AR rendering. Reduce the number of equations or selected resolution."
            }
            val meshes = result.meshes.map(::convertMesh)
            ARGraphAdapterResult.Success(
                ARGraphRenderData(
                    meshes = meshes,
                    axes = axes(result),
                    grid = grid(result),
                    bounds = boundsOf(meshes.flatMap(ARMeshRenderData::vertices)),
                    domainMinimum = result.request.domainMinimum,
                    domainMaximum = result.request.domainMaximum,
                    density = result.request.density,
                ),
            )
        }.getOrElse { ARGraphAdapterResult.GenerationError(it.message ?: "Unable to prepare AR render data.") }
    }

    private fun axes(result: EngineGraphResult.Success): List<ARLineRenderData> {
        val minimum = result.request.domainMinimum
        val maximum = result.request.domainMaximum
        fun line(end: ARVector3, color: EngineColor) = ARLineRenderData(
            ARVector3(0.0, 0.0, 0.0),
            end,
            ARColor(color.red, color.green, color.blue, color.alpha),
        )
        return listOf(
            line(ARVector3(maximum, 0.0, 0.0), result.axisStyle.x),
            line(ARVector3(0.0, maximum, 0.0), result.axisStyle.y),
            line(ARVector3(0.0, 0.0, maximum), result.axisStyle.z),
            line(ARVector3(minimum, 0.0, 0.0), result.axisStyle.x),
            line(ARVector3(0.0, minimum, 0.0), result.axisStyle.y),
            line(ARVector3(0.0, 0.0, minimum), result.axisStyle.z),
        )
    }

    private fun grid(result: EngineGraphResult.Success): List<ARLineRenderData> {
        val minimum = result.request.domainMinimum
        val maximum = result.request.domainMaximum
        val color = result.axisStyle.grid.let { ARColor(it.red, it.green, it.blue, it.alpha) }
        val start = kotlin.math.ceil(minimum).toInt()
        val end = kotlin.math.floor(maximum).toInt()
        if (start > end) return emptyList()
        return buildList {
            for (value in start..end) {
                add(ARLineRenderData(ARVector3(value.toDouble(), minimum, 0.0), ARVector3(value.toDouble(), maximum, 0.0), color))
                add(ARLineRenderData(ARVector3(minimum, value.toDouble(), 0.0), ARVector3(maximum, value.toDouble(), 0.0), color))
            }
        }
    }

    private fun convertMesh(source: EngineMeshSnapshot): ARMeshRenderData {
        require(source.vertices.isNotEmpty()) { "The graph engine returned an empty mesh." }
        require(source.vertices.all(EngineVector3::finite)) { "The graph engine returned non-finite geometry." }
        val vertices = source.vertices.map { ARVector3(it.x, it.y, it.z) }
        val indices = if (source.triangleIndices.isNotEmpty()) source.triangleIndices.toList()
        else regularGridIndices(source.rows, source.columns)
        require(indices.size % 3 == 0 && indices.all { it in vertices.indices }) { "The graph engine returned invalid topology." }
        val normals = vertexNormals(vertices, indices)
        val zMin = vertices.minOf(ARVector3::z)
        val zRange = (vertices.maxOf(ARVector3::z) - zMin).coerceAtLeast(1e-9)
        val colors = vertices.map { vertex ->
            val sampled = sample(source.palette, ((vertex.z - zMin) / zRange).toFloat())
            ARColor(sampled.red, sampled.green, sampled.blue, (sampled.alpha * source.opacity).coerceIn(0f, 1f))
        }
        return ARMeshRenderData(
            equationId = source.equationId,
            canonicalEquation = source.canonicalEquation,
            vertices = vertices,
            indices = indices,
            normals = normals,
            colors = colors,
            bounds = boundsOf(vertices),
            opacity = source.opacity,
        )
    }

    private fun regularGridIndices(rows: Int, columns: Int): List<Int> {
        require(rows >= 2 && columns >= 2) { "Regular graph mesh dimensions are invalid." }
        return buildList((rows - 1) * (columns - 1) * 6) {
            for (row in 0 until rows - 1) for (column in 0 until columns - 1) {
                val a = row * columns + column
                val b = a + 1
                val c = a + columns
                val d = c + 1
                add(a); add(c); add(b)
                add(b); add(c); add(d)
            }
        }
    }

    private fun vertexNormals(vertices: List<ARVector3>, indices: List<Int>): List<ARVector3> {
        val sums = Array(vertices.size) { doubleArrayOf(0.0, 0.0, 0.0) }
        indices.chunked(3).forEach { triangle ->
            val a = vertices[triangle[0]]; val b = vertices[triangle[1]]; val c = vertices[triangle[2]]
            val ux = b.x - a.x; val uy = b.y - a.y; val uz = b.z - a.z
            val vx = c.x - a.x; val vy = c.y - a.y; val vz = c.z - a.z
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx
            triangle.forEach { index ->
                sums[index][0] += nx; sums[index][1] += ny; sums[index][2] += nz
            }
        }
        return sums.map { normal ->
            val length = sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2])
            if (length <= 1e-12 || !length.isFinite()) ARVector3(0.0, 0.0, 1.0)
            else ARVector3(normal[0] / length, normal[1] / length, normal[2] / length)
        }
    }

    private fun boundsOf(vertices: List<ARVector3>): ARGraphBounds {
        require(vertices.isNotEmpty())
        return ARGraphBounds(
            ARVector3(vertices.minOf(ARVector3::x), vertices.minOf(ARVector3::y), vertices.minOf(ARVector3::z)),
            ARVector3(vertices.maxOf(ARVector3::x), vertices.maxOf(ARVector3::y), vertices.maxOf(ARVector3::z)),
        )
    }

    private fun sample(palette: List<EngineColor>, fraction: Float): EngineColor {
        require(palette.isNotEmpty()) { "The graph engine returned no colour palette." }
        if (palette.size == 1) return palette.first()
        val position = fraction.coerceIn(0f, 1f) * palette.lastIndex
        val lower = position.toInt().coerceIn(0, palette.lastIndex)
        val upper = (lower + 1).coerceAtMost(palette.lastIndex)
        val amount = position - lower
        val a = palette[lower]; val b = palette[upper]
        return EngineColor(
            red = a.red + (b.red - a.red) * amount,
            green = a.green + (b.green - a.green) * amount,
            blue = a.blue + (b.blue - a.blue) * amount,
            alpha = a.alpha + (b.alpha - a.alpha) * amount,
        )
    }

    private companion object {
        const val MAX_AR_VERTICES = 1_000_000L
        const val MAX_AR_INDICES = 6_000_000L
    }
}
