package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.ar3dgraph.integration.EngineColor
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineAxisStyle
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineGraphResult
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineMeshSnapshot
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineVector3
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineContract
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphGenerationRequest
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.SpatialSurfaceKind
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.core.SurfaceMesh
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import com.indianservers.aiexplorer.spatial.SurfaceDomain
import com.indianservers.aiexplorer.spatial.TypedSurfaceMesher

/**
 * Additive bridge to the same public engines used by the normal 3D Graph workspace. It performs no
 * parsing, sampling, tolerance, discontinuity, or mathematical calculations of its own.
 */
internal class Existing3DGraphEngineBridge(
    private val explicitEngine: Graph3D = Graph3D(),
    private val typedMesher: TypedSurfaceMesher = TypedSurfaceMesher(),
) : GraphEngineContract {
    override fun generate(request: GraphGenerationRequest): EngineGraphResult {
        if (request.domainMinimum >= request.domainMaximum) {
            return EngineGraphResult.ValidationError("Domain minimum must be less than domain maximum.")
        }
        if (request.density !in 8..56) {
            return EngineGraphResult.ValidationError("Resolution must be between 8 and 56.")
        }
        val enabled = request.equations.filter { it.enabled }
        if (enabled.isEmpty()) return EngineGraphResult.ValidationError("Enter at least one enabled equation.")

        val parsed = enabled.map { equation ->
            val interpretation = SurfaceInputInterpreter.interpret(equation.expression).getOrElse {
                return EngineGraphResult.ValidationError(it.message ?: "Invalid surface equation.")
            }
            equation to interpretation
        }

        return runCatching {
            val meshes = parsed.mapIndexed { index, (equation, interpretation) ->
                val layer = SpatialSurfaceLayer(
                    id = equation.id,
                    expression = interpretation.expression,
                    kind = interpretation.kind,
                    expressionY = interpretation.expressionY,
                    expressionZ = interpretation.expressionZ,
                    opacity = equation.opacity.coerceIn(0.1, 1.0),
                    colorIndex = equation.colorIndex,
                )
                val mesh = when (interpretation.kind) {
                    SpatialSurfaceKind.Explicit -> explicitEngine.mesh(
                        expression = interpretation.expression,
                        min = request.domainMinimum,
                        max = request.domainMaximum,
                        density = request.density,
                    )
                    SpatialSurfaceKind.Implicit,
                    SpatialSurfaceKind.Parametric,
                    -> typedMesh(layer, request)
                }
                require(mesh.vertices.isNotEmpty()) { "The graph engine returned no geometry." }
                require(mesh.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }) {
                    "The graph engine returned non-finite geometry."
                }
                val appearance = layer.workspaceAppearance().copy(colorIndex = layer.colorIndex + index)
                EngineMeshSnapshot(
                    equationId = equation.id,
                    canonicalEquation = interpretation.canonicalEquation,
                    vertices = mesh.vertices.map { EngineVector3(it.x, it.y, it.z) },
                    rows = mesh.rows,
                    columns = mesh.columns,
                    triangleIndices = mesh.triangleIndices.toList(),
                    palette = appearance.palette.colors.map { EngineColor(it.red, it.green, it.blue, it.alpha) },
                    lineColor = appearance.color.let { EngineColor(it.red, it.green, it.blue, it.alpha) },
                    opacity = layer.opacity.toFloat(),
                )
            }
            val axes = WorkspaceVisualStyles.Spectral.axes
            EngineGraphResult.Success(
                request.copy(equations = request.equations.toList()),
                meshes,
                EngineAxisStyle(
                    x = axes.x.let { EngineColor(it.red, it.green, it.blue, it.alpha) },
                    y = axes.y.let { EngineColor(it.red, it.green, it.blue, it.alpha) },
                    z = axes.z.let { EngineColor(it.red, it.green, it.blue, it.alpha) },
                    grid = axes.grid.let { EngineColor(it.red, it.green, it.blue, it.alpha) },
                ),
            )
        }.getOrElse {
            EngineGraphResult.GenerationError(it.message ?: "The existing 3D graph engine could not generate this surface.", it)
        }
    }

    private fun typedMesh(layer: SpatialSurfaceLayer, request: GraphGenerationRequest): SurfaceMesh {
        val domain = SurfaceDomain(
            request.domainMinimum..request.domainMaximum,
            request.domainMinimum..request.domainMaximum,
            request.domainMinimum..request.domainMaximum,
        )
        val definition = when (layer.kind) {
            SpatialSurfaceKind.Explicit -> error("Explicit surfaces use Graph3D.")
            SpatialSurfaceKind.Implicit -> SurfaceDefinition3D.Implicit(layer.id, layer.expression, domain)
            SpatialSurfaceKind.Parametric -> SurfaceDefinition3D.Parametric(
                id = layer.id,
                x = layer.expression,
                y = layer.expressionY,
                z = layer.expressionZ,
                domain = domain,
            )
        }
        val geometry = typedMesher.mesh(definition, request.density).geometry
        return SurfaceMesh(
            vertices = geometry.vertices,
            rows = 1,
            columns = geometry.vertices.size.coerceAtLeast(1),
            triangleIndices = geometry.triangles,
        )
    }
}
