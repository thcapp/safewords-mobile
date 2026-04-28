import Foundation

struct DrillSession: Identifiable, Codable, Hashable {
    let id: UUID
    let groupID: UUID?
    let groupName: String
    let scenario: String
    let startedAt: Date
    let completedAt: Date
    let success: Bool

    init(
        id: UUID = UUID(),
        groupID: UUID?,
        groupName: String,
        scenario: String,
        startedAt: Date = Date(),
        completedAt: Date = Date(),
        success: Bool
    ) {
        self.id = id
        self.groupID = groupID
        self.groupName = groupName
        self.scenario = scenario
        self.startedAt = startedAt
        self.completedAt = completedAt
        self.success = success
    }
}

enum DrillService {
    private static let key = "safewords.drillSessions"
    private static var defaults: UserDefaults? {
        UserDefaults(suiteName: KeychainService.appGroupID)
    }

    static func sessions() -> [DrillSession] {
        guard let data = defaults?.data(forKey: key) else { return [] }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .secondsSince1970
        return (try? decoder.decode([DrillSession].self, from: data)) ?? []
    }

    @discardableResult
    static func record(group: Group?, scenario: String, success: Bool) -> DrillSession {
        let session = DrillSession(
            groupID: group?.id,
            groupName: group?.name ?? "No circle",
            scenario: scenario,
            success: success
        )
        var all = sessions()
        all.insert(session, at: 0)
        save(all)
        return session
    }

    static func clear() {
        defaults?.removeObject(forKey: key)
    }

    private static func save(_ sessions: [DrillSession]) {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .secondsSince1970
        if let data = try? encoder.encode(Array(sessions.prefix(25))) {
            defaults?.set(data, forKey: key)
        }
    }
}
