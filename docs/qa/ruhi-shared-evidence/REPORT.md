# Ruhi shared architecture — recorded implementation and verification

Scope: extend the existing AIExplorer app. The attached blueprints are reference material; no UI redesign, lesson rewrite, full 250-feature implementation, or Filament migration is claimed.

## Execution contract

Voice transcripts or typed text pass through deterministic mathematical-language normalization, TensorFlow intent and engine proposals, typed parsing/validation, current-scene target resolution, module-scoped history, and the existing mathematical engines/renderers. Models do not generate pixels, mesh coordinates, formulas, derivatives, or measurement values.

`RuhiCommand` schema version 1 carries an engine, sealed operation, source modality, target reference, intent/engine confidence proposals and routing reason. Graph operations reuse `GraphAssistant`. Geometry adapters modify the same points, dependencies, shapes and solids that native controls use. Rendering is derived from scene state.

Explicit studio requests and compatible active-scene context govern routing. An incompatible solid/equation may switch studios only when its engine proposal meets 0.90 confidence and a 0.40 top-two margin. Otherwise Ruhi asks for clarification. Whitelisted intent paraphrases use a separate 0.85/0.35 gate; deterministic argument validation remains mandatory.

## Recorded verification

Final delivery emulator recheck did not complete: startup ANRs, stalled capture and external test-storage permission failures prevented a fresh result. Zero methods in device-run.log means unverified, not a pass. previous-combined-device-run.log records an earlier successful seven-method run; cleanup-smoke-device-run.log records the subsequent four shared-engine methods passing, including native Clear/Undo/deletion assertions. Local canvas JSON/images are retained from the earlier retrieval; they are not evidence of a completed final rerun. The display-crop helper compiled but remains unverified on-device.

- JUnit regressions: 61; failures/errors: 0.
- Device instrumentation methods passed in saved log: 0. See device-run.log; a missing success summary is not treated as a pass.

| Native studio | Changed canvas pixels | Recorded result |
|---|---:|---|
| GRAPH_2D | 46698 | PASS |
| GRAPH_3D | 22592 | PASS |
| GEOMETRY_2D | 394523 | PASS |
| GEOMETRY_3D | 376141 | PASS |

- Real Android engine-model inference: PASS; 4 labeled engine probes.
- Rotating cone capture: 26614 changed pixels; touch undo resumes animation: True.
- Native Clear/Undo and deletion metadata assertions passed in cleanup-smoke-device-run.log; the corresponding refreshed JSON could not be retrieved during the final emulator failure.
- Visible-prompt conversation: spoken-form text y equals x squared → red → shift three units right → calculated/pinned vertex (3,0). The native test verifies equation samples, context, response text and the real canvas. This tests speech-style text/transcripts, not microphone recognition quality.
- Both original rendering stress cases and the full 150-command replay are recorded separately in ../ruhi-graph-evidence/REPORT.md.

## Measured engine-model performance

- Unique authored training sentences: 390.
- Validation: 61/75 = 81.33%; macro-F1 0.7414.
- Held-out test: 158/189 = 83.60%; macro-F1 0.7502.
- Accepted at the routing gate: 72/189; accepted classification errors: 0. This small synthetic benchmark does not guarantee correctness on new utterances.
- Splits are disjoint utterance-template groups, including their polite variants; engine concepts and mathematical slots are shared. Validation guided class balancing. Neither template-group separation nor native smoke tests establish general natural-language understanding.
- Accuracy is below the reference document's 98% target. The current increment relies on explicit context, strict grammars, conservative confidence gates and clarification; it does not claim that target is achieved.
- The earlier intent model has its separate 17-sentence in-domain evaluation and narrower split caveats in ../ruhi-graph-evidence/nlp-evaluation.json.

## Deterministic operations and state safety

- 2D graph: existing equation operations, selected explicit-curve translation, quadratic-degree validation, coefficient-derived vertex and a visible pin in the existing canvas.
- 3D graph: existing verified graph actions, meshes, transforms, groups, appearance and history remain available.
- 2D geometry adapter: named points, segments, dependent midpoints, distance, circles/squares/rectangles/basic triangles, selected transforms, color, area/perimeter and explanation. Locked/constrained/dependent edits are guarded rather than silently bypassed.
- 3D geometry adapter: validated cube/cuboid/sphere/cylinder/cone/square-pyramid parameters, actual primitive meshes, volume/surface area, selected transforms, deletion and explanation. Numeric results use existing Geometry3D formulas.
- Angular motion is a deterministic pose function of the monotonic clock and angular velocity. The default rotating cone uses Y at 30 degrees/second. Frame poses do not add history entries. Native touch freezes the current pose within one undoable gesture; undo restores the motion configuration.
- The existing native 3D geometry convention uses Y-up; the graph-surface convention uses Z-up. Command destinations retain each engine's existing coordinate convention.
- Scene data persists with the existing workspace codec. Point-array compaction remaps named references; solid removal is blocked if indexed external dependencies could be retargeted. Undo/redo restores only changed scene fields and metadata. Native Clear cleans the active studio's Ruhi context/motion and supports restoration.
- Prepared edits run off the UI thread; scene, selection or studio-visibility changes prevent stale commits.

## Remaining scope and limitations

- This is a shared-architecture increment, not implementation or certification of all 250 reference commands.
- Engine classification remains limited by the small authored dataset and the measured metrics above. Most mathematical/entity extraction is deterministic; there is no unrestricted LLM or learned formula generator.
- The current 3D renderers remain native Compose projections. Filament/GPU scene-renderer migration and physical GPU/device certification are separate work.
- Existing speech UI/services are unchanged. The shared API accepts reviewed voice transcripts; no new microphone/TTS controls were added, and raw microphone capture was not verified in this emulator run.
- The geometry adapter covers a bounded subset of the existing studios. Many advanced constructions, solid appearance commands, nets, intersection volumes and arbitrary geometry-language variants remain outside it; existing native tools are preserved.
- Graph analytical tools retain local-equation conventions for transformed surface layers. Solid references in commands are snapshot-bound indices; arbitrary external replay of stale envelopes is not exposed.
- Animation resumes from its stored monotonic-clock configuration within the device boot; clock reset/reboot phase continuity is not certified.

## Artifacts and reproduction

- Build: scripts/run_ruhi_graph_audit.ps1 -DeviceSerial emulator-5556. This runs the regression filters and both device test classes.
- Training: install scripts/ruhi-nlp-requirements.txt in a Python 3.12 environment; run scripts/train_ruhi_graph_nlp.py and scripts/train_ruhi_engine_nlp.py.
- Models/vocabularies: app/src/main/assets/ruhi_nlp and app/src/main/assets/ruhi_engine_nlp.
- Dataset/evaluation: engine-dataset.jsonl and engine-evaluation.json.
- APK: app/build/outputs/apk/debug/app-debug.apk; instrumentation APK: app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk.
- Debug-only developer preview: adb shell am start -n com.indianservers.aiexplorer/.GraphVerificationActivity --es verify_ux_workspace geometry3d --es verify_ruhi_command "Ruhi, create a rotating 3D cone.". This activity uses ephemeral verification state and is excluded from release builds.
- No student conversation data or audit correction records are used for training. No automatic retraining or shared-training promotion is introduced.
