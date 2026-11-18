# DreamMMORPG - Godot Prototype

This folder contains the Android-first Godot prototype for DreamMMORPG.

## Open on an Android tablet
1. Install Godot Editor for Android.
2. Download the ZIP from the repository Releases page.
3. Extract it on the tablet.
4. In Godot, import the extracted folder that contains `project.godot`.
5. Open and run the project. The project uses the Compatibility renderer for mobile support.

## Prototype controls
- Tap the ground to move.
- Tap the training dummy to select it.
- Use the bottom action buttons to attack and use Warlord abilities.
- Keyboard: WASD / arrow keys; Space for basic attack.

## Included
- Greybox village square with central ancient tree.
- Elevated isometric-style camera.
- Player movement and touch ray selection.
- Training dummy with health and respawn.
- Warlord Cleave, Overhead Strike, Whirlwind and Charge prototype logic.
- Mobile action bar.

## Not yet included
- Final realistic character/environment art, animation and effects.
- Equipment, stats and passive implementation.
- NPC dialogue, quests, inventory, progression and other classes.
- Multiplayer networking and authoritative game server.
- Android APK export.

The GitHub Actions workflow validates/imports the project and publishes a source ZIP. It does not produce an APK.