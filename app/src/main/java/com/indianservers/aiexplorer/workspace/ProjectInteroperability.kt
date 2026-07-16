package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.Vec2
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

enum class ProjectSectionKind { Mathematics, Notebook, Activities, LearningProgress, Settings, SpatialAnchors, Audit }
data class ProjectSection(val kind: ProjectSectionKind, val content: String, val revision: Long = 0, val required: Boolean = false)
data class AIExplorerProject(val id: String, val createdAt: Long, val modifiedAt: Long, val sections: List<ProjectSection>, val schemaVersion: Int = CURRENT_SCHEMA) {
    companion object { const val CURRENT_SCHEMA = 2 }
}
data class ProjectArchiveRecovery(val project: AIExplorerProject?, val checksumValid: Boolean, val recovered: Boolean, val diagnostics: List<String>, val skippedSections: List<ProjectSectionKind>)

/** Text archive boundary suitable for Android document providers and future ZIP packaging. */
object AIExplorerProjectArchive {
    const val maximumChars = 8_000_000
    fun create(id: String, maths: UniversalMathDocument, createdAt: Long, modifiedAt: Long, extras: List<ProjectSection> = emptyList()) = AIExplorerProject(
        id, createdAt, modifiedAt, listOf(ProjectSection(ProjectSectionKind.Mathematics, UniversalMathDocumentCodec.encode(maths), maths.revision, required = true)) + extras.filterNot { it.kind == ProjectSectionKind.Mathematics },
    )

    fun encode(project: AIExplorerProject): String {
        require(project.sections.size <= 32 && project.sections.map { it.kind }.distinct().size == project.sections.size)
        val lines = mutableListOf("AIEXPLORER_PROJECT|${AIExplorerProject.CURRENT_SCHEMA}|${pack(project.id)}|${project.createdAt}|${project.modifiedAt}")
        project.sections.sortedBy { it.kind.name }.forEach { section ->
            val content = pack(section.content)
            lines += "S|${section.kind}|${section.required}|${section.revision}|${sha256(section.content)}|$content"
        }
        val body = lines.joinToString("\n")
        val result = "$body\nEND|${sha256(body)}"
        require(result.length <= maximumChars) { "Project exceeds the 8 MB archive safety limit." }
        return result
    }

    fun decode(source: String, recover: Boolean = true): ProjectArchiveRecovery {
        if (source.length > maximumChars) return ProjectArchiveRecovery(null, false, false, listOf("Project exceeds the 8 MB safety limit."), emptyList())
        val lines = source.lines().filter(String::isNotBlank); val header = lines.firstOrNull()?.split('|')
        if (header == null || header.firstOrNull() != "AIEXPLORER_PROJECT") return ProjectArchiveRecovery(null, false, false, listOf("Not an AI Explorer project archive."), emptyList())
        val schema = header.getOrNull(1)?.toIntOrNull() ?: return ProjectArchiveRecovery(null, false, false, listOf("Missing archive schema."), emptyList())
        if (schema !in 1..AIExplorerProject.CURRENT_SCHEMA) return ProjectArchiveRecovery(null, false, false, listOf("Archive schema $schema is not supported."), emptyList())
        val end = lines.lastOrNull()?.takeIf { it.startsWith("END|") }; val body = lines.dropLast(if (end == null) 0 else 1).joinToString("\n")
        val globalValid = end?.substringAfter('|') == sha256(body)
        if (!globalValid && !recover) return ProjectArchiveRecovery(null, false, false, listOf("Archive checksum mismatch."), emptyList())
        val diagnostics = mutableListOf<String>(); if (!globalValid) diagnostics += "Archive checksum mismatch; validating sections independently."
        if (schema == 1) diagnostics += "Migrated schema 1 archive to schema ${AIExplorerProject.CURRENT_SCHEMA}."
        val sections = mutableListOf<ProjectSection>(); val skipped = mutableListOf<ProjectSectionKind>()
        lines.drop(1).filter { it.startsWith("S|") }.forEachIndexed { index, line ->
            val fields = line.split('|')
            runCatching {
                val kind = ProjectSectionKind.valueOf(fields[1]); val required = fields[2].toBoolean(); val revision = fields[3].toLong()
                val checksum = if (schema >= 2) fields[4] else null; val content = unpack(fields[if (schema >= 2) 5 else 4])
                if (checksum != null && checksum != sha256(content)) error("section checksum mismatch")
                if (kind == ProjectSectionKind.Mathematics) {
                    val math = UniversalMathDocumentCodec.decode(content, recover)
                    if (math.document == null) error(math.diagnostics.joinToString("; "))
                    if (math.recovered) diagnostics += math.diagnostics
                }
                sections += ProjectSection(kind, content, revision, required)
            }.onFailure {
                val kind = fields.getOrNull(1)?.let { value -> runCatching { ProjectSectionKind.valueOf(value) }.getOrNull() }
                if (kind != null) skipped += kind
                diagnostics += "Skipped damaged section $index${kind?.let { " ($it)" }.orEmpty()}: ${it.message ?: "invalid data"}."
            }
        }
        val missingMaths = sections.none { it.kind == ProjectSectionKind.Mathematics }
        if (missingMaths) return ProjectArchiveRecovery(null, globalValid, !globalValid || skipped.isNotEmpty(), diagnostics + "A recoverable Mathematics section is required.", skipped)
        val id = runCatching { unpack(header[2]) }.getOrDefault("recovered-project")
        val project = AIExplorerProject(id, header.getOrNull(3)?.toLongOrNull() ?: 0, header.getOrNull(4)?.toLongOrNull() ?: 0, sections, AIExplorerProject.CURRENT_SCHEMA)
        return ProjectArchiveRecovery(project, globalValid, !globalValid || schema < 2 || skipped.isNotEmpty() || diagnostics.isNotEmpty(), diagnostics, skipped)
    }

    private fun pack(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun unpack(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

data class ProjectMergeConflict(val objectId: String, val localRevision: Long, val remoteRevision: Long, val resolution: String)
data class ProjectMergeResult(val document: UniversalMathDocument, val conflicts: List<ProjectMergeConflict>, val diagnostics: List<String>)

object ProjectMergeEngine {
    fun merge(base: UniversalMathDocument?, local: UniversalMathDocument, remote: UniversalMathDocument, mergedAt: Long): ProjectMergeResult {
        require(local.id == remote.id && (base == null || base.id == local.id))
        val ids = local.objects.keys + remote.objects.keys + base?.objects.orEmpty().keys
        val conflicts = mutableListOf<ProjectMergeConflict>(); val merged = linkedMapOf<String, UniversalMathObject>()
        ids.sorted().forEach { id ->
            val b = base?.objects?.get(id); val l = local.objects[id]; val r = remote.objects[id]
            val chosen = when {
                l == r -> l
                l == b -> r
                r == b -> l
                l == null && r != null -> { conflicts += ProjectMergeConflict(id, -1, r.objectRevision, "remote kept; local deletion conflicted"); r }
                r == null && l != null -> { conflicts += ProjectMergeConflict(id, l.objectRevision, -1, "local kept; remote deletion conflicted"); l }
                l != null && r != null -> {
                    val winner = when { l.objectRevision > r.objectRevision -> l; r.objectRevision > l.objectRevision -> r; local.modifiedAt > remote.modifiedAt -> l; remote.modifiedAt > local.modifiedAt -> r; else -> if (l.toString() <= r.toString()) l else r }
                    conflicts += ProjectMergeConflict(id, l.objectRevision, r.objectRevision, if (winner === l) "local" else "remote"); winner
                }
                else -> null
            }
            if (chosen != null) merged[id] = chosen
        }
        val document = local.copy(objects = merged, revision = maxOf(local.revision, remote.revision) + 1, modifiedAt = mergedAt)
        val validation = UniversalMathDocumentEngine().validate(document)
        return ProjectMergeResult(document, conflicts, validation.diagnostics + if (validation.valid) emptyList() else listOf("Merged document requires dependency repair."))
    }
}

data class ExchangeCoverage(val format: String, val exported: Int, val imported: Int = 0, val skipped: List<String> = emptyList(), val warnings: List<String> = emptyList()) {
    val complete: Boolean get() = skipped.isEmpty()
}
data class GeoGebraExport(val xml: String, val coverage: ExchangeCoverage)
data class GeoGebraImport(val workspace: WorkspaceState, val coverage: ExchangeCoverage)

object GeoGebraExchange {
    fun exportXml(state: WorkspaceState): GeoGebraExport {
        val elements = mutableListOf<String>(); val commands = mutableListOf<String>(); val skipped = mutableListOf<String>()
        state.points.forEachIndexed { index, point -> elements += "<element type=\"point\" label=\"P$index\"><coords x=\"${point.x}\" y=\"${point.y}\" z=\"1.0\"/></element>" }
        state.functions.forEach { function -> elements += "<expression label=\"${function.name.xml()}\" exp=\"${function.expression.xml()}\"/>" }
        state.shapes.forEach { shape ->
            val labels = shape.pointIndices.map { "P$it" }
            val command = when (shape.type) {
                Shape2DType.Line -> "Line"; Shape2DType.Segment -> "Segment"; Shape2DType.Ray -> "Ray"; Shape2DType.Vector -> "Vector"
                Shape2DType.Circle -> "Circle"; Shape2DType.CircleThreePoints -> "Circle"; Shape2DType.Polygon, Shape2DType.Triangle, Shape2DType.Rectangle, Shape2DType.Square, Shape2DType.RegularPolygon -> "Polygon"
                Shape2DType.Ellipse -> "Ellipse"
                else -> null
            }
            if (command == null || labels.isEmpty()) skipped += "${shape.id}:${shape.type}" else commands += "<command name=\"$command\"><input ${labels.mapIndexed { i, value -> "a$i=\"$value\"" }.joinToString(" ")}/><output a0=\"${shape.name.xml()}\"/></command>"
        }
        val xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><geogebra format=\"5.0\"><construction>${elements.joinToString("")}${commands.joinToString("")}</construction></geogebra>"
        return GeoGebraExport(xml, ExchangeCoverage("GeoGebra geogebra.xml foundation", elements.size + commands.size, skipped = skipped, warnings = listOf("Package this XML as geogebra.xml inside a .ggb ZIP for external testing.")))
    }

    fun importXml(xml: String, base: WorkspaceState = WorkspaceState(points = emptyList(), shapes = emptyList(), functions = emptyList(), solids = emptyList(), vectors3D = emptyList())): GeoGebraImport {
        require(xml.length <= 4_000_000 && "<geogebra" in xml) { "A bounded GeoGebra XML document is required." }
        val points = Regex("<element\\s+type=\"point\"\\s+label=\"([^\"]+)\"[^>]*>\\s*<coords\\s+x=\"([^\"]+)\"\\s+y=\"([^\"]+)\"", RegexOption.IGNORE_CASE).findAll(xml).mapNotNull { match ->
            val x = match.groupValues[2].toDoubleOrNull(); val y = match.groupValues[3].toDoubleOrNull(); if (x == null || y == null) null else match.groupValues[1] to Vec2(x, y)
        }.toList()
        val functions = Regex("<expression\\s+label=\"([^\"]+)\"\\s+exp=\"([^\"]+)\"\\s*/>", RegexOption.IGNORE_CASE).findAll(xml).mapIndexed { index, match -> FunctionDefinition("ggb-f-$index", match.groupValues[1].unxml(), match.groupValues[2].unxml(), listOf("cyan", "violet", "green", "amber")[index % 4]) }.toList()
        val unsupported = Regex("<element\\s+type=\"([^\"]+)\"").findAll(xml).map { it.groupValues[1] }.filterNot { it.equals("point", true) }.distinct().map { "element:$it" }.toList()
        val state = base.copy(points = points.map { it.second }, functions = functions, name = "Imported GeoGebra construction", modifiedAt = System.currentTimeMillis())
        return GeoGebraImport(state, ExchangeCoverage("GeoGebra XML", exported = 0, imported = points.size + functions.size, skipped = unsupported, warnings = if (points.isEmpty() && functions.isEmpty()) listOf("No supported point or expression elements were found.") else emptyList()))
    }
}

object OpenMathExports {
    fun geometrySvg(state: WorkspaceState, width: Int = 1200, height: Int = 800): Pair<String, ExchangeCoverage> {
        require(width in 100..8000 && height in 100..8000)
        fun x(value: Double) = width / 2.0 + value * 50
        fun y(value: Double) = height / 2.0 - value * 50
        val body = mutableListOf<String>(); val skipped = mutableListOf<String>()
        state.shapes.forEach { shape ->
            val p = shape.pointIndices.mapNotNull(state.points::getOrNull)
            when (shape.type) {
                Shape2DType.Segment, Shape2DType.Line, Shape2DType.Ray, Shape2DType.Vector -> if (p.size >= 2) body += "<line x1=\"${x(p[0].x)}\" y1=\"${y(p[0].y)}\" x2=\"${x(p[1].x)}\" y2=\"${y(p[1].y)}\"/>" else skipped += shape.id
                Shape2DType.Circle -> if (p.size >= 2) body += "<circle cx=\"${x(p[0].x)}\" cy=\"${y(p[0].y)}\" r=\"${p[0].distanceTo(p[1]) * 50}\"/>" else skipped += shape.id
                Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square, Shape2DType.RegularPolygon -> if (p.size >= 3) body += "<polygon points=\"${p.joinToString(" ") { "${x(it.x)},${y(it.y)}" }}\"/>" else skipped += shape.id
                else -> skipped += shape.id
            }
        }
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $width $height\"><g fill=\"none\" stroke=\"#20d9ff\" stroke-width=\"3\">${body.joinToString("")}</g></svg>"
        return svg to ExchangeCoverage("SVG", body.size, skipped = skipped)
    }

    fun functionCsv(expression: String, minimum: Double, maximum: Double, rows: Int = 201): Pair<String, ExchangeCoverage> {
        val sample = GraphAnalysis().sample(expression, minimum, maximum, rows - 1)
        val csv = buildString { appendLine("x,y"); sample.points.forEach { appendLine("${it.x},${it.y}") } }
        return csv to ExchangeCoverage("CSV", sample.points.size, skipped = sample.breaks.map { "discontinuity-row-$it" })
    }
}

private fun String.xml() = replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
private fun String.unxml() = replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
