# Chrono

A native Android stopwatch for 1–16 athletes. Designed for a Samsung S24 running Android 16, in portrait orientation.

The first version includes athlete names, shared Start/Stop, independent intermediate times, recent split history in roomy zones, CSV export, local saving, and an awake screen while timing. The requirements are in [goals.md](goals.md); the implementation is explained in [docs/design.md](docs/design.md).

## Try it on your phone

The [test APK](downloads/chrono-0.2.0-debug.apk) is **downloads/chrono-0.2.0-debug.apk** (approximately 8.9 MiB). It installs as **Chrono (test)**. To update the earlier test version, install this APK over it; the application identifier and signing key are unchanged.

1. Upload that APK to your personal webpage, using HTTPS.
2. Add a download link. For example, if the APK and webpage are in the same folder:

   ```html
   <a href="chrono-0.2.0-debug.apk" download>Download Chrono test app</a>
   ```

3. Open your webpage on your phone and download the APK.
4. Open it from the download notification or Samsung's My Files app.
5. If Android asks, allow **Install unknown apps** for the browser or file manager opening the file. Follow your phone's installation prompts; the exact screens vary by region and Android policy.
6. Install and open **Chrono (test)**.
7. Name the athletes, tap **Ready to time**, and tap **Start**.
8. Tap athlete zones, then tap **Stop**. Open **Results** or **Export CSV**. Choose Downloads in the file picker to save the CSV locally.

After installation, the app works without Internet access. Sharing a downloaded APK through a USB cable is another option.

This is a development APK signed automatically by the Android build tools. It is suitable for testing. A release APK uses your private release key. The test and release versions have different application identifiers and install separately; their local sessions are separate. CSV exports let you keep results independently of either installation.

Android explains [website distribution](https://developer.android.com/distribute/marketing-tools/alternative-distribution), [APK signing](https://developer.android.com/studio/publish/app-signing), and [developer verification](https://developer.android.com/developer-verification/guides). Recheck the applicable installation route when distributing a release.

## Open and build the project

1. Open this project folder in Android Studio.
2. Let Gradle sync finish. A first sync downloads libraries and needs Internet access.
3. Install **Android SDK Platform 36** through the SDK Manager if prompted.
4. Select a full **JDK 17** or Android Studio's compatible bundled JDK in the Gradle settings. A Java runtime alone does not include the compiler.
5. Select the `app` run configuration. Choose an emulator or connect your phone with USB debugging enabled, then press Run.

Android Studio manages the machine-specific SDK path in `local.properties`. That file is not part of the portable project.

From a terminal with the SDK configured and a full JDK selected:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The last command requires a connected emulator or test phone and resets the app's test-session data. Run it on an emulator when keeping results on a phone.

The generated APK is `app/build/outputs/apk/debug/app-debug.apk`. The copy under `downloads/` is convenient for transfer to your webpage. Neither generated APKs nor local build caches need to be committed.

## Read the code in this order

1. [Stopwatch.kt](app/src/main/java/fr/aurelien/chrono/Stopwatch.kt): athletes, session records, and timing arithmetic. This file does not depend on Android.
2. [CsvExport.kt](app/src/main/java/fr/aurelien/chrono/CsvExport.kt): turns a completed session into spreadsheet rows.
3. [SessionStore.kt](app/src/main/java/fr/aurelien/chrono/SessionStore.kt): reads and writes the saved JSON file.
4. [ChronoViewModel.kt](app/src/main/java/fr/aurelien/chrono/ChronoViewModel.kt): captures clock readings, coordinates storage, and responds to screen actions.
5. `SetupScreen.kt`, `TimingScreen.kt`, and `ResultsScreen.kt`: the three visible views.
6. [ChronoApp.kt](app/src/main/java/fr/aurelien/chrono/ChronoApp.kt): connects views, CSV destination selection, screen-awake behavior, and confirmation dialogs.
7. `MainActivity.kt` and `AndroidManifest.xml`: Android's entry point and application settings.
8. `app/build.gradle.kts`: dependency versions, Android support, and APK versioning.

## Build a release APK

In Android Studio, choose **Build → Generate Signed Bundle/APK → APK**, select the `app` module, create or select your private keystore, and choose the release build. Keep the keystore and passwords privately backed up and outside the repository and website.

Upload only the resulting signed APK. `assembleRelease` alone produces an unsigned APK unless release signing has been configured, so use the signing wizard for the initial release.

For later updates, retain the application identifier and signing key and increase `versionCode` in `app/build.gradle.kts`. Verify the update preserves saved results. A different signing key cannot update the existing installation.

## Current practical limits

Only the latest session is kept in the app. Export before clearing it. There is no pause/resume or individual finish action.

Running timing can survive backgrounding, locking, activity recreation, and reopening on the same device boot. A reboot marks a previously running session interrupted; saved splits remain exportable. Sudden process termination can lose a split whose background save has not finished.

The 16-zone layout must also be tried on your S24. With very large system text, the app may ask for fewer athletes or a smaller text size before enabling Start. Emulator validation is documented in [docs/testing.md](docs/testing.md).

## Screenshots and verification

See [recent splits with 8 athletes](docs/screenshots/history-eight.png), [16 athletes with larger text](docs/screenshots/sixteen-larger-text.png), and the [validation record](docs/testing.md). The first build passed 18 unit tests and 7 Android tests, with zero lint errors. Version 0.2.0 adds adaptive recent-split history; its verification is recorded in the validation guide.
