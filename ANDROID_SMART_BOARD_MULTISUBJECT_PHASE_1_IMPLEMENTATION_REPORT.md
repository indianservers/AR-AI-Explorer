# Android Smart Board Multi-Subject Phase 1 Implementation Report

## 1. Executive summary

The existing Smart Board is now one unified subject-aware canvas for Auto Detect, Mathematics, Physics, Chemistry, English, and Biology. Phase 1 adds conservative local subject detection, observable evidence, confidence and confirmation handling, concept detection, a lazy capability/handler registry, one recognition orchestrator, typed Phase 1 elements, mixed-subject metadata, undoable classification, schema-v4 persistence, accessible UI controls, offline fallback, and content-free metrics. Existing Mathematics and Physics engines remain the computational owners; no new subject solver or separate Board was created.

## 2. Scope

Implemented Phase 1 only: subject selection/locking, automatic classification, concept candidates, subject-aware recognition routing, capability discovery, Chemistry/English/Biology recognition scaffolds, typed elements, mixed-subject support, relationships, persistence/migration, safe correction, local analytics boundary, and validation. Full Chemistry solving, English correction, Biology reasoning, and Physics expansion remain deferred.

## 3. Confirmation that only one Smart Board exists

There remains one `smartboard` package, navigation entry, `SmartBoardFeatureRoot`, `SmartBoardCanvasView`, `SmartBoardDocument`, repository, recognition review, toolbar, recent-Board list, image pipeline, intelligence panel, and persistence codec. The former subject-specific “new Board” menu items were replaced with one “New Smart Board” action. Subjects are handlers inside this same experience.

## 4. Existing architecture reviewed

Reviewed all four existing Smart Board implementation reports and the complete Smart Board model, canvas, history, recognition, Physics, integration, intelligence, tutor, media, export, persistence, navigation, ViewModel, UI, security, unit-test, and instrumented-test code. The wider audit covered CAS/solver/graph/statistics/geometry/2D/3D, Physics formula/unit/diagram experiences, Chemistry repositories/formula/unit packages, Biology catalogue/diagram/learning packages, ML Kit handwriting/photo recognition, curriculum, learner, assistant, speech/TTS, privacy, and analytics availability.

## 5. Existing subject engines discovered

- Mathematics: `SymbolicCasEngine`, `MathProblemSolver`, graph parser/workspaces, statistics adapter, geometry and 2D/3D routes, ML Kit handwriting, tutor, and verifier.
- Physics: Smart Board Physics formula catalogue, quantity/unit/dimensional systems, numerical handler, graph/vector/diagram routing, circuits/waves/optics experiences, photo recognition, and tutor.
- Chemistry: `BundledElementData`, `ElectronConfigurationEngine`, `BundledChemistryFormulaData`, `OfflineChemistryFormulaRepository`, `ChemistryFormulaCalculator`, `ChemistryUnitSystem`, Chemistry curriculum/learning content, and validators.
- English: existing ML Kit English digital-ink recognition, general learning-workspace OCR contract, Android speech recognition, and TTS. No production grammar, spelling, sentence-parsing, dictionary, pronunciation-coaching, or essay-analysis engine was found.
- Biology: `BundledBiologyCatalogue`, `OfflineBiologyRepository`, glossary/concepts, labelled `BiologyDiagram` assets, learning/assessment content, and planned 3D metadata. No production rough-sketch reconstruction or diagnostic diagram-reasoning engine was found.

## 6. Engine reuse matrix

| Subject | Capability | Existing module | Adapter needed | Current availability |
|---|---|---|---|---|
| Mathematics | CAS/solver | Existing symbolic CAS and solver | Existing Smart Board adapter retained | Available |
| Mathematics | Graph/statistics/geometry/2D/3D | Existing workspaces and adapters | Existing handoffs retained | Available |
| Mathematics | Handwriting | `CasHandwritingRecognizer` / ML Kit | Existing adapter retained | Available locally after model download |
| Physics | Formulae, units, dimensions, numerical work | Existing Smart Board Physics engine | Existing handler retained | Available |
| Physics | Graph/vector/circuit/wave/optics | Existing modules/routes | Existing handoffs retained | Available where previously supported |
| Chemistry | Periodic-table data | `BundledElementData` | Capability registration only in Phase 1 | Available for later lookup |
| Chemistry | Formula catalogue/calculator/units | Chemistry formula packages | Later action adapter | Existing, deferred from Phase 1 execution |
| Chemistry | Equation balancing/stoichiometry/reaction prediction | Not found as a reusable production engine | Future engine/adapter | Missing; never advertised |
| Chemistry | Notation recognition | Existing handwriting recognizer plus deterministic notation validator | Phase 1 adapter added | Available with confirmation |
| English | Handwriting-to-text | Existing ML Kit English digital ink | Phase 1 adapter added | Available locally after model download |
| English | Grammar/spell/POS/essay analysis | No production engine found | Future approved engine | Missing; never advertised |
| English | Speech/TTS | Android learning-workspace integrations | Later command adapter | Existing but deferred |
| Biology | Catalogue/glossary/lessons | Bundled catalogue and offline repository | Later lookup adapter | Existing |
| Biology | Labelled diagram assets | `BiologyDiagram` data | Later visual adapter | Existing assets |
| Biology | Rough-sketch reconstruction/3D anatomy | No ready runtime engine; 3D metadata is planned | Future engine | Missing/deferred |
| Shared | Curriculum/learner/intelligence | Existing curriculum and learner repositories | Context adapter extension point | Available, optional |
| Shared | Product analytics | No general production analytics client found | Typed local boundary added | Local/no-upload only |

## 7. Subject-selection architecture

`SmartBoardSubject` now contains `AUTO`, five subjects, and `GENERAL`. `SmartBoardSubjectMode` records selection, lock, whether the user chose it, and change time. The existing settings overlay provides one keyboard/TalkBack-operable selector and lock switch. Changing mode is an undoable command and preserves elements, selection, viewport, active tool, and confirmed classifications.

## 8. Auto Detect architecture

`DeterministicSmartBoardSubjectDetector` applies Board preference, observable local rules, terminology, formula/unit patterns, diagram metadata, and concept context. It never runs subject engines or AI merely to classify obvious content. `DefaultSmartBoardRecognitionOrchestrator` invokes one existing on-device recognizer, detects the subject, routes only high-confidence Auto content, and returns unresolved/confirmation states conservatively.

## 9. Subject evidence model

The model includes symbol pattern, recognized term, diagram type, formula match, unit match, language pattern, concept match, and user-context evidence. Candidate explanations expose these concise observable facts; they do not expose hidden chain-of-thought.

## 10. Confidence handling

Subject confidence is presented as High, Medium, Low, or Unresolved. High and clearly separated candidates can route in Auto. Medium/Low requires confirmation. Unresolved content remains intact and cannot be sent to an arbitrary handler. Exact detector percentages are not shown. A locked-mode mismatch produces a warning without changing the Board.

## 11. Concept detection

Local concepts include quadratic equations, integration/algebra, Newton’s second law/kinematics, chemical equations, English sentence/paragraph work, and cell biology/biological terminology. Each candidate records evidence and only real capability IDs. Confirmed recognition stores the selected concept against the inserted element.

## 12. Capability registry

`SmartBoardSubjectCapabilityRegistry` exposes installed subjects, real capabilities, lazy handlers, and provider IDs. Mathematics/Physics capabilities reflect their existing engines. Chemistry exposes notation, periodic table, formula catalogue, units, and lessons—but not balancing or stoichiometry. English exposes OCR/text review only. Biology exposes terminology, catalogue, and lessons only.

## 13. Handler registration

The existing Mathematics and Physics handlers are retained. `Phase1SubjectRecognitionHandler` instances provide strictly recognition/classification scaffolding for Chemistry, English, Biology, and General. Factories are lazy and cached once; `AUTO` has no handler. No dynamic class loading or universal subject engine exists.

## 14. Recognition orchestration

The single workflow captures selected strokes, renders the existing high-contrast input, calls the existing on-device handwriting provider once, applies subject detection, builds subject analysis metadata, presents candidates and concept, accepts correction, and inserts one typed element while retaining source strokes. Provider details remain behind contracts.

## 15. Mathematics preservation

Mathematics remains the default for legacy/new API calls that omit a subject. Its existing handler, recognition result, expression element, action analysis, CAS, graph/statistics/geometry handoffs, tutor, verifier, review button label, and success status were preserved. Auto mode does not rewrite historical Mathematics elements.

## 16. Physics preservation

The existing Physics handler, expression/diagram/result types, photo path, formula/unit/dimension/numerical engine, actions, tutor, visual handoffs, serialization, and success status remain intact. Physics may still reuse Mathematics computation while retaining Physics semantics.

## 17. Chemistry recognition foundation

Phase 1 recognizes and normalizes common element-symbol groups, formulae, Unicode subscripts, ions, reaction/reversible arrows, state-symbol evidence, chemical terms, and basic group names. It inserts `ChemistryExpressionElement` with raw/normalized notation, expression type, sources, and confirmed classification. It does not balance, solve stoichiometry, or predict reactions.

## 18. English recognition foundation

English uses the existing on-device `en-IN` handwriting provider and preserves recognized/corrected text, language code, word/sentence/paragraph/list/fill-blank type, line breaks, sources, confidence/classification, and original strokes. Ordinary sentence structure outranks algebra. No grammar correction or rewriting occurs.

## 19. Biology recognition foundation

Biology detects terminology, cell/genetics/taxonomy/process concepts, and supplied diagram/label metadata. `BiologyContentElement` keeps text, content type, unconfirmed label candidates, sources, and classification. Visual estimates remain candidates; no precise sketch reconstruction, anatomy diagnosis, or fake 3D result is produced.

## 20. Mixed-subject architecture

The Board subject is a recognition preference, while `elementSubjectClassifications` stores independent classifications. The canvas and accessible list support all typed elements together. Mixed selection exposes a spoken/text subject composition; incompatible existing math/Physics actions remain disabled naturally because they require a compatible single typed selection.

## 21. Subject relationships

Relationship types now include recognized-as, describes, labels, derived-from, solves, explains, represents, part-of-problem, part-of-diagram, uses-formula, uses-data, and cross-subject-context alongside legacy grouping/source links. Cross-subject ownership is explicit and never inferred solely from proximity.

## 22. Persistence changes

Document schema advanced from 3 to 4. The codec stores Board mode/lock/user-selection time, content classifications, alternate candidates, confirmed status, concepts/capability IDs, expanded relationships, and typed Chemistry/English/Biology records. Damaged/unknown records retain the existing recovery behavior.

## 23. Migration strategy

Schema 0–3 documents decode through backward-compatible optional header fields. Historical Mathematics and Physics subject metadata becomes the Board mode without reclassifying old elements. Unknown/general legacy metadata safely becomes Auto/General context. Existing element payloads and IDs are not rewritten.

## 24. Undo and redo

`ChangeBoardSubjectModeCommand` and `AssignSubjectClassificationCommand` cover mode selection, locking, and content correction. Recognition insertion atomically adds classification/concept metadata and removes it on undo. Source recognition data remains preserved.

## 25. Offline fallback

Detection rules, caching, manual selection, typed insertion, existing local engines, persistence, and undo work without AI. The existing ML Kit model may require first-use download; failure keeps strokes and manual subject selection usable. No Board feature is disabled by missing AI.

## 26. Security

Detection is selection-scoped and capped at 8,000 characters. Board text is never interpreted as an app/tool instruction. There is no arbitrary handler registration, code/formula execution, hidden upload, camera/microphone activation, sharing, or automatic subject conversion. Typed registries, fixed enums, existing image limits, confirmation, and Phase 4 tool allowlists remain in force. Prompt-injection tests verify no delete capability can be created.

## 27. Accessibility

The selector, lock, candidate buttons, correction flow, subject composition, and typed elements have text labels and semantics. Live announcements report detection, confirmation, and mode changes. Badges include text and do not depend on color. The workflow is operable by keyboard/switch controls without handwriting or speech.

## 28. Performance

Handlers/providers are lazy; no Chemistry/Biology/3D assets load at startup. Detection uses a bounded 128-entry SHA-256 fingerprint cache, caps input, records cache hit and latency bucket, and recognition jobs are cancelled when superseded. Existing stable-pause/coroutine behavior keeps work off the drawing path. Analytics never contains raw text, formulae, labels, images, or Board content.

## 29. Files added

- `app/src/main/java/com/indianservers/aiexplorer/smartboard/multisubject/SmartBoardMultiSubject.kt`
- `app/src/main/java/com/indianservers/aiexplorer/smartboard/multisubject/SmartBoardMultiSubjectAnalytics.kt`
- `app/src/test/java/com/indianservers/aiexplorer/smartboard/SmartBoardMultiSubjectPhase1Test.kt`
- `app/src/androidTest/java/com/indianservers/aiexplorer/smartboard/SmartBoardMultiSubjectPhase1UiTest.kt`
- `ANDROID_SMART_BOARD_MULTISUBJECT_PHASE_1_IMPLEMENTATION_REPORT.md`

## 30. Files modified

- `smartboard/models/SmartBoardModels.kt`
- `smartboard/domain/SmartBoardHistory.kt`
- `smartboard/persistence/SmartBoardPersistence.kt`
- `smartboard/canvas/SmartBoardCanvasView.kt`
- `smartboard/export/SmartBoardExporter.kt`
- `smartboard/intelligence/SmartBoardContextAndRules.kt`
- `smartboard/presentation/SmartBoardViewModel.kt`
- `smartboard/presentation/SmartBoardScreen.kt`

No production file outside the Smart Board package was modified for this phase.

## 31. Unit tests

`SmartBoardMultiSubjectPhase1Test` contains **27 passing tests** covering the unified enum/mode/lock, five deterministic subject cases, ambiguous tokens, locked mismatch, candidate evidence/ranking, concepts, Chemistry normalization, English/Biology classification, capabilities, lazy handlers, single-provider routing, unresolved/manual fallback, cache fingerprints, typed serialization, schema migration, undo/redo, mixed classifications, explicit relationships, prompt injection, and content-free bounded analytics.

## 32. UI tests

The new Compose UI suite verifies that the existing Smart Board exposes one selector with all six modes, can switch Chemistry/Biology without leaving the canvas, and can lock/unlock. Existing Phase 1 canvas/tool/edit/save tests continue to compile unchanged apart from preserved compatibility strings.

## 33. Instrumented tests

`.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon` succeeded. Instrumented-test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (2,377,545 bytes). Execution was unavailable because `adb` is not installed/on `PATH` and no device/emulator is connected.

## 34. Existing-module regression tests

`.\gradlew.bat :app:testDebugUnitTest :arengine:testDebugUnitTest --no-daemon` succeeded:

- App: **647 tests, 0 failures**, 82 suites
- AR engine: **26 tests, 0 failures**, 5 suites
- Total: **673 tests, 0 failures**

This includes prior Smart Board Mathematics, Physics, Phase 4 intelligence, graph/geometry/CAS, Chemistry/Biology application, and unrelated module unit suites.

## 35. Android lint result

`.\gradlew.bat :app:lintDebug --no-daemon` completed analysis and reported **5 errors, 25 warnings, 7 hints**. All five errors predate and are outside Smart Board:

- Three API-33 `BigInteger.TWO` usages in `core/ComputationalBreadthCas.kt:330`
- API-33 `InputStream.readNBytes` in `persistence/MathFileExchange.kt:61`
- API-35 `List.removeLast` resolution in `core/NextGenerationSpatialMathematics.kt:40`

No multi-subject Phase 1 file appears in the lint error set. Report: `app/build/reports/lint-results-debug.html`.

## 36. Debug-build result

`.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --no-daemon` succeeded. Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (129,722,759 bytes).

## 37. Release-build result

The same command built `app/build/outputs/apk/release/app-release-unsigned.apk` (114,851,592 bytes). Release signing/distribution was not requested.

## 38. Known limitations

- First-use ML Kit handwriting recognition may require a model download.
- No production English grammar/spell/POS/essay engine exists, so only recognition/review is exposed.
- No reusable production Chemistry balancing, stoichiometry, or reaction-prediction engine was found.
- Biology labelled assets/catalogue exist, but rough-sketch reconstruction and runtime 3D anatomy are not production-ready.
- Imported-image Biology classification can consume typed diagram/label metadata, but no fake visual classifier is used when that metadata is absent; users can classify selected images manually.
- The repository has no general production analytics client; events remain bounded, process-local, and no-upload behind an adapter contract.
- Real-device OCR, stylus, TalkBack, switch, foldable, and lifecycle execution requires Android hardware/emulator and `adb`.

## 39. Deferred Phase 2 capabilities

Add approved adapters for Chemistry periodic-table/formula lookup and unit calculators; English grammar/spell/POS services if a real engine is adopted; Biology catalogue/diagram lookup; richer concept search/recent curriculum selector; image-region OCR/diagram provider metadata; relationship confirmation UI; subject-group action sheets; and consent-aware AI classification only for unresolved cases.

## 40. Phase 3 integration points

The registry can later add verified Chemistry balancing/stoichiometry, English feedback, Biology diagram verification/genetics, and richer cross-subject workflows without changing the canvas or document identity. Subject intelligence handlers can plug into the existing Phase 4 orchestrator once deterministic capabilities and verification gates exist. Future visual/3D routes must continue using interactive engines rather than static answer replacements.

## Validation commands

| Command | Outcome |
|---|---|
| `.\gradlew.bat :app:compileDebugKotlin --no-daemon` | Passed |
| `.\gradlew.bat :app:testDebugUnitTest --tests '*SmartBoardMultiSubjectPhase1Test' --no-daemon` | Passed, 27/27 |
| `.\gradlew.bat :app:testDebugUnitTest :arengine:testDebugUnitTest --no-daemon` | Passed, 673/673 total |
| `.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --no-daemon` | Passed |
| `.\gradlew.bat :app:lintDebug --no-daemon` | Analysis completed; blocked by five pre-existing non-Smart-Board API errors |
| Formatter discovery (`gradlew tasks --all`) | No Spotless, ktlint, or repository formatting task is configured |
| `adb devices` | Not runnable because `adb` is not on `PATH` |
