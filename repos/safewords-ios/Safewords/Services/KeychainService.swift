import Foundation
import Security

/// Manages 32-byte group seeds in the iOS Keychain.
/// Uses App Group access for WidgetKit compatibility.
enum KeychainService {

    /// App Group identifier shared between the main app and widget extension.
    static let appGroupID = "group.app.thc.safewords"

    /// Keychain sharing requires the Team/App Identifier prefix, unlike App Group UserDefaults.
    static var keychainAccessGroup: String? {
        guard let prefix = Bundle.main.object(forInfoDictionaryKey: "AppIdentifierPrefix") as? String,
              !prefix.isEmpty else {
            return nil
        }
        return "\(prefix)\(appGroupID)"
    }

    /// Service identifier for Keychain items.
    private static let service = "app.thc.safewords.seeds"

    // MARK: - CRUD

    /// Save a 32-byte seed for a group.
    @discardableResult
    static func saveSeed(_ seed: Data, forGroup groupID: UUID) -> Bool {
        // Delete any existing entry first
        deleteSeed(forGroup: groupID)

        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: groupID.uuidString,
            kSecValueData as String: seed,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        addAccessGroupIfAvailable(to: &query)

        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    /// Retrieve the seed for a group.
    static func getSeed(forGroup groupID: UUID) -> Data? {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: groupID.uuidString,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        addAccessGroupIfAvailable(to: &query)

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return data
    }

    /// Delete the seed for a group.
    @discardableResult
    static func deleteSeed(forGroup groupID: UUID) -> Bool {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: groupID.uuidString
        ]
        addAccessGroupIfAvailable(to: &query)

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    /// Delete all seeds (used when clearing all app data).
    @discardableResult
    static func deleteAllSeeds() -> Bool {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service
        ]
        addAccessGroupIfAvailable(to: &query)

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    private static func addAccessGroupIfAvailable(to query: inout [String: Any]) {
        if let accessGroup = keychainAccessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }
    }
}
