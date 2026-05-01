# Demo Mode

v1.3.1 introduced demo mode: a way to explore the app before committing to a real group. This doc covers the demo seed, group, lifecycle, and cross-platform parity rules.

## Why

The original onboarding flow forced users to create or join a group before any app surface was reachable. Competitive review (codeword, Supercode, safe_word) confirmed this is industry-standard but actively bad — users want to evaluate a security tool before trusting it with a real shared seed.

Demo mode flips that: a "Try without a group" path on onboarding panel 1 drops the user into a placeholder group with all primitives enabled, so they can poke Plain Mode, Settings, primitives toggles, the cards browser, the challenge sheet, and the override reveal — all backed by a synthetic seed that's clearly not their own.

## The demo group

Both platforms hardcode the same demo group:

| Field | Value |
|---|---|
| ID | `00000000-0000-0000-0000-00000000d013` |
| Name | `Demo · TIGER` |
| Seed | 32 bytes spelling `TIGER-DEMO-SAFEWORDS-V13-DEMO-!!` in ASCII |
| Interval | Daily |
| Primitives | rotating word + challenge/answer + static override, all enabled |
| Members | One: "Demo User", role creator |
| Created | epoch 0 |

The seed bytes are intentionally readable when dumped — anyone reverse-engineering the binary sees this is a synthetic value, not a real user secret.

The demo group ID `0...d013` is reserved; never assign it to a real group. Repository code special-cases this ID to skip Keychain/SecureStorage writes and to short-circuit `deleteGroup` to call `exitDemoMode` instead.

## Cross-platform parity

Both Android (`GroupRepository.DEMO_SEED_BYTES`) and iOS (`GroupStore.demoSeed`) define the same 32-byte sequence. Two devices in demo mode at the same wall-clock time produce the same rotating word, the same numeric code, the same override word, and the same C/A table.

This is testable: open Demo on Android, open Demo on iOS, compare the home-screen word. They must match.

## Lifecycle

### Entering

Triggered by:
- "Try without a group" option on `OnboardingScreen` (Android) / `OnboardingView` (iOS) panel 1

Path:
1. UI calls `GroupRepository.enterDemoMode()` (Android) / `GroupStore.enterDemoMode()` (iOS)
2. Repository checks no real groups exist; bails if any found (refuse to shadow real data)
3. Sets `demoMode = true` flag in EncryptedSharedPreferences (Android) / UserDefaults App Group (iOS)
4. Inserts the demo group into the in-memory groups list
5. Sets it as the active group
6. Navigation pops to Home

### Exiting

Triggered by:
- `createGroup()` or `joinGroup()` — auto-exits before installing the real group
- `resetAllData()` — clears the demo flag along with everything else
- User tapping the "DEMO MODE / Set up your real group →" banner in Plain home

Path:
1. UI or repo calls `exitDemoMode()`
2. Sets `demoMode = false`
3. Clears the prefs flag
4. Removes the demo group from the in-memory groups list
5. If active group was the demo, picks another (or null)
6. Caller may navigate to Onboarding (banner-tap path does this)

### Persistence

Demo mode survives app launches:
- The flag is in EncryptedSharedPreferences / UserDefaults
- On launch, `loadDemoMode()` reinjects the demo group if the flag is true
- The seed is never written to Keychain or SecureStorageService — it's hardcoded, recomputed every launch

This means demo state is local-only. There's no cloud sync of demo state, no notification, nothing crosses to other devices.

## UX surface

### Onboarding entry

Panel 1 of `OnboardingScreen` shows four options:
1. Create a new group (primary)
2. Join with a QR code
3. Restore from a backup
4. **Try without a group** (new in v1.3.1)

### In-app banner

Plain home shows a top banner when `demoMode == true`:

```
┌────────────────────────────────────┐
│ DEMO MODE                          │
│ Set up your real group →           │
└────────────────────────────────────┘
```

Tapping the banner:
1. Calls `exitDemoMode()`
2. Switches `advancedView` to true (so user lands on the standard tabbed UI)
3. Standard navigation finds no groups → routes to Onboarding
4. User picks Create / Join / Restore for real

The banner only renders in Plain home. If the user's already toggled to Advanced view, demo state still works but no banner is shown there — the user has clearly opted into a different surface.

## Reset semantics

`resetAllData()` (Settings → Danger zone) wipes:
- Every real group's seed from Keychain / SecureStorage
- All EncryptedSharedPreferences / UserDefaults
- Cached active group ID
- Demo mode flag

The demo group's seed is never touched (it's not stored). After reset, demo mode is off and the user's back at first-launch state.

## What demo mode is NOT

- **Not a sandbox for real testing.** It's a UX exploration affordance, not a security evaluation environment. A user can't compare two real safewords using demo mode because both sides would be using the same hardcoded seed.
- **Not persisted across app uninstall.** The demo flag lives in app prefs; uninstall removes it.
- **Not safe to use as a real group.** If a malicious actor knew the demo seed (which is hardcoded in the open-source binary, so they will), they could impersonate any demo-mode user. That's fine because demo mode is explicitly exploratory; it's not for any real verification.
