# Smart Board Recognition — Phases 1–3

Date: 2026-07-25

## Outcome

The existing Smart Board now has the first production layer for benchmark-driven, streaming, multimodal Mathematics recognition. It remains one editable vector board. Recognition suggestions never replace ink without confirmation.

## Phase 1 — Benchmark and measurement foundation

Implemented in `SmartBoardAdvancedRecognition.kt`:

- versioned `RecognitionBenchmarkCorpus` and `RecognitionBenchmarkCase` contracts;
- digital-ink, raster and fused input classifications;
- bounded, duplicate-safe `RecognitionBenchmarkRecorder` for consented samples;
- predictions with alternatives, confidence, latency and correction actions;
- exact LaTeX accuracy;
- semantic-expression accuracy;
- normalized symbol accuracy;
- top-three recall;
- Brier confidence-calibration score;
- median and p95 latency;
- median correction actions;
- explicit target thresholds.

The code is ready for a large representative corpus. No accuracy claim against Mathpix is made until that corpus contains varied real users, devices, subjects and difficult notation.

## Phase 2 — Streaming digital-ink recognition

When recognition mode is **Automatic** on Mathematics or Auto boards:

1. Each committed stroke cancels stale work.
2. Recent spatially related strokes form a bounded region.
3. Recognition begins after a 250 ms stability window.
4. The candidate lattice retains up to eight alternatives.
5. Previous-primary agreement produces a stability score.
6. Up to four candidates appear on the board with confidence.
7. Selecting a candidate opens the existing review flow.
8. Ink remains unchanged until final confirmation.

Correction gestures:

- fast scribble and strikethrough patterns are detected from vector timing and direction changes;
- affected strokes are previewed;
- the user chooses **Erase strokes** or **Keep as ink**;
- confirmed deletion remains undoable.

## Phase 3 — Stroke/image fusion

The two-pass engine reuses existing local providers:

- ML Kit digital-ink recognition receives ordered vector strokes, timing and writing area;
- the existing local image recognizer receives a high-contrast rendering of the same region;
- candidates are normalized through the strengthened LaTeX adapter;
- equivalent candidates are merged;
- provider agreement raises confidence;
- shared CAS/Graph parser validity contributes bounded evidence;
- previous stable output contributes bounded temporal evidence;
- provider failure degrades to the remaining local pass rather than losing the source ink.

Manual Mathematics recognition also applies the raster pass and parser reranking after subject routing.

## Safety and privacy

- Recognition runs through existing on-device providers.
- Raster input is generated in memory and is not persisted in the Board document.
- Benchmark recording is an explicit API; it does not silently collect handwriting.
- Candidate confidence is visible.
- Low-confidence output remains editable.
- Correction gestures require confirmation.
- All converted content retains source stroke relationships.

## Current limitation

These phases deliver the architecture, functional fusion and measurement system. Surpassing a mature OCR service in measured recognition accuracy still requires:

- a large consented training/evaluation corpus;
- specialist trained models for advanced notation;
- device-lab validation;
- repeated benchmark-driven model improvements.

The implementation creates the infrastructure needed to measure and pursue that result without making an unsupported accuracy claim.
