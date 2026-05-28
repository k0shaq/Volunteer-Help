# AGENTS.md

## Project
Aidly is an Android Kotlin Jetpack Compose diploma app for a volunteer social network.

## Rules
- Do not rewrite the project from scratch.
- Preserve ViewModel / Repository / Compose architecture.
- Prefer small safe patches.
- Keep existing navigation routes unless necessary.
- Do not change package name.
- Do not add backend, email verification, forgot password, or admin moderation.
- Do not remove simulated volunteer verification.
- Do not block duplicate help requests.
- Use Material3 and existing reusable components when possible.
- Centralize constants and limits instead of scattering magic numbers.
- After meaningful code changes, run assembleDebug when possible.

## Build
- Windows: gradlew.bat assembleDebug
- Unix/macOS/Linux: ./gradlew assembleDebug
