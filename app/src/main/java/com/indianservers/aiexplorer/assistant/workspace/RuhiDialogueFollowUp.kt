package com.indianservers.aiexplorer.assistant.workspace

/** Follow-up language selects an existing answer/action; it never invents scene entities. */
object RuhiDialogueFollowUp {
    fun command(source:String,pending:RuhiPending?,view:RuhiDialogueView?,roleValues:Map<String,Double> = emptyMap(),module:String=""):String? {
        val t=runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("")
        val ordinal=when(t) {
            "choose the first suggested answer","just the first object"->0
            "choose the second suggested answer"->1
            "choose the third suggested answer"->2
            else->null
        }
        if(ordinal!=null&&pending!=null) return view?.suggestions?.getOrNull(ordinal)?.answer
        val corrected=Regex("^no,? make it ([-+]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)) instead$").matchEntire(t)?.groupValues?.get(1)
        if(corrected!=null)when(pending?.kind) {
            "solid_measure_dimension"->return "Use measurement value $corrected"
            "geo_scale"->return corrected
        }
        if(t=="use the same radius as before"&&pending?.kind=="solid_measure_dimension"&&pending.targets.firstOrNull()=="radius") {
            roleValues["$module:radius"]?.takeIf{it.isFinite()&&it>0}?.let{return "Use measurement value $it"}
        }
        if(t=="both of them"&&pending?.kind=="target")return "Both"
        if(t=="use x^2 for the equation"&&pending?.kind=="equation")return "y = x^2"
        if(t=="make a triangle")return "create a triangle"
        if(t=="rotate that")return if(module=="Geometry3D")"rotate the selected object 45 degrees around the Y-axis"else"rotate the selected object 45 degrees"
        if(t=="make it larger")return "scale the selected object by 1.25"
        if(t=="start again with a blank canvas")return "clear the canvas"
        return null
    }
    fun asksOptions(source:String)=runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("") in setOf("what choices do i have","show my choices","show the options")
    fun asksError(source:String)=runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("") in setOf("why did that fail","why did it fail","explain the last error")
}
