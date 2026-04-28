# Android Plain Mode + Lifecycle Audit

**Scope:** Read-only audit of Safewords Android app (`safewords-android`)  
**Date:** 2025-04-20  
**Focus:** Plain mode UI wire-up gaps, exit affordances, group selection, onboarding group creation, and app-wide lifecycle/persistence issues.

---

## Executive Summary

| Category | Status | Severity |
|----------|--------|----------|
| Plain mode exit path | 🔴 **Missing** | Critical |
| Plain mode group selection | ⚠️ **Hardcoded first** | High |
| Plain onboarding group creation | ✅ **Wired** | N/A |
| GroupRepository cold-start race | ✅ **Safe (lazy init)** | N/A |
| Persistence (selectedGroupID) | 🔴 **No equivalent** | High |
| Plain mode toggle survival post-process-kill | ⚠️ **Fragile (rememberSaveable)** | High |
| Camera permission check | ✅ **Correct** | N/A |
| Navigation back-stack cleanup | ✅ **Correct (Onboarding)** | N/A |
| Back-stack cleanup (QRScanner) | ✅ **Correct** | N/A |
| Configuration change handling (remember vs rememberSaveable) | ⚠️ **Mostly correct, one case** | Medium |

---

## PLAIN MODE SCREENS — CLICKABLE WIRE-UP AUDIT

### PlainRoot (lines 72–95)
- **Container composable, no direct interactions**
- Manages plain mode state via `rememberSaveable { mutableStateOf(false) }` for `onboarded`
- Screen switching works via `screen` mutable state
- **Issue:** No exit affordance from any Plain screen; `onExitPlain` callback passed but never called

### PlainTabBar (lines 98–143)
| Action | Status | Notes |
|--------|--------|-------|
| Home tab click | ✅ | `onChange(PlainScreen.Home)` → state update |
| Verify tab click | ✅ | `onChange(PlainScreen.Verify)` → state update |
| Help tab click | ✅ | `onChange(PlainScreen.Help)` → state update |

### PlainHome (lines 180–301)
| Action | Status | Notes |
|--------|--------|-------|
| Group avatar (circle with initial) | 🪲 | Display-only, not clickable; shows `groups.firstOrNull()` |
| "Someone is calling me" button | ✅ | Calls `onVerify()` → switches to PlainScreen.Verify |
| **Exit Plain mode** | 🔴 | **NOT AVAILABLE** |

**Critical Issue:** User enters Plain mode via Settings toggle but has NO way to exit. The only toggle is in Settings (standard mode), which is unreachable once in Plain mode.

### PlainVerify (lines 311–337)
| State | Action | Status |
|-------|--------|--------|
| PlainAsk | "Yes, it matched" | ✅ → phase = "match" |
| PlainAsk | "No, wrong word" | ✅ → phase = "nomatch" |
| PlainAsk | "Cancel" | ✅ → phase = "ask", `onDone()` |
| PlainResult (match) | "All done" | ✅ → `onDone()` |
| PlainResult (nomatch) | "I hung up" | ✅ → `onDone()` |
| PlainResult (nomatch) | "Call them back on a trusted number" | ⏳ | Empty `onSecondary = {}` (deferred) |

### PlainHelp (lines 506–593)
| Item | Click Handler | Status |
|------|----------------|--------|
| "I got a strange call" | `.clickable {}` | 🔴 **Placeholder** |
| "Who is in my circle" | `.clickable {}` | 🔴 **Placeholder** |
| "What's a \"word\"?" | `.clickable {}` | 🔴 **Placeholder** |
| "Change text size" | `.clickable {}` | 🔴 **Placeholder** |
| "Call my family for help" | `.clickable {}` | 🔴 **Placeholder** |
| EMERGENCY "call 911" | Display-only | 🪲 **Not interactive** |

**Opportunity:** PlainHelp is the ideal location for an "Exit High Visibility Mode" toggle, as it's already the help/settings equivalent in Plain mode.

### PlainOnboarding (lines 596–688)
| Step | Action | Status | Notes |
|------|--------|--------|-------|
| Welcome panel | "Show me how" button | ✅ | `step = 1` |
| Start panel | "Create a new group" option | ✅ | `path = "create"; step = 2` → triggers group creation |
| Start panel | "Join with a QR code" option | ✅ | `onJoinWithQR()` → navigation to QRScanner |
| Start panel | "Join with a recovery phrase" option | ⏳ | Deferred, `onClick = {}` |
| Create form | "Create group" CTA | ✅ | Calls `GroupRepository.createGroup(…)` then `onComplete()` |
| Back button | Any step | ✅ | `step -= 1` with state cleanup |

**Group Creation:** YES, `GroupRepository.createGroup()` is called at line 151-155, so Plain onboarding DOES create a real group. ✅

---

## CRITICAL ISSUE: PLAIN MODE EXIT AFFORDANCE

### Current State
- Plain mode toggle lives in **SettingsScreen** (standard mode)
- Once `plainMode = true` (line 88 in SafewordsNavigation), navigation switches to `PlainRoot` and back stack is ignored
- User taps Settings toggle → enters Plain mode → **NO PATH BACK**

### Root Cause
- `onExitPlain = { plainMode = false }` callback is passed to `PlainRoot` (line 92) but is **never invoked** by any Plain screen
- `PlainHelp()` at line 85 receives no callback, so it cannot trigger exit

### Proposed Solutions

**Option A (Recommended):** Add "Exit High Visibility Mode" toggle to PlainHelp
- Insert new help item at end of list: `HelpItem(Icons.Outlined.Settings, "Exit High Visibility", "Return to standard mode")`
- Modify PlainHelp signature: `fun PlainHelp(onExit: () -> Unit = {})`
- Wrap that item in `.clickable { onExit() }`
- Pass `onExit` from PlainRoot down through PlainVerify state logic

**Option B:** Add exit affordance to PlainHome header
- Small "⚙️ Exit" button in top-right corner of PlainHome
- Less discoverable than Option A

**Option C:** Require press-and-hold of a Compose button + confirm dialog
- Safer but more friction-heavy

---

## PLAIN MODE: GROUP SELECTION AUDIT

### Current Behavior
- **PlainHome (line 182):** `val g = groups.firstOrNull()`
- **SettingsScreen (line 50):** `val active = groups.firstOrNull()`

### Issue
- Plain mode always shows `groups.firstOrNull()`, ignoring any "selected" or "active" group concept
- If user has multiple groups, Plain mode has no UI to switch between them
- Android version has **NO equivalent to iOS GroupStore's `activeGroupId`**

### Expected Behavior
- Plain mode should respect user's actively-selected group (if such a concept exists)
- Currently, there is no persistent "active group" tracking

### Recommendation
- **Short term:** Document that Plain mode only shows the first group (oldest/first-created)
- **Medium term:** Add a concept of `selectedGroupId` to GroupRepository (persisted in EncryptedSharedPreferences)
- **Long term:** Add a group picker to PlainHome or PlainHelp

---

## PLAIN ONBOARDING: GROUP CREATION VERIFICATION ✅

### Path
1. User enters Plain mode
2. `PlainOnboarding` shown (line 75-77 in PlainMode.kt)
3. User completes panels, enters group name + creator name
4. **Line 151-155:** `GroupRepository.createGroup(name, creatorName, seedHex)` called
5. `onDone()` callback triggers, `onboarded.value = true`
6. `PlainHome` shown

### Verification
- ✅ **Group IS created via GroupRepository**
- ✅ Seed is persisted via `SecureStorageService.saveSeed(group.id, finalSeedHex)`
- ✅ Group metadata is saved to EncryptedSharedPreferences
- ✅ User is NOT left without a real group

---

## LIFECYCLE & PERSISTENCE AUDIT

### 1. GroupRepository Initialization Timing ⏱️ SAFE

**File:** `/app/src/main/kotlin/com/thc/safewords/service/GroupRepository.kt`

```kotlin
object GroupRepository {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    init {
        loadGroups()  // Line 48
    }
    
    private fun loadGroups() {
        val json = prefs.getString(GROUPS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<List<Group>>() {}.type
            _groups.value = gson.fromJson(json, type) ?: emptyList()
        }
    }
}
```

**Analysis:**
- ✅ Kotlin object singleton ensures `init` runs **once, thread-safely** before first access
- ✅ `loadGroups()` runs synchronously in `init`, so `_groups.value` is populated before any Composable reads it
- ✅ No race condition on cold launch

**Edge case:** If app process is killed and relaunched, `GroupRepository` is re-instantiated fresh, `loadGroups()` re-reads from disk. Safe.

---

### 2. Persistence: Active Group Selection 🔴 MISSING

**Current State:**
- iOS version has `GroupStore.activeGroupId` (persisted)
- Android version has **NO equivalent**

**Impact:**
- HomeScreen defaults to `selected = groups.first()` (line 60, HomeScreen.kt)
- SettingsScreen defaults to `active = groups.firstOrNull()` (line 50, SettingsScreen.kt)
- If user selects a group in HomeScreen and app is killed, selection is lost → defaults to first again on relaunch

**Recommendation:**
- Add `selectedGroupId` to GroupRepository persisted in EncryptedSharedPreferences
- Update HomeScreen to use `GroupRepository.getSelectedGroup()` or fall back to first if not set
- Provide `GroupRepository.setSelectedGroup(id)` when user taps a group

---

### 3. Configuration Changes: Remember vs RememberSaveable 🤔 MOSTLY OK

| State | Used | Scope | Survives Rotation | Survives Process Kill |
|-------|------|-------|-------------------|----------------------|
| **HomeScreen.selected** | `remember` | Recomposition | ❌ No | ❌ No |
| **SettingsScreen.interval** | `remember` | Recomposition | ❌ No | ❌ No |
| **PlainRoot.onboarded** | `rememberSaveable` | SavedInstanceState | ✅ Yes | ❌ Bundle lost |
| **PlainRoot.screen** | `rememberSaveable` | SavedInstanceState | ✅ Yes | ❌ Bundle lost |
| **SafewordsNavigation.plainMode** | `rememberSaveable` | SavedInstanceState | ✅ Yes | ❌ Bundle lost |
| **PlainOnboarding.step** | `remember` | Recomposition only | ❌ No | ❌ No |

**Issues:**
1. **HomeScreen.selected (line 54):** Uses `remember`, so selection is lost on rotation. Should use `rememberSaveable { mutableStateOf<Group?>(null) }` to survive rotation.
2. **SettingsScreen.interval (line 51):** Uses `remember`, lost on rotation. However, it reads from `GroupRepository.getDefaultInterval()`, so on relaunch it's refetched. Acceptable.
3. **SafewordsNavigation.plainMode (line 88):** Uses `rememberSaveable`, survives rotation but **NOT process death**. rememberSaveable uses Android SavedInstanceState (Bundle), which is cleared when process is killed.

**Recommendation for plainMode:**
- If Plain mode toggle should survive app process kill, persist it to EncryptedSharedPreferences
- Read it on app startup in MainActivity or SafewordsApp
- Use rememberSaveable + a side-effect to sync back to disk on change

---

### 4. Camera Permission Check ✅ CORRECT

**File:** QRScannerScreen.kt, lines 77-99

```kotlin
val hasCameraPermission by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
    )
}

val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    hasCameraPermission = isGranted
}

LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
```

**Analysis:**
- ✅ Checks permission synchronously on composition
- ✅ Launches permission request via ActivityResultContracts if denied
- ✅ Handles denial gracefully: shows fallback UI (lines 158-184) with retry button
- ✅ Does not crash if permission denied

---

### 5. Navigation Back-Stack: Onboarding Cleanup ✅ CORRECT

**File:** SafewordsNavigation.kt, lines 112-122

```kotlin
composable(Screen.Onboarding.route) {
    OnboardingScreen(
        onComplete = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        },
        ...
    )
}
```

**Analysis:**
- ✅ `popUpTo(Onboarding, inclusive=true)` removes Onboarding and everything below it
- ✅ User cannot back-press into Onboarding after completion
- ✅ Correct pattern

---

### 6. Navigation Back-Stack: QRScanner Cleanup ✅ CORRECT

**File:** SafewordsNavigation.kt, lines 165-172

```kotlin
composable(Screen.QRScanner.route) {
    QRScannerScreen(
        onGroupJoined = { id ->
            navController.popBackStack(Screen.Groups.route, inclusive = false)
            navController.navigate(Screen.GroupDetail.createRoute(id))
        },
        onBack = { navController.popBackStack() }
    )
}
```

**Analysis:**
- ✅ `popBackStack(Groups, inclusive=false)` removes QRScanner and intermediate screens, lands on Groups
- ✅ Then navigates to GroupDetail
- ✅ User cannot back-press back into QRScanner from GroupDetail without re-opening it
- ✅ Correct pattern

---

### 7. Plain Mode Toggle: Source of Truth 🔴 FRAGILE

**File:** SafewordsNavigation.kt, line 88

```kotlin
var plainMode by rememberSaveable { mutableStateOf(false) }
```

**Issue:**
- `rememberSaveable` saves state to Android SavedInstanceState (Bundle)
- SavedInstanceState is **cleared on process death** (app force-stop or system memory pressure kill)
- User may enable Plain mode, then app is killed → plainMode reverts to false on relaunch
- User expects to return to Plain mode but is back in standard mode

**Observations:**
- SafewordsApp.kt and MainActivity.kt provide no persistent storage for plainMode
- No SavedStateHandle or DataStore used for this value

**Recommendation:**
- Add plainMode persistence to EncryptedSharedPreferences
- Read it in SafewordsApp.kt or as a side-effect in SafewordsNavigation
- Sync changes back to disk immediately when toggle changes

---

### 8. Settings Plain Toggle Round Trip ✅ NAVIGATION CORRECT

**File:** SettingsScreen.kt, line 86

```kotlin
ToggleRow("High visibility mode", plainMode, onPlainModeChange)
```

**File:** SafewordsNavigation.kt, line 144

```kotlin
composable(Screen.Settings.route) {
    SettingsScreen(plainMode = plainMode, onPlainModeChange = { plainMode = it })
}
```

**File:** SafewordsNavigation.kt, lines 91-94

```kotlin
if (plainMode) {
    PlainRoot(onExitPlain = { plainMode = false })
    return
}
```

**Analysis:**
- ✅ Toggle updates `plainMode` state variable
- ✅ Navigation recomposition triggers, Compose evaluates `if (plainMode)` check
- ✅ Transitions to `PlainRoot` smoothly
- ✅ No back-stack management needed (replaces entire content)

**Caveat:**
- User has NO visual feedback that the mode switched (instant recomposition)
- Suggestion: Add a brief toast or transition animation

---

## SUMMARY TABLE: WIRE-UP STATUS

| Feature | Component | Status | Evidence |
|---------|-----------|--------|----------|
| Plain Home → Verify | BigButton click | ✅ | Line 295-299 PlainMode.kt |
| Plain Verify → result | Answer buttons | ✅ | Lines 392-402 PlainMode.kt |
| Plain result → Home | CTA buttons | ✅ | Lines 497, 500 PlainMode.kt |
| Plain Help items | 5 rows | 🔴 Placeholder | Line 545 `.clickable {}` |
| Plain Tab Bar | 3 tabs | ✅ | Lines 119-141 PlainMode.kt |
| Plain mode exit | Any screen | 🔴 Missing | No callback invoked |
| Plain onboarding group creation | CreateGroup call | ✅ | Lines 151-155 PlainMode.kt |
| QR join group creation | GroupRepository.joinGroup | ✅ | QRScannerScreen.kt line 230 |
| Standard onboarding group creation | GroupRepository.createGroup | ✅ | OnboardingScreen.kt line 151 |

---

## RECOMMENDATIONS (Priority Order)

### P0 (Blocking)
1. **Add Plain mode exit affordance** → PlainHelp + toggle callback to SafewordsNavigation
2. **Persist plainMode toggle** → Move to EncryptedSharedPreferences, survive process death
3. **Implement selectedGroupId concept** → Android parity with iOS, allow multi-group selection

### P1 (High)
4. Fill in PlainHelp placeholder items with real handlers (or deferred notes)
5. Implement HomeScreen.selected as `rememberSaveable` to survive rotation
6. Add "active group" UI to PlainHome (optional, given single-group common case)

### P2 (Polish)
7. Visual feedback when toggling Plain mode (toast or fade)
8. Confirm dialog when exiting Plain mode (user intent check)
9. Breadcrumb or "Help" label in PlainOnboarding to clarify context

---

## FILES CHECKED (Read-only)
- `/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt`
- `/app/src/main/kotlin/com/thc/safewords/SafewordsApp.kt`
- `/app/src/main/kotlin/com/thc/safewords/MainActivity.kt`
- `/app/src/main/kotlin/com/thc/safewords/ui/navigation/SafewordsNavigation.kt`
- `/app/src/main/kotlin/com/thc/safewords/service/GroupRepository.kt`
- `/app/src/main/kotlin/com/thc/safewords/service/SecureStorageService.kt`
- `/app/src/main/AndroidManifest.xml`
- `/app/src/main/kotlin/com/thc/safewords/ui/settings/SettingsScreen.kt`
- `/app/src/main/kotlin/com/thc/safewords/ui/qr/QRScannerScreen.kt`
- `/app/src/main/kotlin/com/thc/safewords/ui/onboarding/OnboardingScreen.kt`
- `/app/src/main/kotlin/com/thc/safewords/ui/home/HomeScreen.kt`

