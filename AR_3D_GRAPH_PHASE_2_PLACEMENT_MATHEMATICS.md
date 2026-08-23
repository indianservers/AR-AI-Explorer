# AR 3D Graph — Phase 2 Placement Mathematics

## Screen ray

Placement does not call `Frame.hitTest()` and does not inspect planes, polygons, depth, floors, tables, or walls. For a viewport point `(tapX, tapY)`, pixel coordinates become OpenGL normalized device coordinates:

```text
x_ndc = 2 tapX / width - 1
y_ndc = 1 - 2 tapY / height
```

With column-major ARCore view matrix `V` and projection matrix `P`, the far clip point is unprojected as:

```text
p_far = inverse(P × V) × (x_ndc, y_ndc, 1, 1)
world_far = p_far.xyz / p_far.w
ray_origin = AR camera world position
ray_direction = normalize(world_far - ray_origin)
```

Invalid viewports, non-finite inputs, degenerate direction, invalid homogeneous coordinates, and singular matrices return structured failure.

## Fixed-distance pose

The default distance is the tested constant `1.5f` metres:

```text
placement_position = ray_origin + ray_direction × 1.5
```

The initial orientation faces the camera using yaw only:

```text
yaw = atan2(camera.x - position.x, camera.z - position.z)
quaternion = (0, sin(yaw/2), 0, cos(yaw/2))
```

Pitch and roll are intentionally excluded, keeping the graph upright and preventing camera roll from tilting it. Handedness is unchanged. The renderer applies a graph-root-only `-90°` X rotation, uniform `0.1 m` per mathematical unit scale, and bounds-centering translation; original engine vertices remain untouched.

## Anchor lifecycle

With tracking active, `Session.createAnchor(Pose)` creates a direct world-pose anchor. Replacement is transactional: a replacement is created first, then the prior anchor is detached. If creation fails, the old anchor remains recoverable. Reset detaches the anchor and preserves graph render data; Clear detaches and removes graph data; disposal/session close detach any remaining anchor. Paused tracking consumes no tap and creates no anchor.

Automated coverage includes centre, four corners, edges, portrait/landscape viewports, alternate sizes and projections, normalized rays, invalid dimensions, inversion failure, exact 1.5 m placement, finite poses, yaw-facing orientation, no roll, first/replacement/failure/reset/clear/disposal behavior, 20 replacements, and five reopen cycles.

