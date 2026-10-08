package com.indianservers.aiexplorer.gamifymaths

import kotlin.math.abs
import kotlin.random.Random

internal enum class WorkshopKind { Expression, Pieces, Navigation, Balance, Mixture, Grid, Machine, Sort, Factors, Configurations }
internal data class WorkshopChallenge(
    val kind:WorkshopKind,
    val title:String,
    val prompt:String,
    val hint:String,
    val explanation:String,
    val source:GeneratedRound?=null,
    val parameters:List<Int> = emptyList(),
    val numbers:List<Double> = emptyList(),
    val moves:List<Pair<Int,Int>> = emptyList(),
    val examples:List<Pair<Double,Double>> = emptyList(),
    val witness:List<String> = emptyList(),
    val mastery:Int=0,
) {
    val signature:String get()="$kind|$prompt|$parameters|$numbers|$moves|$examples|${source?.signature}"
    fun accepts(selection:List<Int>, assembly:List<String> = emptyList(), marks:List<Int> = emptyList()):Boolean = when(kind) {
        WorkshopKind.Expression -> evaluateMathAssembly(assembly)?.let {
            it.operations >= (if(mastery>0) 2 else 1) && source!!.accepts(roundNumber(it.value))
        } ?: false
        WorkshopKind.Pieces -> source!!.acceptsPieces(selection)
        WorkshopKind.Navigation -> {
            var x=parameters[0];var y=parameters[1]
            val valid=selection.all{index->if(index !in moves.indices) false else {x+=moves[index].first;y+=moves[index].second;true}}
            valid && selection.size<=parameters[4] && x==parameters[2] && y==parameters[3]
        }
        WorkshopKind.Balance -> {
            val state=balanceState(assembly)
            state!=null && abs(state[0]-1)<1e-8 && abs(state[1])<1e-8 && abs(state[2])<1e-8 && abs(state[3]-numbers[4])<1e-8
        }
        WorkshopKind.Mixture -> selection.size==2 && selection.all{it>=0} && selection.sum()==parameters[2] && selection[0]*parameters[1]==selection[1]*parameters[0]
        WorkshopKind.Grid -> selection.size==2 && selection.all{it in 1..40} && selection[0]*selection[1]==parameters[0] &&
            (parameters[1]==0 || selection[0]==parameters[1]) && (parameters[2]==0 || 2*selection.sum()==parameters[2]) &&
            (mastery!=2 || selection[0]-selection[1]==parameters[3])
        WorkshopKind.Machine -> examples.all { (input,output)->machineOutput(input,assembly)?.let{abs(it-output)<1e-7}==true } && assembly.size>=2
        WorkshopKind.Sort -> {
            val values=selection.mapNotNull{numbers.getOrNull(it)}
            val expected=if(parameters[0]==1) numbers.sortedDescending() else numbers.sorted()
            val positions=when(parameters[1]) {
                1->expected.indices.filter{index->expected.count{it==expected[index]}==expected.groupingBy{it}.eachCount().values.max()}
                2->listOf(0,expected.lastIndex)
                else->listOf((expected.size-1)/2,expected.size/2).distinct()
            }
            selection.size==numbers.size && selection.distinct().size==numbers.size && values==expected && marks.sorted()==positions
        }
        WorkshopKind.Factors -> selection.isNotEmpty() && selection.all{it in listOf(2,3,5,7,11,13)} && selection.fold(1L){product,value->product*value}==parameters[0].toLong()
        WorkshopKind.Configurations -> {
            val configurations=assembly.map{key->key.split(',').map{it.toIntOrNull() ?: -1}}
            configurations.all{it.size==parameters[1] && it.distinct().size==it.size && it.all{value->value in 0 until parameters[0]}} &&
                (mastery==0 || configurations.all{team->team.sumOf{numbers[it].toInt()}%parameters[4]==parameters[5]}) &&
                configurations.map{if(parameters[2]==0) it.sorted() else it}.distinct().size==parameters[3] && configurations.size==parameters[3]
        }
    }
    fun balanceState(assembly:List<String>):List<Double>? {
        val state=numbers.take(4).toMutableList()
        if(state.size!=4 || assembly.size%2!=0) return null
        for(step in assembly.chunked(2)) {
            val amount=step[1].toDoubleOrNull() ?: return null
            when(step[0]) {
                "−"->{state[1]-=amount;state[3]-=amount}
                "+"->{state[1]+=amount;state[3]+=amount}
                "÷"->{if(amount==0.0)return null;state.indices.forEach{state[it]/=amount}}
                "−x"->{state[0]-=amount;state[2]-=amount}
                else->return null
            }
        }
        return state.takeIf{it.all(Double::isFinite)}
    }
    fun machineOutput(input:Double, assembly:List<String>):Double? {
        if(assembly.size%2!=0 || assembly.size>12)return null
        var result=input
        for(step in assembly.chunked(2)) {
            val amount=step[1].toDoubleOrNull() ?: return null
            result=when(step[0]){"+"->result+amount;"×"->result*amount;"²"->result*result;else->return null}
        }
        return result.takeIf(Double::isFinite)
    }
}

internal fun generateWorkshopChallenge(id:String, level:Int, difficulty:Int, seed:Long, focus:Int = -1):WorkshopChallenge {
    require(level in 0 until GeneratedLevelCount && difficulty in 0..3)
    val random=Random(seed)
    val mastery=if(level>=120) level-119 else 0
    val tier=(difficulty+level/30).coerceAtMost(3)
    fun n(min:Int=2,max:Int=10+tier*5)=random.nextInt(min,max+1)
    fun challenge(kind:WorkshopKind,title:String,prompt:String,hint:String,explanation:String,parameters:List<Int> = emptyList(),numbers:List<Double> = emptyList(),
                  moves:List<Pair<Int,Int>> = emptyList(),examples:List<Pair<Double,Double>> = emptyList(),witness:List<String> = emptyList(),source:GeneratedRound?=null) =
        WorkshopChallenge(kind,title,prompt,hint,explanation,source,parameters,numbers,moves,examples,witness,mastery)
    val variant=if(focus>=0 && mastery==0) focus else level
    if(id in listOf("vectors","integer-expedition","matrix-mission")) {
        val startX=n(-3,3);val startY=if(id=="integer-expedition") 0 else n(-3,3)
        val span=if(mastery==2) 5 else 3
        val first=if(id=="integer-expedition") (if(mastery==2) 10 else 5) to 0 else if(mastery>0 || id=="matrix-mission") n(1,span) to n(1,span) else 1 to 0
        var second=if(id=="integer-expedition") (if(mastery==2) 3 else 1) to 0 else if(mastery>0 || id=="matrix-mission") n(-span,-1) to n(1,span) else 0 to 1
        if(id!="integer-expedition" && first.first*second.second==first.second*second.first)second=0 to 1
        val moves=listOf(first,second,-first.first to -first.second,-second.first to -second.second)
        val a=n(1,if(mastery==2) 7 else if(mastery==1) 5 else 3);val b=n(1,if(mastery==2) 7 else if(mastery==1) 5 else 3)
        val sign=if(random.nextBoolean()) 1 else -1
        val x=startX+a*first.first+sign*b*second.first;val y=startY+a*first.second+sign*b*second.second
        val witness=List(a){"0"}+List(b){if(sign>0) "1" else "3"}
        return challenge(WorkshopKind.Navigation,"Vector flight", "Pilot from ($startX,$startY) to ($x,$y) in at most ${a+b} moves. Each arrow applies its labelled vector.",
            "Plan the displacement before moving. Undo a move to explore another route.","Your programmed vector moves reach the destination within the move budget. Every displacement contributes to the final coordinates.",
            listOf(startX,startY,x,y,a+b),moves=moves,witness=witness)
    }
    if(id=="balance") {
        val x=n(2,if(mastery>0) 25 else 12)*(if(mastery==2 && random.nextBoolean()) -1 else 1);val left=n(2,if(mastery>0) 15 else 7);val right=if(mastery>0 || variant%3==2)n(1,left-1) else 0
        val offset=n(2,20);val result=(left-right)*x+offset
        return challenge(WorkshopKind.Balance,"Balance workshop","Isolate x. Apply every operation to BOTH plates: ${left}x + $offset = ${if(right==0) "" else "${right}x + "}$result.",
            "Remove any x terms on the right, remove the constant, then scale both plates equally.","Equal operations preserve the original balance. With one x alone on the left, the right plate shows its generated value.",
            numbers=listOf(left.toDouble(),offset.toDouble(),right.toDouble(),result.toDouble(),x.toDouble()),
            witness=(if(right>0) listOf("−x",right.toString()) else emptyList())+listOf("−",offset.toString(),"÷",(left-right).toString()))
    }
    if(id in listOf("potions","ratio-rangers")) {
        val a=n(1,if(mastery==2) 20 else if(mastery==1) 12 else 5);val b=n(1,if(mastery==2) 20 else if(mastery==1) 12 else 5);val scale=n(2,if(mastery==2) 12 else if(mastery==1) 9 else 4)
        return challenge(WorkshopKind.Mixture,"Mixture lab","Pour ${(a+b)*scale} ml in total. Blue:red must be $a:$b. Adjust BOTH flasks to create a valid batch.",
            "One ratio batch has ${a+b} parts. Scale both ingredients by the same factor.","The mixed volumes satisfy both the total volume and the requested ratio. Matching only one condition would not preserve the recipe.",
            listOf(a,b,(a+b)*scale),witness=listOf((a*scale).toString(),(b*scale).toString()))
    }
    if(id=="times-table") {
        val rows=n(2,if(mastery>0) 18 else 9);val cols=n(2,if(mastery>0) 18 else 9)
        val perimeter=if(mastery==1) 2*(rows+cols) else 0
        val difference=if(mastery==2) rows-cols else 0
        val clue=if(mastery==1) "The perimeter must be $perimeter units." else if(mastery==2) "Rows minus columns must equal $difference." else "Use exactly $rows rows."
        return challenge(WorkshopKind.Grid,"Array galaxy","Build a rectangular star array containing ${rows*cols} stars. $clue", "Area counts rows times columns; a perimeter counts all four edges.",
            "The array satisfies its generated area and dimension constraints. Its rows and columns form a valid multiplication model.",listOf(rows*cols,if(mastery==0) rows else 0,perimeter,difference),witness=listOf(rows.toString(),cols.toString()))
    }
    if(id in listOf("function-machine","patterns")) {
        val coefficient=n(2,7);val offset=n(2,15);val quadratic=mastery>0 || variant%2==1
        val steps=if(mastery==2) listOf("×",coefficient.toString(),"+",offset.toString(),"²","2") else
            (if(quadratic) listOf("²","2") else emptyList())+listOf("×",coefficient.toString(),"+",offset.toString())
        val inputs=List(3){n(1,8)}.distinct().let{ if(it.size<3) listOf(1,3,7) else it }
        fun output(x:Int)=if(mastery==2) (coefficient*x+offset)*(coefficient*x+offset) else coefficient*(if(quadratic)x*x else x)+offset
        return challenge(WorkshopKind.Machine,"Machine circuit","Build one circuit that matches ALL input/output examples. Order matters. Use up to six blocks.",
            "Compare the examples. Squaring before scaling gives a different machine from squaring after adding.","Your assembled circuit reproduces every example, so it models the underlying generated function rather than fitting just one input.",
            numbers=listOf(coefficient.toDouble(),offset.toDouble(),2.0),examples=inputs.map{it.toDouble() to output(it).toDouble()},witness=steps)
    }
    if(id in listOf("data","data-story") && (mastery>0 || variant%3==1)) {
        val values=(1..80).shuffled(random).take(if(mastery==1) 8 else 7).map(Int::toDouble).toMutableList()
        if(mastery==2) {values[1]=values[0];values[2]=values[0]}
        val descending=mastery==1;val mode=mastery==2
        val sorted=if(descending) values.sortedDescending() else values.sorted()
        val order=values.indices.sortedBy{if(descending) -values[it] else values[it]}
        val marks=if(mode) sorted.indices.filter{sorted[it]==values[0]} else listOf((values.size-1)/2,values.size/2).distinct()
        return challenge(WorkshopKind.Sort,"Evidence conveyor","Arrange ALL observations ${if(descending) "largest to smallest" else "smallest to largest"}. Then mark ${if(mode) "every occurrence of the mode" else if(values.size%2==0) "both middle observations" else "the median"}.",
            "Place every observation once. Mark the relevant slots only after ordering the data.","The conveyor contains a valid ordering and correctly marked central or modal observations. This checks the complete data arrangement.",
            listOf(if(descending) 1 else 0,if(mode) 1 else 0),numbers=values,witness=order.map(Int::toString)+listOf("|")+marks.map(Int::toString))
    }
    if(id=="number-theory") {
        val primes=listOf(2,3,5,7,11,13);val factors=List(if(mastery==2) 7 else if(mastery==1) 5 else 3){primes.take(if(mastery>0) 6 else 4).random(random)}
        val target=factors.fold(1){product,value->product*value}
        return challenge(WorkshopKind.Factors,"Prime crypt","Assemble a prime-factor chain whose product unlocks $target. Prime cores can repeat.",
            "Try dividing the target by a prime. Continue factoring the remaining quotient.","Multiplying the assembled prime factors reproduces the generated code. Every selected core is prime, so the factorization is complete.",listOf(target),witness=factors.map(Int::toString))
    }
    if(id in listOf("logic","combinatorics")) {
        val count=if(mastery>0) n(4,5) else n(3,7);val choose=if(mastery==2) 3 else if(mastery==1) 2 else n(2,3);val ordered=mastery==2 || (mastery==0 && count<=4 && random.nextBoolean())
        val energies=if(mastery>0) (1..40).shuffled(random).take(count) else emptyList()
        val modulus=if(mastery>0) n(2,5) else 1
        val remainder=if(mastery>0) energies.take(choose).sum()%modulus else 0
        val configurations=mutableListOf<List<Int>>()
        fun visit(current:List<Int>) {if(current.size==choose) {if((ordered || current==current.sorted()) && (mastery==0 || current.sumOf{energies[it]}%modulus==remainder))configurations+=current;return};repeat(count){if(it !in current)visit(current+it)}}
        visit(emptyList())
        val constraint=if(mastery>0) " Energy: ${energies.mapIndexed{i,v->"${('A'.code+i).toChar()}=$v"}.joinToString(", ")}. Each formation's total energy must leave remainder $remainder when divided by $modulus." else ""
        return challenge(WorkshopKind.Configurations,"Robot formations","Collect ALL ${if(ordered) "ordered codes" else "unordered teams"} of $choose different robots from $count robots. Duplicate formations do not count.$constraint",
            "Use a systematic order so no formation is missed. Decide whether rearranging the same robots creates a new formation.","Your collection contains every distinct valid formation without duplicates. Its size agrees with the generated permutation or combination count.",
            listOf(count,choose,if(ordered) 1 else 0,configurations.size,modulus,remainder),numbers=energies.map(Int::toDouble),witness=configurations.map{it.joinToString(",")})
    }
    if(id=="fractions") {
        val unit=if(mastery>0) 60 else 12
        val denominations=if(mastery>0) listOf(2,3,4,5,6,10) else listOf(2,3,4,6)
        val tiles=List(if(mastery>0) 9 else 6){unit/denominations.random(random)}
        val chosen=tiles.shuffled(random).take(if(mastery>0) 4 else 3);val target=chosen.sum()
        val source=GeneratedRound("Fraction mosaic","Fill $target/$unit of a whole with exactly ${chosen.size} fraction tiles. Each tile is available once.",target.toDouble(),
            "The selected fractional areas add to the target when expressed using a common denominator. The piece count and stock are also satisfied.",
            "Express each tile as parts of the same whole before combining it.",emptyList(),RoundInteraction.BuildSum,tiles,chosen.size)
        return challenge(WorkshopKind.Pieces,source.titleOrTopic(),source.prompt,source.hint,source.explanation,listOf(unit),source=source,witness=chosen.map(Int::toString))
    }
    val baseLevel=level.coerceAtMost(119)
    val focusCount=when(id){"chance"->15;"data","data-story","vectors","potions","ratio-rangers","logic","combinatorics","math-market","integer-expedition","percent-studio","calculus-climber"->3;"times-table","function-machine","matrix-mission","number-theory"->2;else->6}
    val mapped=if(focus>=0 && mastery==0) baseLevel/focusCount*focusCount+focus.coerceAtMost(focusCount-1) else baseLevel
    var source=generateMathRound(id,mapped,difficulty,seed)
    if(source.interaction!=RoundInteraction.Answer) {
        if(mastery>0) source=generateMasteryPieces(id,mastery,seed)
        return challenge(WorkshopKind.Pieces,source.topic,source.prompt,source.hint,source.explanation,source=source)
    }
    if(mastery>0) return generateMasteryFormula(id,mastery,seed)
    return challenge(WorkshopKind.Expression,source.topic,source.prompt,
        "Assemble numbers and operation blocks into a calculation. Run it to inspect YOUR result, then check the method against the mission.",source.explanation,numbers=(source.operands+listOf(0.0,1.0,2.0,100.0,180.0)).distinct(),source=source)
}

private fun GeneratedRound.titleOrTopic()=topic

private fun generateMasteryPieces(id:String,mastery:Int,seed:Long):GeneratedRound {
    val random=Random(seed)
    val normal=generateMathRound(id,119,3,seed)
    return when(id) {
        "forge"->{val target=if(mastery==1)random.nextInt(100000,1000000) else random.nextInt(1000000,10000000); val pieces=(0..4+mastery).flatMap{power->List(9){Math.pow(10.0,power.toDouble()).toInt()}}
            normal.copy(prompt="Mastery $mastery: compose $target with the fewest place-value cores.",answer=target.toDouble(),pieces=pieces,requiredPieces=target.toString().sumOf{it.digitToInt()})}
        "shapes"->{val types=listOf(0,3,4,5,6);val solution=List(6+mastery){types.random(random)}
            normal.copy(prompt="Mastery $mastery: build ${solution.size} pieces with ${solution.sum()} straight sides and ${solution.count{it==0}} curved pieces. Polygon cores may repeat.",answer=solution.sum().toDouble(),pieces=types,requiredPieces=solution.size,requiredCurves=solution.count{it==0})}
        "number-bonds","measure","math-market"->{val tiles=List(10){random.nextInt(if(id=="number-bonds" && mastery==2)-25 else 2,40)};val selected=tiles.shuffled(random).take(4+mastery)
            normal.copy(prompt="Mastery $mastery: assemble exactly ${selected.size} cores to reach ${selected.sum()}. Use each available core at most once.",answer=selected.sum().toDouble(),pieces=tiles,requiredPieces=selected.size,weights=emptyMap(),capacity=0)}
        "optimization-arena"->{val tiles=(5..80).shuffled(random).take(11+mastery);val weights=tiles.associateWith{random.nextInt(2,14)};val capacity=weights.values.sum()/2;val count=if(mastery==2) 5 else 0
            val best=(0 until (1 shl tiles.size)).maxOf{mask->val picked=tiles.filterIndexed{index,_->mask and (1 shl index)!=0};if(picked.sumOf{weights.getValue(it)}<=capacity && (count==0 || picked.size==count)) picked.sum() else 0}
            normal.copy(prompt="Mastery $mastery: maximize cargo value within $capacity kg.${if(count>0) " Pack exactly $count items." else ""}",answer=best.toDouble(),pieces=tiles,weights=weights,capacity=capacity,requiredPieces=count,
                explanation="The greatest feasible cargo value is $best within $capacity kg${if(count>0) " using exactly $count items" else ""}. Comparing complete combinations verifies this optimum.")}
        else->normal
    }
}

private fun generateMasteryFormula(id:String,mastery:Int,seed:Long):WorkshopChallenge {
    val random=Random(seed);val a=random.nextInt(4,15);val b=random.nextInt(3,12);val c=random.nextInt(2,8);val d=random.nextInt(2,8)
    val (prompt,tokens)=when(id) {
        "kitchen"->"A batch uses $a × $b grams plus $c × $d grams. Make ${if(mastery==2)d else 1} batches, then split equally into $c portions. Build the grams-per-portion calculation." to (listOf("(","$a","×","$b","+","$c","×","$d",")")+(if(mastery==2)listOf("×","$d")else emptyList())+listOf("÷","$c"))
        "decimal-harbor"->"Cargo is ${a/10.0} + ${b/100.0} tonnes per trip. Make $c trips, then unload ${d/10.0} tonnes.${if(mastery==2) " Share the remaining load equally among $d crates. Build the tonnes-per-crate calculation." else " Build the remaining-load calculation."}" to (listOf("(","(",roundNumber(a/10.0),"+",roundNumber(b/100.0),")","×","$c","−",roundNumber(d/10.0),")")+(if(mastery==2)listOf("÷","$d")else emptyList()))
        "percent-studio","math-market"->{val price=a*100;val first=b*5;val second=c*5
            "A ₹$price price receives successive discounts of $first% and $second%.${if(mastery==2) " Add ${d*3}% tax to the discounted price." else ""} Build the final-price calculation." to (listOf("$price","×","(","100","−","$first",")","÷","100","×","(","100","−","$second",")","÷","100")+(if(mastery==2)listOf("×","${100+d*3}","÷","100")else emptyList()))}
        "geometry-proof","measure"->{val base=minOf(c,a);val height=minOf(d,b);"A $a × $b rectangle loses ${if(mastery==2)"two disjoint right-triangular sections, each" else "a right-triangular section"} with base $base and height $height. Build the remaining-area calculation." to (listOf("$a","×","$b","−","$base","×","$height","÷","2")+(if(mastery==2)listOf("×","2")else emptyList()))}
        "calculus-climber"->if(mastery==1) "For f(x) = ${c}x³ + ${b}x, construct f′($a)." to listOf("3","×","$c","×","$a","^","2","+","$b") else "Integrate ${3*c}x² + $b from 0 to $a. Build the definite-integral calculation." to listOf("$c","×","$a","^","3","+","$b","×","$a")
        "chance"->if(mastery==1) "A bag has $a red and $b blue balls. Draw three WITHOUT replacement. Construct P(all red), as a decimal to six places." to listOf("$a","÷","${a+b}","×","${a-1}","÷","${a+b-1}","×","${a-2}","÷","${a+b-2}") else "A game pays ₹${a*10} on a win and loses ₹${b*5} otherwise. P(win)=$c/${c+d}. Construct the expected NET payoff." to listOf("${a*10}","×","$c","÷","${c+d}","−","${b*5}","×","$d","÷","${c+d}")
        "data","data-story"->"Observations: $a, $b, $c, $d. Build their mean using all observations." to listOf("(","$a","+","$b","+","$c","+","$d",")","÷","4")
        else->error("Missing mastery formula for $id")
    }
    val result=evaluateMathAssembly(tokens)!!
    val source=GeneratedRound("Mastery ${if(mastery==1) "fusion" else "strategy"}",prompt,result.value,
        "The generated multi-step calculation evaluates to ${roundNumber(result.value)}. Grouping and the order of operations preserve the meaning of every stage.",
        "Break the mission into stages, then join those stages with operation blocks.",emptyList(),operands=tokens.mapNotNull(String::toDoubleOrNull))
    return WorkshopChallenge(WorkshopKind.Expression,source.topic,prompt,source.hint,source.explanation,source,numbers=source.operands.distinct(),witness=tokens,mastery=mastery)
}
