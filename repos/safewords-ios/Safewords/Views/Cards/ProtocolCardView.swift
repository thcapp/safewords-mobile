import SwiftUI

struct SafetyCardTemplate: Decodable {
    let sensitivity: String?
    let biometricGate: Bool?
    let title: String
    let subtitle: String?
    let rules: [String]?
    let footer: String?
    let qrPayload: String?
    let qrCaption: String?
    let warningHeading: String?
    let warningBody: String?
    let columnHeaders: [String]?
}

enum SafetyCardCopy {
    static func template(_ id: String) -> SafetyCardTemplate {
        shared.cards[id] ?? SafetyCardTemplate(
            sensitivity: nil,
            biometricGate: nil,
            title: "Safewords",
            subtitle: nil,
            rules: nil,
            footer: nil,
            qrPayload: nil,
            qrCaption: nil,
            warningHeading: nil,
            warningBody: nil,
            columnHeaders: nil
        )
    }

    private static let shared: SafetyCardCopyFile = {
        guard let url = Bundle.main.url(forResource: "safety-card-copy", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode(SafetyCardCopyFile.self, from: data) else {
            return SafetyCardCopyFile(cards: [:])
        }
        return decoded
    }()
}

private struct SafetyCardCopyFile: Decodable {
    let cards: [String: SafetyCardTemplate]
}

struct CardShell<Content: View>: View {
    let title: String
    let subtitle: String?
    let content: Content

    init(title: String, subtitle: String?, @ViewBuilder content: () -> Content) {
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.system(size: 30, weight: .bold, design: .serif))
                    .foregroundStyle(Color.black)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.black.opacity(0.62))
                }
            }
            Rectangle().fill(Color.black.opacity(0.18)).frame(height: 1)
            content
            Spacer()
        }
        .padding(42)
        .background(Color.white)
    }
}

struct ProtocolCardView: View {
    private let template = SafetyCardCopy.template("protocol")

    var body: some View {
        CardShell(title: template.title, subtitle: template.subtitle) {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(Array((template.rules ?? []).enumerated()), id: \.offset) { index, rule in
                    HStack(alignment: .top, spacing: 12) {
                        Text("\(index + 1)")
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundStyle(Color.white)
                            .frame(width: 28, height: 28)
                            .background(Circle().fill(Color.black))
                        Text(rule)
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(Color.black)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                Spacer(minLength: 18)

                if let payload = template.qrPayload,
                   let qr = QRCodeService.generateQRCode(payload: payload, size: 150) {
                    HStack(spacing: 18) {
                        Image(uiImage: qr)
                            .interpolation(.none)
                            .resizable()
                            .frame(width: 150, height: 150)
                        VStack(alignment: .leading, spacing: 6) {
                            Text(template.qrCaption ?? "Get the app")
                                .font(.system(size: 18, weight: .bold))
                            Text(template.footer ?? "")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(Color.black.opacity(0.58))
                        }
                    }
                }
            }
        }
    }
}

extension String {
    func replacingGroupName(_ groupName: String) -> String {
        replacingOccurrences(of: "{groupName}", with: groupName)
    }
}
