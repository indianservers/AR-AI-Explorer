package com.indianservers.aiexplorer.gamifymaths

import kotlin.math.*
import kotlin.random.Random

internal val speedTemplateCatalogue:List<MathTemplateDescriptor> by lazy {
    buildList{
        listOf("Addition","Subtraction","Multiplication","Division","Percent").forEach{op->repeat(8){v->add(MathTemplateDescriptor("speed-basic.$op.$v","speed-basic",null,v))}}
        listOf("Advanced Calculation","Algebra","Trigonometry").forEach{topic->repeat(12){v->add(MathTemplateDescriptor("speed-advanced.$topic.$v","speed-advanced",null,v))}}
        listOf("speed-basic","speed-advanced").forEach{id->for(mastery in 1..2)repeat(8){v->add(MathTemplateDescriptor("$id.mastery$mastery.$v",id,null,v))}}
    }
}

internal fun generateTemplateSpeedProblem(mode:SpeedCalculationMode,digits:Int,basic:Set<String>,advanced:Set<String>,random:Random,mastery:Int=0,forcedVariant:Int?=null):SpeedProblem {
    val topic=if(mode==SpeedCalculationMode.Basic)basic.ifEmpty{setOf("Addition")}.sorted().random(random)else advanced.ifEmpty{setOf("Advanced Calculation")}.sorted().random(random)
    val variant=forcedVariant ?: random.nextInt(if(mastery>0 || mode==SpeedCalculationMode.Basic)8 else 12)
    val recipe=if(mastery>0)masterySpeedRecipe(mode,mastery,variant,random)
        else if(mode==SpeedCalculationMode.Basic)basicSpeedRecipe(topic,variant,digits,random)
        else advancedSpeedRecipe(topic,variant,random)
    val parsed=checkNotNull(evaluateMathAssembly(mathTemplateTokens(recipe.expression))){"Invalid sprint recipe ${recipe.name}"}
    check(abs(parsed.value-recipe.expected)<maxOf(1e-7,abs(recipe.expected)*1e-12)){"Sprint answer mismatch: $recipe"}
    val id=if(mode==SpeedCalculationMode.Basic)"speed-basic"else "speed-advanced"
    return SpeedProblem(recipe.question,roundNumber(recipe.expected),if(mastery>0)"Mastery $mastery · ${recipe.name}"else topic,
        solution="${recipe.guidance} ${recipe.expression} = ${roundNumber(recipe.expected)}.",templateId=if(mastery>0)"$id.mastery$mastery.${variant.mod(8)}"else "$id.$topic.${variant.mod(if(mode==SpeedCalculationMode.Basic)8 else 12)}",solutionTokens=mathTemplateTokens(recipe.expression))
}

private fun basicSpeedRecipe(topic:String,v:Int,digits:Int,r:Random):ExpressionRecipe {
    val low=when(digits){1->1;2->10;else->100};val high=when(digits){1->10;2->100;else->1000}
    val a=r.nextInt(low,high);val b=r.nextInt(low,high);val c=r.nextInt(2,10);val d=r.nextInt(2,10)
    fun recipe(name:String,e:String,answer:Double,q:String=e)=ExpressionRecipe(name,q,e,answer,"Evaluate the grouped operations in order.")
    fun recipe(name:String,e:String,answer:Int,q:String=e)=recipe(name,e,answer.toDouble(),q)
    return when(topic){
        "Addition"->when(v.mod(8)){
            0->recipe("Two-term sum","$a + $b",a+b)
            1->recipe("Three-term sum","$a + $b + $c",a+b+c)
            2->recipe("Grouped sum","($a + $b) + ($c + $d)",a+b+c+d)
            3->recipe("Doubles","$a + $a + $b",2*a+b)
            4->recipe("Repeated addition","$a + $b + $b + $c",a+2*b+c)
            5->recipe("Decimal sum","${a/10.0} + ${b/10.0} + ${c/10.0}",(a+b+c)/10.0)
            6->recipe("Near hundred","${a*10} + $b + $c",a*10+b+c)
            else->recipe("Two grouped pairs","($a + $c) + ($b + $d)",a+b+c+d)
        }
        "Subtraction"->when(v.mod(8)){
            0->recipe("Difference","${max(a,b)} − ${min(a,b)}",abs(a-b))
            1->recipe("Successive subtraction","${a+b+c} − $b − $c",a)
            2->recipe("Subtract a group","${a+b+c} − ($b + $c)",a)
            3->recipe("Nested difference","${a+b} − ($b − $c)",a+c)
            4->recipe("Repeated difference","${a+b+c+d} − $b − $c − $d",a)
            5->recipe("Decimal difference","${(a+b)/10.0} − ${b/10.0}",a/10.0)
            6->recipe("Signed subtraction","$a − $b − $c",a-b-c)
            else->recipe("Difference of differences","(${a+b} − $b) − ($c − $d)",a-c+d)
        }
        "Multiplication"->when(v.mod(8)){
            0->recipe("Product","$a × $c",a*c)
            1->recipe("Three factors","$a × $c × $d",a*c*d)
            2->recipe("Square product","$c × $c × $a",c*c*a)
            3->recipe("Grouped factors","($a × $c) × ($d × 2)",a*c*d*2)
            4->recipe("Tenths product","${a/10.0} × $c",a*c/10.0)
            5->recipe("Times a multiple","$a × ${c*10}",a*c*10)
            6->recipe("Fourth factor","$a × $c × $d × 2",a*c*d*2)
            else->recipe("Two decimal factors","${a/10.0} × ${b/10.0}",a*b/100.0)
        }
        "Division"->when(v.mod(8)){
            0->recipe("Exact quotient","${a*c} ÷ $c",a)
            1->recipe("Chained quotient","${a*c*d} ÷ $c ÷ $d",a)
            2->recipe("Grouped divisor","${a*c*d} ÷ ($c × $d)",a)
            3->recipe("Divide a quotient","${a*c} ÷ (${c*d} ÷ $d)",a)
            4->recipe("Fractional quotient","$a ÷ $c",a.toDouble()/c)
            5->recipe("Decimal quotient","${a/10.0} ÷ $c",a/10.0/c)
            6->recipe("Quotient of quotients","(${a*c} ÷ $c) ÷ (${d*c} ÷ $c)",a.toDouble()/d)
            else->recipe("Divide by a tenth","$a ÷ ${c/10.0}",a*10.0/c)
        }
        else->{val percent=listOf(5,10,15,20,25,40,50,60,75).random(r);val amount=a*100
            when(v.mod(8)){
                0->recipe("Percentage","$percent × $amount ÷ 100",percent*amount/100,"$percent% of $amount")
                1->recipe("Percentage increase","$amount × (100 + $percent) ÷ 100",amount*(100+percent)/100,"Increase $amount by $percent%.")
                2->recipe("Percentage decrease","$amount × (100 − $percent) ÷ 100",amount*(100-percent)/100,"Decrease $amount by $percent%.")
                3->recipe("Repeated percentage","$c × $amount × $percent ÷ 100",c*amount*percent/100,"Find $percent% of $c lots of $amount.")
                4->recipe("Reverse percentage","${a*percent} × 100 ÷ $percent",a*100,"$percent% of x is ${a*percent}. Find x.")
                5->recipe("Complement percentage","$amount × (100 − $percent) ÷ 100",amount*(100-percent)/100,"$percent% of $amount is used. Find the remainder.")
                6->recipe("Percentage of combined amounts","($amount + ${b*100}) × $percent ÷ 100",(a+b)*percent,"Find $percent% of $amount + ${b*100}.")
                else->recipe("Two reductions","$amount × (100 − $percent) ÷ 100 × (100 − ${c*5}) ÷ 100",amount*(100-percent)/100.0*(100-c*5)/100,"Reduce $amount by $percent%, then by ${c*5}%.")
            }
        }
    }
}

private fun advancedSpeedRecipe(topic:String,v:Int,r:Random):ExpressionRecipe {
    val a=r.nextInt(3,15);val b=r.nextInt(2,12);val c=r.nextInt(2,8);val d=r.nextInt(2,8)
    fun f(name:String,e:String,value:Double,q:String=e)=ExpressionRecipe(name,q,e,value,"Respect brackets, powers and function arguments; angles are in degrees.")
    fun f(name:String,e:String,value:Int,q:String=e)=f(name,e,value.toDouble(),q)
    return when(topic){
        "Trigonometry"->{val angle=listOf(0,30,45,60,120,135,180,210,225,240,300,315,360).random(r);val x=Math.toRadians(angle.toDouble());val s=sin(x);val co=cos(x);val t=tan(x)
            when(v.mod(12)){
                0->f("Scaled sine","$a × sin ($angle)",a*s)
                1->f("Scaled cosine","$a × cos ($angle)",a*co)
                2->f("Scaled tangent","$a × tan ($angle)",a*t)
                3->f("Sine with offset","$a × sin ($angle) + $b",a*s+b)
                4->f("Cosine with offset","$a × cos ($angle) − $b",a*co-b)
                5->f("Pythagorean identity","sin ($angle) ^ 2 + cos ($angle) ^ 2",1.0)
                6->f("Sine–cosine sum","$a × sin ($angle) + $b × cos ($angle)",a*s+b*co)
                7->f("Sine–cosine difference","$a × sin ($angle) − $b × cos ($angle)",a*s-b*co)
                8->f("Negative angle","$a × sin (−$angle) + $b",-a*s+b)
                9->f("Periodic angle","$a × cos (${angle+360}) + $b",a*co+b)
                10->f("Tangent with offset","$a × tan ($angle) − $b",a*t-b)
                else->f("Scaled identity","$a × (sin ($angle) ^ 2 + cos ($angle) ^ 2) + $b",a+b)
            }
        }
        "Algebra"->when(v.mod(12)){
            0->f("Linear equation","(${a*b+c} − $c) ÷ $a",b,"${a}x + $c = ${a*b+c}. Find x.")
            1->f("Grouped equation","${a*(b+c)} ÷ $a − $c",b,"$a(x + $c) = ${a*(b+c)}. Find x.")
            2->f("Subtracted constant","(${a*b-c} + $c) ÷ $a",b,"${a}x − $c = ${a*b-c}. Find x.")
            3->f("Two sides","(${a*b+d} − $d) ÷ $a",b,"${a+c}x + $d = ${c}x + ${a*b+d}. Find x.")
            4->f("Divided variable","($a − $c) × $d",(a-c)*d,"x/$d + $c = $a. Find x.")
            5->f("Ratio equation","$a × $d + $c",a*d+c,"(x − $c)/$d = $a. Find x.")
            6->f("Linear function","$a × $b + $c",a*b+c,"f(x)=${a}x + $c. Find f($b).")
            7->f("Quadratic function","$a × $c ^ 2 + $b",a*c*c+b,"f(x)=${a}x² + $b. Find f($c).")
            8->f("Average equation","2 × $a − $b",2*a-b,"The mean of x and $b is $a. Find x.")
            9->f("Nested equation","(${a*b+c} − $c) ÷ $a − $d",b-d,"$a(x + $d) + $c = ${a*b+c}. Find x.")
            10->f("Fractional coefficient","${a*b} × $d ÷ $c",a*b*d.toDouble()/c,"($c/$d)x = ${a*b}. Find x to 6 decimals.")
            else->f("Difference of squares","($a + $b) ^ 2 − ($a − $b) ^ 2",4*a*b)
        }
        else->when(v.mod(12)){
            0->f("Square and offset","$a ^ 2 − $b",a*a-b)
            1->f("Exact root","√ (${a*a}) + $b",a+b)
            2->f("Grouped multiplication","($a + $b) × $c",(a+b)*c)
            3->f("Mixed precedence","$a + $b × $c − $d",a+b*c-d)
            4->f("Cube quotient","$a ^ 3 ÷ $a",a*a)
            5->f("Root product","√ (${a*a}) × √ (${b*b})",a*b)
            6->f("Power difference","($a + $b) ^ 2 − $a ^ 2",(a+b)*(a+b)-a*a)
            7->f("Grouped division","(${a*c} + ${b*c}) ÷ $c",a+b)
            8->f("Nested arithmetic","($a + $b × $c) ÷ $d",(a+b*c).toDouble()/d)
            9->f("Root of a power","√ (($a + $b) ^ 2)",a+b)
            10->f("Two squared terms","$a ^ 2 + $b ^ 2",a*a+b*b)
            else->f("Two grouped differences","($a − $b) × ($c + $d)",(a-b)*(c+d))
        }
    }
}

private fun masterySpeedRecipe(mode:SpeedCalculationMode,mastery:Int,v:Int,r:Random):ExpressionRecipe {
    val a=r.nextInt(4,if(mastery==2)30 else 16);val b=r.nextInt(2,12);val c=r.nextInt(2,9);val d=r.nextInt(2,9)
    val expressions=if(mode==SpeedCalculationMode.Basic)listOf(
        "($a + $b) × $c − $d" to ((a+b)*c-d).toDouble(),
        "(${a*20} − $b × $c) ÷ $d" to (a*20-b*c).toDouble()/d,
        "($a × $b + $c × $d) ÷ $c" to (a*b+c*d).toDouble()/c,
        "($a + $b + $c) × $d" to ((a+b+c)*d).toDouble(),
        "${a*100} × (100 − ${b*5}) ÷ 100" to (a*(100-b*5)).toDouble(),
        "($a × $b − $c) ÷ $d" to (a*b-c).toDouble()/d,
        "($a + $b) × ($c + $d)" to ((a+b)*(c+d)).toDouble(),
        "${a*100} × (100 + ${b*5}) ÷ 100 − ${c*10}" to (a*(100+b*5)-c*10).toDouble()
    )else listOf(
        "($a ^ 2 + $b ^ 2) ÷ $c" to (a*a+b*b).toDouble()/c,
        "$a × sin (30) + $b × cos (60) − $c" to ((a+b)*.5-c),
        "√ (${a*a}) × ($b + $c) − $d" to (a*(b+c)-d).toDouble(),
        "($a + $b) ^ 2 − $c ^ 2" to ((a+b)*(a+b)-c*c).toDouble(),
        "$a × (sin (45) ^ 2 + cos (45) ^ 2) + $b" to (a+b).toDouble(),
        "($a ^ 3 − $b × $c) ÷ $d" to (a*a*a-b*c).toDouble()/d,
        "($a + $b) × ($a − $b) + $c" to ((a+b)*(a-b)+c).toDouble(),
        "($a × tan (45) + $b) × $c − $d" to ((a+b)*c-d).toDouble()
    )
    val(e,answer)=expressions[v.mod(8)]
    return ExpressionRecipe("Multi-step challenge ${v.mod(8)+1}",e,e,answer,"Evaluate inner groups first, then powers/functions, then multiplication/division, then addition/subtraction.")
}
