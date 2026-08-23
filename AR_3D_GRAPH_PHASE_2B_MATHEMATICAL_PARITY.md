# AR 3D Graph Phase 2B — Mathematical Parity

Result: **PASS for all engine/adapter/render-data comparisons executed**

## Compared path

Each case was generated through the existing 3D engine directly, then through `Existing3DGraphEngineBridge`, then through the AR representation adapter. Tests compare parse/canonical identity, domain, density, rows/columns, exact vertex coordinates, exact index order, normals derived from identical topology, palette/opacity, bounds, invalid-region handling and ordered multi-equation identity. The adapter does not parse or evaluate equations.

## Equation coverage

All 16 required explicit surfaces passed:

1. `z=x^2+y^2`
2. `z=x^2-y^2`
3. `z=sin(x)+cos(y)`
4. `z=sin(sqrt(x^2+y^2))`
5. `z=exp(-(x^2+y^2))`
6. `z=sin(x*y)`
7. `z=cos(x^2+y^2)`
8. `z=(x^2-y^2)/(x^2+y^2+1)`
9. `z=x+2*y`
10. `z=x^2+y`
11. `z=sin(x)+0.5*y`
12. `z=2*x-3*y`
13. `z=1/(x^2+y^2)`
14. `z=sqrt(x-y)`
15. `z=log(x+y)`
16. `z=tan(x)`

An implicit sphere and parametric torus also passed exact native vertex/index comparisons. Multiple surfaces preserved order and IDs. Custom domain `-7..9`, density 12, colour selection, opacity 0.35, axes, grid and aggregate bounds passed. Parametric curves are not an existing surface-workspace output type and were not invented.

## Input and concurrency coverage

Empty and whitespace input, invalid syntax, unknown variables, unbalanced parentheses, unsupported functions, division/non-finite output, very large/small values, repeated Plot taps, equation changes, Clear while generating and leaving while generating were tested. Stale generation results cannot publish after Clear or screen exit.

## Placement/orientation mathematics

Nine canonical tap locations and 100 deterministic random locations produced finite normalized world rays. For each valid ray:

`placement = cameraPosition + normalizedDirection × 1.5 m`

Measured distance was within test tolerance of exactly 1.5 m for corners, edges, portrait/landscape aspect ratios, translated cameras and pitched/rotated views. Inset-local viewport coordinates were honored. Invalid viewport/matrix inputs return controlled errors. Camera-facing transforms remain in the AR layer; engine vertices and mathematical axes are not swapped or negated.

Live camera visual confirmation of mirroring, handedness and projected labels remains blocked without ARCore.
