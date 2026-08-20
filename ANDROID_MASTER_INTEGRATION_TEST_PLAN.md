# Android Master Integration Test Plan

Date: 2026-08-20

Allowed results: `PASS`, `FAIL`, `BLOCKED`, `NOT RUN`.

## Phase 1 — shared UI, navigation, isolation

| IDs | Cases | Planned |
|---|---|---:|
| INT-P1-NAV-001..006 | Launch, Home, each workspace, Home return, Android Back, gesture Back | 6 |
| INT-P1-SWITCH-001..020 | 2D Geometry → 3D Geometry → 2D Graph → 3D Graph → Solver → Home in one session | 20 |
| INT-P1-ISO-001..015 | Populate five workspaces; state, object, camera, units, angles, IDs, autosave and metadata isolation | 15 |
| INT-P1-ACT-001..050 | Undo, Redo, Clear All, Save, Import, Export, Share, Help, Settings, count across five workspaces | 50 |
| INT-P1-LIFE-001..003 | Rotation, activity recreation, process death restore selected workspace | 3 |

Exit: two complete clean runs; no cross-contamination or inconsistent core action.

## Phase 2 — mathematical handoffs

| IDs | Cases | Planned |
|---|---|---:|
| INT-P2-S2G2-001..010 | Solver to 2D Graph required expression/result types | 10 |
| INT-P2-S2G3-001..009 | Solver to 3D Graph required spatial types | 9 |
| INT-P2-GEO2S-001..010 | Geometry measurements/equations to Solver | 10 |
| INT-P2-G2S-001..008 | 2D Graph analyses to Solver | 8 |
| INT-P2-G3S-001..007 | 3D Graph analyses to Solver | 7 |
| INT-P2-COMPAT-001..004 | Compatible geometry/graph and return handoffs | 4 |
| INT-P2-META-001..015 | Type, variables, domain, units, precision, assumptions, styles and source metadata | 15 |
| INT-P2-FAIL-001..010 | Unsupported/stale/conflicting/circular/interrupted transfers | 10 |

Exit: two clean cycles; supported transfers preserve meaning and unsupported transfers fail safely.

## Phase 3 — files and sharing

| IDs | Cases | Planned |
|---|---|---:|
| INT-P3-TYPE-001..006 | Five workspace types and lesson package envelope | 6 |
| INT-P3-SAVE-001..050 | Ten save/reopen/version/metadata checks per workspace | 50 |
| INT-P3-DEVICE-001..010 | Screen/orientation/theme/locale/version compatibility | 10 |
| INT-P3-BAD-001..015 | Corrupt, hostile and oversized inputs | 15 |
| INT-P3-IMG-001..060 | Twelve image-export checks across five visual workspaces | 60 |
| INT-P3-SHARE-001..008 | Share sheet, image, editable file, package and failures | 8 |

Exit: two clean cycles; no mathematical/visual loss and invalid content rejected transactionally.

## Phase 4 — lifecycle, accessibility, devices, performance

| IDs | Cases | Planned |
|---|---|---:|
| INT-P4-LIFE-001..018 | Background, dialogs, rotation, resizing, split screen, death, update and interruption | 18 |
| INT-P4-A11Y-001..014 | TalkBack, keyboard, Switch Access, scaling, contrast, focus, semantics and announcements | 14 |
| INT-P4-DEVICE-001..015 | Required phone/tablet/foldable/OS/orientation matrix | 15 |
| INT-P4-PERF-001..013 | Launch/open/frame/touch/calc/mesh/memory/CPU/GPU/battery/thermal/save/export/crash | 13 |
| INT-P4-PROT-001..010 | Threading, cancellation, limits, adaptive resolution, transactions and recovery | 10 |

Exit: two clean cycles and acceptable physical mid-range performance/accessibility/recovery.

## Phase 5 — release simulation

| IDs | Cases | Planned |
|---|---|---:|
| INT-P5-JOURNEY-001..010 | Ten complete cross-workspace student lessons | 10 |
| INT-P5-UX-001..009 | School-child discoverability/recovery observations | 9 |
| INT-P5-AUTO-001..014 | Unit through installation/static/release suites | 14 |
| INT-P5-RC-001..016 | Release-build, signing, privacy, intents, offline and upgrade checks | 16 |
| INT-P5-CYCLE-001..002 | Consecutive complete release-candidate cycles | 2 |

Total planned cases: **437**. Any earlier phase failure prevents later-phase execution and approval.
