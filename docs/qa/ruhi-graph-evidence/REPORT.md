# Ruhi — native 2D and 3D Graph audit
Current intent model update: see [version 2 command training](../ruhi-nlp-command-training/REPORT.md). The historical rendered replay and its hashes below are retained; this model update does not recertify those pixels.


Recorded on 2026-10-16T15:56:00+05:30

This is the Android/Kotlin AIExplorer application. This uses native meshes and a bundled TensorFlow-trained intent classifier with LiteRT; it does not use Three.js. Results describe graph layers and native CPU-projected canvas pixels, not WebGL/GPU certification. Unsupported results are capability gaps, not passes.

| Test Category | Total | Passed | Failed | Unsupported |
|---|---:|---:|---:|---:|
| Object Creation | 20 | 13 | 0 | 7 |
| Delete & Reset | 10 | 10 | 0 | 0 |
| Positioning | 15 | 15 | 0 | 0 |
| Scaling | 15 | 15 | 0 | 0 |
| Rotation | 15 | 12 | 0 | 3 |
| Appearance | 15 | 14 | 0 | 1 |
| 3D Graphing | 15 | 9 | 0 | 6 |
| Geometry Relations | 15 | 5 | 0 | 10 |
| Conversational AI | 15 | 14 | 0 | 1 |
| Error & Stress Tests | 15 | 13 | 0 | 2 |
| **Total** | **150** | **120** | **0** | **30** |

Rendering skipped: 0. Unrecorded IDs: none.

Scene/geometry checks verified: 120. Applied scene operations: 103. Safe invalid-input rejections: 8. Requests for missing numeric arguments: 9. State/mesh verification is tracked separately from rendering.

Regression JUnit tests recorded: 61; failures: 0.

- Supplemental 2D visible-prompt test: PASS; changed canvas pixels: 46698.
- Device configuration and exact build/model hashes are recorded in device-config.json and source-manifest.json. Previous timeout evidence is archived under baseline-2026-10-15.
- TensorFlow exported-model intent evaluation: 17/17 held-out sentences. Related templates remain in training; this does not establish unrestricted language understanding.
## Evidence provenance

The 120-pass/30-unsupported pixel results below are the retained successful full replay, not a claim that the final delivery emulator recheck completed. The final recheck encountered intermittent capture skips and then emulator startup ANRs/storage permission failures. See intermittent-capture-before-redraw and device-full-run.log. Current source/model hashes and 61 passing JVM regressions are recorded separately. The new native display-crop capture helper compiled, but its complete device replay remains unverified.

- Supported-case parser checks: 103/103. This excludes unsupported sentences and is not an assertion of general English accuracy.
- Supported execution checks: 103/103.
- Render assertions: 150/150; includes unchanged-canvas checks on rejected instructions. Pixel comparison uses the central plot area to exclude overlapping chrome/status text. The 150-case audit captures the Android display and crops to measured canvas bounds, avoiding Compose PixelCopy redraw timeouts; missing captures remain rendering skipped.
- Supported context checks: 15/15.
- Mean command execution: 33.86 ms; maximum: 678.62 ms. Mesh checks and screenshots occur separately.

## What changed

- Connected the existing visible assistant to structured native Graph actions, preserving lesson/search/solver routing for educational queries and other workspaces.
- Added equation plotting, supported solid surface equations, bounded counts, selection, deletion, duplication with a visible offset, material/color/opacity/visibility, grid control, and shared Undo/Redo. Graph command history preserves unrelated fields; selections are separate for 2D and 3D, and failed multi-step history operations leave both stacks unchanged.
- Compound operations stage immutable snapshots; unsupported or invalid clauses leave the complete scene and history unchanged. Mixed Undo/Redo and mutation batches are rejected explicitly. Preparation runs off the UI thread; a changed workspace prevents a stale commit.
- Preserved holes at poles and undefined surface regions; only numerically convergent removable holes are filled. Sampling includes diagonal directions. The canvas and audit use the same geometry boundary.
- Fixed implicit quad triangulation, outward winding, exact-zero edges and duplicate grid-aligned faces. Independent plane coverage and area regressions capture the original failures.
- Preserved grid preferences on Clear, linked authoritative objects, and semantic references across native primary-layer ID changes.
- Added saved color appearance, material handling for indexed meshes, and a separate debug-only audit activity.
- Added persistent object translation, local scaling, orientation and graph groups, including group-centered and world-origin operations. Original sentences lacking numeric arguments request clarification; an explicit-angle compound group edit is tested separately.
- Added a bounded base-mesh cache and shared visible-scene sampling budget to reduce multi-layer rendering load without dropping layers or changing controls.
- Bundled a TensorFlow-trained intent model with confidence/margin gates and deterministic parameter validation. No student data or correction records are used for training.

## Limits

The original scope is not fully implemented. Numeric movement, scaling, rotations and grouping are implemented for 3D graph layers. Animation, point/line/vector constructions, many relationship operations, saved-state restoration and unrestricted conversational variants remain unsupported. Graph2D also supports numeric curve translations and quadratic vertex annotations. These 150 cases cover Graph workspaces; the separate shared geometry adapter supports additional native point/shape/solid operations and rotating solids. See [shared architecture report](../ruhi-shared-evidence/REPORT.md). Requests lacking numeric parameters ask for clarification. A passing rejection test establishes safe failure, not feature support. Random sphere placement is repeatable per command for reproducibility. Surface meshes approximate analytic surfaces at bounded resolution. Numerical removable-hole inference is not a symbolic proof of a multivariable limit. Pixel differences establish visible changes, not complete mathematical or visual correctness for every triangle. Symmetry rotations can correctly retain the silhouette while changing triangulation; independent coordinate and orientation tests cover them. Group membership and selection can leave pixels unchanged. Physical GPU performance is unverified.

The primitive convention is mathematical Z-up. Default cube side is 2; sphere parameter is radius; cylinder/cone/pyramid parameter is height. Explicit surfaces use their declared X domain on both grid axes; implicit surfaces use the stored X/Z and Y bounds. The native surface-layer schema does not currently expose three independent spatial domain intervals.

## Reproduction

Run `scripts/run_ruhi_graph_audit.ps1` from the repository root with a connected Android device/emulator. Use `-DeviceSerial emulator-5556` to select a device. Use `-CaseIds 89,144` or `-Category Appearance` to rerun selected device cases. Use `-StateOnly` for JVM state/geometry checks without claiming rendered passes. Open the debug dashboard with `adb shell am start -n com.indianservers.aiexplorer/.GraphAssistantAuditActivity`; it can rerun one case, a category, failures, or the full state/mesh suite. Dashboard mesh results never become rendered passes automatically.

## Failures

None in the recorded run.

## Unsupported commands

- 10. Create a tetrahedron.
- 12. Create a dodecahedron.
- 13. Create an icosahedron.
- 15. Draw a 3D triangle.
- 16. Create a line segment.
- 17. Plot a point at (2, 3, 4).
- 18. Create a vector from (0, 0, 0) to (3, 4, 5).
- 65. Spin the selected object continuously.
- 66. Stop the rotation.
- 73. Animate one complete rotation.
- 83. Apply different colors to all five cubes.
- 96. Draw a 3D helix.
- 99. Plot the parametric curve x = cos(t), y = sin(t), z = t for 0 ≤ t ≤ 10.
- 100. Plot 100 random points in 3D.
- 101. Display the X, Y and Z axes.
- 104. Label the coordinate axes.
- 105. Change the graph range to −10 through 10 on all three axes.
- 106. Place a sphere inside a cube.
- 107. Put a cube on top of a cylinder.
- 109. Arrange six spheres in a circle.
- 110. Create a pyramid using four points.
- 111. Connect two selected points with a line.
- 112. Draw a vector between the two points.
- 113. Find the midpoint of the selected line.
- 114. Measure the distance between two points.
- 115. Create a plane through three non-collinear points.
- 116. Find the intersection of two planes.
- 135. Restore the scene to its last saved state.
- 147. Repeat the previous command five times.
- 148. Create two cubes, delete one, then restore it.

## Clarification requested

- 41. Move the sphere along the X-axis. — Specify a numeric distance, scale, or rotation angle and axis.
- 48. Make the cylinder taller. — Specify a numeric distance, scale, or rotation angle and axis.
- 55. Stretch the cube along the Z-axis. — Specify a numeric distance, scale, or rotation angle and axis.
- 56. Make the pyramid smaller. — Specify a numeric distance, scale, or rotation angle and axis.
- 71. Rotate the selected object around its own center. — Specify the rotation angle and axis.
- 72. Rotate the cube around the world origin. — Specify the rotation angle and axis.
- 75. Rotate the grouped objects together. — Specify the rotation angle and axis.
- 125. Make the last object bigger. — Specify a numeric distance, scale, or rotation angle and axis.
- 150. Create five cubes, group them, rotate the group, ungroup them, and delete only the first cube. — Specify the rotation angle and axis.
