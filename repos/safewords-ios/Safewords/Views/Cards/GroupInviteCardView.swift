import SwiftUI

struct GroupInviteCardView: View {
    let group: Group
    let seed: Data

    private var template: SafetyCardTemplate {
        SafetyCardCopy.template("groupInvite")
    }

    var body: some View {
        CardShell(
            title: template.title.replacingGroupName(group.name),
            subtitle: template.subtitle?.replacingGroupName(group.name)
        ) {
            VStack(alignment: .leading, spacing: 20) {
                WarningBlock(
                    heading: template.warningHeading?.replacingGroupName(group.name),
                    body: template.warningBody?.replacingGroupName(group.name)
                )

                if let qr = QRCodeService.generateQRCode(for: group, seed: seed, size: 300) {
                    Image(uiImage: qr)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 300, height: 300)
                        .padding(22)
                        .frame(maxWidth: .infinity)
                        .background(RoundedRectangle(cornerRadius: 20).fill(Color.black.opacity(0.04)))
                }

                Text(template.footer ?? "")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.black.opacity(0.62))
            }
        }
    }
}
