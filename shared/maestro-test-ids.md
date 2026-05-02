# Maestro Test ID Registry

This is the **canonical accessibility-tag registry** both apps must implement. Maestro flows reference these IDs by name; the IDs must be byte-identical across iOS and Android so a single YAML flow can target either platform.

**Status**: v1 spec, draft. Until both platforms implement, Maestro flows can't run.

## Naming convention

`{screen-or-feature}.{element-purpose}` in lowercase kebab-case. Examples:
- `plain-home.word-display`
- `onboarding.create-cta`
- `challenge-sheet.match-button`

Keep IDs stable. Renaming an ID is a breaking change for every flow that references it.

## Implementation

**iOS (SwiftUI)**:
```swift
Text(currentWord)
    .accessibilityIdentifier("plain-home.word-display")
```

**Android (Compose)**:
```kotlin
Text(
    text = currentWord,
    modifier = Modifier.testTag("plain-home.word-display"),
)
```

For Compose, ensure `semantics { testTagsAsResourceId = true }` is set at the root so `testTag` becomes a Maestro-discoverable resource ID. Android Maestro selectors use `id:` prefix for these.

## Registry

### Onboarding

| ID | Purpose | iOS file | Android file |
|---|---|---|---|
| `onboarding.welcome-cta` | "Get started" button on welcome panel | `OnboardingView.swift` welcome panel CTA | `OnboardingScreen.kt` PanelWelcome CTA |
| `onboarding.create-cta` | "Create a new group" option on Start panel | `OnboardingView.swift` Start panel | `OnboardingScreen.kt:PanelStart` first OnboardOption |
| `onboarding.join-qr-cta` | "Join with a QR code" option | same | same |
| `onboarding.restore-cta` | "Restore from a backup" / "Join with recovery phrase" option | same | same |
| `onboarding.demo-cta` | "Try without a group" / "Look around first" option (v1.3.1) | same | same |
| `onboarding.create-form.group-name` | Group name text field | OnboardingView Create form | `PanelCreateForm` group name field |
| `onboarding.create-form.creator-name` | Creator name text field | same | same |
| `onboarding.create-form.submit` | "Generate recovery phrase" / "Create group" button | same | same |
| `onboarding.seed-display.create-button` | Final "Create group" button after seed display | OnboardingView seed display CTA | OnboardingScreen seed step CTA |
| `onboarding.seed-display.warning` | Warning copy under seed grid | same | same |

### Recovery phrase entry

| ID | Purpose |
|---|---|
| `recovery-phrase.input` | The TextEditor / TextField for pasting phrase or seed |
| `recovery-phrase.group-name` | Group name field |
| `recovery-phrase.member-name` | Member name field |
| `recovery-phrase.submit` | "Join group" / "Restore" button |
| `recovery-phrase.error` | Error pill / card when parsing fails |

### Plain Home

| ID | Purpose |
|---|---|
| `plain-home.word-display` | The big rotating word OR numeric code (one ID, content varies) |
| `plain-home.countdown` | Countdown pill ("New word in X") |
| `plain-home.group-name` | Group name in header |
| `plain-home.gear-button` | Gear / "Standard view" pill that exits to Advanced |
| `plain-home.demo-banner` | "DEMO MODE / Set up your real group →" banner (only when in demo mode) |
| `plain-home.challenge-cta` | "Challenge someone" button (only when challengeAnswer enabled) |
| `plain-home.tab-word` | Plain tab bar — Word tab |
| `plain-home.tab-check` | Plain tab bar — Check tab (conditional on primitives) |
| `plain-home.tab-help` | Plain tab bar — Help tab |

### Plain Verify

| ID | Purpose |
|---|---|
| `plain-verify.match-yes` | "Yes, it matched" / "Yes — match" big button |
| `plain-verify.match-no` | "No, wrong word" / "No — wrong" big button |
| `plain-verify.result-safe` | "Safe to talk" result panel |
| `plain-verify.result-hangup` | "Hang up now" result panel |
| `plain-verify.done` | "Done" / "All done" button on result panel |

### Plain Help

| ID | Purpose |
|---|---|
| `plain-help.exit` | "Open Advanced View" / "Exit high visibility" button |
| `plain-help.emergency` | Emergency 911 card |
| `plain-help.text-size` | "Change text size" card (iOS — opens iOS Settings) |

### Advanced — Home

| ID | Purpose |
|---|---|
| `home.word-display` | Current word/code in the Advanced hero |
| `home.countdown-ring` | The CountdownRing composable container |
| `home.countdown-text` | The HH:MM:SS countdown text |
| `home.group-pill` | Group selector pill |
| `home.bell` | Bell button (iOS) / settings shortcut |
| `home.empty-create-cta` | "Create a group" CTA in empty state |

### Advanced — Groups

| ID | Purpose |
|---|---|
| `groups.add-button` | Plus / add group FAB |
| `groups.scan-button` | Scan QR FAB (Android specific) |
| `groups.card.<groupId>` | Per-group card; substitute group ID |
| `groups.invite-cta` | Invite-someone CTA (iOS combines with detail) |

### Advanced — Group Detail (Android only)

| ID | Purpose |
|---|---|
| `group-detail.name-edit` | Inline rename field |
| `group-detail.interval-picker` | Interval row |
| `group-detail.invite-cta` | Invite member button |
| `group-detail.danger-leave` | "Leave / delete this group" button |

### Advanced — Verify

| ID | Purpose |
|---|---|
| `verify.empty-state` | "Verify isn't needed for {group}..." copy |
| `verify.text-input` | Free-text "Type what they said" field |
| `verify.check-button` | "Check" button |
| `verify.listen-button` | Microphone / listen button |
| `verify.challenge-cta` | "Open challenge" / "Challenge someone" panel button |
| `verify.result-match` | Match result card |
| `verify.result-mismatch` | Mismatch result card |

### ChallengeSheet

| ID | Purpose |
|---|---|
| `challenge.row-label` | "Row N of M" stepper label (iOS only) |
| `challenge.row-prev` / `challenge.row-next` | Stepper minus/plus (iOS) |
| `challenge.ask-phrase` | "Ask: {phrase}" |
| `challenge.expect-phrase` | "Expect: {phrase}" |
| `challenge.match-yes` | "They said it" button |
| `challenge.match-no` | "Doesn't match" button |
| `challenge.reroll` | "Use a different row" link (Android) |
| `challenge.show-table` | "Show full table" button (iOS) |
| `challenge.full-table` | Full revealed table container |
| `challenge.done` | "Done" button on result panel |

### Settings

| ID | Purpose |
|---|---|
| `settings.section-rotation` | Rotation section header |
| `settings.section-verification` | Verification section (v1.3) |
| `settings.section-security` | Security section |
| `settings.section-danger` | Danger zone section |
| `settings.toggle-numeric` | "Show as 6-digit code" toggle |
| `settings.toggle-static-override` | Static override toggle |
| `settings.toggle-challenge-answer` | Challenge / answer toggle |
| `settings.toggle-plain-mode` | Plain Mode default toggle (iOS) / High visibility mode (Android) |
| `settings.toggle-biometrics` | Require biometrics |
| `settings.action-reveal-override` | Reveal override word row |
| `settings.action-run-challenge` | Run challenge row |
| `settings.action-safety-cards` | Print safety cards row |
| `settings.action-recovery-backup` | Back up seed phrase row |
| `settings.action-rotate-seed` | Rotate group seed row |
| `settings.action-leave-group` | Leave this group row |
| `settings.action-reset-data` | Reset all data row |
| `settings.action-drill` | Run a scam drill row |
| `settings.action-generator` | Single-use word generator row (Android only) |

### PrimitiveSettingsSheet (iOS only)

| ID | Purpose |
|---|---|
| `primitives-sheet.toggle-numeric` | Numeric word format toggle |
| `primitives-sheet.toggle-static-override` | Static override toggle |
| `primitives-sheet.toggle-challenge-answer` | Challenge / answer toggle |
| `primitives-sheet.done` | Done button |

### Override reveal (Android only)

| ID | Purpose |
|---|---|
| `override-reveal.word` | The revealed override word |
| `override-reveal.warning` | "Anyone with this word..." copy |
| `override-reveal.back` | Back button |

### Recovery backup

| ID | Purpose |
|---|---|
| `recovery-backup.unlock` | "Unlock to reveal" button (iOS) — Android auto-prompts on entry |
| `recovery-backup.word.<index>` | Per-word grid cells, index 1-24 (e.g., `recovery-backup.word.01`) |
| `recovery-backup.copy` | Copy to clipboard button |
| `recovery-backup.back` | Back button |

### Safety Cards

| ID | Purpose |
|---|---|
| `safety-cards.row.protocol` | Protocol card row |
| `safety-cards.row.static-override` | Static override card row |
| `safety-cards.row.challenge-wallet` | Challenge wallet card row |
| `safety-cards.row.challenge-protocol` | Challenge full protocol card row |
| `safety-cards.row.recovery` | Recovery phrase card row |
| `safety-cards.row.invite` | Group invite card row |
| `safety-cards.print` | Print button (iOS) |
| `safety-cards.share` | Share button (iOS) |
| `safety-cards.preview` | Preview area (iOS) |

### QR Display

| ID | Purpose |
|---|---|
| `qr-display.qr` | QR code image |
| `qr-display.sms-cta` | "Invite via SMS" button |
| `qr-display.copy-link` | Copy link button (Android) |
| `qr-display.done` | Done / back |

### QR Scanner

| ID | Purpose |
|---|---|
| `qr-scanner.preview` | Camera preview |
| `qr-scanner.permission-cta` | Enable camera button |
| `qr-scanner.recovery-fallback` | "Use recovery code" link |
| `qr-scanner.cancel` | Cancel button |

### Drills

| ID | Purpose |
|---|---|
| `drills.start` | "Run drill now" button |
| `drills.scenario` | Scenario copy area |
| `drills.passed` | "They knew it" button |
| `drills.failed` | "They failed" button |
| `drills.history-row.<n>` | Per-history-entry rows |

### Generator (Android only)

| ID | Purpose |
|---|---|
| `generator.word-display` | Big generated word |
| `generator.regenerate` | Regenerate button |
| `generator.back` | Back button |

### Widget

Widgets aren't testable via Maestro (they're outside the app's accessibility tree). Skip.

---

## Adding new IDs

When you ship a new screen or surface:
1. Add the ID to this file under the appropriate section
2. Implement on both platforms simultaneously, using the agreed string
3. Reference in any Maestro flow that needs to interact with that element
4. Update `docs/testing.md` if the new flow shape needs explanation

Don't ship a flow that targets an ID not in this registry — it will silently break the cross-platform contract.
