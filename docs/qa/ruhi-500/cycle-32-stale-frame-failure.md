# Retained cycle 32 stale-frame failure

The new integration emulator test passed, including the actual bounds chip, live parent edit, independent positive/negative color probes, Riemann/trapezoid panels and accumulation. The existing intersection pixel assertion caught a stale capture after changing the diagonal to a vertical line.

The saved `cycle-32-intersection-failure.png` still shows the previous diagonal and its old intersection markers, while the current model correctly contains the vertical line and its new coordinates. The UIAutomation displayed-frame capture can return the compositor's previous frame immediately after a model edit. This is not accepted as current render proof.

The shared capture helper now forces a window draw and waits for its frame-commit callback. It then PixelCopies the current window buffer, with a dedicated callback thread and a 15-second bounded wait instead of Compose's fixed two-second wait. All original coordinate, color and geometry gates remain. The full fresh cycle must pass before reporting the new capture path as verified.
