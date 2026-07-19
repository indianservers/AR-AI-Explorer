package com.indianservers.aiexplorer.connectedlearning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ConnectedLearningColors(val background:Color,val panel:Color,val primary:Color,val secondary:Color,val success:Color,val warning:Color,val ink:Color,val muted:Color)

@Composable fun ConnectedLearningFeature(journey:ConnectedLearningJourney,colors:ConnectedLearningColors,onExit:()->Unit){
    var currentId by remember{mutableStateOf<String?>(null)};var mode by remember{mutableStateOf(LearningMode.Learn)};var completed by remember{mutableStateOf(setOf<String>())};var query by remember{mutableStateOf("")};var level by remember{mutableStateOf(ConnectedLearningLevel.Class12)}
    BackHandler{if(currentId!=null)currentId=null else onExit()}
    val current=currentId?.let{journey.concepts.find{c->c.id==it}}
    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(colors.primary.copy(.16f),colors.background),radius=1300f)).verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(current==null)journey.title else current.title,color=colors.primary,fontSize=25.sp,fontWeight=FontWeight.ExtraBold);Text(if(current==null)journey.description else "${journey.subject} connected concept · ${current.estimatedMinutes} min",color=colors.muted,fontSize=10.sp)};SmallButton(if(current==null)journey.subject else "Journey",colors){if(current==null)onExit()else currentId=null}}
        if(current==null){
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Search this ${journey.subject} journey")},singleLine=true)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){ConnectedLearningLevel.entries.filter{it in listOf(ConnectedLearningLevel.Class8,ConnectedLearningLevel.Class10,ConnectedLearningLevel.Class12,ConnectedLearningLevel.Undergraduate)}.forEach{item->Chip(item.label,item==level,colors){level=item}}}
            RecommendedNextEngine.recommend(journey,completed,level)?.let{recommended->Panel("Recommended next",colors){Text(recommended.concept.title,color=colors.success,fontWeight=FontWeight.Bold);Text(recommended.reason,color=colors.muted,fontSize=10.sp);SmallButton("Open",colors){currentId=recommended.concept.id}}}
            journey.concepts.filter{(query.isBlank()||it.title.contains(query,true)||it.description.contains(query,true))&&it.level.rank<=level.rank}.forEachIndexed{index,concept->
                val missing=concept.prerequisiteIds.filterNot(completed::contains);Card("${index+1}. ${concept.title}",concept.description,"${concept.level.label} · ${if(concept.id in completed)"evidence recorded" else if(missing.isEmpty())"ready" else "${missing.size} prerequisite to review"}",colors){currentId=concept.id}
            }
        }else{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){LearningMode.entries.forEach{item->Chip(item.name,item==mode,colors){mode=item}}}
            val missing=current.prerequisiteIds.filterNot(completed::contains);if(missing.isNotEmpty())Panel("Prerequisite review · continuing remains optional",colors){missing.forEach{id->Text(journey.concepts.find{it.id==id}?.title?:id,color=colors.warning)};Text("Review is recommended; content is not hard-locked.",color=colors.muted,fontSize=10.sp)}
            when(mode){
                LearningMode.Learn->{Panel("Why it matters",colors){Text(current.whyItMatters,color=colors.ink)};Panel("Learning objectives",colors){current.learningObjectives.forEach{Text("• $it",color=colors.ink)}};Panel("Core explanation",colors){Text(current.coreExplanation,color=colors.ink);Text("Advanced · ${current.advancedExplanation}",color=colors.secondary,fontSize=11.sp)};Panel("Misconception",colors){Text(current.misconception,color=colors.warning);Text(current.realWorldApplication,color=colors.muted,fontSize=10.sp)}}
                LearningMode.Explore->{ActivityDiagram(current,colors);current.activities.filter{it.kind in setOf(LearningActivityKind.Diagram,LearningActivityKind.Formula,LearningActivityKind.Calculator,LearningActivityKind.Interactive,LearningActivityKind.Simulation,LearningActivityKind.Experiment,LearningActivityKind.ProcessView,LearningActivityKind.Future3D)}.forEach{activity->InfoCard(activity.title,activity.description,"${activity.kind} · ${if(activity.available)"available foundation" else "planned, no fake controls"}",colors)}}
                LearningMode.Test->{current.activities.filter{it.kind in setOf(LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision)}.forEach{activity->InfoCard(activity.title,activity.description,activity.kind.name,colors)};Panel("Transparent completion evidence",colors){Text("Lesson + ${current.completionCriteria.practiceAttempts} practice attempts + ${current.completionCriteria.quizMinimumPercent}% quiz + interaction evidence.",color=colors.ink);SmallButton(if(current.id in completed)"Evidence recorded ✓" else "Record reference-journey evidence",colors){completed=completed+current.id}}}
            }
            Panel("Concept relationships",colors){val index=journey.conceptIds.indexOf(current.id);journey.concepts.getOrNull(index-1)?.let{c->Link("Previous",c,colors){currentId=c.id}};journey.concepts.getOrNull(index+1)?.let{c->Link("Next",c,colors){currentId=c.id}};current.prerequisiteIds.mapNotNull{journey.concepts.find{c->c.id==it}}.forEach{c->Link("Prerequisite",c,colors){currentId=c.id}};current.relatedConceptIds.mapNotNull{journey.concepts.find{c->c.id==it}}.forEach{c->Link("Related",c,colors){currentId=c.id}}}
        }
    }
}

@Composable private fun ActivityDiagram(concept:ConnectedConcept,c:ConnectedLearningColors){var focus by remember(concept.id){mutableIntStateOf(0)};val visible=concept.activities.take(8);Panel("Accessible 2D relationship view",c){Canvas(Modifier.fillMaxWidth().height(150.dp).semantics{contentDescription="${concept.title} relationship diagram showing lesson, visual, interaction and assessment links"}){val center=Offset(size.width/2,size.height/2);drawCircle(c.primary.copy(.2f),34f,center);val count=visible.size.coerceAtLeast(1);visible.forEachIndexed{i,activity->val angle=(Math.PI*2*i/count).toFloat();val point=Offset(center.x+kotlin.math.cos(angle)*size.width*.35f,center.y+kotlin.math.sin(angle)*size.height*.35f);drawLine(if(i==focus)c.primary else c.secondary.copy(.7f),center,point,if(i==focus)7f else 3f);drawCircle(if(i==focus)c.primary else if(activity.available)c.success else c.warning,if(i==focus)15f else 10f,point)}};Text("Focused relationship · ${visible.getOrNull(focus)?.title.orEmpty()}",color=c.secondary,fontWeight=FontWeight.Bold);Text(visible.getOrNull(focus)?.description.orEmpty(),color=c.muted,fontSize=10.sp);SmallButton("Focus next relationship",c){if(visible.isNotEmpty())focus=(focus+1)%visible.size}}}
@Composable private fun Link(label:String,concept:ConnectedConcept,c:ConnectedLearningColors,onClick:()->Unit){Text("$label · ${concept.title}",color=c.secondary,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth().clickable(onClick=onClick).padding(7.dp))}
@Composable private fun Card(title:String,body:String,meta:String,c:ConnectedLearningColors,onClick:()->Unit){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.panel).border(1.dp,c.primary.copy(.35f),RoundedCornerShape(15.dp)).clickable(onClick=onClick).padding(11.dp)){Text(title,color=c.primary,fontWeight=FontWeight.Bold);Text(body,color=c.ink,fontSize=11.sp);Text(meta,color=c.muted,fontSize=9.sp)}}
@Composable private fun InfoCard(title:String,body:String,meta:String,c:ConnectedLearningColors){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.panel).border(1.dp,c.primary.copy(.25f),RoundedCornerShape(15.dp)).padding(11.dp)){Text(title,color=c.primary,fontWeight=FontWeight.Bold);Text(body,color=c.ink,fontSize=11.sp);Text(meta,color=c.muted,fontSize=9.sp)}}
@Composable private fun Panel(title:String,c:ConnectedLearningColors,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.panel).border(1.dp,c.primary.copy(.25f),RoundedCornerShape(16.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,color=c.primary,fontWeight=FontWeight.Bold);content()}}
@Composable private fun Chip(label:String,selected:Boolean,c:ConnectedLearningColors,onClick:()->Unit){Text(label,color=if(selected)c.background else c.ink,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(13.dp)).background(if(selected)c.primary else c.panel).clickable(onClick=onClick).padding(horizontal=10.dp,vertical=9.dp))}
@Composable private fun SmallButton(label:String,c:ConnectedLearningColors,onClick:()->Unit){Button(onClick,colors=ButtonDefaults.buttonColors(containerColor=c.primary.copy(.25f),contentColor=c.ink),modifier=Modifier.heightIn(min=44.dp)){Text(label,fontSize=10.sp)}}
