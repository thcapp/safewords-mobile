# Safewords Recovery Phrase Schema v1

Status: proposal for v1.2.0 implementation

This document defines the cross-platform recovery phrase contract for Safewords group seeds. It is intentionally app-level and deterministic: the same 32-byte group seed must encode to the same phrase on Android and iOS, and the same phrase must decode to the same 32-byte group seed.

## Decision

Use **BIP39 English, 24 words, no passphrase, entropy-only**.

Safewords already uses a 256-bit group seed. BIP39 maps entropy to mnemonic words with checksum bits. For 256-bit entropy, BIP39 produces 24 words:

```text
256 entropy bits + 8 checksum bits = 264 bits
264 bits / 11 bits per word index = 24 words
```

Important: Safewords uses the BIP39 mnemonic only as an encoding of the existing group seed. It does **not** use BIP39 PBKDF2 seed derivation, passphrases, wallets, or accounts. Decoding a valid phrase returns the original 32-byte group seed directly.

## Options Considered

| Option | Entropy Fit | Checksum | Library Cost | UX | Decision |
|---|---:|---:|---|---|---|
| BIP39 12-word | 128-bit entropy only | 4 bits | Mature libraries on iOS/Android, or small local implementation | Shorter and easier to type | Rejected. It cannot losslessly encode the current 32-byte/256-bit group seed. |
| BIP39 24-word | 256-bit entropy exactly | 8 bits | Mature libraries, deterministic word list, simple implementation | Longer, but familiar and robust | Recommended. Matches current seed size without weakening security. |
| Custom word code | Flexible | We would need to design one | More code, more test surface, more drift risk | Could be shorter if we choose larger words/check symbols | Rejected for v1.2. Mature checksum and interop matter more. |

## Canonical Wire Format

The canonical serialized phrase is a UTF-8 string containing exactly 24 lowercase English BIP39 words separated by one ASCII space (`U+0020`).

Example:

```text
abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art
```

### Word List

Use the official BIP39 English word list, in its canonical index order, with exactly 2048 lowercase ASCII words.

The word list is part of the recovery contract. Changing the list or order requires a new schema version.

### Normalization

Parsers must normalize user input before validation:

1. Decode as UTF-8.
2. Apply Unicode NFKD normalization.
3. Trim leading and trailing whitespace.
4. Lowercase using a locale-stable rule (`en_US_POSIX` on iOS, `Locale.ROOT` on Android).
5. Split on one or more Unicode whitespace characters.
6. Re-join internally with single ASCII spaces for checksum validation.

Accepted non-canonical input examples:

```text
  ABANDON ABANDON ... ART  
abandon
abandon
...
art
```

Rejected input examples:

```text
1. abandon 2. abandon ...
abandon, abandon, ...
abandon-abandon-...
```

Numbering, commas, hyphens, punctuation, and non-word tokens are not part of v1. They should produce validation errors rather than being silently stripped.

## Encoding Algorithm

Given a 32-byte seed:

1. Let `entropy` be the 256-bit seed bytes.
2. Compute `hash = SHA256(entropy)`.
3. Take the first 8 bits of `hash` as checksum.
4. Append checksum bits to entropy bits, yielding 264 bits.
5. Split the 264 bits into 24 chunks of 11 bits.
6. Interpret each 11-bit chunk as an integer `0...2047`.
7. Map each index to the BIP39 English word list.
8. Join the 24 words with ASCII spaces.

## Decoding Algorithm

Given user input:

1. Normalize as defined above.
2. Require exactly 24 words.
3. Require every word to exist in the BIP39 English list.
4. Map words to 24 11-bit indexes.
5. Concatenate into 264 bits.
6. Split into 256 entropy bits and 8 checksum bits.
7. Compute `SHA256(entropy)` and compare the first 8 bits to the checksum bits.
8. If checksum matches, return the 32-byte entropy as the Safewords group seed.

## Error Codes And User Messages

Implementations should keep internal error codes stable and map them to identical user-facing messages.

| Condition | Error Code | User-Facing Message | Notes |
|---|---|---|---|
| Input is empty after trimming | `EMPTY_INPUT` | `Enter your 24-word recovery phrase.` | Do not show word-list details yet. |
| Word count is not 24 | `WRONG_WORD_COUNT` | `Recovery phrase must be exactly 24 words.` | 12-word BIP39 phrases are valid BIP39 but invalid Safewords group recovery phrases. |
| A token is not in the BIP39 English list | `UNKNOWN_WORD` | `Word {index} is not in the recovery word list: "{word}".` | `{index}` is 1-based after normalization. |
| All words are known but checksum does not match | `BAD_CHECKSUM` | `Recovery phrase checksum is invalid. Check the words and order.` | This catches mistyped valid words and wrong order. |
| Input contains punctuation, numbering, commas, or hyphens that create non-word tokens | `UNKNOWN_WORD` | `Word {index} is not in the recovery word list: "{word}".` | Keep parser strict for v1. |
| Duplicate words appear | none | none | Duplicate words are allowed by BIP39. The all-zero seed intentionally contains repeated `abandon`. Do not reject duplicates. |
| Mixed case or leading/trailing spaces | none | none | Normalize and accept if the phrase is otherwise valid. |

## Test Vector Contract

Both Android and iOS must consume `/shared/recovery-vectors.json` directly in tests. No platform may maintain a private copy of these vectors.

JSON shape:

```json
{
  "version": 1,
  "scheme": "bip39-english-entropy256",
  "wordCount": 24,
  "seedBytes": 32,
  "normalization": "NFKD trim lowercase collapse-whitespace",
  "valid": [
    {
      "id": "zero-256",
      "seedHex": "<64 lowercase hex chars>",
      "mnemonic": "<24 BIP39 English words>"
    }
  ],
  "invalid": [
    {
      "id": "empty-input",
      "input": "",
      "expectedError": "EMPTY_INPUT",
      "expectedMessage": "Enter your 24-word recovery phrase."
    }
  ]
}
```

Required assertions for each valid vector:

- `encode(seedHex) == mnemonic`
- `decode(mnemonic) == seedHex`
- `decode(mixedCaseOrMultilineVariant)` returns the same seed when a `normalizedInput` field exists

Required assertions for each invalid vector:

- `decode(input)` fails
- returned error code equals `expectedError`
- user-facing message equals `expectedMessage`

## Included Vectors

The canonical vectors live in `/shared/recovery-vectors.json`. They include:

| ID | Type | Purpose |
|---|---|---|
| `zero-256` | valid | BIP39 known edge case with repeated `abandon`; proves duplicate words are allowed. |
| `ff-256` | valid | BIP39 known edge case with repeated `zoo`. |
| `increment-00-1f` | valid | Sequential byte seed, useful for manual bit-boundary debugging. |
| `safewords-baseline` | valid | Existing Safewords baseline seed from `shared/test-vectors.json` shifted by one byte range. |
| `alternating-aa55` | valid | Alternating bit pattern. |
| `pattern-deadbeef` | valid | Repeating nontrivial hex pattern. |
| `empty-input` | invalid | Empty field handling. |
| `wrong-word-count-12` | invalid | Rejects 12-word phrases because Safewords requires 256-bit group seeds. |
| `unknown-word` | invalid | Unknown token handling with 1-based word index. |
| `bad-checksum` | invalid | Known words and right count, wrong checksum. |
| `mixed-case-leading-spaces` | valid normalization | Proves case and surrounding whitespace normalize successfully. |

## Implementation Notes

- Store the decoded seed exactly as 32 bytes in SecureStorage/Keychain.
- Do not store the mnemonic unless the user is actively viewing/exporting it.
- Do not send the mnemonic to logs, analytics, crash reports, or notifications.
- If biometric protection is enabled, require it before showing Settings -> Back up seed phrase.
- UI may display the 24 words in a 4x6 grid, but copy/export must use the canonical single-space string.
- QR invites remain the preferred setup path. Recovery phrases are for backup, device loss, and no-camera flows.
