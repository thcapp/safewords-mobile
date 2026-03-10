# TOTP Word Derivation Algorithm

## Overview

Transform a shared secret + current time into a deterministic human-readable phrase, using the same principle as RFC 6238 (TOTP) but mapping to words instead of digits.

## Algorithm

```
Input:
  seed:     Uint8Array (32 bytes / 256 bits)
  interval: number (seconds — 3600, 86400, 604800, or 2592000)
  timestamp: number (unix epoch seconds, default = now)

Steps:
  1. counter = floor(timestamp / interval)
  2. counter_bytes = int64_to_big_endian_bytes(counter)
  3. hash = HMAC-SHA256(key=seed, message=counter_bytes)
  4. // hash is 32 bytes

  // Extract indices (similar to HOTP dynamic truncation)
  5. offset = hash[31] & 0x0F
  6. adj_index  = ((hash[offset] & 0x7F) << 8 | hash[offset+1]) % adjective_list.length
  7. noun_index = ((hash[offset+2] & 0x7F) << 8 | hash[offset+3]) % noun_list.length
  8. number     = ((hash[offset+4] & 0x7F) << 8 | hash[offset+5]) % 100

Output:
  phrase = `${adjectives[adj_index]} ${nouns[noun_index]} ${number}`
  // e.g., "Crimson Eagle 47"
```

## Properties

- **Deterministic**: Same seed + same time window → same phrase on every device
- **Time-zone independent**: Uses UTC unix epoch
- **No network**: Pure local computation
- **Unguessable**: Without the seed, an attacker cannot predict the next word
- **Collision-resistant**: With ~500 adjectives × ~500 nouns × 100 numbers = 25M combinations

## Word Lists

Reuse the curated lists from safewords.io:
- Source: `/data/code/safewords-io/repos/safewords-web/src/lib/data/`
- Adjectives: ~500 words, filtered for phone-safety and memorability
- Nouns: ~500 words, same filtering
- Lists must be identical on all platforms (frozen, versioned)

**CRITICAL**: The word lists are part of the algorithm. If lists change, the same seed produces different words. Lists must be versioned and frozen per group.

## Time Window Calculation

```typescript
function getCurrentCounter(interval: number): number {
  return Math.floor(Date.now() / 1000 / interval);
}

function getTimeRemaining(interval: number): number {
  const now = Date.now() / 1000;
  const nextRotation = (Math.floor(now / interval) + 1) * interval;
  return nextRotation - now; // seconds until next word
}
```

## Test Vectors

These must be generated during implementation and frozen as regression tests:

```
Seed (hex): 0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20
Interval: 86400 (daily)
Timestamp: 1741651200 (2025-03-11 00:00:00 UTC)
Counter: 20157
Expected phrase: [TO BE COMPUTED]
```

## Security Considerations

- Seed must be generated with `crypto.getRandomValues()` (or platform equivalent)
- Seed stored in SecureStore (Keychain/Keystore), never in AsyncStorage
- Seed transmitted only via QR code (in-person, on-screen)
- If a device is compromised, the group creator can regenerate the seed (rotates for everyone who re-scans)
