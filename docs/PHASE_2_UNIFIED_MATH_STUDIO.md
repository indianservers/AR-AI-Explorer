# Phase 2 — Unified Creation and Computation

## Product outcome

AI Explorer now exposes a **Unified Math Studio** from the searchable Maths launcher. Algebra, graph, generated table, geometric key points, parameter sliders, and deterministic result pods are projections of the same typed maths document rather than disconnected tools.

## Delivered capabilities

- Add or edit named functions with safe parsing.
- Define linked functions such as `g(x) = f(x) + a`.
- Topological dependency recalculation with missing-reference and cycle rejection.
- Shared selection, visibility, colour, and parameter state across views.
- Live roots and extrema represented as geometric points on the graph.
- Generated value tables from the same resolved expressions used by the graph.
- Simplify, expand, factor, differentiate, and integrate result pods with rule traces and bounded unsupported states.
- Studio-wide undo and redo history.
- Responsive four-pane tablet layout and focused tab layout on phones.
- Automatic handoff to the Phase 1 workspace autosave/project archive.
- Parameter values persisted in the workspace schema.
- Real `.ggb` ZIP creation and bounded `.ggb` import, with explicit unsupported-object coverage.
- Accessible semantic labels for the studio, expression input, graph, and parameter controls.

## Safety and correctness boundaries

- Arbitrary code is never evaluated.
- Invalid edits retain the last valid document.
- Dependency cycles are rejected transactionally.
- GeoGebra ZIP input is size-, entry-count-, entry-size-, and path-bounded.
- Unsupported CAS operations return a bounded result instead of inventing an answer.
- GeoGebra translation reports skipped objects instead of silently discarding them.

## Quality gate

`validatePhase2UnifiedStudio` runs the full JVM contract suite, including linked recalculation, cycle rejection, transform provenance, undo/redo, visual-state persistence, Phase 1 archive compatibility, and real `.ggb` package round trips.
