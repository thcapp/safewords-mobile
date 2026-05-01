# Safewords Mobile — Page-by-Page Reference

Status: **v1.3.1** on both platforms.
- **Android**: versionCode 15 / versionName 1.3.1 (commit `001527a` and later)
- **iOS**: marketing 1.3.1 / build 7 (commit `db3702f` and later)

This is the canonical reference for every user-reachable screen on both platforms. iOS sections were authored by codex; Android sections by claude. They've been merged so each screen concept shows both implementations side by side.

## Conventions

- Each screen entry has **iOS** and **Android** sub-sections. If a concept exists on only one platform (e.g., `GeneratorScreen` is Android-only), the missing platform is called out.
- Each sub-section follows the same template: **Purpose**, **UI surface**, **User actions**, **Technical implementation**, **Files of record**.
- File paths are absolute under `/data/code/safewords-mobile/` so they're greppable.
- Abbreviations: **ESP** = `EncryptedSharedPreferences` (Android), **App Group** = `group.app.thc.safewords` (iOS, shared between app and widget).
- "GroupStore" (iOS) and "GroupRepository" (Android) are the singletons that own groups + active group + global prefs + demo mode.

## Architecture overview

### iOS

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

### Android

```text
MainActivity (FragmentActivity)
  └─ SafewordsNavigation()                 ← single navigation root
       │
       ├─ if (plainMode) → PlainRoot       ← legacy "high visibility" toggle wins
       │
       ├─ if (!advancedView) → PlainRoot   ← v1.3 default for everyone
       │       PlainHome / PlainVerify / PlainHelp / PlainOnboarding
       │
       └─ else → Scaffold + NavHost
               Onboarding → RecoveryPhrase
               Home → Groups → GroupDetail
               Verify → ChallengeSheet
               Settings → RecoveryBackup
                        → Generator
                        → Drills
                        → SafetyCards
                        → OverrideReveal
               QRScanner / QRDisplay
```

### State sources (both platforms)

| Concept | iOS | Android |
|---|---|---|
| Groups list | `GroupStore.groups: [Group]` | `GroupRepository.groups: StateFlow<List<Group>>` |
| Active group ID | `GroupStore.selectedGroupID: UUID?` | `GroupRepository.activeGroupId: StateFlow<String?>` |
| Demo mode | `GroupStore.demoMode: Bool` | `GroupRepository.demoMode: StateFlow<Boolean>` |
| Plain mode toggle (legacy) | `@AppStorage("plainMode")` | ESP key `plain_mode` |
| Advanced view toggle (v1.3) | (combined with plainMode in iOS) | ESP key `advanced_view_enabled` |
| Per-group seed | iOS Keychain, service `app.thc.safewords.seeds`, App Group access | ESP via `SecureStorageService`, separate prefs file |

---

## Screens

### Application root

#### iOS — SafewordsApp + ContentView

**Purpose**: The app entry point creates the shared `GroupStore`. `ContentView` is the iOS root router — decides between biometric gate, Plain Mode, or Advanced tabbed surface.

**UI surface**:
- `SafewordsApp` mounts `ContentView` in a `WindowGroup` and forces dark appearance.
- `ContentView`: optional full-screen biometric unlock; Plain Mode root when `plainMode == true`, the user is onboarded, and at least one group exists; Advanced view with a custom bottom tab bar otherwise; a floating demo banner when `demoMode == true`.

**User actions**:
- Biometric unlock → sets `biometricUnlocked = true`.
- Plain Home gear → routes to Settings while keeping Plain Mode enabled.
- Demo banner "Set up real group" → calls `groupStore.exitDemoMode()`, resets `onboarded = false`, routes to Onboarding.
- Tab bar taps → mutate the `AppScreen` binding.

**Technical implementation**:
- `@main` app struct owns `@State private var groupStore = GroupStore()`.
- Navigation is state-driven (`@State private var screen: AppScreen`), not `NavigationStack`.
- First-run logic auto-renders `OnboardingView` when `!onboarded && groupStore.groups.isEmpty`.
- Tab bar is hidden for modal-style screens (onboarding, QR display, QR scanner, recovery phrase, recovery backup, safety cards, drills).
- App-level biometrics require enrolled biometrics (no passcode fallback) via `BiometricService.authenticate(reason:)`.

**Files of record**:
- `repos/safewords-ios/Safewords/App/SafewordsApp.swift:1-14`
- `repos/safewords-ios/Safewords/App/ContentView.swift:3-190`
- `repos/safewords-ios/Safewords/Views/Components/CustomTabBar.swift:3-75`
- `repos/safewords-ios/Safewords/Services/BiometricService.swift:1-37`

#### Android — MainActivity + SafewordsNavigation

**Purpose**: `MainActivity` is the single Android Activity. It hosts the Compose tree. `SafewordsNavigation()` is the root composable that decides between Plain (default) and Advanced (tabbed).

**UI surface**:
- `MainActivity`: none (sets `setContent { SafewordsApp() }`).
- `SafewordsNavigation`: in Plain mode just delegates to `PlainRoot`; in Advanced mode renders a `Scaffold` with a custom rounded tab bar (Home / Groups / Verify / Settings).

**User actions**:
- Tab tap → navigates with `popUpTo(start) { saveState = true }; launchSingleTop = true; restoreState = true` (preserves per-tab back stack).
- "Standard view" pill in Plain home → flips `advancedView = true`, calls `GroupRepository.setAdvancedView(true)`.
- Demo banner tap (Plain) → `GroupRepository.exitDemoMode()` + flip to Advanced view.

**Technical implementation**:
- `MainActivity` extends `FragmentActivity` (required for `BiometricPrompt`).
- `SafewordsApp.kt` (Application subclass) registers a runtime broadcast receiver for `ACTION_USER_PRESENT` and `ACTION_SCREEN_ON` to refresh the home-screen widget.
- Local state in nav root: `var advancedView` and `var plainMode`, seeded from `GroupRepository`.
- Tab bar visibility driven by `tabs.any { it.screen.route == route }` — sub-routes (group detail, challenge sheet, etc.) hide the bar.
- Routes: `home`, `groups`, `verify`, `settings`, `onboarding`, `recovery_phrase`, `drills`, `generator`, `recovery_backup`, `safety_cards`, `group/{groupId}`, `qr_display/{groupId}`, `qr_scanner`, `challenge/{groupId}`, `override_reveal/{groupId}`.
- `start = if (groups.isEmpty()) Onboarding else Home`.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/MainActivity.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/SafewordsApp.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/navigation/SafewordsNavigation.kt`

---

### Onboarding flow

Both platforms walk the same conceptual flow: Welcome → Start (4 paths) → Create form → Recovery seed display, with separate routes for Join via QR and Restore via recovery phrase.

#### iOS — OnboardingView

**Purpose**: First-run flow. Four entry points: create new group, join with QR, restore from recovery phrase, or try the demo (v1.3.1).

**UI surface (per panel)**:
- **Welcome**: progress bar 1 of 3, eyebrow `Safewords · 01`, display copy "One word between trust and deception", explanation about AI voice cloning, sample words, CTA `Get started`.
- **Start**: eyebrow `Start · 02`, title "Start a group, or join someone else's", four options (Create / Join with QR / Join with recovery phrase / Look around first), privacy footer.
- **Create form**: eyebrow `Create · 02`, title "Name your group", helper copy about 256-bit seed, fields (Group name + Your name), CTA `Generate recovery phrase`.
- **Recovery seed display**: eyebrow `Seed · 03`, title "Back up this recovery phrase" or "Back up this raw seed", 24-word grid OR 8-char chunked hex fallback, warning copy, CTA `Create group`.

**User actions**:
- `Get started` (Welcome) → `flow = .start`.
- Create (Start) → `flow = .create`.
- Join with QR → `screen = .qrScanner`.
- Join with recovery phrase → `screen = .recoveryPhrase`.
- Look around first → `groupStore.enterDemoMode()`, `onboarded = true`, `screen = .home`.
- Generate recovery phrase (Create form) → `TOTPDerivation.generateSeed()` (calls `SecRandomCopyBytes`); seed held in `pendingSeed` until user taps Create group.
- Create group (Recovery seed display) → saves seed to Keychain via `KeychainService.saveSeed`, writes group metadata to App Group `UserDefaults`, marks onboarding complete, routes Home.

**Technical implementation**:
- `OnboardingView.Flow` drives panel selection (`.welcome`, `.start`, `.create`).
- `primaryDisabled` prevents continuing when fields are empty.
- `RecoveryPhrase.encode(seed:)` is throwing (v1.3.1 replaced `fatalError`/`precondition` with typed errors).
- If BIP39 encoding fails, onboarding displays raw seed fallback rather than crashing.
- Demo mode creates a non-Keychain demo group with ID `00000000-0000-0000-0000-00000000d013` and seed bytes `TIGER-DEMO-SAFEWORDS-V13-DEMO-!!`.
- `enterDemoMode()` only runs when no real groups exist.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/OnboardingView.swift:3-417`
- `repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift:119-127`
- `repos/safewords-ios/Safewords/Crypto/RecoveryPhrase.swift:4-222`
- `repos/safewords-ios/Safewords/Services/GroupStore.swift:54-217`
- `repos/safewords-ios/Safewords/Services/KeychainService.swift:5-96`

#### Android — OnboardingScreen

**Purpose**: First-run flow. Four entry points: create new group, join with QR, restore from recovery phrase, or try the demo.

**UI surface**:
- **Welcome panel** — short product pitch + "Restore from a backup" link
- **Start panel** — four `OnboardOption` rows: Create / Join with QR / Restore / Try without a group
- **Create form** — name + creator name fields, with seed display below
- (Conditional) **Recovery seed display** during create — 4×6 grid of the just-generated 24-word phrase

**User actions**:
- "Create a new group" → `step = 2`, shows form
- "Join with a QR code" → `onJoinWithQR()` → QR scanner route
- "Restore from a backup" → `onJoinWithRecovery()` → recovery phrase route
- "Try without a group" → `onTryDemo()` → triggers `enterDemoMode()`, navigates to Home with the demo group active
- Form "Create" submit → `GroupRepository.createGroup(name, creatorName)` → `onComplete(group.id)` → Home, popping Onboarding off the back stack

**Technical implementation**:
- Step state `var step` and path state `var path`.
- `PanelStart` is a private composable; accepts the four callbacks.
- `OnboardOption` is a private composable for the bordered rounded-rectangle option row with icon + title + subtitle.
- Seed generation: `GroupRepository.createGroup` calls `TOTPDerivation.bytesToHex(generateSeed())` which uses `java.security.SecureRandom`.
- Recovery phrase displayed via `RecoveryPhrase.encode(seed)` falling back to hex on `Bip39.Error.MissingWordlist`.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/onboarding/OnboardingScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/service/GroupRepository.kt:71-89`

---

### Recovery phrase entry (restore)

#### iOS — RecoveryPhraseView

**Purpose**: Restore or join a group from a 24-word BIP39 recovery phrase or a 64-character hex seed.

**UI surface**: Back button, eyebrow `Recovery`, title `Restore a group`, fields for Group name + Your name, `TextEditor` labeled `Recovery phrase or seed`, helper text describing accepted formats, error card on failure, CTA `Join group`.

**User actions**:
- Back → routes Onboarding if no groups exist, otherwise Groups.
- Join group → parses seed via `RecoveryPhraseService.parseSeed(from:)`, writes Keychain + metadata, selects group, marks onboarding complete, routes Home.

**Technical implementation**:
- `RecoveryPhraseService.parseSeed(from:)` accepts either 64 hex characters or a 24-word BIP39 phrase.
- Parsing errors surface via `LocalizedError.errorDescription`.
- `GroupStore.joinGroup(...)` exits demo mode first, then stores the restored seed with the new local group record.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/RecoveryPhraseView.swift:3-139`
- `repos/safewords-ios/Safewords/Services/RecoveryPhraseService.swift:3-67`
- `repos/safewords-ios/Safewords/Services/GroupStore.swift:78-93`

#### Android — RecoveryPhraseScreen

**Purpose**: Restore a group from a 24-word BIP39 phrase or a 64-character hex seed.

**UI surface**: Header "Restore from backup", multi-line text input, helper text, validation error pills (empty / wrong word count / unknown word / bad checksum / invalid hex), "Restore" button.

**User actions**:
- Paste/type a phrase → `parseSeedOrPhrase()` runs on submit; returns `ParseResult.Ok(seed)` / `ParseResult.Err(message)` / `ParseResult.Empty`.
- "Restore" → `GroupRepository.joinGroup(name, seedHex, interval, memberName)`; on success `onJoined()` → Home.

**Technical implementation**:
- `parseSeedOrPhrase(input)` decodes via `RecoveryPhrase.decode(input)` for word phrases (catches `Bip39.Error.*` exceptions) or runs hex validation for 64-char input.
- BIP39 errors map to user-friendly copy via the sealed-class `userMessage` field.
- v1.3.1 wordlist load is defensive — `MissingWordlist` error here means the asset bundle is broken; we fall back to suggesting hex input.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/onboarding/RecoveryPhraseScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/RecoveryPhrase.kt`

---

### Plain Mode root

#### iOS — PlainRoot

**Purpose**: The Plain Mode mini-app. Plain Mode is the v1.3 default home surface.

**UI surface**: One of `PlainHomeView`, `PlainVerifyView`, `PlainHelpView`, or `PlainOnboardingView`. Bottom `A11yTabBar` with `Word`, optional `Check`, and `Help`.

**User actions**:
- Plain tab taps → switch local `PlainScreen`.
- Plain Home gear → invokes the `onSettings` closure supplied by `ContentView`.

**Technical implementation**:
- Local `@State private var screen: PlainScreen = .home`.
- `A11yTabBar` only includes the `Check` tab when `groupStore.hasAnyVerifyPrimitive()` is true.
- Plain Mode controlled by `@AppStorage("plainMode")` in `ContentView` and `SettingsView`.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:8-108, 608-626`
- `repos/safewords-ios/Safewords/App/ContentView.swift:19-30`

#### Android — PlainRoot

**Purpose**: Same — the Plain Mode mini-app, default home for everyone in v1.3.

**UI surface**: One of `PlainHome`, `PlainVerify`, `PlainHelp`, or `PlainOnboarding`. `PlainTabBar` (Word / Check / Help) at bottom.

**Technical implementation**:
- Self-contained mini app with its own `PlainScreen` enum (Home / Verify / Help / Onboarding).
- `PlainRoot(onExitPlain, onSetupReal)` is called from `SafewordsNavigation` with two distinct callbacks: `onExitPlain` flips `advancedView = true`; `onSetupReal` does that *plus* `exitDemoMode()`.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt:75-101`

---

### Plain Home (the v1.3 default front door)

#### iOS — PlainHomeView

**Purpose**: High-visibility word screen for the selected group, optimized for stressful calls and low-vision users.

**UI surface**:
- Header with group avatar, `Your circle`, group name, gear button.
- Hero card with `YOUR WORD NOW` or `YOUR CODE NOW`.
- Current phrase rendered one word per line, or numeric code as a single split-free value.
- Countdown pill `New word in {human time}`.
- Guidance: `Ask: "What is our word?" Do not say it first.`
- Optional `Challenge someone` button when challenge/answer is enabled.
- Optional static override note when static override is enabled.

**User actions**:
- Gear → routes to Advanced Settings while keeping Plain Mode enabled.
- Challenge someone → presents `ChallengeSheet`.
- Plain bottom tabs → switch to Verify or Help.

**Technical implementation**:
- `TimelineView(.periodic(from: .now, by: 1.0))` for a 1 Hz UI tick.
- Calls `groupStore.safeword(for:at:)` (numeric vs adjective/noun/number selected by group primitives).
- Challenge sheet receives the selected group's seed from `groupStore.seed(for:)`.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:110-252`
- `repos/safewords-ios/Safewords/Services/GroupStore.swift:219-252`
- `repos/safewords-ios/Safewords/Views/ChallengeSheet.swift:3-192`

#### Android — PlainHome

**Purpose**: Same — the v1.3 default home with big-word, big-button layout.

**UI surface**:
- (v1.3.1) Demo banner at top **only when** `demoMode == true`: "DEMO MODE / Set up your real group →" on accent background.
- Header: large circular avatar with first letter of group name + "Your circle" / group name + "Standard view" pill in top right.
- Hero card: "YOUR WORD TODAY" label + the rotating word (split per word at 48sp) OR the 6-digit numeric code (single line, 56sp tracked).
- Countdown pill: "New word in {HH:MM:SS}".
- Bottom: tab bar with Word / Check / Help.

**User actions**:
- "Standard view" pill tap → `onExitPlain()` → flips `advancedView = true` (sticky).
- Demo banner tap → `onSetupReal()` → `exitDemoMode()` + flip to Advanced.
- "Check" tab → switches to `PlainScreen.Verify`.
- "Help" tab → `PlainScreen.Help`.

**Technical implementation**:
- Reads `GroupRepository.groups`, `activeGroupId`, `demoMode` via `collectAsState()`.
- `LaunchedEffect(g)` runs a 1-second polling loop calling `GroupRepository.getCurrentSafeword(group.id)` (numeric-aware) and `TOTPDerivation.getTimeRemaining(intervalSeconds)`.
- Word format detection: phrase contains a space → render line-per-word; no space → single line with letter-spacing for the numeric path.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt:186-405` (PlainHome)
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt:DemoBanner`

---

### Plain Verify

#### iOS — PlainVerifyView

**Purpose**: Plain Mode yes/no verification flow.

**UI surface**: Step label `STEP 1 OF 2`. Prompt: `Ask them: "What is our word?"`. Warning that the user must not read the word first. Large buttons: `Yes, it matched` and `No, wrong word`. Result panels: `Safe to talk.` or `Hang up now.`. Optional `Call them back on a trusted number`.

**User actions**:
- Yes → match result.
- No → mismatch guidance.
- Call trusted number → opens SMS/call fallback URL.
- All done / I hung up → resets to ask phase.

**Technical implementation**:
- Local `Phase` enum: `ask`, `match`, `nomatch`.
- No cryptographic check — Plain Verify is human confirmation against the word visible on Plain Home.
- Trusted-number fallback uses `SmsInviteService.fallbackURL(...)`.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:254-402`
- `repos/safewords-ios/Safewords/Services/SmsInviteService.swift:1-52`

#### Android — PlainVerify

**Purpose**: Plain-mode "Did they say the right thing?" screen. Big yes/no buttons.

**UI surface**: Header "Did they say the word?". Two huge buttons: green "Yes — match" and red "No — wrong". After tap: result panel directing user to keep going (yes) or hang up + call back on a known number (no).

**Technical implementation**:
- Self-contained `PlainVerify(onDone)` with `PlainAsk` and `PlainResult` sub-composables.
- No actual cryptographic check — Plain mode trusts the user's eyes; the rotating word on PlainHome is what they verify against.
- Distinct from Advanced VerifyScreen which has the C/A primitive flow.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt` (PlainVerify section)

---

### Plain Help

#### iOS — PlainHelpView

**Purpose**: Plain Mode help screen — call-safety guidance, device settings, trusted-contact fallback, emergency escalation.

**UI surface**: Header `HELP` and title `How can we help?`. Help cards: `I got a strange call`, `Who is in my circle`, `What's a "word"?`, `Change text size`, `Call my family for help`. Emergency card `If you feel unsafe, call 911.`. `Open Advanced View` button.

**User actions**:
- Change text size → opens iOS Settings.
- Call my family for help → fallback SMS/call URL.
- Emergency → `tel://911`.
- Open Advanced View → sets `plainMode = false`.

**Technical implementation**:
- `@AppStorage("plainMode")` directly to exit Plain Mode.
- `@Environment(\.openURL)` for Settings, emergency phone URL, fallback URL.
- Static help items local to the view; no network or remote content.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:404-520`

#### Android — PlainHelp

**Purpose**: Plain-mode help screen with the "Exit high visibility" toggle.

**UI surface**: Help text + "Exit high visibility" big button at the bottom.

**User actions**: "Exit high visibility" → `onExitPlain()` → routes to Advanced view.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt` (PlainHelp section, ~line 527)

---

### Plain Onboarding (sub-flow)

#### iOS — PlainOnboardingView

**Purpose**: Small Plain Mode explainer flow for users entering the high-visibility surface.

**UI surface**: Two panels (`WELCOME` and `HOW IT WORKS`). Large copy "One word keeps you safe." and "Your family picks a secret word.". Example word card "Golden Robin". CTA `Show me how`, then `Get started`. Back button on second panel.

**User actions**:
- CTA on first panel → `step += 1`.
- CTA on second panel → `plainOnboarded = true`.
- Back → `step -= 1`.

**Technical implementation**:
- Local `step` state indexes static `panels` array.
- Completion persists `@AppStorage("plainOnboarded")`.
- Reachable through `PlainScreen.onboarding` enum but not exposed as a normal tab.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:522-606`

#### Android — PlainOnboarding

Similar small explainer reachable through the `PlainScreen.Onboarding` enum on first Plain entry. Persists completion in Compose `rememberSaveable`. Files: `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt`.

---

### Home (Advanced)

#### iOS — HomeView

**Purpose**: Advanced tabbed home. Shows current word/code with countdown ring and group selector.

**UI surface**:
- Group selector pill at top left.
- Bell button at top right (currently routes Settings).
- `CountdownRing` hero with `LIVE · {GROUP}` label.
- Current word split into lines, with optional blur for hold-to-reveal.
- Sequence label `SEQ · ####`.
- Countdown text `HH:MM:SS`.
- Secondary text: `rotates in ...`, optionally `next: {word}` when preview enabled.
- Empty state: `No groups yet` and `Create a group`.

**User actions**:
- Group selector pill → routes Groups.
- Bell → routes Settings.
- Hold on the word when hold-to-reveal is enabled → reveals blurred word while held.
- Create a group in empty state → routes Onboarding.

**Technical implementation**:
- `TimelineView(.periodic(from: .now, by: 1.0))` for countdown progress.
- `CountdownRing` custom SwiftUI view.
- `@AppStorage("revealStyle")` controls hold-to-reveal.
- `@AppStorage("previewNextWord")` for next-word preview.
- Display value via `GroupStore.safeword(for:at:)` (numeric-aware).

**Files of record**:
- `repos/safewords-ios/Safewords/Views/HomeView.swift:3-210`
- `repos/safewords-ios/Safewords/Views/Components/CountdownRing.swift:6-91`

#### Android — HomeScreen

**Purpose**: Same role — standard tabbed UI's home.

**UI surface**:
- Header: SectionLabel "Home" + group selector pill.
- Hero: `CountdownRing` with the current word in the center.
- Group switcher row when there are multiple groups.
- "Share invite" CTA → routes to QR Display.

**User actions**:
- Group pill tap → cycles active group via `GroupRepository.setActiveGroup(...)`.
- "Share invite" → navigates to `qr_display/{groupId}`.

**Technical implementation**:
- Reads `groups`, `activeGroupId` via StateFlow.
- 1Hz coroutine ticker for countdown via `TOTPDerivation.getTimeRemaining(intervalSeconds)`.
- `CountdownRing` is a custom Compose composable (animated arc that drains over the rotation interval).

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/home/HomeScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/components/CountdownRing.kt`

---

### Groups

#### iOS — GroupsView

**Purpose**: Advanced tab for selecting active group, reviewing members, starting invite/setup flows.

**UI surface**: Header `Groups` / `Your circles`. Plus button to add. Group cards with avatar dot, name, `ACTIVE` badge, member count, rotation interval, current safeword, sequence. Selected group's member list. Invite CTA.

**User actions**:
- Group card tap → sets `groupStore.selectedGroupID` and routes Home.
- Plus → routes Onboarding.
- Invite → routes QR Display.

**Technical implementation**:
- Reads `groupStore.groups` and `groupStore.selectedGroup`.
- `GroupDot` plus deterministic `DotPalette.forIndex`.
- iOS does **not** include a separate group detail route; group metadata editing lives in `SettingsView`.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/GroupsView.swift:3-209`
- `repos/safewords-ios/Safewords/Views/Components/GroupDot.swift:4-49`

#### Android — GroupsScreen + GroupDetailScreen

**Purpose**: Lists all groups (`GroupsScreen`) and provides a separate per-group editing page (`GroupDetailScreen`).

**GroupsScreen UI**: Scrollable list of group cards (dot color, group name, member count). FABs: "Scan QR" + "Add group".

**GroupDetailScreen UI**:
- Header: group name (tap to rename inline).
- Current safeword display.
- Interval picker (Hourly / Daily / Weekly / Monthly).
- Member list with role tags.
- "Invite member" → QR Display.
- "Leave / delete this group" danger zone.

**User actions** (detail):
- Inline rename → `GroupRepository.renameGroup(groupId, newName)`.
- Interval pick → `GroupRepository.setRotationInterval(groupId, interval)` — also updates `primitives.rotatingWord.intervalSeconds`.
- Delete → `GroupRepository.deleteGroup(groupId)` (special-cased for demo group: short-circuits to `exitDemoMode`).

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/groups/GroupsScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/groups/GroupDetailScreen.kt`

---

### Verify (Advanced, primitive-conditional)

#### iOS — VerifyView

**Purpose**: Advanced verification surface. Useful only when active group has challenge/answer or static override enabled.

**UI surface**:
- Header `Verify` / `Are they who they say they are?`
- No-group state with `Start setup`.
- Verify-not-needed empty state.
- Optional `Challenge someone` CTA.
- Free-text field `type what they said`.
- `Check` button + microphone/listening button.
- Safety tips and listening panel with `They matched` / `They did not`.
- Result cards: `Verified.` or `Don't trust.`

**User actions**:
- Start setup → routes Onboarding.
- Open Primitives → routes Settings.
- Challenge someone → presents `ChallengeSheet`.
- Type an answer + Check → compares against current word, legacy emergency override, AND static override.
- Mic button → switches to listening phase.
- They matched / They did not → result phase.

**Technical implementation**:
- Local `Phase`: `ready`, `listening`, `match`, `mismatch`.
- `groupStore.verifyNeeded(for:)` returns `group.primitives.needsVerifySurface`.
- Static override comparison computes `Primitives.staticOverride(seed:)` on demand. Not stored.
- Legacy emergency override remains supported through `GroupStore.emergencyOverrideWord`.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/VerifyView.swift:3-348`
- `repos/safewords-ios/Safewords/Crypto/Primitives.swift:28-84`

#### Android — VerifyScreen

**Purpose**: Same role. v1.3 made this conditional on which primitives the active group has.

**UI surface**:
- Header: "Verify · Are they who they say they are?"
- **If group has no Verify-needing primitive**: empty-state copy "Verify isn't needed for {group} right now — both phones show the same word..."
- **If group has challengeAnswer**: ChallengeAnswerPanel → tap to open ChallengeSheet
- **If group has staticOverride** OR legacy: free-text "Type what they said" input + "Listen mode"

**Technical implementation**:
- `Phase` enum: Ready / Listening / Match / Mismatch.
- `currentWord = GroupRepository.getCurrentSafeword(g.id)` (numeric-aware).
- `needsVerify = challengeAnswer.enabled || staticOverride.enabled`.
- The legacy free-text panel is still wired below the empty state — pre-v1.3 muscle memory, not removed.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/verify/VerifyScreen.kt`

---

### ChallengeSheet (challenge/answer Match flow)

#### iOS — ChallengeSheet

**Purpose**: Deterministic challenge/answer flow for groups with the C/A primitive enabled.

**UI surface**: Sheet handle. Eyebrow `Challenge someone`. Group name. Row stepper `Row {n} of {rowCount}` with minus/plus controls. Prompt card with `Ask` and `Expect` phrases. Buttons `They said {expect phrase}` and `Does not match`. Lock row `Show full table` / `Hide full table`. Biometric failure message if reveal fails. Full table after unlock.

**User actions**:
- Minus/plus → changes `rowIndex` within bounds.
- They said expected phrase → dismisses sheet.
- Does not match → dismisses sheet.
- Show full table → device-owner authentication, then toggles table visibility.

**Technical implementation**:
- `row` derives via `Primitives.challengeAnswerRow(seed:tableVersion:rowIndex:)`.
- `rowCount` clamps at at least 1.
- Full-table reveal uses `BiometricService.authenticateDeviceOwner` (allows passcode fallback).
- Table is recomputed locally from the seed; no rows persisted.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/ChallengeSheet.swift:3-192`
- `repos/safewords-ios/Safewords/Crypto/Primitives.swift:56-67`

#### Android — ChallengeSheet

**Purpose**: Same.

**UI surface**:
- Top bar: close icon + "Verify" title.
- "Ask them: {askPhrase}" — large.
- "They should answer: {expectPhrase}" — slightly smaller.
- Two huge buttons: "They said it" (accent) and "Doesn't match".
- "Use a different row" subtle link.
- Result panel with green check or red X + guidance copy + "Done".

**Technical implementation**:
- `var rowIndex by remember { mutableIntStateOf(Random.nextInt(rowCount)) }` — random row at session start.
- `GroupRepository.getChallengeAnswerTable(groupId, rowCount)` → `List<Primitives.ChallengeAnswerRow>` (recomputed every call, cheap).
- `Primitives.challengeAnswerRow(seed, tableVersion, rowIndex)` does HMAC twice with separate domain-separated labels for ask/expect.
- `ChallengePhase` enum (renamed from `Phase` in commit `fc61512` to avoid Kotlin same-package private-name collision with VerifyScreen's `Phase`).

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/verify/ChallengeSheet.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/Primitives.kt:challengeAnswerRow`

---

### Override reveal

#### iOS

iOS does not have a standalone override reveal route. Static override is revealed through `VerifyView`'s match comparison and through the `OverrideCardView` rendered inside `SafetyCardsView` (biometric-gated). There is no dedicated `OverrideRevealView`.

Files involved:
- `repos/safewords-ios/Safewords/Views/VerifyView.swift:3-348` (override comparison)
- `repos/safewords-ios/Safewords/Views/Cards/OverrideCardView.swift:1-59`

#### Android — OverrideRevealScreen

**Purpose**: Biometric-gated reveal of the group's static override word. Word is computed from the seed on demand, never stored.

**UI surface**:
- Top bar: back icon + "Override word" title.
- Auth prompt (system biometric sheet) on first paint.
- After unlock: warning "Anyone with this word can act as you in this group." + the override word in big type (per-word breakdown) + footer reminder about seed rotation.

**Technical implementation**:
- `LaunchedEffect(groupId)` runs `BiometricService.canAuthenticate(activity)` then `BiometricService.authenticate(...)` with title "Reveal override".
- On success: `GroupRepository.getStaticOverride(groupId)` returns `Primitives.staticOverride(seed)` — `HMAC-SHA256(seed, "safewords/static-override/v1")` → standard word derivation.
- No persistence — word lives in `var word` state until screen leaves the back stack.
- If no biometric is enrolled, surfaces "Set a screen lock or biometric to view this." instead of crashing.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/verify/OverrideRevealScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/Primitives.kt:staticOverride`

---

### Settings

#### iOS — SettingsView (with PrimitiveSettingsSheet)

**Purpose**: Advanced preferences, per-group primitive toggles, security actions, practice entry points, destructive data actions.

**UI surface (sections)**:
- `View`: `Use Plain home by default`, Advanced view status.
- `Rotation · {group}`: interval picker, notify toggle, next-word preview toggle.
- `Accessibility`: `Hold to reveal word`.
- `Group`: `Primitives`, `Safety cards`.
- `Widget & Lock Screen`: widget instructions, lock-screen glance toggle, hide-until-unlock toggle.
- `Security`: require biometrics, emergency override word, rotate seed placeholder, seed phrase backup.
- `Practice`: scam drill and drill history.
- `Danger zone`: leave group and reset device.
- Footer: `Safewords v1.3.1 · Offline-first`.

**PrimitiveSettingsSheet**: Per-group toggles for Numeric / Static override / Challenge / answer with note that derived secrets are recomputed from the seed.

**User actions** (most representative):
- Plain home toggle → persists `plainMode`.
- Interval picker → `groupStore.updateGroupInterval` + updates rotating primitive interval.
- Require biometrics → checks `BiometricService.canEvaluate()` before enabling.
- Primitives → presents `PrimitiveSettingsSheet`.
- Safety cards → `SafetyCardsView`.
- Back up seed phrase → `RecoveryBackupView`.
- Run drill / drill history → `DrillsView`.
- Leave group → destructive confirmation, then `GroupStore.deleteGroup`.
- Reset device → wipes groups, seeds, drills, onboarding state.

**Technical implementation**:
- Regular `@AppStorage` for app prefs; App Group `@AppStorage(..., store:)` for widget-visible prefs.
- `PrimitiveSettingsSheet` uses bindings that re-read `groupStore.selectedGroup`.
- `GroupStore.updatePrimitives` normalizes table version, row count, derivation version, interval seconds before saving.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/SettingsView.swift:5-491`
- `repos/safewords-ios/Safewords/Services/GroupStore.swift:95-217`
- `repos/safewords-ios/Safewords/Data/GroupConfig.swift:98-169`

#### Android — SettingsScreen

**Purpose**: All app preferences and per-group toggles.

**UI surface (sections)**:
- **Rotation · {group}** — interval picker, "Notify on rotation", "Include preview of next word".
- **Verification · {group}** (v1.3) — "Show as 6-digit code" (numeric format toggle), "Static override word" enable + "Reveal override word" action, "Challenge / answer table" enable + "Run challenge" action, "Print safety cards".
- **Accessibility** — "High visibility mode" (legacy plainMode toggle).
- **Widget & Lock Screen** — "Lock screen glance", "Hide word until unlock".
- **Security** — "Require biometrics to open", "Emergency override word" (legacy v1.2; superseded by static override but not removed), "Rotate group seed", "Back up seed phrase".
- **Tools** — "Single use word generator".
- **Practice** — "Run a scam drill" + drill history.
- **Danger zone** — "Leave this group", "Reset all data".

**User actions** (Verification section is v1.3 specific):
- "Show as 6-digit code" → `setWordFormat(WordFormat.NUMERIC)` or back to `ADJECTIVE_NOUN_NUMBER`.
- "Static override word" toggle → `setStaticOverrideEnabled(groupId, on)`.
- "Reveal override word" → `onRevealOverride(groupId)` → `OverrideRevealScreen` route.
- "Run challenge" → `onRunChallenge(groupId)` → ChallengeSheet route.
- "Print safety cards" → `onOpenSafetyCards()` → `SafetyCardsScreen` route.

**Technical implementation**:
- `var notify by remember { mutableStateOf(GroupRepository.isNotifyOnRotation()) }` etc. — boolean toggles read once on entry, written on each tap.
- The "Verification" section reads `active?.primitivesOrDefault()` — handles v1.2 groups without primitives by expanding from legacy `interval`.
- The legacy "Emergency override" entry stores a free-text user-entered word in ESP under `emergency_override_{groupId}`; v1.3 static override is a separate, deterministic concept.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/settings/SettingsScreen.kt`

---

### Recovery phrase backup (from Settings)

#### iOS — RecoveryBackupView

**Purpose**: Biometric/passcode-gated display of the active group's recovery phrase from Settings.

**UI surface**: Back button, eyebrow `Back up seed phrase`, title `For {group}` or `No active group`, warning copy, locked state button `Unlock to reveal`, 4-column indexed phrase grid after unlock, `Copy to clipboard` / `Copied` button, footer `Backup format: BIP39 English, 24 words.`

**User actions**:
- Unlock to reveal → device-owner authentication, then phrase derivation.
- Copy → `UIPasteboard.general.string`.

**Technical implementation**:
- `LAContext.evaluatePolicy(.deviceOwnerAuthentication)` directly (passcode fallback allowed).
- `RecoveryPhrase.encode(seed:)` — failures displayed rather than crashing.
- Phrase recomputed on demand, not persisted.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/RecoveryBackupView.swift:5-246`

#### Android — RecoveryBackupScreen

**Purpose**: Settings → Security → "Back up seed phrase". Shows the active group's 24-word BIP39 phrase.

**UI surface**: Auth prompt on entry. 4×6 grid of indexed words. "Copy to clipboard" action. "Done" button.

**Technical implementation**:
- `BiometricService.authenticate(...)` gates entry.
- `RecoveryPhrase.encode(seed)` — fall back to "Recovery word list is unavailable. Use the raw seed: {hex}" on `Bip39.Error.MissingWordlist`.
- Phrase **not** persisted — recomputed every visit.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/settings/RecoveryBackupScreen.kt`

---

### Safety Cards

#### iOS — SafetyCardsView

**Purpose**: Browser and print/share surface for native printable cards.

**UI surface**: Eyebrow `Safety cards`, active group name, auth error card, card rows (Protocol / Static override / Challenge wallet excerpt / Challenge full protocol / Recovery phrase / Group invite). Preview area for selected card. `Print` and `Share` buttons.

**User actions**:
- Tap protocol card → selects immediately (no biometric).
- Tap sensitive card → device-owner authentication, then selects.
- Print → renders SwiftUI card to `UIImage` → `UIPrintInteractionController`.
- Share → renders to `UIImage` → `UIActivityViewController`.

**Technical implementation**:
- `SafetyCardKind.available(for:)` computes visible cards from group primitive config.
- High-sensitivity cards require `BiometricService.authenticateDeviceOwner`.
- `CardRenderer.render` uses SwiftUI `ImageRenderer`.
- Recovery card falls back to raw seed hex if BIP39 encoding fails.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/SafetyCardsView.swift:4-266`
- `repos/safewords-ios/Safewords/Print/CardRenderer.swift:1-38`
- `repos/safewords-ios/Safewords/Views/Cards/{ProtocolCardView,OverrideCardView,ChallengeAnswerCardView,RecoveryPhraseCardView,GroupInviteCardView}.swift`

#### Android — SafetyCardsScreen

**Purpose**: Same — per-group card browser.

**UI surface**:
- Top bar: back + "Safety cards".
- Vertical list of `CardRow`s with lock icon for sensitive types.
- "Print" affordance on the right of each row.

**User actions**:
- Tap protocol row → renders + prints (no biometric).
- Tap any sensitive row → `gateThen(activity)` runs biometric prompt, then renders + prints.
- Tap override row → also calls `markStaticOverridePrinted(groupId)`.

**Technical implementation**:
- `gateThen(activity, action)` checks `BiometricService.canAuthenticate`, falls through if no biometric is enrolled (rationale: blocking the user from their own card is worse than the marginal security gain).
- Each `CardRow` calls `CardRenderer.renderXxxCard(...)` returning a `Bitmap`, then `CardRenderer.print(context, bmp, jobName)` routes to `androidx.print.PrintHelper.printBitmap`.
- Card text loaded from `/shared/safety-card-copy.json` via `SafetyCardCopy.card(context, key)` + `SafetyCardCopy.str(json, key, vars)` (handles `{groupName}` substitution).

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/cards/SafetyCardsScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/print/CardRenderer.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/print/SafetyCardCopy.kt`

---

### QR Display (group invite)

#### iOS — QRDisplayView

**Purpose**: Show a group invite QR for in-person joining; offer SMS invite fallback.

**UI surface**: Eyebrow `Invite · {group}`, title `Share in person`, large QR card with center mark, instructions, security ticker `256-BIT · ROTATING · OFFLINE · {seconds}S`, `Invite via SMS instead`.

**User actions**:
- Invite via SMS → `MFMessageComposeViewController` when available; `sms:` fallback URL otherwise.
- Timer expiration → automatic route back to Groups after 60 seconds.

**Technical implementation**:
- QR generated by `QRCodeService.generateQRCode(for:seed:size:)`.
- Payload includes group metadata + seed (seed-equivalent).
- `SmsInviteService.inviteText(group:seed:)` includes recovery display code or QR-scan instruction.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/QRDisplayView.swift:4-166`
- `repos/safewords-ios/Safewords/Services/QRCodeService.swift:6-129`
- `repos/safewords-ios/Safewords/Services/SmsInviteService.swift:1-52`

#### Android — QRDisplayScreen

**Purpose**: Same.

**UI surface**: QR code in a large card, group name + caveats, "Share via SMS" and "Copy link" actions, "Done" button.

**User actions**:
- "Share via SMS" → `Intent.ACTION_SENDTO` with `sms:` URI prefilled with safewords.io install + invite text via `SmsInviteService`.
- Copy link → clipboard.

**Technical implementation**:
- `QRCodeService.generateQRBitmap(group, size = 512)` — ZXing core to render `BitMatrix`, painted to a Bitmap.
- Payload follows `/shared/qr-schema.json` v1.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/qr/QRDisplayScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/service/QRCodeService.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/service/SmsInviteService.kt`

---

### QR Scanner (camera)

#### iOS — QRScannerView

**Purpose**: Camera-based group invite scanner.

**UI surface**: `NavigationStack` title `Join Group`. Permission state, denied state with `Use recovery code` fallback, scanner state with 280px preview + reticle + torch toggle, group-found state with name field + `Join Group`.

**User actions**:
- Enable camera → AVFoundation video permission.
- Use recovery code → routes RecoveryPhraseView.
- Cancel → caller-provided closure.
- Torch toggle → flips device torch.
- Valid QR → name prompt → Join Group → saves seed + metadata + selects group.

**Technical implementation**:
- AVFoundation directly (`AVCaptureSession`, `AVCaptureDeviceInput`, `AVCaptureMetadataOutput`, `.qr` metadata type).
- `QRCameraPreview` bridges SwiftUI to `QRCameraUIView`.
- `QRCodeService.parseQRCode` decodes JSON payload.
- Invalid scans set `scanError` and refresh preview ID after 0.8s.
- Successful joins call `GroupStore.joinGroup(...)` which exits demo mode first.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/QRScannerView.swift:5-366`

#### Android — QRScannerScreen

**Purpose**: Same.

**UI surface**: Full-screen camera preview, reticle overlay, permission rationale screen if `CAMERA` is denied.

**Technical implementation**:
- CameraX preview + MLKit barcode-scanning (bundled-model variant; the un-bundled MLKit pulls in Firebase telemetry and adds INTERNET permission, which we can't have — see `app/build.gradle.kts:configurations.all { exclude(...) }`).
- `QRCodeService.parseQRPayload(rawPayload)` returns `ParsedGroup?` — null on invalid payload.
- On success: `GroupRepository.joinGroup(name, seedHex, interval, memberName)` — calls `exitDemoMode()` first.
- Lifecycle-aware preview tear-down via `LifecycleOwner` from compose.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/qr/QRScannerScreen.kt`
- `repos/safewords-android/app/build.gradle.kts:14-18` (Firebase telemetry exclusion)

---

### Drills (practice)

#### iOS — DrillsView

**Purpose**: Practice flow for scam-call scenarios.

**UI surface**: Eyebrow `Practice`, title `Scam drills`, idle card with `Run drill now`, running card with scenario copy, result buttons `They knew it` / `They failed`, history list.

**User actions**:
- Run drill now → `running = true`.
- They knew it / They failed → records a `DrillSession`, refreshes history.
- Cancel drill → exits running state without recording.

**Technical implementation**:
- Local `sessions` state seeded from `DrillService.sessions()`.
- `DrillService.record(group:scenario:success:)` stores sessions in `UserDefaults.standard`.
- No network or dynamic content generation.
- Reset device clears drill history through `DrillService.clear()`.

**Files of record**:
- `repos/safewords-ios/Safewords/Views/DrillsView.swift:3-179`
- `repos/safewords-ios/Safewords/Services/DrillService.swift:3-69`

#### Android — DrillsScreen

**Purpose**: Same.

**UI surface**: Multi-step prompt + result. Scenarios drawn from `DrillService.scenarios`.

**Technical implementation**:
- `DrillService` is a stateless object with predefined scenarios + a tiny pass-rate stat.
- No network, no real generation — just a curated list.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/drills/DrillsScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/service/DrillService.kt`

---

### Single-use word generator

#### iOS

Not present in v1.3.1. iOS has no `AppScreen.generator` route and no generator view. Any future port should mirror the Android implementation below.

#### Android — GeneratorScreen

**Purpose**: Settings → Tools → "Single use word generator". Generates a one-off random adjective+noun+number for ad-hoc use (not tied to any group).

**UI surface**: Big word display + "Regenerate" button.

**Technical implementation**:
- `WordGenerator.generate()` — pure random pick (not HMAC) since this isn't tied to a seed.
- No persistence.

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/generator/GeneratorScreen.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/data/WordGenerator.kt`

---

### Widget

#### iOS — Small + Medium

**Small widget**:
- UI: word/code in accent color, up to two lines, clock icon, compact countdown like `1h 02m` or `4:09`. Hidden states `Widget Off`, `Unlock to View`, `No Group`.
- `SafewordsTimelineProvider` loads groups from App Group `UserDefaults` key `safewords.groups`, selected group from `safewords.selectedGroupID`, seed from Keychain via widget-local `WidgetKeychain`.

**Medium widget**:
- UI: group name (small uppercase), word/code (accent title), countdown ring with elapsed progress, compact countdown.
- Same timeline provider feeds both sizes.
- `SafewordsWidgetEntryView` dispatches on `widgetFamily`.
- Widget derivation duplicated in `WidgetTOTP` (does not import the main app module).

**v1.3.1 build 7** added a widget target pre-build resource copy for `adjectives.json`, `nouns.json`, and `safety-card-copy.json` — fixed the widget showing "Error" because the resources weren't in the appex bundle.

**Files of record**:
- `repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:45-390`
- `repos/safewords-ios/SafewordsWidget/SafewordsWidgetBundle.swift:1-9`
- `repos/safewords-ios/project.yml:62-90`

#### Android — Glance widget

**Purpose**: Home-screen widget showing the active group's current word + countdown.

**UI surface**: Compact layout: group name (small, top), current word (large, center), countdown (small, bottom). Tap target spans the whole widget.

**User actions**:
- Tap → `actionStartActivity(MainActivity launch intent)` — opens to Plain home.

**Technical implementation**:
- `SafewordsWidget extends GlanceAppWidget` — uses `androidx.glance.appwidget`.
- `provideContent { ... }` reads `GroupRepository.activeGroup()` and `GroupRepository.getCurrentSafeword(...)` directly.
- Updates triggered by:
  1. WorkManager periodic worker (configurable per `notify_on_rotation` pref)
  2. Runtime broadcast receiver in `SafewordsApp.kt` for `ACTION_USER_PRESENT` / `ACTION_SCREEN_ON`
- Widget receiver class is in the `:app` module (was in a separate `:widget` module pre-v1.2; moved because Android couldn't resolve the receiver class across module boundaries — `ClassNotFoundException`).

**Files of record**:
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/widget/SafewordsWidget.kt`
- `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/SafewordsApp.kt`

---

## Cross-cutting

### Theme + design tokens

**iOS**: `repos/safewords-ios/Safewords/Design/Theme.swift`. `Ink` is the default editorial dark theme; `A11Y` is the Plain Mode high-visibility palette. `Fonts.display` prefers `Fraunces` if registered, else system serif. `A11yFonts` prefers `AtkinsonHyperlegible-Regular`.

**Android**: `repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/theme/`. `Color.kt` has `Ink` (dark theme tokens) and `A11y` (Plain mode tokens) palettes. `Theme.kt` has `SafewordsTheme()` Compose Material3 theme.

See `docs/design-system.md` for the token-by-token table across iOS/Android.

### Storage

| What | iOS | Android |
|---|---|---|
| Group metadata | App Group `UserDefaults`, key `safewords.groups` | ESP key `groups_json` via `GroupRepository` |
| Selected/active group ID | App Group `UserDefaults`, key `safewords.selectedGroupID` | ESP key `active_group_id` |
| Per-group seed | iOS Keychain generic password, service `app.thc.safewords.seeds`, App Group access | ESP via `SecureStorageService` (separate prefs file) |
| Widget-visible prefs | App Group `UserDefaults`, keys `lockScreenGlance`, `hideWordUntilUnlock` | ESP keys |
| App prefs | `@AppStorage` in app defaults | ESP keys (`plain_mode`, `advanced_view_enabled`, `notify_on_rotation`, `preview_next_word`, `lock_screen_glance`, `hide_until_unlock`, `biometric_required`) |
| Legacy emergency override | App Group `UserDefaults`, key prefix `safewords.emergencyOverride.` | ESP key prefix `emergency_override_` |
| Demo mode flag | App Group `UserDefaults`, key `safewords.demoMode` | ESP key `demo_mode` |
| Demo seed | Hardcoded `Data` in `GroupStore`, never written | Hardcoded constant `GroupRepository.DEMO_SEED_BYTES`, never written |
| Drill history | `UserDefaults.standard` via `DrillService` | ESP via `DrillService` |

Both platforms: group metadata excludes demo group records before writing. `resetAllData()` deletes seeds, metadata, selected group, demo flag, and emergency override keys. The demo seed is the one exception to "seeds in keystore only" — it's hardcoded and never persisted.

### Print pipeline (native, offline)

| | iOS | Android |
|---|---|---|
| Render | SwiftUI view → `UIImage` via `ImageRenderer` | Compose-laid-out content → `Bitmap` via `Canvas` |
| Print | `UIPrintInteractionController` | `androidx.print.PrintHelper.printBitmap()` |
| Share | `UIActivityViewController` | system share sheet |
| Sensitive gating | `BiometricService.authenticateDeviceOwner` (passcode fallback OK) | `BiometricService.authenticate` (passcode fallback OK; falls through if no biometric enrolled) |

Card copy: `/shared/safety-card-copy.json` — bundled into both apps (iOS via `project.yml` resources + preBuildScripts copy; Android via `copySharedToAssets` gradle task).

### Cross-platform contract

Every primitive must produce byte-identical output on Android and iOS for the same seed/timestamp/row/etc.

| Contract | Path |
|---|---|
| v1.0 rotating word | `/shared/test-vectors.json` |
| v1.2 BIP39 recovery | `/shared/recovery-vectors.json` + `/shared/recovery-schema.md` |
| v1.3 primitives | `/shared/primitive-vectors.json` |
| v1.2→v1.3 migration | `/shared/migration-vectors.json` |
| Card copy | `/shared/safety-card-copy.json` |
| QR invite payload | `/shared/qr-schema.json` |
| Frozen wordlists | `/shared/wordlists/{adjectives,nouns}.json` (v1.0), `/shared/wordlists/bip39-english.txt` (v1.2) |

iOS tests: `repos/safewords-ios/SafewordsTests/{PrimitiveVectors,Migration,RecoveryPhrase,TOTPDerivation}Tests.swift`.
Android tests: `repos/safewords-android/app/src/test/kotlin/com/thc/safewords/{TOTPDerivationTest,crypto/PrimitivesTest,crypto/Bip39Test,data/GroupSchemaTest}.kt`.

Gradle's `copySharedToTestResources` task copies every shared JSON file (plus adjectives/nouns) into `app/src/test/resources/` before tests run — JVM unit tests can load via classpath without needing `SafewordsApp.instance.assets`.

## Build + test surface

### Android (run on `u5` dev VM — primary VM has no gradle/SDK/keystore)

Sync changes to u5 first, then build/test there:

```bash
# Primary VM → u5 sync
rsync -av repos/safewords-android/app/src/ u5:/home/ultra/code/safewords-mobile/repos/safewords-android/app/src/
rsync -av repos/safewords-android/app/build.gradle.kts u5:.../app/build.gradle.kts
rsync -av repos/safewords-android/gradle/libs.versions.toml u5:.../gradle/libs.versions.toml

# On u5
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && ./gradlew :app:testDebugUnitTest"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && ./gradlew :app:assembleDebug"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane build"      # signed AAB
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane internal"   # build + push to Play
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane closed"     # closed/alpha
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane production" # production (with confirm)
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane promote_to_prod"
ssh u5 "cd /home/ultra/code/safewords-mobile/repos/safewords-android && fastlane metadata"   # listing copy + screenshots only
```

### iOS (GitHub Actions macOS runner; no local Xcode on either VM)

```bash
# Triggered manually
gh workflow run "iOS Release" --ref main -f lane=beta       # archive + TestFlight upload
gh workflow run "iOS Release" --ref main -f lane=validate   # diagnostic run, no upload
```

The macOS runner has Xcode 26.3.0, fastlane match, and the encrypted certificates repo. Resource assertions at the end of each beta build verify the IPA contains:

- `Payload/Safewords.app/bip39-english.txt`
- `Payload/Safewords.app/safety-card-copy.json`
- `Payload/Safewords.app/PlugIns/SafewordsWidget.appex/adjectives.json`
- `Payload/Safewords.app/PlugIns/SafewordsWidget.appex/nouns.json`
- `Payload/Safewords.app/PlugIns/SafewordsWidget.appex/safety-card-copy.json`

### Cross-platform parity validation

Beyond unit tests, you can verify parity end-to-end:
1. Install both Android v1.3.1 and iOS v1.3.1.
2. Both in demo mode on the same date → home screen word should match.
3. OR scan the Android-issued group invite QR with iOS → both phones should show the same rotating word.

See `docs/release-state.md` for the live track snapshot and `docs/release-pipeline-gotchas.md` for failure modes from setting up these pipelines.

## Appendix: deferred or platform-different features

### Implemented on Android but not on iOS in v1.3.1
- **Single-use word generator** (`GeneratorScreen`) — no `AppScreen.generator` on iOS yet
- **Separate group detail screen** (`GroupDetailScreen`) — iOS combines group selection in `GroupsView` and per-group config in `SettingsView`
- **Dedicated override reveal route** (`OverrideRevealScreen`) — iOS reveals static override through Verify matching and safety-card rendering rather than a standalone route

### Deferred on both platforms
- **Pair mode** (per-contact rotating C/A) — v1.5+, deferred until member identity is crypto-stable across devices and recovery
- **Agent Circle** — separate repo `safewords-agent`, not consumer app
- **Expiring invites** — until they exist, the group invite QR is treated as seed-equivalent (high sensitivity)
- **Push notifications** — possible v1.4+, low priority
- **SMS fallback** — modern Android SMS APIs require the app be a default SMS handler, incompatible with our zero-permission stance
- **Custom font asset registration** for `Fraunces` (display serif) and `Atkinson Hyperlegible` (Plain mode body)

See `docs/feature-spec.md` "Deferred / future" for the canonical product list.
