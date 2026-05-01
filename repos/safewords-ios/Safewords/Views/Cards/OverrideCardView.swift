import SwiftUI

struct OverrideCardView: View {
    let group: Group
    let seed: Data

    private var template: SafetyCardTemplate {
        SafetyCardCopy.template("staticOverride")
    }

    private var phrase: String {
        Primitives.staticOverride(seed: seed).phrase
    }

    var body: some View {
        CardShell(
            title: template.title,
            subtitle: template.subtitle?.replacingGroupName(group.name)
        ) {
            VStack(alignment: .leading, spacing: 22) {
                Text(phrase)
                    .font(.system(size: 42, weight: .heavy, design: .serif))
                    .foregroundStyle(Color.black)
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: 18).fill(Color.black.opacity(0.06)))

                WarningBlock(
                    heading: template.warningHeading?.replacingGroupName(group.name),
                    message: template.warningBody?.replacingGroupName(group.name)
                )

                Text(template.footer ?? "")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.black.opacity(0.6))
            }
        }
    }
}

struct WarningBlock: View {
    let heading: String?
    let message: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(heading ?? "Keep this private.")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color.black)
            Text(message ?? "")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Color.black.opacity(0.68))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).stroke(Color.black.opacity(0.22), lineWidth: 1))
    }
}
