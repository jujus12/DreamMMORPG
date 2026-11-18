# Warlord — Class Design

## Visual identity
- Large, muscular, broad-shouldered male warrior.
- Thick beard and rugged, weathered face.
- Heavy layered armor with worn leather, metal plates, straps and a large belt.
- Strong silhouette readable from the isometric camera.
- The supplied Warlord base-mesh reference is the visual direction; this is an original game character, not a recreation of a named existing character.

## Combat identity
A durable front-line melee fighter using heavy weapons, broad sweeps and committed strikes.

## Starting skills
All five skills are unlocked from character creation.

1. **Cleave / frontal cone:** Sweeps the weapon through a forward cone, damaging all enemies in the area.
2. **Overhead Strike / single target:** Raises the weapon and brings it down on one selected target for heavy damage.
3. **Whirlwind / 360-degree area:** Spins in place with the weapon, damaging nearby enemies in a circular radius.
4. **Charge / directional area:** Dashes toward the chosen cursor or touch direction and damages enemies in the landing area.
5. **Battle-Hardened / passive:** Gains 5% additional ATK and 5% additional DEF from equipped items.

## Progression
At a defined level, the player may choose a specialization path. Each path grants its own skills. The specialization names, skill lists, level requirement and balance values are intentionally undecided until the player supplies them.

## Implementation notes
- All skill damage, range, cooldown and resource costs must be configurable data, not hard-coded design assumptions.
- Cone, radius and dash direction must be previewable and readable on mobile.
- The passive bonus applies to item-derived ATK and DEF only, not necessarily base stats; confirm exact stat calculation before implementation.
- Keep the character rig and animation setup extensible for weapon-specific attacks.
