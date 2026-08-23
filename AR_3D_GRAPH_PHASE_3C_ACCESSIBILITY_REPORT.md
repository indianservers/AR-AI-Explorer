# AR 3D Graph Phase 3C — Accessibility Report

Date: 2026-08-21

Automated result: **PASS**. Assistive-technology physical result: **BLOCKED**.

Back, Plot/Update, Help, Reset View, Reset Placement and Clear expose labels/roles/state and minimum 48 dp touch height. Status uses a polite live region. Help instructions survived ten repeated open/read/close cycles. Five controlled responsive configurations covered compact/standard/tablet-like portrait and landscape sizes and 1.5× font scale while keeping the viewport and essential controls reachable.

The accessibility policy explicitly blocks renderer gestures while touch exploration is enabled. Tests verify no placement, rotation or scaling outcome. Disabled/reset states, Clear confirmation behavior, viewport description, error correction, focusable Back and scroll-reachable controls are covered by unit/Compose tests. Theme-derived colors were not overridden.

Unavailable: real TalkBack focus order and announcements, Switch Access, Accessibility Scanner contrast output, hardware keyboard, display-size extremes, gesture/three-button system navigation and assistive interaction over a live camera surface. These require the physical/device matrix and prevent full accessibility release certification.
