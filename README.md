# Question Bank

An offline-first question bank for **Android** and **iOS**, built with **Kotlin Multiplatform**.
There's no account, no login and no network. On first launch the app quietly creates a local profile,
imports the bundled question seed, and opens straight onto the home screen.

**Author:** MH TOUFIK ([@toufikforyou](https://github.com/toufikforyou))

| | Android | iOS |
|---|---|---|
| UI | Jetpack Compose, Material 3, dynamic color, light/dark | SwiftUI (iOS 16+), `NavigationStack` |
| State | `StateFlow` → `collectAsStateWithLifecycle` | `StateFlow` → `watchState` → `@Published` |
| Haptics | `View.performHapticFeedback` (CONFIRM / REJECT) | `UINotificationFeedbackGenerator` |
| Storage | SQLDelight (Android driver) | SQLDelight (native driver) |

## Architecture

```
┌──────────────────────── Platform UI ────────────────────────┐
│  Compose screens                     SwiftUI views           │
│  StoreHolder (ViewModel)             StoreViewModel (ObservableObject)
└───────────────▲──────────────────────────────▲──────────────┘
                │ StateFlow<State> / effects    │ watchState / watchEffects
┌───────────────┴──── shared: presentation ─────┴──────────────┐
│  BaseStore<State, Intent, Effect>  (MVI)                      │
│  HomeStore · QuizStore · BookmarksStore · StatsStore          │
├──────────────────── shared: domain ──────────────────────────┤
│  Entities: Question, QuizSession, QuizScore, UserStats …      │
│  Use cases: GetQuestions, AnswerQuestion, ToggleBookmark,     │
│             GetStats, InitializeApp, GetCategories            │
│  Repository interfaces                                        │
├──────────────────── shared: data ────────────────────────────┤
│  SqlDelight*Repository  ── in-memory cache (SuspendLazy,      │
│                            StateFlow bookmark mirror)          │
│  SeedParser ◄── seed/questions.json (assets / app bundle)     │
└───────────────────────────────────────────────────────────────┘
```

* **Domain** is pure Kotlin. Use cases are stateless and immutable, so any coroutine can call them
  at the same time. Failures come back as `Result` with typed `DomainError`s, such as `EmptyQuestionBank`
  or `InvalidQuizSize`.
* **Data**: repositories own all I/O on an injected IO dispatcher. The catalog is immutable after seeding,
  so it's loaded once into memory behind a `Mutex`. Bookmarks are mirrored in a `StateFlow` and written
  through to SQLite. After the first load, every screen transition reads from memory.
* **Presentation**: one `BaseStore` per screen. Both UIs get identical state, including per-option visual
  state (`OptionState`) and haptic intents (`HapticKind`), so behaviour can't drift between platforms.
* **Onboarding**: `InitializeAppUseCase` creates the local `UserProfile` and seeds the database. It is
  idempotent and runs on every launch.

## Project layout

```
.
├── .github/workflows/
│   ├── ci.yml                 # tests on every push / PR (JVM, emulator, simulator)
│   └── release.yml            # tag v*.*.* → signed AAB/APK + IPA → GitHub Release
├── seed/questions.json        # single source of truth for bundled questions (both platforms)
├── shared/                    # Kotlin Multiplatform core
│   └── src/
│       ├── commonMain/kotlin/dev/toufikforyou/questionbank/
│       │   ├── domain/{model,repository,usecase}/
│       │   ├── data/{cache,local,repository,seed}/
│       │   ├── presentation/{mvi,home,quiz,bookmarks,stats}/
│       │   └── di/AppContainer.kt
│       ├── commonMain/sqldelight/…/QuestionBank.sq
│       ├── commonTest/        # fakes + use case / scoring / store tests
│       ├── androidMain/       # Android SQLite driver, asset seed reader
│       ├── androidUnitTest/   # SQLDelight integration test on JDBC SQLite
│       └── iosMain/           # native driver, bundle seed reader, Swift entry point
├── androidApp/                # Jetpack Compose app
│   └── src/{main,androidTest}/
└── iosApp/                    # SwiftUI app (XcodeGen spec, no checked-in .xcodeproj)
    ├── project.yml
    ├── iosApp/{App,Bridging,Components,Features,Resources}/
    ├── iosAppTests/           # XCTest: view models vs. real Kotlin stores
    └── iosAppUITests/         # XCUITest: end-to-end flows
```

## Getting started

Requirements: JDK 17+, Android Studio (Ladybug or newer), and for iOS a Mac with Xcode 15+ and
[XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`).

```bash
./gradlew :shared:allTests            # all shared tests (JVM + iOS simulator on macOS)
./gradlew :androidApp:installDebug    # run on a connected Android device/emulator
```

```bash
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

Xcode builds the Kotlin framework automatically through the `Compile Kotlin Shared Framework` build phase.

### Adding questions

Edit `seed/questions.json` and **increase `version`**. On next launch the app re-imports the catalog
while keeping the user's attempts and any bookmarks that still point at existing questions.

## Testing

| Layer | Location | Run |
|---|---|---|
| Use cases, scoring, stores (fakes) | `shared/src/commonTest` | `./gradlew :shared:testDebugUnitTest` |
| SQLDelight + real seed | `shared/src/androidUnitTest` | `./gradlew :shared:testDebugUnitTest` |
| Kotlin/Native | same common tests | `./gradlew :shared:iosSimulatorArm64Test` |
| Compose UI + navigation | `androidApp/src/androidTest` | `./gradlew :androidApp:connectedDebugAndroidTest` |
| Swift view models ↔ Kotlin flows | `iosApp/iosAppTests` | `xcodebuild test -scheme iosApp …` |
| iOS end-to-end | `iosApp/iosAppUITests` | same |

## Releasing

Push a semantic version tag and the `Release` workflow does the rest:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

It tests both platforms, builds a signed `.aab` + `.apk` and a signed `.ipa`/`.xcarchive`, and publishes
them with SHA-256 checksums as a GitHub Release that credits the author.

### Release secrets

Add these under **Settings → Secrets and variables → Actions**.

**Android (required)**

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 release.jks` (macOS: `base64 -i release.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Create a keystore once and keep it backed up. If you lose it, you can't update the app on Google Play.

```bash
keytool -genkeypair -v -keystore release.jks -alias questionbank -keyalg RSA -keysize 4096 -validity 10000
```

**iOS (optional)**: without these, the release ships Android only and logs a warning.

| Secret / variable | Value |
|---|---|
| `IOS_DIST_CERTIFICATE_P12_BASE64` | Apple Distribution certificate exported as `.p12`, base64-encoded |
| `IOS_DIST_CERTIFICATE_PASSWORD` | password of that `.p12` |
| `IOS_PROVISIONING_PROFILE_BASE64` | distribution provisioning profile, base64-encoded |
| `IOS_TEAM_ID` | 10-character Apple Developer Team ID |
| variable `IOS_BUNDLE_ID` | defaults to `dev.toufikforyou.questionbank` |
| variable `IOS_EXPORT_METHOD` | `app-store-connect` (default), `release-testing` or `debugging` |
| variable `APP_AUTHOR_NAME` | name credited in release notes (default `MH TOUFIK`) |

## License

[MIT](LICENSE) © 2026 MH TOUFIK
