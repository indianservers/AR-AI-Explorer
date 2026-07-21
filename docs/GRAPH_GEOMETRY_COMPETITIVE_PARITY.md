# Graph, 2D Geometry, and 3D Competitive Parity

Assessment date: 21 July 2026. Rankings measure current product maturity, not only the presence of a domain class or algorithm.

## Implementation update — both phases active

The Phase 1 and Phase 2 foundations are now implemented and exposed in Unified Math Studio.

- Unified commands now cover Graph; 2D free, division, attached and intersection points; triangle centres; lines; parallel/perpendicular constructions; circles; conics; tangents; transformations; seven constraint families; 3D points/vectors/lines/planes; and explicit, implicit and parametric surfaces.
- The studio exposes the command palette, dependency-aware object tokens and cross-domain undo/redo.
- Replay-based unified session persistence stores construction commands and cross-view links with bounded, versioned decoding.
- Phase 2 includes cross-view selection, dependency inspection, deterministic keyboard order, snapping announcements, macro recording/replay and linked value evaluation.
- Graph differentiation, tangent/normal analysis, adaptive integration, area-between-curves, deterministic roots and 100,000-row value tables share one calculus service.
- 3D surface analysis now includes CPU-independent gradients, normals, tangent planes, critical-point classification, marching-squares contours and gradient paths.
- CAS notebooks share assumptions across verified simplify, factor, expand, partial fractions, systems, inequalities, derivatives, integrals, limits, matrices, eigenvalues, row reduction and initial ODE workflows.

Implementation evidence:

- `core/CompetitiveMathParity.kt`
- `core/WorldClassMathSuite.kt`
- `core/DynamicGeometry.kt`
- `phase2/mathstudio/UnifiedMathStudioScreen.kt`
- `phase2/mathstudio/UnifiedMathStudio.kt`
- `WorldClassMathSuiteTest.kt` and `CompetitiveMathParityTest.kt`

| Area | 1st | 2nd | 3rd | AI Explorer position |
|---|---|---|---|---|
| Graphing | Desmos | GeoGebra | AI Explorer | Broad typed graph forms, large tables, regressions, residuals, confidence bands, transformations, audio trace, and structured summaries are implemented. Expression interaction, publishing, collaboration, and end-to-end accessibility still need product hardening. |
| 2D geometry | GeoGebra | Desmos | AI Explorer | Dependency-driven points, intersections, tangency, conics, constraints, locus/trace, transformations, protocol replay, groups, clipboard, and expanded `.ggb` interop exist. Tool discovery, algebra/geometry linkage, sharing, and classroom-scale validation remain behind. |
| 3D geometry | GeoGebra | AI Explorer | Desmos | Explicit, implicit, and parametric surfaces; direct constructions; mesh editing; cross-sections; intersections; projection; measurements; renderer tiers; and device gesture policies exist. GeoGebra remains ahead in proven construction workflow and ecosystem maturity. |

## Phase 1 — parity foundation

Goal: make the strongest existing Graph, Geometry, and 3D engines feel like one mathematics product.

- One typed command grammar for graph expressions, 2D points/midpoints/lines/circles, 3D points/vectors/lines/planes, and all three surface forms.
- One object-token model with view, kind, algebra, dependencies, accessible label, and deterministic focus order.
- Commands execute through the existing production reducers and construction engines, preserving validation and dependency behavior.
- Acceptance gate: commands create resolvable objects in all three views and produce a stable cross-view token sequence.

## Phase 2 — differentiated workflow

Goal: make multi-view construction inspectable, reusable, precise, and keyboard-operable.

- Cross-view links and selection propagation.
- Direct and transitive dependency inspection with invalid-construction detection.
- Deterministic keyboard navigation order across and within views.
- 2D point/grid/angle snapping and 3D grid/axis snapping with announcements.
- Parameterized construction macros replayed through the same validated command pipeline.
- Acceptance gate: linked selections expose every participating view, dependency reports are stable, snap targets are deterministic, and macros reproduce valid protocols.

## Next product increments

### Phase 1 continuation

1. Add the unified command palette and token navigator to the Compose workspace.
2. Add commands for the remaining existing Geometry objects and constraints.
3. Persist unified sessions, links, command history, and surfaces in the project codec.
4. Add import mapping from `.ggb` elements into unified tokens.
5. Add undo/redo transactions spanning multiple views.

### Phase 2 continuation

1. Render snap previews and dependency highlighting on canvas.
2. Add macro recording from selected protocol steps and a reusable construction library.
3. Connect graph values, 2D measurements, and 3D measurements through typed linked variables.
4. Add structured screen-reader summaries for selections, errors, and dependency changes.
5. Run phone, tablet, keyboard, mouse, and stylus interaction matrices plus large-project performance tests.

## Official comparison references

- GeoGebra Calculator Suite: https://help.geogebra.org/hc/en-us/articles/8379325433629-Calculator-Suite
- GeoGebra 3D Calculator: https://help.geogebra.org/hc/en-us/articles/8493632359069-3D-Calculator
- GeoGebra accessibility: https://help.geogebra.org/hc/en-us/articles/20048444963869-Accessibility
- Desmos Graphing Calculator: https://help.desmos.com/hc/en-us/articles/4406040715149
- Desmos tables and regressions: https://help.desmos.com/hc/en-us/articles/4405489674381-Tables and https://help.desmos.com/hc/en-us/articles/4406972958733-Regressions
- Desmos Geometry and transformations: https://help.desmos.com/hc/en-us/articles/15316366009997-Getting-Started-Desmos-Geometry and https://help.desmos.com/hc/en-us/articles/15364983456909
- Desmos 3D: https://help.desmos.com/hc/en-us/articles/19796006153997-Getting-Started-Desmos-3D
