# AI Explorer Gap Analysis vs Top Interactive Maths Tools

Assessment date: 16 July 2026

Scope: interactive maths learning, solving, graphing, construction and exploration tools. This intentionally excludes passive video-tutorial platforms. The benchmark set is:

1. GeoGebra Calculator Suite / Classic
2. Wolfram|Alpha
3. Desmos
4. Symbolab
5. Mathigon Polypad

## Five-phase 5/5 closure programme

### Phase 1 — Trusted maths kernel and universal object graph

**Implementation status (16 July 2026): implemented.**

- One UniversalMathDocument now represents scalars, expressions, equations, functions, 2D/3D points, vectors, matrices, datasets, geometry constructions, probability models, unit measurements, surfaces, solids, manipulatives, notebook cells and AR scenes.
- The existing linked CAS/algebra/graph/table/geometry/probability snapshot and the advanced graph object snapshot now expose the same typed document boundary.
- Workspace functions, points, dependency-driven constructions, solids, vectors, surfaces and AR placements are adapted into the universal dependency graph; notebook cells join the same ViewModel document.
- Revision-checked mutations report all transitively affected objects, refuse stale edits, reject missing dependencies and detect cycles.
- Bidirectional bridge operations apply verified symbolic, point and surface edits back into WorkspaceState; ordinary editors can safely stage incomplete input without entering verified computation.
- The trusted kernel now separates exact proof, deterministic numerical evidence, counterexamples, domain mismatch and inconclusive results.
- Real-domain analysis tracks division by zero, roots, logarithms, inverse trigonometric restrictions and trigonometric denominators, together with declared number-domain, sign, interval and non-zero assumptions.
- Workspace export embeds a schema-versioned, SHA-256-checksummed universal document. Record-level recovery, schema-1 migration and application of recovered state are implemented.
- The Phase 1 regression gate includes 10,000 generated exact integer identity cases plus parser/kernel integration samples, domain counterexamples, revision conflicts, cycles, recovery and round-trip tests.

Phase 2 should now build GeoGebra/Desmos-grade graphing, geometry and fully interactive trigonometry on this authority rather than introducing another state model.

### Phase 2 — GeoGebra/Desmos graph interaction, geometry and interactive trigonometry

**Implementation status (16 July 2026): implemented foundation.**

- Trigonometry now has a single synchronized angle model shared by direct unit-circle dragging, direct wave-cursor dragging, exact special-angle values, quadrant/reference-angle insights and the linked triangle view.
- Sine, cosine and tangent waves support live amplitude, period, phase and vertical-shift transformations. The screen retains compact movable/collapsible panels for phones while the canvas expands for tablets and TVs.
- The triangle kernel solves and validates SSS, SAS and the ambiguous SSA case, reports both valid SSA solutions and derives area, perimeter, circumradius and inradius.
- The identity lab verifies Pythagorean, ratio, double-angle and complementary identities through the trusted symbolic/numeric kernel and exposes domain assumptions instead of asserting identities outside their valid domain.
- A pure graph action reducer adds expression rows, folders, visibility, restrictions, inline validation, automatic parameter discovery, animation frames, revision history and deterministic undo/redo. Existing adaptive graph sampling remains segmented at discontinuities.
- Dynamic geometry now computes arbitrary supported line-like intersections plus line–circle, circle–circle and line–ellipse intersections, including tangent multiplicity behavior.
- Regression tests cover synchronized circle/wave state, transforms, triangle modes, trusted identity evidence, graph action replay/animation, discontinuity segmentation and geometry intersections.

Remaining production depth for this phase includes wiring every graph row/folder action into the Compose expression list, direct inverse curve dragging, complex/ODE/heatmap renderers, sonification, broader conic construction UI, GeoGebra file interoperability, and device performance profiling against the 60 fps gate.

### Phase 3 — Wolfram/Symbolab-style interpretation and solver intelligence

**Implementation status (16 July 2026): implemented foundation.**

- A deterministic query interpreter now normalizes Unicode notation, classifies equation/CAS/calculus/ODE/matrix/statistics/probability/unit intents, ranks alternative readings and reports confidence before calculation.
- Ambiguous inverse-trigonometric notation, ungrouped fractional multiplication and potentially ambiguous trigonometric powers are cited explicitly. Logarithm base and default-variable conventions appear as visible assumptions rather than silent guesses.
- The existing trusted local solver remains the answer authority. Interpretation selects a plan only; unsupported or divergent cases retain their original refusal evidence instead of being overwritten by a generic method error.
- Guided results now expose verified alternate compatible methods, step counts and teaching reasons, plus Exact, Decimal, Domain, Verification, Graph and Table forms with provenance.
- The advanced curriculum solver adds requested-order Maclaurin/Taylor expansions for sine, cosine, exponential and `ln(1+x)` with remainder/domain statements; stable two-sided numerical limits with residual evidence and safe divergence refusal; and first-order linear autonomous ODEs `y'=ay+b` with equilibrium shifting and optional initial conditions.
- Word-problem modelling now includes rectangle area/perimeter in addition to motion and finance, showing quantities, unknowns, relationships and equation setup before solving.
- The Problem Solver UI now has material interpretation, ambiguity, alternate-method and result-form cards while retaining hint-first disclosure and mistake diagnosis.
- Phase 3 regression tests cover notation normalization, ambiguity reporting, alternate methods, exact/decimal authority, Taylor order, convergent/divergent limits, initial-value ODEs and word-problem setup.

Remaining production depth includes a grammar/model-assisted optional interpreter, broader systems of ODEs and transforms, symbolic one-sided/infinite limits, special functions, dimensional compound units, a larger multilingual word-problem corpus and benchmarked parity suites across postgraduate topics.

### Phase 4 — Interactive activities, mastery, classroom, exam and accessibility

**Implementation status (16 July 2026): implemented foundation.**

- A serializable interactive-activity document now composes instruction, trusted maths response, MCQ, live workspace, manipulative-to-formal-maths, proof residual, adaptive branch and reflection blocks into one reachable graph.
- Teacher authoring supports immutable add/replace/remove revisions, graph validation, unreachable/missing-target diagnostics and SHA-256-checksummed export. The Learning Coach now contains a collapsible Activity Studio with block inspection, authoring, validation and learner preview.
- Learner runs record evidence without revealing expected answers, retry failed blocks, calculate scores and update per-skill mastery from attempts and hints. Adaptive branches use accumulated local evidence and schedule review intervals rather than making opaque AI decisions.
- The first production activity links the directly draggable unit circle to trusted identity verification, quadrant reasoning and a written invariant explanation.
- Classroom activity assignments use learner aliases, idempotent event IDs, completion/attempt/hint/score summaries, attention flags and common-struggle blocks. No network transport or personally identifying roster is introduced by this foundation.
- Exam-safe policies restrict modules, tools, hints, network, clipboard, files and AR; every decision is recorded in a SHA-256 chained audit trail with expiry enforcement.
- Accessibility foundations compile segmented graph curves into pitch, stereo pan, timing and emphasized root/turning-point notes, plus a spoken graph summary and semantic keyboard landmarks.
- Learning settings now expose spoken maths, graph audio and large touch-target modes alongside high contrast and reduced motion.
- Phase 4 tests cover activity reachability/export, trusted answer checks, retry and mastery branching, manipulative evidence, classroom idempotency, exam policy/audit integrity, graph sonification and keyboard navigation.

Remaining production depth includes drag-and-drop authoring on a large teacher canvas, signed package import, real-time classroom transport/consent controls, platform text-to-speech and audio playback, switch-device scanning, localized MathML speech, certified secure-exam platform integrations and large-scale classroom usability trials.

### Phase 5 — Production projects, interoperability and release readiness

**Implementation status (16 July 2026): implemented foundation.**

- A complete AI Explorer project archive now wraps the universal maths authority plus notebook, activities, learning progress, settings, spatial anchors and audit sections. Every section and the full archive are SHA-256 checksummed, bounded to 8 MB and schema-versioned.
- Archive recovery independently validates sections: damaged optional metadata is skipped while a verified Mathematics section can still open. Schema-1 migration is explicit and never presented as checksum-valid modern data.
- Deterministic three-way offline merge preserves independent object edits, uses object/document revisions for concurrent edits, reports deletion/edit conflicts and revalidates the resulting universal dependency graph.
- GeoGebra exchange now exports a bounded `geogebra.xml` foundation for points, expressions, lines, segments, rays, vectors, circles, polygons and ellipses, and imports supported point/expression elements. Every operation returns translated, skipped and warning coverage; unsupported objects are never silently discarded.
- Open SVG geometry and CSV function exports include explicit skipped-object/discontinuity coverage.
- Product performance budgets assess p95 frame time, graph evaluations, scene objects, archive size and working-memory estimates, selecting Ultra/High/Balanced/Low/Safety quality without removing core maths.
- Device capability assessment always retains 2D maths, CAS, solver, activities and simulated AR; GPU 3D, live AR and depth occlusion are enabled only when their declared prerequisites pass.
- A release gate combines tests, lint, recovery, migration, merge, interoperability, performance, mandatory AR privacy checks and physical-device AR evidence. Missing physical AR evidence produces a conditional release rather than a false pass.
- The Learning Coach now exposes a compact Project & Device Readiness card showing archive size, typed-object count, adaptive performance, GeoGebra coverage and simulator fallback.
- Phase 5 tests cover archive round-trip/recovery/migration, deterministic merge conflicts, GeoGebra exchange, SVG/CSV coverage, adaptive performance, non-AR fallback and release-gate honesty.

This completes the five-phase foundation. A truthful 5/5 production claim still requires external work that cannot be proven in unit tests alone: physical-device AR drift/depth/thermal QA, signed installer/release pipelines, security review, accessibility testing with disabled learners, classroom pilots, broader GeoGebra round-trip fixtures and independent mathematical benchmark audits.

### Phase 6 — Verification lab and release-candidate evidence

**Implementation status (16 July 2026): implemented foundation; physical evidence pending.**

- A deterministic maths benchmark runner executes bounded, uniquely identified cases through the same local tutor/kernel used by the product. It records expected evidence, actual output, topic, timing and pass rate, and refuses to treat unsupported transformations as successful answers.
- The initial release smoke catalogue covers exact arithmetic, algebra, calculus, trigonometry, matrices, statistics, units, number theory, series, ODEs and a required safe refusal. The catalogue is a regression gate, not a claim of independent benchmark parity.
- An AR device QA engine now evaluates timestamped physical samples for RMS anchor drift, depth error, frame-time p95, tracking loss and peak thermal state. It rejects short/sample-poor runs and cannot infer a physical pass from the simulator.
- Accessibility QA contracts check spoken labels, semantic roles, 48 dp touch targets, keyboard/switch reachability, text contrast and reduced-motion alternatives. This automates component-level evidence while leaving assistive-technology and disabled-learner testing as a required external gate.
- Project/activity payloads can be wrapped in an HMAC-SHA-256 classroom trust envelope using a caller-supplied 256-bit-or-stronger key. Verification rejects unknown keys, tampering, future issue times and expired envelopes; keys are never written into the package.
- A security configuration audit flags unexpected exported components, cleartext traffic, persisted/uploaded camera frames, embedded secrets, sensitive permissions and backup review. Camera-only, offline AR behavior is represented explicitly.
- QA evidence exports are deterministic, sectioned and SHA-256 tamper-evident. The in-app Release QA Lab runs the maths suite and shows accessibility/security status while preserving `NOT_RECORDED` for physical AR.
- Phase 6 tests cover reproducible maths outputs, signed-envelope tamper/expiry handling, unsafe configuration detection, AR evidence thresholds, accessibility blockers and evidence checksum failure.

This phase makes release evidence structured and reviewable. It does not convert pending external work into a pass: production signing credentials, independent maths audit, physical AR device matrix, real TalkBack/switch testing, classroom pilots and professional security review remain release responsibilities.

Official/current references used:

- GeoGebra Calculator Suite: https://help.geogebra.org/hc/en-us/articles/8379325433629-Calculator-Suite
- GeoGebra Calculator tutorial: https://www.geogebra.org/m/etg2rk8j
- Wolfram|Alpha Mathematics examples: https://www.wolframalpha.com/examples/mathematics
- Wolfram|Alpha Step-by-Step examples: https://www.wolframalpha.com/examples/pro-features/step-by-step-solutions
- Desmos calculators: https://www.desmos.com/
- Symbolab solver/calculators: https://www.symbolab.com/
- Mathigon: https://mathigon.org/
- Polypad virtual manipulatives: https://polypad.amplify.com/

## Current AI Explorer baseline

AI Explorer is now an Android-first interactive maths laboratory with:

- Subject hub with Maths active and other sciences staged.
- Dynamic 2D geometry, 2D graphing, 3D solids, 3D surface graphing, trigonometry, and AR spatial mode.
- Direct touch interactions: drag points/shapes, pan, pinch zoom, orbit 3D, manipulate solids, movable menu/tools.
- Advanced graphing foundation: explicit, piecewise, polar, parametric, implicit, tangent, normal, derivative, integral, intersections, extrema, tables, datasets and regression.
- Probability & Statistics lab: Normal, Binomial, Poisson, Uniform, Exponential; interval probabilities; charts; descriptive and early inferential statistics.
- Explainable Problem Solver: arithmetic, percentages, linear/quadratic equations and inequalities, systems, sequences/series, symbolic calculus, definite integrals and descriptive statistics.
- Exact computation slice: rational arithmetic, polynomial expansion/collection/factorization, determinant, inverse, RREF, transpose and selected unit conversions.
- Scientific Calculator: DEG/RAD trig, logs, powers, roots, constants, percentages, factorials, notation modes and starter conversions.
- Math Notebook: named scalar/function assignments, exact/decimal views, dependency recomputation, prior-cell references and Graph handoff.
- Linked maths kernel: first shared-object layer connecting CAS/algebra, graph, table, geometry and probability snapshots.
- Knowledge Intelligence: formulas, MCQs, theorem cards, visual proof cards and maths dictionary.
- ARCore foundation: AR optional dependency, support/install checks, camera permission path, session configuration and simulator fallback.

## Benchmark product positioning

| Product | Core strength | What AI Explorer can learn from it |
|---|---|---|
| GeoGebra | Deeply linked algebra, CAS, graph, geometry, 3D, spreadsheet/probability and classroom ecosystem | Mature cross-view object model, construction tools, spreadsheet, exam/classroom workflows |
| Wolfram|Alpha | Broad computational knowledge, exact symbolic/numeric answers, natural language and step-by-step solving | Coverage breadth, natural-language understanding, exactness, units, assumptions, result explanation |
| Desmos | Fast, elegant interactive graphing and classroom activities | Mobile-first graph UX, sliders, animations, accessibility, activity authoring and teacher dashboard |
| Symbolab | Step-by-step solver coverage with calculators by topic | Method-specific solving, guided steps, topic breadth, structured input templates |
| Mathigon Polypad | Virtual manipulatives and tactile mathematical exploration | Manipulatives, tiles, number bars, geometry components, classroom-friendly exploratory canvas |

## High-level parity matrix

Legend: Strong = production-level compared with category leaders; Partial = working foundation but incomplete; Weak = present only as seed/placeholder; Missing = not yet implemented.

| Capability | GeoGebra | Wolfram|Alpha | Desmos | Symbolab | Mathigon Polypad | AI Explorer today | Gap priority |
|---|---:|---:|---:|---:|---:|---:|---|
| 2D graphing | Strong | Strong | Strong | Strong | Partial | Partial/Strong | Improve UI polish, sliders, inequalities, fields, complex plots |
| 3D graphing | Strong | Strong | Partial | Partial | Weak | Partial | GPU renderer, surface quality, 3D controls, export |
| Dynamic geometry | Strong | Partial | Partial | Weak | Strong | Partial | Full construction tools, loci, constraints, proofs |
| CAS / symbolic algebra | Strong | Strong | Weak | Strong | Weak | Partial | Full simplify/factor/expand/substitute/assumptions/multivariate CAS |
| Natural-language maths | Weak/Partial | Strong | Weak | Partial | Weak | Weak/Partial | Robust parsing, ambiguity handling, multi-step word problems |
| Step-by-step solver | Partial | Strong | Weak | Strong | Weak | Partial | More methods, selectable solution paths, hints, mistakes |
| Scientific calculator | Strong | Strong | Strong | Strong | Weak | Partial | More functions, memories, units, complex/scientific constants |
| Linked algebra-graph-table | Strong | Partial | Strong | Partial | Weak | Partial | Bidirectional dependency graph and cross-selection |
| Spreadsheet / data table | Strong | Partial | Partial | Weak | Weak | Weak/Partial | Spreadsheet cells, import/export, linked lists |
| Statistics and probability | Strong | Strong | Partial | Partial | Weak | Partial | More distributions, tests, regressions, simulation, inference |
| Interactive activities | Strong | Partial | Strong | Partial | Strong | Partial | Authoring, shareable activities, teacher mode |
| Virtual manipulatives | Partial | Weak | Partial | Weak | Strong | Weak | Algebra tiles, fraction bars, geometry manipulatives |
| Notebook/document workflow | Partial | Strong | Weak | Partial | Weak | Partial | Autosave, document history, share/export, multi-page notebooks |
| Accessibility | Strong | Partial | Strong | Partial | Partial | Partial | Screen-reader maths, keyboard, sonification, color themes |
| Classroom/exam mode | Strong | Weak | Strong | Partial | Strong | Weak/Partial | Teacher dashboard, assignments, secure exam mode |
| Mobile/touch ergonomics | Strong | Partial | Strong | Strong | Strong | Partial | Reduce crowded panels, stronger gestures, adaptive layouts |
| Offline capability | Strong | Weak | Partial | Weak | Strong | Strong | Keep as differentiator while adding optional cloud intelligence |
| AR/spatial maths | Missing/Weak | Missing | Missing | Missing | Missing | Partial foundation | Strategic chance to exceed competitors |

## Detailed gap analysis

### 1. GeoGebra gap

GeoGebra remains the strongest benchmark for a complete linked mathematics environment. Its advantage is not just having many tools; it is that algebra, graphing, CAS, geometry, tables, sliders, construction protocols and probability views behave like one ecosystem.

AI Explorer strengths against GeoGebra:

- Better Android-native dark/mobile experience direction.
- Early ARCore/spatial-maths foundation, which GeoGebra does not center as a core differentiator.
- Offline deterministic solver and learning modules can become more guided than GeoGebra’s general-purpose interface.
- Touch-first 3D object manipulation and future AR lessons can create a stronger spatial-learning identity.

Main gaps:

- GeoGebra’s construction toolset is much deeper: loci, transformations, relation checks, symbolic geometry links, mature object properties.
- GeoGebra’s linked views are more bidirectional and mature; AI Explorer has a linked snapshot layer but not yet full reactive object editing across every view.
- GeoGebra has a spreadsheet/data workflow and classroom/exam ecosystem; AI Explorer only has foundations.
- GeoGebra’s `.ggb` save/share ecosystem and activity library are mature.

Top fixes:

1. Convert linked maths kernel into a real bidirectional dependency graph.
2. Add full construction/loci/transform/proof-reason toolset.
3. Add spreadsheet cells, lists and CSV import/export.
4. Add activity authoring, teacher assignments and exam mode.

### 2. Wolfram|Alpha gap

Wolfram|Alpha is the benchmark for breadth and exact computational knowledge: symbolic algebra, calculus, differential equations, number theory, matrices, statistics, units, constants and natural-language interpretation.

AI Explorer strengths against Wolfram|Alpha:

- Direct manipulation and visual workspace are much more central.
- Offline deterministic answers are transparent and app-controlled.
- AR/spatial maths can exceed WA in embodied learning.
- The local Problem Solver can become more pedagogical and less answer-only.

Main gaps:

- Wolfram|Alpha’s symbolic breadth is far ahead: ODEs, transforms, series, number theory, assumptions, special functions, units and domain knowledge.
- Natural-language parsing is much broader and more forgiving.
- WA provides multiple result forms, plots, exact/numeric comparisons and alternate methods.
- AI Explorer currently refuses many unsupported questions; this is honest, but users will feel the coverage gap.

Top fixes:

1. Add an expression-tree CAS with assumptions, exact domains and simplification rules.
2. Expand solver coverage: limits, series, ODEs, matrices, vectors, complex numbers, combinatorics and number theory.
3. Add units/dimensional algebra with significant figures and constants.
4. Add optional AI/cloud interpretation layer that routes to deterministic local kernels and cites uncertainty.

### 3. Desmos gap

Desmos is the benchmark for fast interactive graphing, elegant sliders, classroom activities and accessible graph exploration.

AI Explorer strengths against Desmos:

- Broader scope: 2D geometry, 3D solids, 3D surfaces, probability, statistics, scientific calculator, notebook and AR.
- More solver/CAS direction than Desmos.
- Native Android workspace can integrate subject/menu flows beyond graphing.

Main gaps:

- Desmos graph UX is smoother, cleaner and faster.
- Slider creation/animation is more natural in Desmos.
- Desmos activities and teacher dashboards are a major classroom advantage.
- Accessibility, keyboard entry and graph auditory/alternative representations need to mature.

Top fixes:

1. Add instant parameter sliders for every variable, with animation and play controls.
2. Make graph entry/editing feel as simple as Desmos: rows, colors, visibility, folders, notes.
3. Add accessible graph descriptions, sonification and keyboard workflows.
4. Build activity authoring templates and classroom session mode.

### 4. Symbolab gap

Symbolab is a strong benchmark for topic-specific step-by-step solving: algebra, calculus, trig, matrices, equations, integrals, derivatives and calculators by domain.

AI Explorer strengths against Symbolab:

- More interactive visual workspaces, geometry and future AR.
- Offline explainable solver can become more trustworthy for supported domains.
- Better opportunity to link solver results directly to graph/geometry/table views.

Main gaps:

- Symbolab has broader topic coverage and more method-specific calculators.
- AI Explorer has fewer alternate solution methods.
- AI Explorer lacks mature structural templates for fractions, matrices, derivatives, integrals and systems.
- Guided mistake feedback and progressive hints are early.

Top fixes:

1. Add solver “method selector”: factoring, completing square, quadratic formula, substitution, elimination, integration method, etc.
2. Add structural maths keyboard/templates.
3. Add step-by-step hint mode before full answer reveal.
4. Add more calculators: matrices, complex numbers, vectors, limits, series, ODEs, probability, finance and units.

### 5. Mathigon Polypad gap

Mathigon Polypad is the benchmark for exploratory virtual manipulatives: tiles, polygons, fraction bars, number lines, geometric tools and classroom-ready tactile learning.

AI Explorer strengths against Polypad:

- Stronger computation/solver/graphing/statistics direction.
- 3D and AR spatial-maths path can go beyond Polypad’s 2D manipulative canvas.
- The app’s formulas, MCQs, notebook and problem solver can connect exploration to formal solving.

Main gaps:

- AI Explorer has not yet built a rich manipulative canvas.
- No algebra tiles, fraction bars, integer chips, balance scales, number lines, pattern blocks or drag-and-snap classroom components.
- Visual proof cards exist as content, but not yet as one-tap interactive proof scenes.
- Teacher-friendly activity creation is still foundational.

Top fixes:

1. Add a Manipulatives workspace.
2. Implement snap grids, trays, duplicate/clone gestures and object grouping.
3. Add algebra tiles, fraction bars, number lines, balance scale and geometric tiles.
4. Link manipulatives to equations, graph tables and solver steps.

## AI Explorer differentiators to protect

These are areas where AI Explorer can become better than the benchmarks instead of copying them:

1. AR/spatial maths: place functions, solids, vectors, probability surfaces and geometry constructions in the real world.
2. One object across all views: a theorem, formula, graph, table, proof, notebook cell and AR model should reference the same mathematical object.
3. Offline-first explainability: deterministic local computation with transparent refusal when unsupported.
4. Mobile-first direct manipulation: touch, pinch, drag, orbit and movable controls as primary interaction, not desktop UI squeezed onto phones.
5. Unified learning path: formula → visual proof → interactive lab → problem solver → MCQ → notebook → AR extension.

## Prioritized missing features

### Critical

- Real bidirectional CAS/algebra/graph/table/geometry/probability dependency graph.
- Full symbolic expression tree: simplify, expand, factor, substitute, assumptions, domains and exact values.
- Desmos-quality graph editor: rows, folders, sliders, animations, restrictions, inequalities and fields.
- Structural maths keyboard/input templates.
- More step-by-step solver methods and hint-first pedagogy.
- Autosave/history/export for notebooks and workspaces.

### High

- Spreadsheet/data table with CSV import/export and linked plots.
- Full construction/loci/transform/dynamic-geometry toolset.
- Advanced statistics: ANOVA, chi-square, non-parametric tests, regression diagnostics, resampling and simulations.
- More probability distributions and random experiment simulations.
- Matrix/vector/complex-number workbenches.
- Activity authoring and teacher dashboard.
- Accessibility: screen-reader maths, keyboard navigation, graph sonification and high-contrast themes.

### Medium

- Virtual manipulatives: algebra tiles, fraction bars, balance scale, number lines and pattern blocks.
- `.ggb` import/export research.
- Cloud optional intelligence for natural-language routing and broader explanations.
- More curriculum packs and localized content.
- Secure exam mode.

### Strategic

- Production ARCore renderer with camera compositor, plane/depth hit tests, anchors, occlusion and shared AR lessons.
- GPU-backed 3D renderer shared between 3D graph, solids and AR.
- Collaborative real-time classroom sessions.

## Seven-phase implementation roadmap

This replaces the earlier three-phase sketch with a more practical seven-phase plan. The order is intentional: first make the graphing and shared-object foundation feel excellent, then deepen exact maths, then expand learning, classroom and AR.

### Phase 1 — Desmos-quality graphing and shared object foundation

Goal: make the graph workspace fast, clean, delightful and linked. This is the highest-leverage phase because Desmos-quality graphing becomes the front door for algebra, CAS, tables, statistics, solver output and AR later.

Implementation status, 16 July 2026: Phase 1 Slice 1 through Slice 7 have started. A renderer-neutral `MathObjectGraph` now owns expression rows, auto-detected parameter slider rows, generated table rows, graph samples, roots/extrema and algebra facts. Parameter edits recompute dependent graph/table/algebra outputs together, and Graph Insights exposes the new object-graph row/slider/table counts. The graph definitions panel now uses Desmos-style expression cards with color/visibility controls, duplicate/delete actions, inline diagnostics, resolved-expression previews, live parameter sliders and a generated table preview. Row organization supports collapse/expand, folder labels, note fields and slider play/pause loop animation. Row cross-selection marks an active expression row, promotes its equation chip, drives Graph Insights from the selected expression and emphasizes the selected curve on the canvas. Canvas curve hit testing lets users tap near an explicit or implicit curve to select the matching expression row. Slider animation has loop/bounce modes plus slower/faster speed controls. Graph row metadata and slider playback settings are now persisted in workspace state, exported through workspace JSON and updated through undoable commands. Richer hit-test feedback and full graph-polish work remain the next Phase 1 slices.

Core work:

- Add a `MathObjectGraph` layer that owns expressions, parameters, sliders, tables, graph definitions, solver outputs and notebook links.
- Replace one-way linked snapshots with dependency invalidation and recomputation.
- Add Desmos-style expression rows:
  - each row has expression text, color, visibility, delete, duplicate and error state;
  - rows support notes, folders/groups and collapse/expand;
  - rows can represent functions, constants, sliders, tables, points, inequalities and datasets.
- Auto-detect free variables and offer instant sliders.
- Add slider controls:
  - min/max/step;
  - play/pause animation;
  - loop/bounce modes;
  - speed control;
  - direct drag on slider thumb.
- Improve graph viewport:
  - smooth pan/pinch zoom;
  - double tap to fit;
  - lock axes;
  - equal-scale mode;
  - grid density adaptation;
  - axis labels and tick formatting.
- Render graph types already present in the core but not fully surfaced:
  - inequalities;
  - sequences;
  - vector fields;
  - slope fields;
  - region plots;
  - better implicit curves;
  - domain/range restrictions.
- Add generated table for every expression and parameterized table updates.
- Add cross-selection: tap graph curve → highlight row/table/CAS object; tap row → highlight graph.

Desmos-specific UX target:

- Expression editing must be quick enough to use one-handed on mobile.
- Graph canvas should not be covered by tool grids.
- Common actions must be 1–2 taps: add expression, add slider, hide curve, change color, open table, fit graph.
- Errors should be inline, readable and forgiving.

Tests and acceptance:

- Editing a parameter updates graph samples, table rows and algebra facts together.
- Slider animation changes dependent curves without stale state.
- Inequality/sequence/vector-field definitions produce renderable objects.
- Graph workspace remains usable on small mobile screens, tablets and landscape orientation.

### Phase 2 — CAS, exact maths and structural input

Goal: move from “good solver foundations” to a real exact maths engine that can compete with GeoGebra CAS, Symbolab and parts of Wolfram|Alpha.

Implementation status, 16 July 2026: Phase 2 Slice 1 has started. A shared typed symbolic AST now backs a new `SymbolicCasEngine` with exact rational leaves, variables, sums, products, powers and functions. The first CAS operations support simplify, expand, substitute and small univariate factorization with exact/decimal result rows, assumptions and transformation steps. Fraction normalization, arithmetic identities and univariate polynomial collection are implemented, with safe unsupported-operation refusal. Exact complex and vector helper types provide rectangular complex arithmetic, magnitude-squared, vector dot product and projection foundations. The Math Notebook now exposes CAS rows and a structural maths keyboard seed for fractions, powers, roots, matrices, vectors, systems, derivatives, integrals, sums and limits. Remaining Phase 2 work includes broader multivariate/rational-function CAS, assumptions/domains, equation solving, matrix rank/null/eigen workflows, complex roots/graphs and making calculator/solver/graph all consume the same AST directly.

Core work:

- Introduce a typed symbolic expression tree shared by calculator, solver, notebook, graph and CAS.
- Add simplification rules:
  - arithmetic identities;
  - polynomial collection;
  - rational simplification;
  - power/log/trig identities with safe assumptions;
  - fraction normalization.
- Add operations:
  - simplify;
  - expand;
  - factor;
  - substitute;
  - solve;
  - evaluate exactly/approximately;
  - domain/range basics.
- Expand polynomial support:
  - multivariate terms;
  - rational functions;
  - roots with multiplicity;
  - partial fractions foundation.
- Expand matrix/vector support:
  - matrix arithmetic;
  - rank;
  - null space;
  - eigenvalues/eigenvectors for small cases;
  - vector dot/cross/projection.
- Add complex-number support:
  - rectangular and polar form;
  - magnitude/argument;
  - roots of complex numbers;
  - complex graph foundations.
- Add structural maths keyboard:
  - fractions;
  - powers;
  - roots;
  - matrices;
  - vectors;
  - systems;
  - derivatives;
  - definite/indefinite integrals;
  - sums/products;
  - limits.
- Add CAS rows to notebook and graph:
  - exact result;
  - decimal result;
  - assumptions;
  - transformation steps.

Tests and acceptance:

- Same expression has one AST across calculator, solver, graph, CAS and notebook.
- Exact and decimal outputs agree within tolerance.
- Unsupported transformations refuse safely with explanation.
- Symbolic operations are covered by regression tests and fuzz tests for simple algebra.

### Phase 3 — Symbolab/Wolfram-style step-by-step solver intelligence

Goal: make Problem Solver broader, more teachable and less opaque, while preserving deterministic local verification.

Implementation status, 16 July 2026: Phase 3 foundation is implemented. A new deterministic `MathSolverTutor` adds explicit factoring, completing-square, quadratic-formula, substitution, elimination, graph/table, u-substitution, integration-by-parts, partial-fractions and numerical method selection, with compatibility checks that refuse invalid method/problem pairings. The Problem Solver now starts hint-first, supports next-step, reveal-method, reveal-answer and per-step “Why?” interactions, while the final result remains independently verified by the local kernel. Extended deterministic topic foundations cover removable limits, common Taylor/Maclaurin series, separable `dy/dx = ky` ODEs, arithmetic recurrences, permutations/combinations, GCD/LCM/primality/factorization, simple/compound finance, significant figures and independent uncertainty propagation. A word-problem model extracts quantities, units, relationships, unknowns and equation setup for motion problems and cites ambiguity when information is missing. Learner-working diagnostics identify bracket, sign/arithmetic, real-domain and mixed-unit errors. Verified results expose no-retyping handoffs for Graph, generated tables, Notebook and MCQ practice; Graph and Notebook handoffs are connected to their live workspaces. Broader symbolic limits/ODE families, complete partial-fraction integration, richer dimensional word models, a dedicated spreadsheet workspace and geometry-construction generation remain follow-on depth.

Core work:

- Add solver method selector:
  - factoring;
  - completing the square;
  - quadratic formula;
  - substitution;
  - elimination;
  - graph/table method;
  - integration by substitution;
  - integration by parts;
  - partial fractions;
  - numeric approximation.
- Add hint-first mode:
  - first hint;
  - next step;
  - reveal method;
  - reveal answer;
  - ask “why?” for a step.
- Expand topic coverage:
  - limits;
  - Taylor/Maclaurin series;
  - ODE basics;
  - recurrence relations;
  - combinatorics;
  - number theory;
  - finance maths;
  - units and dimensional algebra;
  - significant figures and uncertainty.
- Add word-problem parser:
  - quantities;
  - units;
  - relationships;
  - known/unknown variables;
  - equation setup before solving.
- Add mistake diagnosis:
  - sign errors;
  - bracket errors;
  - wrong formula choice;
  - domain errors;
  - unit mismatch.
- Link solver output into workspaces:
  - send equation to Graph;
  - send table to Spreadsheet;
  - send construction to Geometry;
  - save derivation to Notebook;
  - generate MCQs from solved example.
- Optional future AI interpreter:
  - converts natural language to deterministic kernel plan;
  - must cite ambiguity;
  - local kernel remains source of truth.

Tests and acceptance:

- Each supported problem has method-tagged steps and verification.
- Solver can provide hints without revealing final answer.
- Word problems show model setup before calculation.
- Solver results can be sent into graph/table/notebook without retyping.

### Phase 4 — GeoGebra-level geometry, tables and statistics

Goal: close the major GeoGebra gaps: construction depth, spreadsheet/list data and probability/statistics workflows.

Implementation status, 16 July 2026: Phase 4 foundation is implemented. The new `DynamicGeometryEngine` provides dependency-driven free, midpoint, translated, rotated, reflected, dilated and line-intersection points plus line, parallel, perpendicular, circle, ellipse, tangent, fixed-angle and locus object definitions. Moving a free parent recomputes every descendant; exact/symbolic distance, slope, polygon area, angle, circle radius/diameter and ellipse parameters are available. Construction protocol entries retain parent dependencies and proof reasons, support visibility changes, dependency trees and step replay, and the Geometry workspace exposes exact measurements and replay controls. A new `MathSpreadsheetEngine` provides editable A1 cells, range formulas, named columns/lists, CSV import/export, three missing-data policies and revision-linked plot series; the Probability & Statistics module now includes a responsive Spreadsheet tab with live plots and regression diagnostics. `Phase4Statistics` adds permutation-backed one-way ANOVA, chi-square, Mann–Whitney, simple/multiple/logistic regression, residuals, confidence/prediction intervals and seeded bootstrap resampling, with assumptions and diagnostics surfaced in the Statistics UI. Probability foundations add Student-t, chi-square, gamma, beta, geometric and hypergeometric models, seeded coin/die/card/Bernoulli/Monty Hall experiments, Monte Carlo π, Bayes trees, conditional probability and a combinatorics lab through a new Experiments tab. Acceptance tests verify dynamic parent recomputation, sheet-to-plot/statistics updates, diagnostic reporting and exact seeded reproducibility. Broader renderer integration for loci/tangents/conics, a cell-by-cell touch grid, exact symbolic conic algebra and production-scale regression solvers remain subsequent depth work.

Geometry work:

- Add construction tools:
  - locus;
  - relation/check;
  - perpendicular/parallel through point;
  - angle with fixed measure;
  - transformations: translate, rotate, reflect, dilate;
  - conics from points/foci;
  - tangent lines;
  - intersection of any supported objects.
- Add exact geometry measurements:
  - symbolic distance;
  - area;
  - angle;
  - slope;
  - circle radius/diameter;
  - conic parameters.
- Add construction protocol improvements:
  - step replay;
  - hide/show construction steps;
  - dependency tree;
  - proof reason notes.

Tables/statistics work:

- Add spreadsheet/list workspace:
  - editable grid;
  - formulas;
  - named columns/lists;
  - CSV import/export;
  - missing-data handling;
  - linked plots.
- Expand statistics:
  - ANOVA;
  - chi-square tests;
  - non-parametric tests;
  - linear/multiple/logistic regression;
  - residual plots;
  - confidence/prediction intervals;
  - bootstrap/resampling;
  - Monte Carlo simulations.
- Expand probability:
  - more distributions;
  - random experiments;
  - Bayes trees;
  - conditional probability visualizer;
  - combinatorics lab.

Tests and acceptance:

- Geometry objects remain dynamic after parent changes.
- Spreadsheet data updates linked plots/statistics.
- Statistical procedures report assumptions and diagnostics.
- Probability simulations are reproducible with seeds.

### Phase 5 — Mathigon/Polypad-style manipulatives and visual proof labs

Goal: make AI Explorer tactile and exploratory, not just computational.

Implementation status, 16 July 2026: Phase 5 foundation is implemented. A new `Manipulatives` workspace and `ManipulativeEngine` provide algebra tiles, fraction bars, integer chips, number-line points/intervals, balance weights, pattern/geometric blocks, rulers, protractors and angle tools. Objects live in six trays and support direct drag/drop, configurable snap grids, duplicate, group/ungroup, rotate, scale, lock/unlock, delete, labels and annotations. The complete scene is revisioned and JSON serializable. Formal adapters translate algebra/unit tiles into equations and graph expressions, combine fraction bars into reduced rational values, convert number-line selections to interval notation, convert scale pans to equations, and generate area/perimeter notebook entries from geometric tiles. A new `VisualProofEngine` supplies 10 runnable parameterized scenes: triangle angle sum, Pythagorean rearrangement, derivative as slope, integral as area, normal probability area, vector addition, matrix area transformation, circle circumference ratio, binomial-square area and shear-invariant area. Each scene supports step playback, parameter dragging, live measurements, invariant residual checks, formal results and “what changes / what stays same” prompts. These labs are available both inside the tactile workspace and from the existing Visual Proofs menu. Acceptance tests verify create/move/snap/group/transform/lock/serialize operations, formal Graph/Equation/Notebook links, 10 unique runnable proofs, playback and invariant preservation. Richer shape-specific rendering, multi-touch rotation/scaling and full deserialization/import remain follow-on polish.

Core work:

- Add Manipulatives workspace with:
  - algebra tiles;
  - fraction bars;
  - integer chips;
  - number lines;
  - balance scale;
  - pattern blocks;
  - geometric tiles;
  - angle/ruler/protractor tools.
- Add manipulation UX:
  - trays;
  - drag/drop;
  - clone/duplicate;
  - snap grids;
  - group/ungroup;
  - rotate/scale;
  - lock/unlock;
  - labels and annotations.
- Link manipulatives to formal maths:
  - algebra tiles → equation/CAS;
  - fraction bars → rational expression;
  - number line → inequalities/intervals;
  - balance scale → equation solving;
  - geometric tiles → area/perimeter formulas.
- Convert visual proof cards into interactive labs:
  - triangle angle sum;
  - Pythagorean theorem;
  - derivative as slope;
  - integral as area;
  - normal distribution area;
  - vector addition;
  - matrix transformation.
- Add proof playback:
  - step-by-step animation;
  - drag-to-test invariants;
  - “what changes / what stays same” prompts.

Tests and acceptance:

- Manipulative objects can be created, moved, snapped, grouped and serialized.
- At least 10 visual proofs become runnable interactive scenes.
- Manipulative state can generate an equation, graph or notebook entry.

### Phase 6 — Classroom, activities, accessibility and exam mode

Goal: compete with Desmos Classroom, GeoGebra Activities and Polypad classroom use.

Activity/classroom work:

- Add activity authoring:
  - screens;
  - prompts;
  - tool restrictions;
  - checkpoints;
  - hints;
  - rubrics;
  - teacher notes;
  - solution paths.
- Add learner session flow:
  - start activity;
  - save progress;
  - submit;
  - retry;
  - reflection prompts.
- Add teacher dashboard:
  - progress overview;
  - common misconceptions;
  - submissions;
  - anonymized class responses;
  - export reports.
- Add offline-first sync boundary for future accounts/cloud.
- Add secure exam mode:
  - locked toolset;
  - visible exam banner;
  - offline timer;
  - integrity events;
  - exportable audit report.

Accessibility work:

- Screen-reader-safe math labels.
- Keyboard and D-pad navigation.
- High-contrast themes.
- Color-blind palettes.
- Graph sonification.
- Spoken graph summaries.
- Large-touch mode.
- Localization-ready strings.

Tests and acceptance:

- Activity package validates schema and can be resumed.
- Teacher dashboard summarizes checkpoints.
- Exam mode visibly locks the configured toolset.
- Core graph and solver flows are keyboard/screen-reader accessible.

### Phase 7 — AR spatial maths and production 3D renderer

Goal: exceed the benchmark tools by making maths spatial, embodied and collaborative.

**Implementation status (16 July 2026): implemented.** A renderer-neutral spatial scene now compiles solids, surface graphs, vector fields, probability surfaces, annotations, materials, axes and uncertainty-labelled measurements into one OpenGL ES 3 draw plan. Geometry 3D, Graph 3D and AR use that same compiler. Typed ray picking covers points, curves, surfaces, solids, fields and annotations. The live AR path now has an external-camera-texture compositor and frame loop, ARCore view/projection matrices, plane/depth/point hit tests, native anchors, anchor-state/relocalization models, automatic depth acquisition, environmental light estimates, safe placement validation, adaptive thermal policies, and the no-camera simulator fallback. Six spatial lessons cover conics, vectors, gradients, cross-sections, probability landscapes and an astronomy/physics bridge. Classroom sharing is represented by a transport-neutral future envelope and remains deliberately disconnected. Physical-device depth quality, drift, thermal and privacy checks remain release QA and are documented in AR_PRIVACY_SAFETY_CHECKLIST.md.

Renderer work:

- Add GPU-backed 3D renderer shared by:
  - 3D solids;
  - 3D surface graphs;
  - vector fields;
  - probability surfaces;
  - AR scene rendering.
- Add picking/hit testing for points, curves, surfaces, solids and annotations.
- Add high-quality materials, labels, axes and measurement overlays.

ARCore work:

- Complete camera compositor and frame loop.
- Add real plane/depth hit tests.
- Add anchors and relocalization handling.
- Add depth occlusion and environment lighting.
- Add measurement uncertainty display.
- Add safe placement bounds and thermal/performance handling.
- Add privacy/safety review checklist.

Spatial learning work:

- Place graphs, surfaces, geometry constructions, vectors and solids into real space.
- Add AR lessons:
  - conic sections;
  - vector addition;
  - surface gradients;
  - volumes/cross-sections;
  - probability surfaces;
  - astronomy/physics future subject bridge.
- Add shared AR/classroom spatial sessions as future network layer.

Tests and acceptance:

- Non-AR devices keep full simulator fallback.
- ARCore-supported devices can place, move, scale and persist mathematical objects.
- 3D renderer is reused outside AR, not isolated.
- Spatial measurements clearly show educational-estimate status.

## Recommended next implementation slice

Start with Phase 1, Slice 1:

1. Add a `MathObjectGraph` model with expression rows, parameter rows and table rows.
2. Build dependency invalidation/recompute from the current linked maths kernel.
3. Add Desmos-style row metadata: color, visible, collapsed, error, folder id and note text.
4. Auto-detect parameters and generate slider definitions.
5. Add tests proving that editing a parameter updates algebra facts, graph samples and table rows together.

This directly attacks the strongest Desmos and GeoGebra gaps while also preparing CAS, notebook, solver, spreadsheet and AR reuse.
