import XCTest
@testable import Safewords

final class PrimitiveVectorsTests: XCTestCase {
    private lazy var vectors: PrimitiveVectorFile = {
        let text = readTextResource(name: "primitive-vectors", ext: "json", fallback: "shared/primitive-vectors.json")
        do {
            return try JSONDecoder().decode(PrimitiveVectorFile.self, from: Data(text.utf8))
        } catch {
            XCTFail("Could not decode primitive-vectors.json: \(error)")
            return PrimitiveVectorFile.empty
        }
    }()

    func testStaticOverrideVectors() throws {
        for vector in vectors.staticOverrideVectors {
            let fixture = try XCTUnwrap(vector.seed)
            let result = Primitives.staticOverride(seed: try seed(named: fixture))
            assertPhrase(result, matches: vector, context: "static override \(fixture)")
        }
    }

    func testNumericVectors() throws {
        for vector in vectors.numericVectors {
            let result = Primitives.numeric(
                seed: try seed(named: vector.seed),
                intervalSeconds: vector.interval,
                timestamp: TimeInterval(vector.timestamp)
            )
            XCTAssertEqual(result.counter, vector.counter, "counter \(vector.note ?? vector.seed)")
            XCTAssertEqual(result.hashHex, vector.hashHex, "hash \(vector.note ?? vector.seed)")
            XCTAssertEqual(result.offset, vector.offset, "offset \(vector.note ?? vector.seed)")
            XCTAssertEqual(result.code, vector.code, "code \(vector.note ?? vector.seed)")
        }
    }

    func testChallengeAnswerVectors() throws {
        for vector in vectors.challengeAnswerVectors {
            let result = Primitives.challengeAnswerRow(
                seed: try seed(named: vector.seed),
                tableVersion: vector.tableVersion,
                rowIndex: vector.rowIndex
            )
            assertPhrase(result.ask, matches: vector.ask, context: "ask row \(vector.rowIndex)")
            assertPhrase(result.expect, matches: vector.expect, context: "expect row \(vector.rowIndex)")
        }
    }

    private func assertPhrase(_ result: PrimitivePhrase, matches expected: PhraseVector, context: String) {
        XCTAssertEqual(result.hashHex, expected.hashHex, "hash \(context)")
        XCTAssertEqual(result.offset, expected.offset, "offset \(context)")
        XCTAssertEqual(result.adjIndex, expected.adjIndex, "adj \(context)")
        XCTAssertEqual(result.nounIndex, expected.nounIndex, "noun \(context)")
        XCTAssertEqual(result.number, expected.number, "number \(context)")
        XCTAssertEqual(result.phrase, expected.phrase, "phrase \(context)")
    }

    private func seed(named name: String) throws -> Data {
        guard let hex = vectors.fixtures[name] else {
            throw TestError.missingFixture(name)
        }
        return try dataFromHex(hex)
    }
}

private struct PrimitiveVectorFile: Decodable {
    let fixtures: [String: String]
    let staticOverrideVectors: [StaticOverrideVector]
    let numericVectors: [NumericVector]
    let challengeAnswerVectors: [ChallengeAnswerVector]

    static let empty = PrimitiveVectorFile(
        fixtures: [:],
        staticOverrideVectors: [],
        numericVectors: [],
        challengeAnswerVectors: []
    )
}

private typealias StaticOverrideVector = PhraseVector

private struct PhraseVector: Decodable {
    let seed: String?
    let offset: Int
    let adjIndex: Int
    let nounIndex: Int
    let number: Int
    let phrase: String
    let hashHex: String
}

private struct NumericVector: Decodable {
    let seed: String
    let interval: Int
    let timestamp: Int
    let note: String?
    let counter: Int64
    let hashHex: String
    let offset: Int
    let code: String
}

private struct ChallengeAnswerVector: Decodable {
    let seed: String
    let tableVersion: Int
    let rowIndex: Int
    let ask: PhraseVector
    let expect: PhraseVector
}

private enum TestError: Error {
    case invalidHex(String)
    case missingFixture(String)
}

private func readTextResource(name: String, ext: String, fallback: String) -> String {
    let bundle = Bundle(for: PrimitiveVectorsTests.self)
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
        guard let byte = UInt8(hex[index..<next], radix: 16) else {
            throw TestError.invalidHex(hex)
        }
        data.append(byte)
        index = next
    }
    return data
}
