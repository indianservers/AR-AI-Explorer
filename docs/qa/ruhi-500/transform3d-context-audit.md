# 3D transformation context audit

The source batch retains all 25 original commands and structured parameters. Row 436 includes a pyramid, 440 includes a torus, 442 includes a cuboid, and 449 starts in an exploded state. Rows 429/447 select the cylinder, 433/435 the sphere, and 437 explicitly selects both solids. Row 450 starts after a recorded x45 rotation, so its scoped undo has genuine history to restore. These are declared contexts, not altered expected behavior.

Implementation uses deterministic native dimensions/poses and shared mesh generation. Mirror parity is preserved in snapshot schema 15, JSON export and the universal solid properties; older snapshots default to unmirrored. Logical face explosion changes the display mesh without changing the mathematical solid. Group rotations use a shared centroid and compose rotations in world coordinates; independent tests cover all three axes, arbitrary orientations and unrelated ongoing motion.

Current proof does not complete the broader context requirements:

- Native solids still use positional references. Same-type structural reorder/replacement is not a stable identity; dependent recipes and hierarchy need further coordinated migration.
- Exploded display meshes do not yet have independent face/vertex hit-test mapping and AR display-style parity tests.
- Scoped rotation undo preserves later independent field edits, but arbitrary later structural edits are rejected conservatively. General selective-history dependency policies remain incomplete.
- New style keys are remapped by the shared removal helper. Exhaustive deletion, save/restore and running-group interaction coverage belongs to the remaining scene-edit batch.
- Named/color/bulk commands retain the existing adapter instead of silently dropping their qualifiers. Full alternative wording and clarification/suggestion policies still need independent coverage.
- Camera commands use the existing geometry camera presets. The wider cross-engine axis/up convention needs a coordinated audit; source labels alone do not prove every camera interaction.

Development retained regressions for spin-with-an-angle routing, legacy partly-transparent material, locked-solid rejection and a missing fixture pyramid. The fixes preserve original expectations. The focused emulator test captures two committed motion frames with the Compose clock explicitly controlled and stops motion before leaving the scene.

Cycle 38 code review found that submitRuhiAssistant prepared transforms against temporary history and then committed a generic graph transaction. That lost the scoped rotation label and per-field undo behavior on the actual text-input path. The real commit now uses the same native transform transaction type, and native verification explicitly undoes a visible 37-degree rotation after scaling while preserving the new dimensions, then redoes it. Current-solid read-only measurements also preserve their no-undo-entry contract through this asynchronous path. Cycle 38 is retained as superseded evidence; the late-source freshness gate requires the fresh cycle.
