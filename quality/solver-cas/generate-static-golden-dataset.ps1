$ErrorActionPreference = 'Stop'

$rows = [System.Collections.Generic.List[object]]::new()
$sequence = 0

function Add-GoldenCase {
    param(
        [string]$Category,
        [string]$Subcategory,
        [string]$Difficulty,
        [string]$InputText,
        [string]$Expected,
        [string]$Derivation,
        [string]$Tags,
        [bool]$Supported = $true,
        [string]$Comparison = 'exact_compact',
        [string]$Approximate = 'not_applicable',
        [string]$Tolerance = '0',
        [string]$SolutionSet = 'single exact result',
        [string]$Assumptions = 'standard real arithmetic',
        [string]$Domain = 'all entered values satisfy the displayed operation',
        [string]$Warnings = 'none',
        [string]$ErrorClass = 'none',
        [string]$StepSummary = 'interpret; calculate exactly; verify independently'
    )
    $script:sequence++
    $prefix = switch ($Category) {
        'arithmetic' { 'ARI' }
        'algebra_symbolic' { 'ALG' }
        'equations_inequalities' { 'EQI' }
        'calculus' { 'CAL' }
        'matrices' { 'MAT' }
        'vectors_complex' { 'VEC' }
        default { 'ERR' }
    }
    $id = '{0}-{1:d4}' -f $prefix, $script:sequence
    $rows.Add([ordered]@{
        case_id = $id
        category = $Category
        subcategory = $Subcategory
        difficulty = $Difficulty
        input = $InputText
        normalized_input = $InputText.Trim()
        expected_exact_result = $(if ($Supported) { $Expected } else { 'NO_RESULT' })
        expected_approximate_result = $Approximate
        tolerance = $Tolerance
        expected_solution_set = $SolutionSet
        assumptions = $Assumptions
        domain_constraints = $Domain
        units = 'dimensionless'
        angle_mode = 'not_applicable'
        expected_warnings = $Warnings
        expected_error_classification = $ErrorClass
        expected_step_summary = $StepSummary
        expected_verification = $(if ($Supported) { 'Verified' } else { 'NotApplicable' })
        independent_derivation = $Derivation
        tags = $Tags
        regression_id = "SCAS-GOLD-$id"
        expected_supported = $Supported.ToString().ToLowerInvariant()
        comparison = $Comparison
    }) | Out-Null
}

# Arithmetic: 80 precedence, 40 fractions, 30 percentages, 25 roots, 25 absolute values.
foreach ($a in 1..20) { foreach ($b in 1..4) {
    Add-GoldenCase arithmetic order_of_operations Easy "$a + $b * 2" ($a + 2 * $b) "Compute multiplication before addition: $a + 2*$b." 'integer|precedence|exact'
} }
foreach ($n in 1..40) {
    Add-GoldenCase arithmetic rational_addition Medium "$n/$($n+1) + 1/$($n+1)" '1' 'Combine equal denominators; the numerator becomes the denominator.' 'fraction|rational|exact'
}
foreach ($p in 1..30) {
    Add-GoldenCase arithmetic percentage Easy "$p% of 200" (2 * $p) "Convert $p percent to $p/100 and multiply by 200." 'percentage|exact'
}
foreach ($n in 1..25) {
    $square = $n * $n
    Add-GoldenCase arithmetic perfect_square_root Easy "sqrt($square)" $n "The non-negative square root of $square is $n." 'root|integer|exact' -Domain 'radicand is a non-negative perfect square'
}
foreach ($n in 1..25) {
    Add-GoldenCase arithmetic absolute_value Easy "|-$n|" $n 'Absolute value is distance from zero.' 'absolute-value|integer|exact'
}

# Symbolic algebra: 200 coefficient collections and 100 distributive expansions.
foreach ($a in 1..20) { foreach ($b in 1..10) {
    $coefficient = $a + $b
    Add-GoldenCase algebra_symbolic combine_like_terms Easy "$a*x+$b*x" "$coefficient*x" "Factor x and add coefficients: ($a+$b)x." 'algebra|like-terms|exact' -Comparison 'symbolic_numeric'
} }
foreach ($k in 2..11) { foreach ($c in 1..10) {
    $constant = $k * $c
    Add-GoldenCase algebra_symbolic distributive_expansion Medium "expand $k*(x+$c)" "$constant + $k*x" "Distribute $k to x and $c." 'algebra|expand|distributive' -Comparison 'symbolic_numeric'
} }

# Equations and inequalities: 100 linear equations, 50 inequalities, 50 systems.
foreach ($target in -49..50) {
    $coefficient = 2 + (($target + 49) % 9)
    $constant = 7 - (($target + 49) % 5)
    $right = $coefficient * $target + $constant
    Add-GoldenCase equations_inequalities linear_equation Easy "$coefficient*x+$constant=$right" "x = $target" "Subtract $constant and divide by non-zero coefficient $coefficient; substitute x=$target to verify." 'equation|linear|substitution' -SolutionSet "{$target}" -Domain 'x is real; coefficient is non-zero'
}
foreach ($index in 0..49) {
    $target = $index - 24
    $magnitude = 2 + ($index % 7)
    if (($index % 2) -eq 0) {
        $right = $magnitude * $target + 4
        Add-GoldenCase equations_inequalities linear_inequality Medium "$magnitude*x+4<=$right" "x <= $target" "Subtract 4 and divide by positive $magnitude without reversing the relation." 'inequality|linear|boundary' -SolutionSet "(-infinity,$target]" -Domain 'x is real'
    } else {
        $coefficient = -$magnitude
        $right = $coefficient * $target + 4
        Add-GoldenCase equations_inequalities negative_coefficient_inequality Medium "$coefficient*x+4<=$right" "x >= $target" "Subtract 4 and divide by negative $coefficient, reversing the relation." 'inequality|negative-divisor|boundary' -SolutionSet "[$target,infinity)" -Domain 'x is real'
    }
}
foreach ($x in 1..10) { foreach ($y in 1..5) {
    $sum = $x + $y
    $difference = $x - $y
    Add-GoldenCase equations_inequalities simultaneous_linear_system Medium "x+y=$sum;x-y=$difference" "x = $x, y = $y" 'Add the equations to obtain 2x, then substitute to obtain y; verify both originals.' 'system|linear|elimination' -SolutionSet "{($x,$y)}" -Domain 'x and y are real; determinant is non-zero'
} }

# Calculus: 100 power-rule derivatives, 50 antiderivatives, 50 removable limits.
foreach ($a in 2..21) { foreach ($n in 2..6) {
    $power = $n - 1
    $coefficient = $a * $n
    Add-GoldenCase calculus polynomial_derivative Medium "differentiate $a*x^$n" "$coefficient*x^$power" "Apply d(a*x^n)/dx=a*n*x^(n-1); independently check centred finite differences." 'calculus|derivative|power-rule' -Comparison 'symbolic_numeric' -Domain 'x is real'
} }
foreach ($q in 1..10) { foreach ($n in 1..5) {
    $inputCoefficient = $q * ($n + 1)
    $power = $n + 1
    $expected = if ($q -eq 1) { "x^$power + C" } else { "$q*x^$power + C" }
    Add-GoldenCase calculus polynomial_antiderivative Medium "integrate $inputCoefficient*x^$n" $expected "Apply the reverse power rule; differentiate the expected family to recover $inputCoefficient*x^$n." 'calculus|integral|power-rule' -Comparison 'antiderivative_numeric' -SolutionSet "{$expected | C is real}" -Assumptions 'C is an arbitrary real constant' -Domain 'x is real'
} }
foreach ($a in 1..50) {
    $square = $a * $a
    $expected = 2 * $a
    Add-GoldenCase calculus removable_limit Hard "limit (x^2-$square)/(x-$a) as x -> $a" $expected "For x != $a factor x^2-$square=(x-$a)(x+$a), cancel, then evaluate x+$a at x=$a." 'calculus|limit|removable-singularity' -Comparison 'numeric_tolerance' -Approximate $expected -Tolerance '1e-8' -Domain "x approaches $a through the punctured real neighbourhood" -Warnings "original quotient is undefined at x=$a" -StepSummary 'factor on punctured domain; cancel common factor; evaluate the limit'
}

# Matrices: exact entrywise operations, products, transposes, and determinants.
foreach ($i in 1..40) {
    $a = $i; $b = $i + 1; $c = $i + 2; $d = $i + 3
    $input = "matrixadd([[$a,$b],[$c,$d]],[[1,2],[3,4]])"
    $expected = "[[$($a+1), $($b+2)], [$($c+3), $($d+4)]]"
    Add-GoldenCase matrices matrix_addition Easy $input $expected 'Add entries in identical row and column positions.' 'matrix|addition|2x2' -Assumptions 'both matrices are 2 by 2' -Domain 'matrix dimensions agree'
}
foreach ($i in 1..20) {
    $a = $i + 5; $b = $i + 6; $c = $i + 7; $d = $i + 8
    $input = "matrixsubtract([[$a,$b],[$c,$d]],[[1,2],[3,4]])"
    $expected = "[[$($a-1), $($b-2)], [$($c-3), $($d-4)]]"
    Add-GoldenCase matrices matrix_subtraction Easy $input $expected 'Subtract entries in identical row and column positions.' 'matrix|subtraction|2x2' -Assumptions 'both matrices are 2 by 2' -Domain 'matrix dimensions agree'
}
foreach ($i in 1..20) {
    $p = $i + 1; $q = $i + 2
    $input = "matrixmultiply([[$p,0],[0,$q]],[[2,3],[4,5]])"
    $expected = "[[$($p*2), $($p*3)], [$($q*4), $($q*5)]]"
    Add-GoldenCase matrices matrix_multiplication Medium $input $expected 'Use independent row-column dot products; diagonal left scaling multiplies each target row.' 'matrix|multiplication|2x2' -Assumptions 'inner dimensions are both 2' -Domain 'columns(A)=rows(B)'
}
foreach ($i in 1..10) {
    $input = "transpose([[$i,$($i+1),$($i+2)],[$($i+3),$($i+4),$($i+5)]])"
    $expected = "[[$i, $($i+3)], [$($i+1), $($i+4)], [$($i+2), $($i+5)]]"
    Add-GoldenCase matrices transpose Easy $input $expected 'Exchange row and column indices.' 'matrix|transpose|2x3'
}
foreach ($i in 1..10) {
    $input = "det([[$i,1],[2,$($i+1)]])"
    $expected = $i * ($i + 1) - 2
    Add-GoldenCase matrices determinant Medium $input $expected "Compute ad-bc=$i*$($i+1)-1*2." 'matrix|determinant|2x2'
}

# Complex arithmetic plus explicit vector requests that must fail closed until supported.
foreach ($a in 1..10) { foreach ($b in 1..6) {
    Add-GoldenCase vectors_complex complex_rectangular Easy "complex $a+$($b)i" "$a + $($b)i" 'Read the independently specified real and imaginary components.' 'complex|rectangular|exact' -Assumptions 'i^2=-1; principal argument convention' -Domain 'complex plane'
} }
$complexProducts = 0
foreach ($a in 1..5) { foreach ($b in 1..2) { foreach ($c in 1..2) { foreach ($d in 1..2) {
    if ($complexProducts -ge 20) { continue }
    $real = $a * $c - $b * $d
    $imaginary = $a * $d + $b * $c
    $expected = if ($real -eq 0) { "$($imaginary)i" } elseif ($real -gt 0) { "$real + $($imaginary)i" } else { "$real + $($imaginary)i" }
    Add-GoldenCase vectors_complex complex_multiplication Medium "complex multiply $a+$($b)i ; $c+$($d)i" $expected 'Expand (a+bi)(c+di) and use i^2=-1; real=ac-bd, imaginary=ad+bc.' 'complex|multiply|exact' -Assumptions 'i^2=-1'
    $complexProducts++
} } } }
foreach ($i in 1..20) {
    Add-GoldenCase vectors_complex vector_dot_product_unsupported Hard "dot([$i,1],[1,$i])" 'NO_RESULT' 'No production vector-dot command contract exists; the safe result is no answer.' 'vector|unsupported|fail-closed' -Supported $false -SolutionSet 'not_applicable' -Assumptions 'none' -Domain 'command unsupported' -Warnings 'unsupported operation' -ErrorClass 'unsupported' -StepSummary 'recognize unsupported command; return no result'
}

# Invalid, undefined, ambiguous, and unsupported inputs: 25 cases in each family.
foreach ($i in 1..25) {
    Add-GoldenCase invalid_undefined_ambiguous_unsupported division_by_zero Easy "$i/0" 'NO_RESULT' 'Division by zero is undefined, so no mathematical value may be presented.' 'invalid|undefined|division-by-zero' -Supported $false -SolutionSet 'empty' -Assumptions 'none' -Domain 'denominator must be non-zero' -Warnings 'undefined expression' -ErrorClass 'undefined' -StepSummary 'detect zero denominator; stop without an answer'
}
foreach ($i in 1..25) {
    $order = 25 + $i
    Add-GoldenCase invalid_undefined_ambiguous_unsupported invalid_complex_root_order Medium "complex roots 1+i order $order" 'NO_RESULT' 'The verified local complex-root contract caps the requested order; an out-of-range order must return no roots.' 'invalid|complex-roots|fail-closed' -Supported $false -SolutionSet 'empty' -Assumptions 'principal argument convention' -Domain 'root order must be within the supported positive range' -Warnings 'root order out of supported range' -ErrorClass 'invalid_argument' -StepSummary 'validate root order; stop without an answer'
}
foreach ($i in 1..25) {
    Add-GoldenCase invalid_undefined_ambiguous_unsupported malformed_syntax Easy "$i+(" 'NO_RESULT' 'An unmatched parenthesis has no parse tree and therefore no result.' 'invalid|parse-error|parenthesis' -Supported $false -SolutionSet 'empty' -Assumptions 'none' -Domain 'syntax must be balanced' -Warnings 'malformed input' -ErrorClass 'parse_error' -StepSummary 'parse; identify unmatched parenthesis; stop'
}
foreach ($i in 1..25) {
    Add-GoldenCase invalid_undefined_ambiguous_unsupported unsupported_contour_prose Hard "evaluate contour integral case $i around the unit circle" 'NO_RESULT' 'No verified contour-integral command schema matches this prose, so the engine must not invent a value.' 'unsupported|contour-integral|fail-closed' -Supported $false -SolutionSet 'not_applicable' -Assumptions 'none' -Domain 'unsupported command grammar' -Warnings 'unsupported operation' -ErrorClass 'unsupported' -StepSummary 'intent guard rejects unsupported prose; return no result'
}

$expectedCounts = [ordered]@{
    arithmetic = 200
    algebra_symbolic = 300
    equations_inequalities = 200
    calculus = 200
    matrices = 100
    vectors_complex = 100
    invalid_undefined_ambiguous_unsupported = 100
}
if ($rows.Count -ne 1200) { throw "Expected 1200 rows, found $($rows.Count)." }
foreach ($entry in $expectedCounts.GetEnumerator()) {
    $actual = @($rows | Where-Object { $_.category -eq $entry.Key }).Count
    if ($actual -ne $entry.Value) { throw "Category $($entry.Key): expected $($entry.Value), found $actual." }
}
if (@($rows.case_id | Sort-Object -Unique).Count -ne 1200) { throw 'Case IDs are not unique.' }
if (@($rows.input | Sort-Object -Unique).Count -ne 1200) { throw 'Inputs are not unique.' }

$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$target = Join-Path $root 'SOLVER_CAS_GOLDEN_DATASET.jsonl'
$jsonLines = $rows | ForEach-Object { $_ | ConvertTo-Json -Compress }
[System.IO.File]::WriteAllLines($target, $jsonLines, [System.Text.UTF8Encoding]::new($false))
Write-Output "Wrote $($rows.Count) static cases to $target"
