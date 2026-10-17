package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.workspace.WorkspaceState

/** Layout measurement is not a student edit. Callers must still check selection and workspace visibility. */
object RuhiPreparedSceneGuard {
    fun onlyViewportMetricsChanged(before:WorkspaceState,current:WorkspaceState):Boolean {
        val metrics=setOf(RuhiIntersectionLanguage.ASPECT,RuhiSceneEditLanguage.ASPECT)
        return before.copy(labSessionValues=before.labSessionValues.filterKeys{it !in metrics},modifiedAt=0L)==current.copy(labSessionValues=current.labSessionValues.filterKeys{it !in metrics},modifiedAt=0L)
    }
}
