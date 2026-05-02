import SwiftUI

struct VerifyView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen

    enum Phase { case ready, listening, match, mismatch }

    @State private var phase: Phase = .ready
    @State private var typed: String = ""
    @State private var pulse: Bool = false
    @State private var showChallenge = false

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                header.padding(.horizontal, 20).padding(.top, 62)

                SwiftUI.Group {
                    if groupStore.selectedGroup == nil {
                        noGroupPanel
                    } else if let group = groupStore.selectedGroup, !groupStore.verifyNeeded(for: group) {
                        verifyNotNeededPanel(group: group)
                    } else {
                        switch phase {
                        case .ready: readyPanel
                        case .listening: listeningPanel
                        case .match: ResultCard(match: true, onBack: reset)
                            .accessibilityIdentifier("verify.result-match")
                        case .mismatch: ResultCard(match: false, onBack: reset)
                            .accessibilityIdentifier("verify.result-mismatch")
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 30)
                .padding(.bottom, 140)
                .frame(maxHeight: .infinity, alignment: .top)
            }
        }
        .sheet(isPresented: $showChallenge) {
            if let group = groupStore.selectedGroup,
               let seed = groupStore.seed(for: group.id) {
                ChallengeSheet(group: group, seed: seed)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionLabel(text: "Verify")
            Text("Are they who\nthey say they are?")
                .font(Fonts.display(30))
                .tracking(-1.0)
                .lineSpacing(1)
                .foregroundStyle(Ink.fg)
            Text("Ask them for today's \(groupStore.selectedGroup?.name ?? "group") word. Don't read it to them.")
                .font(Fonts.body(14))
                .foregroundStyle(Ink.fgMuted)
                .lineSpacing(3)
        }
    }

    private var readyPanel: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let group = groupStore.selectedGroup,
               group.primitives.challengeAnswer.enabled {
                Button {
                    showChallenge = true
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "questionmark.bubble")
                            .font(.system(size: 16, weight: .semibold))
                        Text("Challenge someone")
                            .font(Fonts.body(15, weight: .semibold))
                        Spacer()
                        Image(systemName: "arrow.right")
                            .font(.system(size: 12, weight: .medium))
                    }
                    .foregroundStyle(Ink.accentInk)
                    .padding(16)
                    .background(RoundedRectangle(cornerRadius: 18).fill(Ink.accent))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("verify.challenge-cta")
                .padding(.bottom, 18)
            }

            VStack(alignment: .leading, spacing: 14) {
                SectionLabel(text: "Their answer")
                TextField("", text: $typed, prompt: Text("type what they said").foregroundColor(Ink.fgMuted))
                    .font(Fonts.mono(22))
                    .foregroundStyle(Ink.fg)
                    .tint(Ink.accent)
                    .padding(.top, -4)
                    .accessibilityIdentifier("verify.text-input")
                Rectangle().fill(Ink.rule).frame(height: 1).padding(.horizontal, -20)
                HStack(spacing: 8) {
                    Button { check() } label: {
                        Text("Check")
                            .font(Fonts.body(14.5, weight: .semibold))
                            .foregroundStyle(typed.isEmpty ? Ink.fgMuted : Ink.accentInk)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Capsule().fill(typed.isEmpty ? Ink.bgInset : Ink.accent))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("verify.check-button")
                    Button { phase = .listening } label: {
                        Image(systemName: "mic")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundStyle(Ink.fg)
                            .frame(width: 48, height: 48)
                            .background(Capsule().fill(Ink.bgInset))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("verify.listen-button")
                }
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Ink.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
            )

            SectionLabel(text: "If they can't give it")
                .padding(.horizontal, 4).padding(.top, 20)

            VStack(spacing: 0) {
                tipRow(n: 1, title: "Hang up immediately.", sub: "A real person will understand.")
                tipRow(n: 2, title: "Call them back on a known number.", sub: "Don't dial what they gave you.")
                tipRow(n: 3, title: "Try an emergency override word.", sub: "If they know the fallback, trust cautiously.")
            }
            .padding(.top, 10)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Ink.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
            )
        }
    }

    private var noGroupPanel: some View {
        VStack(spacing: 14) {
            Image(systemName: "person.2.slash")
                .font(.system(size: 38, weight: .light))
                .foregroundStyle(Ink.accent)
            Text("Create a group first.")
                .font(Fonts.display(24))
                .foregroundStyle(Ink.fg)
            Button("Start setup") { screen = .onboarding }
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .padding(.horizontal, 22).padding(.vertical, 13)
                .background(Capsule().fill(Ink.accent))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }

    private func verifyNotNeededPanel(group: Group) -> some View {
        VStack(spacing: 18) {
            Image(systemName: "checkmark.shield")
                .font(.system(size: 42, weight: .light))
                .foregroundStyle(Ink.accent)
            Text("Verify isn't needed for \(group.name) right now")
                .font(Fonts.display(25))
                .tracking(-0.7)
                .foregroundStyle(Ink.fg)
                .multilineTextAlignment(.center)
            Text("Both phones show the same word. Add challenge/answer or a static override in Settings → Group → Primitives if you'd like a tap-to-confirm flow.")
                .font(Fonts.body(14))
                .foregroundStyle(Ink.fgMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
            Button("Open Primitives") { screen = .settings }
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .padding(.horizontal, 22)
                .padding(.vertical, 13)
                .background(Capsule().fill(Ink.accent))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 46)
        .accessibilityIdentifier("verify.empty-state")
    }

    private func tipRow(n: Int, title: String, sub: String) -> some View {
        VStack(spacing: 0) {
            if n > 1 { Rectangle().fill(Ink.rule).frame(height: 0.5) }
            HStack(alignment: .top, spacing: 12) {
                Text("\(n)")
                    .font(Fonts.mono(11, weight: .semibold))
                    .foregroundStyle(Ink.accent)
                    .frame(width: 22, height: 22)
                    .background(Circle().fill(Ink.tickFill))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(Fonts.body(14, weight: .medium)).foregroundStyle(Ink.fg)
                    Text(sub).font(Fonts.body(12.5)).foregroundStyle(Ink.fgMuted)
                }
                Spacer()
            }
            .padding(.horizontal, 16).padding(.vertical, 14)
        }
    }

    private var listeningPanel: some View {
        VStack(spacing: 28) {
            ZStack {
                ForEach(0..<3, id: \.self) { i in
                    Circle()
                        .stroke(Ink.accent, lineWidth: 1)
                        .opacity(pulse ? 0 : (0.6 - Double(i) * 0.15))
                        .scaleEffect(pulse ? 1.0 : 0.3)
                        .animation(
                            .easeOut(duration: 2.4)
                                .repeatForever(autoreverses: false)
                                .delay(Double(i) * 0.4),
                            value: pulse
                        )
                }
                Circle()
                    .fill(Ink.accent)
                    .frame(width: 72, height: 72)
                    .overlay(
                        Image(systemName: "mic.fill")
                            .font(.system(size: 28))
                            .foregroundStyle(Ink.accentInk)
                    )
            }
            .frame(width: 180, height: 180)
            .onAppear { pulse = true }

            VStack(spacing: 6) {
                Text("Listening…")
                    .font(Fonts.display(24))
                    .tracking(-0.6)
                    .foregroundStyle(Ink.fg)
                Text("Ask: \"What's our word?\"")
                    .font(Fonts.body(13))
                    .foregroundStyle(Ink.fgMuted)
            }

            HStack(spacing: 10) {
                choiceButton("They matched") { phase = .match }
                choiceButton("They did not") { phase = .mismatch }
            }

            Button("Cancel") { reset() }
                .font(Fonts.body(13))
                .foregroundStyle(Ink.fgMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 20)
    }

    private func choiceButton(_ label: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(Fonts.body(14, weight: .medium))
                .foregroundStyle(Ink.fg)
                .padding(.horizontal, 18).padding(.vertical, 12)
                .background(Capsule().stroke(Ink.rule, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func check() {
        guard let group = groupStore.selectedGroup,
              let word = groupStore.currentSafeword(for: group) else {
            phase = .mismatch
            return
        }
        let answer = typed.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        let override = groupStore.emergencyOverrideWord(groupID: group.id)?
            .lowercased()
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let staticOverride = group.primitives.staticOverride.enabled
            ? groupStore.seed(for: group.id).map { Primitives.staticOverride(seed: $0).phrase.lowercased() }
            : nil
        if answer == word.lowercased()
            || (!answer.isEmpty && (override.map { answer == $0 } ?? false))
            || (!answer.isEmpty && (staticOverride.map { answer == $0 } ?? false)) {
            phase = .match
        } else {
            phase = .mismatch
        }
    }

    private func reset() {
        phase = .ready
        typed = ""
    }
}

private struct ResultCard: View {
    let match: Bool
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle()
                    .fill(match ? Ink.ok.opacity(0.15) : Ink.tickFill)
                    .overlay(Circle().stroke(match ? Ink.ok : Ink.accent, lineWidth: 1))
                    .frame(width: 120, height: 120)
                Image(systemName: match ? "checkmark" : "exclamationmark.triangle")
                    .font(.system(size: match ? 50 : 44, weight: .medium))
                    .foregroundStyle(match ? Ink.ok : Ink.accent)
            }

            VStack(spacing: 10) {
                Text(match ? "Verified." : "Don't trust.")
                    .font(Fonts.display(30))
                    .tracking(-1.0)
                    .foregroundStyle(match ? Ink.ok : Ink.accent)
                Text(match
                    ? "They gave the correct word. This is them."
                    : "They could not produce the word. Hang up and call a known number.")
                    .font(Fonts.body(14))
                    .foregroundStyle(Ink.fgMuted)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .frame(maxWidth: 280)
            }

            if !match {
                HStack(spacing: 10) {
                    Image(systemName: "phone")
                        .font(.system(size: 14)).foregroundStyle(Ink.accent)
                    Text("Hang up. Call them on their known number.")
                        .font(Fonts.body(13))
                        .foregroundStyle(Ink.fg)
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Ink.bgElev)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Ink.rule, lineWidth: 0.5))
                )
            }

            Button("Done", action: onBack)
                .font(Fonts.body(13))
                .foregroundStyle(Ink.fgMuted)
                .padding(.horizontal, 22).padding(.vertical, 12)
                .background(Capsule().stroke(Ink.rule, lineWidth: 0.5))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 40)
    }
}
