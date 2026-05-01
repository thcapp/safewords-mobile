# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Native mobile apps for rotating TOTP-based safewords. **Two native apps** — Swift/SwiftUI for iOS, Kotlin/Compose for Android. Companion to [safewords.io](https://safewords.io) — extends the web concept with **automatic rotation** using time-based cryptography.

**Status: SHIPPING v1.3.1** — Both apps on internal testing tracks (Android Play internal, iOS TestFlight). Production submission gated on listing copy refresh, fresh screenshots, and Console-only declarations. See `docs/release-state.md` for the live snapshot.

## Project Layout

This is a **Based** managed project (`/data/code/safewords-mobile/`). Configuration in `.base.yaml`, realm: `thc`.

| Directory | Purpose |
|-----------|---------|
| `repos/safewords-ios/` | Swift/SwiftUI iOS app (Xcode project) |
| `repos/safewords-android/` | Kotlin/Compose Android app (Gradle project) |
| `shared/` | Cross-platform specs: word lists, test vectors, QR schema |
| `docs/` | Feature specs, architecture docs — read these before implementing |
| `queue/` | Pending work items (process in numeric order) |
| `inbox/` | Drop zone for unprocessed input |
| `output/` | Builds, artifacts |
| `ops/` | Scripts, tools |
| `state/` | Logs, memory, context |

### Key Documentation

- **Doc index**: `docs/README.md` — start here, ordered reading list and by-role guide
- **Feature spec**: `docs/feature-spec.md` — original product requirements + Phase 4 (BIP39, primitives, Plain default, cards, demo mode)
- **v1.3 architecture**: `docs/v1.3-architecture.md` — as-built reference for the v1.3 surface area
- **v1.3 design brief**: `docs/v1.3-best-in-class-design.md` — the locked design (claude + codex iteration)
- **Safety cards**: `docs/safety-cards.md` — card system, sensitivity tiers, render pipelines, gating
- **Demo mode**: `docs/demo-mode.md` — synthetic seed, group lifecycle, parity rules
- **TOTP algorithm**: `docs/totp-word-algorithm.md` — core crypto algorithm spec
- **Release state**: `docs/release-state.md` — Play + TestFlight snapshot
- **Pipeline gotchas**: `docs/release-pipeline-gotchas.md` — every release-pipeline failure mode and its fix

### Cross-platform contracts

All under `/shared/`. Both apps must produce byte-identical results from these:

- `test-vectors.json` — v1.0 rotating word derivations
- `recovery-vectors.json` + `recovery-schema.md` — v1.2 BIP39 contract (entropy-only, 24-word, no PBKDF2)
- `primitive-vectors.json` — v1.3 static override / numeric / challenge-answer
- `migration-vectors.json` — v1.2 → v1.3 group config schema migration
- `safety-card-copy.json` — card copy + sensitivity tiers
- `qr-schema.json` — QR invite payload format
- `wordlists/{adjectives,nouns}.json` — 197 + 300, frozen v1
- `wordlists/bip39-english.txt` — BIP39 English (2048 words)

### Related Codebases

- **safewords-io** (`/data/code/safewords-io/`) — Web app (SvelteKit)
  - Word lists source: `repos/safewords-web/src/lib/data/en/wordlists/{adjectives,nouns}.ts`
  - Mobile marketing page: `repos/safewords-web/src/routes/app/+page.svelte`

## Commands

### iOS (from repos/safewords-ios/)
```bash
# Generate Xcode project (requires xcodegen)
xcodegen generate

# Build
xcodebuild -scheme Safewords -destination 'platform=iOS Simulator,name=iPhone 15' build

# Test
xcodebuild -scheme Safewords -destination 'platform=iOS Simulator,name=iPhone 15' test

# Open in Xcode
open Safewords.xcodeproj
```

### Android (run on `u5` dev VM — primary VM has no gradle/SDK/keystore)

Sync changes to u5 first, then build/test there:
```bash
# From primary VM
rsync -av repos/safewords-android/app/src/ u5:/home/ultra/code/safewords-mobile/repos/safewords-android/app/src/
rsync -av repos/safewords-android/app/build.gradle.kts u5:.../app/build.gradle.kts
rsync -av repos/safewords-android/gradle/libs.versions.toml u5:.../gradle/libs.versions.toml

# Then on u5
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && ./gradlew :app:testDebugUnitTest"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && ./gradlew :app:assembleDebug"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane build"      # signed AAB
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane internal"   # build + push to Play
```

**Validate non-trivial Android changes on u5 before committing.** Code that compiles on the primary VM mentally doesn't validate against the real Kotlin compiler, dependency graph, or test environment. Three classes of bugs only surface there: same-package private name collisions, missing `androidx.*` dependencies, and runtime-vs-test resource access (`SafewordsApp.instance` is unavailable in JVM unit tests). The memory file at `~/.claude/projects/-data-code-safewords-mobile/memory/feedback_validate_on_u5.md` has more.

## Technical Stack

### iOS
- **Swift 5.9+** / **SwiftUI** with iOS 17+ deployment target
- **CryptoKit** for HMAC-SHA256 (stdlib, zero dependencies)
- **Keychain** via Security framework + App Group for widget access
- **WidgetKit** for home screen widget
- **AVFoundation** for QR scanning, **CoreImage** for QR generation

### Android
- **Kotlin 2.0+** / **Jetpack Compose** with API 26+ (Android 8) minimum
- **javax.crypto.Mac** for HmacSHA256 (stdlib)
- **EncryptedSharedPreferences** (AndroidX Security) for seed storage
- **Jetpack Glance** for home screen widget
- **CameraX + ML Kit** for QR scanning, **ZXing** for QR generation

## Architecture Constraints

- **Zero network dependency** for core function — every primitive is purely on-device
- **No backend server, no user accounts** — groups are local + QR-shared seeds
- **Deterministic cross-platform** — same seed + time = identical word on iOS and Android (every primitive in `/shared/primitive-vectors.json` is fixture-tested)
- **Word lists are frozen** — 197 adjectives, 300 nouns + BIP39 English. Changing lists breaks sync. Embed v1 in app bundle.
- **Secure storage only** — seeds in platform keychain/keystore, never plain storage. Demo mode's hardcoded seed is the one exception (it's never written to keystore).
- **Minimal permissions** — camera (QR scan only). SMS and push notifications are deferred indefinitely.
- **Plain Mode is the default home** since v1.3 — not opt-in accessibility, the front door for everyone
- **Static derivations from seed only** — never store derived secrets (override word, C/A table, recovery phrase) in group metadata. Recompute on demand.
- **Cross-agent coordination via based MCP** — `based_message` is the active ping (writes inbox + activates terminal); `based_mail` is passive (inbox only). Default to message for any agent-to-agent interaction.

## TOTP Core Algorithm

```
HMAC-SHA256(seed, floor(timestamp / interval)) → extract bytes → map to adjective + noun + number
→ "Breezy Rocket 75"
```

```
offset = hash[31] & 0x0F
adj_idx  = ((hash[offset] & 0x7F) << 8 | hash[offset+1]) % 197
noun_idx = ((hash[offset+2] & 0x7F) << 8 | hash[offset+3]) % 300
number   = ((hash[offset+4] & 0x7F) << 8 | hash[offset+5]) % 100
```

Full spec in `docs/totp-word-algorithm.md`. Both platforms validate against `shared/test-vectors.json`.

## v1.3 verification primitives

Beyond the rotating word, v1.3 adds three more primitives — all deterministic from the same seed, all in `docs/v1.3-architecture.md` with full byte-level specs:

- **Numeric** — `(hash[offset..offset+3] & masks) % 1_000_000`, rendered as 6 zero-padded digits (RFC 4226 dynamic truncation)
- **Static override** — `HMAC-SHA256(seed, "safewords/static-override/v1")` → standard word derivation
- **Challenge / answer** — per row: `HMAC-SHA256(seed, "safewords/challenge-answer/v{N}/{ask|expect}/{rowIndex}")` → standard word derivation

Group config schema is at v2 (`schemaVersion: 2` + `primitives` object). v1.2 groups auto-migrate on read. Test contracts: `/shared/primitive-vectors.json` and `/shared/migration-vectors.json`.

## Design Language

- Dark theme, teal accent (#0f766e / #2dd4bf), amber CTA (#d97706)
- System fonts (SF Pro iOS, Roboto Android)
- Match safewords.io/app marketing page mockups

## CLI Tools

Use credential-injecting wrappers (auto-detect profile from realm `thc`):
- `gh` — GitHub CLI (aliased to `gh-wrap`)
- `aw` — Appwrite CLI wrapper (if needed later)
- Never use raw `appwrite` CLI directly

## Safety

- Never commit seed values, `.env` contents, or API keys
- Never hardcode cryptographic constants — derive from standard algorithms
- TOTP derivation must be deterministic and cross-platform
- Seed generation: `SecRandomCopyBytes` (iOS) / `SecureRandom` (Android)
- Both apps' unit tests must pass all shared test vectors before any release
