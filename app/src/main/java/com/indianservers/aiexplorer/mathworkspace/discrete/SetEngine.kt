package com.indianservers.aiexplorer.mathworkspace.discrete

object SetEngine {
    fun parse(text:String):LinkedHashSet<String> = text.trim().removePrefix("{").removeSuffix("}").split(',').map{it.trim()}.filter{it.isNotEmpty()}.toCollection(linkedSetOf())
    fun union(a:Set<String>,b:Set<String>)=a union b
    fun intersection(a:Set<String>,b:Set<String>)=a intersect b
    fun difference(a:Set<String>,b:Set<String>)=a-b
    fun symmetricDifference(a:Set<String>,b:Set<String>)=(a-b)+(b-a)
    fun product(a:Set<String>,b:Set<String>)=a.flatMap{x->b.map{y->x to y}}
    fun format(s:Set<String>)="{"+s.joinToString(", ")+"}"
}
