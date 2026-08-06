# Solver Accuracy Audit: Grade 10 to Postgraduate

Date: 6 August 2026

## Executive Result

The audit ran 204 deterministic mathematics questions across school,
undergraduate, and postgraduate domains.

| Metric | Result |
|---|---:|
| Total questions | 204 |
| Questions expected to be answerable by the current Solver | 144 |
| Correct and independently verified answers | 144 |
| Incorrect answers after fixes | 0 |
| Safely unsupported questions | 60 |
| Accuracy on answerable corpus | 100.00% |
| Broad corpus answer coverage | 70.59% |

Accuracy and coverage are deliberately separate. A safely refused question is not
counted as a correct mathematical answer.

## Domain Results

| Domain | Cases | Correct | Safely unsupported | Incorrect |
|---|---:|---:|---:|---:|
| Arithmetic | 12 | 12 | 0 | 0 |
| Fractions | 10 | 10 | 0 | 0 |
| Percentages and ratios | 8 | 8 | 0 | 0 |
| Linear equations | 13 | 13 | 0 | 0 |
| Inequalities | 9 | 9 | 0 | 0 |
| Linear systems | 5 | 5 | 0 | 0 |
| Trigonometry | 9 | 9 | 0 | 0 |
| Coordinate geometry | 12 | 12 | 0 | 0 |
| Functions | 4 | 4 | 0 | 0 |
| Sequences and series | 14 | 14 | 0 | 0 |
| Number theory | 19 | 19 | 0 | 0 |
| Matrices | 8 | 8 | 0 | 0 |
| Calculus | 12 | 12 | 0 | 0 |
| Complex numbers | 5 | 5 | 0 | 0 |
| Statistics | 10 | 3 | 7 | 0 |
| Natural-language reasoning | 12 | 1 | 11 | 0 |
| General geometry | 8 | 0 | 8 | 0 |
| Probability | 12 | 0 | 12 | 0 |
| Vectors and vector calculus | 10 | 0 | 10 | 0 |
| Postgraduate mathematics | 12 | 0 | 12 | 0 |

## Education-Level Finding

- Grade 10 symbolic topics are the strongest part of the current Solver.
- Grade 11-12 symbolic trigonometry, matrices, sequences, complex numbers, and
  supported calculus commands performed correctly in this corpus.
- Undergraduate coverage is selective rather than comprehensive.
- Postgraduate breadth is not implemented in the Solver. All 12 PG audit cases
  were safely rejected.
- The app must not be marketed as a general PG mathematics solver yet.

## NLP and Word Problems

Twelve natural-language questions were tested. Only the deterministic HCF wording
was answered. Eleven broader word problems were safely rejected.

The Solver does not currently contain a general natural-language mathematical
interpreter. Its reliable interface remains the structured math keyboard and
documented commands.

## Probability and Statistics

Probability coverage in the Solver is currently zero for the tested binomial,
conditional, Bayes, Poisson, normal, geometric, hypergeometric, Markov, and
moment-generating-function questions.

Statistics currently supports the tested direct mean, median, and population
standard-deviation commands. Confidence intervals, hypothesis tests, regression,
ANOVA, chi-square, and PCA were safely rejected.

Probability and statistics engines elsewhere in the app are not automatically
Solver capabilities until they are routed through structured Solver steps,
explanations, and verification.

## Defects Found and Fixed

The first run exposed ten unsafe false-positive routes. Unknown prose could be
misread as:

- implicit multiplication;
- a statistics mean request;
- an integration request.

Examples included a train-speed problem, confidence intervals, proof requests,
Laplace-transform language, contour integrals, and topology terminology.

`SolverInputIntentGuard` now rejects unknown prose and unsupported advanced topics
before symbolic dispatch. Valid structured commands, degree-radian conversion,
direct supported statistics, and the verified natural-language HCF form remain
available.

The calculus comparator also treats mathematically equivalent forms such as
`4*5*x^4 - 2` and `20*x^4 - 2` as equal using symbolic and independent numeric
checks.

## Verification Method

The persistent corpus is in:

`app/src/test/java/com/indianservers/aiexplorer/solver/SolverComprehensiveAccuracyAuditTest.kt`

For answerable cases the test checks:

- the Solver presents the result as supported;
- exact, normalized, symbolic, or independently sampled equivalence to the
  expected answer;
- verified status;
- no failed presentation gate.

For unsupported cases it checks:

- no final answer is emitted;
- the result cannot be presented as correct;
- the Solver fails closed.

Generated row-level evidence is written to:

`app/build/reports/solver-accuracy/solver-accuracy-cases.csv`

## Test Commands

```text
gradlew :app:testDebugUnitTest --tests
  "com.indianservers.aiexplorer.solver.SolverComprehensiveAccuracyAuditTest"

gradlew :app:testDebugUnitTest --tests
  "com.indianservers.aiexplorer.solver.*"
  :app:compileDebugAndroidTestKotlin
```

Both commands pass.

## Accuracy Caveats

- This is a curated deterministic regression corpus, not a proof of universal
  mathematical correctness.
- The answerable cases are intentionally drawn from documented supported syntax,
  so 100% answer accuracy must not be read as 100% coverage of mathematics.
- Numerical equivalence sampling supplements symbolic comparison only for
  expressions already expected to be equivalent.
- Device UI, OCR, and Math Camera input were not part of this Solver audit.

## Recommended Next Accuracy Work

1. Add a typed probability and distribution strategy layer.
2. Add structured descriptive and inferential statistics strategies.
3. Build a deterministic word-problem semantic schema with explicit quantities,
   units, relationships, and ambiguity handling.
4. Add vectors, vector calculus, and geometry theorem strategies.
5. Expand undergraduate and PG operators only with independent verification.
6. Grow this corpus with external textbook questions and blinded expected answers.

