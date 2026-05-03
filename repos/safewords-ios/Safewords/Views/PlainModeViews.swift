import SwiftUI
import UIKit

// Plain / accessibility mode — high visibility screens for elderly + children.
// Atkinson Hyperlegible font (falls back to system), WCAG AAA amber-on-navy,
// minimum 20px body, 72px+ hit targets, plain language (no jargon).

enum PlainScreen: String, CaseIterable {
    case home, verify, help, onboarding
}

enum A11yFonts {
    static func body(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        if UIFont(name: "AtkinsonHyperlegible-Regular", size: size) != nil {
            return .custom("AtkinsonHyperlegible-Regular", size: size).weight(weight)
        }
        return .system(size: size, weight: weight)
    }
    static func display(_ size: CGFloat, weight: Font.Weight = .bold) -> Font {
        body(size, weight: weight)
    }
}

// ── Shared big button ─────────────────────────────────────────────
struct BigButton: View {
    enum Variant { case primary, ghost }
    let label: String
    var variant: Variant = .primary
    var iconSystemName: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                if let icon = iconSystemName {
                    Image(systemName: icon)
                        .font(.system(size: 22, weight: .bold))
                }
                Text(label)
                    .font(A11yFonts.body(22, weight: .bold))
                    .tracking(-0.2)
                Spacer(minLength: 0)
            }
            .foregroundStyle(variant == .primary ? A11Y.accentInk : A11Y.fg)
            .padding(.horizontal, 22).padding(.vertical, 18)
            .frame(maxWidth: .infinity, minHeight: 72, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(variant == .primary ? A11Y.accent : A11Y.bgElev)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(variant == .primary ? Color.clear : A11Y.rule, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

// ── Plain tab bar ─────────────────────────────────────────────────
struct A11yTabBar: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var active: PlainScreen

    private var tabs: [(key: PlainScreen, label: String, icon: String)] {
        var items: [(key: PlainScreen, label: String, icon: String)] = [
            (.home, "Word", "checkmark.shield")
        ]
        if groupStore.hasAnyVerifyPrimitive() {
            items.append((.verify, "Check", "phone"))
        }
        items.append((.help, "Help", "bell"))
        return items
    }

    var body: some View {
        HStack(spacing: 6) {
            ForEach(tabs, id: \.key) { tab in
                Button { active = tab.key } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 22, weight: .medium))
                        Text(tab.label)
                            .font(A11yFonts.body(15, weight: .bold))
                    }
                    .foregroundStyle(active == tab.key ? A11Y.accentInk : A11Y.fg)
                    .frame(maxWidth: .infinity, minHeight: 60)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .fill(active == tab.key ? A11Y.accent : Color.clear)
                    )
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier(tabIdentifier(for: tab.key))
            }
        }
        .padding(6)
        .background(
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .fill(A11Y.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 26).stroke(A11Y.rule, lineWidth: 2))
                .shadow(color: .black.opacity(0.45), radius: 40, y: 10)
        )
        .padding(.horizontal, 10)
        .padding(.bottom, 34)
    }

    private func tabIdentifier(for screen: PlainScreen) -> String {
        switch screen {
        case .home: return "plain-home.tab-word"
        case .verify: return "plain-home.tab-check"
        case .help: return "plain-home.tab-help"
        case .onboarding: return "plain-home.tab-word"
        }
    }
}

// ── Plain Home — the word, big ────────────────────────────────────
struct PlainHomeView: View {
    @Environment(GroupStore.self) private var groupStore
    var onCall: () -> Void = {}
    var onSettings: () -> Void = {}
    @State private var showingChallenge = false

    var body: some View {
        ZStack {
            A11Y.bg.ignoresSafeArea()
            if let group = groupStore.selectedGroup {
                TimelineView(.periodic(from: .now, by: 1.0)) { ctx in
                    let ts = ctx.date.timeIntervalSince1970
                    let remaining = TOTPDerivation.getTimeRemaining(
                        interval: rotationIntervalSeconds(for: group), timestamp: ts
                    )
                    content(group: group, remaining: remaining, ts: ts)
                }
            } else {
                Text("Pick a circle first.")
                    .font(A11yFonts.body(22))
                    .foregroundStyle(A11Y.fg)
            }
        }
    }

    private func content(group: Group, remaining: TimeInterval, ts: TimeInterval) -> some View {
        let phrase = groupStore.safeword(for: group, at: ts) ?? "—"
        let humanTime = humanTimeString(remaining)
        let title = group.primitives.rotatingWord.wordFormat == .numeric ? "YOUR CODE NOW" : "YOUR WORD NOW"

        return VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(A11Y.accent).frame(width: 52, height: 52)
                    Text(String(group.name.prefix(1)))
                        .font(A11yFonts.body(26, weight: .heavy))
                        .foregroundStyle(A11Y.accentInk)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Your circle")
                        .font(A11yFonts.body(15, weight: .bold))
                        .foregroundStyle(A11Y.fgMuted)
                    Text(group.name)
                        .font(A11yFonts.body(22, weight: .bold))
                        .tracking(-0.3)
                        .foregroundStyle(A11Y.fg)
                        .accessibilityIdentifier("plain-home.group-name")
                }
                Spacer()
                Button(action: onSettings) {
                    Image(systemName: "gearshape")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(A11Y.fg)
                        .frame(width: 56, height: 56)
                        .background(Circle().fill(A11Y.bgElev).overlay(Circle().stroke(A11Y.rule, lineWidth: 2)))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("plain-home.gear-button")
            }
            .padding(.horizontal, 8).padding(.bottom, 16)

            // Hero card
            VStack(spacing: 28) {
                HStack(spacing: 8) {
                    Circle().fill(A11Y.accent).frame(width: 12, height: 12)
                    Text(title)
                        .font(A11yFonts.body(18, weight: .bold))
                        .tracking(0.3)
                        .foregroundStyle(A11Y.accent)
                }

                VStack(spacing: 2) {
                    ForEach(Array(phrase.split(separator: " ").enumerated()), id: \.offset) { _, w in
                        Text(String(w))
                            .font(A11yFonts.body(48, weight: .heavy))
                            .tracking(-1.5)
                            .foregroundStyle(A11Y.fg)
                    }
                }
                .accessibilityIdentifier("plain-home.word-display")

                HStack(spacing: 10) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.system(size: 22))
                        .foregroundStyle(A11Y.accent)
                    Text("New word in \(humanTime)")
                        .font(A11yFonts.body(20, weight: .bold))
                        .foregroundStyle(A11Y.fg)
                }
                .padding(.horizontal, 22).padding(.vertical, 16)
                .background(Capsule().fill(A11Y.bgInset))
                .accessibilityIdentifier("plain-home.countdown")

                Text("Ask: \"What is our word?\"\nDo not say it first.")
                    .font(A11yFonts.body(16))
                    .foregroundStyle(A11Y.fgMuted)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .frame(maxWidth: 300)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28).padding(.horizontal, 22)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(A11Y.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 28).stroke(A11Y.rule, lineWidth: 2))
                )

            if group.primitives.challengeAnswer.enabled {
                BigButton(label: "Challenge someone", iconSystemName: "questionmark.bubble.fill") {
                    showingChallenge = true
                }
                .accessibilityIdentifier("plain-home.challenge-cta")
                .padding(.top, 16)
            }

            if group.primitives.staticOverride.enabled {
                Text("Static override is available in Settings → Safety cards.")
                    .font(A11yFonts.body(15, weight: .bold))
                    .foregroundStyle(A11Y.fgMuted)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: 18).fill(A11Y.bgInset))
                    .padding(.top, 14)
            }
        }
        .padding(.horizontal, 18).padding(.top, 62).padding(.bottom, 120)
        .sheet(isPresented: $showingChallenge) {
            if let seed = groupStore.seed(for: group.id) {
                ChallengeSheet(group: group, seed: seed)
            }
        }
    }

    private func humanTimeString(_ remaining: TimeInterval) -> String {
        let r = Int(remaining)
        let h = r / 3600
        let m = (r % 3600) / 60
        if h > 0 { return "\(h) hour\(h == 1 ? "" : "s") left" }
        return "\(m) minute\(m == 1 ? "" : "s") left"
    }

    private func rotationIntervalSeconds(for group: Group) -> Int {
        group.primitives.rotatingWord.intervalSeconds > 0 ? group.primitives.rotatingWord.intervalSeconds : group.interval.seconds
    }
}

// ── Plain Verify — big yes/no buttons ─────────────────────────────
struct PlainVerifyView: View {
    @Environment(GroupStore.self) private var groupStore
    @Environment(\.openURL) private var openURL
    enum Phase { case ask, match, nomatch }
    @State private var phase: Phase = .ask

    var body: some View {
        ZStack {
            A11Y.bg.ignoresSafeArea()
            switch phase {
            case .ask: askPanel
            case .match: resultPanel(safe: true,
                title: "Safe to talk.",
                body: "They said the right word. This is really them.",
                primary: ("All done", { phase = .ask }))
            case .nomatch: resultPanel(safe: false,
                title: "Hang up now.",
                body: "They did not know the word. This is not your family. Do not send money. Do not share anything.",
                primary: ("I hung up", { phase = .ask }),
                secondary: ("Call them back on a trusted number", {
                    if let url = SmsInviteService.fallbackURL(body: "I got a strange call. Please call me on a trusted number.") {
                        openURL(url)
                    }
                }))
            }
        }
    }

    private var askPanel: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("STEP 1 OF 2")
                .font(A11yFonts.body(16, weight: .bold))
                .tracking(0.3)
                .foregroundStyle(A11Y.accent)
                .padding(.horizontal, 8).padding(.top, 8)

            (Text("Ask them:\n") + Text("\"What is our word?\"").foregroundColor(A11Y.accent))
                .font(A11yFonts.body(34, weight: .heavy))
                .tracking(-0.8)
                .lineSpacing(4)
                .foregroundStyle(A11Y.fg)
                .padding(.horizontal, 8).padding(.top, 10).padding(.bottom, 24)

            (Text("Do not read the word to them.").bold().foregroundColor(A11Y.fg)
                + Text(" They must say it themselves."))
                .font(A11yFonts.body(19))
                .lineSpacing(4)
                .foregroundStyle(A11Y.fgMuted)
                .padding(22)
                .background(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .fill(A11Y.bgElev)
                        .overlay(RoundedRectangle(cornerRadius: 22).stroke(A11Y.rule, lineWidth: 2))
                )

            Spacer()

            VStack(spacing: 14) {
                Text("Did they say the right word?")
                    .font(A11yFonts.body(22, weight: .heavy))
                    .foregroundStyle(A11Y.fg)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)

                answerButton(label: "Yes, it matched", color: A11Y.ok, textColor: Color(hex: "#052e14"), icon: "checkmark") { phase = .match }
                answerButton(label: "No, wrong word", color: A11Y.danger, textColor: Color(hex: "#3a0a0a"), icon: "xmark") { phase = .nomatch }

                Button("Cancel") { phase = .ask }
                    .font(A11yFonts.body(17, weight: .semibold))
                    .foregroundStyle(A11Y.fgMuted)
                    .underline()
                    .padding(14)
            }
        }
        .padding(.horizontal, 18).padding(.top, 62).padding(.bottom, 120)
    }

    private func answerButton(label: String, color: Color, textColor: Color, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(Color.black.opacity(0.18)).frame(width: 48, height: 48)
                    Image(systemName: icon)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(textColor)
                }
                Text(label)
                    .font(A11yFonts.body(24, weight: .heavy))
                    .foregroundStyle(textColor)
                Spacer()
            }
            .padding(22)
            .frame(maxWidth: .infinity, minHeight: 80, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous).fill(color)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(label == "Yes, it matched" ? "plain-verify.match-yes" : "plain-verify.match-no")
    }

    private func resultPanel(
        safe: Bool, title: String, body: String,
        primary: (String, () -> Void),
        secondary: (String, () -> Void)? = nil
    ) -> some View {
        let tone = safe ? A11Y.ok : A11Y.danger
        let iconColor = safe ? Color(hex: "#052e14") : Color(hex: "#3a0a0a")
        let cardBg = safe ? A11Y.ok.opacity(0.15) : A11Y.danger.opacity(0.15)

        return VStack(spacing: 16) {
            VStack(spacing: 20) {
                ZStack {
                    Circle().fill(tone).frame(width: 120, height: 120)
                    Image(systemName: safe ? "checkmark" : "xmark")
                        .font(.system(size: 60, weight: .bold))
                        .foregroundStyle(iconColor)
                }
                .padding(.bottom, 8)

                Text(title)
                    .font(A11yFonts.body(44, weight: .heavy))
                    .tracking(-1)
                    .foregroundStyle(tone)
                    .multilineTextAlignment(.center)

                Text(body)
                    .font(A11yFonts.body(20, weight: .medium))
                    .foregroundStyle(A11Y.fg)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 320)
                    .lineSpacing(4)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal, 22).padding(.vertical, 40)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(cardBg)
                    .overlay(RoundedRectangle(cornerRadius: 28).stroke(tone, lineWidth: 2))
            )

            BigButton(label: primary.0, action: primary.1)
                .accessibilityIdentifier("plain-verify.done")
            if let s = secondary {
                BigButton(label: s.0, variant: .ghost, iconSystemName: "phone", action: s.1)
            }
        }
        .padding(.horizontal, 18).padding(.top, 62).padding(.bottom, 120)
        .accessibilityIdentifier(safe ? "plain-verify.result-safe" : "plain-verify.result-hangup")
    }
}

// ── Plain Help ────────────────────────────────────────────────────
struct PlainHelpView: View {
    @AppStorage("plainMode") private var plainMode: Bool = true
    @Environment(\.openURL) private var openURL

    private struct Item { let icon: String; let label: String; let sub: String }

    private let items: [Item] = [
        .init(icon: "phone",    label: "I got a strange call",    sub: "What to do right now"),
        .init(icon: "person.2", label: "Who is in my circle",     sub: "See your family and friends"),
        .init(icon: "arrow.triangle.2.circlepath", label: "What's a \"word\"?", sub: "A short, simple explanation"),
        .init(icon: "textformat.size",  label: "Change text size",       sub: "Make everything bigger"),
        .init(icon: "gearshape", label: "Call my family for help", sub: "Ring a trusted person"),
    ]

    var body: some View {
        ZStack {
            A11Y.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("HELP")
                        .font(A11yFonts.body(16, weight: .bold))
                        .tracking(0.3)
                        .foregroundStyle(A11Y.accent)
                        .padding(.horizontal, 8).padding(.top, 8)
                    Text("How can we help?")
                        .font(A11yFonts.body(34, weight: .heavy))
                        .tracking(-0.8)
                        .foregroundStyle(A11Y.fg)
                        .padding(.horizontal, 8).padding(.top, 8).padding(.bottom, 22)

                    VStack(spacing: 12) {
                        ForEach(items.indices, id: \.self) { i in itemCard(items[i]) }
                    }

                    Button {
                        if let url = URL(string: "tel://911") {
                            openURL(url)
                        }
                    } label: {
                        VStack(spacing: 6) {
                            Text("EMERGENCY")
                                .font(A11yFonts.body(14, weight: .bold))
                                .tracking(0.5)
                                .foregroundStyle(A11Y.danger)
                            Text("If you feel unsafe, call 911.")
                                .font(A11yFonts.body(20, weight: .bold))
                                .foregroundStyle(A11Y.fg)
                        }
                        .padding(18)
                        .frame(maxWidth: .infinity)
                        .background(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(A11Y.danger.opacity(0.12))
                                .overlay(RoundedRectangle(cornerRadius: 18).stroke(A11Y.danger, lineWidth: 2))
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("plain-help.emergency")
                    .padding(.top, 22)

                    BigButton(label: "Open Advanced View", variant: .ghost, iconSystemName: "arrow.uturn.left") {
                        plainMode = false
                    }
                    .accessibilityIdentifier("plain-help.exit")
                    .padding(.top, 18)
                }
                .padding(.horizontal, 18).padding(.top, 62).padding(.bottom, 120)
            }
            .scrollIndicators(.hidden)
        }
    }

    @ViewBuilder
    private func itemCard(_ item: Item) -> some View {
        if item.label == "Change text size" {
            itemCardButton(item)
                .accessibilityIdentifier("plain-help.text-size")
        } else {
            itemCardButton(item)
        }
    }

    private func itemCardButton(_ item: Item) -> some View {
        Button { handle(item) } label: {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(A11Y.bgInset).frame(width: 52, height: 52)
                    Image(systemName: item.icon)
                        .font(.system(size: 22, weight: .medium))
                        .foregroundStyle(A11Y.accent)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(item.label).font(A11yFonts.body(20, weight: .bold))
                        .foregroundStyle(A11Y.fg).tracking(-0.2)
                    Text(item.sub).font(A11yFonts.body(15))
                        .foregroundStyle(A11Y.fgMuted)
                }
                Spacer()
                Image(systemName: "arrow.right")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(A11Y.fgFaint)
            }
            .padding(18)
            .frame(maxWidth: .infinity, minHeight: 80, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(A11Y.bgElev)
                    .overlay(RoundedRectangle(cornerRadius: 18).stroke(A11Y.rule, lineWidth: 2))
            )
        }
        .buttonStyle(.plain)
    }

    private func handle(_ item: Item) {
        switch item.label {
        case "Change text size":
            if let url = URL(string: UIApplication.openSettingsURLString) {
                openURL(url)
            }
        case "Call my family for help":
            if let url = SmsInviteService.fallbackURL(body: "I need help with a suspicious call. Please call me on a trusted number.") {
                openURL(url)
            }
        default:
            break
        }
    }
}

// ── Plain Onboarding ──────────────────────────────────────────────
struct PlainOnboardingView: View {
    @AppStorage("plainOnboarded") private var onboarded: Bool = false
    @State private var step = 0

    private let panels: [(eyebrow: String, title: String, body: String, cta: String)] = [
        ("WELCOME", "One word keeps you safe.",
         "Bad people can copy any voice now. If someone calls and sounds like family, you need a way to be sure it's really them.",
         "Show me how"),
        ("HOW IT WORKS", "Your family picks a secret word.",
         "We give you a new word every day. When someone calls, ask them to say it. Only your real family will know it.",
         "Get started"),
    ]

    var body: some View {
        ZStack {
            A11Y.bg.ignoresSafeArea()
            let p = panels[step]
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    ForEach(panels.indices, id: \.self) { i in
                        RoundedRectangle(cornerRadius: 3).fill(i <= step ? A11Y.accent : A11Y.bgElev)
                            .frame(height: 6)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(.bottom, 28)

                Text(p.eyebrow)
                    .font(A11yFonts.body(16, weight: .bold))
                    .tracking(0.3)
                    .foregroundStyle(A11Y.accent)
                    .padding(.horizontal, 4)

                Text(p.title)
                    .font(A11yFonts.body(40, weight: .heavy))
                    .tracking(-1.2)
                    .foregroundStyle(A11Y.fg)
                    .padding(.horizontal, 4).padding(.top, 8)
                    .lineSpacing(4)

                Text(p.body)
                    .font(A11yFonts.body(22, weight: .medium))
                    .foregroundStyle(A11Y.fgMuted)
                    .padding(.horizontal, 4).padding(.top, 18)
                    .lineSpacing(4)

                VStack(spacing: 10) {
                    Text("EXAMPLE WORD")
                        .font(A11yFonts.body(14, weight: .bold))
                        .tracking(0.4)
                        .foregroundStyle(A11Y.fgMuted)
                    Text("Golden Robin")
                        .font(A11yFonts.body(38, weight: .heavy))
                        .tracking(-1)
                        .foregroundStyle(A11Y.accent)
                }
                .frame(maxWidth: .infinity)
                .padding(22)
                .background(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .fill(A11Y.bgElev)
                        .overlay(RoundedRectangle(cornerRadius: 22).stroke(A11Y.rule, lineWidth: 2))
                )
                .padding(.top, 30)

                Spacer()

                VStack(spacing: 12) {
                    BigButton(label: p.cta) {
                        if step < panels.count - 1 { step += 1 }
                        else { onboarded = true }
                    }
                    .accessibilityIdentifier(step < panels.count - 1 ? "plain-onboarding.cta-next" : "plain-onboarding.cta-done")
                    if step > 0 {
                        Button("Back") { step -= 1 }
                            .font(A11yFonts.body(18, weight: .semibold))
                            .foregroundStyle(A11Y.fgMuted)
                            .padding(14)
                            .accessibilityIdentifier("plain-onboarding.back")
                    }
                }
            }
            .padding(.horizontal, 18).padding(.top, 62).padding(.bottom, 56)
        }
    }
}

// ── Plain container: tab bar + current screen ─────────────────────
struct PlainRoot: View {
    @State private var screen: PlainScreen = .home
    var onSettings: () -> Void = {}

    var body: some View {
        ZStack(alignment: .bottom) {
            SwiftUI.Group {
                switch screen {
                case .home: PlainHomeView(onCall: { screen = .verify }, onSettings: onSettings)
                case .verify: PlainVerifyView()
                case .help: PlainHelpView()
                case .onboarding: PlainOnboardingView()
                }
            }
            A11yTabBar(active: $screen)
        }
        .background(A11Y.bg.ignoresSafeArea())
    }
}
