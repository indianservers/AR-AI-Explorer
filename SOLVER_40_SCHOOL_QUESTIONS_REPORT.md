# Solver Report: 40 Supplied School Questions

## Result

All 40 questions supplied in the two screenshots are now:

- recognized from their English school-question wording;
- solved with an exact final answer;
- presented with at least two structured steps;
- linked to registered mathematical rules;
- accompanied by non-empty explanations;
- independently marked verified;
- protected by permanent regression tests.

Question 39 also produces a bar-chart visual specification with the five supplied
marks.

## Answers

| No. | Answer |
|---:|---|
| 1 | `8x + 5` |
| 2 | `x = 6` |
| 3 | `x = 10` |
| 4 | `19` |
| 5 | `x^2 + 8x + 15` |
| 6 | `6(x + 3)` |
| 7 | `(x + 3)(x + 4)` |
| 8 | `7/9` |
| 9 | `7/8` |
| 10 | `8/27` |
| 11 | `5.6 * 10^-4` |
| 12 | `42` |
| 13 | `13` |
| 14 | `3` |
| 15 | `INR 1,020` |
| 16 | `INR 1,200` |
| 17 | `15%` |
| 18 | `INR 2,100` |
| 19 | `INR 675` |
| 20 | `8 days` |
| 21 | `INR 1,500 and INR 2,100` |
| 22 | `18` |
| 23 | `48 deg, 72 deg, 96 deg, 144 deg` |
| 24 | `24 deg` |
| 25 | `1,440 deg` |
| 26 | Area `216 cm^2`; perimeter `60 cm` |
| 27 | `153 cm^2` |
| 28 | `96 cm^2` |
| 29 | `294 cm^2` |
| 30 | `720 cm^3` |
| 31 | `490pi cm^3` |
| 32 | Rectangle |
| 33 | `(4, 3)` |
| 34 | `19` |
| 35 | `11` |
| 36 | `4` |
| 37 | `1/2` |
| 38 | `{HH, HT, TH, TT}` |
| 39 | Bars: `35, 42, 28, 46, 39` |
| 40 | `3/10` |

## Implemented Question Families

- Instructional algebra: simplify, solve, substitute, expand, factor, evaluate.
- Exact arithmetic: fractions, powers, roots, scientific notation.
- Number theory: least perfect-square multiplier.
- Commercial arithmetic: discount, simple interest, compound interest, percentage
  increase.
- Ratio and proportion: direct proportion, inverse work, ratio division, fourth
  proportional.
- Geometry and mensuration: polygon angles, rectangle, trapezium, rhombus, cube,
  cuboid, cylinder.
- Coordinate geometry: plotting/figure recognition and reflection.
- Elementary statistics: mean, median, mode, and bar chart.
- Elementary probability: dice, coin sample spaces, and finite divisibility.

## Safety Boundary

This is deterministic parameterized language recognition, not unrestricted AI
chat. Unknown prose still passes through `SolverInputIntentGuard` and is rejected
without a fabricated answer.

## Regression Test

`app/src/test/java/com/indianservers/aiexplorer/solver/SolverSchoolQuestionNlpTest.kt`

The test uses the exact supplied wording, including numbering, punctuation,
currency wording, units, and coordinate notation.

