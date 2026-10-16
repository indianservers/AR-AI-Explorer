# Giving Ruhi graph instructions

Open **3D Graph** and use the existing AI assistant prompt. Create the object before editing it. In **2D Graph**, equation plotting and curve appearance commands remain available; solid transforms and groups require 3D Graph.

Examples:

- `Create a cube.`
- `Move the cube to (3, 2, 1).`
- `Translate the cube by vector (2, 3, 4).`
- `Shift the sphere 5 units right.`
- `Scale the cube by 1.5.`
- `Set the cube's side length to 5 units.`
- `Resize the selected shape to 4 x 5 x 6.`
- `Rotate the cube 45 degrees around the Y-axis.`
- `Rotate the selected object 30 degrees around the world origin.`
- `Create two cubes, group them, rotate the group 45 degrees, then ungroup them.`
- `Delete objects inside the selected group.`
- `Undo 2 actions.` / `Redo 2 actions.`
- `Plot z = x^2 + y^2.` / in 2D: `Plot y = x^2.`

TensorFlow-recognized paraphrases include `Relocate the cube to (3, 2, 1)`, `Enlarge the cube by a factor of 2`, `Shrink the sphere by a factor of 2`, `Turn the cube by 30 degrees around the X-axis`, `Cluster the selected cubes`, and `Erase the last sphere`.

Use a number when requesting a transform. Instructions such as `make it bigger`, `move along the X-axis`, or `rotate the group` ask for the missing value. Give the complete instruction with that value in your next prompt. Ruhi does not guess a distance or angle.

Coordinates use Z-up: right is +X, up is +Z, backward is +Y. Numeric rotation without an axis defaults to Z; tilt defaults to X. Clockwise is viewed from +Z. Groups rotate and scale around their member-center mean unless a numeric world-origin rotation is requested. Sizes refer to local object axes. Undo treats a compound command as one action.

The local intent model assists a bounded command grammar. It does not provide unrestricted chat comprehension, continuous animations, or arbitrary geometry constructions. Low-confidence paraphrases do not execute an invented action. No student conversation is used for training.
# Shared studio commands

Ruhi also routes to the existing 2D/3D geometry studios. Examples include “In 2D geometry, create a circle with radius 3” and “Ruhi, create a rotating 3D cone.” In Graph2D, try “draw y equals x squared”, “shift it three units right”, then “show the vertex”. See the [shared architecture and supported scope](qa/RUHI_SHARED_ARCHITECTURE.md) and [test report](qa/ruhi-shared-evidence/REPORT.md).

