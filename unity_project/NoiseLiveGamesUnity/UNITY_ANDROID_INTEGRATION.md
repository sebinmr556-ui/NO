# NOISE LIVE – Unity 6 (URP) as a Library Android Integration Guide

## 1. Architecture Overview
NOISE LIVE uses a hybrid social + 3D multiplayer gaming architecture:
- **Android Host App (`:app`)**: Kotlin + Jetpack Compose, Firebase Auth, Cloud Firestore (`game_rooms`, `game_invites`, `game_history`), Firebase Storage, and **LiveKit Voice Chat (`LiveKitAudioRouter`)**.
- **Unity 6 Game Engine (`/unity_project/NoiseLiveGamesUnity`)**: Universal Render Pipeline (URP), Unity Netcode for GameObjects, Unity Lobby, and Unity Relay.
- **Built-in 3D Projected Physics & Multiplayer Engine (`Playable3DGamesEngine.kt`)**: Runs immediately inside the Android app and synchronizes real-time state over Firestore + LiveKit voice chat, while `UnityGameBridge.kt` automatically delegates to `com.unity3d.player.UnityPlayerActivity` whenever the exported `:unityLibrary` module is linked.

## 2. Exporting Unity 6 as an Android Library (`unityLibrary`)
1. Open `/unity_project/NoiseLiveGamesUnity` in **Unity 6 (6000.0+)**.
2. Go to **File → Build Profiles → Android**:
   - Enable **Export Project** (Unity as a Library).
   - Scripting Backend: **IL2CPP**.
   - Target Architectures: **ARM64**.
   - Export to `/unity_project/exported_android/`.
3. In `settings.gradle.kts`, include `:unityLibrary` if building locally with the exported Unity library:
   ```kotlin
   // Optional when exported unityLibrary folder is present:
   // include(":unityLibrary")
   // project(":unityLibrary").projectDir = file("unity_project/exported_android/unityLibrary")
   ```
4. `UnityGameBridge.isUnityLibraryLinked()` automatically detects `com.unity3d.player.UnityPlayerActivity` at runtime and passes `NOISE_UNITY_SESSION_PAYLOAD` containing User ID, Username, Profile Photo, Room ID, Selected Game, Player List, Voice Channel ID, and Graphics Quality settings.
