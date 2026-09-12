# Miya (Android)

Android client for Miya — a personal media library app for browsing and playing back remote
audio and photo assets against a self-hosted "MiyaServer" (Python/Strawberry GraphQL). This is a
feature-for-feature port of the iOS app at `../Miya`, which is built strictly on The Composable
Architecture (TCA). Despite the "Home" name, **this is not a smart-home app** — there is no
HomeKit/Matter/MQTT/BLE equivalent anywhere in scope. "Home" refers to the Home screen.

The full port plan, staged, lives in the session that created this scaffold; treat this file as
the durable reference for how the Android side of the port is built, not the plan itself.

## Commands

```bash
# Build
./gradlew :app:assembleDebug

# Install + launch on a running emulator/device
./gradlew :app:installDebug
adb shell am start -n com.hurtado.miya/.MainActivity

# Unit tests (Stage-by-stage; see Testing below — there's no existing suite to mirror)
./gradlew :app:testDebugUnitTest

# Point debug builds at a different MiyaServer without editing build files
./gradlew :app:assembleDebug -PMIYA_SERVER_URL=https://your-server:8000
```

`JAVA_HOME` must point at a JDK the installed Gradle wrapper version supports (17–21; **not** a
JBR/JDK newer than what that Gradle release documents — Gradle 8.9 does not run on JDK 25). If
`~/Library/Android/sdk` isn't picked up automatically, set it in `local.properties`
(`sdk.dir=...`) — that file is gitignored, so it's set up per machine, not committed.

## Architecture

Jetpack Compose, single-Activity, no Fragments. **Strictly no `ViewModel`-as-god-object** —
`ViewModel` subclasses in this codebase exist *only* to hold a `Store` + its `CoroutineScope`
(see `HomeViewModel`); all state transitions and business logic live in `Reducer`
implementations, mirroring the iOS rule that bans `ObservableObject`/`@Published`/`@StateObject`
in favor of TCA reducers. If you find yourself adding a mutable `var` or business-logic method
directly on a `ViewModel`, it belongs in a `Reducer` instead.

### The "TCA-lite" runtime (`architecture/Store.kt`)

A small hand-rolled MVI runtime mirrors TCA's vocabulary 1:1 rather than pulling in a 3rd-party
MVI library, so every reducer here has an exact counterpart in the iOS Swift source to diff
against:

| TCA (iOS) | Here |
|---|---|
| `@ObservableState struct State` | plain immutable `data class State` |
| `enum Action` with nested `View`/`Delegate` cases | `sealed interface Action` with nested `sealed interface View`/`Delegate` |
| `@Reducer` / `Reduce { state, action in ... }` | `class FooReducer : Reducer<FooState, FooAction>`, `reduce(state, action): Pair<State, Effect<Action>>` |
| `Effect<Action>` / `.run { send in }` / `.cancellable(id:)` | `Effect.Run(id, cancelInFlight, block)`, `Effect.Cancel`, `Effect.Merge` |
| `Store<State, Action>` | `Store<State, Action>` (backed by `StateFlow`, effects run on a `CoroutineScope`) |
| `@Dependency` / `@DependencyClient` | Hilt-injected interfaces with `Fixture`/`Live` implementations bound in `di/` modules |
| `StackState` + `NavigationStack` | Navigation-Compose `NavHost`, typed routes |
| `@Presents` / `ifLet` | nullable state + `ModalBottomSheet`/overlay, driven the same way |

New features follow the existing shape: `FooModels.kt` (if it owns domain types),
`FooFeature.kt` (`FooState`, `FooAction`, `FooReducer`), `FooViewModel.kt` (Hilt `ViewModel`
wrapping a `Store`), `FooScreen.kt` (the composable, `hiltViewModel()`-injected). Reusable,
store-free composables (no `Store`, no Hilt) go in `views/`, not next to a feature.

### Directory layout

```
app/src/main/java/com/hurtado/miya/
├── architecture/   the Store/Reducer/Effect runtime — framework code, not feature code
├── features/<name>/  one package per iOS Feature: *Models, *Feature (state/action/reducer),
│                      *ViewModel, *Screen
├── views/           store-free reusable composables (CoverTile, PreviewCard, SectionCardGrid, ...)
├── services/        HomeClient/AuthClient/session/audio-engine interfaces + implementations
│   ├── fixture/     offline-mode implementations reading bundled JSON (assets/*.json)
│   └── graphql/     Apollo-generated code + Live* implementations
├── di/              Hilt modules — this is where Fixture vs Live client selection happens
└── ui/theme/        Color.kt, Type.kt (font-role overrides), Theme.kt
```

## Typography is the brand — do not use default Material type

The iOS app overrides every standard text role with Snell Roundhand (script, titles, 48/36) and
Times New Roman (serif, body, 24/16/12) — see `Miya/Extensions/Font.swift` on iOS. Neither font
ships on Android, so `ui/theme/Type.kt` substitutes the closest free, redistributable
equivalents, bundled as static `.ttf`s in `res/font/` (not fetched at runtime, since the app has
an offline fixture mode that must render identically without network):

- **Tinos** (OFL) — metrically compatible with Times New Roman — serif/body role
- **Yellowtail** (Apache 2.0) — free script face — title role

Always reach for `MiyaTypography`'s roles (`MaterialTheme.typography.*`, mapped in `Type.kt`)
rather than hardcoding a `FontFamily`/`fontSize` inline, the same discipline the iOS code applies
by routing every text style through the `Font.swift` overrides. See
`app/src/main/assets/licenses/NOTICE.md` for full attribution if you touch the font files.

## Offline fixture mode vs. live GraphQL

Mirrors iOS's `RunMode.isFixtureMode`: with no `MIYA_SERVER_URL` configured, `HomeClient` is
bound to `services/fixture/FixtureHomeClient`, which reads `home_sections.json`/`albums.json`
from `app/src/main/assets/` (copied verbatim from the iOS `Resources/` fixtures) and implements
search/author queries locally with diacritic- and case-insensitive folding. Debug builds default
`BuildConfig.MIYA_SERVER_URL` to a real server address (override with
`-PMIYA_SERVER_URL=...` or the `MIYA_SERVER_URL` env var); release builds always ship with it
empty — **fixture mode must stay debug-only**, exactly like the iOS `#if DEBUG` gate. The
Fixture/Live split happens in `di/ClientsModule.kt`; don't branch on build type inside a client
implementation itself.

## Two-client separation (once Stage 6/7 auth + live GraphQL land)

The data-plane client (sections/albums/search, bearer-authenticated, single 401-retry) and the
auth-plane client (`signInWithGoogle`/`refreshSession`/`signOut`/`viewer`, token-free) must stay
two separate Apollo `ApolloClient` instances with separate interceptor chains — never merge them
into one client with conditional auth headers. This mirrors the iOS
`MiyaGraphQLClient`/`MiyaAuthAPI` split and exists so a transport failure during refresh can never
be misread as an auth rejection.

## Session & auth rules

- No secrets (tokens, refresh tokens) in `State`/`Action` types or logs — they get printed by
  debug tooling and crash reports. Redact before logging, same as iOS's `CustomDump` redaction.
- Session refresh must be single-flight (guard with a `Mutex` + shared `Deferred`, not a plain
  `suspend fun`) — refresh tokens are single-use/rotated server-side, and a concurrent double
  redemption reads as a replay and revokes the whole session.
- A transport/network failure during refresh or restore must **not** sign the user out; only an
  explicit server rejection should. Keep the cached profile and let the user in when offline.

## Audio playback rule

The Media3 player must be started by a **reducer effect**, not by a Composable's `LaunchedEffect`
scoped to a screen. The mini-bar/full-player UI swap on collapse/expand means a
composable-scoped effect would kill playback every time the player minimizes — this is the exact
bug the iOS CLAUDE.md calls out for `AudioPlayerEngine`/`SongPreviewFeature`, and the reason
`MiyaPlaybackService` + the eventual `SongPreviewFeature` own the player lifecycle, not a screen.

## Jetpack Compose / Kotlin conventions

- **State hoisting**: composables in `views/` take plain data + lambdas, never a `Store` or
  `ViewModel` — only `*Screen.kt` files call `hiltViewModel()`.
- Prefer `StateFlow`/`Flow` and structured concurrency (`viewModelScope`, `Dispatchers.IO` for
  blocking I/O) over callbacks; no raw `Thread`/`AsyncTask`.
- `remember`/`derivedStateOf` only for view-local, disposable state (scroll positions, transient
  UI toggles) — anything that survives navigation or matters to business logic lives in a
  `Store`'s `State`.
- Material 3 components; dark mode and dynamic type (Compose font scaling) must both work without
  layout breakage, matching the iOS HIG-compliance rule (Dynamic Type + VoiceOver + dark mode).
- Accessibility: every icon-only control needs a `contentDescription`; don't ship a TalkBack
  regression relative to the iOS VoiceOver labels for the same control.
- Hilt for all DI — `@HiltViewModel`, constructor injection into `Reducer`s and clients, modules
  under `di/`. Don't hand-wire singletons.
- Immutable `data class` state; reducers return new state via `.copy(...)`, never mutate in
  place.

## Testing

The iOS app has no test target (see its CLAUDE.md) so there is no existing suite to port — the
inline comments in the Swift reducers are the closest thing to a behavioral spec when writing
Android tests. Preferred approach for new coverage:

- **Reducer tests**: plain JUnit — call `reducer.reduce(state, action)` and assert on the
  returned `(State, Effect)` pair; this needs no Android framework, no Compose, no coroutines
  test infra beyond `kotlinx-coroutines-test` for effect bodies.
- **Store/Flow tests**: Turbine for asserting `StateFlow` emissions across a sequence of `send`s.
- **Compose UI tests**: `androidx.compose.ui.test` + `createComposeRule` for screen-level
  smoke tests, sparingly — prefer reducer tests for logic coverage.

## Known constraints from the local toolchain

- Only Android SDK Platform 35 and 37 are installed locally (`~/Library/Android/sdk/platforms`);
  AGP 8.7.2 is only tested through `compileSdk 35`, so `compileSdk`/`targetSdk` are pinned to 35
  in `app/build.gradle.kts` until AGP is bumped alongside SDK 37 support. Don't bump one without
  the other.
- Gradle 8.9 (via the wrapper) requires a JDK the release supports; the system JBR bundled with
  the installed Android Studio may be newer than Gradle supports — if `./gradlew` fails with an
  opaque version-number exception, check `java -version` against Gradle's compatibility matrix
  before debugging anything else.
