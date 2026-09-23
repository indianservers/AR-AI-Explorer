package com.indianservers.aiexplorer.mathworkspace.vector

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop

class VectorLabViewModel : ViewModel() {
    val vectors = mutableStateListOf(
        WorkspaceVector("a", "A", 2.0, 1.0, 0.0, 0xFF2878F0),
        WorkspaceVector("b", "B", -1.0, 4.0, 0.0, 0xFFD94B5B),
    )
    val dimension = mutableStateOf(VectorDimension.TwoD)
    val selectedA = mutableStateOf("a")
    val selectedB = mutableStateOf("b")
    val operation = mutableStateOf(VectorOperation.Add)
    val panel = mutableStateOf(VectorPanel.Vectors)
    val sheetStop = mutableStateOf(WorkspaceSheetStop.Peek)
    val zoom = mutableStateOf(1f)
    val pan = mutableStateOf(Offset.Zero)
    val showGrid = mutableStateOf(true)
    val alpha = mutableStateOf("2")
    val beta = mutableStateOf("-1")
    val canUndo: Boolean get() = undoStates.isNotEmpty()
    val canRedo: Boolean get() = redoStates.isNotEmpty()
    private val undoStates = mutableListOf<List<WorkspaceVector>>()
    private val redoStates = mutableListOf<List<WorkspaceVector>>()
    private var dragBefore: List<WorkspaceVector>? = null
    private var nextId = 0

    fun addVector(): String {
        saveUndo()
        val n = ++nextId
        val name = "v$n"
        val id = "vector-$n"
        vectors.add(WorkspaceVector(id, name, 1.0, 1.0, 0.0, palette[(n - 1) % palette.size]))
        if (selectedA.value !in vectors.map { it.id }) selectedA.value = id
        return id
    }

    fun update(id: String, edit: (WorkspaceVector) -> WorkspaceVector) {
        val index = vectors.indexOfFirst { it.id == id }
        if (index < 0) return
        val next = edit(vectors[index])
        if (next == vectors[index]) return
        saveUndo()
        vectors[index] = next
    }

    fun remove(id: String) {
        val index = vectors.indexOfFirst { it.id == id }
        if (index < 0) return
        saveUndo()
        vectors.removeAt(index)
        if (selectedA.value == id) selectedA.value = vectors.firstOrNull()?.id.orEmpty()
        if (selectedB.value == id) selectedB.value = vectors.firstOrNull { it.id != selectedA.value }?.id.orEmpty()
    }

    fun beginEndpointDrag() { dragBefore = vectors.toList() }
    fun dragEndpoint(id: String, delta: MathVector3) {
        val index = vectors.indexOfFirst { it.id == id }
        if (index >= 0) vectors[index] = vectors[index].let { it.copy(x = it.x + delta.x, y = it.y + delta.y, z = it.z + delta.z) }
    }
    fun endEndpointDrag() {
        val before = dragBefore ?: return
        if (before != vectors.toList()) { undoStates += before; redoStates.clear() }
        dragBefore = null
    }
    fun undo() {
        val old = undoStates.removeLastOrNull() ?: return
        redoStates += vectors.toList()
        vectors.clear(); vectors.addAll(old)
    }
    fun redo() {
        val next = redoStates.removeLastOrNull() ?: return
        undoStates += vectors.toList()
        vectors.clear(); vectors.addAll(next)
    }
    fun resetView() { zoom.value = 1f; pan.value = Offset.Zero }
    fun fitToVectors() {
        val extent = vectors.filter { it.visible }.maxOfOrNull { maxOf(kotlin.math.abs(it.x), kotlin.math.abs(it.y), kotlin.math.abs(it.z)) } ?: 1.0
        zoom.value = (4.2 / extent.coerceAtLeast(1e-12)).toFloat().coerceIn(.0001f, 100000f)
        pan.value = Offset.Zero
    }

    private fun saveUndo() { undoStates += vectors.toList(); redoStates.clear() }

    private companion object {
        val palette = listOf(0xFF2878F0, 0xFFD94B5B, 0xFF17976D, 0xFF7552D9, 0xFFB87800)
    }
}
