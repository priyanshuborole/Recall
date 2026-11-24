# Recall — project guide for Claude Code

Recall is a private memory-logging app: capture a moment in under 15 seconds (voice, text, photo),
recall it years later (Timeline, On this day). Built for the owner first, Play Store later.

## Decisions already made (don't revisit without asking)

- **Platforms:** Android and iOS only. Desktop and web were removed on 2026-11-20.
- **Build tool:** Kotlin Toolchain (`./kotlin`, YAML config), not Gradle. Version pinned in `./kotlin`.
- **Package / bundle id:** `com.zerotwodev.recall`.
- **Local-first:** memories never touch our servers. Room on device is the source of truth.
- **No DI framework:** a plain `AppContainer` built per platform by `createAppContainer(...)`.
- **Navigation:** small shared back stack in `shared/src/ui/App.kt` (`Navigator`, `Screen`).
- **Themes:** Warm paper and Fresh sage, each light/dark, chosen on the Appearance screen.
  Palettes in `shared/src/ui/theme/Color.kt`. Mood colours never change with the theme.
- **AI:** Firebase AI Logic + Gemini Flash-Lite, paid tier, nothing stored. Not built yet.
  The toolchain reads `androidApp/google-services.json` itself; iOS adds Firebase as a Swift package.
- **Voice capture:** hold to speak, release to save, slide left to cancel (design board 4). Max 60 s.
- **Voice is text-only and on-device (decided 2026-11-23):** Android's on-device `SpeechRecognizer` turns
  speech into text while the mic is held. No audio file, no AI, no cloud, no playback. Needs Android 12+.
- **Backup:** Android Auto Backup covers the database and settings only. Never add `media/`: the 25 MB app
  limit could stop the database backing up too. Photos need their own backup plan later.

## Layout

```
shared/        kmp/lib (android, iosArm64, iosSimulatorArm64)
  src/           common: domain/, data/ (+ data/local = Room), presentation/ (ViewModels), ui/, util/
  src@android/   createAppContainer(context): DB file + SharedPreferences
  src@ios/       createAppContainer(): DB in Documents + NSUserDefaults
  schemas/       Room schemas per platform — commit them
  test/          common tests
androidApp/    android/app: RecallApp, MainActivity, res/, backup rules
iosApp/        ios/app: ViewController.kt, iosApp.swift, module.xcodeproj
```

## Rules

- Only `src@android`, `src@ios` and the app modules may call platform APIs.
- New platform feature = interface in `shared/src`, one implementation per platform,
  created in each `createAppContainer`, exposed on `AppContainer`. Screens see only the interface.
- Data rules (keep for future sync): UUID string ids, `createdAt`/`updatedAt` on every row,
  soft deletes via `deletedAt`, media referenced by file name in the app's media folder.
- Room 3 (`androidx.room3`), bundled SQLite driver. DAO functions are `suspend` or return `Flow`.
  Changing an entity = bump the DB version and add a migration.
- Strings shown to users: plain, warm, no streaks or guilt. Never claim "on device" for cloud work.

## Commands

```bash
./kotlin run -m androidApp     # emulator or connected phone
./kotlin run -m iosApp         # needs Xcode (run `xcodebuild -runFirstLaunch` once)
./kotlin test -m shared
./kotlin clean                 # if a build seems to use stale code
./kotlin --help                # more commands
```

## Plan and status

The PRD, the Build plan (32 tasks with status) and step-by-step instructions for tasks 1–10 live in the
"Memory App — PRD" doc on claude.ai: https://claude.ai/artifact/1ST4ApSxhCWnkFNRhbtJGa
UI designs (Warm and Fresh boards, light and dark): https://claude.ai/artifact/T8ktTvy8pdpMSxW8J8c8AM
Those pages need the owner's claude.ai login; ask the owner to paste a section if you need it.

**Built so far:** app shell; Room data layer; Home (greeting, today's question, On this day, recent,
capture bar); text capture with mood; Timeline with month groups and search; Memory detail (hide, delete);
Settings (reminder time and app lock are saved but do nothing yet); Appearance screen with both themes.

**Next tasks, in order** (details in the doc's "Steps: Foundation & Capture" tab):
1. Build and run both apps; fix first-build errors
2. Commit Room schemas; push to https://github.com/priyanshuborole/Recall
3. Bundle Fraunces and DM Sans in `shared/composeResources/font/`; typography becomes a composable
4. Android window theme and iOS status bar follow the chosen theme and mode
5. `AudioRecorder` + `MicPermission` per platform (MediaRecorder / AVAudioRecorder)
6. Hold-to-record overlay on Home with timer, 34-bar waveform, slide-left cancel
7. `MediaDao`; save voice memory + AUDIO media row in one transaction
8. Photo picker (FileKit, else platform pickers); copy as `<uuid>.jpg`, max 2048 px, 85% JPEG
9. Capture review screen (board 5): editable title, transcript, Mood/People/Place chips, photos, Done
10. People and places DAOs; reuse a person by name (case-insensitive); one place per memory

Known fix already applied: `rememberModalBottomSheetState` is deprecated; use
`rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(Hidden, Expanded))`.
