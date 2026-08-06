# Solver Phase 3 Known Limitations

## Mathematical scope

- Calculus support follows the existing verified elementary local kernels; it
  is not a general theorem prover.
- General one-sided limits, piecewise continuity, extrema classification,
  optimization and broad differential equations are not complete.
- Symbolic area-between-curves is not generalized in the Solver.
- Complex parsing focuses on explicit rectangular values and root commands.
- General complex functions, branch cuts and symbolic complex simplification
  are not implemented.

## Visual scope

- Visuals are 2D declarative explanations. Solver intentionally does not load
  the app's 3D engine.
- Coordinate graphs use bounded sampling and are explanatory; they do not prove
  global behavior outside the displayed domain.
- Integration rectangles approximate signed area. The symbolic or adaptive
  numerical verification remains authoritative.
- Text labels are presented beside the canvas rather than painted into every
  diagram.
- Formula visuals are attached only when the relevant formula is recognized.

## Interaction and testing

- Playback advances whole visual specifications rather than interpolating every
  algebra tile continuously.
- Instrumented UI tests compile but require an Android device or emulator to
  execute.
- Physical-device frame timing and low-memory testing remain outstanding.
- Screenshot golden testing is not configured in the repository; mathematical
  specification tests are used instead.

## Explicit exclusions

- Math Camera and image input are not connected.
- OCR and image-to-LaTeX are untouched.
- Existing graph, 2D and 3D workspace state is not reused or modified.
- No network API, runtime model or remote service is used.

