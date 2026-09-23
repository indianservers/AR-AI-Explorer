package com.indianservers.aiexplorer.mathworkspace.tiles

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import kotlin.math.roundToInt

class MathTilesViewModel : ViewModel() {
    val tiles = mutableStateListOf<AlgebraTile>()
    val selectedIds = mutableStateListOf<String>()
    val cancellingIds = mutableStateListOf<String>()
    val sheetStop = mutableStateOf(WorkspaceSheetStop.Peek)
    val zoom = mutableStateOf(1f)
    val pan = mutableStateOf(Offset.Zero)
    val multiSelect = mutableStateOf(false)
    val revealPairs = mutableStateOf(false)
    val expressionInput = mutableStateOf("x^2 + 3x + 2")
    val inputError = mutableStateOf("")
    val expression: String get() = AlgebraTileEngine.expression(tiles)
    val pairs: List<TileZeroPair> get() = AlgebraTileEngine.zeroPairs(tiles)
    val canUndo: Boolean get() = undoStates.isNotEmpty()
    val canRedo: Boolean get() = redoStates.isNotEmpty()
    private val undoStates = mutableListOf<List<AlgebraTile>>()
    private val redoStates = mutableListOf<List<AlgebraTile>>()
    private var dragBefore: List<AlgebraTile>? = null
    private var nextId = 0
    private var nextGroup = 0

    fun addTile(kind: TileKind, x: Float = (nextId % 4 - 1.5f) * 1.6f, y: Float = (nextId / 4) * 1.7f - .85f): String {
        saveUndo()
        val id = "tile-${++nextId}"
        tiles += AlgebraTile(id, kind, x, y)
        selectedIds.clear(); selectedIds += id
        return id
    }

    fun beginPaletteDrag(kind: TileKind): String {
        dragBefore = tiles.toList()
        val id = "tile-${++nextId}"
        tiles += AlgebraTile(id, kind, 0f, 0f)
        selectedIds.clear(); selectedIds += id
        return id
    }

    fun select(id: String) {
        if (multiSelect.value) {
            if (id in selectedIds) selectedIds.remove(id) else selectedIds += id
        } else {
            selectedIds.clear(); selectedIds += id
        }
    }

    fun prepareDrag(id: String) {
        if (id !in selectedIds) {
            if (multiSelect.value) selectedIds += id else { selectedIds.clear(); selectedIds += id }
        }
    }

    fun beginDrag() { dragBefore = tiles.toList() }
    fun move(id: String, dx: Float, dy: Float) {
        val item = tiles.firstOrNull { it.id == id } ?: return
        val moving = when {
            item.groupId != null -> tiles.filter { it.groupId == item.groupId }.map { it.id }.toSet()
            multiSelect.value && id in selectedIds -> selectedIds.toSet()
            else -> setOf(id)
        }
        for (i in tiles.indices) if (tiles[i].id in moving) tiles[i] = tiles[i].copy(x = tiles[i].x + dx, y = tiles[i].y + dy)
    }
    fun endDrag() {
        val old = dragBefore ?: return
        if (old != tiles.toList()) { undoStates += old; redoStates.clear() }
        dragBefore = null
    }
    fun deleteSelected() {
        if (selectedIds.isEmpty()) return
        saveUndo()
        tiles.removeAll { it.id in selectedIds }
        selectedIds.clear()
    }
    fun duplicateSelected() {
        if (selectedIds.isEmpty()) return
        saveUndo()
        val copies = tiles.filter { it.id in selectedIds }.map { it.copy(id = "tile-${++nextId}", x = it.x + .35f, y = it.y + .35f, groupId = null) }
        tiles.addAll(copies); selectedIds.clear(); selectedIds.addAll(copies.map { it.id })
    }
    fun clear() {
        if (tiles.isEmpty()) return
        saveUndo(); tiles.clear(); selectedIds.clear(); cancellingIds.clear(); revealPairs.value = false
    }
    fun snapSelected() {
        if (selectedIds.isEmpty()) return
        saveUndo()
        for (i in tiles.indices) if (tiles[i].id in selectedIds) tiles[i] = tiles[i].copy(x = tiles[i].x.roundToInt().toFloat(), y = tiles[i].y.roundToInt().toFloat())
    }
    fun groupSelected() {
        if (selectedIds.size < 2) return
        saveUndo(); val group = "group-${++nextGroup}"
        for (i in tiles.indices) if (tiles[i].id in selectedIds) tiles[i] = tiles[i].copy(groupId = group)
    }
    fun ungroupSelected() {
        if (selectedIds.isEmpty()) return
        saveUndo(); for (i in tiles.indices) if (tiles[i].id in selectedIds) tiles[i] = tiles[i].copy(groupId = null)
    }
    fun removeZeroPairs() {
        val removeIds = pairs.flatMap { listOf(it.positiveId, it.negativeId) }.toSet()
        if (removeIds.isEmpty()) return
        saveUndo(); tiles.removeAll { it.id in removeIds }; selectedIds.removeAll { it in removeIds }; cancellingIds.clear(); revealPairs.value = false
    }
    fun beginZeroPairCancellation() { cancellingIds.clear(); cancellingIds.addAll(pairs.flatMap { listOf(it.positiveId, it.negativeId) }) }
    fun buildFromInput() {
        AlgebraTileEngine.parse(expressionInput.value).onSuccess { parsed ->
            saveUndo()
            tiles.clear(); selectedIds.clear()
            val dimensions = parsed.tiles.map { tileWidth(it) }
            val gap = .16f
            val totalWidth = dimensions.sum() + gap * (dimensions.size - 1).coerceAtLeast(0)
            var cursor = -totalWidth / 2f
            parsed.tiles.forEachIndexed { index, kind ->
                val width = dimensions[index]
                addTileWithoutHistory(kind, cursor + width / 2f, 0f)
                cursor += width + gap
            }
            zoom.value = (5.8f / totalWidth.coerceAtLeast(5.8f)).coerceIn(.02f, 1f)
            pan.value = Offset.Zero
            inputError.value = ""
        }.onFailure { inputError.value = it.message ?: "Expression could not be converted to tiles." }
    }
    fun undo() { val old = undoStates.removeLastOrNull() ?: return; redoStates += tiles.toList(); tiles.clear(); tiles.addAll(old); selectedIds.clear() }
    fun redo() { val next = redoStates.removeLastOrNull() ?: return; undoStates += tiles.toList(); tiles.clear(); tiles.addAll(next); selectedIds.clear() }
    fun resetView() { zoom.value = 1f; pan.value = Offset.Zero }
    private fun addTileWithoutHistory(kind: TileKind, x: Float, y: Float) { tiles += AlgebraTile("tile-${++nextId}", kind, x, y) }
    private fun saveUndo() { undoStates += tiles.toList(); redoStates.clear() }
    private fun tileWidth(kind: TileKind): Float = if (kind.family == TileFamily.Unit) .36f else 1.2f
}
