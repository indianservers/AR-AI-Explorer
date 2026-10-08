package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
internal fun MathWorkshopBoard(game:MathsGame,round:WorkshopChallenge,solved:Boolean,onAttempt:(Boolean)->Unit={},onSolved:()->Unit) {
    var selection by rememberSaveable{mutableStateOf(arrayListOf<Int>())}
    var assembly by rememberSaveable{mutableStateOf(arrayListOf<String>())}
    var marks by rememberSaveable{mutableStateOf(arrayListOf<Int>())}
    var custom by rememberSaveable{mutableStateOf("")}
    var valueEditor by rememberSaveable{mutableStateOf(false)}
    var status by rememberSaveable{mutableStateOf("Build your solution on the board")}
    fun add(value:Int){if(!solved){selection=ArrayList(selection+value);status="Construction updated"}}
    fun addToken(token:String){if(!solved && assembly.size<80){assembly=ArrayList(assembly+token);status="Method updated"}}
    fun check(){val passed=round.accepts(selection,assembly,marks);onAttempt(passed);if(passed){status="Construction verified";onSolved()}else status="Not complete yet. Check every condition and try another move."}
    WorkshopScene(game,round,selection,assembly)
    when(round.kind) {
        WorkshopKind.Expression -> {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                items(assembly.indices.toList()){index->WorkshopChip(assembly[index],game.accent,enabled=!solved,description="Remove token ${index+1}"){assembly=ArrayList(assembly.filterIndexed{i,_->i!=index})}}
            }
            Text("DRAG CORES UP TO THE BOARD • TAP TO PLACE",color=GameMuted,fontSize=10.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                items(round.numbers.distinct()){number->WorkshopChip(roundNumber(number),game.accent,enabled=!solved,description="Place number ${roundNumber(number)}"){addToken(roundNumber(number))}}
                item{WorkshopChip("Custom core",game.accent,enabled=!solved){valueEditor=!valueEditor}}
            }
            if(valueEditor) {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    items(listOf("0","1","2","3","4","5","6","7","8","9",".","−","⌫")){digit->
                        WorkshopChip(digit,game.accent,enabled=!solved){custom=when(digit){"⌫"->custom.dropLast(1);"−"->if(custom.startsWith('-'))custom.drop(1) else "-$custom";else->if(custom.length<14)custom+digit else custom}}
                    }
                }
                TextButton(onClick={if(custom.toDoubleOrNull()?.isFinite()==true){addToken(custom);custom="";valueEditor=false}},enabled=!solved){Text("Place ${custom.ifBlank{"number"}}",color=game.accent)}
            }
            LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                items(listOf("+","−","×","÷","(",")","^","√","C","P","!","sin","cos","tan")){token->WorkshopChip(token,GameGold,enabled=!solved,description="Place operation $token"){addToken(token)}}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick={status=evaluateMathAssembly(assembly)?.let{"Your method produces ${roundNumber(it.value)}"}?:"Complete the expression: check brackets and missing operators."},enabled=!solved){Text("Run")}
                Button(onClick={assembly=ArrayList(assembly.dropLast(1))},enabled=!solved && assembly.isNotEmpty()){Text("Undo")}
                Button(onClick={check()},enabled=!solved && assembly.isNotEmpty()){Text("Check")}
            }
        }
        WorkshopKind.Pieces -> {
            val source=round.source!!
            val shapes=source.interaction==RoundInteraction.BuildShapes
            val unit=round.parameters.firstOrNull()?:1
            Text(if(shapes)"${selection.size} pieces • ${selection.sum()} sides • ${selection.count{it==0}} curved"
                else if(source.capacity>0)"Value ${selection.sum()} • ${selection.sumOf{source.weights[it]?:0}}/${source.capacity} kg"
                else if(unit>1)"Filled ${selection.sum()}/$unit • ${selection.size} tiles"
                else "Total ${selection.sum()} • ${selection.size} cores",color=GameInk,fontSize=14.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                items(source.pieces.distinct()){value->
                    val available=if(shapes)selection.size<source.requiredPieces else selection.count{it==value}<source.pieces.count{it==value}
                    val label=if(shapes)when(value){0->"○";3->"△";4->"□";5->"⬠";else->"⬡"}else if(source.capacity>0)"$value / ${source.weights[value]}kg"
                        else if(unit>1)"$value/$unit" else "$value"
                    WorkshopChip(label,game.accent,enabled=available && !solved,description=if(shapes)"Add piece with $value straight sides" else "Add $label core"){add(value)}
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick={selection=ArrayList(selection.dropLast(1))},enabled=!solved && selection.isNotEmpty()){Text("Undo")}
                Button(onClick={selection=arrayListOf()},enabled=!solved && selection.isNotEmpty()){Text("Clear")}
                Button(onClick={check()},enabled=!solved && selection.isNotEmpty()){Text("Check build")}
            }
        }
        WorkshopKind.Navigation -> {
            val x=round.parameters[0]+selection.sumOf{round.moves[it].first};val y=round.parameters[1]+selection.sumOf{round.moves[it].second}
            Text("Ship ($x,$y) • ${selection.size}/${round.parameters[4]} moves",color=GameInk,fontSize=14.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                round.moves.forEachIndexed{index,move->WorkshopChip("(${move.first},${move.second})",game.accent,Modifier.weight(1f),!solved && selection.size<round.parameters[4],description="Move by (${move.first},${move.second})"){add(index)}}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick={selection=ArrayList(selection.dropLast(1))},enabled=!solved && selection.isNotEmpty()){Text("Undo move")}
                Button(onClick={check()},enabled=!solved){Text("Land ship")}
            }
        }
        WorkshopKind.Balance,WorkshopKind.Machine -> {
            val balance=round.kind==WorkshopKind.Balance
            val numbers=if(balance)round.numbers.take(4).distinct() else round.numbers.distinct()
            var operand by rememberSaveable{mutableStateOf(roundNumber(numbers.first()))}
            if(balance) {
                val state=round.balanceState(assembly)!!
                Text("${roundNumber(state[0])}x + ${roundNumber(state[1])} = ${roundNumber(state[2])}x + ${roundNumber(state[3])}",color=GameInk,fontSize=16.sp)
            } else Text(round.examples.joinToString(" • "){(input,expected)->"${roundNumber(input)} → ${roundNumber(expected)}"},color=GameInk,fontSize=14.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {items(numbers){number->WorkshopChip(roundNumber(number),game.accent,enabled=!solved,description="Select core ${roundNumber(number)}"){operand=roundNumber(number)}}}
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("Core $operand",color=GameInk,fontSize=12.sp,modifier=Modifier.weight(1f))
                TextButton(onClick={operand=roundNumber((operand.toDoubleOrNull()?:0.0)-1)},enabled=!solved){Text("−1")}
                TextButton(onClick={operand=roundNumber((operand.toDoubleOrNull()?:0.0)+1)},enabled=!solved){Text("+1")}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                (if(balance)listOf("−","+","÷","−x") else listOf("+","×","²")).forEach{operator->
                    WorkshopChip(if(operator=="−x")"− core·x" else operator,GameGold,Modifier.weight(1f),!solved && assembly.size<if(balance)32 else 12,description=if(balance)"Apply $operator to both sides" else "Add $operator block"){
                        val next=ArrayList(assembly+listOf(operator,if(operator=="²")"2" else operand))
                        if(!balance || round.balanceState(next)!=null)assembly=next else status="That operation is undefined. Use a non-zero divisor."
                    }
                }
            }
            Text("Steps: "+assembly.chunked(2).joinToString(" → "){it.joinToString(" ")}.ifEmpty{"empty"},color=GameMuted,fontSize=12.sp,maxLines=2)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick={assembly=ArrayList(assembly.dropLast(2))},enabled=!solved && assembly.isNotEmpty()){Text("Undo block")}
                Button(onClick={check()},enabled=!solved){Text(if(balance)"Unlock" else "Test circuit")}
            }
        }
        WorkshopKind.Mixture,WorkshopKind.Grid -> {
            val mix=round.kind==WorkshopKind.Mixture
            if(selection.size!=2) SideEffect{selection=arrayListOf(if(mix)0 else 1,if(mix)0 else 1)}
            val current=if(selection.size==2)selection else listOf(if(mix)0 else 1,if(mix)0 else 1)
            current.forEachIndexed{index,value->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("${if(mix)if(index==0)"Blue" else "Red" else if(index==0)"Rows" else "Columns"}: $value${if(mix)" ml" else ""}",color=GameInk,fontSize=14.sp,modifier=Modifier.weight(1f))
                    listOf(-1,1,if(mix)5 else 3).forEach{delta->
                        WorkshopChip(if(delta>0)"+$delta" else "$delta",game.accent,enabled=!solved,description="${if(delta>0) "Increase" else "Decrease"} ${if(mix) if(index==0) "blue volume" else "red volume" else if(index==0) "rows" else "columns"} by ${abs(delta)}"){val next=current.toMutableList();next[index]=(value+delta).coerceIn(if(mix)0 else 1,if(mix)round.parameters[2] else 40);selection=ArrayList(next)}
                    }
                }
            }
            Text(if(mix)"Batch: ${current.sum()}/${round.parameters[2]} ml" else "Area: ${current[0]*current[1]} • perimeter: ${2*current.sum()}",color=GameMuted,fontSize=12.sp)
            Button(onClick={check()},enabled=!solved){Text(if(mix)"Mix batch" else "Launch array")}
        }
        WorkshopKind.Sort -> {
            Text("Place observations; tap a placed slot to mark it",color=GameMuted,fontSize=11.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                items(selection.indices.toList()){position->WorkshopChip(roundNumber(round.numbers[selection[position]]),if(position in marks)GameGold else game.accent,enabled=!solved,description="Mark slot ${position+1}"){marks=ArrayList(if(position in marks)marks-position else marks+position)}}
            }
            LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                items(round.numbers.indices.toList()){index->WorkshopChip(roundNumber(round.numbers[index]),game.accent,enabled=index !in selection && !solved,description="Place observation ${index+1}: ${roundNumber(round.numbers[index])}"){add(index)}}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick={selection=ArrayList(selection.dropLast(1));marks=ArrayList(marks.filter{it<selection.size})},enabled=!solved && selection.isNotEmpty()){Text("Undo slot")}
                Button(onClick={check()},enabled=!solved){Text("Verify evidence")}
            }
        }
        WorkshopKind.Factors -> {
            Text("Product: ${selection.fold(1L){product,value->product*value}} • ${selection.size} prime cores",color=GameInk,fontSize=14.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {items(listOf(2,3,5,7,11,13)){prime->WorkshopChip("$prime",game.accent,enabled=!solved && selection.size<12,description="Add prime $prime"){add(prime)}}}
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Button(onClick={selection=ArrayList(selection.dropLast(1))},enabled=!solved && selection.isNotEmpty()){Text("Remove core")}
                Button(onClick={check()},enabled=!solved){Text("Unlock crypt")}
            }
        }
        WorkshopKind.Configurations -> {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {items((0 until round.parameters[0]).toList()){index->WorkshopChip("Robot ${('A'.code+index).toChar()}"+(round.numbers.getOrNull(index)?.let{" · ${roundNumber(it)}"}?:""),game.accent,enabled=!solved && index !in selection && selection.size<round.parameters[1],description="Choose robot ${('A'.code+index).toChar()}"){add(index)}}}
            Text("Collected ${assembly.size} distinct formations",color=GameInk,fontSize=14.sp)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {items(assembly){configuration->WorkshopChip(configuration.split(',').joinToString(""){('A'.code+it.toInt()).toChar().toString()},game.accent,enabled=false){}}}
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Button(onClick={selection=ArrayList(selection.dropLast(1))},enabled=!solved && selection.isNotEmpty()){Text("Undo")}
                Button(onClick={val key=(if(round.parameters[2]==0)selection.sorted() else selection).joinToString(",");if(key !in assembly){assembly=ArrayList(assembly+key);selection=arrayListOf()}else status="Formation already collected. Try another."},enabled=!solved && selection.size==round.parameters[1]){Text("Collect")}
                Button(onClick={check()},enabled=!solved){Text("Check")}
            }
        }
    }
    Text(status,color=if(solved)GameGreen else GameMuted,fontSize=11.sp)
}

@Composable
private fun WorkshopChip(label:String,color:Color,modifier:Modifier=Modifier,enabled:Boolean=true,description:String=label,onPlace:()->Unit) {
    var dragX by remember{mutableFloatStateOf(0f)};var dragY by remember{mutableFloatStateOf(0f)}
    val place by rememberUpdatedState(onPlace)
    Box(modifier.heightIn(min=44.dp).graphicsLayer{translationX=dragX;translationY=dragY}
        .clip(RoundedCornerShape(13.dp)).background(Brush.verticalGradient(listOf(color.copy(if(enabled).35f else .08f),GamePanel)))
        .border(1.dp,color.copy(if(enabled).65f else .12f),RoundedCornerShape(13.dp))
        .semantics{contentDescription=description}
        .pointerInput(enabled,label){if(enabled)detectDragGestures(onDragEnd={val placed=dragY < -24;dragX=0f;dragY=0f;if(placed)place()},onDragCancel={dragX=0f;dragY=0f}){change,amount->change.consume();dragX+=amount.x;dragY+=amount.y}}
        .clickable(enabled=enabled,onClick=onPlace).padding(horizontal=12.dp,vertical=7.dp),contentAlignment=Alignment.Center) {
        Text(label,color=if(enabled)GameInk else GameMuted.copy(.45f),fontSize=14.sp,maxLines=1)
    }
}

@Composable
private fun WorkshopScene(game:MathsGame,round:WorkshopChallenge,selection:List<Int>,assembly:List<String>) {
    val glow by animateFloatAsState(if(selection.isEmpty() && assembly.isEmpty()).25f else .65f,label="workshop glow")
    Canvas(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(20.dp))
        .background(Brush.radialGradient(listOf(game.accent.copy(.22f),GamePanel))).border(1.dp,game.accent.copy(.45f),RoundedCornerShape(20.dp))
        .semantics{contentDescription="Interactive ${round.title} workspace"}) {
        val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textAlign=android.graphics.Paint.Align.CENTER;textSize=11.sp.toPx()}
        fun text(value:String,x:Float,y:Float){drawContext.canvas.nativeCanvas.drawText(value,x,y,paint)}
        val w=size.width;val h=size.height
        when(round.kind) {
            WorkshopKind.Navigation -> {
                val p=round.parameters;var x=p[0];var y=p[1];val points=mutableListOf(x to y)
                selection.forEach{x+=round.moves[it].first;y+=round.moves[it].second;points+=x to y}
                val extent=maxOf(p.take(4).maxOf{abs(it)},points.maxOf{max(abs(it.first),abs(it.second))},4)+2
                fun position(point:Pair<Int,Int>)=Offset(w/2+point.first*w/(2*extent),h/2-point.second*h/(2*extent))
                for(i in -extent..extent){val xx=w/2+i*w/(2*extent);val yy=h/2+i*h/(2*extent);drawLine(GameMuted.copy(.1f),Offset(xx,0f),Offset(xx,h));drawLine(GameMuted.copy(.1f),Offset(0f,yy),Offset(w,yy))}
                points.zipWithNext().forEach{(a,b)->drawLine(game.accent,position(a),position(b),3.dp.toPx())}
                val target=position(p[2] to p[3]);drawCircle(GameGold,8.dp.toPx(),target,style=Stroke(2.dp.toPx()))
                val ship=position(x to y);drawCircle(game.accent,6.dp.toPx(),ship);text("★",target.x,target.y+4.dp.toPx())
            }
            WorkshopKind.Mixture -> {
                val total=round.parameters[2].toFloat()
                repeat(2){i->val x=w*(if(i==0).26f else .67f);val bw=w*.12f;val top=h*.2f;val height=h*.65f
                    drawRoundRect(GameInk.copy(.1f),Offset(x,top),Size(bw,height),androidx.compose.ui.geometry.CornerRadius(7f,7f),style=Stroke(2.dp.toPx()))
                    val fill=((selection.getOrNull(i)?:0)/total).coerceIn(0f,1f)*height
                    drawRoundRect(if(i==0)GameBlue else GameRed,Offset(x,top+height-fill),Size(bw,fill),androidx.compose.ui.geometry.CornerRadius(5f,5f))
                    text(if(i==0)"BLUE" else "RED",x+bw/2,h*.15f)}
            }
            WorkshopKind.Grid -> {
                val rows=selection.getOrNull(0)?:1;val columns=selection.getOrNull(1)?:1
                repeat(rows){row->repeat(columns){col->drawCircle(game.accent.copy(glow+.2f),min(w/(columns+1),h/(rows+1))*.23f,Offset((col+1)*w/(columns+1),(row+1)*h/(rows+1)))}}
            }
            WorkshopKind.Sort -> {
                val max=round.numbers.maxOrNull()?:1.0;val slot=w/maxOf(round.numbers.size,1)
                selection.forEachIndexed{i,index->val value=round.numbers[index];val height=(value/max*(h*.6)).toFloat();drawRoundRect(game.accent,Offset(i*slot+4,h*.8f-height),Size(slot-8,height),androidx.compose.ui.geometry.CornerRadius(4f,4f));text(roundNumber(value),i*slot+slot/2,h*.95f)}
            }
            WorkshopKind.Pieces,WorkshopKind.Factors,WorkshopKind.Configurations -> {
                val values=selection.takeLast(12);val slot=w/(maxOf(values.size,1)+1)
                values.forEachIndexed{i,value->val center=Offset(slot*(i+1),h*.5f);val radius=min(slot*.35f,h*.27f)
                    if(round.source?.interaction==RoundInteraction.BuildShapes) {
                        if(value==0)drawCircle(game.accent,radius,center,style=Stroke(3.dp.toPx())) else {
                            val path=Path();repeat(value){j->val angle=-PI/2+j*2*PI/value;val point=center+Offset((cos(angle)*radius).toFloat(),(sin(angle)*radius).toFloat());if(j==0)path.moveTo(point.x,point.y)else path.lineTo(point.x,point.y)};path.close();drawPath(path,game.accent,style=Stroke(2.dp.toPx()))
                        }
                    } else {drawRoundRect(game.accent.copy(glow),center-Offset(radius,radius),Size(radius*2,radius*2),androidx.compose.ui.geometry.CornerRadius(8f,8f));text(if(round.kind==WorkshopKind.Configurations)('A'.code+value).toChar().toString() else "$value",center.x,center.y+4.dp.toPx())}
                    if(i>0)drawLine(game.accent.copy(.5f),Offset(slot*i+radius,h/2),Offset(center.x-radius,h/2),2.dp.toPx())
                }
                if(values.isEmpty())text("Drag or tap a core to begin",w/2,h/2)
            }
            WorkshopKind.Balance -> {
                val s=round.balanceState(assembly)!!;drawLine(game.accent,Offset(w*.25f,h*.35f),Offset(w*.75f,h*.35f),3.dp.toPx())
                drawLine(GameMuted,Offset(w/2,h*.35f),Offset(w/2,h*.8f),3.dp.toPx())
                text("${roundNumber(s[0])}x + ${roundNumber(s[1])}",w*.25f,h*.75f);text("${roundNumber(s[2])}x + ${roundNumber(s[3])}",w*.75f,h*.75f)
            }
            WorkshopKind.Machine -> {
                round.examples.forEachIndexed{i,(input,_)->val x=w*(i+1)/4;text(roundNumber(input),x,h*.25f);drawLine(game.accent,Offset(x,h*.35f),Offset(x,h*.6f),3.dp.toPx());text(round.machineOutput(input,assembly)?.let(::roundNumber)?:"?",x,h*.82f)}
            }
            WorkshopKind.Expression -> {
                repeat(6){i->drawCircle(game.accent.copy(.14f),h*(.15f+i*.05f),Offset(w/2,h/2),style=Stroke(2.dp.toPx()))}
                text(if(assembly.isEmpty())"ASSEMBLE YOUR METHOD" else assembly.takeLast(12).joinToString(" "),w/2,h*.55f)
            }
        }
    }
}
