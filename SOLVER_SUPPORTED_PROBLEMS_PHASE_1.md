# Solver Supported Problems - Phase 1

## Input

Solver accepts editable plain mathematical text through the existing app math keyboard.

Accepted notation includes:

- Operators: `+`, `-`, `*`, `/`, `x` by adjacency, `^`, `%`
- Relations: `=`, `<`, `<=`, `>`, `>=` and keyboard Unicode equivalents
- Grouping: nested parentheses and absolute-value bars
- Roots: `sqrt(81)`, `root(27, 3)` parses, while exact solving currently supports square roots only
- Ratios: `12:18`
- Systems: separate two equations with `;`
- Implicit multiplication: `2x`, `3(x + 1)`, `(x + 1)(x - 1)`
- Mixed fractions: `3 1/2`
- Superscript digits produced by the keyboard

## Arithmetic

Supported:

- Integer and terminating-decimal arithmetic
- Exact fractions and fraction reduction
- Negative numbers and unary signs
- Nested order of operations
- Integer powers
- Exact rational square roots
- Percent expressions such as `25% * 80`
- Absolute values such as `|-17|`
- Integer ratio reduction such as `12:18`
- Numeric proportion checks such as `2/3 = 4/6`

Results remain exact where possible. For example, `1/3 + 1/6` returns `1/2`.

## Basic algebra

Supported:

- Simplifying and combining like terms
- Expanding basic products and bracketed expressions
- Simple factorisation where the bundled exact CAS reports a safe result
- Basic polynomial arithmetic
- One-variable linear equations
- Linear equations with constant fractions, such as `x/3 + 1/2 = 5/6`
- One-variable linear inequalities
- Negative-coefficient inequalities with relation reversal
- Two linear equations in two variables, separated by a semicolon
- One-variable colon proportions such as `x:3 = 4:6`

## Actions

- `Solve`: selects the deterministic Phase 1 strategy.
- `Simplify`: requests simplification for supported expression classes.
- `Check`: runs the same offline parse/classification pipeline and displays structured verification.

## Steps and verification

Each supported result includes:

- Original and transformed typed expressions
- Operation and registered mathematical rule
- Plain-language reason
- Affected expression paths
- Equivalence/reversibility information
- Exact substitution, exact evaluation, sampled equivalence, system substitution, or inequality boundary verification

If verification fails, Solver does not present the result as correct.
