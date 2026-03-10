import Foundation

/// Test vector data matching /data/code/safewords-mobile/shared/test-vectors.json
struct TestVector: Decodable {
    let seed: String
    let interval: Int
    let timestamp: Int
    let note: String
    let counter: Int64
    let hash: String
    let offset: Int
    let adjIndex: Int
    let nounIndex: Int
    let number: Int
    let expectedPhrase: String

    var seedData: Data {
        TOTPTestHelper.dataFromHex(seed)
    }
}

struct TestVectorsFile: Decodable {
    let version: Int
    let vectors: [TestVector]
}

/// All test vectors, loaded from the embedded JSON.
let testVectors: [TestVector] = {
    let vectors: [TestVector] = [
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 86400,
            timestamp: 1741651200,
            note: "2025-03-11 00:00:00 UTC, daily",
            counter: 20158,
            hash: "ebfce99c78967ba3e54992356b5444ea5730cf6cf53fb8b5e085a83a13e79657",
            offset: 7,
            adjIndex: 127,
            nounIndex: 234,
            number: 75,
            expectedPhrase: "breezy rocket 75"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 86400,
            timestamp: 1741737600,
            note: "2025-03-12 00:00:00 UTC, daily",
            counter: 20159,
            hash: "4441c1f0b86a10076a333ffce0cd0d92b0118bfae5ccb19c835da59625e3ff03",
            offset: 3,
            adjIndex: 94,
            nounIndex: 152,
            number: 98,
            expectedPhrase: "proud lantern 98"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 3600,
            timestamp: 1741651200,
            note: "2025-03-11 00:00:00 UTC, hourly",
            counter: 483792,
            hash: "b3b41d0ca30d0c74f5b3520454f3b0648c1ae9208573b6f21f645f23fa0859cb",
            offset: 11,
            adjIndex: 123,
            nounIndex: 216,
            number: 40,
            expectedPhrase: "misty tambourine 40"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 3600,
            timestamp: 1741654800,
            note: "2025-03-11 01:00:00 UTC, hourly",
            counter: 483793,
            hash: "e8ffa10773db4024f99bb5b3830d8087ce24dbd3d8a0e581f066274ac194a811",
            offset: 1,
            adjIndex: 168,
            nounIndex: 107,
            number: 60,
            expectedPhrase: "distant volcano 60"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 604800,
            timestamp: 1741651200,
            note: "2025-03-11 00:00:00 UTC, weekly",
            counter: 2879,
            hash: "b95c2b5082cdd0062516949e05f214ebe0dda3ab44615b474b5e450431303f29",
            offset: 9,
            adjIndex: 67,
            nounIndex: 185,
            number: 4,
            expectedPhrase: "chalky ribbon 4"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 2592000,
            timestamp: 1741651200,
            note: "2025-03-11 00:00:00 UTC, monthly",
            counter: 671,
            hash: "16fa91e8cd995efc7379e864bfd30c7acac51d5e19a93bafbc0497ee0368d9ab",
            offset: 11,
            adjIndex: 181,
            nounIndex: 260,
            number: 34,
            expectedPhrase: "salty bunker 34"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 86400,
            timestamp: 0,
            note: "epoch zero, daily",
            counter: 0,
            hash: "c2cce957d808d548792a6ab7940ba65963c8be6fb6778fdb8e354c69cac6df74",
            offset: 4,
            adjIndex: 78,
            nounIndex: 232,
            number: 18,
            expectedPhrase: "merry pulsar 18"
        ),
        TestVector(
            seed: "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            interval: 86400,
            timestamp: 1741651200,
            note: "all-FF seed, daily",
            counter: 20158,
            hash: "5f8e3833dc998b73cba4479726b2f2e265c1bb8d5cda98e51bef564e8533282c",
            offset: 12,
            adjIndex: 56,
            nounIndex: 10,
            number: 49,
            expectedPhrase: "toasty coyote 49"
        ),
        TestVector(
            seed: "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval: 86400,
            timestamp: 1741694400,
            note: "2025-03-11 12:00:00 UTC (mid-day), daily - should match first vector",
            counter: 20158,
            hash: "ebfce99c78967ba3e54992356b5444ea5730cf6cf53fb8b5e085a83a13e79657",
            offset: 7,
            adjIndex: 127,
            nounIndex: 234,
            number: 75,
            expectedPhrase: "breezy rocket 75"
        )
    ]
    return vectors
}()

/// Helper for hex conversion in tests (avoids depending on main target for this).
enum TOTPTestHelper {
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
