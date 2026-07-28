# Smart Board Recognition — Phases 7–9

## Outcome

The Smart Board recognition pipeline now adds explainable Board context, opt-in local correction
learning, and production release safeguards. These phases do not convert AI Explorer into a
static answer engine:

- recognizers still return candidate expressions;
- context may only reorder those candidates;
- evidence is visible before insertion;
- handwriting remains editable and unchanged until confirmation;
- correction learning is local, bounded, optional, and clearable;
- model activation is blocked when benchmarks regress.

## Phase 7 — Explainable contextual reranking

`SmartBoardContextualRecognitionReranker` uses the existing candidate lattice and deterministic
Board context:

- active Board subject;
- recent expression structures;
- active variables;
- nearby text labels;
- parser verification;
- selected quality tier;
- optional local personalization.

Every score contribution is represented by `RecognitionRerankEvidence`. Recognition Review shows
why the leading candidate ranked first. Candidate text is immutable inside the reranker, so this
stage cannot generate unreviewed mathematics.

Quality tiers:

- **Fast** — provider ranking with minimal contextual work;
- **Balanced** — small verified-structure preference;
- **Accurate** — stronger verified-structure preference and an explicit penalty for unverified
  structures.

## Phase 8 — Privacy-preserving local personalization

Personalization is disabled by default. When enabled, confirmation records only:

- the recognized expression;
- the user-confirmed expression;
- a bounded count;
- the last-used timestamp.

It never stores stroke geometry, raster images, Board/document identifiers, surrounding Board
text, account identifiers, or raw recognition requests.

`RecognitionPersonalizationProfile`:

- is capped at 256 correction pairs;
- uses logarithmically bounded ranking influence;
- is persisted locally in DataStore;
- can be cleared from Board Settings;
- does not change a candidate unless the recognizer already returned that candidate.

## Phase 9 — Production quality gates and rollback

`SmartBoardRecognitionQualityGate` evaluates:

- top-three recall;
- confidence calibration;
- median and P95 latency;
- median correction actions;
- semantic-accuracy regression against the active baseline;
- representative-corpus size.

`RecognitionModelManifest` requires:

- a model ID and version;
- a SHA-256 package digest;
- minimum schema compatibility;
- supported input types;
- quality tier;
- verified-package state.

`SmartBoardRecognitionReleaseController` refuses unverified, incompatible, or benchmark-regressing
models. It retains the previous verified manifest for explicit rollback.

Optional content-free runtime diagnostics store only:

- input kind;
- latency bucket;
- confidence bucket;
- candidate count;
- selected rank;
- whether a correction was required;
- timestamp.

No recognized expression or Board content enters diagnostics. Runtime health recommends rollback
when slow-result or correction rates exceed bounded thresholds.

## User controls

Board Settings now provides:

- recognition quality tier;
- opt-in local correction learning;
- local correction count;
- clear-recognition-learning action;
- opt-in content-free diagnostics;
- runtime health summary;
- clear-diagnostics action.

Recognition Review displays contextual evidence and reminds the user that context only reorders
on-device candidates.

## Verification

Focused tests cover:

- explainable reranking without candidate invention;
- bounded personalization and codec round trips;
- regression-blocking quality gates and safe release activation;
- content-free runtime health and rollback recommendations.

Verification completed on 2026-07-27:

- debug app and unit-test Kotlin compilation: passed;
- focused Phase 7–9 tests: 4 passed;
- complete Smart Board unit-test package: 14 suites, 136 tests, zero failures, zero errors,
  zero skipped.
