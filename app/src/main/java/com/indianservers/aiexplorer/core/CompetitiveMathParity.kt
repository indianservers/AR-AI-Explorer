package com.indianservers.aiexplorer.core

import com.indianservers.aiexplorer.spatial.SpatialConstruction3D
import com.indianservers.aiexplorer.spatial.SpatialConstructionDocument
import com.indianservers.aiexplorer.spatial.SpatialConstructionEngine
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

enum class MathStudioView { Graph, Geometry2D, Spatial3D }
enum class StudioTokenKind { GraphExpression, Point, Line, Circle, Conic, Vector, Plane, Surface, Constraint, Measurement }

data class StudioObjectToken(
    val id: String,
    val view: MathStudioView,
    val kind: StudioTokenKind,
    val dependencies: List<String>,
    val algebra: String,
    val accessibleLabel: String,
    val focusOrder: Int,
)

data class UnifiedConstructionSession(
    val graph: GraphWorkspaceState = GraphWorkspaceState(),
    val geometry: DynamicGeometryDocument = DynamicGeometryDocument(),
    val spatial: SpatialConstructionDocument = SpatialConstructionDocument(),
    val surfaces: List<SurfaceDefinition3D> = emptyList(),
    val commands: List<String> = emptyList(),
    val links: List<CrossViewLink> = emptyList(),
    val revision: Long = 0,
)

sealed interface UnifiedConstructionCommand {
    val id: String
    data class Graph(override val id: String, val expression: String) : UnifiedConstructionCommand
    data class Point2D(override val id: String, val point: Vec2) : UnifiedConstructionCommand
    data class Midpoint2D(override val id: String, val first: String, val second: String) : UnifiedConstructionCommand
    data class Section2D(override val id: String, val first: String, val second: String, val ratio: Double) : UnifiedConstructionCommand
    data class TriangleCenter2D(override val id: String, val kind: String, val first: String, val second: String, val third: String) : UnifiedConstructionCommand
    data class PointOn2D(override val id: String, val objectId: String, val parameter: Double) : UnifiedConstructionCommand
    data class Intersection2D(override val id: String, val firstObject: String, val secondObject: String, val branch: Int) : UnifiedConstructionCommand
    data class Line2D(override val id: String, val first: String, val second: String) : UnifiedConstructionCommand
    data class Parallel2D(override val id: String, val through: String, val lineA: String, val lineB: String) : UnifiedConstructionCommand
    data class Perpendicular2D(override val id: String, val through: String, val lineA: String, val lineB: String) : UnifiedConstructionCommand
    data class Circle2D(override val id: String, val center: String, val through: String) : UnifiedConstructionCommand
    data class Conic2D(override val id: String, val kind: String, val first: String, val second: String, val third: String) : UnifiedConstructionCommand
    data class Tangent2D(override val id: String, val conic: String, val at: String) : UnifiedConstructionCommand
    data class Transform2D(override val id: String, val source: String, val transformation: GeometryTransformation) : UnifiedConstructionCommand
    data class Constraint2D(override val id: String, val constraint: GeometryConstraint) : UnifiedConstructionCommand
    data class Point3D(override val id: String, val point: Vec3) : UnifiedConstructionCommand
    data class Vector3D(override val id: String, val start: String, val end: String) : UnifiedConstructionCommand
    data class Line3DCommand(override val id: String, val first: String, val second: String) : UnifiedConstructionCommand
    data class Plane3DCommand(override val id: String, val first: String, val second: String, val third: String) : UnifiedConstructionCommand
    data class ExplicitSurface(override val id: String, val expression: String) : UnifiedConstructionCommand
    data class ImplicitSurface(override val id: String, val equation: String) : UnifiedConstructionCommand
    data class ParametricSurface(override val id: String, val x: String, val y: String, val z: String) : UnifiedConstructionCommand
}

object UnifiedConstructionCommandParser {
    fun parse(source: String): UnifiedConstructionCommand {
        val match = Regex("([A-Za-z][A-Za-z0-9_]*)\\s*\\((.*)\\)").matchEntire(source.trim()) ?: error("Use commandName(arguments).")
        val name = match.groupValues[1].lowercase(); val arguments = splitArguments(match.groupValues[2])
        fun id() = arguments.firstOrNull()?.trim()?.also { require(it.matches(Regex("[A-Za-z][A-Za-z0-9_-]*"))) } ?: error("The command needs an object id.")
        fun number(index: Int) = arguments.getOrNull(index)?.trim()?.toDoubleOrNull() ?: error("Argument ${index + 1} must be numeric.")
        fun ref(index: Int) = arguments.getOrNull(index)?.trim()?.takeIf(String::isNotBlank) ?: error("Missing reference ${index + 1}.")
        return when (name) {
            "graph" -> UnifiedConstructionCommand.Graph(id(), arguments.drop(1).joinToString(",").trim())
            "point2d", "point" -> UnifiedConstructionCommand.Point2D(id(), Vec2(number(1), number(2)))
            "midpoint" -> UnifiedConstructionCommand.Midpoint2D(id(), ref(1), ref(2))
            "section" -> UnifiedConstructionCommand.Section2D(id(), ref(1), ref(2), number(3))
            "centroid", "circumcenter", "incenter", "orthocenter" -> UnifiedConstructionCommand.TriangleCenter2D(id(), name, ref(1), ref(2), ref(3))
            "pointon" -> UnifiedConstructionCommand.PointOn2D(id(), ref(1), number(2))
            "intersection" -> UnifiedConstructionCommand.Intersection2D(id(), ref(1), ref(2), arguments.getOrNull(3)?.trim()?.toIntOrNull() ?: 0)
            "line2d" -> UnifiedConstructionCommand.Line2D(id(), ref(1), ref(2))
            "parallel" -> UnifiedConstructionCommand.Parallel2D(id(), ref(1), ref(2), ref(3))
            "perpendicular" -> UnifiedConstructionCommand.Perpendicular2D(id(), ref(1), ref(2), ref(3))
            "circle" -> UnifiedConstructionCommand.Circle2D(id(), ref(1), ref(2))
            "ellipse", "hyperbola", "parabola" -> UnifiedConstructionCommand.Conic2D(id(), name, ref(1), ref(2), ref(3))
            "tangent" -> UnifiedConstructionCommand.Tangent2D(id(), ref(1), ref(2))
            "translate" -> UnifiedConstructionCommand.Transform2D(id(), ref(1), GeometryTransformation.Translation(number(2), number(3)))
            "rotate" -> UnifiedConstructionCommand.Transform2D(id(), ref(1), GeometryTransformation.Rotation(ref(2), number(3)))
            "reflect" -> UnifiedConstructionCommand.Transform2D(id(), ref(1), GeometryTransformation.Reflection(ref(2), ref(3)))
            "dilate" -> UnifiedConstructionCommand.Transform2D(id(), ref(1), GeometryTransformation.Dilation(ref(2), number(3)))
            "equal" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.Equal(id(), ref(1), ref(2), ref(3), ref(4)))
            "parallelconstraint" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.Parallel(id(), ref(1), ref(2), ref(3), ref(4)))
            "perpendicularconstraint" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.Perpendicular(id(), ref(1), ref(2), ref(3), ref(4)))
            "fixedlength" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.FixedLength(id(), ref(1), ref(2), number(3)))
            "fixedangle" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.FixedAngle(id(), ref(1), ref(2), ref(3), number(4)))
            "concentric" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.Concentric(id(), ref(1), ref(2)))
            "tangentconstraint" -> UnifiedConstructionCommand.Constraint2D(id(), GeometryConstraint.Tangent(id(), ref(1), ref(2)))
            "point3d" -> UnifiedConstructionCommand.Point3D(id(), Vec3(number(1), number(2), number(3)))
            "vector3d" -> UnifiedConstructionCommand.Vector3D(id(), ref(1), ref(2))
            "line3d" -> UnifiedConstructionCommand.Line3DCommand(id(), ref(1), ref(2))
            "plane3d" -> UnifiedConstructionCommand.Plane3DCommand(id(), ref(1), ref(2), ref(3))
            "surface" -> UnifiedConstructionCommand.ExplicitSurface(id(), arguments.drop(1).joinToString(",").trim())
            "implicitsurface" -> UnifiedConstructionCommand.ImplicitSurface(id(), arguments.drop(1).joinToString(",").trim())
            "parametricsurface" -> { require(arguments.size == 4); UnifiedConstructionCommand.ParametricSurface(id(), arguments[1], arguments[2], arguments[3]) }
            else -> error("Unknown construction command '$name'.")
        }
    }

    fun catalog(): Map<MathStudioView, List<String>> = mapOf(
        MathStudioView.Graph to listOf("graph(id, expression)"),
        MathStudioView.Geometry2D to listOf(
            "point2d(id,x,y)", "midpoint(id,A,B)", "section(id,A,B,ratio)", "centroid(id,A,B,C)", "circumcenter(id,A,B,C)", "incenter(id,A,B,C)", "orthocenter(id,A,B,C)",
            "pointOn(id,object,t)", "intersection(id,object1,object2,branch)", "line2d(id,A,B)", "parallel(id,P,A,B)", "perpendicular(id,P,A,B)", "circle(id,center,through)",
            "ellipse(id,F1,F2,P)", "parabola(id,F,D1,D2)", "hyperbola(id,F1,F2,P)", "tangent(id,conic,P)",
            "translate(id,source,dx,dy)", "rotate(id,source,center,degrees)", "reflect(id,source,A,B)", "dilate(id,source,center,scale)",
            "equal(id,A,B,C,D)", "parallelConstraint(id,A,B,C,D)", "perpendicularConstraint(id,A,B,C,D)", "fixedLength(id,A,B,length)", "fixedAngle(id,A,V,B,degrees)", "concentric(id,c1,c2)", "tangentConstraint(id,line,conic)",
        ),
        MathStudioView.Spatial3D to listOf("point3d(id,x,y,z)", "vector3d(id,A,B)", "line3d(id,A,B)", "plane3d(id,A,B,C)", "surface(id,z=f(x,y))", "implicitSurface(id,F(x,y,z)=0)", "parametricSurface(id,x(u,v),y(u,v),z(u,v))"),
    )

    private fun splitArguments(source: String): List<String> {
        val result = mutableListOf<String>(); val token = StringBuilder(); var depth = 0
        source.forEach { character -> when (character) { '(' -> { depth++; token.append(character) }; ')' -> { depth--; token.append(character) }; ',' -> if (depth == 0) { result += token.toString().trim(); token.clear() } else token.append(character); else -> token.append(character) } }
        result += token.toString().trim(); return result
    }
}

class UnifiedConstructionEngine(
    private val geometryEngine: DynamicGeometryEngine = DynamicGeometryEngine(),
    private val spatialEngine: SpatialConstructionEngine = SpatialConstructionEngine(),
) {
    fun execute(session: UnifiedConstructionSession, source: String): UnifiedConstructionSession = execute(session, UnifiedConstructionCommandParser.parse(source), source)

    fun execute(session: UnifiedConstructionSession, command: UnifiedConstructionCommand, source: String = command.toString()): UnifiedConstructionSession {
        require(tokens(session).none { it.id == command.id }) { "Object '${command.id}' already exists." }
        val updated = when (command) {
            is UnifiedConstructionCommand.Graph -> {
                val typed = TypedGraphExpressionParser.parse(command.expression)
                val row = InteractiveGraphRow(command.id, command.expression, "cyan", typed = typed)
                val graph = GraphWorkspaceReducer().reduce(session.graph.copy(rows = session.graph.rows + row), GraphWorkspaceAction.EditExpression(command.id, command.expression))
                session.copy(graph = graph)
            }
            is UnifiedConstructionCommand.Point2D -> session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, DynamicPointRule.Free(command.point)), "Command input"))
            is UnifiedConstructionCommand.Midpoint2D -> session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, DynamicPointRule.Midpoint(command.first, command.second)), "Midpoint command"))
            is UnifiedConstructionCommand.Section2D -> session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, DynamicPointRule.Section(command.first, command.second, command.ratio)), "Division point command"))
            is UnifiedConstructionCommand.TriangleCenter2D -> {
                val rule = when (command.kind) {
                    "centroid" -> DynamicPointRule.Centroid(command.first, command.second, command.third)
                    "circumcenter" -> DynamicPointRule.Circumcenter(command.first, command.second, command.third)
                    "incenter" -> DynamicPointRule.Incenter(command.first, command.second, command.third)
                    "orthocenter" -> DynamicPointRule.Orthocenter(command.first, command.second, command.third)
                    else -> error("Unknown triangle center '${command.kind}'.")
                }
                session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, rule), "Triangle center command"))
            }
            is UnifiedConstructionCommand.PointOn2D -> session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, DynamicPointRule.PointOnObject(command.objectId, command.parameter)), "Point-on-object command"))
            is UnifiedConstructionCommand.Intersection2D -> session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, DynamicPointRule.ObjectIntersection(command.firstObject, command.secondObject, command.branch)), "Intersection command"))
            is UnifiedConstructionCommand.Line2D -> session.copy(geometry = geometryEngine.addObject(session.geometry, DynamicGeometryObject.Line(command.id, command.first, command.second), "Line command"))
            is UnifiedConstructionCommand.Parallel2D -> session.copy(geometry = geometryEngine.addObject(session.geometry, DynamicGeometryObject.Parallel(command.id, command.through, command.lineA, command.lineB), "Parallel command"))
            is UnifiedConstructionCommand.Perpendicular2D -> session.copy(geometry = geometryEngine.addObject(session.geometry, DynamicGeometryObject.Perpendicular(command.id, command.through, command.lineA, command.lineB), "Perpendicular command"))
            is UnifiedConstructionCommand.Circle2D -> session.copy(geometry = geometryEngine.addObject(session.geometry, DynamicGeometryObject.Circle(command.id, command.center, command.through), "Circle command"))
            is UnifiedConstructionCommand.Conic2D -> {
                val conic = when (command.kind) {
                    "ellipse" -> DynamicGeometryObject.Ellipse(command.id, command.first, command.second, command.third)
                    "hyperbola" -> DynamicGeometryObject.Hyperbola(command.id, command.first, command.second, command.third)
                    "parabola" -> DynamicGeometryObject.Parabola(command.id, command.first, command.second, command.third)
                    else -> error("Unknown conic '${command.kind}'.")
                }
                session.copy(geometry = geometryEngine.addObject(session.geometry, conic, "Conic command"))
            }
            is UnifiedConstructionCommand.Tangent2D -> {
                val conic = session.geometry.objects.firstOrNull { it.id == command.conic } ?: error("Unknown conic '${command.conic}'.")
                val tangent = if (conic is DynamicGeometryObject.Circle) DynamicGeometryObject.TangentCircle(command.id, command.conic, command.at) else DynamicGeometryObject.TangentConic(command.id, command.conic, command.at)
                session.copy(geometry = geometryEngine.addObject(session.geometry, tangent, "Tangent command"))
            }
            is UnifiedConstructionCommand.Transform2D -> session.copy(geometry = geometryEngine.addPoint(session.geometry, DynamicPoint(command.id, DynamicPointRule.Transform(command.source, command.transformation)), "Transformation command"))
            is UnifiedConstructionCommand.Constraint2D -> session.copy(geometry = geometryEngine.addConstraint(session.geometry, command.constraint, "Constraint command"))
            is UnifiedConstructionCommand.Point3D -> session.copy(spatial = spatialEngine.add(session.spatial, SpatialConstruction3D.Point(command.id, command.point)))
            is UnifiedConstructionCommand.Vector3D -> session.copy(spatial = spatialEngine.add(session.spatial, SpatialConstruction3D.Vector(command.id, command.start, command.end)))
            is UnifiedConstructionCommand.Line3DCommand -> session.copy(spatial = spatialEngine.add(session.spatial, SpatialConstruction3D.Line(command.id, command.first, command.second)))
            is UnifiedConstructionCommand.Plane3DCommand -> session.copy(spatial = spatialEngine.add(session.spatial, SpatialConstruction3D.Plane(command.id, command.first, command.second, command.third)))
            is UnifiedConstructionCommand.ExplicitSurface -> session.copy(surfaces = session.surfaces + SurfaceDefinition3D.Explicit(command.id, command.expression))
            is UnifiedConstructionCommand.ImplicitSurface -> session.copy(surfaces = session.surfaces + SurfaceDefinition3D.Implicit(command.id, command.equation))
            is UnifiedConstructionCommand.ParametricSurface -> session.copy(surfaces = session.surfaces + SurfaceDefinition3D.Parametric(command.id, command.x, command.y, command.z))
        }
        return updated.copy(commands = session.commands + source, revision = session.revision + 1)
    }

    fun tokens(session: UnifiedConstructionSession): List<StudioObjectToken> = buildList {
        session.graph.rows.forEach { row -> add(StudioObjectToken(row.id, MathStudioView.Graph, StudioTokenKind.GraphExpression, emptyList(), row.source, "Graph expression ${row.id}: ${row.source}", size)) }
        session.geometry.protocol.forEach { step ->
            val point = session.geometry.points.firstOrNull { it.id == step.id }; val objectValue = session.geometry.objects.firstOrNull { it.id == step.id }
            val kind = when (objectValue) { is DynamicGeometryObject.Line -> StudioTokenKind.Line; is DynamicGeometryObject.Circle -> StudioTokenKind.Circle; is DynamicGeometryObject.Ellipse, is DynamicGeometryObject.Parabola, is DynamicGeometryObject.Hyperbola -> StudioTokenKind.Conic; else -> if (point != null) StudioTokenKind.Point else StudioTokenKind.Constraint }
            add(StudioObjectToken(step.id, MathStudioView.Geometry2D, kind, step.dependencies, step.title, "2D ${kind.name.lowercase()} ${step.id}. ${step.reason}", size))
        }
        session.spatial.order.forEach { id ->
            val entity = session.spatial.entities.getValue(id); val kind = when (entity) { is SpatialConstruction3D.Point -> StudioTokenKind.Point; is SpatialConstruction3D.FreeVector, is SpatialConstruction3D.Vector -> StudioTokenKind.Vector; is SpatialConstruction3D.Line, is SpatialConstruction3D.DirectLine -> StudioTokenKind.Line; is SpatialConstruction3D.Plane, is SpatialConstruction3D.DirectPlane -> StudioTokenKind.Plane }
            add(StudioObjectToken(id, MathStudioView.Spatial3D, kind, entity.dependencies, entity.toString(), "3D ${kind.name.lowercase()} $id", size))
        }
        session.surfaces.forEach { add(StudioObjectToken(it.id, MathStudioView.Spatial3D, StudioTokenKind.Surface, emptyList(), it.toString(), "3D surface ${it.id}", size)) }
    }
}

data class CrossViewLink(val id: String, val members: Set<String>) { init { require(members.size >= 2) } }
data class CrossSelection(val requested: String, val selected: Set<String>, val views: Set<MathStudioView>, val announcement: String)
data class StudioDependencyInspection(val id: String, val direct: List<String>, val transitive: Set<String>, val dependents: Set<String>, val valid: Boolean, val explanation: String)

class CrossViewConstructionInspector(private val construction: UnifiedConstructionEngine = UnifiedConstructionEngine()) {
    fun link(session: UnifiedConstructionSession, id: String, members: Set<String>): UnifiedConstructionSession {
        val known = construction.tokens(session).map { it.id }.toSet(); require(members.all { it in known })
        return session.copy(links = session.links.filterNot { it.id == id } + CrossViewLink(id, members), revision = session.revision + 1)
    }

    fun select(session: UnifiedConstructionSession, id: String): CrossSelection {
        val tokens = construction.tokens(session); require(tokens.any { it.id == id })
        val selected = session.links.firstOrNull { id in it.members }?.members ?: setOf(id)
        val views = tokens.filter { it.id in selected }.map { it.view }.toSet()
        return CrossSelection(id, selected, views, "Selected ${selected.joinToString()} across ${views.joinToString { it.name }}")
    }

    fun inspect(session: UnifiedConstructionSession, id: String): StudioDependencyInspection {
        val tokens = construction.tokens(session); val byId = tokens.associateBy { it.id }; val token = byId[id] ?: error("Unknown token '$id'.")
        val transitive = linkedSetOf<String>(); var valid = true
        fun visit(current: String, stack: Set<String>) {
            if (current in stack) { valid = false; return }
            byId[current]?.dependencies.orEmpty().forEach { dependency -> if (dependency !in byId) valid = false else if (transitive.add(dependency)) visit(dependency, stack + current) }
        }
        visit(id, emptySet())
        val dependents = tokens.filter { candidate -> candidate.dependencies.any { it == id || it in transitive } }.map { it.id }.toSet()
        return StudioDependencyInspection(id, token.dependencies, transitive, dependents, valid,
            if (valid) "${token.accessibleLabel}; depends on ${transitive.joinToString().ifBlank { "no other objects" }}." else "The construction has a missing or circular dependency.")
    }

    fun keyboardOrder(session: UnifiedConstructionSession, view: MathStudioView? = null): List<StudioObjectToken> = construction.tokens(session).filter { view == null || it.view == view }.sortedWith(compareBy<StudioObjectToken> { it.view.ordinal }.thenBy { it.focusOrder })
}

enum class ProductionSnapKind { None, Grid, ExistingPoint, Axis, Angle }
data class SnapResult2D(val point: Vec2, val kind: ProductionSnapKind, val targetId: String? = null, val distance: Double, val announcement: String)
data class SnapResult3D(val point: Vec3, val kind: ProductionSnapKind, val distance: Double, val announcement: String)

object ProductionSnapEngine {
    fun snap2D(point: Vec2, existing: Map<String, Vec2>, grid: Double = 1.0, tolerance: Double = .18, angleOrigin: Vec2? = null, angleStepDegrees: Double = 15.0): SnapResult2D {
        require(grid > 0 && tolerance > 0 && angleStepDegrees > 0)
        val candidates = mutableListOf<SnapResult2D>()
        existing.forEach { (id, value) -> candidates += SnapResult2D(value, ProductionSnapKind.ExistingPoint, id, point.distanceTo(value), "Snapped to point $id") }
        val gridPoint = Vec2(round(point.x / grid) * grid, round(point.y / grid) * grid)
        candidates += SnapResult2D(gridPoint, ProductionSnapKind.Grid, distance = point.distanceTo(gridPoint), announcement = "Snapped to grid ${gridPoint.x}, ${gridPoint.y}")
        angleOrigin?.let { origin ->
            val delta = point - origin; val radius = delta.distanceTo(Vec2(0.0, 0.0)); val angle = atan2(delta.y, delta.x); val step = angleStepDegrees * PI / 180; val snapped = round(angle / step) * step
            val value = origin + Vec2(radius * cos(snapped), radius * sin(snapped)); candidates += SnapResult2D(value, ProductionSnapKind.Angle, distance = point.distanceTo(value), announcement = "Snapped to ${round(snapped * 180 / PI).toInt()} degree ray")
        }
        return candidates.minByOrNull { it.distance }?.takeIf { it.distance <= tolerance } ?: SnapResult2D(point, ProductionSnapKind.None, distance = 0.0, announcement = "Free position")
    }

    fun snap3D(point: Vec3, grid: Double = 1.0, tolerance: Double = .18): SnapResult3D {
        require(grid > 0 && tolerance > 0)
        val gridPoint = Vec3(round(point.x / grid) * grid, round(point.y / grid) * grid, round(point.z / grid) * grid)
        val gridDistance = (point - gridPoint).magnitude()
        val axes = listOf(Vec3(point.x, 0.0, 0.0), Vec3(0.0, point.y, 0.0), Vec3(0.0, 0.0, point.z))
        val axis = axes.minBy { (point - it).magnitude() }; val axisDistance = (point - axis).magnitude()
        return when { axisDistance <= minOf(tolerance, gridDistance) -> SnapResult3D(axis, ProductionSnapKind.Axis, axisDistance, "Snapped to a coordinate axis"); gridDistance <= tolerance -> SnapResult3D(gridPoint, ProductionSnapKind.Grid, gridDistance, "Snapped to 3D grid"); else -> SnapResult3D(point, ProductionSnapKind.None, 0.0, "Free 3D position") }
    }
}

data class ConstructionMacro(val name: String, val parameters: List<String>, val commands: List<String>) {
    init { require(name.isNotBlank() && parameters.distinct().size == parameters.size && commands.isNotEmpty()) }
}

object ConstructionMacroEngine {
    fun replay(session: UnifiedConstructionSession, macro: ConstructionMacro, arguments: Map<String, String>, prefix: String = ""): UnifiedConstructionSession {
        require(macro.parameters.all { it in arguments })
        val engine = UnifiedConstructionEngine()
        return macro.commands.fold(session) { state, template ->
            var command = template
            arguments.forEach { (name, value) -> command = command.replace("{{$name}}", value) }
            if (prefix.isNotBlank()) command = command.replace("{{prefix}}", prefix)
            engine.execute(state, command)
        }
    }
}
