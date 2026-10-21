package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

internal data class WorkshopSolution(val answer:String,val method:String,val selection:List<Int> = emptyList(),val assembly:List<String> = emptyList(),val marks:List<Int> = emptyList())

internal fun workshopSolution(round:WorkshopChallenge):WorkshopSolution=when(round.kind){
    WorkshopKind.Expression->WorkshopSolution(roundNumber(checkNotNull(round.source).answer),round.explanation,assembly=round.witness)
    WorkshopKind.Pieces->{
        val source=checkNotNull(round.source)
        val selected=if(round.witness.isNotEmpty())round.witness.map(String::toInt)else when(source.interaction){
            RoundInteraction.BuildPlaceValue->source.answer.toInt().toString().reversed().flatMapIndexed{i,char->List(char.digitToInt()){Math.pow(10.0,i.toDouble()).toInt()}}
            RoundInteraction.BuildShapes->{
                val memo=mutableMapOf<Pair<Int,Int>,List<Int>?>()
                fun search(count:Int,sides:Int):List<Int>?{
                    if(count==0)return if(sides==0)emptyList()else null
                    if(sides<0)return null
                    val key=count to sides;if(key in memo)return memo[key]
                    val found=source.pieces.filter{it>0}.firstNotNullOfOrNull{p->search(count-1,sides-p)?.let{listOf(p)+it}}
                    memo[key]=found;return found
                }
                List(source.requiredCurves){0}+checkNotNull(search(source.requiredPieces-source.requiredCurves,source.answer.toInt()))
            }
            else->(0 until (1 shl source.pieces.size)).asSequence().map{mask->source.pieces.filterIndexed{i,_->mask and (1 shl i)!=0}}.first(source::acceptsPieces)
        }
        check(source.acceptsPieces(selected))
        val unit=round.parameters.firstOrNull() ?: 1
        val answer=when{source.interaction==RoundInteraction.BuildShapes->"${source.requiredPieces} pieces, ${roundNumber(source.answer)} sides, ${source.requiredCurves} curved"
            source.capacity>0->"Maximum value ${roundNumber(source.answer)}"
            unit>1->"${roundNumber(source.answer)}/$unit of a whole"
            else->"Total ${roundNumber(source.answer)} using ${selected.size} pieces"}
        WorkshopSolution(answer,round.explanation+" One valid selection: "+selected.joinToString{if(unit>1)"$it/$unit"else "$it"},selection=selected)
    }
    WorkshopKind.Navigation->WorkshopSolution("Destination (${round.parameters[2]},${round.parameters[3]})",round.explanation,selection=round.witness.map(String::toInt))
    WorkshopKind.Mixture->WorkshopSolution("Blue ${round.witness[0]} ml; red ${round.witness[1]} ml",round.explanation,selection=round.witness.map(String::toInt))
    WorkshopKind.Grid->WorkshopSolution("${round.witness[0]} rows × ${round.witness[1]} columns",round.explanation,selection=round.witness.map(String::toInt))
    WorkshopKind.Factors->WorkshopSolution(round.witness.joinToString(" × ")+" = ${round.parameters[0]}",round.explanation,selection=round.witness.map(String::toInt))
    WorkshopKind.Balance->WorkshopSolution("x = ${roundNumber(round.numbers[4])}",round.explanation,assembly=round.witness)
    WorkshopKind.Machine->WorkshopSolution(round.witness.chunked(2).joinToString(" → "){if(it[0]=="²")"square"else "${it[0]} ${it[1]}"},round.explanation,assembly=round.witness)
    WorkshopKind.Configurations->WorkshopSolution("${round.parameters[3]} distinct formations",round.explanation+"\n"+round.witness.joinToString(" · "){team->team.split(',').joinToString(""){('A'.code+it.toInt()).toChar().toString()}},assembly=round.witness)
    WorkshopKind.Sort->{val split=round.witness.indexOf("|");val order=round.witness.take(split).map(String::toInt);val marks=round.witness.drop(split+1).map(String::toInt)
        WorkshopSolution(order.joinToString{roundNumber(round.numbers[it])},round.explanation,selection=order,marks=marks)}
}

internal fun WorkshopChallenge.attemptFeedback(selection:List<Int>,assembly:List<String>,marks:List<Int> = emptyList()):String {
    if(accepts(selection,assembly,marks))return "Construction verified"
    return when(kind){
        WorkshopKind.Expression->{val actual=evaluateMathAssembly(assembly);val requiredOperations=if(mastery>0)2 else 1
            when{actual==null->"The expression is incomplete or undefined. Check operators, brackets and divisors."
                actual.operations < requiredOperations ->"Show a calculation using operation tiles, not just the answer."
                else->"Your calculation gives ${roundNumber(actual.value)}. Compare it with the correct answer below."}}
        WorkshopKind.Mixture->if(selection.size!=2 || selection.sum()!=parameters[2])"The two volumes must total ${parameters[2]} ml."else "The total is right, but the blue:red ratio must be ${parameters[0]}:${parameters[1]}."
        WorkshopKind.Grid->if(selection.size!=2 || selection[0]*selection[1]!=parameters[0])"Rows × columns must be ${parameters[0]}."else "The area is right. Check the additional row, column, ratio or perimeter clue."
        WorkshopKind.Balance->"Keep equal operations on both sides until the left is exactly one x and no constant."
        WorkshopKind.Machine->{val mismatch=examples.firstOrNull{(input,expected)->machineOutput(input,assembly)?.let{abs(it-expected)<1e-7}!=true}
            mismatch?.let{"For input ${roundNumber(it.first)}, the output must be ${roundNumber(it.second)}. Check block order and selected values."} ?: "Use at least one operation block."}
        WorkshopKind.Factors->"Use prime cores whose product equals ${parameters[0]}; repeated primes may be needed."
        WorkshopKind.Navigation->"The route must reach (${parameters[2]},${parameters[3]}) within ${parameters[4]} moves."
        WorkshopKind.Sort->if(selection.size!=numbers.size)"Place every reading exactly once."else "Check the ordering and which positions are marked."
        WorkshopKind.Configurations->if(assembly.any{key->!validConfiguration(key.split(',').map{it.toIntOrNull() ?: -1})})"At least one formation violates a stated condition."else "You need ${parameters[3]} distinct valid formations; your collection has ${assembly.distinct().size}."
        WorkshopKind.Pieces->"Check the target, available stock and required piece count. The correct construction is shown below."
    }
}

@Composable internal fun MathSolutionFeedback(round:WorkshopChallenge,gameId:String?=null){
    val solution=remember(round.signature){workshopSolution(round)}
    var expanded by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().glossySurface(15,Color(0xFFEAF7EF)).padding(10.dp).semantics{contentDescription="Verified solution"}){
        Text("Correct answer: ${solution.answer}",color=Color(0xFF17623F),fontSize=13.sp,fontWeight=FontWeight.Bold)
        TextButton(onClick={expanded=true},contentPadding=PaddingValues(0.dp)){Text("View worked solution",fontSize=12.sp)}
    }
    if(expanded)AlertDialog(onDismissRequest={expanded=false},title={Text("Worked solution")},
        text={Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Answer: ${solution.answer}",fontWeight=FontWeight.Bold)
            val lesson=gameId?.let{mathTeachingPlan(it,round.prompt)}
            if(lesson!=null){Text("1 · Choose the model",fontWeight=FontWeight.Bold);Text(lesson.model)}
            Text("2 · Follow the calculation",fontWeight=FontWeight.Bold)
            Text(coloredMathText(solution.method))
            if(lesson!=null){Text("3 · Verify independently",fontWeight=FontWeight.Bold);Text(lesson.check);Text("Think about it",fontWeight=FontWeight.Bold);Text(lesson.reflection)}
        }},
        confirmButton={TextButton(onClick={expanded=false}){Text("Close")}})
}
