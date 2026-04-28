# iOS v1.1.0 Wire-Status Audit

Project: `repos/safewords-ios`
Phase: 1, read-only audit
Date: 2026-04-28

Status key: ✅ wired, ⚠️ placeholder/incomplete, 🔴 broken, 🪲 design-only, ⏳ deferred.

## Summary

| Area | Status | Notes |
|---|---:|---|
| Standard navigation | ⚠️ | Home/Groups/Verify/Settings/AddMember route, but QR scanner, recovery phrase, drills, group creation, and settings detail routes are absent. |
| Golden onboarding | 🔴 | Onboarding can mark the user onboarded without creating or joining a group. |
| GroupStore core CRUD | ✅ | Group create/join/update/delete methods exist, but most screens do not call them. |
| Keychain seed persistence | 🔴 | Keychain access group is likely wrong for signed iOS builds; save failures are ignored. |
| Settings | 🔴 | Most rows are visual-only; interval and leave-group controls do not touch `GroupStore`. |
| Plain mode round-trip | 🔴 | User can enter Plain mode but has no exit back to standard mode. |
| QR invite | ⚠️ | QR rendering/parsing exists; SMS row is no-op; scanner is unreachable and has retry/permission gaps. |
| Widget | ⚠️ | Reads shared store/keychain, but timeline remaining time is wrong and widget settings rows are not backed. |
| Missing v1.1.0 screens | 🔴 | No `DrillsView.swift` or `RecoveryPhraseView.swift`; no `AppScreen` cases for either. |
| Versioning | ⚠️ | `MARKETING_VERSION` and Info.plist values are still `1.0.0`; build is still `1`. |

Verification note: `xcodebuild` and `xcodegen` are not installed in this environment, so this phase is code audit only. The expected design bundle path `/tmp/design-extract/safewords-app/` is also absent; this report uses repo docs and current source.

## P0 Findings

| Finding | Status | Evidence | Impact |
|---|---:|---|---|
| Keychain queries use `group.app.thc.safewords`, while entitlements grant `$(AppIdentifierPrefix)group.app.thc.safewords`. | 🔴 | `KeychainService.appGroupID` and `kSecAttrAccessGroup`: `repos/safewords-ios/Safewords/Services/KeychainService.swift:9`, `:26`, `:41`, `:62`, `:75`; entitlement value: `repos/safewords-ios/project.yml:49` | `SecItemAdd`/`CopyMatching` can fail on device; seeds may never persist or read. |
| Keychain save failures are ignored. | 🔴 | `GroupStore.createGroup` calls `KeychainService.saveSeed` without checking result: `repos/safewords-ios/Safewords/Services/GroupStore.swift:53`; `joinGroup`: `:71` | UI can create/join groups with missing seeds, producing no safeword. |
| Onboarding does not create or join a group. | 🔴 | `groupStore` is injected but unused: `repos/safewords-ios/Safewords/Views/OnboardingView.swift:4`; final CTA only sets `screen = .home`: `:53`-`:58` | Fresh-install golden path lands in an empty Home after setting `onboarded = true`. |
| All onboarding choices collapse to the same mock seed panel. | 🔴 | Create, Join QR, and Join recovery rows are rendered at `repos/safewords-ios/Safewords/Views/OnboardingView.swift:142`-`:157`; each `onboardOption` only does `step += 1`: `:173`-`:174` | Create-group, QR join, and recovery-phrase flows are not functional. |
| App routing has no scanner, recovery, drill, or group-create destinations. | 🔴 | `ContentView` switch only maps home/groups/verify/settings/onboarding/addMember: `repos/safewords-ios/Safewords/App/ContentView.swift:28`-`:35`; enum cases only include those screens: `repos/safewords-ios/Safewords/Views/HomeView.swift:186`-`:189` | Existing `QRScannerView` is unreachable; required v1.1.0 flows cannot be launched. |
| Plain mode has no exit path. | 🔴 | `ContentView` swaps to `PlainRoot` when `plainMode` is true: `repos/safewords-ios/Safewords/App/ContentView.swift:5`, `:10`-`:11`; Plain tabs are only Word/Check/Help: `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:63`-`:67`; Help buttons are empty: `:409`-`:437` | User can enter High visibility mode and cannot return to standard Settings. |
| Settings interval picker does not update the selected group. | 🔴 | Local state only: `repos/safewords-ios/Safewords/Views/SettingsView.swift:7`; buttons set only `selectedInterval`: `:118`-`:130`; `GroupStore.updateGroupInterval` exists but is not called: `repos/safewords-ios/Safewords/Services/GroupStore.swift:90`-`:95` | Rotation setting appears changed but persists nothing and Home countdown does not update. |
| "Leave this group" is a no-op. | 🔴 | Row has no action: `repos/safewords-ios/Safewords/Views/SettingsView.swift:54`-`:57`; `dangerRow` only calls optional `action?()`: `:177`-`:188` | Required group removal flow is absent. |
| QR scanner invalid scan stops scanning without recovery. | 🔴 | Camera sets `hasScanned = true` and stops before parsing: `repos/safewords-ios/Safewords/Views/QRScannerView.swift:235`-`:249`; invalid parse only sets `scanError`: `:133`-`:140` | One bad/non-Safewords QR bricks the scanner screen until dismissal. |
| QR scanner has no permission request/denied state and is unreachable. | 🔴 | Camera setup silently returns on input failure: `repos/safewords-ios/Safewords/Views/QRScannerView.swift:192`-`:199`; no route from `ContentView`: `repos/safewords-ios/Safewords/App/ContentView.swift:28`-`:35` | Join via QR cannot be completed from onboarding. |

## Standard Screens

### ContentView / Navigation

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Plain-mode root switch | ✅ | `repos/safewords-ios/Safewords/App/ContentView.swift:5`, `:10`-`:13` | `@AppStorage("plainMode")` swaps entire root. |
| Main tab routing | ✅ | `repos/safewords-ios/Safewords/App/ContentView.swift:28`-`:35` | Four tabs plus onboarding/addMember. |
| Tab bar hiding | ✅ | `repos/safewords-ios/Safewords/App/ContentView.swift:39`-`:50` | Hidden during onboarding and QR display. |
| Missing destinations | 🔴 | `repos/safewords-ios/Safewords/Views/HomeView.swift:186`-`:189` | No scanner, recovery phrase, drills, group detail, or create-group route. |

### HomeView

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Live safeword/countdown | ✅ | `repos/safewords-ios/Safewords/Views/HomeView.swift:13`-`:20`, `:27`-`:29` | Uses selected group and `TimelineView`. |
| Group pill navigation | ✅ | `repos/safewords-ios/Safewords/Views/HomeView.swift:34`-`:36` | Routes to Groups. |
| Hold-to-reveal | ✅ | `repos/safewords-ios/Safewords/Views/HomeView.swift:6`, `:93`-`:95`, `:137`-`:144` | Backing setting exists, but Settings has no UI to change it. |
| Bell button | ⚠️ | `repos/safewords-ios/Safewords/Views/HomeView.swift:58`-`:68` | Empty action. |
| Empty-state create CTA | ⚠️ | `repos/safewords-ios/Safewords/Views/HomeView.swift:146`-`:164` | Routes to onboarding, but onboarding cannot create a group. |
| `next: •••••` text | 🪲 | `repos/safewords-ios/Safewords/Views/HomeView.swift:122`-`:127` | Hard-coded placeholder. |

### OnboardingView

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Carousel UI | ✅ | `repos/safewords-ios/Safewords/Views/OnboardingView.swift:17`-`:75` | Visual panels and back/CTA work. |
| Create a new group | 🔴 | `repos/safewords-ios/Safewords/Views/OnboardingView.swift:143`-`:147`, `:173`-`:174` | Advances to seed panel only; no form and no `createGroup`. |
| Join with QR | 🔴 | `repos/safewords-ios/Safewords/Views/OnboardingView.swift:148`-`:152`, `:173`-`:174` | Advances to seed panel only; does not present scanner. |
| Join with recovery phrase | 🔴 | `repos/safewords-ios/Safewords/Views/OnboardingView.swift:153`-`:157`, `:173`-`:174` | No recovery entry screen or parser. |
| Seed backup panel | ⚠️ | `repos/safewords-ios/Safewords/Views/OnboardingView.swift:8`-`:11`, `:224`-`:239` | Shows hard-coded sample words, not the generated seed or a real recovery phrase. |
| Completion | 🔴 | `repos/safewords-ios/Safewords/Views/OnboardingView.swift:53`-`:58`; `repos/safewords-ios/Safewords/App/ContentView.swift:23`-`:26` | Marks onboarded by navigating home, even when no group exists. |

### GroupsView

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Groups list | ✅ | `repos/safewords-ios/Safewords/Views/GroupsView.swift:15`-`:18` | Renders stored groups. |
| Group card selection | ✅ | `repos/safewords-ios/Safewords/Views/GroupsView.swift:73`-`:77` | Sets `selectedGroupID` and routes Home. |
| Add/invite button | ⚠️ | `repos/safewords-ios/Safewords/Views/GroupsView.swift:59`-`:60`; `repos/safewords-ios/Safewords/App/ContentView.swift:34` | Opens QRDisplay for current group. It does not create a second group as required by the v1.1.0 checklist. |
| Member rows | 🪲 | `repos/safewords-ios/Safewords/Views/GroupsView.swift:137`-`:174` | Device labels and "SYNCED" status are fake/static. |
| Group management | ⚠️ | `repos/safewords-ios/Safewords/Views/GroupsView.swift:73`-`:77` | No rename/delete/detail/member-management flow. |

### VerifyView

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Typed verification | ✅ | `repos/safewords-ios/Safewords/Views/VerifyView.swift:184`-`:190` | Compares trimmed/lowercased input against selected group's current safeword. |
| Result states | ✅ | `repos/safewords-ios/Safewords/Views/VerifyView.swift:23`-`:24`, `:199`-`:255` | Match/mismatch cards reset correctly. |
| No-group handling | 🔴 | `repos/safewords-ios/Safewords/Views/VerifyView.swift:184`-`:185` | Falls back to a dummy `Group(name: "")`; if no seed exists, tapping Check does nothing. |
| Empty Check state | ⚠️ | `repos/safewords-ios/Safewords/Views/VerifyView.swift:61`-`:68` | Looks disabled when empty but button still fires. |
| Mic/listening flow | ⚠️ | `repos/safewords-ios/Safewords/Views/VerifyView.swift:70`, `:123`-`:168` | Demo-only; match/mismatch buttons are explicitly labeled demo at `:160`-`:162`. |
| Emergency override tip | ⚠️ | `repos/safewords-ios/Safewords/Views/VerifyView.swift:91`-`:94` | Copy references override word, but Settings/store do not back one. |

### QRDisplayView

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Back button | ✅ | `repos/safewords-ios/Safewords/Views/QRDisplayView.swift:29`-`:41` | Routes back to Groups. |
| QR rendering | ✅ | `repos/safewords-ios/Safewords/Views/QRDisplayView.swift:53`-`:67`; service: `repos/safewords-ios/Safewords/Services/QRCodeService.swift:28`-`:55` | Real QR generation if seed can be read. |
| SMS invite | ⚠️ | `repos/safewords-ios/Safewords/Views/QRDisplayView.swift:107`-`:130` | Button action is empty. |
| Invite lifecycle | ⚠️ | `repos/safewords-ios/Safewords/Views/QRDisplayView.swift:7`-`:27` | No expiry/auto-dismiss timer or "invite sent" outcome. |

### QRScannerView

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Scanner camera view | ✅ | `repos/safewords-ios/Safewords/Views/QRScannerView.swift:47`-`:52`, `:157`-`:220` | AVFoundation QR metadata session is implemented. |
| QR parse + join | ✅ | `repos/safewords-ios/Safewords/Views/QRScannerView.swift:133`-`:151`; parser: `repos/safewords-ios/Safewords/Services/QRCodeService.swift:60`-`:78` | Internal scan path can create a group. |
| App reachability | 🔴 | `repos/safewords-ios/Safewords/App/ContentView.swift:28`-`:35` | No route or sheet presents this view. |
| Invalid QR retry | 🔴 | `repos/safewords-ios/Safewords/Views/QRScannerView.swift:235`-`:249`, `:133`-`:140` | Invalid scan stops the session and leaves no rescan control. |
| Camera permission state | 🔴 | `repos/safewords-ios/Safewords/Views/QRScannerView.swift:192`-`:199` | No `AVCaptureDevice.requestAccess`, no denied/restricted UI. |
| Post-join navigation | ⚠️ | `repos/safewords-ios/Safewords/Views/QRScannerView.swift:143`-`:151`; `repos/safewords-ios/Safewords/Services/GroupStore.swift:76`-`:78` | Dismisses only; joining a second group does not make it active. |

## Settings

| Row/control | Status | Evidence | Notes |
|---|---:|---|---|
| High visibility mode | ✅ | `repos/safewords-ios/Safewords/Views/SettingsView.swift:26`-`:28` | Toggle persists `plainMode` and reroutes via root. |
| Rotation interval | 🔴 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:112`-`:134` | Changes local state only; not persisted to selected group. |
| Notify on rotation | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:21`, row helper `:146`-`:164` | Static row with chevron, no action/backing pref. |
| Include preview of next word | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:23`, row helper `:146`-`:164` | Static row with chevron, no action/backing pref. |
| Home screen widget | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:30`-`:35`, row helper `:146`-`:164` | Static installed/on/off labels. |
| Lock screen glance | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:33`, row helper `:146`-`:164` | No lock-screen/widget setting exists. |
| Hide word until unlock | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:35`, row helper `:146`-`:164` | No backing store and widget ignores it. |
| Require Face ID to open | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:39`, row helper `:146`-`:164` | No LocalAuthentication integration. |
| Emergency override word | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:41`, row helper `:146`-`:164` | No model/store support. |
| Rotate group seed | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:43`, row helper `:146`-`:164` | No action; would need seed replacement + QR/recovery consequences. |
| Back up seed phrase | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:45`, row helper `:146`-`:164` | No recovery phrase display/export. |
| Run a scam drill | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:48`-`:52`, row helper `:146`-`:164` | No `DrillsView` or route. |
| Drill history | 🪲 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:51`, row helper `:146`-`:164` | Static "6 passed". |
| Leave this group | 🔴 | `repos/safewords-ios/Safewords/Views/SettingsView.swift:55`, `:177`-`:188` | No action. |
| Reset device | ✅ | `repos/safewords-ios/Safewords/Views/SettingsView.swift:57`, `:74`-`:79`, `:191`-`:195` | Deletes groups and seeds, subject to keychain access-group issue. |
| Footer version | ⚠️ | `repos/safewords-ios/Safewords/Views/SettingsView.swift:60`-`:63` | Hard-coded `v1.0`. |

## Plain Mode

| Surface | Status | Evidence | Notes |
|---|---:|---|---|
| Plain root/onboarding | ✅ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:528`-`:545`; onboarding `:441`-`:524` | Uses separate `plainOnboarded` flow. |
| Plain Home word display | ✅ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:109`-`:115`, `:125`-`:126` | Same derivation/store as standard mode. |
| Plain Home "Someone is calling me" CTA | 🔴 | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:151`-`:196` | Required CTA is absent; user must use tab bar to reach Check. |
| Plain Home no-group state | ⚠️ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:117`-`:120` | "Pick a circle first." has no route to create/join/pick a circle. |
| Plain Verify yes/no flow | ✅ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:214`-`:229`, `:268`-`:270`, `:304`-`:349` | Big-button result flow works by user judgment. |
| Trusted-number CTA | ⚠️ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:223`-`:227`, `:344`-`:347` | Secondary action is empty. |
| Plain Help items | ⚠️ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:357`-`:363`, `:409`-`:437`; docs say these were future stubs: `docs/plain-mode.md:130`-`:133` | Current v1.1.0 scope needs at least exit/settings and practical drill/help behavior. |
| Emergency 911 callout | ✅ | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:385`-`:400` | Visible static callout. |
| Exit high visibility | 🔴 | `repos/safewords-ios/Safewords/Views/PlainModeViews.swift:63`-`:67`, `:409`-`:437` | No control toggles `plainMode = false`. |

## Services And Data

| Service/API | Status | Evidence | Notes |
|---|---:|---|---|
| Create group | ✅ | `repos/safewords-ios/Safewords/Services/GroupStore.swift:46`-`:63` | Generates seed, creates creator member, persists group. |
| Join group | ✅ | `repos/safewords-ios/Safewords/Services/GroupStore.swift:65`-`:81` | Persists scanned seed and member. |
| Update name/interval/member/delete | ✅ | `repos/safewords-ios/Safewords/Services/GroupStore.swift:83`-`:120` | Methods exist, but settings/group screens rarely call them. |
| Selected group persistence | ✅ | `repos/safewords-ios/Safewords/Services/GroupStore.swift:13`-`:29`, `:169`-`:175` | Persists selected ID in App Group defaults. |
| Join active selection | ⚠️ | `repos/safewords-ios/Safewords/Services/GroupStore.swift:76`-`:78` | New joined group becomes active only if no group was selected. |
| Current safeword | ✅ | `repos/safewords-ios/Safewords/Services/GroupStore.swift:124`-`:148` | Uses Keychain seed + TOTP. |
| Preferences/drills/recovery/SMS support | 🔴 | `repos/safewords-ios/Safewords/Services/GroupStore.swift:31`-`:36`, `:150`-`:177` | Store only persists groups and selected group; no backing model for most Settings rows. |
| QR encode/decode | ✅ | `repos/safewords-ios/Safewords/Services/QRCodeService.swift:11`-`:23`, `:28`-`:55`, `:60`-`:78` | Raw JSON payload with base64url seed matches current Android service shape. |
| QR schema strictness | ⚠️ | `shared/qr-schema.json:6`-`:30`; parser `repos/safewords-ios/Safewords/Services/QRCodeService.swift:60`-`:78` | Parser validates version/seed/interval, but does not reject extra fields or enforce name length. |
| Recovery phrase codec | 🔴 | `repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift:116`-`:149` | Only random seed generation and hex helper exist; no mnemonic/recovery phrase encode/decode. |
| SMS invite service | 🔴 | `repos/safewords-ios/Safewords/Views/QRDisplayView.swift:107`-`:130`; feature spec expects iOS composer: `docs/feature-spec.md:139`-`:152` | No `MFMessageComposeViewController` bridge or `sms:` fallback. |

## Widget

| Element | Status | Evidence | Notes |
|---|---:|---|---|
| Widget target/root | ✅ | `repos/safewords-ios/SafewordsWidget/SafewordsWidgetBundle.swift:4`-`:8`; `repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:248`-`:258` | Small + medium static configuration. |
| Shared group/seed loading | ✅ | `repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:136`-`:157`, keychain `:334`-`:351` | Reads selected group from App Group defaults and seed from keychain. Subject to same access-group concern. |
| Timeline current remaining | 🔴 | `repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:79`-`:100` | First timeline entry uses `periodStart` and computes remaining from `periodStart`, so it displays a full interval rather than actual remaining time. |
| Snapshot current entry | ✅ | `repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:114`-`:133` | Uses `now` and actual remaining time. |
| Widget settings rows | 🪲 | Settings rows: `repos/safewords-ios/Safewords/Views/SettingsView.swift:30`-`:36`; widget view ignores prefs: `repos/safewords-ios/SafewordsWidget/SafewordsWidget.swift:173`-`:243` | Installed/lock-screen/hide-until-unlock labels are not backed. |

## Project And Missing Files

| Item | Status | Evidence | Notes |
|---|---:|---|---|
| iOS version bump | ⚠️ | `repos/safewords-ios/project.yml:14`-`:15`, app Info values `:37`-`:38`, widget Info values `:71`-`:72` | Still `1.0.0` / `1`; v1.1.0 requires `MARKETING_VERSION = 1.1.0`, `CURRENT_PROJECT_VERSION = 3`. |
| Drills screen | 🔴 | `rg --files repos/safewords-ios` found no `DrillsView.swift`; route enum lacks drill case: `repos/safewords-ios/Safewords/Views/HomeView.swift:186`-`:189` | Required by Settings "Run a scam drill". |
| Recovery phrase screen | 🔴 | `rg --files repos/safewords-ios` found no `RecoveryPhraseView.swift`; onboarding recovery row is placeholder: `repos/safewords-ios/Safewords/Views/OnboardingView.swift:153`-`:157` | Required by onboarding join path and seed backup. |
| Group detail screen | ⚠️ | Current tree has no active `GroupDetailView.swift`; group cards only select: `repos/safewords-ios/Safewords/Views/GroupsView.swift:73`-`:77` | Rename/delete/member management must happen elsewhere or be restored. |

## Recommended Phase 3 Fix Order

1. Fix keychain access-group handling and make `GroupStore` surface seed save/read failures.
2. Replace onboarding with real create/join branching: create form, QR scanner route, recovery phrase route, and actual `onboarded` completion only after a group exists.
3. Expand `AppScreen`/routing for QR scanner, recovery phrase, drills, and any group-management screens or sheets.
4. Wire Settings rows to real store/service logic or remove/defer rows that cannot be honestly implemented for v1.1.0.
5. Add Plain Help "Exit high visibility" and Plain Home "Someone is calling me" CTA.
6. Repair QR scanner permission, invalid-scan retry, and post-join navigation.
7. Add SMS invite composer/fallback and drill flow.
8. Fix widget timeline remaining time and hide/unlock settings behavior.
9. Bump iOS version/build after functional fixes land.
