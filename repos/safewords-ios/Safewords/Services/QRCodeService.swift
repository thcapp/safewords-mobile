import Foundation
import CoreImage
import CoreImage.CIFilterBuiltins
import UIKit

/// Generates and parses QR codes for sharing group seeds.
enum QRCodeService {

    // MARK: - QR Payload

    /// QR code payload structure (v1).
    struct QRPayload: Codable {
        let v: Int
        let name: String
        let seed: String  // base64url-encoded, no padding
        let interval: String

        init(group: Group, seed: Data) {
            self.v = 1
            self.name = group.name
            self.seed = seed.base64URLEncodedString()
            self.interval = group.interval.rawValue
        }
    }

    // MARK: - Generation

    /// Generate a QR code UIImage from a group and its seed.
    static func generateQRCode(for group: Group, seed: Data, size: CGFloat = 250) -> UIImage? {
        let payload = QRPayload(group: group, seed: seed)

        guard let jsonData = try? JSONEncoder().encode(payload),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            return nil
        }

        // Encode the JSON as a QR code
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(jsonString.utf8)
        filter.correctionLevel = "M"

        guard let outputImage = filter.outputImage else { return nil }

        // Scale the QR code to the desired size
        let scaleX = size / outputImage.extent.size.width
        let scaleY = size / outputImage.extent.size.height
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))

        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }

        return UIImage(cgImage: cgImage)
    }

    // MARK: - Parsing

    /// Parse a scanned QR code string into group information.
    static func parseQRCode(_ string: String) -> ParsedGroup? {
        guard let data = string.data(using: .utf8),
              let payload = try? JSONDecoder().decode(QRPayload.self, from: data) else {
            return nil
        }

        guard payload.v == 1 else { return nil }

        guard let seed = Data(base64URLEncoded: payload.seed),
              seed.count == 32 else {
            return nil
        }

        guard let interval = RotationInterval(rawValue: payload.interval) else {
            return nil
        }

        return ParsedGroup(name: payload.name, seed: seed, interval: interval)
    }

    /// Parsed group data from a QR code.
    struct ParsedGroup {
        let name: String
        let seed: Data
        let interval: RotationInterval
    }
}

// MARK: - Base64URL Extensions

extension Data {
    /// Encode data as base64url (no padding).
    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    /// Decode base64url-encoded string (no padding) into Data.
    init?(base64URLEncoded string: String) {
        var base64 = string
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")

        // Add padding if necessary
        let remainder = base64.count % 4
        if remainder > 0 {
            base64 += String(repeating: "=", count: 4 - remainder)
        }

        self.init(base64Encoded: base64)
    }
}
