# Game Design — DreamMMORPG

## Pillars
1. **Readable tactical combat:** players can select a target, position freely, and manually activate abilities.
2. **Meaningful progression:** defeating monsters grants experience and useful loot.
3. **Social world:** zones are shared spaces where players can meet and cooperate.
4. **Mobile-first usability:** large touch targets, clear feedback, scalable UI and short interaction paths.

## First playable slice
- One controllable adventurer
- One compact outdoor zone connected to a safe outpost
- One basic monster type
- Target selection, basic attack and one manually triggered ability
- Health, death/respawn and simple XP feedback

## Camera and controls
- Isometric perspective with a fixed, readable angle and smooth follow.
- Desktop testing: WASD movement and mouse target selection.
- Touch prototype: left-side drag movement; tap a monster to select it; use an on-screen ability button in the next UI pass.

## Hybrid combat definition
- Target-based: selecting a monster establishes the combat target.
- Action-based: movement, facing, range and ability timing still matter.
- The server will eventually validate damage, cooldowns, movement and rewards. The client must never be authoritative for MMO state.

## Out of scope for the first milestone
Guilds, auction house, player trading, large-scale PvP, crafting, multiple classes, cross-zone seamless streaming and monetization.
