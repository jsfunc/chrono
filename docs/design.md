# How Chrono works

## One state, three views

The app has three views: setup, timing, and results. `ChronoApp` selects the visible view and connects its buttons to `ChronoViewModel`. A navigation library is unnecessary for these three local destinations.

A ViewModel is an Android object that retains application state when Android recreates the visible activity. It survives that recreation but not process death; local saving handles reopening later.

Kotlin is the language used throughout. Jetpack Compose describes the screen as functions of the current state. When a tap creates a new state snapshot, Compose updates the affected interface. Android documents this toolkit in the [Compose overview](https://developer.android.com/compose).

## Athletes and split records

An athlete has a number and a name. Numbers distinguish two athletes with the same name.

The session stores one shared monotonic start timestamp. Each tap stores the athlete number and its elapsed milliseconds since that start. A split number and interval are derived from that athlete's ordered taps. This avoids storing several potentially inconsistent versions of the same time.

For Alice's taps at 12.3 and 25.1 seconds, her totals are 12.3 and 25.1; her intervals are 12.3 and 12.8. Bob's taps do not enter Alice's calculation.

The ViewModel captures the timestamp first. Formatting and file writes happen afterward. `SystemClock.elapsedRealtime()` measures time since boot, including sleep, and is unaffected by the phone's calendar clock. See the [SystemClock reference](https://developer.android.com/reference/android/os/SystemClock).

The visible shared timer refreshes about every 100 milliseconds while the app is visible. Its refreshes do not accumulate the stopwatch time; it always subtracts the recorded start from the current clock. An update delayed by Android cannot make the stopwatch drift.

Internal values retain milliseconds. Screen and CSV values round to the nearest tenth after subtraction. Rounding can cause a tenth-second difference if someone manually subtracts already-rounded totals.

## Session states

Before Start there is no session. Start creates one with status Running. Stop records the shared elapsed duration and changes the status to Stopped. Stop does not create athlete splits.

A session interrupted by a reboot or an unverifiable clock has status Interrupted. It retains recorded taps but has no invented final duration.

Zone taps only create records while Running. Start cannot replace an existing session. New Session cannot clear a running session, and clearing completed results requires a confirmation.

## Layout and touch behavior

Use one column for up to four athletes, and two columns above that. Allocate equal height to each grid row so every zone remains visible. Eight athletes occupy four rows; sixteen occupy eight.

The header contains the shared current time. Zones hold their latest recorded total and interval, which remain readable until their next tap. Roomy zones also show up to three earlier splits beneath them, newest first, in a small table with split number, total, and interval. All displayed times come from the same saved session records. The bottom shared button has a fixed accessible area.

A zone uses Android's standard clickable gesture: one completed tap gives one split and a brief highlight. There is no artificial cooldown that rejects rapid taps.

For recent history, the zone first reserves room for its name and latest values, then calculates how many history rows fit into the remaining height, including the phone's text scaling. It measures the actual number widths before displaying the table, so long session times and narrow zones do not force clipped columns. The cap of three earlier rows keeps the timing screen quick to read; the full list stays available in Results. History rows share the entire zone's click handler.

Dense grids use compact typography. Before Start, a layout that cannot accommodate the current text size asks the user to reduce the athlete count or text size. The grid never silently becomes a scrolling list during timing. Full names remain available in results and CSV when the zone label is shortened.

The app uses a light background, dark text, teal for Start, and red for Stop. Athlete identity comes from names and numbers, so color is not required to distinguish athletes.

## Saving and recovery

Store the roster and latest session in `session.json` in the app's private internal storage. JSON makes this small data structure inspectable without introducing a database. It is not directly browseable through ordinary phone file managers; use CSV export for your results.

Queue state snapshots in order and write them on a background thread. This keeps storage work out of touch handling and prevents an earlier save from replacing a later one. `AtomicFile` preserves the previous complete file if a replacement is interrupted. See the [AtomicFile reference](https://developer.android.com/reference/android/util/AtomicFile).

A queued event can still be lost if Android terminates the process before its write finishes. Storage failure displays a persistent message; results remain in memory and can be exported or saved again.

Android's boot count is saved with the start timestamp. A running session resumes only when the boot count still matches and its elapsed clock is plausible. Otherwise, preserve it as Interrupted. An unreadable saved file is kept intact and reported rather than silently overwritten.

Automatic Android backup is disabled because elapsed timestamps from one device boot should not be restored onto another device.

## Awake screen and offline operation

Set `View.keepScreenOn` while timing is running. Android uses this to keep the visible window awake. It allows normal sleep after Stop or when the app is covered. Timing still advances logically while the phone is locked, because elapsed time is calculated from the monotonic clock.

No background service is needed just to measure that elapsed duration. No Internet or broad storage permission is requested.

## CSV

Android's Create Document picker lets the user choose the export destination. A cancelled picker changes nothing. The writer uses UTF-8, comma delimiters, explicit decimal points, and properly quoted text fields.

Export one row per split and a separate stop row per athlete. The stop row represents the common session duration, including athletes with no taps, and has no interval or split number.

Names beginning with a spreadsheet-formula prefix receive a protective apostrophe in the CSV. Otherwise the full name, including Unicode characters, is retained.

## Build tools and versions

The build uses Gradle 8.13, Android Gradle Plugin 8.12.1, Kotlin/Compose compiler 2.0.21, and Compose BOM 2024.09.00. These versions were selected from the locally cached toolchain and verified by building and testing together. They are pinned for repeatable builds, not advertised as the latest releases.

A BOM selects matching Compose library versions. The Kotlin and Compose compiler plugins use the same version. See the [Compose BOM guidance](https://developer.android.com/develop/ui/compose/bom) and [AGP 8.12 compatibility table](https://developer.android.com/build/releases/agp-8-12-0-release-notes).

The app compiles and targets Android 16 (API 36), matching the owner's phone. The minimum is Android 8 (API 26), which also provides Java's date/time APIs used for session dates. A full JDK 17 is sufficient for compilation. The Gradle wrapper downloads the specified build tool, so installing a separate system Gradle is unnecessary.

The test application identifier ends in `.debug`. It installs separately from the release app and makes early testing easy to identify. Before distributing a release, create and privately retain a release signing key; signing proves that an update belongs to the same app.
