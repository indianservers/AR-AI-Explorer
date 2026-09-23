package com.indianservers.aiexplorer.mathworkspace.calculus

import kotlin.math.*

class FunctionParser private constructor(private val root: Node) {
    fun value(x: Double): Double = runCatching { root.eval(x) }.getOrDefault(Double.NaN)
    companion object {
        fun parse(expression: String): FunctionParser {
            val clean = expression.trim().replace("−", "-").replace("×", "*").replace("÷", "/").replace("π", "pi")
            require(clean.isNotEmpty()) { "Enter a function of x." }
            return FunctionParser(Parser(clean).parse())
        }
    }
    private sealed interface Node { fun eval(x: Double): Double }
    private data class NumberNode(val value: Double):Node { override fun eval(x:Double)=value }
    private data object XNode:Node { override fun eval(x:Double)=x }
    private data class Unary(val op:Char,val a:Node):Node { override fun eval(x:Double)=if(op=='-') -a.eval(x) else a.eval(x) }
    private data class Binary(val op:Char,val a:Node,val b:Node):Node { override fun eval(x:Double):Double { val l=a.eval(x);val r=b.eval(x);return when(op){'+'->l+r;'-'->l-r;'*'->l*r;'/'->l/r;'^'->l.pow(r);else->Double.NaN} } }
    private data class Func(val name:String,val arg:Node):Node { override fun eval(x:Double):Double { val v=arg.eval(x); return when(name){"sin"->sin(v);"cos"->cos(v);"tan"->tan(v);"exp"->exp(v);"ln","log"->ln(v);"sqrt"->sqrt(v);"abs"->abs(v);"asin"->asin(v);"acos"->acos(v);"atan"->atan(v);else->Double.NaN} } }

    private class Parser(source:String) {
        private val tokens=Regex("(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?|[A-Za-z_][A-Za-z_0-9]*|[^\\s]").findAll(source).map{it.value}.toList()
        private var i=0
        private fun peek()=tokens.getOrNull(i)
        private fun take()=tokens.getOrNull(i++) ?: throw IllegalArgumentException("Unexpected end of expression.")
        fun parse():Node { val n=expression(); require(i==tokens.size){"Unexpected token '${peek()}'."}; return n }
        private fun expression():Node { var n=term(); while(peek()=="+"||peek()=="-") { val op=take()[0];n=Binary(op,n,term()) };return n }
        private fun term():Node { var n=unary(); while(true) { when(peek()) {"*"->{take();n=Binary('*',n,unary())};"/"->{take();n=Binary('/',n,unary())};else->if(startsPrimary(peek())) n=Binary('*',n,unary()) else return n } } }
        private fun unary():Node = when(peek()) {"+"->{take();Unary('+',unary())};"-"->{take();Unary('-',unary())};else->power()}
        private fun power():Node { val n=primary(); return if(peek()=="^"){take();Binary('^',n,unary())}else n }
        private fun primary():Node {
            val token=take()
            token.toDoubleOrNull()?.let{return NumberNode(it)}
            if(token=="("){val n=expression();require(take()==")"){"Missing closing parenthesis."};return n}
            if(token.equals("x",true)) return XNode
            if(token.equals("pi",true))return NumberNode(PI);if(token.equals("e",true))return NumberNode(Math.E)
            val name=token.lowercase();require(name in setOf("sin","cos","tan","exp","ln","log","sqrt","abs","asin","acos","atan")){"Unknown symbol '$token'."}
            val arg=if(peek()=="("){take();val n=expression();require(take()==")"){"Missing closing parenthesis."};n}else unary()
            return Func(name,arg)
        }
        private fun startsPrimary(token:String?) = token!=null && (token=="(" || token.firstOrNull()?.isLetter()==true || token.firstOrNull()?.isDigit()==true || token.startsWith("."))
    }
}
