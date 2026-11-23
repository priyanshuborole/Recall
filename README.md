# Recall

A private memory-logging app for **Android and iOS**, built with the
[Kotlin Toolchain](https://kotlin-toolchain.org/latest/) and Compose Multiplatform.
All screens, logic and data rules live in `shared`; each app module is a thin entry point.

## Modules

| Module | Product | What's in it |
|---|---|---|
| `shared` | `kmp/lib` (Android, iOS) | Domain, Room database, ViewModels, Compose UI and themes |
| `androidApp` | `android/app` | `RecallApp` (creates the data container), `MainActivity`, backup rules |
| `iosApp` | `ios/app` | `ViewController.kt` + `iosApp.swift`, Xcode project |

## Source sets in `shared`

```
shared/
├─ src/            common: domain/, data/ (Room in data/local, settings), presentation/ (ViewModels), ui/, util/
├─ src@android/    createAppContainer(context): DB file + SharedPreferences
├─ src@ios/        createAppContainer(): DB in Documents + NSUserDefaults
├─ schemas/        Room schemas, one folder per platform — commit them
└─ test/           common tests
```

Rule: only `src@<platform>` folders and the app modules may use platform APIs.

## Running

```bash
./kotlin run -m androidApp   # emulator or connected device
./kotlin run -m iosApp       # needs Xcode
./kotlin test -m shared
```

## Data rules (sync-ready)

- UUID string ids, `createdAt` / `updatedAt` on every record, soft deletes via `deletedAt`
- Media referenced by file name, never absolute path
- Android Auto Backup covers the database and settings (`androidApp/res/xml`)

## Notes

- Fonts: the design uses Fraunces and DM Sans. Put the `.ttf` files in `shared/composeResources/font/`
  and use them in `ui/theme/Theme.kt`.
- iOS signing: open `iosApp/module.xcodeproj` › Signing & Capabilities and pick your team.
