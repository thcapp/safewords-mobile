# Maestro Testing — Coordination Plan

**Status**: in flight
**Goal**: Add Maestro UI tests for the highest-value cross-platform flows. Solve the stale-screenshots gate. Wire pre-release smoke gates into both platforms' fastlane.

**Scope**: deliberately narrow. We're not aiming for 80% UI coverage — unit tests already cover every primitive, migration, and BIP39 vector. Maestro is the integration tripwire for "did the nav graph or demo flow break" plus the screenshot capture pipeline.

## Phases

### Phase 0 — Test ID registry (DONE)
- `/shared/maestro-test-ids.md` — canonical accessibility-tag registry both platforms must implement.

### Phase 1 — Tag the apps (in parallel)

**Android** (subagent A): walk every Compose screen, add `Modifier.testTag(...)` against the registry. Must also set `semantics { testTagsAsResourceId = true }` at the root so testTag becomes a resource ID Maestro can find via `id:`.

**iOS** (codex): walk every SwiftUI surface, add `.accessibilityIdentifier(...)` against the same registry. No special wiring needed — SwiftUI exposes accessibilityIdentifier directly to UIAutomation/Maestro.

**Tooling install** (subagent C, in parallel with the tagging work):
- Install Maestro CLI on `u5` via `curl -Ls https://get.maestro.mobile.dev | bash`
- Smoke-test against the existing v1.3.1 debug APK on the emulator
- Report back with version + working hello-world flow output

### Phase 2 — Authoring flows (claude, after Phase 1)

Four flows in `maestro/flows/`:
1. `onboarding-create.yaml` — happy path, ends on Plain home with a real word
2. `onboarding-demo.yaml` — Try without a group → demo banner appears → Set up real group wipes demo
3. `onboarding-restore.yaml` — paste seed `0102...1f20`, expect known phrase from `/shared/test-vectors.json` (validates the cross-platform contract end-to-end through the UI)
4. `parity-check.yaml` — runs Demo on both apps, asserts the words match

Plus one screenshot flow:
- `screenshots.yaml` — walks Plain home, Settings → Verification, Safety Cards, ChallengeSheet, demo state. Captures fresh v1.3 visuals for Play + App Store listings. **Solves the stale-screenshots production-submission gate.**

All flows live under `maestro/` at the repo root, not under `repos/safewords-{ios,android}/` — they target both apps from the same source.

### Phase 3 — CI / fastlane wiring (subagent B, after Phase 2 lands)

**Android Fastfile** (`repos/safewords-android/fastlane/Fastfile`):
- New lane `:maestro_test` — runs the 4 phase-1 flows against the emulator
- New lane `:screenshots_maestro` — runs `screenshots.yaml` and dumps captures into `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- Modify `:internal` lane to call `:maestro_test` first; fail closed

**iOS Fastfile** (`repos/safewords-ios/fastlane/Fastfile`):
- Same lane shape but targeting iOS simulator
- Hook into `:beta` lane

**GH Actions**:
- iOS `ios-release.yml` — add a step before the build that runs Maestro against a freshly booted simulator
- Add new workflow `android-test.yml` (we don't have one yet) that runs `:app:testDebugUnitTest` AND Maestro flows against the emulator

### Phase 4 — Documentation (final, after all wiring works)

`docs/testing.md` — covers:
- Maestro install + first run
- How the registry works
- How to add a new flow
- The screenshot pipeline
- The pre-release gate
- Common failures (covered by Maestro gotchas appendix in `docs/release-pipeline-gotchas.md`)

Update `docs/README.md` index to point at `testing.md`.

## Coordination

- **Worker A** (general-purpose subagent): Android tagging
- **Worker B** (general-purpose subagent): fastlane / CI wiring (Phase 3)
- **Worker C** (general-purpose subagent): Maestro install + smoke
- **Worker D** (general-purpose subagent): docs/testing.md (Phase 4)
- **Codex**: iOS tagging (Phase 1)
- **Claude (this session)**: registry, plan, YAML flows (Phase 2), final review, merges

Each worker reports back with a diff and a summary of what they did. Claude commits.

## Constraints

- **No automated biometric flows** — system biometric sheets aren't in the accessibility tree. Tests skip card printing + override reveal.
- **No automated camera flows** — emulator camera is unreliable. Skip QR scanner; rely on `QRCodeService.parseQRPayload` unit test + manual smoke.
- **iOS local runs need a Mac** — both VMs lack one. iOS Maestro runs on the GH Actions macos-latest runner; Android Maestro runs on `u5`.

## Success criteria

1. All 4 phase-1 flows pass against both apps (where applicable — parity-check needs both running)
2. `screenshots.yaml` produces a complete set of fresh v1.3 captures in the right device sizes for Play + App Store
3. Pre-release fastlane lanes fail closed if any flow regresses
4. `docs/testing.md` documents the workflow end-to-end so the next person doesn't need to ask
