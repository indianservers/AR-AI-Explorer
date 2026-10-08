from pathlib import Path
p=Path('tmp/interactive_sprint.py');s=p.read_text(encoding='utf-8').replace('Text("ANSWER MODE"','Text("ANSWER STYLE"').replace("b=s.index('\\n        PrimaryGameButton',a)","b=s.index('\\n        val hasSelection',a)")
s=s.replace("# Keep the composable signature",'''s=s.replace('if (answerMode == SpeedAnswerMode.Typed) "Solve as many as you can. Enter a decimal for fractional answers."\\n            else "Solve as many as you can. Tap the correct answer from four choices."', '"Assemble mathematical methods to charge the reactor before time runs out."')
# Keep the composable signature''')
p.write_text(s,encoding='utf-8')
