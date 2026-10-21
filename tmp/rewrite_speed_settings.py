from pathlib import Path
import subprocess
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/SpeedCalculationGame.kt')
s=subprocess.check_output(['git','show','HEAD:'+p.as_posix()]).decode('utf-8')
start=s.index('    Column(\n',s.index('private fun SpeedSettingsScreen'))
end=s.index('\n@Composable\ninternal fun SpeedPlayScreen',start)
s=s[:start]+'''    CompactSettingsPage(title, accent, "${LocalPreviousGameBest.current} solved", onBack) {
        SettingsCard("Time Limit", "Choose duration", accent) {
            SettingsChoices(DurationChoices.map(::formatDuration), { DurationChoices[it] == duration }, accent,
                { onDuration(DurationChoices[it]) })
        }
        if (mastery == 0) {
            if (mode == SpeedCalculationMode.Basic) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val size: @Composable (Modifier) -> Unit = { modifier ->
                        SettingsCard("Number Size", "Operand / quotient range", accent, modifier) {
                            SettingsChoices(listOf("Single", "Double", "Triple"), { it + 1 == digits }, accent, { onDigits(it + 1) })
                        }
                    }
                    val operations: @Composable (Modifier) -> Unit = { modifier ->
                        SettingsCard("Operations", "Tap to include or exclude", accent, modifier) {
                            SettingsChoices(listOf("+", "−", "×", "÷", "%"), { BasicOperations[it] in selectedBasic }, accent,
                                { onToggleBasic(BasicOperations[it]) })
                        }
                    }
                    if (maxWidth >= 340.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        size(Modifier.weight(1f)); operations(Modifier.weight(1f))
                    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        size(Modifier); operations(Modifier)
                    }
                }
            } else {
                SettingsCard("Challenge Topics", "Tap to include or exclude", accent) {
                    SettingsChoices(listOf("Calculation", "Trigonometry", "Algebra"), { AdvancedTopics[it] in selectedAdvanced }, accent,
                        { onToggleAdvanced(AdvancedTopics[it]) })
                }
            }
        }
        SettingsCard("Reactor Level", if (mode == SpeedCalculationMode.Basic)
            when (mastery) { 1 -> "Grouped arithmetic with generated operands."; 2 -> "Multi-step arithmetic with division."; else -> "Use your selected number size and operations." }
            else when (mastery) { 1 -> "Construct a derivative at a generated input."; 2 -> "Solve a generated two-equation system."; else -> "Use your selected calculation topics." }, accent) {
            SettingsChoices(listOf("Standard", "Mastery 1", "Mastery 2"), { it == mastery }, accent, onMastery)
        }
        val hasSelection = mastery > 0 || if (mode == SpeedCalculationMode.Basic) selectedBasic.isNotEmpty() else selectedAdvanced.isNotEmpty()
        PrimaryGameButton("Start ${formatDuration(duration)} Sprint", accent, onStart, enabled = hasSelection)
        Text("Build and check calculations before time runs out.", color = GameMuted, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
''' + s[end:]
p.write_text(s,encoding='utf-8')
