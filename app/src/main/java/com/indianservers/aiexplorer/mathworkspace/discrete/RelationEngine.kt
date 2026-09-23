package com.indianservers.aiexplorer.mathworkspace.discrete

data class RelationProperties(val reflexive:Boolean,val symmetric:Boolean,val antisymmetric:Boolean,val transitive:Boolean) { val equivalence get()=reflexive&&symmetric&&transitive; val partialOrder get()=reflexive&&antisymmetric&&transitive }
object RelationEngine {
    fun parsePairs(text:String):Set<Pair<String,String>> = Regex("\\(([^,()]+),([^,()]+)\\)").findAll(text).map{it.groupValues[1].trim() to it.groupValues[2].trim()}.toSet()
    fun properties(elements:Set<String>,r:Set<Pair<String,String>>):RelationProperties {
        val refl=elements.all{it to it in r};val sym=r.all{(a,b)->b to a in r};val anti=r.all{(a,b)->a==b||b to a !in r};val trans=r.all{(a,b)->elements.filter{b to it in r}.all{a to it in r}}
        return RelationProperties(refl,sym,anti,trans)
    }
    fun equivalenceClasses(elements:Set<String>,r:Set<Pair<String,String>>):List<Set<String>> { val unseen=elements.toMutableSet();val out=mutableListOf<Set<String>>();while(unseen.isNotEmpty()){val x=unseen.first();val c=elements.filterTo(linkedSetOf()){(x to it) in r&&(it to x) in r};out+=c;unseen-=c};return out }
    fun hasseEdges(elements:Set<String>,r:Set<Pair<String,String>>):Set<Pair<String,String>> = r.filterTo(linkedSetOf()){(a,b)->a!=b&&!elements.any{c->c!=a&&c!=b&&(a to c)in r&&(c to b)in r}}
}
