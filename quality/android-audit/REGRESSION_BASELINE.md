# Regression Baseline

Baseline date: 2026-08-20  
Commit: `6ffd865+local-fixes`

Commands:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest --tests "com.indianservers.aiexplorer.*Spatial*" --tests "com.indianservers.aiexplorer.SolidCatalogTest" --tests "com.indianservers.aiexplorer.SharedSpatialMathSchemaTest" --tests "com.indianservers.aiexplorer.UnifiedSpatialMathControllerTest" --tests "com.indianservers.aiexplorer.Graph*" --tests "com.indianservers.aiexplorer.AdvancedGraphFeatureEngineTest" --tests "com.indianservers.aiexplorer.GeometryGraph*"
```

Expected:

- Build passes.
- Focused unit regression passes.
- 3D Geometry Clear All opens confirmation, Cancel preserves object, Confirm clears only current workspace.
- 2D Graph Clear All opens confirmation, Cancel preserves expression, Confirm clears only current workspace.

