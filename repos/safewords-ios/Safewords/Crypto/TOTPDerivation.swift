import Foundation
import CryptoKit

/// Core TOTP-based safeword derivation engine.
/// Deterministic: same seed + same time window = same phrase on every device.
enum TOTPDerivation {

    // MARK: - Word Lists (frozen v1 — changing these breaks cross-device sync)

    static let adjectives: [String] = {
        guard let url = Bundle.main.url(forResource: "adjectives", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let words = try? JSONDecoder().decode([String].self, from: data) else {
            fatalError("Failed to load adjectives.json from bundle")
        }
        assert(words.count == 197, "Adjective list must contain exactly 197 words")
        return words
    }()

    static let nouns: [String] = {
        guard let url = Bundle.main.url(forResource: "nouns", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let words = try? JSONDecoder().decode([String].self, from: data) else {
            fatalError("Failed to load nouns.json from bundle")
        }
        assert(words.count == 300, "Noun list must contain exactly 300 words")
        return words
    }()

    // MARK: - Constants

    static let adjectiveCount = 197
    static let nounCount = 300
    static let numberMod = 100

    // MARK: - Derivation

    /// Derive a safeword phrase from a seed, interval, and timestamp.
    ///
    /// Algorithm:
    /// 1. counter = floor(timestamp / interval)
    /// 2. counter_bytes = int64 big-endian
    /// 3. hash = HMAC-SHA256(key=seed, message=counter_bytes)
    /// 4. offset = hash[31] & 0x0F
    /// 5. adj_idx  = ((hash[offset] & 0x7F) << 8 | hash[offset+1]) % 197
    /// 6. noun_idx = ((hash[offset+2] & 0x7F) << 8 | hash[offset+3]) % 300
    /// 7. number   = ((hash[offset+4] & 0x7F) << 8 | hash[offset+5]) % 100
    /// 8. phrase   = "\(adjective) \(noun) \(number)"
    static func deriveSafeword(seed: Data, interval: Int, timestamp: TimeInterval) -> String {
        let indices = deriveIndices(seed: seed, interval: interval, timestamp: timestamp)
        let adjective = adjectives[indices.adjIndex]
        let noun = nouns[indices.nounIndex]
        return "\(adjective) \(noun) \(indices.number)"
    }

    /// Derive a safeword phrase with capitalized first letters for display.
    /// Example: "Breezy Rocket 75"
    static func deriveSafewordCapitalized(seed: Data, interval: Int, timestamp: TimeInterval) -> String {
        let indices = deriveIndices(seed: seed, interval: interval, timestamp: timestamp)
        let adjective = adjectives[indices.adjIndex].capitalized
        let noun = nouns[indices.nounIndex].capitalized
        return "\(adjective) \(noun) \(indices.number)"
    }

    /// Internal: derive the raw indices from the HMAC hash.
    static func deriveIndices(seed: Data, interval: Int, timestamp: TimeInterval) -> (adjIndex: Int, nounIndex: Int, number: Int) {
        let counter = Int64(floor(timestamp / Double(interval)))
        let counterBytes = counterToBytes(counter)
        let key = SymmetricKey(data: seed)
        let hmac = HMAC<SHA256>.authenticationCode(for: counterBytes, using: key)
        let hash = Array(Data(hmac))

        let offset = Int(hash[31] & 0x0F)
        let adjIndex = (Int(hash[offset] & 0x7F) << 8 | Int(hash[offset + 1])) % adjectiveCount
        let nounIndex = (Int(hash[offset + 2] & 0x7F) << 8 | Int(hash[offset + 3])) % nounCount
        let number = (Int(hash[offset + 4] & 0x7F) << 8 | Int(hash[offset + 5])) % numberMod

        return (adjIndex, nounIndex, number)
    }

    // MARK: - Time Helpers

    /// Seconds remaining until the next rotation.
    static func getTimeRemaining(interval: Int, timestamp: TimeInterval? = nil) -> TimeInterval {
        let now = timestamp ?? Date().timeIntervalSince1970
        let nextRotation = (floor(now / Double(interval)) + 1) * Double(interval)
        return nextRotation - now
    }

    /// Current counter value for the given interval.
    static func getCurrentCounter(interval: Int, timestamp: TimeInterval? = nil) -> Int64 {
        let now = timestamp ?? Date().timeIntervalSince1970
        return Int64(floor(now / Double(interval)))
    }

    /// The Date when the current period started.
    static func currentPeriodStart(interval: Int) -> Date {
        let now = Date().timeIntervalSince1970
        let start = floor(now / Double(interval)) * Double(interval)
        return Date(timeIntervalSince1970: start)
    }

    /// The Date when the next rotation will occur.
    static func nextRotationDate(interval: Int) -> Date {
        let now = Date().timeIntervalSince1970
        let next = (floor(now / Double(interval)) + 1) * Double(interval)
        return Date(timeIntervalSince1970: next)
    }

    /// Progress through the current period (0.0 = just started, 1.0 = about to rotate).
    static func progress(interval: Int) -> Double {
        let remaining = getTimeRemaining(interval: interval)
        return 1.0 - (remaining / Double(interval))
    }

    // MARK: - Seed Generation

    /// Generate a cryptographically secure random 32-byte seed.
    static func generateSeed() -> Data {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        guard status == errSecSuccess else {
            fatalError("Failed to generate random seed: \(status)")
        }
        return Data(bytes)
    }

    // MARK: - Helpers

    /// Convert an Int64 counter to 8-byte big-endian representation.
    private static func counterToBytes(_ counter: Int64) -> Data {
        var bigEndian = counter.bigEndian
        return Data(bytes: &bigEndian, count: 8)
    }

    /// Parse a hex string into Data.
    static func dataFromHex(_ hex: String) -> Data {
        var data = Data()
        var index = hex.startIndex
        while index < hex.endIndex {
            let nextIndex = hex.index(index, offsetBy: 2)
            let byteString = hex[index..<nextIndex]
            if let byte = UInt8(byteString, radix: 16) {
                data.append(byte)
            }
            index = nextIndex
        }
        return data
    }
}
