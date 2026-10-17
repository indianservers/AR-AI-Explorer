# Retained cycle 29 capture failure

The native source replay and the new derivative/slider test completed without a mathematical assertion failure. The existing intersection test failed while capturing its post-edit image: Compose `captureToImage()` exceeded its fixed 2000 ms callback timeout. The live instrumentation continued through the other tests; it was not restarted on an observation timeout.

The shared canvas capture helper now reads the displayed emulator frame through UIAutomation and crops it to the canvas semantics bounds in window pixels. It retains the real rendering, the same canvas dimensions, and all existing independent coordinate, marker-color, parent-edit, persistence and viewport checks. It does not render an expected image or alter production mathematics.

A fresh build and full cycle are required to verify the new capture path. Cycle 29 remains failed and its logs/evidence are retained.
