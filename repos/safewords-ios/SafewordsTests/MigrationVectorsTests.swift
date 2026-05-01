import XCTest
@testable import Safewords

final class MigrationVectorsTests: XCTestCase {
    private lazy var vectors: MigrationVectorFile = {
        let text = readTextResource(name: "migration-vectors", ext: "json", fallback: "shared/migration-vectors.json")
        do {
            return try JSONDecoder().decode(MigrationVectorFile.self, from: Data(text.utf8))
        } catch {
            XCTFail("Could not decode migration-vectors.json: \(error)")
            return MigrationVectorFile(v1_2ToV1_3: [])
        }
    }()

    func testV12ToV13MigrationVectors() throws {
        for vector in vectors.v1_2ToV1_3 {
            let migrated = vector.input.migrated()
            XCTAssertEqual(migrated, vector.output, vector.name)
        }
    }
}

private struct MigrationVectorFile: Decodable {
    let v1_2ToV1_3: [MigrationVector]

    private enum CodingKeys: String, CodingKey {
        case v1_2ToV1_3 = "v1.2_to_v1.3"
    }
}

private struct MigrationVector: Decodable {
    let name: String
    let input: GroupConfigRecord
    let output: GroupConfigRecord
}

private func readTextResource(name: String, ext: String, fallback: String) -> String {
    let bundle = Bundle(for: MigrationVectorsTests.self)
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
