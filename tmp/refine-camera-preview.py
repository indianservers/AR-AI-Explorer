from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/HandCameraPreview.kt');s=p.read_text();s=s.replace('texture=view','view.contentDescription = "Camera preview without AR"; texture=view');s=s.replace('fun stop() { camera?.setPreviewCallback(null); camera?.stopPreview(); camera?.release(); camera = null }','''fun stop() {
            val opened = camera; camera = null
            if (opened != null) { runCatching { opened.setPreviewCallback(null); opened.stopPreview() }; opened.release() }
        }''');p.write_text(s)
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/ArGraphPhaseOneUiTest.kt');s=p.read_text();s=s.replace('compose.onNodeWithContentDescription("Hand gestures off").assertIsDisplayed().performClick()\n        compose.onNodeWithContentDescription("Hand gestures on").assertIsDisplayed().performClick()', 'compose.onNodeWithContentDescription("Hand gestures on").assertIsDisplayed().performClick()\n        compose.onNodeWithContentDescription("Hand gestures off").assertIsDisplayed().performClick()');p.write_text(s)
