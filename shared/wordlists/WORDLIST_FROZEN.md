# Wordlists are frozen for v1

The two JSON files in this directory — `adjectives.json` (197 entries) and `nouns.json` (300 entries) — are part of the **TOTP algorithm contract**, not loose data. They are immutable for the lifetime of v1.

## Why

The TOTP derivation in `docs/totp-word-algorithm.md` uses:

```
adj_idx  = ((hash[offset]   & 0x7F) << 8 | hash[offset+1]) % 197
noun_idx = ((hash[offset+2] & 0x7F) << 8 | hash[offset+3]) % 300
```

Changing the count or order of either list changes which word a given (seed, time) pair produces. The same seed will produce a different safeword on the day after the change vs. the day before it — silently, with no error. Two devices on the same group, one on the old list and one on the new, will disagree about today's word and the verify flow will fail. Every test vector in `shared/test-vectors.json` will become invalid.

This is the highest-risk class of change the project has — it cannot be feature-flagged, it cannot be rolled out gradually, and it has no error path that surfaces the divergence.

## What this means

- **Do not add words.** Even appending to the end shifts indexes via `% 197` / `% 300` and breaks past safewords.
- **Do not remove words.** Same problem.
- **Do not reorder.** Same problem.
- **Do not "fix" capitalization, hyphenation, or punctuation.** The exact UTF-8 strings here are what the apps render.
- **Do not edit the file format.** JSON arrays in this exact shape are what `WordLists.kt` (Android) and the iOS equivalent parse.

## How to expand them (if/when the time comes)

If you genuinely need a larger word universe, that is a **v2 algorithm bump**, not a wordlist edit:

1. Decide the new counts. Document the rationale.
2. Bump the algorithm version in `docs/totp-word-algorithm.md`. Add a `version` field to the QR schema and group seed metadata.
3. Generate a new test vector pack at `shared/test-vectors-v2.json`. Keep `shared/test-vectors.json` for v1 verification.
4. Implement v2 alongside v1 in both apps. New groups default to v2; existing v1 groups stay on v1 until the user explicitly migrates (which itself requires every member to re-scan a v2 QR).
5. Coordinate the release across mobile-android, mobile-ios, and safewords-io's `src/lib/data/shared/wordlists/` (which copies these files for app-parity surfaces like the inline `/` generator).

## Where else these lists live

- **safewords-io**: `repos/safewords-web/src/lib/data/shared/wordlists/` — exact copies, used by the inline `/` generator and any web surface that claims app-parity.
- **safewords-io expanded lists**: `repos/safewords-web/src/lib/data/en/wordlists/` (256/512) — a separate, expanded set used by `/generator`'s explicit-pick UI. NOT consumed by the algorithm and NOT subject to this freeze. The two-tier model is intentional.

If you're considering modifying anything in this directory, stop and read this file first. If you've read this file and still believe the change is necessary, follow the v2 process above rather than editing in place.

— Pinned 2026-04-28
