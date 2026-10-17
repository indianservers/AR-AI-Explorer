package com.indianservers.aiexplorer.workspace

/** Preserve named references when native tools compact point arrays. */
object StudioReferenceMetadata {
    const val POINT_PREFIX="ruhi.studio.point."
    private const val DERIVATIVE_POINT_PREFIX="ruhi.derivative.point."
    private const val SLOPE_PREFIX="ruhi.geometry2d.slope."
    private const val ANGLE_PREFIX="ruhi.geometry2d.angle."
    private const val REFERENCE_ANGLE_PREFIX="ruhi.geometry2d.referenceAngle."
    fun remapPoints(values:Map<String,String>,indices:Map<Int,Int>):Map<String,String> {
        val base=values.filterKeys{!it.startsWith(SLOPE_PREFIX)&&!it.startsWith(ANGLE_PREFIX)&&!it.startsWith(REFERENCE_ANGLE_PREFIX)}.mapNotNull { (key,value)->
        if(!key.startsWith(POINT_PREFIX)&&!key.startsWith(DERIVATIVE_POINT_PREFIX)) key to value else value.toIntOrNull()?.let(indices::get)?.let { key to it.toString() }
        }.toMap()
        val marks=values.filterKeys{it.startsWith(SLOPE_PREFIX)}.values.mapNotNull{value->val p=value.split('|');val ids=p.firstOrNull()?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty();if(p.size!=2||ids.size!=2)return@mapNotNull null;val a=indices[ids[0]] ?: return@mapNotNull null;val b=indices[ids[1]] ?: return@mapNotNull null;SLOPE_PREFIX+"$a.$b" to "$a,$b|${p[1]}"}.toMap()
        val angles=values.filterKeys{it.startsWith(ANGLE_PREFIX)}.values.mapNotNull{value->
            val p=value.split('|');val ids=p[0].split(',').mapNotNull(String::toIntOrNull)
            if(p.size !in 2..3||ids.size!=3)return@mapNotNull null
            val mapped=ids.map{indices[it] ?: return@mapNotNull null}
            val polygon=p.getOrNull(2)?.split(',')?.map{it.toIntOrNull()?.let(indices::get) ?: return@mapNotNull null}
            ANGLE_PREFIX+mapped.joinToString(".") to (mapped.joinToString(",")+"|"+p[1]+if(polygon==null)""else "|"+polygon.joinToString(","))
        }.toMap()
        val referenceAngles=values.filterKeys{it.startsWith(REFERENCE_ANGLE_PREFIX)}.values.mapNotNull{value->
            val p=value.split('|');if(p.size!=2)return@mapNotNull null
            val ids=p[1].split(',').map{it.toIntOrNull()?.let(indices::get) ?: return@mapNotNull null}
            REFERENCE_ANGLE_PREFIX+p[0]+"."+ids.joinToString(".") to (p[0]+"|"+ids.joinToString(","))
        }.toMap()
        return base+marks+angles+referenceAngles
    }
    fun restorePointReferences(current:Map<String,String>,before:Map<String,String>):Map<String,String> {
        fun owned(k:String)=k.startsWith(POINT_PREFIX)||k.startsWith(DERIVATIVE_POINT_PREFIX)||k.startsWith(SLOPE_PREFIX)||k.startsWith(ANGLE_PREFIX)||k.startsWith(REFERENCE_ANGLE_PREFIX)
        return current.filterKeys{!owned(it)}+before.filterKeys(::owned)
    }
    fun requireSafeSolidRemoval(state:WorkspaceState,removed:Set<Int>) {
        val first=removed.minOrNull() ?: return
        val affected=(first..state.solids.lastIndex).map { "solid-$it" }.toSet()
        require(state.universalMathDocument?.objects?.values?.none { obj -> obj.id !in affected && obj.dependencies.any { it in affected } } != false) {
            "Remove linked dependents before deleting solids whose indexed references would change."
        }
    }
    fun remapSolidAppearance(values:Map<String,String>,keep:List<Int>):Map<String,String> {
        val prefixes=listOf("ruhi.studio.appearance.","ruhi.geometry3d.name.","ruhi.transform3d.opacity.","ruhi.transform3d.mode.","ruhi.transform3d.explosion.")
        val selection="ruhi.geometry3d.selection"
        val base=values.filterKeys{key->prefixes.none(key::startsWith)&&key!=selection&&key!="ruhi.transform3d.groupMotion"}
        val remapped=prefixes.flatMap{prefix->keep.mapIndexedNotNull{next,old->values[prefix+old]?.let{prefix+next to it}}}.toMap()
        val indices=keep.withIndex().associate{it.value to it.index}
        val selected=values[selection]?.split(',')?.mapNotNull{it.toIntOrNull()?.let(indices::get)}.orEmpty()
        return base+remapped+if(selected.isEmpty())emptyMap() else mapOf(selection to selected.joinToString(","))
    }
}
