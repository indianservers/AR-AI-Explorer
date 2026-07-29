package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.Shape2DType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class ShapeExplorer2DPreset(
    val id: String,
    val label: String,
    val type: Shape2DType,
    val points: List<Vec2>,
    val formula: String,
)

private fun regularShapePoints(sides: Int, radius: Double = 2.4): List<Vec2> = (0 until sides).map { index ->
    val angle = -PI / 2 + index * 2 * PI / sides
    Vec2(cos(angle) * radius, sin(angle) * radius)
}

internal val ShapeExplorer2DShapes = listOf(
    ShapeExplorer2DPreset("triangle", "Triangle", Shape2DType.Triangle, listOf(Vec2(-2.4, -1.6), Vec2(2.4, -1.6), Vec2(0.0, 2.2)), "A = 1/2 x b x h; P = a + b + c"),
    ShapeExplorer2DPreset("right-triangle", "Right Triangle", Shape2DType.Triangle, listOf(Vec2(-2.2, -1.7), Vec2(2.2, -1.7), Vec2(-2.2, 2.0)), "A = 1/2 x b x h; c^2 = a^2 + b^2"),
    ShapeExplorer2DPreset("equilateral", "Equilateral Triangle", Shape2DType.Triangle, regularShapePoints(3), "A = sqrt(3)a^2/4; P = 3a"),
    ShapeExplorer2DPreset("isosceles", "Isosceles Triangle", Shape2DType.Triangle, listOf(Vec2(-2.3, -1.7), Vec2(2.3, -1.7), Vec2(0.0, 2.4)), "A = bh/2; P = 2a+b"),
    ShapeExplorer2DPreset("scalene", "Scalene Triangle", Shape2DType.Triangle, listOf(Vec2(-2.6, -1.6), Vec2(2.1, -1.9), Vec2(.7, 2.3)), "A = sqrt(s(s-a)(s-b)(s-c))"),
    ShapeExplorer2DPreset("obtuse", "Obtuse Triangle", Shape2DType.Triangle, listOf(Vec2(-2.7, -1.4), Vec2(2.5, -1.4), Vec2(-1.4, 1.8)), "A = ab sin(C)/2"),
    ShapeExplorer2DPreset("square", "Square", Shape2DType.Square, listOf(Vec2(-2.0, -2.0), Vec2(2.0, 2.0)), "A = a^2; P = 4a; d = a sqrt(2)"),
    ShapeExplorer2DPreset("rectangle", "Rectangle", Shape2DType.Rectangle, listOf(Vec2(-2.6, -1.6), Vec2(2.6, 1.6)), "A = l x w; P = 2(l + w)"),
    ShapeExplorer2DPreset("parallelogram", "Parallelogram", Shape2DType.Polygon, listOf(Vec2(-2.6, -1.5), Vec2(1.5, -1.5), Vec2(2.6, 1.5), Vec2(-1.5, 1.5)), "A = b x h; P = 2(a + b)"),
    ShapeExplorer2DPreset("rhombus", "Rhombus", Shape2DType.Polygon, listOf(Vec2(0.0, -2.5), Vec2(2.2, 0.0), Vec2(0.0, 2.5), Vec2(-2.2, 0.0)), "A = d1 x d2 / 2; P = 4a"),
    ShapeExplorer2DPreset("trapezoid", "Trapezoid", Shape2DType.Polygon, listOf(Vec2(-2.8, -1.6), Vec2(2.8, -1.6), Vec2(1.7, 1.6), Vec2(-1.7, 1.6)), "A = (a + b)h / 2; P = a + b + c + d"),
    ShapeExplorer2DPreset("isosceles-trapezoid", "Isosceles Trapezoid", Shape2DType.Polygon, listOf(Vec2(-2.8, -1.6), Vec2(2.8, -1.6), Vec2(1.5, 1.6), Vec2(-1.5, 1.6)), "A = (a+b)h/2; equal legs"),
    ShapeExplorer2DPreset("kite", "Kite", Shape2DType.Polygon, listOf(Vec2(0.0, -2.7), Vec2(1.8, -0.2), Vec2(0.0, 2.4), Vec2(-1.8, -0.2)), "A = d1 x d2 / 2; P = 2(a + b)"),
    ShapeExplorer2DPreset("pentagon", "Pentagon", Shape2DType.Polygon, regularShapePoints(5), "A = 1/2 x apothem x perimeter"),
    ShapeExplorer2DPreset("hexagon", "Hexagon", Shape2DType.Polygon, regularShapePoints(6), "A = 3 sqrt(3)a^2/2; P = 6a"),
    ShapeExplorer2DPreset("heptagon", "Heptagon", Shape2DType.Polygon, regularShapePoints(7), "A = 7a^2 cot(pi/7)/4; P = 7a"),
    ShapeExplorer2DPreset("octagon", "Octagon", Shape2DType.Polygon, regularShapePoints(8), "A = 2(1 + sqrt(2))a^2; P = 8a"),
    ShapeExplorer2DPreset("decagon", "Decagon", Shape2DType.Polygon, regularShapePoints(10), "A = 5a^2 sqrt(5 + 2sqrt(5))/2; P = 10a"),
    ShapeExplorer2DPreset("nonagon", "Nonagon", Shape2DType.Polygon, regularShapePoints(9), "A = 9a^2 cot(pi/9)/4; P = 9a"),
    ShapeExplorer2DPreset("dodecagon", "Dodecagon", Shape2DType.Polygon, regularShapePoints(12), "A = 3(2+sqrt(3))a^2; P = 12a"),
    ShapeExplorer2DPreset("star", "Five-point Star", Shape2DType.Polygon, (0 until 10).map { i -> val r = if (i % 2 == 0) 2.5 else 1.05; val angle = -PI / 2 + i * PI / 5; Vec2(cos(angle) * r, sin(angle) * r) }, "Ten-vertex concave polygon"),
    ShapeExplorer2DPreset("arrow", "Arrow", Shape2DType.Polygon, listOf(Vec2(-2.7,-.7),Vec2(.4,-.7),Vec2(.4,-1.6),Vec2(2.8,0.0),Vec2(.4,1.6),Vec2(.4,.7),Vec2(-2.7,.7)), "Area by polygon decomposition"),
    ShapeExplorer2DPreset("circle", "Circle", Shape2DType.Circle, listOf(Vec2(0.0, 0.0), Vec2(2.4, 0.0)), "A = pi r^2; C = 2 pi r"),
    ShapeExplorer2DPreset("ellipse", "Ellipse", Shape2DType.Ellipse, listOf(Vec2(0.0, 0.0), Vec2(2.8, 0.0), Vec2(0.0, 1.7)), "A = pi ab; P is approximately pi[3(a+b)-sqrt((3a+b)(a+3b))]"),
)

internal data class ShapeFormulaItem(val name: String, val expression: String)

internal fun ShapeExplorer2DPreset.category(): String = when {
    type == Shape2DType.Circle || type == Shape2DType.Ellipse -> "Curves"
    label.contains("Triangle") -> "Triangles"
    label in setOf("Square", "Rectangle", "Parallelogram", "Rhombus", "Trapezoid", "Kite") -> "Quadrilaterals"
    else -> "Polygons"
}

internal fun shape2DFormulaLibrary(label: String): List<ShapeFormulaItem> = when (label.substringBefore(" Copy")) {
    "Triangle" -> listOf(ShapeFormulaItem("Area: base-height", "A = bh/2"), ShapeFormulaItem("Area: Heron", "A = sqrt(s(s-a)(s-b)(s-c))"), ShapeFormulaItem("Semiperimeter", "s = (a+b+c)/2"), ShapeFormulaItem("Perimeter", "P = a+b+c"), ShapeFormulaItem("Inradius", "r = A/s"), ShapeFormulaItem("Circumradius", "R = abc/(4A)"))
    "Right Triangle" -> listOf(ShapeFormulaItem("Area", "A = ab/2"), ShapeFormulaItem("Pythagoras", "c^2 = a^2+b^2"), ShapeFormulaItem("Perimeter", "P = a+b+c"), ShapeFormulaItem("Inradius", "r = (a+b-c)/2"), ShapeFormulaItem("Circumradius", "R = c/2"))
    "Equilateral Triangle" -> listOf(ShapeFormulaItem("Area", "A = sqrt(3)a^2/4"), ShapeFormulaItem("Perimeter", "P = 3a"), ShapeFormulaItem("Height", "h = sqrt(3)a/2"), ShapeFormulaItem("Inradius", "r = sqrt(3)a/6"), ShapeFormulaItem("Circumradius", "R = sqrt(3)a/3"))
    "Square" -> listOf(ShapeFormulaItem("Area", "A = a^2"), ShapeFormulaItem("Perimeter", "P = 4a"), ShapeFormulaItem("Diagonal", "d = a sqrt(2)"), ShapeFormulaItem("Inradius", "r = a/2"), ShapeFormulaItem("Circumradius", "R = a/sqrt(2)"))
    "Rectangle" -> listOf(ShapeFormulaItem("Area", "A = lw"), ShapeFormulaItem("Perimeter", "P = 2(l+w)"), ShapeFormulaItem("Diagonal", "d = sqrt(l^2+w^2)"), ShapeFormulaItem("Circumradius", "R = d/2"))
    "Parallelogram" -> listOf(ShapeFormulaItem("Area", "A = bh"), ShapeFormulaItem("Vector area", "A = |a x b|"), ShapeFormulaItem("Perimeter", "P = 2(a+b)"), ShapeFormulaItem("Diagonals", "p^2+q^2 = 2(a^2+b^2)"))
    "Rhombus" -> listOf(ShapeFormulaItem("Area: diagonals", "A = d1d2/2"), ShapeFormulaItem("Area: base-height", "A = ah"), ShapeFormulaItem("Perimeter", "P = 4a"), ShapeFormulaItem("Diagonal identity", "d1^2+d2^2 = 4a^2"), ShapeFormulaItem("Inradius", "r = A/(2a)"))
    "Trapezoid" -> listOf(ShapeFormulaItem("Area", "A = (a+b)h/2"), ShapeFormulaItem("Midsegment", "m = (a+b)/2"), ShapeFormulaItem("Perimeter", "P = a+b+c+d"))
    "Kite" -> listOf(ShapeFormulaItem("Area", "A = d1d2/2"), ShapeFormulaItem("Perimeter", "P = 2(a+b)"), ShapeFormulaItem("Diagonal relation", "d1 is perpendicular to d2"))
    "Circle" -> listOf(ShapeFormulaItem("Area", "A = pi r^2"), ShapeFormulaItem("Circumference", "C = 2 pi r = pi d"), ShapeFormulaItem("Diameter", "d = 2r"), ShapeFormulaItem("Arc length", "L = r theta"), ShapeFormulaItem("Sector area", "Asector = r^2 theta/2"), ShapeFormulaItem("Chord", "c = 2r sin(theta/2)"))
    "Ellipse" -> listOf(ShapeFormulaItem("Area", "A = pi ab"), ShapeFormulaItem("Perimeter approximation", "P ~ pi[3(a+b)-sqrt((3a+b)(a+3b))]"), ShapeFormulaItem("Focal distance", "c = sqrt(a^2-b^2)"), ShapeFormulaItem("Eccentricity", "e = c/a"), ShapeFormulaItem("Foci property", "PF1+PF2 = 2a"))
    else -> listOf(ShapeFormulaItem("Area", "A = ns^2/[4 tan(pi/n)]"), ShapeFormulaItem("Perimeter", "P = ns"), ShapeFormulaItem("Interior angle", "alpha = (n-2)180/n"), ShapeFormulaItem("Angle sum", "S = (n-2)180"), ShapeFormulaItem("Apothem", "a = s/[2 tan(pi/n)]"), ShapeFormulaItem("Circumradius", "R = s/[2 sin(pi/n)]"), ShapeFormulaItem("Diagonals", "D = n(n-3)/2"))
}

internal fun solidFormulaLibrary(type: SolidType): List<ShapeFormulaItem> = when (type) {
    SolidType.Cube -> listOf(ShapeFormulaItem("Volume", "V = a^3"), ShapeFormulaItem("Surface area", "S = 6a^2"), ShapeFormulaItem("Lateral area", "L = 4a^2"), ShapeFormulaItem("Face diagonal", "d = a sqrt(2)"), ShapeFormulaItem("Space diagonal", "D = a sqrt(3)"))
    SolidType.Cuboid -> listOf(ShapeFormulaItem("Volume", "V = lwh"), ShapeFormulaItem("Surface area", "S = 2(lw+lh+wh)"), ShapeFormulaItem("Lateral area", "L = 2h(l+w)"), ShapeFormulaItem("Space diagonal", "D = sqrt(l^2+w^2+h^2)"))
    SolidType.Sphere -> listOf(ShapeFormulaItem("Volume", "V = 4 pi r^3/3"), ShapeFormulaItem("Surface area", "S = 4 pi r^2"), ShapeFormulaItem("Great-circle area", "A = pi r^2"), ShapeFormulaItem("Circumference", "C = 2 pi r"))
    SolidType.Hemisphere -> listOf(ShapeFormulaItem("Volume", "V = 2 pi r^3/3"), ShapeFormulaItem("Curved area", "C = 2 pi r^2"), ShapeFormulaItem("Total area", "S = 3 pi r^2"), ShapeFormulaItem("Base area", "B = pi r^2"))
    SolidType.Cylinder -> listOf(ShapeFormulaItem("Volume", "V = pi r^2h"), ShapeFormulaItem("Curved area", "C = 2 pi rh"), ShapeFormulaItem("Total area", "S = 2 pi r(r+h)"), ShapeFormulaItem("Base area", "B = pi r^2"), ShapeFormulaItem("Axial diagonal", "d = sqrt(h^2+4r^2)"))
    SolidType.Cone -> listOf(ShapeFormulaItem("Volume", "V = pi r^2h/3"), ShapeFormulaItem("Slant height", "s = sqrt(r^2+h^2)"), ShapeFormulaItem("Curved area", "C = pi rs"), ShapeFormulaItem("Total area", "S = pi r(r+s)"), ShapeFormulaItem("Base area", "B = pi r^2"))
    SolidType.Frustum -> listOf(ShapeFormulaItem("Volume", "V = pi h(R^2+Rr+r^2)/3"), ShapeFormulaItem("Slant height", "s = sqrt(h^2+(R-r)^2)"), ShapeFormulaItem("Curved area", "C = pi(R+r)s"), ShapeFormulaItem("Total area", "S = C+pi(R^2+r^2)"))
    SolidType.Pyramid -> listOf(ShapeFormulaItem("Volume", "V = Bh/3"), ShapeFormulaItem("Lateral area", "L = ps/2"), ShapeFormulaItem("Surface area", "S = B+L"), ShapeFormulaItem("Square slant height", "s = sqrt(h^2+(a/2)^2)"))
    SolidType.TriangularPrism -> listOf(ShapeFormulaItem("Volume", "V = Bh"), ShapeFormulaItem("Lateral area", "L = ph"), ShapeFormulaItem("Surface area", "S = 2B+ph"))
    SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.OctagonalPrism -> listOf(ShapeFormulaItem("Volume", "V = Bh"), ShapeFormulaItem("Lateral area", "L = ph"), ShapeFormulaItem("Surface area", "S = 2B+ph"), ShapeFormulaItem("Regular base", "B = ns^2/[4 tan(pi/n)]"))
    SolidType.Tetrahedron -> listOf(ShapeFormulaItem("Volume", "V = a^3/(6 sqrt(2))"), ShapeFormulaItem("Surface area", "S = sqrt(3)a^2"), ShapeFormulaItem("Height", "h = a sqrt(2/3)"), ShapeFormulaItem("Inradius", "r = a sqrt(6)/12"))
    SolidType.TriangularPyramid -> listOf(ShapeFormulaItem("Volume", "V = Bh/3"), ShapeFormulaItem("Base", "B = bh/2"), ShapeFormulaItem("Faces", "F = 4"))
    SolidType.Octahedron -> listOf(ShapeFormulaItem("Volume", "V = sqrt(2)a^3/3"), ShapeFormulaItem("Surface area", "S = 2 sqrt(3)a^2"), ShapeFormulaItem("Inradius", "r = a sqrt(6)/6"), ShapeFormulaItem("Circumradius", "R = a sqrt(2)/2"))
    SolidType.Wedge -> listOf(ShapeFormulaItem("Volume", "V = bhl/2"), ShapeFormulaItem("Triangle base", "B = bh/2"), ShapeFormulaItem("Surface area", "S = bh+l(b+h+s)"))
    SolidType.Torus -> listOf(ShapeFormulaItem("Volume", "V = 2 pi^2Rr^2"), ShapeFormulaItem("Surface area", "S = 4 pi^2Rr"), ShapeFormulaItem("Outer diameter", "Dout = 2(R+r)"), ShapeFormulaItem("Inner diameter", "Din = 2(R-r)"))
    SolidType.Ellipsoid -> listOf(ShapeFormulaItem("Volume", "V = 4 pi abc/3"), ShapeFormulaItem("Surface approximation", "S ~ 4 pi[(a^p b^p+a^p c^p+b^p c^p)/3]^(1/p)"), ShapeFormulaItem("Sphere case", "a=b=c=r"))
    SolidType.Paraboloid -> listOf(ShapeFormulaItem("Volume", "V = pi r^2h/2"), ShapeFormulaItem("Base area", "B = pi r^2"), ShapeFormulaItem("Curved area", "C = pi r[(r^2+4h^2)^(3/2)-r^3]/(6h^2)"))
    SolidType.Capsule -> listOf(ShapeFormulaItem("Volume", "V = pi r^2l+4 pi r^3/3"), ShapeFormulaItem("Surface area", "S = 2 pi rl+4 pi r^2"), ShapeFormulaItem("Total height", "h = l+2r"))
}

internal fun GeometryTool.requiredTapCount(): Int = when (this) {
    GeometryTool.Point -> 1
    GeometryTool.Midpoint -> 2
    GeometryTool.Line, GeometryTool.Segment, GeometryTool.Ray, GeometryTool.Vector,
    GeometryTool.Rectangle, GeometryTool.Square, GeometryTool.Circle, GeometryTool.RegularPolygon -> 2
    GeometryTool.PointOnObject, GeometryTool.Tangent, GeometryTool.Centroid, GeometryTool.Circumcenter, GeometryTool.Incenter, GeometryTool.Orthocenter,
    GeometryTool.Parallel, GeometryTool.Perpendicular, GeometryTool.AngleBisector,
    GeometryTool.Triangle, GeometryTool.CircleThreePoints, GeometryTool.Arc, GeometryTool.Ellipse -> 3
    GeometryTool.Polygon -> 4
    GeometryTool.Intersection -> 4
    GeometryTool.Select, GeometryTool.Measure -> 0
}

internal fun GeometryTool.toShape2DType(): Shape2DType? = when (this) {
    GeometryTool.Line -> Shape2DType.Line
    GeometryTool.Segment -> Shape2DType.Segment
    GeometryTool.Ray -> Shape2DType.Ray
    GeometryTool.Vector -> Shape2DType.Vector
    GeometryTool.Parallel -> Shape2DType.Parallel
    GeometryTool.Perpendicular -> Shape2DType.Perpendicular
    GeometryTool.AngleBisector -> Shape2DType.AngleBisector
    GeometryTool.Triangle -> Shape2DType.Triangle
    GeometryTool.Polygon -> Shape2DType.Polygon
    GeometryTool.RegularPolygon -> Shape2DType.RegularPolygon
    GeometryTool.Rectangle -> Shape2DType.Rectangle
    GeometryTool.Square -> Shape2DType.Square
    GeometryTool.Circle -> Shape2DType.Circle
    GeometryTool.CircleThreePoints -> Shape2DType.CircleThreePoints
    GeometryTool.Arc -> Shape2DType.Arc
    GeometryTool.Ellipse -> Shape2DType.Ellipse
    else -> null
}

internal fun GeometryTool.toPointDependencyType(): PointDependencyType? = when (this) {
    GeometryTool.Midpoint -> PointDependencyType.Midpoint
    GeometryTool.Centroid -> PointDependencyType.Centroid
    GeometryTool.Circumcenter -> PointDependencyType.Circumcenter
    GeometryTool.Incenter -> PointDependencyType.Incenter
    GeometryTool.Orthocenter -> PointDependencyType.Orthocenter
    GeometryTool.Intersection -> PointDependencyType.Intersection
    GeometryTool.PointOnObject -> PointDependencyType.PointOnObject
    GeometryTool.Tangent -> PointDependencyType.TangentPoint
    else -> null
}

internal fun defaultSolid(type: SolidType): Solid = when (type) {
    SolidType.Cube -> Solid(type, width = 2.0)
    SolidType.Cuboid -> Solid(type, width = 2.4, height = 1.6, depth = 1.4, radius = .8)
    SolidType.Sphere -> Solid(type, width = 2.0, height = 2.0, depth = 2.0, radius = 1.0)
    SolidType.Hemisphere -> Solid(type, width = 2.0, height = 1.0, depth = 2.0, radius = 1.0)
    SolidType.Cylinder -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = .9)
    SolidType.Cone -> Solid(type, width = 2.0, height = 2.5, depth = 2.0, radius = .9)
    SolidType.Frustum -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = 1.0, topRadius = .55)
    SolidType.Pyramid -> Solid(type, width = 2.2, height = 2.4, depth = 2.2, radius = .9)
    SolidType.TriangularPrism -> Solid(type, width = 2.2, height = 2.0, depth = 2.6)
    SolidType.PentagonalPrism -> Solid(type, width = 2.0, height = 2.3, depth = 2.0, radius = 1.0)
    SolidType.HexagonalPrism -> Solid(type, width = 2.0, height = 2.3, depth = 2.0, radius = 1.0)
    SolidType.OctagonalPrism -> Solid(type, width = 2.0, height = 2.3, depth = 2.0, radius = 1.0)
    SolidType.Tetrahedron -> Solid(type, width = 2.4)
    SolidType.TriangularPyramid -> Solid(type, width = 2.4, height = 2.6, depth = 2.2)
    SolidType.Octahedron -> Solid(type, width = 2.4)
    SolidType.Wedge -> Solid(type, width = 2.5, height = 1.8, depth = 2.4)
    SolidType.Torus -> Solid(type, width = 1.15, height = 1.0, depth = 1.0, radius = .42)
    SolidType.Ellipsoid -> Solid(type, width = 2.6, height = 1.7, depth = 2.0, radius = 1.0)
    SolidType.Paraboloid -> Solid(type, width = 2.0, height = 2.5, depth = 2.0, radius = 1.15)
    SolidType.Capsule -> Solid(type, width = 2.0, height = 3.0, depth = 2.0, radius = .8)
}
