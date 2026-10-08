# Mobile maths games audit

Historical layout pass before the generated-game upgrade. See [the current dynamic games audit](dynamic-games-audit.md) for the live catalogue, renamed games, new levels and latest checks.

Verified on 6 October 2026 using an Android phone emulator at **360 × 640 dp**, normal font scale, with status and navigation bar safe areas.

**29 catalogue games; 293 playable screens checked. 293/293 fit without scrolling (maximum measured scroll: 0 dp).**

Validation: debug app and instrumentation APKs built successfully; 18 game unit tests passed; 5 phone instrumentation tests passed. The phone tests check every specialized level and general mission, both square-card menus, hidden shape labels and worked-answer hints, and saved scores/settings after rebuilding the game screen.

The probability and statistics worlds share 15 mini-games with three levels each. Both entry routes are included in the catalogue below; each shared playable screen is measured once.

| Game | Play screens checked | Result |
| --- | ---: | --- |
| Speed Calculation | 1 | Pass |
| Advanced Speed | 1 | Pass |
| Number Forge | 6 | Pass |
| Maths Kitchen | 6 | Pass |
| Fraction Factory | 6 | Pass |
| Potion Lab | 18 | Pass |
| Balance Vault | 72 | Pass |
| Shape Architect | 18 | Pass |
| Rescue Engineer | 21 | Pass |
| Vector Voyager | 8 | Pass |
| Pattern Core | 8 | Pass |
| Data Detective | 45 shared | Pass |
| Chance Reactor | 45 shared | Pass |
| Logic Grid | 8 | Pass |
| Number Bond Garden | 5 | Pass |
| Times Table Galaxy | 5 | Pass |
| Decimal Harbor | 5 | Pass |
| Math Market | 5 | Pass |
| Integer Expedition | 5 | Pass |
| Ratio Rangers | 5 | Pass |
| Percent Studio | 5 | Pass |
| Data Story Lab | 5 | Pass |
| Function Machine | 5 | Pass |
| Geometry Proof Lab | 5 | Pass |
| Calculus Climber | 5 | Pass |
| Matrix Mission | 5 | Pass |
| Number Theory Crypt | 5 | Pass |
| Combinatorics Command | 5 | Pass |
| Optimization Arena | 5 | Pass |

Changes verified:

- Two square game cards per row on Home, Worlds and the probability arcade menu.
- Existing PNG maths artwork on catalogue cards; individual illustrated arcade icons.
- Smaller mobile boards, compact controls, less duplicate instruction text, and paired bridge actions.
- Shape recognition uses geometric properties and unlabeled visual choices; fraction equivalence no longer displays its answer in the comparison diagram.
- Worked explanations appear after a correct check. Pre-answer hints describe a method.
- A settings button opens the saved compact-play preference. Speed games default to tap answers and retain typing as an option.
- Best completed missions and speed scores survive reopening; speed scores are kept separate from mission progress. The previous best is shown when entering a game.
- Ruler exercises show numbered marks for subtracting the start position from the end position.

Scope: the no-scroll measurement covers the initial playable state at the stated phone size and font scale. Optional hints, worked feedback, the software keyboard, smaller landscape screens and larger accessibility text retain scrolling so controls stay reachable.

Evidence: [screen measurements](mobile-games/final/layout.csv), [catalogue](mobile-games/final/catalogue.csv), [home](mobile-games/final/home.png), [shape recognition](mobile-games/final/shapes-0.png), [measurement](mobile-games/final/measure-0.png), [bridge](mobile-games/final/bridge-0.png), [probability menu](mobile-games/final/probability-menu.png), [settings](mobile-games/final/settings.png).
