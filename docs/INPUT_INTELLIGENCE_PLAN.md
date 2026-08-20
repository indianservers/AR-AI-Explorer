# Input Intelligence Plan

## Goal

Make mathematical entry faster and more forgiving across Graph 2D, Graph 3D, CAS,
Solver, Calculus, Matrix, Statistics, Sets, and Science inputs while preserving the
existing adaptive keyboard layout and offline-first behavior.

## Product rules

- Keep the keyboard shell, pages, key grids, navigation, and action row unchanged.
- Reuse the existing structured-entry row for intelligence; never add keyboard height.
- Never rewrite user input silently. Repairs are previewed and applied only on tap.
- Preserve cursor and selection through every completion or repair.
- Keep analysis deterministic, local, testable, and independent of a network model.
- Prefer parser-safe notation in inserted templates.

## Phase 1 - Shared intelligence engine (implemented)

- Add a typed catalog of common functions and commands with signatures, parameters,
  descriptions, templates, examples, and workspace relevance.
- Detect the token fragment at the cursor and rank prefix matches by workspace.
- Recognize one-edit function-name mistakes and offer explicit repair actions.
- Detect missing closing delimiters, implicit multiplication, Unicode operators, and
  unwrapped simple function arguments.
- Detect the active nested function call and current parameter.
- Suggest useful values for angles, variables, bounds, matrices, conditions, and names.
- Provide Graph 2D, Graph 3D, Calculus, Matrix, Sets, Statistics, Science, and general examples.
- Apply edits with bounded replacement ranges and an explicit post-edit cursor position.

## Phase 2 - Keyboard and editor integration (implemented)

- Turn the existing structured-entry row into a context-sensitive suggestion strip.
- Priority order: repair, autocomplete, parameter values, then examples.
- Keep structural fraction/root/power controls available through the `Math` chip.
- Restore the suggestion strip automatically after a structural edit.
- Show the active function parameter in the strip and the existing editor tip line.
- Carry the same integration to every screen already using `IntentAwareMathField` or
  `IntentAwareMathValueField`, including Graph, CAS, Solver, Notebook, and Calculator.

## Phase 3 - Quality gates (implemented for the shared layer)

- Unit-test cursor placement, completion, explicit syntax repair, function parameters,
  typo recovery, and workspace examples.
- Compile the complete Android Kotlin source set to catch Compose integration issues.
- Keep existing adaptive-keyboard and smart-input regression tests in the gate.

## Next iteration

- Learn local recency rankings from accepted suggestions without storing raw questions.
- Add parser-derived diagnostics for invalid arity and domain-sensitive warnings.
- Add symbol-table suggestions from notebook variables, graph sliders, and named geometry objects.
- Add TalkBack announcements for suggestion changes without interrupting continuous typing.
- Add instrumented phone/tablet tests for long labels, large text, and landscape mode.
- Measure acceptance, dismissal, repair success, and time-to-valid-expression locally.

## Acceptance criteria

- Suggestions appear in one existing keyboard row with no keyboard height increase.
- A completion never replaces text outside the active identifier.
- A repair is never applied without a user tap.
- Function hints identify the active parameter inside nested calls.
- Empty inputs show examples relevant to the active workspace.
- Existing keyboard pages and structural controls remain reachable.
- Focused unit tests and `:app:compileDebugKotlin` pass.
