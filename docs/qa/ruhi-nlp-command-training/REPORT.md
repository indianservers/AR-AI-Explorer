# Ruhi requested-command TensorFlow training

This increment trains recognition of the 89 developer-supplied commands while preserving the existing UI, lessons, deterministic parser and mathematical engines. Recognition does not add missing geometric constructors or bypass validation.

- Bundled model: TensorFlow-trained LiteRT model, 26 intent labels; SHA-256 `782fa1398227e8dde6c2b5d6a1e0af3d3b2ba07641400a49511316af4ef55ffa`.
- Training: 4501 unique authored examples, including the explicitly requested developer commands and bounded numeric/object/polite variants. No harvested student conversations or correction records.
- Supplied-command recognition replay: **89/89**. These sentences informed training; this is training fit, not generalization accuracy.
- Supplied examples meeting the unchanged confidence gate: 72/89. Top-label recognition alone is not execution approval.
- Development paraphrases: 36/36; accepted 31, accepted errors 0. Complete sentences are excluded from training, but related templates are present and these results were inspected during development.
- Separate frozen sentence evaluation: **26/26** (100.0%); accepted 23, accepted errors 0. This is a small authored benchmark, not unrestricted NLP or speech accuracy.
- Actual Android model inference over the 89 examples: PASS; saved instrumentation summary: 2 methods passed.

- JVM regressions: 61; failures/errors: 0. These include the original 150-command state/mesh audit, not a new rendered replay.

Confidence gates remain 0.85 probability and 0.35 top-two margin. Only whitelisted paraphrases can become canonical executable text. Models do not supply coordinates, dimensions, mesh vertices, formulas or pixels. Invalid coordinates, nonfinite sizes and excessive angles remain subject to deterministic validation. Missing parameters still require clarification; unknown engine operations remain unsupported.

## Requested examples

The execution-status column is the existing Graph3D audit expectation under its preconditions, not a new rendered verification. Shared geometry adapters may differ. Compound actions remain atomic.

| ID | Command | Expected intent | Predicted intent | Confidence | Existing graph behavior |
|---:|---|---|---|---:|---|
| 10 | Create a tetrahedron. | CREATE | CREATE | 0.950 | UNSUPPORTED |
| 12 | Create a dodecahedron. | CREATE | CREATE | 0.950 | UNSUPPORTED |
| 13 | Create an icosahedron. | CREATE | CREATE | 0.680 | UNSUPPORTED |
| 15 | Draw a 3D triangle. | CREATE | CREATE | 0.997 | UNSUPPORTED |
| 16 | Create a line segment. | CREATE | CREATE | 0.972 | UNSUPPORTED |
| 17 | Plot a point at (2, 3, 4). | CREATE | CREATE | 0.990 | UNSUPPORTED |
| 18 | Create a vector from (0, 0, 0) to (3, 4, 5). | CREATE | CREATE | 0.994 | UNSUPPORTED |
| 29 | Delete objects inside the selected group. | DELETE | DELETE | 0.944 | APPLIED |
| 31 | Move the cube to (3, 2, 1). | MOVE | MOVE | 0.994 | APPLIED |
| 32 | Shift the sphere 5 units right. | MOVE | MOVE | 0.967 | APPLIED |
| 33 | Move the cube 2 units upward. | MOVE | MOVE | 0.997 | APPLIED |
| 34 | Move the cone backward by 3 units. | MOVE | MOVE | 0.983 | APPLIED |
| 35 | Place the cylinder at the origin. | MOVE | MOVE | 0.907 | APPLIED |
| 36 | Move all cubes 2 units left. | MOVE | MOVE | 0.989 | APPLIED |
| 37 | Position the sphere above the cube. | MOVE | MOVE | 0.710 | APPLIED |
| 38 | Put the cone next to the cylinder. | MOVE | MOVE | 0.358 | APPLIED |
| 39 | Move the selected object to (0, 0, 0). | MOVE | MOVE | 0.982 | APPLIED |
| 40 | Translate the cube by vector (2, 3, 4). | MOVE | MOVE | 0.994 | APPLIED |
| 41 | Move the sphere along the X-axis. | MOVE | MOVE | 0.721 | CLARIFICATION |
| 42 | Align all objects along the Y-axis. | MOVE | MOVE | 0.312 | APPLIED |
| 43 | Place three cubes in a row. | ARRANGE | ARRANGE | 0.984 | APPLIED |
| 44 | Distribute five spheres evenly. | ARRANGE | ARRANGE | 0.972 | APPLIED |
| 45 | Move all objects to the positive X region. | MOVE | MOVE | 0.727 | APPLIED |
| 46 | Double the cube's size. | SCALE | SCALE | 0.798 | APPLIED |
| 47 | Reduce the sphere to half its size. | SCALE | SCALE | 0.947 | APPLIED |
| 48 | Make the cylinder taller. | SCALE | SCALE | 0.700 | CLARIFICATION |
| 49 | Set the cube's side length to 5 units. | SCALE | SCALE | 0.985 | APPLIED |
| 50 | Increase the cone's height to 8 units. | SCALE | SCALE | 0.948 | APPLIED |
| 51 | Scale all objects by 1.5. | SCALE | SCALE | 0.900 | APPLIED |
| 52 | Make the cube twice as wide. | SCALE | SCALE | 0.941 | APPLIED |
| 53 | Change the sphere radius to 3. | SCALE | SCALE | 0.981 | APPLIED |
| 54 | Set the cylinder radius to 2. | SCALE | SCALE | 0.991 | APPLIED |
| 55 | Stretch the cube along the Z-axis. | SCALE | SCALE | 0.858 | CLARIFICATION |
| 56 | Make the pyramid smaller. | SCALE | SCALE | 0.712 | CLARIFICATION |
| 57 | Resize the selected shape to 4 × 5 × 6. | SCALE | SCALE | 0.878 | APPLIED |
| 58 | Make all cubes the same size. | SCALE | SCALE | 0.781 | APPLIED |
| 59 | Restore the selected object's original size. | SCALE | SCALE | 0.599 | APPLIED |
| 61 | Rotate the cube 45 degrees. | ROTATE | ROTATE | 0.996 | APPLIED |
| 62 | Rotate the sphere around the X-axis by 90 degrees. | ROTATE | ROTATE | 0.999 | APPLIED |
| 63 | Turn the cylinder upside down. | ROTATE | ROTATE | 0.923 | APPLIED |
| 64 | Rotate all cubes 30 degrees around the Y-axis. | ROTATE | ROTATE | 0.998 | APPLIED |
| 65 | Spin the selected object continuously. | ANIMATE | ANIMATE | 0.974 | UNSUPPORTED |
| 66 | Stop the rotation. | STOP_ANIMATION | STOP_ANIMATION | 0.990 | UNSUPPORTED |
| 67 | Tilt the cube 15 degrees. | ROTATE | ROTATE | 0.988 | APPLIED |
| 68 | Rotate the cone clockwise by 90 degrees. | ROTATE | ROTATE | 0.994 | APPLIED |
| 69 | Align the cylinder with the Z-axis. | ROTATE | ROTATE | 0.509 | APPLIED |
| 70 | Reset the cube's rotation. | ROTATE | ROTATE | 0.849 | APPLIED |
| 71 | Rotate the selected object around its own center. | ROTATE | ROTATE | 0.816 | CLARIFICATION |
| 72 | Rotate the cube around the world origin. | ROTATE | ROTATE | 0.842 | CLARIFICATION |
| 73 | Animate one complete rotation. | ANIMATE | ANIMATE | 0.888 | UNSUPPORTED |
| 74 | Rotate the sphere 180 degrees about the Y-axis. | ROTATE | ROTATE | 0.997 | APPLIED |
| 75 | Rotate the grouped objects together. | ROTATE | ROTATE | 0.629 | CLARIFICATION |
| 83 | Apply different colors to all five cubes. | COLOR | COLOR | 0.932 | UNSUPPORTED |
| 96 | Draw a 3D helix. | CREATE | CREATE | 0.997 | UNSUPPORTED |
| 99 | Plot the parametric curve x = cos(t), y = sin(t), z = t for 0 ≤ t ≤ 10. | CREATE | CREATE | 0.997 | UNSUPPORTED |
| 100 | Plot 100 random points in 3D. | CREATE | CREATE | 0.991 | UNSUPPORTED |
| 101 | Display the X, Y and Z axes. | AXES | AXES | 0.996 | UNSUPPORTED |
| 104 | Label the coordinate axes. | AXES | AXES | 0.989 | UNSUPPORTED |
| 105 | Change the graph range to −10 through 10 on all three axes. | RANGE | RANGE | 1.000 | UNSUPPORTED |
| 106 | Place a sphere inside a cube. | RELATION | RELATION | 0.970 | UNSUPPORTED |
| 107 | Put a cube on top of a cylinder. | RELATION | RELATION | 0.992 | UNSUPPORTED |
| 108 | Stack three cubes vertically. | ARRANGE | ARRANGE | 0.956 | APPLIED |
| 109 | Arrange six spheres in a circle. | ARRANGE | ARRANGE | 0.995 | UNSUPPORTED |
| 110 | Create a pyramid using four points. | CREATE | CREATE | 0.984 | UNSUPPORTED |
| 111 | Connect two selected points with a line. | CREATE | CREATE | 0.963 | UNSUPPORTED |
| 112 | Draw a vector between the two points. | CREATE | CREATE | 0.955 | UNSUPPORTED |
| 113 | Find the midpoint of the selected line. | MEASURE | MEASURE | 0.990 | UNSUPPORTED |
| 114 | Measure the distance between two points. | MEASURE | MEASURE | 0.991 | UNSUPPORTED |
| 115 | Create a plane through three non-collinear points. | CREATE | CREATE | 0.988 | UNSUPPORTED |
| 116 | Find the intersection of two planes. | MEASURE | MEASURE | 0.983 | UNSUPPORTED |
| 117 | Group the selected cubes. | GROUP | GROUP | 0.978 | APPLIED |
| 118 | Ungroup the selected objects. | UNGROUP | UNGROUP | 0.992 | APPLIED |
| 120 | Mirror the cube across the YZ-plane. | MIRROR | MIRROR | 0.997 | APPLIED |
| 122 | Create two spheres and move them apart. | BATCH | BATCH | 0.992 | APPLIED |
| 123 | Make another cube just like the previous one. | DUPLICATE | DUPLICATE | 0.965 | APPLIED |
| 125 | Make the last object bigger. | SCALE | SCALE | 0.755 | CLARIFICATION |
| 126 | Move it 3 units higher. | MOVE | MOVE | 0.951 | APPLIED |
| 128 | Rotate that same object by 45 degrees. | ROTATE | ROTATE | 0.983 | APPLIED |
| 130 | Create five cubes and color the third one green. | BATCH | BATCH | 0.984 | APPLIED |
| 131 | Select the smallest sphere. | SELECT | SELECT | 0.989 | APPLIED |
| 132 | Delete all objects except the red cube. | DELETE | DELETE | 0.969 | APPLIED |
| 135 | Restore the scene to its last saved state. | RESTORE | RESTORE | 0.999 | UNSUPPORTED |
| 138 | Move the cube to (abc, 2, 3). | MOVE | MOVE | 0.996 | INVALID |
| 139 | Rotate the cube by 999999 degrees. | ROTATE | ROTATE | 0.992 | INVALID |
| 142 | Make the cube infinitely large. | SCALE | SCALE | 0.892 | INVALID |
| 147 | Repeat the previous command five times. | REPEAT | REPEAT | 0.995 | UNSUPPORTED |
| 148 | Create two cubes, delete one, then restore it. | BATCH | BATCH | 0.995 | UNSUPPORTED |
| 149 | Reset everything, create a sphere, and move it to (5, 5, 5). | BATCH | BATCH | 0.998 | APPLIED |
| 150 | Create five cubes, group them, rotate the group, ungroup them, and delete only the first cube. | BATCH | BATCH | 0.995 | CLARIFICATION |

## Separate evaluation errors

None in the recorded small benchmark.

## Reproduction

Use the Python 3.12 environment with scripts/ruhi-nlp-requirements.txt. Run scripts/train_ruhi_graph_nlp.py, then scripts/evaluate_ruhi_requested_nlp.py. The independent evaluation never trains the model. Run the two Android methods bundledTensorFlowModelRecognizesAndExecutesParaphrases and requestedCommandExamplesHaveCorrectTensorFlowIntents in RuhiGraphRenderingTest for native inference. Build the debug APK to package the new assets.

Supporting files: evaluation.json, independent-evaluation.json, command_examples.json, training.log, build.log and device-run.log. The separate engine-selection classifier is unchanged; its existing 83.6% benchmark is not the intent score above. The earlier 150-command rendering report is historical evidence and was not recertified by this model-only increment.
