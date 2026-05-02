# Safewords Mobile — Documentation

Native iOS (Swift/SwiftUI) and Android (Kotlin/Compose) apps for rotating TOTP-based safewords. Companion to [safewords.io](https://safewords.io).

**Current ship state** (2026-05-01): v1.3.1 on both platforms. Android on Play internal track; iOS on TestFlight. See `release-state.md`.

## Read in this order

| # | Doc | What it's for |
|---|-----|---------------|
| 1 | [developer-guide.md](developer-guide.md) | Start here. Prerequisites, clone-to-build instructions for both platforms (Android builds run on `u5`), repo layout, safety rules, where docs live. |
| 2 | [feature-spec.md](feature-spec.md) | Product requirements — original spec plus Phase 4 (BIP39, primitives, Plain default, cards, demo mode). |
| 3 | [v1.3-architecture.md](v1.3-architecture.md) | **As-built reference for v1.3** — primitives, group config schema, demo mode, native card rendering. Read after feature-spec. |
| 4 | [v1.3-best-in-class-design.md](v1.3-best-in-class-design.md) | Locked design brief that drove v1.3 (claude + codex iteration log). |
| 5 | [safewords-mobile.md](safewords-mobile.md) | **Page-by-page reference** — every screen on both platforms, side by side, with technical implementation details. The one-stop tour. |
| 6 | [safety-cards.md](safety-cards.md) | Cards system: types, sensitivity tiers, copy schema, render pipelines, biometric gating. |
| 7 | [demo-mode.md](demo-mode.md) | Demo seed, group, lifecycle, cross-platform parity. |
| 8 | [design-system.md](design-system.md) | Visual design tokens. Ink + A11Y palettes, typography, components, motion. |
| 9 | [ios-architecture.md](ios-architecture.md) | iOS code reference. Every file, every screen, routing, widget, services, build. |
| 10 | [android-architecture.md](android-architecture.md) | Android code reference. Module layout, every screen, navigation, Glance widget, services, Gradle. |
| 11 | [plain-mode.md](plain-mode.md) | Plain Mode — the v1.3 default home. Large-type layout, screen walkthrough, testing notes. |
| 12 | [totp-algorithm-reference.md](totp-algorithm-reference.md) | Cross-platform TOTP derivation reference. Algorithm, contract, test vectors, pitfalls. |
| 13 | [totp-word-algorithm.md](totp-word-algorithm.md) | Original algorithm spec (frozen). Source of truth for the v1.0 derivation rules. |
| 14 | [word-lists.md](word-lists.md) | Frozen 197-adjective + 300-noun v1 lists and how they're embedded. |
| 15 | [testing.md](testing.md) | **How we test, end-to-end.** Unit + Maestro layers, running flows on `u5` (Android) and the macOS runner (iOS), the test-ID registry, screenshot pipeline, pre-release gates, parity validation, common flow-authoring failures. Read this before adding or running a Maestro flow. |
| 16 | [release-state.md](release-state.md) | Audit-able snapshot of Play and TestFlight tracks. |
| 17 | [release-pipeline-gotchas.md](release-pipeline-gotchas.md) | Postmortem of every failure mode we hit setting up Play and TestFlight pipelines (includes the Maestro environment gotchas appendix). |
| 18 | [design-handoff-log.md](design-handoff-log.md) | Record of design handoffs and deferred work. |
| 19 | [best-in-class-expansion-proposal.md](best-in-class-expansion-proposal.md) | Codex's original v1.3 expansion proposal (historical, superseded by v1.3-best-in-class-design.md). |

## By role

**New engineer, day one** → `developer-guide.md` → `v1.3-architecture.md` → platform-specific architecture doc.

**Designer / product** → `feature-spec.md` → `v1.3-best-in-class-design.md` → `design-system.md` → `plain-mode.md`.

**Security / crypto reviewer** → `totp-algorithm-reference.md` → `totp-word-algorithm.md` → `v1.3-architecture.md` (primitive derivations section) → `../shared/recovery-schema.md` → `word-lists.md`.

**Release / ops** → `release-state.md` → `release-pipeline-gotchas.md` → `testing.md` → `developer-guide.md` (build commands).

**Adding or debugging a Maestro flow** → `testing.md` → `../shared/maestro-test-ids.md` → `release-pipeline-gotchas.md` (Maestro section).

**Picking up deferred work** → `design-handoff-log.md` → `feature-spec.md` "Deferred / future" → relevant architecture doc.

## Out-of-scope references

- `/data/code/safewords-mobile/.claude/CLAUDE.md` — working instructions for Claude Code sessions on this project
- `/data/code/safewords-mobile/shared/` — cross-platform contracts (word lists, test vectors, primitive derivations, migration vectors, card copy)
- `/data/code/safewords-io/` — the web app this mobile project extends

## Conventions

- All docs are markdown.
- Absolute paths are used when referencing source files so the docs are greppable.
- Hex colors and algorithm values match the native implementation verbatim; if the code changes, update the docs in the same commit.
- v1.3 docs live alongside historical specs; historical specs aren't deleted, just updated to point at the as-built reference.
