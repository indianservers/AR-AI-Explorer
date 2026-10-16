# Ruhi conversations in the existing studios

Ruhi now has a stateful dialogue manager above the four existing mathematical adapters. Lessons retain their original routing. The existing studio assistant gains contextual answer chips, compact recent-message bubbles, restored pending requests and a speech-input launcher. The full-screen dimming interceptor is disabled only in visualization studios, so the exposed canvas remains available while the panel is open.

## Decision boundary

`RuhiDialogueManager` normalizes text or a recognized voice transcript, reads bounded conversation memory, obtains a TensorFlow dialogue proposal, resolves IDs against the current scene, fills deterministic slots and either executes, asks one question, explains, cancels/resumes, or reports an unsupported operation. `RuhiStudioAssistant`, native mathematical models and scoped workspace commands perform scene edits. Compound multi-object edits stage privately and commit once. A failed member leaves the entire batch unchanged.

Model probabilities never authorize an operation by themselves. The dialogue model has 11 advisory labels. Confident cancel/resume paraphrases are restricted to reviewed grammars; mathematical entities and dimensions are parsed deterministically. The model does not generate coordinates, formulas, meshes or pixels. It is a small English feature model, not an unrestricted conversational language model or a learned mathematical entity extractor.

## Defaults and required slots

| Request | Deterministic behavior |
|---|---|
| Draw a graph | Ask for the function; equation chips are actionable |
| Compare two functions | Ask for both; “sin and cos” plots both immediately |
| Draw a circle | Radius 1; explicit radius is used exactly |
| Draw a square / triangle | Side 2; triangle defaults to equilateral |
| Draw a rectangle | 3 by 2 |
| Create a cube | Side 2 |
| Create a cylinder / cone | Radius 1, height 2; either explicit parameter preserves the other default |
| Create a square pyramid | Base side 2, height 2 |
| Rotate it | Standard 45° about Z in 2D/Graph3D, Y in native Geometry3D |
| Make it bigger / smaller | Multiply dimensions by 1.25 / 0.8 |
| Make it taller | Increase native solid height by 1 unit, within the dimension budget |
| Move it | Ask for distance and direction; retain the selected target IDs |
| Make it red, ambiguous scene | Ask using actual object names; “Both” stages an atomic update |

Native Geometry3D is Y-up; Graph3D retains Z-up. Invalid/nonfinite dimensions and excessive values are rejected. Unsupported tools remain unsupported rather than being fabricated by NLP.

## Memory, targets and recovery

Versioned, bounded binary/Base64 memory lives in the existing workspace lab metadata and round-trips through `WorkspaceProjectCodec`. It stores workspace/studio, pending intent/source/target IDs, scene fingerprint, last created and modified IDs, last expression, repeatable size edit and the latest 24 user/reply pairs. It is distinct from native selection and scene undo history. Dialogue-only messages do not add undo steps. Recent bubbles and pending chips can be restored when reopening the panel.

Existing selected IDs win; a valid recent reference or unique object can resolve a pronoun. Supported references include ordinals, previous/last object, both/all, a red curve, larger circles and left-side objects. Pending target or movement answers are rejected after an intervening scene change rather than retargeting a reused index. Geometry solids still use guarded snapshot-bound indices; this is not a migration of the native solid schema to permanent UUIDs.

Pending graph/comparison requests survive a supported quadratic explanation and unrelated legacy lesson queries. Invalid function answers retain the pending request. Cancel clears the request without deleting geometry; Resume restores its prompt/options. The selected cylinder's height/radius can be corrected independently; triangle type/side edits preserve its identity. “Make another one” creates a separate, offset copy. Appearance metadata is compacted with native solid deletion and cleared with the scene.

## Computation and evidence

Roots/intersections use the existing robust numerical graph engine, with finite residual-checked results in the current linear X viewport (`centerX ± 7/zoom`). Marker records are invalidated when expressions change. Triangle angles come from control-point vectors; cylinder/cone/pyramid volumes use native deterministic formulas. Exact symbolic completeness is not claimed for numerical root/intersection searches.

`scripts/train_ruhi_dialogue_nlp.py` generates 1,100 synthetic multi-turn scenarios and records expected module, object counts, parameters, pending state and status at each turn. The generated TSV is a test transport for the same JSONL, not a separate implementation oracle. JVM replays invoke the real dialogue/scene boundary and check numerical samples, dimensions, identity, appearance and undo. They do not claim rendered passes. Android instrumentation separately drives the visible input and chips, checks real scene mutations and compares actual display pixels.

See [the recorded report](ruhi-dialogue-evidence/REPORT.md) for test counts, model hashes, screenshots and measured limits. The 1,100 scenarios contain 5,350 turns but many shared linguistic templates; scenario count is not independent-language accuracy. The small held-out dialogue benchmark is reported separately. No student conversation data is used for training; runtime conversation memory stays in the workspace.

Speech uses Android's installed recognition activity. Availability and audio processing follow that recognizer; it is not a new on-device speech model. Automated tests exercise text and reviewed speech-style transcripts, not microphone accuracy. Arbitrary tutoring topics retain the existing coach/lesson path. Unreviewed constructions, reference phrasing, complex slot structures and existing unsupported engine operations are outside this bounded grammar.
