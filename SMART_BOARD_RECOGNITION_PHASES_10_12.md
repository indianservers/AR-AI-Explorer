# Smart Board Recognition and Interaction — Phases 10–12

These phases complete the recognition roadmap by converting recognized content into directly
editable classroom objects. They deliberately reuse AI Explorer's CAS, Graph, 3D Graph and
Geometry engines instead of turning the board into a static answer surface.

## Phase 10 — Semantic direct manipulation

- Every recognized mathematical expression keeps a structural semantic tree.
- Teachers can select meaningful subexpressions and apply Simplify, Expand, Factor or Negate.
- The shared symbolic CAS performs transformations; the board replaces only the selected node.
- The full expression tree is rebuilt and persisted after each edit.
- Every operation is a single undoable command.

## Phase 11 — Editable reconstruction

- Recognized matrices can become first-class editable tables.
- Table headers, cells, rows and columns are editable on the board with undo/redo.
- Table data, source links, visibility and bounds survive save/open.
- Graphable relations offer hand-offs to the existing editable 2D Graph and 3D Graph engines.
- Coordinates and vectors offer hand-off to the existing Geometry editor.
- Reconstructed objects retain links to their source expression for inspection.

## Phase 12 — Classroom authoring and presentation tools

- A classroom toolbox creates circles, triangles, rectangles, line segments, arrows, right-angle
  markers, coordinate axes, number lines, sticky notes and blank tables.
- Laser pointer and spotlight modes are transient presentation tools and never contaminate saved
  board content or undo history.
- A large classroom timer provides start, pause and common lesson presets.
- New objects remain selectable, movable, duplicable, hideable, deletable and keyboard reachable.
- Tables and semantic content expose structured accessibility descriptions.

## Data and compatibility

- Board schema version 8 introduces the `TableElement` record.
- Existing schema versions migrate through the established bounded migration path.
- Tables participate in rendering, selection, duplication, translation, export, intelligence
  context and tutor context.
- LaTeX export emits a tabular representation; structured export identifies dimensions.

## Verification

`SmartBoardFinalToolsTest` covers semantic subtree transformation, matrix reconstruction,
schema-8 persistence and directly-created classroom shapes. The existing Smart Board unit and
integration suites continue to cover history, recognition, save/open/edit and engine hand-offs.
