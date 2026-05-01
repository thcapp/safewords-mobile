import SwiftUI

struct OnboardingView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen
    @AppStorage("onboarded") private var onboarded: Bool = false

    private enum Flow {
        case welcome, start, create
    }

    @State private var flow: Flow = .welcome
    @State private var groupName = "Family"
    @State private var creatorName = ""
    @State private var pendingSeed: Data?
    @State private var pendingRecoveryCode: String?
    @State private var showingRawSeedBackup = false
    @State private var errorMessage: String?

    var body: some View {
        ZStack(alignment: .top) {
            Ink.bg.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                progress
                    .padding(.bottom, 32)

                SwiftUI.Group {
                    switch flow {
                    case .welcome: panelWelcome
                    case .start: panelStart
                    case .create: panelCreate
                    }
                }

                Spacer(minLength: 0)
                footer
            }
            .padding(.horizontal, 28)
            .padding(.top, 70)
            .padding(.bottom, 40)
        }
        .onAppear {
            if !groupStore.groups.isEmpty, flow == .welcome {
                flow = .start
            }
        }
    }

    private var progress: some View {
        HStack(spacing: 6) {
            ForEach(0..<3, id: \.self) { i in
                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(i <= progressIndex ? Ink.accent : Ink.rule)
                    .frame(height: 3)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var progressIndex: Int {
        switch flow {
        case .welcome: return 0
        case .start: return 1
        case .create: return pendingSeed == nil ? 1 : 2
        }
    }

    private var footer: some View {
        HStack(spacing: 10) {
            if flow != .welcome {
                Button { goBack() } label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Ink.fgMuted)
                        .frame(width: 56, height: 48)
                        .background(Capsule().stroke(Ink.rule, lineWidth: 0.5))
                }
                .buttonStyle(.plain)
            }

            Button(action: primaryAction) {
                HStack(spacing: 8) {
                    Text(primaryLabel).font(Fonts.body(15, weight: .semibold)).tracking(-0.1)
                    Image(systemName: "arrow.right").font(.system(size: 13, weight: .semibold))
                }
                .foregroundStyle(primaryDisabled ? Ink.fgMuted : Ink.accentInk)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(Capsule().fill(primaryDisabled ? Ink.bgInset : Ink.accent))
            }
            .buttonStyle(.plain)
            .disabled(primaryDisabled)
        }
    }

    private var primaryLabel: String {
        switch flow {
        case .welcome: return "Get started"
        case .start: return "Create a new group"
        case .create: return pendingSeed == nil ? "Generate recovery phrase" : "Create group"
        }
    }

    private var primaryDisabled: Bool {
        switch flow {
        case .create:
            return groupName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                creatorName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        default:
            return false
        }
    }

    private func primaryAction() {
        switch flow {
        case .welcome:
            flow = .start
        case .start:
            flow = .create
        case .create:
            if pendingSeed == nil {
                generateRecoverySeed()
            } else {
                createGroup()
            }
        }
    }

    private func goBack() {
        errorMessage = nil
        switch flow {
        case .welcome:
            break
        case .start:
            screen = groupStore.groups.isEmpty ? .home : .groups
        case .create:
            if pendingSeed != nil {
                pendingSeed = nil
                pendingRecoveryCode = nil
                showingRawSeedBackup = false
            } else {
                flow = .start
            }
        }
    }

    private func generateRecoverySeed() {
        let seed = TOTPDerivation.generateSeed()
        pendingSeed = seed
        do {
            pendingRecoveryCode = try RecoveryPhrase.encode(seed: seed)
            showingRawSeedBackup = false
            errorMessage = nil
        } catch {
            pendingRecoveryCode = RecoveryPhraseService.seedHex(seed)
            showingRawSeedBackup = true
            errorMessage = "Couldn't load the recovery word list. Back up this raw seed instead; it restores the same group."
        }
    }

    private func createGroup() {
        guard let seed = pendingSeed else { return }
        let groupName = groupName.trimmingCharacters(in: .whitespacesAndNewlines)
        let creatorName = creatorName.trimmingCharacters(in: .whitespacesAndNewlines)
        if groupStore.createGroup(name: groupName, interval: .daily, creatorName: creatorName, seed: seed) != nil {
            onboarded = true
            screen = .home
        } else {
            errorMessage = "Could not save the group seed. Check keychain access and try again."
        }
    }

    private var panelWelcome: some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionLabel(text: "Safewords · 01")

            (Text("One word between\n") + Text("trust").foregroundColor(Ink.accent) + Text(" and deception."))
                .font(Fonts.display(42))
                .tracking(-1.4)
                .lineSpacing(2)
                .foregroundStyle(Ink.fg)
                .padding(.top, 28)

            Text("AI can clone any voice in 3 seconds. Your safeword verifies the people who matter — no server, no account, no data collected.")
                .font(Fonts.body(16))
                .foregroundStyle(Ink.fgMuted)
                .lineSpacing(4)
                .frame(maxWidth: 320, alignment: .leading)
                .padding(.top, 20)

            VStack(spacing: 6) {
                Text("crimson eagle 47").font(Fonts.mono(14)).foregroundStyle(Ink.fgFaint)
                Text("silent river 12").font(Fonts.mono(14)).foregroundStyle(Ink.fgMuted)
                Text("violet anchor 88")
                    .font(Fonts.body(22, weight: .medium))
                    .foregroundStyle(Ink.accent)
                    .padding(.horizontal, 12).padding(.vertical, 4)
                    .background(RoundedRectangle(cornerRadius: 6).fill(Ink.tickFill))
                Text("bronze kite 34").font(Fonts.mono(14)).foregroundStyle(Ink.fgMuted)
                Text("silver fox 55").font(Fonts.mono(14)).foregroundStyle(Ink.fgFaint)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .padding(.horizontal, 24)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Ink.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
            )
            .padding(.top, 40)
        }
    }

    private var panelStart: some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionLabel(text: "Start · 02")

            Text("Start a group, or\njoin someone else's.")
                .font(Fonts.display(36))
                .tracking(-1.2)
                .foregroundStyle(Ink.fg)
                .padding(.top, 28)

            VStack(spacing: 12) {
                onboardOption(
                    title: "Create a new group",
                    sub: "Generate a private seed and back it up.",
                    icon: "plus",
                    primary: true
                ) { flow = .create }
                onboardOption(
                    title: "Join with a QR code",
                    sub: "Scan a QR shared by a group member.",
                    icon: "qrcode"
                ) { screen = .qrScanner }
                onboardOption(
                    title: "Join with a recovery phrase",
                    sub: "Restore an existing seed from backup.",
                    icon: "arrow.triangle.2.circlepath"
                ) { screen = .recoveryPhrase }
                onboardOption(
                    title: "Look around first",
                    sub: "Use a clearly marked demo group. Set up your real group later.",
                    icon: "eye"
                ) {
                    groupStore.enterDemoMode()
                    onboarded = true
                    screen = .home
                }
            }
            .padding(.top, 28)

            Spacer()

            Text("Everything stays on your device.\nNo accounts. No data collection.")
                .font(Fonts.body(11.5))
                .foregroundStyle(Ink.fgFaint)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .lineSpacing(4)
                .padding(.top, 24)
        }
    }

    private var panelCreate: some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionLabel(text: pendingSeed == nil ? "Create · 02" : "Seed · 03")

            Text(pendingSeed == nil ? "Name your group." : (showingRawSeedBackup ? "Back up this raw seed." : "Back up this recovery phrase."))
                .font(Fonts.display(36))
                .tracking(-1.2)
                .foregroundStyle(Ink.fg)
                .padding(.top, 28)

            if pendingSeed == nil {
                Text("This creates a real 256-bit seed on this device. Write down the recovery phrase before you finish.")
                    .font(Fonts.body(15))
                    .foregroundStyle(Ink.fgMuted)
                    .lineSpacing(4)
                    .padding(.top, 10)

                VStack(spacing: 16) {
                    formField("Group name", text: $groupName, prompt: "Family")
                    formField("Your name", text: $creatorName, prompt: "Alex")
                }
                .padding(18)
                .background(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(Ink.bgElev)
                        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
                )
                .padding(.top, 24)
            } else if pendingSeed != nil {
                Text(showingRawSeedBackup
                    ? "Anyone with this 64-character seed can join your group. Keep it offline."
                    : "Anyone with these 24 words can join your group. Keep them offline.")
                    .font(Fonts.body(15))
                    .foregroundStyle(Ink.fgMuted)
                    .lineSpacing(4)
                    .padding(.top, 10)

                recoveryCodeCard(code: pendingRecoveryCode ?? "", rawSeed: showingRawSeedBackup)
                    .padding(.top, 24)
            }

            if let errorMessage {
                Text(errorMessage)
                    .font(Fonts.body(12.5))
                    .foregroundStyle(Ink.warn)
                    .lineSpacing(3)
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Ink.tickFill))
                    .padding(.top, 16)
            }
        }
    }

    private func formField(_ label: String, text: Binding<String>, prompt: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(Fonts.body(12, weight: .medium))
                .foregroundStyle(Ink.fgMuted)
            TextField("", text: text, prompt: Text(prompt).foregroundColor(Ink.fgFaint))
                .font(Fonts.body(18, weight: .medium))
                .foregroundStyle(Ink.fg)
                .tint(Ink.accent)
            Rectangle().fill(Ink.rule).frame(height: 1)
        }
    }

    private func recoveryCodeCard(code: String, rawSeed: Bool) -> some View {
        let words = rawSeed
            ? stride(from: 0, to: code.count, by: 8).map { offset -> String in
                let start = code.index(code.startIndex, offsetBy: offset)
                let end = code.index(start, offsetBy: 8, limitedBy: code.endIndex) ?? code.endIndex
                return String(code[start..<end])
            }
            : code.split(separator: " ").map(String.init)
        return VStack(alignment: .leading, spacing: 14) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 2), spacing: 10) {
                ForEach(Array(words.enumerated()), id: \.offset) { index, word in
                    HStack(spacing: 8) {
                        Text(String(format: "%02d", index + 1))
                            .font(Fonts.mono(10))
                            .foregroundStyle(Ink.fgFaint)
                            .frame(width: 18, alignment: .leading)
                        Text(word)
                            .font(Fonts.mono(13, weight: .medium))
                            .foregroundStyle(Ink.fg)
                        Spacer(minLength: 0)
                    }
                }
            }
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 14))
                    .foregroundStyle(Ink.accent)
                Text(rawSeed
                    ? "Write every block down before tapping Create group. This raw seed restores the same group."
                    : "Write this down before tapping Create group. It restores this group's seed if you lose your phone.")
                    .font(Fonts.body(12.5))
                    .foregroundStyle(Ink.accent)
                    .lineSpacing(3)
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 14).fill(Ink.tickFill))
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
        )
    }

    private func onboardOption(
        title: String,
        sub: String,
        icon: String,
        primary: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(primary ? Color.black.opacity(0.12) : Ink.bgInset)
                        .frame(width: 40, height: 40)
                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(primary ? Ink.accentInk : Ink.fg)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(Fonts.body(15, weight: .semibold)).tracking(-0.2)
                        .foregroundStyle(primary ? Ink.accentInk : Ink.fg)
                    Text(sub).font(Fonts.body(12.5))
                        .foregroundStyle(primary ? Ink.accentInk.opacity(0.7) : Ink.fgMuted)
                        .lineSpacing(2)
                }
                Spacer()
                Image(systemName: "arrow.right")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle((primary ? Ink.accentInk : Ink.fg).opacity(0.5))
            }
            .padding(18)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(primary ? Ink.accent : Ink.bgElev)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(primary ? Color.clear : Ink.rule, lineWidth: 0.5)
                    )
            )
        }
        .buttonStyle(.plain)
    }
}
