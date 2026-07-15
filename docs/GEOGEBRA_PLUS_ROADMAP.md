# AI Explorer: Six-Phase Roadmap To A World-Class Interactive Math App

Goal: make AI Explorer a premium mobile-native mathematics laboratory that is easier, more beautiful, and more learning-focused than GeoGebra on phones and tablets.

The approved visual direction remains: near-black workspace, cyan/violet math objects, luminous grids, radiant circular points, glass panels, crisp axes, and professional scientific typography.

The product must stay **AI Explorer**. Do not use AR wording in app title, modules, buttons, settings, or user-facing copy.

## Product North Star

AI Explorer should feel like a full-screen mathematical instrument:

- 90% or more of the screen is the active workspace in normal use.
- Panels are hidden, collapsible, contextual, or bottom-sheet based.
- Tools appear exactly when needed and disappear when the user returns to exploration.
- Every visible control performs a real mathematical or workspace action.
- Touch, drag, pinch, rotate, haptics, and mobile sensors are used thoughtfully.
- The same math engines support 2D, 3D, graphing, activities, persistence, and future renderers.

## Target Feature Groups

- Dynamic 2D Geometry
- Coordinate Geometry
- 2D Graphing Calculator
- Advanced Calculus Graphing
- 3D Solid Geometry Lab
- 3D Function And Surface Graphing
- Trigonometry And Unit Circle Lab
- Formula Proofs And Visual Theorems
- Guided Interactive Learning
- Saved Workspaces
- Export And Sharing
- Accessibility And Classroom Readiness

---

## Phase 1: Workspace-First Mobile Shell

### Objective

Make the app feel like a real mobile math workspace by hiding large panels and making the canvas the main experience in 2D, 3D, graph, and 3D graph modules.

### Implemented In This Pass

- App-level workspace overlay instead of fixed top/bottom layout.
- Floating top chrome.
- Floating bottom mode selector.
- Hidden left, right, and bottom panels by default.
- Mini dock for:
  - Focus
  - Tools
  - Info
  - Panel
- Module launchers:
  - 2D: Tools, Measure, Construct
  - 3D: Solids, 3D Tools, Transform
  - Graph: Fx, Analysis, Trace
  - 3D Graph: Equation, Surface, 3D Ctrl
- 3D solids panel exposes more shape types.
- 3D and 3D graph workspaces include zoom controls.
- 3D solids renderer now draws distinct projected silhouettes for cube/cuboid, pyramid, cone, cylinder, sphere/hemisphere, and torus.

### UX Requirements

- Canvas owns the screen in idle state.
- Side panels open only when requested.
- Bottom inspectors remain collapsible.
- Top and bottom chrome must not prevent math interaction.
- Tool access must stay one tap away.

### Engineering Requirements

- Shared panel state:
  - Left panel
  - Right panel
  - Bottom panel
  - Chrome visibility
- Reusable overlay components:
  - Mini dock
  - Floating panel launchers
  - Equation chips
  - Glass panel
- Preserve current math behavior and tests.

### Acceptance Criteria

- User can switch modules while panels remain hidden by default.
- User can reveal tools, insights, and controls on demand.
- 2D, 3D, graph, and 3D graph remain functional.
- Debug and release builds pass.

---

## Phase 2: GeoGebra-Class 2D Construction Engine

### Objective

Make 2D geometry truly interactive like GeoGebra, but cleaner for mobile.

### Started Implementation

- Added Trigonometry as a new primary module.
- Added a sample mobile-native Trigonometry workspace with:
  - Unit circle
  - Angle ray
  - Sine and cosine projections
  - Tangent guide
  - Quadrant sign cards
  - Right-triangle visual card
  - Trigonometric identities card
  - Sine wave pane
  - Angle slider
  - Special-angle buttons
  - Collapsible Tools, Insights, and Controls panels
- Added hidden/collapsible controls following the 90% workspace-first rule.
- Added multi-tap 2D construction flow:
  - Point: 1 tap
  - Line/segment/ray: 2 taps
  - Circle: 2 taps
  - Rectangle/square: 2 taps
  - Triangle: 3 taps
  - Arc: 3 taps
  - Polygon: 4 taps in the current starter flow
- Added persistent 2D construction objects to workspace state.
- Added gradient-filled futuristic 2D object rendering.
- Added draggable construction vertices so objects can be moved/resized by moving points.
- Added compact 3D graph example chips for paraboloid, saddle, wave, and plane without consuming workspace space.

### Target User Features

- Place points directly by tapping the workspace.
- Drag free points smoothly.
- Create objects through step-by-step touch flows.
- Construct dependent objects that update live.
- Select, rename, hide, lock, style, duplicate, and delete objects.
- Use a collapsible object drawer and contextual property inspector.
- Use haptic feedback for snaps and valid construction steps.
- Use a precise coordinate input sheet when needed.

### Tool Groups

- Selection:
  - Select
  - Move
  - Multi-select
  - Box select
  - Lasso select where practical
- Points:
  - Free point
  - Point by coordinates
  - Point on object
  - Intersection point
  - Midpoint
  - Division point
  - Centroid
  - Circumcentre
  - Incentre
  - Orthocentre
- Lines:
  - Infinite line
  - Segment
  - Ray
  - Vector
  - Parallel
  - Perpendicular
  - Perpendicular bisector
  - Angle bisector
  - Tangent
  - Secant
- Shapes:
  - Triangle
  - Right triangle
  - Isosceles triangle
  - Equilateral triangle
  - Scalene triangle classifier
  - Rectangle
  - Square
  - Parallelogram
  - Rhombus
  - Trapezium/trapezoid
  - Kite
  - Cyclic quadrilateral
  - Inscribed polygon
  - Circumscribed polygon
  - Regular polygon
  - Free polygon
  - Circle by center and point
  - Circle by center and radius
  - Circle through three points
  - Concentric circles
  - Annulus
  - Arc by three points
  - Sector
  - Semicircle
  - Ellipse
  - Hyperbola and parabola as conic constructions where practical
- Trigonometry:
  - Unit circle
  - Sine, cosine, tangent visual ratios
  - Angle in degrees/radians
  - Special angles
  - Right-triangle ratios
  - Law of sines visualizer
  - Law of cosines visualizer
  - Trigonometric identities as dynamic diagrams
- Transform:
  - Translate
  - Rotate
  - Reflect over line
  - Reflect over point
  - Dilate
  - Shear
  - Copy
  - Group
- Measure:
  - Coordinates
  - Distance
  - Segment length
  - Slope
  - Angle
  - Perimeter
  - Area
  - Radius
  - Diameter
  - Circumference
  - Arc length
  - Sector area
  - Equation of line
- Equation of circle
- Trigonometric ratios
- Circumcircle and incircle formula values
- Area formulas shown through construction

### Engine Targets

- Persistent 2D object model.
- Dependency graph with recomputation order.
- Degenerate geometry detection.
- Circular dependency prevention.
- Hit testing and object picking.
- Snapping engine.
- Command-based undo/redo with drag coalescing.
- Style engine for points, lines, fills, labels, and measurements.

### Mobile-Native Interaction

- Tap to place.
- Drag to move.
- Long press for object context menu.
- Two-finger pan.
- Pinch zoom.
- Double tap fit/reset.
- Haptic snap feedback.
- Bottom-sheet property editor.
- Keyboard-aware coordinate input.

### Tests

- Point placement.
- Drag point.
- Add triangle.
- Add line/segment/ray.
- Add circle/arc.
- Dependency update.
- Snap targets.
- Undo/redo.
- Invalid geometry messages.

### Acceptance Criteria

- A student can construct, edit, and measure a triangle using mostly the canvas.
- Dependent constructions update live while dragging.
- 2D tools feel direct, not form-driven.
- Trigonometry diagrams can be built and measured from the same 2D workspace.

---

## Phase 3: Advanced 2D Graphing And Calculus Lab

### Objective

Build a graphing workspace that supports live functions, parameters, tracing, and calculus analysis while keeping the canvas open.

### Target User Features

- Add and edit multiple equations.
- Hide equation panel and use equation chips.
- Add sliders for detected parameters.
- Drag sliders and see graph updates immediately.
- Trace along curves.
- Find roots, intersections, extrema, and intercepts.
- Show tangent and normal lines.
- Show derivative function.
- Shade definite integrals.
- Compare area between curves.
- Export graph image and workspace JSON.

### Function Support Targets

- Linear
- Quadratic
- Polynomial
- Rational
- Absolute value
- Radical
- Exponential
- Logarithmic
- Trigonometric
- Inverse trigonometric
- Hyperbolic
- Piecewise
- Parametric
- Polar
- Inequalities
- Point lists
- Tables
- Unit circle functions
- Trigonometric transformations

### Engine Targets

- Strong expression parser.
- Domain restrictions.
- Parameter detection.
- Adaptive sampling.
- Discontinuity detection.
- Numerical derivative.
- Numerical integral.
- Root finding.
- Intersection solving.
- Extrema detection.
- Tangent and normal calculation.
- Annotation layout engine.
- Trigonometric period/amplitude/phase analysis.

### Mobile-Native Interaction

- Equation chips at top of workspace.
- Swipe-up graph inspector with tabs:
  - Plot
  - Sliders
  - Trace
  - Analysis
  - Table
- Drag trace point directly on curve.
- Haptic snap at roots, intersections, extrema, and intercepts.
- Pinch zoom and two-finger pan.
- Fit selected function.
- Use hardware keyboard and custom math keyboard.
- Use unit-circle helper and angle slider for trigonometric functions.

### Required Accuracy Targets

Default graph:

- `f(x) = x^2 - 4x + 3`
- `g(x) = x - 1`

Correct:

- Vertex `(2, -1)`
- Roots `1` and `3`
- Intersections `(1, 0)` and `(4, 3)`
- `g` slope `1`

### Acceptance Criteria

- Graphs are generated from real expressions.
- Function edits update live.
- Trace, sliders, tangent, derivative, and integral tools are functional.
- Trigonometric graphs explain amplitude, period, phase shift, and asymptotes where applicable.
- Graph workspace remains mostly canvas-first.

### Phase 3 Implementation Started

- Added compact graph tool chips for Plot, Trace, Tangent, Derivative, Integral, and Intersections so the main canvas keeps priority.
- Added live native sliders for the default quadratic and line examples:
  - `f(x) = ax^2 + bx + c`
  - `g(x) = mx + b`
- Added tap-and-drag trace interaction directly on the graph canvas.
- Added tangent slope, numerical derivative, integral shading, and intersection/root highlight overlays.
- Kept equations, analysis, and controls behind collapsible panels so the graph workspace remains interaction-first.

### Next Phase 3 Targets

- Add direct draggable curve parameters and haptic snap points.
- Add normal line, area between curves, and table view.
- Add editable expression chips with a native math keyboard.
- Add pan, pinch zoom, fit-to-function, and selected-function focus.
- Add export image support in addition to workspace JSON.

---

## Phase 4: 3D Solids Laboratory

### Objective

Make the 3D workspace a complete solid geometry lab where users place, select, transform, measure, and learn from 3D objects.

### Target User Features

- Add 3D objects from a floating add menu.
- Select object, face, edge, and vertex where practical.
- Move, rotate, and scale objects.
- Use numeric transform input.
- Toggle solid, transparent, wireframe, x-ray, faces, edges, vertices, and labels.
- Measure volume, surface area, edges, radius, height, and angles.
- Show Euler formula validation.
- Use cube net unfolding, exploded view, cross-section, and volume fill animation.

### Shape Targets

- 3D point
- 3D line
- Segment
- Vector
- Plane
- Cube
- Cuboid
- Sphere
- Hemisphere
- Cylinder
- Cone
- Frustum
- Pyramid
- Prism
- Tetrahedron
- Octahedron
- Icosahedron where practical
- Torus
- Custom polygon extrusion
- Solids generated from 2D shapes
- Solids of revolution for formula-proof activities

### Renderer Targets

- Introduce `Renderer3D` adapter.
- Keep Compose projection as fallback.
- Evaluate and integrate SceneView/Filament if stable.
- Add object picking.
- Add camera controller.
- Add mesh buffers and material system.

### Mobile-Native Interaction

- One-finger orbit.
- Two-finger pan.
- Pinch zoom.
- Double tap fit.
- Long press context menu.
- Transform handles.
- Rotation rings.
- Scale handles.
- Haptic feedback for snap angles.

### Acceptance Criteria

- Users can place and manipulate multiple 3D solids.
- Measurements are correct.
- Solids remain interactive with panels hidden.

### Phase 4 Implementation Started

- Added workspace-level 3D solid positions so solids are no longer fixed demo drawings.
- Added undoable solid move and transform commands.
- Added first-class 3D vectors with start/end points, components, magnitude, selection, movement, resizing, and export.
- Added a compact floating 3D add strip for Cube, Cuboid, Sphere, Cylinder, Cone, Pyramid, and Torus.
- Added Vector to the compact floating 3D add strip.
- Added direct canvas object picking:
  - Tap a solid to select it.
  - Tap a vector to select it.
  - Drag a solid to move it across the 3D workspace.
- Drag a vector to translate it across the 3D workspace.
- Added selected-object glow and label on the 3D canvas.
- Added collapsible selected-solid details with volume, surface area, faces, edges, and vertices.
- Added selected-object dimension controls for width, height, depth, and radius.
- Added selected-vector controls for `dx`, `dy`, and `dz`.
- Added solid export data to workspace JSON.
- Added vector export data to workspace JSON.

### Next Phase 4 Targets

- Add transform handles, rotation rings, and scale handles directly on selected solids.
- Add object labels, face/edge/vertex picking, and cross-section preview.
- Add pinch zoom, one-finger orbit, two-finger pan, and double-tap fit.
- Add more shape families: prism, tetrahedron, hemisphere, frustum, and extruded polygons.
- Add Euler formula proof mode and volume-fill animations.

---

## Phase 5: 3D Function Graphing And Surface Analysis

### Objective

Create a professional 3D graphing module for mathematical surfaces, contours, slices, gradients, and visual explanations.

### Target User Features

- Enter `z = f(x, y)` equations.
- Add multiple surfaces.
- Rotate, pan, zoom, and fit.
- Change domain and mesh density.
- Toggle surface, wireframe, contours, projected contours, normals, gradient, and bounding box.
- Slice by:
  - `z = constant`
  - `x = constant`
  - `y = constant`
  - Free plane where practical
- Trace a point on the surface.
- Show gradient and normal vector at selected point.
- Explain surface classification.

### Surface Targets

- Planes
- Elliptic paraboloids
- Hyperbolic paraboloids
- Cones
- Spheres
- Ellipsoids
- Hyperboloids
- Trigonometric surfaces
- Exponential surfaces
- Radial surfaces
- Saddle surfaces

### Engine Targets

- Mesh generation from parsed expression.
- Domain clipping.
- Invalid value clipping.
- Normal calculation.
- Adaptive mesh density.
- Contour generation.
- Slice curve generation.
- Surface classification.
- Gradient approximation.

### Required Accuracy Target

For `z = x^2 + y^2`:

- Classification: elliptic paraboloid.
- Vertex: `(0, 0, 0)`.
- Opens upward.
- Rotational symmetry about z-axis.
- Domain: all ordered pairs `(x, y)`.
- Range: `z >= 0`.
- Sample values:
  - `z(0,0)=0`
  - `z(1,0)=1`
  - `z(0,1)=1`
  - `z(1,1)=2`

### Acceptance Criteria

- 3D graph pane feels like a real manipulable 3D mathematical object.
- Rotation, zoom, slices, and contours are interactive.
- Surface insights are mathematically correct.

### Phase 5 Implementation Started

- Added expanded surface examples:
  - Paraboloid
  - Saddle
  - Wave
  - Plane
  - Cone
  - Ripple
- Added canvas-first 3D graph interaction:
  - Drag in non-trace modes to rotate and tilt the surface.
  - Tap or drag in Trace mode to move the trace point.
- Added surface display toggles for wireframe, contours, slice, gradient, and bounding box.
- Added editable slice level plus trace `x` and `y` controls.
- Added a selected trace point with live `z` evaluation.
- Added numerical gradient vector overlay at the trace point.
- Added bounding-box, contour, and slice rendering on the 3D surface canvas.
- Kept equation, insight, and controls in collapsible panels so the 3D graph stays workspace-first.

### Next Phase 5 Targets

- Add true pinch zoom and two-finger pan with velocity-aware native gestures.
- Add multiple simultaneous surfaces and per-surface materials.
- Add proper marching-squares contour curves and slice curves across both mesh directions.
- Add normals, tangent planes, gradient descent/ascent paths, and critical-point detection.
- Add domain controls, adaptive mesh quality, and surface presets for sphere, ellipsoid, hyperboloid, and parametric surfaces.

---

## Phase 6: Learning System, Persistence, Export, Polish, And Production Hardening

### Objective

Turn the app into a complete learning product with workspaces, activities, export, accessibility, and performance polish.

### Guided Learning Targets

- Activity engine:
  - Activity ID
  - Objective
  - Initial workspace
  - Allowed tools
  - Step validation
  - Hints
  - Completion explanation
  - Progress
  - Bookmark
- Initial activities:
  - Plot two points and find midpoint.
  - Construct perpendicular bisector.
  - Verify Pythagoras.
  - Prove area of triangle as half of rectangle.
  - Prove area of parallelogram by rearrangement.
  - Prove circle circumference and area through dynamic approximation.
  - Prove angle sum of triangle.
  - Prove sine, cosine, and tangent ratios.
  - Explore unit circle and radian measure.
  - Derive law of sines.
  - Derive law of cosines.
  - Visualize compound angle identities.
  - Visualize `sin^2(x) + cos^2(x) = 1`.
  - Explore triangle angle sum.
  - Explore roots and intersections.
  - Explore derivative and tangent.
  - Explore cylinder/cone volume.
  - Verify Euler formula.
  - Explore paraboloid slices.
  - Build and measure a prism.

### Formula Proof And Trigonometry Targets

- Interactive proof cards connected to live workspaces.
- Step-by-step formula derivations:
  - Distance formula
  - Midpoint formula
  - Slope formula
  - Triangle area
  - Circle circumference
  - Circle area
  - Pythagoras theorem
  - Quadratic formula visual derivation
  - Trigonometric ratios
  - Unit circle identities
  - Sine rule
  - Cosine rule
  - Surface area and volume formulas for cube, cuboid, cylinder, cone, sphere, prism, and pyramid
- Each proof should include:
  - Visual model
  - Drag handles
  - Formula panel
  - Step explanation
  - Try-it-yourself validation
  - Misconception hints
  - Completed proof summary

### Persistence Targets

- Room database:
  - Workspaces
  - Objects
  - Equations
  - Activity progress
  - Recent history
  - Migrations
- DataStore settings:
  - Theme
  - High contrast
  - Reduced motion
  - Haptics
  - Grid defaults
  - Snap defaults
  - Decimal precision
  - Graph quality
  - 3D quality

### Export Targets

- Workspace JSON.
- Transparent PNG.
- PNG with background.
- Shareable workspace file.
- Import validation.
- Schema migrations.

### Quality Targets

- Smooth 60 FPS interactions where feasible.
- Background computation for heavy math.
- Mesh simplification during gestures.
- Cache graph samples and meshes.
- No renderer leaks.
- Accessibility descriptions for math objects.
- Keyboard navigation where practical.
- Tablet/foldable layouts.

### Acceptance Criteria

- Workspaces save, reopen, duplicate, rename, and delete.
- Guided activities validate live user work.
- Export/import works safely.
- App remains stable under rotation and process recreation.

### Phase 6 Implementation Started

- Added an in-app Learning Coach overlay that stays hidden until opened.
- Added guided activity definitions for:
  - Midpoint and distance
  - Triangle angle sum
  - Quadratic roots and vertex
  - Unit circle trigonometry
  - 3D volume lab
  - Paraboloid slices
- Added activity routing so each lesson opens the correct workspace module.
- Added activity progress state and completion tracking.
- Added learning cards with objective, target task, hint, and proof summary.
- Added a compact workspace JSON preview inside the learning panel.
- Added top dock actions for Learn and Export while keeping panels collapsible.

### Next Phase 6 Targets

- Add real validation rules per activity instead of manual completion.
- Persist workspaces, completed lessons, and settings using Room and DataStore.
- Add import flow with schema validation and migrations.
- Add PNG export and shareable workspace package export.
- Add accessibility navigation and stronger content descriptions for every math object.
- Add performance profiling, mesh/sample caching, and gesture-specific low-quality preview rendering.

### Balanced Phase Completion Pass

- Added live validation for the starter guided activities:
  - Midpoint and distance checks for two separated points.
  - Triangle activity checks for a triangle construction.
  - Quadratic activity checks for the target quadratic form.
  - Trigonometry activity checks that the unit-circle module is active.
  - 3D volume activity checks for multiple solids / sphere addition.
  - Paraboloid activity checks for `x^2 + y^2`.
- Changed activity completion so lessons must pass validation before being marked complete.
- Added in-app workspace management foundation:
  - Save current workspace snapshot.
  - Restore saved workspace snapshot.
  - Duplicate saved workspace.
  - Delete saved workspace.
- Added app settings model and Learning Coach toggles for haptics, snap, high contrast, and reduced motion.
- Added learning package export containing:
  - Workspace JSON
  - Active activity
  - Completed activity IDs
  - Last validation state
  - App settings
- Added package preview in the Learning Coach without mutating app status.

### Remaining Production Hardening

- Replace in-memory workspace library with Room persistence.
- Replace in-memory settings with DataStore.
- Add full JSON import and schema migration execution.
- Add instrumented UI tests for panel hiding, activity validation, and workspace restore.
- Add real PNG/share export through Android sharing APIs.

---

## Definition Of Better Than GeoGebra

AI Explorer becomes better than GeoGebra on mobile when it delivers:

- A cleaner 90% canvas-first workspace.
- Faster contextual tools.
- More beautiful mathematical rendering.
- Stronger guided explanations.
- Better beginner defaults.
- Powerful advanced graph and 3D tools.
- Mobile-native gestures and haptics.
- Reliable saved workspaces.
- A modular engine ready for future visualization environments.
