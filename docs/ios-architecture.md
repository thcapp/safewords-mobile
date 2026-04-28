# iOS Architecture

Reference documentation for the Safewords iOS implementation at `repos/safewords-ios/`.

This document covers every Swift source file, the design system, screen structure, routing, widget, data layer, and build tooling. It is the single source of truth for how the iOS app is organized.

---

## 1. Overview

**Safewords iOS** is a native SwiftUI application that displays a rotating TOTP-derived "safeword" phrase (e.g. `Breezy Rocket 75`) to verify family/trusted contacts against AI voice-clone scams. Same seed + same time window on any device produces the same phrase, with zero network dependency.

| Attribute | Value |
|-----------|-------|
| Language | Swift 5.9+ |
| UI framework | SwiftUI |
| Deployment target | iOS 17.0 |
| Bundle ID | `com.thc.safewords` |
| App Group | `group.com.thc.safewords` |
| Crypto | Apple `CryptoKit` (`HMAC<SHA256>`) |
| Seed storage | Keychain (App-Group scoped, `kSecAttrAccessibleAfterFirstUnlock`) |
| Metadata storage | `UserDefaults(suiteName:)` of the App Group |
| Randomness | `SecRandomCopyBytes` for 32-byte seeds |
| Widget | WidgetKit static configuration, Small + Medium families |
| Camera | AVFoundation `AVCaptureMetadataOutput` (QR) |
| QR generation | Core Image `CIFilter.qrCodeGenerator` |
| Dependencies | Zero external packages (stdlib only) |

The project is managed with **xcodegen** (`project.yml`) so the `.xcodeproj` is not checked in. A `Package.swift` also exposes a `SafewordsCore` library target (`Crypto/`, `Models/`, `Services/`, `Data/` resources) so crypto + models can be reused / unit-tested without SwiftUI.

Three Xcode targets are produced:
1. **Safewords** — main app, everything under `Safewords/`.
2. **SafewordsWidget** — app extension, `SafewordsWidget/`. Bundles its own copies of `adjectives.json` / `nouns.json` and duplicates minimal crypto + keychain + model code (see [§8 Widget](#8-widget)).
3. **SafewordsTests** — XCTest target, `SafewordsTests/` against the `Safewords` main target.

---

## 2. File layout

Top-level under `/data/code/safewords-mobile/repos/safewords-ios/`:

```
Package.swift               # SPM manifest (SafewordsCore library product)
project.yml                 # xcodegen spec (targets, entitlements, resources)
Safewords/                  # main app sources
  Safewords.entitlements    # App Group + keychain access groups
  App/
  Crypto/
  Data/                     # JSON word lists (bundled resources)
  Design/
  Models/
  Services/
  Views/
    Components/
SafewordsWidget/            # widget extension (separate target)
  SafewordsWidget.entitlements
  SafewordsWidgetBundle.swift
  SafewordsWidget.swift
SafewordsTests/             # XCTest target
  TestVectors.swift
  TOTPDerivationTests.swift
```

### File-by-file table

| File | Role |
|------|------|
| `Package.swift` | SPM manifest. Declares `SafewordsCore` library (excludes `App/` and `Views/`). Processes `Data/*.json` as resources. Wires `SafewordsTests` test target. |
| `project.yml` | xcodegen spec. Sets iOS 17 deployment, Swift 5.9, three targets, App Group entitlement, QR camera usage string, bundles `adjectives.json` + `nouns.json` as resources in both app and widget. |
| `Safewords/Safewords.entitlements` | `com.apple.security.application-groups` = `group.com.thc.safewords`; matching `keychain-access-groups`. |
| `Safewords/App/SafewordsApp.swift` | `@main` entry point. Creates the `@State private var groupStore = GroupStore()`, injects via `.environment(groupStore)`, forces `.preferredColorScheme(.dark)`. |
| `Safewords/App/ContentView.swift` | Root router. Branches on `@AppStorage("plainMode")` to choose `PlainRoot` vs `mainRoot`; in main mode branches on `@AppStorage("onboarded")` and an `AppScreen` enum to swap `HomeView`/`GroupsView`/`VerifyView`/`SettingsView`/`OnboardingView`/`QRDisplayView`, overlaying `CustomTabBar` except during onboarding / invite flows. |
| `Safewords/Crypto/TOTPDerivation.swift` | Core deterministic engine. Loads `adjectives.json` (197) and `nouns.json` (300) from bundle, asserts counts. Implements `deriveSafeword`, `deriveSafewordCapitalized`, `deriveIndices`, `getTimeRemaining`, `getCurrentCounter`, `currentPeriodStart`, `nextRotationDate`, `progress`, `generateSeed` (via `SecRandomCopyBytes`), `dataFromHex`. |
| `Safewords/Data/adjectives.json` | Frozen v1 adjective list — 197 lowercase entries. Bundled as a resource into both app and widget. |
| `Safewords/Data/nouns.json` | Frozen v1 noun list — 300 lowercase entries. Bundled as a resource into both app and widget. |
| `Safewords/Design/Theme.swift` | Design tokens. Two palettes (`Ink`, `A11Y`), `Fonts` family helpers (`display`/`body`/`mono`) with custom-font-or-system fallback, and a `Color(hex:)` extension. |
| `Safewords/Models/Group.swift` | `Group` struct (`id`, `name`, `interval`, `members`, `createdAt`) + `RotationInterval` enum (`hourly`/`daily`/`weekly`/`monthly`) with `seconds`, `displayName`, `shortLabel`. |
| `Safewords/Models/Member.swift` | `Member` struct (`id`, `name`, `role`, `joinedAt`) + `Role` enum (`creator`/`member`). Extensions compute `initials` and a deterministic `colorIndex` (0–7). |
| `Safewords/Services/KeychainService.swift` | Enum-namespaced keychain CRUD for 32-byte seeds. Uses `kSecAttrService = "com.thc.safewords.seeds"`, `kSecAttrAccount = group UUID`, `kSecAttrAccessGroup = group.com.thc.safewords`. `saveSeed`, `getSeed`, `deleteSeed`, `deleteAllSeeds`. |
| `Safewords/Services/GroupStore.swift` | `@Observable` store. Persists `[Group]` to the App Group `UserDefaults` (key `safewords.groups`, ISO seconds date strategy), persists `selectedGroupID`. Exposes `createGroup`, `joinGroup`, `updateGroupName`, `updateGroupInterval`, `addMember`, `removeMember`, `deleteGroup`, `currentSafeword(for:)`, `safeword(for:at:)`, `seed(for:)`. |
| `Safewords/Services/QRCodeService.swift` | Generates + parses QR payloads. `QRPayload` struct (`v`, `name`, `seed` base64url, `interval`). `generateQRCode(for:seed:size:)` uses `CIFilter.qrCodeGenerator`, correction `"M"`. `parseQRCode` validates `v == 1`, 32-byte seed, known interval. Adds base64url (un)encoding as `Data` extensions. |
| `Safewords/Views/HomeView.swift` | Home screen hero. Uses `TimelineView(.periodic(from:.now, by: 1.0))` to tick once per second; draws group pill + bell, `CountdownRing` hero with the phrase, countdown string, and `SEQ · NNNN` counter. Supports `revealStyle = "holdReveal"` for blur-until-held, via a `DragGesture(minimumDistance: 0)`. Empty state CTA routes to onboarding. Also defines the shared `AppScreen` enum (home/groups/verify/settings/onboarding/addMember). |
| `Safewords/Views/OnboardingView.swift` | 3-panel carousel with progress dots, back arrow, and accent-capsule CTA. Panel 0 "One word between trust and deception" (welcome + stacked-words visual). Panel 1 "Start a group, or join someone else's" (create / join-QR / join-recovery options). Panel 2 "Back up your seed" (12-word grid + amber warning card). Completes onboarding by setting `screen = .home`, which `ContentView` converts into `onboarded = true`. |
| `Safewords/Views/GroupsView.swift` | Groups list. Header ("Your circles") + plus button → `.addMember`. For each `Group` renders a `groupCard` with `GroupDot`, name + `ACTIVE` chip, member count, rotation label, current safeword in mono, vertical `SEQ NNNN` on the right. Below the list: members section with `statusTag` ("SYNCED" dot). |
| `Safewords/Views/VerifyView.swift` | Verification flow. Four `Phase`s: `.ready` (textfield + Check / Mic buttons, plus 3-step "if they can't give it" tips), `.listening` (3 animated pulse rings + mic circle, demo match/mismatch buttons), `.match` / `.mismatch` (`ResultCard` with large circle + headline + advice card). Compares typed value case-insensitively against `groupStore.currentSafeword`. |
| `Safewords/Views/QRDisplayView.swift` | "Share in person" invite screen. Header with back arrow → `.groups`. Renders the generated QR (via `QRCodeService.generateQRCode`) inverted on a card with accent center dot. Alt-action card "Invite via SMS instead". Footer chip: `256-BIT · ROTATING · OFFLINE`. |
| `Safewords/Views/QRScannerView.swift` | Camera-based QR join flow. `NavigationStack` with cancel toolbar; `QRCameraPreview` (`UIViewRepresentable` wrapping `QRCameraUIView`, an `AVCaptureSession` + `AVCaptureMetadataOutput` with `[.qr]` types and success haptic). On scan parses via `QRCodeService.parseQRCode`, prompts for the joiner's name, then calls `groupStore.joinGroup`. Includes torch toggle. Not wired to `AppScreen` — presented from the join flow. |
| `Safewords/Views/SettingsView.swift` | Preferences. Scroll of card sections: Rotation (interval segmented control), Accessibility (`plainMode` toggle → `@AppStorage`), Widget & Lock Screen, Security, Practice, Danger zone (reset confirmation alert → deletes all groups + `KeychainService.deleteAllSeeds`). Footer: "Safewords v1.0 · Offline-first". |
| `Safewords/Views/PlainModeViews.swift` | Entire Plain mode — see [§6](#6-plain-mode). Contains `PlainScreen` enum, `A11yFonts`, `BigButton`, `A11yTabBar`, `PlainHomeView`, `PlainVerifyView`, `PlainHelpView`, `PlainOnboardingView`, `PlainRoot`. |
| `Safewords/Views/Components/CountdownRing.swift` | Custom 60-tick dashed dial countdown — see [§5](#5-custom-components). |
| `Safewords/Views/Components/CustomTabBar.swift` | Floating pill tab bar with 4 tabs (Word/Groups/Verify/Settings). |
| `Safewords/Views/Components/GroupDot.swift` | Filled circle + centered initial; also defines `DotPalette` (5 colors) and `forIndex(_:)`. |
| `Safewords/Views/Components/SectionLabel.swift` | Small uppercase eyebrow, 1.4 tracking, muted by default. |
| `SafewordsWidget/SafewordsWidgetBundle.swift` | `@main` widget bundle with `SafewordsWidget()` inside. |
| `SafewordsWidget/SafewordsWidget.swift` | Full widget implementation: `SafewordsTimelineProvider`, `SafewordsEntry`, small + medium views, widget-local duplicates of `WidgetRotationInterval`, `WidgetGroup`, `WidgetMember`, `WidgetTOTP`, `WidgetKeychain`, and `Color(hex:)`. |
| `SafewordsWidget/SafewordsWidget.entitlements` | Same App Group + keychain access group as the main app (required to read shared UserDefaults + Keychain). |
| `SafewordsTests/TestVectors.swift` | Source of truth test vectors (loaded from `shared/test-vectors.json`) + hex helper. |
| `SafewordsTests/TOTPDerivationTests.swift` | Asserts every shared vector (phrase, counter, hash, offset, indices), word-list sizes, mid-period == start-period, determinism, seed length/randomness, capitalization. |

### Directory walkthrough

**`Safewords/App/`** — App entry + top-level routing.
- `SafewordsApp.swift` creates the shared `GroupStore` and sets a dark color scheme.
- `ContentView.swift` is the routing brain; see [§7](#7-app-routing).

**`Safewords/Crypto/`** — Pure, view-free cryptography.
- `TOTPDerivation.swift` is the only file. Ships all constants (`adjectiveCount = 197`, `nounCount = 300`, `numberMod = 100`), loads the JSON lists lazily, and exposes the seed/time → phrase pipeline plus time helpers.

**`Safewords/Data/`** — Bundled resources only, no Swift.
- `adjectives.json` / `nouns.json` are the frozen v1 word lists extracted from safewords-io. Both the main target and widget target list these as resources in `project.yml`.

**`Safewords/Design/`** — Design-system tokens.
- `Theme.swift` defines `Ink` (default) and `A11Y` (Plain mode) enums, the `Fonts` helper, and `Color(hex:)`.

**`Safewords/Models/`** — Codable domain types.
- `Group.swift` — `Group` + `RotationInterval`.
- `Member.swift` — `Member` + `Role` + initials/color-index extensions.

**`Safewords/Services/`** — Platform-bridging services.
- `KeychainService.swift` — seed CRUD (App Group shared).
- `GroupStore.swift` — `@Observable` in-memory + `UserDefaults` state machine, wraps `KeychainService` + `TOTPDerivation`.
- `QRCodeService.swift` — payload codec + Core Image QR render.

**`Safewords/Views/`** — All SwiftUI screens; one file per top-level screen. Plain mode lives entirely inside `PlainModeViews.swift` to keep its design system cohesive.

**`Safewords/Views/Components/`** — Reusable primitives used across multiple screens.

---

## 3. Design system

Everything design-related lives in `Safewords/Design/Theme.swift`.

### Ink palette (default)

Editorial, near-monochrome with a single ember accent. All foreground tints are derived from a warm off-white `#F5F2EC` at decreasing opacities.

| Token | Hex (or expression) | Usage |
|-------|---------------------|-------|
| `Ink.bg` | `#0B0B0C` | Screen background |
| `Ink.bgElev` | `#151517` | Elevated card fills |
| `Ink.bgInset` | `#1C1C1F` | Inset / inner row fills |
| `Ink.fg` | `#F5F2EC` | Primary foreground |
| `Ink.fgMuted` | `#F5F2EC` @ 55% | Secondary foreground |
| `Ink.fgFaint` | `#F5F2EC` @ 32% | Tertiary foreground |
| `Ink.rule` | `#F5F2EC` @ 8% | Dividers and hairline strokes |
| `Ink.accent` | `#E8553A` | Ember accent (primary CTAs, progress) |
| `Ink.accentInk` | `#0B0B0C` | Text color on top of `accent` |
| `Ink.ok` | `#9DBF8A` | Success green |
| `Ink.warn` | `#E8A13A` | Warning amber |
| `Ink.tickFill` | `#E8553A` @ 18% | Ember chip background |

### A11Y palette (Plain mode)

High-contrast amber-on-navy for the accessibility mode. All pairings aim for WCAG AAA.

| Token | Hex | Usage |
|-------|-----|-------|
| `A11Y.bg` | `#0B1220` | Deep navy background |
| `A11Y.bgElev` | `#18243C` | Card fill |
| `A11Y.bgInset` | `#24354F` | Inner pill / inset row |
| `A11Y.fg` | `white` | Primary text |
| `A11Y.fgMuted` | `#CBD5E1` | Secondary text |
| `A11Y.fgFaint` | `#94A3B8` | Tertiary text |
| `A11Y.rule` | `white` @ 22% | Stroke at 2px line width |
| `A11Y.accent` | `#FFD23F` | Amber (primary CTAs, highlights) |
| `A11Y.accentInk` | `#0B1220` | Dark text on top of amber |
| `A11Y.ok` | `#4ADE80` | Safe green |
| `A11Y.danger` | `#FF6B6B` | Warning / hang-up red |
| `A11Y.tickFill` | `#FFD23F` @ 22% | Amber chip background |

### `Fonts` helpers

Three size-and-weight factories with graceful fallbacks when a custom font isn't registered in the bundle:

```swift
Fonts.display(42, weight: .regular)   // Fraunces serif, else .system .serif
Fonts.body(16,   weight: .medium)     // system sans
Fonts.mono(12,   weight: .regular)    // system monospaced
```

`A11yFonts` (in `PlainModeViews.swift`) mirrors this shape and prefers Atkinson Hyperlegible (`AtkinsonHyperlegible-Regular`), falling back to system sans.

### `Color(hex:)` initializer

Accepts 3- or 6-char hex, with or without `#`. Parses via `Scanner.scanHexInt64`, splits RGB into Doubles, and constructs `Color(red:green:blue:)`. Duplicated verbatim in the widget (`SafewordsWidget.swift`) so the extension is available in that compilation unit without depending on the app module.

---

## 4. Standard-mode screens

All standard (Ink-mode) screens live in `Safewords/Views/*.swift`. The shared navigation enum is declared at the bottom of `HomeView.swift`:

```swift
enum AppScreen: String, CaseIterable {
    case home, groups, verify, settings, onboarding, addMember
}
```

### 4.1 `HomeView`

**File:** `Safewords/Views/HomeView.swift`

**State**
- `@Environment(GroupStore.self) groupStore`
- `@Binding var screen: AppScreen`
- `@AppStorage("revealStyle") revealStyle: String = "always"` — `"always"` or `"holdReveal"`.
- `@State var held: Bool = false` — true while the user is pressing the phrase for hold-to-reveal.

**Structure**
- Background `Ink.bg` ignoring safe area.
- If `groupStore.selectedGroup == nil`, `emptyState` (shield icon + "No groups yet" + amber CTA to onboarding).
- Otherwise, a `TimelineView(.periodic(from: .now, by: 1.0))` ticks once per second, recomputing `remaining` / `progress` / `phrase` for that instant.
- Top bar: leading capsule pill with `GroupDot` + group name + chevron, tapping routes to `.groups`. Trailing circle bell button.
- Hero: `CountdownRing(progress:)` 340×340 framed, with inner `VStack` containing a tiny LIVE dot + `SectionLabel`, the phrase stacked one word per line in `Fonts.display(46)` with `-1.5` tracking, and `SEQ · 0001` counter in mono.
- Below hero: `HH:MM:SS` countdown in mono 28pt + "rotates in …" subtitle.

**Hold-to-reveal**
When `revealStyle == "holdReveal"`, the stacked words apply `.blur(radius: 14)` and a `DragGesture(minimumDistance: 0)` toggles `held`. When `held` is false the ring overlay shows "Hold to reveal" at the bottom.

**Matches the handoff**: the dashed countdown dial, the three-line stacked typography, the capsule pill at top-left, and the `SEQ · NNNN` caption all mirror the Figma hero mockup.

### 4.2 `OnboardingView`

**File:** `Safewords/Views/OnboardingView.swift`

**State**
- `@Binding var screen: AppScreen`
- `@State var step: Int = 0` — 0..2.
- Hard-coded `sampleSeed` array of 12 dummy words for panel 2.

**Structure**
- 3 step dots at top; the active index scales 2× horizontally with `.scaleEffect(x:, anchor:.leading)`.
- Panel switcher via `if step == 0 { panelWelcome } else if step == 1 { panelStart } else { panelSeed }`.
- Footer: optional back arrow (step > 0) + amber CTA capsule. CTA label toggles `"Get started"` → `"Continue"` → `"I've saved it"`; the final press sets `screen = .home`, and `ContentView` flips `@AppStorage("onboarded") = true` on that transition.

**Panel 1 — Welcome (`panelWelcome`)**
`SectionLabel("Safewords · 01")`, display headline `One word between [trust] and deception.` (the word `trust` is ember), body copy about voice cloning, then a `VStack` of 5 sample phrases styled as stacked noise: two faint mono lines, one accent-highlighted phrase in the middle on `Ink.tickFill`, two more faint lines.

**Panel 2 — Start (`panelStart`)**
3 `onboardOption` rows: primary `"Create a new group"` (ember-filled card), then `"Join with a QR code"`, `"Join with a recovery phrase"`. Each taps `step += 1` (placeholder wiring).

**Panel 3 — Seed (`panelSeed`)**
3-column `LazyVGrid` showing 12 numbered words on a card, then an ember `⚠` warning card on `Ink.tickFill`.

### 4.3 `GroupsView`

**File:** `Safewords/Views/GroupsView.swift`

**State**
- `@Environment(GroupStore.self) groupStore`
- `@Binding var screen: AppScreen`

**Structure**
- Header "Your circles" + trailing circular `+` button → `.addMember` (invite screen).
- `groupCard` per group: 44pt `GroupDot` (color from `DotPalette.forIndex(idx)`), name + `ACTIVE` chip (shown when `group.id == groupStore.selectedGroupID`), member count + rotation interval, current phrase in mono, a rotated vertical `SEQ NNNN` on the right edge. Active card gets an ember stroke.
- Tapping a card sets `selectedGroupID` and routes to `.home`.
- If a group is selected, a `Members · <name>` section lists `memberRow`s with a green `SYNCED` status tag.

### 4.4 `VerifyView`

**File:** `Safewords/Views/VerifyView.swift`

**State**
- `@Environment(GroupStore.self) groupStore`
- `@Binding var screen: AppScreen`
- `@State var phase: Phase = .ready` where `enum Phase { case ready, listening, match, mismatch }`
- `@State var typed: String = ""`
- `@State var pulse: Bool = false` — drives the 3-ring animation.

**Phases**
- `.ready` — the default `readyPanel`: an "Their answer" card with a mono textfield prompt, a bottom row containing an amber "Check" button (disabled when empty) and a mic circle that jumps to `.listening`. Below: 3 numbered tip rows ("Hang up immediately", "Call them back on a known number", "Try an emergency override word") on a second card.
- `.listening` — concentric animated circles: 3 strokes of `Ink.accent` each with `.easeOut(duration: 2.4).repeatForever(autoreverses: false).delay(i * 0.4)`, scaling 0.3 → 1.0. Center ember circle with `mic.fill` icon. Two demo buttons force a match/mismatch for testing. A "Cancel" link resets to `.ready`.
- `.match` / `.mismatch` — render `ResultCard(match:)` (private struct at file bottom). Green checkmark + "Verified." + "They gave the correct word. This is them." for match; ember triangle + "Don't trust." + "Hang up. Call them on their known number." card for mismatch.

**Check logic (`check()`):**
```swift
if typed.lowercased().trimmingCharacters(in: .whitespaces)
    == word.lowercased() { phase = .match } else { phase = .mismatch }
```

### 4.5 `QRDisplayView`

**File:** `Safewords/Views/QRDisplayView.swift`

**Structure**
- Header: back arrow → `.groups`, `Invite · <name>` eyebrow + "Share in person" title.
- Content: 240×240 QR image (via `QRCodeService.generateQRCode`) rendered with `.interpolation(.none)`, `.colorInvert()` on `Ink.bgElev`, wrapped in a 28-corner card with a soft shadow. A small ember center dot is overlaid.
- Helper text with a highlighted "Join with QR" span.
- Footer chip: `🔒 256-BIT · ROTATING · OFFLINE` in faint mono.
- Alt action: "Invite via SMS instead" card (placeholder).

### 4.6 `SettingsView`

**File:** `Safewords/Views/SettingsView.swift`

**State**
- `@AppStorage("plainMode")`, `@AppStorage("revealStyle")`
- `@State var selectedInterval: RotationInterval = .daily`
- `@State var showResetConfirmation: Bool = false`

**Structure**
Scrollable list of card sections, each rendered by the `section(label:)` helper:
1. **Rotation · <group>** — interval segmented control + "Notify on rotation" + "Include preview of next word".
2. **Accessibility** — `toggleRow("High visibility mode", binding: $plainMode)`. Flipping this toggle immediately reroutes to `PlainRoot` via `ContentView`.
3. **Widget & Lock Screen** — installed state rows.
4. **Security** — Face ID, Emergency override word, Rotate seed, Backup.
5. **Practice** — scam drill cadence.
6. **Danger zone** — leave group + reset device (fires an `.alert` confirming, then deletes every group via `groupStore.deleteGroup(id)` plus `KeychainService.deleteAllSeeds()`).

Footer: `Safewords v1.0 · Offline-first` / `No server. No account. No data collection.`

---

## 5. Custom components

All in `Safewords/Views/Components/`.

### 5.1 `CountdownRing`

**File:** `Safewords/Views/Components/CountdownRing.swift`

A view that takes a `progress: Double` (0…1) and an inner `@ViewBuilder content`, drawing:

1. **60 ticks** — one per `i in 0..<60`. Every 5th tick (`big = i % 5 == 0`) draws thicker (`lineWidth: 1`) and slightly outside the ring; the others are `lineWidth: 0.6`. Tick angle is `Double(i)/60 * 2π - π/2` so position 0 is at 12 o'clock. Ticks with `Double(i)/60 < progress` render at 90% opacity (elapsed); upcoming ticks at 25%.
2. **Progress arc** — a `Circle().trim(from: 0, to: progress).stroke(Ink.accent, lineWidth: 1.5, lineCap: .round).rotationEffect(-90°)`.
3. **Leading-edge knob** — a 10pt ember circle plus a 16pt accent-25% ring, positioned at `(cx + cos(angle) * r, cy + sin(angle) * r)` where `angle = progress * 2π - π/2`.
4. **Content** — `content()` rendered dead-center inside the ring.

The ring radius is `min(width, height)/2 - 12` from a `GeometryReader`.

### 5.2 `CustomTabBar`

**File:** `Safewords/Views/Components/CustomTabBar.swift`

Floating pill bar, 4 tabs: Word / Groups / Verify / Settings, each a `VStack(icon, label)`. The active tab gets `Ink.bgInset` fill on a 22-corner inset; the whole bar is a `RoundedRectangle(cornerRadius: 28)` on `Ink.bgElev` with a hairline rule, 40pt black shadow. Padding: 12 horizontal, 26 from bottom safe area.

### 5.3 `GroupDot`

**File:** `Safewords/Views/Components/GroupDot.swift`

Solid colored `Circle` with a centered capital initial at `size * 0.42` point, white foreground. The sibling `DotPalette` enum holds the 5 avatar colors used across the app:

| Color | Hex |
|-------|-----|
| red | `#E8553A` |
| blue | `#6E94E7` |
| green | `#9DBF8A` |
| orange | `#E89B5E` |
| purple | `#B47AE8` |

`DotPalette.forIndex(i)` uses a non-negative modulo wheel so `hashValue % 5` inputs always map cleanly.

### 5.4 `SectionLabel`

**File:** `Safewords/Views/Components/SectionLabel.swift`

One-line eyebrow: uppercases the supplied string, `Fonts.body(11, weight: .medium)` with `.tracking(1.4)`, defaulting to `Ink.fgMuted` (overridable via `color:`).

---

## 6. Plain mode

All in `Safewords/Views/PlainModeViews.swift`. Designed for elderly family members and children.

### 6.1 A11Y palette

See [§3](#3-design-system). Key choices:
- **Background `#0B1220` + accent `#FFD23F`** yields ~15:1 contrast (WCAG AAA).
- **Body text is white** on deep navy (17:1).
- **2px strokes** throughout (vs. the hairline 0.5px in Ink mode) for low-vision readability.
- **Minimum text size 20pt** for body; 34–48pt for headlines and the phrase itself.

### 6.2 `A11yFonts`

Prefers the Atkinson Hyperlegible font family if registered (`AtkinsonHyperlegible-Regular`), else falls back to the system sans. Both `body` and `display` route through the same factory — Plain mode intentionally avoids the editorial serif of Ink mode to keep letterforms unambiguous.

### 6.3 `BigButton`

A rounded-rectangle button with:
- **Min height 72pt** (hit target, well above Apple's 44pt HIG minimum).
- Label in `A11yFonts.body(22, weight: .bold)`.
- Optional leading 22pt bold SF Symbol.
- Two variants: `.primary` (amber fill, dark ink text) and `.ghost` (elevated navy, white text, 2pt amber-less rule stroke).
- 18pt corner radius.

### 6.4 `A11yTabBar`

3 tabs: Word / Check / Help. Each is a 60pt-tall `VStack(icon, label)` that fills a rounded amber pill when active. Outer pill is `A11Y.bgElev`-filled, 26 corner, 2pt rule stroke, 40pt shadow at 45% black. No Settings tab in Plain mode — the mode is intentionally narrow.

### 6.5 Screens

Four screens routed from `PlainRoot`:

| Screen | Purpose |
|--------|---------|
| `PlainHomeView` | The word, big. Header with circle initial + "Your circle" + group name. Hero card: amber `● YOUR WORD TODAY` chip; phrase split across lines at `A11yFonts.body(48, weight: .heavy)` with `-1.5` tracking; pill capsule `↻ New word in N minutes left`; footnote "Share this word only with your family." Uses the same `TimelineView(.periodic)` pattern as Home. |
| `PlainVerifyView` | Three phases: `.ask` (big amber "Ask them: 'What is our word?'" + instruction card + two huge answer buttons — green "Yes, it matched" and red "No, wrong word"); `.match` (green circle + checkmark + "Safe to talk." + "They said the right word. This is really them." + BigButton "All done"); `.nomatch` (red circle + X + "Hang up now." + long plain-language warning + BigButton "I hung up" + ghost BigButton "Call them back on a trusted number"). |
| `PlainHelpView` | A `ScrollView` of 5 labeled help items (each a 80pt-minimum tall card with icon circle, bold title, subtitle, chevron). Bottom: a red-bordered emergency card "If you feel unsafe, call 911." |
| `PlainOnboardingView` | Two-panel intro: ("WELCOME", "One word keeps you safe.", ...) and ("HOW IT WORKS", "Your family picks a secret word.", ...). Each shows an `EXAMPLE WORD · Golden Robin` card. Progress bar uses 6pt rounded rectangles. Persists its own completion via `@AppStorage("plainOnboarded")`. |

### 6.6 `PlainRoot`

Container that gates onboarding with `@AppStorage("plainOnboarded")`. After onboarding, shows `A11yTabBar` pinned to the bottom and swaps `PlainHomeView`/`PlainVerifyView`/`PlainHelpView` in a `ZStack(alignment: .bottom)`.

---

## 7. App routing

**File:** `Safewords/App/ContentView.swift`

Two persistent flags drive routing:

| `@AppStorage` key | Meaning |
|-------------------|---------|
| `plainMode` | `true` → render `PlainRoot`; else render `mainRoot`. Toggled from `SettingsView`. |
| `onboarded` | `true` → skip first-run onboarding in main mode. Set when `OnboardingView` sets `screen = .home`. |

`mainRoot` is a `ZStack(alignment: .bottom)`:

1. Background `Ink.bg.ignoresSafeArea()`.
2. A `Group { ... }` switch:
   - If `!onboarded && groupStore.groups.isEmpty` → `OnboardingView` (it owns the back-to-home transition; `.onChange(of: screen)` flips `onboarded = true` when the user lands on `.home`).
   - Else, switch on the `@State var screen: AppScreen`: `.home` / `.groups` / `.verify` / `.settings` / `.onboarding` / `.addMember` (`QRDisplayView`).
3. `CustomTabBar(active: $screen)` overlaid with a `.transition(.opacity)`, gated by `tabBarShown`:
   - Hidden during `.onboarding` and `.addMember`.
   - Otherwise shown as long as the user has onboarded OR has at least one group.

The whole thing is forced `.preferredColorScheme(.dark)`.

Plain mode has its own independent router inside `PlainRoot` (see above). Flipping `plainMode` in Settings causes `ContentView.body` to re-evaluate and swap the entire root subtree.

---

## 8. Widget

**Files:**
- `SafewordsWidget/SafewordsWidgetBundle.swift`
- `SafewordsWidget/SafewordsWidget.swift`
- `SafewordsWidget/SafewordsWidget.entitlements`

### Target separation

The widget is a separate app-extension target. Swift extension code does not share compiled modules with the main app (app extensions cannot directly import the host app's module). To avoid adding a Swift framework target, `SafewordsWidget.swift` **duplicates** the minimum code it needs:

- `WidgetRotationInterval` — mirrors `RotationInterval`.
- `WidgetGroup`, `WidgetMember` — mirror `Group` / `Member` for decoding the shared `UserDefaults` blob.
- `WidgetTOTP` — a compact reimplementation of `TOTPDerivation.deriveSafewordCapitalized`, `getTimeRemaining`, and `nextRotationDate` (same algorithm, same modulos: `% 197`, `% 300`, `% 100`).
- `WidgetKeychain` — a read-only `getSeed(forGroup:)` using the same service + access group.
- `Color(hex:)` — duplicated extension.

Both targets list `Safewords/Data/adjectives.json` and `Safewords/Data/nouns.json` under `resources` in `project.yml` so each has its own bundle copy. `WidgetTOTP` loads them from `Bundle.main` exactly as the app does.

### Timeline provider

`SafewordsTimelineProvider` implements `TimelineProvider`:

- `placeholder` — static `"Breezy Rocket 75"` preview.
- `getSnapshot` — derives the current entry or falls back to placeholder.
- `getTimeline` — loads the selected group + seed, generates 5 entries (`i in 0..<5`) for the current period and the next 4, each starting exactly at its period boundary (`counter * interval`), then sets a `.after(nextRotationDate(interval:))` reload policy so WidgetKit refreshes as soon as the current word rotates.

`loadDefaultGroup` reads `UserDefaults(suiteName: "group.com.thc.safewords")` → key `safewords.groups` (JSON) and `safewords.selectedGroupID` (string), then reads the matching seed from the keychain.

### Views

Two families are supported via `.supportedFamilies([.systemSmall, .systemMedium])`:

- **`SafewordsWidgetSmallView`** — centered phrase in `Color(hex: "#E8553A")` (Ink ember), with a tiny `clock` + remaining time caption. `containerBackground` = `#0B0B0C`.
- **`SafewordsWidgetMediumView`** — leading column with uppercase group name + phrase, trailing 44pt countdown circle using `Circle().trim(from:0, to: 1 - remaining/interval).stroke(#E8553A, lineWidth: 4).rotationEffect(-90°)` over a faded ring, plus compact remaining time.

The `formatCompact` helper renders `1h 23m` or `12:34`.

---

## 9. Data & services

### 9.1 `GroupStore` (`@Observable`)

- Lives in `Safewords/Services/GroupStore.swift`.
- Declared `@Observable final class GroupStore` (Swift Observation macro, iOS 17+), injected as `.environment(groupStore)` and consumed via `@Environment(GroupStore.self)`.
- Reads/writes group metadata to `UserDefaults(suiteName: KeychainService.appGroupID)` under keys `safewords.groups` (JSON-encoded `[Group]`) and `safewords.selectedGroupID` (string UUID).
- `createGroup` generates a 32-byte seed via `TOTPDerivation.generateSeed`, stores it with `KeychainService.saveSeed`, appends and sorts by `createdAt`.
- `joinGroup` is the QR-scan path: caller passes the parsed seed + interval + member name.
- `currentSafeword(for:)` / `safeword(for:at:)` fetch the seed from keychain and delegate to `TOTPDerivation.deriveSafewordCapitalized`.
- `deleteGroup` removes the seed then the metadata, fixing up `selectedGroupID`.

### 9.2 `KeychainService`

- `Safewords/Services/KeychainService.swift` — `enum KeychainService` (namespace, no instances).
- Uses `kSecClassGenericPassword`, `kSecAttrService = "com.thc.safewords.seeds"`, `kSecAttrAccount = group.id.uuidString`, `kSecAttrAccessGroup = "group.com.thc.safewords"`, `kSecAttrAccessible = kSecAttrAccessibleAfterFirstUnlock` (so the widget can read post-boot).
- API: `saveSeed`, `getSeed`, `deleteSeed`, `deleteAllSeeds`.
- `saveSeed` pre-deletes any existing entry so it doubles as an upsert.

### 9.3 `QRCodeService`

- `Safewords/Services/QRCodeService.swift`.
- `QRPayload` is `{ v: 1, name, seed (base64url-no-padding), interval }` — matches `shared/qr-schema.json`.
- `generateQRCode(for:seed:size:)` serializes the payload as JSON, passes it to `CIFilter.qrCodeGenerator` with correction level `"M"`, rescales the output via `CGAffineTransform(scaleX:y:)` to the requested pixel size, and returns a `UIImage` rendered via `CIContext.createCGImage`.
- `parseQRCode(_:)` validates `v == 1`, decodes the base64url seed (adding padding), asserts `seed.count == 32`, and looks up the interval in `RotationInterval(rawValue:)`.
- Includes `Data.base64URLEncodedString()` / `Data(base64URLEncoded:)` extensions that strip / re-add `=` padding and substitute `-_` for `+/`.

### 9.4 `TOTPDerivation`

- `Safewords/Crypto/TOTPDerivation.swift`.
- Uses Apple `CryptoKit.HMAC<SHA256>.authenticationCode(for:using:)` — no third-party crypto.
- Loads word lists at first access via `Bundle.main.url(forResource:withExtension:)`, decodes with `JSONDecoder`, asserts `words.count == 197` / `== 300`.
- The algorithm is the standard "HOTP-style dynamic truncation with modulo mapping":

```
counter  = floor(timestamp / interval)
bytes    = Int64(counter).bigEndian
hash     = HMAC-SHA256(key: seed, message: bytes)
offset   = hash[31] & 0x0F
adjIdx   = ((hash[offset    ] & 0x7F) << 8 | hash[offset + 1]) % 197
nounIdx  = ((hash[offset + 2] & 0x7F) << 8 | hash[offset + 3]) % 300
number   = ((hash[offset + 4] & 0x7F) << 8 | hash[offset + 5]) % 100
phrase   = "\(adjective) \(noun) \(number)"
```

- `generateSeed()` calls `SecRandomCopyBytes(kSecRandomDefault, 32, &bytes)` and traps on non-`errSecSuccess`.
- Time helpers (`getTimeRemaining`, `getCurrentCounter`, `currentPeriodStart`, `nextRotationDate`, `progress`) are pure and share the same `floor(now / interval)` anchor.

### 9.5 Tests

`SafewordsTests/TOTPDerivationTests.swift` runs every vector in `shared/test-vectors.json` through `deriveSafeword`, `getCurrentCounter`, direct `HMAC<SHA256>` re-check, the offset byte, and the index triple. It also enforces:

- word-list sizes (197 / 300),
- determinism (same inputs, same output),
- mid-period == start-period (e.g. 12:00 UTC produces the same daily word as 00:00 UTC),
- different daily counters produce different phrases,
- seed length and randomness,
- capitalization (`"breezy rocket 75"` → `"Breezy Rocket 75"`).

---

## 10. Build

### Prerequisites

- Xcode 15+ / iOS 17 SDK.
- [`xcodegen`](https://github.com/yonaskolb/XcodeGen) (Homebrew: `brew install xcodegen`).

### Generate the Xcode project

From `repos/safewords-ios/`:

```bash
xcodegen generate
open Safewords.xcodeproj
```

`project.yml` regenerates `.xcodeproj` from scratch on every run — do not edit the project file by hand. Key highlights:

- **Three targets**: `Safewords` (application), `SafewordsWidget` (app-extension embedded in Safewords), `SafewordsTests` (unit-test bundle depending on `Safewords`).
- **Deployment target** iOS 17.0, `SWIFT_VERSION = 5.9`, universal (`TARGETED_DEVICE_FAMILY = "1,2"`).
- **Bundle IDs**: `com.thc.safewords`, `com.thc.safewords.widget`, `com.thc.safewords.tests`.
- **Entitlements** (both app + widget): App Group `group.com.thc.safewords` and a matching `keychain-access-groups` entry (`$(AppIdentifierPrefix)group.com.thc.safewords`).
- **Info plist values** (inline): `NSCameraUsageDescription`, portrait-only, launch screen, display name.
- **Resources**: `Safewords/Data/adjectives.json` and `Safewords/Data/nouns.json` are declared as resources on **both** the app target and the widget target.
- **Scheme**: `Safewords` (builds app + widget + test target for `test` action).

### Build & test

```bash
# Build for Simulator
xcodebuild -scheme Safewords \
  -destination 'platform=iOS Simulator,name=iPhone 15' build

# Run unit tests (validates all shared TOTP vectors)
xcodebuild -scheme Safewords \
  -destination 'platform=iOS Simulator,name=iPhone 15' test
```

### SPM library

`Package.swift` declares a `SafewordsCore` library (`Crypto/`, `Models/`, `Services/`, `Data/` resources) that excludes `App/` and `Views/` so the crypto + data layer can be tested or reused without SwiftUI. The test target `SafewordsTests` depends on it for non-Xcode test runs.

### Signing

`project.yml` leaves `DEVELOPMENT_TEAM = ""` and uses `CODE_SIGN_STYLE = Automatic`. Set a team in Xcode (or `project.yml`) before running on device. The App Group and keychain-access-group entitlements require a paid Apple developer team that owns the `com.thc` prefix (or an updated prefix matching your team).

---

## Cross-references

- Algorithm specification: `docs/totp-word-algorithm.md`
- Feature specification: `docs/feature-spec.md`
- Shared word lists: `shared/wordlists/{adjectives,nouns}.json`
- Cross-platform test vectors: `shared/test-vectors.json`
- QR schema: `shared/qr-schema.json`
- Sibling Android implementation: `repos/safewords-android/`
