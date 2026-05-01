import SwiftUI

struct RecoveryPhraseCardView: View {
    let group: Group
    let phrase: String

    private var template: SafetyCardTemplate {
        SafetyCardCopy.template("recoveryPhrase")
    }

    private var words: [String] {
        phrase.split(separator: " ").map(String.init)
    }

    var body: some View {
        CardShell(
            title: template.title.replacingGroupName(group.name),
            subtitle: template.subtitle?.replacingGroupName(group.name)
        ) {
            VStack(alignment: .leading, spacing: 18) {
                WarningBlock(
                    heading: template.warningHeading?.replacingGroupName(group.name),
                    message: template.warningBody?.replacingGroupName(group.name)
                )

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                    ForEach(Array(words.enumerated()), id: \.offset) { index, word in
                        VStack(alignment: .leading, spacing: 3) {
                            Text("\(index + 1)")
                                .font(.system(size: 8, weight: .bold, design: .monospaced))
                                .foregroundStyle(Color.black.opacity(0.45))
                            Text(word)
                                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                                .foregroundStyle(Color.black)
                                .lineLimit(1)
                                .minimumScaleFactor(0.75)
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(RoundedRectangle(cornerRadius: 8).fill(Color.black.opacity(0.05)))
                    }
                }

                Text(template.footer ?? "")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.black.opacity(0.6))
            }
        }
    }
}
