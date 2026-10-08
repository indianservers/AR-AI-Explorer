# Dynamic offline maths games audit

Updated 6 October 2026. All 29 catalogue games were inspected. The live dispatcher now runs generated sessions for all 27 learning worlds and generated timed rounds for the two calculation sprints. Fixed prototype banks are no longer used by the live dispatcher. Older internal prototype screens remain in source for regression comparison.

## Names

- Speed Calculation → Calculation Sprint
- Advanced Speed → Advanced Calculation Sprint
- Fraction Factory → Fraction Quest
- Balance Vault → Equation Escape
- Pattern Core → Pattern Detective
- Shape Architect → Geometry Builder

Existing IDs are unchanged, so saved best scores and progress continue to load. Probability and data worlds now record their own progress.

## Playable changes

Each learning world has 120 levels, grouped into four stages of 30. Generated coefficients, operands, data sets, inventories and target constraints replace memorized question banks. Seeds are saved with the session, so rotating the screen does not silently change a question. Replays draw fresh seeds. Recent complete tasks are rejected during a run; construction signatures include the available pieces. Finite mathematical facts may naturally recur.

Settings are saved separately per world: four difficulty settings, four or six choices, answer entry with an on-screen keypad, hints on/off, and mixed topics or focused practice where a world has multiple topics. Construction rounds use piece controls in either answer mode. Answers and worked explanations remain hidden until a successful check.

Six worlds include construction rather than answer selection:

| World | Interaction |
| --- | --- |
| Number Forge | Compose a generated number with the minimum place-value blocks; 2–5 digit targets. |
| Number Bond Garden | Select a specified number of available tiles to reach a generated sum. |
| Geometry Builder | Construct a collection satisfying piece count, straight-side count and curved-piece count. |
| Rescue Engineer | Assemble a generated bridge span from a limited beam inventory; measurement rounds are also generated. |
| Math Market | Build an exact-budget basket from available price tiles; change and price rounds are generated. |
| Optimization Arena | Pack value/weight cargo under a generated capacity, and find the true optimum. |

Chance Reactor includes 15 generated topic families: bags, spinners, coins, cards, dependent draws, permutations, combinations, binomial trials, normal standardization, z-scores, sampling, mean, median, population standard deviation and population variance.

## Catalogue checklist

| Game | Live generator | Learning levels |
| --- | --- | ---: |
| Calculation Sprint | Yes | Timed rounds |
| Advanced Calculation Sprint | Yes | Timed rounds |
| Number Forge | Yes | 120 |
| Maths Kitchen | Yes | 120 |
| Fraction Quest | Yes | 120 |
| Potion Lab | Yes | 120 |
| Equation Escape | Yes | 120 |
| Geometry Builder | Yes | 120 |
| Rescue Engineer | Yes | 120 |
| Vector Voyager | Yes | 120 |
| Pattern Detective | Yes | 120 |
| Data Detective | Yes | 120 |
| Chance Reactor | Yes | 120 |
| Logic Grid | Yes | 120 |
| Number Bond Garden | Yes | 120 |
| Times Table Galaxy | Yes | 120 |
| Decimal Harbor | Yes | 120 |
| Math Market | Yes | 120 |
| Integer Expedition | Yes | 120 |
| Ratio Rangers | Yes | 120 |
| Percent Studio | Yes | 120 |
| Data Story Lab | Yes | 120 |
| Function Machine | Yes | 120 |
| Geometry Proof Lab | Yes | 120 |
| Calculus Climber | Yes | 120 |
| Matrix Mission | Yes | 120 |
| Number Theory Crypt | Yes | 120 |
| Combinatorics Command | Yes | 120 |
| Optimization Arena | Yes | 120 |

## Verification

The final Android phone pass checked **335 play states at 360 × 640 dp** with safe areas: 281 representative level screens plus 54 expert six-choice/answer-entry variants. Every measured state fit without vertical scrolling (maximum 0 dp). Every topic family and every stage is represented. **22 unit tests and 7 device tests passed**; saved-instance-state restoration, score persistence, menu card dimensions and settings are included. The screenshots below show the final live screens.

Generator tests check 25,920 seeded rounds across all 120 levels of every learning world, including deterministic regeneration, varied replay tasks, unique selectable answers, finite numeric values, and feasible construction targets. Independent formula checks cover geometry, statistics, probability, calculus, ratios and matrix determinants. Cargo optima are independently checked with dynamic programming. Exhausted timed-round pools add real arithmetic and update the answer, instead of appending a cosmetic zero term.

Evidence: [screen measurements](mobile-games/dynamic/layout.csv), [catalogue](mobile-games/dynamic/catalogue.csv), [device results](mobile-games/dynamic/device-results.log), [build results](mobile-games/dynamic/build-results.log), [home cards](mobile-games/dynamic/home.png), [geometry construction](mobile-games/dynamic/shapes-0.png), [cargo optimizer](mobile-games/dynamic/optimization-arena-0.png).

Optional hints, feedback, larger accessibility fonts and very small/landscape screens retain scrolling to keep controls reachable. The 120 levels are generated practice progression; this does not implement an official prize-contest ranking system.

## Suggested additional games (not implemented)

| Game | What the player does | Maths developed |
| --- | --- | --- |
| Number Target | Arrange four generated numbers and operators to make a reachable target; validate the actual expression. | Operations, grouping, flexible calculation |
| Fraction Mosaic | Drag fractional tiles to fill a generated whole or area without gaps. | Equivalence, fraction addition, area |
| Coordinate Maze | Program vector moves through generated obstacles to a destination. | Coordinates, transformations, planning |
| Symmetry Studio | Draw or place the reflected/rotated counterpart of a generated figure. | Spatial reasoning, reflection, rotation |
| Equation Workshop | Move balanced weights and apply equal operations to both sides to isolate a variable. | Algebra and equality |
