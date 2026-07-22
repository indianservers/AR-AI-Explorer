# Shared Mathematics Experience

AI Explorer's Unified Math Studio now treats Algebra, Graph, Table, 2D, 3D, CAS, Proof, Timeline and Inspector as live representations of one canonical mathematics document.

## Delivered capabilities

- **Universal live object graph:** `SharedObjectGraphBuilder` creates canonical, representation and derived nodes for the persisted document and live Graph/2D/3D construction protocol. Dependency and representation edges remain queryable without copying mathematical state into individual views.
- **Synchronized split workspace:** users can keep one to four panes open. Compact screens focus one of the active panes; larger screens arrange the same selection in a one-row or 2×2 layout.
- **Cross-view selection:** selecting a canonical object or any representation selects its complete representation set and produces an accessibility announcement listing the affected views.
- **Semantic undo timeline:** every expression edit, parameter move, mode/layout change and Graph/2D/3D construction records a human-readable label, revisions, selection and timestamp in one bounded history.
- **Branchable exploration:** editing after undo preserves both futures. Branches can be inspected, compared, opened and merged into a preferred continuation.
- **Variable provenance:** the inspector reports origin view, explicit assumptions, direct dependencies, transitive dependency chain and downstream dependents.
- **Session recording and replay:** state-changing direct manipulations are recorded with elapsed time. Timeline scrubbing restores a fully interactive state from which exploration can continue.
- **Adaptive workspace modes:** Explore, Learn, Proof, Exam, Research and Presentation configure appropriate linked views while preserving the same underlying document.

## Main implementation

- `app/src/main/java/com/indianservers/aiexplorer/phase2/mathstudio/SharedMathExperience.kt`
- `app/src/main/java/com/indianservers/aiexplorer/phase2/mathstudio/UnifiedMathStudio.kt`
- `app/src/main/java/com/indianservers/aiexplorer/phase2/mathstudio/UnifiedMathStudioScreen.kt`
- `app/src/test/java/com/indianservers/aiexplorer/SharedMathExperienceTest.kt`

## Verification

`SharedMathExperienceTest` covers graph representations and dependencies, synchronized selection and provenance, workspace modes and layouts, semantic branching/compare/merge, and deterministic recording/replay.
