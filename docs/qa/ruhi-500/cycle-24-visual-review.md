# Cycle 24 retained assertion failure

The synchronized canvas displays the implicit circle, line and both correct intersection markers. See `cycle-24-implicit-inspection.png`.

The new pixel test failed near (624, 950), but the captured bitmap is 1038 by 2232. For the requested centered, unit-zoom camera, the independently calculated crossing at (sqrt(2), sqrt(2)) projects to approximately (624, 1011). The assertion had read mutable viewport aspect metadata after capture; keyboard resizing had changed it.

The corrected test projects from the captured bitmap dimensions and the explicit requested camera, asserts its center and zoom, and still requires blue pixels near each expected crossing. It does not use production marker positions to generate its expected coordinates. Cycle 24 remains a failed cycle and does not establish the new pixel gate.
