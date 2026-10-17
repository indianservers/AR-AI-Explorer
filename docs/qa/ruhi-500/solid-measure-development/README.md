# Solid measurement development evidence

The initial compile failure is retained: Paint.color shadowed the outer Compose Color, and a local function needed its own statement. Both were corrected before tests.

Five focused JVM methods passed, including all 25 unchanged source measurement rows with independent formulas, units and topology checks. A focused Android test passed on emulator-5556, including a real sphere-selection chip, native radius edit, literal cone reference drawing, topology and Euler counts.

Visual review of cone.png found the existing empty-scene message obscured the literal reference drawing. The message now only appears when no measurement reference is visible, and overlay labels avoid the existing orientation control. A native assertDoesNotExist gate protects this correction in the fresh full cycle. The old cone image is retained as development evidence, not accepted delivery proof.

Solid references currently use positions and conservative scene guards. A same-type reorder of referenced solids can look like dimension edits; stable solid IDs remain an explicitly incomplete requirement. Measurements use current dimensions, never model-predicted numbers or pixels.

Cycle 36 pre-training regression correctly stopped on legacy single-face queries, an out-of-scope slice query, numeric answer precision, curved cylinder area, and the established read-only-query undo contract. Routing now leaves those explanation/slice queries with the existing adapter, maps curved area to lateral area, emits 12-significant-digit answers, and does not add an undo entry for current-solid measurements. Literal reference drawings remain reversible. Existing regression expectations were preserved; the focused combined regression passed before fresh cycle 37.
