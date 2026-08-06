# Solver Phase 1 Repository Audit

## Scope and date

This audit was completed before production implementation of the new `Solver` feature. It covers Phase 1 only. Math Camera, OCR, image recognition, graphing, geometry, statistics, lessons, themes, and the existing `Problem Solver` remain isolated.

## 1. Current Android architecture

- UI: native Android with Jetpack Compose and Material 3. The application is a single-activity Compose app.
- State: predominantly MVVM-style state holders. `ExplorerViewModel` owns the main shell state; several feature packages own smaller `ViewModel` classes.
- Navigation: the main mathematics shell uses additive Boolean destinations and `MathModule` workspace selection rather than a Compose `NavHost`. Some subject features have package-local navigation models.
- Dependency injection: no Hilt, Dagger, or Koin framework is configured. Dependencies are constructed explicitly.
- Persistence: Preferences DataStore is used for durable app/learning state. Small isolated features also use application-scoped `SharedPreferences`. No Room database is configured.
- Mathematical libraries: repository-owned `ExpressionEngine`, `SymbolicCasEngine`, `AdvancedSymbolicCas`, `ExtendedSymbolicCas`, exact `BigInteger` rational arithmetic, graph analysis, statistics, geometry, number-theory, calculus, and matrix engines.
- Parsing: repository-owned recursive-descent numeric parser and a separate exact symbolic CAS parser. No external parsing library is used.
- Graph rendering: Compose `Canvas` and repository-owned sampling/rendering engines. No external plotting library is present.
- LaTeX: no WebView/MathJax/KaTeX runtime. `FormulaLatexRenderer.kt` converts supported LaTeX-like source into readable Unicode/plain text.
- Tests: JUnit 4 local tests, AndroidX JUnit/Espresso, Compose UI test, and macrobenchmark modules. At audit time the repository contained 116 test files and approximately 782 `@Test` methods.

## 2. Existing mathematical keyboard

- Main components: `IntentAwareMathField`, `IntentAwareMathValueField`, `AdaptiveMathKeyboardPopup`, and `AdaptiveMathKeyboard`.
- Produced format: editable plain mathematical text, not a token stream. Examples include `pi`, `sqrt()`, `^`, `*`, `/`, Unicode relation symbols, and structured matrix text.
- Cursor behavior: `TextFieldValue` preserves selection and caret position. The keyboard supports insertion at the caret, selection replacement, left/right movement, backspace, clear, undo, redo, copy, cut, and paste.
- Symbols: arithmetic, variables, constants, powers, roots, fractions, functions, calculus, relations, brackets, sets, statistics, matrices, units, and scientific constants.
- Reuse decision: Solver uses `IntentAwareMathValueField` with `MathKeyboardContext.GENERAL`. No keyboard source is copied or modified.

## 3. Existing mathematical capabilities

- Parsing/evaluation: floating-point expression parsing with precedence, implicit multiplication, comparisons, functions, and graph-oriented constructs.
- Exact CAS: `ExactRational`, typed `SymbolicExpression`, simplification, expansion, substitution, factoring, derivatives, integrals, limits, equations/systems, inequalities, and matrix operations.
- Existing solver: `MathProblemSolver` and `MathSolverTutor` cover many broad problem classes and are used by the existing `Problem Solver` screen.
- Graphing: explicit, implicit, polar, parametric, inequality, data, 3D surface, trace, calculus overlays, and interaction engines.
- Units: scientific unit/conversion engines exist.
- Number theory: prime, modular, proof, and discrete-mathematics engines exist.
- Reuse decision: Phase 1 uses `ExactRational` and safe portions of `SymbolicCasEngine`; it introduces a Solver-owned public AST, positional parser, classifier, step contract, verification contract, and history boundary.

## 4. Isolated existing modules

The new Solver must not call or modify Camera/OCR/image classes, graph rendering, 2D/3D geometry, statistics/probability, lessons, keyboard implementation, theme implementation, existing saved-data codecs, or the legacy `Problem Solver` engine/UI.

## 5. Reusable components

- `ExactRational` for normalized exact arithmetic.
- `SymbolicCasEngine` for deterministic exact simplification, expansion, and simple factorisation after Solver parsing succeeds.
- `IntentAwareMathValueField` for keyboard-only input and cursor support.
- Existing Material theme and shared shell components.
- Android application storage for an isolated Solver history repository.

## 6. Missing capabilities addressed by Phase 1

- A self-contained Solver feature package.
- Public typed AST that represents relations, roots, fractions, lists, and systems.
- Token ranges and actionable syntax errors.
- Deterministic Phase 1 classifier with evidence/confidence.
- Traceable rule registry and structured equivalence-preserving steps.
- Structured verification that gates confident presentation.
- Solver-only local history with reopen/delete/clear.
- Explicit offline architecture checks.
- A separate `Solver` destination that does not replace `Problem Solver`.

## 7. Recommended Solver architecture

```text
solver/
  domain/model/          AST, classifications, steps, verification, results
  domain/parser/         normalizer, tokenizer, positional recursive-descent parser
  domain/classifier/     deterministic Phase 1 classification
  domain/steps/          rule registry
  domain/engine/         exact evaluator and Phase 1 solving pipeline
  domain/verification/   independent checks
  domain/repository/     history contract
  data/history/          isolated local history implementation
  presentation/          screen, ViewModel, UI state
  testing/               offline architecture guard
```

Pipeline:

```text
KeyboardSolverInputSource
  -> normalization
  -> tokenization/parser
  -> classification
  -> Phase 1 strategy
  -> structured steps
  -> verification
  -> local history
```

Future Camera/Image/Handwriting sources may implement `SolverInputSource`, but no such implementation or UI entry is part of Phase 1.

## 8. Expected file changes

Added:

- `app/src/main/java/com/indianservers/aiexplorer/solver/**`
- `app/src/test/java/com/indianservers/aiexplorer/solver/**`
- Solver Phase 1 audit, dependency, implementation, support, and limitation reports.

Minimally modified:

- `MainActivity.kt`: add a separate Solver shell state, destination branch, and tool entry.

Not modified:

- Math Camera/OCR/image code
- adaptive keyboard internals
- graph/2D/3D engines
- existing Solver/CAS behavior
- manifest permissions
- Gradle dependencies
- existing persistence formats
- theme definitions

