> Historical baseline below. The latest verified run is cycle78 in the [shared architecture report](../ruhi-500/REPORT.md):697 JVM tests and54 emulator tests passed, including a fresh replay of all300 requests. Current outcomes are191 APPLIED,23 CLARIFICATION,6 INVALID and80 UNSUPPORTED. These are audited outcomes, not300 implemented constructions. [Current native results](../ruhi-500/cycle-78-device/geometry300-native.json). Native point-defined planes and point-linked line/segment/ray constructions are independently checked; the baseline below predates them. [Fresh visual review](../ruhi-500/cycle-78-visual-review.md) records saved missing-context questions and explicit instruction completion and retained context/engine gaps.

# Ruhi: 300 kid-style requests in native 3D geometry

Verification: **PASS**

TensorFlow supplied-example intent fit: **300/300**. Every supplied example was training data; this measures fit, not generalization.
Small held-out development check: **17/20**; confidence gate accepted 11, with 0 accepted errors. Related templates remain in training, and this set was inspected during development. It is not broad NLP accuracy.

JVM scene replay: **300/300** expected outcomes; 78 JUnit methods, 0 failures/errors.
Android emulator: **300/300** native TensorFlow intent and scene checks. Rendering was checked separately through the visible assistant on the real geometry canvas; all 300 were not individually screenshot-certified.

| Native outcome | Requests |
|---|---:|
| APPLIED | 186 |
| CLARIFICATION | 23 |
| INVALID | 6 |
| UNSUPPORTED | 85 |

APPLIED includes read-only mathematical answers and selection, as well as edits. Clarifications, rejections and unsupported responses count as safe expected outcomes, not implemented constructions.

The visible assistant creates a cube, changes its side to 5, colors it red, moves it +2 along X and rotates it 90° around Z. The test checks volume 125, rejects a negative sphere radius without adding a solid, and compares canvas pixels.

The saved image was visually inspected. The existing 2× camera view clips part of the translated side-5 cube at the right edge; automatic camera fitting is not implemented by this change (request 300 remains unsupported). Pixel changes certify rendering, not full viewport containment.

## Test context

Each numbered case starts in an explicit fixture, rather than treating this mixed list as one continuous conversation. The default scene contains a side-2 cube at (1,2,3), a 6×4×2 box, radius-3 sphere at (3,2,1), radius-2 height-5 cylinder, radius-3 height-6 cone, side-4 height-7 square pyramid, 2×3×5 triangular prism, and side-2 tetrahedron. A=(2,0,0), B=(0,3,0), C=(0,0,4); vectors AB and AC are present. Creation cases start empty. “Both” cases add a second matching solid; selected cone/pyramid/sphere cases select that target; the blue-color case seeds a blue sphere; the no-cones case removes cones. Undo/redo and load cases seed a prior change/save. See the checked-in audit fixture for exact contexts.

Additional multi-turn tests check independent copies, target preservation, coordinate edits, scale/axis questions and answers, rejection after scene changes, workspace persistence, finite meshes and scoped undo. Questions do not create undo entries.

## Training and runtime

The bundled TensorFlow model proposes a geometry intent. Reviewed language rules extract and validate dimensions, named targets and coordinates; native geometry formulas and meshes produce results. No model generates pixels, formulas, coordinates or arbitrary code. Invalid radius/length inputs keep the scene unchanged. Model confidence never overrides validation. No student conversations are collected or used for training.

Creation defaults are explicit in the response. Unspecified cube side is 2; radius is 1; height is 2. “Bigger” uses 1.25 and “smaller” uses 0.8; ask-first requests wait for a factor/axis. Solid dimensions are limited to 50 units and coordinates to ±10000. Explicit Z movement uses Z. Native solid meshes have their intrinsic height along Y; downward/upside-down turns therefore reverse Y using a half-turn about X.

The existing UI layout and lesson content are unchanged; native appearance mapping was extended for requested colors. Voice hardware/microphone recognition was not certified in this run.

## Remaining engine limitations

Unsupported items remain visible below. They include native line/ray/plane constructions, arbitrary line/plane intersections, slicing and nets, several scene arrangements/comparisons, hide/lock/outline controls, selective color undo, and camera fitting. They were not made to pass by inventing geometry or silently editing a different solid. Save/load stores a native scene snapshot inside the current workspace, not an external file/export.

## Evidence

- [Native results](device/native-300.json) · [Canvas assertions](device/canvas-result.json) · [Model evaluation](model-evaluation.json)
- [Empty canvas](device/empty.png) · [Transformed red cube](device/red-cube-transformed.png)
- [Build log](build.log) · [Device log](device-run.log) · [Source/model hashes](source-manifest.json)

## All supplied requests

| # | Request | Expected outcome | Actual native outcome |
|---:|---|---|---|
| 1 | Can you draw a cube for me? | APPLIED | APPLIED |
| 2 | Make a ball. | APPLIED | APPLIED |
| 3 | Draw a cone like an ice cream cone. | APPLIED | APPLIED |
| 4 | Can you make a cylinder like a tin can? | APPLIED | APPLIED |
| 5 | Draw a box that is longer than it is tall. | APPLIED | APPLIED |
| 6 | Make a pyramid with a square bottom. | APPLIED | APPLIED |
| 7 | Draw a pyramid with a triangle bottom. | APPLIED | APPLIED |
| 8 | Can you draw a triangular prism? | APPLIED | APPLIED |
| 9 | Make a ring like a doughnut. | APPLIED | APPLIED |
| 10 | Draw half a ball. | APPLIED | APPLIED |
| 11 | Make an egg-shaped solid. | APPLIED | APPLIED |
| 12 | Can you draw a capsule shape? | APPLIED | APPLIED |
| 13 | Draw a hollow tube. | UNSUPPORTED | UNSUPPORTED |
| 14 | Make a cone with its top cut off. | APPLIED | APPLIED |
| 15 | Draw a cube with sides of 4 units. | APPLIED | APPLIED |
| 16 | Make a sphere with radius 3. | APPLIED | APPLIED |
| 17 | Draw a cylinder with radius 2 and height 5. | APPLIED | APPLIED |
| 18 | Make a cone with radius 3 and height 6. | APPLIED | APPLIED |
| 19 | Draw a box that is 6 long, 4 wide, and 2 high. | APPLIED | APPLIED |
| 20 | Make a square pyramid with base side 4 and height 7. | APPLIED | APPLIED |
| 21 | Draw a triangular prism that is 5 units long. | APPLIED | APPLIED |
| 22 | Make a sphere with diameter 10. | APPLIED | APPLIED |
| 23 | Draw a cube with side length 2.5. | APPLIED | APPLIED |
| 24 | Make a cylinder that is 8 units tall. | APPLIED | APPLIED |
| 25 | Draw a point at (1, 2, 3). | APPLIED | APPLIED |
| 26 | Put a point at the origin. | APPLIED | APPLIED |
| 27 | Add a point called A at (2, 0, 0). | APPLIED | APPLIED |
| 28 | Add a point called B at (0, 3, 0). | APPLIED | APPLIED |
| 29 | Add a point called C at (0, 0, 4). | APPLIED | APPLIED |
| 30 | Draw a line from A to B. | UNSUPPORTED | UNSUPPORTED |
| 31 | Join B and C with a segment. | UNSUPPORTED | UNSUPPORTED |
| 32 | Draw a ray starting at A and going through C. | UNSUPPORTED | UNSUPPORTED |
| 33 | Make an arrow from the origin to (2, 3, 4). | APPLIED | APPLIED |
| 34 | Draw a plane through A, B, and C. | UNSUPPORTED | UNSUPPORTED |
| 35 | Show me the xy-plane. | UNSUPPORTED | UNSUPPORTED |
| 36 | Draw a plane parallel to the floor at z = 3. | UNSUPPORTED | UNSUPPORTED |
| 37 | Draw a triangle using A, B, and C. | UNSUPPORTED | UNSUPPORTED |
| 38 | Make a tetrahedron using four points. | UNSUPPORTED | UNSUPPORTED |
| 39 | Draw two cubes next to each other. | APPLIED | APPLIED |
| 40 | Make three spheres with different sizes. | APPLIED | APPLIED |
| 41 | How many solids are on my screen? | APPLIED | APPLIED |
| 42 | How many cubes did I draw? | APPLIED | APPLIED |
| 43 | How many spheres are there? | APPLIED | APPLIED |
| 44 | What is this shape called? | APPLIED | APPLIED |
| 45 | Which shape looks like a ball? | APPLIED | APPLIED |
| 46 | Which solid has a pointy top? | APPLIED | APPLIED |
| 47 | Which solid has two circular ends? | APPLIED | APPLIED |
| 48 | Which objects have only flat faces? | APPLIED | APPLIED |
| 49 | Which objects have curved surfaces? | APPLIED | APPLIED |
| 50 | Which objects have both flat and curved surfaces? | APPLIED | APPLIED |
| 51 | How many faces does this cube have? | APPLIED | APPLIED |
| 52 | How many edges does this cube have? | APPLIED | APPLIED |
| 53 | How many corners does this cube have? | APPLIED | APPLIED |
| 54 | How many faces does this box have? | APPLIED | APPLIED |
| 55 | How many edges does this triangular prism have? | APPLIED | APPLIED |
| 56 | How many corners does this square pyramid have? | APPLIED | APPLIED |
| 57 | How many faces does this tetrahedron have? | APPLIED | APPLIED |
| 58 | Does a sphere have any corners? | APPLIED | APPLIED |
| 59 | Does a cylinder have any vertices? | APPLIED | APPLIED |
| 60 | Is this object a cube or a cuboid? | APPLIED | APPLIED |
| 61 | Are all the faces of this cube squares? | APPLIED | APPLIED |
| 62 | Are these two solids the same kind of shape? | CLARIFICATION | CLARIFICATION |
| 63 | Which object has the most vertices? | APPLIED | APPLIED |
| 64 | Which objects have a circular base? | APPLIED | APPLIED |
| 65 | What is the name of the selected object? | APPLIED | APPLIED |
| 66 | What are A’s coordinates? | APPLIED | APPLIED |
| 67 | Where is point B? | APPLIED | APPLIED |
| 68 | Which point is at (1, 2, 3)? | APPLIED | APPLIED |
| 69 | Which point is closest to (2, 3, 4)? | APPLIED | APPLIED |
| 70 | Is any point exactly at the origin? | APPLIED | APPLIED |
| 71 | What is the distance from A to B? | APPLIED | APPLIED |
| 72 | How far is C from the origin? | APPLIED | APPLIED |
| 73 | How far apart are these two points? | CLARIFICATION | CLARIFICATION |
| 74 | What is the midpoint of AB? | APPLIED | APPLIED |
| 75 | Mark the halfway point between A and C. | APPLIED | APPLIED |
| 76 | Which point is highest? | APPLIED | APPLIED |
| 77 | Which point has the smallest x-coordinate? | APPLIED | APPLIED |
| 78 | Which point is closest to the xy-plane? | APPLIED | APPLIED |
| 79 | Is A above the xy-plane? | APPLIED | APPLIED |
| 80 | Is B on the z-axis? | APPLIED | APPLIED |
| 81 | Do A and B have the same height? | APPLIED | APPLIED |
| 82 | Which points have z equal to zero? | APPLIED | APPLIED |
| 83 | How far is A from the x-axis? | APPLIED | APPLIED |
| 84 | How far is B from the yz-plane? | APPLIED | APPLIED |
| 85 | Are A, B, and C on one straight line? | APPLIED | APPLIED |
| 86 | Are these four points on the same plane? | CLARIFICATION | CLARIFICATION |
| 87 | Is A inside this sphere? | UNSUPPORTED | UNSUPPORTED |
| 88 | Is B on the surface of this sphere? | UNSUPPORTED | UNSUPPORTED |
| 89 | Is C inside this box? | UNSUPPORTED | UNSUPPORTED |
| 90 | Which points are outside the selected solid? | UNSUPPORTED | UNSUPPORTED |
| 91 | What is the volume of this cube? | APPLIED | APPLIED |
| 92 | How much space is inside this box? | APPLIED | APPLIED |
| 93 | What is the volume of the sphere? | APPLIED | APPLIED |
| 94 | Find the volume of this cylinder. | APPLIED | APPLIED |
| 95 | How much space does this cone take up? | APPLIED | APPLIED |
| 96 | What is the volume of this square pyramid? | APPLIED | APPLIED |
| 97 | Find the volume of this triangular prism. | APPLIED | APPLIED |
| 98 | What is the total surface area of this cube? | APPLIED | APPLIED |
| 99 | How much wrapping paper would cover this box? | APPLIED | APPLIED |
| 100 | What is the surface area of the sphere? | APPLIED | APPLIED |
| 101 | Find the total surface area of this cylinder. | APPLIED | APPLIED |
| 102 | What is the curved surface area of this cylinder? | APPLIED | APPLIED |
| 103 | Find the curved surface area of this cone. | APPLIED | APPLIED |
| 104 | What is the total surface area of this cone? | APPLIED | APPLIED |
| 105 | What is the area of the pyramid’s base? | APPLIED | APPLIED |
| 106 | How big is one face of this cube? | APPLIED | APPLIED |
| 107 | What is the area of the cylinder’s circular base? | APPLIED | APPLIED |
| 108 | How long is the edge of this cube? | APPLIED | APPLIED |
| 109 | What is the height of this cone? | APPLIED | APPLIED |
| 110 | What is the radius of this sphere? | APPLIED | APPLIED |
| 111 | What is the diameter of this cylinder? | APPLIED | APPLIED |
| 112 | What are this box’s length, width, and height? | APPLIED | APPLIED |
| 113 | How long is the diagonal across this box? | APPLIED | APPLIED |
| 114 | What is the diagonal across one cube face? | APPLIED | APPLIED |
| 115 | How long is the sloping side of this cone? | APPLIED | APPLIED |
| 116 | Where is the center of this sphere? | APPLIED | APPLIED |
| 117 | Where is the center of this box? | APPLIED | APPLIED |
| 118 | What is the distance between the centers of these spheres? | CLARIFICATION | CLARIFICATION |
| 119 | Which solid has the biggest volume? | APPLIED | APPLIED |
| 120 | Which solid has the smallest surface area? | APPLIED | APPLIED |
| 121 | Is this cube bigger than that sphere by volume? | CLARIFICATION | CLARIFICATION |
| 122 | Do these two boxes have the same volume? | CLARIFICATION | CLARIFICATION |
| 123 | How much bigger is this cylinder than that cone? | CLARIFICATION | CLARIFICATION |
| 124 | If their bases and heights match, is the cone one-third of the cylinder? | APPLIED | APPLIED |
| 125 | Can you show the volume formula using this solid’s numbers? | APPLIED | APPLIED |
| 126 | Move the cube 2 units to the right. | APPLIED | APPLIED |
| 127 | Move the sphere 3 units up the z-axis. | APPLIED | APPLIED |
| 128 | Push the cone 4 units along the y-axis. | APPLIED | APPLIED |
| 129 | Move the cylinder 2 units in the negative x direction. | APPLIED | APPLIED |
| 130 | Put the sphere’s center at (3, 2, 1). | APPLIED | APPLIED |
| 131 | Move point A to (4, 5, 6). | APPLIED | APPLIED |
| 132 | Move B one unit closer to the xy-plane. | APPLIED | APPLIED |
| 133 | Put the cube’s center at the origin. | APPLIED | APPLIED |
| 134 | Move all spheres 2 units along the x-axis. | APPLIED | APPLIED |
| 135 | Move both cubes up by 3 units. | APPLIED | APPLIED |
| 136 | Move only the selected solid. | CLARIFICATION | CLARIFICATION |
| 137 | Move the box by (1, 2, 3). | APPLIED | APPLIED |
| 138 | Bring the cone back to where it was. | UNSUPPORTED | UNSUPPORTED |
| 139 | Put the two spheres next to each other without overlap. | UNSUPPORTED | UNSUPPORTED |
| 140 | Move this sphere until it just touches that sphere. | UNSUPPORTED | UNSUPPORTED |
| 141 | Put the cube on the xy-plane. | UNSUPPORTED | UNSUPPORTED |
| 142 | Put the cone on top of the box. | UNSUPPORTED | UNSUPPORTED |
| 143 | Line up the centers of these three spheres. | UNSUPPORTED | UNSUPPORTED |
| 144 | Leave an equal gap between these cubes. | UNSUPPORTED | UNSUPPORTED |
| 145 | Move everything 5 units along the z-axis. | APPLIED | APPLIED |
| 146 | Rotate the cube 90 degrees around the z-axis. | APPLIED | APPLIED |
| 147 | Turn the box 45 degrees around the x-axis. | APPLIED | APPLIED |
| 148 | Spin the cone 180 degrees around the y-axis. | APPLIED | APPLIED |
| 149 | Rotate the selected solid by 30 degrees. | APPLIED | APPLIED |
| 150 | Turn the cube around its own center. | CLARIFICATION | CLARIFICATION |
| 151 | Rotate the triangle around line AB. | UNSUPPORTED | UNSUPPORTED |
| 152 | Rotate point A 90 degrees around the z-axis. | APPLIED | APPLIED |
| 153 | Rotate both cubes together around the origin. | UNSUPPORTED | UNSUPPORTED |
| 154 | Turn the cylinder so it lies along the x-axis. | APPLIED | APPLIED |
| 155 | Make the cone point downward. | APPLIED | APPLIED |
| 156 | Flip the pyramid upside down. | APPLIED | APPLIED |
| 157 | Rotate the box one full turn. | APPLIED | APPLIED |
| 158 | Turn it another 15 degrees. | APPLIED | APPLIED |
| 159 | Turn it back by 15 degrees. | APPLIED | APPLIED |
| 160 | Does rotating this solid change its volume? | APPLIED | APPLIED |
| 161 | Reflect A across the xy-plane. | APPLIED | APPLIED |
| 162 | Flip the cube across the yz-plane. | APPLIED | APPLIED |
| 163 | Reflect the triangle across the xz-plane. | UNSUPPORTED | UNSUPPORTED |
| 164 | Make a mirror copy of this cone across the xy-plane. | UNSUPPORTED | UNSUPPORTED |
| 165 | Reflect the sphere’s center through the origin. | APPLIED | APPLIED |
| 166 | Make this cube twice as big in every direction. | APPLIED | APPLIED |
| 167 | Shrink the sphere to half its size. | APPLIED | APPLIED |
| 168 | Make the cylinder twice as tall. | APPLIED | APPLIED |
| 169 | Make the box wider without changing its height. | APPLIED | APPLIED |
| 170 | Change the cube’s side length to 5. | APPLIED | APPLIED |
| 171 | Change the sphere’s radius to 4. | APPLIED | APPLIED |
| 172 | Set the cylinder’s diameter to 6. | APPLIED | APPLIED |
| 173 | Make the cone’s height 10. | APPLIED | APPLIED |
| 174 | Set the box dimensions to 8 by 5 by 3. | APPLIED | APPLIED |
| 175 | Enlarge the selected solid by a factor of 1.5. | APPLIED | APPLIED |
| 176 | Make every sphere twice its current radius. | APPLIED | APPLIED |
| 177 | Give all cubes a side length of 3. | APPLIED | APPLIED |
| 178 | Keep the cylinder’s radius the same and halve its height. | APPLIED | APPLIED |
| 179 | If I double this cube’s sides, what happens to its volume? | APPLIED | APPLIED |
| 180 | If I double this sphere’s radius, what happens to its surface area? | APPLIED | APPLIED |
| 181 | Color the cube red. | APPLIED | APPLIED |
| 182 | Make the sphere blue. | APPLIED | APPLIED |
| 183 | Change all spheres to green. | APPLIED | APPLIED |
| 184 | Paint every cube orange. | APPLIED | APPLIED |
| 185 | Make only the selected cone purple. | APPLIED | APPLIED |
| 186 | Change all blue solids to yellow. | APPLIED | APPLIED |
| 187 | Give the box a pink outline. | UNSUPPORTED | UNSUPPORTED |
| 188 | Make the cylinder partly transparent. | APPLIED | APPLIED |
| 189 | Make the sphere completely see-through. | UNSUPPORTED | UNSUPPORTED |
| 190 | Show only the edges of the cube. | UNSUPPORTED | UNSUPPORTED |
| 191 | Show the cube’s faces again. | UNSUPPORTED | UNSUPPORTED |
| 192 | Make the selected object’s edges thicker. | UNSUPPORTED | UNSUPPORTED |
| 193 | Change all solid outlines to black. | UNSUPPORTED | UNSUPPORTED |
| 194 | Hide the cone. | UNSUPPORTED | UNSUPPORTED |
| 195 | Show the cone again. | UNSUPPORTED | UNSUPPORTED |
| 196 | Hide every sphere. | UNSUPPORTED | UNSUPPORTED |
| 197 | Show all hidden solids. | UNSUPPORTED | UNSUPPORTED |
| 198 | Hide everything except the selected cube. | UNSUPPORTED | UNSUPPORTED |
| 199 | Lock the box so I cannot move it. | UNSUPPORTED | UNSUPPORTED |
| 200 | Unlock every cube. | UNSUPPORTED | UNSUPPORTED |
| 201 | Rename this sphere Ball. | APPLIED | APPLIED |
| 202 | Call this cube MyCube. | APPLIED | APPLIED |
| 203 | Show the names of all points. | UNSUPPORTED | UNSUPPORTED |
| 204 | Hide the object labels. | UNSUPPORTED | UNSUPPORTED |
| 205 | Select all spheres. | APPLIED | APPLIED |
| 206 | Select the smallest cube. | APPLIED | APPLIED |
| 207 | Select the red cone. | APPLIED | APPLIED |
| 208 | Select both boxes. | APPLIED | APPLIED |
| 209 | Clear my selection. | APPLIED | APPLIED |
| 210 | Make a copy of the selected pyramid. | APPLIED | APPLIED |
| 211 | How long is segment AB? | APPLIED | APPLIED |
| 212 | Which way does vector AB point? | APPLIED | APPLIED |
| 213 | What are the three components of this vector? | APPLIED | APPLIED |
| 214 | How long is this arrow? | APPLIED | APPLIED |
| 215 | Make a unit vector pointing the same way. | APPLIED | APPLIED |
| 216 | Add these two vectors. | APPLIED | APPLIED |
| 217 | Subtract vector AC from vector AB. | APPLIED | APPLIED |
| 218 | What is the angle between these two arrows? | APPLIED | APPLIED |
| 219 | Are these vectors perpendicular? | APPLIED | APPLIED |
| 220 | Are these vectors parallel? | APPLIED | APPLIED |
| 221 | Find the dot product of these vectors. | APPLIED | APPLIED |
| 222 | Find a vector perpendicular to both arrows. | APPLIED | APPLIED |
| 223 | Does point C lie on line AB? | UNSUPPORTED | UNSUPPORTED |
| 224 | Are these two 3D lines parallel? | UNSUPPORTED | UNSUPPORTED |
| 225 | Do these two lines meet? | UNSUPPORTED | UNSUPPORTED |
| 226 | Where do these lines cross? | UNSUPPORTED | UNSUPPORTED |
| 227 | Are these lines skew? | UNSUPPORTED | UNSUPPORTED |
| 228 | What is the shortest distance between these lines? | UNSUPPORTED | UNSUPPORTED |
| 229 | How far is point A from this line? | UNSUPPORTED | UNSUPPORTED |
| 230 | Mark the closest point on the line to A. | UNSUPPORTED | UNSUPPORTED |
| 231 | Draw a line through A parallel to BC. | UNSUPPORTED | UNSUPPORTED |
| 232 | Draw a line through A perpendicular to this plane. | UNSUPPORTED | UNSUPPORTED |
| 233 | Draw a plane through A parallel to the xy-plane. | UNSUPPORTED | UNSUPPORTED |
| 234 | Draw a plane through A, B, and C. | UNSUPPORTED | UNSUPPORTED |
| 235 | What is the equation of this plane? | UNSUPPORTED | UNSUPPORTED |
| 236 | Is point D on this plane? | UNSUPPORTED | UNSUPPORTED |
| 237 | Is this line parallel to the plane? | UNSUPPORTED | UNSUPPORTED |
| 238 | Where does this line go through the plane? | UNSUPPORTED | UNSUPPORTED |
| 239 | How far is A from this plane? | UNSUPPORTED | UNSUPPORTED |
| 240 | Drop a perpendicular from A to the plane. | UNSUPPORTED | UNSUPPORTED |
| 241 | Are these two planes perpendicular? | UNSUPPORTED | UNSUPPORTED |
| 242 | Are these two planes parallel? | UNSUPPORTED | UNSUPPORTED |
| 243 | What is the angle between these planes? | UNSUPPORTED | UNSUPPORTED |
| 244 | Draw the line where these planes meet. | UNSUPPORTED | UNSUPPORTED |
| 245 | Which direction is straight out from this plane? | UNSUPPORTED | UNSUPPORTED |
| 246 | Slice the cube with a horizontal plane. | UNSUPPORTED | UNSUPPORTED |
| 247 | What shape is this cube’s cross-section? | UNSUPPORTED | UNSUPPORTED |
| 248 | Slice the sphere through its center. | UNSUPPORTED | UNSUPPORTED |
| 249 | What is the radius of that circular slice? | UNSUPPORTED | UNSUPPORTED |
| 250 | Move the slicing plane upward by one unit. | UNSUPPORTED | UNSUPPORTED |
| 251 | Can a tilted slice through this cylinder make an ellipse? | UNSUPPORTED | UNSUPPORTED |
| 252 | Cut the cone with a plane parallel to its base. | UNSUPPORTED | UNSUPPORTED |
| 253 | Show the sphere and plane’s intersection. | UNSUPPORTED | UNSUPPORTED |
| 254 | Does this plane touch the sphere at just one point? | UNSUPPORTED | UNSUPPORTED |
| 255 | Draw a tangent plane at this point on the sphere. | UNSUPPORTED | UNSUPPORTED |
| 256 | Are these two spheres touching? | UNSUPPORTED | UNSUPPORTED |
| 257 | Are these spheres overlapping? | UNSUPPORTED | UNSUPPORTED |
| 258 | Is the small sphere completely inside the big sphere? | UNSUPPORTED | UNSUPPORTED |
| 259 | Where do these two spheres intersect? | UNSUPPORTED | UNSUPPORTED |
| 260 | Does this line pass through the sphere? | UNSUPPORTED | UNSUPPORTED |
| 261 | Mark where the line enters and leaves the sphere. | UNSUPPORTED | UNSUPPORTED |
| 262 | Is this line tangent to the sphere? | UNSUPPORTED | UNSUPPORTED |
| 263 | Is the cube completely inside the sphere? | UNSUPPORTED | UNSUPPORTED |
| 264 | Are the box and the plane intersecting? | UNSUPPORTED | UNSUPPORTED |
| 265 | Show me a net of this cube. | UNSUPPORTED | UNSUPPORTED |
| 266 | Draw another one like that. | APPLIED | APPLIED |
| 267 | Make the new one smaller. | APPLIED | APPLIED |
| 268 | No, I meant the first sphere. | APPLIED | APPLIED |
| 269 | Change that one to red too. | APPLIED | APPLIED |
| 270 | Move them together. | CLARIFICATION | CLARIFICATION |
| 271 | Keep the other objects where they are. | CLARIFICATION | CLARIFICATION |
| 272 | Do the same thing to the second cube. | CLARIFICATION | CLARIFICATION |
| 273 | What about the blue sphere? | CLARIFICATION | CLARIFICATION |
| 274 | Tell me its volume now. | APPLIED | APPLIED |
| 275 | Why did that answer change? | CLARIFICATION | CLARIFICATION |
| 276 | Show me the steps for that answer. | CLARIFICATION | CLARIFICATION |
| 277 | Can you explain that more simply? | CLARIFICATION | CLARIFICATION |
| 278 | Which object did you just change? | APPLIED | APPLIED |
| 279 | Did you change both cubes or only one? | CLARIFICATION | CLARIFICATION |
| 280 | Keep A fixed while moving B. | CLARIFICATION | CLARIFICATION |
| 281 | Draw a sphere with radius minus 3. | INVALID | INVALID |
| 282 | Make a cube with side length zero. | INVALID | INVALID |
| 283 | Draw a plane through these three points on one straight line. | INVALID | INVALID |
| 284 | Draw a line using the same point twice. | INVALID | INVALID |
| 285 | Find the distance from A to Z when Z is not on the board. | CLARIFICATION | CLARIFICATION |
| 286 | Change all cones to red when there are no cones. | CLARIFICATION | CLARIFICATION |
| 287 | Color the selected solid blue when I have selected nothing. | CLARIFICATION | CLARIFICATION |
| 288 | Move the locked cube 2 units right. | UNSUPPORTED | UNSUPPORTED |
| 289 | Give this box a height of 1000000000. | INVALID | INVALID |
| 290 | Put a 3D point at (2, 3). | INVALID | INVALID |
| 291 | Make it bigger, but ask me how much first. | CLARIFICATION | CLARIFICATION |
| 292 | Rotate it, but ask me which axis first. | CLARIFICATION | CLARIFICATION |
| 293 | Undo the last thing you did. | APPLIED | APPLIED |
| 294 | Redo that change. | APPLIED | APPLIED |
| 295 | Undo only the last color change. | UNSUPPORTED | UNSUPPORTED |
| 296 | Delete the selected sphere. | APPLIED | APPLIED |
| 297 | Clear all solids but keep my points and lines. | APPLIED | APPLIED |
| 298 | Save this 3D scene. | APPLIED | APPLIED |
| 299 | Load my saved scene and keep the colors and sizes. | APPLIED | APPLIED |
| 300 | Reset the camera so I can see all my objects. | UNSUPPORTED | UNSUPPORTED |

Latest replay: cycle 49 trained-model APK passed the complete 27-test native suite and all 454 JVM regression methods. The original 300-row native replay still separates 188 APPLIED, 23 CLARIFICATION, 6 INVALID and 83 UNSUPPORTED outcomes; 300/300 audit outcomes does not mean 300 implemented actions. See [current native replay](../ruhi-500/cycle-49-device/geometry300-native.json) and [current full report](../ruhi-500/REPORT.md). UI layout and lessons were preserved.

Cycle 50 repeats the 300-command regression within a passing 28-test native suite and 458 JVM tests. The new restored blue-target movement workflow is verified separately; the [full report](../ruhi-500/REPORT.md) retains remaining contextual requirements and distinguishes seed recognition from implemented action coverage.

Cycle 51 again passed the original 300-command regression in a 29-test native suite and 464 JVM tests, with typed other-object action inheritance checked separately. The replay still records 188 APPLIED, 23 CLARIFICATION, 6 INVALID and 83 UNSUPPORTED. See [current report](../ruhi-500/REPORT.md) and [native replay](../ruhi-500/cycle-51-device/geometry300-native.json).

Cycle 52 again replays the original 300 requests within a passing 30-test native suite and 473 JVM tests. The preserve-cube pending-action constraint is verified separately from source-wide coverage. See [current report](../ruhi-500/REPORT.md) and [current native replay](../ruhi-500/cycle-52-device/geometry300-native.json).

Cycle 53 again replays the original 300 requests within a passing 32-test native suite and 484 JVM tests. New unit-display workflows verify current native 2D and 3D geometry separately; this is not a claim that every unsupported construction or every measurement channel is implemented. See [current report](../ruhi-500/REPORT.md) and [native replay](../ruhi-500/cycle-53-device/geometry300-native.json).

Cycle 54 passed 489 JVM methods and all 32 native tests. The 500-row source replay now verifies every case in its declared valid context, including all 25 contextual rows, with original parameter/snapshot evidence. This does not resolve the earlier 300-command audit's 83 unsupported requests or the separately reproduced broader contextual failures. See [current report](../ruhi-500/REPORT.md).
