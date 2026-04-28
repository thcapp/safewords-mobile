# Design Handoff Implementation Log

A record of the design-to-native translation performed on **2026-04-20**.
This doc is the source of truth for "what matched spec, what shipped, what is
still outstanding." Future developers picking up the visual work should start
here before touching `repos/safewords-ios` or `repos/safewords-android`.

---

## 1. Handoff summary

| Field           | Value                                                           |
| --------------- | --------------------------------------------------------------- |
| Date received   | 2026-04-20                                                      |
| Source tool     | Claude Design                                                   |
| Bundle format   | Static HTML/CSS/JS React prototype + shared component exports   |
| Primary file    | `Safewords App.html`                                            |
| Supporting      | `project/` (top-level prototype), `components/` (primitives), `chats/chat1.md` (design intent transcript), `README.md` |
| Target surfaces | iOS (Swift/SwiftUI) and Android (Kotlin/Compose)                |
| Implementation  | Landed same day across both native apps                         |

The bundle shipped a browsable prototype rather than Figma artboards. Each
screen was a React component in the HTML; shared primitives (countdown ring,
group dot, section label, tab bar, big button) were isolated in
`components/` so the native ports could mirror their API shape.

> **Archive note.** The unpacked bundle was consumed during the
> implementation pass and is no longer sitting in `/tmp/design-extract/`.
> If future work needs the original HTML, re-request the bundle from
> Claude Design or restore from the Based artifacts store — this doc
> captures the deltas, not the source.

---

## 2. User intent extracted from the chat

From `chats/chat1.md` and the prototype's ordering:

- **iOS-primary.** Design chose SwiftUI-idiomatic metaphors (capsule pills,
  SF system iconography, App Group widget language) as the reference, with
  Android as a faithful mirror rather than a Material-first redesign.
- **Dark by default.** Light was explored but not prioritized.
- **Editorial typography.** Serif display (Fraunces) for the safeword
  itself, mono for technical affordances (SEQ numbers, countdown), sans
  for body. Not "app chrome" — closer to a printed page.
- **Word-as-hero.** The safeword is the content; chrome recedes. Rings,
  tick marks, SEQ label all orbit around the phrase without competing.
- **Calm + trustworthy tone.** No glassy gradients, no heavy shadows in the
  hero, no security-theater iconography. The accent ember does one job:
  mark "live / active / rotating."
- **Three visual directions explored**:
  - **Ink** — editorial, warm paper-white on deep near-black, single ember
    accent. Shipped.
  - **Signal** — security-tool feel, higher contrast, more mono, more
    chrome. Designed but not shipped.
  - **Paper** — warm, human, light-leaning. Designed but not shipped.
- **Dark + light variants** were each explored per direction.
- **Three reveal styles** for the hero word: `always`, `hold`
  (tap-and-hold to unblur), and `ambient` (fades in on proximity / app
  foregrounding).
- **Plain mode** was added later in the chat, explicitly framed as a
  separate surface for elderly users and children. Not a theme swap — an
  information-architecture simplification with larger type, plain
  language rewrites ("your circle" not "group"), 72px+ hit targets,
  and a reduced 3-tab IA.

---

## 3. Screens in the handoff

### Standard mode
- Onboarding (3 panels: welcome, start, seed backup)
- Home (live safeword + countdown ring)
- Groups (circle list + member roster)
- Add Member / QR display
- Verify (ask-for-word flow with listening, match, mismatch)
- Settings

### Plain mode (accessibility surface)
- Welcome (2-panel onboarding with example word)
- My Word (the word, big)
- Check (2-step yes/no verification)
- Help (cards + emergency 911 callout)

---

## 4. What's shipped

Both platforms implement the full screen set in **Ink dark** only.

| Screen              | iOS source                                     | Android source                                                          | Notes |
| ------------------- | ---------------------------------------------- | ----------------------------------------------------------------------- | ----- |
| Onboarding          | `Views/OnboardingView.swift`                   | `ui/onboarding/OnboardingScreen.kt`                                     | 3 panels, ember CTA, seed grid on step 3 |
| Home                | `Views/HomeView.swift`                         | `ui/home/HomeScreen.kt`                                                 | Countdown ring + LIVE pill + SEQ + countdown string |
| Groups              | `Views/GroupsView.swift`                       | `ui/groups/GroupsScreen.kt` (+ `GroupDetailScreen.kt`)                  | Active pill, member rows with sync status |
| Add Member / QR     | `Views/QRDisplayView.swift`                    | `ui/qr/QRDisplayScreen.kt`                                              | "Share in person" header, SMS fallback row |
| Verify              | `Views/VerifyView.swift`                       | `ui/verify/VerifyScreen.kt`                                             | Ready / listening / match / mismatch panels |
| Settings            | `Views/SettingsView.swift`                     | `ui/settings/SettingsScreen.kt`                                         | Grouped sections, Danger zone, reset flow |
| Plain Onboarding    | `Views/PlainModeViews.swift` (`PlainOnboardingView`) | `ui/plain/PlainMode.kt` (`PlainOnboarding`)                       | 2-panel with Golden Robin example |
| Plain My Word       | `Views/PlainModeViews.swift` (`PlainHomeView`) | `ui/plain/PlainMode.kt` (`PlainHome`)                                   | Hero card, "New word in N hours left" pill |
| Plain Check         | `Views/PlainModeViews.swift` (`PlainVerifyView`) | `ui/plain/PlainMode.kt` (`PlainVerify`, `PlainAsk`, `PlainResult`) | Yes/No big buttons, "Hang up now" red panel |
| Plain Help          | `Views/PlainModeViews.swift` (`PlainHelpView`) | `ui/plain/PlainMode.kt` (`PlainHelp`)                                   | Help cards + emergency 911 card |

**Theme variants shipped: Ink dark only.** Signal, Paper, and all Light
variants are deferred. The Ink palette is defined identically on both
platforms:

- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Design/Theme.swift`
- `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/theme/Color.kt`

Plain mode uses its own `A11Y` / `A11y` palette (amber-on-navy, WCAG AAA).

---

## 5. What was deferred

Explicit list of design deliverables that were **not** implemented on
2026-04-20:

1. **Signal theme** — the security-tool visual direction. Designed, not
   ported. No tokens exist on either platform.
2. **Paper theme** — the warm / human / light-leaning direction. Same
   status.
3. **Light mode variants** — every direction shipped a light pair. None
   implemented. Both apps force `.preferredColorScheme(.dark)` (iOS) /
   rely on `Ink.bg` directly (Android).
4. **Ambient reveal style** — of the three reveal modes only `always`
   and `holdReveal` have code paths. `ambient` is not a value in the
   `revealStyle` picker, and no timer / foreground-detection wiring
   exists.
5. **Hold-to-reveal gesture asymmetry** — wired on iOS Home
   (`HomeView.swift` lines ~137–144, driven by the `revealStyle`
   `@AppStorage`). **Android always shows the word**; the Compose
   HomeScreen has no equivalent gesture yet.
6. **Ink-specific custom fonts.**
   - Design calls for **Fraunces** (editorial display) and **Atkinson
     Hyperlegible** (Plain mode).
   - Neither is registered on either platform. The code *probes* for
     them and falls back to system serif / system sans:
     - iOS: `Fonts.display` in `Design/Theme.swift` calls
       `UIFont(name: "Fraunces", size:)` and falls back to
       `.system(design: .serif)`. `A11yFonts.body` does the same for
       `AtkinsonHyperlegible-Regular`.
     - Android: `ui/theme/Type.kt` uses only `FontWeight` /
       `FontFamily.Default`; no `FontFamily` is declared and there is
       no `res/font/` directory.
   - To fix: drop the `.ttf` / `.otf` files in, register them
     (iOS: add to `Info.plist` under `UIAppFonts` and include in the
     bundle resources list in `project.yml`; Android: place in
     `app/src/main/res/font/` and declare `FontFamily(Font(R.font.…))`
     in `Type.kt`).
7. **Widget theme coverage** — `SafewordsWidget.swift` uses Ink ember
   (`#E8553A`) and Ink bg (`#0B0B0C`) hard-coded. No Signal / Paper
   widget variants; no Light variant honoring iOS system appearance.
   The Android Glance widget (`widget/…/SafewordsWidget.kt`) has not
   been audited for theme parity in this pass.
8. **Design's Tweaks panel / all-screens preview** — the HTML bundle
   shipped a side panel that let the reviewer see every screen in every
   theme at once. Not ported to native (and intentionally so — it's a
   design-review affordance, not a product feature). Noted in §8.

---

## 6. Subtle design choices preserved

These are the small, load-bearing details that are easy to regress in a
re-skin. They are in the code today:

- **60-tick dashed countdown ring** — every 5th tick is larger; elapsed
  ticks brighten to 0.9 alpha, unelapsed stay at 0.25.
  - iOS: `Views/Components/CountdownRing.swift` lines 17–33.
  - Android: `ui/components/CountdownRing.kt` lines 47–61.
- **Ember progress arc + knob on the leading edge.** Both a filled
  circle (5–10dp / pt) and a 1pt / 1dp stroked halo at 25% alpha. Both
  platforms.
- **LIVE indicator with pulsing dot.** 6pt ember dot next to
  `LIVE · <GROUP NAME>` on Home; renders inside the ring, sitting above
  the phrase.
- **SEQ number in mono, tracked wide.** Formatted as `%04d`, at 11pt
  letter-spacing 1.5.
- **"Share in person" header on QR.** The QR screen leads with a
  deliberate editorial headline rather than "Invite QR" or similar.
- **"Only share this in person. Anyone who scans it joins your group
  permanently."** — shown under the QR as a warning, not as a tooltip.
- **SMS-invite fallback row.** Present as a secondary affordance on QR
  display ("For family without the app — they get the rotating word by
  text.") — wired as a `Button` but currently no-ops on tap (handler is
  deliberately empty pending the SMS fallback feature in Phase 2 of the
  feature spec).
- **"256-BIT · ROTATING · OFFLINE" lockup.** Appears below the QR,
  mono 11pt, letter-spacing 1. Small lock glyph leading. Same on both
  platforms.
- **Plain-mode plain-language rewrites.**
  - "group" → "circle"
  - "rotation interval" → "a new one comes tomorrow"
  - "mismatch" → "Hang up now. They did not know the word."
  - "match" → "Safe to talk."
- **Emergency 911 callout** in Plain Help — amber-free red
  (`A11Y.danger`) panel, bold `If you feel unsafe, call 911.`

---

## 7. Platform adaptations

Where the design's HTML had no direct native analog, the choices below
were made:

| Design element (HTML)                       | iOS adaptation                                              | Android adaptation                                         |
| ------------------------------------------- | ----------------------------------------------------------- | ---------------------------------------------------------- |
| Simulated iOS status bar (drawn in SVG)     | Native status bar — do not draw                             | Native status bar                                          |
| Custom tab bar (pill with bg-elev + shadow) | `Views/Components/CustomTabBar.swift` — SwiftUI `Capsule`/`RoundedRectangle`, intentionally non-`TabView` | `ui/navigation/SafewordsNavigation.kt` `CustomTabBar` — `Row` + `RoundedCornerShape(28.dp)`, intentionally non-`NavigationBar` (avoids Material pill/indicator) |
| QR code (CSS-rendered demo grid)            | Real QR via `CoreImage.CIFilter.qrCodeGenerator()` in `Services/QRCodeService.swift` | Real QR via ZXing `MultiFormatWriter` in `service/QRCodeService.kt` |
| Animated expanding rings (Verify listening) | SwiftUI `.repeatForever` on three scaled/faded `Circle`s (`VerifyView.swift` lines 124–137) | `rememberInfiniteTransition` + `scale` modifier on a bordered circle (`VerifyScreen.kt` lines 220–227) |
| Hover states                                | Not translated (no hover on touch)                          | Not translated                                             |
| CSS `backdrop-filter: blur()` (hold reveal) | `.blur(radius: 14)` on the phrase stack                     | Not yet implemented (see §5, item 5)                       |
| Editorial serif display                     | `Font.system(design: .serif)` fallback                      | `FontWeight.Normal` on system sans fallback                |

---

## 8. Known gaps / follow-ups

Ordered by what a reviewer is likely to catch first:

1. **Run on device to verify countdown-ring knob at edge angles.** The
   trig math (`angle = progress * 2π − π/2`) is correct in both
   implementations, but on device the 5pt / 5dp filled circle +
   8pt / 8dp halo can get clipped at the top dead-center when
   progress crosses 0/1. Visual QA required on real hardware, not just
   simulator.
2. **Widget preview "phone-in-phone" card.** The design's Home screen
   included a miniature widget preview inside a home-screen card (a
   "here's what your widget looks like right now" affordance). Not
   implemented. Would live inside `HomeView` / `HomeScreen` below the
   hero.
3. **All-screens-at-once preview mode.** The handoff bundle had a
   Tweaks panel with every screen rendered in a grid. Not a product
   feature — called out here only so future design reviews don't
   expect it.
4. **Real QR generation in iOS `QRDisplayView`.** `QRDisplayView.swift`
   already calls `QRCodeService.generateQRCode(for:seed:size:)`
   (lines 56–64) and the service itself is fully implemented with
   `CIFilter.qrCodeGenerator()`. Confirm on device that the generated
   payload decodes cleanly against the Android scanner and matches
   `shared/qr-schema.json` v1. Suspected follow-up because the color
   invert + `.interpolation(.none)` combination can occasionally render
   off-white rather than pure paper-white on some iPhone displays.
5. **Ambient reveal style.** Either wire it or remove it from the
   Settings copy so it doesn't ship a dead option.
6. **Custom fonts (Fraunces / Atkinson Hyperlegible).** Until these
   register, the editorial intent is lost. This is the single biggest
   visual regression from the handoff — both Ink and Plain mode look
   "nice system app" rather than "designed object."
7. **Android hold-to-reveal.** Add a `pointerInput` / `awaitPointerEvent`
   gesture to `HomeScreen.kt` matching iOS's `DragGesture(minimumDistance: 0)`
   and the `revealStyle == "holdReveal"` branch.
8. **SMS invite handler.** The row is present on both platforms but
   taps are no-ops. Wire it to the SMS fallback flow when Phase 2
   lands.
9. **Signal / Paper / Light.** Tracked as separate future work. Palettes
   exist in the design bundle; port into `Theme.swift` / `Color.kt` as
   sibling objects (`Signal`, `Paper`, etc.) and introduce a
   `currentTheme` environment value before branching any view code.

---

## 9. Design-to-code mapping

Each prototype screen mapped to its shipped native counterpart.

| Design screen (in bundle)       | iOS file                                                                                                          | Android file                                                                                                                           |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| `Safewords App.html` (shell)    | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/App/SafewordsApp.swift` + `App/ContentView.swift`      | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/MainActivity.kt` + `ui/navigation/SafewordsNavigation.kt` |
| Onboarding (welcome/start/seed) | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift`                            | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/onboarding/OnboardingScreen.kt`          |
| Home / live safeword            | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/HomeView.swift`                                  | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/home/HomeScreen.kt`                      |
| Groups list + members           | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/GroupsView.swift`                                | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/groups/GroupsScreen.kt` + `ui/groups/GroupDetailScreen.kt` |
| Add Member / QR display         | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/QRDisplayView.swift`                             | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/qr/QRDisplayScreen.kt`                   |
| Verify (ask / listen / result)  | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/VerifyView.swift`                                | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/verify/VerifyScreen.kt`                  |
| Settings                        | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SettingsView.swift`                              | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/settings/SettingsScreen.kt`              |
| Plain Welcome                   | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift` → `PlainOnboardingView`    | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt` → `PlainOnboarding`  |
| Plain My Word                   | `Views/PlainModeViews.swift` → `PlainHomeView`                                                                    | `ui/plain/PlainMode.kt` → `PlainHome`                                                                                                  |
| Plain Check                     | `Views/PlainModeViews.swift` → `PlainVerifyView`                                                                  | `ui/plain/PlainMode.kt` → `PlainVerify` (+ `PlainAsk`, `PlainResult`)                                                                  |
| Plain Help                      | `Views/PlainModeViews.swift` → `PlainHelpView`                                                                    | `ui/plain/PlainMode.kt` → `PlainHelp`                                                                                                  |
| Shared: Ink theme tokens        | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Design/Theme.swift` (`Ink`, `A11Y`, `Fonts`)           | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/theme/Color.kt` (`Ink`, `A11y`, `DotPalette`) |
| Shared: CountdownRing primitive | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Components/CountdownRing.swift`                  | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/components/CountdownRing.kt`             |
| Shared: GroupDot / SectionLabel | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Components/GroupDot.swift` + `SectionLabel.swift` | `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/components/DesignComponents.kt`          |
| Shared: Custom tab bar          | `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Components/CustomTabBar.swift`                   | `CustomTabBar` inside `ui/navigation/SafewordsNavigation.kt`                                                                           |
| Widget preview (home-screen)    | `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift`                           | `/data/code/safewords-mobile/repos/safewords-android/widget/src/main/kotlin/com/thc/safewords/widget/SafewordsWidget.kt`               |

---

## TL;DR for reviewers

- **Shipped:** every screen in the handoff, on both platforms, in the Ink
  dark theme. Plain mode ships in full. Custom tab bars, countdown ring,
  QR generation, verify flow, and editorial copy all match spec.
- **Deferred:** Signal + Paper themes, all Light variants, ambient reveal
  style, Android hold-to-reveal, custom fonts (Fraunces / Atkinson
  Hyperlegible), widget theme variants, phone-in-phone widget preview
  card, Tweaks panel.
- **Biggest visual regression from spec:** missing custom fonts. The
  system-serif fallback reads "nice app" instead of "designed object."
  Fix is small (register `.ttf` / `.otf`) and high-value.
