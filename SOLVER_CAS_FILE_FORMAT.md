# Solver/CAS File Format

Date: 2026-08-20

Status: **FAIL**. Inspection found no Solver/CAS save/import/export/share implementation satisfying the requested contract. No format-version, migration, corrupt-input, unsupported-version, checksum, transactional-import, autosave, process-death, or handoff result can be claimed.

Required future envelope: format/app versions; `SOLVER_CAS` workspace type; IDs and metadata; timestamps; curriculum/difficulty/tags; problem, hints, portable expression and assumptions; results, steps, verification, precision, units, angle mode; linked workspaces, preview, and checksum.

Import must validate and migrate temporary state, verify version/type/checksum, and replace the current session atomically only after success. Failure must preserve the current session.
