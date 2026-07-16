# Mobile UI enhancement checklist

Implemented 16 July 2026. These changes use progressive disclosure so the mathematical canvas remains the primary surface on phones while tablet, TV, keyboard, mouse, and accessibility behavior remains available.

1. **Collapsible top shell** — tap the centered title to show or hide the workspace subtitle and history actions.
2. **Compact phone header** — smaller type, padding, and action spacing increase usable canvas height.
3. **Translucent header surface** — context remains readable without visually separating the learner from the canvas.
4. **Collapsible bottom navigation** — phones initially show only the current workspace and an expand control.
5. **Expandable six-workspace switcher** — one tap reveals every Maths workspace.
6. **Workspace navigation icons** — each 2D, 3D, graph, trigonometry, and AR destination has a transparent symbolic icon.
7. **Smaller icon-and-label navigation cells** — two-line compact cells prevent wrapped or clipped labels.
8. **Draggable Mathematics menu** — drag its header to move it away from the object being studied.
9. **Bounded overlay movement** — movable surfaces are constrained to practical screen bounds.
10. **Orientation-safe overlay reset** — movable surfaces return to a safe position after size/orientation class changes.
11. **Collapsed quick-action dock** — `More` opens a small handle instead of a full-height menu.
12. **Draggable quick-action dock** — drag the handle to reposition Focus/Learn/Tools/Info actions.
13. **Expandable quick actions** — the full dock is shown only when explicitly requested.
14. **Movable collapsed workspace tool launcher** — the three large left-side buttons become a single `Open tools` control; tap toggles it and press-drag moves it anywhere within safe screen bounds.
15. **One workspace panel at a time** — opening Tools, Info, or Controls closes the previous workspace panel.
16. **Collapsed equation chips** — equations become a small count badge until the learner requests them.
17. **Available-first Mathematics menu** — working destinations are immediately visible; future destinations do not consume phone height.
18. **Show/hide planned submenu group** — planned Maths areas can be revealed without permanently lengthening the menu.
19. **Show/hide workspace group** — the workspace shortcut grid is independently collapsible.
20. **Compact menu descriptions** — phone cards show concise titles; descriptions use progressive disclosure.
21. **Transparent icons on every shared action button** — existing menus inherit icons without per-screen duplication.
22. **Transparent submenu icons** — Problem Solver, formulas, MCQs, proofs, dictionary, probability, and other Maths entries have distinct symbols.
23. **Transparent subject icons** — every subject card uses the same quiet badge system, including disabled future subjects.
24. **Iconic, accessible panel headers** — panels show a semantic icon, larger close target, and drag guidance for movable surfaces.
25. **Denser scroll-safe panels** — reduced inner padding, animated size changes, bounded height, and scrolling keep controls reachable on short screens.

## Interaction notes

- Transparent icons supplement labels; they do not replace accessible text.
- All expandable controls retain at least a 42dp visual control height and larger padded tap targets where possible.
- The canvas gestures remain unchanged: the UI compacts around the maths rather than taking touch ownership from it.
- Graph analysis modes are no longer rendered as a permanent button grid over the graph; open the movable launcher and choose `Trace` to access them.
- Tablet/TV layouts begin expanded where space permits; phones begin with the most compact navigation state.
