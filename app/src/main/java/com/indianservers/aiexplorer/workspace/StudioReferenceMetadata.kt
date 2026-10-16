package com.indianservers.aiexplorer.workspace

/** Preserve named references when native tools compact point arrays. */
object StudioReferenceMetadata {
    const val POINT_PREFIX="ruhi.studio.point."
    fun remapPoints(values:Map<String,String>,indices:Map<Int,Int>):Map<String,String> = values.mapNotNull { (key,value)->
        if(!key.startsWith(POINT_PREFIX)) key to value else value.toIntOrNull()?.let(indices::get)?.let { key to it.toString() }
    }.toMap()
    fun restorePointReferences(current:Map<String,String>,before:Map<String,String>):Map<String,String> = current.filterKeys { !it.startsWith(POINT_PREFIX) }+before.filterKeys { it.startsWith(POINT_PREFIX) }
    fun requireSafeSolidRemoval(state:WorkspaceState,removed:Set<Int>) {
        val first=removed.minOrNull() ?: return
        val affected=(first..state.solids.lastIndex).map { "solid-$it" }.toSet()
        require(state.universalMathDocument?.objects?.values?.none { obj -> obj.id !in affected && obj.dependencies.any { it in affected } } != false) {
            "Remove linked dependents before deleting solids whose indexed references would change."
        }
    }
    fun remapSolidAppearance(values:Map<String,String>,keep:List<Int>):Map<String,String> {
        val prefix="ruhi.studio.appearance."
        return values.filterKeys{!it.startsWith(prefix)}+keep.mapIndexedNotNull{next,old->values[prefix+old]?.let{prefix+next to it}}.toMap()
    }
}
