# Solver Module: Class 9 and Class 10 Accuracy Report

## Scope

- Source: the four supplied screenshots.
- Questions tested: 80 (40 Class 9 and 40 Class 10).
- Execution path: `Phase3SolverEngine` through the same school-question interpreter used by the app.
- Well-posed questions: 77.
- Questions with omitted numerical data: 3 (Class 10 questions 34, 36, and 37).
- Correct step-by-step solutions for well-posed questions: 77/77.
- Correct missing-data diagnoses: 3/3.
- Incorrect or fabricated answers: 0.
- Every solved question has at least three structured steps, a registered rule, affected terms, an exact final answer, and verified status.

## Class 9 Results

1. **Irrationality of √7 — PASS.** Assume `√7=p/q` in lowest terms; square to obtain `p²=7q²`; conclude 7 divides both p and q; contradiction. **Answer:** √7 is irrational.
2. **Represent √5 — PASS.** Mark a 2-unit base; construct a 1-unit perpendicular; obtain hypotenuse `√(2²+1²)=√5`; transfer that radius to the number line. **Answer:** point at distance √5 from 0.
3. **Rationalise `5/(√3+1)` — PASS.** Multiply by `(√3-1)/(√3-1)`; denominator becomes `3-1`; reduce. **Answer:** `5(√3-1)/2`.
4. **Conjugate product — PASS.** Apply `(a+b)(a-b)=a²-b²`; calculate `9-2`. **Answer:** `7`.
5. **Recurring decimal — PASS.** Let `x=0.272727...`; form `100x=27.272727...`; subtract to get `99x=27`; reduce. **Answer:** `3/11`.
6. **Evaluate polynomial — PASS.** Substitute `x=2`; calculate `8-8+7`. **Answer:** `p(2)=7`.
7. **Polynomial zero — PASS.** Set `5x-20=0`; isolate `5x=20`; divide by 5. **Answer:** `x=4`.
8. **Factor quadratic — PASS.** Find integers -4 and -5; write factors; expand to verify. **Answer:** `(x-4)(x-5)`.
9. **Difference of cubes — PASS.** Rewrite as `(2a)³-(3b)³`; apply `u³-v³`; simplify. **Answer:** `(2a-3b)(4a²+6ab+9b²)`.
10. **Binomial cube — PASS.** Apply `(a-b)³`; substitute `a=2x`, `b=3y`; simplify coefficients. **Answer:** `8x³-36x²y+54xy²-27y³`.
11. **Linear substitution — PASS.** Put `x=3`; obtain `6+3y=12`; isolate and divide. **Answer:** `y=2`.
12. **Three linear-equation solutions — PASS.** Use `y=(8-x)/2`; substitute three x-values; verify each pair. **Answer:** `(0,4), (2,3), (4,2)`.
13. **Plot a line — PASS.** Set `x=0` for `(0,6)`; set `y=0` for `(3,0)`; join the points. **Answer:** `y=6-2x`.
14. **Point on y-axis — PASS.** On the y-axis `x=0`; five units below gives `y=-5`. **Answer:** `(0,-5)`.
15. **Distances from axes — PASS.** Distance from x-axis is `|y|`; distance from y-axis is `|x|`. **Answer:** 7 units and 4 units.
16. **Euclid's fifth postulate — PASS.** Identify two lines and a transversal; require same-side interior sum below 180°; state that the lines meet on that side.
17. **Vertically opposite angles — PASS.** Write two linear-pair equations; subtract the shared angle; repeat for the second pair. **Answer:** both pairs are equal.
18. **Supplementary-angle ratio — PASS.** Set angles to `4k,5k`; use `9k=180°`; find `k=20°`. **Answer:** `80°` and `100°`.
19. **Isosceles base-angle theorem — PASS.** Bisect the vertex angle; prove two triangles congruent by SAS; use corresponding parts. **Answer:** angles opposite equal sides are equal.
20. **Isosceles triangle angle — PASS.** `AB=AC` gives `∠B=∠C=55°`; apply triangle sum. **Answer:** `∠A=70°`.
21. **Triangle angle-sum proof — PASS.** Draw a parallel through a vertex; transfer the two base angles by alternate interior angles; use a straight angle. **Answer:** `∠A+∠B+∠C=180°`.
22. **Parallelogram diagonal proof — PASS.** Use alternate interior angles from opposite parallel sides and the common diagonal; apply ASA. **Answer:** the two triangles are congruent.
23. **Rhombus area — PASS.** Use `A=d₁d₂/2`; substitute 24 and 10. **Answer:** `120 cm²`.
24. **Equal-chord theorem — PASS.** Join four radii; use equal radii and equal chords; apply SSS; infer equal central angles.
25. **Circle radius from chord — PASS.** Half the chord is 8; form a right triangle with distance 6; calculate `r=√(8²+6²)`. **Answer:** `10 cm`.
26. **SSS construction — PASS.** Draw base 7 cm; arcs of radius 6 cm and 5 cm locate the third vertex; join and measure. **Answer:** verified 5-6-7 triangle.
27. **Heron's formula — PASS.** `s=21`; evaluate `√[21·8·7·6]`. **Answer:** `84 cm²`.
28. **Equilateral Heron area — PASS.** `s=18`; evaluate `√[18·6·6·6]`. **Answer:** `36√3 cm²`.
29. **Cylinder curved surface — PASS.** Use `2πrh`; substitute 7 and 12. **Answer:** `168π cm²`, or `528 cm²` using `π=22/7`.
30. **Cone total surface — PASS.** Use `πr(l+r)`; substitute 5 and 13. **Answer:** `90π cm²`.
31. **Sphere volume — PASS.** Use `4πr³/3`; substitute 6. **Answer:** `288π cm³`.
32. **Hemisphere volume — PASS.** Use `2πr³/3`; substitute 7. **Answer:** `686π/3 cm³`.
33. **Mean of first ten natural numbers — PASS.** Sum 1 through 10 to obtain 55; divide by 10. **Answer:** `5.5`.
34. **Median — PASS.** Sort to `12,14,15,16,18,19,20`; select the fourth value. **Answer:** `16`.
35. **Frequency distribution — PASS.** Tally each distinct observation; verify frequencies total 10. **Answer:** `4:3, 5:2, 6:2, 7:2, 8:1`.
36. **Histogram — PASS.** Put intervals on x-axis and frequency on y-axis; draw touching bars at heights 5, 8, 12, and 7; verify scale and boundaries.
37. **Coin empirical probability — PASS.** Divide observed heads by trials; reduce `112/200`. **Answer:** `14/25=0.56`.
38. **Die empirical probability — PASS.** Divide observed sixes by trials; reduce `48/300`. **Answer:** `4/25=0.16`.
39. **Terminating decimals are rational — PASS.** For n decimal places, multiply by `10ⁿ` to get integer N; rearrange as `N/10ⁿ`. **Conclusion:** rational by definition.
40. **Irrationality of √3 — PASS.** Assume a lowest-term fraction; square; prove 3 divides numerator and denominator; contradiction. **Answer:** √3 is irrational.

## Class 10 Results

1. **Euclidean HCF — PASS.** `867=255·3+102`; `255=102·2+51`; `102=51·2`; last non-zero remainder. **Answer:** `HCF=51`.
2. **HCF and LCM by primes — PASS.** `144=2⁴·3²`; `180=2²·3²·5`; use minimum and maximum powers. **Answer:** `HCF=36`, `LCM=720`.
3. **Irrationality of √5 — PASS.** Reduced-fraction contradiction proves numerator and denominator would both be divisible by 5. **Answer:** √5 is irrational.
4. **Terminating expansion — PASS.** Reduce fraction; factor denominator as `5⁵`; denominator contains only 2/5 factors; evaluate. **Answer:** terminating, `0.00416`.
5. **Quadratic zeroes — PASS.** Factor as `(x-3)(x-4)`; apply zero-product property. **Answer:** `x=3,4`.
6. **Polynomial from zeroes — PASS.** Form `(x-3)(x+5)`; expand and combine. **Answer:** `x²+2x-15`.
7. **Zero-coefficient relations — PASS.** Identify `a=2,b=-5,c=-3`; apply `α+β=-b/a`, `αβ=c/a`. **Answer:** sum `5/2`, product `-3/2`.
8. **Polynomial division — PASS.** Divide leading terms in three rounds; subtract each product; confirm zero remainder. **Answer:** quotient `2x²+7x+3`, remainder `0`.
9. **Simultaneous equations — PASS.** Scale equations to cancel y; solve `13x=38`; back-substitute and verify. **Answer:** `x=38/13`, `y=31/13`.
10. **Consistency — PASS.** Coefficient ratios are equal but constant ratio differs; lines are distinct and parallel. **Answer:** inconsistent, no solution.
11. **Quadratic by factorisation — PASS.** Factor `(x-4)(x-5)`; set each factor to zero. **Answer:** `x=4,5`.
12. **Quadratic formula — PASS.** `a=3,b=-5,c=-2`; discriminant 49; evaluate both branches. **Answer:** `x=2,-1/3`.
13. **Nature of roots — PASS.** Calculate discriminant `16-40=-24`; classify negative discriminant. **Answer:** two distinct non-real complex conjugate roots.
14. **AP 25th term — PASS.** `a=7,d=4`; use `aₙ=a+(n-1)d`. **Answer:** `103`.
15. **AP sum — PASS.** `a=5,d=4,n=30`; use `Sₙ=n[2a+(n-1)d]/2`. **Answer:** `1890`.
16. **AP term index — PASS.** Solve `78=3+5(n-1)`. **Answer:** the 16th term.
17. **Basic Proportionality Theorem — PASS.** Compare triangle area ratios with common altitudes; use equal areas on the same base between parallels; derive `AD/DB=AE/EC`.
18. **Areas of similar triangles — PASS.** Square the corresponding-side ratio. **Answer:** `9:25`.
19. **Coordinate distance — PASS.** Differences are 6 and 8; use `√(6²+8²)`. **Answer:** `10 units`.
20. **Midpoint — PASS.** Average corresponding coordinates. **Answer:** `(1,2)`.
21. **Internal section — PASS.** Apply weighted coordinate averages for ratio `1:2`. **Answer:** `(4,7)`.
22. **Coordinate triangle area — PASS.** Use horizontal base length 6 and perpendicular height 4. **Answer:** `12 square units`.
23. **Trig ratios from tangent — PASS.** Model a 3-4-5 triangle. **Answer:** `sinθ=3/5`, `cosθ=4/5`.
24. **Identity proof — PASS.** Start with `sin²θ+cos²θ=1`; divide by `cos²θ`; rearrange. **Answer:** `sec²θ-tan²θ=1` where defined.
25. **Special-angle evaluation — PASS.** `sin30°=cos60°=1/2`; square and add. **Answer:** `1/2`.
26. **Tower height — PASS.** Set `tan45°=h/20`; use `tan45°=1`. **Answer:** `20 m`.
27. **Tangent-radius theorem — PASS.** Show the radius to contact is the shortest centre-to-tangent segment; shortest distance to a line is perpendicular.
28. **Equal tangents theorem — PASS.** Join radii and common hypotenuse; use right angles and RHS congruence; infer `PA=PB`.
29. **Tangent construction — PASS.** Draw OP; bisect it; intersect the circle with the auxiliary circle on OP as diameter; join the two contact points to P; verify right angles.
30. **Sector area — PASS.** Use `(90/360)π(14²)`. **Answer:** `49π cm²`, or `154 cm²`.
31. **Minor segment — PASS.** Calculate quarter-circle sector `49π/4`; subtract right-triangle area `49/2`. **Answer:** `49(π-2)/4 cm²`.
32. **Composite solid — PASS.** Cone volume `12π`; hemisphere volume `18π`; add. **Answer:** `30π cm³`.
33. **Recast spheres — PASS.** Conserve volume; cancel common constants; cube radius ratio `(6/2)³`. **Answer:** `27 spheres`.
34. **Grouped mean — INCOMPLETE SOURCE, CORRECTLY DIAGNOSED.** The question provides no class intervals or frequencies. The solver identifies the assumed-mean method and requests both inputs. No numerical answer is fabricated.
35. **Grouped median — PASS.** `N=30`, `N/2=15`; cumulative frequencies `5,14,26,30`; median class `20-30`; apply formula. **Answer:** `125/6≈20.833`.
36. **Grouped mode — INCOMPLETE SOURCE, CORRECTLY DIAGNOSED.** The modal class, preceding frequency, succeeding frequency, and class width cannot be obtained because the table is absent. The solver requests class intervals and frequencies.
37. **Less-than ogive — INCOMPLETE SOURCE, CORRECTLY DIAGNOSED.** Upper class boundaries and cumulative frequencies are absent. The solver names those missing inputs instead of inventing a graph or median.
38. **Two-dice probability — PASS.** Count 36 ordered outcomes; favourable pairs are `(3,6),(4,5),(5,4),(6,3)`; reduce. **Answer:** `1/9`.
39. **Red face card — PASS.** Two red suits times three face cards gives 6 favourable cards among 52. **Answer:** `3/26`.
40. **Not-blue ball — PASS.** Total 20; red plus green gives 13 favourable balls. **Answer:** `13/20`.

## Quality Findings

### Mathematical accuracy

- All 77 determinate final answers agree with independent exact calculations or standard theorem statements.
- Proof questions contain a premise, theorem or congruence argument, and explicit conclusion.
- Construction questions include the construction sequence and a verification condition.
- Exact forms are retained (`π`, radicals, and fractions) rather than prematurely rounded.
- Domain wording is included for the trigonometric identity where division by `cos²θ` is used.

### NLP understanding

- Question numbering and terminal punctuation are ignored.
- Common Unicode notation is normalized: superscript powers, minus, multiplication, division, radicals, degrees, triangle and angle symbols.
- The module recognizes command intent such as classify, represent, rationalise, prove, construct, plot, determine, and evaluate.
- The module does not silently infer missing tables.

### Step quality

- Minimum structured steps for each solved question: 3.
- Proofs and constructions generally contain 4 or 5 steps.
- Every step includes a registered mathematical rule, explanation, expression/result, and affected-term marker.
- Every determinate result is marked `Verified`; omitted-data questions are marked `Inconclusive`.

## Regression Test

Permanent test:

`app/src/test/java/com/indianservers/aiexplorer/solver/SolverClass9And10CurriculumTest.kt`

The test uses the exact wording and notation transcribed from all four screenshots. It fails if:

- a well-posed question is unsupported;
- a final answer differs from the checked result;
- fewer than three steps are returned;
- a step lacks explanation, affected terms, or a registered rule;
- verification is not `Verified`; or
- an omitted-data question fabricates a numerical result instead of requesting the missing table.

## Build Verification

- Exact 80-question regression: passed.
- Earlier exact 40-question school regression: passed.
- Comprehensive supported/unsupported safety audit: passed.
- Complete Solver unit suite: 55 test methods, 0 failures, 0 skipped.
- Debug Android-test Kotlin compilation: passed.
- Debug APK assembly: passed.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
