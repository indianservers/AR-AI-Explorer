# Geometry/Graph Staged Expression Implementation Report

## Outcome

Partially typed function and surface expressions now remain visible, editable, revisioned, and persistent without being treated as verified mathematics.

## Behaviour

- Invalid drafts retain the original stable object ID.
- Source text and parser diagnostics are stored in `UniversalMathPayload.Symbolic`.
- `UniversalMathValueStatus.ParseError` makes the object unusable by dependent computation.
- Staged parse diagnostics do not make the whole document structurally corrupt.
- Missing dependencies, cycles, stale revisions and schema violations still fail closed.
- Completing the expression routes through strict symbolic editing and restores `Valid` status.
- Graph 2D and Graph 3D editor callbacks now use the staged-edit boundary.
- Invalid 3D surface drafts survive workspace serialization and restoration.

## Verification

Focused staged-expression, batch transaction, unified 2D, unified spatial, universal schema and trusted-kernel tests pass with `BUILD SUCCESSFUL`.
