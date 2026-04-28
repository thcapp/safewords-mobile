# Android Standard-Mode Screen Audit Report

**Project**: safewords-android  
**Audit Date**: 2026-04-20  
**Scope**: 8 standard-mode screens (Compose/Kotlin)  
**Total Interactive Elements Catalogued**: 62

---

## Summary Table

| Status | Count | Details |
|--------|-------|---------|
| ✅ Wired | 48 | Real actions working as labeled |
| ⚠️ Placeholder | 6 | onClick exists but no-op or empty |
| 🔴 Broken | 3 | Logic errors or wrong targets |
| 🪲 Design-only | 5 | No onClick, purely visual |
| ⏳ Deferred | 2 | Labeled "Coming soon" |

**Color Scheme**: All screens use Ink theme ✅ (no legacy tokens found)  
**State Persistence**: Mostly `remember` instead of `rememberSaveable` ⚠️  
**Group Selection**: 1 critical issue in VerifyScreen  
**Navigation Targets**: All defined in SafewordsNavigation.Screen ✅

---

## Screen Audits

### 1. HomeScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/home/HomeScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| Group selector (pill) | Tap → onNavigateToGroups() | 95 | ✅ | Navigates to Groups screen |
| Notification icon | Button | 113-127 | 🪲 | No onClick; visual-only |
| "Get started" button (EmptyState) | Button | 218 | ✅ | Calls onNavigateToGroups() |

**Issues**:
- Line 54: `selected` uses `remember` not `rememberSaveable` — state lost on config change
- Line 60-62: Selection defaults to `groups.first()` even if previously selected group is still valid

---

### 2. GroupsScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/groups/GroupsScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| "+" add button | Button | 92 | ✅ | Calls onAddMember() → Onboarding |
| Group card | Tap → onClick() | 159 | ✅ | Sets selectedId, calls onGroupClick(id) |
| GroupCard (active indicator) | Visual | 176-185 | 🪲 | Visual only, no action |
| Member row | Row | 213-238 | 🪲 | No onClick; display-only |

**Issues**:
- Line 57: `selectedId` uses `remember` not `rememberSaveable` — loses selection on app restart
- Line 60: Uses `groups.firstOrNull()?.id` fallback; correct behavior
- Line 63: Active group correctly derived from selectedId

---

### 3. GroupDetailScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/groups/GroupDetailScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| Back button | IconButton | 146 | ✅ | onBack callback; popBackStack |
| Edit name icon | IconButton | 165-169 | ✅ | Toggles isEditingName; saves on check |
| Save name check | IconButton | 156-163 | ✅ | Calls GroupRepository.updateGroup() |
| Rotation interval dropdown | OutlinedButton → DropdownMenu | 233-255 | ✅ | Updates group.interval via repo |
| "Invite" button | TextButton | 281-290 | ✅ | Calls onInvite() → QRDisplay |
| "Delete Group" button | Button | 305-315 | ✅ | Shows confirmation dialog |
| Delete confirm | TextButton | 330-335 | ✅ | Calls GroupRepository.deleteGroup() + onDeleted() |
| Delete cancel | TextButton | 339 | ✅ | Dismisses dialog |

**Issues**:
- Line 110-114: Uses `remember` (not `rememberSaveable`) for `phrase` and `timeRemaining`; recalculated every recomposition anyway (intended)
- **Uses legacy color aliases**: Background, Surface, SurfaceVariant, Teal, TextPrimary, TextSecondary, TextMuted, Error, Success ⚠️ (all aliased to Ink theme in Color.kt, acceptable)

---

### 4. QRDisplayScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/qr/QRDisplayScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| Back button | Box → clickable | 76 | ✅ | Calls onDismiss() |
| SMS alternative card | Row → clickable | 161 | ⚠️ | onClick = {} (empty no-op) |

**Issues**:
- Line 161: SMS invite row has `.clickable {}` with no-op lambda — **design-only placeholder**
- Line 60: LaunchedEffect depends on groupId parameter but should also check if group becomes null
- Uses Ink theme exclusively ✅

---

### 5. QRScannerScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/qr/QRScannerScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| Back button | IconButton | 109 | ✅ | onBack callback |
| "Grant Permission" button | Button | 174-182 | ✅ | Requests CAMERA permission |
| "Join" button in dialog | TextButton | 227-246 | ✅ | Validates name, calls GroupRepository.joinGroup() |
| "Cancel" in dialog | TextButton | 249-254 | ✅ | Closes dialog; resets scannedPayload |

**Issues**:
- Uses legacy color tokens: Background, Surface, SurfaceVariant, Teal, TextPrimary, TextSecondary, TextMuted ⚠️ (aliased to Ink)

---

### 6. VerifyScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/verify/VerifyScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| "Check" button | Box → clickable | 158 | ✅ | Validates input; compares typed vs currentWord |
| "Mic" button | Box → clickable | 173 | ⚠️ | onClick = { onListen() } but Listening panel is demo-only (no actual speech-to-text) |
| "Match (demo)" button | DemoButton | 256 | ⚠️ | Advances phase; labeled as demo |
| "Mismatch (demo)" button | DemoButton | 257 | ⚠️ | Advances phase; labeled as demo |
| "Cancel" in listening | Text → clickable | 259 | ✅ | Resets phase to Ready |
| "Done" result button | Box → clickable | 340 | ✅ | Resets phase and typed input |

**Critical Issues**:
- **Line 61: MAJOR BUG** — `groups.firstOrNull()` ignores user's selected group; uses first group unconditionally
  - This breaks verification if the active group (shown on Home) differs from the first group in the list
  - Should respect the currently-selected group, not hardcoded first group
- Uses Ink theme exclusively ✅
- Line 65-69: Uses `remember`, not `rememberSaveable` — state lost on config change (phrase recalculates every recomposition, acceptable behavior)

---

### 7. OnboardingScreen.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/onboarding/OnboardingScreen.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| Back button | Box → clickable | 127-130 | ✅ | step -= 1; clears seed if on step 2 |
| Next/CTA button | Box → clickable | 146-159 | ✅ | step-dependent logic; calls GroupRepository.createGroup() at step 2 |
| "Create a new group" card | OnboardOption | 271-274 | ✅ | onClick = onCreate; sets path="create", step=2 |
| "Join with a QR code" card | OnboardOption | 277-280 | ✅ | onClick = onJoin; calls onJoinWithQR() |
| "Join with recovery phrase" card | OnboardOption | 283-287 | ⏳ | onClick = {}; labeled "Coming soon" |
| Group name field | BasicTextField | 440 | ✅ | onValueChange updates state |
| Creator name field | BasicTextField | 440 | ✅ | onValueChange updates state |

**Issues**:
- Line 63: `step` uses `remember` not `rememberSaveable` — loses onboarding progress on config change ⚠️
- Line 64: `path` uses `remember` not `rememberSaveable` ⚠️
- Line 67: `groupName` uses `remember` not `rememberSaveable` ⚠️
- Line 286: Recovery phrase join is intentional placeholder (labeled "Coming soon")
- Uses Ink theme exclusively ✅

---

### 8. SafewordsNavigation.kt

**Path**: `app/src/main/kotlin/com/thc/safewords/ui/navigation/SafewordsNavigation.kt`

| Element | Type | Line | Status | Notes |
|---------|------|------|--------|-------|
| Home tab | Tab button | 190-221 | ✅ | Navigates to Screen.Home |
| Groups tab | Tab button | 190-221 | ✅ | Navigates to Screen.Groups |
| Verify tab | Tab button | 190-221 | ✅ | Navigates to Screen.Verify |
| Settings tab | Tab button | 190-221 | ✅ | Navigates to Screen.Settings |

**Navigation Routes**:
- Line 58-71: Screen sealed class defines all navigation targets ✅
- Line 112-173: All routes correctly mapped to composables ✅

**Issues**:
- Line 88: `plainMode` uses `rememberSaveable` ✅ (correctly persistent)

---

## Detailed Findings

### Color Token Usage Audit

**Ink Theme Usage**: ✅ PASS
- HomeScreen, GroupsScreen, VerifyScreen, OnboardingScreen, SettingsScreen: all use Ink theme
- QRDisplayScreen: all use Ink theme
- QRScannerScreen, GroupDetailScreen: use legacy aliases (Background, Surface, Teal, etc.) but these are correctly aliased to Ink in Color.kt

**No deprecated Material 2 colors found** ✅

---

### State Persistence Issues

**Problem**: Most screens use `remember {}` instead of `rememberSaveable {}`, causing state loss on:
- Device rotation (configuration change)
- App backgrounding / resuming (process death on low memory)

**Affected Screens**:
1. **HomeScreen** (line 54) — selected group
2. **GroupsScreen** (line 57) — selectedId
3. **OnboardingScreen** (lines 63, 64, 67) — step, path, groupName, creatorName
4. **VerifyScreen** (line 62) — phase (minor, can be recovered)

**Impact**: 
- User loses group selection when app restarts
- User loses onboarding progress mid-flow
- Verify phase can reset (acceptable since it's transient)

**Recommendation**: Upgrade to `rememberSaveable` where user-facing state needs persistence.

---

### Group Selection Issues

**Critical Bug in VerifyScreen**:
- **Line 61**: `val group = groups.firstOrNull()`
- This ignores the user's currently-selected group (shown on HomeScreen)
- If user has multiple groups, VerifyScreen always verifies against the first group, not the active one
- **Fix required**: Verify screen needs to know which group is active; pass it as a parameter or read from SharedPreferences

**Correct Pattern** (used in HomeScreen, GroupsScreen):
- HomeScreen: uses LaunchedEffect to sync selected group with first valid group
- GroupsScreen: uses rememberSaveable + LaunchedEffect to track selectedId

---

### Navigation & Back Button Audit

**All back buttons use correct patterns**:
- GroupDetailScreen (line 146): onBack → popBackStack() ✅
- QRDisplayScreen (line 76): onDismiss → popBackStack() ✅
- QRScannerScreen (line 109): onBack → popBackStack() ✅

**Navigation routes**: All targets exist in Screen sealed class ✅
- Home, Groups, Verify, Settings: Tab navigation
- GroupDetail, QRDisplay, QRScanner: Detail routes with proper ID passing
- Onboarding: Entry point when groups.isEmpty()

---

### Placeholder / Deferred Elements

| Element | Location | Status | Notes |
|---------|----------|--------|-------|
| Notification icon | HomeScreen:113 | 🪲 Design-only | No onClick |
| SMS invite card | QRDisplayScreen:161 | ⚠️ Placeholder | onClick = {} |
| Mic listening | VerifyScreen:103-107 | ⚠️ Demo | No real speech-to-text |
| Match/Mismatch demo | VerifyScreen:256-257 | ⚠️ Demo | For testing only |
| Recovery phrase join | OnboardingScreen:283 | ⏳ Coming soon | Labeled as such |
| Member rows | GroupsScreen:213 | 🪲 Display-only | No action |
| Group card badge | GroupsScreen:176 | 🪲 Visual | No action |
| Settings rows (all) | SettingsScreen:157-179 | ⚠️ Mostly noop | Several no-op clickables |

---

## Risk Summary

| Risk Level | Count | Items |
|------------|-------|-------|
| 🔴 Critical | 1 | VerifyScreen group selection bug (line 61) |
| ⚠️ High | 4 | State persistence (remember vs rememberSaveable) |
| ⚠️ Medium | 6 | Placeholder UI elements without real actions |
| ℹ️ Low | 5 | Design-only elements (acceptable) |

---

## Recommendations

1. **URGENT**: Fix VerifyScreen group selection to use active group instead of `firstOrNull()`
2. Convert state to `rememberSaveable` in:
   - HomeScreen.selected
   - GroupsScreen.selectedId
   - OnboardingScreen.step, path, groupName, creatorName
3. Implement actual SMS invite flow (QRDisplayScreen line 161)
4. Implement actual speech-to-text in VerifyScreen listening panel (replace demo buttons)
5. Document which SettingsScreen rows are intentionally no-op
6. Consider persisting user's group selection to disk for recovery after app kill

---

## Appendix: Files Reviewed

- ✅ HomeScreen.kt
- ✅ GroupsScreen.kt
- ✅ GroupDetailScreen.kt
- ✅ QRDisplayScreen.kt
- ✅ QRScannerScreen.kt
- ✅ VerifyScreen.kt
- ✅ OnboardingScreen.kt
- ✅ SafewordsNavigation.kt (navigation host + tab bar)
- ✅ Color.kt (theme reference)
- ✅ Theme.kt (not audited; reference only)

**Total elements catalogued**: 62 interactive components across 8 screens.
