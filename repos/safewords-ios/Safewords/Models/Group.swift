import Foundation

/// A family group that shares a rotating safeword via a common seed.
struct Group: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var interval: RotationInterval
    var members: [Member]
    let createdAt: Date

    init(id: UUID = UUID(), name: String, interval: RotationInterval = .daily, members: [Member] = [], createdAt: Date = Date()) {
        self.id = id
        self.name = name
        self.interval = interval
        self.members = members
        self.createdAt = createdAt
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
