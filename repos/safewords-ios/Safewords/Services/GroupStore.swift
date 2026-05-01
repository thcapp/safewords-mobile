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
            return groups.first { $0.id == id } ?? groups.first
        }
        return groups.first
    }

    private let defaults: UserDefaults?

    private enum Keys {
        static let groups = "safewords.groups"
        static let selectedGroup = "safewords.selectedGroupID"
        static let emergencyOverridePrefix = "safewords.emergencyOverride."
    }

    init() {
        defaults = UserDefaults(suiteName: KeychainService.appGroupID)
        loadGroups()
        loadSelectedGroup()
    }

    // MARK: - CRUD

    /// Create a new group with a fresh random seed.
    @discardableResult
    func createGroup(
        name: String,
        interval: RotationInterval = .daily,
        creatorName: String,
        seed providedSeed: Data? = nil
    ) -> Group? {
        let seed = providedSeed ?? TOTPDerivation.generateSeed()
        let creator = Member(name: creatorName, role: .creator)
        let group = Group(name: name, interval: interval, members: [creator])

        guard KeychainService.saveSeed(seed, forGroup: group.id) else {
            return nil
        }
        groups.append(group)
        groups.sort { $0.createdAt < $1.createdAt }
        saveGroups()
        selectedGroupID = group.id

        return group
    }

    /// Create a group from a scanned QR code (joining an existing group).
    @discardableResult
    func joinGroup(name: String, seed: Data, interval: RotationInterval, memberName: String) -> Group? {
        let member = Member(name: memberName, role: .member)
        let group = Group(name: name, interval: interval, members: [member])

        guard KeychainService.saveSeed(seed, forGroup: group.id) else {
            return nil
        }
        groups.append(group)
        groups.sort { $0.createdAt < $1.createdAt }
        saveGroups()
        selectedGroupID = group.id

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
        groups[index].primitives.rotatingWord.intervalSeconds = interval.seconds
        saveGroups()
    }

    /// Update a group's verification primitive configuration.
    func updatePrimitives(_ groupID: UUID, _ update: (inout PrimitivesConfig) -> Void) {
        guard let index = groups.firstIndex(where: { $0.id == groupID }) else { return }
        update(&groups[index].primitives)
        groups[index].primitives = groups[index].primitives.normalized(legacyInterval: groups[index].interval)
        saveGroups()
    }

    func setWordFormat(groupID: UUID, format: WordFormat) {
        updatePrimitives(groupID) { config in
            config.rotatingWord.wordFormat = format
        }
    }

    func setChallengeAnswerEnabled(groupID: UUID, enabled: Bool) {
        updatePrimitives(groupID) { config in
            config.challengeAnswer.enabled = enabled
        }
    }

    func setStaticOverrideEnabled(groupID: UUID, enabled: Bool) {
        updatePrimitives(groupID) { config in
            config.staticOverride.enabled = enabled
        }
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
        defaults?.removeObject(forKey: Keys.emergencyOverridePrefix + groupID.uuidString)
        saveGroups()

        if selectedGroupID == groupID {
            selectedGroupID = groups.first?.id
        }
    }

    /// Store or clear a per-group fallback word for emergency verification.
    func setEmergencyOverrideWord(groupID: UUID, word: String?) {
        let key = Keys.emergencyOverridePrefix + groupID.uuidString
        let trimmed = word?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let trimmed, !trimmed.isEmpty {
            defaults?.set(trimmed, forKey: key)
        } else {
            defaults?.removeObject(forKey: key)
        }
    }

    /// Return a per-group fallback word, if one has been configured.
    func emergencyOverrideWord(groupID: UUID) -> String? {
        defaults?.string(forKey: Keys.emergencyOverridePrefix + groupID.uuidString)
    }

    /// Delete all local group metadata, seeds, and per-group fallback words.
    func resetAllData() {
        for id in groups.map(\.id) {
            KeychainService.deleteSeed(forGroup: id)
            defaults?.removeObject(forKey: Keys.emergencyOverridePrefix + id.uuidString)
        }
        groups.removeAll()
        saveGroups()
        selectedGroupID = nil
        KeychainService.deleteAllSeeds()
    }

    // MARK: - Safeword Derivation

    /// Get the current safeword for a group (capitalized for display).
    func currentSafeword(for group: Group) -> String? {
        guard let seed = KeychainService.getSeed(forGroup: group.id) else { return nil }
        let timestamp = Date().timeIntervalSince1970
        return displayValue(for: group, seed: seed, timestamp: timestamp)
    }

    /// Get the current safeword for a group at a specific timestamp.
    func safeword(for group: Group, at timestamp: TimeInterval) -> String? {
        guard let seed = KeychainService.getSeed(forGroup: group.id) else { return nil }
        return displayValue(for: group, seed: seed, timestamp: timestamp)
    }

    func hasAnyVerifyPrimitive() -> Bool {
        groups.contains { $0.primitives.needsVerifySurface }
    }

    func verifyNeeded(for group: Group) -> Bool {
        group.primitives.needsVerifySurface
    }

    private func displayValue(for group: Group, seed: Data, timestamp: TimeInterval) -> String {
        let interval = group.primitives.rotatingWord.intervalSeconds > 0
            ? group.primitives.rotatingWord.intervalSeconds
            : group.interval.seconds
        if group.primitives.rotatingWord.wordFormat == .numeric {
            return Primitives.numeric(seed: seed, intervalSeconds: interval, timestamp: timestamp).code
        }
        return TOTPDerivation.deriveSafewordCapitalized(
            seed: seed,
            interval: interval,
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
