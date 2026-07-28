# Smart Board Recognition — Phases 4–6

## Outcome

The Smart Board recognition pipeline now continues beyond character recognition into editable
semantic structure, reviewable whole-board understanding, and specialist routing. These phases
reuse the existing CAS, typed graph, geometry-shape, Physics, Chemistry, Biology, persistence,
history, and accessibility foundations.

Recognition remains assistive:

- source ink is preserved;
- interpretations are shown before insertion;
- whole-board relationships require confirmation;
- accepted relationships are undoable;
- unsupported notation remains editable instead of being discarded.

## Phase 4 — Semantic expression trees

`SemanticExpressionTree` gives each recognized mathematical expression a renderer-independent
tree. Supported structural nodes include numbers, variables, negation, sums, products, powers,
functions, equations, inequalities, matrices, matrix rows, piecewise expressions, vectors,
coordinates, and unknown/editable fallbacks.

Each tree contains:

- stable path-based node IDs for future subexpression selection;
- authored LaTeX and the shared engine expression;
- MathML;
- an accessible spoken form;
- parser-verification state;
- confidence and conservative source-stroke links.

The implementation delegates ordinary expression parsing to `SymbolicCasEngine`, uses
`SmartBoardLatexAdapter` for notation preparation, and adds only the structural containers that
the shared CAS intentionally does not own.

The semantic tree is attached to `MathExpressionElement`. Board schema 7 persists and restores
the complete tree. Old schemas continue to migrate through the existing migration path.

## Phase 5 — Whole-board understanding

`SmartBoardWholeBoardUnderstandingEngine` creates a bounded, local interpretation of visible
board content:

- identifies formulas, derivations, graphs, geometry diagrams, data tables, annotations, results,
  and problem regions;
- uses persisted object types plus spatial proximity;
- suggests label, representation, derivation, and data-use relationships;
- removes duplicates against existing relationships;
- caps output for predictable memory and UI cost.

The Smart Board **More → Understand whole board** command opens a review card. It explains every
suggestion and its confidence. Nothing is written until **Add relationships** is selected.
Acceptance uses `AddRelationshipsCommand`, so one Undo restores the pre-analysis document.

## Phase 6 — Specialist recognizers

`SmartBoardSpecialistRecognitionRegistry` ranks specialist interpretations for:

- algebra and calculus;
- matrices;
- graphs;
- geometry;
- tables and data;
- Physics;
- Chemistry;
- Biology;
- mixed text and notation.

The registry is a router, not a parallel math engine. Evidence comes from the shared expression
analyzer, typed graph parser, semantic tree, existing shape recognition, board subject, unit
patterns, and existing subject terminology. Recognition Review displays the leading intent,
confidence, and actions before insertion.

## Save/open and editing behavior

- Newly recognized Mathematics objects are inserted with semantic trees.
- Direct LaTeX insertion creates semantic trees.
- Editing LaTeX rebuilds the tree from the new authored expression.
- Save/open preserves semantic structure, MathML, spoken form, confidence, and source mapping.
- Unknown structures remain editable and can be reinterpreted later.

## Verification

Focused tests cover:

- typed equation structure, MathML, spoken form, and source-stroke associations;
- matrix structure and specialist routing;
- schema-7 semantic-tree save/open round trips;
- whole-board region classification and non-destructive relationship suggestions.

