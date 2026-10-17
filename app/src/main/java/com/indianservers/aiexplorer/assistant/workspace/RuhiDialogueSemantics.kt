package com.indianservers.aiexplorer.assistant.workspace

/** The student's contextual request and the executed native operation retain separate roles. */
object RuhiDialogueSemantics {
    fun parameters(source:String):Map<String,Any> {
        val text=RuhiMathLanguage.normalize(source).replace(",","").replace('’','\'')
        Regex("^no make it ([-+]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)) instead$").matchEntire(text)?.let{return mapOf("correction" to it.groupValues[1].toDouble(),"slot" to "pending_numeric_parameter")}
        return when(text) {
        "draw a graph"->mapOf("intent" to "plot","expression" to "missing")
        "make it larger"->mapOf("target" to "contextual_last_object","scale" to "increase")
        "rotate that"->mapOf("target" to "contextual_last_object","operation" to "rotate")
        "what about the other 1"->mapOf("target" to "contextual_other_object","intent" to "inherit")
        "use the same radius as before"->mapOf("radius" to "previous_radius","target" to "pending_object")
        "actually i meant the blue 1"->mapOf("correction_target" to "blue_object")
        "both of them"->mapOf("target" to "both_pending_candidates")
        "just the first object"->mapOf("target" to "first_pending_candidate")
        "cancel that"->mapOf("action" to "cancel_pending")
        "continue where we stopped"->mapOf("action" to "resume_pending")
        "what choices do i have"->mapOf("action" to "suggest_options")
        "make a triangle"->mapOf("action" to "create_triangle","defaults" to true)
        "draw it over there"->mapOf("target" to "last_object","location" to "deictic_ambiguous")
        "use x^2 for the equation"->mapOf("expression" to "x^2","pending" to "graph")
        "choose the second suggested answer"->mapOf("choice_index" to 2)
        "explain the shape we are viewing"->mapOf("action" to "explain","target" to "current_scene")
        "compare this with the previous graph"->mapOf("targets" to listOf("selected_graph","previous_graph"))
        "start again with a blank canvas"->mapOf("action" to "clear_scene")
        "can we make the angle 60 degrees"->mapOf("target" to "selected_angle","angle_degrees" to 60)
        "now show the measurements"->mapOf("target" to "contextual_object","measurements" to true)
        "use centimeters instead"->mapOf("units" to "cm","target" to "current_measurements")
        "do not change the existing cube"->mapOf("constraint" to "preserve_cube","target" to "pending_action")
        "why did that fail"->mapOf("action" to "explain_last_error")
        "show me the drawing step by step"->mapOf("action" to "step_by_step","target" to "last_construction")
        else->emptyMap()
        }
    }
    fun annotate(source:String,input:RuhiExecutionResult):RuhiExecutionResult {
        val roles=parameters(source);if(roles.isEmpty())return input
        val result=if(input.status==GraphCommandStatus.APPLIED&&input.dialogue?.pending!=true)input.copy(dialogue=input.dialogue?.copy(suggestions=emptyList()))else input
        val frame=result.semanticFrame
        if(frame?.intent=="DIALOGUE_CONTEXT"&&roles.all{frame.parameters[it.key]==it.value})return result
        val native=if(frame!=null&&frame.intent!="DIALOGUE_CONTEXT")mapOf("resolved_action_intent" to frame.intent,"resolved_action_parameters" to frame.parameters)else emptyMap()
        return result.copy(semanticFrame=RuhiSemanticFrame("DIALOGUE_CONTEXT",result.command?.engine ?: frame?.engine ?: RuhiEngine.from(result.state.module) ?: RuhiEngine.GEOMETRY_2D,frame?.parameters.orEmpty()+native+roles))
    }
}
