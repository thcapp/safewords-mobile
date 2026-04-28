# Android Architecture

Comprehensive architectural documentation for the Safewords Android app at
`repos/safewords-android/`.

---

## 1. Overview

Safewords for Android is a native Kotlin + Jetpack Compose application that
generates rotating TOTP-based family "safewords" entirely on-device. It is the
Android counterpart to the iOS Swift/SwiftUI app and shares its crypto contract
(the same seed + time produces the same phrase on both platforms, verified
against `shared/test-vectors.json`).

**Key characteristics**

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| UI framework | Jetpack Compose (BOM 2024.12.01) + Material 3 |
| Min SDK | 26 (Android 8.0 "Oreo") |
| Target / Compile SDK | 34 (Android 14) |
| JVM target | 11 |
| Package / namespace | `com.thc.safewords` |
| Application ID | `com.thc.safewords` |
| Version | 1.0.0 (versionCode 1) |
| Crypto | `javax.crypto.Mac` HmacSHA256 (stdlib, zero 3rd-party) |
| Secure storage | AndroidX `EncryptedSharedPreferences` (`security-crypto` 1.1.0-alpha06) |
| Widget | Jetpack Glance 1.1.1 + WorkManager 2.10.0 |
| QR | ZXing 3.5.3 (generation) + ML Kit barcode-scanning 17.3.0 + CameraX 1.4.1 (scanning) |
| JSON | Gson 2.11.0 (word lists + group persistence + QR payloads) |
| Navigation | `androidx.navigation:navigation-compose` 2.8.5 |
| AGP | 8.5.2 |

The app has **zero network dependency** for its core function. There is no
backend, no account system, and no telemetry. Groups are formed locally; seeds
are shared peer-to-peer via QR codes.

---

## 2. Module Layout

A two-module Gradle build (`settings.gradle.kts` → `include(":app")`,
`include(":widget")`). Version catalog lives in `gradle/libs.versions.toml`.

```
repos/safewords-android/
├── settings.gradle.kts          Module include list ("Safewords" project)
├── build.gradle.kts             Root build — applies no plugins, declares aliases
├── gradle.properties            JVM args, AndroidX opt-in, non-transitive R
├── gradle/libs.versions.toml    Single source of truth for all versions
├── app/
│   ├── build.gradle.kts         applicationId com.thc.safewords, minSdk 26
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     Camera permission, MainActivity, widget receiver
│       │   ├── assets/wordlists/       Bundled frozen word lists (JSON)
│       │   │   ├── adjectives.json
│       │   │   └── nouns.json
│       │   ├── kotlin/com/thc/safewords/  (see §3)
│       │   └── res/                    launcher icons, colors, themes, strings
│       └── test/kotlin/com/thc/safewords/TOTPDerivationTest.kt
└── widget/
    ├── build.gradle.kts         com.android.library, depends on :app
    └── src/main/
        ├── AndroidManifest.xml  (empty; components registered in app manifest)
        ├── kotlin/com/thc/safewords/widget/
        │   ├── SafewordsWidget.kt           GlanceAppWidget
        │   └── SafewordsWidgetReceiver.kt   Receiver + WorkManager worker
        └── res/
            ├── layout/widget_loading.xml
            ├── xml/safewords_widget_info.xml  250x80dp, 30-min update
            └── values/strings.xml
```

The widget module has `implementation(project(":app"))` so it can call into
`GroupRepository`, `TOTPDerivation`, and `SecureStorageService` directly —
there is no IPC. The app manifest declares the widget's receiver because
widget providers must be visible to the launcher, which scans the application's
manifest.

---

## 3. Source Tree — `com.thc.safewords/`

```
com/thc/safewords/
├── SafewordsApp.kt          Application subclass; exposes `instance` singleton
├── MainActivity.kt          ComponentActivity → SafewordsTheme → SafewordsNavigation
├── crypto/
│   └── TOTPDerivation.kt    HMAC-SHA256 core (shared spec in docs/totp-word-algorithm.md)
├── data/
│   └── WordLists.kt         Lazy loader that reads assets/wordlists/*.json via Gson
├── model/
│   ├── Group.kt             data class + RotationInterval enum
│   └── Member.kt            data class + Role enum
├── service/
│   ├── GroupRepository.kt   Singleton + StateFlow<List<Group>> (see §10)
│   ├── SecureStorageService.kt  EncryptedSharedPreferences wrapper for seeds
│   └── QRCodeService.kt     ZXing generation + parse (URL-safe base64 seeds)
└── ui/
    ├── components/
    │   ├── CountdownRing.kt       60-tick dial Canvas primitive
    │   ├── DesignComponents.kt    SectionLabel, GroupDot, ElevatedCard, text helpers
    │   └── SafewordDisplay.kt     Legacy (capitalized) phrase display
    ├── home/HomeScreen.kt         Live word + CountdownRing hero
    ├── onboarding/OnboardingScreen.kt  3-panel intro (Welcome / Start / Seed)
    ├── groups/
    │   ├── GroupsScreen.kt        List + per-group card + member roster
    │   └── GroupDetailScreen.kt   Material3 edit / delete screen (legacy palette)
    ├── verify/VerifyScreen.kt     Ready / Listening / Match / Mismatch phases
    ├── qr/
    │   ├── QRDisplayScreen.kt     Invite card with ZXing-rendered bitmap
    │   └── QRScannerScreen.kt     CameraX + ML Kit scanner + join dialog
    ├── settings/SettingsScreen.kt Section-grouped preferences + plain-mode toggle
    ├── plain/PlainMode.kt         Accessibility-mode root + all its screens
    ├── navigation/SafewordsNavigation.kt  NavHost, Screen sealed class, CustomTabBar
    └── theme/
        ├── Color.kt      Ink & A11y palettes + DotPalette + legacy aliases
        ├── Type.kt       Material3 Typography scale
        └── Theme.kt      MaterialTheme wiring
```

### File-by-file reference

| File | Responsibility |
|---|---|
| `SafewordsApp.kt` | `Application` subclass; assigns `instance` so singletons can reach the `Context`. |
| `MainActivity.kt` | Single-activity host; calls `enableEdgeToEdge()` and mounts `SafewordsTheme { SafewordsNavigation() }`. |
| `crypto/TOTPDerivation.kt` | `object` with `deriveSafeword(seed, interval, timestamp)`, `getCurrentCounter`, `getTimeRemaining`, `formatTimeRemaining`, `hexToBytes`, `bytesToHex`. Constants: `ADJECTIVE_COUNT=197`, `NOUN_COUNT=300`, `NUMBER_MODULUS=100`. |
| `data/WordLists.kt` | Lazy `val adjectives`, `val nouns` loaded from `assets/wordlists/*.json` via Gson `TypeToken<List<String>>`. |
| `model/Group.kt` | `data class Group(id=UUID, name, interval, members, createdAt)` + `RotationInterval` enum: HOURLY 3600s, DAILY 86400s, WEEKLY 604800s, MONTHLY 2592000s. |
| `model/Member.kt` | `data class Member(id=UUID, name, role, joinedAt)` + `Role { CREATOR, MEMBER }`. |
| `service/GroupRepository.kt` | Source of truth for groups. Stores metadata as JSON in an `EncryptedSharedPreferences` file and exposes a `StateFlow<List<Group>>`. Seed bytes live in `SecureStorageService`. |
| `service/SecureStorageService.kt` | Minimal CRUD (`saveSeed`/`getSeed`/`deleteSeed`/`hasSeed`) keyed by `seed_<groupId>` in a separate encrypted prefs file. |
| `service/QRCodeService.kt` | Builds a `QRPayload(v=1, name, seed (url-safe base64), interval)` JSON, encodes via `QRCodeWriter`; parses the inverse. Rejects non-v1 payloads and seeds != 32 bytes. |
| `ui/components/CountdownRing.kt` | 60-tick dashed-dial Canvas primitive (see §6). |
| `ui/components/DesignComponents.kt` | `SectionLabel`, `GroupDot`, `ElevatedCard`, plus `displayStyle`/`monoStyle` helpers. |
| `ui/components/SafewordDisplay.kt` | Legacy large phrase renderer (capitalizes "breezy rocket 75" → "Breezy Rocket 75"). Used by `GroupDetailScreen`. |
| `ui/home/HomeScreen.kt` | Selected-group hero, 1-Hz recomposition via `delay(1000L)` loop, empty-state CTA. |
| `ui/onboarding/OnboardingScreen.kt` | 3 panels, step indicator, back/forward CTAs, seed-word grid + warning callout. |
| `ui/groups/GroupsScreen.kt` | Scrollable group cards + per-group members list. Uses `DotPalette` hashed from group/member id. |
| `ui/groups/GroupDetailScreen.kt` | Material3 classic edit screen; still uses the legacy palette aliases. |
| `ui/verify/VerifyScreen.kt` | 4-phase state machine (see §5). |
| `ui/qr/QRDisplayScreen.kt` | Shows a 240-dp QR bitmap plus SMS-alternative row. |
| `ui/qr/QRScannerScreen.kt` | CameraX `PreviewView` + ML Kit `BarcodeScanning`, AlertDialog for name entry before join. Runs on a dedicated `Executors.newSingleThreadExecutor()`. |
| `ui/settings/SettingsScreen.kt` | Grouped sections (Rotation, Accessibility, Widget, Security, Practice, Danger zone). Contains the `plainMode` toggle. |
| `ui/plain/PlainMode.kt` | All 5 plain-mode composables in one file (see §7). |
| `ui/navigation/SafewordsNavigation.kt` | NavHost, `Screen` sealed class, `CustomTabBar`. Owns `plainMode` Saveable. |
| `ui/theme/Color.kt` | Ink + A11y palettes, `DotPalette`, back-compat legacy aliases. |
| `ui/theme/Type.kt` | Full Material3 `Typography` scale. |
| `ui/theme/Theme.kt` | `SafewordsTheme` composable wires the legacy aliases into `darkColorScheme`. |
| `test/.../TOTPDerivationTest.kt` | JUnit 4 vector tests with inlined word lists (no Android context). |
| `widget/SafewordsWidget.kt` | `GlanceAppWidget.provideGlance` reads the first group's seed and renders the widget content. |
| `widget/SafewordsWidgetReceiver.kt` | `GlanceAppWidgetReceiver` + `WidgetUpdateWorker`; schedules a 30-minute `PeriodicWorkRequest`. |

---

## 4. Design System — `ui/theme/`

### `Color.kt`

Two complete palettes plus an avatar-color palette, plus back-compat aliases.

**`object Ink`** — editorial near-mono dark theme with a single "ember" accent.
Ported from the Safewords handoff bundle (`Safewords App.html`).

| Token | ARGB | Notes |
|---|---|---|
| `bg` | `0xFF0B0B0C` | Near-black canvas |
| `bgElev` | `0xFF151517` | Raised card surface |
| `bgInset` | `0xFF1C1C1F` | Sunken inset surface |
| `fg` | `0xFFF5F2EC` | Primary type ("parchment") |
| `fgMuted` | `fg` α 0.55 | Secondary type |
| `fgFaint` | `fg` α 0.32 | Tertiary / axes / ticks |
| `rule` | `fg` α 0.08 | Hairline border |
| `accent` | `0xFFE8553A` | Ember (CTA, active state, arc) |
| `accentInk` | `0xFF0B0B0C` | Text on accent (= bg) |
| `ok` | `0xFF9DBF8A` | Match / synced green |
| `warn` | `0xFFE8A13A` | Warning amber |
| `tickFill` | `accent` α 0.18 | Ember fill for chips/tips |

**`object A11y`** — high-visibility palette used only by plain / accessibility
mode. Aims for WCAG AAA contrast pairings.

| Token | ARGB | Notes |
|---|---|---|
| `bg` | `0xFF0B1220` | Deep navy |
| `bgElev` | `0xFF18243C` | Card |
| `bgInset` | `0xFF24354F` | Sunken |
| `fg` | `Color.White` | Primary |
| `fgMuted` | `0xFFCBD5E1` | Secondary |
| `fgFaint` | `0xFF94A3B8` | Tertiary |
| `rule` | White α 0.22 | Strong 2-dp borders |
| `accent` | `0xFFFFD23F` | High-contrast yellow CTA |
| `accentInk` | `0xFF0B1220` | Ink on accent (= bg) |
| `ok` | `0xFF4ADE80` | Safe green |
| `danger` | `0xFFFF6B6B` | Danger red |
| `tickFill` | `accent` α 0.22 | Highlight fill |

**`val DotPalette`** — 5 fixed avatar colors, matching the iOS `DotPalette`:
`E8553A` (ember), `6E94E7` (blue), `9DBF8A` (green), `E89B5E` (peach),
`B47AE8` (lilac). Callers index by `abs(id.hashCode()) % DotPalette.size`.

**Back-compat aliases** — the classic screens (`QRScannerScreen`,
`GroupDetailScreen`, `SafewordDisplay`) were written against an older token
set. `Color.kt` keeps those names alive by aliasing each to an `Ink.*`
value so the code compiles unchanged:

```
Background, Surface, SurfaceVariant, SurfaceBright, Teal, TealDark,
TealMuted, Amber, AmberLight, TextPrimary, TextSecondary, TextMuted,
TextSubtle, Error, Success, AvatarColors
```

All of these now resolve to Ink tokens (`Teal = Ink.accent`,
`Background = Ink.bg`, etc.).

### `Type.kt`

Full Material3 `Typography` object — 13 styles from `displayLarge` (40 sp /
48 sp line height, Bold, −0.5 sp tracking) down to `labelSmall` (10 sp /
14 sp, Medium). No custom font files; the system font stack is used on
Android (Roboto / system default).

### `Theme.kt`

A single `SafewordsTheme` composable wraps `MaterialTheme` with a
`darkColorScheme` built from the legacy aliases (`Teal` → primary, `Amber` →
secondary, etc.). Most screens draw directly with `Ink.*`/`A11y.*` tokens, so
the Material color scheme is primarily a safety net for legacy widgets
(`TopAppBar`, `AlertDialog`, `OutlinedTextField`, `Switch`).

---

## 5. Standard Screens

All standard screens use the **Ink** palette. They apply a 62-dp top padding
(status bar + breathing room) and a ~120–140-dp bottom padding to clear the
floating tab bar.

### `HomeScreen`

Live rotating safeword for the currently selected group. Subscribes to
`GroupRepository.groups`, auto-selects the first group on load, and runs a
1-Hz `LaunchedEffect(selected)` loop that recomputes:

- `phrase` = `TOTPDerivation.deriveSafeword(seed, interval.seconds, now)`
- `remaining` = seconds until the next rotation
- `progress` = `1 - remaining / interval.seconds` (drives the countdown arc)

Layout: top-row `GroupDot` pill (tap → Groups) + notification bell pill,
then a 340-dp `CountdownRing` whose inner content renders the ember `LIVE ·
GROUPNAME` label, the multi-line phrase (each word on its own line at 46 sp,
−1.5 sp tracking), a `SEQ · NNNN` counter, and finally an `HH:MM:SS` monospaced
countdown plus "rotates in …" footnote. Shows an `EmptyState` CTA when there
are no groups.

### `OnboardingScreen`

3-panel flow controlled by `var step by mutableIntStateOf(0)`. A progress
indicator at the top shows three capsule bars (active one is 2× wide and
ember-filled). Footer has a back `ArrowBack` circle (when `step > 0`) and a
weight-1 primary CTA pill that advances or calls `onComplete()`.

| Step | Panel | Contents |
|---|---|---|
| 0 | `PanelWelcome` | "Safewords · 01" eyebrow; styled headline ("One word between **trust** and deception."); explainer copy; a mock safeword feed with the middle row highlighted in `Ink.tickFill` + ember type. |
| 1 | `PanelStart` | "Start · 02" eyebrow; three `OnboardOption` rows: Create (primary / ember fill), Join via QR, Join via recovery phrase. |
| 2 | `PanelSeed` | "Seed · 03" eyebrow; a 12-word mock recovery phrase in a 3-column grid; an ember warning banner with the `Outlined.Warning` icon. |

### `GroupsScreen`

Scrollable list of `GroupCard`s — each card renders a `GroupDot` avatar,
group name, optional "ACTIVE" chip (on the selected group, using `tickFill` +
ember type), members count, interval label, the current derived safeword in
ember, and a rotated `SEQ NNNN` counter. Below the active card: a section
titled `Members · <name>` containing a divided roster of `MemberRow`s (each
showing name, "Last seen just now", and an ember "SYNCED" indicator).
Top-right `Add` button routes to invite flow.

### `VerifyScreen`

State machine driven by `private enum class Phase { Ready, Listening, Match,
Mismatch }`. Holds `typed` (the answer text field) and `currentWord`
(remembered against `group?.id`, never re-derived per tick).

| Phase | UI |
|---|---|
| `Ready` | `ReadyPanel`: card with `BasicTextField` + ember cursor, Check CTA pill (disabled + muted until non-empty), mic-button that transitions to `Listening`. Below, the "If they can't give it" tips card with 3 numbered `TipRow`s. Typing triggers case-insensitive `equals` against `currentWord`. |
| `Listening` | Animated pulsating ember ring (180 dp, infinite `rememberInfiniteTransition` with a 2.4-s linear tween), a 72-dp ember mic disc in the center, and two demo buttons + Cancel link. |
| `Match` | `ResultCard(match=true)`: 120-dp circle with `ok` tint, green title "Verified.", explanatory body, Done button. |
| `Mismatch` | `ResultCard(match=false)`: ember circle with Warning icon, "Don't trust." title, red-coded guidance + a "Hang up. Call them on their known number." info row with phone icon. |

### `QRDisplayScreen`

Receives `groupId` from nav args. On mount, calls
`QRCodeService.generateQRBitmap(group, 512)` and renders the 240-dp result
inside a rounded elevated card. Tip paragraph ("Have them open Safewords, tap
**Join with QR**, …"), a warning subtitle, a `256-BIT · ROTATING · OFFLINE`
lock-badge row, and a bottom "Invite via SMS instead" row.

### `SettingsScreen`

Takes `plainMode: Boolean` + `onPlainModeChange` from navigation. Sections
(via the `SettingsSection` scaffold composable) group controls into rounded
card groups separated by `Divider` hairlines:

1. **Rotation** — `IntervalPicker` (4 pill-shaped buttons for HOURLY /
   DAILY / WEEKLY / MONTHLY; writes to `GroupRepository.setDefaultInterval`);
   static "Notify on rotation" and "Include preview of next word" rows.
2. **Accessibility** — the "High visibility mode" `Switch` is the live
   control that hoists `plainMode` and sends navigation into `PlainRoot`.
3. **Widget & Lock Screen**, **Security**, **Practice**, **Danger zone** —
   placeholder `SettingsRow`/`DangerRow` entries wired to no-op clicks.

Bottom footer shows "Safewords v1.0 · Offline-first / No server. No account.
No data collection.".

---

## 6. Custom Components — `ui/components/`

### `CountdownRing.kt`

The single most distinctive UI primitive. `CountdownRing(progress: Float,
size: Dp = 340.dp, content: @Composable () -> Unit)` draws a dashed dial on
`foundation.Canvas` and renders `content` centered inside it.

Draw algorithm:

1. `animateFloatAsState` smooths `progress` toward its target over a 500-ms
   linear tween (`LinearEasing`), so the dial creeps continuously rather than
   snapping once per second.
2. Compute `r = minDimension/2 - 12dp` and `(cx, cy) = center`.
3. **Sixty tick lines** — for `i in 0 until 60`, `big = i % 5 == 0`. Angle
   `a = (i/60) * 2π - π/2` so tick 0 is at 12 o'clock. Each tick draws from
   `r1 = r - (6 | 3) dp` to `r2 = r + (2 | 0) dp`. Color is `Ink.fgFaint`
   at α 0.9 when elapsed (i.e. `i/60 < animated`), α 0.25 otherwise. Stroke
   widths: 1 dp for big ticks, 0.6 dp for small. `StrokeCap.Round`.
4. **Progress arc** — a 1.5-dp ember `Stroke` arc from `-90°` sweeping
   `animated * 360°`, drawn around the same radius.
5. **Knob** — a 5-dp solid ember disc at the leading edge, plus an 8-dp
   ember-α-0.25 stroked outer halo ring.

This is the direct Compose port of the iOS `CountdownRing` / Safewords App
handoff primitive.

### `DesignComponents.kt`

| Helper | Behavior |
|---|---|
| `SectionLabel(text, color = Ink.fgMuted, modifier)` | Uppercases the text and renders at 11 sp / Medium / 1.4 sp tracking. Used for the "VERIFY", "SETTINGS", "SEED · 03" eyebrows. |
| `GroupDot(initial, color, size = 36.dp, modifier)` | Solid color circle with a capped single-letter initial in White, sized at ~42 % of the dot. The core avatar primitive. |
| `ElevatedCard(cornerRadius=20, borderColor=Ink.rule, background=Ink.bgElev, content)` | Rounded `Box` with the canonical 0.5-dp rule border — used inline by most screens even though they often re-implement the same combination directly. |
| `displayStyle(sizeSp, tracking=-1.2)` | Convenience `TextStyle` builder for the "ink/serif" display feel. |
| `monoStyle(sizeSp, tracking=0.3)` | Convenience for the monospaced/countdown style. |

`SafewordDisplay.kt` is the earlier (legacy) "phrase renderer" kept for
`GroupDetailScreen`. It capitalizes each word token except numeric ones.

---

## 7. Plain Mode — `ui/plain/PlainMode.kt`

A self-contained accessibility surface targeting elderly users and children.
Activated from Settings → "High visibility mode". The entire file uses the
`A11y` palette exclusively — there is no Ink token anywhere in Plain mode.

### Design invariants

- **Contrast** — `A11y.fg = White` against `A11y.bg = #0B1220` reaches WCAG
  AAA. CTAs use yellow `#FFD23F` on navy, also AAA. The ok/danger chrome
  uses the very dark analogs `#052E14` / `#3A0A0A` as text-on-color for
  sufficient contrast on the green/red answer buttons.
- **Hit targets** — every primary tap target is at least 72 dp tall
  (`BigButton.defaultMinSize = 72.dp`; `AnswerButton` = 80 dp; tab items =
  60 dp). Well above the Material 48-dp minimum.
- **Typography** — headline 34–44 sp `ExtraBold` with −0.8 to −1.2 sp
  tracking; body 19–22 sp Medium with 27–32 sp line height; buttons 22–24 sp
  `ExtraBold`.
- **Borders** — Plain mode uses `2.dp` borders everywhere (vs 0.5 dp in Ink
  mode) to stay visible through screen magnifiers.

### Internal structure

```
private enum class PlainScreen { Home, Verify, Help, Onboarding }

@Composable fun PlainRoot(onExitPlain: () -> Unit = {})
@Composable private fun PlainTabBar(active, onChange, modifier)
@Composable private fun BigButton(label, onClick, primary = true, icon)
@Composable private fun PlainHome(onVerify)
@Composable private fun PlainVerify(onDone)       // switches ask/match/nomatch
@Composable private fun PlainAsk(onMatch, onMismatch, onCancel)
@Composable private fun AnswerButton(label, bg, fg, icon, onClick)
@Composable private fun PlainResult(safe, title, body, primaryLabel, onPrimary, secondaryLabel?, onSecondary)
@Composable private fun PlainHelp()
@Composable private fun PlainOnboarding(onDone)
```

**`PlainRoot`** holds `onboarded: rememberSaveable` plus `screen:
rememberSaveable<PlainScreen>`. If not onboarded, shows `PlainOnboarding`
first. Otherwise shows the selected screen with `PlainTabBar` overlaid at
`Alignment.BottomCenter`.

**`PlainTabBar`** is a 3-tab bar (Home "Word" / Verify "Check" / Help
"Help") with the active tab rendered as a fully-filled accent-yellow pill
holding inverted `accentInk` label text at 15 sp Bold. Tabs sit in a
rounded (26-dp radius) `A11y.bgElev` container with a 2-dp `A11y.rule`
border.

**`PlainHome`** polls the first group's safeword on a 1-s loop, renders the
"YOUR WORD TODAY" card with each word on its own 48-sp `ExtraBold` line,
plus an inline `A11y.bgInset` pill showing "New word in Xh left" via
`humanTime(remaining)`. Below the hero sits a single `BigButton` labeled
"Someone is calling me" wired to `onVerify`.

**`PlainVerify`** is a phase-string state machine (`"ask" / "match" /
"nomatch"`) that swaps between `PlainAsk` and two variants of `PlainResult`.
`PlainAsk` shows "STEP 1 OF 2", the annotated prompt "Ask them: *What is our
word?*", a `bgElev` tip card reinforcing *Do not read the word*, and two
gigantic answer buttons:

- Green `A11y.ok` background with ink `#052E14` Check icon + label.
- Red `A11y.danger` background with ink `#3A0A0A` Close icon + label.

`PlainResult` fills the hero card with a 120-dp tone-colored circle, a 44-sp
title (green "Safe to talk." / red "Hang up now."), a 20-sp body, and one or
two `BigButton`s for next steps.

**`PlainHelp`** is a static scroll list of 5 help tiles (each an 80-dp tap
target) plus a prominent red-outlined EMERGENCY card ("If you feel unsafe,
call 911.").

**`PlainOnboarding`** is a 2-panel flow with a progress indicator, an
`EXAMPLE WORD` card showing "Golden Robin" in accent yellow at 38 sp, and a
Back link below the primary CTA once `step > 0`.

---

## 8. Navigation — `ui/navigation/SafewordsNavigation.kt`

### `Screen` sealed class

```
sealed class Screen(val route: String) {
    data object Home        : Screen("home")
    data object Groups      : Screen("groups")
    data object Verify      : Screen("verify")
    data object Settings    : Screen("settings")
    data object Onboarding  : Screen("onboarding")
    data object GroupDetail : Screen("group/{groupId}")  { fun createRoute(id) = "group/$id" }
    data object QRDisplay   : Screen("qr_display/{groupId}") { fun createRoute(id) = ... }
    data object QRScanner   : Screen("qr_scanner")
}
```

### `SafewordsNavigation()`

The composable entry point mounted by `MainActivity`. Responsibilities:

1. Owns `var plainMode by rememberSaveable { mutableStateOf(false) }`. While
   `plainMode == true` it renders `PlainRoot` and returns — completely
   short-circuiting the Compose navigation graph.
2. Otherwise, `rememberNavController()` + `currentBackStackEntryAsState()`
   drive a `Scaffold` whose `bottomBar` is the `CustomTabBar` — only shown
   when the current route is one of the four top-level tab routes
   (`showTabBar = tabs.any { it.screen.route == route }`).
3. `startDestination` is `Onboarding` if `GroupRepository.groups.value` is
   empty, otherwise `Home`.
4. Declares `composable(...)` entries for every `Screen`. `GroupDetail` and
   `QRDisplay` take `navArgument("groupId") { type = NavType.StringType }`.
5. Top-level tab navigation uses the canonical
   `popUpTo(graph.findStartDestination().id) { saveState = true };
   launchSingleTop = true; restoreState = true` pattern so tab state is
   preserved across switches.

### `CustomTabBar`

The bottom tab bar is **not** `NavigationBar` — it is a bespoke floating pill.
Four tabs: Word / Groups / Verify / Settings, using outlined Material icons
(`Shield`, `Groups`, `Phone`, `Settings`). The Row is clipped to a 28-dp
corner radius, painted `Ink.bgElev` with a 0.5-dp `Ink.rule` border, and
padded 12 dp horizontally, 26 dp vertically — the 26-dp bottom inset is what
makes it "float" above the navigation gesture area on modern devices. The
active tab is rendered with `Ink.bgInset` background and `Ink.fg` icon/label;
inactive tabs stay transparent with `Ink.fgMuted`.

---

## 9. Widget — `widget/`

An independent Android library module that depends on `:app` so it can reuse
the repository and crypto code. Components are declared in the **app**
manifest because widget providers must be discoverable from the main
application manifest; `widget/AndroidManifest.xml` is therefore empty.

### `SafewordsWidget : GlanceAppWidget`

`provideGlance(context, id)` reads `GroupRepository.groups.value`, picks the
first group, pulls the seed via `GroupRepository.getGroupSeed(id)`, and
derives the current phrase with `TOTPDerivation.deriveSafeword`. The phrase
is capitalized (words get `titlecase(Locale.ROOT)`; pure-digit tokens are
left alone so "breezy rocket 75" becomes "Breezy Rocket 75"). The widget
then calls `provideContent { WidgetContent(...) }`.

`WidgetContent` is a Glance `Column` on an `Ink.bgElev` background:

- Group name, 12 sp Medium, muted parchment.
- Capitalized phrase, 22 sp Bold, ember accent (`#E8553A`).
- "Rotates in HH:MM:SS" footer via `TOTPDerivation.formatTimeRemaining`,
  11 sp muted.

Because the widget is its own module it cannot reach `ui.theme.Ink` (that's
in the `:app` module's UI package), so the Ink ARGB values are duplicated
inline as `ColorProvider(day = …, night = …)`. `ColorProvider`'s two
arguments enforce the exact same color in light and dark system modes —
the widget always looks "Ink".

### `SafewordsWidgetReceiver : GlanceAppWidgetReceiver`

- `override val glanceAppWidget = SafewordsWidget()`.
- `onEnabled` schedules a **30-minute** `PeriodicWorkRequest` under the
  unique name `"safewords_widget_update"` with
  `ExistingPeriodicWorkPolicy.KEEP`.
- `onDisabled` cancels that unique work.
- `class WidgetUpdateWorker : Worker` — its `doWork()` just returns
  `Result.success()`; the receiver's invocation by WorkManager is itself
  what causes Glance to re-enter `provideGlance` and recompose.

`safewords_widget_info.xml` declares a 250-dp × 80-dp widget (4×2 cell
target) that resizes horizontally and vertically, with
`android:updatePeriodMillis="1800000"` (30 minutes — matches the
WorkManager cadence).

---

## 10. Data & Services

### `GroupRepository` (singleton `object`)

The app's single source of truth for groups.

- **Storage** — metadata is serialized as a JSON array via Gson and written
  to the `safewords_groups_prefs` `EncryptedSharedPreferences` file (backed
  by an AES-256-GCM master key in the Android Keystore). A companion
  `DEFAULT_INTERVAL_KEY` string is stored in the same file.
- **Reactive state** — exposes `val groups: StateFlow<List<Group>>` backed
  by an internal `MutableStateFlow` that's hydrated in `init { loadGroups()
  }`. All mutations go through `saveGroups` which updates the flow *and*
  persists the JSON.
- **Creation** — `createGroup(name, creatorName, interval)` generates a
  fresh 32-byte seed via `java.security.SecureRandom`, hex-encodes it, calls
  `SecureStorageService.saveSeed(groupId, hex)`, creates a `Member` with
  `Role.CREATOR`, and appends the new `Group` to the list.
- **Joining** — `joinGroup(name, seedHex, interval, memberName)` accepts
  the pre-parsed QR payload values, saves the seed, creates a `Member` with
  `Role.MEMBER`, and appends the group.
- **Queries** — `getGroup(id)`, `getGroupSeed(id): ByteArray?` (returns the
  decoded seed bytes), `getCurrentSafeword(id): String?` (convenience
  one-shot derivation), `getDefaultInterval()`/`setDefaultInterval(i)`.
- **Mutations** — `updateGroup(group)`, `deleteGroup(id)` (also deletes the
  seed), `addMember(id, m)`, `removeMember(id, memberId)`.

### `SecureStorageService` (singleton `object`)

Thin wrapper around a *separate* encrypted prefs file
(`safewords_secure_prefs`) so seeds are never stored in the same record as
group metadata. Keys are prefixed `seed_<groupId>`; values are 64-char hex
strings. Same AES-256-SIV / AES-256-GCM schemes and same Keystore-backed
`MasterKeys.AES256_GCM_SPEC` master key.

### `QRCodeService` (singleton `object`)

QR payload schema (JSON):

```json
{ "v": 1, "name": "Johnson Family",
  "seed": "<url-safe base64 32 bytes, no wrap, no padding>",
  "interval": "daily" }
```

- `generateQRBitmap(group, size = 512): Bitmap?` — looks up the hex seed,
  decodes to bytes, URL-safe base64-encodes, builds the JSON payload, then
  runs ZXing's `QRCodeWriter.encode(... MARGIN=2, CHARACTER_SET=UTF-8)` and
  converts the `BitMatrix` into an `ARGB_8888` bitmap (black modules on
  white).
- `parseQRPayload(raw): ParsedGroup?` — Gson-deserialize, require `v == 1`
  and `seedBytes.size == 32`, re-hex-encode, map `interval` string through
  `RotationInterval.fromKey`. Returns a `ParsedGroup(name, seedHex,
  interval)` or `null` on any failure.

### `TOTPDerivation` (singleton `object`)

The crypto core. Matches the spec in `docs/totp-word-algorithm.md` and the
frozen vectors in `shared/test-vectors.json`:

1. `require(seed.size == 32)`, `require(interval > 0)`.
2. `counter = timestamp / interval`, serialized big-endian via
   `ByteBuffer.allocate(8).putLong(counter).array()`.
3. `hash = HmacSHA256(seed).doFinal(counterBytes)` via
   `javax.crypto.Mac.getInstance("HmacSHA256")`.
4. `offset = hash[31] & 0x0F` (dynamic truncation — RFC 4226 style).
5. Three indices extracted by `((hash[o+n] & 0x7F) << 8 | (hash[o+n+1] &
   0xFF)) % modulus` where the moduli are 197, 300, 100.
6. Return `"$adjective $noun $number"` — lowercase words + a 0–99 integer
   (no zero-pad).

Helper methods on the same object: `getCurrentCounter(interval)`,
`getTimeRemaining(interval)` (seconds until next rotation),
`formatTimeRemaining(seconds)` → `"HH:MM:SS"` or `"MM:SS"`,
`hexToBytes`/`bytesToHex`.

### `WordLists` (singleton `object`)

Lazy `val adjectives` and `val nouns` properties that each `open` the
corresponding asset file via `SafewordsApp.instance.assets` and Gson-parse
it into a `List<String>`. Lists are **frozen v1** — 197 adjectives, 300
nouns — and must remain byte-identical across app versions to preserve
cross-device and cross-platform determinism.

---

## 11. Build

### Commands (run from `repos/safewords-android/`)

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (minified, ProGuard enabled)
./gradlew assembleRelease

# Unit tests (Android-free; runs on the JVM)
./gradlew test

# Just the app-module tests
./gradlew :app:test

# Install debug on connected device / emulator
./gradlew installDebug

# Full clean build
./gradlew clean build
```

### Gradle module structure

- **Root `build.gradle.kts`** only declares plugin aliases with `apply false`;
  each subproject applies what it needs.
- **`:app`** — `com.android.application` + `kotlin-android` + `kotlin-compose`.
  Minifies on `release` using `proguard-android-optimize.txt` +
  `proguard-rules.pro`.
- **`:widget`** — `com.android.library` + `kotlin-android` + `kotlin-compose`.
  Pulls in `:app` via `implementation(project(":app"))`; reuses the Compose
  BOM, Glance, Glance-Material3, WorkManager, Security-Crypto, and Gson.
- Both modules target JVM 11 (`compileOptions` + `kotlinOptions.jvmTarget =
  "11"`) and enable `buildFeatures { compose = true }`.
- **Version catalog** — every library + plugin is pinned in
  `gradle/libs.versions.toml`. Bumping a dependency means editing this one
  file. Compose libraries are sourced via the `compose-bom` platform
  dependency in `:app` and re-used in `:widget`.

### Tests

`app/src/test/kotlin/com/thc/safewords/TOTPDerivationTest.kt` runs on the
local JVM (no emulator required) using JUnit 4 and inlined copies of the
197-word adjective list and 300-word noun list so the crypto path can be
validated against `shared/test-vectors.json` without booting an Android
context. This is the primary guard that the Android implementation stays
byte-compatible with iOS.
