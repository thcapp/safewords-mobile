# Safewords Mobile — Feature Specification

## Overview

A native mobile app (iOS + Android) that generates **time-based rotating safewords** shared across family members' devices. Uses the same cryptographic principle as TOTP (Time-based One-Time Password) but outputs human-readable words instead of 6-digit codes.

## Core Concept

```
Shared Seed (256-bit) + Time Window → HMAC-SHA256 → Word Mapping → "Crimson Eagle 47"
```

Every device with the same seed, at the same time, produces the same phrase. No server, no internet, no accounts.

---

## Phase 1: MVP

### F1. TOTP Word Derivation Engine

**What**: Given a seed and the current time, derive a human-readable phrase.

**Algorithm**:
1. Compute `counter = floor(unix_timestamp / interval_seconds)`
2. Compute `hash = HMAC-SHA256(seed, counter_as_bytes)`
3. Extract deterministic indices from hash bytes
4. Map indices to word lists: `adjective + noun + number(0-99)`
5. Format: "Crimson Eagle 47"

**Intervals**: Hourly (3600s), Daily (86400s), Weekly (604800s), Monthly (≈2592000s)

**Word lists**: Reuse from safewords.io web app (`/data/code/safewords-io/repos/safewords-web/src/lib/data/`) — adjectives, nouns, already curated to be phone-safe and memorable.

**Requirements**:
- Deterministic: same inputs always produce same output
- Cross-platform: identical results on iOS, Android, and (future) web
- Time-zone independent: uses UTC epoch
- Testable: given seed + timestamp → known output (create test vectors)

### F2. Family Groups

**What**: Create and manage groups of family members who share a safeword.

**Data model**:
```typescript
interface Group {
  id: string;             // UUID
  name: string;           // "Johnson Family"
  seed: Uint8Array;       // 256-bit, stored in SecureStore
  interval: Interval;     // 'hourly' | 'daily' | 'weekly' | 'monthly'
  members: Member[];
  createdAt: number;      // unix timestamp
}

interface Member {
  id: string;
  name: string;           // Display name
  role: 'creator' | 'member';
  joinedAt: number;
}
```

**Operations**:
- Create group → generates random seed, stores in SecureStore
- Join group → scan QR code containing seed (in-person only)
- View current word for each group
- Edit group name, interval
- Remove member (local only — cannot remotely revoke)
- Delete group (deletes local seed)

**Storage**:
- Seeds: platform SecureStore (Keychain on iOS, Keystore on Android)
- Group metadata: encrypted local storage
- No cloud sync, no server

### F3. QR Code Sharing

**What**: Share a group's seed via QR code displayed on-screen, scanned by another device.

**QR payload** (JSON, base64url-encoded):
```json
{
  "v": 1,
  "name": "Johnson Family",
  "seed": "<base64url-encoded 256-bit seed>",
  "interval": "daily"
}
```

**Security considerations**:
- QR must be shown/scanned in person — this is the trust model
- The app should warn: "Only share this QR code in person"
- After scanning, the seed lives in SecureStore on the new device
- Consider: show QR briefly with auto-dismiss timer?

### F4. Home Screen Widget

**What**: A widget showing the current safeword without opening the app.

**iOS (WidgetKit)**:
- Small widget: safeword text + countdown
- Medium widget: safeword + group name + countdown
- Timeline-based: pre-compute entries for the next rotation window
- Access seed via App Group shared Keychain

**Android (App Widget)**:
- Similar layout to iOS
- Update via WorkManager on interval
- Access seed from encrypted SharedPreferences

**Design**: Matches the mockup on safewords.io/app — frosted glass look, current word prominent, small countdown timer.

### F5. Main App Screens

**Navigation** (tab bar):
1. **Home** — Current safeword for default group, large display, countdown ring
2. **Groups** — List of groups, tap to view/manage
3. **Settings** — Interval defaults, notifications, about

**Home screen**:
- Large circular countdown ring (SVG/canvas)
- Current word in large text
- "Rotates in HH:MM:SS" countdown
- Group selector if multiple groups exist

**Group detail screen**:
- Group name (editable)
- Member list with colored initials
- Current safeword for this group
- "Invite Member" button → shows QR code
- "Join Group" button → opens camera for QR scan
- Interval selector
- Delete group (with confirmation)

---

## Phase 2

### F6. SMS Fallback

**What**: Automatically send an SMS with the current safeword to designated family members when it rotates.

**Setup**:
- In group settings, toggle "SMS fallback" per member
- Enter phone number for SMS-only members
- Set quiet hours (no SMS between 10pm-7am)

**Behavior**:
- When safeword rotates, if SMS enabled, send message:
  "Safewords: Your {group_name} family word is '{word}'. Valid until {expiry}."
- Rate limit: max 1 SMS per rotation interval
- Requires SMS permission on Android; uses MFMessageComposeViewController on iOS

### F7. Push Notifications

**What**: Optional notification when a safeword rotates.

- Local notifications only (no server needed)
- Schedule based on rotation interval
- "Your family safeword has changed. Open to view."
- Tapping opens the app to the Home screen

---

## Phase 3

### F8. Challenge-Response Mode

**What**: Instead of just a static word, generate rotating question-answer pairs.

**Algorithm**: Extend TOTP derivation to produce both a question template and answer:
- Q: "What color is our family {noun}?" → A: "Crimson"
- Q: "What number comes after our {adjective}?" → A: "47"

### F9. Emergency Override

**What**: A static backup word that works even without the app.

- Generated once per group, stored alongside the seed
- Displayed in group settings under "Emergency"
- Single-use: after use, must regenerate
- Serves as fallback when someone can't access their device

---

## Non-Functional Requirements

- **Startup time**: < 2 seconds to display current word
- **Bundle size**: < 30MB
- **Battery**: Widget updates must not drain battery (use timeline-based updates)
- **Accessibility**: VoiceOver/TalkBack support, minimum 44pt touch targets
- **Localization**: English initially, structure for future i18n (reuse safewords.io locale keys where applicable)

## Design Language

Follow the safewords.io design system:
- Dark theme for primary screens (matches app marketing page)
- Font: system font stack (SF Pro on iOS, Roboto on Android)
- Accent color: teal (#0f766e / #2dd4bf)
- CTA color: amber (#d97706)
- Countdown ring: teal gradient on dark background
