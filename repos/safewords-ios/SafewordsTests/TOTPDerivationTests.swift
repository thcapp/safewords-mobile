import XCTest
import CryptoKit
@testable import Safewords

final class TOTPDerivationTests: XCTestCase {

    // MARK: - Test Vector Validation

    /// Verify all test vectors produce the expected phrases.
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

    /// Verify each test vector's counter value.
    func testCounterValues() {
        for (i, vector) in testVectors.enumerated() {
            let counter = TOTPDerivation.getCurrentCounter(
                interval: vector.interval,
                timestamp: TimeInterval(vector.timestamp)
            )

            XCTAssertEqual(
                counter, vector.counter,
                "Vector \(i) (\(vector.note)): expected counter \(vector.counter) but got \(counter)"
            )
        }
    }

    /// Verify each test vector's HMAC hash.
    func testHMACHashes() {
        for (i, vector) in testVectors.enumerated() {
            let seed = vector.seedData
            let counter = Int64(floor(Double(vector.timestamp) / Double(vector.interval)))
            var bigEndian = counter.bigEndian
            let counterData = Data(bytes: &bigEndian, count: 8)
            let key = SymmetricKey(data: seed)
            let hmac = HMAC<SHA256>.authenticationCode(for: counterData, using: key)
            let hashHex = Data(hmac).map { String(format: "%02x", $0) }.joined()

            XCTAssertEqual(
                hashHex, vector.hash,
                "Vector \(i) (\(vector.note)): HMAC hash mismatch"
            )
        }
    }

    /// Verify offset extraction from each test vector.
    func testOffsetExtraction() {
        for (i, vector) in testVectors.enumerated() {
            let hashBytes = TOTPTestHelper.dataFromHex(vector.hash)
            let offset = Int(hashBytes[31] & 0x0F)

            XCTAssertEqual(
                offset, vector.offset,
                "Vector \(i) (\(vector.note)): expected offset \(vector.offset) but got \(offset)"
            )
        }
    }

    /// Verify index extraction from each test vector.
    func testIndexExtraction() {
        for (i, vector) in testVectors.enumerated() {
            let seed = vector.seedData
            let indices = TOTPDerivation.deriveIndices(
                seed: seed,
                interval: vector.interval,
                timestamp: TimeInterval(vector.timestamp)
            )

            XCTAssertEqual(
                indices.adjIndex, vector.adjIndex,
                "Vector \(i) (\(vector.note)): adjective index mismatch"
            )
            XCTAssertEqual(
                indices.nounIndex, vector.nounIndex,
                "Vector \(i) (\(vector.note)): noun index mismatch"
            )
            XCTAssertEqual(
                indices.number, vector.number,
                "Vector \(i) (\(vector.note)): number mismatch"
            )
        }
    }

    // MARK: - Word List Validation

    func testAdjectiveListSize() {
        XCTAssertEqual(TOTPDerivation.adjectives.count, 197, "Must have exactly 197 adjectives")
    }

    func testNounListSize() {
        XCTAssertEqual(TOTPDerivation.nouns.count, 300, "Must have exactly 300 nouns")
    }

    func testWordListsNotEmpty() {
        for adj in TOTPDerivation.adjectives {
            XCTAssertFalse(adj.isEmpty, "Adjective should not be empty")
        }
        for noun in TOTPDerivation.nouns {
            XCTAssertFalse(noun.isEmpty, "Noun should not be empty")
        }
    }

    // MARK: - Time Helpers

    func testGetTimeRemaining() {
        // At timestamp 0, hourly interval: remaining should be 3600
        let remaining = TOTPDerivation.getTimeRemaining(interval: 3600, timestamp: 0)
        XCTAssertEqual(remaining, 3600, accuracy: 0.001)

        // At timestamp 1800 (halfway through an hour): remaining should be 1800
        let halfRemaining = TOTPDerivation.getTimeRemaining(interval: 3600, timestamp: 1800)
        XCTAssertEqual(halfRemaining, 1800, accuracy: 0.001)

        // At timestamp 3599 (1 second before rotation): remaining should be 1
        let almostDone = TOTPDerivation.getTimeRemaining(interval: 3600, timestamp: 3599)
        XCTAssertEqual(almostDone, 1, accuracy: 0.001)
    }

    func testGetCurrentCounter() {
        XCTAssertEqual(TOTPDerivation.getCurrentCounter(interval: 86400, timestamp: 0), 0)
        XCTAssertEqual(TOTPDerivation.getCurrentCounter(interval: 86400, timestamp: 86399), 0)
        XCTAssertEqual(TOTPDerivation.getCurrentCounter(interval: 86400, timestamp: 86400), 1)
        XCTAssertEqual(TOTPDerivation.getCurrentCounter(interval: 3600, timestamp: 7200), 2)
    }

    // MARK: - Determinism

    /// Same inputs must always produce the same output.
    func testDeterminism() {
        let seed = TOTPTestHelper.dataFromHex(
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"
        )
        let timestamp: TimeInterval = 1741651200
        let interval = 86400

        let phrase1 = TOTPDerivation.deriveSafeword(seed: seed, interval: interval, timestamp: timestamp)
        let phrase2 = TOTPDerivation.deriveSafeword(seed: seed, interval: interval, timestamp: timestamp)
        let phrase3 = TOTPDerivation.deriveSafeword(seed: seed, interval: interval, timestamp: timestamp)

        XCTAssertEqual(phrase1, phrase2)
        XCTAssertEqual(phrase2, phrase3)
    }

    /// Mid-period timestamps produce the same word as period-start timestamps.
    func testMidPeriodSameAsStart() {
        let seed = TOTPTestHelper.dataFromHex(
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"
        )

        // 2025-03-11 00:00:00 UTC and 2025-03-11 12:00:00 UTC should produce the same daily phrase
        let phraseStart = TOTPDerivation.deriveSafeword(seed: seed, interval: 86400, timestamp: 1741651200)
        let phraseMid = TOTPDerivation.deriveSafeword(seed: seed, interval: 86400, timestamp: 1741694400)

        XCTAssertEqual(phraseStart, phraseMid)
    }

    /// Different timestamps in different periods produce different words (with high probability).
    func testDifferentPeriodsProduceDifferentWords() {
        let seed = TOTPTestHelper.dataFromHex(
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"
        )

        let phrase1 = TOTPDerivation.deriveSafeword(seed: seed, interval: 86400, timestamp: 1741651200)
        let phrase2 = TOTPDerivation.deriveSafeword(seed: seed, interval: 86400, timestamp: 1741737600)

        XCTAssertNotEqual(phrase1, phrase2)
    }

    // MARK: - Seed Generation

    func testGenerateSeedLength() {
        let seed = TOTPDerivation.generateSeed()
        XCTAssertEqual(seed.count, 32, "Seed must be 32 bytes")
    }

    func testGenerateSeedRandomness() {
        let seed1 = TOTPDerivation.generateSeed()
        let seed2 = TOTPDerivation.generateSeed()
        XCTAssertNotEqual(seed1, seed2, "Two generated seeds should be different")
    }

    // MARK: - Hex Parsing

    func testDataFromHex() {
        let data = TOTPDerivation.dataFromHex("0102030405")
        XCTAssertEqual(data, Data([0x01, 0x02, 0x03, 0x04, 0x05]))
    }

    func testDataFromHexAllFF() {
        let data = TOTPDerivation.dataFromHex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
        XCTAssertEqual(data.count, 32)
        XCTAssertTrue(data.allSatisfy { $0 == 0xFF })
    }

    // MARK: - Capitalization

    func testCapitalizedOutput() {
        let seed = TOTPTestHelper.dataFromHex(
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"
        )

        let phrase = TOTPDerivation.deriveSafewordCapitalized(
            seed: seed,
            interval: 86400,
            timestamp: 1741651200
        )

        // "breezy rocket 75" -> "Breezy Rocket 75"
        XCTAssertEqual(phrase, "Breezy Rocket 75")
    }
}
