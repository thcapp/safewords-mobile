# TOTP Word Derivation Algorithm

## Overview

Transform a shared secret + current time into a deterministic human-readable phrase, using the same principle as RFC 6238 (TOTP) but mapping to words instead of digits.

## Algorithm

```
Input:
  seed:     bytes (32 bytes / 256 bits)
  interval: int (seconds — 3600, 86400, 604800, or 2592000)
  timestamp: int (unix epoch seconds, default = now)

Steps:
  1. counter = floor(timestamp / interval)
  2. counter_bytes = int64_to_big_endian_bytes(counter)
  3. hash = HMAC-SHA256(key=seed, message=counter_bytes)
  4. // hash is 32 bytes

  // Extract indices (similar to HOTP dynamic truncation)
  5. offset = hash[31] & 0x0F
  6. adj_index  = ((hash[offset] & 0x7F) << 8 | hash[offset+1]) % 197
  7. noun_index = ((hash[offset+2] & 0x7F) << 8 | hash[offset+3]) % 300
  8. number     = ((hash[offset+4] & 0x7F) << 8 | hash[offset+5]) % 100

Output:
  phrase = "${adjectives[adj_index]} ${nouns[noun_index]} ${number}"
  // e.g., "breezy rocket 75" (display as "Breezy Rocket 75")
```

## Word Lists (Frozen v1)

Extracted from safewords.io and frozen in `shared/wordlists/`:
- **197 adjectives** — `shared/wordlists/adjectives.json`
- **300 nouns** — `shared/wordlists/nouns.json`
- Source: `/data/code/safewords-io/repos/safewords-web/src/lib/data/en/wordlists/`

**CRITICAL**: The word lists are part of the algorithm. If lists change, the same seed produces different words. Lists are versioned and frozen — both platforms embed identical copies.

## Properties

- **Deterministic**: Same seed + same time window → same phrase on every device
- **Time-zone independent**: Uses UTC unix epoch
- **No network**: Pure local computation
- **Unguessable**: Without the seed, an attacker cannot predict the next word
- **Combination space**: 197 × 300 × 100 = **5,910,000 combinations** (~22.5 bits of entropy)

## Platform Implementations

| Platform | HMAC Library | Counter Encoding | Byte Handling |
|----------|-------------|-----------------|---------------|
| iOS (Swift) | `CryptoKit.HMAC<SHA256>` | `Int64.bigEndian` → `Data` | UInt8 (unsigned, no masking needed) |
| Android (Kotlin) | `javax.crypto.Mac("HmacSHA256")` | `ByteBuffer.putLong()` (big-endian default) | Signed bytes — must use `toInt() and 0xFF` |

## Test Vectors

Frozen in `shared/test-vectors.json`. All platforms must produce identical output:

| Seed | Interval | Timestamp | Counter | Expected Phrase |
|------|----------|-----------|---------|----------------|
| `0102...1f20` | daily (86400) | 1741651200 | 20158 | breezy rocket 75 |
| `0102...1f20` | daily (86400) | 1741737600 | 20159 | proud lantern 98 |
| `0102...1f20` | hourly (3600) | 1741651200 | 483792 | misty tambourine 40 |
| `0102...1f20` | hourly (3600) | 1741654800 | 483793 | distant volcano 60 |
| `0102...1f20` | weekly (604800) | 1741651200 | 2879 | chalky ribbon 4 |
| `0102...1f20` | monthly (2592000) | 1741651200 | 671 | salty bunker 34 |
| `0102...1f20` | daily (86400) | 0 | 0 | merry pulsar 18 |
| `ffff...ffff` | daily (86400) | 1741651200 | 20158 | toasty coyote 49 |

Mid-interval verification: timestamp 1741694400 (2025-03-11 12:00 UTC) produces the same daily phrase as 1741651200 (midnight) → "breezy rocket 75".

## Time Window Helpers

```swift
// Seconds remaining until next rotation
func getTimeRemaining(interval: Int, timestamp: TimeInterval) -> TimeInterval {
    let nextRotation = (floor(timestamp / Double(interval)) + 1) * Double(interval)
    return nextRotation - timestamp
}

// Current counter value
func getCurrentCounter(interval: Int, timestamp: TimeInterval) -> Int64 {
    return Int64(floor(timestamp / Double(interval)))
}
```

## Security Considerations

- Seed must be generated with `SecRandomCopyBytes` (iOS) or `SecureRandom` (Android)
- Seed stored in Keychain (iOS) or EncryptedSharedPreferences (Android), never in plain storage
- Seed transmitted only via QR code (in-person, on-screen)
- If a device is compromised, the group creator can regenerate the seed (rotates for everyone who re-scans)
