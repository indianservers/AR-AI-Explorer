# Ruhi 500-row test and TensorFlow training loop

Current cycle: 72. **The full goal remains in progress.**

All 500 original Excel rows and their expected behavior, structured slots and follow-up policies are preserved. Baseline execution verified 10 distance behaviors. Current native behavior verification proves 500/500 source cases in declared valid contexts; **0 remain unverified in that replay**. This does not prove every context or conditional follow-up. Unsupported/unverified behavior is not relabeled as passing.

TensorFlow seed intent fit: 500/500. Android seed intent recognition: 500/500. These are training examples, not an independent language benchmark.
Point/segment-slot head: 10/10 held-out phrasing templates; their point and segment names are absent from slot training. This is a small repeatedly inspected check, not broad NLP accuracy.
Unseen-label/current-coordinate scene checks: 600 in Android, plus 600 JVM checks. Both 2D and 3D scenes are tested at two sets of coordinates, using different labels. Renaming points leaves model input features invariant. Distances are calculated by the deterministic engine, not learned or copied from the workbook.

JVM regression methods: 594, failures/errors: 0. Android instrumentation: 48 passed. The instrumentation also reruns the earlier 300-command scene/model audit and visible solid-edit test.

## Visible follow-up verification

Missing-point creation chip and coordinate answer resume the original distance question: True. The visible assistant answers 5 units for Alpha7/Beta9, then 10 units for the same utterance after the scene coordinates change. No extra points are added. See [screenshot](cycle-72-device/named-point-answer.png).

## Midpoint and slope evidence

Midpoint behaviors: 25/25; slope behaviors: 25/25. Source slots are checked against independent coordinate calculations. Named midpoint markers depend on both endpoints, explicit-coordinate midpoints preserve existing points, custom labels reject collisions, split commands create two subsegments, and annotations survive save/load and undo/redo.
Real 2D canvas verification: True. Midpoint pixels changed by 12029 and slope-triangle pixels by 55508. Native parent editing updated the same midpoint and the live slope annotation. See [marker](cycle-72-device/midpoint-marker.png), [slope triangle](cycle-72-device/slope-triangle.png), and [parent edit](cycle-72-device/midpoint-after-parent-drag.png).

Fixtures preserve the previous scene. Row 168 frees the requested new label M while keeping its previous unlabelled coordinate; collisions are tested separately. Row 185 selects an actually vertical segment, matching the question's premise. Row 186 supplies a red CD segment. Every row runs in a fresh scene. The visible test uses Alpha7/Beta9 and a separately named segment. Full missing-context/follow-up policy verification remains incomplete and is not implied by successful valid-context actions.

## Angle evidence

Named angle behaviors: 25/25, checked against source slots and independent dot-product/acos calculations. Valid-context action proof is separate from full conditional follow-up proof; remaining dialogue policies still require independent tests.
Visible arc and degree-label check: True. Editing the same named vertices changes the measured angle from 90 to 45 degrees. See [right angle](cycle-72-device/angle-right.png) and [edited angle](cycle-72-device/angle-after-parent-drag.png).

Native bisector construction and parent references verified: True. Counterclockwise arc verified at 270 degrees, changing to 90 after a parent edit. See [bisector](cycle-72-device/angle-bisector.png) and [counterclockwise arc](cycle-72-device/angle-counterclockwise.png).

Additional valid-context fixtures: row 208 includes triangle DEF, and row 224 includes triangle PQR, alongside the original scene. These are required scene contexts, not learned or fabricated answers. Missing and ambiguous polygons remain clarification cases.

Selected-vertex chip resumed the original classification request: True. A native point selection at a concave polygon corner was classified as reflex; editing its parent made the same marker a right angle: True. See [selected vertex](cycle-72-device/angle-selected-vertex.png), [reflex interior](cycle-72-device/angle-reflex.png), and [edited interior](cycle-72-device/angle-reflex-after-parent-drag.png).

Valid selection fixtures: rows 206/209 select vertex B of ABC, row 212 selects a genuinely right triangle, and row 218 selects quadrilateral ABCD. The original expected slots are unchanged. JVM checks cover pending vertex save/load, resume, cancellation, stale-scene rejection, displayed P-number references, and concave polygon validation. These checks do not certify all remaining dialogue policies.

Reference-angle canvas verification: True. A line intersection moved from (0,0) to (2,0), and its annotation changed from 90 to 63.43494882292201 degrees. Origin measurements use (0,0) even when a point named O is elsewhere. Positive-X inclination is checked at 123.69006752597979 degrees; no helper points are added. See [intersection](cycle-72-device/reference-angle-intersection.png), [edited intersection](cycle-72-device/reference-angle-intersection-after-parent-drag.png), [origin](cycle-72-device/reference-angle-origin.png), and [axis](cycle-72-device/reference-angle-axis.png).

Reference fixtures: row 205 includes named 2D vectors u/v; row 211 includes intersecting lines l/m; row 219 supplies nonzero origin rays A=(2,0), B=(0,3); row 225 includes segment BC after AB. All other scene contents remain present. Infinite-line angles use the smaller angle; connected segments use their shared-vertex angle. Parallel lines, zero directions and missing references are tested without changing geometry.

Native 3D vector angle verified: True. The assistant asks for a missing vector pair and resumes from its answer chip; stable-ID arc references follow a native vector edit from 90 to 45 degrees with a nonzero Z component. Selection, the live arc and save/load are checked without helper points or solids. See [pair question](cycle-72-device/spatial-angle-question.png), [right angle](cycle-72-device/spatial-angle-right.png), and [edited 3D arc](cycle-72-device/spatial-angle-after-vector-edit.png).

Row 220 runs in native 3D geometry with explicitly selected stable vector IDs, using directions (3,0,4) and (0,4,3); its independent oracle uses the full 3D dot product and validates arc endpoints/radius. Additional tests cover renamed/reordered vectors, zero/parallel/antiparallel vectors, stale answers, cancellation, undo and opaque IDs. Canonical document schema 4 encodes dependency IDs and construction arguments individually; schema 3 files remain readable.

Native plane and dihedral canvas verification: True. Both planes are created through Ruhi from named 3D points, selected, and measured through the missing-pair chip. A native parent edit updates the same dihedral marker from 90 to 45 degrees. Plane definitions, canonical point dependencies, circular arc endpoints, save/load, deletion/undo and legacy workspace migration are checked. See [created planes](cycle-72-device/planes-selected.png), [pair question](cycle-72-device/plane-angle-question.png), [right dihedral](cycle-72-device/plane-angle-right.png), and [edited dihedral](cycle-72-device/plane-angle-after-point-edit.png).

Row 221 supplies two translated native planes defined by explicit stable point IDs. The independent oracle computes both equations and their smaller, unoriented dihedral angle, checks that the arc anchor lies on both planes, and checks arc endpoints/radius. Infinite planes are displayed as bounded patches. Parallel planes report zero with an orientation marker on the first plane. Plane records were introduced in snapshot schema 12; schema 13 added dependent circle recipes; schema 14 adds a live triangle-vertex recipe; current snapshot schema 15 also preserves solid mirror parity and reads older snapshots.
Legacy 300-request regression: 300/300 expected outcomes. Requests 34/234 now construct point-defined planes and are checked against x/2 + y/3 + z/4 = 1. Their reviewed expected status was upgraded from UNSUPPORTED to APPLIED because the construction is implemented and independently checked; the original requests and trained intent labels are unchanged. The 500-row workbook oracle is unchanged.

## Remaining scope

| Intent | Rows requiring behavior proof |
|---|---:|

Recognition success does not prove construction, rendering, calculus, selection, or follow-up behavior. The source expectations remain the acceptance criteria for subsequent loops. A read-only question may legitimately leave the scene unchanged, while a construction must change the intended model and have visible/persistent evidence.

## Reproduce

`scripts/import_ruhi_500.py <source.xlsx>` imports without modifying the source workbook. `scripts/run_ruhi_seed500_cycle.ps1 -Cycle <next-number>` runs all seed/regression tests before training, trains both TensorFlow heads, rebuilds, retests and verifies in the emulator. Inspect behavior-gaps.json and fix engine/parser gaps between cycles; repeatedly refitting unchanged data cannot implement missing operations.

## Evidence

- [Native seed results](cycle-72-device/seed500-native.json) · [Point-slot and coordinate checks](cycle-72-device/unseen-labels-native.json)
- [Visible assistant assertions](cycle-72-device/visible-assistant.json) · [Training evaluation](cycle-72-model.json)
- [Baseline results](baseline-scene-results.json) · [Original workbook fields](seed-rows.json) · [Original source SHA-256](source.json)
- [Requirement audit and next work](LOOP_STATE.json) · [All remaining rows](behavior-gaps.json) · [Packaged model/source hashes](source-manifest.json)

## Row-by-row state

| ID | Command | Workbook intent | Native status | Workbook behavior proof |
|---:|---|---|---|---|
| 1 | distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 2 | What is the distance from C to D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 3 | Find the distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 4 | Calculate the distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 5 | Measure the distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 6 | distance between C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 7 | What is the distance between C and D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 8 | Find the distance between points C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 9 | Calculate the distance between point C and point D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 10 | Measure the distance between C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 11 | How far is C from D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 12 | How far are C and D apart? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 13 | Tell me the distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 14 | Give me the distance between C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 15 | Compute the distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 16 | Work out the distance between C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 17 | Please find the distance from C to D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 18 | Can you measure the distance between C and D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 19 | Could you calculate the distance from point C to point D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 20 | What is the straight-line distance from C to D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 21 | Find the Euclidean distance between C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 22 | What is the separation between C and D? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 23 | Find the length between C and D | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 24 | distance from C to D now | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 25 | Kindly measure the distance from C to D in units | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 26 | distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 27 | What is the distance from A to B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 28 | Find the distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 29 | Calculate the distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 30 | Measure the distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 31 | distance between A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 32 | What is the distance between A and B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 33 | Find the distance between points A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 34 | Calculate the distance between point A and point B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 35 | Measure the distance between A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 36 | How far is A from B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 37 | How far are A and B apart? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 38 | Tell me the distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 39 | Give me the distance between A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 40 | Compute the distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 41 | Work out the distance between A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 42 | Please find the distance from A to B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 43 | Can you measure the distance between A and B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 44 | Could you calculate the distance from point A to point B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 45 | What is the straight-line distance from A to B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 46 | Find the Euclidean distance between A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 47 | What is the separation between A and B? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 48 | Find the length between A and B | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 49 | distance from A to B now | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 50 | Kindly measure the distance from A to B in units | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 51 | distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 52 | What is the distance from D to C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 53 | Find the distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 54 | Calculate the distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 55 | Measure the distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 56 | distance between D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 57 | What is the distance between D and C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 58 | Find the distance between points D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 59 | Calculate the distance between point D and point C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 60 | Measure the distance between D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 61 | How far is D from C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 62 | How far are D and C apart? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 63 | Tell me the distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 64 | Give me the distance between D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 65 | Compute the distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 66 | Work out the distance between D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 67 | Please find the distance from D to C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 68 | Can you measure the distance between D and C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 69 | Could you calculate the distance from point D to point C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 70 | What is the straight-line distance from D to C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 71 | Find the Euclidean distance between D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 72 | What is the separation between D and C? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 73 | Find the length between D and C | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 74 | distance from D to C now | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 75 | Kindly measure the distance from D to C in units | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 76 | distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 77 | What is the distance from P to Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 78 | Find the distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 79 | Calculate the distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 80 | Measure the distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 81 | distance between P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 82 | What is the distance between P and Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 83 | Find the distance between points P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 84 | Calculate the distance between point P and point Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 85 | Measure the distance between P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 86 | How far is P from Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 87 | How far are P and Q apart? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 88 | Tell me the distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 89 | Give me the distance between P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 90 | Compute the distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 91 | Work out the distance between P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 92 | Please find the distance from P to Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 93 | Can you measure the distance between P and Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 94 | Could you calculate the distance from point P to point Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 95 | What is the straight-line distance from P to Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 96 | Find the Euclidean distance between P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 97 | What is the separation between P and Q? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 98 | Find the length between P and Q | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 99 | distance from P to Q now | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 100 | Kindly measure the distance from P to Q in units | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 101 | distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 102 | What is the distance from A1 to B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 103 | Find the distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 104 | Calculate the distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 105 | Measure the distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 106 | distance between A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 107 | What is the distance between A1 and B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 108 | Find the distance between points A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 109 | Calculate the distance between point A1 and point B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 110 | Measure the distance between A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 111 | How far is A1 from B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 112 | How far are A1 and B2 apart? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 113 | Tell me the distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 114 | Give me the distance between A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 115 | Compute the distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 116 | Work out the distance between A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 117 | Please find the distance from A1 to B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 118 | Can you measure the distance between A1 and B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 119 | Could you calculate the distance from point A1 to point B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 120 | What is the straight-line distance from A1 to B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 121 | Find the Euclidean distance between A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 122 | What is the separation between A1 and B2? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 123 | Find the length between A1 and B2 | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 124 | distance from A1 to B2 now | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 125 | Kindly measure the distance from A1 to B2 in units | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 126 | distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 127 | What is the distance from M to N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 128 | Find the distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 129 | Calculate the distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 130 | Measure the distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 131 | distance between M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 132 | What is the distance between M and N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 133 | Find the distance between points M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 134 | Calculate the distance between point M and point N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 135 | Measure the distance between M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 136 | How far is M from N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 137 | How far are M and N apart? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 138 | Tell me the distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 139 | Give me the distance between M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 140 | Compute the distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 141 | Work out the distance between M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 142 | Please find the distance from M to N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 143 | Can you measure the distance between M and N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 144 | Could you calculate the distance from point M to point N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 145 | What is the straight-line distance from M to N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 146 | Find the Euclidean distance between M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 147 | What is the separation between M and N? | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 148 | Find the length between M and N | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 149 | distance from M to N now | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 150 | Kindly measure the distance from M to N in units | MEASURE_DISTANCE | APPLIED | VERIFIED |
| 151 | Find the midpoint of AB | FIND_MIDPOINT | APPLIED | VERIFIED |
| 152 | Where is the middle of segment CD? | FIND_MIDPOINT | APPLIED | VERIFIED |
| 153 | Mark the center point between P and Q | FIND_MIDPOINT | APPLIED | VERIFIED |
| 154 | Bisect line segment MN | FIND_MIDPOINT | APPLIED | VERIFIED |
| 155 | Show the halfway point of A1B2 | FIND_MIDPOINT | APPLIED | VERIFIED |
| 156 | Compute the midpoint between R and S | FIND_MIDPOINT | APPLIED | VERIFIED |
| 157 | Put a point halfway along XY | FIND_MIDPOINT | APPLIED | VERIFIED |
| 158 | Locate the midpoint of segment EF | FIND_MIDPOINT | APPLIED | VERIFIED |
| 159 | What are the midpoint coordinates of GH? | FIND_MIDPOINT | APPLIED | VERIFIED |
| 160 | Split UV into two equal segments | FIND_MIDPOINT | APPLIED | VERIFIED |
| 161 | Construct the midpoint between K and L | FIND_MIDPOINT | APPLIED | VERIFIED |
| 162 | Mark the midpoint of the selected segment | FIND_MIDPOINT | APPLIED | VERIFIED |
| 163 | Draw the midpoint of the blue line | FIND_MIDPOINT | APPLIED | VERIFIED |
| 164 | Give the midpoint of the last segment | FIND_MIDPOINT | APPLIED | VERIFIED |
| 165 | Which point is halfway between T and W? | FIND_MIDPOINT | APPLIED | VERIFIED |
| 166 | Calculate the halfway position on JK | FIND_MIDPOINT | APPLIED | VERIFIED |
| 167 | Add the midpoint of PQ to the drawing | FIND_MIDPOINT | APPLIED | VERIFIED |
| 168 | Label the middle of AB as M | FIND_MIDPOINT | APPLIED | VERIFIED |
| 169 | Show midpoint coordinates for A at (0,0) and B at (6,8) | FIND_MIDPOINT | APPLIED | VERIFIED |
| 170 | Find the midpoint of (2,3) and (8,11) | FIND_MIDPOINT | APPLIED | VERIFIED |
| 171 | Construct the midpoint of the line I just drew | FIND_MIDPOINT | APPLIED | VERIFIED |
| 172 | Mark midpoint without showing the full line | FIND_MIDPOINT | APPLIED | VERIFIED |
| 173 | Place a marker at the center of QR | FIND_MIDPOINT | APPLIED | VERIFIED |
| 174 | Draw the bisecting point for segment BC | FIND_MIDPOINT | APPLIED | VERIFIED |
| 175 | Where does segment NP divide equally? | FIND_MIDPOINT | APPLIED | VERIFIED |
| 176 | Find the slope of AB | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 177 | What is the gradient of CD? | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 178 | Calculate rise over run from P to Q | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 179 | Measure the slope of the selected line | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 180 | Is AB increasing or decreasing? | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 181 | Show the slope triangle of MN | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 182 | Compute the slope between (1,2) and (3,8) | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 183 | Slope of the line through (0,5) and (5,0) | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 184 | Does the last line have positive slope? | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 185 | Why is this vertical line slope undefined? | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 186 | Find the gradient of the red line | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 187 | Annotate slope on the current graph | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 188 | Show rise and run separately for AB | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 189 | Determine if PQ is horizontal | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 190 | Find the line slope connecting X and Y | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 191 | How steep is segment EF? | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 192 | Show slope as a fraction for AB | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 193 | Compute the slope of y = 3x + 2 | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 194 | What slope does y = -2x + 7 have? | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 195 | Compare slopes of AB and CD | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 196 | Make the slope triangle visible on KL | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 197 | Tell me the change in y per x unit for GH | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 198 | Slope from A1 to B2 please | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 199 | Mark the gradient of the newest segment | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 200 | Display the slope value next to this line | CALCULATE_SLOPE | APPLIED | VERIFIED |
| 201 | Measure angle ABC | MEASURE_ANGLE | APPLIED | VERIFIED |
| 202 | What is the angle at B in triangle ABC? | MEASURE_ANGLE | APPLIED | VERIFIED |
| 203 | Show ∠PQR in degrees | MEASURE_ANGLE | APPLIED | VERIFIED |
| 204 | Find the angle between AB and AC | MEASURE_ANGLE | APPLIED | VERIFIED |
| 205 | Calculate the angle formed by vectors u and v | MEASURE_ANGLE | APPLIED | VERIFIED |
| 206 | Measure the selected corner of the polygon | MEASURE_ANGLE | APPLIED | VERIFIED |
| 207 | How many degrees is the angle at C? | MEASURE_ANGLE | APPLIED | VERIFIED |
| 208 | Display all angles inside triangle DEF | MEASURE_ANGLE | APPLIED | VERIFIED |
| 209 | Is this angle acute or obtuse? | MEASURE_ANGLE | APPLIED | VERIFIED |
| 210 | Calculate the exterior angle at vertex A | MEASURE_ANGLE | APPLIED | VERIFIED |
| 211 | Show the angle between intersecting lines l and m | MEASURE_ANGLE | APPLIED | VERIFIED |
| 212 | Mark the right angle in this triangle | MEASURE_ANGLE | APPLIED | VERIFIED |
| 213 | Find angle between the x-axis and AB | MEASURE_ANGLE | APPLIED | VERIFIED |
| 214 | Angle from OA to OB counterclockwise | MEASURE_ANGLE | APPLIED | VERIFIED |
| 215 | Measure ∠XYZ | MEASURE_ANGLE | APPLIED | VERIFIED |
| 216 | Check whether angle PQR equals 60 degrees | MEASURE_ANGLE | APPLIED | VERIFIED |
| 217 | Label the largest angle in triangle ABC | MEASURE_ANGLE | APPLIED | VERIFIED |
| 218 | Label the smallest corner of the quadrilateral | MEASURE_ANGLE | APPLIED | VERIFIED |
| 219 | Show the angle at origin between A and B | MEASURE_ANGLE | APPLIED | VERIFIED |
| 220 | What is the angle between these two 3D vectors? | MEASURE_ANGLE | APPLIED | VERIFIED |
| 221 | Measure the dihedral angle between two selected planes | MEASURE_ANGLE | APPLIED | VERIFIED |
| 222 | Show the angle bisector of ∠ABC | MEASURE_ANGLE | APPLIED | VERIFIED |
| 223 | Calculate angle BAC | MEASURE_ANGLE | APPLIED | VERIFIED |
| 224 | Draw an arc showing the measured angle at Q | MEASURE_ANGLE | APPLIED | VERIFIED |
| 225 | Tell me the angle between the last two segments | MEASURE_ANGLE | APPLIED | VERIFIED |
| 226 | Draw a circle | CREATE_CIRCLE | APPLIED | VERIFIED |
| 227 | Create a circle of radius 5 | CREATE_CIRCLE | APPLIED | VERIFIED |
| 228 | Place a circle at (2,3) with radius 4 | CREATE_CIRCLE | APPLIED | VERIFIED |
| 229 | Draw a circle centered on point A | CREATE_CIRCLE | APPLIED | VERIFIED |
| 230 | Make a circle through A and centered at B | CREATE_CIRCLE | APPLIED | VERIFIED |
| 231 | Construct a circle with diameter AB | CREATE_CIRCLE | APPLIED | VERIFIED |
| 232 | Draw a radius 7 circle at the origin | CREATE_CIRCLE | APPLIED | VERIFIED |
| 233 | Add a unit circle | CREATE_CIRCLE | APPLIED | VERIFIED |
| 234 | Make a small circle inside the big one | CREATE_CIRCLE | APPLIED | VERIFIED |
| 235 | Draw a circle tangent to the x-axis centered at (3,2) | CREATE_CIRCLE | APPLIED | VERIFIED |
| 236 | Create a circle passing through A, B, and C | CREATE_CIRCLE | APPLIED | VERIFIED |
| 237 | Show the circumcircle of triangle ABC | CREATE_CIRCLE | APPLIED | VERIFIED |
| 238 | Construct the incircle of triangle ABC | CREATE_CIRCLE | APPLIED | VERIFIED |
| 239 | Draw another circle with radius equal to the first | CREATE_CIRCLE | APPLIED | VERIFIED |
| 240 | Make a concentric circle with twice the radius | CREATE_CIRCLE | APPLIED | VERIFIED |
| 241 | Draw a circle of diameter 10 | CREATE_CIRCLE | APPLIED | VERIFIED |
| 242 | Add a red circle centered at (0,0) | CREATE_CIRCLE | APPLIED | VERIFIED |
| 243 | Put circle C1 at coordinates (-2,4) | CREATE_CIRCLE | APPLIED | VERIFIED |
| 244 | Create two circles that intersect | CREATE_CIRCLE | APPLIED | VERIFIED |
| 245 | Draw two externally tangent circles | CREATE_CIRCLE | APPLIED | VERIFIED |
| 246 | Make a circle of radius 2.5 | CREATE_CIRCLE | APPLIED | VERIFIED |
| 247 | Circle centered at P passing through Q | CREATE_CIRCLE | APPLIED | VERIFIED |
| 248 | Replace the selected circle radius with 9 | CREATE_CIRCLE | APPLIED | VERIFIED |
| 249 | Draw a circle around the selected triangle | CREATE_CIRCLE | APPLIED | VERIFIED |
| 250 | Construct the circle x² + y² = 16 | CREATE_CIRCLE | APPLIED | VERIFIED |
| 251 | Draw a triangle | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 252 | Make an equilateral triangle of side 5 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 253 | Construct a right triangle with legs 3 and 4 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 254 | Draw triangle ABC through the existing points | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 255 | Create an isosceles triangle with equal sides 6 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 256 | Make a triangle with sides 3, 4, and 5 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 257 | Build an acute triangle | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 258 | Draw an obtuse triangle | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 259 | Construct a triangle using A=(0,0), B=(5,0), C=(2,4) | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 260 | Create a scalene triangle | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 261 | Show a triangle with base 8 and height 5 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 262 | Draw an equilateral triangle centered on the origin | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 263 | Make a right triangle with hypotenuse 13 and one leg 5 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 264 | Construct an isosceles triangle with base 4 and height 7 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 265 | Draw a triangle rotated 30 degrees | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 266 | Create the triangle defined by points P, Q, and R | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 267 | Make a triangle with a 60-degree angle at A | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 268 | Draw a triangle with side AB fixed at 6 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 269 | Make the current triangle equilateral | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 270 | Construct a second triangle congruent to the first | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 271 | Draw a triangle similar to ABC at scale 2 | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 272 | Place a triangle above the x-axis | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 273 | Create triangle DEF with D(1,1), E(7,1), F(4,6) | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 274 | Draw a triangle showing Pythagoras theorem | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 275 | Make a triangle with interior angles 30, 60 and 90 degrees | CREATE_TRIANGLE | APPLIED | VERIFIED |
| 276 | Plot y = x squared | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 277 | Graph y = 2x + 1 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 278 | Draw the sine curve | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 279 | Show y = cos(x) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 280 | Plot y = x cubed | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 281 | Sketch the absolute-value graph | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 282 | Can you graph y = one over x? | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 283 | Display the natural logarithm function | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 284 | Plot y = square root of x | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 285 | Draw y = e to the x | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 286 | Visualize y = tan(x) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 287 | Graph the line x = 4 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 288 | Show the parabola y = x² - 4x + 3 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 289 | Plot y = sin(3x) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 290 | Graph y = 2 to the x | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 291 | Draw the constant function y = 5 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 292 | Plot the cubic y = x³ - x | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 293 | Graph y = 1/(x-2) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 294 | Draw y = x² only for x between -2 and 2 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 295 | Plot the function f(x) = 3x - 7 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 296 | Draw the upper half of x² + y² = 9 | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 297 | Plot the polar curve r = 2cos(3θ) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 298 | Graph x = cos(t), y = sin(t) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 299 | Show y = floor(x) | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 300 | Draw the piecewise function y = x if x≥0, otherwise -x | PLOT_FUNCTION_2D | APPLIED | VERIFIED |
| 301 | Where do y = x and y = x² meet? | FIND_INTERSECTION | APPLIED | VERIFIED |
| 302 | Find intersections of y = 2x and y = 4 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 303 | Mark the crossing points of the two graphs | FIND_INTERSECTION | APPLIED | VERIFIED |
| 304 | Show where sine and cosine intersect | FIND_INTERSECTION | APPLIED | VERIFIED |
| 305 | Calculate the intersection of y = x² and y = 4 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 306 | Find the roots of y = x² - 9 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 307 | Where does the red line cross the blue curve? | FIND_INTERSECTION | APPLIED | VERIFIED |
| 308 | Mark all intersections in the current viewport | FIND_INTERSECTION | APPLIED | VERIFIED |
| 309 | Find x where 3x + 2 equals 11 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 310 | Intersect x = 2 with y = x² | FIND_INTERSECTION | APPLIED | VERIFIED |
| 311 | Show the x-intercepts of the selected graph | FIND_INTERSECTION | APPLIED | VERIFIED |
| 312 | Where does the graph touch the y-axis? | FIND_INTERSECTION | APPLIED | VERIFIED |
| 313 | Compute crossing of y = x + 1 and y = -x + 5 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 314 | Find intersection of circles centered at A and B | FIND_INTERSECTION | APPLIED | VERIFIED |
| 315 | Mark intersections of line AB with circle C1 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 316 | Do the selected curves intersect at all? | FIND_INTERSECTION | APPLIED | VERIFIED |
| 317 | Give intersection coordinates to four decimals | FIND_INTERSECTION | APPLIED | VERIFIED |
| 318 | Show only positive-x intersection points | FIND_INTERSECTION | APPLIED | VERIFIED |
| 319 | Count the intersections of these plotted curves | FIND_INTERSECTION | APPLIED | VERIFIED |
| 320 | Label each intersection I1, I2 and so on | FIND_INTERSECTION | APPLIED | VERIFIED |
| 321 | Which intersection is closest to the origin? | FIND_INTERSECTION | APPLIED | VERIFIED |
| 322 | Show the crossing of y = x³ and y = x | FIND_INTERSECTION | APPLIED | VERIFIED |
| 323 | Find solutions to sin(x) = 0.5 within 0 to 2π | FIND_INTERSECTION | APPLIED | VERIFIED |
| 324 | Intersect y = sqrt(x) with y = x - 2 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 325 | Highlight the intersection nearest x = 1 | FIND_INTERSECTION | APPLIED | VERIFIED |
| 326 | Differentiate y = x³ | DIFFERENTIATE | APPLIED | VERIFIED |
| 327 | Find the derivative of sin(x) | DIFFERENTIATE | APPLIED | VERIFIED |
| 328 | Show the slope of y = x² at x = 3 | DIFFERENTIATE | APPLIED | VERIFIED |
| 329 | Draw a tangent to y = x² at x = 2 | DIFFERENTIATE | APPLIED | VERIFIED |
| 330 | Plot the derivative of the selected curve | DIFFERENTIATE | APPLIED | VERIFIED |
| 331 | Compute d/dx of e^x | DIFFERENTIATE | APPLIED | VERIFIED |
| 332 | Find the rate of change of 3x² + 2x | DIFFERENTIATE | APPLIED | VERIFIED |
| 333 | What is the derivative of ln(x)? | DIFFERENTIATE | APPLIED | VERIFIED |
| 334 | Draw the normal to y = x² at x = 1 | DIFFERENTIATE | APPLIED | VERIFIED |
| 335 | Show the tangent slope at the selected point | DIFFERENTIATE | APPLIED | VERIFIED |
| 336 | Find the stationary points of x³ - 3x | DIFFERENTIATE | APPLIED | VERIFIED |
| 337 | Where is the slope zero on this curve? | DIFFERENTIATE | APPLIED | VERIFIED |
| 338 | Show dy/dx for y = cos(x) | DIFFERENTIATE | APPLIED | VERIFIED |
| 339 | Differentiate y = 1/x | DIFFERENTIATE | APPLIED | VERIFIED |
| 340 | Calculate the second derivative of x⁴ | DIFFERENTIATE | APPLIED | VERIFIED |
| 341 | Plot f prime for f(x) = x² - 4 | DIFFERENTIATE | APPLIED | VERIFIED |
| 342 | Show how the tangent changes as x moves | DIFFERENTIATE | APPLIED | VERIFIED |
| 343 | Calculate derivative at x = 0 for sin(x) | DIFFERENTIATE | APPLIED | VERIFIED |
| 344 | Find critical points of y = x⁴ - 2x² | DIFFERENTIATE | APPLIED | VERIFIED |
| 345 | What is the instantaneous gradient here? | DIFFERENTIATE | APPLIED | VERIFIED |
| 346 | Draw tangents at x = -1 and x = 1 to x² | DIFFERENTIATE | APPLIED | VERIFIED |
| 347 | Differentiate sqrt(x) where defined | DIFFERENTIATE | APPLIED | VERIFIED |
| 348 | Show derivative of tan(x) excluding singularities | DIFFERENTIATE | APPLIED | VERIFIED |
| 349 | Calculate the slope at x = 5 on y = 2x + 3 | DIFFERENTIATE | APPLIED | VERIFIED |
| 350 | Explain why absolute value is not differentiable at zero | DIFFERENTIATE | APPLIED | VERIFIED |
| 351 | Integrate x² from zero to three | INTEGRATE | APPLIED | VERIFIED |
| 352 | Find area under sin(x) between 0 and π | INTEGRATE | APPLIED | VERIFIED |
| 353 | Calculate ∫ cos(x) dx from 0 to π/2 | INTEGRATE | APPLIED | VERIFIED |
| 354 | Shade under y = x from x = 0 to x = 4 | INTEGRATE | APPLIED | VERIFIED |
| 355 | Evaluate the integral of 2x from 1 to 5 | INTEGRATE | APPLIED | VERIFIED |
| 356 | Find the signed area for y = x from -1 to 1 | INTEGRATE | APPLIED | VERIFIED |
| 357 | Show the area between y = x and y = x² on [0,1] | INTEGRATE | APPLIED | VERIFIED |
| 358 | Integrate e^x between 0 and 1 | INTEGRATE | APPLIED | VERIFIED |
| 359 | Find the area beneath y = 4 for x from 0 to 3 | INTEGRATE | APPLIED | VERIFIED |
| 360 | Use a Riemann sum for x² on [0,2] | INTEGRATE | APPLIED | VERIFIED |
| 361 | Visualize trapezoid-rule approximation of sin(x) over [0,π] | INTEGRATE | APPLIED | VERIFIED |
| 362 | Calculate the net integral of x³ from -2 to 2 | INTEGRATE | APPLIED | VERIFIED |
| 363 | Shade the region between x² and 4 | INTEGRATE | APPLIED | VERIFIED |
| 364 | Integrate the selected function | INTEGRATE | CLARIFICATION | VERIFIED |
| 365 | What is the antiderivative of 2x? | INTEGRATE | APPLIED | VERIFIED |
| 366 | Show integral accumulation from 0 to x for sin(t) | INTEGRATE | APPLIED | VERIFIED |
| 367 | Find area enclosed by y = 2x and y = x² | INTEGRATE | APPLIED | VERIFIED |
| 368 | Estimate ∫ 1/(1+x²) dx from -1 to 1 | INTEGRATE | APPLIED | VERIFIED |
| 369 | Compute the integral of x² from -3 to 0 | INTEGRATE | APPLIED | VERIFIED |
| 370 | Highlight positive and negative area separately on sin(x) over 0 to 2π | INTEGRATE | APPLIED | VERIFIED |
| 371 | Compare left and right Riemann sums for x² | INTEGRATE | CLARIFICATION | VERIFIED |
| 372 | Shade area under the currently selected curve between 1 and 4 | INTEGRATE | APPLIED | VERIFIED |
| 373 | Find the integral of ln(x) from 1 to e | INTEGRATE | APPLIED | VERIFIED |
| 374 | Calculate integral of 3x² from 0 to 2 | INTEGRATE | APPLIED | VERIFIED |
| 375 | What is the absolute area between sin(x) and the x-axis from 0 to 2π? | INTEGRATE | APPLIED | VERIFIED |
| 376 | Plot z = x² + y² | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 377 | Draw saddle z = x² - y² | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 378 | Show a sine wave surface z = sin(x)cos(y) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 379 | Graph the plane z = 2x + y | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 380 | Render a Gaussian hill | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 381 | Plot the cone z = sqrt(x²+y²) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 382 | Draw the surface z = xy | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 383 | Make a 3D ripple z = sin(sqrt(x²+y²)) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 384 | Create a paraboloid z = x²/4 + y²/9 | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 385 | Graph z = 1/(1+x²+y²) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 386 | Draw z = exp(-x²) sin(y) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 387 | Plot z = x³ - 3xy² | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 388 | Graph z = ln(x²+y²) where defined | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 389 | Show surface z = cos(x) + cos(y) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 390 | Plot z = x² - 2y² | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 391 | Render a hemisphere z = sqrt(9-x²-y²) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 392 | Plot the surface z = /x/ + /y/ | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 393 | Display z = sin(xy) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 394 | Show the surface z = x/y without connecting the singularity | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 395 | Create z = x²+y² on the square [-2,2] × [-2,2] | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 396 | Plot z = sin(x)sin(y) as wireframe | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 397 | Show z = -sqrt(x²+y²) | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 398 | Graph z = (x²-y²)/(x²+y²) where defined | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 399 | Create a 3D surface for z = 5 | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 400 | Show two surfaces z = x²+y² and z = 4 together | PLOT_SURFACE_3D | APPLIED | VERIFIED |
| 401 | What is the volume of this sphere? | MEASURE_SOLID | APPLIED | VERIFIED |
| 402 | Calculate the volume of a radius-3 sphere | MEASURE_SOLID | APPLIED | VERIFIED |
| 403 | Find surface area of the selected cube | MEASURE_SOLID | APPLIED | VERIFIED |
| 404 | Volume of a cube with side 5 | MEASURE_SOLID | APPLIED | VERIFIED |
| 405 | Measure the volume of cylinder radius 2 height 8 | MEASURE_SOLID | APPLIED | VERIFIED |
| 406 | What is lateral area of this cone? | MEASURE_SOLID | APPLIED | VERIFIED |
| 407 | Find the total area of a cone with radius 3 and height 4 | MEASURE_SOLID | APPLIED | VERIFIED |
| 408 | Calculate surface area of a sphere radius 7 | MEASURE_SOLID | APPLIED | VERIFIED |
| 409 | How much volume does this cuboid enclose? | MEASURE_SOLID | APPLIED | VERIFIED |
| 410 | Find the diagonal of a cube whose side is 6 | MEASURE_SOLID | APPLIED | VERIFIED |
| 411 | What is the height of the selected cylinder? | MEASURE_SOLID | APPLIED | VERIFIED |
| 412 | Give the radius of this sphere | MEASURE_SOLID | APPLIED | VERIFIED |
| 413 | Calculate cube edge total for side 4 | MEASURE_SOLID | APPLIED | VERIFIED |
| 414 | Compute volume of a cone radius 5 height 12 | MEASURE_SOLID | APPLIED | VERIFIED |
| 415 | Find volume of a hemisphere radius 4 | MEASURE_SOLID | APPLIED | VERIFIED |
| 416 | Show cuboid diagonal for 3 by 4 by 12 | MEASURE_SOLID | APPLIED | VERIFIED |
| 417 | Calculate pyramid volume for base area 20 and height 9 | MEASURE_SOLID | APPLIED | VERIFIED |
| 418 | Compare the volumes of the cube and sphere | MEASURE_SOLID | APPLIED | VERIFIED |
| 419 | Show surface area and volume together for this cylinder | MEASURE_SOLID | APPLIED | VERIFIED |
| 420 | Calculate sphere diameter given radius 2.5 | MEASURE_SOLID | APPLIED | VERIFIED |
| 421 | What is the slant height of this cone? | MEASURE_SOLID | APPLIED | VERIFIED |
| 422 | Find base area of a radius-6 cylinder | MEASURE_SOLID | APPLIED | VERIFIED |
| 423 | Volume of a rectangular prism 2×3×10 | MEASURE_SOLID | APPLIED | VERIFIED |
| 424 | How many faces, edges and vertices does the cube have? | MEASURE_SOLID | APPLIED | VERIFIED |
| 425 | Verify Euler formula on the selected convex polyhedron | MEASURE_SOLID | APPLIED | VERIFIED |
| 426 | Rotate the cube 45 degrees around x | TRANSFORM_3D | APPLIED | VERIFIED |
| 427 | Spin the selected object around the y-axis | TRANSFORM_3D | APPLIED | VERIFIED |
| 428 | Move the sphere 3 units up | TRANSFORM_3D | APPLIED | VERIFIED |
| 429 | Make this cylinder twice as tall | TRANSFORM_3D | APPLIED | VERIFIED |
| 430 | Scale the cube by 1.5 | TRANSFORM_3D | APPLIED | VERIFIED |
| 431 | Turn the cone 90 degrees about z | TRANSFORM_3D | APPLIED | VERIFIED |
| 432 | Move the newest solid five units left | TRANSFORM_3D | APPLIED | VERIFIED |
| 433 | Double the size of the selected sphere | TRANSFORM_3D | APPLIED | VERIFIED |
| 434 | Slide the cube towards positive y by 4 | TRANSFORM_3D | APPLIED | VERIFIED |
| 435 | Make this sphere transparent | TRANSFORM_3D | APPLIED | VERIFIED |
| 436 | Change the pyramid to wireframe | TRANSFORM_3D | APPLIED | VERIFIED |
| 437 | Rotate both solids together around z | TRANSFORM_3D | APPLIED | VERIFIED |
| 438 | Mirror this object across the yz-plane | TRANSFORM_3D | APPLIED | VERIFIED |
| 439 | Reset the cube orientation | TRANSFORM_3D | APPLIED | VERIFIED |
| 440 | Move the torus center to (0,0,5) | TRANSFORM_3D | APPLIED | VERIFIED |
| 441 | Duplicate the sphere and shift the new one right | TRANSFORM_3D | APPLIED | VERIFIED |
| 442 | Stretch the cuboid along the z-axis | TRANSFORM_3D | APPLIED | VERIFIED |
| 443 | Give the sphere 30 percent opacity | TRANSFORM_3D | APPLIED | VERIFIED |
| 444 | Rotate camera to look down from above | TRANSFORM_3D | APPLIED | VERIFIED |
| 445 | Show the cube from an isometric angle | TRANSFORM_3D | APPLIED | VERIFIED |
| 446 | Orbit the camera 60 degrees clockwise | TRANSFORM_3D | APPLIED | VERIFIED |
| 447 | Zoom closer to the selected cylinder | TRANSFORM_3D | APPLIED | VERIFIED |
| 448 | Explode the faces of the cube | TRANSFORM_3D | APPLIED | VERIFIED |
| 449 | Put the exploded cube back together | TRANSFORM_3D | APPLIED | VERIFIED |
| 450 | Undo that last 3D rotation | TRANSFORM_3D | APPLIED | VERIFIED |
| 451 | Make the last graph blue | EDIT_SCENE | APPLIED | VERIFIED |
| 452 | Color the selected triangle red | EDIT_SCENE | APPLIED | VERIFIED |
| 453 | Hide the coordinate grid | EDIT_SCENE | APPLIED | VERIFIED |
| 454 | Show the x and y axes | EDIT_SCENE | APPLIED | VERIFIED |
| 455 | Label the circle as C1 | EDIT_SCENE | APPLIED | VERIFIED |
| 456 | Thicken the red line | EDIT_SCENE | APPLIED | VERIFIED |
| 457 | Make the plotted curve dashed | EDIT_SCENE | APPLIED | VERIFIED |
| 458 | Put a label next to point A | EDIT_SCENE | APPLIED | VERIFIED |
| 459 | Hide measurements on the triangle | EDIT_SCENE | APPLIED | VERIFIED |
| 460 | Highlight the endpoints of AB | EDIT_SCENE | APPLIED | VERIFIED |
| 461 | Clear the canvas | EDIT_SCENE | APPLIED | VERIFIED |
| 462 | Delete the last object | EDIT_SCENE | APPLIED | VERIFIED |
| 463 | Duplicate the selected circle | EDIT_SCENE | APPLIED | VERIFIED |
| 464 | Rename the current scene to My Experiment | EDIT_SCENE | APPLIED | VERIFIED |
| 465 | Save the current construction | EDIT_SCENE | APPLIED | VERIFIED |
| 466 | Undo the last change | EDIT_SCENE | APPLIED | VERIFIED |
| 467 | Redo what I just undid | EDIT_SCENE | APPLIED | VERIFIED |
| 468 | Reset zoom to fit all objects | EDIT_SCENE | APPLIED | VERIFIED |
| 469 | Center the viewport on point B | EDIT_SCENE | APPLIED | VERIFIED |
| 470 | Make the mesh 50 percent see-through | EDIT_SCENE | APPLIED | VERIFIED |
| 471 | Show only the second graph | EDIT_SCENE | APPLIED | VERIFIED |
| 472 | Remove labels from all points | EDIT_SCENE | APPLIED | VERIFIED |
| 473 | Change the selected line to green | EDIT_SCENE | APPLIED | VERIFIED |
| 474 | Show a darker graph grid | EDIT_SCENE | APPLIED | VERIFIED |
| 475 | Restore the default scene colors | EDIT_SCENE | APPLIED | VERIFIED |
| 476 | Draw a graph | DIALOGUE_CONTEXT | CLARIFICATION | VERIFIED |
| 477 | Make it larger | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 478 | Rotate that | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 479 | What about the other one? | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 480 | Use the same radius as before | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 481 | No, make it 7 instead | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 482 | Actually I meant the blue one | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 483 | Both of them | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 484 | Just the first one | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 485 | Cancel that | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 486 | Continue where we stopped | DIALOGUE_CONTEXT | CLARIFICATION | VERIFIED |
| 487 | What choices do I have? | DIALOGUE_CONTEXT | CLARIFICATION | VERIFIED |
| 488 | Make a triangle | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 489 | Draw it over there | DIALOGUE_CONTEXT | CLARIFICATION | VERIFIED |
| 490 | Use x squared for the equation | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 491 | Choose the second suggested answer | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 492 | Explain the shape we are viewing | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 493 | Compare this with the previous graph | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 494 | Start again with a blank canvas | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 495 | Can we make the angle 60 degrees? | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 496 | Now show the measurements | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 497 | Use centimeters instead | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 498 | Do not change the existing cube | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 499 | Why did that fail? | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
| 500 | Show me the drawing step by step | DIALOGUE_CONTEXT | APPLIED | VERIFIED |
Circle construction canvas verification: True. The 25 source circle rows have independent numeric and constraint checks. Emulator checks cover named-center clarification, live triangle incircle/circumcircle, parent edits, tangent circles, red rendering and persistence. See [triangle circles](cycle-72-device/circle-triangle-after-parent-drag.png) and [tangent circles](cycle-72-device/circle-tangent-after-parent-drag.png).


Cycle 13 retained an emulator failure: the parser assigned the word centered as a circle label in a red-circle request. Cycle 14 excludes grammar words from optional labels and adds a JVM regression assertion; see [failed instrumentation](cycle-13-device.log).
Triangle canvas verification: True. All 25 original triangle rows are checked against side, angle, point-label, placement, similarity and modification constraints by an independent coordinate oracle. Native construction reports all side lengths and interior angles. Tests cover renamed labels, live copies, invalid constraints, atomic missing-point replies, undo/redo and save/load. See [equilateral](cycle-72-device/triangle-equilateral.png), [right triangle](cycle-72-device/triangle-right.png), and [edited congruent copy](cycle-72-device/triangle-congruent-after-parent-drag.png).

Triangle context fixtures reserve requested coordinate labels without changing original point coordinates; fixed-side row 268 supplies an existing AB of length 6, while mismatching existing lengths are rejected. Selected-triangle edits retain its ID and first side and add a dependent third control, preserving original shared points. The default Draw a triangle retains the existing equilateral side-2 construction; other under-specified constructions use deterministic choices. Conditional clarification policies outside the tested cases remain incomplete.

Cycle 15 pre-training regression caught a default-shape compatibility regression: Draw a triangle must retain the existing equilateral side-2 construction. Cycle 16 preserves that default; the original 1100-conversation expectations are unchanged. See [failed pre-test](pre-cycle-15-test.log) and retained XML in cycle-15-failed-pretest/.
Function canvas verification: True. All 25 original function-plotting rows have independent mathematical identities, domain and branch checks against the native typed engine used by the canvas. The app plots named functions, explicit/implicit, polar, parametric, restricted, upper-circle and piecewise definitions; no pixel or formula approximation is learned. Native checks cover the equation suggestion chip, domain save/load, the vertical line, floor steps, all requested curve types and a two-finger pan that preserves the function definitions. See [restricted parabola](cycle-72-device/function-restricted.png), [vertical line](cycle-72-device/function-vertical.png), [polar rose](cycle-72-device/function-polar.png), [piecewise](cycle-72-device/function-piecewise.png), [floor](cycle-72-device/function-floor.png), and [panned axes](cycle-72-device/function-after-pan.png).

Development retained two failures: grid-aligned x=4 vanished in marching squares, and the evaluator read scientific notation as implicit multiplication by e. Native grid-edge contours and scientific numeric tokens are now handled explicitly. Adaptive sampling stops unresolved discontinuities and floor/ceil jumps, including at large coordinate values. Named function assignments update a uniquely matching name while preserving its stable ID, appearance and unrelated curves. Full conditional policy coverage remains incomplete.

The post-cycle-17 follow-up additionally checks generalized piecewise jumps (including large constant offsets) and restricted rational poles. Piecewise branches use lazy native conditions with adaptive refinement; restricted curves clip independently sampled continuous segments to their predicate. The 25 original plotting expectations and all source parameters are unchanged. See [piecewise jump](cycle-72-device/function-piecewise-jump.png) and [restricted pole](cycle-72-device/function-restricted-pole.png).

Cycle 18 stopped before training because the legacy equation-stripping helper mistook a comparison inside a lazy conditional for an equation assignment. Typed samplers now pass explicit y= assignments to the adaptive engine so the conditional remains intact. The failed pre-test and XML are retained.
Intersection canvas verification: True. All 25 original intersection rows have independent closed-form coordinate/count/selection checks and unchanged source slots. Queries bind current function/shape IDs; stored query records recompute markers from current definitions, coordinates and the actual measured canvas aspect. All-current-graph queries include newly visible curves. Tangencies deduplicate; original curve/domain residual checks reject poles and extraneous squared roots. Native checks cover the visible pair chip, an edit moving a crossing from (1,1) to (2,4), circles following changed parent controls, tangency, save/load and panning markers outside the viewport. See [pair prompt](cycle-72-device/intersection-pair-question.png), [crossings](cycle-72-device/intersection-crossings.png), [edited curve](cycle-72-device/intersection-after-curve-edit.png), [circles](cycle-72-device/intersection-circles-after-parent-edit.png), and [panned view](cycle-72-device/intersection-after-pan.png).

Rows 314/315 supply valid named native circles and a line in shared scene state; their original target parameters are unchanged. Inline equations display temporary reference curves without creating duplicate scene objects. Selection prompts persist stable encoded IDs and reject stale scene replies. Queries preserve source objects and create one reversible annotation transaction. Original valid-context actions are verified; full missing-context and follow-up coverage across all 500 rows remains incomplete.
Development retained a compile import failure and one routing failure: Find x where ... was parsed but missed by the language acceptance guard. The guard now admits that equation form without memorizing its coefficients.

Current intersection type coverage includes explicit/piecewise functions, vertical lines, native named lines/circles, and implicit equations against explicit functions or vertical lines. Implicit/implicit, implicit/native-circle, polar/parametric and restricted implicit contexts remain explicit requirement gaps. The current verified-row figure counts independently verified source-fixture behaviors, not exhaustive context-type or follow-up completion.

Cycle 21 checks polite Ruhi-prefixed instructions, numeric precision phrasing and marker deletion. The language gate admits intersection queries and dedicated selection/clear commands; unrelated move/delete actions are not reinterpreted as intersection queries.

Cycle 72 adds implicit/explicit and implicit/vertical intersections. Candidates are normalized for equation magnitude and validated against the original implicit equation and explicit domain. Vertical queries use the actual visible Y range. Tests cover changed circle-equation coefficients, tangencies, positive-domain filtering, poles, scaled equations and save/load. Native canvas proof: True. See [implicit curve](cycle-72-device/intersection-implicit.png), [parent edit](cycle-72-device/intersection-implicit-after-edit.png) and [vertical line](cycle-72-device/intersection-implicit-vertical.png). This extends context coverage; it does not increase the verified original-row count or certify all curve combinations.

Cycle 23 visual review caught blank implicit screenshots despite numeric passes: the mounted camera ignored a workspace import after panning. External viewport changes now synchronize with the canvas, while gesture acknowledgements preserve the current camera. Native assertions require blue marker pixels at independently projected coordinates for implicit/explicit, changed-parent and implicit/vertical scenes. See [retained failed visual review](cycle-23-visual-review.md).

Cycle 24 retained a pixel assertion failure caused by reading mutable viewport-aspect metadata after capture. Pixel expectations now derive from each captured bitmap and the requested center/zoom, independently of production marker coordinates. See [retained failure analysis](cycle-24-visual-review.md).

Derivative valid-context behaviors verified: 25/25. Symbolic formulas, first/second derivatives, tangent/normal lines, evaluation slopes and polynomial stationary points use deterministic native mathematics. Source function IDs remain stable and annotations recompute after edits. Independent oracle identities cover the original row parameters; the live tangent-slider request reuses the existing native Trace x slider, tested at two x positions. Native derivative overlay, parent edit, live slider, tangent, normal, absolute-value explanation and persistence proof: True. See [derivative](cycle-72-device/derivative-curve.png), [parent edit](cycle-72-device/derivative-after-parent-edit.png), [tangent](cycle-72-device/derivative-tangent.png), [normal](cycle-72-device/derivative-normal.png). General nonsmooth critical-point analysis, selected-point dependency coverage and exhaustive follow-up policies remain requirements; valid fixture actions do not certify them.

See the [derivative context completion audit](derivative-context-audit.md) for remaining requirements beyond the valid-context source fixtures. Development retained routing failures for normalized slope-zero and Show derivative phrasing, plus the private panel-property compile failure. Cycle 28 retained a missing generated D8 dex output failure before emulator installation; no failed build is counted as native proof.

Cycle 29 retained a Compose screenshot callback timeout in the existing intersection test. Canvas captures now crop the displayed emulator frame to actual semantics bounds, preserving all coordinate and pixel gates without the fixed two-second PixelCopy callback deadline. See [retained failure](cycle-29-render-capture-failure.md).

Cycle 30 visual review found that opening the existing Graph Controls for the tangent slider also enabled an unrelated transformation-preview parabola. Ruhi tangent-slider mode now suppresses that preview while preserving the actual selected curve, live tangent and existing controls. The subsequent fresh cycle retains the same mathematical checks and captures the corrected slider view.

Integration source behaviors verified: 25/25. This includes 23 direct actions and 2 explicitly required bounds clarifications, with their resumed integrals independently checked. The total source-behavior count includes those two correct questions, not 25 invented interval executions. Original source slots are unchanged. Signed/absolute and positive/negative areas use checked native quadrature; 12-panel Riemann and trapezoid estimates remain separate. Tests verify closed-form primitives, intersection-derived bounds, arbitrary coefficients, domain rejection, stable selected-curve references, persistence, undo/redo, pending restore/resume/cancel and stale-scene rejection. Native proof: True. See [bounds question](cycle-72-device/integral-bounds-question.png), [live parent edit](cycle-72-device/integral-after-parent-edit.png), [between curves](cycle-72-device/integral-between-curves.png), [sign colors](cycle-72-device/integral-split-signs.png), [Riemann panels](cycle-72-device/integral-riemann.png), [trapezoids](cycle-72-device/integral-trapezoid.png), [accumulation](cycle-72-device/integral-accumulation.png). General improper integrals, oscillatory certification, broader function types/named pairs and exhaustive context/policy coverage remain requirements. Development retained a test-source syntax failure and the signed-area routing failure.

Cycle 32 retained a stale UIAutomation frame: the intersection image showed the previous diagonal after a vertical-line edit, and the independent marker-pixel gate correctly failed. Canvas capture now waits for a committed draw and PixelCopies the current window buffer with a dedicated callback thread and bounded 15-second waits. This preserves all mathematical/pixel gates while addressing both stale compositor frames and the earlier fixed two-second screenshot timeout. See [retained failure](cycle-32-stale-frame-failure.md).

3D surface source behaviors verified: 25/25. Independent native vertex identities check all original equations, requested bounds, hemisphere support and wireframe style at densities 13 and 24. Two-surface creation is one reversible transaction and preserves existing layers. The shared native mesh path respects independent x/y domains; denominator sign checks and finite/removable-limit probes prevent sampled cells from bridging detected poles, including a shifted pole between finite vertices. Sinc removable-hole behavior remains covered by existing regressions. Native proof: True. See [paraboloid](cycle-72-device/surface-paraboloid.png), [wireframe](cycle-72-device/surface-wireframe.png), [hemisphere](cycle-72-device/surface-hemisphere.png), [pole](cycle-72-device/surface-pole.png), [paired layers](cycle-72-device/surface-paired.png), [native parent edit](cycle-72-device/surface-after-parent-edit.png). Development retained the 3xy-squared tokenizer failure; the parser now inserts coordinate multiplication with correct exponent precedence. Finite sampling is not a proof for arbitrarily small/discontinuous features; broader domain/context requirements and the remaining workbook families remain incomplete.

Cycle 34 visual review found the hemisphere's sampled rim was truncated by whole-cell rejection. Finite domain-boundary triangles now clip to convergent edge limits; poles and isolated nonremovable holes cannot acquire caps. The independent oracle and native test require at least 12 distinct rim coordinates on x^2+y^2=9 with z approximately zero, in addition to every vertex satisfying the original sphere equation. The fresh cycle includes the prior pole and sinc regressions.

Solid measurement source behaviors verified: 25/25. Independent closed-form checks cover volume, total/lateral/base area, dimensions, diagonals, edge totals, comparisons, native cube topology and Euler counts. Values use current native dimensions; formulas and units remain deterministic. Missing scene references use actual object chips; missing numeric dimensions accumulate across replies. Literal dimensions produce a labeled reference drawing without creating scene solids. Native proof: True. See [sphere](cycle-72-device/solid-measure-sphere.png), [native radius edit](cycle-72-device/solid-measure-after-radius-edit.png), [cone reference](cycle-72-device/solid-measure-cone.png), [topology](cycle-72-device/solid-measure-topology.png), [Euler](cycle-72-device/solid-measure-euler.png). Solid records have positional references, not stable IDs: changes to scene size, a referenced type, or an unreferenced object invalidate the annotation conservatively. Reordering indistinguishable same-type referenced solids cannot be distinguished from dimension edits; stable solid IDs and full contextual policy coverage remain requirements. UI layout/theme and lessons are unchanged. See [remaining measurement context requirements](solid-measure-context-audit.md).

3D transform source behaviors verified: 25/25. Independent source-slot checks validate native poses, dimensions, handedness, shared-pivot motion, style and camera state. The visible emulator test checks changed pixels for rotation/scale, opacity, wireframe, separated/reassembled logical faces, camera presets and two committed animation frames, plus stopping motion and visible rotation-specific undo after scaling. See [transformed solids](cycle-72-device/transform-rotated-scaled.png), [opacity](cycle-72-device/transform-opacity.png), [wireframe](cycle-72-device/transform-wireframe.png), [exploded faces](cycle-72-device/transform-exploded.png), [top camera](cycle-72-device/transform-camera-top.png), [first group frame](cycle-72-device/transform-group-first.png), [second group frame](cycle-72-device/transform-group-second.png). Workbook fixtures supply the required pyramid, torus, cuboid and selected objects. Row 449 begins exploded; row 450 has a genuine recorded prior rotation. Scoped rotation undo preserves later independent field edits. Full positional-reference, hierarchy, mesh-picking and dialogue policies remain in progress; these source fixtures do not certify every interaction. See [remaining transformation contexts](transform3d-context-audit.md).

Scene-edit source behaviors verified: 25/25. Original engine ALL means the active compatible workspace: graph rows run in Graph2D, mesh opacity in Graph3D, and geometric references in Geometry2D. Source parameters and expectations are unchanged. Native proof checks actual colors, line widths/dashes, axes/grid, labels, endpoint highlights, hidden measurement text, viewport centering/fit, independent circle controls, clear/undo and a project saved through the existing library then reopened in an isolated SQLite database. See [geometry labels](cycle-72-device/scene-circle-label.png), [endpoint highlights](cycle-72-device/scene-endpoints.png), [circle copy](cycle-72-device/scene-circle-copy.png), [graph styles](cycle-72-device/scene-graph-dashed.png), [solo graph](cycle-72-device/scene-graph-solo.png), [mesh opacity](cycle-72-device/scene-mesh-opacity.png). Geometry/formulas and native IDs are preserved by style edits. The source corpus does not certify every cross-engine interaction; remaining requirements are in [the scene-edit context audit](scene-edit-context-audit.md).

Cycle 40 retained two native regression failures: an empty curve list after a stale earlier Plotted reply satisfied a test gate, and Compose measure/layout reentrancy in the legacy capture path. Function verification now requires the current dialogue turn and a native function before proceeding; scene-edit gates also correlate the current turn. All canvas screenshots, including the legacy 3D test, use the shared committed-window PixelCopy helper without forcing a Compose image measurement. Mathematical, persistence and pixel assertions remain intact. The focused function and legacy checks passed before this fresh full cycle. See [retained device failures](cycle-40-device.log).

Cycle 41 retained an angle-command failure caused solely by keyboard-driven Geometry2D viewport-aspect metadata changing during asynchronous preparation. The layout-only recovery now recomputes every intent against current authoritative state instead of being limited to scene edits. Real scene edits, zoom changes, selection changes and workspace visibility changes still reject stale preparation. The fresh cycle retains all 21 native tests and the same mathematical and pixel assertions. See [retained angle failure](cycle-41-device.log) and [context audit](scene-edit-context-audit.md).

Dialogue foundations: saved pending choices retain the displayed labels and exact bound actions, including composite comparison answers. Ordinal follow-ups and equation completion resume the original task; stored conversation preserves the student's words. Actual validation/unsupported errors and asynchronous preparation rejection reasons survive restoration. JVM tests cover first/both selection with one undo, opaque circle IDs, unchanged inactive geometry and error explanation. The visible assistant resumes a restored equation and selects the second restored comparison choice while preserving its earlier graph: True. This is additional contextual evidence; the 25 DIALOGUE_CONTEXT source rows remain unverified until their complete fixture/oracle and native requirements are implemented. See [equation](cycle-72-device/dialogue-equation-resumed.png), [comparison](cycle-72-device/dialogue-comparison-resumed.png) and [remaining requirements](dialogue-context-audit.md).

Typed numeric dialogue: schema 4 retains validated scalar roles by workspace module, including actual native circular-solid radii; no model supplies these values. A correction fills the original pending dimension via its existing validator, preserving the explicit other dimensions. Missing prior radius asks for radius; invalid negative answers retain the original pending action. Three unseen prior radii and independent cone/cylinder volume identities pass JVM checks. Visible restored-radius reuse and height correction preserve the existing sphere: True. See [radius](cycle-72-device/dialogue-prior-radius.png) and [height](cycle-72-device/dialogue-corrected-height.png). Broader pending creation/correction kinds, unit conventions, original dialogue fixtures and remaining context requirements remain incomplete.

Cycle 44 retained two native test failures: missing Compose hierarchies during intersection and graph-style input actions. Both completed in a focused two-test rerun. These helpers now wait for the actual input hierarchy before each input phase, and require the current dialogue turn before accepting a response. Missing roots are observable as an empty collection during the bounded readiness wait; execution, mathematical state, native pixels and the existing 30-second timeout remain asserted. This addresses a test readiness gap; the original failures are retained in [cycle 44](cycle-44-device.log). The fresh full cycle remains authoritative for delivery.

Conversational angle editing moves the native arm point through Unified2DMathController, preserving both anchors and arm length while recomputing dependent points. The existing arc measures current points; no cached degree label replaces geometry. Six JVM methods cover arbitrary angles and clockwise sides, exterior/oriented modes, actual choice restoration/staleness, independent acos checks, save/load, undo/redo, missing construction and locked/dependent arm rejection. The visible 60-degree edit changes native pixels and a midpoint; a subsequent parent edit changes the same live arc: True. See [before](cycle-72-device/dialogue-angle-before.png), [60 degrees](cycle-72-device/dialogue-angle-sixty.png) and [parent edit](cycle-72-device/dialogue-angle-parent-edit.png). Editing a dependent arm through its construction remains a separate context requirement. No permanent angle lock is added. All 25 original dialogue rows and broader context policies still require their complete independent audit.

Existing-graph comparison uses saved creation order by stable IDs and the current formulas, preserving graph count and definitions. It reports formulas and a deterministic sample comparison at x=0, including an undefined-domain response where appropriate; it is not a complete symbolic equivalence or global graph analysis. Six JVM methods cover rename/reorder, formula edits, read-only history, restored composite choices, stale answers, restricted domains and delete/undo chronology. A visible comparison after native edits/reordering reports differences -4 and then -8 after another parent edit, without replacement plots: True. See [comparison](cycle-72-device/dialogue-previous-graph.png) and [updated formula](cycle-72-device/dialogue-previous-graph-edited.png). Memory schema 5 also retains the actual pending question and answer bindings for resume; earlier schemas remain readable.

Cycle 46 retained one native angle timeout: scene import selected the first arm point, and the new resolver incorrectly required every selected point to be the vertex. The resolver now prefers vertex matches when they exist, otherwise resolves the marked angles in the selected shape/scene. A regression covers the imported-point case and explicit vertex selection among multiple marks. Failure diagnostics retain future reply/state/selection snapshots. A new resume test also exposed natural Move the cube to (...) syntax missing the prior center-to adapter; both forms now use the same validated position action, including actual target clarification and undo. All original mathematical and pixel checks remain in the fresh full suite. See [retained angle failure](cycle-46-device.log). All original dialogue rows and follow-up requirements still need their complete independent audit.

Construction dialogue evaluates native controls and dependencies for explanations, measurement overlays and step order. Native triangle area changes from 9 to 15 after a parent edit, and the same midpoint dependency appears in the steps. Measurement visibility changes actual pixels; a clarified copy uses independent controls centered at (8,4), preserving the original and ordinary undo. The 3D workflow grows the last cube by the existing 1.25 factor, rotates it 45 degrees about the existing default Y axis, moves it to (3,2,1), measures native volume 15.625, and places an independent copy while preserving the other solids. Native 2D proof: True; 3D proof: True. See [measurements](cycle-72-device/construction-measurements-shown.png), [2D copy](cycle-72-device/construction-copy-at-answer.png), [solid measurements](cycle-72-device/construction-solid-measurements.png) and [solid copy](cycle-72-device/construction-solid-copy-at-answer.png). Seventeen JVM methods additionally check opaque object bindings, restored/stale choices, invalid coordinates, dimensional measurements, dependency cycles, one-step undo, preservation of existing group animation, known pronouns after restoration, and direct successful explanation interruptions followed by resumption. Unknown targets still require clarification. Suspended nested questions, comprehensive color/other-object references, unit conversion and preserve-object constraints remain unfinished. Source-row totals are unchanged until all dialogue source contexts have independent oracles.

Cycle 48 was interrupted when the owned emulator disappeared; it is not counted as a passing native run. Cycle 49 rebuilds after correcting mixed measurement labels. One computed value now feeds both canvas paint and accessibility: triangle area 9 / perimeter 14.485281, then area 15 / perimeter 17.661904 after the parent edit. Nine JVM methods cover native shape quantities, signed segment components, vertical slopes, circle controls, group bounds and small/large finite formatting. Native assertions check exact displayed values and changed pixels. Layout and lessons remain unchanged.

Color-target correction resolves the actual pending candidate set from native shape styles, graph colors and solid appearance metadata. A single blue match delegates its bound action; multiple or absent matches ask a targeted question with actual choices, and stale geometry rejects the old task. Student wording stays in history and the edit uses ordinary undo. Four JVM methods cover opaque IDs, reversed color ordering, save/load, repeated blue objects, missing context and stale scenes. The visible restored-pending workflow moves only the blue cube to (3,2,1), preserves the red cube, changes native pixels and undoes: True. See [before](cycle-72-device/dialogue-blue-before.png) and [moved](cycle-72-device/dialogue-blue-moved.png). Post-applied target correction, other-object operation inheritance, units and preserve-object constraints remain unfinished. All original dialogue rows still require the complete source fixture/oracle matrix.

Other-object follow-up stores validated operation roles in a bounded, versioned action record, including actual target IDs, scale factors, angles, vectors and measurement kinds. It delegates the operation to existing validators on the actual other object, asks bound choices when several remain, and preserves student wording and ordinary undo. Native scale factor 1.5 is reused after save/load: the first cube remains volume 27 and the other cuboid becomes volume 202.5; native proof: True. See [before](cycle-72-device/dialogue-other-before.png) and [other scaled](cycle-72-device/dialogue-other-scaled.png). Six JVM methods also verify a three-component translation, deterministic current-solid volume, restored ordinal choices, stable 2D IDs after reorder and edited controls, missing context, stale positional solids and invalidation after an unrecorded action. Multi-object/pivot actions are not flattened into a single-target replay. Broader action families, post-applied target correction, units, preserve-object constraints and the complete original dialogue matrix remain unfinished; source totals are unchanged.

Visual review of the other-object before/after screenshots confirms that the yellow cube remains unchanged while the blue cuboid grows. At the existing 2x camera zoom, the cuboid is clipped at the right edge in both captures. The assertions certify native dimensions, volume, undo and visible rendering changes, not complete viewport containment. Automatic camera fitting remains a separate unfinished native 3D control requirement.

Preserve-cube constraints bind the actual pending action and actual protected native cube. Remaining targets are filtered and displayed as bound choices; multiple cubes require a protected-cube choice. The action is validated in private history, then geometry, appearance and effective motion of the cube are compared before one public commit. A contradictory broad edit is discarded without an undo entry, and further contradictory edits stay pending until selection or cancellation. Eight JVM methods cover restoration, actual target choices, two cubes, cube-only contradictions, stale scenes, missing context, private rejection and retained existing animation. A separate regression prevents generic multi-object scaling from becoming a single-target action record. Native proof after restored pending scale: cube/blue appearance unchanged, sphere radius 3 and volume 36*pi, visible pixel change and ordinary undo: True. See [before](cycle-72-device/dialogue-preserve-before.png) and [safe edit](cycle-72-device/dialogue-preserve-scaled.png). The full original dialogue matrix and remaining unit and correction policies still require implementation/verification; source totals are unchanged.

Visual review of preserve-cube captures confirms that the blue cube stays in the same pose and size while the sphere grows. The existing 2x camera view clips part of the enlarged sphere at the right edge; native dimensions, volume and undo are independently asserted, but complete viewport containment is not certified. Automatic native 3D camera fitting remains unfinished.

Geometry units now retain separate base-coordinate and display units per module. An explicit meter-valued context converts lengths by 100, areas by 10000 and volumes by 1000000 for centimeter display, while coordinates, camera, slopes, counts and angles retain their native meaning. Unknown provenance asks one actual base-unit question with restored choices; a legacy unit label alone is not treated as proof of coordinate units. Eleven JVM methods cover powers, restoration/undo, module scoping, current edits, exact nonzero small conversion factors, circles/segments and finite extreme values. Native triangle proof: True (area 60000 cm² / perimeter 1200 cm, then area 75000 after a parent edit). Native cube proof: True (area 240000 cm², volume 8000000 cm³, side 200 cm; after actual native scaling, volume 27000000 cm³). The same quantity strings feed native canvas labels and accessibility; screenshots and numeric assertions supplement each other. See [triangle](cycle-72-device/units-triangle-centimeters.png), [cube](cycle-72-device/units-cube-centimeters.png), and [current cube edit](cycle-72-device/units-cube-parent-edit.png). Native geometry is unchanged by display conversion and ordinary undo restores the prior display. The full original dialogue fixture/oracle matrix, broader correction policies and completion audit remain unfinished; source totals are unchanged.

The centimeter triangle/cube captures and the cube after native scaling were visually reviewed. Verified unit channels are selected 2D geometry and 3D solid quantities/dimension annotations. Point-distance query/annotation channels, explicit physical-unit input suffixes, graph physical-unit contexts and broader follow-up channels still require integration/audit; this result does not certify those paths.

Original dialogue rows 476-500 now run with declared real prerequisite conversations and restored native state. The production manager establishes graph questions, prior dimensions/actions, actual failed commands, graph creation order and target choices; the original workbook utterances and fields are unchanged. All source roles are returned separately from the validated native operation. Independent arithmetic/control checks verify actions, current explanations, construction dependencies, units and preserved objects; questions are resumed through their actual answer bindings. Five JVM matrix methods additionally run 200 current-scene variants of the same original utterances, verify one-step undo/read-only history and reject contextless responses, false applied states and canned answers. These are scene/context checks, not a new NLP generalization benchmark. Native dialogue cases verified: 25/25; source/parameter/snapshot audit: True. [Per-row native verification](cycle-72-dialogue-source-verification.json) retains source fields and hashes of the saved prerequisite/result archives in the native results. This completes the declared valid-context source replay only. Post-applied target correction, other measurement channels, broader engine/context and conditional follow-up policies remain open in the requirement audit; the full goal is not marked complete.

The separate cycle 54 compiled-production audit retained two failures beyond the declared source replay: blue-target correction after an applied edit and centimeter display for point distances. See [retained counterexamples](cycle-54-context-gap-counterexamples.json). The latest compiled audit records 2 still-unverified contexts in context-gap-counterexamples.json; subsequent cycles refresh this before training. These checks supplement the source replay and do not certify all remaining policies.

Point-distance replies now share the geometry length-unit formatter. Their native-coordinate square-root formula is explicitly labeled with its base unit; display and native numeric roles remain distinct. Conversation schema 6 retains only endpoint names/module per geometry workspace, recomputing from current points when display units change; schemas 1-5 remain readable. Eleven JVM methods cover 2D/3D lengths, immediate conversion, restored/current points, module scoping, read-only history, unknown units, reverse conversion, missing references, zero distance and schema-5 compatibility. Native 2D proof: True (600 cm agreeing with the existing selected segment, then 500 cm after a native edit, with unit undo unaffected by a readonly query). Native 3D proof: True (300 cm, then 700 cm using all three edited components). See [2D reply](cycle-72-device/point-distance-answer-2d.png), [segment](cycle-72-device/point-distance-segment-centimeters.png), [3D reply](cycle-72-device/point-distance-answer-3d.png), and [3D edit](cycle-72-device/point-distance-answer-3d-parent-edit.png). The point query remains read-only; it does not create a new annotation object. Existing selected segment measurements and the answer now use consistent units. Broader contextual requirements remain in the requirement audit.

Completed-action target correction native proof: True. A bounded exact before/after snapshot restores the wrong target and replays the validated operation on the actual blue object in private history before one public commit. Ten JVM methods cover move/scale/translation/rotation, restored choices, 2D stable shape IDs, stale geometry/color/order, unit preservation, missing/read-only context, invalid replay, and undo/redo. The emulator reopens an already-applied move, verifies exact restoration of the red cube, the blue cube's original dimensions and appearance, changed canvas pixels, and atomic undo/redo. See [wrong target](cycle-72-device/dialogue-blue-applied-wrong.png), [corrected scene](cycle-72-device/dialogue-blue-applied-corrected.png), and [visible answer](cycle-72-device/dialogue-blue-applied-answer.png). This does not establish every target-correction operation or all conditional follow-up policies.

Selected-point derivative native proof: True. Derivative annotation schema 2 stores an opaque remappable native-point binding, resolves current coordinates, and checks curve membership before computing or rendering. Schema 1 remains readable as a fixed-coordinate annotation. Nine JVM methods cover both original utterances, changed coordinates/formulas, native compaction/deletion/reference restoration, explicit x versus live points, restored off-curve clarification, undo/redo and old-schema compatibility. The emulator verifies a saved annotation moving from (1,1) to (3,9), slope 6, stale-overlay suppression after moving off the curve, and an explicit-x reply after restoring the clarification. Source points/functions are not recreated or projected by the assistant. See [before](cycle-72-device/derivative-point-before.png), [moved](cycle-72-device/derivative-point-moved.png), [question](cycle-72-device/derivative-point-off-curve-question.png), and [explicit x](cycle-72-device/derivative-point-explicit-x.png). Nonsmooth/domain and other derivative follow-up requirements remain open.

The point-binding implementation initially passed numeric checks in cycle 58 but failed visual review: its Compose drawing cache omitted native points and point bindings. See [retained failure](cycle-58-visual-failure.md). The current cache includes both, and the emulator checks the actual canvas drawing payload at (3,9), then its unavailable diagnostic after an off-curve move. See [review](cycle-59-visual-review.md) for the verified update and remaining summary-chip overlap.

Derivative partial-answer retention native proof: True. Dialogue schema 7 adds a bounded pending-slot map while retaining schemas 1-6. Validated curve IDs, point indices and finite x coordinates survive sequential questions and saved sessions; stale scenes reject their bindings. Nine JVM methods cover both original utterances, plain numeric/named replies, both missing-slot orders, invalid x values, selection drift, cancellation, schema-6 compatibility and off-curve choices. The emulator verifies x=2 then a restored curve question yielding slope 12 for the current cubic, plus a restored selected-point/curve question yielding slope 4 for the square without reselecting the point. See [curve question](cycle-72-device/derivative-slots-curve-question.png), [numeric answer](cycle-72-device/derivative-slots-numeric-answer.png), and [point answer](cycle-72-device/derivative-slots-point-answer.png). Broader derivative and engine follow-up requirements remain open.

Derivative domain follow-up native proof: True. Coordinate chips for a known curve are drawn only from examples with a finite two-sided derivative. If a chosen curve invalidates an earlier x answer, Ruhi retains the curve, drops that invalid x and asks for another coordinate. Eight JVM methods cover logarithm/square-root/reciprocal/absolute-value domains, validated chip execution, no usable sampled locations, a nonsmooth selected point, repeated invalid locations, explicit literal rejection, stale choices and saved-curve resumption. The emulator verifies a saved x=0 followed by a logarithm choice, valid positive chips, restored x=2 yielding slope 0.5, and an absolute-value corner followed by x=-1 yielding slope -1 without moving the source point. See [question](cycle-72-device/derivative-domain-log-question.png), [log answer](cycle-72-device/derivative-domain-log-answer.png), and [corner answer](cycle-72-device/derivative-domain-corner-answer.png). This checks offered samples, not global domain discovery or nonsmooth critical-point enumeration.

Distance missing-point policy matrix: True. All 150 original requests are replayed in both native geometry studios across one-missing, both-missing, invalid/cancel and renamed-label workflows: 1200 checked conversations. They verify no unsolicited points, actual endpoint chips, saved-session coordinate replies, deterministic distance 5 for (0,0,0)/(3,4,0), one undo entry per explicit creation, cancellation, and unchanged inactive geometry. Four JVM methods check the same matrix before training. Android additionally verifies all 150 supplied intent predictions with bundled TensorFlow. This proves those conditional workflows, not every possible distance context or broad NLP generalization. See [native row/scenario evidence](cycle-72-device/distance-policy-matrix.json). Existing visible missing-point captures remain the UI evidence.

Cycle 62's direct screenshot captured an old 5-unit reply beside the updated (6,8,0) endpoint. The current test waits for a newly recorded matching dialogue turn and a committed PixelCopy frame. A focused replay visibly captured 10 units and finished cleanly after the owned emulator was cold-started with four cores and 4096 MB RAM following an Android system-server watchdog failure. The failed focused attempt and watchdog records remain retained; see cycle-62-visual-review.md. The full current cycle independently enforces current-turn and committed-capture flags.

Choose-existing-point follow-up proof: True. All 150 original distance utterances run in both geometry studios across one existing replacement, two existing replacements and mixed existing/create workflows: 900 conversations. Explicit endpoint bindings survive saved sessions. Selecting an existing point does not create or rename geometry or add undo entries; mixed creation has one independently undoable creation. Six JVM methods additionally check current-coordinate recomputation, unit replies using the actual chosen pair, ambiguous/stale names, unrelated roles and cancellation. The emulator clicks the restored visible chip and captures current distances 5 and 10 after editing the native endpoint. See [question](cycle-72-device/choose-point-question.png), [answer](cycle-72-device/choose-point-answer-10.png), and [all row/scenario results](cycle-72-device/choose-point-matrix.json). This verifies uniquely named scene-point choices; duplicate names still require disambiguation by renaming, rather than silently choosing a point.

Absolute-value coordinate audit: current-source hash match True; confirmed coordinate counterexamples 0. The original zero case and explicit x=2, x=-3 and plain 'at 2' variations are recorded in [compiled probe evidence](cycle-72-absolute-value-coordinate-audit.json). Known failures are retained in LOOP_STATE.json even when the original 500 valid-context cases pass. This probe checks a necessary coordinate condition and does not independently certify every explanation, derivative domain or rendering behavior.

Installed APK provenance: True. Both the app and instrumentation APK SHA256 values read from emulator-5556 match this cycle's local build bytes. The separate packaged-model checks verify bundled TensorFlow assets and metadata against source files. See [installed/build hashes](cycle-72-installed-apk-verification.json). These checks identify the artifacts used by the runtime suite; they do not replace its mathematical or visible assertions.

Absolute-value explanation native proof: True. Explicit positive/negative and spoken coordinates are validated and retained; a missing coordinate asks one essential question with restored numeric replies. The point marker uses (x, abs(x)). A deterministic exact abs(x) branch rule supplies slopes +1/-1 away from zero; zero retains its unequal one-sided explanation. Six JVM methods cover the original request, corrected false premises, small/subnormal nonzero coordinates, invalid input/cancellation, unrelated curves, saved replies and undo/redo. Native actual-drawing assertions and committed captures verify (2,2), (-3,3) and (0,0), a restored missing-x answer and unchanged unrelated curve data. See [positive answer](cycle-72-device/absolute-value-positive-answer.png), [negative marker](cycle-72-device/absolute-value-negative-marker.png), and [zero answer](cycle-72-device/absolute-value-zero-answer.png). This closes the reproduced explanation-coordinate defect, not broader critical-point/domain analysis.

Lexical point-label head probe: 34/34 correct intents, 34/34 correct raw endpoint mappings and 34/34 complete endpoint mappings above the runtime confidence gate. The head probe alone does not establish wrong whole-assistant actions. Model hashes and per-token evidence are retained in [development probe](cycle-72-lexical-point-label-audit.json). These inspected examples are not an independent language benchmark.

Compiled lexical-label execution probe: 32/32 restored-scene parser/engine cases passed. Unlike the head-only probe, this independently checks mathematical dispatch/state behavior. All declared replay cases pass; broader named-object contexts still require their own evidence. This JVM probe has no Android TensorFlow installation or UI; installed-assistant confirmation remains required. Source hashes, independent expected distances 5 (2D) and 13 (3D), replies and unchanged-point/history assertions are retained in [execution evidence](cycle-72-lexical-point-execution-audit.json).

Lexical/Unicode endpoint integration: True. Explicit point-distance requests are resolved before broad geometry cues. Entity spans are masked by position for intent/role features and compatible pair-operation cues; original label values still resolve against the scene. All 150 distance rows run with 17 lexical/Unicode labels across both geometry studios (5100 JVM cases). Six JVM methods also preserve ordinary slope/midpoint commands and virtual-origin fallback while using actual coordinates for a scene point named origin. Native TensorFlow checks 68 exact label/template/studio cases above confidence thresholds, independent distances 5 and 13, saved scene points and unchanged history. The visible assistant answers distance 5 for the 2D point named slope. See [native cases](cycle-72-device/lexical-point-native.json) and [visible reply](cycle-72-device/lexical-point-distance-answer.png). This is inspected development coverage, not broad unseen-language accuracy. No point-name vocabulary was added to training.

Critical-point semantics probe: 6/6 compiled cases verified. Independent expectations include the original polynomial, finite abs-composition corners (including one with no extremum), smooth abs(x^2), stationary-only and excluded-domain controls. Current omissions are retained as known counterexamples rather than hidden by the 25 valid source fixtures. See [source-hashed probe](cycle-72-critical-point-semantics-audit.json). This is a compiled parser/engine development probe without Android TensorFlow or UI, not full critical-point/domain certification.

Critical-point native proof: True. Six independently specified cases preserve the original polynomial, include finite absolute-value corners (including a non-extremum) and smooth abs(x^2), distinguish stationary points, and exclude a reciprocal pole. The real canvas follows a saved source-function edit from (0,0) to (2,4). Small nonzero symbolic coefficients remain nonzero. See [current curve](cycle-72-device/critical-point-after-edit.png) and [answer](cycle-72-device/critical-point-answer.png). General domain boundaries and non-isolated stationary intervals remain incomplete.

Empty derivative-workspace native follow-up proof: True. Selected derivative/stationary/slider requests ask for an explicit formula when no visible function exists; saved formula chips and plain equations resume the original analysis. Existing x/point roles are retained. An invalid saved x is reasked before committing a new native curve and its linked analysis in one undo transaction. Seven JVM methods cover original source utterances, invalid formulas, cancellation/stale scenes, domain and saved replies; the emulator clicks a restored cubic example and verifies atomic undo/redo, then restores x=0 plus ln(x), reasks x and renders ln(2) with slope 0.5. See [formula question](cycle-72-device/derivative-formula-question.png) and [current answer](cycle-72-device/derivative-formula-answer.png). Broader interrupted/cross-intent and domain coverage remain incomplete.

Empty selected-integral follow-up probe: 2/2 compiled cases verified. Original rows 364 and 372 are independently checked with no curve, a y=x^2 formula reply, saved bounds and expected areas 8/3 on [0,2] and 21 on [1,4]. Failed cases, if any, remain in known_context_counterexamples; plotting a curve alone does not satisfy these actions. See [source-hashed evidence](cycle-72-empty-integral-follow-up-audit.json). This development probe does not run Android TensorFlow or certify UI behavior.

Empty integration-workspace native follow-up proof: True. Original rows 364 and 372 now retain their integral/shading action and any supplied bounds through formula replies and saved sessions. Missing bounds are asked before committing a native curve plus analysis as one undoable action. Invalid formulas or intervals do not create a curve; valid replacement bounds resume. Seven JVM methods verify both rows, independent varied-coefficient areas, reversed/domain intervals, save/restore, cancellation/stale scenes and hidden-curve preservation. Native proof clicks a restored formula example, verifies area 21 and atomic undo/redo, compares shading pixels with the same unshaded native curve, follows an edit to area 63, then recovers the required-bounds request with area 8/3. See [shading](cycle-72-device/integral-formula-shaded.png), [edited curve](cycle-72-device/integral-formula-edited.png) and [answer](cycle-72-device/integral-formula-answer.png). Broader integration/domain and conditional-context requirements remain incomplete.

Mathematical-word formula-reply probe: 6/6 compiled cases verified. Original selected derivative/integration requests are independently checked with formal y=x^2 and equivalent y equals x squared and x squared replies. Failed cases, if any, remain explicit. The independent expectations are slope 4 at x=2 and integral 8/3 on [0,2]. Any failures remain in known_context_counterexamples. See [source-hashed language probe](cycle-72-formula-reply-language-audit.json). This is parser/engine development evidence, not microphone or Android TensorFlow verification.

Mathematical-word formula replies native proof: True. Derivative and integration pending formula replies use shared mathematical-word normalization, while conversation keeps the original wording. Six original-utterance/formula tuples verify native values, with formal controls and saved roles. Seven JVM methods additionally cover varied/implicit coefficients, operators, polite/contextual prefixes, invalid formulas, original domains, undo/redo and unrelated-command interruption. The emulator obtains derivative 11 at x=1.5 for the word reply y equals three x squared plus two x, and shades area 21 on [1,4] for x squared. See [derivative answer](cycle-72-device/formula-word-derivative-answer.png) and [integral shading](cycle-72-device/formula-word-integral-shading.png). This verifies supplied text, not microphone recognition or broad language accuracy.

Endpoint-pair clarification probe: 2/2 compiled cases verified. Original midpoint/slope requests for absent AB are checked through the requested Sun907 and Moon811 endpoint reply. The existing points (0,0) and (6,8) independently require midpoint (3,4) or slope 4/3. Any failures remain in known_context_counterexamples. See [source-hashed endpoint probe](cycle-72-endpoint-pair-follow-up-audit.json). This is compiled parser/engine development evidence without Android TensorFlow or UI.

Endpoint clarification native proof: True. Original rows 151 and 176 retain midpoint/slope purpose after an unresolved segment question. Saved contextual endpoint chips and plain names resolve actual current points without aliases/renaming or substituting a distance intent. Eight JVM methods cover independent midpoint/slope values, multiple unresolved compared segments, current-coordinate reads, lexical/Unicode labels, invalid/ambiguous replies, custom-label/split flags, cancellation, persistence and history. Native proof clicks a restored endpoint pair after a parent move, obtains midpoint (2,3), checks atomic undo/redo, follows another parent edit to (1,2), then gets slope 1.5/rise 6/run 4 from a plain saved reply with no query undo entry. See [midpoint](cycle-72-device/endpoint-pair-midpoint.png), [live edit](cycle-72-device/endpoint-pair-midpoint-edited.png), [slope answer](cycle-72-device/endpoint-pair-slope-answer.png). Broader conditional/context policies remain incomplete.

Midpoint replacement-label probe: 1/3 compiled cases verified. Original row 168 succeeds when M is free, but existing-label and endpoint-then-label contexts lose the requested construction when the student supplies Center907. Expected new coordinates are (0.5,0.5) and (3,4), with the old M point preserved and one construction history entry. These failures remain explicit in known_context_counterexamples. See [source-hashed label probe](cycle-72-midpoint-label-follow-up-audit.json). This is compiled parser/engine development evidence without Android TensorFlow or UI.
