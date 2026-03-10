# Word Lists Reference

## Source

Word lists are defined in the safewords.io web app:
```
/data/code/safewords-io/repos/safewords-web/src/lib/data/en/wordlists/
├── adjectives.ts   (~208 words)
├── nouns.ts        (~315 words)
└── verbs.ts        (~106 words)
```

Re-exported via:
```
/data/code/safewords-io/repos/safewords-web/src/lib/data/wordlists/
├── adjectives.ts   → re-exports from en/
├── nouns.ts        → re-exports from en/
└── verbs.ts        → re-exports from en/
```

## Format

Each file exports a `string[]`:
```typescript
export const adjectives: string[] = [
  "golden", "silver", "purple", "crimson", ...
];
```

Words are curated to be:
- Phone-safe (easy to say/hear clearly over a phone call)
- Memorable (concrete, vivid, common vocabulary)
- Unambiguous (no homophones or easily confused words)
- Family-friendly (no offensive or scary words)

## Combination Space

For the "Adjective + Noun + Number" pattern:
- ~208 adjectives × ~315 nouns × 100 numbers = **~6.5M combinations**
- Entropy: ~22.6 bits (adjective+noun alone = ~16 bits; with number = ~22.6 bits)

For stronger phrases ("Noun + Verb + Noun + Number"):
- ~315 × ~106 × ~315 × 100 = **~1.05B combinations** (~30 bits)

## Versioning Strategy

**CRITICAL**: Word lists are integral to the TOTP algorithm. If lists change, the same seed+time produces different words, breaking sync between devices.

Options:
1. **Freeze v1 lists in the app bundle** — simplest, most reliable
2. **Version lists with group metadata** — group stores list version, app ships multiple versions
3. **Hash the list** — store SHA256 of the combined list alongside the seed to detect mismatches

Recommendation: Option 1 for MVP. Embed a frozen copy. If lists ever need updating, treat it as a new "algorithm version" and handle migration.
