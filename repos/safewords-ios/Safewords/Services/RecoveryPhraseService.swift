import Foundation

enum RecoveryPhraseService {
    enum RecoveryError: LocalizedError {
        case invalidSeedFormat
        case invalidHexCharacters

        var errorDescription: String? {
            switch self {
            case .invalidSeedFormat:
                return "Recovery phrase must be exactly 24 words, or seed must be 64 hex characters."
            case .invalidHexCharacters:
                return "Hex seed contains characters other than 0-9 and a-f."
            }
        }
    }

    static func displayCode(for seed: Data) -> String {
        (try? RecoveryPhrase.encode(seed: seed)) ?? seedHex(seed)
    }

    static func parseSeed(from input: String) throws -> Data {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            throw RecoveryPhraseError.emptyInput
        }

        let compact = trimmed
            .lowercased(with: Locale(identifier: "en_US_POSIX"))
            .replacingOccurrences(of: #"[\s-]"#, with: "", options: .regularExpression)

        if compact.count == 64 {
            guard compact.range(of: #"^[0-9a-f]{64}$"#, options: .regularExpression) != nil else {
                throw RecoveryError.invalidHexCharacters
            }
            return Data(hexString: compact)
        }

        if trimmed.contains(where: \.isLetter), trimmed.contains(where: \.isWhitespace) {
            return try RecoveryPhrase.decode(input: trimmed)
        }

        if let data = Data(base64URLEncoded: trimmed), data.count == 32 {
            return data
        }

        throw RecoveryError.invalidSeedFormat
    }

    static func seedHex(_ seed: Data) -> String {
        seed.map { String(format: "%02x", $0) }.joined()
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
