# 2D Graph Test Report

Legend: Pass = verified in live UI or focused JVM regression; Partial = attempted but not fully UI-verified; Blocked = not exposed/not completed in current phone UI.

| ID | Case | Status | Evidence / issue |
|---|---|---|---|
| A2DGR-001 | Open 2D Graph normally | Pass | Home → Graph opened workspace. |
| A2DGR-002 | Test Clear All | Pass | Fixed `AND-2DGR-001`; Cancel and Confirm verified. Undo gap: `AND-2DGR-002`. |
| A2DGR-003 | Pan and zoom | Partial | Interactive graph canvas present; full gesture sweep not completed. `ENH-2DGR-001`. |
| A2DGR-004 | Configure axes and grid | Partial | Graph UI/source coverage exists; full settings path not completed. `ENH-2DGR-001`. |
| A2DGR-005 | Test portrait, landscape, and tablet layouts | Partial | Phone portrait tested only. `ENH-2DGR-004`. |
| A2DGR-006 | Plot individual points | Partial | Graph engines covered; point plotting UI not fully completed. `ENH-2DGR-001`. |
| A2DGR-007 | Plot a coordinate table | Partial | Table engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-008 | Test expression editor | Pass | `f(x)=x` editor displayed valid expression. |
| A2DGR-009 | Test implicit multiplication | Pass | Focused graph JVM regression passed. |
| A2DGR-010 | Style and manage graphs | Partial | Graph row/editor present; full style management not completed. `ENH-2DGR-001`. |
| A2DGR-011 | Plot horizontal lines | Pass | Focused graph JVM regression/expression engine covered. |
| A2DGR-012 | Plot vertical lines | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-013 | Plot slope-intercept equations | Pass | Focused graph JVM regression/expression engine covered. |
| A2DGR-014 | Plot standard and point-slope forms | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-015 | Plot parallel and perpendicular lines | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-002`. |
| A2DGR-016 | Find line intersections | Pass | Focused graph feature regression covered. |
| A2DGR-017 | Model a school word problem | Partial | No dedicated UI scenario completed. `ENH-2DGR-001`. |
| A2DGR-018 | Plot `y=x²` | Pass | Focused graph regression/expression engine covered. |
| A2DGR-019 | Transform a quadratic | Pass | Focused graph regression covered. |
| A2DGR-020 | Plot a quadratic with two roots | Pass | Focused graph regression covered. |
| A2DGR-021 | Plot quadratics with one or no real roots | Pass | Focused graph regression covered. |
| A2DGR-022 | Plot cubic functions | Pass | Focused graph regression covered. |
| A2DGR-023 | Plot higher-degree polynomials | Pass | Focused graph regression covered. |
| A2DGR-024 | Compare factored and expanded forms | Pass | Focused graph regression covered. |
| A2DGR-025 | Plot repeated roots | Pass | Focused graph regression covered. |
| A2DGR-026 | Plot `y=1/x` | Pass | Focused graph regression covered. |
| A2DGR-027 | Transform a rational function | Partial | Engine covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-028 | Plot a rational function with a hole | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-029 | Plot square-root functions | Pass | Focused graph regression covered. |
| A2DGR-030 | Plot cube-root and nth-root functions | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-031 | Plot absolute-value functions | Pass | Focused graph regression covered. |
| A2DGR-032 | Plot piecewise functions | Partial | Typed graph source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-033 | Plot floor, ceiling, and sign functions | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-034 | Apply domain restrictions | Partial | Source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-035 | Plot exponential functions | Pass | Focused graph regression covered. |
| A2DGR-036 | Transform exponential functions | Partial | Engine covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-037 | Plot logarithmic functions | Pass | Focused graph regression covered. |
| A2DGR-038 | Compare inverse exponential and logarithmic graphs | Partial | Engine covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-039 | Plot sine | Pass | Focused graph regression covered. |
| A2DGR-040 | Plot cosine | Pass | Focused graph regression covered. |
| A2DGR-041 | Plot tangent | Pass | Focused graph regression covered. |
| A2DGR-042 | Plot secant, cosecant, and cotangent | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-043 | Transform a trigonometric function | Partial | Engine covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-044 | Plot equivalent trigonometric identities | Partial | Engine covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-045 | Plot inverse trigonometric functions | Partial | Engine/source covered; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-046 | Plot a circle | Partial | Implicit/conic source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-047 | Plot an ellipse | Partial | Implicit/conic source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-048 | Plot parabolas in different orientations | Partial | Implicit/conic source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-049 | Plot a hyperbola | Partial | Implicit/conic source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-050 | Plot implicit relations | Partial | Typed graph source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-051 | Plot a parametric circle | Partial | Typed graph source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-052 | Plot a parametric Lissajous curve | Partial | Typed graph source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-053 | Plot polar graphs | Partial | Typed graph source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-054 | Find roots and intercepts | Pass | Focused graph feature regression covered. |
| A2DGR-055 | Find graph intersections | Pass | Focused graph feature regression covered. |
| A2DGR-056 | Trace curves and create tangent/normal lines | Partial | Feature engine/source covered; UI path not completed. `ENH-2DGR-002`. |
| A2DGR-057 | Identify extrema and inflection points | Partial | Feature engine/source covered; UI path not completed. `ENH-2DGR-002`. |
| A2DGR-058 | Use sliders and animation | Partial | Source coverage exists; UI path not completed. `ENH-2DGR-001`. |
| A2DGR-059 | Save, reopen, export, and share a complex graph | Blocked | Not completed in this pass. `ENH-2DGR-004`. |
| A2DGR-060 | Stress-test 2D Graph | Partial | Focused JVM regression passed; full device stress not completed. `ENH-2DGR-004`. |

