from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GeneratedMathRounds.kt')
s=p.read_text(encoding='utf-8')
a=s.index('        "chance" -> {')
b=s.index('        "logic", "combinatorics" -> {',a)
s=s[:a]+'''        "chance" -> {
            val red=n(2,10+tier*5); val blue=n(2,10+tier*5); val total=red+blue
            val draws=n(2,8)
            fun probability(topic:String, prompt:String, value:Double, explanation:String, hint:String, inputs:List<Double>):GeneratedRound {
                val correct=roundNumber(value)
                val choices=mutableSetOf(correct)
                while(choices.size<choiceCount) {
                    val candidate=random.nextInt(0,101)/100.0
                    if(abs(candidate-value)>.000001) choices.add(roundNumber(candidate))
                }
                return GeneratedRound(topic, "$prompt Enter a fraction or decimal to 6 places.", value, explanation, hint, choices.shuffled(random), operands=inputs)
            }
            val trialCount=n(3,8); val wins=n(1,trialCount-1)
            when(level%15) {
                0 -> probability("Chance Explorer", "A bag has $red red and $blue blue counters. Draw one uniformly. Find P(red).", red.toDouble()/total,
                    "There are $red favourable counters among $total equally likely counters. Therefore P(red) = $red/$total.", "Count favourable outcomes and divide by all outcomes.", listOf(red.toDouble(),blue.toDouble()))
                1 -> integer("Spin and Win", "A fair spinner has $total equal sectors, $red marked WIN. Spin ${total*draws} times. What is the expected number of wins?", red*draws,
                    "Expected wins = trials × win probability = ${total*draws} × $red/$total = ${red*draws}. Actual wins may differ.", "Multiply trials by the probability of a win.", listOf(red,blue,draws))
                2 -> probability("Coin Flipper", "Flip a fair coin $trialCount times. Find P(exactly $wins heads).", combination(trialCount,wins)/2.0.pow(trialCount),
                    "Choose $wins head positions among $trialCount flips, then divide by the ${2.0.pow(trialCount).toInt()} equally likely sequences.", "Count the head-position combinations, then divide by all sequences.", listOf(trialCount.toDouble(),wins.toDouble()))
                3 -> probability("Card Picker", "A shuffled deck contains $red star cards and $blue moon cards. Pick one uniformly. Find P(moon).", blue.toDouble()/total,
                    "The deck contains $total cards and $blue favourable moon cards. The probability is $blue/$total.", "Count the requested cards among the entire deck.", listOf(red.toDouble(),blue.toDouble()))
                4 -> probability("Tree Builder", "A bag has $red red and $blue blue counters. Draw two WITHOUT replacement. Find P(both red).", red.toDouble()/total*(red-1)/(total-1),
                    "The first red probability is $red/$total. After a red draw, it becomes ${red-1}/${total-1}. Multiply these conditional probabilities.", "Update both counts after the first draw; it is not replaced.", listOf(red.toDouble(),blue.toDouble()))
                5 -> { val count=n(4,10); val pick=n(2,3); var result=1; repeat(pick){result*=count-it}
                    integer("Permutations Pro", "Arrange $pick different robots chosen from $count robots. How many ordered arrangements?", result,
                        "Multiply the decreasing available robot counts. Order matters, so do not divide by the arrangements of the selected robots.", "The next position has one fewer available robot.", listOf(count,pick)) }
                6 -> { val count=n(4,12); val pick=n(2,3)
                    integer("Combinations Champ", "Choose a team of $pick from $count players. How many unordered teams?", combination(count,pick),
                        "Count ordered selections, then divide by the $pick! orders of each team. This gives ${combination(count,pick)} teams.", "Divide away the repeated orders of the same team.", listOf(count,pick)) }
                7 -> { val p=n(1,4)/5.0; val chance=combination(trialCount,wins)*p.pow(wins)*(1-p).pow(trialCount-wins)
                    probability("Binomial Boost", "An independent trial succeeds with probability ${roundNumber(p)}. Run $trialCount trials. Find P(exactly $wins successes).", chance,
                        "Use C(n,k) × p^k × (1−p)^(n−k). The combination counts the positions of the $wins successes.", "Combine the position count with the success and failure probabilities.", listOf(trialCount.toDouble(),wins.toDouble(),p)) }
                8,9 -> { val z=n(-3,3); val mean=n(20,80); val sd=n(2,10); val x=mean+z*sd
                    integer(if(level%15==8) "Normal Navigator" else "Z-score Quest", "A normal model has mean $mean and standard deviation $sd. Observation $x has what z-score?", z,
                        "Standardize the observation: z = ($x − $mean) ÷ $sd = $z. The sign tells which side of the mean it lies on.", "Subtract the mean, then divide by the standard deviation.", listOf(mean,sd,x)) }
                10 -> integer("Sampling Safari", "A population is ${red*5}% supporters. In a random sample of ${draws*20} people, what is the expected supporter count?", red*draws,
                    "Multiply sample size by the supporter proportion: ${draws*20} × ${red*5}/100 = ${red*draws}. A random sample can vary.", "Expected counts use population proportion times sample size.", listOf(red*5,draws*20))
                else -> {
                    val center=n(10,limit*2); val spread=n(1,8)
                    val values=listOf(center-2*spread,center-spread,center,center+spread,center+2*spread)
                    when(level%15) {
                        11 -> integer("Mean Machine", "Find the mean of ${values.shuffled(random).joinToString()}.", center,
                            "The deviations around $center cancel. The total is ${5*center}; dividing by five gives the mean $center.", "Add all observations and divide by the count.", values)
                        12 -> integer("Median Mission", "Find the median of ${values.shuffled(random).joinToString()}.", center,
                            "Sorting the five observations puts $center in the third, middle position. That observation is the median.", "Sort the observations before choosing the middle.", values)
                        13 -> answer("Standard Deviation Detective", "Find the POPULATION standard deviation of ${values.shuffled(random).joinToString()} (6 decimal places).", kotlin.math.sqrt(2.0)*spread,
                            "The mean is $center. Squared deviations total ${10*spread*spread}; divide by 5, then take the square root of ${2*spread*spread}.", "Average squared deviations using the population count, then take the square root.", values.map(Int::toDouble), .5)
                        else -> integer("Statistics Challenge", "Find the POPULATION variance of ${values.shuffled(random).joinToString()}.", 2*spread*spread,
                            "The mean is $center. The five squared deviations sum to ${10*spread*spread}; divide by the population count 5 to get ${2*spread*spread}.", "Variance is the mean squared deviation, without a square root.", values)
                    }
                }
            }
        }
'''+s[b:]
p.write_text(s,encoding='utf-8')
