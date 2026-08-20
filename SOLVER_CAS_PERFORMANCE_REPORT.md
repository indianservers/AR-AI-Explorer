# Solver/CAS Performance Report

Date: 2026-08-20

| Check | Status | Evidence |
|---|---|---|
| Solver work off UI thread | PASS (source/unit) | ViewModel uses `Dispatchers.Default`; existing performance tests pass. |
| Progress/cancellation plumbing | PASS (source/unit) | Paths exist and unit suite passes. |
| 60 consecutive live solves | PASS | Device batch completed in 159.573 s without crash; not a latency-percentile benchmark. |
| Time/memory/complexity limits | NOT RUN | No complete adversarial benchmark or memory profile. |
| Stale-result cancellation/debounce | NOT RUN | Dedicated rapid-edit instrumentation not completed. |
| Mid-range physical device | NOT RUN | Only `emulator-5554` exercised. |
| Lifecycle/process-death under load | NOT RUN | Failure injection not performed. |

Performance gate: **FAIL** because physical-device, trace, memory, percentile, and lifecycle evidence is incomplete.
