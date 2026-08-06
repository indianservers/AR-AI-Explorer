package com.indianservers.aiexplorer.features.numbertheory.visualproofs.data

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryParameter
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualModel

internal object NumberTheoryPhase3Catalog {
    val topics = (modularTopics() + exponentTopics() + specialTopics()).map {
        it.copy(completedInPhase = 3)
    }

    private fun modularTopics() = listOf(
        phase2Topic(
            "modular-addition", NumberTheoryProofCategory.Modular, "Addition Modulo n",
            setOf("clock addition", "add remainders", "modular sum"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ModularClock, "(a + b) mod n = ((a mod n) + (b mod n)) mod n",
            "addition modulo n can reduce each addend before adding",
            listOf(
                NumberTheoryParameter("a", "First move", -20, 30, 8),
                NumberTheoryParameter("b", "Second move", -20, 30, 7),
                NumberTheoryParameter("modulus", "Clock size", 2, 16, 12),
            ),
            listOf(
                Triple("Stand at zero on an n-position clock.", "Each full turn changes the integer but not its position.", "start = 0"),
                Triple("Move a steps.", "The first endpoint is a mod n.", "0 → a mod n"),
                Triple("Continue b more steps.", "The second movement begins where the first ended.", "a → a+b"),
                Triple("Remove complete turns.", "Only the final remainder remains.", "(a+b) mod n"),
                Triple("Reduce before adding.", "Removing full turns from either move does not change the endpoint.", "(a mod n + b mod n) mod n"),
            ),
            "Where will the second movement finish?",
            listOf("A full turn is exactly n steps and returns to the same position.", "Removing full turns from a or b preserves both endpoints.", "Sequential movement represents ordinary addition.", "Therefore reducing before or after adding reaches the same modular position."),
            "Do not stop after adding the two remainders; reduce their sum modulo n again.",
            phase2Practice("Where does 8+7 land modulo 12?", listOf("1", "3", "7", "15"), 1, "The full sum is 15, which is one turn plus remainder 3."),
        ),
        phase2Topic(
            "modular-multiplication", NumberTheoryProofCategory.Modular, "Multiplication Modulo n",
            setOf("repeated modular jumps", "multiply remainders", "mod table"), NumberTheoryProofLevel.SeniorSecondary,
            NumberTheoryVisualModel.ModularClock, "(ab) mod n = ((a mod n)(b mod n)) mod n",
            "multiplication modulo n can reduce either factor before multiplying",
            listOf(
                NumberTheoryParameter("a", "Jump size", -10, 15, 4),
                NumberTheoryParameter("b", "Jump count", 0, 12, 5),
                NumberTheoryParameter("modulus", "Clock size", 2, 16, 7),
            ),
            listOf(
                Triple("Choose a jump of a positions.", "One jump represents one copy of a.", "a"),
                Triple("Repeat the jump b times.", "The path represents repeated addition.", "a+a+...+a, b times"),
                Triple("Count the total linear movement.", "Repeated addition equals multiplication.", "ab"),
                Triple("Remove complete clock turns.", "The endpoint is the product remainder.", "ab mod n"),
                Triple("Reverse the factors.", "b jumps of a and a jumps of b have the same total.", "ab = ba"),
            ),
            "Which clock position follows the final repeated jump?",
            listOf("Multiplication by b is b repeated additions of a.", "Each complete n-step turn may be discarded.", "The remaining endpoint is exactly ab mod n.", "Commutativity gives the same endpoint when the factors are reversed."),
            "The product remainder is not generally the ordinary product; complete turns must be removed.",
            phase2Practice("Where does 4×5 land modulo 7?", listOf("0", "3", "6", "20"), 2, "Twenty is two complete turns of seven plus remainder 6."),
        ),
        phase2Topic(
            "negative-modulo", NumberTheoryProofCategory.Modular, "Negative Numbers Modulo n",
            setOf("negative remainder", "backward clock", "minus one mod"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ModularClock, "-a mod n is the position reached by moving backward a steps.",
            "negative integers move backward on a modular clock and normalize into zero through n minus one",
            listOf(
                NumberTheoryParameter("value", "Integer", -40, -1, -1),
                NumberTheoryParameter("modulus", "Clock size", 2, 16, 7),
            ),
            listOf(
                Triple("Begin at zero.", "Clock positions use normalized remainders 0 through n-1.", "0 ≤ r < n"),
                Triple("Move backward for a negative integer.", "Crossing below zero wraps to the final clock positions.", "negative movement"),
                Triple("Add a complete turn n.", "The integer changes, but the endpoint does not.", "a ≡ a+n (mod n)"),
                Triple("Repeat until the label is nonnegative.", "The endpoint is the normalized remainder.", "r = ((a mod n)+n) mod n"),
                Triple("Compare forward and backward routes.", "Equivalent integers share one position.", "a-r is divisible by n"),
            ),
            "Which nonnegative clock position represents the selected negative integer?",
            listOf("Moving by n positions is a complete turn.", "Adding n therefore preserves the endpoint.", "Repeatedly adding n reaches the unique representative from 0 through n-1.", "That representative satisfies the division-algorithm remainder condition."),
            "Programming languages may report a negative remainder; normalize it before displaying a modular class.",
            phase2Practice("What is -1 modulo 7?", listOf("-1", "0", "1", "6"), 3, "One step backward from zero reaches position 6."),
        ),
        phase2Topic(
            "remainder-classes", NumberTheoryProofCategory.Modular, "Remainder Classes",
            setOf("congruence classes", "remainder buckets", "residue class"), NumberTheoryProofLevel.SeniorSecondary,
            NumberTheoryVisualModel.RemainderBuckets, "Every integer belongs to exactly one remainder class modulo n.",
            "integers belong to the same remainder class exactly when their difference is divisible by n",
            listOf(
                NumberTheoryParameter("value", "Center integer", -20, 20, 3),
                NumberTheoryParameter("modulus", "Number of classes", 2, 10, 4),
            ),
            listOf(
                Triple("Create n buckets labelled 0 through n-1.", "Each label is a possible remainder.", "0,1,...,n-1"),
                Triple("Divide nearby integers by n.", "Place each integer in its remainder bucket.", "a = qn+r"),
                Triple("Inspect one bucket vertically.", "Adjacent entries differ by exactly n.", "..., r-n, r, r+n, ..."),
                Triple("Compare two entries in a bucket.", "Their difference is divisible by n.", "a ≡ b (mod n)"),
                Triple("Check every integer once.", "The division algorithm gives one unique normalized remainder.", "exactly one class"),
            ),
            "Which bucket receives the selected integer?",
            listOf("The division algorithm assigns each integer a unique remainder from 0 through n-1.", "Equal remainders differ only by whole multiples of n.", "Conversely, a difference divisible by n produces equal remainders.", "The buckets therefore partition all integers into disjoint congruence classes."),
            "A remainder class contains infinitely many integers; it is not the same thing as its single label.",
            phase2Practice("Which shares a class with 3 modulo 4?", listOf("5", "6", "7", "8"), 2, "Seven minus three is four, a complete modulus."),
        ),
    )

    private fun exponentTopics() = listOf(
        phase2Topic(
            "exponent-product", NumberTheoryProofCategory.Powers, "Product of Powers With the Same Base",
            setOf("add exponents", "same base product", "power law"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ExponentChain, "aᵐ × aⁿ = aᵐ⁺ⁿ",
            "multiplying powers with the same base adds their exponents",
            exponentParameters(2, 3),
            listOf(
                Triple("Expand aᵐ into m equal base blocks.", "Each block is a factor a.", "a×...×a, m factors"),
                Triple("Expand aⁿ into n more base blocks.", "The base is identical in both chains.", "a×...×a, n factors"),
                Triple("Join the multiplication chains.", "Multiplication keeps every factor.", "m+n factors of a"),
                Triple("Compress the joined chain.", "The exponent counts all repeated factors.", "aᵐ×aⁿ=aᵐ⁺ⁿ"),
            ),
            "How many base blocks remain after the chains join?",
            exponentProductWhy(),
            "Exponents add only when multiplying powers with the same base; aᵐ+aⁿ does not become aᵐ⁺ⁿ.",
            phase2Practice("Simplify x³×x⁴.", listOf("x⁷", "x¹²", "2x⁷", "x"), 0, "The chains contain 3+4=7 factors of x."),
        ),
        phase2Topic(
            "exponent-quotient", NumberTheoryProofCategory.Powers, "Quotient of Powers With the Same Base",
            setOf("subtract exponents", "cancel powers", "power quotient"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ExponentChain, "aᵐ ÷ aⁿ = aᵐ⁻ⁿ, for a ≠ 0 and m ≥ n",
            "dividing powers with the same nonzero base subtracts their exponents",
            listOf(
                NumberTheoryParameter("base", "Base a", 2, 9, 2),
                NumberTheoryParameter("m", "Numerator m", 5, 9, 7),
                NumberTheoryParameter("n", "Denominator n", 0, 4, 3),
            ),
            listOf(
                Triple("Expand the numerator into m base factors.", "Every factor equals a.", "aᵐ"),
                Triple("Expand the denominator into n base factors.", "The denominator requires a nonzero base.", "aⁿ"),
                Triple("Cancel matching factors in pairs.", "Each a÷a pair equals one.", "n cancelled pairs"),
                Triple("Count uncancelled numerator factors.", "Exactly m-n copies remain.", "aᵐ⁻ⁿ"),
                Triple("State the condition.", "Division by zero is never permitted.", "a ≠ 0"),
            ),
            "How many numerator factors remain after cancellation?",
            listOf("Expanding exposes m numerator and n denominator factors.", "Each denominator factor cancels exactly one equal numerator factor.", "The uncancelled count is m-n.", "Nonzero a guarantees every cancellation a÷a is valid."),
            "Never cancel factors when a=0; the original quotient would divide by zero.",
            phase2Practice("Simplify y⁸÷y³ for y≠0.", listOf("y⁵", "y¹¹", "y²⁴", "y"), 0, "Three denominator factors cancel, leaving five."),
        ),
        phase2Topic(
            "power-of-power", NumberTheoryProofCategory.Powers, "Power of a Power",
            setOf("multiply exponents", "nested power", "power law"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ExponentChain, "(aᵐ)ⁿ = aᵐⁿ",
            "raising a power to another power multiplies the exponents",
            listOf(
                NumberTheoryParameter("base", "Base a", 2, 4, 2),
                NumberTheoryParameter("m", "Inner exponent m", 1, 4, 3),
                NumberTheoryParameter("n", "Outer exponent n", 1, 4, 4),
            ),
            listOf(
                Triple("Build one group representing aᵐ.", "The group contains m base factors.", "aᵐ"),
                Triple("Copy that whole group n times.", "The outer exponent counts groups.", "(aᵐ)ⁿ"),
                Triple("Open every group.", "There are n groups with m factors each.", "m×n factors"),
                Triple("Join the complete factor chain.", "The exponent counts all mn copies.", "aᵐⁿ"),
            ),
            "How many base factors are inside all groups together?",
            listOf("The inner exponent m counts factors per group.", "The outer exponent n counts identical groups.", "Repeated multiplication creates m×n factors total.", "Compressing that chain gives a to the product mn."),
            "Do not add the exponents: the outer power repeats a whole m-factor group n times.",
            phase2Practice("Simplify (p³)⁴.", listOf("p⁷", "p¹²", "4p³", "p⁸¹"), 1, "Four groups of three p factors contain 3×4=12 factors."),
        ),
        phase2Topic(
            "zero-exponent", NumberTheoryProofCategory.Powers, "Why a⁰ = 1",
            setOf("zero power", "power zero", "why exponent zero"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.ExponentChain, "a⁰ = 1 for a ≠ 0",
            "every nonzero base raised to the zero power equals one",
            listOf(NumberTheoryParameter("base", "Nonzero base a", 1, 12, 5)),
            listOf(
                Triple("Start with a quotient aᵐ÷aᵐ.", "Any nonzero quantity divided by itself equals one.", "aᵐ÷aᵐ=1"),
                Triple("Apply the quotient exponent count.", "All m factors cancel.", "aᵐ⁻ᵐ"),
                Triple("Subtract the exponents.", "The remaining exponent is zero.", "a⁰"),
                Triple("Match both descriptions.", "The same valid quotient equals one and a⁰.", "a⁰=1"),
                Triple("Keep the domain condition.", "For a=0, the starting quotient is 0÷0 and invalid.", "a≠0"),
            ),
            "What remains after every nonzero base factor cancels?",
            listOf("A nonzero expression divided by itself equals one.", "Expanded equal powers cancel factor for factor.", "The exponent law labels the empty factor chain a⁰.", "Therefore consistency requires a⁰=1 for nonzero a."),
            "The rule does not justify 0⁰; the proof explicitly requires a nonzero base.",
            phase2Practice("What is 12⁰?", listOf("0", "1", "12", "undefined"), 1, "The nonzero base satisfies the zero-exponent rule."),
        ),
        phase2Topic(
            "negative-exponent", NumberTheoryProofCategory.Powers, "Negative Exponents",
            setOf("reciprocal power", "minus exponent", "inverse power"), NumberTheoryProofLevel.SeniorSecondary,
            NumberTheoryVisualModel.ExponentChain, "a⁻ⁿ = 1 ÷ aⁿ for a ≠ 0",
            "a negative exponent denotes the reciprocal of the corresponding positive power",
            listOf(
                NumberTheoryParameter("base", "Nonzero base a", 2, 9, 2),
                NumberTheoryParameter("n", "Positive n", 1, 6, 3),
            ),
            listOf(
                Triple("Write a⁰÷aⁿ.", "The numerator equals one for nonzero a.", "1÷aⁿ"),
                Triple("Apply the quotient exponent rule.", "Subtracting n from zero creates -n.", "a⁰⁻ⁿ=a⁻ⁿ"),
                Triple("Expand the denominator.", "It contains n base factors.", "1÷(a×...×a)"),
                Triple("Match the two expressions.", "A negative exponent records reciprocal placement.", "a⁻ⁿ=1÷aⁿ"),
                Triple("Keep the base nonzero.", "A zero base would create division by zero.", "a≠0"),
            ),
            "Where do the n base factors appear when the exponent becomes negative?",
            listOf("The quotient rule turns 0-n into the exponent -n.", "The same quotient has numerator a⁰=1.", "Its denominator is the positive power aⁿ.", "Thus the negative power is exactly the reciprocal, for nonzero a."),
            "A negative exponent does not make the value negative; it creates a reciprocal.",
            phase2Practice("What is 2⁻³?", listOf("-8", "1/8", "8", "-1/8"), 1, "The reciprocal of 2³ is 1/8."),
        ),
    )

    private fun specialTopics() = listOf(
        phase2Topic(
            "perfect-numbers", NumberTheoryProofCategory.Special, "Perfect Numbers and Divisor Pairing",
            setOf("proper divisor sum", "perfect number", "divisor pairs"), NumberTheoryProofLevel.SeniorSecondary,
            NumberTheoryVisualModel.DivisorMap, "A perfect number equals the sum of its positive proper divisors.",
            "a perfect number is equal to the sum of all of its positive divisors smaller than itself",
            listOf(NumberTheoryParameter("value", "Test number", 2, 500, 28)),
            listOf(
                Triple("Generate every factor pair of n.", "Each pair multiplies exactly to n.", "ab=n"),
                Triple("Collect all positive divisors.", "Factor pairs reveal divisors from both sides.", "d divides n"),
                Triple("Remove n itself.", "The remaining values are proper divisors.", "d<n"),
                Triple("Add the proper divisors.", "Compare their exact sum with n.", "sum of proper divisors"),
                Triple("Classify the number.", "Equal means perfect; smaller deficient; larger abundant.", "compare sum with n"),
            ),
            "Does the proper-divisor sum equal, exceed, or fall below the selected number?",
            listOf("Factor generation finds every positive divisor exactly.", "Removing n leaves precisely the proper divisors.", "Their exact sum defines deficient, perfect, or abundant status.", "Equality is therefore a complete verification for the selected finite number."),
            "Do not include the number itself in the proper-divisor sum.",
            phase2Practice("Why is 28 perfect?", listOf("It is even", "1+2+4+7+14=28", "28=4×7", "It has six divisors"), 1, "Its positive proper divisors sum exactly to 28."),
        ),
    )

    private fun exponentParameters(m: Int, n: Int) = listOf(
        NumberTheoryParameter("base", "Base a", 2, 8, 2),
        NumberTheoryParameter("m", "First exponent m", 1, 6, m),
        NumberTheoryParameter("n", "Second exponent n", 1, 6, n),
    )

    private fun exponentProductWhy() = listOf(
        "The exponent counts repeated factors of the base.",
        "Multiplying preserves every factor from both chains.",
        "The joined chain therefore contains m+n copies.",
        "Compressing the chain yields the exponent sum for every common base.",
    )
}
