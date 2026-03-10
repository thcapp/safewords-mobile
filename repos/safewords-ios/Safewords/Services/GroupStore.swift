import Foundation
import SwiftUI

/// Observable store managing all safeword groups.
/// Group metadata lives in shared UserDefaults (App Group); seeds live in Keychain.
@Observable
final class GroupStore {

    /// All groups, sorted by creation date.
    private(set) var groups: [Group] = []

    /// The currently selected group ID (persisted).
    var selectedGroupID: UUID? {
        didSet {
            if let id = selectedGroupID {
                defaults?.set(id.uuidString, forKey: Keys.selectedGroup)
            } else {
                defaults?.removeObject(forKey: Keys.selectedGroup)
            }
        }
    }

    /// The currently selected group.
    var selectedGroup: Group? {
        if let id = selectedGroupID {
            return groups.first { $0.id == id }
        }
        return groups.first
    }

    private let defaults: UserDefaults?

    private enum Keys {
        static let groups = "safewords.groups"
        static let selectedGroup = "safewords.selectedGroupID"
    }

    init() {
        defaults = UserDefaults(suiteName: KeychainService.appGroupID)
        loadGroups()
        loadSelectedGroup()
    }

    // MARK: - CRUD

    /// Create a new group with a fresh random seed.
    @discardableResult
    func createGroup(name: String, interval: RotationInterval = .daily, creatorName: String) -> Group {
        let seed = TOTPDerivation.generateSeed()
        let creator = Member(name: creatorName, role: .creator)
        let group = Group(name: name, interval: interval, members: [creator])

        KeychainService.saveSeed(seed, forGroup: group.id)
        groups.append(group)
        groups.sort { $0.createdAt < $1.createdAt }
        saveGroups()

        if selectedGroupID == nil {
            selectedGroupID = group.id
        }

        return group
    }

    /// Create a group from a scanned QR code (joining an existing group).
    @discardableResult
    func joinGroup(name: String, seed: Data, interval: RotationInterval, memberName: String) -> Group {
        let member = Member(name: memberName, role: .member)
        let group = Group(name: name, interval: interval, members: [member])

        KeychainService.saveSeed(seed, forGroup: group.id)
        groups.append(group)
        groups.sort { $0.createdAt < $1.createdAt }
        saveGroups()

        if selectedGroupID == nil {
            selectedGroupID = group.id
        }

        return group
    }

    /// Update a group's name.
    func updateGroupName(_ groupID: UUID, name: String) {
        guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
        groups[index].name = name
        saveGroups()
    }

    /// Update a group's rotation interval.
    func updateGroupInterval(_ groupID: UUID, interval: RotationInterval) {
        guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
        groups[index].interval = interval
        saveGroups()
    }

    /// Add a member to a group.
    func addMember(_ member: Member, toGroup groupID: UUID) {
        guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
        groups[index].members.append(member)
        saveGroups()
    }

    /// Remove a member from a group.
    func removeMember(_ memberID: UUID, fromGroup groupID: UUID) {
        guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
        groups[index].members.removeAll { $0.id == memberID }
        saveGroups()
    }

    /// Delete a group and its seed.
    func deleteGroup(_ groupID: UUID) {
        KeychainService.deleteSeed(forGroup: groupID)
        groups.removeAll { $0.id == groupID }
        saveGroups()

        if selectedGroupID == groupID {
            selectedGroupID = groups.first?.id
        }
    }

    // MARK: - Safeword Derivation

    /// Get the current safeword for a group (capitalized for display).
    func currentSafeword(for group: Group) -> String? {
        guard let seed = KeychainService.getSeed(forGroup: group.id) else { return nil }
        let timestamp = Date().timeIntervalSince1970
        return TOTPDerivation.deriveSafewordCapitalized(
            seed: seed,
            interval: group.interval.seconds,
            timestamp: timestamp
        )
    }

    /// Get the current safeword for a group at a specific timestamp.
    func safeword(for group: Group, at timestamp: TimeInterval) -> String? {
        guard let seed = KeychainService.getSeed(forGroup: group.id) else { return nil }
        return TOTPDerivation.deriveSafewordCapitalized(
            seed: seed,
            interval: group.interval.seconds,
            timestamp: timestamp
        )
    }

    /// Get the seed data for a group (for QR sharing).
    func seed(for groupID: UUID) -> Data? {
        KeychainService.getSeed(forGroup: groupID)
    }

    // MARK: - Persistence

    private func saveGroups() {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .secondsSince1970
        if let data = try? encoder.encode(groups) {
            defaults?.set(data, forKey: Keys.groups)
        }
    }

    private func loadGroups() {
        guard let data = defaults?.data(forKey: Keys.groups) else { return }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .secondsSince1970
        if let decoded = try? decoder.decode([Group].self, from: data) {
            groups = decoded.sorted { $0.createdAt < $1.createdAt }
        }
    }

    private func loadSelectedGroup() {
        if let idString = defaults?.string(forKey: Keys.selectedGroup),
           let id = UUID(uuidString: idString) {
            selectedGroupID = id
        } else {
            selectedGroupID = groups.first?.id
        }
    }
}
