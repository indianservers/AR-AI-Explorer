package com.indianservers.aiexplorer.gamifymaths

import kotlin.math.pow
import kotlin.math.sqrt

internal data class BuiltCalculation(val value: Double, val operations: Int)

/** A bounded arithmetic parser, never executable code or a network evaluator. */
internal fun evaluateMathAssembly(tokens: List<String>): BuiltCalculation? = try {
    if(tokens.isEmpty() || tokens.size>80) null else AssemblyParser(tokens).parse()
} catch (_: IllegalArgumentException) { null }

private class AssemblyParser(private val tokens:List<String>) {
    private var position=0
    private var operations=0
    fun parse():BuiltCalculation {
        val value=expression(0)
        require(position==tokens.size && value.isFinite())
        return BuiltCalculation(value,operations)
    }
    private fun precedence(token:String)=when(token){"+","−"->1;"×","÷"->2;"^","C","P"->3;else->-1}
    private fun expression(minimum:Int):Double {
        var left=atom()
        while(position<tokens.size && precedence(tokens[position])>=minimum) {
            val operator=tokens[position++];val priority=precedence(operator)
            val right=expression(if(operator=="^") priority else priority+1)
            operations++
            left=when(operator) {
                "+"->left+right;"−"->left-right;"×"->left*right
                "÷"->{require(right!=0.0);left/right}
                "^"->{require(kotlin.math.abs(right)<=20);left.pow(right)}
                "C","P"->{
                    require(left==left.toInt().toDouble() && right==right.toInt().toDouble())
                    val n=left.toInt();val k=right.toInt();require(n in 0..100 && k in 0..n)
                    var product=1.0
                    for(i in 1..k) product=product*(n-k+i)/(if(operator=="C") i else 1)
                    product
                }
                else->throw IllegalArgumentException()
            }
            require(left.isFinite())
        }
        return left
    }
    private fun atom():Double {
        require(position<tokens.size)
        val token=tokens[position++]
        var value=when(token) {
            "("->{val result=expression(0);require(position<tokens.size && tokens[position++]==")");result}
            "−"->-atom()
            "√"->{operations++;val result=atom();require(result>=0);sqrt(result)}
            "sin","cos","tan"->{operations++;val angle=Math.toRadians(atom());when(token){"sin"->kotlin.math.sin(angle);"cos"->kotlin.math.cos(angle);else->{require(kotlin.math.abs(kotlin.math.cos(angle))>1e-9);kotlin.math.tan(angle)}}}
            else->token.toDoubleOrNull()?.also{require(it.isFinite())} ?: throw IllegalArgumentException()
        }
        while(position<tokens.size && tokens[position]=="!") {
            position++;operations++;require(value==value.toInt().toDouble() && value in 0.0..20.0)
            value=(1..value.toInt()).fold(1.0){product,n->product*n}
        }
        return value
    }
}
