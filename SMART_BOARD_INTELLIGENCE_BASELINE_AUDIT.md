# Smart Board Intelligence Baseline Audit

Audit date: 24 July 2026  
Repository: `C:\Indian Servers\AIExplorer`  
Scope boundary: the existing unified Smart Board only.

## Executive finding

AI Explorer already has one substantial Smart Board implementation. It is not appropriate to create another module, canvas, subject board, recognition stack, persistence service, CAS, graph engine or tutor. The missing capability that blocks the requested completion criteria is a structured, confidence-aware auto-shape workflow.

The existing Board already provides the correct extension seams for the remaining work:

- one `SmartBoardDocument` with sealed element types;
- one vector `SmartBoardCanvasView`;
- command-based history;
- SQLite document/recovery persistence and DataStore preferences;
- app-private image assets;
- ML Kit digital-ink/photo recognition adapters;
- subject detection, concept metadata and capability registries;
- Mathematics/Physics action adapters and existing graph/geometry routes;
- unified intelligence and tutor orchestration;
- cancellable, delayed recognition in `SmartBoardViewModel`;
- PDF/PNG export and a structured accessible Board Outline.

The safest implementation is therefore an isolated shape-recognition package, one new structured element type, one undoable conversion command composed from existing commands, a small suggestion state in the existing ViewModel, canvas rendering/editing through existing selection/move/duplicate/delete operations, persistence migration, and focused tests.

## 1. Existing entry points and navigation

- App entry: `MainActivity.kt`.
- Smart Board launcher: `ExplorerViewModel.openSmartBoard()`.
- Feature root: `SmartBoardFeatureRoot` in `smartboard/presentation/SmartBoardScreen.kt`.
- Route identity: `SmartBoardRoute.path == "smart_board"`.
- Existing module return integration: callbacks from `SmartBoardFeatureRoot` open Graph 2D, Graph 3D, Geometry 2D, Geometry 3D and Physics, with `returnToSmartBoard` saved state.

There is one Board destination and one feature root. Subject selection stays inside the same Board.

## 2. Existing Smart Board components

| Concern | Existing implementation |
|---|---|
| Canvas | `canvas/SmartBoardCanvasView.kt` |
| Coordinates/selection | `canvas/SmartBoardGeometry.kt` |
| Models | `models/SmartBoardModels.kt`, subject result models |
| ViewModel | `presentation/SmartBoardViewModel.kt` |
| UI | `presentation/SmartBoardScreen.kt` |
| History | `domain/SmartBoardHistory.kt` |
| Persistence | `persistence/SmartBoardPersistence.kt` |
| Media | `media/SmartBoardMedia.kt` |
| Recognition | `recognition/SmartBoardRecognition.kt` |
| Subject orchestration | `multisubject/SmartBoardMultiSubject.kt` |
| Intelligence | `intelligence/*` |
| Tutor | `tutor/*` |
| Mathematics routing | `integration/SmartBoardMathIntegration.kt` |
| Physics | `physics/*` |
| Export | `export/SmartBoardExporter.kt` |
| Security | `security/SmartBoardSecurity.kt` |

There are no fragments, separate Smart Board activities, WorkManager workers or Room entities. The feature uses Compose around a custom Android `View`, an `AndroidViewModel`, SQLiteOpenHelper, DataStore and app-private files.

## 3. Canvas and stroke architecture

`StrokeElement` stores timestamped document-space points, pressure, tool, width, opacity, colour, bounds and creation time. The canvas:

- consumes historical motion points;
- distinguishes stylus/finger/mouse behavior;
- supports pressure-aware strokes;
- caches bounded `Path` objects;
- handles selection, erase, move, pan and pinch zoom;
- announces interactions accessibly;
- commits only completed strokes to the ViewModel.

The active stroke remains local to the View, so the full document is not persisted or copied on every pointer move.

## 4. Existing tools and document operations

Implemented tools include pen, pencil, highlighter, eraser, lasso, rectangle selection and pan. Existing operations cover:

- add/delete/move/duplicate;
- group/ungroup;
- reorder;
- clear;
- undo/redo;
- viewport/background;
- image import/rotate/crop;
- recognition-region add/move/resize/merge/split;
- subject/concept assignment;
- recognized results and relationships;
- export.

Pages and first-class layers are not implemented in the current document model. Adding them is not required for the core auto-shape completion and would be a separate backward-compatible project.

## 5. Existing recognition and OCR

- `MlKitMathRecognitionAdapter` wraps the existing `CasHandwritingRecognizer`.
- Mathematics and Physics handlers reuse the shared provider.
- Chemistry, English, Biology and General use Phase 1 typed recognition adapters.
- Image Physics recognition wraps the existing photo recognizer.
- Recognition preserves confidence, alternatives, source strokes and correction.
- Automatic recognition is delayed and cancellable in the ViewModel.

No recognizer is hard-wired into the Android canvas.

## 6. Existing region detection

`SmartBoardRegionOperations` supports add/move/resize/delete/reorder/merge/split. `SmartBoardRegionDetector` groups strokes by spatial row/proximity. Regions retain stable IDs, bounds, ordering and source element IDs.

The current region model does not persist processing state or full candidate payloads. That is acceptable for ephemeral auto-shape suggestions; accepted shapes should be document elements while rejected candidates should not inflate saved Boards.

## 7. Existing subject and concept intelligence

The subject enum contains Auto, Mathematics, Physics, Chemistry, English, Biology and General. The deterministic detector uses text/notation/context evidence and conservatively handles ambiguous tokens. Per-element classifications, candidates, confidence, user confirmation and concept metadata are persisted.

The capability registry lazy-loads subject handlers and exposes only real capabilities. Chemistry, English and Biology computational breadth is partial and must remain capability-gated.

## 8. Existing engines available for reuse

| Need | Reused component |
|---|---|
| Parse/CAS/solve/verify | existing core CAS through `SmartBoardCasAdapter` |
| Graph 2D/3D | existing `MathModule.Graph2D`/`Graph3D` navigation callbacks |
| Geometry 2D/3D | existing module navigation and `DynamicGeometryEngine` |
| Statistics | existing Smart Board math/statistics action |
| Physics quantities/units/dimensions | `physics/PhysicsSmartBoardEngine.kt` |
| Biology curriculum/models | bundled Biology catalogue and existing routes |
| Handwriting/photo | existing ML Kit/CAS recognition adapters |
| AR/3D | reusable `:arengine` consumed by `:app` |

Auto-shape fitting itself is not present. Existing dynamic geometry engines operate on already-structured constructions and are not rough-stroke recognizers.

## 9. AI, privacy, analytics and background work

Basic Board and deterministic intelligence are offline. Smart Board analytics are bounded and content-free. Tool invocations are allowlisted and require explicit user linkage/approval. No Smart Board WebView or arbitrary JavaScript path exists.

Recognition, media, persistence, CAS, tutor and intelligence work uses coroutine dispatchers. Stale recognition jobs are cancelled. There is no need to add a background worker for short-lived local shape fitting.

## 10. Storage and migrations

- SQLite database: Boards, recovery, intelligence sessions and tutor conversations.
- DataStore: input, accessibility and recognition/intelligence preferences.
- App-private files: bounded imported images and exports.
- Current document schema: 5.

A structured shape element requires schema 6, a dedicated record codec and migration compatibility. Old schemas must continue to decode with an empty shape set naturally.

## 11. Design system and accessibility

The Board uses existing Material 3/Board colours, compact tool buttons, panels and content descriptions. It has a structured Board Outline. Shape suggestions should reuse these components, avoid a permanent large toolbar expansion and expose accept/dismiss/alternate controls with text labels.

## 12. Existing tests

JVM suites cover geometry, history/persistence, recognition, integration, multi-subject classification, Physics, intelligence, tutor/security and final-audit remediations. Compose instrumented tests cover opening the Board and core subject/tutor UI. Before this work the full local total was 702 passing tests.

Missing tests are shape preprocessing/fitting, temporal/spatial grouping, confidence thresholds, structured-shape persistence, conversion history and suggestion behavior.

## 13. Permissions and feature flags

The app manifest has contextual CAMERA, INTERNET and RECORD_AUDIO permissions plus optional hardware declarations. Auto-shape recognition needs no new permission.

No general feature-flag infrastructure exists. The correct isolated alternative is a persisted Smart Board preference, defaulting conservatively, that disables shape recognition without disabling drawing.

## 14. Current implemented intelligence capabilities

- delayed/on-demand handwriting recognition;
- Mathematics notation and typed expressions;
- subject detection and override;
- concept metadata;
- mixed-subject ownership;
- contextual recommendations/actions;
- Mathematics and Physics engine routing;
- confidence/alternatives/correction;
- region management;
- deterministic tutor hints, next steps and verification;
- offline fallback and safe unavailable states.

## 15. Missing capabilities

High priority:

1. structured `ShapeElement`;
2. deterministic stroke preprocessing/fitting;
3. ranked shape candidates and confidence policy;
4. multi-stroke temporal/spatial grouping;
5. non-destructive suggestion lifecycle;
6. accept/dismiss/alternate/manual-recognize UI;
7. undoable stroke-to-shape conversion;
8. shape rendering, selection, move, duplicate, delete and style preservation;
9. shape persistence/export/accessibility;
10. a persisted auto-shape enable flag.

Deferred:

- full pages/layers;
- rotation/resize handles and rich fill editor;
- laboratory/molecule/organ symbol recognition;
- production graph-sketch reconstruction;
- multilingual text models;
- additional Chemistry/English/Biology computational engines;
- device-specific draw-and-hold gesture validation.

## 16. Integration opportunities

- Run deterministic shape fitting on `Dispatchers.Default`.
- Reuse the existing delayed recognition cadence and cancellation model.
- Use recent stroke timestamps and expanded bounds for grouping.
- Store suggestions only in ViewModel state.
- On acceptance, execute one composite history command that removes source strokes and inserts a shape plus `RECOGNIZED_FROM` relationship.
- Keep source strokes inside the command for undo.
- Reuse selection/move/duplicate/delete/reorder/group operations through a structured element translation branch.
- Rank actions through existing subject/intelligence context after conversion.

## 17. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Handwriting falsely converted | never auto-commit; confidence threshold and explicit accept |
| Every loop becomes a circle | closure, radial variance, corner and aspect tests; ranked alternatives |
| UI jank | background fitting, bounded points/candidates and cancellation |
| Data loss | original strokes retained in history command; suggestions ephemeral |
| Migration regression | additive schema record and tests for historical versions |
| Tool overcrowding | contextual suggestion card and one manual action |
| Subject-engine duplication | shape recognition outputs geometry only; existing routers remain owners |
| Dirty worktree collision | touch only Smart Board files and launcher tests when necessary |

## 18. Files likely to be modified

- `smartboard/models/SmartBoardModels.kt`
- `smartboard/domain/SmartBoardHistory.kt`
- `smartboard/canvas/SmartBoardCanvasView.kt`
- `smartboard/persistence/SmartBoardPersistence.kt`
- `smartboard/presentation/SmartBoardViewModel.kt`
- `smartboard/presentation/SmartBoardScreen.kt`
- `smartboard/export/SmartBoardExporter.kt`
- Smart Board JVM and Compose tests

New isolated files may be added under `smartboard/recognition` or `smartboard/shapes`.

## 19. Files and modules that must remain untouched

- unrelated subject hubs and screens;
- core CAS/graph/geometry/statistics engine outputs;
- `:arengine` public contracts;
- unrelated databases and migrations;
- global theme;
- package/application IDs;
- Gradle dependency versions;
- existing deep-link contracts;
- unrelated build outputs and user modifications.

## 20. Proposed implementation sequence

1. Add shape contracts, preprocessing and deterministic recognizer tests.
2. Add additive schema/persistence and history conversion tests.
3. Render structured shapes and reuse selection/move/duplicate/delete.
4. Add ViewModel suggestion/cancellation/accept/dismiss/manual-recognize workflow.
5. Add a compact accessible suggestion UI and persisted enable setting.
6. Integrate converted shapes into subject/action context and export.
7. Run focused tests, full JVM regression, Android-test compilation and packages.
8. Complete architecture, reuse, auto-shape, test and implementation documents.

## 21. Backward compatibility

Schema changes must be additive. Existing element records retain their formats. Schema 0–5 decoders remain supported. Auto-shape is optional and drawing works when disabled. No existing engine contract or navigation callback changes are required.

## 22. Test plan

Unit tests:

- normalization/simplification;
- line orientation;
- circle/ellipse;
- rectangle/square;
- triangle/right triangle;
- arrow/polygon/axes/angle;
- low-confidence handwriting-like rejection;
- temporal/spatial grouping;
- candidate ranking;
- shape codec and schema migration;
- accept/undo/redo conversion;
- move/duplicate/delete compatibility;
- subject/action context.

UI/source tests:

- suggestion card semantics;
- accept/dismiss/alternate;
- manual recognition;
- recognition preference;
- existing Board launcher and subject/tutor controls.

Regression:

- full `:app` and `:arengine` JVM suites;
- Android-test compilation/APK;
- debug/release APK and AAB;
- lint findings separated by affected package.

## 23. Performance considerations

- cap recognizer input points and use Douglas–Peucker simplification;
- evaluate a bounded recent stroke group;
- cache only accepted document structures, not preview bitmaps;
- cancel stale jobs on new ink;
- do not serialize a suggestion;
- avoid recognition on the UI thread;
- retain existing bounded path cache and autosave debounce;
- do not load subject engines for geometric fitting.

## Audit decision

Proceed with an isolated deterministic auto-shape foundation and workflow. Do not create a new module or broad new engine stack. Preserve current handwriting/subject/tutor systems and integrate through the existing document, history, ViewModel and UI boundaries.
