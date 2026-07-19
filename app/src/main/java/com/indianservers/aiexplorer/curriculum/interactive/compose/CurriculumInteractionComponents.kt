package com.indianservers.aiexplorer.curriculum.interactive.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.curriculum.experience.DynamicExplanation
import com.indianservers.aiexplorer.curriculum.interaction.InteractiveActivityScreen
import com.indianservers.aiexplorer.curriculum.interaction.PlotPoint
import com.indianservers.aiexplorer.curriculum.interactive.*

@Composable fun InteractiveConceptBlock(activity:CurriculumInteractiveActivity,modifier:Modifier=Modifier){Box(modifier){InteractiveActivityScreen(activity.id,embedded=true)}}

@Composable fun InteractiveDiagramCanvas(description:String,highlighted:Boolean,points:List<PlotPoint>,modifier:Modifier=Modifier){
    Canvas(modifier.fillMaxWidth().height(220.dp).semantics{contentDescription=description}){
        if(points.isNotEmpty()){
            val minX=points.minOf{it.x};val maxX=points.maxOf{it.x}.let{v->if(v==minX)v+1 else v}
            val minY=points.minOf{it.y};val maxY=points.maxOf{it.y}.let{v->if(v==minY)v+1 else v}
            fun project(v:PlotPoint):Offset=Offset(((v.x-minX)/(maxX-minX)*size.width).toFloat(),(size.height-(v.y-minY)/(maxY-minY)*size.height).toFloat())
            points.zipWithNext().forEach{pair->drawLine(Color(0xFF1565C0),project(pair.first),project(pair.second),5f)}
            points.forEach{drawCircle(Color(0xFFE65100),7f,project(it))}
        }
        if(highlighted)drawRect(Color(0xFFFFA000),style=Stroke(8f))
    }
}

@Composable fun SimulationControlBar(modes:Set<InteractionMode>,selected:InteractionMode,onMode:(InteractionMode)->Unit,onReset:()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){modes.forEach{mode->FilterChip(selected==mode,{onMode(mode)},{Text(mode.name.lowercase().replaceFirstChar{it.uppercase()})})};OutlinedButton(onClick=onReset){Text("Reset")}}}

@Composable fun InteractiveGraphPanel(points:List<PlotPoint>,description:String){InteractiveDiagramCanvas(description,false,points)}

@Composable fun PredictionPromptCard(prompt:PredictionPrompt,onSubmit:(String)->Unit){var answer by remember(prompt.id){mutableStateOf("")};ElevatedCard{Column(Modifier.padding(12.dp)){Text("Predict: ${prompt.prompt}");OutlinedTextField(answer,{answer=it},label={Text("Your prediction")});Button({onSubmit(answer)},enabled=answer.isNotBlank()){Text("Lock prediction")}}}}

@Composable fun ObservationPanel(prompt:ObservationPrompt,onRecord:(String)->Unit){var observation by remember(prompt.id){mutableStateOf("")};Column{Text("Observe: ${prompt.prompt}");OutlinedTextField(observation,{observation=it},label={Text("Evidence")});Button({onRecord(observation)},enabled=observation.isNotBlank()){Text("Record observation")}}}

@Composable fun ChallengePanel(challenge:InteractiveChallenge,onCheck:()->Boolean){var result by remember(challenge.id){mutableStateOf<Boolean?>(null)};ElevatedCard{Column(Modifier.padding(12.dp)){Text(challenge.title,style=MaterialTheme.typography.titleMedium);Text(challenge.instruction);Button({result=onCheck()}){Text("Check target")};result?.let{Text(if(it)"Target achieved" else "Not yet—use the measurable outputs as evidence.")}}}}

@Composable fun DynamicExplanationPanel(value:DynamicExplanation){Column(Modifier.fillMaxWidth().padding(12.dp).semantics{contentDescription="${value.whatIsHappening}. ${value.whatChanged}. ${value.whyItChanged}"}){Text("What is happening? ${value.whatIsHappening}");Text("What changed? ${value.whatChanged}");Text("Why? ${value.whyItChanged}");Text("Rule: ${value.governingRule}");Text("Notice: ${value.notice}");Text("Model limit: ${value.simplification}")}}

@Composable fun DiagramLayerSelector(layers:List<com.indianservers.aiexplorer.curriculum.production.InteractiveDiagramLayer>,visible:Set<String>,onToggle:(String,Boolean)->Unit){Column{layers.forEach{layer->Row(Modifier.fillMaxWidth().clickable{onToggle(layer.id,layer.id !in visible)}.padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(layer.label);Switch(layer.id in visible,{onToggle(layer.id,it)})}}}}

@Composable fun FormulaVisualiser(formula:String,outputs:List<InteractiveOutputDefinition>,onVariableFocus:(String)->Unit){Column{Text(formula,style=MaterialTheme.typography.titleMedium);outputs.filter{it.variableId!=null}.forEach{output->AssistChip({onVariableFocus(output.variableId!!)},{Text("${output.label}: ${output.unit.orEmpty()}")})}}}

@Composable fun ContentVisualLinkText(text:String,link:ContentVisualLink,onAction:(ContentVisualAction)->Unit){Text(text,Modifier.clickable{onAction(link.action)}.semantics{contentDescription="$text. ${link.instructionalPrompt}"},color=MaterialTheme.colorScheme.primary)}

@Composable fun AccessibleVisualState(accessibility:InteractionAccessibilityDefinition,currentState:String){Text("${accessibility.summary} Current state: $currentState",Modifier.semantics{contentDescription="${accessibility.dynamicStateTemplate}. $currentState"})}
