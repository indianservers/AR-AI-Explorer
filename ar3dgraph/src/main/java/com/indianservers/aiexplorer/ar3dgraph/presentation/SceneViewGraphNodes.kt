package com.indianservers.aiexplorer.ar3dgraph.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphBounds
import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphRenderData
import com.indianservers.aiexplorer.ar3dgraph.integration.ARMeshRenderData
import io.github.sceneview.SceneScope
import io.github.sceneview.loaders.MaterialLoader
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
internal fun SceneScope.SceneViewGraphSurfaceNodes(
    materialLoader: MaterialLoader,
    graph: ARGraphRenderData,
) {
    val material = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF18D4D4), metallic = 0f, roughness = 0.35f, reflectance = 0.6f)
    }
    graph.meshes.forEach { mesh ->
        val resources = remember(engine, mesh) { SceneViewGraphMeshResources.create(engine, graph.bounds, mesh) }
        MeshNode(
            primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
            vertexBuffer = resources.vertexBuffer,
            indexBuffer = resources.indexBuffer,
            boundingBox = resources.boundingBox,
            materialInstance = material,
        )
    }
}

private data class SceneViewGraphMeshResources(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer,
    val boundingBox: Box,
) {
    companion object {
        fun create(engine: Engine, graphBounds: ARGraphBounds, mesh: ARMeshRenderData): SceneViewGraphMeshResources {
            val vertices = normalizedVertices(graphBounds, mesh)
            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(vertices.size / 3)
                .bufferCount(1)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
                .build(engine)
            vertexBuffer.setBufferAt(engine, 0, floatBuffer(vertices))

            val indices = mesh.indices.toIntArray()
            val indexBuffer = IndexBuffer.Builder()
                .indexCount(indices.size)
                .bufferType(IndexBuffer.Builder.IndexType.UINT)
                .build(engine)
            indexBuffer.setBuffer(engine, intBuffer(indices))

            val box = boundingBox(vertices)
            return SceneViewGraphMeshResources(vertexBuffer, indexBuffer, box)
        }

        private fun normalizedVertices(graphBounds: ARGraphBounds, mesh: ARMeshRenderData): FloatArray {
            val center = graphBounds.centre
            val span = maxOf(
                graphBounds.maximum.x - graphBounds.minimum.x,
                graphBounds.maximum.y - graphBounds.minimum.y,
                graphBounds.maximum.z - graphBounds.minimum.z,
                1e-6,
            )
            val scale = 0.85f / span.toFloat()
            return FloatArray(mesh.vertices.size * 3).also { out ->
                mesh.vertices.forEachIndexed { index, vertex ->
                    val offset = index * 3
                    out[offset] = ((vertex.x - center.x) * scale).toFloat()
                    out[offset + 1] = ((vertex.z - center.z) * scale).toFloat()
                    out[offset + 2] = ((vertex.y - center.y) * scale).toFloat()
                }
            }
        }

        private fun boundingBox(vertices: FloatArray): Box {
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            var i = 0
            while (i < vertices.size) {
                val x = vertices[i]
                val y = vertices[i + 1]
                val z = vertices[i + 2]
                minX = minOf(minX, x); minY = minOf(minY, y); minZ = minOf(minZ, z)
                maxX = maxOf(maxX, x); maxY = maxOf(maxY, y); maxZ = maxOf(maxZ, z)
                i += 3
            }
            return Box(
                floatArrayOf((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f),
                floatArrayOf((maxX - minX) / 2f, (maxY - minY) / 2f, (maxZ - minZ) / 2f),
            )
        }

        private fun floatBuffer(values: FloatArray) =
            ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(values)
                .apply { rewind() }

        private fun intBuffer(values: IntArray) =
            ByteBuffer.allocateDirect(values.size * Int.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
                .put(values)
                .apply { rewind() }
    }
}
