# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Native mobile apps for rotating TOTP-based family safewords. **Two native apps** — Swift/SwiftUI for iOS, Kotlin/Compose for Android. Companion to [safewords.io](https://safewords.io) — extends the web concept with **automatic rotation** using time-based cryptography.

**Status: IMPLEMENTATION** — Shared foundation complete, native apps in development.

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

- **Feature spec**: `docs/feature-spec.md` — full product requirements with all phases
- **TOTP algorithm**: `docs/totp-word-algorithm.md` — core crypto algorithm spec (HMAC-SHA256 → word mapping)
- **Test vectors**: `shared/test-vectors.json` — frozen seed+time→phrase pairs for cross-platform validation
- **QR schema**: `shared/qr-schema.json` — QR payload format specification
- **Word lists**: `shared/wordlists/` — 197 adjectives, 300 nouns (frozen v1, extracted from safewords-io)

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

### Android (from repos/safewords-android/)
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install on connected device/emulator
./gradlew installDebug
```

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

- **Zero network dependency** for core function — TOTP word derivation is purely on-device
- **No backend server, no user accounts** — groups are local + QR-shared seeds
- **Deterministic cross-platform** — same seed + time = identical word on iOS and Android
- **Word lists are frozen** — 197 adjectives, 300 nouns. Changing lists breaks sync. Embed v1 in app bundle.
- **Secure storage only** — seeds in platform keychain/keystore, never plain storage
- **Minimal permissions** — camera (QR scan only); SMS and notifications are Phase 2
- **Shared test vectors** — `shared/test-vectors.json` is the cross-platform contract

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
