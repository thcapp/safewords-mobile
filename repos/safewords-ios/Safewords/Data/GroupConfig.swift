import Foundation

enum WordFormat: String, Codable, CaseIterable, Hashable, Identifiable {
    case adjectiveNounNumber = "adjective_noun_number"
    case numeric

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .adjectiveNounNumber:
            return "Words"
        case .numeric:
            return "6 digits"
        }
    }
}

struct RotatingWordConfig: Codable, Hashable, Equatable {
    var enabled: Bool
    var intervalSeconds: Int
    var wordFormat: WordFormat

    init(enabled: Bool = true, intervalSeconds: Int = RotationInterval.daily.seconds, wordFormat: WordFormat = .adjectiveNounNumber) {
        self.enabled = enabled
        self.intervalSeconds = intervalSeconds
        self.wordFormat = wordFormat
    }

    static func legacy(interval: RotationInterval) -> RotatingWordConfig {
        RotatingWordConfig(enabled: true, intervalSeconds: interval.seconds, wordFormat: .adjectiveNounNumber)
    }

    private enum CodingKeys: String, CodingKey {
        case enabled
        case intervalSeconds
        case wordFormat
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        enabled = try container.decodeIfPresent(Bool.self, forKey: .enabled) ?? true
        intervalSeconds = try container.decodeIfPresent(Int.self, forKey: .intervalSeconds) ?? RotationInterval.daily.seconds
        wordFormat = try container.decodeIfPresent(WordFormat.self, forKey: .wordFormat) ?? .adjectiveNounNumber
    }
}

struct ChallengeAnswerConfig: Codable, Hashable, Equatable {
    var enabled: Bool
    var tableVersion: Int
    var rowCount: Int

    init(enabled: Bool = false, tableVersion: Int = 1, rowCount: Int = 100) {
        self.enabled = enabled
        self.tableVersion = tableVersion
        self.rowCount = rowCount
    }

    private enum CodingKeys: String, CodingKey {
        case enabled
        case tableVersion
        case rowCount
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        enabled = try container.decodeIfPresent(Bool.self, forKey: .enabled) ?? false
        tableVersion = try container.decodeIfPresent(Int.self, forKey: .tableVersion) ?? 1
        rowCount = try container.decodeIfPresent(Int.self, forKey: .rowCount) ?? 100
    }
}

struct StaticOverrideConfig: Codable, Hashable, Equatable {
    var enabled: Bool
    var derivationVersion: Int
    var printedAt: Date?

    init(enabled: Bool = false, derivationVersion: Int = 1, printedAt: Date? = nil) {
        self.enabled = enabled
        self.derivationVersion = derivationVersion
        self.printedAt = printedAt
    }

    private enum CodingKeys: String, CodingKey {
        case enabled
        case derivationVersion
        case printedAt
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        enabled = try container.decodeIfPresent(Bool.self, forKey: .enabled) ?? false
        derivationVersion = try container.decodeIfPresent(Int.self, forKey: .derivationVersion) ?? 1
        printedAt = try container.decodeIfPresent(Date.self, forKey: .printedAt)
    }
}

struct PrimitivesConfig: Codable, Hashable, Equatable {
    static let schemaVersion = 2

    var rotatingWord: RotatingWordConfig
    var challengeAnswer: ChallengeAnswerConfig
    var staticOverride: StaticOverrideConfig

    init(
        rotatingWord: RotatingWordConfig,
        challengeAnswer: ChallengeAnswerConfig = ChallengeAnswerConfig(),
        staticOverride: StaticOverrideConfig = StaticOverrideConfig()
    ) {
        self.rotatingWord = rotatingWord
        self.challengeAnswer = challengeAnswer
        self.staticOverride = staticOverride
    }

    static func legacy(interval: RotationInterval) -> PrimitivesConfig {
        PrimitivesConfig(rotatingWord: .legacy(interval: interval))
    }

    static func legacy(intervalSeconds: Int) -> PrimitivesConfig {
        PrimitivesConfig(
            rotatingWord: RotatingWordConfig(
                enabled: true,
                intervalSeconds: intervalSeconds,
                wordFormat: .adjectiveNounNumber
            )
        )
    }

    var needsVerifySurface: Bool {
        challengeAnswer.enabled || staticOverride.enabled
    }

    func normalized(legacyInterval: RotationInterval) -> PrimitivesConfig {
        var copy = self
        if copy.rotatingWord.intervalSeconds <= 0 {
            copy.rotatingWord.intervalSeconds = legacyInterval.seconds
        }
        if copy.challengeAnswer.tableVersion <= 0 {
            copy.challengeAnswer.tableVersion = 1
        }
        if copy.challengeAnswer.rowCount <= 0 {
            copy.challengeAnswer.rowCount = 100
        }
        if copy.staticOverride.derivationVersion <= 0 {
            copy.staticOverride.derivationVersion = 1
        }
        return copy
    }

    private enum CodingKeys: String, CodingKey {
        case rotatingWord
        case challengeAnswer
        case staticOverride
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        rotatingWord = try container.decodeIfPresent(RotatingWordConfig.self, forKey: .rotatingWord)
            ?? RotatingWordConfig(enabled: true, intervalSeconds: RotationInterval.daily.seconds, wordFormat: .adjectiveNounNumber)
        challengeAnswer = try container.decodeIfPresent(ChallengeAnswerConfig.self, forKey: .challengeAnswer)
            ?? ChallengeAnswerConfig()
        staticOverride = try container.decodeIfPresent(StaticOverrideConfig.self, forKey: .staticOverride)
            ?? StaticOverrideConfig()
    }
}

struct GroupConfigRecord: Codable, Equatable {
    var schemaVersion: Int?
    var id: UUID
    var name: String
    var seed: String
    var intervalSeconds: Int?
    var primitives: PrimitivesConfig?

    private enum CodingKeys: String, CodingKey {
        case schemaVersion
        case id
        case name
        case seed
        case intervalSeconds
        case primitives
    }

    func migrated() -> GroupConfigRecord {
        guard (schemaVersion ?? 1) < PrimitivesConfig.schemaVersion || primitives == nil else {
            return self
        }

        return GroupConfigRecord(
            schemaVersion: PrimitivesConfig.schemaVersion,
            id: id,
            name: name,
            seed: seed,
            intervalSeconds: nil,
            primitives: .legacy(intervalSeconds: intervalSeconds ?? RotationInterval.daily.seconds)
        )
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(schemaVersion, forKey: .schemaVersion)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(seed, forKey: .seed)
        try container.encodeIfPresent(intervalSeconds, forKey: .intervalSeconds)
        try container.encodeIfPresent(primitives, forKey: .primitives)
    }
}
