# Smart Board Engine Reuse Map

| Smart Board interaction | Reused authority | Board responsibility | Known boundary |
|---|---|---|---|
| Graph Editor | `SmartBoardGraphAdapter`, typed Graph parser, Graph 2D/3D modules | Persist editable `GraphConfigurationElement`, launch route | Full curve handles stay in Graph workspace |
| LaTeX insert/edit | `SafeLatexPreview`, shared expression analyzer, CAS parser evidence | Safe live preview and editable `MathExpressionElement` | Unsupported LaTeX remains previewable but is not claimed CAS-readable |
| Offline Physics formula identification | `PhysicsFormulaMatcher`, `OfflinePhysicsFormulaRepository` | Display match, confidence, variables and canonical form | Identification is not evaluation or proof |
| Mathematics formula identification | deterministic canonical structure matcher | Immediate offline teaching context | Named-pattern library is intentionally conservative |
| CAS actions | `SmartBoardCasAdapter`, symbolic CAS and solver | Explicit action and result insertion | Unsupported operations return a bounded error |
| Geometry handoff | existing Geometry 2D/3D modules | Route selected recognized geometry | Construction remains editable in Geometry |
| Subject intelligence | existing subject registries and handlers | Context, confidence and user correction | No silent subject reassignment |
| Save/open | existing repository and schema codec | Restore document, selection lifecycle and undo-ready elements | Undo history restarts after open by design |

## Graph Editor issues found and addressed

- Previously a graph configuration could be created by a math action, but lacked a direct board editor.
- A selected graph now exposes **Edit graph** and **Open graph**.
- The More menu exposes a Graph Editor that creates a persisted, selectable configuration.
- Updating a graph is undoable and reuses typed Graph validation.
- Explicit, implicit, polar, parametric, piecewise and inequality inputs are validated by the shared typed Graph engine before handoff.
- 3D inputs are validated by generating a bounded finite mesh with the shared `Graph3D` engine rather than using a syntax heuristic.
- Common LaTeX graph input is converted to engine notation while the authored LaTeX remains unchanged.
- Launching the full editor preserves the specialist engine’s direct manipulation rather than embedding a weaker renderer.

## LaTeX tool issues found and addressed

- Recognition review supported correction, but direct insertion was not discoverable.
- The More menu now exposes **Insert LaTeX / identify formula**.
- Selected math objects expose **Edit LaTeX**.
- Validation, live accessible preview, parser-readability feedback and offline formula identification are shown before insertion.
- Common fractions, roots, scripts, functions, Greek constants and relation operators receive a separate engine-ready representation for Graph/CAS reuse.
- The validator now rejects file/network/macro primitives, unknown commands and unsupported environments while retaining common mathematical matrices and cases.
- Inserted notation is a movable, selectable, persistable and reopenable board object.
