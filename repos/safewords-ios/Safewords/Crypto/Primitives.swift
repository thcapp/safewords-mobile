import CryptoKit
import Foundation

struct PrimitivePhrase: Equatable {
    let offset: Int
    let adjIndex: Int
    let nounIndex: Int
    let number: Int
    let phrase: String
    let hashHex: String
}

struct NumericPrimitive: Equatable {
    let counter: Int64
    let offset: Int
    let code: String
    let hashHex: String
}

struct ChallengeAnswerRow: Equatable, Identifiable {
    let rowIndex: Int
    let ask: PrimitivePhrase
    let expect: PrimitivePhrase

    var id: Int { rowIndex }
}

enum Primitives {
    static let staticOverrideLabel = "safewords/static-override/v1"

    static func staticOverride(seed: Data) -> PrimitivePhrase {
        standardWordDerivation(hash: hmac(seed: seed, message: Data(staticOverrideLabel.utf8)))
    }

    static func numeric(seed: Data, intervalSeconds: Int, timestamp: TimeInterval) -> NumericPrimitive {
        let counter = Int64(floor(timestamp / Double(intervalSeconds)))
        var bigEndian = counter.bigEndian
        let message = Data(bytes: &bigEndian, count: 8)
        let hash = hmac(seed: seed, message: message)
        let offset = Int(hash[31] & 0x0f)
        let codeInt = (
            (Int(hash[offset] & 0x7f) << 24)
                | (Int(hash[offset + 1] & 0xff) << 16)
                | (Int(hash[offset + 2] & 0xff) << 8)
                | Int(hash[offset + 3] & 0xff)
        ) % 1_000_000

        return NumericPrimitive(
            counter: counter,
            offset: offset,
            code: String(format: "%06d", codeInt),
            hashHex: hash.hexString
        )
    }

    static func challengeAnswerRow(seed: Data, tableVersion: Int = 1, rowIndex: Int) -> ChallengeAnswerRow {
        let askLabel = "safewords/challenge-answer/v\(tableVersion)/ask/\(rowIndex)"
        let expectLabel = "safewords/challenge-answer/v\(tableVersion)/expect/\(rowIndex)"
        return ChallengeAnswerRow(
            rowIndex: rowIndex,
            ask: standardWordDerivation(hash: hmac(seed: seed, message: Data(askLabel.utf8))),
            expect: standardWordDerivation(hash: hmac(seed: seed, message: Data(expectLabel.utf8)))
        )
    }

    static func challengeAnswerRows(seed: Data, tableVersion: Int = 1, count: Int) -> [ChallengeAnswerRow] {
        (0..<count).map { challengeAnswerRow(seed: seed, tableVersion: tableVersion, rowIndex: $0) }
    }

    static func standardWordDerivation(hash: [UInt8]) -> PrimitivePhrase {
        let offset = Int(hash[31] & 0x0f)
        let adjIndex = ((Int(hash[offset] & 0x7f) << 8) | Int(hash[offset + 1] & 0xff)) % TOTPDerivation.adjectiveCount
        let nounIndex = ((Int(hash[offset + 2] & 0x7f) << 8) | Int(hash[offset + 3] & 0xff)) % TOTPDerivation.nounCount
        let number = ((Int(hash[offset + 4] & 0x7f) << 8) | Int(hash[offset + 5] & 0xff)) % TOTPDerivation.numberMod
        let phrase = "\(TOTPDerivation.adjectives[adjIndex]) \(TOTPDerivation.nouns[nounIndex]) \(number)"
        return PrimitivePhrase(
            offset: offset,
            adjIndex: adjIndex,
            nounIndex: nounIndex,
            number: number,
            phrase: phrase,
            hashHex: hash.hexString
        )
    }

    private static func hmac(seed: Data, message: Data) -> [UInt8] {
        let key = SymmetricKey(data: seed)
        return Array(HMAC<SHA256>.authenticationCode(for: message, using: key))
    }
}

private extension Array where Element == UInt8 {
    var hexString: String {
        map { String(format: "%02x", $0) }.joined()
    }
}
