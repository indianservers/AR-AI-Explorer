package com.indianservers.aiexplorer.mathworkspace.vector

data class WorkspaceVector(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val color: Long,
    val visible: Boolean = true,
)

enum class VectorDimension(val title: String) { TwoD("2D"), ThreeD("3D") }

enum class VectorOperation(val title: String) {
    Add("A + B"), Subtract("A − B"), Scalar("αA + βB"), Magnitude("|A|"), Unit("Unit A"),
    Dot("A · B"), Cross("A × B"), Angle("Angle"), Projection("projᵦ A"),
}

enum class VectorPanel { Vectors, Operations }
