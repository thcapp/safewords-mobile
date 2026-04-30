import XCTest
@testable import Safewords

final class RecoveryPhraseTests: XCTestCase {
    private lazy var bip39: Bip39 = {
        let words = readTextResource(name: "bip39-english", ext: "txt", fallback: "shared/wordlists/bip39-english.txt")
            .split(separator: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return Bip39(wordlist: words)
    }()

    private lazy var vectors: RecoveryVectorFile = {
        let text = readTextResource(name: "recovery-vectors", ext: "json", fallback: "shared/recovery-vectors.json")
        let data = Data(text.utf8)
        do {
            return try JSONDecoder().decode(RecoveryVectorFile.self, from: data)
        } catch {
            XCTFail("Could not decode recovery-vectors.json: \(error)")
            return RecoveryVectorFile()
        }
    }()

    func testEncodeMatchesEveryVector() throws {
        for vector in vectors.valid {
            let seed = try dataFromHex(vector.seedHex)
            XCTAssertEqual(try bip39.encode(seed: seed), vector.mnemonic, "encode(\(vector.id))")
        }
    }

    func testDecodeMatchesEveryVector() throws {
        for vector in vectors.valid {
            let decoded = try bip39.decode(input: vector.mnemonic)
            XCTAssertEqual(decoded, try dataFromHex(vector.seedHex), "decode(\(vector.id))")
        }
    }

    func testDecodeNormalizesInputForVectorsWithNormalizedInput() throws {
        for vector in vectors.valid {
            guard let normalizedInput = vector.normalizedInput else { continue }
            let decoded = try bip39.decode(input: normalizedInput)
            XCTAssertEqual(decoded, try dataFromHex(vector.seedHex), "decode(normalizedInput of \(vector.id))")
        }
    }

    func testDecodeRejectsEveryInvalidVectorWithDocumentedError() throws {
        for vector in vectors.invalid {
            do {
                _ = try bip39.decode(input: vector.input)
                XCTFail("decode(\(vector.id)) should have thrown \(vector.expectedError)")
            } catch let error as RecoveryPhraseError {
                XCTAssertEqual(error.code, vector.expectedError, "error code for \(vector.id)")
                XCTAssertEqual(error.userMessage, vector.expectedMessage, "error message for \(vector.id)")

                if case let .unknownWord(index, word) = error {
                    if let expectedWordIndex = vector.expectedWordIndex {
                        XCTAssertEqual(index, expectedWordIndex, "word index for \(vector.id)")
                    }
                    if let expectedUnknownWord = vector.expectedUnknownWord {
                        XCTAssertEqual(word, expectedUnknownWord, "unknown word for \(vector.id)")
                    }
                }
            } catch {
                XCTFail("decode(\(vector.id)) threw unexpected error: \(error)")
            }
        }
    }

    func testEncodeThenDecodeRoundTripsForRandomSeeds() throws {
        var rng = SystemRandomNumberGenerator()
        for _ in 0..<10 {
            let seed = Data((0..<32).map { _ in UInt8.random(in: 0...255, using: &rng) })
            let phrase = try bip39.encode(seed: seed)
            XCTAssertEqual(try bip39.decode(input: phrase), seed)
        }
    }

    func testParseSeedAcceptsLegacySpacedHex() throws {
        let seedHex = vectors.valid[0].seedHex
        let spacedHex = stride(from: 0, to: seedHex.count, by: 8)
            .map { offset -> String in
                let start = seedHex.index(seedHex.startIndex, offsetBy: offset)
                let end = seedHex.index(start, offsetBy: 8, limitedBy: seedHex.endIndex) ?? seedHex.endIndex
                return String(seedHex[start..<end])
            }
            .joined(separator: " ")

        XCTAssertEqual(try RecoveryPhraseService.parseSeed(from: spacedHex), try dataFromHex(seedHex))
    }

    private func readTextResource(name: String, ext: String, fallback: String) -> String {
        let bundle = Bundle(for: RecoveryPhraseTests.self)
        if let url = bundle.url(forResource: name, withExtension: ext),
           let text = try? String(contentsOf: url, encoding: .utf8) {
            return text
        }

        let sourceFile = URL(fileURLWithPath: #filePath)
        let repoRoot = sourceFile
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let fallbackURL = repoRoot.appendingPathComponent(fallback)
        if let text = try? String(contentsOf: fallbackURL, encoding: .utf8) {
            return text
        }

        XCTFail("Missing test resource \(name).\(ext)")
        return ""
    }

    private func dataFromHex(_ hex: String) throws -> Data {
        guard hex.count.isMultiple(of: 2) else {
            throw TestError.invalidHex(hex)
        }

        var data = Data()
        var index = hex.startIndex
        while index < hex.endIndex {
            let next = hex.index(index, offsetBy: 2)
            let byteString = hex[index..<next]
            guard let byte = UInt8(byteString, radix: 16) else {
                throw TestError.invalidHex(hex)
            }
            data.append(byte)
            index = next
        }
        return data
    }
}

private struct RecoveryVectorFile: Decodable {
    let version: Int
    let scheme: String
    let wordCount: Int
    let seedBytes: Int
    let normalization: String
    let valid: [ValidRecoveryVector]
    let invalid: [InvalidRecoveryVector]

    init(
        version: Int = 0,
        scheme: String = "",
        wordCount: Int = 0,
        seedBytes: Int = 0,
        normalization: String = "",
        valid: [ValidRecoveryVector] = [],
        invalid: [InvalidRecoveryVector] = []
    ) {
        self.version = version
        self.scheme = scheme
        self.wordCount = wordCount
        self.seedBytes = seedBytes
        self.normalization = normalization
        self.valid = valid
        self.invalid = invalid
    }
}

private struct ValidRecoveryVector: Decodable {
    let id: String
    let seedHex: String
    let mnemonic: String
    let normalizedInput: String?
}

private struct InvalidRecoveryVector: Decodable {
    let id: String
    let input: String
    let expectedError: String
    let expectedMessage: String
    let expectedWordIndex: Int?
    let expectedUnknownWord: String?
}

private enum TestError: Error {
    case invalidHex(String)
}
