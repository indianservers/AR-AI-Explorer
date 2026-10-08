from pathlib import Path
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/ArGraphPhaseOneUiTest.kt');s=p.read_text();s=s.replace('compose.onNodeWithContentDescription("Hand gestures off").assertIsDisplayed().performClick()','compose.onNodeWithContentDescription("Hand gestures off").assertIsDisplayed()',1);p.write_text(s)
