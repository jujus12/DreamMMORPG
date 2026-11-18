# DreamMMORPG — Godot Prototype

This folder is the engine-specific playable prototype. The Unity scaffold is intentionally preserved elsewhere in the repository.

## Current foundation
- Godot 4.x, GDScript (Android editor does not support C#).
- Isometric-style camera starting point.
- 3D player movement using keyboard actions.
- Target dummy with health and basic damage interface.
- Simple village-square blockout with an ancient-tree placeholder.

## Next milestones
1. Add touch-to-move and touch target selection.
2. Implement Warlord skills: Cleave, Overhead Strike, Whirlwind, Charge, and equipment passive.
3. Add class/stat/skill data as engine-neutral resources or JSON.
4. Replace primitive blockout meshes with proper models, animation, lighting, and environment art.
5. Design multiplayer authority and persistence separately before connecting a server.

## Tablet workflow
The official Godot Android editor can open and edit projects on Android. It is experimental and has limitations; use GDScript and the Compatibility renderer. Keep this prototype lightweight for the Lenovo tablet.
