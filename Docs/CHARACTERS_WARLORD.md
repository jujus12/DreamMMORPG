# Character Classes and Warlord

## Planned launch classes
1. **Warlord** — classic frontline warrior; weapon-based area and single-target attacks, mobility and passive equipment bonuses.
2. **Mage** — spellcaster (details to be designed).
3. **Runeseer** — priest / support / buffer (details to be designed).
4. **Assassin** — agile close-range damage dealer (details to be designed).
5. **Summoner** — beast- and creature-commanding magic user (details to be designed).

All five classes share the same high-level progression structure: a starter kit of skills, followed by a specialization/path choice at a defined level where applicable. Some classes may have a single fixed progression path. Exact level and branch details are intentionally undecided.

## Warlord — starter skills

All five starter skills are available from the beginning.

| # | Name (working) | Type | Design |
|---|---|---|---|
| 1 | Cleaving Arc | Active / AoE | Sweeps the weapon through a forward cone and damages all enemies in the area. |
| 2 | Overhead Strike | Active / single target | Raises the weapon and brings it down on one target for damage. |
| 3 | Whirlwind | Active / circular AoE | Spins in place with the weapon, damaging enemies around the Warlord. |
| 4 | Charge | Active / movement + AoE | Dashes toward the cursor direction and damages enemies in a defined area. |
| 5 | Battle-Hardened | Passive | Equipped items grant 5% more ATK and 5% more DEF. |

### Combat implementation notes
- Skill damage, range, cooldowns, costs and animation timings are not yet specified.
- The server must ultimately validate movement, damage, cooldowns and equipment-derived stats.
- The first playable prototype can use placeholder values and primitive hit areas, clearly marked as prototype values.
- The Warlord reference image is a large, muscular, bearded fighter with rugged leather/cloth armor, belts and tattoos. Treat it as visual direction, not a final production model.

## Village hub
The first scene is a safe village square:
- A large ancient tree is the central landmark.
- Several NPC placeholders stand around the square.
- Multiple training dummies provide targets for combat testing.
- Simple low-cost buildings, stone paths, grass and warm lighting establish a readable fantasy hub.
- The village is a safe zone; combat rules can be added later.
