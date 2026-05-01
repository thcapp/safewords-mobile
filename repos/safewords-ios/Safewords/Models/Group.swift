import Foundation

/// A family group that shares a rotating safeword via a common seed.
struct Group: Identifiable, Codable, Hashable {
    var schemaVersion: Int
    let id: UUID
    var name: String
    var interval: RotationInterval
    var primitives: PrimitivesConfig
    var members: [Member]
    let createdAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        interval: RotationInterval = .daily,
        primitives: PrimitivesConfig? = nil,
        members: [Member] = [],
        createdAt: Date = Date()
    ) {
        self.schemaVersion = PrimitivesConfig.schemaVersion
        self.id = id
        self.name = name
        self.interval = interval
        self.primitives = (primitives ?? .legacy(interval: interval)).normalized(legacyInterval: interval)
        self.members = members
        self.createdAt = createdAt
    }

    private enum CodingKeys: String, CodingKey {
        case schemaVersion
        case id
        case name
        case interval
        case primitives
        case members
        case createdAt
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        schemaVersion = try container.decodeIfPresent(Int.self, forKey: .schemaVersion) ?? PrimitivesConfig.schemaVersion
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        interval = try container.decodeIfPresent(RotationInterval.self, forKey: .interval) ?? .daily
        primitives = (try container.decodeIfPresent(PrimitivesConfig.self, forKey: .primitives) ?? .legacy(interval: interval))
            .normalized(legacyInterval: interval)
        members = try container.decodeIfPresent([Member].self, forKey: .members) ?? []
        createdAt = try container.decodeIfPresent(Date.self, forKey: .createdAt) ?? Date()
        schemaVersion = PrimitivesConfig.schemaVersion
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(PrimitivesConfig.schemaVersion, forKey: .schemaVersion)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(interval, forKey: .interval)
        try container.encode(primitives.normalized(legacyInterval: interval), forKey: .primitives)
        try container.encode(members, forKey: .members)
        try container.encode(createdAt, forKey: .createdAt)
    }
}

/// Rotation interval for safeword derivation.
enum RotationInterval: String, Codable, CaseIterable, Identifiable {
    case hourly
    case daily
    case weekly
    case monthly

    var id: String { rawValue }

    /// Duration of the interval in seconds.
    var seconds: Int {
        switch self {
        case .hourly:  return 3_600
        case .daily:   return 86_400
        case .weekly:  return 604_800
        case .monthly: return 2_592_000
        }
    }

    /// Human-readable display name.
    var displayName: String {
        switch self {
        case .hourly:  return "Every Hour"
        case .daily:   return "Every Day"
        case .weekly:  return "Every Week"
        case .monthly: return "Every Month"
        }
    }

    /// Short label for compact display.
    var shortLabel: String {
        rawValue.capitalized
    }
}
