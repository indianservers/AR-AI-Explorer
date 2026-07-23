package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArCoordinateTransform
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitPolicy
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArLocalTransform
import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArScene
import com.indianservers.aiexplorer.arengine.contract.ArSceneObject
import com.indianservers.aiexplorer.arengine.contract.ArScenePlacement
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

enum class ArPlacementSurface { Horizontal, Vertical, Arbitrary, FeaturePoint, InstantEstimate, Simulator }

data class ArPlacementPreview(
    val hit: ArHitCandidate?,
    val placement: ArScenePlacement,
    val surface: ArPlacementSurface?,
    val canCommit: Boolean,
    val confidence: Double,
    val uncertaintyMeters: Double,
    val estimatedScale: String,
    val guidance: String,
)

object ArPlacementCoordinator {
    fun preview(
        candidates: Iterable<ArHitCandidate>,
        current: ArScenePlacement,
        sceneExtentUnits: Double,
    ): ArPlacementPreview {
        require(sceneExtentUnits.isFinite() && sceneExtentUnits >= 0.0)
        val hit = ArHitPolicy.rank(candidates).firstOrNull()
            ?: return ArPlacementPreview(
                hit = null,
                placement = current,
                surface = null,
                canCommit = false,
                confidence = 0.0,
                uncertaintyMeters = Double.POSITIVE_INFINITY,
                estimatedScale = scaleLabel(current),
                guidance = "Move slowly and aim at a textured surface.",
            )
        val extentMeters = sceneExtentUnits * current.metersPerMathUnit * current.localTransform.uniformScale
        val canCommit = hit.confidence >= .35 && hit.uncertaintyMeters <= .25 && extentMeters <= 2.5
        return ArPlacementPreview(
            hit = hit,
            placement = current.copy(anchorId = null, anchorPose = hit.pose),
            surface = when (hit.type) {
                ArHitType.Plane -> if (abs(hit.pose.orientation.rotate(ArVector3.Up).y) >= .7) ArPlacementSurface.Horizontal else ArPlacementSurface.Vertical
                ArHitType.Depth -> ArPlacementSurface.Arbitrary
                ArHitType.OrientedPoint -> ArPlacementSurface.FeaturePoint
                ArHitType.InstantPlacement -> ArPlacementSurface.InstantEstimate
                ArHitType.Simulator -> ArPlacementSurface.Simulator
            },
            canCommit = canCommit,
            confidence = hit.confidence,
            uncertaintyMeters = hit.uncertaintyMeters,
            estimatedScale = scaleLabel(current),
            guidance = when {
                !canCommit && extentMeters > 2.5 -> "Use Fit scale; the scene exceeds the safe placement extent."
                !canCommit -> "Keep scanning for a more reliable surface."
                hit.type == ArHitType.InstantPlacement -> "Estimated surface: hold still while ARCore refines the placement."
                else -> "Preview ready. Tap to place without changing the mathematical scene."
            },
        )
    }

    fun commit(preview: ArPlacementPreview, anchor: ArAnchorHandle): ArScenePlacement {
        require(preview.canCommit && preview.hit != null) { "A reliable preview is required before placement." }
        return preview.placement.copy(anchorId = anchor.id, anchorPose = anchor.pose)
    }

    private fun scaleLabel(placement: ArScenePlacement): String {
        val meters = placement.metersPerMathUnit * placement.localTransform.uniformScale
        return if (meters >= 1.0) "1 unit = ${format(meters)} m" else "1 unit = ${format(meters * 100.0)} cm"
    }

    private fun format(value: Double) = "%.2f".format(java.util.Locale.US, value)
}

data class ArRay(val origin: ArVector3, val direction: ArVector3) {
    init {
        require(direction.magnitude() > 1e-12)
    }

    val unitDirection: ArVector3 get() = direction * (1.0 / direction.magnitude())
}

enum class ArSubObjectKind { Whole, Vertex, Edge, Face }

data class ArPickHit(
    val objectId: String,
    val kind: ArSubObjectKind,
    val subObjectIndex: Int?,
    val distance: Double,
    val pointUnits: ArVector3,
    val occlusionRank: Int = 0,
)

object ArScenePicker {
    fun pickAll(
        scene: ArScene,
        worldRay: ArRay,
        toleranceUnits: Double = .08,
        includeOccluded: Boolean = true,
    ): List<ArPickHit> {
        require(toleranceUnits.isFinite() && toleranceUnits > 0.0)
        val localOrigin = ArCoordinateTransform.worldToMath(worldRay.origin, scene.placement)
        val localPoint = ArCoordinateTransform.worldToMath(worldRay.origin + worldRay.unitDirection, scene.placement)
        val ray = ArRay(localOrigin, localPoint - localOrigin)
        val hits = scene.objects.asSequence()
            .filter { it.visible && it.selectable }
            .flatMap { pickObject(it, ray, toleranceUnits).asSequence() }
            .sortedWith(compareBy<ArPickHit> { it.distance }.thenBy { it.objectId }.thenBy { it.kind.ordinal }.thenBy { it.subObjectIndex ?: -1 })
            .toList()
        if (includeOccluded) return hits.mapIndexed { index, hit -> hit.copy(occlusionRank = index) }
        return hits.firstOrNull()?.let(::listOf).orEmpty()
    }

    private fun pickObject(objectValue: ArSceneObject, ray: ArRay, tolerance: Double): List<ArPickHit> {
        val mesh = objectValue.mesh.transformed(objectValue.localTransform)
        val direction = ray.unitDirection
        val faces = mesh.triangleIndices.chunked(3).mapIndexedNotNull { index, triangle ->
            if (triangle.size != 3) return@mapIndexedNotNull null
            val distance = rayTriangle(
                ray.origin,
                direction,
                mesh.vertices[triangle[0]],
                mesh.vertices[triangle[1]],
                mesh.vertices[triangle[2]],
            ) ?: return@mapIndexedNotNull null
            ArPickHit(objectValue.id, ArSubObjectKind.Face, index, distance, ray.origin + direction * distance)
        }
        val edges = mesh.lineIndices.chunked(2).mapIndexedNotNull { index, edge ->
            if (edge.size != 2) return@mapIndexedNotNull null
            val value = raySegment(ray.origin, direction, mesh.vertices[edge[0]], mesh.vertices[edge[1]], tolerance)
                ?: return@mapIndexedNotNull null
            ArPickHit(objectValue.id, ArSubObjectKind.Edge, index, value.first, value.second)
        }
        val vertices = mesh.vertices.mapIndexedNotNull { index, vertex ->
            val distance = raySphere(ray.origin, direction, vertex, max(tolerance, mesh.pointRadiusUnits))
                ?: return@mapIndexedNotNull null
            ArPickHit(objectValue.id, ArSubObjectKind.Vertex, index, distance, vertex)
        }
        val nearest = (faces + edges + vertices).minByOrNull(ArPickHit::distance)
        val whole = nearest?.copy(kind = ArSubObjectKind.Whole, subObjectIndex = null)
        return listOfNotNull(whole) + vertices + edges + faces
    }

    private fun ArMesh.transformed(transform: ArLocalTransform) = copy(
        vertices = vertices.map {
            transform.offsetMeters + transform.orientation.rotate(it * transform.uniformScale)
        },
    )

    private fun rayTriangle(origin: ArVector3, direction: ArVector3, a: ArVector3, b: ArVector3, c: ArVector3): Double? {
        val edge1 = b - a
        val edge2 = c - a
        val h = cross(direction, edge2)
        val determinant = edge1.dot(h)
        if (abs(determinant) < 1e-12) return null
        val inverse = 1.0 / determinant
        val s = origin - a
        val u = inverse * s.dot(h)
        if (u !in 0.0..1.0) return null
        val q = cross(s, edge1)
        val v = inverse * direction.dot(q)
        if (v < 0.0 || u + v > 1.0) return null
        return (inverse * edge2.dot(q)).takeIf { it >= 0.0 }
    }

    private fun raySegment(origin: ArVector3, direction: ArVector3, a: ArVector3, b: ArVector3, radius: Double): Pair<Double, ArVector3>? {
        val segment = b - a
        val w = origin - a
        val aa = direction.dot(direction)
        val bb = direction.dot(segment)
        val cc = segment.dot(segment)
        if (cc < 1e-15) return raySphere(origin, direction, a, radius)?.let { it to a }
        val dd = direction.dot(w)
        val ee = segment.dot(w)
        val denominator = aa * cc - bb * bb
        var segmentT = if (abs(denominator) < 1e-15) 0.0 else ((aa * ee - bb * dd) / denominator).coerceIn(0.0, 1.0)
        var rayT = max(0.0, (bb * segmentT - dd) / aa)
        segmentT = ((bb * rayT + ee) / cc).coerceIn(0.0, 1.0)
        rayT = max(0.0, (bb * segmentT - dd) / aa)
        val point = a + segment * segmentT
        return (rayT to point).takeIf { (origin + direction * rayT - point).magnitude() <= radius }
    }

    private fun raySphere(origin: ArVector3, direction: ArVector3, center: ArVector3, radius: Double): Double? {
        val offset = origin - center
        val b = offset.dot(direction)
        val c = offset.dot(offset) - radius * radius
        val discriminant = b * b - c
        if (discriminant < 0.0) return null
        val root = sqrt(discriminant)
        return listOf(-b - root, -b + root).firstOrNull { it >= 0.0 }
    }
}

data class ArSelectionState(
    val objectIds: Set<String> = emptySet(),
    val primaryObjectId: String? = null,
    val subObject: ArPickHit? = null,
    val hiddenObjectIds: Set<String> = emptySet(),
    val isolatedObjectIds: Set<String>? = null,
    val lockedObjectIds: Set<String> = emptySet(),
)

object ArSelectionEngine {
    fun select(state: ArSelectionState, hit: ArPickHit, additive: Boolean): ArSelectionState {
        val ids = if (additive) {
            if (hit.objectId in state.objectIds) state.objectIds - hit.objectId else state.objectIds + hit.objectId
        } else {
            setOf(hit.objectId)
        }
        return state.copy(
            objectIds = ids,
            primaryObjectId = hit.objectId.takeIf(ids::contains),
            subObject = hit.takeIf { it.objectId in ids },
        )
    }

    fun cycle(hits: List<ArPickHit>, current: ArPickHit?, forward: Boolean = true): ArPickHit? {
        val ordered = hits.distinctBy { Triple(it.objectId, it.kind, it.subObjectIndex) }
        if (ordered.isEmpty()) return null
        val currentIndex = ordered.indexOfFirst {
            current != null && it.objectId == current.objectId && it.kind == current.kind && it.subObjectIndex == current.subObjectIndex
        }
        return ordered[(currentIndex + if (forward) 1 else -1).mod(ordered.size)]
    }

    fun hideSelected(state: ArSelectionState) = state.copy(
        hiddenObjectIds = state.hiddenObjectIds + state.objectIds,
        objectIds = emptySet(),
        primaryObjectId = null,
        subObject = null,
    )

    fun isolate(state: ArSelectionState) = state.copy(isolatedObjectIds = state.objectIds.takeIf { it.isNotEmpty() })
    fun showAll(state: ArSelectionState) = state.copy(hiddenObjectIds = emptySet(), isolatedObjectIds = null)
    fun toggleLock(state: ArSelectionState) = state.copy(
        lockedObjectIds = if (state.objectIds.all(state.lockedObjectIds::contains)) {
            state.lockedObjectIds - state.objectIds
        } else {
            state.lockedObjectIds + state.objectIds
        },
    )

    fun isVisible(state: ArSelectionState, id: String) =
        id !in state.hiddenObjectIds && (state.isolatedObjectIds == null || id in state.isolatedObjectIds)
}

enum class ArSnapKind { Axis, Plane, Point, Vertex, Edge, Face }

data class ArSnapTarget(
    val id: String,
    val kind: ArSnapKind,
    val point: ArVector3,
    val direction: ArVector3? = null,
    val normal: ArVector3? = null,
)

data class ArSnapPreview(
    val rawPoint: ArVector3,
    val snappedPoint: ArVector3,
    val target: ArSnapTarget?,
    val distance: Double,
)

object ArConstraintSnapEngine {
    fun snap(raw: ArVector3, targets: Iterable<ArSnapTarget>, tolerance: Double = .18): ArSnapPreview {
        require(tolerance.isFinite() && tolerance > 0.0)
        val candidates = targets.map { target ->
            val projected = when (target.kind) {
                ArSnapKind.Axis, ArSnapKind.Edge -> target.direction?.let { projectLine(raw, target.point, it) } ?: target.point
                ArSnapKind.Plane, ArSnapKind.Face -> target.normal?.let { projectPlane(raw, target.point, it) } ?: target.point
                ArSnapKind.Point, ArSnapKind.Vertex -> target.point
            }
            Triple(target, projected, (raw - projected).magnitude())
        }
        val best = candidates.minWithOrNull(compareBy<Triple<ArSnapTarget, ArVector3, Double>> { it.third }.thenBy { it.first.kind.ordinal }.thenBy { it.first.id })
        return if (best != null && best.third <= tolerance) {
            ArSnapPreview(raw, best.second, best.first, best.third)
        } else {
            ArSnapPreview(raw, raw, null, best?.third ?: Double.POSITIVE_INFINITY)
        }
    }

    fun sceneTargets(scene: ArScene): List<ArSnapTarget> = buildList {
        add(ArSnapTarget("axis-x", ArSnapKind.Axis, ArVector3.Zero, direction = ArVector3(1.0, 0.0, 0.0)))
        add(ArSnapTarget("axis-y", ArSnapKind.Axis, ArVector3.Zero, direction = ArVector3(0.0, 1.0, 0.0)))
        add(ArSnapTarget("axis-z", ArSnapKind.Axis, ArVector3.Zero, direction = ArVector3(0.0, 0.0, 1.0)))
        scene.objects.filter(ArSceneObject::visible).forEach { objectValue ->
            val vertices = objectValue.mesh.vertices.map { objectValue.localTransform.offsetMeters + objectValue.localTransform.orientation.rotate(it * objectValue.localTransform.uniformScale) }
            vertices.forEachIndexed { index, vertex -> add(ArSnapTarget("${objectValue.id}:v$index", ArSnapKind.Vertex, vertex)) }
            objectValue.mesh.lineIndices.chunked(2).forEachIndexed { index, edge ->
                if (edge.size == 2) {
                    val a = vertices[edge[0]]
                    val b = vertices[edge[1]]
                    add(ArSnapTarget("${objectValue.id}:e$index", ArSnapKind.Edge, a, direction = b - a))
                }
            }
            objectValue.mesh.triangleIndices.chunked(3).forEachIndexed { index, face ->
                if (face.size == 3) {
                    val a = vertices[face[0]]
                    val b = vertices[face[1]]
                    val c = vertices[face[2]]
                    add(ArSnapTarget("${objectValue.id}:f$index", ArSnapKind.Face, a, normal = cross(b - a, c - a)))
                }
            }
        }
    }

    private fun projectLine(point: ArVector3, origin: ArVector3, direction: ArVector3): ArVector3 {
        val denominator = direction.dot(direction)
        if (denominator < 1e-15) return origin
        return origin + direction * ((point - origin).dot(direction) / denominator)
    }

    private fun projectPlane(point: ArVector3, origin: ArVector3, normal: ArVector3): ArVector3 {
        val denominator = normal.dot(normal)
        if (denominator < 1e-15) return origin
        return point - normal * ((point - origin).dot(normal) / denominator)
    }
}

enum class ArGizmoMode { Translate, Rotate, Scale }
enum class ArGizmoAxis { X, Y, Z, Uniform }

data class ArTransformCommand(
    val label: String,
    val objectIds: Set<String>,
    val before: Map<String, ArLocalTransform>,
    val after: Map<String, ArLocalTransform>,
)

data class ArTransformGesture(
    val mode: ArGizmoMode,
    val axis: ArGizmoAxis,
    val before: Map<String, ArLocalTransform>,
    val preview: Map<String, ArLocalTransform> = before,
)

object ArGizmoEngine {
    fun begin(scene: ArScene, selection: ArSelectionState, mode: ArGizmoMode, axis: ArGizmoAxis): ArTransformGesture {
        val editable = selection.objectIds
            .filterNot(selection.lockedObjectIds::contains)
            .mapNotNull { id -> scene.objects.firstOrNull { it.id == id }?.let { id to it.localTransform } }
            .toMap()
        return ArTransformGesture(mode, axis, editable)
    }

    fun preview(
        gesture: ArTransformGesture,
        translation: ArVector3 = ArVector3.Zero,
        rotationDegrees: Double = 0.0,
        scaleFactor: Double = 1.0,
    ): ArTransformGesture {
        require(rotationDegrees.isFinite() && scaleFactor.isFinite() && scaleFactor > 0.0)
        val transformed = gesture.before.mapValues { (_, start) ->
            when (gesture.mode) {
                ArGizmoMode.Translate -> start.copy(offsetMeters = start.offsetMeters + constrain(translation, gesture.axis))
                ArGizmoMode.Rotate -> start.copy(orientation = (start.orientation * rotation(gesture.axis, rotationDegrees)).normalized())
                ArGizmoMode.Scale -> start.copy(uniformScale = (start.uniformScale * scaleFactor).coerceIn(.05, 20.0))
            }
        }
        return gesture.copy(preview = transformed)
    }

    fun apply(scene: ArScene, gesture: ArTransformGesture): ArScene = scene.copy(
        revision = scene.revision + 1,
        objects = scene.objects.map { objectValue ->
            gesture.preview[objectValue.id]?.let { objectValue.copy(localTransform = it) } ?: objectValue
        },
    )

    fun end(gesture: ArTransformGesture): ArTransformCommand? {
        if (gesture.before == gesture.preview) return null
        return ArTransformCommand(
            label = "${gesture.mode.name.lowercase().replaceFirstChar(Char::uppercase)} ${gesture.before.size} spatial object(s)",
            objectIds = gesture.before.keys,
            before = gesture.before,
            after = gesture.preview,
        )
    }

    private fun constrain(value: ArVector3, axis: ArGizmoAxis) = when (axis) {
        ArGizmoAxis.X -> ArVector3(value.x, 0.0, 0.0)
        ArGizmoAxis.Y -> ArVector3(0.0, value.y, 0.0)
        ArGizmoAxis.Z -> ArVector3(0.0, 0.0, value.z)
        ArGizmoAxis.Uniform -> value
    }

    private fun rotation(axis: ArGizmoAxis, degrees: Double) = when (axis) {
        ArGizmoAxis.X -> ArQuaternion.fromEulerDegrees(degrees, 0.0, 0.0)
        ArGizmoAxis.Y -> ArQuaternion.fromEulerDegrees(0.0, degrees, 0.0)
        ArGizmoAxis.Z -> ArQuaternion.fromEulerDegrees(0.0, 0.0, degrees)
        ArGizmoAxis.Uniform -> ArQuaternion.fromEulerDegrees(degrees, degrees, degrees)
    }
}

data class ArObjectGroup(val id: String, val name: String, val objectIds: Set<String>) {
    init {
        require(id.isNotBlank() && name.isNotBlank() && objectIds.isNotEmpty())
    }
}

data class ArClipboard(val objects: List<ArSceneObject> = emptyList())

data class ArSceneEditResult(
    val scene: ArScene,
    val selection: ArSelectionState,
    val removedObjectIds: Set<String> = emptySet(),
)

object ArSceneEditor {
    fun delete(scene: ArScene, selection: ArSelectionState): ArSceneEditResult {
        val removed = selection.objectIds
        return ArSceneEditResult(
            scene = scene.copy(
                revision = scene.revision + 1,
                objects = scene.objects.filterNot { it.id in removed },
                annotations = scene.annotations.filterNot { it.objectId in removed },
                measurements = scene.measurements.filterNot { it.objectIds.any(removed::contains) },
            ),
            selection = selection.copy(objectIds = emptySet(), primaryObjectId = null, subObject = null),
            removedObjectIds = removed,
        )
    }

    fun copy(scene: ArScene, selection: ArSelectionState) =
        ArClipboard(scene.objects.filter { it.id in selection.objectIds })

    fun paste(
        scene: ArScene,
        selection: ArSelectionState,
        clipboard: ArClipboard,
        idFactory: (String) -> String,
        offset: ArVector3 = ArVector3(.15, .15, .15),
    ): ArSceneEditResult {
        val duplicates = clipboard.objects.map { source ->
            source.copy(
                id = idFactory(source.id),
                label = "${source.label} copy",
                localTransform = source.localTransform.copy(offsetMeters = source.localTransform.offsetMeters + offset),
                dependencyIds = emptySet(),
            )
        }
        require(duplicates.map { it.id }.distinct().size == duplicates.size)
        require(duplicates.none { duplicate -> scene.objects.any { it.id == duplicate.id } })
        val ids = duplicates.mapTo(linkedSetOf(), ArSceneObject::id)
        return ArSceneEditResult(
            scene.copy(revision = scene.revision + 1, objects = scene.objects + duplicates),
            selection.copy(objectIds = ids, primaryObjectId = ids.lastOrNull(), subObject = null),
        )
    }
}

enum class ArPointerDevice { Touch, Stylus, Mouse }

data class ArPrecisionPolicy(
    val movementMultiplier: Double,
    val pickingTolerancePixels: Double,
    val hoverPreview: Boolean,
    val pressureIndependent: Boolean = true,
)

object ArInputPrecisionPolicy {
    fun forDevice(device: ArPointerDevice, precisionMode: Boolean, hovering: Boolean = false) = when (device) {
        ArPointerDevice.Stylus -> ArPrecisionPolicy(
            movementMultiplier = if (precisionMode) .2 else .35,
            pickingTolerancePixels = if (precisionMode) 10.0 else 16.0,
            hoverPreview = hovering,
        )
        ArPointerDevice.Mouse -> ArPrecisionPolicy(
            movementMultiplier = if (precisionMode) .35 else .65,
            pickingTolerancePixels = 14.0,
            hoverPreview = hovering,
        )
        ArPointerDevice.Touch -> ArPrecisionPolicy(
            movementMultiplier = if (precisionMode) .4 else 1.0,
            pickingTolerancePixels = 24.0,
            hoverPreview = false,
        )
    }
}

data class ArTrackingRecovery(
    val scene: ArScene,
    val selection: ArSelectionState,
    val manipulationEnabled: Boolean,
    val guidance: String,
)

object ArTrackingRecoveryCoordinator {
    fun lost(scene: ArScene, selection: ArSelectionState) = ArTrackingRecovery(
        scene = scene,
        selection = selection,
        manipulationEnabled = false,
        guidance = "Tracking paused. Hold still and return to the last tracked area; mathematical state is preserved.",
    )

    fun recovered(value: ArTrackingRecovery) = value.copy(
        manipulationEnabled = true,
        guidance = "Tracking recovered. Continue manipulating the same selected objects.",
    )
}

private fun cross(a: ArVector3, b: ArVector3) = ArVector3(
    a.y * b.z - a.z * b.y,
    a.z * b.x - a.x * b.z,
    a.x * b.y - a.y * b.x,
)
