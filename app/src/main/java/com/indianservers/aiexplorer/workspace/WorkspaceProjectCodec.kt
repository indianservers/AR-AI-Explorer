package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.AnchorTrackingState
import com.indianservers.aiexplorer.spatial.SpatialPose
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import com.indianservers.aiexplorer.spatial.TrackingQuality
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

data class WorkspaceProjectRecovery(
    val state: WorkspaceState?,
    val recovered: Boolean,
    val diagnostics: List<String>,
)

/** Complete, deterministic workspace snapshot embedded beside the canonical maths document. */
object WorkspaceSnapshotCodec {
    const val currentSchema = 5
    private const val maximumChars = 8_000_000

    fun encode(state: WorkspaceState): String {
        val records = buildList {
            add(listOf("W", pack(state.id), pack(state.name), state.module.name, state.modifiedAt.toString(), pack(state.surfaceExpression)).joinToString("|"))
            state.points.forEach { add("P|${it.x}|${it.y}") }
            state.shapes.forEach { shape ->
                add(listOf("S", pack(shape.id), shape.type.name, pack(shape.name), shape.visible, shape.locked, pack(shape.styleKey), shape.pointIndices.joinToString(",")).joinToString("|"))
            }
            state.pointDependencies.forEach { dependency ->
                add(listOf("D", dependency.outputIndex, dependency.inputIndices.joinToString(","), dependency.type.name, pack(dependency.name), dependency.parameters.joinToString(",")).joinToString("|"))
            }
            state.geometryConstraints.forEach { constraint ->
                add(listOf("C", pack(constraint.id), constraint.type.name, constraint.pointIndices.joinToString(","), constraint.shapeIds.joinToString(",") { pack(it) }, constraint.target ?: "").joinToString("|"))
            }
            state.geometryGroups.forEach { group -> add(listOf("G", pack(group.id), pack(group.name), group.locked, group.visible, group.shapeIds.joinToString(",") { pack(it) }).joinToString("|")) }
            state.functions.forEach { function ->
                add(listOf("F", pack(function.id), pack(function.name), pack(function.expression), pack(function.colorKey), function.visible).joinToString("|"))
            }
            state.graphRowMetadata.toSortedMap().forEach { (id, metadata) ->
                add(listOf("R", pack(id), metadata.collapsed, pack(metadata.note), pack(metadata.folder)).joinToString("|"))
            }
            state.graphSliderMetadata.toSortedMap().forEach { (parameter, metadata) ->
                add(listOf("L", pack(parameter), metadata.speed, metadata.mode.name, metadata.direction, metadata.value ?: "").joinToString("|"))
            }
            state.solids.forEach { solid ->
                add(listOf("O", solid.type.name, solid.width, solid.height, solid.depth, solid.radius, solid.topRadius,
                    solid.position.x, solid.position.y, solid.position.z, solid.rotation.x, solid.rotation.y, solid.rotation.z).joinToString("|"))
            }
            state.vectors3D.forEach { vector ->
                add(listOf("V", pack(vector.id), pack(vector.name), vector.start.x, vector.start.y, vector.start.z,
                    vector.end.x, vector.end.y, vector.end.z).joinToString("|"))
            }
            state.spatialPlacement.let { placement ->
                add(listOf("A", pack(placement.anchorId), placement.pose.positionMeters.x, placement.pose.positionMeters.y,
                    placement.pose.positionMeters.z, placement.pose.rotationDegrees.x, placement.pose.rotationDegrees.y,
                    placement.pose.rotationDegrees.z, placement.pose.uniformScale, placement.scaleMode.name,
                    placement.metersPerMathUnit, placement.trackingQuality.name, placement.estimated,
                    placement.depthOcclusionEnabled, placement.measurementUncertaintyMeters,
                    placement.environmentIntensity, placement.placedAt ?: -1L, placement.anchorTrackingState.name,
                    pack(placement.relocalizationMessage)).joinToString("|"))
            }
            state.universalMathDocument?.let { document -> add("U|${pack(UniversalMathDocumentCodec.encode(document))}") }
        }
        val body = records.joinToString("\n")
        return "AIEXPLORER_WORKSPACE|$currentSchema|${sha256(body)}\n$body".also {
            require(it.length <= maximumChars) { "Workspace exceeds the 8 MB safety limit." }
        }
    }

    fun decode(source: String, recover: Boolean = true): WorkspaceProjectRecovery {
        if (source.length > maximumChars) return WorkspaceProjectRecovery(null, false, listOf("Workspace exceeds the 8 MB safety limit."))
        val lines = source.lineSequence().filter(String::isNotBlank).toList()
        val header = lines.firstOrNull()?.split('|')
        if (header?.firstOrNull() != "AIEXPLORER_WORKSPACE") return WorkspaceProjectRecovery(null, false, listOf("Missing workspace header."))
        val schema = header.getOrNull(1)?.toIntOrNull() ?: return WorkspaceProjectRecovery(null, false, listOf("Missing workspace schema."))
        if (schema !in 1..currentSchema) return WorkspaceProjectRecovery(null, false, listOf("Workspace schema $schema is not supported."))
        val records = lines.drop(1)
        val checksumValid = header.getOrNull(2) == sha256(records.joinToString("\n"))
        if (!checksumValid && !recover) return WorkspaceProjectRecovery(null, false, listOf("Workspace checksum mismatch."))
        val diagnostics = mutableListOf<String>()
        if (!checksumValid) diagnostics += "Workspace checksum mismatch; valid records were recovered."
        if (schema < currentSchema) diagnostics += "Migrated workspace schema $schema to $currentSchema."
        val workspace = records.firstOrNull { it.startsWith("W|") }?.split('|')
            ?: return WorkspaceProjectRecovery(null, !checksumValid, diagnostics + "Workspace metadata is missing.")
        return runCatching {
            val points = mutableListOf<Vec2>(); val shapes = mutableListOf<Shape2D>(); val dependencies = mutableListOf<PointDependency>(); val constraints = mutableListOf<GeometryConstraint2D>(); val groups = mutableListOf<GeometryGroup2D>()
            val functions = mutableListOf<FunctionDefinition>(); val rows = linkedMapOf<String, GraphRowMetadataState>()
            val sliders = linkedMapOf<String, GraphSliderMetadataState>(); val solids = mutableListOf<Solid>(); val vectors = mutableListOf<Vector3D>()
            var placement = SpatialScenePlacement()
            var universalDocument: UniversalMathDocument? = null
            records.drop(1).forEachIndexed { index, record ->
                runCatching {
                    val f = record.split('|')
                    when (f[0]) {
                        "P" -> points += Vec2(f[1].toDouble(), f[2].toDouble())
                        "S" -> shapes += Shape2D(unpack(f[1]), Shape2DType.valueOf(f[2]), f[7].csvInts(), unpack(f[3]), f[4].toBoolean(), f[5].toBoolean(), unpack(f[6]))
                        "D" -> dependencies += PointDependency(f[1].toInt(), f[2].csvInts(), PointDependencyType.valueOf(f[3]), unpack(f[4]), f.getOrElse(5) { "" }.csvDoubles())
                        "C" -> constraints += GeometryConstraint2D(unpack(f[1]), GeometryConstraint2DType.valueOf(f[2]), f[3].csvInts(), f[4].csvPackedStrings(), f.getOrNull(5)?.toDoubleOrNull())
                        "G" -> groups += GeometryGroup2D(unpack(f[1]), unpack(f[2]), f[5].csvPackedStrings().toSet(), f[3].toBoolean(), f[4].toBoolean())
                        "F" -> functions += FunctionDefinition(unpack(f[1]), unpack(f[2]), unpack(f[3]), unpack(f[4]), f[5].toBoolean())
                        "R" -> rows[unpack(f[1])] = GraphRowMetadataState(f[2].toBoolean(), unpack(f[3]), unpack(f[4]))
                        "L" -> sliders[unpack(f[1])] = GraphSliderMetadataState(f[2].toDouble(), GraphSliderPlaybackMode.valueOf(f[3]), f[4].toInt(), f.getOrNull(5)?.toDoubleOrNull())
                        "O" -> solids += Solid(SolidType.valueOf(f[1]), f[2].toDouble(), f[3].toDouble(), f[4].toDouble(), f[5].toDouble(), f[6].toDouble(), Vec3(f[7].toDouble(), f[8].toDouble(), f[9].toDouble()), Vec3(f[10].toDouble(), f[11].toDouble(), f[12].toDouble()))
                        "V" -> vectors += Vector3D(unpack(f[1]), Vec3(f[3].toDouble(), f[4].toDouble(), f[5].toDouble()), Vec3(f[6].toDouble(), f[7].toDouble(), f[8].toDouble()), unpack(f[2]))
                        "A" -> placement = SpatialScenePlacement(anchorId = unpack(f[1]), pose = SpatialPose(Vec3(f[2].toDouble(), f[3].toDouble(), f[4].toDouble()), Vec3(f[5].toDouble(), f[6].toDouble(), f[7].toDouble()), f[8].toDouble()), scaleMode = ARScaleMode.valueOf(f[9]), metersPerMathUnit = f[10].toDouble(), trackingQuality = TrackingQuality.valueOf(f[11]), estimated = f[12].toBoolean(), depthOcclusionEnabled = f[13].toBoolean(), measurementUncertaintyMeters = f[14].toDouble(), environmentIntensity = f[15].toFloat(), placedAt = f[16].toLong().takeIf { it >= 0 }, anchorTrackingState = f.getOrNull(17)?.let { AnchorTrackingState.valueOf(it) } ?: AnchorTrackingState.Tracking, relocalizationMessage = f.getOrNull(18)?.let(::unpack).orEmpty())
                        "U" -> universalDocument = UniversalMathDocumentCodec.decode(unpack(f[1]), recover).document
                    }
                }.onFailure { diagnostics += "Skipped damaged workspace record $index: ${it.message ?: "invalid data"}." }
            }
            WorkspaceState(id = unpack(workspace[1]), name = unpack(workspace[2]), module = MathModule.valueOf(workspace[3]),
                points = points, shapes = shapes, pointDependencies = dependencies, geometryConstraints = constraints, geometryGroups = groups, functions = functions,
                solids = solids, vectors3D = vectors, graphRowMetadata = rows, graphSliderMetadata = sliders,
                surfaceExpression = unpack(workspace[5]), spatialPlacement = placement, universalMathDocument = universalDocument, modifiedAt = workspace[4].toLong()).recomputed()
        }.fold(
            onSuccess = { WorkspaceProjectRecovery(it, !checksumValid || diagnostics.isNotEmpty() || schema < currentSchema, diagnostics) },
            onFailure = { WorkspaceProjectRecovery(null, !checksumValid, diagnostics + (it.message ?: "Workspace could not be decoded.")) },
        )
    }

    private fun String.csvInts(): List<Int> = if (isBlank()) emptyList() else split(',').map(String::toInt)
    private fun String.csvDoubles(): List<Double> = if (isBlank()) emptyList() else split(',').map(String::toDouble)
    private fun String.csvPackedStrings(): List<String> = if (isBlank()) emptyList() else split(',').map(::unpack)
    private fun pack(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun unpack(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

/** Public project-file boundary: canonical document plus a lossless app snapshot and recovery diagnostics. */
object WorkspaceProjectCodec {
    fun encode(state: WorkspaceState): String = AIExplorerProjectArchive.encode(
        AIExplorerProjectArchive.create(
            id = state.id,
            maths = UniversalWorkspaceBridge.fromWorkspace(state),
            createdAt = state.modifiedAt,
            modifiedAt = state.modifiedAt,
            extras = listOf(ProjectSection(ProjectSectionKind.Audit, WorkspaceSnapshotCodec.encode(state), state.modifiedAt)),
        ),
    )

    fun decode(source: String, recover: Boolean = true): WorkspaceProjectRecovery {
        val archive = AIExplorerProjectArchive.decode(source, recover)
        val project = archive.project ?: return WorkspaceProjectRecovery(null, archive.recovered, archive.diagnostics)
        val snapshot = project.sections.firstOrNull { it.kind == ProjectSectionKind.Audit }
            ?.let { WorkspaceSnapshotCodec.decode(it.content, recover) }
        val maths = project.sections.first { it.kind == ProjectSectionKind.Mathematics }
        if (snapshot?.state != null) {
            if (snapshot.state.universalMathDocument == null) return WorkspaceProjectRecovery(snapshot.state, archive.recovered || snapshot.recovered, archive.diagnostics + snapshot.diagnostics)
            val canonical = UniversalMathDocumentCodec.decode(maths.content, recover)
            return WorkspaceProjectRecovery(snapshot.state.copy(universalMathDocument = canonical.document), archive.recovered || snapshot.recovered || canonical.recovered,
                archive.diagnostics + snapshot.diagnostics + canonical.diagnostics)
        }
        val base = WorkspaceState(id = project.id, points = emptyList(), shapes = emptyList(), pointDependencies = emptyList(), functions = emptyList(), solids = emptyList(), vectors3D = emptyList())
        val restored = WorkspaceJson.applyRecoveredMathDocument(maths.content, base)
        return restored.fold(
            onSuccess = { WorkspaceProjectRecovery(it, true, archive.diagnostics + snapshot?.diagnostics.orEmpty() + "Recovered from the canonical maths document.") },
            onFailure = { WorkspaceProjectRecovery(null, true, archive.diagnostics + snapshot?.diagnostics.orEmpty() + (it.message ?: "Project could not be recovered.")) },
        )
    }
}
