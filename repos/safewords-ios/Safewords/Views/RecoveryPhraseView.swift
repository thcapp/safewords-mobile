import SwiftUI

struct RecoveryPhraseView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen
    @AppStorage("onboarded") private var onboarded: Bool = false

    @State private var groupName = "Recovered group"
    @State private var memberName = ""
    @State private var recoveryInput = ""
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.horizontal, 20)
                        .padding(.top, 62)

                    VStack(alignment: .leading, spacing: 16) {
                        field("Group name", text: $groupName, prompt: "Family", identifier: "recovery-phrase.group-name")
                        field("Your name", text: $memberName, prompt: "Alex", identifier: "recovery-phrase.member-name")

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Recovery phrase or seed")
                                .font(Fonts.body(12, weight: .medium))
                                .foregroundStyle(Ink.fgMuted)
                            TextEditor(text: $recoveryInput)
                                .font(Fonts.mono(14))
                                .foregroundStyle(Ink.fg)
                                .scrollContentBackground(.hidden)
                                .frame(minHeight: 150)
                                .padding(12)
                                .background(RoundedRectangle(cornerRadius: 14).fill(Ink.bgInset))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Ink.rule, lineWidth: 0.5))
                                .accessibilityIdentifier("recovery-phrase.input")
                        }

                        Text("Paste a 24-word recovery phrase or a 64-character hex seed, with or without spaces. The group name and your name are local labels.")
                            .font(Fonts.body(12.5))
                            .foregroundStyle(Ink.fgMuted)
                            .lineSpacing(3)

                        if let errorMessage {
                            Text(errorMessage)
                                .font(Fonts.body(12.5))
                                .foregroundStyle(Ink.warn)
                                .lineSpacing(3)
                                .padding(14)
                                .background(RoundedRectangle(cornerRadius: 14).fill(Ink.tickFill))
                                .accessibilityIdentifier("recovery-phrase.error")
                        }

                        Button(action: join) {
                            Text("Join group")
                                .font(Fonts.body(15, weight: .semibold))
                                .foregroundStyle(joinDisabled ? Ink.fgMuted : Ink.accentInk)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Capsule().fill(joinDisabled ? Ink.bgInset : Ink.accent))
                        }
                        .buttonStyle(.plain)
                        .disabled(joinDisabled)
                        .accessibilityIdentifier("recovery-phrase.submit")
                    }
                    .padding(18)
                    .background(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .fill(Ink.bgElev)
                            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Ink.rule, lineWidth: 0.5))
                    )
                    .padding(.horizontal, 16)
                    .padding(.top, 28)
                    .padding(.bottom, 80)
                }
            }
            .scrollIndicators(.hidden)
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { screen = groupStore.groups.isEmpty ? .onboarding : .groups } label: {
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
            VStack(alignment: .leading, spacing: 2) {
                SectionLabel(text: "Recovery")
                Text("Restore a group")
                    .font(Fonts.display(24))
                    .tracking(-0.6)
                    .foregroundStyle(Ink.fg)
            }
            Spacer()
        }
    }

    private var joinDisabled: Bool {
        groupName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            memberName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            recoveryInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func field(_ label: String, text: Binding<String>, prompt: String, identifier: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(Fonts.body(12, weight: .medium))
                .foregroundStyle(Ink.fgMuted)
            TextField("", text: text, prompt: Text(prompt).foregroundColor(Ink.fgFaint))
                .font(Fonts.body(17, weight: .medium))
                .foregroundStyle(Ink.fg)
                .tint(Ink.accent)
                .accessibilityIdentifier(identifier)
            Rectangle().fill(Ink.rule).frame(height: 1)
        }
    }

    private func join() {
        do {
            let seed = try RecoveryPhraseService.parseSeed(from: recoveryInput)
            let name = groupName.trimmingCharacters(in: .whitespacesAndNewlines)
            let member = memberName.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let group = groupStore.joinGroup(name: name, seed: seed, interval: .daily, memberName: member) else {
                errorMessage = "Could not save this seed. Check keychain access and try again."
                return
            }
            groupStore.selectedGroupID = group.id
            onboarded = true
            screen = .home
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? "Invalid recovery phrase."
        }
    }
}
