package com.indianservers.aiexplorer.gamifymaths

import kotlin.math.*
import kotlin.random.Random

internal data class MathTemplateDescriptor(val id:String,val gameId:String,val kind:WorkshopKind?,val variant:Int)
internal val workshopTemplateCatalogue:List<MathTemplateDescriptor> by lazy {
    gamifyGamesForAudit().filterNot{it.id.startsWith("speed-")}.flatMap{game->
        val kinds=when(game.id){
            "data","data-story"->listOf(WorkshopKind.Expression,WorkshopKind.Sort)
            "measure","math-market"->listOf(WorkshopKind.Expression,WorkshopKind.Pieces)
            else->listOf(generateBaseWorkshopChallenge(game.id,0,0,42L).kind)
        }
        kinds.flatMap{kind->List(if(kind==WorkshopKind.Expression)12 else 8){v->MathTemplateDescriptor("${game.id}.${kind.name.lowercase()}.$v",game.id,kind,v)}}
    }
}

internal fun generateWorkshopChallenge(id:String,level:Int,difficulty:Int,seed:Long,focus:Int = -1):WorkshopChallenge {
    require(level in 0 until GeneratedLevelCount && difficulty in 0..3)
    val random=Random(seed xor 0x4D415448L)
    // Progress controls scale; it no longer pins a replay to one question family.
    val familyCount=when(id){"chance"->15;"data","data-story","math-market"->3;"measure","kitchen","geometry-proof"->6;else->6}
    val contentLevel=if(level<120 && focus<0)level/30*30+random.nextInt(familyCount)else level
    val base=generateBaseWorkshopChallenge(id,contentLevel,difficulty,seed,focus)
    val variant=random.nextInt(if(base.kind==WorkshopKind.Expression)12 else 8)
    return instantiateWorkshopTemplate(id,base,variant,difficulty+level/30,seed,focus)
}

internal fun instantiateWorkshopTemplate(id:String,base:WorkshopChallenge,variant:Int,tier:Int,seed:Long,focus:Int = -1):WorkshopChallenge {
    val random=Random(seed xor 0x56415249414E54L)
    val expanded=when(base.kind){
        WorkshopKind.Expression->if(focus>=0 && base.mastery==0)base else expandExpressionTemplate(id,base,variant,tier.coerceIn(0,3),random)
        WorkshopKind.Machine->machineTemplate(base,variant,random,focus)
        WorkshopKind.Configurations->configurationTemplate(base,variant,random,focus)
        WorkshopKind.Mixture->mixtureTemplate(base,variant)
        WorkshopKind.Grid->gridTemplate(base,variant)
        WorkshopKind.Factors->factorTemplate(base,variant,random)
        WorkshopKind.Navigation->navigationTemplate(base,variant,random)
        WorkshopKind.Balance->balanceTemplate(base,variant)
        WorkshopKind.Sort->sortTemplate(base,if(focus==1 && base.mastery==0)listOf(0,1,5,6,7)[variant%5]else variant,random)
        WorkshopKind.Pieces->pieceTemplate(id,base,variant,random)
    }
    return expanded.copy(templateId="$id.${base.kind.name.lowercase()}.$variant")
}

private fun machineTemplate(base:WorkshopChallenge,v:Int,r:Random,focus:Int):WorkshopChallenge {
    val a=r.nextInt(2,9);val b=r.nextInt(2,14);val c=r.nextInt(2,7)
    val quadratic=focus==1 || base.mastery>0 || (focus<0 && v%2==1)
    val steps=if(!quadratic)when(v%4){
        0->listOf("×","$a","+","$b")
        1->listOf("+","$b","×","$a")
        2->listOf("×","$a","+","-$b")
        else->listOf("+","$b","×","$a","+","$c")
    }else when(v%4){
        0->listOf("²","2","×","$a","+","$b")
        1->listOf("×","$a","+","$b","²","2")
        2->listOf("+","$b","²","2","×","$a")
        else->listOf("²","2","+","$b","×","$a","+","$c")
    }
    val inputs=(1..12).shuffled(r).take(3).map(Int::toDouble)
    val draft=base.copy(witness=steps)
    return draft.copy(prompt="Build one ${if(quadratic)"quadratic" else "linear"} circuit matching ALL three examples. Order matters; use at most six blocks.",
        numbers=steps.chunked(2).map{it[1].toDouble()}.distinct(),examples=inputs.map{it to checkNotNull(draft.machineOutput(it,steps))},
        explanation="Apply ${steps.chunked(2).joinToString(" → "){if(it[0]=="²")"square" else "${it[0]} ${it[1]}"}} in order. The same generated rule matches every example.")
}

private fun configurationTemplate(base:WorkshopChallenge,v:Int,r:Random,focus:Int):WorkshopChallenge {
    val count=if(focus==1)r.nextInt(3,5)else r.nextInt(4,6)
    val choose=if(base.mastery==2)3 else 2
    val ordered=if(focus>=0 && base.mastery==0)focus==1 else v%2==1 || base.mastery==2
    val energy=(2..40).shuffled(r).take(count)
    val anchor=(0 until count).shuffled(r).take(choose)
    val sum=anchor.sumOf{energy[it]}
    val modulus=if(v in 2..3 || base.mastery>0)r.nextInt(2,6)else 1
    val rules=when(v){
        4->mapOf("requiredRobot" to anchor[0])
        5->if(ordered)mapOf("firstRobot" to anchor[0])else mapOf("excludedRobot" to (0 until count).first{it !in anchor})
        6->mapOf("maxEnergy" to sum)
        7->mapOf("minEnergy" to sum)
        else->emptyMap()
    }
    val draft=base.copy(parameters=listOf(count,choose,if(ordered)1 else 0,0,modulus,sum%modulus),numbers=energy.map(Int::toDouble),rules=rules)
    val teams=mutableListOf<List<Int>>()
    fun visit(team:List<Int>){if(team.size==choose){if((ordered || team==team.sorted()) && draft.validConfiguration(team))teams+=team;return};repeat(count){if(it !in team)visit(team+it)}}
    visit(emptyList());check(teams.isNotEmpty())
    val clue=buildList{
        if(modulus>1)add("The total energy must leave remainder ${sum%modulus} modulo $modulus.")
        rules["requiredRobot"]?.let{add("Include robot ${('A'.code+it).toChar()}.")}
        rules["excludedRobot"]?.let{add("Do not use robot ${('A'.code+it).toChar()}.")}
        rules["firstRobot"]?.let{add("Robot ${('A'.code+it).toChar()} must be first.")}
        rules["maxEnergy"]?.let{add("The total energy must be at most $it.")}
        rules["minEnergy"]?.let{add("The total energy must be at least $it.")}
    }.joinToString(" ")
    return draft.copy(parameters=draft.parameters.toMutableList().apply{this[3]=teams.size},
        prompt="Collect ALL ${if(ordered)"ordered codes" else "unordered teams"} of $choose from $count robots. $clue".trim(),
        witness=teams.map{it.joinToString(",")},explanation="There are ${teams.size} valid ${if(ordered)"ordered codes" else "unordered teams"}. Test each candidate against the stated conditions and count each valid formation once.")
}

private fun mixtureTemplate(base:WorkshopChallenge,v:Int):WorkshopChallenge {
    val(a,b,total)=base.parameters;val blue=base.witness[0].toInt();val red=base.witness[1].toInt()
    val text=when(v){
        0->"Pour $total ml. Blue:red must be $a:$b."
        1->"A $total ml batch contains $a blue parts for every $b red parts. Set both volumes."
        2->"Blue is $a/${a+b} of a $total ml batch; the rest is red. Set both volumes."
        3->"Red is $b/${a+b} of a $total ml batch; the rest is blue. Set both volumes."
        4->"A recipe uses $a ml blue and $b ml red per batch. Scale it to $total ml."
        5->"Make $total ml with blue:red=$a:$b. The blue-minus-red difference must be ${blue-red} ml."
        6->"Prepare ${total/(a+b)} identical batches, each using $a ml blue and $b ml red. Set the combined volumes."
        else->"The combined volume is $total ml. For each ${a+b} ml, use $a ml blue and $b ml red."
    }
    return base.copy(prompt=text,explanation="${a+b} ratio parts make $total ml. One part is ${total/(a+b)} ml; blue=$blue ml and red=$red ml. Their sum and ratio satisfy the recipe.")
}

private fun gridTemplate(base:WorkshopChallenge,v:Int):WorkshopChallenge {
    val rows=base.witness[0].toInt();val cols=base.witness[1].toInt();val area=rows*cols
    val perimeter=2*(rows+cols);var rule=emptyMap<String,Int>()
    var fixedRows=0;var fixedPerimeter=0
    val clue=when(v){
        0->{fixedRows=rows;"Use exactly $rows rows."}
        1->{rule=mapOf("columns" to cols);"Use exactly $cols columns."}
        2->{fixedPerimeter=perimeter;"Its perimeter must be $perimeter units."}
        3->{rule=mapOf("difference" to rows-cols);"Rows minus columns must be ${rows-cols}."}
        4->{rule=mapOf("rowRatio" to rows,"columnRatio" to cols);"Rows:columns must be $rows:$cols."}
        5->{fixedRows=rows;"Each of the $rows rows must contain the same number of stars."}
        6->{fixedPerimeter=perimeter;"Rows plus columns must equal ${rows+cols}."}
        else->{rule=mapOf("columns" to cols);"Each column must contain $rows stars and there must be $cols columns."}
    }
    if(base.mastery==1)fixedPerimeter=perimeter
    val extra=if(base.mastery==1 && v !in listOf(2,6))" Perimeter=$perimeter."else if(base.mastery==2 && v!=3)" Rows minus columns=${rows-cols}."else ""
    return base.copy(parameters=listOf(area,fixedRows,fixedPerimeter,rows-cols),rules=rule,prompt="Build a rectangular array of $area stars. $clue$extra",
        explanation="$rows rows × $cols columns = $area stars. Perimeter=2×($rows+$cols)=$perimeter; the dimensions also satisfy the stated clue.")
}

private fun factorTemplate(base:WorkshopChallenge,v:Int,r:Random):WorkshopChallenge {
    val target=base.parameters[0];val multiplier=listOf(2,3,5,7).random(r)
    val prompt=when(v){
        0->"Build the prime-factor chain for $target. Prime cores may repeat."
        1->"A lock displays ${target.toLong()*multiplier} ÷ $multiplier. Prime-factor its result."
        2->"Build prime factors for gcd($target, ${target.toLong()*multiplier})."
        3->"Build prime factors for lcm($target, $multiplier)."
        4->"A code is ${target.toLong()+multiplier} − $multiplier. Build its prime-factor chain."
        5->"Prime-factor the product ${base.witness.first()} × ${target/base.witness.first().toInt()}."
        6->"A vault's code is $target. Use its complete prime factorization, including repeated factors."
        else->"A code is ${target.toLong()*multiplier} with one factor $multiplier removed. Build the remaining prime factors."
    }
    if(v!=3)return base.copy(prompt=prompt,explanation="${base.witness.joinToString(" × ")} = $target. Every core is prime, so this is a complete prime factorization.")
    fun gcd(a:Int,b:Int):Int=if(b==0)a else gcd(b,a%b)
    val lcm=target/gcd(target,multiplier)*multiplier
    var left=lcm;val factors=mutableListOf<String>();for(p in listOf(2,3,5,7,11,13))while(left%p==0){factors+="$p";left/=p}
    return base.copy(prompt=prompt,parameters=listOf(lcm),witness=factors,explanation="LCM=$lcm. Its prime factors are ${factors.joinToString(" × ")}.")
}

private fun navigationTemplate(base:WorkshopChallenge,v:Int,r:Random):WorkshopChallenge {
    val start=base.parameters.take(2);val count=base.parameters[4]
    val route=when(v){
        0->base.witness
        1->base.witness.reversed()
        2->base.witness.map{((it.toInt()+2)%4).toString()}
        3->List(count){if(it%2==0)"0"else "1"}
        4->List(count){if(it%2==0)"2"else "3"}
        5->List(count){if(it<count/2)"0"else "3"}
        6->List(count){r.nextInt(base.moves.size).toString()}
        else->base.witness.shuffled(r)
    }.let{candidate->if(candidate.map{base.moves[it.toInt()].first}.sum()==0 && candidate.map{base.moves[it.toInt()].second}.sum()==0)base.witness else candidate}
    val x=start[0]+route.sumOf{base.moves[it.toInt()].first};val y=start[1]+route.sumOf{base.moves[it.toInt()].second}
    return base.copy(parameters=listOf(start[0],start[1],x,y,count),witness=route,
        prompt=when(v){
            0,1->"Pilot from (${start[0]},${start[1]}) to ($x,$y) in at most $count moves."
            2,3->"Start at (${start[0]},${start[1]}). Program displacement (${x-start[0]},${y-start[1]}) using at most $count labelled vector moves."
            4,5->"Reach ($x,$y) from (${start[0]},${start[1]}). Each arrow adds its displayed vector; budget=$count moves."
            else->"A ship starts at (${start[0]},${start[1]}) and must dock at ($x,$y). Build a route of at most $count moves."
        },explanation="A valid route is ${route.joinToString(" → "){val m=base.moves[it.toInt()];"(${m.first},${m.second})"}}. Its displacement is (${x-start[0]},${y-start[1]}), reaching ($x,$y).")
}

private fun balanceTemplate(base:WorkshopChallenge,v:Int):WorkshopChallenge {
    val x=base.numbers[4];val left=base.numbers[0]*(if(v in listOf(2,5)) -1 else 1)
    val right=base.numbers[2];val offset=base.numbers[1]*(if(v in listOf(1,3,6)) -1 else 1)
    val result=(left-right)*x+offset
    val witness=(if(right!=0.0)listOf("−x",roundNumber(right))else emptyList())+listOf("−",roundNumber(offset),"÷",roundNumber(left-right))
    fun constant(n:Double)=if(n<0)"− ${roundNumber(-n)}"else "+ ${roundNumber(n)}"
    return base.copy(numbers=listOf(left,offset,right,result,x),witness=witness,
        prompt="${if(v%2==0)"Isolate x"else "Solve the balance"}: ${roundNumber(left)}x ${constant(offset)} = ${if(right==0.0)roundNumber(result)else "${roundNumber(right)}x ${constant(result)}"}. Apply equal operations to both plates.",
        explanation="Remove ${roundNumber(right)}x from both sides, subtract ${roundNumber(offset)}, then divide by ${roundNumber(left-right)}. The solution is x=${roundNumber(x)}.")
}

private fun sortTemplate(base:WorkshopChallenge,v:Int,r:Random):WorkshopChallenge {
    val count=if(v in listOf(1,5))8 else 7
    val values=(1..90).shuffled(r).take(count).map{if(v==6)(it-45).toDouble()else if(v==7)it/10.0 else it.toDouble()}.toMutableList()
    val mode=v in listOf(2,3) || base.mastery==2
    if(mode){values[1]=values[0];values[2]=values[0];if(v==3){values[4]=values[3];values[5]=values[3]}}
    val descending=v in listOf(4,5) || base.mastery==1
    val range=v==4 && base.mastery==0
    val sorted=if(descending)values.sortedDescending()else values.sorted()
    val frequencies=sorted.groupingBy{it}.eachCount();val maxFrequency=frequencies.values.max()
    val marks=if(mode)sorted.indices.filter{frequencies[sorted[it]]==maxFrequency}else if(range)listOf(0,sorted.lastIndex)else listOf((count-1)/2,count/2).distinct()
    val order=values.indices.sortedBy{if(descending)-values[it]else values[it]}
    return base.copy(numbers=values,parameters=listOf(if(descending)1 else 0,if(mode)1 else if(range)2 else 0),witness=order.map(Int::toString)+"|"+marks.map(Int::toString),
        prompt="Order ALL readings ${if(descending)"largest to smallest"else "smallest to largest"}. Mark ${if(mode)"every occurrence of each mode"else if(range)"both extreme readings"else if(count%2==0)"both middle readings"else "the median"}.",
        explanation="Correct order: ${sorted.joinToString{roundNumber(it)}}. Mark positions ${marks.joinToString{(it+1).toString()}} (counting from one).")
}

private fun pieceTemplate(id:String,base:WorkshopChallenge,v:Int,r:Random):WorkshopChallenge {
    val source=checkNotNull(base.source)
    if(source.interaction==RoundInteraction.BuildPlaceValue){
        val old=source.answer.toInt();val shift=10.0.pow(maxOf(0,old.toString().length-2)).toInt()
        val target=when(v){1->old+1;2->maxOf(10,old-1);3->old/10*10;4->old/shift*shift;5->old+shift;6->old+2*shift;else->old}
        val places=target.toString().indices.map{10.0.pow(it).toInt()};val pieces=places.flatMap{place->List(9){place}}
        val selected=places.flatMap{p->List(target/p%10){p}}
        val q=when(v){1->"Build the successor of $old.";2->"Build the predecessor of $old.";3->"Build $old rounded DOWN to a multiple of 10.";4->"Build $old rounded DOWN to a multiple of $shift.";5->"Build $old + $shift.";6->"Build $old + ${2*shift}.";7->"Compose $target from its place values.";else->"Build $target."}+" Use the fewest place-value blocks."
        val updated=source.copy(prompt=q,answer=target.toDouble(),pieces=pieces,requiredPieces=selected.size,explanation="${selected.joinToString(" + ")} = $target; ${selected.size} blocks is the sum of its decimal digits.")
        return base.copy(source=updated,prompt=q,witness=selected.map(Int::toString),explanation=updated.explanation)
    }
    if(source.interaction==RoundInteraction.BuildShapes){
        val types=when(v){0->listOf(0,3,4);1->listOf(3,4);2->listOf(0,4,6);3->listOf(3,6);4->listOf(4,5);5->listOf(0,3,5);6->listOf(3,4,5,6);else->listOf(0,3,4,5,6)}
        val solution=List(source.requiredPieces){types.random(r)}
        val q="Build ${solution.size} pieces with ${solution.sum()} straight sides and ${solution.count{it==0}} curved pieces. Only the displayed shape types may be used."
        val updated=source.copy(prompt=q,answer=solution.sum().toDouble(),pieces=types,requiredCurves=solution.count{it==0},explanation="One valid construction has side counts ${solution.joinToString()}. Total sides=${solution.sum()}, curves=${solution.count{it==0}}.")
        return base.copy(source=updated,prompt=q,witness=solution.map(Int::toString),explanation=updated.explanation)
    }
    if(id=="optimization-arena"){
        val capacity=maxOf(source.weights.values.min(),(source.weights.values.sum()*(.35+v*.045)).toInt())
        val exactCount=if(v in listOf(2,5,7))minOf(3,source.pieces.size)else 0
        val feasible=(0 until (1 shl source.pieces.size)).asSequence().map{mask->source.pieces.filterIndexed{i,_->mask and (1 shl i)!=0}}
            .filter{(exactCount==0 || it.size==exactCount) && it.sumOf{source.weights.getValue(it)}<=capacity}.toList()
        val selected=feasible.maxByOrNull{it.sum()} ?: return pieceTemplate(id,base,0,r)
        val q="Maximize cargo value within $capacity kg.${if(exactCount>0)" Use exactly $exactCount items."else ""} Each item is available once."
        val updated=source.copy(prompt=q,answer=selected.sum().toDouble(),capacity=capacity,requiredPieces=exactCount,explanation="Best value=${selected.sum()}; choose ${selected.joinToString()} with total weight ${selected.sumOf{source.weights.getValue(it)}} kg. All feasible subsets were compared.")
        return base.copy(source=updated,prompt=q,witness=selected.map(Int::toString),explanation=updated.explanation)
    }
    val unit=base.parameters.firstOrNull() ?: 1
    val count=source.requiredPieces.takeIf{it>0} ?: minOf(3,source.pieces.size)
    var selected=source.pieces.shuffled(r).take(count)
    if(v in listOf(5,6)){
        val target=selected.sum();val choices=(0 until (1 shl source.pieces.size)).asSequence().map{mask->source.pieces.filterIndexed{i,_->mask and (1 shl i)!=0}}.filter{it.sum()==target}.toList()
        selected=if(v==5)choices.minBy{it.size}else choices.maxBy{it.size}
    }
    val target=selected.sum()
    val q=if(id=="fractions")when(v){
        0->"Fill $target/$unit of a whole using exactly ${selected.size} tiles."
        1->"Build the fraction ${target*2}/${unit*2} using exactly ${selected.size} available tiles."
        2->"Fill ${ceil(target.toDouble()/unit).toInt()+1} − ${(ceil(target.toDouble()/unit).toInt()+1)*unit-target}/$unit of a whole using ${selected.size} tiles."
        3->"Combine ${selected.size} fraction tiles to cover $target of $unit equal parts."
        4->"Make $target/$unit using ${selected.size} tiles; each tile may be used once."
        5->"Make $target/$unit using the fewest available tiles."
        6->"Make $target/$unit using the most available tiles."
        else->"Construct an area equal to ${roundNumber(target.toDouble()/unit)} wholes with ${selected.size} tiles (the fraction target is $target/$unit)."
    }else when(v){
        0->"Connect ${selected.size} number tiles to make $target. Each tile can be used once."
        1->"A target is ${target+10} − 10. Build it with ${selected.size} available cores."
        2->"Build $target using ${selected.size} cores; use each tile at most once."
        3->"Combine exactly ${selected.size} available pieces to reach the total $target."
        4->"A project needs ${target+20} units and already has 20. Build the remainder with ${selected.size} cores."
        5->"Build $target using the fewest available cores."
        6->"Build $target using the most available cores."
        else->"Split the target $target into exactly ${selected.size} available number cores."
    }
    val explanation="One correct selection is ${selected.joinToString(" + "){if(unit>1)"$it/$unit"else "$it"}} = ${if(unit>1)"$target/$unit"else "$target"}. It uses ${selected.size} available tiles."
    val updated=source.copy(prompt=q,answer=target.toDouble(),requiredPieces=selected.size,explanation=explanation)
    return base.copy(source=updated,prompt=q,witness=selected.map(Int::toString),explanation=explanation)
}
