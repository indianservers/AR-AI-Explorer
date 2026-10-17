# Critical-point development, cycle 68

The original workbook row 344 and its expected polynomial coordinates remain unchanged. The compiled six-case probe previously reproduced three omissions: abs(x), abs(x-2)+2*x and abs(x^2). These now return independently specified points.

Local absolute-value branches are resolved from the symbolic expression and current domain. Their analytic left/right slopes distinguish finite corners from stationary points, including a corner with no extremum. The original function/domain is authoritative; reciprocal poles are excluded. SymbolicCalculus formatting preserves small nonzero coefficients instead of rounding them to zero, and derivative-root scanning scales nonzero derivative values before isolation.

Seven JVM methods cover the original polynomial, tiny amplitudes, shifted/scaled corners, nested and canceled branches, rational corners/poles, selected-function edits, save/restore, annotation undo/redo and branch-aware slope queries. The 25-method derivative/explanation regression batch passed. An earlier batch had a source-326 derivative failure; the isolated diagnostic and two subsequent combined batches passed with unchanged derivative expectations. That failure is retained and its cause is not established; the full cycle must pass before reporting.

A proposed abs(x)/x empty-stationary expectation was corrected: this function is constant on either side of its excluded zero and has non-isolated stationary sets. abs(x)/x+x is the nonstationary pole control. Expectations were not changed to accept an incorrect mathematical result.

Focused native instrumentation passed one test in 12.859 seconds. Six independent elementary-calculus cases run through the installed Android assistant and bundled TensorFlow services. The visible selected curve starts at abs(x), is edited to abs(x-2)+2*x and saved/restored; drawing semantics and committed pixels show (2,4), and the visible current answer agrees. Files: critical-point-focused-device/critical-point-canvas.json and critical-point-answer.png.

Remaining scope: general domain boundaries, restricted endpoints, piecewise and fractional-power singularities, non-isolated stationary intervals, and numerical root-isolation limitations. These examples do not certify global critical-point enumeration. No UI layout or lesson change was made for this fix.
