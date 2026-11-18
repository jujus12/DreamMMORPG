# Prototype scene setup

The repository currently contains the first gameplay scripts. Create the initial scene in Unity:

1. Create a new **3D (Built-In Render Pipeline)** project with Unity 6 LTS, or open this repository as a Unity project after adding the standard Unity project metadata.
2. Save a scene as `Assets/Scenes/Prototype.unity`.
3. Add a Plane at (0, 0, 0). Add a few cubes/capsules as temporary obstacles.
4. Create a Capsule named `Player` at (0, 1, 0). Add a **CharacterController** component and `TopDownPlayerController`.
5. Create an empty object named `Main Camera Rig`, attach `IsometricCameraFollow`, and assign Player as Target. Make the Main Camera a child of the rig or assign it in the component.
6. Create a Capsule named `Training Monster`, add a Collider and `Targetable`. Place it a few metres from the player.
7. Add `HybridCombatController` to Player. Assign the camera and set the target layer mask to the layer used by monsters (or Everything for the first test).
8. Enter Play mode. Use WASD or drag on the left half of the screen to move; click/tap a monster to select it; press Space or tap the right half of the screen to perform a basic attack.

The current scripts are a code foundation, not a finished game scene. Art, animation, touch HUD, networking and save data are later milestones.
