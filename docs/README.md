# Safewords Mobile — Documentation

Native iOS (Swift/SwiftUI) and Android (Kotlin/Compose) apps for rotating TOTP-based family safewords. Companion to [safewords.io](https://safewords.io).

## Read in this order

| # | Doc | What it's for |
|---|-----|---------------|
| 1 | [developer-guide.md](developer-guide.md) | Start here. Prerequisites, clone-to-build instructions for both platforms, repo layout, safety rules. |
| 2 | [feature-spec.md](feature-spec.md) | Product requirements — what the app does, all phases, user stories. |
| 3 | [design-system.md](design-system.md) | Visual design tokens. Ink + A11Y palettes, typography, components, motion. Per-token comparison table across design → iOS → Android. |
| 4 | [ios-architecture.md](ios-architecture.md) | iOS code reference. Every file, every screen, routing, widget, services, build. |
| 5 | [android-architecture.md](android-architecture.md) | Android code reference. Module layout, every screen, navigation, Glance widget, services, Gradle. |
| 6 | [plain-mode.md](plain-mode.md) | Accessibility mode guide. Why it exists, screen-by-screen walkthrough, testing notes, future work. |
| 7 | [totp-algorithm-reference.md](totp-algorithm-reference.md) | Technical reference for the cross-platform TOTP derivation. Algorithm, contract, test vectors, pitfalls. |
| 8 | [totp-word-algorithm.md](totp-word-algorithm.md) | Original algorithm spec (frozen). Source of truth for the derivation rules. |
| 9 | [word-lists.md](word-lists.md) | The frozen 197-adjective + 300-noun v1 lists and how they're embedded. |
| 10 | [design-handoff-log.md](design-handoff-log.md) | Record of the 2026-04-20 Claude Design handoff — what shipped, what was deferred, platform adaptations. |

## By role

**New engineer, day one** → `developer-guide.md` → `ios-architecture.md` or `android-architecture.md` depending on your platform.

**Designer / product** → `feature-spec.md` → `design-system.md` → `plain-mode.md`.

**Security / crypto reviewer** → `totp-algorithm-reference.md` → `totp-word-algorithm.md` → `word-lists.md`.

**Picking up deferred work** → `design-handoff-log.md` (known gaps list) → relevant architecture doc.

## Out-of-scope references

- `/data/code/safewords-mobile/.claude/CLAUDE.md` — working instructions for Claude Code sessions
- `/data/code/safewords-mobile/shared/` — cross-platform contracts (word lists, test vectors, QR schema)
- `/data/code/safewords-io/` — the web app this mobile project extends

## Conventions

- All docs are markdown, linted via ordinary `prettier`-compatible wrapping.
- Absolute paths are used when referencing source files so the docs are greppable.
- Hex colors and algorithm values match the native implementation verbatim; if the code changes, update the docs in the same PR.
