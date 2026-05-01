# Safewords TOTP Algorithm — Technical Reference

A comprehensive technical reference for the Safewords rotating-phrase algorithm and the cross-platform contract that both the iOS (Swift/SwiftUI) and Android (Kotlin/Compose) apps must honor.

This document is the authoritative reference for the v1.0 rotating-word cryptographic core. v1.3 adds three additional primitives (numeric, static override, challenge/answer) that share the same HMAC + word-derivation foundation; see [`v1.3-architecture.md`](./v1.3-architecture.md) for byte-level specs of those primitives. v1.2 added BIP39 24-word recovery phrases; see [`../shared/recovery-schema.md`](../shared/recovery-schema.md).

For the narrative spec of the original v1.0 algorithm see [`totp-word-algorithm.md`](./totp-word-algorithm.md); for product-level requirements see [`feature-spec.md`](./feature-spec.md).

---

## 1. Core Algorithm

Safewords is a word-based variant of RFC 6238 TOTP. Instead of producing a 6-digit numeric code, it maps the HMAC output to an `adjective noun NN` phrase drawn from two frozen word lists.

```
Input:
  seed      : 32 bytes (256 bits)
  interval  : seconds (3600, 86400, 604800, or 2592000)
  timestamp : unix epoch seconds

Procedure:
  counter        = floor(timestamp / interval)
  counter_bytes  = big_endian_int64(counter)          // exactly 8 bytes
  hash           = HMAC_SHA256(key=seed, msg=counter_bytes)  // 32 bytes

  // Dynamic truncation (RFC 4226 §5.3 style)
  offset    = hash[31] & 0x0F                                      // 0..15
  adj_idx   = ((hash[offset]   & 0x7F) << 8 | hash[offset+1]) % 197
  noun_idx  = ((hash[offset+2] & 0x7F) << 8 | hash[offset+3]) % 300
  number    = ((hash[offset+4] & 0x7F) << 8 | hash[offset+5]) % 100

Output:
  phrase = "${adjectives[adj_idx]} ${nouns[noun_idx]} ${number}"
  // e.g., "breezy rocket 75"  (display as "Breezy Rocket 75")
```

The dynamic-truncation trick lets a single 32-byte HMAC output produce three independent-looking indices without bias toward any fixed byte range: the last nibble selects where the 6-byte extraction window begins. The `& 0x7F` mask on each high byte removes the sign bit so the reconstructed 15-bit integer is identical on signed-byte platforms (Kotlin) and unsigned-byte platforms (Swift).

Combination space: `197 × 300 × 100 = 5,910,000` phrases (~22.5 bits). This is sufficient for human-readable group coordination, not for general cryptographic authentication.

---

## 2. Formula Walkthrough

Using test vector #1 as a concrete trace:

- `seed  = 0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20`
- `interval = 86400` (daily)
- `timestamp = 1741651200` (2025-03-11 00:00:00 UTC)

1. `counter = 1741651200 / 86400 = 20158`
2. `counter_bytes = 00 00 00 00 00 00 4E BE` (big-endian int64)
3. `hash = HMAC_SHA256(seed, counter_bytes)`
   = `ebfce99c78967ba3e54992356b5444ea5730cf6cf53fb8b5e085a83a13e79657`
4. `offset = hash[31] & 0x0F = 0x57 & 0x0F = 7`
5. `adj_idx = ((0xa3 & 0x7F) << 8 | 0xe5) % 197 = (0x23e5) % 197 = 9189 % 197 = 127` → `adjectives[127] = "breezy"`
6. `noun_idx = ((0x49 & 0x7F) << 8 | 0x92) % 300 = 0x4992 % 300 = 18834 % 300 = 234` → `nouns[234] = "rocket"`
7. `number = ((0x35 & 0x7F) << 8 | 0x6b) % 100 = 0x356b % 100 = 13675 % 100 = 75`
8. Phrase: `"breezy rocket 75"`

The intermediate values (`counter`, `hash`, `offset`, `adjIndex`, `nounIndex`, `number`) are all frozen in `shared/test-vectors.json` so implementations can assert each step, not just the final phrase.

---

## 3. Deterministic Cross-Platform Contract

The algorithm is pure and deterministic:

> **Same seed + same timestamp + same interval ⇒ byte-identical HMAC ⇒ identical phrase, on every device and platform, forever.**

For this to hold, every implementation must agree on:

| Point of ambiguity | The contract |
|---|---|
| HMAC key material | Raw 32 seed bytes (no KDF, no padding, no hashing) |
| HMAC message | Exactly 8 bytes, big-endian signed int64 of `counter` |
| Counter type | Signed 64-bit integer (`Int64` / `Long`) |
| Counter for `timestamp < 0` | Floor division (not truncation toward zero); mobile apps do not generate negative timestamps but division direction still matters if one is injected |
| Byte ordering in truncation | Network byte order (big-endian), high byte first |
| Sign handling | Mask every "high" byte with `0x7F`; mask every "low" byte with `0xFF` when converting signed bytes to int |
| Word list | Frozen v1 (see §4) — ordinal position is the contract |
| Output separator | Single ASCII space between each of the three tokens |
| Case | Stored lowercase in word lists; UI may title-case for display, but the canonical phrase is lowercase |

Anything that violates these rules breaks cross-device sync silently (phrases still "look valid" but don't match). All changes must be validated against `shared/test-vectors.json` before release.

---

## 4. Word Lists (Frozen v1)

- 197 adjectives — `shared/wordlists/adjectives.json`
- 300 nouns — `shared/wordlists/nouns.json`
- Source of truth (imported once, then frozen): `/data/code/safewords-io/repos/safewords-web/src/lib/data/en/wordlists/`

**The modulos in the algorithm (`% 197`, `% 300`, `% 100`) must match the list sizes exactly.** If you append a word, `% 197` becomes `% 198` and every pre-existing phrase shifts to a new index. Every group's QR-shared seed would silently stop matching. To prevent this:

- Both apps `assert` list sizes at load time (see `TOTPDerivation.swift:16,26` and `TOTPDerivationTest.kt:129-130`).
- iOS bundles the JSON files directly; the list is loaded and count-asserted as a lazy static on first access:
  ```swift
  assert(words.count == 197, "Adjective list must contain exactly 197 words")
  ```
- Android loads identical JSON via `WordLists`, also length-checked.
- If the product ever needs a larger space (e.g. v2 with more words), it must ship as a new, additive algorithm (`v: 2` in the QR payload), not as a silent list swap.

---

## 5. Seed

A seed is 32 cryptographically random bytes (256 bits).

| | iOS | Android |
|---|---|---|
| RNG | `SecRandomCopyBytes(kSecRandomDefault, 32, &bytes)` | `java.security.SecureRandom().nextBytes(...)` |
| Storage | Keychain (`kSecClassGenericPassword`) + App Group `group.com.thc.safewords` for widget access | `EncryptedSharedPreferences` (AndroidX Security, AES-256-GCM master key in Keystore) |
| Plaintext persistence | **Never** — no `UserDefaults`, no `SharedPreferences`, no file | **Never** — no plain `SharedPreferences`, no file |
| In-memory lifetime | Minimal — loaded only during derivation or QR export | Same |

Seed generation in the iOS implementation (`TOTPDerivation.swift:119-126`):

```swift
static func generateSeed() -> Data {
    var bytes = [UInt8](repeating: 0, count: 32)
    let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
    guard status == errSecSuccess else {
        fatalError("Failed to generate random seed: \(status)")
    }
    return Data(bytes)
}
```

On the Android side, seed generation lives alongside group creation code (not in `TOTPDerivation.kt`, which deals only with derivation). The contract is: `ByteArray` of length 32 from `SecureRandom`, passed into `TOTPDerivation.deriveSafeword`. The derivation function guards the size:

```kotlin
require(seed.size == 32) { "Seed must be 32 bytes" }
```

---

## 6. Rotation Intervals

Four supported intervals, encoded as seconds:

| Label | Seconds | Use case |
|---|---|---|
| `hourly` | `3600` | High-trust channel, fast refresh (e.g. active ops) |
| `daily` | `86400` | Default — family/household safeword |
| `weekly` | `604800` | Low-friction, lower-sensitivity groups |
| `monthly` | `2592000` (= 30 × 86400) | Archival/long-lived codes |

Note: `monthly` uses a fixed 30-day window, not calendar months. This keeps the algorithm timezone-agnostic (pure unix epoch arithmetic). All phrases change at the boundary of whole `interval` windows relative to epoch, not relative to local midnight.

Time helpers (iOS, `TOTPDerivation.swift:84-94`):

```swift
static func getTimeRemaining(interval: Int, timestamp: TimeInterval? = nil) -> TimeInterval {
    let now = timestamp ?? Date().timeIntervalSince1970
    let nextRotation = (floor(now / Double(interval)) + 1) * Double(interval)
    return nextRotation - now
}

static func getCurrentCounter(interval: Int, timestamp: TimeInterval? = nil) -> Int64 {
    let now = timestamp ?? Date().timeIntervalSince1970
    return Int64(floor(now / Double(interval)))
}
```

Android equivalents (`TOTPDerivation.kt:65-77`):

```kotlin
fun getCurrentCounter(interval: Int): Long {
    val now = System.currentTimeMillis() / 1000
    return now / interval
}

fun getTimeRemaining(interval: Int): Long {
    val now = System.currentTimeMillis() / 1000
    val nextRotation = (now / interval + 1) * interval
    return nextRotation - now
}
```

---

## 7. Test Vectors

Frozen in `shared/test-vectors.json`. Both platforms must pass every one of these before any release. The intermediate fields (`counter`, `hash`, `offset`, `adjIndex`, `nounIndex`, `number`) exist so that implementations can unit-test each stage, not only the final phrase.

| # | Seed | Interval | Timestamp | Counter | Hash (first 8 bytes) | Offset | adj/noun/num | Expected |
|---|---|---|---|---|---|---|---|---|
| 1 | `0102…1f20` | daily 86400 | 1741651200 | 20158 | `ebfce99c78967ba3` | 7 | 127 / 234 / 75 | `breezy rocket 75` |
| 2 | `0102…1f20` | daily 86400 | 1741737600 | 20159 | `4441c1f0b86a1007` | 3 | 94 / 152 / 98 | `proud lantern 98` |
| 3 | `0102…1f20` | hourly 3600 | 1741651200 | 483792 | `b3b41d0ca30d0c74` | 11 | 123 / 216 / 40 | `misty tambourine 40` |
| 4 | `0102…1f20` | hourly 3600 | 1741654800 | 483793 | `e8ffa10773db4024` | 1 | 168 / 107 / 60 | `distant volcano 60` |
| 5 | `0102…1f20` | weekly 604800 | 1741651200 | 2879 | `b95c2b5082cdd006` | 9 | 67 / 185 / 4 | `chalky ribbon 4` |
| 6 | `0102…1f20` | monthly 2592000 | 1741651200 | 671 | `16fa91e8cd995efc` | 11 | 181 / 260 / 34 | `salty bunker 34` |
| 7 | `0102…1f20` | daily 86400 | 0 | 0 | `c2cce957d808d548` | 4 | 78 / 232 / 18 | `merry pulsar 18` |
| 8 | `ffff…ffff` | daily 86400 | 1741651200 | 20158 | `5f8e3833dc998b73` | 12 | 56 / 10 / 49 | `toasty coyote 49` |
| 9 | `0102…1f20` | daily 86400 | 1741694400 | 20158 | `ebfce99c78967ba3` | 7 | 127 / 234 / 75 | `breezy rocket 75` |

Commentary:

- **Vectors 1–2** exercise consecutive daily windows against the canonical seed and should produce distinct phrases.
- **Vectors 3–4** do the same for hourly; counters are much larger (`~483k`) and validate int64 handling.
- **Vectors 5–6** cover weekly and monthly intervals — they confirm the interval is just a divisor and nothing about the algorithm is window-length-aware.
- **Vector 7** pins the epoch-zero edge case; this catches off-by-one or signed/unsigned errors in the counter.
- **Vector 8** uses an all-`0xFF` seed; this catches sign-extension bugs in languages with signed bytes (Kotlin, Java).
- **Vector 9** is timestamp `1741694400` (midday on the same UTC day as vector 1). The counter is identical to vector 1, so the phrase must match — this is the "mid-interval" determinism test.

---

## 8. Platform Implementations

| Platform | File | Library | Entry point |
|---|---|---|---|
| iOS | [`repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift`](../repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift) | `CryptoKit.HMAC<SHA256>` | `TOTPDerivation.deriveSafeword(seed:interval:timestamp:)` |
| Android | [`repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/TOTPDerivation.kt`](../repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/TOTPDerivation.kt) | `javax.crypto.Mac("HmacSHA256")` | `TOTPDerivation.deriveSafeword(seed, interval, timestamp)` |

### 8.1 The contract each must satisfy

Given identical `(seed, interval, timestamp)` inputs:

1. Compute `counter = floor(timestamp / interval)` as a signed 64-bit integer.
2. Encode `counter` as 8 big-endian bytes.
3. Compute `HMAC-SHA256(key=seed, message=counter_bytes)`.
4. Extract `offset`, then the three masked 15-bit indices, then apply the frozen modulos (`197`, `300`, `100`).
5. Concatenate `"${adjectives[adj_idx]} ${nouns[noun_idx]} ${number}"` with single-space separators.

### 8.2 Side-by-side: core derivation

**iOS** (`TOTPDerivation.swift:66-79`):

```swift
static func deriveIndices(seed: Data, interval: Int, timestamp: TimeInterval)
    -> (adjIndex: Int, nounIndex: Int, number: Int) {
    let counter = Int64(floor(timestamp / Double(interval)))
    let counterBytes = counterToBytes(counter)
    let key = SymmetricKey(data: seed)
    let hmac = HMAC<SHA256>.authenticationCode(for: counterBytes, using: key)
    let hash = Array(Data(hmac))

    let offset = Int(hash[31] & 0x0F)
    let adjIndex  = (Int(hash[offset]     & 0x7F) << 8 | Int(hash[offset + 1])) % adjectiveCount
    let nounIndex = (Int(hash[offset + 2] & 0x7F) << 8 | Int(hash[offset + 3])) % nounCount
    let number    = (Int(hash[offset + 4] & 0x7F) << 8 | Int(hash[offset + 5])) % numberMod

    return (adjIndex, nounIndex, number)
}
```

**Android** (`TOTPDerivation.kt:34-60`):

```kotlin
fun deriveSafeword(seed: ByteArray, interval: Int, timestamp: Long): String {
    require(seed.size == 32) { "Seed must be 32 bytes" }
    require(interval > 0)    { "Interval must be positive" }

    val counter = timestamp / interval
    val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(seed, "HmacSHA256"))
    val hash = mac.doFinal(counterBytes)

    val offset = hash[31].toInt() and 0x0F

    val adjIdx  = ((hash[offset].toInt()     and 0x7F) shl 8 or
                   (hash[offset + 1].toInt() and 0xFF)) % ADJECTIVE_COUNT
    val nounIdx = ((hash[offset + 2].toInt() and 0x7F) shl 8 or
                   (hash[offset + 3].toInt() and 0xFF)) % NOUN_COUNT
    val number  = ((hash[offset + 4].toInt() and 0x7F) shl 8 or
                   (hash[offset + 5].toInt() and 0xFF)) % NUMBER_MODULUS

    val adjective = WordLists.adjectives[adjIdx]
    val noun      = WordLists.nouns[nounIdx]

    return "$adjective $noun $number"
}
```

Note the subtle Kotlin requirement: because `Byte` is signed (`-128..127`), every low byte must be `and 0xFF`-masked when converted to `Int`, otherwise `0x80..0xFF` bytes sign-extend to negative integers and corrupt the shift. Swift's `UInt8` does not have this problem. Vector #8 (all-`0xFF` seed) exists specifically to catch regressions here.

### 8.3 Counter encoding, side-by-side

**iOS** (`TOTPDerivation.swift:131-134`):

```swift
private static func counterToBytes(_ counter: Int64) -> Data {
    var bigEndian = counter.bigEndian
    return Data(bytes: &bigEndian, count: 8)
}
```

**Android** (`TOTPDerivation.kt:39`):

```kotlin
val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
```

`ByteBuffer.allocate` defaults to `BIG_ENDIAN`, matching Swift's `Int64.bigEndian`. Any change to either side (e.g. `ByteBuffer.order(LITTLE_ENDIAN)` on Android or `counter.littleEndian` on iOS) breaks the contract.

### 8.4 Unit tests that validate against the vectors

**iOS** ([`SafewordsTests/TOTPDerivationTests.swift:10-26`](../repos/safewords-ios/SafewordsTests/TOTPDerivationTests.swift)):

```swift
func testAllTestVectors() {
    for (i, vector) in testVectors.enumerated() {
        let seed = vector.seedData
        XCTAssertEqual(seed.count, 32, "Vector \(i): seed should be 32 bytes")

        let phrase = TOTPDerivation.deriveSafeword(
            seed: seed,
            interval: vector.interval,
            timestamp: TimeInterval(vector.timestamp)
        )

        XCTAssertEqual(
            phrase, vector.expectedPhrase,
            "Vector \(i) (\(vector.note)): expected \"\(vector.expectedPhrase)\" but got \"\(phrase)\""
        )
    }
}
```

The iOS suite also tests each intermediate stage independently: `testCounterValues`, `testHMACHashes`, `testOffsetExtraction`, `testIndexExtraction`. This means a regression can be localized to the exact step where it diverged from the frozen vectors.

**Android** ([`app/src/test/kotlin/.../TOTPDerivationTest.kt:165-243`](../repos/safewords-android/app/src/test/kotlin/com/thc/safewords/TOTPDerivationTest.kt)):

```kotlin
@Test
fun testVector1_daily_1741651200() {
    val result = deriveSafewordForTest(
        seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
        interval = 86400,
        timestamp = 1741651200
    )
    assertEquals("breezy rocket 75", result)
}

// ...one @Test per vector, covering all 8 primary vectors plus mid-window and
//    different-seed determinism checks.
```

The Android test file inlines the word lists so the derivation logic can be unit-tested without an Android `Context` (JVM-only runner). The production code path (`TOTPDerivation.deriveSafeword` + `WordLists` loaded from assets) is exercised by instrumentation tests and the app itself.

---

## 9. QR Schema (v1)

Defined in [`shared/qr-schema.json`](../shared/qr-schema.json). A QR code encodes a JSON object, which is serialized as compact JSON and then base64url-encoded for QR-safety.

```json
{
  "v": 1,
  "name": "Smith family",
  "seed": "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA",
  "interval": "daily"
}
```

Fields:

| Field | Type | Constraints | Purpose |
|---|---|---|---|
| `v` | integer | MUST be `1` | Schema version — consumers must refuse unknown versions |
| `name` | string | 1–100 chars | Human-readable group label (displayed on receive) |
| `seed` | string | 43 chars, pattern `^[A-Za-z0-9_-]{43}$` | 256-bit seed, base64url (no padding) |
| `interval` | string | one of `hourly`, `daily`, `weekly`, `monthly` | Rotation period |

`additionalProperties: false` — any extra field invalidates the payload, so v2 additions must bump `v`.

**Size budget.** A typical payload is:

```
{"v":1,"name":"Smith family","seed":"<43>","interval":"daily"}
```

≈ 85–100 bytes of JSON. QR Version 5 (37×37) at error-correction level M handles up to ~154 alphanumeric chars; base64url fits comfortably. Group names longer than ~60 chars push the QR to higher versions and may degrade scan reliability on low-light cameras. UI should warn past 50 chars even though the schema permits 100.

Versioning rules:

- Changing the algorithm (new word list, new truncation, new output format) requires `v: 2`.
- Adding an optional field compatibly (e.g. `createdAt`) also requires `v: 2` because `additionalProperties: false`.
- Consumers MUST refuse any `v` they don't recognize and surface an "update the app" message.

---

## 10. Security Properties

- **Offline-first.** Derivation requires only the seed and the system clock. Zero network traffic, zero backend. The app works on a device in airplane mode indefinitely.
- **No accounts, no server.** There is no user ID, no session, no analytics pipeline tied to cryptographic state. Groups are entirely local: a seed, a name, an interval, stored in platform secure storage.
- **Seed never leaves device except via in-person QR.** The only export path is a QR code rendered on-screen, which the receiving device scans through its camera. The seed is never transmitted over the network, never written to shared storage, and never exposed to widget containers in plaintext (widgets re-derive from the keychain-stored seed via the App Group, not from a cached phrase).
- **Deterministic group consensus.** Because the algorithm is pure, every member of a group — phones, tablets, widgets — computes the same phrase at the same wall-clock time without any coordination protocol. There is no master, no sync server, no "who has the latest?" race.
- **Forward unpredictability without the seed.** Given any number of past phrases, an adversary cannot predict the next phrase: HMAC-SHA256 is a PRF, so outputs for different counters are computationally independent. The ~22.5-bit phrase space is not a hardness parameter — the seed is.
- **Phrase revocation is coarse.** There is no per-phrase revocation. Compromise response is "rotate the seed" (the creator generates a new seed and re-shares QR); old phrases are invalid against the new seed as soon as members re-scan.

---

## 11. Potential Pitfalls

**Word-list mutation breaks everything, silently.**
Appending, removing, reordering, or editing a word changes the meaning of every index. Two devices with different list versions will produce different phrases from the same seed and appear "broken" with no error. Mitigations:
- Both apps assert list sizes at load (`== 197` / `== 300`).
- Lists live in `shared/wordlists/` and are embedded verbatim in each app bundle.
- A list change requires a new algorithm version (`v: 2` in the QR schema) and a deliberate migration — not a silent asset update.

**Clock skew between devices.**
Because the counter is `floor(timestamp / interval)`, two devices close to a window boundary can land in different windows. Concretely, for a 1-hour interval, a 30-second skew at 59:45 vs 00:15 across two devices puts them in different counters and produces different phrases. Mitigations:
- Rely on OS-level NTP; both iOS and Android phones are effectively synchronized by default.
- Consider showing the previous phrase for `min(30s, interval × 0.01)` after rotation to cushion skew (UI decision, not algorithm).
- Document the boundary issue in any user-facing "codes don't match" troubleshooting flow.

**Seed leakage via screenshot or screen recording.**
The QR payload is the seed. A screenshot of the QR share screen exposes the seed to the device's photo library, iCloud/Google Photos backup, messaging apps, and anyone with device access. Mitigations:
- Use platform screen-capture suppression where possible (iOS: detect `UIScreen.captured`; Android: `WindowManager.LayoutParams.FLAG_SECURE` on the QR share activity).
- Warn the user in-UI ("don't screenshot or share this image").
- Keep the QR on screen only while actively sharing; auto-dismiss after scan completion or a short timeout.

**Signed-byte traps in Kotlin/Java.**
On the JVM, `ByteArray` elements are signed (`-128..127`). A forgotten `and 0xFF` during shifting turns any byte ≥ `0x80` into a large negative int, which then shifts incorrectly. Vector #8 (all-`0xFF` seed) exists to catch this; any PR touching the derivation on Android should leave vector #8 green.

**`Int` overflow in the 15-bit extraction.**
The masked high byte (`& 0x7F`) is at most `0x7F = 127`, shifted left 8 places → `0x7F00 = 32512`, OR'd with a byte (0..255) → max `0x7FFF = 32767`. Fits in `Int16` / `Int32` comfortably. Do not "optimize" by removing the `& 0x7F`; it exists to make the two-byte reconstruction identical on signed and unsigned platforms.

**Timestamp units.**
Seconds, not milliseconds. On Android, `System.currentTimeMillis() / 1000` is the canonical source. Passing milliseconds into `deriveSafeword` produces a counter 1000× too large and phrases that don't match anything.

**Monthly = 30 days, not calendar months.**
Intentional: keeps the algorithm timezone-independent and trivially computable. A "monthly" phrase does not rotate at the start of April; it rotates every 2,592,000 seconds. Surface this in the UI if users expect calendar semantics.

**Negative timestamps.**
Not a realistic concern on a mobile device (the system clock would have to be set to before 1970), but `floor(negative / positive)` differs from `negative / positive` in most languages for negative operands. Kotlin's `Long` division truncates toward zero; Swift's `floor` rounds toward negative infinity. Since no real device produces this, both apps implicitly assume `timestamp >= 0`. If this ever becomes relevant, the two platforms must be aligned on one rounding rule.

---

## Quick Reference

- Algorithm spec (narrative): [`docs/totp-word-algorithm.md`](./totp-word-algorithm.md)
- This reference: [`docs/totp-algorithm-reference.md`](./totp-algorithm-reference.md)
- Test vectors: [`shared/test-vectors.json`](../shared/test-vectors.json)
- Word lists: [`shared/wordlists/adjectives.json`](../shared/wordlists/adjectives.json), [`shared/wordlists/nouns.json`](../shared/wordlists/nouns.json)
- QR schema: [`shared/qr-schema.json`](../shared/qr-schema.json)
- iOS implementation: [`repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift`](../repos/safewords-ios/Safewords/Crypto/TOTPDerivation.swift)
- iOS tests: [`repos/safewords-ios/SafewordsTests/TOTPDerivationTests.swift`](../repos/safewords-ios/SafewordsTests/TOTPDerivationTests.swift)
- Android implementation: [`repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/TOTPDerivation.kt`](../repos/safewords-android/app/src/main/kotlin/com/thc/safewords/crypto/TOTPDerivation.kt)
- Android tests: [`repos/safewords-android/app/src/test/kotlin/com/thc/safewords/TOTPDerivationTest.kt`](../repos/safewords-android/app/src/test/kotlin/com/thc/safewords/TOTPDerivationTest.kt)
