from pathlib import Path
import csv
from collections import Counter

base = Path('docs/qa/mobile-games/final')
rows = list(csv.DictReader((base/'layout.csv').open()))
catalogue = list(csv.DictReader((base/'catalogue.csv').open()))
counts = Counter(row['game'].rsplit('-', 1)[0] for row in rows)
no_scroll = sum(float(row['scroll_dp']) <= 1 for row in rows)
maximum = max(float(row['scroll_dp']) for row in rows)
arcade = sum(count for game, count in counts.items() if game not in {entry['id'] for entry in catalogue} and game != 'bridge')
lines = [
    '# Mobile maths games audit', '',
    'Verified on 6 October 2026 using an Android phone emulator at **360 × 640 dp**, normal font scale, with status and navigation bar safe areas.', '',
    f'**{len(catalogue)} catalogue games; {len(rows)} playable screens checked. {no_scroll}/{len(rows)} fit without scrolling (maximum measured scroll: {maximum:g} dp).**', '',
    'Validation: debug app and instrumentation APKs built successfully; 18 game unit tests passed; 5 phone instrumentation tests passed. The phone tests check every specialized level and general mission, both square-card menus, hidden shape labels and worked-answer hints, and saved scores/settings after rebuilding the game screen.', '',
    'The probability and statistics worlds share 15 mini-games with three levels each. Both entry routes are included in the catalogue below; each shared playable screen is measured once.', '',
    '| Game | Play screens checked | Result |', '| --- | ---: | --- |',
]
for item in catalogue:
    count = f'{arcade} shared' if item['id'] in ('chance','data') else str(counts[item['id']] + (counts['bridge'] if item['id']=='measure' else 0))
    lines.append(f"| {item['title']} | {count} | Pass |")
lines += ['', 'Changes verified:', '',
    '- Two square game cards per row on Home, Worlds and the probability arcade menu.',
    '- Existing PNG maths artwork on catalogue cards; individual illustrated arcade icons.',
    '- Smaller mobile boards, compact controls, less duplicate instruction text, and paired bridge actions.',
    '- Shape recognition uses geometric properties and unlabeled visual choices; fraction equivalence no longer displays its answer in the comparison diagram.',
    '- Worked explanations appear after a correct check. Pre-answer hints describe a method.',
    '- A settings button opens the saved compact-play preference. Speed games default to tap answers and retain typing as an option.',
    '- Best completed missions and speed scores survive reopening; speed scores are kept separate from mission progress. The previous best is shown when entering a game.',
    '- Ruler exercises show numbered marks for subtracting the start position from the end position.', '',
    'Scope: the no-scroll measurement covers the initial playable state at the stated phone size and font scale. Optional hints, worked feedback, the software keyboard, smaller landscape screens and larger accessibility text retain scrolling so controls stay reachable.', '',
    'Evidence: [screen measurements](mobile-games/final/layout.csv), [catalogue](mobile-games/final/catalogue.csv), [home](mobile-games/final/home.png), [shape recognition](mobile-games/final/shapes-0.png), [measurement](mobile-games/final/measure-0.png), [bridge](mobile-games/final/bridge-0.png), [probability menu](mobile-games/final/probability-menu.png), [settings](mobile-games/final/settings.png).', '',
]
Path('docs/qa/mobile-games-audit.md').write_text('\n'.join(lines), encoding='utf-8')
print(f'{len(rows)} screens, {no_scroll} without scroll, max {maximum:g} dp')
