from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GamifyMathsScreen.kt')
s=p.read_text(encoding='utf-8')
start=s.index('private val MathsGame.levelCount: Int')
end=s.index('private val CoreGames',start)
s=s[:start]+'''private val MathsGame.levelCount: Int
    get() = if (id.startsWith("speed-")) 1 else GeneratedLevelCount

'''+s[end:]
start=s.index('                "forge" -> NumberForgeGame')
end=s.index('            }',start)
s=s[:start]+'''                else -> GeneratedMathGameScreen(selectedGame, currentCompleted, { selectedGameId = null }, recordComplete)
'''+s[end:]
s=s.replace('val linkedProgressKeys = if (selectedGame.id in setOf("chance", "data")) listOf("chance", "data") else listOf(selectedGame.id)', 'val linkedProgressKeys = listOf(selectedGame.id)')
for old,new in [('"Speed Calculation"','"Calculation Sprint"'),('"Advanced Speed"','"Advanced Calculation Sprint"'),('"Fraction Factory"','"Fraction Quest"'),('"Balance Vault"','"Equation Escape"'),('"Pattern Core"','"Pattern Detective"'),('"Shape Architect"','"Geometry Builder"')]: s=s.replace(old,new)
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/SpeedCalculationGame.kt')
s=p.read_text(encoding='utf-8')
start=s.index('    // A neutral term preserves')
end=s.index('\n}\n',start)
s=s[:start]+'''    // Exhausted small fact pools grow into a genuinely different calculation.
    val candidate = if (mode == SpeedCalculationMode.Basic) {
        basicProblem(digits, selectedBasic.ifEmpty { setOf("Addition") }, random)
    } else {
        advancedProblem(selectedAdvanced.ifEmpty { setOf("Advanced Calculation") }, random)
    }
    var offset = random.nextInt(1, 1001)
    var prompt: String
    do {
        prompt = """\\left(${candidate.prompt}\\right) + $offset"""
        offset++
    } while (!usedPrompts.add(prompt))
    return candidate.copy(prompt = prompt, answer = compactNumber(candidate.answer.toDouble() + offset - 1))'''+s[end:]
s=s.replace('"Speed Calculation"','"Calculation Sprint"').replace('"Advanced Speed"','"Advanced Calculation Sprint"')
p.write_text(s,encoding='utf-8')
