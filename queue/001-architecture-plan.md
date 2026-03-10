# Task: Produce Architecture & Implementation Plan

## Priority: HIGH
## Status: READY

## Objective

Before writing any code, produce a comprehensive technical architecture document that covers:

1. **Expo project setup** — SDK version, dependencies, project structure, Expo Router configuration
2. **TOTP word derivation module** — Implement and test the core algorithm with frozen test vectors. This is the foundation everything else builds on.
3. **Data layer** — Zustand store design, SecureStore integration, group/member CRUD
4. **QR code flow** — Encoding format, camera permissions, scanning UX
5. **Widget architecture** — How to bridge the TOTP module to iOS WidgetKit and Android App Widgets via Expo
6. **Navigation structure** — Tab bar, screen hierarchy, deep linking
7. **Testing strategy** — Unit tests for crypto, integration tests for storage, E2E for critical flows
8. **Build & distribution** — EAS Build configuration, app store submission checklist

## Key Decisions Needed

- **State management**: Zustand vs. Jotai vs. React Context — given the simple data model, which fits best?
- **Widget bridge**: expo-apple-targets (community) vs. custom native module for WidgetKit?
- **Word list versioning**: Embed in app bundle vs. fetch from CDN (with offline fallback)?
- **Navigation**: Expo Router file-based routing — flat or nested layout?
- **Styling**: NativeWind (Tailwind) vs. StyleSheet vs. Tamagui?
- **Crypto library**: expo-crypto (HMAC support?) vs. react-native-quick-crypto vs. noble-hashes (JS-only)?

## Inputs

- `docs/feature-spec.md` — Full feature specification
- `docs/totp-word-algorithm.md` — TOTP derivation algorithm spec
- `/data/code/safewords-io/repos/safewords-web/src/lib/data/` — Existing word lists
- `/data/code/safewords-io/repos/safewords-web/src/routes/app/+page.svelte` — Marketing page with UI mockups

## Output

Write the architecture plan to `docs/architecture-plan.md`. It should be detailed enough that an agent can implement Phase 1 from it without ambiguity.
