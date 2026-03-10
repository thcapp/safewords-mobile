import WidgetKit
import SwiftUI
import CryptoKit

// MARK: - Widget-local RotationInterval (mirrors main app, needed for decoding)

enum WidgetRotationInterval: String, Codable, CaseIterable, Identifiable {
    case hourly
    case daily
    case weekly
    case monthly

    var id: String { rawValue }

    var seconds: Int {
        switch self {
        case .hourly:  return 3_600
        case .daily:   return 86_400
        case .weekly:  return 604_800
        case .monthly: return 2_592_000
        }
    }
}

// MARK: - Widget-local models (mirrors main app, needed for decoding shared UserDefaults)

struct WidgetGroup: Identifiable, Codable {
    let id: UUID
    var name: String
    var interval: WidgetRotationInterval
    var members: [WidgetMember]
    let createdAt: Date
}

struct WidgetMember: Identifiable, Codable {
    let id: UUID
    var name: String
    var role: String
    let joinedAt: Date
}

// MARK: - Timeline Provider

struct SafewordsTimelineProvider: TimelineProvider {

    func placeholder(in context: Context) -> SafewordsEntry {
        SafewordsEntry(
            date: Date(),
            groupName: "Family",
            phrase: "Breezy Rocket 75",
            timeRemaining: 3600,
            intervalSeconds: 86400
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (SafewordsEntry) -> Void) {
        let entry = currentEntry() ?? placeholder(in: context)
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SafewordsEntry>) -> Void) {
        guard let (group, seed) = loadDefaultGroup() else {
            let entry = SafewordsEntry(
                date: Date(),
                groupName: nil,
                phrase: "No Group",
                timeRemaining: 0,
                intervalSeconds: 86400
            )
            let timeline = Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(3600)))
            completion(timeline)
            return
        }

        var entries: [SafewordsEntry] = []
        let now = Date()
        let interval = group.interval.seconds

        // Generate entries for the current period and the next few periods
        for i in 0..<5 {
            let timestamp = now.timeIntervalSince1970 + Double(i * interval)
            let counter = Int64(floor(timestamp / Double(interval)))
            let periodStart = Date(timeIntervalSince1970: Double(counter) * Double(interval))

            let phrase = WidgetTOTP.deriveSafewordCapitalized(
                seed: seed,
                interval: interval,
                timestamp: periodStart.timeIntervalSince1970
            )

            let remaining = WidgetTOTP.getTimeRemaining(
                interval: interval,
                timestamp: periodStart.timeIntervalSince1970
            )

            let entry = SafewordsEntry(
                date: periodStart,
                groupName: group.name,
                phrase: phrase,
                timeRemaining: remaining,
                intervalSeconds: interval
            )
            entries.append(entry)
        }

        // Reload after the current period ends
        let nextRotation = WidgetTOTP.nextRotationDate(interval: interval)
        let timeline = Timeline(entries: entries, policy: .after(nextRotation))
        completion(timeline)
    }

    // MARK: - Data Loading

    private func currentEntry() -> SafewordsEntry? {
        guard let (group, seed) = loadDefaultGroup() else { return nil }
        let now = Date()
        let timestamp = now.timeIntervalSince1970
        let phrase = WidgetTOTP.deriveSafewordCapitalized(
            seed: seed,
            interval: group.interval.seconds,
            timestamp: timestamp
        )
        let remaining = WidgetTOTP.getTimeRemaining(
            interval: group.interval.seconds,
            timestamp: timestamp
        )
        return SafewordsEntry(
            date: now,
            groupName: group.name,
            phrase: phrase,
            timeRemaining: remaining,
            intervalSeconds: group.interval.seconds
        )
    }

    private func loadDefaultGroup() -> (WidgetGroup, Data)? {
        let defaults = UserDefaults(suiteName: "group.com.thc.safewords")

        guard let groupsData = defaults?.data(forKey: "safewords.groups") else { return nil }

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .secondsSince1970
        guard let groups = try? decoder.decode([WidgetGroup].self, from: groupsData),
              let firstGroup = groups.first else { return nil }

        let selectedID: UUID
        if let idString = defaults?.string(forKey: "safewords.selectedGroupID"),
           let id = UUID(uuidString: idString) {
            selectedID = id
        } else {
            selectedID = firstGroup.id
        }

        guard let group = groups.first(where: { $0.id == selectedID }) else { return nil }
        guard let seed = WidgetKeychain.getSeed(forGroup: group.id) else { return nil }

        return (group, seed)
    }
}

// MARK: - Timeline Entry

struct SafewordsEntry: TimelineEntry {
    let date: Date
    let groupName: String?
    let phrase: String
    let timeRemaining: TimeInterval
    let intervalSeconds: Int
}

// MARK: - Widget Views

struct SafewordsWidgetSmallView: View {
    let entry: SafewordsEntry

    var body: some View {
        VStack(spacing: 8) {
            Text(entry.phrase)
                .font(.subheadline.bold())
                .foregroundStyle(Color(hex: "#2dd4bf"))
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)
                .lineLimit(2)

            HStack(spacing: 4) {
                Image(systemName: "clock")
                    .font(.caption2)
                Text(formatCompact(entry.timeRemaining))
                    .font(.caption2.monospacedDigit())
            }
            .foregroundStyle(.secondary)
        }
        .padding()
        .containerBackground(for: .widget) {
            Color(hex: "#0a0a0a")
        }
    }
}

struct SafewordsWidgetMediumView: View {
    let entry: SafewordsEntry

    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                if let name = entry.groupName {
                    Text(name)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                        .tracking(1)
                }

                Text(entry.phrase)
                    .font(.title3.bold())
                    .foregroundStyle(Color(hex: "#2dd4bf"))
                    .minimumScaleFactor(0.7)
                    .lineLimit(2)
            }

            Spacer()

            VStack(spacing: 4) {
                ZStack {
                    Circle()
                        .stroke(Color(hex: "#0f766e").opacity(0.3), lineWidth: 4)
                    Circle()
                        .trim(from: 0, to: CGFloat(1.0 - entry.timeRemaining / Double(entry.intervalSeconds)))
                        .stroke(Color(hex: "#2dd4bf"), style: StrokeStyle(lineWidth: 4, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                }
                .frame(width: 44, height: 44)

                Text(formatCompact(entry.timeRemaining))
                    .font(.caption2.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .containerBackground(for: .widget) {
            Color(hex: "#0a0a0a")
        }
    }
}

// MARK: - Widget Definition

struct SafewordsWidget: Widget {
    let kind: String = "SafewordsWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SafewordsTimelineProvider()) { entry in
            SafewordsWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Safewords")
        .description("Shows your current family safeword.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct SafewordsWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    let entry: SafewordsEntry

    var body: some View {
        switch family {
        case .systemSmall:
            SafewordsWidgetSmallView(entry: entry)
        case .systemMedium:
            SafewordsWidgetMediumView(entry: entry)
        default:
            SafewordsWidgetSmallView(entry: entry)
        }
    }
}

// MARK: - Widget-local TOTP derivation (duplicated — widget is a separate target)

enum WidgetTOTP {
    static let adjectives: [String] = {
        guard let url = Bundle.main.url(forResource: "adjectives", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let words = try? JSONDecoder().decode([String].self, from: data) else {
            return []
        }
        return words
    }()

    static let nouns: [String] = {
        guard let url = Bundle.main.url(forResource: "nouns", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let words = try? JSONDecoder().decode([String].self, from: data) else {
            return []
        }
        return words
    }()

    static func deriveSafewordCapitalized(seed: Data, interval: Int, timestamp: TimeInterval) -> String {
        let counter = Int64(floor(timestamp / Double(interval)))
        var bigEndian = counter.bigEndian
        let counterData = Data(bytes: &bigEndian, count: 8)
        let key = SymmetricKey(data: seed)
        let hmac = HMAC<SHA256>.authenticationCode(for: counterData, using: key)
        let hash = Array(Data(hmac))

        let offset = Int(hash[31] & 0x0F)
        let adjIndex = (Int(hash[offset] & 0x7F) << 8 | Int(hash[offset + 1])) % 197
        let nounIndex = (Int(hash[offset + 2] & 0x7F) << 8 | Int(hash[offset + 3])) % 300
        let number = (Int(hash[offset + 4] & 0x7F) << 8 | Int(hash[offset + 5])) % 100

        guard adjIndex < adjectives.count, nounIndex < nouns.count else {
            return "Error"
        }

        let adj = adjectives[adjIndex].capitalized
        let noun = nouns[nounIndex].capitalized
        return "\(adj) \(noun) \(number)"
    }

    static func getTimeRemaining(interval: Int, timestamp: TimeInterval) -> TimeInterval {
        let nextRotation = (floor(timestamp / Double(interval)) + 1) * Double(interval)
        return nextRotation - timestamp
    }

    static func nextRotationDate(interval: Int) -> Date {
        let now = Date().timeIntervalSince1970
        let next = (floor(now / Double(interval)) + 1) * Double(interval)
        return Date(timeIntervalSince1970: next)
    }
}

// MARK: - Widget-local Keychain access

enum WidgetKeychain {
    private static let service = "com.thc.safewords.seeds"
    private static let appGroupID = "group.com.thc.safewords"

    static func getSeed(forGroup groupID: UUID) -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: groupID.uuidString,
            kSecAttrAccessGroup as String: appGroupID,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return data
    }
}

// MARK: - Widget-local Color extension

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - Helpers

private func formatCompact(_ seconds: TimeInterval) -> String {
    let total = Int(seconds)
    let h = total / 3600
    let m = (total % 3600) / 60
    let s = total % 60
    if h > 0 { return String(format: "%dh %02dm", h, m) }
    return String(format: "%d:%02d", m, s)
}

#Preview(as: .systemSmall) {
    SafewordsWidget()
} timeline: {
    SafewordsEntry(date: .now, groupName: "Family", phrase: "Breezy Rocket 75", timeRemaining: 3600, intervalSeconds: 86400)
}

#Preview(as: .systemMedium) {
    SafewordsWidget()
} timeline: {
    SafewordsEntry(date: .now, groupName: "Family", phrase: "Breezy Rocket 75", timeRemaining: 3600, intervalSeconds: 86400)
}
