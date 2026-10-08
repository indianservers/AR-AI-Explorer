from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GamifyMathsScreen.kt')
s=p.read_text(encoding='utf-8');a=s.index('internal data class GameMission(');b=s.index('internal enum class GameDifficulty',a);s=s[:a]+s[b:]
s=s.replace('    val missions: List<GameMission>,\n','').replace('        emptyList(),\n','')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/ExpandedMathsGames.kt');s=p.read_text(encoding='utf-8').replace('        missions = emptyList(),\n','');p.write_text(s,encoding='utf-8')
