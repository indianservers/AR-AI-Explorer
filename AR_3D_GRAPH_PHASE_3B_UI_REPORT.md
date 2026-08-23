# AR 3D Graph Phase 3B — UI Report

Date: 2026-08-21

The AR screen retains the existing theme, top app bar, equation/settings fields, camera-first composition and Phase 3A behavior. Controls are now grouped into a bounded, vertically scrollable panel: Plot/Update and Help on the first row; Reset View, Reset Placement and Clear on the second. Buttons have a 48 dp minimum height. The camera viewport keeps the remaining height and remains the coordinate space supplied directly to the renderer.

Help is a short dismissible dialog containing the required gesture guidance and definitions for Reset View, Reset Placement and Clear. It can be reopened without changing graph state. Placement/status text uses a polite accessibility live region and does not use repeated snackbars or permanent large overlays.

Responsive connected coverage passed five profiles: 320×640, 640×320, 411×914, 800×1280 and 1280×800 dp. The final profile used 1.5× font scale. Viewport and Back remained displayed in every profile. The controls panel scrolls instead of clipping when height or text space is constrained. Status/navigation-bar insets remain owned by the existing Scaffold. Cutout and three-button-navigation emulation were unavailable.

The existing camera-view local coordinates remain unchanged by the control-panel layout; no overlay offset is added to touch coordinates and recomposition does not create an anchor.
