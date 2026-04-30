import SwiftUI

struct HomeView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen
    @AppStorage("revealStyle") private var revealStyle: String = "always"
    @AppStorage("previewNextWord") private var previewNextWord: Bool = false
    @State private var held = false

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()

            if let group = groupStore.selectedGroup {
                TimelineView(.periodic(from: .now, by: 1.0)) { ctx in
                    let ts = ctx.date.timeIntervalSince1970
                    let remaining = TOTPDerivation.getTimeRemaining(interval: group.interval.seconds, timestamp: ts)
                    let total = Double(group.interval.seconds)
                    let progress = 1.0 - remaining / total
                    content(group: group, progress: progress, remaining: remaining, timestamp: ts)
                }
            } else {
                emptyState
            }
        }
    }

    private func content(group: Group, progress: Double, remaining: TimeInterval, timestamp: TimeInterval) -> some View {
        let phrase = groupStore.safeword(for: group, at: timestamp) ?? "—"
        let words = phrase.split(separator: " ").map(String.init)

        return ZStack(alignment: .top) {
            // Top bar: group pill + bell
            HStack {
                Button {
                    screen = .groups
                } label: {
                    HStack(spacing: 8) {
                        GroupDot(initial: String(group.name.prefix(1)), color: groupColor(for: group), size: 24)
                        Text(group.name)
                            .font(Fonts.body(13, weight: .medium))
                            .foregroundStyle(Ink.fg)
                        Image(systemName: "chevron.down")
                            .font(.system(size: 9, weight: .medium))
                            .foregroundStyle(Ink.fgMuted)
                    }
                    .padding(.leading, 7)
                    .padding(.trailing, 12)
                    .padding(.vertical, 7)
                    .background(
                        Capsule().fill(Ink.bgElev)
                            .overlay(Capsule().stroke(Ink.rule, lineWidth: 0.5))
                    )
                }
                .buttonStyle(.plain)

                Spacer()

                Button { screen = .settings } label: {
                    Image(systemName: "bell")
                        .font(.system(size: 17))
                        .foregroundStyle(Ink.fg)
                        .frame(width: 38, height: 38)
                        .background(
                            Circle().fill(Ink.bgElev)
                                .overlay(Circle().stroke(Ink.rule, lineWidth: 0.5))
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 18)
            .padding(.top, 6)

            // Hero
            VStack(spacing: 28) {
                CountdownRing(progress: progress) {
                    VStack(spacing: 14) {
                        HStack(spacing: 6) {
                            Circle().fill(Ink.accent).frame(width: 6, height: 6)
                            SectionLabel(
                                text: "LIVE · \(group.name.uppercased())",
                                color: Ink.accent
                            )
                        }

                        VStack(spacing: 0) {
                            ForEach(Array(words.enumerated()), id: \.offset) { _, word in
                                Text(word)
                                    .font(Fonts.display(46, weight: .regular))
                                    .tracking(-1.5)
                                    .foregroundStyle(Ink.fg)
                            }
                        }
                        .blur(radius: shouldBlur ? 14 : 0)
                        .animation(.easeInOut(duration: 0.18), value: held)
                        .gesture(holdGesture)

                        Text("SEQ · \(sequenceString(for: group, at: timestamp))")
                            .font(Fonts.mono(11))
                            .tracking(1.5)
                            .foregroundStyle(Ink.fgFaint)
                            .padding(.top, 4)
                    }
                    .padding(.horizontal, 24)
                }
                .frame(width: 340, height: 340)
                .overlay(alignment: .bottom) {
                    if revealStyle == "holdReveal" && !held {
                        Text("Hold to reveal")
                            .font(Fonts.body(13))
                            .foregroundStyle(Ink.fgMuted)
                            .offset(y: -60)
                    }
                }

                VStack(spacing: 8) {
                    Text(countdownString(remaining: remaining))
                        .font(Fonts.mono(28))
                        .tracking(2)
                        .foregroundStyle(Ink.fg)
                        .monospacedDigit()

                    let rem = Int(remaining)
                    let h = rem / 3600
                    let m = (rem % 3600) / 60
                    let s = rem % 60
                    Text(secondaryCountdownText(group: group, timestamp: timestamp, hours: h, minutes: m, seconds: s))
                        .font(Fonts.body(12))
                        .foregroundStyle(Ink.fgMuted)
                }
            }
            .padding(.top, 60)
            .frame(maxWidth: .infinity)
        }
        .foregroundStyle(Ink.fg)
    }

    // Hold gesture for hold-to-reveal style.
    private var holdGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { _ in if revealStyle == "holdReveal" { held = true } }
            .onEnded { _ in held = false }
    }

    private var shouldBlur: Bool { revealStyle == "holdReveal" && !held }

    private var emptyState: some View {
        VStack(spacing: 24) {
            Image(systemName: "shield")
                .font(.system(size: 48, weight: .light))
                .foregroundStyle(Ink.accent)
            Text("No groups yet")
                .font(Fonts.display(28))
                .foregroundStyle(Ink.fg)
            Text("Create your first group to start\nsharing rotating safewords.")
                .font(Fonts.body(15))
                .foregroundStyle(Ink.fgMuted)
                .multilineTextAlignment(.center)
            Button("Create a group") { screen = .onboarding }
                .font(Fonts.body(15, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .padding(.horizontal, 22)
                .padding(.vertical, 14)
                .background(Capsule().fill(Ink.accent))
        }
    }

    private func countdownString(remaining: TimeInterval) -> String {
        let r = Int(remaining)
        let h = r / 3600
        let m = (r % 3600) / 60
        let s = r % 60
        return String(format: "%02d:%02d:%02d", h, m, s)
    }

    private func sequenceString(for group: Group, at timestamp: TimeInterval) -> String {
        let counter = Int(timestamp) / group.interval.seconds
        return String(format: "%04d", counter % 10_000)
    }

    private func secondaryCountdownText(
        group: Group,
        timestamp: TimeInterval,
        hours: Int,
        minutes: Int,
        seconds: Int
    ) -> String {
        let remaining = hours > 0 ? "\(hours)h \(minutes)m" : "\(minutes)m \(seconds)s"
        guard previewNextWord,
              let next = groupStore.safeword(for: group, at: timestamp + Double(group.interval.seconds)) else {
            return "rotates in \(remaining)"
        }
        return "rotates in \(remaining) · next: \(next)"
    }

    private func groupColor(for group: Group) -> Color {
        let idx = abs(group.id.hashValue) % 5
        return DotPalette.forIndex(idx)
    }
}

// App-wide screen enum used by our custom tab bar and state navigation.
enum AppScreen: String, CaseIterable {
    case home, groups, verify, settings, onboarding, addMember, qrScanner, recoveryPhrase, recoveryBackup, drills
}
