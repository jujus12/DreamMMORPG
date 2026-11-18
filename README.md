# DreamMMORPG

An Android-first, isometric 3D online RPG prototype inspired by classic MMORPG progression and modern action combat.

## Design decisions
- **Platform:** Android
- **Engine:** Unity 6 LTS
- **Camera:** Isometric / three-quarter top-down
- **Visual direction:** Realistic dark-fantasy
- **World structure:** Large connected zones
- **Combat:** Hybrid — target selection plus manually triggered abilities
- **Initial scope:** Small playable prototype, not a full-scale MMO

## Prototype milestones
1. Player movement and isometric camera
2. Target selection and hybrid combat
3. First zone: a small safe outpost and nearby monster field
4. Health, monster respawn, XP and leveling
5. Online session and seeing other players

## Repository layout
- `Assets/Scripts/Player` — player movement and camera
- `Assets/Scripts/Combat` — target selection and combat
- `Assets/Scripts/World` — reusable world/zone scripts
- `Docs` — design and setup notes

## Open the project
1. Install **Unity Hub** on a Windows, macOS, or Linux computer.
2. Install a current **Unity 6 LTS** editor with **Android Build Support**, Android SDK & NDK Tools, and OpenJDK.
3. Clone this repository and open its project folder in Unity Hub.
4. Create a 3D scene, add a Plane, then add a Player object with a CharacterController and the scripts documented in `Docs/PROTOTYPE_SETUP.md`.

> Unity Editor does not run natively on Android tablets. The tablet can be used to review the repository and test APK builds; creating and editing the Unity scene requires a desktop computer or a suitable remote/cloud workstation.

## Important
The first milestone is a local playable foundation. Online multiplayer, persistence, economy, trading, guilds and large-scale PvP will be added only after the core gameplay is stable.
