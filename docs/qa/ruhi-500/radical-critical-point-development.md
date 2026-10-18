# Real radical critical and stationary analysis: retained development failures

Current compiled source-hashed probe: 4/8 verified. Uses the original critical/stationary language patterns of rows 344/336 with explicit development expressions; original workbook rows, parameters and expectations are unchanged.

Independent real identities:

- sqrt(x^2)=abs(x): finite critical corner (0,0), unequal one-sided slopes -1/+1, not stationary. Critical corner currently missing.
- sqrt((x-1.25)^2)+2*x: finite non-extremum critical point (1.25,2.5), one-sided slopes +1/+3. Currently missing.
- sqrt(x^4)=x^2: smooth stationary and critical point (0,0). Both modes currently miss it because an unsimplified derivative can be undefined there.

Passing controls: literal abs(x) critical origin, sqrt(x^2) has no stationary point, cancellation sqrt(x^2)-sqrt(x^2)+x has no critical points, reciprocal 1/sqrt(x^2) excludes zero and has no critical points. A missing list entry is not an acceptable substitute for computing the requested analysis.

Next: extend domain-preserving local analytic branch analysis beyond literal abs spelling. Even-power square-root compositions must respect real identities and original domains, including shifted/scaled/composed expressions and cancellation. Do not use numerical smallness to turn tiny slopes into stationary points. Add meaningful independent JVM cases, retain current failures, verify live saved curves/undo/native markers and rerun all 500 before training the next model cycle. Broader fractional, restricted and nonisolated critical domains remain separate incomplete requirements.

Evidence: radical-critical-point-audit.json and cycle-74-radical-critical-point-audit.log. Original source fields and expected mathematics are preserved. This is compiled parser/engine development evidence, not Android TensorFlow or screenshot certification. Cycle-74 app source remains frozen while its full native run is live.

## Cycle 75 implementation/development

Cycle 74 terminally passed (610 JVM methods, 50 native methods); its four radical counterexamples remained explicit. Eight new radical JVM methods then ran against old production: seven failed, one domain control passed; no compile errors. Retained radical-critical-before-fix.log and .xml. Two additional independent CAS numeric tests were added after diagnosing fractional powers: x^(1/2) rendered as x^1/2 and evaluated to 0.125 rather than 0.5 at x=0.25; sqrt(x) derivative rendered as 1/2*x^-1/2 and evaluated to 0.25 rather than 0.5 at x=1. Both failures are retained in radical-cas-render-before-fix.log/.xml. The third failure in that batch localized the shifted flat-root issue.

Implementation normalizes real even-power square-root and half-power compositions into local absolute-value branches, including positive constants/products and negative even powers. Actual original values, restrictions and finite two-sided domains stay authoritative. Branch zero equations unwrap powers/products to isolate true parent roots instead of seeking only flat power zeros. The shared CAS renderer now groups fractional numbers when used as powers/bases. Branch slope evaluation keeps structural shifted factors to avoid false zeros from expanded polynomial cancellation. Multiple-root candidate refinement uses normalized finite differences and a bounded multiplicity correction; the existing 1e-6 point uniqueness tolerance was not widened.

Development failures are retained separately: the first branch implementation left two failing methods; after formatter/root-isolation fixes one flat-root method still returned a spurious near-zero point; a first refinement exposed expanded-derivative cancellation near sqrt((x-0.375)^8). Those were not relabeled as passes. The final ten-method radical class plus existing critical/derivative/absolute-value and five shared CAS suites passes all 70 methods. Tests include independent rational-power values and elementary radical derivatives, shifted/tiny coefficients, powers 4/6/8, half-power spelling, products, cancellation/poles, original domains, near-corner and factored flat-root slopes, save/history/live formulas.

Original critical probe verifies 6/6, radical probe verifies 8/8; both now hash five production sources including core/SymbolicCas.kt. The 1,200 original solver/CAS corpus also passes without changing its labels or expected values. APK/native focused check and full cycle 75 remain pending here; no Android or full-goal claim follows from these compiled results. Full cycle 75 gates all 500 before TensorFlow training, expanded JVM/shared-CAS/solver tests and exactly 51 native methods. Broader fractional, restricted, nonisolated and clustered-root/domain analyses remain incomplete.


Focused native test passed in 34.341 seconds, including all eight original-pattern radical tuples and the live current-curve edit/reload to (2,4). Fresh focused edited-corner and answer captures opened and inspected: the cyan corner and D1 marker agree with (2,4), and the Applied answer reports it. D1 and the existing trace label overlap slightly; this is not full annotation-layout certification. Critical-point recovery is distinct from derivative-curve sampling: the separate derivative-helper probe found four finite removable-hole failures (4/8 total), retained for the next cycle with no workbook changes.


Full cycle 75 passes: 664 JVM methods, unchanged 1,200 solver records, 51 native methods in 1,047.133 seconds, all model/APK/source-field guards. Fresh final captures reviewed. The four critical/stationary counterexamples are resolved; four separate derivative-helper holes remain and are not counted as fixed.
