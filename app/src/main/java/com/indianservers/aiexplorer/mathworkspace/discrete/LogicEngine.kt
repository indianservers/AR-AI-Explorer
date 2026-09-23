package com.indianservers.aiexplorer.mathworkspace.discrete

data class TruthRow(val values:Map<String,Boolean>,val result:Boolean)
data class TruthTable(val variables:List<String>,val rows:List<TruthRow>,val classification:String)
object LogicEngine {
    fun evaluate(expression:String,values:Map<String,Boolean>):Boolean = Parser(expression,values).parse()
    fun table(expression:String):TruthTable {
        val vars=Regex("[A-Za-z][A-Za-z0-9_]*").findAll(expression.uppercase()).map{it.value}.filter{it !in setOf("AND","OR","NOT","XOR","TRUE","FALSE")}.distinct().sorted().toList()
        require(vars.size<=8){"Use at most 8 variables"}
        val rows=(0 until (1 shl vars.size)).map{mask->val m=vars.mapIndexed{index,s->s to (mask and (1 shl (vars.lastIndex-index))!=0)}.toMap();TruthRow(m,evaluate(expression,m))}
        val truths=rows.map{it.result}.toSet();return TruthTable(vars,rows,when(truths){setOf(true)->"Tautology";setOf(false)->"Contradiction";else->"Contingency"})
    }
    private class Parser(text:String,val values:Map<String,Boolean>) {
        private val ts=Regex("<->|<=>|->|=>|&&|\\|\\||[()!~¬∧∨⊕→↔]|[A-Za-z][A-Za-z0-9_]*|[01]").findAll(text.uppercase().replace("EQUIVALENT","<->").replace("IMPLIES","->")).map{it.value}.toList();private var i=0
        fun parse():Boolean {val v=equiv();if(i!=ts.size)error("Invalid logic expression near ${ts[i]}");return v}
        private fun equiv():Boolean {var a=implies();while(take("<->")||take("<=>")||take("↔")){val b=implies();a=a==b};return a}
        private fun implies():Boolean {var a=or();while(take("->")||take("=>")||take("→")){val b=or();a=!a||b};return a}
        private fun or():Boolean {var a=and();while(take("OR")||take("||")||take("∨")){val b=and();a=a||b};return a}
        private fun and():Boolean {var a=xor();while(take("AND")||take("&&")||take("∧")){val b=xor();a=a&&b};return a}
        private fun xor():Boolean {var a=not();while(take("XOR")||take("⊕")){val b=not();a=a xor b};return a}
        private fun not():Boolean {if(take("NOT")||take("!")||take("~")||take("¬"))return !not();if(take("(")){val x=equiv();if(!take(")"))error("Missing )");return x};val t=ts.getOrNull(i++)?:error("Expected proposition");return when(t){"TRUE","1"->true;"FALSE","0"->false;else->values[t]?:error("Missing value for $t")}}
        private fun take(s:String):Boolean {if(ts.getOrNull(i)==s){i++;return true};return false}
    }
}
