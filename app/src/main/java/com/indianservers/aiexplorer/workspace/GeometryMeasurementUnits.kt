package com.indianservers.aiexplorer.workspace

import java.math.BigDecimal

/** Coordinates stay in base units. Only dimensional quantities acquire display-unit factors. */
object GeometryMeasurementUnits {
    enum class Length(val symbol:String,val meters:Double){METER("m",1.0),CENTIMETER("cm",.01),MILLIMETER("mm",.001)}
    data class Context(val base:Length?=null,val display:Length?=base) {
        val known get()=base!=null&&display!=null
        private fun decimalFactor(power:Int):BigDecimal {require(power in 0..3);return if(known)BigDecimal.valueOf(base!!.meters).divide(BigDecimal.valueOf(display!!.meters)).pow(power)else BigDecimal.ONE}
        fun factor(power:Int):Double=decimalFactor(power).toDouble()
        fun factorText(power:Int):String=decimalFactor(power).stripTrailingZeros().toPlainString()
        fun suffix(power:Int):String=if(power==0)""else if(known)display!!.symbol+when(power){2->"²";3->"³";else->""}else when(power){1->"units";2->"square units";else->"cubic units"}
        fun number(value:Double,power:Int,format:(Double)->String):String {
            val factor=factor(power);val scaled=value*factor
            if(value.isFinite()&&(!scaled.isFinite()||value!=0.0&&scaled==0.0))return BigDecimal.valueOf(value).multiply(decimalFactor(power)).stripTrailingZeros().toEngineeringString()
            val text=format(scaled)
            return if(scaled.isFinite()&&(text in setOf("Infinity","-Infinity","NaN")||scaled!=0.0&&text.toDoubleOrNull()==0.0))scaled.toString()else text
        }
        fun text(value:Double,power:Int,format:(Double)->String={it.toString()})=number(value,power,format)+suffix(power).takeIf{it.isNotEmpty()}?.let{" $it"}.orEmpty()
    }
    private fun key(module:MathModule,role:String)="${module.name}::ruhi.measure.$role"
    fun read(s:WorkspaceState):Context {
        fun parse(role:String)=s.labSessionValues[key(s.module,role)]?.let{value->Length.entries.firstOrNull{it.symbol==value}}
        val base=parse("base");return Context(base,parse("display") ?: base)
    }
    fun set(s:WorkspaceState,base:Length,display:Length)=s.copy(labSessionValues=s.labSessionValues+mapOf(key(s.module,"base") to base.symbol,key(s.module,"display") to display.symbol))
}
