import SwiftUI
import MessageUI

struct QRDisplayView: View {
    @Environment(GroupStore.self) private var groupStore
    @Environment(\.openURL) private var openURL
    @Binding var screen: AppScreen
    @State private var secondsRemaining = 60
    @State private var smsBody = ""
    @State private var showSMSComposer = false

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            VStack(spacing: 0) {
                header.padding(.horizontal, 20).padding(.top, 62)

                Spacer(minLength: 20)

                if let group = groupStore.selectedGroup {
                    content(group: group)
                } else {
                    Text("Select a group first.")
                        .font(Fonts.body(15))
                        .foregroundStyle(Ink.fgMuted)
                }

                Spacer()
                altActions.padding(.horizontal, 16).padding(.bottom, 120)
            }
        }
        .task { await countdownAndDismiss() }
        .sheet(isPresented: $showSMSComposer) {
            MessageComposeView(body: smsBody) {
                showSMSComposer = false
            }
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { screen = .groups } label: {
                Image(systemName: "arrow.left")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Ink.fg)
                    .frame(width: 38, height: 38)
                    .background(
                        Circle().fill(Ink.bgElev)
                            .overlay(Circle().stroke(Ink.rule, lineWidth: 0.5))
                    )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("qr-display.done")
            VStack(alignment: .leading, spacing: 2) {
                SectionLabel(text: "Invite · \(groupStore.selectedGroup?.name ?? "")")
                Text("Share in person")
                    .font(Fonts.display(22))
                    .tracking(-0.6)
                    .foregroundStyle(Ink.fg)
            }
            Spacer()
        }
    }

    private func content(group: Group) -> some View {
        VStack(spacing: 20) {
            ZStack {
                if let seed = groupStore.seed(for: group.id),
                   let img = QRCodeService.generateQRCode(for: group, seed: seed, size: 240) {
                    Image(uiImage: img)
                        .interpolation(.none)
                        .resizable()
                        .frame(width: 240, height: 240)
                        .foregroundStyle(Ink.fg)
                        .colorInvert()
                        .background(Ink.bgElev)
                } else {
                    RoundedRectangle(cornerRadius: 12).fill(Ink.bgInset).frame(width: 240, height: 240)
                }

                Circle().fill(Ink.bgElev).frame(width: 28, height: 28)
                Circle().fill(Ink.accent).frame(width: 10, height: 10)
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(Ink.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 28).stroke(Ink.rule, lineWidth: 0.5))
                    .shadow(color: .black.opacity(0.3), radius: 20, y: 20)
            )
            .accessibilityIdentifier("qr-display.qr")

            VStack(spacing: 12) {
                (Text("Have them open Safewords, tap ")
                    + Text("Join with QR").foregroundColor(Ink.accent)
                    + Text(", and scan this."))
                    .font(Fonts.body(14.5, weight: .medium))
                    .foregroundStyle(Ink.fg)
                    .multilineTextAlignment(.center)
                Text("Only share this in person. Anyone who scans it joins your group permanently.")
                    .font(Fonts.body(12.5))
                    .foregroundStyle(Ink.fgMuted)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }
            .frame(maxWidth: 280)

            HStack(spacing: 6) {
                Image(systemName: "lock")
                    .font(.system(size: 11)).foregroundStyle(Ink.fgFaint)
                Text("256-BIT · ROTATING · OFFLINE · \(secondsRemaining)S")
                    .font(Fonts.mono(11))
                    .tracking(1)
                    .foregroundStyle(Ink.fgFaint)
            }
            .padding(.top, 4)
        }
    }

    private var altActions: some View {
        Button { inviteViaSMS() } label: {
            HStack(spacing: 12) {
                Image(systemName: "message")
                    .font(.system(size: 17)).foregroundStyle(Ink.fgMuted)
                VStack(alignment: .leading, spacing: 1) {
                    Text("Invite via SMS instead")
                        .font(Fonts.body(14, weight: .medium)).foregroundStyle(Ink.fg)
                    Text("For family without the app — they get the rotating word by text.")
                        .font(Fonts.body(11.5)).foregroundStyle(Ink.fgMuted)
                        .lineSpacing(2)
                }
                Spacer()
                Image(systemName: "arrow.right")
                    .font(.system(size: 13)).foregroundStyle(Ink.fgFaint)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Ink.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Ink.rule, lineWidth: 0.5))
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("qr-display.sms-cta")
    }

    private func inviteViaSMS() {
        guard let group = groupStore.selectedGroup else { return }
        let body = SmsInviteService.inviteText(group: group, seed: groupStore.seed(for: group.id))
        if MFMessageComposeViewController.canSendText() {
            smsBody = body
            showSMSComposer = true
        } else if let url = SmsInviteService.fallbackURL(body: body) {
            openURL(url)
        }
    }

    private func countdownAndDismiss() async {
        secondsRemaining = 60
        while secondsRemaining > 0 {
            try? await Task.sleep(nanoseconds: 1_000_000_000)
            if Task.isCancelled { return }
            secondsRemaining -= 1
        }
        if screen == .addMember {
            screen = .groups
        }
    }
}
