# Solver Phase 1 Dependency Report

## Decision

No new dependency is added for Solver Phase 1.

## Reused platform and repository code

| Component | Purpose | Offline | Licence/binary impact |
|---|---|---:|---|
| Kotlin/JDK `BigInteger` and `BigDecimal` | exact rational arithmetic | Yes | platform; no APK dependency addition |
| Existing `ExactRational` | normalized fractions and exact operations | Yes | repository-owned |
| Existing `SymbolicCasEngine` | deterministic simplification, expansion, and supported factoring | Yes | repository-owned |
| Existing Compose/Material 3 | Solver UI | Yes | already bundled |
| Existing Android ViewModel | state and lifecycle | Yes | already bundled |
| Existing `SharedPreferences` | isolated Solver history | Yes | Android platform |
| Existing math keyboard | keyboard-only text entry | Yes | repository-owned |

## Rejected additions

- Remote CAS, AI, or web APIs: prohibited by the offline requirement.
- MathJax/KaTeX WebView: unnecessary for Phase 1 and adds runtime/size complexity.
- Third-party CAS: existing exact repository engines cover Phase 1 primitives; adding one without a full licence/size/thread-safety evaluation would duplicate capability.
- JSON/database library: Solver history is small and isolated; an encoded local record format avoids schema and dependency changes.

## Network safeguard

The Solver package contains no networking imports or clients. Tests scan Solver production sources for networking APIs and remote-service identifiers. The app manifest is not changed by this feature.

