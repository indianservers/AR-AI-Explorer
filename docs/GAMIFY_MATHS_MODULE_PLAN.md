# GamifyMaths Module Plan

## Product direction

GamifyMaths is a native, interactive maths-puzzle universe for learners from roughly Grades 4–10. It is designed around direct manipulation, visible mathematical consequences, short explanations, adaptive mastery and non-routine IQ challenges. It must not become a collection of multiple-choice worksheets with decorative game art.

Core learning loop:

1. Discover a concept by manipulating objects.
2. Predict what an action will do.
3. Solve a spatial or logical puzzle.
4. Receive precise feedback at the point of error.
5. Explain why the solution works.
6. Apply the concept in a less familiar boss puzzle.

Every world should eventually contain Concept Scan, Training Missions, Puzzle Missions, Challenge Gates, a Boss Mission, an open Master Lab and an optional IQ Rift.

## Visual system

- Deep indigo mathematical universe with restrained particles and orbital geometry.
- Glossy translucent panels with clear foreground/background separation.
- Luminous 3D mathematical objects that look draggable.
- One accent colour and object language per world.
- Large touch targets, strong focus states and high-contrast interaction zones.
- Motion communicates state: snapping, balance, splitting, rotation, flow or graph movement.
- Decoration never competes with the active mathematical object.

## Twelve game worlds

### 1. Number Forge

Subtopics: place value, ordering, integers, factors, multiples, primes, divisibility, powers, roots and number properties.

Game plan: learners drag digits, operators and number cores into a forge to construct a target under move, energy or component constraints. Advanced puzzles allow multiple solutions and reward the most elegant construction.

### 2. Maths Kitchen

Subtopics: addition, subtraction, multiplication, division, order of operations, estimation, rounding, time, money and unit conversion.

Game plan: learners combine, divide and scale quantities to complete recipes while managing limited ingredients and utensils. Incorrect arithmetic produces a visible quantity or consistency error.

### 3. Fraction Factory

Subtopics: equivalent fractions, comparison, mixed numbers, fraction operations, decimals, percentages and conversions.

Game plan: learners cut, rotate, combine and pack fractional components into exact production orders. Waste and overlap expose misconceptions visually.

### 4. Potion Lab

Subtopics: ratios, unit rates, direct proportion, inverse proportion, scale, mixtures, speed, percentages, discounts and profit/loss.

Game plan: learners pour and combine luminous fluids while resizing recipes. The final colour, volume and concentration react to the mathematical ratio used.

### 5. Balance Vault

Subtopics: variables, expressions, equations, inequalities, substitution, identities and simultaneous equations.

Game plan: learners manipulate a physical balance and vault mechanism. Every operation must preserve equality; illegal changes immediately tilt or fracture the balance field.

### 6. Shape Architect

Subtopics: angles, triangles, polygons, circles, symmetry, congruence, similarity, transformations and construction.

Game plan: learners drag, rotate, reflect and join holographic components to satisfy a blueprint. Constraints are expressed geometrically rather than as answer boxes.

### 7. Measure Nexus

Subtopics: length, mass, capacity, time, perimeter, area, surface area, volume and unit conversion.

Game plan: learners scan, resize and assemble habitats, containers and transport systems within material and measurement limits.

### 8. Vector Voyager

Subtopics: coordinates, quadrants, slope, distance, midpoint, linear graphs, functions and transformations.

Game plan: learners manipulate waypoints, control vectors and edit graph paths to navigate a spacecraft through obstacles and energy gates.

### 9. Pattern Core

Subtopics: visual patterns, arithmetic sequences, geometric sequences, recursive rules and function machines.

Game plan: learners repair a prediction engine by dragging missing signals, constructing rules and testing them against unseen sequence terms.

### 10. Data Detective

Subtopics: tables, charts, mean, median, mode, range, distributions, outliers, sampling and misleading graphs.

Game plan: learners investigate cases by arranging evidence, building appropriate charts and challenging statistically misleading claims.

### 11. Chance Reactor

Subtopics: sample spaces, experimental probability, compound events, expected value, dependent events and fairness.

Game plan: learners construct and run probability machines with dice, spinners, gates and randomisers, then compare predictions with experimental results.

### 12. Logic Grid

Subtopics: deduction, classification, permutations, combinations, counting paths, spatial reasoning, strategy and optimisation.

Game plan: learners escape connected chambers by manipulating clues, routes and constraints. Later puzzles combine several valid facts and reward concise reasoning.

## Implemented first slice

- Native GamifyMaths entry in the Mathematics Explorer.
- Glossy home screen with a real journey continuation route.
- Twelve routed world cards.
- Worlds, Progress and Profile destinations with working state.
- A playable mission screen for every world.
- Shared code-native game chrome: top bars, hearts, hints, scores, level maps, glossy panels, progress, rewards, feedback and draggable tiles.
- Number Forge: six functional levels covering base-ten construction, expanded form, block counting, comparison, number words and a combined challenge.
- Maths Kitchen: six functional recipe levels covering addition, subtraction, multiplication, division, operation order and estimation.
- Fraction Factory: six functional levels covering equivalence, comparison, addition, subtraction, mixed numbers and percentages, with Canvas-drawn fraction discs.
- Balance Vault / Algebra Adventure: six chapters with twelve levels each (72 total). Chapters cover variables and simple equations, multi-step linear equations, expressions and distribution, fractional/decimal equations and inequalities, systems of equations, and quadratic equations.
- Every algebra level includes distinct answer choices, a targeted hint, worked steps and a code-drawn balance, algebra-tile, inequality, system-graph or quadratic visual.
- Shape Architect: six concepts with three progressive challenges each (18 total), including identification, quadrilateral construction, triangle building, combined objects, symmetry and multi-shape challenges.
- Potion Lab: six concepts with three progressive challenges each (18 total), including addition, subtraction, multiplication, equal sharing, fraction fills and mixed-operation challenges.
- Rescue Engineer: seven concepts with three progressive challenges each (21 total), covering ruler measurement, perimeter, rectangular area, triangle area, circle area, volume/unit conversion and structural bridge missions.
- Rescue Engineer bridge missions use finite material inventories, selectable beams, joints, delete, undo, reset and deterministic structural validation for span, connectivity and minimum segment strength.
- Probability Arcade: 15 separate subgames with three levels each (45 total): Chance Explorer, Spin & Win, Coin Flipper, Card Picker, Tree Builder, Permutations Pro, Combinations Champ, Binomial Boost, Normal Navigator, Z-Score Quest, Sampling Safari, Mean Machine, Median Mission, Std Dev Detective and Statistics Challenge.
- Probability experiments and visuals are code-native: animated spinner and coin, probability trees, cards, arrangements, binomial bars, normal curves, z-score lines, samples, data tiles and scatter plots.
- Drag/tap validation, hints, retry, explanations, unlocking and next-level progression.
- Adaptive layout: stacked phone portrait screens, two-pane landscape/tablet/TV play areas and multi-column level maps on wide displays.
- Smart TV access: focusable controls and click/OK alternatives for every drag interaction.

The first slice supplies a functional learning path. The remaining eight generic world interactions will be replaced progressively by their distinct mechanics as approved UI references are supplied.

## Asset policy

- Supplied UI screenshots are design references only and are not bundled in the app.
- Mathematical manipulatives and game scenery should be drawn with Compose layout, Canvas and reusable shapes wherever practical.
- Shared game resources must be preferred over copied per-screen controls.
- Add a raster asset only when code drawing cannot preserve the required visual meaning or quality.
- Inspect every supplied reference before implementation and map it to the correct game; never assume adjacent images represent the same world.
- Temporary reference exports, if required, stay outside packaged Android resources.

## Packaging size

- The universal debug APK was large primarily because it contained native libraries for four ABIs.
- ONNX Runtime, ML Kit OCR and ML Kit Digital Ink were extracted with Smart Board and removed from AI Maths Explorer. The obsolete 2.34 MB raster splash was replaced by a code-native animated splash.
- Debug APK output is split by ABI so a phone or TV receives only the native architecture it can execute.
- The app keeps arm64-v8a and armeabi-v7a device builds plus x86_64 emulator/TV testing builds; obsolete x86 is excluded.
- Verified post-extraction debug APKs are approximately 47.6 MB per ABI and contain none of the extracted OCR/Smart Board native artifacts.
