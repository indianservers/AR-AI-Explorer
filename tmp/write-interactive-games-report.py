from pathlib import Path
report='''# Interactive maths games audit — 6 October 2026

The complete live catalogue contains 29 offline games. All live tasks are generated from seeds and mathematical rules. There is no authored question/answer bank in the production game sources. Skill templates, operator labels, shape properties, and mathematical constants remain fixed; those are the rules used to generate tasks.

The audit found that the earlier dynamic screens still mostly presented answer choices. The live screens now use ten interactive board mechanics. Calculation games require assembling and running an expression rather than choosing an answer; a single literal is rejected. Construction games check inventory, counts, sums, geometry, or cargo constraints. Each game has its own local 192×192 PNG illustration and editable SVG source.

## Complete game checklist

All rows below were checked for fresh generation, solvable boards, invalid submissions, and individual artwork. Each learning world has 120 ordinary levels plus two direct-access advanced boards (122 total), with four difficulty settings and optional method hints. Both calculation sprints have Standard, Mastery I, and Mastery II presets.

| Game | Interactive play | Advanced I | Advanced II |
|---|---|---|---|
| Calculation Sprint | Timed number/operator reactor | Grouped arithmetic | Multi-step arithmetic with division |
| Advanced Calculation Sprint | Timed advanced formula reactor | Construct a derivative | Solve a generated two-equation system |
| Number Forge | Place-value core assembly, minimum blocks | Six-digit construction | Seven-digit construction |
| Maths Kitchen | Assemble recipe calculations | Combine recipe quantities and share | Scale multiple batches before sharing |
| Fraction Quest | Build a fraction mosaic from limited stock | Four tiles over denominator 60 | Another generated four-tile challenge over 60 |
| Potion Lab | Pour both flasks to satisfy volume and ratio | Larger ratios and batches | Ratios up to 20 and larger batch scales |
| Equation Escape | Apply equivalent operations to both plates | Variable terms on both sides | Larger equations, including negative solutions |
| Geometry Builder | Assemble polygons and curves against constraints | Seven pieces including pentagons/hexagons | Eight-piece construction |
| Rescue Engineer | Measurement formula reactor and beam assembly | Five-core constrained span | Six-core constrained span |
| Vector Voyager | Pilot a ship with vector moves and a move budget | Generated vector basis | Larger basis and longer planned routes |
| Pattern Detective | Infer and assemble a circuit from three examples | Square, scale, then add | Scale, add, then square |
| Data Detective | Construct statistics and order evidence | Descending even-sized data; mark both middle slots | Sort repeated data; mark every mode occurrence |
| Chance Reactor | Construct probability/statistics calculations | Three dependent draws without replacement | Expected net payoff |
| Logic Grid | Collect distinct robot formations | Energy-constrained unordered formations | Energy-constrained ordered three-robot codes |
| Number Bond Garden | Join limited number cores to meet an exact target | Five cores | Six cores, including signed values |
| Times Table Galaxy | Resize a star array | Meet both area and perimeter | Meet area and row–column difference |
| Decimal Harbor | Assemble decimal cargo calculations | Trips and unloading | Share the remaining cargo among crates |
| Math Market | Price calculations and limited basket construction | Five-core basket | Six-core basket |
| Integer Expedition | Navigate a signed number line | Longer routes | Moves of 10 and 3 with a longer budget |
| Ratio Rangers | Adjust two ingredient volumes | Larger generated ratios | More demanding ratios and scales |
| Percent Studio | Assemble percentage calculations | Successive discounts | Discounts followed by tax |
| Data Story Lab | Construct statistics and sort observations | Descending data and two middle slots | Mode with repeated observations |
| Function Machine | Assemble a circuit that matches all examples | Quadratic then affine | Affine then quadratic |
| Geometry Proof Lab | Assemble geometry calculations | Remove one triangular section | Remove two disjoint triangular sections |
| Calculus Climber | Construct derivative/integral calculations | Cubic derivative at a generated input | Definite integral of a quadratic |
| Matrix Mission | Navigate using a generated vector basis | Plan with a generated basis | Larger basis and displacement |
| Number Theory Crypt | Assemble prime factors to unlock a product | Five prime factors | Seven prime factors |
| Combinatorics Command | Collect complete formation sets | Energy-constrained unordered teams | Energy-constrained ordered codes |
| Optimization Arena | Assemble cargo with maximum value under capacity | Larger inventory | Larger inventory plus exactly five items |

## Generation and answer verification

- Ordinary and advanced boards generate new quantities, inventories, equations, example pairs, targets, moves, and/or formation constraints from a seed. Replaying starts a new seed. The generator and board edits survive saved-state restoration.
- Expected answers come from calculations on generated data. The expression engine checks operator precedence, grouping, powers, roots, factorials, combinations, permutations, and degree-based trigonometry; undefined and nonfinite expressions are rejected.
- Navigation checks the actual displacement and budget. Balance checks all transformed coefficients. Mixtures check both total and ratio. Arrays check all requested dimensions. Sorting checks the complete ordering and marked slots. Factor chains check the product. Robot collections check every valid distinct formation, including generated energy restrictions. Cargo checks the optimum and packing conditions.
- Worked explanations are shown after a valid construction. Geometry palettes show shape symbols and their properties, without naming the requested answer. Method hints do not display a completed solution.
- Removed unused prototype screens and their literal question/answer banks, including the old probability mini-game banks. The catalogue no longer contains a static mission-bank field. Some pure generator tests still exercise generated distractors; live boards do not render those choices.
- Finite mathematics can naturally recur, especially elementary facts. Fresh generation prevents reliance on a fixed authored sequence; it does not promise that a fact will never repeat.

## Validation

The debug application and Android test APK build successfully. The maths unit suite passes 25 tests, including 13,176 seeded live-board cases (27 worlds × 122 levels × 4 variations), every interaction kind, replay diversity at ordinary/Mastery I/Mastery II, generated sprint presets, independent mathematical checks, parser edge cases, and wrong construction rejection.

Device checks use an Android emulator at 360×640 dp. See the accompanying CSV for measured play-screen scroll and screenshots for the menu and representative boards. Initial play, including both advanced boards, is checked separately from the naturally scrollable game catalogue. Additional touch tests solve all ten mechanics, restore a dragged fraction tile, preserve the current sprint and placed block, and verify settings/progress persistence.

Evidence: `mobile-games/interactive/`. Build log: `tmp/interactive-games-complete-build.log`. Device log: `tmp/interactive-games-complete-device.log`.

The games run and generate tasks offline. This change does not add contest registration, centralized ranking, or score authentication for a large event. Previous best scores remain local to the device.
'''
Path('docs/qa/interactive-games-audit.md').write_text(report,encoding='utf-8')
