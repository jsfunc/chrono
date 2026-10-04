# Chrono — project goals

Status: first test build implemented and verified on an Android 16 emulator; physical-phone validation and a release signing key remain delivery steps. Confirmed target phone: Samsung S24 with Android 16. See README.md for setup and installation, and docs/design.md for implementation explanations.

## Purpose

Create a simple Android application for timing several athletes simultaneously.

The screen contains a named zone for each athlete. Everyone starts together using one bottom button. Tapping an athlete's zone records both their total elapsed time and their interval since the previous tap.

The project must remain understandable to someone building their first mobile application. Explain each significant design choice, source file, and build step.

## Confirmed requirements

| Feature | Requirement |
| --- | --- |
| Platform | Android application installed from an APK |
| Distribution | APK downloadable from the owner's personal webpage |
| Orientation | Portrait only |
| Athlete count | Typically 8; maximum 16 |
| Identification | Configurable name for each athlete's zone |
| Starting | One shared Start button at the bottom |
| Stopping | The same button becomes Stop and stops everyone |
| Intermediate times | Each tap records total elapsed time and interval since that athlete's previous tap |
| Recent split display | Show several recent intermediate times in zones with enough room |
| Export | CSV file suitable for spreadsheets |
| Screen behavior | Screen stays awake during timing |
| Language | English |
| Resolution | Times displayed and exported to 0.1 second |
| Development approach | Clean, simple implementation with explanations |

## Proposed defaults

These choices fill gaps in the discussion and can be revised before implementation:

- Support 1–16 athletes, initially showing 8.
- Work offline after installation.
- Remember athlete names and the current or latest completed session locally.
- Keep one session rather than building a session-history browser.
- Provide a separate New Session action after stopping.
- Ask before clearing results for a new session.
- Do not include pause, resume, individual finishes, or editing recorded splits initially.
- Elapsed time continues if the app is temporarily covered or the phone is locked. Recording a split requires returning to the app.
- Recover saved results after restarting the app. Recovery of an actively running timer after a phone reboot is outside the first version.

Target phone: Samsung S24 running Android 16. The first build targets Android 16 (API 36), with Android 8 (API 26) as the minimum. Final readability and touch validation still require the physical phone.

## User workflow

### 1. Configure athletes

Choose the athlete count and enter names before starting.

Blank names receive defaults such as "Athlete 1." Each athlete also has a stable number, so duplicate names do not mix results.

The timing screen displays the athletes in the configured order. Names and athlete count remain fixed throughout a session.

### 2. Start together

Tap the shared Start button.

The application captures one start timestamp for the entire session. All athletes therefore have exactly the same starting reference.

The button immediately becomes Stop.

### 3. Record intermediate times

Tap anywhere inside an athlete's zone.

That athlete receives a new split record containing:

- Split number.
- Total time since the shared start.
- Interval since their previous tap.

The first interval equals the first total time.

Each athlete's records are independent. Athletes can be tapped in any order and can have different numbers of splits.

### 4. Stop together

Tap Stop to end timing for everyone immediately.

Stop records the common session duration. It does not add an intermediate split to every athlete or imply that they finished at that moment.

Further taps cannot add timing records.

### 5. Review and export

Review each athlete's complete split list and export the session to CSV.

Results remain available until a new session replaces them. New Session asks for confirmation before clearing the previous results. Athlete names remain available for reuse.

Stopping is final for that session; there is no resume action initially.

## Screen layout

Use three simple views: setup, timing, and results.

### Setup

Provide an athlete-count selector and a list of editable names. Dismiss the keyboard before opening the timing screen.

### Timing

Divide the screen into:

- A small header showing the shared elapsed time and session state.
- Athlete zones filling the middle.
- A fixed bottom area containing the shared Start/Stop button.

Proposed grid:

| Athlete count | Arrangement |
| --- | --- |
| 1–4 | One column |
| 5–16 | Two columns |
| 8 | Two columns × four rows |
| 16 | Two columns × eight rows |

Fill zones left to right, then top to bottom. For odd counts, leave the unused position inactive.

All athletes must remain visible without scrolling during timing. Respect system status bars, camera cutouts, and the navigation area.

Each zone shows the athlete's number and name, split count, latest recorded total, and latest interval. Before the first tap, recorded times show a dash.

The shared timer shows the current elapsed time. Each zone keeps its latest recorded total and interval prominent. When space allows, it also shows up to three earlier splits underneath, newest first, with the split number, total, and interval. The history size adapts to zone height, width, and system text size. These rows remain part of the zone's tap target and do not scroll.

Use readable digits, clear boundaries, strong contrast, and a brief visual highlight when a split is accepted. The entire zone acts as its touch target.

Long names may be shortened on the grid, but remain complete in results and exports.

The 16-athlete layout requires testing on the owner's phone. Check normal and enlarged font settings, readable text, and sufficient touch-target size. Aim for touch targets at least 48 dp high, where dp is Android's unit that adapts to screen density. If the layout does not fit practically, revise the design before proceeding. See [Compose accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

### Results and session states

| State | Shared bottom action | Zone behavior |
| --- | --- | --- |
| Ready | Start | No timing records |
| Running | Stop | Record a split |
| Stopped | New Session | Open recorded results |

After Stop, make each athlete's complete split list accessible. Provide Export CSV and New Session actions.

Cancelled exports and cancelled reset confirmations must preserve results. Start and Stop act immediately, without a confirmation that would delay timing. Repeated button input must respect the updated state and cannot create overlapping sessions.

## Timing calculations

Let:

- `S` be the shared start timestamp.
- `T[n]` be an athlete's timestamp for tap number `n`.

Calculate:

```text
Total[n] = T[n] − S
Interval[1] = T[1] − S
Interval[n] = T[n] − T[n−1]
```

For example:

| Tap | Total | Interval |
| --- | ---: | ---: |
| First | 12.3 seconds | 12.3 seconds |
| Second | 25.1 seconds | 12.8 seconds |

Capture timestamps immediately when handling Start, Stop, and zone taps, before saving or formatting data.

Use Android's monotonic elapsed-time clock. Changing the phone's calendar time must not affect results. Calculate durations from timestamps rather than counting display refreshes.

Store milliseconds internally. Calculate differences before rounding, then display and export to the nearest tenth of a second. Refresh the shared visible time approximately every tenth of a second.

Display durations as `mm:ss.t`, allowing minutes beyond 59. Export numeric seconds with one decimal place.

Independently rounded totals and intervals can occasionally differ by a tenth when subtracted manually. Display resolution also does not remove human reaction time.

A completed tap produces one record. Avoid arbitrary delays that could reject legitimate rapid taps.

If a zone tap and Stop occur close together, their processing order determines whether the split is included.

## CSV format

Export one session as a UTF-8 CSV file.

Suggested filename:

```text
chrono_YYYY-MM-DD_HH-mm-ss.csv
```

Use Android's file picker so the user chooses where to save it.

| Column | Meaning |
| --- | --- |
| `session_id` | Unique session identifier |
| `started_at` | Start date and time, including timezone offset |
| `athlete_number` | Athlete's stable number |
| `athlete_name` | Complete configured name |
| `event` | `split` or `stop` |
| `split_number` | Athlete's split number; blank for stop |
| `total_seconds` | Time since the shared start |
| `interval_seconds` | Time since the previous tap; blank for stop |

Group records by athlete, ordered by split number. Follow each athlete's splits with a stop row recording the common session duration.

This represents athletes who received no taps and clearly distinguishes Stop from an intermediate time.

Example:

```csv
session_id,started_at,athlete_number,athlete_name,event,split_number,total_seconds,interval_seconds
example-001,2026-10-04T10:00:00+02:00,1,Alice,split,1,12.3,12.3
example-001,2026-10-04T10:00:00+02:00,1,Alice,split,2,25.1,12.8
example-001,2026-10-04T10:00:00+02:00,1,Alice,stop,,30.0,
example-001,2026-10-04T10:00:00+02:00,2,Bob,stop,,30.0,
```

Use commas as separators and decimal points for numbers. Correctly escape names containing commas, quotes, or line breaks. Handle names that spreadsheets could interpret as formulas. Explain delimiter selection when importing into a spreadsheet configured for a different locale.

An interrupted session with no known Stop exports its saved splits without inventing stop records.

Report export failures clearly and retain the session for another attempt.

## Technical choices and explanations

These are proposed implementation choices. Choose exact dependency versions when implementation starts and explain every introduced dependency and meaningful project file.

**Kotlin:** Use one language for Android interface code and timing logic.

**Jetpack Compose:** Describe the interface in Kotlin and update it from session state. This is Android's recommended native interface toolkit. [Android Compose overview](https://developer.android.com/compose)

**Android Studio and Gradle:** Use the standard Android development tools to inspect the project, run it, and produce APKs.

**Small application structure:** Use one Android activity hosting setup, timing, and results. An activity hosts the visible Android interface. Keep timing calculations separate from interface rendering so they are easy to explain and test.

**Timestamp-based timing:** Use `SystemClock.elapsedRealtime()`, which includes device sleep and is unaffected by calendar-clock adjustments. [SystemClock reference](https://developer.android.com/reference/android/os/SystemClock)

**Local saving:** Use a small local JSON file for configuration and the latest session. Save timing events in order, outside immediate touch handling, and use atomic replacement to protect the previous saved file. Save after Start, each split, and Stop, and report storage failures. Abrupt termination may lose an event whose save has not completed.

Restore active timing only when stored timestamps can be established as belonging to the current phone boot. Otherwise, preserve the splits and mark the session interrupted.

**Screen stays awake:** Set Android's View.keepScreenOn property while the running timing screen is visible. Clear it after Stop; backgrounding the app allows normal screen sleep. [Android screen-on guidance](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on)

**CSV destination:** Use Android's document picker instead of requesting broad storage access. [Android file access guide](https://developer.android.com/training/data-storage/shared/documents-files)

No account, server, advertising, analytics, cloud synchronization, or Internet permission is needed. The website hosts the installation file; the installed app runs independently. Timestamp calculations do not require a continuously running background service.

## APK installation and updates

An APK is the file Android installs as an application. The personal website is a download location, not the place where the stopwatch runs.

The delivery instructions must explain these steps:

1. Install Android Studio and the required Android SDK tools.
2. Open the project and explain its folders and build configuration.
3. Test the application on an emulator and the owner's phone.
4. Generate a signed release APK using **Build → Generate Signed Bundle/APK**, choosing APK.
5. Explain the signing key and keystore. Back them up privately because future compatible updates need the same signing identity.
6. Upload the APK to the personal webpage and provide an HTTPS download link. Keep signing keys and passwords private and outside the repository.
7. Open the webpage on the Android phone and download the APK.
8. Open the downloaded file. If prompted, allow **Install unknown apps** for the browser or file manager being used. The exact wording depends on the phone and Android version.
9. Complete installation and launch the app from its icon.
10. Run a short timing session and export a CSV to verify the complete workflow.

Android documents both [website APK distribution](https://developer.android.com/distribute/marketing-tools/alternative-distribution) and [release signing](https://developer.android.com/studio/publish/app-signing).

Check installation requirements for the owner's phone and region at delivery time. Android's developer-verification rollout may add requirements or installation steps depending on the distribution route. [Developer-verification guide](https://developer.android.com/developer-verification/guides)

For updates, build an APK with the same application identifier and signing key and an increased version code. Download and install it as an update, then verify that saved data remains intact.

USB installation can be documented as a development alternative. The requested webpage-download path remains the delivery goal. Publishing to Google Play is outside the first version.

## Development and learning sequence

1. Complete this specification and identify the target phone.
2. Create the minimal Android project and explain how to run it.
3. Build athlete setup and the portrait grid.
4. Validate the 8- and 16-athlete layouts on the phone.
5. Add shared Start/Stop and independent splits.
6. Add screen-awake behavior and local saving.
7. Add results review and CSV export.
8. Validate the complete workflow.
9. Produce the signed APK and explain webpage installation and updates.

Before each step, explain its purpose and design choices. Afterward, identify the relevant code and demonstrate how to test it.

Use descriptive names, short functions, and comments explaining reasons. Update this document when agreed choices change.

The owner should understand how the grid is calculated, how a zone identifies an athlete, how session states control the interface, how timestamps become splits, how records are stored and exported, and how a project becomes an installable APK.

The first test build is implemented. Release signing, physical-phone validation, and webpage hosting remain delivery steps. The current work does not include publishing a release or uploading a webpage.

## Acceptance criteria

The first version is complete when:

- Named zones support 1–16 athletes in portrait orientation.
- All zones and the shared button remain visible and usable on the target phone.
- Start gives everyone one common starting timestamp.
- Each running-zone tap adds exactly one record to the correct athlete, including taps on a displayed history row.
- Roomy zones show recent split totals and intervals in newest-first order. Compact zones keep the latest values readable.
- Totals and intervals calculate correctly and show tenths of a second.
- The 12.3 / 25.1 example produces intervals 12.3 / 12.8, independently of other athletes' taps.
- Stop freezes everyone without creating artificial splits.
- Timing works offline and the screen stays awake while visibly running.
- Temporary backgrounding or locking preserves elapsed-time calculations within the same device boot.
- Saved results reopen correctly and invalid reboot recovery is marked interrupted.
- CSV includes every split, distinguishes stop records, and represents athletes with zero taps.
- Names with commas, quotes, non-ASCII characters, and spreadsheet-sensitive prefixes are handled correctly.
- Cancelled or failed exports preserve results.
- New Session requires confirmation before clearing results.
- The downloaded signed APK installs and runs on the owner's phone.
- A later test update preserves saved data.
- The owner receives understandable design, build, installation, and update instructions.

Use focused automated tests for timing arithmetic, session transitions, recovery decisions, and CSV formatting. Test touch usability, screen behavior, export, and installation on the actual phone.

## Deferred features

Individual starts or finishes, pause/resume, landscape mode, more than 16 athletes, automatic sensors, Bluetooth accessories, editing splits, historical-session browsing, cloud synchronization, automatic updates, iPhone support, and app-store publication.
