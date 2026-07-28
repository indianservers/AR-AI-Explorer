# Smart Board Intelligence Architecture

## Product invariant

AI Explorer has one Smart Board document, one canvas and one undo history. Intelligence augments that document; it does not create a second “AI board” or convert the experience into a static answer page.

## On-device interaction pipeline

```text
stylus / touch / keyboard / image
        |
        v
vector strokes and editable board elements
        |
        +--> delayed geometric stroke grouping
        |        |
        |        v
        |    deterministic shape candidates
        |        |
        |        v
        |    user preview: accept / alternative / keep ink / dismiss
        |
        +--> subject recognition and expression review
                 |
                 +--> local expression analysis
                 +--> bundled Physics formula catalogue
                 +--> existing CAS / Graph / Geometry / subject handlers
```

Recognition never silently destroys source ink. Accepted conversions are a single undoable command, retain `RECOGNIZED_FROM` relationships, and can restore hidden source strokes.

## Streaming multimodal Mathematics recognition

Automatic Mathematics recognition now maintains a candidate lattice after each committed stroke. Ordered digital ink and an in-memory high-contrast raster pass are fused, then reranked with shared-parser evidence and previous-result stability. Provider failure degrades to the available pass. Live candidates open the existing review workflow; they never directly replace source ink.

The benchmark layer records exact, semantic, symbol, top-three, calibration, latency and correction metrics using a versioned corpus contract. Recording APIs are explicit and bounded; handwriting is not collected implicitly.

## Core contracts

- `SmartBoardElement` remains the universal persisted canvas object.
- `ShapeElement` stores clean vector geometry, source stroke IDs, confidence, style, rotation and lock state.
- `AutoShapeRecognizer` returns ranked candidates; it does not mutate the document.
- `OfflineFormulaIdentifier` identifies canonical mathematics locally and delegates Physics matches to the existing bundled formula catalogue.
- `SmartBoardGraphAdapter` validates and routes graph expressions to the existing Graph engine.
- `SafeLatexPreview` is the validation boundary for handwritten, recognized and directly entered notation.
- `SmartBoardCommandHistory` owns every conversion and edit so reopened content behaves like newly created content.

## Confidence and user control

- Normal auto-suggestions require higher confidence than forced manual recognition.
- The suggestion surface shows the proposed shape, confidence and alternatives.
- “Keep original” is a first-class action.
- Formula identification is labelled offline and does not imply a proof or evaluation.
- Unsupported input remains editable; it is not fabricated into an answer.

## Persistence

Schema 6 adds structured shapes without invalidating earlier boards. Versions 0–5 migrate to the current schema. Graph configurations, LaTeX expressions, source links and styles remain editable after decode/open.

## Deliberate reuse boundaries

The Smart Board stores intent and configuration. Rendering and manipulation remain with the specialist engines:

- Graph handles, curves and tables: Graph engine
- 2D/3D construction: Geometry engines
- symbolic operations: CAS
- Physics recognition and formula metadata: Physics Smart Board engine/catalogue

This boundary avoids divergent parsers and preserves direct manipulation.
