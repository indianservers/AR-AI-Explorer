# AI Explorer

AI Explorer is a native Android mathematics exploration app built with Kotlin and Jetpack Compose.

The current implementation replaces the starter XML project with a dark-first Compose application using the package `com.indianservers.aiexplorer`. It includes functional 2D coordinate geometry, 2D graphing, projected 3D solid exploration, 3D surface graphing, command history, workspace JSON export, accessibility descriptions, and automated math tests.

## Build

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

## APKs

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release-ready unsigned APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Architecture

- `core`: renderer-independent math, expression parsing, 2D geometry, 2D graph analysis, 3D solid measurements, and 3D surface sampling.
- `workspace`: workspace model, command-based undo/redo, and versioned JSON export.
- `MainActivity.kt`: Compose app shell and renderers for the four primary workspaces.
- `test`: unit tests for the mathematical acceptance cases.
- `androidTest`: Compose launch and accessibility smoke tests.

## Notes

This pass intentionally uses a Compose canvas projection for 3D instead of SceneView/Filament. The math engine and mesh generation are isolated from the renderer so a production 3D renderer can be integrated later without rewriting the domain layer.

