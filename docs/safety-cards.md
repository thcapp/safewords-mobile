# Safety Cards

v1.3 introduced printable cards rendered natively on each platform. This doc covers the system end-to-end: copy, sensitivity tiers, render pipeline, biometric gating, and how to add a new card type.

## Goals

1. **Bridge digital and physical**. A card on a fridge or in a wallet is a household-level safety behavior, not just an app behavior.
2. **Stay offline**. Card rendering must not touch the network. The web app's printables exist for desktop; mobile renders its own.
3. **Match sensitivity to gate**. Some cards are safe to print often; some are recovery material and must be biometric-gated.

## Card types

Six templates ship in v1.3.1. Each is keyed in `/shared/safety-card-copy.json` so iOS and Android render identical text.

| Card | Key | Sensitivity | Gate | Renders |
|---|---|---|---|---|
| Protocol | `protocol` | Low | None | "Ask: what's our word? / Don't say it first / Hang up if wrong" + install QR |
| Static override | `staticOverride` | High | Biometric | The override word + warning copy |
| Challenge / answer wallet | `challengeAnswerWallet` | High | Biometric | First 24 ask/expect pairs, fits a fold-card |
| Challenge / answer protocol | `challengeAnswerProtocol` | High | Biometric | Full 100-row table, fits a folded letter |
| Recovery phrase | `recoveryPhrase` | High | Biometric | 24-word BIP39 phrase + warning |
| Group invite | `groupInvite` | High | Biometric | The QR; treated as seed-equivalent until expiring invites exist |

## Sensitivity tiers

**Low** = instructional only. The protocol card has no group-specific secrets — just the rules of engagement plus an install QR pointing at `safewords.io/app`. Safe to leave on a fridge, hand to a neighbor, post in a workplace breakroom.

**High** = anyone holding this card can verify (or restore) as you. Static override word, challenge/answer table, recovery phrase, and group invite QR all let an attacker impersonate a group member or recreate the group. These are stored folded, never photographed, never put in cloud notes. The biometric gate is a tripwire — the user must explicitly authenticate before the system print sheet appears.

## Copy schema

`/shared/safety-card-copy.json` is the source of truth for card text. Both platforms load it as a bundled asset and substitute `{groupName}` at render time.

```json
{
  "version": 1,
  "cards": {
    "protocol": {
      "sensitivity": "low",
      "biometricGate": false,
      "title": "Safewords Protocol",
      "subtitle": "Ask. Listen. Hang up if it's wrong.",
      "rules": ["Ask: \"What is our Safewords word?\"", ...],
      "footer": "safewords.io · Pre-agreed proof of identity",
      "qrPayload": "https://safewords.io/app",
      "qrCaption": "Get the app"
    },
    "staticOverride": {
      "sensitivity": "high",
      "biometricGate": true,
      "title": "Safewords Override",
      "subtitle": "{groupName} — emergency override word",
      "warningHeading": "Anyone with this card can verify as {groupName}.",
      "warningBody": "Treat it like a key. ...",
      "footer": "Override is fixed for this group seed. Rotate the seed to change it."
    },
    ...
  }
}
```

Adding a new card type requires:
1. New entry in `safety-card-copy.json` with the sensitivity tier and copy
2. Per-platform render template (see below)
3. Entry in `SafetyCardsScreen` (Android) and `SafetyCardsView` (iOS)

## Render pipeline

### Android (`com.thc.safewords.print`)

`CardRenderer.kt` provides one render function per template:
```kotlin
fun renderProtocolCard(context, groupName, rules, title, subtitle, footer): Bitmap
fun renderOverrideCard(groupName, word, warningHeading, warningBody, footer): Bitmap
fun renderRecoveryCard(groupName, words, warningHeading, warningBody, footer): Bitmap
fun renderChallengeAnswerCard(groupName, rows, title, subtitle, warningHeading, warningBody): Bitmap
fun renderInviteCard(groupName, qrBitmap, title, subtitle, warningHeading, warningBody, footer): Bitmap
```

Each function:
1. Allocates an ARGB letter-size bitmap (2550 × 3300 px @ 300dpi) or wallet-size (1050 × 600 px)
2. Draws header, hero content, warning, footer using `Canvas` + `Paint` primitives
3. Returns the bitmap

`CardRenderer.print()` routes the bitmap to `androidx.print.PrintHelper.printBitmap()` which surfaces the system print dialog.

### iOS (`Safewords/Print/`)

`CardRenderer.swift` exposes one function per template returning a `UIImage`. Each card has a SwiftUI view in `Safewords/Views/Cards/` that renders the layout, then `UIGraphicsImageRenderer` rasterizes it. The image is presented via:
- `UIPrintInteractionController` for direct print
- `UIActivityViewController` for AirDrop / Save PDF / share sheet

### Why bitmap instead of PDF

- Both platforms have built-in bitmap-to-print pipelines that handle scaling, monochrome conversion, and orientation
- PDF would require a layout library or platform-specific PDF context
- For single-page cards with short text + QR, a 300dpi bitmap is indistinguishable from PDF on print

## Biometric gating

High-sensitivity cards prompt for auth before the print dialog. The gate lives in the browser screen, not the renderer — so the renderer remains a pure transform from data to bitmap.

**Android**: `BiometricService.canAuthenticate(activity)` checks for biometric or device credential availability. If yes, `BiometricService.authenticate(...)` shows the prompt. On success, the print path runs. If no biometric is enrolled, the gate falls through (rationale: the user can already unlock the device, blocking them from their own card is worse than the marginal security gain).

**iOS**: `LAContext.canEvaluatePolicy(.deviceOwnerAuthentication)` + `evaluatePolicy(...)` — same shape. On no-auth-available, surfaces an error rather than falling through, since the iOS LocalAuthentication API is more reliable about reporting state.

Configuration is per-card via the `biometricGate` field in `safety-card-copy.json`.

## Group invite card — special case

The invite card prints the group join QR. Until expiring invites exist, this QR contains the group seed and is therefore seed-equivalent. We tag it `high` sensitivity and biometric-gate it.

Future v1.4+ work: signed/expiring invite payloads. Once invites have:
- Recipient binding (only one specific device can use it)
- Time expiry (e.g., 24 hours)
- One-time semantics (consumed on first scan)

…the invite card can drop to `medium` sensitivity. Until then, treat it like recovery material.

## Print shortcuts

Both platforms expose Settings → "Print safety cards" to reach `SafetyCardsScreen` / `SafetyCardsView`. The browser:
1. Lists every card available for the active group
2. Hides templates whose primitive isn't enabled (e.g., no override card if `staticOverride.enabled = false`)
3. Shows a small lock icon next to high-sensitivity rows
4. Tapping a row triggers gate → render → print

## Adding a new card

1. **Schema**: add an entry to `/shared/safety-card-copy.json` with `sensitivity`, `biometricGate`, and copy strings (use `{groupName}` for group-specific substitution)
2. **Renderer**: add `renderXxxCard(...)` to `CardRenderer.kt` and `CardRenderer.swift`
3. **View**: add a SwiftUI card view in `Safewords/Views/Cards/`; Android draws directly via Canvas, no separate file needed
4. **Browser entry**: add a `CardRow` block to `SafetyCardsScreen.kt` and `SafetyCardsView.swift` that calls the renderer, gates if `biometricGate`, and routes to `print()`
5. **Test vectors**: if the card consumes a primitive's output, add fixture vectors to `/shared/primitive-vectors.json`

Both platforms must produce the same logical content from the same group config. Visual layout can vary (Compose Canvas vs SwiftUI primitives) but the words must match.
