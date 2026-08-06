# Solver Future Recognized-Text Integration Guide

## Current State

No image or recognition flow is connected to Solver. Phase 4 adds only:

```text
ExternalMathInput.RecognizedText
        |
SolverInputAdapter
        |
Keyboard-compatible normalization
        |
Existing Solver pipeline
```

## Future Integration Rules

1. Keep recognition in its existing owning module.
2. Pass only recognized text across the adapter boundary.
3. Display the normalized expression for user review before solving.
4. Preserve source spans or provide a mapping if recognition confidence is shown.
5. Require explicit user confirmation for ambiguous notation.
6. Route the confirmed text through the existing parser, classifier, strategies,
   explanations, verification, hints, tutor, and practice engines.
7. Never bypass verification because input originated outside the keyboard.

## Prohibited Changes During This Phase

No recognition module modifications, image libraries, image permissions, Solver
image buttons, or direct image routing were added.

