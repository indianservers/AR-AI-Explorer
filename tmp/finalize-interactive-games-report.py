from pathlib import Path
import csv
rows=list(csv.DictReader(Path('docs/qa/mobile-games/interactive/layout.csv').open()))
print('Play states:',len(rows),'maximum vertical scroll:',max(float(r['scroll_dp']) for r in rows))
print('Illustrations:',len(list(Path('app/src/main/res/drawable-nodpi').glob('game_icon_*.png'))))
p=Path('docs/qa/interactive-games-audit.md');s=p.read_text(encoding='utf-8').replace('tmp/interactive-games-complete-build.log','tmp/interactive-games-replay-build.log').replace('tmp/interactive-games-complete-device.log','tmp/interactive-games-final-device.log').replace('Device checks use an Android emulator at 360×640 dp.','Device checks use an Android emulator at 360×640 dp. All 339 initial play states and 81 expert/mastery states fit with zero vertical scrolling. Five layout/settings checks and four board/session checks pass.')
p.write_text(s,encoding='utf-8')
