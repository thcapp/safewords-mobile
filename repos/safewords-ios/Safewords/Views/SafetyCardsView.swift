import SwiftUI
import UIKit

struct SafetyCardsView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen

    @State private var selectedKind: SafetyCardKind?
    @State private var authError: String?
    @State private var shareImage: ShareImage?

    private var activeGroup: Group? {
        groupStore.selectedGroup
    }

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    header

                    if let authError {
                        Text(authError)
                            .font(Fonts.body(12.5))
                            .foregroundStyle(Ink.warn)
                            .padding(14)
                            .background(RoundedRectangle(cornerRadius: 14).fill(Ink.tickFill))
                    }

                    if let group = activeGroup, let seed = groupStore.seed(for: group.id) {
                        cardList(group: group, seed: seed)
                        if let selectedKind {
                            preview(kind: selectedKind, group: group, seed: seed)
                        }
                    } else {
                        Text("Create or join a group before printing safety cards.")
                            .font(Fonts.body(14))
                            .foregroundStyle(Ink.fgMuted)
                            .padding(16)
                            .background(RoundedRectangle(cornerRadius: 18).fill(Ink.bgElev))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 62)
                .padding(.bottom, 90)
            }
            .scrollIndicators(.hidden)
        }
        .sheet(item: $shareImage) { item in
            ShareSheet(items: [item.image])
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { screen = .settings } label: {
                Image(systemName: "arrow.left")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Ink.fg)
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(Ink.bgElev).overlay(Circle().stroke(Ink.rule, lineWidth: 0.5)))
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                SectionLabel(text: "Safety cards")
                Text(activeGroup?.name ?? "No group")
                    .font(Fonts.display(26))
                    .tracking(-0.8)
                    .foregroundStyle(Ink.fg)
            }

            Spacer()
        }
    }

    private func cardList(group: Group, seed: Data) -> some View {
        let kinds = SafetyCardKind.available(for: group)
        return VStack(spacing: 0) {
            ForEach(kinds) { kind in
                Button {
                    Task { await select(kind) }
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: kind.icon)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(kind.requiresAuth ? Ink.accent : Ink.fgMuted)
                            .frame(width: 28)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(kind.title)
                                .font(Fonts.body(14.5, weight: .semibold))
                                .foregroundStyle(Ink.fg)
                            Text(kind.requiresAuth ? "Requires unlock before rendering" : "Instructional card")
                                .font(Fonts.body(11.5))
                                .foregroundStyle(Ink.fgMuted)
                        }
                        Spacer()
                        Image(systemName: selectedKind == kind ? "checkmark" : "chevron.right")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(Ink.fgFaint)
                    }
                    .padding(14)
                }
                .buttonStyle(.plain)
                if kind != kinds.last {
                    Rectangle().fill(Ink.rule).frame(height: 0.5).padding(.leading, 16)
                }
            }
        }
        .background(RoundedRectangle(cornerRadius: 20).fill(Ink.bgElev).overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5)))
    }

    private func preview(kind: SafetyCardKind, group: Group, seed: Data) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionLabel(text: "Preview")
            cardView(kind: kind, group: group, seed: seed)
                .frame(height: 360)
                .scaleEffect(0.46)
                .frame(maxWidth: .infinity)
                .clipped()
                .background(RoundedRectangle(cornerRadius: 18).fill(Color.white))

            HStack(spacing: 10) {
                Button("Print") {
                    if let image = render(kind: kind, group: group, seed: seed) {
                        CardRenderer.printImage(image, jobName: kind.title)
                    }
                }
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Capsule().fill(Ink.accent))

                Button("Share") {
                    if let image = render(kind: kind, group: group, seed: seed) {
                        shareImage = ShareImage(image: image)
                    }
                }
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(Ink.fg)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Capsule().stroke(Ink.rule, lineWidth: 0.5))
            }
        }
        .padding(.top, 6)
    }

    @ViewBuilder
    private func cardView(kind: SafetyCardKind, group: Group, seed: Data) -> some View {
        switch kind {
        case .protocol:
            ProtocolCardView()
        case .staticOverride:
            OverrideCardView(group: group, seed: seed)
        case .challengeWallet:
            ChallengeAnswerCardView(group: group, seed: seed, variant: .wallet)
        case .challengeProtocol:
            ChallengeAnswerCardView(group: group, seed: seed, variant: .protocolFull)
        case .recoveryPhrase:
            RecoveryPhraseCardView(group: group, phrase: (try? RecoveryPhrase.encode(seed: seed)) ?? RecoveryPhraseService.seedHex(seed))
        case .groupInvite:
            GroupInviteCardView(group: group, seed: seed)
        }
    }

    @MainActor
    private func render(kind: SafetyCardKind, group: Group, seed: Data) -> UIImage? {
        switch kind {
        case .protocol:
            return CardRenderer.render(ProtocolCardView())
        case .staticOverride:
            return CardRenderer.render(OverrideCardView(group: group, seed: seed))
        case .challengeWallet:
            return CardRenderer.render(ChallengeAnswerCardView(group: group, seed: seed, variant: .wallet))
        case .challengeProtocol:
            return CardRenderer.render(ChallengeAnswerCardView(group: group, seed: seed, variant: .protocolFull))
        case .recoveryPhrase:
            let phrase = (try? RecoveryPhrase.encode(seed: seed)) ?? RecoveryPhraseService.seedHex(seed)
            return CardRenderer.render(RecoveryPhraseCardView(group: group, phrase: phrase))
        case .groupInvite:
            return CardRenderer.render(GroupInviteCardView(group: group, seed: seed))
        }
    }

    @MainActor
    private func select(_ kind: SafetyCardKind) async {
        authError = nil
        guard kind.requiresAuth else {
            selectedKind = kind
            return
        }
        let ok = await BiometricService.authenticateDeviceOwner(reason: "Show \(kind.title)")
        if ok {
            selectedKind = kind
        } else {
            authError = "Unlock before rendering this high-sensitivity card."
        }
    }
}

private enum SafetyCardKind: String, CaseIterable, Identifiable {
    case `protocol`
    case staticOverride
    case challengeWallet
    case challengeProtocol
    case recoveryPhrase
    case groupInvite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .protocol:
            return "Protocol card"
        case .staticOverride:
            return "Static override"
        case .challengeWallet:
            return "Challenge wallet excerpt"
        case .challengeProtocol:
            return "Challenge full protocol"
        case .recoveryPhrase:
            return "Recovery phrase"
        case .groupInvite:
            return "Group invite"
        }
    }

    var icon: String {
        switch self {
        case .protocol:
            return "list.bullet.rectangle"
        case .staticOverride:
            return "key"
        case .challengeWallet, .challengeProtocol:
            return "questionmark.bubble"
        case .recoveryPhrase:
            return "doc.text"
        case .groupInvite:
            return "qrcode"
        }
    }

    var requiresAuth: Bool {
        self != .protocol
    }

    static func available(for group: Group) -> [SafetyCardKind] {
        var kinds: [SafetyCardKind] = [.protocol, .recoveryPhrase, .groupInvite]
        if group.primitives.staticOverride.enabled {
            kinds.insert(.staticOverride, at: 1)
        }
        if group.primitives.challengeAnswer.enabled {
            kinds.insert(.challengeWallet, at: min(2, kinds.count))
            kinds.insert(.challengeProtocol, at: min(3, kinds.count))
        }
        return kinds
    }
}

private struct ShareImage: Identifiable {
    let id = UUID()
    let image: UIImage
}
