from pathlib import Path
root=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths')
def balanced_end(s,start):
 depth=0;quoted=False;escaped=False
 for i in range(start,len(s)):
  ch=s[i]
  if quoted:
   if escaped:escaped=False
   elif ch=='\\':escaped=True
   elif ch=='"':quoted=False
   continue
  if ch=='"':quoted=True
  elif ch=='(':depth+=1
  elif ch==')':
   depth-=1
   if depth==0:return i+1
 raise RuntimeError('unbalanced')
for filename in ['GamifyMathsScreen.kt','ExpandedMathsGames.kt']:
 p=root/filename;s=p.read_text(encoding='utf-8')
 import re
 starts=list(re.finditer(r'listOf\(\s*GameMission\(',s))
 for m in reversed(starts):
  opening=s.index('(',m.start());end=balanced_end(s,opening);s=s[:m.start()]+'emptyList()'+s[end:]
 if filename=='GamifyMathsScreen.kt':
  s=s.replace('import com.indianservers.aiexplorer.gamifymaths.probability.ProbabilityStatisticsArcadeGame\n','')
  a=s.index('@Composable\nprivate fun DropReactor');b=s.index('@Composable\nprivate fun BottomNavigation',a);s=s[:a]+s[b:]
 p.write_text(s,encoding='utf-8')
# All these unreachable prototype screens carried fixed banks. Retain the bridge mathematics contract.
p=root/'RescueEngineerGame.kt';s=p.read_text(encoding='utf-8');a=s.index('internal data class RescueBridgeAssessment');b=s.index('private val EngineerConcepts',a)
p.write_text('package com.indianservers.aiexplorer.gamifymaths\n\n'+s[a:b],encoding='utf-8')
# Use a fixed, verified list of files inside this repository; no recursive filesystem operations.
legacy=['AlgebraAdventureGame.kt','NumberForgeGame.kt','MathsKitchenGame.kt','FractionFactoryGame.kt','ShapeArchitectGame.kt','PotionLabGame.kt']
for filename in legacy:(root/filename).unlink()
for p in (root/'probability').glob('*.kt'):p.unlink()
