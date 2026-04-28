# Android Settings + Services Audit

**Source**: Phase 1C explore agent (returned findings inline; captured here for posterity).
**Date**: 2026-04-27.

## Settings rows summary

| Section | Row | Status | Backing function |
|---|---|---|---|
| Rotation | Interval picker (1h/6h/1d/1w) | ✅ wired | `setDefaultInterval()` exists |
| Rotation | Notify on rotation | ⚠️ placeholder | needs `setNotifyOnRotation()` |
| Rotation | Include preview of next word | ⚠️ placeholder | needs `setPreviewNextWord()` |
| Accessibility | High visibility mode | ✅ wired | callback flips plainMode state |
| Widget | Home screen widget | 🪲 design-only | no backing code |
| Widget | Lock screen glance | ⚠️ placeholder | needs `setLockScreenGlance()` |
| Widget | Hide word until unlock | ⚠️ placeholder | needs `setHideUntilUnlock()` |
| Security | Require Biometrics | ⚠️ placeholder | needs `BiometricService` + prefs |
| Security | Emergency override word | 🔴 broken | no storage |
| Security | Rotate group seed | ⏳ deferred | requires new crypto rotation |
| Security | Back up seed phrase | ⏳ deferred | needs BIP39 export |
| Practice | Run a scam drill | 🔴 broken | no destination, no DrillService |
| Practice | Drill history | 🔴 broken | no storage |
| Danger | Leave this group | 🔴 broken | no UI delete confirmation wired |
| Danger | Reset device | 🔴 broken | no confirmation, no full reset method |

## Critical: Active group selection

Every screen uses `groups.firstOrNull()` to read the active group. Need:

- `setActiveGroup(groupId: String)`
- `val activeGroupId: StateFlow<String?>` 
- Persist in SharedPreferences under `active_group_id`

Without this, multi-group support is broken: switching groups in GroupsScreen has no effect on Verify/Plain mode/widget.

## GroupRepository extension list

```kotlin
// Active group selection
fun setActiveGroup(groupId: String)
val activeGroupId: StateFlow<String?>

// Per-group settings
fun setRotationInterval(groupId: String, interval: RotationInterval)
fun setEmergencyOverrideWord(groupId: String, word: String?)
fun getEmergencyOverrideWord(groupId: String): String?

// Global settings (boolean prefs)
fun setBiometricRequired(enabled: Boolean)
fun isBiometricRequired(): Boolean
fun setNotifyOnRotation(enabled: Boolean)
fun isNotifyOnRotation(): Boolean
fun setPreviewNextWord(enabled: Boolean)
fun isPreviewNextWord(): Boolean
fun setLockScreenGlance(enabled: Boolean)
fun isLockScreenGlance(): Boolean
fun setHideUntilUnlock(enabled: Boolean)
fun isHideUntilUnlock(): Boolean

// Seed lifecycle
fun rotateGroupSeed(groupId: String)        // generates new seed, keeps group
fun exportSeedPhrase(groupId: String): List<String>?  // BIP39 mnemonic from seed

// Reset
fun resetAllData()  // delete every group, every seed, every pref
```

## New services needed

### `service/DrillService.kt`
Stores drill sessions in EncryptedSharedPreferences. API:
- `data class DrillSession(val timestamp: Long, val passed: Boolean)`
- `fun runDrill(): DrillPrompt` — returns a scenario prompt
- `fun submitAttempt(attempt: String, prompt: DrillPrompt): Boolean`
- `fun getHistory(): List<DrillSession>`

### `service/SmsInviteService.kt`
- `fun shareViaSms(context: Context, group: Group, currentWord: String)` — fires `Intent.ACTION_SENDTO` with `sms:` URI and prefilled body. No SMS permission required.

### `service/BiometricService.kt`
- `fun isAvailable(): Boolean`
- `fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit)` — uses AndroidX BiometricPrompt

## New screens needed

### `ui/onboarding/RecoveryPhraseScreen.kt`
- BasicTextField for 12/24-word entry OR hex paste
- Validates word list (BIP39) or hex format
- Calls `GroupRepository.joinGroup(seedHex=..., name=..., memberName=...)`

### `ui/drills/DrillsScreen.kt`
- "Run drill now" CTA → DrillPromptScreen
- History list below
- Pass/fail counts in header

### `ui/drills/DrillPromptScreen.kt`
- Shows scenario ("A caller says they're your grandchild and need money. They give you the word: ____. Type it.")
- User types answer
- Next: result screen pass/fail, save to history

## SecureStorageService

Already adequate. Used for seeds (per-group keys) and the encrypted preferences file.

## Status totals

- ✅ Wired: 2 of 16 Settings rows
- ⚠️ Placeholder: 7
- 🔴 Broken / no destination: 4
- 🪲 Design-only: 1
- ⏳ Deferred (genuine future work): 2

11 new GroupRepository functions, 3 new services, 3 new screens.
