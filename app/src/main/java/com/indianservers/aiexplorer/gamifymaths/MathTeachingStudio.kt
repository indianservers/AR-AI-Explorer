package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

internal data class MathTeachingPlan(val concept:String,val model:String,val plan:String,val check:String,val misconception:String,val reflection:String)

/** Subject-specific teaching, independent of artwork and of the generated answer. */
internal fun mathTeachingPlan(id:String,prompt:String=""):MathTeachingPlan {
    fun lesson(concept:String,model:String,plan:String,check:String,trap:String,reflection:String)=MathTeachingPlan(concept,model,plan,check,trap,reflection)
    val text=prompt.lowercase()
    return when(id){
        "speed-basic"->lesson("Fluent calculation","Estimate → group → calculate","Estimate the size first. Look for number bonds, doubles or a useful factor before building the calculation.","Use an inverse operation or compare with your estimate.","Speed comes after accuracy. Brackets change the order of operations.","Which regrouping would save you a step?")
        "speed-advanced"->lesson("Structure before speed","Brackets → functions/powers → × ÷ → + −","Identify the function or algebraic relationship before calculating. Trigonometric angles here use degrees.","Check signs, domains and whether the magnitude is reasonable.","A function name acts on its argument; it is not a multiplication tile. Keep negative angles grouped.","Could an identity or equivalent expression make this shorter?")
        "forge"->lesson("Place value","digit × place value","Break the target into hundreds, tens and ones, continuing to larger places when needed. Read whether the question asks for a changed or rounded number.","Add the block values. For the fewest blocks, exchange ten of one place for one of the next.","A digit and its place value are different: a digit's position determines its contribution.","Which exchange keeps the total unchanged?")
        "kitchen"->lesson("Scaling quantities","quantity per serving × servings","Name each quantity and its unit. Decide whether you are combining, sharing or scaling before selecting operations.","Check the units and reverse the scaling where possible.","Adding servings does not mean adding the ingredient amounts only once.","How would the method change for twice as many servings?")
        "fractions"->lesson("Equal parts of one whole","numerator / denominator","Read the target and tile-count rule. Convert available fractions to the same unit before choosing pieces.","Add numerators only after the denominators represent the same-sized parts; also count the tiles.","Equal-looking tiles can represent different fractions. Meeting the total alone may miss a tile-count rule.","Can you make an equivalent total with a different selection?")
        "potions","ratio-rangers"->lesson("Ratio and scale","one part = total ÷ sum of ratio parts","Add the blue and red ratio parts. Find the volume of one part, then scale each colour separately.","Check both blue + red = total and blue × red-parts = red × blue-parts.","A correct total can still have the wrong ratio. Blue:red has a fixed order.","If the total doubled, what would stay the same?")
        "balance"->lesson("Preserving equality","same operation on both sides","Remove variable terms from one side, then constants. Divide by the remaining coefficient to isolate one x.","Substitute your x into both original sides; their values must match.","Moving a term is shorthand for applying an inverse operation to both sides, including negative terms.","Why does each operation preserve the solution?")
        "shapes"->lesson("Geometric properties","pieces → straight sides + curves","Classify each displayed shape by its straight sides and curved boundaries. Meet the total and piece-count constraints together.","Count pieces, straight sides and curved pieces separately.","Turning a shape does not change its properties. A curved boundary is not a straight side.","Could a different combination satisfy every constraint?")
        "data","data-story"->when{
            "variance" in text || "standard deviation" in text->lesson("Measuring variability","mean → deviations → squared deviations","Find the mean, then each deviation from it. Use the population or sample divisor specified by the question; take the square root only for standard deviation.","Squared deviations and variance cannot be negative. Standard deviation has the data's original units.","The mean of signed deviations is not the variance: positive and negative deviations cancel.","How would adding the same constant to every reading affect the spread?")
            "median" in text || "middle" in text->lesson("Median","order → centre","Sort all readings, keeping repeated values. Find one centre for an odd count or two centres for an even count.","There should be the same number of readings on either side of the centre.","The median depends on position, not on which value appears most often.","Would one extreme reading change the median much?")
            "mode" in text->lesson("Frequency and modes","value → number of occurrences","Count occurrences of each value. Follow the question's marking rule when there are ties.","Check that no other value occurs more often.","There can be more than one mode. Repeated observations still belong in the data.","How would one extra observation change the modes?")
            "range" in text || "extreme" in text->lesson("Spread","range = largest − smallest","Find the two extremes after ordering the readings.","The range must be nonnegative and uses the original data's units.","Range is a difference, not the number of observations.","Which observation has the greatest effect on this measure?")
            else->lesson("Summarising data","total ↔ count × mean","Identify the requested statistic. For a mean, total all readings and divide by the count; weighted data needs the total weight.","For a mean, multiplying back by the count or weight should recover the total.","Mean, median and mode answer different questions. Do not ignore repeated or weighted readings.","What does this statistic reveal, and what does it hide?")
        }
        "patterns","function-machine"->lesson("Functions and rules","input → ordered operations → output","Choose a possible rule and test it on every example. Build operations in the order in which they act.","A rule must reproduce every shown output, not just the first one.","Add then multiply usually differs from multiply then add. Squaring affects the whole current value.","What new input would best distinguish two possible rules?")
        "number-bonds"->lesson("Part–whole relationships","part + part = whole","Read the target and stock limits. Choose parts that combine to the required whole and meet the count rule.","Recombine all chosen parts, then check how many pieces you used.","Reusing a tile may be forbidden even if its value fits.","Which two parts could you exchange without changing the whole?")
        "decimal-harbor"->lesson("Decimal place value","ones | tenths | hundredths","Track place values when combining decimals. When multiplying or dividing, estimate first to locate the decimal point.","Compare with an estimate and use the inverse operation.","More decimal digits does not necessarily mean a larger number.","How could you represent the same quantity using a different unit?")
        "math-market"->lesson("Value and financial reasoning","identify the base → apply the change","Separate quantities, prices and percentage bases. Apply successive discounts or taxes in the stated order; cargo tasks also require a stock and capacity check.","Recalculate the final total and verify units and capacity when present.","Two successive percentage changes do not usually combine by simply adding the percentages.","Which choice gives the best value, and why?")
        "integer-expedition"->lesson("Directed change","start + signed moves = finish","Read the sign and size of each move. Track the running position and remaining moves.","Sum the signed moves and add them to the start.","A negative move changes direction; it is not a smaller positive move.","Can you reach the same destination with fewer moves?")
        "matrix-mission","vectors"->lesson("Vector displacement","(x, y) + (Δx, Δy)","Subtract start coordinates from target coordinates. Build that displacement from the available moves within the limit.","Add horizontal and vertical components separately; compare with the target.","The first coordinate changes horizontally. Positive y moves upward even though screen pixels run downward.","Can two different routes have the same displacement?")
        "geometry-proof","measure"->lesson("Geometry and measurement","label → choose a relationship → substitute","Identify the requested quantity before selecting a formula. For a composite shape, split it into parts with known dimensions.","Check whether units should be length, square units or cubic units.","Perimeter measures a boundary; area measures coverage. A drawing may not be to scale.","Which dimensions matter to the requested quantity?")
        "calculus-climber"->lesson("Change and accumulation","differentiate / integrate → evaluate","Decide whether the mission asks for a derivative or an integral. Apply the rule symbolically before substituting the requested point or bounds.","For a derivative, check a simple term. For a definite integral, use upper value minus lower value.","A derivative's exponent decreases; an antiderivative's increases. Do not substitute before applying the required rule.","What does the result describe: local change or accumulated quantity?")
        "chance"->if("payoff" in text || "expected" in text)lesson("Expected value","Σ(probability × value)","List outcomes and their probabilities. Multiply each outcome's signed value by its probability, then add.","Probabilities should sum to one. The expectation should lie between the smallest and largest possible values.","Expected value is a long-run average, not a guaranteed result. Losses must carry a negative sign.","Could an unlikely outcome still have a large effect?")else lesson("Probability models","favourable / total; then combine events","Identify the event and sample space. For multiple draws, decide whether replacement changes the next denominator.","A probability must lie between zero and one. Check complements and any overlap in combined events.","Without replacement, both available counts and the total change. 'Or' and 'and' need different reasoning.","What would change if the draw were made with replacement?")
        "times-table"->lesson("Arrays and factors","rows × columns; boundary = 2(rows + columns)","Use the target star count to find factor pairs. Choose a pair that also satisfies the row, column or perimeter clue.","Check both the product and every extra clue.","The same area can have different perimeters. Rows and columns may not be interchangeable under the question's rule.","Which arrangement has the smallest perimeter for this total?")
        "percent-studio"->lesson("Percent and its base","part = whole × percent / 100","Identify which quantity is unknown: part, whole or percentage. Rearrange the relationship before entering values.","Check what 100% represents. Compare the answer with useful benchmarks such as 10% or 50%.","The percentage base matters. A percentage increase and an equal percentage decrease do not cancel.","How would your method change if the whole were unknown?")
        "number-theory"->lesson("Prime structure","target = product of prime factors","Use divisibility tests and divide by prime factors until the remaining quotient is one. If the mission asks for GCD or LCM, determine that target first.","Multiply your prime cores back together to recover the target.","One is not prime. Repeated prime factors are allowed and sometimes necessary.","What does the factorisation tell you about divisibility?")
        "logic","combinatorics"->lesson("Systematic counting","choose → test constraints → collect once","First decide whether order matters. Fix one robot and list possible partners systematically; check each formation against every energy or inclusion rule.","Check for missing formations and duplicates. For unordered teams, swapping positions adds no new team.","Counting every permutation as a new team overcounts unordered selections.","How could you organise the search so that none are missed?")
        "optimization-arena"->lesson("Constrained optimisation","maximise value subject to capacity","Track total weight and value separately. Compare feasible combinations and enforce any exact item-count rule.","Verify the weight limit, stock limits and whether another feasible set has greater value.","The most valuable single item or best value-per-kilogram choice need not produce the best combination.","Which excluded item would improve the build if capacity increased?")
        else->error("Missing teaching plan for $id")
    }
}

@Composable internal fun MathStrategyDialog(gameId:String,prompt:String,instructions:String,onClose:()->Unit){
    val lesson=remember(gameId,prompt){mathTeachingPlan(gameId,prompt)}
    var tab by remember{mutableIntStateOf(0)}
    AlertDialog(onDismissRequest=onClose,title={Text(lesson.concept)},text={
        Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("Plan","Check","Reflect").forEachIndexed{i,label->FilterChip(selected=tab==i,onClick={tab=i},modifier=Modifier.semantics{contentDescription="Strategy $label"},label={Text(label,fontSize=12.sp)})}
            }
            Column(Modifier.fillMaxWidth().background(Color(0xFFEDF5FF),RoundedCornerShape(14.dp)).padding(14.dp)){
                Text("THE MATHEMATICAL MODEL",fontSize=10.sp,color=Color(0xFF49678A))
                Text(lesson.model,fontWeight=FontWeight.Bold,color=Color(0xFF153F7B))
            }
            when(tab){
                0->{Text(lesson.plan);if(instructions.isNotBlank()){Text("Controls",fontWeight=FontWeight.Bold);Text(instructions)}}
                1->{Text(lesson.check);Text("Watch out",fontWeight=FontWeight.Bold,color=Color(0xFF8E5300));Text(lesson.misconception)}
                else->{Text(lesson.reflection);Text("Explain your choice of method before trying another mission.",fontSize=12.sp)}
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text("Back to mission")}})
}

@Composable internal fun MathMissionDebrief(gameId:String,round:WorkshopChallenge){
    val lesson=remember(gameId,round.signature){mathTeachingPlan(gameId,round.prompt)}
    Column(Modifier.fillMaxWidth().background(Color(0xFFEAF7F0),RoundedCornerShape(12.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text("Mission complete · ${lesson.concept}",color=Color(0xFF17623F),fontWeight=FontWeight.Bold,fontSize=12.sp)
        Text(lesson.reflection,color=Color(0xFF315E4C),fontSize=12.sp)
    }
}
