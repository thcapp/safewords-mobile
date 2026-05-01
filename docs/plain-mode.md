# Plain Mode — The default home (since v1.3)

A guide for developers working on Safewords' high-visibility "Plain mode." This document explains **why** Plain Mode exists, **who** it is for, and the concrete rules that must be preserved as the apps evolve.

> Plain Mode is not a theme. It is a different product, built on the same cryptography, for a different user.

## Status: default since v1.3

Pre-v1.3, Plain Mode was an **opt-in** accessibility variant reachable via Settings → "High visibility mode." The standard tabbed UI was the front door.

**v1.3 inverted that.** Plain Mode is now the **default home** for every user on first launch. The big-word, big-button layout is the front door. The standard tabbed UI demotes to "Advanced view," opt-in via a gear icon in the Plain home top-right. The view preference is sticky — once a user picks Advanced, they stay there until they toggle back.

Reasoning: under stress, the home screen should already be the call-decision screen. There's no second tab to hunt for. Plain Mode was always the right design; the v1.3 work was relabeling it as the default rather than the fallback.

The legacy "High visibility mode" toggle in Settings still exists and still works as a separate control — it forces Plain UX even when a user has explicitly opted into Advanced view. Two flags coexist:
- `advanced_view_enabled` (v1.3) — opt-in to the tabbed UI
- `plain_mode` (v1.0) — accessibility forcing of the Plain UX regardless of advanced view

The default for both is "Plain wins": fresh installs see Plain home. The `advanced_view_enabled` toggle is the user-facing knob; `plain_mode` is the historical accessibility setting that's still honored.

---

## 1. Purpose

Safewords was built in response to a specific threat: voice-cloning scams. A grandparent answers the phone, hears what sounds exactly like their grandchild in distress, and wires money. A child answers a call that sounds exactly like a parent telling them to get in a strange car.

The people targeted by these scams are, overwhelmingly:

- **Elderly adults** — often with reduced vision, declining contrast sensitivity, early cognitive fatigue, and an emotional vulnerability to calls from family.
- **Children** — still learning to read, with smaller comfortable reading distances, and a strong instinct to obey an adult voice.

A scam-verification app is *most valuable for exactly the demographics least served by default mobile UX*. Standard dark-mode fintech aesthetics, thin dividers, tiny tap targets, metaphorical icons, and vocabulary like "group," "passphrase," or "rotation interval" are not neutral — they are **barriers** between a vulnerable user and a tool that can keep them safe.

Plain mode exists so that the Safewords app is usable by an 82-year-old with cataracts and an 8-year-old with reading difficulties, sitting alone, under stress, on a possibly-scam phone call, **the first time they open the app**.

If that sounds like a high bar, it is. That is the point.

---

## 2. Design Priorities

These are non-negotiable. When in doubt, err toward larger, louder, simpler.

| Priority | Rule |
|---|---|
| **Typography floor** | Body text **≥ 20 px**. CTAs **≥ 28 px** equivalent weight (bold 22 sp in code, visually heavier). Hero word **≥ 48 px**. |
| **Contrast** | **WCAG AAA (7:1)** or better against the background. Amber #FFD23F on navy #0B1220 lands at ~13.8:1 — this is deliberate headroom for low-vision users. |
| **Surfaces** | **Solid backgrounds only.** No translucency over photos, no frosted glass, no thin hairlines. 2 px opaque borders only. |
| **Hit targets** | **56–72 px minimum** for any tap. Hero CTAs are 72–80 px tall. Icons always pair with a text label. |
| **Language** | **Zero jargon.** "your word" not "safeword." "your circle" not "group." "New word in 2 hours left" not "TOTP rotation in 2h." |
| **One action per screen** | Each screen has exactly one thing to do. Verify has one question. Home has one word. Help is a menu. |
| **Navigation** | The tab bar with labeled icons (Word / Check / Help) is **always visible** except during onboarding. No hidden gestures. No long-press menus. No swipe-to-reveal. |
| **Feedback** | Visual + color + shape + position — never color alone. A red button is red, is on the bottom, and says "No, wrong word." |

---

## 3. Screens

Plain mode has exactly **four** screens. Every one is intentional. Do not add a fifth without serious justification.

### 3.1 Welcome / Onboarding (2 panels)

**Imagine:** A dark navy screen. Two thin amber progress ticks at the top — the first one filled. Below, a small amber label "WELCOME" in spaced caps. Then, filling most of the screen in a huge near-display weight: **"One word keeps you safe."** Underneath, in a lighter muted gray at a readable 22 px: *"Bad people can copy any voice now. If someone calls and sounds like family, you need a way to be sure it's really them."* Below that, a dark elevated card with a subtle 2 px border, centered inside it the tiny label **"EXAMPLE WORD"** and then, in amber and huge: **Golden Robin**. A single amber CTA at the bottom, full-width, 72 px tall: **"Show me how"**.

**Panel 2** replaces the headline with "Your family picks a secret word." The example card stays — "Golden Robin" persists across both panels as an anchor users will recognize when they see real words later. The CTA changes to "Get started."

**Why two panels, not one?** Because the fear-framing ("bad people can copy any voice") and the mechanism ("your family picks a secret word") are distinct ideas, and loading them onto one screen violates the "one thing per screen" rule.

**Why "Golden Robin"?** Two syllables each, familiar English, concrete (a color + a bird). It reads the same way real safewords read, so users are already calibrated when they see their real word.

**Implementation:** `PlainOnboardingView` (iOS) / `PlainOnboarding` (Android). Gated by `@AppStorage("plainOnboarded")` on iOS and `rememberSaveable` on Android. Once complete, the app never shows onboarding again until the user reinstalls or clears data.

### 3.2 My Word (Home)

**Imagine:** Top-left, a 52 px amber circle with a single bold capital — the first letter of the user's circle name. Next to it, muted label "Your circle" and below it in bold 22 px, the circle name ("Family"). Below, filling the remaining height, a dark navy elevated card with a 2 px white-alpha border. Inside: a tiny amber dot + the label "YOUR WORD TODAY" in caps. Then, centered and massive, the word stacked one line per token:

> **Golden**
> **Robin**

Each word is 48 px, extra-bold, with tight letter-spacing. Below, a pill — a rounded capsule in a slightly brighter navy with an amber refresh icon and the text "New word in 14 hours left." Underneath, a small centered caption: *"Share this word only with your family. A new one comes tomorrow."* At the very bottom, above the tab bar, a full-width amber 72 px CTA: **"Someone is calling me"**.

**Why is the word split across lines?** On elderly phones (iPhone SE 2nd gen, Pixel 4a, many budget Androids), a long compound word at 48 px would truncate or shrink. Stacking tokens guarantees **each word stays at hero size**. The split also chunks the phrase into memorable units.

**Why "New word in X hours left" and not a countdown timer?** A ticking clock creates anxiety. A human-language sentence ("14 hours left") lets the user know *roughly* what's coming without feeling rushed. The app updates the sentence silently.

**Implementation:** `PlainHomeView` / `PlainHome`. Uses the same `TOTPDerivation` the Ink mode uses — no separate algorithm.

### 3.3 Check (Verify)

This is the screen that does the work. It is a two-step wizard. No back button mid-flow except Cancel.

#### Step 1 — Ask

**Imagine:** Small amber eyebrow "STEP 1 OF 2." Below, a 34 px extra-bold headline:

> Ask them:
> **"What is our word?"**

The quoted question is amber. Below, a dark elevated card with a 2 px border containing the warning: **"Do not read the word to them."** in bold, followed by *"They must say it themselves."* in muted gray.

Below the card, after visual breathing room, the prompt **"Did they say the right word?"** centered, extra-bold, 22 px. Then two huge answer buttons:

- **Green** (#4ADE80), 80 px tall, with a white-on-dark-green checkmark icon in a translucent black circle, and the text **"Yes, it matched"** in 24 px extra-bold dark green ink.
- **Red** (#FF6B6B), 80 px tall, with an X icon, **"No, wrong word"** in dark red ink.

A small underlined "Cancel" link below both, muted — this is deliberately the smallest and quietest element on the screen.

**Why not a text input?** Typing the word would require the user to read the screen word, hear the caller say it, and compare — three cognitive tasks. The big Yes/No model reduces it to one judgment ("did that sound right?") and offloads nothing to fine motor skills.

**Why color + icon + position + label on each button?** Redundancy. A colorblind user still sees checkmark-top and X-bottom. A user who can't read still sees green-up and red-down. A user who misses both still reads the label.

**Why "Do not read the word to them" in bold?** This is the single most common mistake real users will make in an actual scam call. The scammer says "don't you recognize me?" and the grandparent's reflex is to help — including by reading out the word. Bolding this instruction is harm reduction.

#### Step 2a — Match ("Safe to talk")

**Imagine:** A large translucent green panel with a 2 px green border fills most of the screen. Centered inside: a 120 px solid green circle with a huge 60 px dark-green checkmark. Below, in 44 px green extra-bold: **"Safe to talk."** Below that, in white 20 px medium: *"They said the right word. This is really them."* At the bottom, a full-width amber CTA: **"All done"**.

The reassurance is explicit. Users at this moment often feel residual panic — the copy ("This is really them") is written to *land* that reassurance, not just confirm it.

#### Step 2b — Mismatch ("Hang up now")

**Imagine:** The same layout but red. Giant red circle, huge X. **"Hang up now."** in 44 px red. Body text: *"They did not know the word. This is not your family. Do not send money. Do not share anything."* Primary CTA: **"I hung up"**. Secondary ghost CTA with a phone icon: **"Call them back on a trusted number"**.

**Every sentence in the mismatch body is a separate imperative.** Users in shock need telegraphed, ordered instructions. "Do not send money" and "Do not share anything" are listed as distinct items because a scammer's script will specifically ask for both.

**Implementation:** `PlainVerifyView` / `PlainVerify`. State machine is `ask | match | nomatch`. No network, no logging, no analytics on this screen — users are panicking, and anything that stalls the UI is harmful.

### 3.4 Help

**Imagine:** Small amber "HELP" eyebrow, a 34 px headline **"How can we help?"**, then a vertical stack of five 80 px cards. Each card has a 52 px amber icon circle on the left, a bold 20 px label, a muted 15 px sub-caption, and a small right-arrow. Below the list, separated by spacing, a distinct red-tinted card with a red 2 px border:

> **EMERGENCY**
> If you feel unsafe, call 911.

The five help items are:

1. **I got a strange call** — What to do right now
2. **Who is in my circle** — See your family and friends
3. **What's a "word"?** — A short, simple explanation
4. **Change text size** — Make everything bigger
5. **Call my family for help** — Ring a trusted person

**Why is 911 always visible?** Because a user who is on Plain mode *at all* may be the kind of user who would otherwise not think to dial 911 during a scam attempt. The callout is not a link — it is a reminder. The app does not auto-dial, because an accidental auto-dial to 911 is a harm of its own.

**Why is "Change text size" a help item and not buried in settings?** Discoverability. An elderly user who can barely read the screen should not have to navigate to a Settings tab they cannot see.

**Implementation:** `PlainHelpView` / `PlainHelp`. The tap actions for the list items are stubbed in code today (`Button {}` / `clickable {}`); these are deliberate placeholders — filling them in is future work, but the **presence and ordering** of the items is load-bearing.

---

## 4. The Toggle

Plain mode is **not** a separate app. It is a global switch on the one app.

- **Location:** Settings → **"High visibility mode"**.
- **iOS persistence:** `@AppStorage("plainMode")` (at the root) and `@AppStorage("plainOnboarded")` for first-run gating.
- **Android persistence:** `rememberSaveable` at the root composable (swap to DataStore when a settings screen is added).
- **Effect:** Switches the entire app globally. The Ink-mode tab bar (Home / Circles / Verify / Settings etc.) is replaced by the Plain tab bar with **three tabs only: Word / Check / Help**. Settings does not appear in Plain mode's tab bar — users return to Settings via the Help screen's "Change text size" item or by toggling the switch off in Ink mode.

**Why only three tabs?** Because the Plain user, by definition, is struggling with complexity. Five tabs is four too many. Circles management, history, export — all of that lives in Ink mode and does not exist in Plain mode.

**Who turns it on?** Often not the user themselves. A family member sets up the app, enables Plain mode, hands the phone back. Design accordingly — the *setter* is a fluent adult in Ink mode; the *user* is elderly or young in Plain mode. The toggle lives in Ink mode's Settings for exactly this reason.

---

## 5. Accessibility Principles

### Plain language replaces jargon, everywhere

| Don't say | Do say |
|---|---|
| safeword | your word |
| group | your circle |
| rotation / rotates | "New word in X hours left" / "A new one comes tomorrow" |
| verify | "Someone is calling me" / "Check" |
| mismatch | "wrong word" / "Hang up now" |
| authenticate | "Did they say the right word?" |

If you find yourself writing any word in the left column into a user-visible string in Plain mode, stop. Find the phrase a 10-year-old or an 80-year-old would use.

### Icons always pair with labels

Every actionable icon in Plain mode has a text label next to it or below it. The tab bar has icon + label. The answer buttons have icon + label. The help list has icon + label + sub-caption. There are **no icon-only controls** in Plain mode. Users who don't recognize a shield symbol read "Word" underneath and understand.

### The 911 callout is always present

In Help, the emergency callout is the last thing on the page — scroll position guarantees a user who reaches the bottom has passed it. On small screens it is within the first viewport. Its red-on-red styling is distinct from every other element in the app so users who remember "the red one at the bottom" can find it again under stress.

### Redundant encoding

Every critical state is encoded by at least two of: color, icon, position, label. A user who cannot perceive one channel still perceives the meaning.

### Motion and animation

There is no decorative motion in Plain mode. The hero word does not animate in. The timer does not count down per-second (it updates silently in the background). Transitions are instant. Motion is reserved for functional feedback (button press depression).

---

## 6. Typography

**Preferred face:** [Atkinson Hyperlegible](https://brailleinstitute.org/freefont) — designed by the Braille Institute specifically for low-vision readers. It distinguishes characters (I / l / 1, O / 0) that ambiguous fonts collapse.

**Fallback:** SF Pro (iOS) / Roboto (Android) system fonts. Implementation uses a safe fallback:

```swift
// iOS — A11yFonts
if UIFont(name: "AtkinsonHyperlegible-Regular", size: size) != nil {
    return .custom("AtkinsonHyperlegible-Regular", size: size).weight(weight)
}
return .system(size: size, weight: weight)
```

**Sizes (floor, not ceiling):**

| Role | Size | Weight | Notes |
|---|---|---|---|
| Hero word (home) | 48 px | ExtraBold (800) | -1.5 letter-spacing |
| Onboarding title | 40 px | ExtraBold | -1.2 letter-spacing |
| Result title ("Safe to talk.") | 44 px | ExtraBold | -1 letter-spacing |
| Example word | 38 px | ExtraBold | -1 letter-spacing |
| Ask headline | 34 px | ExtraBold | Line-height 38 |
| Answer button label | 24 px | ExtraBold | — |
| Primary CTA | 22 px | Bold | — |
| Body | 19–22 px | Medium–Regular | Line-height ≥ 1.4× |
| Pill ("New word in X…") | 20 px | Bold | — |
| Help item label | 20 px | Bold | — |
| Tab bar label | 15 px | Bold | Smallest text in the app |
| Eyebrow (CAPS) | 14–18 px | Bold | 0.3–0.5 letter-spacing |

Note the tab bar labels are 15 px — the **only** text below 19 px in Plain mode. They pair with 22 px icons, so the effective target size is the full 60 px tab. If you find yourself adding any other text below 19 px, stop and reconsider.

---

## 7. Color Choices

The Plain palette (`A11Y` on iOS, `A11y` on Android) is a deliberate re-skin, not a tint:

| Token | Value | Role | Contrast on bg |
|---|---|---|---|
| `bg` | `#0B1220` | Deep navy base | — |
| `bgElev` | `#18243C` | Elevated cards | — |
| `bgInset` | `#24354F` | Pills, inset controls | — |
| `fg` | `#FFFFFF` | Primary text | 18.4:1 (AAA) |
| `fgMuted` | `#CBD5E1` | Secondary text | ~12:1 (AAA) |
| `fgFaint` | `#94A3B8` | Tertiary — icons only, never body | ~6:1 (AA) |
| `rule` | rgba(255,255,255,0.22) | 2 px borders | — |
| **`accent`** | **`#FFD23F`** | **Amber CTA, highlights** | **13.8:1 (AAA+)** |
| `accentInk` | `#0B1220` | Text on amber | 13.8:1 (AAA+) |
| `ok` | `#4ADE80` | "Yes, it matched" | 11.1:1 (AAA) |
| `danger` | `#FF6B6B` | "No, wrong word" / 911 | 4.8:1 on navy for text (AA); paired with dark ink for button labels |

**Why amber #FFD23F?** It's warmer than the teal in the Ink theme and reads unambiguously as *call-to-action* across cultures and lighting conditions. Its contrast ratio against the navy base is 13.8:1 — substantially beyond WCAG AAA's 7:1 threshold. This headroom matters because older users often have yellowed lens tissue that reduces effective contrast by 20–30%.

**Why green-for-safe and red-for-danger?** Universally recognized, pre-literate, cross-cultural. When a grandparent sees the red X screen, they know *something is wrong* before they read a word. The green/red pair on the answer buttons makes the screen legible in peripheral vision — the caller is demanding an answer, the user glances down, sees green-on-top, taps.

**What is `Color(hex: "#052e14")` and `#3a0a0a`?** Those are the dark-green and dark-red *ink* colors used for text/icons *on top of* the ok/danger buttons. They're specifically chosen to give 7:1+ contrast **against** the bright button color, so the button remains AAA-legible at all sizes.

---

## 8. Testing Suggestions

### System-level accessibility settings to test with

**iOS (Settings → Accessibility):**

- **Display & Text Size → Larger Text** — drag to the maximum AX5. Plain mode should remain legible; Ink mode screens typically break at this setting, Plain mode should not.
- **Display & Text Size → Bold Text** — should not cause layout collapse.
- **Display & Text Size → Increase Contrast** — our palette is already AAA, but verify nothing inverts badly.
- **Display & Text Size → Reduce Transparency** — no-op for us (we use solid fills), which is the correct behavior.
- **VoiceOver** — every interactive element must announce. Test the Verify flow end-to-end with VoiceOver on, eyes closed.
- **Switch Control** — tab order should be logical (top-to-bottom, one action per screen).

**Android (Settings → Accessibility):**

- **Display size** and **Font size** — push both to max. Layouts must reflow, never truncate the hero word or CTAs.
- **High contrast text** — we're already AAA; verify nothing regresses.
- **TalkBack** — equivalent to VoiceOver; every card, button, and pill must have a content description.
- **Color correction / inversion** — our design assumes none, but should remain *usable* with monochrome or deuteranopia filters on.

### Real-user testing

Automated contrast checks are necessary but not sufficient. Before shipping any changes to Plain mode:

1. **Put the phone in the hands of an 80-year-old.** Ask them to verify a fake scam call. Watch where their finger hovers. Listen for "I don't understand what this means."
2. **Put the phone in the hands of a 9-year-old who reads at a 2nd-grade level.** Ask them what "your circle" means, what they would tap if a stranger called.
3. **Hand the phone to someone with corrected vision at arm's length.** Can they read the hero word without bringing the phone closer?
4. **Use the phone outdoors in bright sunlight.** Dark mode washes out — does the amber CTA still pop?
5. **Use the phone with a tremor simulator or a thick-tipped stylus.** Can every button be hit without zooming?

Ship only after all five pass.

---

## 9. Future Work

Items discussed during design but deferred for first ship:

- **Drills / practice mode.** A "pretend someone is calling you" flow that walks users through Verify against a fake inbound call, so the first time they use Check is *not* the first time they see it. High value; needs careful copy so users don't confuse drills with real calls.
- **Text-size slider.** The Help screen already has "Change text size" stubbed — this would jump to a Plain-mode-scoped slider (base 19 px / large 22 px / huge 26 px) so users can push beyond our floor without leaving the app to change OS settings.
- **Family member quick-call shortcuts.** One-tap call buttons for "Call Mom" / "Call Dad" / "Call John" below the Verify mismatch screen, so a confused user can reach a trusted person immediately. Requires a contact-picker in Ink mode's Settings, per-circle-member.
- **Audio prompts.** Optional TTS readback of the hero word ("Your word today is Golden Robin") for users with severe visual impairment. Needs care — reading the word aloud while on a scam call is the exact failure mode we warn about.
- **Emergency contact integration.** The "Call them back on a trusted number" ghost button on the mismatch screen currently has a no-op action (`{}`). Populating this with the user's designated trusted contact from the circle is straightforward and high value.
- **Haptic feedback on match/mismatch.** A soft haptic on match, a stronger one on mismatch — reinforces the judgment for users who may mistrust on-screen color.

---

## 10. Implementation Pointers

For developers editing Plain mode code:

- **iOS:** `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Views/PlainModeViews.swift`
- **Android:** `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/plain/PlainMode.kt`
- **Color tokens iOS:** `/data/code/safewords-mobile/repos/safewords-ios/Safewords/Design/Theme.swift` (enum `A11Y`)
- **Color tokens Android:** `/data/code/safewords-mobile/repos/safewords-android/app/src/main/kotlin/com/thc/safewords/ui/theme/Color.kt` (object `A11y`)

Keep the two platforms visually identical. If iOS gets a new screen, Android gets it in the same PR. Divergence here is a regression — a family with a mixed-OS household depends on the Plain UX being the same on Grandma's iPhone and Grandpa's Pixel.

---

## Closing Note

Plain mode is the most important mode in this app. Ink mode is the pretty one for the people who don't need the app. Plain mode is the one that actually keeps someone's grandmother from sending $12,000 to a scammer at 11pm on a Tuesday.

If you are changing anything in Plain mode — a string, a spacing value, a color — please find one elderly person and one child, and watch them use it afterward. If they both succeed, ship it. If either fails, revert.
