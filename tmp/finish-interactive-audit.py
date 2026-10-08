from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/RescueEngineerGame.kt');p.write_text(p.read_text(encoding='utf-8').rstrip()+'\n',encoding='utf-8')
p=Path('docs/qa/interactive-games-audit.md');s=p.read_text(encoding='utf-8').replace('Device log: `tmp/interactive-games-final-device.log`.','Touch/session retest log: `tmp/interactive-games-final-device.log` (all four pass). Layout/settings log: `tmp/interactive-games-complete-device.log` (all five pass; its replay assertion was corrected and passed in the retest).').replace('Replaying starts a new seed.','Replaying starts a new seed. Next-board generation also rejects an immediately repeated construction prompt.')
p.write_text(s,encoding='utf-8')
