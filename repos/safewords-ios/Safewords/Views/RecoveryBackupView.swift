import LocalAuthentication
import SwiftUI
import UIKit

struct RecoveryBackupView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen

    @State private var unlocked = false
    @State private var phrase: String?
    @State private var errorMessage: String?
    @State private var copied = false

    private var activeGroup: Group? {
        groupStore.selectedGroup
    }

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.horizontal, 20)
                        .padding(.top, 62)

                    Text("Write these 24 words down on paper and keep them somewhere only you can reach.")
                        .font(Fonts.body(14.5))
                        .foregroundStyle(Ink.fgMuted)
                        .lineSpacing(4)
                        .padding(.horizontal, 24)
                        .padding(.top, 20)

                    warningCard
                        .padding(.horizontal, 16)
                        .padding(.top, 20)

                    content
                        .padding(.horizontal, 16)
                        .padding(.top, 20)
                        .padding(.bottom, 90)
                }
            }
            .scrollIndicators(.hidden)
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { screen = .settings } label: {
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
            .accessibilityIdentifier("recovery-backup.back")

            VStack(alignment: .leading, spacing: 2) {
                SectionLabel(text: "Back up seed phrase")
                Text(activeGroup.map { "For \($0.name)" } ?? "No active group")
                    .font(Fonts.display(24))
                    .tracking(-0.6)
                    .foregroundStyle(Ink.fg)
            }

            Spacer()
        }
    }

    @ViewBuilder
    private var content: some View {
        if let errorMessage {
            messageCard(errorMessage, icon: "exclamationmark.triangle", accent: true)
        }

        if activeGroup == nil {
            messageCard("Create or join a group before backing up a seed phrase.", icon: "person.2.slash", accent: false)
        } else if !unlocked {
            unlockCard
        } else if let phrase {
            phraseCard(phrase)
            copyButton(phrase)
                .padding(.top, 14)
            Text("Backup format: BIP39 English, 24 words.")
                .font(Fonts.body(11.5))
                .foregroundStyle(Ink.fgFaint)
                .padding(.top, 12)
        } else {
            messageCard("Loading recovery phrase...", icon: "hourglass", accent: false)
        }
    }

    private var warningCard: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Ink.accent)
            Text("Anyone with these words can restore this group and generate the same safewords. Do not screenshot, email, or store them in cloud notes.")
                .font(Fonts.body(12.5))
                .foregroundStyle(Ink.fg)
                .lineSpacing(3)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Ink.tickFill)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Ink.accent.opacity(0.35), lineWidth: 0.5))
        )
    }

    private var unlockCard: some View {
        Button {
            unlock()
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "lock.open")
                    .font(.system(size: 15, weight: .semibold))
                Text("Unlock to reveal")
                    .font(Fonts.body(15, weight: .semibold))
            }
            .foregroundStyle(Ink.accentInk)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(Capsule().fill(Ink.accent))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("recovery-backup.unlock")
    }

    private func phraseCard(_ phrase: String) -> some View {
        let words = phrase.split(separator: " ").map(String.init)
        return LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4),
            spacing: 8
        ) {
            ForEach(Array(words.enumerated()), id: \.offset) { index, word in
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(index + 1)")
                        .font(Fonts.mono(9))
                        .foregroundStyle(Ink.fgFaint)
                    Text(word)
                        .font(Fonts.mono(11.5, weight: .medium))
                        .foregroundStyle(Ink.fg)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8)
                .padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Ink.bgInset))
                .accessibilityIdentifier(String(format: "recovery-backup.word.%02d", index + 1))
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Ink.rule, lineWidth: 0.5))
        )
    }

    private func copyButton(_ phrase: String) -> some View {
        Button {
            UIPasteboard.general.string = phrase
            copied = true
        } label: {
            HStack(spacing: 10) {
                Image(systemName: copied ? "checkmark" : "doc.on.doc")
                    .font(.system(size: 14, weight: .medium))
                Text(copied ? "Copied" : "Copy to clipboard")
                    .font(Fonts.body(14, weight: .semibold))
            }
            .foregroundStyle(Ink.fg)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Capsule().stroke(Ink.rule, lineWidth: 0.5))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("recovery-backup.copy")
    }

    private func messageCard(_ message: String, icon: String, accent: Bool) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(accent ? Ink.accent : Ink.fgMuted)
            Text(message)
                .font(Fonts.body(13))
                .foregroundStyle(accent ? Ink.accent : Ink.fgMuted)
                .lineSpacing(3)
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Ink.bgElev))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Ink.rule, lineWidth: 0.5))
    }

    private func unlock() {
        let context = LAContext()
        var authError: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &authError) else {
            errorMessage = "Set up Face ID, Touch ID, or a device passcode before showing recovery words."
            return
        }

        Task {
            do {
                let ok = try await context.evaluatePolicy(
                    .deviceOwnerAuthentication,
                    localizedReason: "Show Safewords recovery phrase"
                )
                await MainActor.run {
                    if ok {
                        revealPhrase()
                    } else {
                        errorMessage = "Couldn't unlock. Try again."
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Couldn't unlock. Try again."
                }
            }
        }
    }

    private func revealPhrase() {
        guard let activeGroup else {
            errorMessage = "Create or join a group before backing up a seed phrase."
            return
        }
        guard let seed = groupStore.seed(for: activeGroup.id) else {
            errorMessage = "Couldn't read the seed for this group."
            return
        }

        do {
            phrase = try RecoveryPhrase.encode(seed: seed)
            unlocked = true
            errorMessage = nil
        } catch {
            errorMessage = "Couldn't generate the recovery phrase."
        }
    }
}
