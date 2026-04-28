import Foundation

enum RecoveryPhraseService {
    enum RecoveryError: LocalizedError {
        case invalidLength
        case unsupportedWords

        var errorDescription: String? {
            switch self {
            case .invalidLength:
                return "Paste a 64-character hex seed or 43-character base64url seed."
            case .unsupportedWords:
                return "Word recovery phrases are not available in this build. Paste the hex recovery code shown when the group was created."
            }
        }
    }

    static func displayCode(for seed: Data) -> String {
        seed.map { String(format: "%02x", $0) }
            .joined()
            .chunked(into: 8)
            .joined(separator: " ")
    }

    static func parseSeed(from input: String) throws -> Data {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        let compact = trimmed
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "-", with: "")
            .replacingOccurrences(of: "\n", with: "")
            .lowercased()

        if compact.range(of: #"^[0-9a-f]{64}$"#, options: .regularExpression) != nil {
            return Data(hexString: compact)
        }

        if let data = Data(base64URLEncoded: trimmed), data.count == 32 {
            return data
        }

        let wordCount = trimmed.split(whereSeparator: \.isWhitespace).count
        if wordCount == 12 || wordCount == 24 {
            throw RecoveryError.unsupportedWords
        }
        throw RecoveryError.invalidLength
    }
}

private extension String {
    func chunked(into size: Int) -> [String] {
        stride(from: 0, to: count, by: size).map { offset in
            let start = index(startIndex, offsetBy: offset)
            let end = index(start, offsetBy: size, limitedBy: endIndex) ?? endIndex
            return String(self[start..<end])
        }
    }
}

private extension Data {
    init(hexString: String) {
        var data = Data()
        var index = hexString.startIndex
        while index < hexString.endIndex {
            let next = hexString.index(index, offsetBy: 2)
            let byte = hexString[index..<next]
            data.append(UInt8(byte, radix: 16) ?? 0)
            index = next
        }
        self = data
    }
}
