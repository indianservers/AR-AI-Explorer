# Scene-edit context audit

The 25 original EDIT_SCENE rows retain their commands, expected actions, source engine ALL, parameters and follow-up guidance. ALL means the active compatible workspace. Rows 451/454/456/457/471 run in Graph2D, row 470 in Graph3D, and the geometric references in Geometry2D. These fixtures do not certify every engine/object combination.

Declared context includes a circle for rows 455/463, a selected triangle for 452/459, a selected curve for 457, initially hidden axes for 454, hidden point labels for 458 and custom colors for 475. Rows 466/467 have real recorded undo/redo history. No expected source slot is replaced by a weaker expectation.

Current proof covers native IDs/formulas/control preservation, independent circle duplication, current named-point/segment resolution, graph line styles, opacity, labels, measurement text, viewport movement, clear/undo and project-library saving. The app's normal save library is fed by the assistant command. Android proof reopens the resulting project using the production SQLite store in an isolated test database, leaving the user's library alone.

Development retained two real submission failures. The displayed message said the scene or selection changed during preparation. Geometry viewport measurements update when the keyboard changes the canvas size. The new guard permits recomputation against current state only when the differences are viewport metrics/timestamps and selection/visibility are unchanged. Point, camera, name, selection and other object changes still reject the prepared edit. The native tests retain failure semantics/state and exercise the text-input path.

Additional correctness checks distinguish an actual red straight line (including implicit vertical lines) from a red curve, distinguish finite segment endpoints from an infinite line's controls, and interpret see-through percentage as transparency rather than opacity. Source 50 percent opacity remains exactly 0.5.

Remaining requirements:

- Full cross-engine fit/centering and styling coverage, including unbounded graphs, singular camera projections and native 3D display controls. The current fit helper certifies bounded 2D geometry (including circular extents and actual aspect); it does not prove fitting every graph type.
- Linked-object deletion/cascade policies and positional solid identity/remapping, including scene restoration after active group motion.
- Hiding every dependent measurement family on a shape, not only the current native selection measurement text; all selection/picking and style-control interactions.
- Save/restore aliases and full pending-command resume/parameter clarification policies across all source intents. A valid-context action plus specific restored object chips does not certify every conversation.
- Broad independent NLP generalization remains distinct from training-fit accuracy on the original 500 examples.

The first full cycle (40) retained a function-test race (an earlier Plotted reply could satisfy the gate before the new curve existed) and legacy Compose capture measure/layout reentrancy. The current tests correlate the current dialogue turn with actual native state. Both test classes now share the committed-window PixelCopy capture helper; the legacy force-measure capture is removed. No math/pixel expectation was weakened. A focused rerun of both failing tests passed before cycle 41.

Cycle 41 completed all 21 native tests with one failure: the angle command was rejected after only Geometry2D viewport-aspect metadata changed during preparation. The same layout-only guard now recomputes commands for every intent against the current authoritative state; real scene, selection, visibility and camera changes still reject. This is a production submission fix, not an increased timeout. Cycle 42 reruns the entire training and verification pipeline.
