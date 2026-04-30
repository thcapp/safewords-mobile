import CryptoKit
import Foundation

enum RecoveryPhraseError: Error, Equatable {
    case emptyInput
    case wrongWordCount
    case unknownWord(index: Int, word: String)
    case badChecksum

    var code: String {
        switch self {
        case .emptyInput:
            return "EMPTY_INPUT"
        case .wrongWordCount:
            return "WRONG_WORD_COUNT"
        case .unknownWord:
            return "UNKNOWN_WORD"
        case .badChecksum:
            return "BAD_CHECKSUM"
        }
    }

    var userMessage: String {
        switch self {
        case .emptyInput:
            return "Enter your 24-word recovery phrase."
        case .wrongWordCount:
            return "Recovery phrase must be exactly 24 words."
        case let .unknownWord(index, word):
            return "Word \(index) is not in the recovery word list: \"\(word)\"."
        case .badChecksum:
            return "Recovery phrase checksum is invalid. Check the words and order."
        }
    }
}

extension RecoveryPhraseError: LocalizedError {
    var errorDescription: String? {
        userMessage
    }
}

struct Bip39 {
    static let expectedWordCount = 24
    static let expectedEntropyBytes = 32
    static let expectedVocabularySize = 2_048

    private static let checksumBits = 8
    private static let bitsPerWord = 11

    let wordlist: [String]
    private let wordIndex: [String: Int]

    init(wordlist: [String]) {
        precondition(
            wordlist.count == Self.expectedVocabularySize,
            "BIP39 English wordlist must have \(Self.expectedVocabularySize) words (got \(wordlist.count))"
        )
        self.wordlist = wordlist
        self.wordIndex = Dictionary(uniqueKeysWithValues: wordlist.enumerated().map { ($0.element, $0.offset) })
    }

    func encode(seed: Data) throws -> String {
        guard seed.count == Self.expectedEntropyBytes else {
            throw RecoveryPhraseRuntimeError.invalidSeedLength(seed.count)
        }

        let checksumByte = Self.sha256(seed)[0]
        var indices: [Int] = []
        indices.reserveCapacity(Self.expectedWordCount)

        var bitBuffer: UInt64 = 0
        var bitsInBuffer = 0

        for byte in seed {
            bitBuffer = (bitBuffer << 8) | UInt64(byte)
            bitsInBuffer += 8
            drainWords(from: &bitBuffer, bitsInBuffer: &bitsInBuffer, into: &indices)
        }

        bitBuffer = (bitBuffer << Self.checksumBits) | UInt64(checksumByte)
        bitsInBuffer += Self.checksumBits
        drainWords(from: &bitBuffer, bitsInBuffer: &bitsInBuffer, into: &indices)

        guard indices.count == Self.expectedWordCount else {
            throw RecoveryPhraseRuntimeError.invalidWordIndexCount(indices.count)
        }

        return indices.map { wordlist[$0] }.joined(separator: " ")
    }

    func decode(input: String) throws -> Data {
        let tokens = normalize(input: input)
        if tokens.isEmpty {
            throw RecoveryPhraseError.emptyInput
        }
        guard tokens.count == Self.expectedWordCount else {
            throw RecoveryPhraseError.wrongWordCount
        }

        let indices = try tokens.enumerated().map { offset, token in
            guard let index = wordIndex[token] else {
                throw RecoveryPhraseError.unknownWord(index: offset + 1, word: token)
            }
            return index
        }

        var seed = Data()
        seed.reserveCapacity(Self.expectedEntropyBytes)
        var bitBuffer: UInt64 = 0
        var bitsInBuffer = 0

        for index in indices {
            bitBuffer = (bitBuffer << Self.bitsPerWord) | UInt64(index)
            bitsInBuffer += Self.bitsPerWord
            while bitsInBuffer >= 8 && seed.count < Self.expectedEntropyBytes {
                let shift = bitsInBuffer - 8
                seed.append(UInt8((bitBuffer >> shift) & 0xff))
                bitsInBuffer -= 8
                bitBuffer &= Self.lowBitMask(bitsInBuffer)
            }
        }

        guard bitsInBuffer == Self.checksumBits else {
            throw RecoveryPhraseRuntimeError.invalidTrailingBitCount(bitsInBuffer)
        }

        let providedChecksum = UInt8(bitBuffer & 0xff)
        let expectedChecksum = Self.sha256(seed)[0]
        guard providedChecksum == expectedChecksum else {
            throw RecoveryPhraseError.badChecksum
        }

        return seed
    }

    func normalize(input: String) -> [String] {
        let normalized = input.decomposedStringWithCompatibilityMapping
        let lowered = normalized
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased(with: Locale(identifier: "en_US_POSIX"))

        guard !lowered.isEmpty else { return [] }
        return lowered.split(whereSeparator: \.isWhitespace).map(String.init)
    }

    private func drainWords(from bitBuffer: inout UInt64, bitsInBuffer: inout Int, into indices: inout [Int]) {
        while bitsInBuffer >= Self.bitsPerWord {
            let shift = bitsInBuffer - Self.bitsPerWord
            indices.append(Int((bitBuffer >> shift) & 0x7ff))
            bitsInBuffer -= Self.bitsPerWord
            bitBuffer &= Self.lowBitMask(bitsInBuffer)
        }
    }

    private static func sha256(_ data: Data) -> [UInt8] {
        Array(SHA256.hash(data: data))
    }

    private static func lowBitMask(_ bits: Int) -> UInt64 {
        bits == 0 ? 0 : (UInt64(1) << bits) - 1
    }
}

enum RecoveryPhrase {
    private static let bip39: Bip39 = {
        Bip39(wordlist: loadWordlist(from: .main))
    }()

    static func encode(seed: Data) throws -> String {
        try bip39.encode(seed: seed)
    }

    static func decode(input: String) throws -> Data {
        try bip39.decode(input: input)
    }

    static func normalize(input: String) -> [String] {
        bip39.normalize(input: input)
    }

    static func loadWordlist(from bundle: Bundle) -> [String] {
        guard let url = bundle.url(forResource: "bip39-english", withExtension: "txt"),
              let raw = try? String(contentsOf: url, encoding: .utf8) else {
            fatalError("bip39-english.txt is missing from bundle resources")
        }
        return raw
            .split(separator: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }
}

private enum RecoveryPhraseRuntimeError: Error {
    case invalidSeedLength(Int)
    case invalidWordIndexCount(Int)
    case invalidTrailingBitCount(Int)
}
