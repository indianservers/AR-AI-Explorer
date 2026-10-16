# Ruhi — native 2D and 3D Graph audit

Recorded on 2026-10-15T22:49:44+05:30

This is the Android/Kotlin AIExplorer application. No Three.js implementation or external model was used. Results describe graph layers and native CPU-projected canvas pixels, not WebGL/GPU certification. Unsupported results are capability gaps, not passes.

| Test Category | Total | Passed | Failed | Unsupported |
|---|---:|---:|---:|---:|
| Object Creation | 20 | 12 | 0 | 7 |
| Delete & Reset | 10 | 9 | 0 | 1 |
| Positioning | 15 | 0 | 0 | 15 |
| Scaling | 15 | 1 | 0 | 14 |
| Rotation | 15 | 0 | 0 | 15 |
| Appearance | 15 | 14 | 0 | 1 |
| 3D Graphing | 15 | 9 | 0 | 6 |
| Geometry Relations | 15 | 1 | 0 | 14 |
| Conversational AI | 15 | 6 | 0 | 9 |
| Error & Stress Tests | 15 | 7 | 0 | 7 |
| **Total** | **150** | **59** | **0** | **89** |

Rendering skipped: 2. Unrecorded IDs: none.

**Verification remains incomplete:** at least one supported case has no valid canvas capture. A passing instrumentation method does not promote skipped cases to passes.

Scene/geometry checks verified: 61. Applied scene operations: 56. Safe invalid-input rejections: 5. State/mesh verification is tracked separately from rendering.

Regression JUnit tests recorded: 42; failures: 0.

- Supplemental 2D visible-prompt test: PASS; changed canvas pixels: 46698.
- Device: API 35 Codex_AR_Review, 1080×2400 at density 420. Stress cases 19 and 146 also exhausted capture retries at 720×1280/density 240; the display was restored. See device-config.json and device-stress-retry.json.
- Supported-case parser checks: 56/56. This excludes unsupported sentences and is not an assertion of general English accuracy.
- Supported execution checks: 56/56.
- Render assertions: 148/148; includes unchanged-canvas checks on rejected instructions. Pixel comparison uses the central plot area to exclude overlapping chrome/status text. Capture retries are bounded; missing captures remain rendering skipped.
- Supported context checks: 15/15.
- Mean command execution: 91.44 ms; maximum: 4369.36 ms. Mesh checks and screenshots occur separately.

## What changed

- Connected the existing visible assistant to structured native Graph actions, preserving lesson/search/solver routing for educational queries and other workspaces.
- Added equation plotting, supported solid surface equations, bounded counts, selection, deletion, duplication with a visible offset, material/color/opacity/visibility, grid control, and shared Undo/Redo. Graph command history preserves unrelated fields; selections are separate for 2D and 3D, and failed multi-step history operations leave both stacks unchanged.
- Compound operations stage immutable snapshots; unsupported or invalid clauses leave the complete scene and history unchanged. Mixed Undo/Redo and mutation batches are rejected explicitly. Preparation runs off the UI thread; a changed workspace prevents a stale commit.
- Preserved holes at poles and undefined surface regions; only numerically convergent removable holes are filled. Sampling includes diagonal directions. The canvas and audit use the same geometry boundary.
- Fixed implicit quad triangulation, outward winding, exact-zero edges and duplicate grid-aligned faces. Independent plane coverage and area regressions capture the original failures.
- Preserved grid preferences on Clear, linked authoritative objects, and semantic references across native primary-layer ID changes.
- Added saved color appearance, material handling for indexed meshes, and a separate debug-only audit activity.

## Limits

The original scope is not fully implemented. General object movement, scaling, rotation/animation, groups, point/line/vector constructions, most relationship operations, saved-state restoration, and unrestricted conversational variants remain unsupported. A passing rejection test establishes safe failure, not feature support. Random sphere placement is repeatable per command for reproducibility. Surface meshes approximate analytic surfaces at bounded resolution. Numerical removable-hole inference is not a symbolic proof of a multivariable limit. Pixel differences establish visible changes, not complete mathematical or visual correctness for every triangle. Physical GPU performance is unverified.

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
- 29. Delete objects inside the selected group.
- 31. Move the cube to (3, 2, 1).
- 32. Shift the sphere 5 units right.
- 33. Move the cube 2 units upward.
- 34. Move the cone backward by 3 units.
- 35. Place the cylinder at the origin.
- 36. Move all cubes 2 units left.
- 37. Position the sphere above the cube.
- 38. Put the cone next to the cylinder.
- 39. Move the selected object to (0, 0, 0).
- 40. Translate the cube by vector (2, 3, 4).
- 41. Move the sphere along the X-axis.
- 42. Align all objects along the Y-axis.
- 43. Place three cubes in a row.
- 44. Distribute five spheres evenly.
- 45. Move all objects to the positive X region.
- 46. Double the cube's size.
- 47. Reduce the sphere to half its size.
- 48. Make the cylinder taller.
- 49. Set the cube's side length to 5 units.
- 50. Increase the cone's height to 8 units.
- 51. Scale all objects by 1.5.
- 52. Make the cube twice as wide.
- 53. Change the sphere radius to 3.
- 54. Set the cylinder radius to 2.
- 55. Stretch the cube along the Z-axis.
- 56. Make the pyramid smaller.
- 57. Resize the selected shape to 4 × 5 × 6.
- 58. Make all cubes the same size.
- 59. Restore the selected object's original size.
- 61. Rotate the cube 45 degrees.
- 62. Rotate the sphere around the X-axis by 90 degrees.
- 63. Turn the cylinder upside down.
- 64. Rotate all cubes 30 degrees around the Y-axis.
- 65. Spin the selected object continuously.
- 66. Stop the rotation.
- 67. Tilt the cube 15 degrees.
- 68. Rotate the cone clockwise by 90 degrees.
- 69. Align the cylinder with the Z-axis.
- 70. Reset the cube's rotation.
- 71. Rotate the selected object around its own center.
- 72. Rotate the cube around the world origin.
- 73. Animate one complete rotation.
- 74. Rotate the sphere 180 degrees about the Y-axis.
- 75. Rotate the grouped objects together.
- 83. Apply different colors to all five cubes.
- 96. Draw a 3D helix.
- 99. Plot the parametric curve x = cos(t), y = sin(t), z = t for 0 ≤ t ≤ 10.
- 100. Plot 100 random points in 3D.
- 101. Display the X, Y and Z axes.
- 104. Label the coordinate axes.
- 105. Change the graph range to −10 through 10 on all three axes.
- 106. Place a sphere inside a cube.
- 107. Put a cube on top of a cylinder.
- 108. Stack three cubes vertically.
- 109. Arrange six spheres in a circle.
- 110. Create a pyramid using four points.
- 111. Connect two selected points with a line.
- 112. Draw a vector between the two points.
- 113. Find the midpoint of the selected line.
- 114. Measure the distance between two points.
- 115. Create a plane through three non-collinear points.
- 116. Find the intersection of two planes.
- 117. Group the selected cubes.
- 118. Ungroup the selected objects.
- 120. Mirror the cube across the YZ-plane.
- 122. Create two spheres and move them apart.
- 123. Make another cube just like the previous one.
- 125. Make the last object bigger.
- 126. Move it 3 units higher.
- 128. Rotate that same object by 45 degrees.
- 130. Create five cubes and color the third one green.
- 131. Select the smallest sphere.
- 132. Delete all objects except the red cube.
- 135. Restore the scene to its last saved state.
- 138. Move the cube to (abc, 2, 3).
- 139. Rotate the cube by 999999 degrees.
- 142. Make the cube infinitely large.
- 147. Repeat the previous command five times.
- 148. Create two cubes, delete one, then restore it.
- 149. Reset everything, create a sphere, and move it to (5, 5, 5).
- 150. Create five cubes, group them, rotate the group, ungroup them, and delete only the first cube.
