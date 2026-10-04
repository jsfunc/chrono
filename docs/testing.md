# Validation

Target phone: Samsung S24 with Android 16. Physical-phone validation still requires installing the APK on that phone.

## Automated checks

Run with a full Java development kit selected:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The connected tests use an Android emulator and reset the test app's session file. Do not run them against a phone containing results you want to keep.

Unit tests cover shared starts, independent intervals, stop behavior, rapid taps, invalid taps, reset protection, blank/duplicate names, maximum athlete count, same-boot recovery, reboot/unknown-boot interruption, rounding boundaries, CSV escaping, spreadsheet formula handling, and exports without fabricated finish times.

Android tests cover naming, actual screen taps, results, cancelled and confirmed reset, all 16 visible zones, backgrounding/activity recreation, and JSON storage with corrupt-save preservation.

## Checks on your S24

1. Try 8 athletes, enter names, and check zone order and readability.
2. Start and alternate taps between several athletes. Confirm each split count and first total/interval match.
3. Record several splits and stop. Inspect the complete Results list.
4. Export a CSV to Downloads. Open it in a text viewer or spreadsheet and inspect both times and the separate stop rows.
5. Cancel a second export and cancel New Session; results should remain.
6. Close and reopen the app; the saved completed session should remain.
7. During another session, briefly lock the phone and return. Confirm timing includes that interval.
8. Leave a session visibly running beyond the phone's normal screen timeout; the screen should stay on. Stop and check that normal timeout behavior returns.
9. Try 16 athletes, including the last zone, using normal and larger system text sizes. Check practical tap accuracy as well as visibility.
10. Before installing a future update, retain a session and export it. Confirm the update keeps that session.

Tenth-second display resolution does not remove human reaction time. An emulator cannot establish touch usability on your physical phone.

## Initial build verification — 4 October 2026

- `assembleDebug` succeeded; the APK is approximately 8.9 MiB.
- 18 JVM unit tests passed: 13 stopwatch/recovery/rounding tests and 5 CSV tests.
- 7 Android tests passed on an Android 16 (API 36) emulator, including a regression test for all 16 total and interval values at 1.3x text scaling.
- Android lint passed with zero errors. Advisory warnings include available newer dependency versions and the intentional portrait orientation. Versions are pinned to the tested toolchain; see design.md.
- APK signature verification passed (APK Signature Scheme v2).
- A CSV exported through the actual Android file picker contained 5 recorded splits and 8 separate stop rows. Every exported total and interval matched the saved millisecond data after rounding.
- Cancelling the actual CSV picker left the saved session unchanged.
- Force-stopping and reopening the process restored completed results and also preserved a running session's original start reference and five recorded taps on the same device boot.
- With charger stay-awake disabled and the effective timeout set to 10 seconds, the display remained awake for 12 seconds while running. After Stop, it slept normally. Original emulator settings were restored.
- All 16 names, total lines, and interval lines were checked in the actual emulator UI at normal and 1.3x text scaling after fixing clipped interval text.

Screenshots from the built app:

- [Setup](screenshots/setup.png)
- [Timing 8 athletes](screenshots/timing-eight.png)
- [Stopped session](screenshots/stopped-eight.png)
- [Results](screenshots/results.png)
- [16 athletes](screenshots/sixteen.png)
- [16 athletes with larger text](screenshots/sixteen-larger-text.png)

Not yet verified: installation, touch usability, and update behavior on the physical S24; a privately signed release build and public hosting. The current delivered artifact is a signed development/test APK, not a published release.


## Version 0.2.0 — adaptive recent split history

- 18 unit tests and 9 Android tests passed, with zero lint errors.
- Roomy zones show the latest total/interval plus up to three earlier splits, newest first.
- A layout test checks the ordering, visibility, and tapping of recent rows; another checks that narrow zones with larger text keep the latest values without a clipped history table.
- The 16-zone larger-text regression continues to pass, using multiple saved splits to exercise compact-zone history suppression.
- Installing 0.2.0 over 0.1.0 on the emulator preserved all 8 athlete names and all 56 splits in an example saved session.
- [Recent split history with 8 athletes](screenshots/history-eight.png) shows the built app using that example session. On this screen, two earlier splits fit beneath the latest split in every zone.

The updated test APK is `downloads/chrono-0.2.0-debug.apk`. Physical S24 validation remains a user-side check.
