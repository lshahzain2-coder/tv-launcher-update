# MStar Modern Launcher v1.1 (Android 4.0.x / API 15)

Fixes in this version
- Crash fix: View.setBackground() (API 16) replaced with setBackgroundDrawable()
- App list now loads in the background (no freeze when returning Home)
- launchMode singleTask (no stacked copies), Back no longer exits
- Removed unneeded GET_TASKS permission and the gear glyph old fonts can't draw
- Build files updated: Android Gradle Plugin 7.4.2, Gradle 7.5, no JCenter

Build APK (Windows, Android Studio)
1. Extract the zip, then File > Open the MStarModernLauncher folder.
2. If asked to upgrade the Android Gradle Plugin, choose "Don't remind me" / No.
3. Install JDK 11 (Studio's bundled JDK works) and SDK Platform 33 if prompted.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. APK: app/build/outputs/apk/debug/app-debug.apk

Install on the TV
1. Copy app-debug.apk to a FAT32 USB stick.
2. TV Settings > Security > Unknown sources: ON.
3. Open the APK from the TV's file manager and install.
4. Press Home and choose MStar Home (try "Just once" first).
