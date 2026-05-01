import SwiftUI

struct ChallengeAnswerCardView: View {
    enum Variant {
        case wallet
        case protocolFull

        var templateID: String {
            switch self {
            case .wallet:
                return "challengeAnswerWallet"
            case .protocolFull:
                return "challengeAnswerProtocol"
            }
        }

        var rowCount: Int {
            switch self {
            case .wallet:
                return 24
            case .protocolFull:
                return 100
            }
        }
    }

    let group: Group
    let seed: Data
    let variant: Variant

    private var template: SafetyCardTemplate {
        SafetyCardCopy.template(variant.templateID)
    }

    private var rows: [ChallengeAnswerRow] {
        Primitives.challengeAnswerRows(
            seed: seed,
            tableVersion: group.primitives.challengeAnswer.tableVersion,
            count: variant.rowCount
        )
    }

    var body: some View {
        CardShell(
            title: template.title.replacingGroupName(group.name),
            subtitle: template.subtitle?.replacingGroupName(group.name)
        ) {
            VStack(alignment: .leading, spacing: 12) {
                WarningBlock(
                    heading: template.warningHeading?.replacingGroupName(group.name),
                    body: template.warningBody?.replacingGroupName(group.name)
                )

                VStack(spacing: 0) {
                    header
                    ForEach(rows) { row in
                        HStack(alignment: .top, spacing: 8) {
                            Text("\(row.rowIndex + 1)")
                                .frame(width: 26, alignment: .leading)
                            Text(row.ask.phrase)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(row.expect.phrase)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .font(.system(size: variant == .wallet ? 10.5 : 7.8, weight: .medium, design: .monospaced))
                        .foregroundStyle(Color.black)
                        .padding(.vertical, variant == .wallet ? 4 : 1.5)
                        if row.rowIndex < rows.count - 1 {
                            Rectangle().fill(Color.black.opacity(0.1)).frame(height: 0.5)
                        }
                    }
                }
            }
        }
    }

    private var header: some View {
        let headers = template.columnHeaders ?? ["#", "I ask", "They answer"]
        return HStack(spacing: 8) {
            Text(headers[safe: 0] ?? "#").frame(width: 26, alignment: .leading)
            Text(headers[safe: 1] ?? "I ask").frame(maxWidth: .infinity, alignment: .leading)
            Text(headers[safe: 2] ?? "They answer").frame(maxWidth: .infinity, alignment: .leading)
        }
        .font(.system(size: 10, weight: .bold, design: .monospaced))
        .foregroundStyle(Color.black.opacity(0.65))
        .padding(.bottom, 6)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
