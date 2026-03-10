# Word Lists Reference

## Frozen v1 Lists

Word lists are extracted from safewords.io and frozen in `shared/wordlists/`:

| File | Count | Source |
|------|-------|--------|
| `shared/wordlists/adjectives.json` | **197** | `safewords-io/.../en/wordlists/adjectives.ts` |
| `shared/wordlists/nouns.json` | **300** | `safewords-io/.../en/wordlists/nouns.ts` |

Each platform embeds an identical copy:
- **iOS**: `Safewords/Data/adjectives.json`, `nouns.json`
- **Android**: `app/src/main/assets/wordlists/adjectives.json`, `nouns.json`

## Original Source

```
/data/code/safewords-io/repos/safewords-web/src/lib/data/en/wordlists/
├── adjectives.ts   (197 words)
├── nouns.ts        (300 words)
└── verbs.ts        (106 words — not used in TOTP derivation)
```

## Curation Criteria

Words are curated to be:
- **Phone-safe** — easy to say/hear clearly over a phone call
- **Memorable** — concrete, vivid, common vocabulary
- **Unambiguous** — no homophones or easily confused words
- **Family-friendly** — no offensive or scary words

## Combination Space

For the "Adjective + Noun + Number" pattern:
- 197 adjectives × 300 nouns × 100 numbers = **5,910,000 combinations**
- Entropy: ~22.5 bits

## Versioning

**CRITICAL**: Word lists are integral to the TOTP algorithm. If lists change, the same seed+time produces different words, breaking sync between devices.

**Strategy**: Freeze v1 lists in the app bundle. Both platforms embed identical JSON files. If lists ever need updating, treat it as a new algorithm version — existing groups continue using v1 lists, new groups can use v2.

The single source of truth is `shared/wordlists/`. When lists need updating:
1. Update JSON files in `shared/wordlists/`
2. Copy to both `repos/safewords-ios/Safewords/Data/` and `repos/safewords-android/app/src/main/assets/wordlists/`
3. Bump version in test vectors
4. Run cross-platform tests
