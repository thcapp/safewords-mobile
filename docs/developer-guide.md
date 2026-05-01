# Developer Guide — Safewords Mobile

Welcome! This is the first document you should read. It walks you from `git clone` to a running app on both iOS and Android, and points you at the deeper docs when you need them.

If something here is stale or wrong, fix it — this file lives in `docs/developer-guide.md` and everyone benefits when it stays fresh.

---

## 1. Project overview

**Safewords Mobile** is a pair of native mobile apps for **rotating TOTP-based family safewords**. Think Google Authenticator, but instead of a 6-digit code it emits a memorable phrase like `Breezy Rocket 75` that a trusted circle (family, friends, co-workers) can use as a verbal verification code that changes on a schedule.

- **iOS** — Swift / SwiftUI, iOS 17+
- **Android** — Kotlin / Jetpack Compose, API 26+ (Android 8)
- **Companion to** [safewords.io](https://safewords.io) — the web app already ships the word-list concept; the mobile apps extend it with **automatic rotation** using deterministic, on-device HMAC-SHA256.

Design goals that constrain every decision:

- **Zero network dependency** for the core function — no backend, no accounts.
- **Deterministic cross-platform** — the same seed + time must produce the same phrase on iOS and Android, byte for byte.
- **Seeds never leave the device** in plaintext; QR sharing is the only distribution path.
- **Frozen wordlists** (v1 = 197 adjectives, 300 nouns). Changing them breaks sync with existing groups.

Two apps, one contract — the cross-platform contract lives in `shared/`:
- `test-vectors.json` — original v1.0 TOTP rotating word vectors
- `recovery-vectors.json` — v1.2 BIP39 recovery phrase encode/decode vectors
- `primitive-vectors.json` — v1.3 static override / numeric / challenge-answer derivations
- `migration-vectors.json` — v1.2 → v1.3 group config schema migration
- `safety-card-copy.json` — shared card copy strings for native printable cards

If you change a derivation, every vector file becomes the test contract both apps must pass.

---

## 2. Repository layout

This is a **Based**-managed project (`.base.yaml` at root, realm `thc`). The top-level layout is the standard Based template:

| Directory  | Purpose |
|------------|---------|
| `repos/safewords-ios/`     | Swift / SwiftUI iOS app. Xcode project is **generated** via `xcodegen` from `project.yml`. |
| `repos/safewords-android/` | Kotlin / Compose Android app. Gradle 8+ multi-module (`:app`, `:widget`). |
| `shared/`                  | Cross-platform contracts: wordlists, test vectors, QR schema. Both apps consume these. |
| `docs/`                    | Feature specs, architecture docs, algorithm reference. **Read these before implementing.** |
| `queue/`                   | Numbered pending work items. Process in numeric order (`001-...`, `002-...`). |
| `inbox/`                   | Drop zone for unprocessed input (designs, notes, assets). |
| `output/`                  | Builds, APKs, IPAs, generated artifacts. |
| `ops/`                     | Scripts and tooling that support the project but aren't shipped. |
| `state/`                   | Logs, session memory, context — mostly tooling-owned. |
| `templates/`               | Project scaffolding templates. |

You will spend most of your time in `repos/`, `shared/`, and `docs/`.

---

## 3. Prerequisites

### iOS

- **macOS** with **Xcode 15+** installed (iOS 17 SDK).
- **xcodegen** — the Xcode project is generated from `project.yml`, not committed:
  ```bash
  brew install xcodegen
  ```
- An **iOS 17 simulator** (ships with Xcode 15). Physical device optional.
- An Apple Developer account is **not required** for simulator builds.

### Android

- **JDK 17+** (Temurin 17 or 21 recommended).
- **Android Studio Hedgehog+** *or* standalone **Gradle 8+** with the Android SDK command-line tools.
- **Android SDK Platform 34** (`compileSdk = 34`) and build-tools 34.
- An emulator image (API 26+) or a physical device with USB debugging.

### Both

- `git`, a POSIX shell.
- `gh` for GitHub (use the credential-injecting wrapper — see §8).

---

## 4. Getting started — iOS

```bash
cd repos/safewords-ios

# Generate the Xcode project from project.yml
xcodegen generate

# Open in Xcode
open Safewords.xcodeproj
```

In Xcode:

1. Select the **Safewords** scheme.
2. Pick any iOS 17+ simulator (iPhone 15 is the reference target).
3. Hit **Cmd+R** to build and run.

### Running tests (CLI)

```bash
xcodebuild -scheme Safewords \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  test
```

### Targets

The project has three targets, all defined in `project.yml`:

- **Safewords** — the main app (bundle ID `com.thc.safewords`).
- **SafewordsWidget** — WidgetKit extension (`com.thc.safewords.widget`).
- **SafewordsTests** — XCTest bundle.

The widget and app share state through App Group `group.com.thc.safewords` and the keychain-access-group of the same name. Entitlements are declared in `project.yml` and regenerated on every `xcodegen generate` — do not hand-edit them.

Wordlist JSON is bundled into both app and widget (`Safewords/Data/adjectives.json`, `nouns.json`), copied from `shared/wordlists/`.

---

## 5. Getting started — Android

```bash
cd repos/safewords-android

# Debug build
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug

# Unit tests
./gradlew test
```

Or open `repos/safewords-android/` in Android Studio and press the green **Run** button.

### Modules

- `:app` — main application (`com.thc.safewords`, `minSdk 26`, `compileSdk 35`). Single-module since v1.2 — the widget code moved into `:app` when the previous `:widget` module caused a `ClassNotFoundException` for the AppWidgetProvider receiver.

Key deps (from `app/build.gradle.kts` and `libs.versions.toml`):

- Jetpack Compose + Material3
- CameraX + ML Kit barcode scanning (QR input)
- ZXing (QR generation)
- `androidx.security.crypto` — EncryptedSharedPreferences for seed storage
- Gson for wordlist JSON

---

## 6. Shared assets

Everything under `shared/` is a **cross-platform contract**. Both apps read from it; breaking changes ripple into every existing user's groups.

| File | What it is |
|------|------------|
| `shared/wordlists/adjectives.json` | 197 adjectives, frozen at v1. |
| `shared/wordlists/nouns.json`      | 300 nouns, frozen at v1. |
| `shared/wordlists/bip39-english.txt` | BIP39 English wordlist (2048 words), v1.2+. |
| `shared/test-vectors.json`         | v1.0 rotating-word derivations. Both apps must pass all. |
| `shared/recovery-vectors.json`     | v1.2 BIP39 encode/decode test vectors (valid + invalid cases). |
| `shared/recovery-schema.md`        | BIP39 recovery contract spec (entropy-only, 24-word, no PBKDF2). |
| `shared/primitive-vectors.json`    | v1.3 static override / numeric / challenge-answer derivations. |
| `shared/migration-vectors.json`    | v1.2 → v1.3 group config schema migration. |
| `shared/safety-card-copy.json`     | v1.3 card copy + sensitivity tier per template. |
| `shared/qr-schema.json`            | v1 QR payload format for sharing a seed with a family member. |

**Rules:**

1. **Do not mutate wordlists or test vectors in v1.** The algorithm uses `% 197` and `% 300`; adding or removing a word shifts every index and silently breaks every deployed group.
2. Any change to the algorithm, QR schema, or list length requires **bumping the protocol version** and shipping migration logic on both platforms at the same time.
3. When copying the JSONs into the iOS or Android bundles, keep them byte-identical to `shared/`. Prefer a build-time copy step over hand editing.

---

## 7. Key commands cheat sheet

### iOS (from `repos/safewords-ios/`)

| Task | Command |
|------|---------|
| Regenerate Xcode project | `xcodegen generate` |
| Build | `xcodebuild -scheme Safewords -destination 'platform=iOS Simulator,name=iPhone 15' build` |
| Test | `xcodebuild -scheme Safewords -destination 'platform=iOS Simulator,name=iPhone 15' test` |
| Open in Xcode | `open Safewords.xcodeproj` |

### Android (from `repos/safewords-android/` — usually run on `u5` dev VM)

The primary VM (`/data/code/safewords-mobile/`) **does not have gradle, the Android SDK, or the keystore**. Build/test tooling lives on `u5`. Sync source first, then run remotely:

```bash
# Sync from primary → u5
rsync -av repos/safewords-android/app/src/ u5:/home/ultra/code/safewords-mobile/repos/safewords-android/app/src/
rsync -av repos/safewords-android/app/build.gradle.kts u5:.../app/build.gradle.kts
rsync -av repos/safewords-android/gradle/libs.versions.toml u5:.../gradle/libs.versions.toml

# Then build/test on u5
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && ./gradlew :app:testDebugUnitTest"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && ./gradlew :app:assembleDebug"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane build"      # signed AAB
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane internal"   # build + push to Play internal track
```

Validate non-trivial Android changes on `u5` BEFORE committing. Three classes of bugs only surface on a real compile:
1. Kotlin same-package private name collisions (we hit one with two `Phase` enums in the same package)
2. Missing dependencies (`androidx.print` for `PrintHelper` was missing initially)
3. Test-time vs runtime resource access (singletons that depend on `SafewordsApp.instance` fail in JVM unit tests)

| Task | Command |
|------|---------|
| Debug build | `./gradlew assembleDebug` |
| Install on device | `./gradlew installDebug` |
| Unit tests | `./gradlew test` |
| Instrumented tests | `./gradlew connectedAndroidTest` |
| Lint | `./gradlew lint` |
| Clean | `./gradlew clean` |

Format-on-save is expected for both platforms — use Xcode's default Swift formatter and Android Studio's Kotlin formatter. There's no enforced formatter config beyond IDE defaults right now.

---

## 8. Credential-injecting CLI wrappers

This machine is configured with wrappers that auto-detect your profile from the `thc` realm and inject the right credentials. **Always use the wrappers; never the raw upstream CLI.**

| Wrapper | Wraps | When to use |
|---------|-------|-------------|
| `gh`    | GitHub CLI (aliased to `gh-wrap`) | All GitHub ops — `gh pr create`, `gh issue view`, `gh repo clone`, etc. |
| `aw`    | Appwrite CLI | Only if/when a cloud component gets added. Not currently used. |

**Do not** run raw `appwrite ...`. Doing so will either fail on auth or target the wrong project.

---

## 9. Safety rules

These are non-negotiable. Violating any of them can compromise user trust.

1. **Never commit seed values, `.env` contents, keystores, or API keys.** If you need a local dev seed, generate one and keep it in the simulator/emulator, not in the repo.
2. **Seed generation must use the platform CSPRNG:**
   - iOS: `SecRandomCopyBytes`
   - Android: `java.security.SecureRandom`
   No `Random()`, no `arc4random_uniform()`, no "good enough" shortcuts.
3. **Seeds live in secure storage only:**
   - iOS: Keychain, in the App Group access group.
   - Android: `EncryptedSharedPreferences`.
   Never write a seed to plain `UserDefaults` / `SharedPreferences` / a file / a log.
4. **TOTP derivation must match `shared/test-vectors.json`.** Both apps ship unit tests that run the full vector set. Those tests must be green before any release build is cut.
5. **Never hardcode crypto constants** — always derive from standard algorithms (`HMAC-SHA256`, etc.).
6. **Minimal permissions.** Camera is required for QR scanning; that's it. SMS and notifications are Phase 2 and must not be requested in v1.

---

## 10. Design handoff workflow

Designs arrive from **Claude Design** as self-contained HTML/CSS/JS prototype bundles (dropped into `inbox/` or linked from a queue item). They are **visual references**, not code to port.

**Rules of the road:**

- Implement designs **natively** — SwiftUI on iOS, Compose on Android. Do not embed a WebView.
- **Match the visual output** — colors, spacing, typography, motion — pixel-close.
- **Do not copy the prototype's structure.** HTML/CSS idioms (flexbox nesting, CSS grid tricks, div soup) rarely map cleanly to SwiftUI's layout model or Compose's slot API. Re-express the design in the native vocabulary.
- When in doubt about a token (color, radius, spacing), consult **`docs/design-system.md`** — that's the authoritative source for what the design system says, regardless of what the prototype renders.

Core design tokens for quick reference:

- Dark theme, teal accent (`#0f766e` / `#2dd4bf`), amber CTA (`#d97706`).
- System fonts: SF Pro (iOS), Roboto (Android).
- Match the `safewords.io/app` marketing-page aesthetic.

---

## 11. Where to look

The `docs/` directory is organized by concern. Start here when you need deeper context:

| File | What you'll find |
|------|------------------|
| `docs/feature-spec.md`              | Full product requirements across all phases. The "what and why." |
| `docs/v1.3-best-in-class-design.md` | The locked v1.3 design brief (claude + codex iteration log). |
| `docs/v1.3-architecture.md`         | As-built v1.3 reference: primitives, group config, demo mode, cards, render pipelines. |
| `docs/safety-cards.md`              | Card system: types, sensitivity tiers, render pipelines, biometric gating. |
| `docs/demo-mode.md`                 | Demo seed, group, lifecycle, and cross-platform parity rules. |
| `docs/release-state.md`             | Live snapshot of Play and TestFlight tracks; updated as releases ship. |
| `docs/release-pipeline-gotchas.md`  | Postmortem of every weird thing we hit setting up Play + TestFlight pipelines. |
| `docs/totp-word-algorithm.md`       | Core algorithm spec (HMAC-SHA256 → adjective + noun + number). |
| `docs/totp-algorithm-reference.md`  | Algorithm reference + cross-platform contract + test-vector explanation. |
| `shared/recovery-schema.md`         | BIP39 recovery contract spec (entropy-only, 24-word, no PBKDF2). |
| `docs/ios-architecture.md`          | iOS code reference — module map, where things live, conventions. |
| `docs/android-architecture.md`      | Android code reference — module map, where things live, conventions. |
| `docs/design-system.md`             | Visual design tokens (colors, type, spacing, motion). |
| `docs/plain-mode.md`                | Plain Mode (the v1.3 default home) — large-type layout. |
| `docs/word-lists.md`                | Wordlist provenance, curation rules, v1 freeze rationale. |
| `docs/design-handoff-log.md`        | Record of design handoffs and what shipped vs. was deferred. |
| `docs/best-in-class-expansion-proposal.md` | Codex's original v1.3 expansion proposal (historical). |

---

## 12. Contributing

### Branching

- Default branch for this repo is **`master`**.
- Feature branches: `feature/<short-name>` or `fix/<short-name>`.
- Rebase before merging; prefer small, focused PRs.

### Commit style

Follow the style of recent commits — run `git log --oneline -n 20` to see the pattern. Short, imperative subject line, optional body for "why." Example from the current history:

```
Implement two native apps (iOS Swift + Android Kotlin) with shared TOTP foundation
```

### PR workflow

1. Push your branch and open a PR with `gh pr create`.
2. PR title mirrors the commit subject style (short, imperative).
3. PR body: what + why, test plan, screenshots for UI changes (both iOS and Android if the change is cross-platform).
4. **Before merging:** both apps' unit tests green, including the `shared/test-vectors.json` suite.

### Picking up work

Check `queue/` for numbered work items. Process in numeric order unless a later item is explicitly unblocked. Each queue file describes scope, acceptance criteria, and handoff notes.

---

## Troubleshooting quick hits

- **`xcodegen generate` says "command not found"** → `brew install xcodegen`.
- **Xcode builds but widget doesn't appear** → make sure App Group `group.com.thc.safewords` is enabled for both targets; re-run `xcodegen generate` after editing `project.yml`.
- **`./gradlew` fails with "Unsupported class file major version"** → wrong JDK. Use JDK 17: `java -version` should report `17.x` or `21.x`.
- **Test vectors fail on one platform only** → check byte order and modulo operations. The algorithm uses big-endian byte pairs; a `toInt()` on Android that silently sign-extends is the usual culprit.
- **Cross-platform phrases disagree** → almost always a wordlist drift. Diff `repos/safewords-ios/Safewords/Data/*.json` against `shared/wordlists/*.json`.

---

Welcome aboard. When you hit something that took more than 15 minutes to figure out, add a line to the troubleshooting section above — the next engineer will thank you.
