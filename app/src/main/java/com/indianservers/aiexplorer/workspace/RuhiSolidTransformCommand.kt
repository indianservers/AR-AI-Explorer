package com.indianservers.aiexplorer.workspace

/** Field-level history permits undoing a rotation while preserving a later translation or styling. */
data class RuhiSolidTransformCommand(val from:WorkspaceState,val to:WorkspaceState,override val label:String):WorkspaceCommand {
    override fun apply(state:WorkspaceState)=merge(state,to)
    override fun undo(state:WorkspaceState)=merge(state,from)
    private fun merge(state:WorkspaceState,target:WorkspaceState):WorkspaceState {
        require(state.id==from.id&&state.module==from.module&&from.solids.size==to.solids.size&&state.solids.size==from.solids.size){"The solid structure changed; confirm the current scene before undoing this transform."}
        val solids=state.solids.mapIndexed{i,current->val a=from.solids[i];val b=to.solids[i];val q=target.solids[i]
            require(current.type==a.type){"The solid target changed."}
            current.copy(width=if(a.width!=b.width)q.width else current.width,height=if(a.height!=b.height)q.height else current.height,depth=if(a.depth!=b.depth)q.depth else current.depth,radius=if(a.radius!=b.radius)q.radius else current.radius,topRadius=if(a.topRadius!=b.topRadius)q.topRadius else current.topRadius,position=if(a.position!=b.position)q.position else current.position,rotation=if(a.rotation!=b.rotation)q.rotation else current.rotation,mirrorX=if(a.mirrorX!=b.mirrorX)q.mirrorX else current.mirrorX)}
        val values=state.labSessionValues.toMutableMap();(from.labSessionValues.keys+to.labSessionValues.keys).filter{from.labSessionValues[it]!=to.labSessionValues[it]}.forEach{k->target.labSessionValues[k]?.let{values[k]=it} ?: values.remove(k)}
        return GraphWorkspaceCommand.reconcileDocument(state,state.copy(solids=solids,labSessionValues=values,modifiedAt=System.currentTimeMillis()))
    }
}
