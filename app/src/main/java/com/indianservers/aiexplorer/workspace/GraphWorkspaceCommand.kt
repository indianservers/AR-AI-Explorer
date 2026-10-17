package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.*

/** Studio history restores changed scene fields without overwriting the other graph workspace. */
data class GraphWorkspaceCommand(val from: WorkspaceState, val to: WorkspaceState, override val label: String) : WorkspaceCommand {
    override fun apply(state: WorkspaceState) = merge(state, to)
    override fun undo(state: WorkspaceState) = merge(state, from)
    private fun merge(state: WorkspaceState, target: WorkspaceState): WorkspaceState {
        require(state.id == from.id && state.module == from.module) { "Open the graph that owns this history." }
        val values = state.labSessionValues.toMutableMap()
        (from.labSessionValues.keys + to.labSessionValues.keys).filter { from.labSessionValues[it] != to.labSessionValues[it] }.forEach { key ->
            target.labSessionValues[key]?.let { values[key] = it } ?: values.remove(key)
        }
        val merged = state.copy(
            name = if(from.name!=to.name)target.name else state.name,
            points = if(from.points!=to.points) target.points else state.points,
            shapes = if(from.shapes!=to.shapes) target.shapes else state.shapes,
            pointDependencies = if(from.pointDependencies!=to.pointDependencies) target.pointDependencies else state.pointDependencies,
            geometryConstraints = if(from.geometryConstraints!=to.geometryConstraints) target.geometryConstraints else state.geometryConstraints,
            geometryGroups = if(from.geometryGroups!=to.geometryGroups) target.geometryGroups else state.geometryGroups,
            solids = if(from.solids!=to.solids) target.solids else state.solids,
            vectors3D=if(from.vectors3D!=to.vectors3D) target.vectors3D else state.vectors3D,
            points3D=if(from.points3D!=to.points3D) target.points3D else state.points3D,
            planes3D=if(from.planes3D!=to.planes3D) target.planes3D else state.planes3D,
            spatialPlacement=if(from.spatialPlacement!=to.spatialPlacement) target.spatialPlacement else state.spatialPlacement,
            graphSliderMetadata=if(from.graphSliderMetadata!=to.graphSliderMetadata) target.graphSliderMetadata else state.graphSliderMetadata,
            functions = if (from.functions != to.functions) target.functions else state.functions,
            graphRowMetadata = if (from.graphRowMetadata != to.graphRowMetadata) target.graphRowMetadata else state.graphRowMetadata,
            graph2DView = if (from.graph2DView != to.graph2DView) target.graph2DView else state.graph2DView,
            surfaceLayers = if (from.surfaceLayers != to.surfaceLayers) target.surfaceLayers else state.surfaceLayers,
            surfaceExpression = if (from.surfaceExpression != to.surfaceExpression) target.surfaceExpression else state.surfaceExpression,
            graph3DView = if (from.graph3DView != to.graph3DView) target.graph3DView else state.graph3DView,
            labSessionValues = values, modifiedAt = System.currentTimeMillis())
        return reconcileDocument(state, merged)
    }
    companion object {
    /** Replace only changed legacy projections; retain unrelated authoritative linked objects. */
    fun reconcileDocument(before: WorkspaceState, after: WorkspaceState): WorkspaceState {
        val stored = before.universalMathDocument ?: return after.copy(universalMathDocument = null)
        val old = UniversalWorkspaceBridge.fromWorkspace(before.copy(universalMathDocument = null))
        val fresh = UniversalWorkspaceBridge.fromWorkspace(after.copy(universalMathDocument = null))
        val changed = (old.objects.keys + fresh.objects.keys).filter { old.objects[it] != fresh.objects[it] }.toSet()
        val objects = (old.objects + stored.objects).filterKeys { it !in changed } + fresh.objects.filterKeys { it in changed }
        val document = stored.copy(objects = objects, revision = stored.revision + 1, modifiedAt = System.currentTimeMillis())
        val validation = UniversalMathDocumentEngine().validate(document)
        require(validation.valid) { validation.diagnostics.joinToString("; ") }
        return after.copy(universalMathDocument = UniversalMathRuntime().recompute(document).document)
    }
    }
}
