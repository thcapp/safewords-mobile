import Foundation

/// A member within a safeword group.
struct Member: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var role: Role
    let joinedAt: Date

    init(id: UUID = UUID(), name: String, role: Role = .member, joinedAt: Date = Date()) {
        self.id = id
        self.name = name
        self.role = role
        self.joinedAt = joinedAt
    }
}

/// Role within a group.
enum Role: String, Codable, CaseIterable {
    case creator
    case member

    var displayName: String {
        rawValue.capitalized
    }
}

extension Member {
    /// Two-letter initials for avatar display.
    var initials: String {
        let parts = name.split(separator: " ")
        if parts.count >= 2 {
            return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
        }
        return String(name.prefix(2)).uppercased()
    }

    /// Deterministic color index based on member ID for avatar coloring.
    var colorIndex: Int {
        abs(id.hashValue) % 8
    }
}
