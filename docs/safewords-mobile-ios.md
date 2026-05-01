# Safewords iOS - Page-by-Page Reference

Status: v1.3.1 / build 7 (commit `db3702f` and later)
Target: iOS 17.0+
Bundle IDs: `app.thc.safewords`, `app.thc.safewords.widget`

This is the iOS half of the unified `safewords-mobile.md`. Claude maintains the Android half at `docs/safewords-mobile-android.md`. They get merged into one canonical reference.

## Conventions

- Each screen section: **Purpose**, **UI surface**, **User actions**, **Technical implementation**, **Files of record**.
- File paths are absolute under `/data/code/safewords-mobile/repos/safewords-ios/` so they are greppable.
- `GroupStore` is the app-wide observable model that owns groups, selected group, demo mode, derivation helpers, and group persistence.
- App Group means `group.app.thc.safewords`, shared between the app and widget.

## Architecture overview

```text
SafewordsApp
  -> ContentView
       |
       |-- if requireBiometrics && !unlocked -> BiometricGateView
       |
       |-- if plainMode && onboarded && groups exist -> PlainRoot
       |      PlainHomeView / PlainVerifyView / PlainHelpView / PlainOnboardingView
       |
       `-- else -> custom state router + CustomTabBar
              OnboardingView
              HomeView
              GroupsView
              VerifyView -> ChallengeSheet
              SettingsView -> PrimitiveSettingsSheet
                           -> RecoveryBackupView
                           -> SafetyCardsView
                           -> DrillsView
              QRDisplayView
              QRScannerView
              RecoveryPhraseView

Widget extension
  -> SafewordsWidgetBundle
       -> SafewordsWidget
            -> SafewordsTimelineProvider
                 -> SafewordsWidgetSmallView / SafewordsWidgetMediumView
```

State sources:
- `GroupStore.groups: [Group]` - group metadata decoded from App Group `UserDefaults`.
- `GroupStore.selectedGroupID: UUID?` - selected group ID persisted in App Group `UserDefaults`.
- `GroupStore.demoMode: Bool` - v1.3.1 demo state flag persisted in App Group `UserDefaults`.
- `@AppStorage` booleans: `plainMode`, `onboarded`, `plainOnboarded`, `requireBiometrics`, `notifyOnRotation`, `previewNextWord`, `lockScreenGlance`, `hideWordUntilUnlock`.
- `@AppStorage` strings: `revealStyle` and per-group emergency override words through `GroupStore`.
- Per-group seeds live in Keychain generic-password records under service `app.thc.safewords.seeds` with App Group keychain access when available.

---

## Screens

### SafewordsApp

**Purpose**: The app entry point. Creates the shared `GroupStore` and injects it into SwiftUI.

**UI surface**: None directly. It mounts `ContentView` in a `WindowGroup` and forces the app to dark appearance.

**User actions**: N/A.

**Technical implementation**:
- `@main` app struct owns `@State private var groupStore = GroupStore()`.
- `ContentView` receives the store through `.environment(groupStore)`.
- The store constructor loads App Group `UserDefaults`, restores demo mode, and chooses the selected group.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/App/SafewordsApp.swift:1-14`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:3-48`

---

### ContentView

**Purpose**: The iOS root router. Decides whether the user sees the biometric gate, Plain Mode, or the Advanced tabbed surface.

**UI surface**:
- Optional full-screen biometric unlock page.
- Plain Mode root when `plainMode == true`, the user is onboarded, and at least one group exists.
- Advanced view with a custom bottom tab bar when not in Plain Mode.
- A floating demo banner when `GroupStore.demoMode == true`.

**User actions**:
- Biometric unlock -> sets `biometricUnlocked = true`.
- Plain Home gear -> sets `showingPlainSettings = true` and routes to Settings.
- Demo banner "Set up real group" -> calls `groupStore.exitDemoMode()`, resets `onboarded = false`, and routes to Onboarding.
- Tab bar taps -> mutate the `AppScreen` binding.

**Technical implementation**:
- Navigation is state-driven, not `NavigationStack` based: `@State private var screen: AppScreen`.
- First-run logic auto-renders `OnboardingView` when `!onboarded && groupStore.groups.isEmpty`.
- Tab bar is hidden for modal-style screens: onboarding, QR display, QR scanner, recovery phrase, recovery backup, safety cards, and drills.
- App-level biometrics use `BiometricService.authenticate(reason:)`, which requires enrolled biometrics rather than passcode fallback.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/App/ContentView.swift:3-105`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/App/ContentView.swift:109-190`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Components/CustomTabBar.swift:3-75`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/BiometricService.swift:1-37`

---

### Onboarding - Welcome Panel

**Purpose**: Introduce the product and explain why rotating safewords matter before asking the user to create or join anything.

**UI surface**:
- Progress bar step 1 of 3.
- Eyebrow `Safewords \u00B7 01`.
- Display copy: `One word between trust and deception.`
- Explanation copy: `AI can clone any voice in 3 seconds... no server, no account, no data collected.`
- Sample rotating words with one accent-highlighted example.
- Footer CTA: `Get started`.

**User actions**:
- `Get started` -> `flow = .start`.

**Technical implementation**:
- `OnboardingView.Flow` drives panel selection: `.welcome`, `.start`, `.create`.
- The primary CTA label comes from `primaryLabel`.
- The panel is pure local SwiftUI state; no persistence happens yet.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:3-49`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:97-129`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:174-214`

---

### Onboarding - Start Panel

**Purpose**: Present all first-run paths without forcing real group creation.

**UI surface**:
- Eyebrow `Start \u00B7 02`.
- Title `Start a group, or join someone else's.`
- Four option rows:
- `Create a new group` - primary accent row.
- `Join with a QR code`.
- `Join with a recovery phrase`.
- `Look around first`.
- Privacy footer: `Everything stays on your device. No accounts. No data collection.`

**User actions**:
- Create -> `flow = .create`.
- Join with QR -> `screen = .qrScanner`.
- Join with recovery phrase -> `screen = .recoveryPhrase`.
- Look around first -> `groupStore.enterDemoMode()`, `onboarded = true`, `screen = .home`.

**Technical implementation**:
- `onboardOption(...)` renders the reusable option row.
- Demo mode creates a non-Keychain demo group with ID `00000000-0000-0000-0000-00000000d013` and seed bytes `TIGER-DEMO-SAFEWORDS-V13-DEMO-!!`.
- `enterDemoMode()` only runs when no real groups exist, so it cannot override a user's live data.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:215-264`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:377-417`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:198-217`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:303-324`

---

### Onboarding - Create Form

**Purpose**: Name a new group and collect the creator's local display name before generating a real 256-bit seed.

**UI surface**:
- Eyebrow `Create \u00B7 02`.
- Title `Name your group.`
- Helper copy: `This creates a real 256-bit seed on this device...`
- Text fields: `Group name`, `Your name`.
- Footer CTA: `Generate recovery phrase`.

**User actions**:
- Type names -> local `@State` updates.
- Generate recovery phrase -> creates a random seed and switches the panel into seed backup mode.
- Back -> returns to the Start panel.

**Technical implementation**:
- `primaryDisabled` prevents continuing when either field is empty.
- `TOTPDerivation.generateSeed()` calls `SecRandomCopyBytes` for 32 bytes.
- The seed is held in `pendingSeed` until the user taps Create group. It is not saved before the backup step.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:105-162`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:266-318`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift:119-127`

---

### Onboarding - Recovery Seed Display

**Purpose**: Force a paper backup of the new group's seed before the group is committed.

**UI surface**:
- Eyebrow `Seed \u00B7 03`.
- Title is either `Back up this recovery phrase.` or `Back up this raw seed.`
- 24-word grid when BIP39 succeeds.
- 8-character chunked raw hex fallback when the BIP39 wordlist cannot load.
- Warning copy explaining that anyone with the phrase or seed can restore the group.
- Footer CTA: `Create group`.

**User actions**:
- Create group -> saves the seed to Keychain, writes group metadata, marks onboarding complete, routes Home.
- Back -> clears `pendingSeed`, `pendingRecoveryCode`, and fallback state.

**Technical implementation**:
- `RecoveryPhrase.encode(seed:)` is throwing. v1.3.1 replaced BIP39 `fatalError`/`precondition` paths with typed errors.
- If encoding fails, onboarding displays a raw seed fallback and error copy rather than crashing.
- `GroupStore.createGroup(... seed:)` saves the seed through `KeychainService.saveSeed`, exits demo mode first, persists metadata to App Group `UserDefaults`, and selects the group.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:148-172`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/OnboardingView.swift:332-375`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/RecoveryPhrase.swift:4-222`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:54-75`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/KeychainService.swift:5-96`

---

### RecoveryPhraseView

**Purpose**: Restore or join a group from a 24-word BIP39 recovery phrase or a 64-character hex seed.

**UI surface**:
- Back button.
- Eyebrow `Recovery`.
- Title `Restore a group`.
- Fields: `Group name`, `Your name`.
- `TextEditor` labeled `Recovery phrase or seed`.
- Helper text describing accepted formats.
- Error card when parsing or saving fails.
- CTA: `Join group`.

**User actions**:
- Back -> routes to Onboarding if no groups exist, otherwise Groups.
- Join group -> parses seed, writes Keychain + metadata, selects group, marks onboarding complete, routes Home.

**Technical implementation**:
- `RecoveryPhraseService.parseSeed(from:)` accepts either 64 hex characters or a 24-word BIP39 phrase.
- Parsing errors surface via `LocalizedError.errorDescription`.
- `GroupStore.joinGroup(...)` exits demo mode first and stores the restored seed with the new local group record.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/RecoveryPhraseView.swift:3-139`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/RecoveryPhraseService.swift:3-67`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:78-93`

---

### PlainRoot

**Purpose**: The Plain Mode mini-app. Plain Mode is the v1.3 default home surface.

**UI surface**:
- One of `PlainHomeView`, `PlainVerifyView`, `PlainHelpView`, or `PlainOnboardingView`.
- Bottom `A11yTabBar` with `Word`, optional `Check`, and `Help`.

**User actions**:
- Plain tab taps -> switch local `PlainScreen`.
- Plain Home gear -> invokes the `onSettings` closure supplied by `ContentView`.

**Technical implementation**:
- Uses local `@State private var screen: PlainScreen = .home`.
- `A11yTabBar` only includes the `Check` tab when `groupStore.hasAnyVerifyPrimitive()` is true.
- Plain Mode is controlled by `@AppStorage("plainMode")` in `ContentView` and `SettingsView`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:8-108`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:608-626`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/App/ContentView.swift:19-30`

---

### PlainHomeView

**Purpose**: High-visibility word screen for the selected group, optimized for stressful calls and low-vision users.

**UI surface**:
- Header with group avatar, `Your circle`, group name, and gear button.
- Hero card with `YOUR WORD NOW` or `YOUR CODE NOW`.
- Current phrase rendered one word per line, or numeric code as a single split-free value.
- Countdown pill: `New word in {human time}`.
- Guidance: `Ask: "What is our word?" Do not say it first.`
- Optional `Challenge someone` button when challenge/answer is enabled.
- Optional static override note when static override is enabled.

**User actions**:
- Gear -> routes to Advanced Settings while keeping Plain Mode enabled.
- Challenge someone -> presents `ChallengeSheet`.
- Plain bottom tabs -> switch to Verify or Help.

**Technical implementation**:
- Uses `TimelineView(.periodic(from: .now, by: 1.0))` for a 1 Hz UI tick.
- Calls `groupStore.safeword(for:at:)`, which selects numeric vs adjective/noun/number output based on group primitives.
- Challenge sheet receives the selected group's seed from `groupStore.seed(for:)`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:110-252`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:219-252`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/ChallengeSheet.swift:3-192`

---

### PlainVerifyView

**Purpose**: Plain Mode yes/no verification flow for people who should not have to type or navigate Advanced verification.

**UI surface**:
- Step label `STEP 1 OF 2`.
- Prompt: `Ask them: "What is our word?"`
- Warning that the user must not read the word first.
- Large buttons: `Yes, it matched` and `No, wrong word`.
- Result panels: `Safe to talk.` or `Hang up now.`
- Optional secondary action: `Call them back on a trusted number`.

**User actions**:
- Yes -> shows match result.
- No -> shows mismatch guidance.
- Call trusted number -> opens SMS/call fallback URL with a canned message.
- All done / I hung up -> resets to ask phase.

**Technical implementation**:
- Local `Phase` enum: `ask`, `match`, `nomatch`.
- No cryptographic check happens here. Plain Verify is human confirmation against the word visible on Plain Home.
- Trusted-number fallback uses `SmsInviteService.fallbackURL(...)`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:254-402`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/SmsInviteService.swift:1-52`

---

### PlainHelpView

**Purpose**: Plain Mode help screen for call-safety guidance, device settings access, trusted-contact fallback, and emergency escalation.

**UI surface**:
- Header `HELP` and title `How can we help?`
- Help cards:
- `I got a strange call`
- `Who is in my circle`
- `What's a "word"?`
- `Change text size`
- `Call my family for help`
- Emergency card: `If you feel unsafe, call 911.`
- `Open Advanced View` button.

**User actions**:
- Change text size -> opens iOS Settings.
- Call my family for help -> opens fallback SMS/call URL.
- Emergency -> opens `tel://911`.
- Open Advanced View -> sets `plainMode = false`.

**Technical implementation**:
- Uses `@AppStorage("plainMode")` directly to exit Plain Mode.
- Uses `@Environment(\.openURL)` for Settings, emergency phone URL, and fallback URL dispatch.
- Static help items are local to the view; no network or remote content.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:404-520`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/SmsInviteService.swift:1-52`

---

### PlainOnboardingView

**Purpose**: Small Plain Mode explainer flow for users entering the high-visibility surface.

**UI surface**:
- Two panels: `WELCOME` and `HOW IT WORKS`.
- Large copy: `One word keeps you safe.` and `Your family picks a secret word.`
- Example word card: `Golden Robin`.
- CTA button: `Show me how`, then `Get started`.
- Back button on second panel.

**User actions**:
- CTA on first panel -> `step += 1`.
- CTA on second panel -> `plainOnboarded = true`.
- Back -> `step -= 1`.

**Technical implementation**:
- Local `step` state indexes a static `panels` array.
- Completion persists `@AppStorage("plainOnboarded")`.
- The current `PlainRoot` can route to this screen through the `PlainScreen.onboarding` enum, but the tab bar does not expose it as a normal tab.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:522-606`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:608-626`

---

### HomeView (Advanced)

**Purpose**: Advanced tabbed home. Shows the current word/code with a countdown ring and group selector.

**UI surface**:
- Group selector pill at top left.
- Bell button at top right, currently routes Settings.
- `CountdownRing` hero with `LIVE \u00B7 {GROUP}` label.
- Current word split into lines, with optional blur for hold-to-reveal.
- Sequence label `SEQ \u00B7 ####`.
- Countdown text `HH:MM:SS`.
- Secondary text: `rotates in ...`, optionally `next: {word}` when preview is enabled.
- Empty state: `No groups yet` and `Create a group`.

**User actions**:
- Group selector pill -> routes Groups.
- Bell -> routes Settings.
- Hold on the word when hold-to-reveal is enabled -> reveals blurred word while held.
- Create a group in empty state -> routes Onboarding.

**Technical implementation**:
- Uses `TimelineView(.periodic(from: .now, by: 1.0))` for countdown progress.
- `CountdownRing` handles animated ring rendering.
- `@AppStorage("revealStyle")` controls hold-to-reveal behavior.
- `@AppStorage("previewNextWord")` controls whether the next derived value is shown.
- Display value is fetched through `GroupStore.safeword(for:at:)`, so numeric format works here too.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/HomeView.swift:3-210`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Components/CountdownRing.swift:6-91`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:219-252`

---

### GroupsView

**Purpose**: Advanced tab for selecting the active group, reviewing members, and starting invite/setup flows.

**UI surface**:
- Header `Groups` / `Your circles`.
- Plus button to add/create a group.
- Group cards with avatar dot, name, `ACTIVE` badge, member count, rotation interval, current safeword, and sequence.
- Selected group's member list.
- Invite CTA: `Invite someone to {group}`.

**User actions**:
- Group card tap -> sets `groupStore.selectedGroupID` and routes Home.
- Plus -> routes Onboarding.
- Invite -> routes QR Display.

**Technical implementation**:
- Reads `groupStore.groups` and `groupStore.selectedGroup`.
- Uses `GroupDot` plus deterministic `DotPalette.forIndex`.
- Shows current safeword via `groupStore.currentSafeword(for:)`.
- This screen does not include a separate group detail route on iOS; group metadata editing lives in Settings.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/GroupsView.swift:3-209`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Components/GroupDot.swift:4-49`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:95-160`

---

### VerifyView (Advanced)

**Purpose**: Advanced verification surface. It conditionally appears useful only when the active group has challenge/answer or static override enabled.

**UI surface**:
- Header `Verify` and title `Are they who they say they are?`
- No-group state with `Start setup`.
- Verify-not-needed empty state: `Verify isn't needed for {group} right now...`
- Optional `Challenge someone` CTA when challenge/answer is enabled.
- Free-text field `type what they said`.
- `Check` button and microphone/listening button.
- Safety tips: hang up, call known number, try override word.
- Listening panel with mic pulse and `They matched` / `They did not`.
- Result cards: `Verified.` or `Don't trust.`

**User actions**:
- Start setup -> routes Onboarding.
- Open Primitives -> routes Settings.
- Challenge someone -> presents `ChallengeSheet`.
- Type an answer + Check -> compares against current word, legacy emergency override, and static override.
- Mic button -> switches to listening phase.
- They matched / They did not -> result phase.
- Result back button -> resets field and phase.

**Technical implementation**:
- Local `Phase`: `ready`, `listening`, `match`, `mismatch`.
- `groupStore.verifyNeeded(for:)` returns `group.primitives.needsVerifySurface`.
- Static override comparison computes `Primitives.staticOverride(seed:)` on demand. It is not stored.
- The legacy emergency override remains supported through `GroupStore.emergencyOverrideWord`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/VerifyView.swift:3-348`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/ChallengeSheet.swift:3-192`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:166-181`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/Primitives.swift:28-84`

---

### ChallengeSheet

**Purpose**: Deterministic challenge/answer flow for groups with the challenge/answer primitive enabled.

**UI surface**:
- Sheet handle.
- Eyebrow `Challenge someone`.
- Group name.
- Row stepper: `Row {n} of {rowCount}` with minus/plus controls.
- Prompt card with `Ask` and `Expect` phrases.
- Buttons: `They said {expect phrase}` and `Does not match`.
- Lock row: `Show full table` / `Hide full table`.
- Biometric failure message if table reveal fails.
- Full table after unlock: row number, ask phrase, expect phrase.

**User actions**:
- Minus/plus -> changes `rowIndex` within bounds.
- They said expected phrase -> dismisses sheet.
- Does not match -> dismisses sheet.
- Show full table -> runs device-owner authentication, then toggles table visibility.

**Technical implementation**:
- `row` derives with `Primitives.challengeAnswerRow(seed:tableVersion:rowIndex:)`.
- `rowCount` clamps at at least 1 from group primitive config.
- Full-table reveal uses `BiometricService.authenticateDeviceOwner`, allowing passcode fallback.
- The table is recomputed locally from the seed; no challenge rows are persisted.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/ChallengeSheet.swift:3-192`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/Primitives.swift:56-67`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/BiometricService.swift:27-36`

---

### SettingsView

**Purpose**: Advanced preferences, per-group primitive toggles, security actions, practice entry points, and destructive data actions.

**UI surface**:
- Header `Settings` / `Preferences`.
- `View`: `Use Plain home by default`, Advanced view status.
- `Rotation \u00B7 {group}`: interval picker, notify toggle, next-word preview toggle.
- `Accessibility`: `Hold to reveal word`.
- `Group`: `Primitives`, `Safety cards`.
- `Widget & Lock Screen`: widget instructions, lock-screen glance toggle, hide-until-unlock toggle.
- `Security`: require biometrics, emergency override word, rotate seed placeholder, seed phrase backup.
- `Practice`: scam drill and drill history.
- `Danger zone`: leave group and reset device.
- Footer: `Safewords v1.3.1 \u00B7 Offline-first`.

**User actions**:
- Plain home toggle -> persists `plainMode`.
- Interval picker -> `groupStore.updateGroupInterval`, also updates rotating primitive interval.
- Notify/preview/hold/lock-screen/hide toggles -> persist through `@AppStorage`.
- Require biometrics -> verifies biometric availability before enabling.
- Primitives -> presents `PrimitiveSettingsSheet`.
- Safety cards -> routes `SafetyCardsView`.
- Emergency override -> presents text-entry sheet with Clear and Save.
- Back up seed phrase -> routes `RecoveryBackupView`.
- Run drill / drill history -> routes `DrillsView`.
- Leave group -> destructive confirmation, then `GroupStore.deleteGroup`.
- Reset device -> wipes groups, seeds, drills, and onboarding state.

**Technical implementation**:
- Uses regular `@AppStorage` for app prefs and App Group `@AppStorage(..., store:)` for widget-visible prefs.
- `PrimitiveSettingsSheet` toggles numeric format, static override, and challenge/answer through `GroupStore`.
- Biometric toggle checks `BiometricService.canEvaluate()` before setting `requireBiometrics`.
- Reset calls `GroupStore.resetAllData()` and `DrillService.clear()`, then restores Plain Mode default.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SettingsView.swift:5-388`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SettingsView.swift:390-491`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:95-217`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/BiometricService.swift:1-37`

---

### PrimitiveSettingsSheet

**Purpose**: Per-group v1.3 primitive configuration.

**UI surface**:
- Sheet handle.
- Eyebrow `Group primitives`.
- Group name.
- Toggles:
- `Numeric word format` - 6-digit code instead of words.
- `Static override` - fixed emergency phrase derived from seed.
- `Challenge / answer` - deterministic challenge table and match sheet.
- Note that derived secrets are recomputed from the seed and not stored in metadata.
- `Done` button.

**User actions**:
- Toggle numeric -> `groupStore.setWordFormat(... .numeric)` or `.adjectiveNounNumber`.
- Toggle static override -> `groupStore.setStaticOverrideEnabled`.
- Toggle challenge/answer -> `groupStore.setChallengeAnswerEnabled`.
- Done -> dismisses the sheet.

**Technical implementation**:
- Reads the current selected group from `GroupStore`.
- Uses bindings that re-read `groupStore.selectedGroup` so UI stays synchronized after mutation.
- `GroupStore.updatePrimitives` normalizes table version, row count, derivation version, and interval seconds before saving.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SettingsView.swift:390-491`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:110-134`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Data/GroupConfig.swift:98-169`

---

### RecoveryBackupView

**Purpose**: Biometric/passcode-gated display of the active group's recovery phrase from Settings.

**UI surface**:
- Back button.
- Eyebrow `Back up seed phrase`.
- Title `For {group}` or `No active group`.
- Warning: `Anyone with these words can restore this group...`
- Locked state button: `Unlock to reveal`.
- 4-column indexed phrase grid after unlock.
- `Copy to clipboard` / `Copied` button.
- Footer: `Backup format: BIP39 English, 24 words.`
- Error message cards for missing group, missing seed, failed auth, or phrase generation failure.

**User actions**:
- Back -> routes Settings.
- Unlock to reveal -> device-owner authentication, then phrase derivation.
- Copy -> writes phrase to `UIPasteboard.general.string`.

**Technical implementation**:
- Uses `LAContext.evaluatePolicy(.deviceOwnerAuthentication)` directly, so passcode fallback is allowed.
- Pulls seed from `groupStore.seed(for:)`.
- Calls `RecoveryPhrase.encode(seed:)`; failures are displayed rather than crashing.
- Phrase is recomputed on demand and not persisted.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/RecoveryBackupView.swift:5-246`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/RecoveryPhrase.swift:174-213`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:253-261`

---

### SafetyCardsView

**Purpose**: Browser and print/share surface for native printable safety cards.

**UI surface**:
- Back button.
- Eyebrow `Safety cards`.
- Active group name.
- Auth error card when unlock fails.
- Card rows:
- `Protocol card`
- `Static override` when static override is enabled.
- `Challenge wallet excerpt` and `Challenge full protocol` when challenge/answer is enabled.
- `Recovery phrase`
- `Group invite`
- Preview area for the selected card.
- `Print` and `Share` buttons.

**User actions**:
- Tap protocol card -> selects immediately.
- Tap sensitive card -> prompts device-owner authentication, then selects.
- Print -> renders the selected SwiftUI card to `UIImage` and opens `UIPrintInteractionController`.
- Share -> renders to `UIImage` and presents `UIActivityViewController`.
- Back -> routes Settings.

**Technical implementation**:
- `SafetyCardKind.available(for:)` computes visible cards from group primitive config.
- All high-sensitivity cards require `BiometricService.authenticateDeviceOwner`.
- `CardRenderer.render` uses SwiftUI `ImageRenderer`; `CardRenderer.printImage` wraps UIKit printing.
- Share sheet is a `UIViewControllerRepresentable`.
- Recovery card falls back to raw seed hex if BIP39 encoding fails.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SafetyCardsView.swift:4-266`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Print/CardRenderer.swift:1-38`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Cards/ProtocolCardView.swift:1-127`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Cards/OverrideCardView.swift:1-59`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Cards/ChallengeAnswerCardView.swift:1-94`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Cards/RecoveryPhraseCardView.swift:1-50`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/Cards/GroupInviteCardView.swift:1-38`

---

### QRDisplayView

**Purpose**: Show a group invite QR for in-person joining and offer SMS invite fallback text.

**UI surface**:
- Back button.
- Eyebrow `Invite \u00B7 {group}`.
- Title `Share in person`.
- Large QR code card with center mark.
- Instructions: `Have them open Safewords, tap Join with QR, and scan this.`
- Warning: `Only share this in person...`
- Security ticker: `256-BIT \u00B7 ROTATING \u00B7 OFFLINE \u00B7 {seconds}S`.
- Alternate action: `Invite via SMS instead`.

**User actions**:
- Back -> routes Groups.
- Invite via SMS -> opens `MFMessageComposeViewController` when available or a `sms:` fallback URL otherwise.
- Timer expiration -> automatically routes back to Groups after 60 seconds if still on QR Display.

**Technical implementation**:
- QR code is generated by `QRCodeService.generateQRCode(for:seed:size:)`.
- Payload includes group metadata and seed. It is seed-equivalent and should only be shared in person.
- `SmsInviteService.inviteText(group:seed:)` includes either a recovery display code for the seed or a QR-scan instruction.
- Message composer bridge is `MessageComposeView`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/QRDisplayView.swift:4-166`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/QRCodeService.swift:6-129`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/SmsInviteService.swift:1-52`

---

### QRScannerView

**Purpose**: Camera-based group invite scanner.

**UI surface**:
- `NavigationStack` title `Join Group`.
- Toolbar `Cancel`.
- Permission state: camera icon, `Scan a Safewords QR`, `Enable camera`.
- Denied state: `Camera is off`, `Use recovery code`.
- Scanner state: 280 px camera preview, accent reticle, torch toggle, error text.
- Group found state: group name, rotation interval, name field, `Join Group`.

**User actions**:
- Enable camera -> requests AVFoundation video permission.
- Use recovery code -> routes RecoveryPhraseView.
- Cancel -> caller-provided cancel closure, usually Onboarding or Groups.
- Torch toggle -> flips device torch through `QRCameraUIView`.
- Valid QR detected -> shows name prompt.
- Join Group -> saves seed and metadata, selects group, routes Home.

**Technical implementation**:
- Uses AVFoundation directly: `AVCaptureSession`, `AVCaptureDeviceInput`, `AVCaptureMetadataOutput`, and `.qr` metadata type.
- `QRCameraPreview` bridges SwiftUI to `QRCameraUIView`.
- `QRCodeService.parseQRCode` decodes the JSON payload to a parsed group.
- Invalid scans set `scanError` and refresh the preview ID after 0.8s.
- Successful joins call `GroupStore.joinGroup(...)`, which exits demo mode first.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/QRScannerView.swift:5-263`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/QRScannerView.swift:265-366`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/QRCodeService.swift:75-129`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:78-93`

---

### DrillsView

**Purpose**: Practice flow for scam-call scenarios.

**UI surface**:
- Back button.
- Eyebrow `Practice`.
- Title `Scam drills`.
- Idle card: `Practice before a real call.`, explanation, `Run drill now`.
- Running card: `Drill in progress`, `Someone is calling.`, scenario copy, and guidance to ask the word.
- Result buttons: `They knew it` and `They failed`.
- History list with recent `Passed` or `Needs practice` entries.

**User actions**:
- Back -> routes Settings.
- Run drill now -> sets `running = true`.
- They knew it / They failed -> records a `DrillSession`, refreshes history, exits running state.
- Cancel drill -> exits running state without recording.

**Technical implementation**:
- Local `sessions` state is seeded from `DrillService.sessions()`.
- `DrillService.record(group:scenario:success:)` stores sessions in `UserDefaults.standard` under a local key.
- No network or dynamic content generation.
- Reset device in Settings clears drill history through `DrillService.clear()`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/DrillsView.swift:3-179`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/DrillService.swift:3-69`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SettingsView.swift:381-388`

---

### Widget - Small

**Purpose**: Home-screen widget showing the active group's current word and compact countdown.

**UI surface**:
- Word/code in accent color, up to two lines.
- Clock icon.
- Compact countdown such as `1h 02m` or `4:09`.
- Hidden states: `Widget Off`, `Unlock to View`, or `No Group`.

**User actions**:
- System widget tap launches the containing app through WidgetKit default behavior.
- Widget visibility and hiding behavior are controlled in Settings, not inside the widget.

**Technical implementation**:
- `SafewordsTimelineProvider` loads groups from App Group `UserDefaults` key `safewords.groups`.
- It loads selected group ID from `safewords.selectedGroupID`.
- It reads the seed from Keychain using widget-local `WidgetKeychain`.
- Small widget renders `SafewordsWidgetSmallView`.
- v1.3.1 build 7 adds a widget target pre-build resource copy for `adjectives.json`, `nouns.json`, and `safety-card-copy.json`.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:45-180`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:194-225`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:312-390`
- `/data/code/safewords-mobile/repos/safewords-ios/project.yml:62-90`

---

### Widget - Medium

**Purpose**: Wider widget variant with group name, current word, countdown ring, and countdown text.

**UI surface**:
- Group name in small uppercase type.
- Word/code in accent title type.
- Countdown ring showing elapsed interval progress.
- Compact countdown under the ring.
- Hidden states match the small widget.

**User actions**:
- Same as small widget: widget tap opens the app, behavior is configured in Settings.

**Technical implementation**:
- The same timeline provider feeds both widget sizes.
- `SafewordsWidgetEntryView` dispatches on `widgetFamily`.
- Medium widget renders `SafewordsWidgetMediumView`.
- Widget derivation is duplicated in `WidgetTOTP`; it does not import the main app module.

**Files of record**:
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:227-278`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:281-306`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidgetBundle.swift:1-9`

---

## Cross-cutting

### Theme + design tokens

The main app theme is in `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Design/Theme.swift`.

- `Ink` is the default editorial dark theme: near-black background, warm foreground, ember accent.
- `A11Y` is the Plain Mode high-visibility palette: navy background, white foreground, amber accent, green/red result colors.
- `Fonts.display` prefers `Fraunces` if registered, otherwise system serif.
- `A11yFonts` prefers `AtkinsonHyperlegible-Regular` if registered, otherwise system.
- See `docs/design-system.md` for shared platform token intent.

Files:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Design/Theme.swift:1-72`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift:12-23`

### Storage

| What | Where | Class |
|---|---|---|
| Group metadata JSON | App Group `UserDefaults`, key `safewords.groups` | `GroupStore` |
| Selected group ID | App Group `UserDefaults`, key `safewords.selectedGroupID` | `GroupStore` |
| Per-group seed | iOS Keychain generic password, service `app.thc.safewords.seeds` | `KeychainService` |
| Widget-visible prefs | App Group `UserDefaults`, keys `lockScreenGlance`, `hideWordUntilUnlock` | `SettingsView`, widget provider |
| Plain/app prefs | `@AppStorage` in app defaults | `ContentView`, `SettingsView`, Plain views |
| Legacy emergency override | App Group `UserDefaults`, key prefix `safewords.emergencyOverride.` | `GroupStore` |
| Demo mode flag | App Group `UserDefaults`, key `safewords.demoMode` | `GroupStore` |
| Demo seed | Hardcoded `Data` in `GroupStore`, never written to Keychain | `GroupStore` |
| Drill history | Standard `UserDefaults` through `DrillService` | `DrillService` |

Notes:
- Group metadata excludes demo group records before writing.
- `resetAllData()` deletes seeds, metadata, selected group, demo flag, and emergency override keys.
- Widget Keychain access uses the same App Identifier Prefix + App Group access group as the app.

Files:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:36-48`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/GroupStore.swift:263-301`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Services/KeychainService.swift:5-96`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:148-180`

### Print pipeline

The print pipeline is native and offline:

- `SafetyCardsView` lists available templates from group primitive config.
- Sensitive templates require device-owner authentication before rendering.
- Each SwiftUI card view is rendered to a `UIImage` with `ImageRenderer`.
- Printing uses `UIPrintInteractionController`.
- Sharing uses `UIActivityViewController`.
- Card copy and resource bundling come from `/shared/safety-card-copy.json`, copied into the app and widget bundles by `project.yml`.

Files:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/SafetyCardsView.swift:4-266`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Print/CardRenderer.swift:1-38`
- `/data/code/safewords-mobile/repos/safewords-ios/project.yml:31-58`
- `/data/code/safewords-mobile/shared/safety-card-copy.json`

### Cross-platform contract

Every primitive must match Android for the same seed, timestamp, row, and shared JSON contract:

- `/data/code/safewords-mobile/shared/primitive-vectors.json`
- `/data/code/safewords-mobile/shared/migration-vectors.json`
- `/data/code/safewords-mobile/shared/recovery-vectors.json`
- `/data/code/safewords-mobile/shared/test-vectors.json`
- `/data/code/safewords-mobile/shared/wordlists/bip39-english.txt`

iOS tests:
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsTests/PrimitiveVectorsTests.swift:1-154`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsTests/MigrationVectorsTests.swift:1-57`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsTests/RecoveryPhraseTests.swift:1-179`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsTests/TOTPDerivationTests.swift:1-226`
- `/data/code/safewords-mobile/repos/safewords-ios/SafewordsTests/TestVectors.swift:1-166`

Algorithm files:
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift:1-145`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/Primitives.swift:1-99`
- `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Crypto/RecoveryPhrase.swift:1-222`

### Build + test surface

Local Linux VM:
- No local Xcode or Swift compiler is available here.
- Markdown and YAML can be validated locally.

GitHub Actions:
- Workflow: `.github/workflows/ios-release.yml`
- `workflow_dispatch` input `lane=beta` builds and uploads to TestFlight.
- `lane=validate` only checks App Store Connect capability access and does not build/upload.
- Build 7 successful beta run: `25200007351`.

Fastlane:
- `bundle exec fastlane ios validate` - App Store Connect validation.
- `bundle exec fastlane ios build` - signed App Store IPA.
- `bundle exec fastlane ios beta` - build plus TestFlight upload.
- `bundle exec fastlane ios release` - build plus App Store release lane.

Resource assertions:
- Fastlane checks the IPA for exact app/widget resource paths after export:
- `Payload/Safewords.app/bip39-english.txt`
- `Payload/Safewords.app/safety-card-copy.json`
- `Payload/Safewords.app/PlugIns/SafewordsWidget.appex/adjectives.json`
- `Payload/Safewords.app/PlugIns/SafewordsWidget.appex/nouns.json`
- `Payload/Safewords.app/PlugIns/SafewordsWidget.appex/safety-card-copy.json`

Files:
- `/data/code/safewords-mobile/.github/workflows/ios-release.yml`
- `/data/code/safewords-mobile/repos/safewords-ios/fastlane/Fastfile:1-320`
- `/data/code/safewords-mobile/repos/safewords-ios/project.yml:1-128`

## Appendix: deferred or platform-different features

Implemented on Android but not currently user-reachable on iOS:
- Single-use word generator. There is no `AppScreen.generator` and no iOS generator view in v1.3.1.
- Separate group detail screen. iOS combines group selection/member overview in `GroupsView` and per-group configuration in `SettingsView`.
- Dedicated override reveal route. iOS reveals static override through Verify matching and safety-card rendering rather than a standalone route.

Deferred on both platforms:
- Pair mode with counterparty-keyed challenge/answer.
- Agent Circle.
- Expiring invites. Current QR payloads are seed-equivalent.
- Push notifications.
- Custom font asset registration for Fraunces / Atkinson Hyperlegible.
