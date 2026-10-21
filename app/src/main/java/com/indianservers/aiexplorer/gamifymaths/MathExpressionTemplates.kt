package com.indianservers.aiexplorer.gamifymaths

import kotlin.math.*
import kotlin.random.Random

internal val expressionTemplateGames=setOf("kitchen","decimal-harbor","math-market","percent-studio","data","data-story","geometry-proof","calculus-climber","chance","measure")
internal fun mathTemplateTokens(expression:String):List<String> = Regex("sin|cos|tan|π|e|\\d+(?:\\.\\d+)?|[()+−×÷^√!CP]")
    .findAll(expression.replace('-', '−')).map{it.value}.toList()

internal data class ExpressionRecipe(val name:String,val question:String,val expression:String,val expected:Double,val guidance:String,val visuals:Map<String,Double> = emptyMap())

/** Each entry is a mathematical recipe. Values are sampled only after the recipe is chosen. */
internal fun expressionRecipe(id:String,variant:Int,tier:Int,random:Random):ExpressionRecipe {
    val a=random.nextInt(4,13+tier*5);val b=random.nextInt(3,10+tier*3);val c=random.nextInt(2,7+tier);val d=random.nextInt(2,7+tier)
    val v=variant.mod(12)
    fun r(name:String,q:String,e:String,value:Double,hint:String)=ExpressionRecipe(name,q,e,value,hint)
    fun r(name:String,q:String,e:String,value:Int,hint:String)=r(name,q,e,value.toDouble(),hint)
    return when(id){
        "kitchen"->when(v){
            0->r("Combine deliveries","Three deliveries contain $a, $b and $c kg. Find the total.","$a + $b + $c",a+b+c,"Add all three deliveries.")
            1->r("Ingredient balance","Start with ${a+b+c} cups. Use $b cups, then $c cups. How many remain?","${a+b+c} − $b − $c",a,"Subtract each use from the stock.")
            2->r("Two batch orders","Make $a batches of $b rolls and $c batches of $d rolls. Find the total rolls.","$a × $b + $c × $d",a*b+c*d,"Multiply each batch order, then add.")
            3->r("Share after cooking","Cook ${a*b} portions and ${c*b} portions. Share all portions equally among $b tables. How many per table?","(${a*b} + ${c*b}) ÷ $b",a+c,"Combine the quantities before dividing.")
            4->r("Recipe scaling","A recipe uses $a g flour and $b g oats per batch. Make $c batches. Find the combined mass.","($a + $b) × $c",(a+b)*c,"Add ingredients for one batch, then scale.")
            5->r("Packing leftovers","Bake ${a*c} biscuits. Keep $b and share the rest among $c boxes. Find biscuits per box (6 decimal places).","(${a*c} − $b) ÷ $c",(a*c-b).toDouble()/c,"Subtract the reserved biscuits, then share.")
            6->r("Meal cost","Buy $a meals at ₹$b each and pay ₹$c delivery. Find the total bill.","$a × $b + $c",a*b+c,"Multiply meal count by price, then add delivery.")
            7->r("Kitchen change","Pay ₹${a*b+c+d} for $a ingredients costing ₹$b each. Find the change.","${a*b+c+d} − $a × $b",c+d,"Calculate the cost before subtracting it from payment.")
            8->r("Waste allowance","Prepare ${a*100} g. Waste ${b*5}%. Find usable grams.","${a*100} × (100 − ${b*5}) ÷ 100",a*(100-b*5),"Keep the percentage remaining after waste.")
            9->r("Fill two containers","A pump fills $a ml per second for $b seconds, then $c seconds. Find the volume.","$a × ($b + $c)",a*(b+c),"Add the durations, then multiply by the rate.")
            10->r("Recipe ratio","Flour:sugar is $a:$b. Use ${a*c} g flour. Find the combined flour and sugar mass.","${a*c} + ${a*c} ÷ $a × $b",(a+b)*c,"Find the scale factor, then add both ingredient masses.")
            else->r("Average serving","Two pots contain ${a*c} ml and ${b*c} ml. Pour $c equal servings. Find ml per serving.","(${a*c} + ${b*c}) ÷ $c",a+b,"Combine the pots and divide by serving count.")
        }
        "decimal-harbor"->{val x=a/10.0;val y=b/10.0;val z=c/10.0
            when(v){
                0->r("Cargo manifest","Add $x t, $y t and $z t of cargo.","$x + $y + $z",x+y+z,"Align decimal places and add every load.")
                1->r("Unload in stages","Start with ${roundNumber(x+y+z)} t. Unload $y t and $z t. Find the remainder.","${roundNumber(x+y+z)} − $y − $z",x,"Subtract both unloaded quantities.")
                2->r("Repeated voyages","Carry $x t and $y t per voyage. Make $c voyages. Find total tonnes.","($x + $y) × $c",(x+y)*c,"Find one voyage's load, then scale.")
                3->r("Equal crates","Divide ${roundNumber(x+y)} t equally among $c crates, then add $z t to each. Find tonnes per crate (6 decimal places).","($x + $y) ÷ $c + $z",(x+y)/c+z,"Share the initial load before adding the extra cargo.")
                4->r("Harbor invoice","Ship $c loads costing ₹$x each and pay ₹$y handling. Find the bill.","$c × $x + $y",c*x+y,"Multiply shipment count by cost and add handling.")
                5->r("Mass conversion","A crate contains $x kg plus $y kg. Find the mass in grams.","($x + $y) × 1000",(x+y)*1000,"Combine kilograms, then multiply by 1000.")
                6->r("Travel time","Sail $x km and $y km at $z km/h. Find hours (6 decimal places).","($x + $y) ÷ $z",(x+y)/z,"Time equals total distance divided by speed.")
                7->r("Decimal comparison","Find how much $c loads of $x t exceed $y t.","$c × $x − $y",c*x-y,"Compute the repeated load before comparing.")
                8->r("Two freight charges","Charge ₹$x for each of $c parcels and ₹$y for each of $d parcels. Find revenue.","$x × $c + $y × $d",x*c+y*d,"Add the two groups' charges.")
                9->r("Average cargo","Three crates weigh $x, $y and $z t. Find their mean (6 decimal places).","($x + $y + $z) ÷ 3",(x+y+z)/3,"Divide total weight by three.")
                10->r("Decimal scale","Increase $x t by ${b*5}%. Find the new mass.","$x × (100 + ${b*5}) ÷ 100",x*(100+b*5)/100,"Keep the original 100% and add the increase.")
                else->r("Container allocation","Load $x t in each of $c containers, then split the cargo equally between $d ships. Find tonnes per ship (6 decimal places).","$x × $c ÷ $d",x*c/d,"Compute total cargo, then share it.")
            }
        }
        "percent-studio","math-market"->{val price=a*100;val p=b*5;val q=c*5
            val recipe=when(v){
                0->r("Percent of a quantity","Find $p% of $price.","$p × $price ÷ 100",p*price/100,"Multiply by the percentage and divide by 100.")
                1->r("Discount amount","A ₹$price item has a $p% discount. Find the saving.","$price × $p ÷ 100",price*p/100,"The saving is the stated percentage of the original price.")
                2->r("Sale price","Reduce ₹$price by $p%. Find the sale price.","$price × (100 − $p) ÷ 100",price*(100-p)/100,"Use the percentage left after the discount.")
                3->r("Tax included","Add $q% tax to ₹$price. Find the final price.","$price × (100 + $q) ÷ 100",price*(100+q)/100,"Add tax to the original 100%.")
                4->r("Successive discounts","A ₹$price item is discounted $p%, then $q%. Find its final price.","$price × (100 − $p) ÷ 100 × (100 − $q) ÷ 100",price*(100-p)/100.0*(100-q)/100,"Apply the second discount to the reduced price.")
                5->r("Percentage change","A price rises from ₹$price to ₹${price+a*p}. Find the percentage increase.","(${price+a*p} − $price) ÷ $price × 100",p.toDouble(),"Divide the increase by the original price, then multiply by 100.")
                6->r("Reverse percentage","$p% of a quantity is ${a*p}. Find the original quantity.","${a*p} × 100 ÷ $p",a*100,"Divide by the percentage as a fraction of 100.")
                7->r("Profit margin","Buy for ₹$price and sell for ₹${price+a*q}. Find profit as a percentage of cost.","(${price+a*q} − $price) ÷ $price × 100",q.toDouble(),"Profit percentage uses the purchase cost as the base.")
                8->r("Checkout change","Pay ₹${price*c+d*10} for $c items at ₹$price each. Find change.","${price*c+d*10} − $c × $price",d*10,"Multiply unit price by quantity before subtracting.")
                9->r("Buy several at a discount","Buy $c items priced ₹$price each with $p% off the whole bill. Find the amount paid.","$c × $price × (100 − $p) ÷ 100",c*price*(100-p)/100,"Find the full bill, then apply the reduction.")
                10->r("Discount then tax","Take $p% off ₹$price, then add $q% tax. Find the final amount.","$price × (100 − $p) ÷ 100 × (100 + $q) ÷ 100",price*(100-p)/100.0*(100+q)/100,"Tax applies to the discounted amount.")
                else->r("Compare two offers","Offer A saves $p% on ₹$price. Offer B saves ₹${a*q}. Find A's saving minus B's saving.","$price × $p ÷ 100 − ${a*q}",a*(p-q),"Calculate both savings using the same original price.")
            }
            val visuals=when(v){
                0,1,2,4,10,11->mapOf("percent" to p.toDouble(),"whole" to price.toDouble())
                3->mapOf("percent" to q.toDouble(),"whole" to price.toDouble())
                6->mapOf("percent" to p.toDouble(),"knownPart" to (a*p).toDouble())
                9->mapOf("percent" to p.toDouble(),"whole" to (c*price).toDouble())
                else->emptyMap()
            }
            recipe.copy(visuals=visuals)
        }
        "chance"->{val total=a+b+c;val trials=d*10;val win=a*10;val loss=b*5
            when(v){
                0->r("Three-colour draw","A box has $a red, $b blue and $c green counters. Draw one uniformly. Find P(red). Give a fraction or 6-decimal answer.","$a ÷ ($a + $b + $c)",a.toDouble()/total,"Favourable outcomes are red; all three colours contribute to the total.")
                1->r("Complement event","A jar has $a gold and $b silver tokens. Find P(not gold) on one random draw (fraction or 6 decimals).","1 − $a ÷ ($a + $b)",b.toDouble()/(a+b),"Subtract P(gold) from one.")
                2->r("Either colour","A box has $a red, $b blue and $c green tokens. Find P(red or green) in one draw (fraction or 6 decimals).","($a + $c) ÷ ($a + $b + $c)",(a+c).toDouble()/total,"Add disjoint favourable colours before dividing by the total.")
                3->r("Independent draws","A bag has $a red and $b blue balls. Replace the first draw before drawing again. Find P(both red), to 6 decimals.","($a ÷ ($a + $b)) ^ 2",(a.toDouble()/(a+b)).pow(2),"Replacement keeps both probabilities the same.")
                4->r("Dependent draws","A bag has $a red and $b blue balls. Draw twice without replacement. Find P(both red), to 6 decimals.","$a ÷ ${a+b} × ${a-1} ÷ ${a+b-1}",a.toDouble()/(a+b)*(a-1)/(a+b-1),"After one red draw, both the red count and total decrease.")
                5->r("Ordered mixed draw","A bag has $a red and $b blue balls. Draw twice without replacement. Find P(red then blue), to 6 decimals.","$a ÷ ${a+b} × $b ÷ ${a+b-1}",a.toDouble()/(a+b)*b/(a+b-1),"The blue count is unchanged after removing a red ball.")
                6->r("At least one success","A token is red with probability $a/${a+b}. Make two independent draws. Find P(at least one red), to 6 decimals.","1 − ($b ÷ ${a+b}) ^ 2",1-(b.toDouble()/(a+b)).pow(2),"Use one minus the probability of no red draws.")
                7->r("Expected count","A spinner has $a winning sectors and $b losing sectors, all equally likely. Spin $trials times. Find expected wins (6 decimals).","$trials × $a ÷ ${a+b}",trials*a.toDouble()/(a+b),"Expected count equals trials times win probability.")
                8->r("Expected net payoff","Win ₹$win with probability $c/${c+d}; otherwise lose ₹$loss. Find the expected net payoff (6 decimals).","$win × $c ÷ ${c+d} − $loss × $d ÷ ${c+d}",(win*c-loss*d).toDouble()/(c+d),"Weight both net outcomes by their probabilities.")
                9->r("Experimental probability","In $total trials, red occurred $a times, blue $b and green $c. Find the experimental P(blue or green), to 6 decimals.","($b + $c) ÷ $total",(b+c).toDouble()/total,"Divide the observed event frequency by total trials.")
                10->r("Fair numbered tickets","Tickets 1 to ${a*2} are equally likely. Find P(even or ticket 1), to 6 decimals.","($a + 1) ÷ ${a*2}",(a+1).toDouble()/(a*2),"There are a even tickets and one additional, disjoint ticket.")
                else->r("Exactly one success","A draw wins with probability $a/${a+b}. Two independent draws are made. Find P(exactly one win), to 6 decimals.","2 × $a ÷ ${a+b} × $b ÷ ${a+b}",2.0*a*b/(a+b).toDouble().pow(2),"Count win–lose and lose–win; the orders are disjoint.")
            }
        }
        "data","data-story"->{val xs=listOf(a,b,c,d);val text=xs.shuffled(random).joinToString();val sum=xs.sum();val sorted=xs.sorted();val max=xs.max();val min=xs.min()
            when(v){
                0->r("Mean evidence","Data: $text. Find the mean (6 decimals).","($a + $b + $c + $d) ÷ 4",sum/4.0,"Add every observation and divide by four.")
                1->r("Range evidence","Data: $text. Find the range plus the minimum.","($max − $min) + $min",max,"Range is maximum minus minimum.")
                2->r("Midrange","Data: $text. Find the midpoint of the smallest and largest values.","($min + $max) ÷ 2",(min+max)/2.0,"Midrange averages the two extremes, not every observation.")
                3->r("Even median","Data: $text. Find the median.","(${sorted[1]} + ${sorted[2]}) ÷ 2",(sorted[1]+sorted[2])/2.0,"Sort first, then average the two middle observations.")
                4->r("Weighted mean","There are $c observations equal to $a and $d observations equal to $b. Find their mean (6 decimals).","($a × $c + $b × $d) ÷ ($c + $d)",(a*c+b*d).toDouble()/(c+d),"Use the frequencies as weights and divide by total frequency.")
                5->r("Missing observation","Four observations have mean $a. Three are ${a-1}, ${a+2} and ${a-3}. Find the missing observation.","4 × $a − ${a-1} − ${a+2} − ${a-3}",a+2,"Recover the total from mean times count, then subtract known observations.")
                6->r("Updated mean","The mean of $c readings is $a. A new reading $b is added. Find the new mean (6 decimals).","($c × $a + $b) ÷ ${c+1}",(c*a+b).toDouble()/(c+1),"Recover the old total, add the new reading, then use the new count.")
                7->r("Mean after removal","The mean of $c readings is $a. Remove a reading $b. Find the remaining mean (6 decimals).","($c × $a − $b) ÷ ${c-1}",(c*a-b).toDouble()/(c-1),"Subtract the removed value from the total and reduce the count.")
                8->r("Linear data transform","A dataset has mean $a. Multiply every value by $c and add $b. Find the new mean.","$a × $c + $b",a*c+b,"The mean undergoes the same linear transformation.")
                9->r("Population variance","Data: ${a-b}, $a, ${a+b}. Find the population variance (6 decimals).","($b ^ 2 + $b ^ 2) ÷ 3",2.0*b*b/3,"Average squared deviations using population count three.")
                10->r("Population deviation","Data: ${a-b}, $a, ${a+b}. Find the population standard deviation (6 decimals).","√ (($b ^ 2 + $b ^ 2) ÷ 3)",sqrt(2.0*b*b/3),"Take the square root of population variance.")
                else->r("Combined groups","Group A has $c readings with mean $a; group B has $d with mean $b. Find their combined total.","$c × $a + $d × $b",c*a+d*b,"Find each group's total before combining.")
            }
        }
        "calculus-climber"->when(v){
            0->r("Quadratic slope","For f(x) = ${c}x² + ${b}x, construct f′($a).","2 × $c × $a + $b",2*c*a+b,"Differentiate using the power rule, then substitute.")
            1->r("Cubic slope","For f(x) = ${c}x³ + ${b}x, construct f′($a).","3 × $c × $a ^ 2 + $b",3*c*a*a+b,"The derivative of x³ is 3x².")
            2->r("Second derivative","For f(x) = ${c}x³ + ${b}x², find f″($a).","6 × $c × $a + 2 × $b",6*c*a+2*b,"Apply the power rule twice.")
            3->r("Linear accumulation","Integrate ${2*c}x + $b from 0 to $a.","$c × $a ^ 2 + $b × $a",c*a*a+b*a,"Integrate each term and evaluate both bounds.")
            4->r("Quadratic accumulation","Integrate ${3*c}x² + $b from 0 to $a.","$c × $a ^ 3 + $b × $a",c*a*a*a+b*a,"An antiderivative is cx³ + bx.")
            5->r("Two nonzero bounds","Integrate ${2*c}x from $d to ${d+a}.","$c × ((${d+a}) ^ 2 − $d ^ 2)",c*((d+a)*(d+a)-d*d),"Evaluate the antiderivative at upper and lower limits.")
            6->r("Tangent intercept","For f(x) = ${c}x², find the y-intercept of the tangent at x=$a.","$c × $a ^ 2 − 2 × $c × $a × $a",-c*a*a,"Use intercept = f(a) − a f′(a).")
            7->r("Difference quotient limit","Find the limit as x→$a of (x² + ${b}x − ${a*a+b*a})/(x − $a).","2 × $a + $b",2*a+b,"This limit is the derivative of x² + bx at a.")
            8->r("Product rule","Let f(x)=x(${c}x + $b). Find f′($a).","2 × $c × $a + $b",2*c*a+b,"Expand the product or apply the product rule.")
            9->r("Chain rule","For f(x)=(${c}x + $b)², find f′($a).","2 × $c × ($c × $a + $b)",2*c*(c*a+b),"Multiply the outer derivative by the inner derivative.")
            10->r("Average rate","For f(x)=${c}x², find the average rate of change from x=$d to x=${d+a}.","$c × ((${d+a}) ^ 2 − $d ^ 2) ÷ $a",c*(2*d+a),"Divide the change in f by the change in x.")
            else->r("Stationary value","For f(x)=${c}x² − ${2*c*a}x + $b, find its minimum value.","$b − $c × $a ^ 2",b-c*a*a,"Set the derivative to zero and substitute the stationary input.")
        }
        "geometry-proof","measure"->when(v){
            0->r("Perimeter","A rectangular base is $a m × $b m. Find its perimeter in metres.","2 × ($a + $b)",2*(a+b),"Add length and width, then double.")
            1->r("Triangle area","A triangle has base ${2*a} m and height $b m. Find its area.","${2*a} × $b ÷ 2",a*b,"A triangle has half the area of the matching rectangle.")
            2->r("Box volume","A box is $a × $b × $c cm. Find its volume.","$a × $b × $c",a*b*c,"Multiply all three perpendicular dimensions.")
            3->r("Missing angle","Two triangle angles are ${a*3}° and ${b*3}°. Find the third angle.","180 − ${a*3} − ${b*3}",180-a*3-b*3,"The interior angles of a triangle sum to 180°.")
            4->r("Exterior angle","A triangle has remote interior angles ${a*3}° and ${b*3}°. Find twice their corresponding exterior angle.","2 × (${a*3} + ${b*3})",2*(a*3+b*3),"An exterior angle equals the sum of the two remote interior angles.")
            5->r("Composite area","A $a × $b rectangle has a $c × $d rectangle attached without overlap. Find total area.","$a × $b + $c × $d",a*b+c*d,"Add the disjoint rectangle areas.")
            6->r("Frame area","An outer square has side ${a+c}; an inner square has side $a. Find the frame area.","${a+c} ^ 2 − $a ^ 2",(a+c)*(a+c)-a*a,"Subtract the inner area from the outer area.")
            7->r("Scale drawing","A rectangle is $a × $b units. Scale each dimension by $c. Find its new area.","$a × $b × $c ^ 2",a*b*c*c,"Scaling lengths by c scales area by c².")
            8->r("Cuboid surface","A closed cuboid measures $a × $b × $c. Find its surface area.","2 × ($a × $b + $b × $c + $a × $c)",2*(a*b+b*c+a*c),"Add the three different face areas and double.")
            9->r("Circle circumference","A circle has radius $a. Using π as the constant, find its circumference to 6 decimals.","2 × π × $a",2*Math.PI*a,"Circumference equals 2πr.")
            10->r("Trapezium area","A trapezium has parallel sides $a and $b and height $c. Find its area.","($a + $b) × $c ÷ 2",(a+b)*c/2.0,"Average the parallel sides, then multiply by height.")
            else->r("Polygon angle sum","A polygon has ${a+3} sides. Find its interior-angle sum.","(${a+3} − 2) × 180",(a+1)*180,"Split an n-sided polygon into n−2 triangles.")
        }
        else->error("No expression recipe for $id")
    }
}

internal fun expandExpressionTemplate(id:String,base:WorkshopChallenge,variant:Int,tier:Int,random:Random):WorkshopChallenge {
    val recipe=expressionRecipe(id,variant,tier,random)
    val tokens=mathTemplateTokens(recipe.expression)
    val evaluated=checkNotNull(evaluateMathAssembly(tokens)){"Invalid template $id/$variant: ${recipe.expression}"}
    check(abs(evaluated.value-recipe.expected)<maxOf(1e-7,abs(recipe.expected)*1e-12)){"Template answer mismatch $id/$variant"}
    val answer=roundNumber(recipe.expected)
    val options=linkedSetOf(answer)
    var offset=1
    while(options.size<4){options+=roundNumber(recipe.expected+offset++ * if(abs(recipe.expected)<1).01 else 1.0)}
    val explanation="${recipe.guidance} ${recipe.expression} = $answer."
    val source=GeneratedRound(recipe.name,recipe.question,recipe.expected,explanation,recipe.guidance,options.shuffled(random),operands=tokens.mapNotNull(String::toDoubleOrNull).distinct(),visuals=if(id in setOf("geometry-proof","measure") && variant.mod(12)==0){val nums=Regex("\\d+").findAll(recipe.question).take(2).map{it.value.toDouble()}.toList();mapOf("length" to nums[0],"width" to nums[1])}else recipe.visuals)
    return base.copy(title=recipe.name,prompt=recipe.question,hint=recipe.guidance,explanation=explanation,source=source,numbers=(source.operands+listOf(0.0,1.0,2.0,100.0)).distinct(),witness=tokens)
}
