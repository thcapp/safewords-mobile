# safewords-mobile

Native mobile app for rotating TOTP-based family safewords. iOS & Android via React Native / Expo.

**Status: PLANNING** — No code exists yet. The first task is to produce a detailed technical plan before writing any code.

## Product Context

Companion app to [safewords.io](https://safewords.io) — a web app that helps families create verification safewords to defend against AI deepfake scams. The web app (static SvelteKit site) provides a one-time generator, protocol builder, drills, and educational content. This mobile app extends the concept with **automatic rotation** of safewords using time-based cryptography.

The marketing page describing the planned features is live at: https://safewords.io/app
Source: `/data/code/safewords-io/repos/safewords-web/src/routes/app/+page.svelte`

## Core Features (Planned)

### Phase 1 — MVP
1. **TOTP-based rotating safewords** — Derive a human-readable word/phrase from a shared cryptographic seed + current time window. All devices with the same seed show the same word. Configurable rotation: hourly, daily, weekly, monthly.
2. **Family groups** — Create a group, share the seed via QR code (in-person only). View members list. No accounts, no server, no cloud.
3. **Home screen widget** — iOS WidgetKit / Android App Widget showing the current safeword and countdown to next rotation.
4. **Offline-first** — Everything works without internet. Seed stored locally in secure storage (Keychain / Keystore).

### Phase 2
5. **SMS fallback** — For family members who won't install the app, send an automatic SMS with the current word when it rotates.
6. **Push notifications** — Notify when a safeword rotates (optional).

### Phase 3
7. **Challenge-response** — Rotating Q&A pairs derived from the seed. Emergency static override (single-use).

## Technical Direction

### Framework: React Native + Expo
- **Expo SDK 53+** with Expo Router for navigation
- **EAS Build** for app store builds
- TypeScript throughout
- Expo SecureStore for seed storage
- expo-crypto for HMAC/SHA operations
- expo-widgets (community) or native modules for home screen widgets

### TOTP Word Derivation
The core algorithm: `HMAC-SHA256(seed, floor(timestamp / interval))` → map output bytes to a word list → produce human-readable phrase (e.g., "Crimson Eagle 47"). Same algorithm on every device = same word at the same time. No server needed.

### Data Model (Local Only)
- **Group**: `{ id, name, seed (encrypted), interval, createdAt, members[] }`
- **Member**: `{ id, name, role, joinedAt }`
- **Seed**: 256-bit random, stored in platform secure storage, never transmitted digitally
- **Settings**: `{ notificationsEnabled, smsEnabled, smsRecipients[] }`

### Key Constraints
- **Zero network dependency** for core function (word derivation)
- **No user accounts** — groups are purely local + QR-shared
- **No backend server** — all computation is on-device
- **Secure storage only** — seeds never touch AsyncStorage or plain files
- **Minimal permissions** — camera (QR scan), SMS (Phase 2 only), notifications (Phase 2)

## Structure

```
safewords-mobile/
├── repos/safewords-app/    # Main Expo app (to be created)
│   ├── app/                # Expo Router pages
│   ├── src/
│   │   ├── crypto/         # TOTP derivation, HMAC, seed generation
│   │   ├── components/     # Shared UI components
│   │   ├── stores/         # Zustand stores (groups, settings)
│   │   ├── services/       # QR, SMS, notifications
│   │   ├── widgets/        # Widget bridge code
│   │   └── types/          # TypeScript interfaces
│   ├── assets/             # Fonts, images
│   └── package.json
├── docs/                   # Feature specs, architecture docs
├── inbox/                  # Drop zone for input
├── ops/                    # Scripts, tools
├── output/                 # Builds, artifacts
├── queue/                  # Pending work items
├── state/                  # Logs, memory, context
└── templates/              # Reusable templates
```

## CLI Tools

Use credential-injecting wrappers instead of raw CLI commands:
- `gh` — aliased to `gh-wrap`, auto-switches GitHub account based on realm
- `aw` — Appwrite CLI wrapper (if needed later)
- `wr` — Wrangler wrapper (not needed for this project)

These wrappers auto-detect the correct profile from this base's realm (`thc`).

## Related Bases

- **safewords-io** (`/data/code/safewords-io/`) — The web app. Marketing page at `/app` describes the mobile features. Word lists in `repos/safewords-web/src/lib/data/` could be reused.

## Flow

```
queue/ (input) → process → output/ (result)
                    ↓
               state/ (memory)
```

## Safety

- Never commit seed values, API keys, or `.env` contents
- Never hardcode cryptographic constants — derive from standard algorithms
- The TOTP derivation must be deterministic and cross-platform (same seed + time = same word on iOS, Android, and potentially web)
