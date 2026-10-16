# Ruhi graph command architecture

Ruhi uses the existing assistant overlay in the native Android/Kotlin application. Lessons, educational search, solver routing and screen layouts are unchanged. This is not a Three.js implementation.

## Execution and language

`GraphAssistant.execute` parses complete bounded instructions into `GraphAction` values. Compound clauses are parsed first, then applied to immutable staged state, and committed as one scoped `GraphWorkspaceCommand`. A rejected clause leaves the scene and history unchanged. The visible assistant prepares edits on a worker thread and rejects a stale commit if the workspace changed meanwhile.

`TensorFlowGraphNlp` loads the bundled TensorFlow-trained model using the LiteRT CPU Interpreter. `GraphLanguageRouter` uses predictions only for a restricted set of paraphrase grammars: relocate/reposition, enlarge/shrink by a numeric factor, turn by an angle, cluster/combine, dissolve/break apart a group, and erase. Confidence must be at least 0.85 and the top-two score margin at least 0.35. Targets, coordinates, dimensions, budgets and execution remain deterministic. A prediction cannot invent arguments, generate code, bypass validation, or turn a lesson question into a scene edit. Canonical instructions retain their deterministic parser. This is an English intent classifier, not an unrestricted language model.

Model training is offline, from authored synthetic templates in `scripts/train_ruhi_graph_nlp.py`, with no student conversations or audit correction records. The model card and held-out evaluation describe its limited benchmark. Runtime inference requires no network and performs no training.

## Transform and grouping model

A surface layer stores `GraphTransform` (local scale, orthogonal orientation, world translation) and persistent `graphGroup` membership. Project serialization and linked-document style serialization round-trip both. The same `GraphSurfaceGeometry` boundary produces transformed meshes for visible rendering, hit selection and audit checks. Equations and domains remain in local coordinates; analytical tools operating on the equation retain that local coordinate convention.

Numeric movement supports absolute centers, vector/directional translations, relative placement above/next to existing layers, alignment and positive-X placement. Up is +Z, right is +X, backward is +Y. Absolute group placement uses the mean of member centers.

Scaling supports numeric factors, doubling/halving, width, side length, height, radius, three local dimensions, equal cube sizes and restoring original scale. Primitive dimensions use stored analytic parameters; rendered meshes remain bounded approximations. Group scaling also scales member center offsets about their mean center. Negative/zero/infinite factors are rejected. Mirroring uses an orthogonal reflection rather than a negative size.

Rotations support numeric degrees about X/Y/Z, upside-down, clockwise, resetting orientation, axis alignment and numeric rotation about the world origin. Unspecified axes on numeric rotation default to Z; tilt defaults to X; clockwise is viewed from +Z. Members rotate around their mean center as a group, preserving relative distances. Requests missing a numeric angle, distance or size ask for clarification. Continuous and one-cycle animation remain unsupported in Graph3D. The separate shared Geometry3D adapter supports rotating solids.

The [shared architecture](RUHI_SHARED_ARCHITECTURE.md) now dispatches commands to all four existing studios. Graph2D additionally supports numeric horizontal/vertical curve translations and validated quadratic vertex annotations. Geometry operations use native mathematical models and the same scoped history boundary; UI and lessons retain their existing layouts.

Groups are memberships, not duplicate objects. Selecting a member provides group context; group-targeted movement, scale, rotation, ungroup and deletion act on stable member IDs. Grouping does not change pixels. Undo/redo restores changed graph fields and metadata without overwriting the other graph module. Multi-step history operations stage every transition before changing the stacks.

## Rendering and geometry

The native renderer uses a projected Compose canvas. Meshes use an LRU cache of 32 immutable base geometries keyed by equations, kind, domain and density; object transforms do not require resampling equations. Visible layers share a sampling budget: requested density is capped by `max(8, floor(24 / sqrt(visible layer count)))`. Single-layer requested quality is preserved without this multi-layer cap. Transforms also expand the camera range when required to retain moved/enlarged objects. This bounds multi-layer triangle load without changing UI controls or dropping objects.

The explicit sampler preserves undefined regions and tests numerical convergence before filling removable holes, including diagonal directions. The implicit mesher orders polygon intersections cyclically, preserves zero edges, orients triangles, and deduplicates grid-aligned faces. Independent coverage and area tests preserve the original failure evidence. Finite sampling is not a proof of a symbolic multivariable limit.

The stored domain schema couples X/Z bounds. Three independent spatial domain intervals are not implemented. Physical GPU performance and WebGL are not certified.

## Verification and limits

All 150 original sentences remain in `GraphAuditCases`; supported cases have explicit scene preconditions established through the same command boundary. New assertions check coordinates, orientation, dimensions, group membership, identity, undo and real mesh topology. A separate compound regression supplies the numeric angle absent from sentence 150. The original sentence correctly requests that missing value atomically.

The device runner captures the canvas with up to three bounded Compose redraw attempts. Exhausted capture remains NOT_RUN, never a pass. Symmetry rotations may retain a silhouette while changing triangulation; numeric orientation and transformed vertices are checked independently. Group/selection metadata edits can correctly leave pixels unchanged. The 2D test submits through the actual visible prompt and IME Search. The TensorFlow test invokes the real Android interpreter and executes a model-recognized paraphrase.

`GraphAssistantAuditActivity` is debug-only; student releases contain no audit dashboard. Use `scripts/run_ruhi_graph_audit.ps1` for full/category/individual reruns. HTML reports display saved evidence rather than executing commands.

Remaining gaps include continuous animation, point/line/vector constructions and many geometric relations, saved-scene restoration, unrestricted conversational instructions, and implicit argument guessing. Unsupported operations never claim a scene update. See the latest evidence report for exact outcomes.
