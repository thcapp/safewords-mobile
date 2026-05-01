# Safewords Android — Page-by-Page Reference

Status: v1.3.1 / versionCode 15 (commit `001527a` and later)
Module: single `:app` module under `repos/safewords-android/`
Min SDK: 26 (Android 8) · Target/Compile SDK: 35

This is the Android half of the unified `safewords-mobile.md`. Codex maintains the iOS half at `safewords-mobile-ios.md`. They get merged into one canonical reference.

## Conventions

- Each screen section: **Purpose**, **UI surface**, **User actions**, **Technical implementation**, **Files of record**.
- File paths are absolute under `repos/safewords-android/` so they're greppable.
- "EncryptedSharedPreferences" abbreviated to **ESP** in this doc.
- "GroupRepository" is the singleton that owns groups + active group + global prefs.

## Architecture overview

```
MainActivity (FragmentActivity)
  └─ SafewordsNavigation()                 ← single navigation root
       │
       ├─ if (plainMode) → PlainRoot       ← legacy "high visibility" toggle wins
       │
       ├─ if (!advancedView) → PlainRoot   ← v1.3 default for everyone
       │       PlainHome / PlainVerify / PlainHelp / PlainOnboarding
       │       (gear-icon-equivalent "Standard view" pill exits to Advanced)
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

State sources:
- `GroupRepository.groups: StateFlow<List<Group>>` — all groups
- `GroupRepository.activeGroupId: StateFlow<String?>` — selected group
- `GroupRepository.demoMode: StateFlow<Boolean>` — v1.3.1 demo-mode flag
- ESP-backed booleans: `plain_mode`, `advanced_view_enabled`, `notify_on_rotation`, `preview_next_word`, `lock_screen_glance`, `hide_until_unlock`, `biometric_required`
- ESP-backed strings: `groups_json`, `active_group_id`, `default_interval`, `emergency_override_*` (legacy), `demo_mode`

Per-group seeds live in `SecureStorageService` (also ESP-backed but keyed separately so JSON dump leak doesn't expose seeds).

---

## Screens

### MainActivity

**Purpose**: The single Android Activity. Hosts the Compose tree and registers a runtime broadcast receiver for `ACTION_USER_PRESENT` and `ACTION_SCREEN_ON` to refresh the home-screen widget.

**UI surface**: None (sets `setContent { SafewordsApp() }`).

**User actions**: N/A.

**Technical implementation**:
- Extends `FragmentActivity` (required for `BiometricPrompt`).
- `setContent` mounts `SafewordsApp()` which applies the dark-mode theme and renders `SafewordsNavigation()`.
- The receiver in `SafewordsApp.kt` (Application subclass) fires `SafewordsWidget.update(...)` so the widget shows a fresh word after the user unlocks the device.

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/MainActivity.kt`
- `app/src/main/kotlin/com/thc/safewords/SafewordsApp.kt`

---

### SafewordsNavigation (root composable)

**Purpose**: The Compose nav root. Decides between Plain (default) and Advanced (tabbed) and hosts the `NavHost` for every Advanced-view route.

**UI surface**:
- In Plain mode: just delegates to `PlainRoot`.
- In Advanced mode: a `Scaffold` with a custom rounded tab bar at the bottom (Home / Groups / Verify / Settings).

**User actions**:
- Tab tap → navigates with `popUpTo(start) { saveState = true }; launchSingleTop = true; restoreState = true` (preserves per-tab back stack).
- "Standard view" pill in Plain home → flips `advancedView = true`, calls `GroupRepository.setAdvancedView(true)`.
- Demo banner tap (Plain) → `GroupRepository.exitDemoMode()` + flip to Advanced view.

**Technical implementation**:
- `var advancedView` and `var plainMode` are local state seeded from `GroupRepository`.
- Tab bar visibility is driven by `tabs.any { it.screen.route == route }` — sub-routes (group detail, challenge sheet, etc.) hide the bar.
- Routes: `home`, `groups`, `verify`, `settings`, `onboarding`, `recovery_phrase`, `drills`, `generator`, `recovery_backup`, `safety_cards`, `group/{groupId}`, `qr_display/{groupId}`, `qr_scanner`, `challenge/{groupId}`, `override_reveal/{groupId}`.
- `start = if (groups.isEmpty()) Onboarding else Home` — fresh installs land on Onboarding, repeat installs on Home.

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/navigation/SafewordsNavigation.kt`

---

### OnboardingScreen

**Purpose**: First-run flow. Four entry points: create new group, join with QR, restore from recovery phrase, or try the demo.

**UI surface**: Step-progressed panels:
1. **Welcome** — short product pitch + "Restore from a backup" link
2. **Start** — four `OnboardOption` rows: Create / Join with QR / Restore / Try without a group
3. **Create form** — name + creator name fields, with seed display below
4. (Conditional) **Recovery seed display** during create — 4×6 grid of the just-generated 24-word phrase

**User actions**:
- "Create a new group" → `step = 2`, shows form
- "Join with a QR code" → calls `onJoinWithQR()` → navigates to QR scanner route
- "Restore from a backup" → calls `onJoinWithRecovery()` → navigates to recovery phrase entry route
- "Try without a group" (v1.3.1) → calls `onTryDemo()` → triggers `enterDemoMode()`, navigates to Home with the demo group active
- Form "Create" submit → `GroupRepository.createGroup(name, creatorName)` → if non-null, `onComplete(group.id)` → navigates to Home, popping Onboarding off the back stack

**Technical implementation**:
- Step state `var step` and path state `var path` ("create" only set when user picked Create).
- `PanelStart` is a private composable invoked by the panel switch; it accepts the four callbacks.
- `OnboardOption` is a private composable for the bordered rounded-rectangle option row with icon + title + subtitle.
- Seed generation is handled by `GroupRepository.createGroup` which calls `TOTPDerivation.bytesToHex(generateSeed())` internally.
- Recovery phrase displayed during create is `RecoveryPhraseService.displayCode(for: seed)` (Android calls `RecoveryPhrase.encode(seed)` falling back to hex).

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/onboarding/OnboardingScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/service/GroupRepository.kt:71-89` (createGroup)

---

### RecoveryPhraseScreen

**Purpose**: Restore a group from a 24-word BIP39 phrase or a 64-character hex seed.

**UI surface**:
- Header "Restore from backup"
- Multi-line text input (paste target)
- Helper text explaining accepted formats
- Validation error pills (empty / wrong word count / unknown word / bad checksum / invalid hex)
- "Restore" button (disabled until input parses)

**User actions**:
- Paste/type a phrase → `parseSeedOrPhrase()` runs on submit; returns `ParseResult.Ok(seed)`, `ParseResult.Err(message)`, or `ParseResult.Empty`
- "Restore" → calls `GroupRepository.joinGroup(name, seedHex, interval, memberName)` with synthesized defaults; on success, `onJoined()` → Home

**Technical implementation**:
- `parseSeedOrPhrase(input)` either decodes via `RecoveryPhrase.decode(input)` for word phrases (catches `Bip39.Error.*` exceptions) or runs hex validation for 64-char input
- BIP39 errors map to user-friendly copy via the sealed-class `userMessage` field
- v1.3.1 made the wordlist load defensive — a `MissingWordlist` error here means the asset bundle is broken; we fall back to suggesting hex input

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/onboarding/RecoveryPhraseScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/crypto/RecoveryPhrase.kt`

---

### PlainRoot / PlainHome

**Purpose**: The v1.3 default home. Big-word, big-button layout for stressed/elderly/child users — but now the front door for everyone.

**UI surface**:
- (v1.3.1) Demo banner at top **only when** `demoMode == true`: "DEMO MODE / Set up your real group →" on accent background
- Header: large circular avatar with first letter of group name + "Your circle" / group name + "Standard view" pill in top right
- Hero card: "YOUR WORD TODAY" label + the rotating word (split per word at 48sp) OR the 6-digit numeric code (single line, 56sp tracked)
- Countdown pill: "New word in {HH:MM:SS}" with refresh icon
- Bottom: tab bar with Word / Check / Help (Plain has its own simpler bar)

**User actions**:
- "Standard view" pill tap → `onExitPlain()` → flips `advancedView = true` (sticky)
- Demo banner tap → `onSetupReal()` → `exitDemoMode()` + flip to Advanced
- "Check" tab → switches to `PlainScreen.Verify`
- "Help" tab → `PlainScreen.Help`

**Technical implementation**:
- `PlainRoot` is a self-contained mini app with its own `PlainScreen` enum (Home / Verify / Help / Onboarding) and bottom bar (`PlainTabBar`).
- `PlainHome` reads `GroupRepository.groups`, `activeGroupId`, `demoMode` via `collectAsState()`.
- `LaunchedEffect(g)` runs a 1-second polling loop calling `GroupRepository.getCurrentSafeword(group.id)` (numeric-aware) and `TOTPDerivation.getTimeRemaining(intervalSeconds)`.
- Word format detection: phrase contains a space → render line-per-word; no space → single line with letter-spacing for the numeric path.

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt:75-101` (PlainRoot)
- `app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt:186-405` (PlainHome)
- `app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt:DemoBanner` (bottom of file)
- `app/src/main/kotlin/com/thc/safewords/service/GroupRepository.kt` (getCurrentSafeword)

---

### PlainVerify

**Purpose**: The Plain-mode "Did they say the right thing?" screen. Big yes/no buttons.

**UI surface**:
- Header: "Did they say the word?"
- Two huge buttons: green "Yes — match" and red "No — wrong"
- After tap: result panel directing user to keep going (yes) or hang up + call back on a known number (no)

**User actions**:
- "Yes" tap → match result, "Done" returns to PlainHome
- "No" tap → mismatch result with hang-up guidance, "Done" returns

**Technical implementation**:
- Self-contained composable `PlainVerify(onDone)` with `PlainAsk` and `PlainResult` sub-composables
- No actual cryptographic check — Plain mode trusts the user's eyes; the rotating word on PlainHome is what they verify against
- Distinct from Advanced VerifyScreen which has the C/A primitive flow

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt` (PlainVerify section)

---

### PlainHelp

**Purpose**: Plain-mode help/about screen with the "Exit high visibility" toggle.

**UI surface**: Help text + "Exit high visibility" big button at the bottom.

**User actions**: "Exit high visibility" → `onExitPlain()` → routes to Advanced view.

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt` (PlainHelp section, ~line 527)

---

### HomeScreen (Advanced)

**Purpose**: The standard tabbed UI's home. Shows the current rotating word for the active group with the countdown ring.

**UI surface**:
- Header: SectionLabel "Home" + group selector pill
- Hero: `CountdownRing` with the current word in the center, large display
- Group switcher row when there are multiple groups
- "Share invite" CTA → routes to QR Display

**User actions**:
- Group pill tap → cycles active group via `GroupRepository.setActiveGroup(...)`
- "Share invite" → navigates to `qr_display/{groupId}` route
- Tabs (parent scaffold) → other Advanced tabs

**Technical implementation**:
- Reads `groups`, `activeGroupId` via StateFlow
- 1Hz coroutine ticker for countdown using `TOTPDerivation.getTimeRemaining(intervalSeconds)`
- `CountdownRing` is a custom composable in `ui/components/CountdownRing.kt` — animated arc that drains over the rotation interval

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/ui/components/CountdownRing.kt`

---

### GroupsScreen

**Purpose**: Lists all groups, lets the user manage them.

**UI surface**: Scrollable list of group cards. Each card shows the dot color, group name, member count. Floating action buttons: "Scan QR" + "Add group" (which routes to Onboarding).

**User actions**:
- Card tap → `onGroupClick(id)` → navigates to `group/{groupId}`
- "Scan QR" → QR scanner route
- "Add group" → Onboarding route

**Technical implementation**:
- Reads `GroupRepository.groups` — collectAsState
- Uses `GroupDot` from `ui/components/DesignComponents.kt` for the per-group color circle
- `dotColorFor(groupId)` is a stable hash function so each group has a deterministic color

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/groups/GroupsScreen.kt`

---

### GroupDetailScreen

**Purpose**: Manage one group: rename, change rotation interval, view current word, invite members, delete.

**UI surface**:
- Header: group name (tap to rename inline)
- Current safeword display
- Interval picker (Hourly / Daily / Weekly / Monthly)
- Member list with role tags
- "Invite member" button → QR Display
- "Leave / delete this group" danger zone

**User actions**:
- Inline rename → `GroupRepository.renameGroup(groupId, newName)`
- Interval pick → `GroupRepository.setRotationInterval(groupId, interval)` — also updates `primitives.rotatingWord.intervalSeconds`
- Invite → routes to `qr_display/{groupId}`
- Delete with confirm → `GroupRepository.deleteGroup(groupId)` (special-case for demo group: short-circuits to `exitDemoMode`)

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/groups/GroupDetailScreen.kt`

---

### VerifyScreen (Advanced)

**Purpose**: Verify someone said the right word. v1.3 made this conditional on which primitives the active group has.

**UI surface**:
- Header: "Verify · Are they who they say they are?"
- **If group has no Verify-needing primitive**: empty-state copy "Verify isn't needed for {group} right now — both phones show the same word..."
- **If group has challengeAnswer**: ChallengeAnswerPanel → tap to open ChallengeSheet
- **If group has staticOverride** OR legacy: free-text "Type what they said" input + "Listen mode" alternative

**User actions**:
- Free-text typed → on `Check` tap, compare to `currentWord` (case-insensitive); transition to Match or Mismatch result panel
- "Listen mode" → puts a "Match" / "Mismatch" pair on screen for hands-free
- "Open challenge" tap → `onRunChallenge(group.id)` → routes to `challenge/{groupId}` (ChallengeSheet)
- Result panel "Done" → returns to Ready phase

**Technical implementation**:
- `Phase` enum: Ready / Listening / Match / Mismatch
- `currentWord` derived via `GroupRepository.getCurrentSafeword(g.id)` (numeric-aware)
- `needsVerify = challengeAnswer.enabled || staticOverride.enabled`
- The legacy free-text panel is still wired below the empty state — pre-v1.3 muscle memory, not removed yet

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/verify/VerifyScreen.kt`

---

### ChallengeSheet

**Purpose**: The v1.3 binary "Match / Doesn't match" UX for groups with challenge/answer enabled.

**UI surface**:
- Top bar: close icon + "Verify" title
- "Ask them: {askPhrase}" — large
- "They should answer: {expectPhrase}" — slightly smaller
- Two huge buttons: "They said it" (accent) and "Doesn't match"
- "Use a different row" subtle link below
- After tap: result panel with green check or red X icon + guidance copy + "Done"

**User actions**:
- "They said it" → match phase → "You can keep talking, but stay alert" copy → "Done" closes
- "Doesn't match" → mismatch phase → "Hang up. Call them back on a number you already know" copy → "Done" closes
- "Use a different row" → re-rolls to a random row index in [0, rowCount)

**Technical implementation**:
- `var rowIndex by remember { mutableIntStateOf(Random.nextInt(rowCount)) }` — random row at session start
- `GroupRepository.getChallengeAnswerTable(groupId, rowCount)` returns `List<Primitives.ChallengeAnswerRow>` — table is recomputed every call (cheap, deterministic from seed)
- `Primitives.challengeAnswerRow(seed, tableVersion, rowIndex)` does HMAC twice with separate domain-separated labels for ask/expect
- Result panel uses `ChallengePhase` enum (renamed from `Phase` in commit `fc61512` to avoid Kotlin same-package private-name collision with VerifyScreen's `Phase`)

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/verify/ChallengeSheet.kt`
- `app/src/main/kotlin/com/thc/safewords/crypto/Primitives.kt:challengeAnswerRow`

---

### OverrideRevealScreen

**Purpose**: Biometric-gated reveal of the group's static override word. Word is computed from the seed on demand, never stored.

**UI surface**:
- Top bar: back icon + "Override word" title
- Auth prompt (system biometric sheet) on first paint
- After unlock: warning "Anyone with this word can act as you in this group." + the override word in big type (per-word breakdown) + footer reminder about seed rotation

**User actions**:
- Back → returns to Settings
- The system biometric sheet handles cancel/fail itself

**Technical implementation**:
- `LaunchedEffect(groupId)` runs `BiometricService.canAuthenticate(activity)` then `BiometricService.authenticate(...)` with title "Reveal override"
- On success: `GroupRepository.getStaticOverride(groupId)` returns `Primitives.staticOverride(seed)` — `HMAC-SHA256(seed, "safewords/static-override/v1")` → standard word derivation
- No persistence — word lives in `var word` state until screen leaves the back stack
- If no biometric is enrolled, surfaces "Set a screen lock or biometric to view this." instead of crashing

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/verify/OverrideRevealScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/crypto/Primitives.kt:staticOverride`
- `app/src/main/kotlin/com/thc/safewords/service/BiometricService.kt`

---

### SettingsScreen

**Purpose**: All app preferences and per-group toggles.

**UI surface**: A scrollable column of `SettingsSection` cards:
- **Rotation · {group}** — interval picker, "Notify on rotation", "Include preview of next word"
- **Verification · {group}** (v1.3) — "Show as 6-digit code" (numeric format toggle), "Static override word" enable + "Reveal override word" action, "Challenge / answer table" enable + "Run challenge" action, "Print safety cards"
- **Accessibility** — "High visibility mode" (legacy plainMode toggle)
- **Widget & Lock Screen** — "Lock screen glance", "Hide word until unlock"
- **Security** — "Require biometrics to open", "Emergency override word" (legacy v1.2 setting; superseded by static override primitive but not removed), "Rotate group seed", "Back up seed phrase"
- **Tools** — "Single use word generator"
- **Practice** — "Run a scam drill" + drill history
- **Danger zone** — "Leave this group", "Reset all data"

**User actions** (most representative):
- Interval pick → `setRotationInterval` (also updates primitives)
- "Show as 6-digit code" → `setWordFormat(WordFormat.NUMERIC)` or back to ADJECTIVE_NOUN_NUMBER
- "Static override word" toggle → `setStaticOverrideEnabled(groupId, on)` — purely a config flip; the override word itself is always derivable from the seed
- "Reveal override word" → `onRevealOverride(groupId)` → `OverrideRevealScreen` route
- "Run challenge" → `onRunChallenge(groupId)` → ChallengeSheet route
- "Print safety cards" → `onOpenSafetyCards()` → SafetyCardsScreen route
- "Back up seed phrase" → `onBackupSeedPhrase()` → RecoveryBackup route
- "Leave this group" with confirm → `deleteGroup(groupId)`
- "Reset all data" with confirm → `resetAllData()` — wipes ESP, all seeds, demo flag

**Technical implementation**:
- `var notify by remember { mutableStateOf(GroupRepository.isNotifyOnRotation()) }` etc. — boolean toggles read once on entry, written on each tap
- The "Verification" section's primitives state comes from `active?.primitivesOrDefault()` — handles v1.2 groups without primitives by expanding from legacy `interval`
- The legacy "Emergency override" entry stores a free-text user-entered word in ESP under `emergency_override_{groupId}`; v1.3 static override is a separate, deterministic concept and doesn't write here

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/settings/SettingsScreen.kt`

---

### RecoveryBackupScreen

**Purpose**: Settings → Security → "Back up seed phrase". Shows the active group's 24-word BIP39 phrase.

**UI surface**:
- Auth prompt on entry
- 4×6 grid of indexed words (`01. apple`, `02. boat`, ...)
- "Copy to clipboard" action
- "Done" button returns to Settings

**User actions**:
- Copy → places phrase in clipboard with auto-clear after 60s (Android 13+)
- Done → returns

**Technical implementation**:
- `BiometricService.authenticate(...)` gates entry
- `RecoveryPhrase.encode(seed)` produces the phrase; if it throws `Bip39.Error.MissingWordlist`, fall back to "Recovery word list is unavailable. Use the raw seed: {hex}"
- The phrase is **not** persisted — recomputed every visit

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/settings/RecoveryBackupScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/crypto/RecoveryPhrase.kt`

---

### SafetyCardsScreen

**Purpose**: Browse and print every card available for the active group. v1.3 addition.

**UI surface**:
- Top bar: back icon + "Safety cards"
- Vertical list of `CardRow`s:
  - "Protocol card" (no lock icon — low sensitivity)
  - "Static override card" 🔒 (if `staticOverride.enabled`)
  - "Challenge / answer · wallet" 🔒 + "Challenge / answer · full" 🔒 (if `challengeAnswer.enabled`)
  - "Recovery phrase card" 🔒
  - "Group invite card" 🔒
- Each row: title, subtitle, lock icon for sensitive, "Print" affordance on the right

**User actions**:
- Tap protocol row → renders + prints (no biometric)
- Tap any sensitive row → `gateThen(activity)` runs biometric prompt, then renders + prints
- Tap override row → also calls `markStaticOverridePrinted(groupId)` to record the print

**Technical implementation**:
- `gateThen(activity, action)` checks `BiometricService.canAuthenticate`, falls through if no biometric is enrolled (rationale: blocking the user from their own card is worse than the marginal security gain)
- Each `CardRow` calls a corresponding `CardRenderer.renderXxxCard(...)` returning a `Bitmap`, then `CardRenderer.print(context, bmp, jobName)` routes to `androidx.print.PrintHelper.printBitmap`
- Card text loaded from `/shared/safety-card-copy.json` via `SafetyCardCopy.card(context, key)` + `SafetyCardCopy.str(json, key, vars)` which handles `{groupName}` substitution

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/cards/SafetyCardsScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/print/CardRenderer.kt`
- `app/src/main/kotlin/com/thc/safewords/print/SafetyCardCopy.kt`
- `app/src/main/assets/safety-card-copy.json` (gradle copies from `/shared/`)

---

### QRDisplayScreen

**Purpose**: Show a group invite QR for sharing.

**UI surface**:
- The QR code in a large card
- Group name + caveats below
- "Share via SMS" and "Copy link" actions
- "Done" returns to caller

**User actions**:
- "Share via SMS" → opens `Intent.ACTION_SENDTO` with `sms:` URI prefilled with safewords.io install + invite text via `SmsInviteService`
- Copy link → clipboard
- Auto-dismiss timer (24h) is enforced by the QR payload itself, not the UI

**Technical implementation**:
- `QRCodeService.generateQRBitmap(group, size = 512)` — uses ZXing core to render a `com.google.zxing.qrcode.encoder.QRCode`-derived `BitMatrix`, painted to a Bitmap
- The QR payload follows `/shared/qr-schema.json` v1: a JSON object with the seed (hex), group name, member info, signed by HMAC of the seed itself

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/qr/QRDisplayScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/service/QRCodeService.kt`
- `app/src/main/kotlin/com/thc/safewords/service/SmsInviteService.kt`

---

### QRScannerScreen

**Purpose**: Camera scanner for group invite QRs.

**UI surface**:
- Full-screen camera preview
- Reticle overlay
- Permission rationale screen if `CAMERA` is denied

**User actions**:
- Camera permission granted → MLKit barcode scanner active; on detection, parse, validate, call `joinGroup`
- Permission denied → fall back to "Open settings" link

**Technical implementation**:
- CameraX preview + MLKit barcode-scanning (bundled-model variant; the un-bundled MLKit pulls in Firebase telemetry and adds INTERNET permission, which we can't have — see `app/build.gradle.kts:configurations.all { exclude(...) }`)
- `QRCodeService.parseQRPayload(rawPayload)` returns `ParsedGroup?` — null on invalid payload
- On success: `GroupRepository.joinGroup(name, seedHex, interval, memberName)` → calls `exitDemoMode()` first, then creates the real group
- Lifecycle-aware preview tear-down via `LifecycleOwner` from compose

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/qr/QRScannerScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/service/QRCodeService.kt`
- `app/build.gradle.kts:14-18` (Firebase telemetry exclusion)

---

### DrillsScreen

**Purpose**: "Run a scam drill" — practice flow that generates a fake-call scenario.

**UI surface**: Multi-step prompt + result. Scenarios drawn from `DrillService.scenarios`.

**User actions**: Step through prompts, mark "I would hang up" / "I would verify" — DrillService records pass/fail.

**Technical implementation**:
- `DrillService` is a stateless object with predefined scenarios + a tiny pass-rate stat
- No network, no real generation — just a curated list

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/drills/DrillsScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/service/DrillService.kt`

---

### GeneratorScreen

**Purpose**: Settings → Tools → "Single use word generator". Generates a one-off random adjective+noun+number for ad-hoc use (not tied to any group).

**UI surface**: Big word display + "Regenerate" button.

**User actions**: Regenerate → re-rolls.

**Technical implementation**:
- `WordGenerator.generate()` — pure random pick (not HMAC) since this isn't tied to a seed
- No persistence

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/ui/generator/GeneratorScreen.kt`
- `app/src/main/kotlin/com/thc/safewords/data/WordGenerator.kt`

---

### Widget (Glance app widget)

**Purpose**: Home-screen widget showing the active group's current word + countdown.

**UI surface**:
- Compact layout: group name (small, top), current word (large, center), countdown (small, bottom)
- Tap target spans the whole widget — opens the app

**User actions**:
- Tap → `actionStartActivity(MainActivity launch intent)` — opens to Plain home

**Technical implementation**:
- `SafewordsWidget extends GlanceAppWidget` — uses `androidx.glance.appwidget`
- `provideContent { ... }` reads `GroupRepository.activeGroup()` and `GroupRepository.getCurrentSafeword(...)` directly (Glance composables can call into repository from process scope)
- Updates triggered by:
  1. WorkManager periodic worker (configurable per `notify_on_rotation` pref)
  2. Runtime broadcast receiver in `SafewordsApp.kt` for `ACTION_USER_PRESENT` / `ACTION_SCREEN_ON`
- Widget receiver class is in the `:app` module (was in a separate `:widget` module pre-v1.2; moved because Android couldn't resolve the receiver class across module boundaries — `ClassNotFoundException`)

**Files of record**:
- `app/src/main/kotlin/com/thc/safewords/widget/SafewordsWidget.kt`
- `app/src/main/kotlin/com/thc/safewords/SafewordsApp.kt` (receiver registration)

---

## Cross-cutting

### Theme + design tokens

Everything is in `app/src/main/kotlin/com/thc/safewords/ui/theme/`:
- `Color.kt` — `Ink` (dark theme tokens) and `A11y` (Plain mode tokens) palettes
- `Theme.kt` — `SafewordsTheme()` Compose Material3 theme
- See `docs/design-system.md` for token-by-token table across iOS/Android.

### Storage

| What | Where | Class |
|---|---|---|
| Group metadata (JSON) | EncryptedSharedPreferences key `groups_json` | `GroupRepository` |
| Per-group seed (hex) | ESP via `SecureStorageService` (separate prefs file) | `SecureStorageService` |
| Active group ID | ESP key `active_group_id` | `GroupRepository` |
| All boolean prefs | ESP keys (`plain_mode`, `advanced_view_enabled`, etc.) | `GroupRepository` |
| Demo mode flag | ESP key `demo_mode` | `GroupRepository` |
| Demo seed | **Hardcoded constant** in `GroupRepository.DEMO_SEED_BYTES` — never written | (in-memory only) |

ESP backed by `MasterKeys.AES256_GCM_SPEC` — keys + values both encrypted at rest under hardware-backed Android Keystore.

### Print pipeline

`app/src/main/kotlin/com/thc/safewords/print/`:
- `CardRenderer.kt` — Canvas drawing → ARGB Bitmap → `PrintHelper.printBitmap()`. Letter-size 300dpi for high-sensitivity cards (2550×3300 px), wallet-size 1050×600 for the C/A excerpt.
- `SafetyCardCopy.kt` — loads `/shared/safety-card-copy.json` from app assets, handles `{groupName}` substitution.

### Cross-platform contract

Every primitive must produce byte-identical output on Android and iOS for the same seed/timestamp/row. Verified by:
- `app/src/test/kotlin/com/thc/safewords/crypto/PrimitivesTest.kt` — every `staticOverrideVectors[]` / `numericVectors[]` / `challengeAnswerVectors[]` entry in `/shared/primitive-vectors.json`
- `app/src/test/kotlin/com/thc/safewords/data/GroupSchemaTest.kt` — every migration case in `/shared/migration-vectors.json`
- `app/src/test/kotlin/com/thc/safewords/crypto/Bip39Test.kt` — every entry in `/shared/recovery-vectors.json`
- `app/src/test/kotlin/com/thc/safewords/TOTPDerivationTest.kt` — original v1.0 vectors from `/shared/test-vectors.json`

Gradle's `copySharedToTestResources` task copies every shared JSON file (plus adjectives/nouns) into `app/src/test/resources/` before tests run, so JVM unit tests can load via classpath without needing `SafewordsApp.instance.assets`.

## Build + test surface

Run on `u5` dev VM (the primary VM has no gradle/SDK/keystore):

| Goal | Command (after rsyncing source to u5) |
|---|---|
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Debug APK | `./gradlew :app:assembleDebug` |
| Signed AAB | `fastlane build` |
| Push to Play internal | `fastlane internal` |
| Push to closed/alpha | `fastlane closed` |
| Push to production | `fastlane production` |
| Promote internal → prod | `fastlane promote_to_prod` |
| Listing metadata only | `fastlane metadata` |

See `docs/release-state.md` for the live track snapshot and `docs/release-pipeline-gotchas.md` for failure modes.

## Appendix: deferred features

See `docs/feature-spec.md` "Deferred / future" for the full list. Not implemented on Android (or iOS):
- Pair mode (per-contact rotating C/A) — v1.5+
- Agent Circle — separate repo `safewords-agent`, not consumer app
- Expiring invites (the group invite QR is currently seed-equivalent)
- SMS fallback (modern Android SMS APIs are incompatible with our zero-permission stance — won't ship)
- Push notifications (possible v1.4+, low priority)
- Custom fonts (Fraunces / Atkinson Hyperlegible registration still pending)
