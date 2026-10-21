# RivaDraws

RivaDraws is an offline-first Android colouring book prototype focused on adult colouring patterns.

## Features in this starter
- Three vector-drawn mandala / rosette / geometric designs (drawn locally; no network needed)
- Tap-to-fill mode for enclosed white regions
- Finger brush mode
- Colour palette
- Undo up to 12 recent actions
- Save artwork as PNG to `Pictures/RivaDraws`

## Requirements
- Android Studio with JDK 17
- Android SDK platform 35
- Gradle 8.9 or a compatible Gradle installation (Android Gradle Plugin 8.7.3)

## Open and build
1. Download or clone this repository.
2. Open the project root in Android Studio.
3. Allow Gradle sync to complete and install any requested Android SDK components.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio will show the generated APK location when the build completes.

The repository does not include the Gradle wrapper JAR, so opening the project in Android Studio is the simplest first build route.

## Notes
This is an initial source-code foundation, not a tested release APK. Flood-fill is designed for enclosed white regions and may need tuning for small or anti-aliased areas. Run it on an emulator/device and test tap fill, brush, undo, rotation, and PNG saving before distributing it.
