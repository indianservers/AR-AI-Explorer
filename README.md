# AI Explorer

AI Explorer is a native Android mathematics exploration app built with Kotlin and Jetpack Compose.

The current implementation replaces the starter XML project with a dark-first Compose application using the package `com.indianservers.aiexplorer`. It includes functional 2D coordinate geometry, 2D graphing, projected 3D solid exploration, 3D surface graphing, command history, workspace JSON export, accessibility descriptions, and automated math tests.

## Subject hub and adaptive shell

- The first screen is a learning-laboratory hub. Maths is active; Physics, Chemistry, Biology, Astro Physics, and IQ Labs are visible as disabled future subjects.
- Selecting Maths opens the current Explorer without changing existing workspaces.
- The Mathematics Menu opens the active visual workspaces, Problem Solver, Math Notebook, Knowledge Intelligence areas, and Probability & Statistics.
- Safe-area insets, adaptive card sizing, non-wrapping weighted navigation, and scrollable panels support portrait/landscape phones and tablets.
- Focusable controls, optional touchscreen/Leanback declarations, a TV launcher entry, and a TV banner support Smart TV/D-pad operation.
- The large action dock is closed by default and opens from `More`, preventing it from covering the graph on mobile screens.
- Mobile chrome now uses progressive disclosure: collapsible top and bottom bars, a one-button tool launcher, hidden-until-needed equation chips and planned submenus, and mutually exclusive workspace panels.
- The Mathematics menu and compact quick-action dock can be dragged away from the active object; their positions reset safely after an orientation/layout-class change.
- Transparent symbolic icons are applied consistently to subjects, Maths submenus, workspace navigation, panel headers, and shared action buttons while retaining accessible text labels.
- The graph no longer displays the full Plot/Trace/Tangent/Calculus/Data tool grid over the canvas. These modes live in the collapsible Controls panel, reached from the movable `Open tools` launcher.
- `Open tools` supports tap-to-expand and press-drag repositioning with screen-bound constraints and an orientation-safe reset.
- The complete 25-item implementation checklist is in `UI_ENHANCEMENTS.md`.

## Direct interaction

- 2D geometry: tap and drag points or complete shapes; drag empty space to pan, pinch to zoom, and double-tap to fit.
- Graphing: drag the trace in Trace mode, drag the canvas in Plot mode, and use two fingers to pan/zoom in every mode.
- 3D solids: drag empty space to orbit, use two fingers to pan/zoom, and use the on-canvas Move/Rotate/Scale modes to transform selected solids. Switch among object, vertex, edge, and face picking, perspective/orthographic cameras, cross-sections, clipping, and wire/filled rendering.
- 3D surfaces: one-finger drag orbits by default; two fingers pan, pinch zooms, and two-finger twist rolls. Tap X/Y/Z or XY/XZ/YZ for canonical axis and plane views, double-tap to refocus, or choose Trace before dragging the trace point.
- Direct manipulation is previewed live and committed as one atomic undo/redo command when the gesture ends.

## Dynamic geometry

- Constructions reuse existing points when tapped and grid-snap newly placed points.
- Derived midpoint, intersection, centroid, circumcenter, incenter, and orthocenter points update live when their parent points move.
- Advanced objects include vectors, parallel/perpendicular lines, angle bisectors, three-point circles, ellipses, and regular polygons.
- Translated, rotated, reflected, and dilated copies remain dynamically linked to their source objects.
- The object inspector supports rename, show/hide, lock/unlock, style cycling, deletion, and undo/redo.
- The construction protocol displays the current undoable construction sequence.

## Advanced graphing and analysis

- Editable, addable, hideable, and deletable graph definitions.
- Explicit, piecewise, polar, parametric, and implicit plotting.
- Live parameter `a` sliders for parameterized expressions.
- Trace, tangent, normal, derivative, integral, area-between-curves, intersections, roots, and extrema analysis.
- Value tables, editable point datasets, linear regression, descriptive statistics, normal PDF, and binomial probability.
- Comparisons and lazy `if(condition, true, false)` expressions plus multi-argument `min`/`max`.
- Domain-aware advanced graph definitions classify explicit, polar, parametric, implicit, inequality, sequence, and vector-field inputs.
- Adaptive explicit sampling refines curved regions, separates discontinuities, reports evaluation diagnostics, computes sampled range, and estimates arc length.
- Plot-ready sequence, inequality-grid, and normalized vector-field APIs provide a renderer-independent boundary for the next graph UI expansion.

## Analytic 2D and 3D foundation

- 2D lines support signed-side tests, projection, distance, intersections, circle-line intersections, conic classification, convex hulls, and bounds.
- The 2D inspector exposes the analytic equation of the active line alongside construction measurements.
- 3D lines and planes support projection, signed distance, line-plane and plane-plane intersections, plane angle, triangle area, and bounds.
- The solid inspector reports mesh-derived 3D bounds.
- Surface calculus provides the point, partial derivatives, gradient, unit normal, and tangent plane for `z=f(x,y)`; live gradients and normals appear in Surface Insights.

## Probability and distributions

- Probability & Statistics is an active Mathematics destination rather than a planned placeholder.
- Interactive Normal, Binomial, Poisson, Uniform, and Exponential distributions have validated parameters and consistent PDF/PMF, CDF, quantile, interval-probability, moments, and plot-sample APIs.
- The responsive lab provides parameter sliders, lower/upper interval controls, shaded continuous density or discrete probability bars, mean, variance, standard deviation, median, and 90th percentile.
- Discrete calculations use log-space PMFs where appropriate, and invalid parameter domains fail explicitly.
- The Statistics lab accepts editable data and provides interactive histograms, box plots, dot plots, empirical CDFs, and normal Q-Q plots; tapping a mark reports the selected interval, value, or percentile.
- Descriptive analysis includes mean, median, multimode, five-number summary, range, IQR, population/sample variance and standard deviation, standard error, coefficient of variation, mean/median absolute deviation, skewness, excess kurtosis, Tukey fences, and outliers.
- Inferential foundations include confidence intervals, one-sample and Welch two-sample t tests, covariance, and Pearson/Spearman correlation.
- A staged School, Higher Secondary, Undergraduate, and Postgraduate learning path connects each topic to concepts, an interactive lab, and a learning outcome. Advanced PG fitting labs remain an explicit future expansion rather than being represented as already complete.

## Explainable problem solver

- Accepts natural-language prompts or mathematical notation from the Mathematics Menu.
- Classifies and solves arithmetic, named functions/constants, percentages, one-variable linear and quadratic equations, two-variable linear systems, symbolic calculus, and descriptive-statistics questions.
- Adds sign-chart reasoning for linear and quadratic inequalities, plus arithmetic/geometric sequence and series problems.
- Symbolic derivatives cover sums, products, quotients, constant/general powers, chain rule, trigonometric and inverse-trigonometric functions, exponentials, logarithms, roots, absolute values, higher orders up to 12, and partial derivatives.
- Symbolic antiderivatives cover linear combinations, powers, constant multiples, logarithmic forms, common trigonometric/exponential/root substitutions, and `ln(x)` integration by parts.
- Definite integrals accept `from … to …` bounds, including constants such as `pi`, and use adaptive Simpson integration with reversed-limit verification when no symbolic primitive is needed.
- Produces typed, numbered transformations instead of an opaque answer-only response.
- The Problem Solver UI now presents capability chips, a material-style answer summary, confidence bar, step count, verification status, and richer examples.
- Verifies algebra by substitution, calculus with numerical/symbolic checks, and data calculations with range/count invariants.
- Runs offline on the deterministic maths kernel and refuses ambiguous or unsupported questions rather than inventing a result.
- The solver boundary is intentionally extensible toward exact symbolic algebra, assumptions, units, geometry proofs, matrices, ODEs, and richer mathematical language.
- The first benchmark-parity computation slice adds exact rational arithmetic, polynomial expansion/collection/rational-root factorization, exact matrix determinant/inverse/RREF/transpose, and dimension-checked common unit conversions.

The current GeoGebra/Wolfram|Alpha gap assessment and three-phase implementation plan are documented in [GAP_ANALYSIS.md](GAP_ANALYSIS.md).

## Scientific Calculator

- Scientific Calculator is an active Maths destination from the Mathematics Menu.
- The module evaluates arithmetic, powers, roots, trigonometric and inverse-trigonometric functions, logs, exponentials, percentages, factorials, min/max, rounding helpers, and constants.
- DEG/RAD mode changes trigonometric interpretation and displays the normalized expression used by the engine.
- Results show decimal, scientific notation, engineering notation, exact hints where available, and a compact explanation of the evaluation pipeline.
- Built-in content includes reference cards for common scientific operations, constants such as `pi`, `e`, `c`, `g`, `h`, and `k`, plus starter unit conversions for length, area, mass, speed, energy, angle, and pressure.

## Knowledge Intelligence

- Formulas, Theorems, Visualize Formulas, Visual Proofs, Maths Dictionary, and MCQs are active Maths destinations instead of planned placeholders.
- The formula library has 15 browsable categories, including Geometry, Calculus, Trigonometry, Probability, Statistics, Linear Algebra, Number Theory, Combinatorics, Complex Numbers, Differential Equations, and Numerical Methods.
- Each formula category contains at least 17 formulas, and formula expressions are stored in LaTeX/KaTeX-style notation such as `\frac{}`, `\sqrt{}`, `\sum`, and braced exponents.
- The offline catalog covers school, undergraduate, and postgraduate seed material across algebra, calculus, geometry, probability, and statistics.
- Search and filters work across formulas, theorem statements, visual proof plans, dictionary terms, and MCQ explanations.
- MCQs are now a shared quiz module for Maths, Physics, Chemistry, Biology, Astro Physics, and IQ Labs.
- The shared MCQ bank contains over 100 questions across Basic, Intermediate, and Advanced levels.
- Each quiz session serves 15 questions with score, progress, answer locking, instant explanations, restart, and completion feedback.
- Visual proof entries carry workspace targets and construction steps so they can be wired into the existing 2D, graph, statistics, and 3D graph labs.

## Smart Maths Editor

- The Problem Solver input behaves like a lightweight mathematical IDE while retaining a normal editable text field and cursor selection.
- Functions, numbers, variables, constants, operators, natural-language keywords, and nested bracket levels receive distinct colors; unmatched brackets receive an error treatment.

## Unified Math Notebook

- Create named values with `a := 2`, dependent values with `b := a^2 + 3`, and graphable functions with `f(x) := a*x^2 + b`.
- Redefining or deleting a symbol atomically recomputes all dependent cells and shows missing/circular-dependency diagnostics without inventing a result.
- Toggle exact and decimal output, reuse earlier scalar results through `#1`, `#2`, and send a resolved function directly into the Graph workspace.
- Notebook inputs survive Android saved-state recreation; the engine also exposes a versioned JSON export boundary for future document persistence and sync.

## Linked maths kernel

- A shared linked-object layer now connects CAS/algebra, graph, table, geometry, and probability views from the same workspace definitions.
- Function objects expose canonical algebra, exact facts where available, graph samples, roots/extrema, and table rows with exact/numeric provenance.
- Geometry objects expose exact-style measurements such as segment distance and midpoint values from parent points.
- Probability distributions expose summary statistics, interval probability, graph plot points, and table rows through the same snapshot boundary.
- The Learning Coach shows linked-view counts and kernel diagnostics so cross-view consistency is visible while exploring.
- Live diagnostics report balanced/missing/mismatched brackets and distinguish mathematical tokens from natural-language text.
- Cursor-aware shortcuts insert `sin`, `cos`, `tan`, square root, logarithm, exponential, powers, `pi`, integral/derivative prompts, and definite bounds at the active selection.
- Context hints recommend higher/partial derivatives, definite bounds, and nested chain-rule expressions.

## Spatial mathematics

- Twelve solid families, including frustums, triangular prisms, Platonic solids, hemispheres, and torus.
- Renderer-neutral mesh topology shared by drawing, sub-object hit testing, highlighting, and section-plane intersection.
- Live volume, total surface area, face, edge, and vertex measurements.
- Direct object/vertex/edge/face selection plus local horizontal cross-sections and clipping.
- Perspective and orthographic projection modes; Canvas remains the fallback renderer boundary for a future GPU/ARCore view.

## Guided learning and classroom foundation

- Authored lesson definitions with allowed tools, accessibility labels, multiple checkpoints, proofs, progressive hints, and misconception feedback.
- Per-lesson progress tracks completed checkpoints, attempts, hint use, timestamps, and completion without reducing learning to a score.
- Learner and teacher views show assignment completion, support signals, and checkpoint-level evidence.
- Idempotent offline learning-operation queue provides a stable boundary for future authenticated synchronization.
- Versioned learning packages contain workspace, progress, and assignment metadata with JSON escaping, size limits, schema checks, and structural validation.

## ARCore spatial mode

- AR is optional: unsupported devices retain the complete app and an accessible seated spatial simulator.
- Runtime ARCore support/install/update checks and on-demand camera permission.
- Session configuration includes horizontal/vertical planes, environmental HDR, autofocus, and capability-gated Depth.
- Shared mathematical solids, measurements and transforms; explicit `1 unit` to meters/centimeters scale.
- Direct scene drag, pinch and twist gestures with atomic undo, local anchor persistence, placement reset and depth preference.
- Tracking/safety guidance and clear disclosure that spatial measurements are educational estimates.

## Build

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

## APKs

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release-ready unsigned APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Architecture

- `core`: renderer-independent math, explainable problem solving, expression parsing, adaptive/advanced graphing, analytic 2D/3D geometry, probability distributions, 3D solid measurements/meshes/cross-sections, and surface calculus.
- `learning`: authored lessons, checkpoint evaluation, progress, assignments, teacher summaries, offline operations, and validated learning packages.
- `spatial`: ARCore session/capability boundary, spatial placement, math-to-meter transforms, scale policy, tracking guidance, and simulator fallback.
- `workspace`: workspace model, command-based undo/redo, and versioned JSON export.
- `MainActivity.kt`: Compose app shell and renderers for the four primary workspaces.
- `test`: unit tests for the mathematical acceptance cases.
- `androidTest`: Compose launch and accessibility smoke tests.

## Notes

Studio 3D and the universal AR fallback use Compose Canvas projection. ARCore installation/session/Depth integration and renderer-neutral spatial state are present; a production GPU camera compositor, frame hit-testing and native anchor renderer remain the next physical-device integration step.
