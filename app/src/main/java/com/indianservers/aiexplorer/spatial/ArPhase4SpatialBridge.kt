package com.indianservers.aiexplorer.spatial

import android.opengl.Matrix
import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArBlendMode
import com.indianservers.aiexplorer.arengine.contract.ArColor
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArLocalTransform
import com.indianservers.aiexplorer.arengine.contract.ArMaterial
import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArObjectKind
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArScene
import com.indianservers.aiexplorer.arengine.contract.ArSceneObject
import com.indianservers.aiexplorer.arengine.contract.ArScenePlacement
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.interaction.ArPickHit
import com.indianservers.aiexplorer.arengine.interaction.ArRay
import com.indianservers.aiexplorer.arengine.interaction.ArScenePicker
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionEngine
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState

object ArPhase4SpatialBridge {
    fun scene(
        source: SpatialRenderScene,
        placement: SpatialScenePlacement,
        anchor: ArAnchorHandle?,
        selection: ArSelectionState = ArSelectionState(),
        revision: Long = source.hashCode().toLong().and(Long.MAX_VALUE),
    ): ArScene {
        val anchorPose = anchor?.pose ?: ArPose(
            ArVector3(placement.pose.positionMeters.x, placement.pose.positionMeters.y, placement.pose.positionMeters.z),
        )
        val worldOffset = ArVector3(
            placement.pose.positionMeters.x - anchorPose.positionMeters.x,
            placement.pose.positionMeters.y - anchorPose.positionMeters.y,
            placement.pose.positionMeters.z - anchorPose.positionMeters.z,
        )
        val anchorLocalOffset = anchorPose.orientation.normalized().conjugate().rotate(worldOffset)
        return ArScene(
            id = source.id,
            revision = revision,
            objects = source.primitives.map { primitive ->
                val visible = primitive.visible && ArSelectionEngine.isVisible(selection, primitive.id)
                val selected = primitive.id in selection.objectIds
                val color = primitive.material.colorRgba
                ArSceneObject(
                    id = primitive.id,
                    kind = primitive.kind.toArKind(),
                    label = primitive.label,
                    mesh = ArMesh(
                        vertices = primitive.geometry.vertices.map { ArVector3(it.x, it.y, it.z) },
                        triangleIndices = primitive.geometry.triangles,
                        lineIndices = primitive.geometry.lines.flatMap { listOf(it.first, it.second) },
                        pointRadiusUnits = primitive.geometry.pointRadius,
                    ),
                    material = ArMaterial(
                        color = if (selected) ArColor(1f, .72f, .12f, 1f) else ArColor(
                            color.getOrElse(0) { 1f }.coerceIn(0f, 1f),
                            color.getOrElse(1) { 1f }.coerceIn(0f, 1f),
                            color.getOrElse(2) { 1f }.coerceIn(0f, 1f),
                            color.getOrElse(3) { 1f }.coerceIn(0f, 1f),
                        ),
                        metallic = primitive.material.metallic.coerceIn(0f, 1f),
                        roughness = primitive.material.roughness.coerceIn(0f, 1f),
                        emissive = if (selected) .28f else primitive.material.emissive.coerceAtLeast(0f),
                        blendMode = when (primitive.material.blendMode) {
                            SpatialBlendMode.Opaque -> ArBlendMode.Opaque
                            SpatialBlendMode.Transparent -> ArBlendMode.Transparent
                            SpatialBlendMode.Additive -> ArBlendMode.Additive
                        },
                    ),
                    visible = visible,
                    selectable = primitive.selectable,
                    metadata = mapOf("spatialKind" to primitive.kind.name),
                )
            },
            placement = ArScenePlacement(
                anchorId = anchor?.id ?: placement.anchorId.takeIf(String::isNotBlank),
                anchorPose = anchorPose,
                localTransform = ArLocalTransform(
                    offsetMeters = anchorLocalOffset,
                    orientation = ArQuaternion.fromEulerDegrees(
                        placement.pose.rotationDegrees.x,
                        placement.pose.rotationDegrees.y,
                        placement.pose.rotationDegrees.z,
                    ),
                    uniformScale = placement.pose.uniformScale,
                ),
                metersPerMathUnit = placement.metersPerMathUnit,
            ),
            environmentIntensity = source.environmentIntensity,
            depthOcclusionEnabled = source.depthOcclusion,
        )
    }

    fun pick(
        screenPoint: ArVector2,
        viewportWidth: Int,
        viewportHeight: Int,
        frame: ArFrameSnapshot,
        scene: ArScene,
        includeOccluded: Boolean = true,
    ): List<ArPickHit> {
        if (viewportWidth <= 0 || viewportHeight <= 0) return emptyList()
        val ray = worldRay(screenPoint, viewportWidth, viewportHeight, frame) ?: return emptyList()
        return ArScenePicker.pickAll(scene, ray, includeOccluded = includeOccluded)
    }

    private fun worldRay(
        screenPoint: ArVector2,
        viewportWidth: Int,
        viewportHeight: Int,
        frame: ArFrameSnapshot,
    ): ArRay? {
        val viewProjection = FloatArray(16)
        Matrix.multiplyMM(
            viewProjection,
            0,
            frame.camera.projectionMatrix.values.toFloatArray(),
            0,
            frame.camera.viewMatrix.values.toFloatArray(),
            0,
        )
        val inverse = FloatArray(16)
        if (!Matrix.invertM(inverse, 0, viewProjection, 0)) return null
        val x = screenPoint.x / viewportWidth * 2f - 1f
        val y = 1f - screenPoint.y / viewportHeight * 2f
        val near = unproject(inverse, x, y, -1f) ?: return null
        val far = unproject(inverse, x, y, 1f) ?: return null
        return ArRay(near, far - near)
    }

    private fun unproject(inverse: FloatArray, x: Float, y: Float, z: Float): ArVector3? {
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, inverse, 0, floatArrayOf(x, y, z, 1f), 0)
        val w = result[3]
        if (!w.isFinite() || kotlin.math.abs(w) < 1e-7f) return null
        return ArVector3((result[0] / w).toDouble(), (result[1] / w).toDouble(), (result[2] / w).toDouble())
    }

    private fun SpatialPrimitiveKind.toArKind() = when (this) {
        SpatialPrimitiveKind.Point -> ArObjectKind.Point
        SpatialPrimitiveKind.Curve, SpatialPrimitiveKind.VectorField -> ArObjectKind.Curve
        SpatialPrimitiveKind.Surface, SpatialPrimitiveKind.ProbabilitySurface -> ArObjectKind.Surface
        SpatialPrimitiveKind.Solid -> ArObjectKind.Solid
        SpatialPrimitiveKind.Annotation -> ArObjectKind.Annotation
    }
}
