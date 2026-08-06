package com.indianservers.aiexplorer.features.numbertheory.visualproofs.data

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryParameter
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualModel

internal object NumberTheoryPhase2PatternsCatalog {
    val topics = listOf(
        phase2Topic(
            "even-sum", NumberTheoryProofCategory.Patterns, "Sum of the First n Even Numbers",
            setOf("even number sum", "2 plus 4", "twice triangular"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.Staircase, "2 + 4 + ... + 2n = n(n + 1)",
            "the first n even numbers sum to n times n plus one",
            listOf(NumberTheoryParameter("n", "Terms n", 1, 20, 6)),
            listOf(
                Triple("Build rows of 2, 4, through 2n tiles.", "Each row is twice its row number.", "S = 2 + 4 + ... + 2n"),
                Triple("Split every row into two equal parts.", "Each half is a natural-number staircase.", "S = 2(1 + 2 + ... + n)"),
                Triple("Turn one staircase.", "The two copies interlock into a rectangle.", "n rows and n + 1 columns"),
                Triple("Count the rectangle.", "No halving remains because both staircases were the original even rows.", "S = n(n + 1)"),
            ),
            "What rectangle will the split staircases form?",
            listOf("Every even term 2k is two copies of k.", "The two copies interlock into an n by n+1 rectangle.", "The rectangle contains n(n+1) tiles.", "The construction works for every positive n."),
            "Do not divide by two again; the even-number arrangement already contains both staircases.",
            phase2Practice("What is 2+4+...+16?", listOf("64", "72", "80", "144"), 1, "There are 8 terms, so the rectangle has 8×9=72 tiles."),
        ),
        phase2Topic(
            "square-odd-difference", NumberTheoryProofCategory.Patterns, "Square Numbers as Consecutive Odd Sums",
            setOf("odd border", "nested squares", "n square minus"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.TileGrid, "n² - (n - 1)² = 2n - 1",
            "n squared minus n minus one squared equals two n minus one",
            listOf(NumberTheoryParameter("n", "Outer side n", 2, 20, 6)),
            listOf(
                Triple("Build an (n-1) by (n-1) square.", "This is the previous square number.", "(n - 1)²"),
                Triple("Add a row of n tiles.", "The top edge reaches the new width.", "+ n"),
                Triple("Add a column of n-1 tiles.", "The shared corner was already placed.", "+ (n - 1)"),
                Triple("Count the L-shaped border.", "The new layer is always odd.", "n + n - 1 = 2n - 1"),
                Triple("Repeat for larger squares.", "Successive odd layers build every square number.", "1 + 3 + ... + (2n - 1) = n²"),
            ),
            "Why does the new border contain one fewer tile than 2n?",
            listOf("The new row contributes n tiles.", "The new column shares its corner with that row.", "It therefore contributes only n-1 additional tiles.", "Their total 2n-1 is the difference of the two square areas."),
            "Counting n tiles in both strips counts their shared corner twice.",
            phase2Practice("Which odd layer grows 8² into 9²?", listOf("15", "16", "17", "19"), 2, "For outer side n=9, the border has 2n-1=17 tiles."),
        ),
        phase2Topic(
            "consecutive-integer-sum", NumberTheoryProofCategory.Patterns, "Sum of Consecutive Integers",
            setOf("consecutive sum", "terms times average", "pair ends"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.BarStaircase, "sum = number of terms × average",
            "the sum equals the number of terms times their average",
            listOf(
                NumberTheoryParameter("start", "First term", -10, 20, 3),
                NumberTheoryParameter("count", "Term count", 2, 12, 6),
            ),
            listOf(
                Triple("Arrange consecutive values as bars.", "Every bar rises by one.", "a, a+1, ..., a+n-1"),
                Triple("Pair the first and last bars.", "Their sum matches the second and next-to-last pair.", "first + last"),
                Triple("Draw the average height.", "Each pair balances around the same midpoint.", "average = (first + last) ÷ 2"),
                Triple("Flatten the bars to the average.", "The number of bars does not change.", "sum = count × average"),
                Triple("Check odd and even counts.", "A middle bar, when present, already equals the average.", "2S = n(first + last)"),
            ),
            "What remains constant when the outer terms are paired?",
            listOf("Terms equally far from the ends have a constant pair sum.", "Each pair averages to the midpoint of the sequence.", "Redistributing height preserves the total.", "Thus count times average gives the exact sum for odd or even counts."),
            "For an even pair sum, the average may still be a half-integer; the final sum remains an integer.",
            phase2Practice("What is 7+8+9+10+11?", listOf("35", "40", "45", "50"), 2, "Five terms have average 9, so the sum is 5×9=45."),
        ),
        phase2Topic(
            "divisibility-2", NumberTheoryProofCategory.Divisibility, "Divisibility Rule for 2",
            setOf("even test", "divisible by two", "last digit"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.PlaceValueBlocks, "A number is divisible by 2 exactly when its last digit is even.",
            "a whole number is divisible by two exactly when its units digit is even",
            listOf(NumberTheoryParameter("value", "Number", 0, 9999, 2468)),
            divisibilitySteps(2, "all tens, hundreds, and thousands can be paired", "the units digit"),
            "Will one unit remain after every block is paired?",
            placeValueWhy("2", "even"),
            "Zero is even and is divisible by every nonzero divisor, including 2.",
            phase2Practice("Which number is divisible by 2?", listOf("431", "572", "995", "1007"), 1, "Only 572 ends in an even digit."),
        ),
        phase2Topic(
            "divisibility-9", NumberTheoryProofCategory.Divisibility, "Divisibility Rule for 9",
            setOf("digit sum nine", "division test 9", "digital root"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.PlaceValueBlocks, "N mod 9 = digit sum mod 9",
            "a number and its digit sum have the same remainder modulo nine",
            listOf(NumberTheoryParameter("value", "Number", 0, 99999, 729)),
            listOf(
                Triple("Break the number into decimal place values.", "Each power of ten is one more than a multiple of nine.", "10ᵏ ≡ 1 (mod 9)"),
                Triple("Bundle each place into groups of nine.", "One unit remains for every copy of a place-value block.", "d×10ᵏ ≡ d (mod 9)"),
                Triple("Move the leftovers into one tray.", "The tray contains the sum of the digits.", "N ≡ digit sum (mod 9)"),
                Triple("Reduce the digit sum again if needed.", "Repeated reduction preserves the remainder.", "729 → 18 → 9"),
                Triple("Compare both remainders.", "They are identical for every decimal integer.", "9 divides N exactly when 9 divides its digit sum"),
            ),
            "Why may the digit sum be reduced repeatedly?",
            placeValueWhy("9", "a multiple of nine"),
            "Divisibility by 3 does not imply divisibility by 9; the digit sum must itself be divisible by 9.",
            phase2Practice("Which is divisible by 9?", listOf("352", "738", "1001", "811"), 1, "The digits of 738 sum to 18, which is divisible by 9."),
        ),
        phase2Topic(
            "divisibility-4", NumberTheoryProofCategory.Divisibility, "Divisibility Rule for 4",
            setOf("last two digits", "division test 4"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.PlaceValueBlocks, "Only the final two digits determine divisibility by 4.",
            "a decimal integer is divisible by four exactly when its last two digits are divisible by four",
            listOf(NumberTheoryParameter("value", "Number", 0, 99999, 2316)),
            divisibilityTailSteps(4, 100, "two"),
            "Which part of the number can be removed without changing divisibility by 4?",
            tailWhy(4, 100, "two"),
            "Testing only the last digit is not sufficient for divisibility by 4.",
            phase2Practice("Which number is divisible by 4?", listOf("2314", "2316", "2318", "2322"), 1, "The last two digits are 16, and 16 is divisible by 4."),
        ),
        phase2Topic(
            "divisibility-8", NumberTheoryProofCategory.Divisibility, "Divisibility Rule for 8",
            setOf("last three digits", "division test 8"), NumberTheoryProofLevel.School,
            NumberTheoryVisualModel.PlaceValueBlocks, "Only the final three digits determine divisibility by 8.",
            "a decimal integer is divisible by eight exactly when its last three digits are divisible by eight",
            listOf(NumberTheoryParameter("value", "Number", 0, 99999, 14312)),
            divisibilityTailSteps(8, 1000, "three"),
            "Why do thousands and all larger place values disappear in groups of eight?",
            tailWhy(8, 1000, "three"),
            "The last two digits rule belongs to 4, not 8; for 8, retain three digits.",
            phase2Practice("Which number is divisible by 8?", listOf("5410", "5412", "5416", "5418"), 2, "The last three digits are 416, and 416÷8=52."),
        ),
        phase2Topic(
            "divisibility-5-10", NumberTheoryProofCategory.Divisibility, "Divisibility Rules for 5 and 10",
            setOf("ends in zero or five", "multiples five ten"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.NumberLine, "Multiples of 5 end in 0 or 5; multiples of 10 end in 0.",
            "multiples of five end in zero or five and multiples of ten end in zero",
            listOf(NumberTheoryParameter("value", "Number", 0, 9999, 735)),
            listOf(
                Triple("Mark jumps of five on a number line.", "The units digits alternate between 0 and 5.", "0, 5, 10, 15, ..."),
                Triple("Mark jumps of ten.", "Every landing point has units digit 0.", "0, 10, 20, 30, ..."),
                Triple("Separate the tens from the units.", "Every full ten is divisible by both 5 and 10.", "N = 10q + r"),
                Triple("Inspect the units remainder.", "Only r=0 or r=5 completes a group of five.", "5 | N iff r is 0 or 5"),
                Triple("Compare the two tests.", "Only r=0 completes an additional group of ten.", "10 | N iff r is 0"),
            ),
            "Does the selected number land on the 5 track, the 10 track, or both?",
            listOf("Every decimal number is a multiple of ten plus its units digit.", "The multiple-of-ten part never changes either test.", "Only units 0 or 5 are divisible by five.", "Only unit 0 is divisible by ten."),
            "A number ending in 5 is divisible by 5 but not by 10.",
            phase2Practice("Which number is divisible by both 5 and 10?", listOf("125", "230", "345", "555"), 1, "A number divisible by 10 ends in 0, and is therefore also divisible by 5."),
        ),
        phase2Topic(
            "divisibility-11", NumberTheoryProofCategory.Divisibility, "Divisibility Rule for 11",
            setOf("alternating digit sum", "division test eleven"), NumberTheoryProofLevel.SeniorSecondary,
            NumberTheoryVisualModel.RemainderBuckets, "N mod 11 equals its alternating digit sum mod 11.",
            "a number has the same remainder modulo eleven as its alternating digit sum",
            listOf(NumberTheoryParameter("value", "Number", 0, 99999, 2728)),
            listOf(
                Triple("Colour digits in alternating positions.", "Powers of ten alternate between 1 and -1 modulo 11.", "10ᵏ ≡ (-1)ᵏ (mod 11)"),
                Triple("Move alternating digits into two trays.", "One tray is positive and the other negative.", "(8 + 7) - (2 + 2)"),
                Triple("Subtract the tray totals.", "The difference has the same remainder as the number.", "15 - 4 = 11"),
                Triple("Test the difference.", "A multiple of eleven means the original number is divisible by eleven.", "2728 ≡ 0 (mod 11)"),
                Triple("Try another digit length.", "Starting from the units side preserves the alternating signs.", "alternating sum mod 11"),
            ),
            "What difference will the positive and negative digit trays produce?",
            listOf("Ten is congruent to -1 modulo eleven.", "Successive powers therefore alternate signs.", "Replacing each place value by its sign preserves the remainder.", "A zero remainder proves divisibility by eleven."),
            "Alternate from the units side consistently; changing every sign changes the difference but not whether 11 divides it.",
            phase2Practice("Is 2728 divisible by 11?", listOf("Yes", "No", "Only by 9"), 0, "(8+7)-(2+2)=11, which is divisible by 11."),
        ),
        phase2Topic(
            "parity-last-digit", NumberTheoryProofCategory.Divisibility, "Why the Last Digit Determines Parity",
            setOf("odd even last digit", "parity proof"), NumberTheoryProofLevel.Foundation,
            NumberTheoryVisualModel.PlaceValueBlocks, "A whole number has the same parity as its units digit.",
            "a whole number is odd or even according to its units digit",
            listOf(NumberTheoryParameter("value", "Number", 0, 99999, 4387)),
            divisibilitySteps(2, "all complete tens pair with no remainder", "the units digit"),
            "After pairing every ten-block, which objects can still be unpaired?",
            placeValueWhy("2", "even"),
            "The last digit determines parity only in a base whose place-value base is even, including decimal.",
            phase2Practice("What is the parity of 91,234?", listOf("Odd", "Even", "Neither"), 1, "Every higher place value is even, and the units digit 4 is even."),
        ),
    )

    private fun divisibilitySteps(divisor: Int, higherPlaces: String, decidingPart: String) = listOf(
        Triple("Build the number from place-value blocks.", "Separate tens, hundreds, and units.", "N = place values + units"),
        Triple("Pair or group the higher-place blocks.", higherPlaces.replaceFirstChar(Char::uppercase), "10, 100, ... are divisible by $divisor"),
        Triple("Move complete groups aside.", "No higher-place remainder remains.", "N mod $divisor = units mod $divisor"),
        Triple("Inspect $decidingPart.", "It alone determines the final remainder.", "divisibility test"),
    )

    private fun placeValueWhy(divisor: String, result: String) = listOf(
        "Every decimal place above units contains a factor of ten.",
        "Those place-value contributions are $result and leave no remainder modulo $divisor.",
        "Only the units contribution can change the remainder.",
        "Therefore the units test is valid for every whole number.",
    )

    private fun divisibilityTailSteps(divisor: Int, place: Int, digits: String) = listOf(
        Triple("Split the number into a prefix and final $digits digits.", "The prefix is multiplied by $place.", "N = ${place}q + r"),
        Triple("Group each $place-block by $divisor.", "$place is exactly divisible by $divisor.", "$place ≡ 0 (mod $divisor)"),
        Triple("Remove the prefix groups.", "They contribute no remainder.", "N mod $divisor = r mod $divisor"),
        Triple("Test the final $digits digits.", "Their remainder is the whole number's remainder.", "$divisor divides N iff $divisor divides r"),
    )

    private fun tailWhy(divisor: Int, place: Int, digits: String) = listOf(
        "The digits before the final $digits represent a multiple of $place.",
        "$place is divisible by $divisor, so that prefix contributes zero remainder.",
        "Removing a multiple of $divisor preserves divisibility.",
        "The final $digits digits therefore determine the result for every whole number.",
    )
}
