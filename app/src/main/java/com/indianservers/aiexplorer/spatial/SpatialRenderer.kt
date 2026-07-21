package com.indianservers.aiexplorer.spatial

import android.opengl.GLES30
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.SurfaceMesh
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SpatialPrimitiveKind { Point, Curve, Surface, Solid, VectorField, ProbabilitySurface, Annotation }
enum class SpatialBlendMode { Opaque, Transparent, Additive }
data class SpatialMaterial(
    val name: String,
    val colorRgba: List<Float>,
    val metallic: Float = 0f,
    val roughness: Float = .55f,
    val emissive: Float = 0f,
    val blendMode: SpatialBlendMode = SpatialBlendMode.Opaque,
    val doubleSided: Boolean = true,
)
data class SpatialGeometry(val vertices: List<Vec3>, val triangles: List<Int> = emptyList(), val lines: List<Pair<Int, Int>> = emptyList(), val pointRadius: Double = .05)
data class SpatialPrimitive(
    val id: String,
    val kind: SpatialPrimitiveKind,
    val geometry: SpatialGeometry,
    val material: SpatialMaterial,
    val label: String = id,
    val selectable: Boolean = true,
    val visible: Boolean = true,
)
data class SpatialAnnotation(val id: String, val position: Vec3, val text: String, val colorRgba: List<Float> = listOf(1f, 1f, 1f, 1f))
data class SpatialMeasurementOverlay(val id: String, val from: Vec3, val to: Vec3, val value: Double, val unit: String, val uncertainty: Double, val educationalEstimate: Boolean = true) {
    val display: String get() = String.format(java.util.Locale.US, "%.3f ± %.3f %s · educational estimate", value, uncertainty, unit)
}
data class SpatialRenderScene(
    val id: String,
    val primitives: List<SpatialPrimitive>,
    val annotations: List<SpatialAnnotation> = emptyList(),
    val measurements: List<SpatialMeasurementOverlay> = emptyList(),
    val axesVisible: Boolean = true,
    val depthOcclusion: Boolean = false,
    val environmentIntensity: Float = 1f,
)
data class SpatialRay(val origin: Vec3, val direction: Vec3)
data class SpatialPickResult(val primitiveId: String, val kind: SpatialPrimitiveKind, val distance: Double, val worldPosition: Vec3, val label: String)

object SharedSpatialSceneBuilder {
    private val cyan = SpatialMaterial("cyan glass", listOf(.12f, .82f, 1f, .88f), metallic = .15f, roughness = .3f)
    private val violet = SpatialMaterial("violet", listOf(.58f, .34f, 1f, .84f), metallic = .1f, roughness = .4f)
    private val green = SpatialMaterial("vector", listOf(.2f, .9f, .58f, 1f), emissive = .1f)
    private val axisX = SpatialMaterial("x axis", listOf(1f, .25f, .3f, 1f), metallic = .1f, roughness = .35f)
    private val axisY = SpatialMaterial("y axis", listOf(.25f, 1f, .45f, 1f), metallic = .1f, roughness = .35f)
    private val axisZ = SpatialMaterial("z axis", listOf(.25f, .6f, 1f, 1f), metallic = .1f, roughness = .35f)

    fun build(
        id: String,
        solids: List<Solid> = emptyList(),
        surface: SurfaceMesh? = null,
        vectors: List<Vector3D> = emptyList(),
        annotations: List<SpatialAnnotation> = emptyList(),
        probabilitySurface: SurfaceMesh? = null,
    ): SpatialRenderScene {
        val primitives = mutableListOf(
            SpatialPrimitive("axis-x", SpatialPrimitiveKind.Curve, SpatialGeometry(listOf(Vec3(-3.0, 0.0, 0.0), Vec3(3.0, 0.0, 0.0)), lines = listOf(0 to 1), pointRadius = .025), axisX, "x axis", selectable = false),
            SpatialPrimitive("axis-y", SpatialPrimitiveKind.Curve, SpatialGeometry(listOf(Vec3(0.0, -3.0, 0.0), Vec3(0.0, 3.0, 0.0)), lines = listOf(0 to 1), pointRadius = .025), axisY, "y axis", selectable = false),
            SpatialPrimitive("axis-z", SpatialPrimitiveKind.Curve, SpatialGeometry(listOf(Vec3(0.0, 0.0, -3.0), Vec3(0.0, 0.0, 3.0)), lines = listOf(0 to 1), pointRadius = .025), axisZ, "z axis", selectable = false),
        )
        solids.forEachIndexed { index, solid ->
            val mesh = SolidMeshFactory.create(solid)
            primitives += SpatialPrimitive(
                "solid-$index", SpatialPrimitiveKind.Solid,
                SpatialGeometry(mesh.vertices.map { it + solid.position }, mesh.faces.flatMap { face -> triangulate(face) }, mesh.edges.map { it.first to it.second }),
                if (index % 2 == 0) cyan else violet, solid.type.name,
            )
        }
        surface?.let { mesh ->
            primitives += SpatialPrimitive("surface", SpatialPrimitiveKind.Surface, surfaceGeometry(mesh), cyan.copy(name = "surface", blendMode = SpatialBlendMode.Transparent), "Mathematical surface")
        }
        probabilitySurface?.let { mesh ->
            primitives += SpatialPrimitive("probability", SpatialPrimitiveKind.ProbabilitySurface, surfaceGeometry(mesh), violet.copy(name = "probability", blendMode = SpatialBlendMode.Transparent), "Probability surface")
        }
        vectors.forEachIndexed { index, vector ->
            primitives += SpatialPrimitive("vector-$index", SpatialPrimitiveKind.Curve, SpatialGeometry(listOf(vector.start, vector.end), lines = listOf(0 to 1), pointRadius = .08), green, vector.name)
        }
        return SpatialRenderScene(id, primitives, annotations)
    }

    fun vectorField(id: String, origins: List<Vec3>, vectors: List<Vec3>): SpatialRenderScene {
        require(origins.size == vectors.size)
        val primitives = origins.indices.map { index ->
            SpatialPrimitive("field-$index", SpatialPrimitiveKind.VectorField, SpatialGeometry(listOf(origins[index], origins[index] + vectors[index]), lines = listOf(0 to 1), pointRadius = .04), green, "Field vector $index")
        }
        return SpatialRenderScene(id, primitives)
    }

    private fun surfaceGeometry(mesh: SurfaceMesh): SpatialGeometry {
        val triangles = mutableListOf<Int>()
        for (row in 0 until mesh.rows - 1) for (column in 0 until mesh.columns - 1) {
            val a = row * mesh.columns + column; val b = a + 1; val c = a + mesh.columns; val d = c + 1
            triangles += listOf(a, c, b, b, c, d)
        }
        return SpatialGeometry(mesh.vertices, triangles)
    }
    private fun triangulate(face: List<Int>): List<Int> = if (face.size < 3) emptyList() else (1 until face.lastIndex).flatMap { listOf(face[0], face[it], face[it + 1]) }
}

object SpatialPicking {
    fun hitTest(scene: SpatialRenderScene, ray: SpatialRay, maxDistance: Double = 100.0): List<SpatialPickResult> {
        val direction = normalize(ray.direction)
        val primitiveHits = scene.primitives.filter { it.visible && it.selectable && it.geometry.vertices.isNotEmpty() }.mapNotNull { primitive ->
            val distance = preciseDistance(ray.origin, direction, primitive.geometry) ?: return@mapNotNull null
            if (distance !in 0.0..maxDistance) return@mapNotNull null
            SpatialPickResult(primitive.id, primitive.kind, distance, ray.origin + direction * distance, primitive.label)
        }
        val annotationHits = scene.annotations.mapNotNull { annotation ->
            val padding = .08
            val minimum = annotation.position - Vec3(padding, padding, padding); val maximum = annotation.position + Vec3(padding, padding, padding)
            val distance = rayBox(ray.origin, direction, minimum, maximum) ?: return@mapNotNull null
            SpatialPickResult(annotation.id, SpatialPrimitiveKind.Annotation, distance, ray.origin + direction * distance, annotation.text)
        }
        return (primitiveHits + annotationHits).sortedBy { it.distance }
    }

    private fun preciseDistance(origin: Vec3, direction: Vec3, geometry: SpatialGeometry): Double? {
        val triangleHit = geometry.triangles.chunked(3).asSequence().filter { it.size == 3 }.mapNotNull { triangle ->
            rayTriangle(origin, direction, geometry.vertices[triangle[0]], geometry.vertices[triangle[1]], geometry.vertices[triangle[2]])
        }.minOrNull()
        if (triangleHit != null) return triangleHit
        val lineHit = geometry.lines.asSequence().mapNotNull { (a, b) -> raySegment(origin, direction, geometry.vertices[a], geometry.vertices[b], geometry.pointRadius) }.minOrNull()
        if (lineHit != null) return lineHit
        val pointHit = geometry.vertices.asSequence().mapNotNull { raySphere(origin, direction, it, geometry.pointRadius) }.minOrNull()
        if (pointHit != null) return pointHit
        return null
    }

    private fun rayTriangle(origin: Vec3, direction: Vec3, a: Vec3, b: Vec3, c: Vec3): Double? {
        val edge1 = b - a; val edge2 = c - a; val h = cross(direction, edge2); val determinant = edge1.dot(h)
        if (abs(determinant) < 1e-12) return null
        val inverse = 1.0 / determinant; val s = origin - a; val u = inverse * s.dot(h); if (u !in 0.0..1.0) return null
        val q = cross(s, edge1); val v = inverse * direction.dot(q); if (v < 0.0 || u + v > 1.0) return null
        return (inverse * edge2.dot(q)).takeIf { it >= 0.0 }
    }

    private fun raySegment(origin: Vec3, direction: Vec3, a: Vec3, b: Vec3, radius: Double): Double? {
        val segment = b - a; val w = origin - a; val aa = direction.dot(direction); val bb = direction.dot(segment); val cc = segment.dot(segment); val dd = direction.dot(w); val ee = segment.dot(w)
        if (cc < 1e-15) return raySphere(origin, direction, a, radius)
        val denominator = aa * cc - bb * bb; var segmentT = if (abs(denominator) < 1e-15) 0.0 else ((aa * ee - bb * dd) / denominator).coerceIn(0.0, 1.0)
        var rayT = max(0.0, (bb * segmentT - dd) / aa); segmentT = ((bb * rayT + ee) / cc).coerceIn(0.0, 1.0); rayT = max(0.0, (bb * segmentT - dd) / aa)
        return rayT.takeIf { distance(origin + direction * rayT, a + segment * segmentT) <= radius }
    }

    private fun raySphere(origin: Vec3, direction: Vec3, center: Vec3, radius: Double): Double? {
        val offset = origin - center; val b = offset.dot(direction); val c = offset.dot(offset) - radius * radius; val discriminant = b * b - c
        if (discriminant < 0) return null; val root = sqrt(discriminant); return listOf(-b - root, -b + root).firstOrNull { it >= 0 }
    }

    private fun cross(a: Vec3, b: Vec3) = Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    fun nearestPoint(scene: SpatialRenderScene, world: Vec3, tolerance: Double): SpatialPickResult? =
        scene.primitives.asSequence().filter { it.visible && it.selectable }.flatMap { primitive -> primitive.geometry.vertices.asSequence().map { primitive to it } }
            .map { (primitive, point) -> Triple(primitive, point, distance(point, world)) }.filter { it.third <= tolerance }.minByOrNull { it.third }
            ?.let { SpatialPickResult(it.first.id, it.first.kind, it.third, it.second, it.first.label) }
            ?: scene.annotations.minByOrNull { distance(it.position, world) }?.takeIf { distance(it.position, world) <= tolerance }
                ?.let { SpatialPickResult(it.id, SpatialPrimitiveKind.Annotation, distance(it.position, world), it.position, it.text) }

    private fun rayBox(origin: Vec3, direction: Vec3, minimum: Vec3, maximum: Vec3): Double? {
        var near = Double.NEGATIVE_INFINITY; var far = Double.POSITIVE_INFINITY
        listOf(Triple(origin.x, direction.x, minimum.x to maximum.x), Triple(origin.y, direction.y, minimum.y to maximum.y), Triple(origin.z, direction.z, minimum.z to maximum.z)).forEach { (o, d, range) ->
            if (abs(d) < 1e-12) { if (o !in range.first..range.second) return null }
            else {
                val first = (range.first - o) / d; val second = (range.second - o) / d
                near = max(near, min(first, second)); far = min(far, max(first, second)); if (near > far) return null
            }
        }
        return if (far < 0) null else max(0.0, near)
    }
    private fun bounds(vertices: List<Vec3>, padding: Double = 0.0) =
        Vec3(vertices.minOf { it.x } - padding, vertices.minOf { it.y } - padding, vertices.minOf { it.z } - padding) to
            Vec3(vertices.maxOf { it.x } + padding, vertices.maxOf { it.y } + padding, vertices.maxOf { it.z } + padding)
    private fun normalize(value: Vec3): Vec3 { val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z); require(length > 0); return value * (1 / length) }
    private fun distance(a: Vec3, b: Vec3) = sqrt((a.x - b.x).pow2() + (a.y - b.y).pow2() + (a.z - b.z).pow2())
    private fun Double.pow2() = this * this
}

data class GpuDrawCall(val primitiveId: String, val firstIndex: Int, val indexCount: Int, val lineFirst: Int, val lineCount: Int, val material: SpatialMaterial)
data class GpuRenderPlan(val vertices: FloatArray, val triangleIndices: IntArray, val lineIndices: IntArray, val calls: List<GpuDrawCall>, val sceneId: String)

object SharedGpuSceneCompiler {
    fun compile(scene: SpatialRenderScene): GpuRenderPlan {
        val vertices = mutableListOf<Float>(); val triangles = mutableListOf<Int>(); val lines = mutableListOf<Int>(); val calls = mutableListOf<GpuDrawCall>()
        scene.primitives.filter { it.visible }.forEach { primitive ->
            val vertexBase = vertices.size / 10
            primitive.geometry.vertices.forEach { point ->
                vertices += listOf(
                    point.x.toFloat(), point.y.toFloat(), point.z.toFloat(),
                    primitive.material.colorRgba[0], primitive.material.colorRgba[1], primitive.material.colorRgba[2], primitive.material.colorRgba[3],
                    primitive.material.metallic, primitive.material.roughness, primitive.material.emissive,
                )
            }
            val triangleFirst = triangles.size; triangles += primitive.geometry.triangles.map { it + vertexBase }
            val lineFirst = lines.size; primitive.geometry.lines.forEach { lines += it.first + vertexBase; lines += it.second + vertexBase }
            calls += GpuDrawCall(primitive.id, triangleFirst, triangles.size - triangleFirst, lineFirst, lines.size - lineFirst, primitive.material)
        }
        return GpuRenderPlan(vertices.toFloatArray(), triangles.toIntArray(), lines.toIntArray(), calls, scene.id)
    }
}

/** OpenGL ES 3 backend shared by normal 3D and the AR camera compositor. */
class OpenGlEsSpatialRenderer {
    private var program = 0; private var vertexBuffer = 0; private var triangleBuffer = 0; private var lineBuffer = 0
    private var plan: GpuRenderPlan? = null

    fun initialize() {
        val vertex = compileShader(GLES30.GL_VERTEX_SHADER, "#version 300 es\nuniform mat4 uMvp; layout(location=0) in vec3 aPosition; layout(location=1) in vec4 aColor; layout(location=2) in vec3 aMaterial; out vec4 vColor; out vec3 vPosition; out vec3 vMaterial; void main(){vColor=aColor;vPosition=aPosition;vMaterial=aMaterial;gl_Position=uMvp*vec4(aPosition,1.0);}")
        val fragment = compileShader(GLES30.GL_FRAGMENT_SHADER, "#version 300 es\nprecision highp float; uniform float uEnvironment; in vec4 vColor; in vec3 vPosition; in vec3 vMaterial; out vec4 color; void main(){vec3 dx=dFdx(vPosition);vec3 dy=dFdy(vPosition);vec3 n=normalize(cross(dx,dy));if(!gl_FrontFacing)n=-n;vec3 l=normalize(vec3(.35,.8,.45));float diffuse=.22+.78*abs(dot(n,l));float rough=clamp(vMaterial.y,.05,1.);float metallic=clamp(vMaterial.x,0.,1.);float spec=pow(max(dot(reflect(-l,n),normalize(vec3(.1,.25,1.))),0.),mix(64.,4.,rough))*mix(.18,.75,metallic);vec3 rgb=vColor.rgb*(diffuse*max(uEnvironment,.25))+spec+vColor.rgb*vMaterial.z;color=vec4(rgb,vColor.a);}")
        program = GLES30.glCreateProgram(); GLES30.glAttachShader(program, vertex); GLES30.glAttachShader(program, fragment); GLES30.glLinkProgram(program)
        val status = IntArray(1); GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0); require(status[0] == GLES30.GL_TRUE) { GLES30.glGetProgramInfoLog(program) }
        val buffers = IntArray(3); GLES30.glGenBuffers(3, buffers, 0); vertexBuffer = buffers[0]; triangleBuffer = buffers[1]; lineBuffer = buffers[2]
    }

    fun upload(value: GpuRenderPlan) {
        plan = value
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffer)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, value.vertices.size * 4, floatBuffer(value.vertices), GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, triangleBuffer)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, value.triangleIndices.size * 4, intBuffer(value.triangleIndices), GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, lineBuffer)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, value.lineIndices.size * 4, intBuffer(value.lineIndices), GLES30.GL_DYNAMIC_DRAW)
    }

    fun render(viewProjection: FloatArray, clear: Boolean = true, environmentIntensity: Float = 1f) {
        require(viewProjection.size == 16 && program != 0)
        if (clear) { GLES30.glClearColor(.015f, .025f, .04f, 1f); GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT) }
        GLES30.glEnable(GLES30.GL_DEPTH_TEST); GLES30.glEnable(GLES30.GL_BLEND); GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glUseProgram(program); GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "uMvp"), 1, false, viewProjection, 0)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "uEnvironment"), environmentIntensity.coerceIn(.25f, 2.5f))
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffer); GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 40, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 40, 12)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, 40, 28)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, triangleBuffer)
        plan?.calls?.forEach { call -> if (call.indexCount > 0) GLES30.glDrawElements(GLES30.GL_TRIANGLES, call.indexCount, GLES30.GL_UNSIGNED_INT, call.firstIndex * 4) }
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, lineBuffer)
        GLES30.glLineWidth(2f)
        plan?.calls?.forEach { call -> if (call.lineCount > 0) GLES30.glDrawElements(GLES30.GL_LINES, call.lineCount, GLES30.GL_UNSIGNED_INT, call.lineFirst * 4) }
    }

    fun release() {
        if (program != 0) GLES30.glDeleteProgram(program)
        if (vertexBuffer != 0) GLES30.glDeleteBuffers(3, intArrayOf(vertexBuffer, triangleBuffer, lineBuffer), 0)
        program = 0; vertexBuffer = 0; triangleBuffer = 0; lineBuffer = 0; plan = null
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type); GLES30.glShaderSource(shader, source); GLES30.glCompileShader(shader)
        val status = IntArray(1); GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0); require(status[0] == GLES30.GL_TRUE) { GLES30.glGetShaderInfoLog(shader) }
        return shader
    }
    private fun floatBuffer(values: FloatArray) = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values); position(0) }
    private fun intBuffer(values: IntArray) = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().apply { put(values); position(0) }
}
