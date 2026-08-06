package com.indianservers.aiexplorer.features.numbertheory.visualproofs.data

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryParameter
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryPractice
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofStep
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualModel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualProofTopic

object NumberTheoryVisualProofCatalog {
    private fun steps(vararg rows: Triple<String, String, String>) = rows.mapIndexed { index, row ->
        NumberTheoryProofStep(
            id = "step-${index + 1}",
            instruction = row.first,
            observation = row.second,
            expression = row.third,
            spokenExpression = row.third
                .replace("×", " times ")
                .replace("÷", " divided by ")
                .replace("²", " squared")
                .replace("≡", " is congruent to "),
        )
    }

    private fun practice(prompt: String, options: List<String>, answer: Int, explanation: String) =
        NumberTheoryPractice(prompt, options, answer, explanation)

    val topics: List<NumberTheoryVisualProofTopic> = listOf(
        NumberTheoryVisualProofTopic(
            "natural-sum", NumberTheoryProofCategory.Patterns, "Sum of the First n Natural Numbers",
            setOf("sum 1 to n", "triangular sum", "staircase sum"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.Staircase, "1 + 2 + … + n = n(n + 1) ÷ 2",
            "one plus two through n equals n times n plus one divided by two",
            listOf(NumberTheoryParameter("n", "Rows n", 1, 20, 6)),
            steps(
                Triple("Build rows containing 1 through n dots.", "The row lengths grow by one.", "S = 1 + 2 + … + n"),
                Triple("Duplicate the staircase.", "There are now two equal copies.", "2S = 2(1 + 2 + … + n)"),
                Triple("Rotate the second copy.", "Its long rows fill the short rows of the first.", "n + 1 dots per joined row"),
                Triple("Join both copies.", "They form a complete n by n+1 rectangle.", "2S = n(n + 1)"),
                Triple("Split the rectangle equally.", "One staircase is exactly half.", "S = n(n + 1) ÷ 2"),
                Triple("Change n and verify.", "Only the dimensions change; the pairing always works.", "1 + … + n = n(n + 1) ÷ 2"),
            ),
            "How many columns will the joined rectangle have?",
            listOf("Two identical staircases always interlock.", "The rectangle has n rows and n+1 columns.", "Its n(n+1) dots split equally between the copies.", "Therefore one staircase has n(n+1)/2 dots for every positive n."),
            "Checking a few sums reveals a pattern, but the rearrangement is what proves it for every n.",
            practice("For n=8, what rectangle do two staircases form?", listOf("8×8", "8×9", "7×8", "9×9"), 1, "The joined row pairs 1 with 8, 2 with 7, and so on, giving n+1=9 columns."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "odd-sum", NumberTheoryProofCategory.Patterns, "Odd Numbers Build Squares",
            setOf("odd numbers make squares", "sum of odd numbers"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.TileGrid, "1 + 3 + 5 + … + (2n − 1) = n²",
            "the first n odd numbers sum to n squared",
            listOf(NumberTheoryParameter("n", "Square side n", 1, 15, 6)),
            steps(
                Triple("Start with one tile.", "It is a 1 by 1 square.", "1 = 1²"),
                Triple("Add an L-shaped border of three.", "The shape becomes 2 by 2.", "1 + 3 = 2²"),
                Triple("Add the next odd border.", "Two strips of length n and one corner grow the square.", "(n−1)² + (2n−1) = n²"),
                Triple("Tap each layer.", "Layer k contains exactly 2k−1 tiles.", "1, 3, 5, …"),
                Triple("Generalize the construction.", "Every new odd border makes the next complete square.", "Σ(2k−1) = n²"),
            ),
            "How many tiles must the next border contain?",
            listOf("An (n−1) square lacks one row and one column to become an n square.", "Those strips contain n+n tiles but share one corner.", "The new count is 2n−1.", "Repeating these borders produces exactly n² tiles."),
            "Counting both strips as n+n double-counts their shared corner.",
            practice("Which border grows a 5×5 square into a 6×6 square?", listOf("9", "10", "11", "12"), 2, "The difference 6²−5² is 36−25=11, the sixth odd number."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "triangular-numbers", NumberTheoryProofCategory.Patterns, "Triangular Numbers",
            setOf("triangle dots", "Tn", "triangular number"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.Staircase, "Tₙ = n(n + 1) ÷ 2", "T n equals n times n plus one divided by two",
            listOf(NumberTheoryParameter("n", "Rows n", 1, 20, 5)),
            steps(
                Triple("Grow a dot triangle row by row.", "Row n contributes n new dots.", "Tₙ = Tₙ₋₁ + n"),
                Triple("Make a second congruent triangle.", "Both have the same number of dots.", "2Tₙ"),
                Triple("Turn and join the copies.", "They complete a rectangle.", "2Tₙ = n(n+1)"),
                Triple("Halve the rectangle.", "One triangle is one of two equal parts.", "Tₙ = n(n+1)÷2"),
            ),
            "What is the difference between consecutive triangular numbers?",
            listOf("The next triangle adds one full row.", "That row contains n dots.", "Two copies always complete an n by n+1 rectangle.", "This proves both the recurrence and closed formula."),
            "Do not confuse the number of rows n with the total dot count Tₙ.",
            practice("What is T₇?", listOf("21", "28", "35", "49"), 1, "T₇=7×8÷2=28."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "consecutive-squares", NumberTheoryProofCategory.Patterns, "Difference of Consecutive Squares",
            setOf("square border", "difference squares"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.TileGrid, "(n + 1)² − n² = 2n + 1", "n plus one squared minus n squared equals two n plus one",
            listOf(NumberTheoryParameter("n", "Inner side n", 1, 20, 5)),
            steps(
                Triple("Build an n by n square.", "It contains n² tiles.", "n²"),
                Triple("Add one row and one column.", "The square side becomes n+1.", "(n+1)²"),
                Triple("Mark the shared corner once.", "The border is n+n+1, not n+n+2.", "2n+1"),
                Triple("Subtract the inner area.", "Only the odd border remains.", "(n+1)²−n²=2n+1"),
            ),
            "Why must the added border always be odd?",
            listOf("The two length-n strips contribute 2n tiles.", "One additional corner closes the larger square.", "Thus the difference is 2n+1.", "The geometric border and algebraic expansion count the same tiles."),
            "The corner is not part of both length-n strips; count it exactly once.",
            practice("What is 12²−11² without multiplying the squares?", listOf("21", "22", "23", "24"), 2, "Set n=11: 2n+1=23."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "arithmetic-sum", NumberTheoryProofCategory.Patterns, "Arithmetic Sequence Sum",
            setOf("arithmetic series", "Sn", "first plus last"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.BarStaircase, "Sₙ = n(a₁ + aₙ) ÷ 2", "sum equals number of terms times first plus last divided by two",
            listOf(NumberTheoryParameter("a", "First term", 1, 10, 2), NumberTheoryParameter("d", "Difference", 1, 8, 3), NumberTheoryParameter("n", "Terms", 2, 15, 6)),
            steps(
                Triple("Build bars for the sequence.", "Each bar grows by the common difference.", "a, a+d, …"),
                Triple("Duplicate and reverse the bars.", "Short and tall bars now pair.", "S + S"),
                Triple("Inspect every pair.", "Each has height first+last.", "a₁+aₙ"),
                Triple("Count n equal pairs.", "The doubled sum is a rectangle.", "2Sₙ=n(a₁+aₙ)"),
                Triple("Take one copy.", "Divide the rectangle by two.", "Sₙ=n(a₁+aₙ)÷2"),
            ),
            "What sum does every outer pair share?",
            listOf("Reversing does not change either copy's sum.", "Aligned terms all total a₁+aₙ.", "There are n aligned pairs.", "Half their rectangle gives the original series sum."),
            "For an odd number of terms, the middle term pairs with itself across the two copies.",
            practice("For 3,7,11,15, what is first+last?", listOf("15", "16", "18", "20"), 2, "3+15=18; every reversed pair has that same total."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "divisibility-3", NumberTheoryProofCategory.Divisibility, "Divisibility Rule for 3",
            setOf("division test", "digit sum", "divisible by three"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.PlaceValueBlocks, "N mod 3 = digit sum mod 3", "a number and its digit sum have equal remainder modulo three",
            listOf(NumberTheoryParameter("value", "Number", 10, 9999, 372)),
            steps(
                Triple("Split the number into place values.", "Each digit multiplies a power of ten.", "N=Σdₖ10ᵏ"),
                Triple("Group every power of ten by threes.", "10,100,1000 each leave one remainder unit.", "10ᵏ≡1 (mod 3)"),
                Triple("Move leftover units into one tray.", "The tray contains the digit sum.", "N≡Σdₖ (mod 3)"),
                Triple("Compare both remainders.", "They always match.", "3|N iff 3|digit sum"),
            ),
            "Will the digit-sum tray divide evenly into groups of three?",
            listOf("Ten is one more than a multiple of three.", "Every power of ten is therefore congruent to one.", "Each place contributes the same remainder as its digit.", "So the original number and digit sum share a remainder."),
            "Divisibility by 3 does not imply divisibility by 9.",
            practice("Is 4,572 divisible by 3?", listOf("Yes", "No", "Only by 9", "Cannot tell"), 0, "4+5+7+2=18, which is divisible by 3."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "factor-rectangles", NumberTheoryProofCategory.Factors, "Factor Pairs as Rectangles",
            setOf("factors", "rectangle factors", "factor grid"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.FactorRectangle, "a × b = N means a and b are factors of N", "a times b equals N means a and b form a factor pair",
            listOf(NumberTheoryParameter("value", "Tiles", 2, 120, 24)),
            steps(
                Triple("Lay out N unit tiles.", "Area is fixed at N.", "N tiles"),
                Triple("Try complete equal rows.", "No tile may be left over.", "rows×columns=N"),
                Triple("Record each rectangle.", "Turning it swaps the same pair.", "(a,b)=(b,a) as a pair"),
                Triple("Inspect all possibilities.", "A prime has only 1 by N.", "factor pairs"),
            ),
            "How many different rectangles can these tiles make?",
            listOf("A complete rectangle partitions N into equal rows.", "Its side lengths multiply to N.", "Every divisor creates exactly one partner N/divisor.", "Checking through √N finds every unordered pair."),
            "Rotating a 3×8 rectangle to 8×3 does not create a new factor pair.",
            practice("Which is not a factor pair of 24?", listOf("1×24", "2×12", "3×8", "5×6"), 3, "5×6=30, not 24."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "lcm-cycles", NumberTheoryProofCategory.GcdLcm, "LCM Through Repeating Cycles",
            setOf("lowest common multiple", "first alignment", "repeating events"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.CycleTrack, "lcm(a,b) is the first positive shared landing", "least common multiple is the first positive step shared by both cycles",
            listOf(NumberTheoryParameter("a", "Cycle A", 2, 15, 4), NumberTheoryParameter("b", "Cycle B", 2, 15, 6)),
            steps(
                Triple("Start both event tracks at zero.", "Their markers are aligned.", "0"),
                Triple("Advance each by its own period.", "Each visits its multiples.", "a,2a,… and b,2b,…"),
                Triple("Predict the first positive alignment.", "It must be divisible by both periods.", "common multiple"),
                Triple("Run until both markers meet.", "The first meeting is the least common multiple.", "lcm(a,b)"),
            ),
            "Which positive step will both tracks land on first?",
            listOf("One track marks all multiples of a.", "The other marks all multiples of b.", "Shared marks are common multiples.", "The earliest positive shared mark is their LCM."),
            "Zero is a common multiple but LCM means the least positive common multiple.",
            practice("Events repeat every 4 and 6 minutes. First reunion?", listOf("8", "10", "12", "24"), 2, "12 is the first positive number divisible by both 4 and 6."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "euclidean-algorithm", NumberTheoryProofCategory.GcdLcm, "Euclidean Algorithm",
            setOf("HCF", "highest common factor", "gcd rectangle"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.EuclideanRectangle, "gcd(a,b) = gcd(b, a mod b)", "greatest common divisor is preserved when replacing a by its remainder after division by b",
            listOf(NumberTheoryParameter("a", "Long side a", 2, 120, 48), NumberTheoryParameter("b", "Short side b", 2, 80, 18)),
            steps(
                Triple("Build an a by b rectangle.", "A common square side must divide both dimensions.", "a×b"),
                Triple("Tile with the largest b by b squares.", "The uncovered strip has width a mod b.", "a=qb+r"),
                Triple("Continue with b by r.", "A divisor common to a and b is common to b and r.", "gcd(a,b)=gcd(b,r)"),
                Triple("Repeat until remainder zero.", "The last nonzero divisor tiles every earlier rectangle.", "gcd"),
            ),
            "What width remains after the next square tiling?",
            listOf("Subtracting whole copies of b from a does not change common divisors.", "The remainder r=a−qb carries the same common-divisor information.", "Each smaller rectangle repeats this invariant.", "The final exact square side is the GCD."),
            "The last quotient is not the GCD; the last nonzero divisor is.",
            practice("48=2×18+12 and 18=1×12+6. What is gcd(48,18)?", listOf("2", "6", "12", "18"), 1, "12=2×6+0, so the last nonzero divisor is 6."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "unique-factorization", NumberTheoryProofCategory.Classical, "Fundamental Theorem of Arithmetic",
            setOf("prime factorization", "unique factors", "factor tree"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.PrimeFactorTree, "Every integer greater than 1 has one prime factorization, apart from order",
            "every integer greater than one has a unique prime factorization apart from factor order",
            listOf(NumberTheoryParameter("value", "Integer", 2, 500, 60)),
            steps(
                Triple("Choose a composite number.", "It can split into smaller factors.", "N=a×b"),
                Triple("Continue splitting composite branches.", "Every branch eventually reaches primes.", "prime leaves"),
                Triple("Try another factor-tree route.", "The branch order changes.", "different tree"),
                Triple("Sort the final prime blocks.", "Every route gives the same multiset.", "N=∏pᵢᵉⁱ"),
            ),
            "Will another factor-tree route change the final prime blocks?",
            listOf("Existence follows by repeatedly splitting composites into smaller factors.", "The process must terminate at primes.", "If two different prime products existed, a prime on one side would divide a prime on the other.", "That forces matching prime factors repeatedly, establishing uniqueness."),
            "The product p₁p₂…pₙ+1 in Euclid's proof need not itself be prime.",
            practice("What is the prime factorization of 60?", listOf("2×30", "2²×3×5", "3×20", "5×12"), 1, "Only 2²×3×5 is expressed entirely as prime factors."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "sieve", NumberTheoryProofCategory.Primes, "Sieve of Eratosthenes",
            setOf("prime grid", "find primes", "sieve"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.SieveGrid, "Cross multiples of each prime through √N; uncrossed values are prime",
            "cross multiples of each prime up to square root N; remaining values are prime",
            listOf(NumberTheoryParameter("limit", "Upper limit", 10, 100, 50)),
            steps(
                Triple("Circle 2.", "It is the first prime.", "2"),
                Triple("Cross multiples of 2 above it.", "Those numbers have factor 2.", "4,6,8,…"),
                Triple("Circle the next uncrossed number.", "It has no smaller prime factor.", "next prime"),
                Triple("Cross its remaining multiples.", "Start at p²; smaller multiples were handled earlier.", "p²,p(p+1),…"),
                Triple("Stop after p exceeds √N.", "Every composite ≤N has a factor ≤√N.", "p>√N"),
            ),
            "Why can crossing stop after √N?",
            listOf("If N=ab and both factors exceeded √N, their product would exceed N.", "Thus every composite has at least one factor at most √N.", "That factor has a prime divisor at most √N.", "Crossing multiples of those primes removes every composite."),
            "One is neither prime nor composite and must not be circled.",
            practice("For a sieve to 100, which prime is the last needed for crossing?", listOf("5", "7", "11", "13"), 1, "√100=10, so process primes 2,3,5,7; the next prime 11 is beyond the stopping point."),
            1,
        ),
        NumberTheoryVisualProofTopic(
            "modular-clock", NumberTheoryProofCategory.Modular, "Modular Arithmetic as a Clock",
            setOf("clock maths", "congruence", "negative modulo"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ModularClock, "a ≡ b (mod n) exactly when n divides a − b",
            "a is congruent to b modulo n exactly when n divides their difference",
            listOf(NumberTheoryParameter("value", "Integer a", -30, 40, 17), NumberTheoryParameter("modulus", "Modulus n", 2, 16, 12)),
            steps(
                Triple("Draw n positions around a clock.", "Positions represent remainders 0 through n−1.", "mod n"),
                Triple("Walk to integer a.", "Every complete turn uses n steps.", "a=qn+r"),
                Triple("Remove complete turns.", "Only the final position r remains.", "a≡r (mod n)"),
                Triple("Try negative integers.", "Backward movement normalizes to the same remainder set.", "0≤r<n"),
            ),
            "Which clock position will the integer land on?",
            listOf("A full turn changes the integer by n but not its position.", "Therefore integers at one position differ by multiples of n.", "Conversely, a difference divisible by n is a whole number of turns.", "This is exactly the definition of congruence modulo n."),
            "A remainder should be normalized into 0 through n−1, even for negative integers.",
            practice("Where does −1 land modulo 7?", listOf("−1", "0", "1", "6"), 3, "Moving one step backward from 0 reaches position 6, so −1≡6 mod 7."),
            1,
        ),
    ) + NumberTheoryPhase2PatternsCatalog.topics +
        NumberTheoryPhase2FactorsPrimesCatalog.topics +
        NumberTheoryPhase3Catalog.topics

    val completedTopics: List<NumberTheoryVisualProofTopic> get() = topics.filter { it.completedInPhase <= 3 }

    fun topic(id: String): NumberTheoryVisualProofTopic? = topics.firstOrNull { it.id == id }

    fun topicsFor(category: NumberTheoryProofCategory, includeRoadmap: Boolean = false): List<NumberTheoryVisualProofTopic> =
        topics.filter { it.category == category && (includeRoadmap || it.completedInPhase <= 3) }

    fun search(query: String, level: NumberTheoryProofLevel): List<NumberTheoryVisualProofTopic> {
        val normalized = query.trim().lowercase()
        return topics.filter {
            it.completedInPhase <= 3 && it.level.ordinal <= level.ordinal &&
                (normalized.isBlank() || normalized in it.title.lowercase() || it.aliases.any { alias -> normalized in alias.lowercase() })
        }
    }

    private fun roadmapTopics(): List<NumberTheoryVisualProofTopic> {
        val definitions = listOf(
            Triple("even-sum", "Sum of the First n Even Numbers", NumberTheoryProofCategory.Patterns),
            Triple("square-odd-difference", "Square Numbers as Consecutive Odd Sums", NumberTheoryProofCategory.Patterns),
            Triple("consecutive-integer-sum", "Sum of Consecutive Integers", NumberTheoryProofCategory.Patterns),
            Triple("divisibility-2", "Divisibility Rule for 2", NumberTheoryProofCategory.Divisibility),
            Triple("divisibility-9", "Divisibility Rule for 9", NumberTheoryProofCategory.Divisibility),
            Triple("divisibility-4", "Divisibility Rule for 4", NumberTheoryProofCategory.Divisibility),
            Triple("divisibility-8", "Divisibility Rule for 8", NumberTheoryProofCategory.Divisibility),
            Triple("divisibility-5-10", "Divisibility Rules for 5 and 10", NumberTheoryProofCategory.Divisibility),
            Triple("divisibility-11", "Divisibility Rule for 11", NumberTheoryProofCategory.Divisibility),
            Triple("parity-last-digit", "Why the Last Digit Determines Parity", NumberTheoryProofCategory.Divisibility),
            Triple("multiples-line", "Multiples on the Number Line", NumberTheoryProofCategory.Factors),
            Triple("gcd-grouping", "GCD Through Largest Equal Grouping", NumberTheoryProofCategory.GcdLcm),
            Triple("gcd-lcm-product", "Relationship Between GCD and LCM", NumberTheoryProofCategory.GcdLcm),
            Triple("prime-building-blocks", "Prime Factorization as Building Blocks", NumberTheoryProofCategory.Factors),
            Triple("composite-sqrt", "Why Composite Numbers Have a Factor at Most √n", NumberTheoryProofCategory.Primes),
            Triple("euclid-primes", "Infinitely Many Primes — Euclid's Proof", NumberTheoryProofCategory.Classical),
            Triple("prime-gaps", "Prime Gaps Explorer", NumberTheoryProofCategory.Primes),
            Triple("twin-primes", "Twin Primes Explorer", NumberTheoryProofCategory.Primes),
            Triple("modular-addition", "Addition Modulo n", NumberTheoryProofCategory.Modular),
            Triple("modular-multiplication", "Multiplication Modulo n", NumberTheoryProofCategory.Modular),
            Triple("negative-modulo", "Negative Numbers Modulo n", NumberTheoryProofCategory.Modular),
            Triple("remainder-classes", "Remainder Classes", NumberTheoryProofCategory.Modular),
            Triple("exponent-product", "Product of Powers", NumberTheoryProofCategory.Powers),
            Triple("exponent-quotient", "Quotient of Powers", NumberTheoryProofCategory.Powers),
            Triple("power-of-power", "Power of a Power", NumberTheoryProofCategory.Powers),
            Triple("zero-exponent", "Why a⁰ = 1", NumberTheoryProofCategory.Powers),
            Triple("negative-exponent", "Negative Exponents", NumberTheoryProofCategory.Powers),
            Triple("perfect-numbers", "Perfect Numbers and Divisor Pairing", NumberTheoryProofCategory.Special),
        )
        val completedInPhase2 = (
            NumberTheoryPhase2PatternsCatalog.topics +
                NumberTheoryPhase2FactorsPrimesCatalog.topics
            ).mapTo(mutableSetOf()) { it.id }
        return definitions.filterNot { it.first in completedInPhase2 }.map { (id, title, category) ->
            NumberTheoryVisualProofTopic(
                id, category, title, setOf(title.lowercase()), NumberTheoryProofLevel.School,
                when (category) {
                    NumberTheoryProofCategory.Modular -> NumberTheoryVisualModel.ModularClock
                    NumberTheoryProofCategory.Divisibility -> NumberTheoryVisualModel.PlaceValueBlocks
                    NumberTheoryProofCategory.Powers -> NumberTheoryVisualModel.ExponentChain
                    NumberTheoryProofCategory.Primes -> NumberTheoryVisualModel.SieveGrid
                    NumberTheoryProofCategory.GcdLcm -> NumberTheoryVisualModel.CycleTrack
                    else -> NumberTheoryVisualModel.NumberLine
                },
                "Interactive proof roadmap", "interactive proof roadmap", emptyList(), emptyList(),
                "What invariant makes this construction work?", emptyList(),
                "A visible pattern is evidence, not automatically a proof.",
                practice("This proof is scheduled for Phase 3.", listOf("Phase 1", "Phase 2", "Phase 3"), 2, "The roadmap preserves a stable topic ID before its renderer is released."),
                3,
            )
        }
    }
}
