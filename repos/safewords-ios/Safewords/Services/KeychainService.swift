import Foundation
import Security

/// Manages 32-byte group seeds in the iOS Keychain.
/// Uses App Group access for WidgetKit compatibility.
enum KeychainService {

    /// App Group identifier shared between the main app and widget extension.
    static let appGroupID = "group.com.thc.safewords"

    /// Service identifier for Keychain items.
    private static let service = "com.thc.safewords.seeds"

    // MARK: - CRUD

    /// Save a 32-byte seed for a group.
    @discardableResult
    static func saveSeed(_ seed: Data, forGroup groupID: UUID) -> Bool {
        // Delete any existing entry first
        deleteSeed(forGroup: groupID)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: groupID.uuidString,
            kSecAttrAccessGroup as String: appGroupID,
            kSecValueData as String: seed,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]

        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    /// Retrieve the seed for a group.
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

        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return data
    }

    /// Delete the seed for a group.
    @discardableResult
    static func deleteSeed(forGroup groupID: UUID) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: groupID.uuidString,
            kSecAttrAccessGroup as String: appGroupID
        ]

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    /// Delete all seeds (used when clearing all app data).
    @discardableResult
    static func deleteAllSeeds() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccessGroup as String: appGroupID
        ]

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}
