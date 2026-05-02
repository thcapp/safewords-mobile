import SwiftUI

struct DrillsView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen
    @State private var sessions: [DrillSession] = DrillService.sessions()
    @State private var running = false
    @State private var scenario = "A caller says they are family and asks for money urgently."

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.horizontal, 20)
                        .padding(.top, 62)

                    if running {
                        drillPrompt
                            .padding(.horizontal, 16)
                            .padding(.top, 28)
                    } else {
                        runCard
                            .padding(.horizontal, 16)
                            .padding(.top, 28)
                        history
                            .padding(.horizontal, 16)
                            .padding(.top, 24)
                    }
                }
                .padding(.bottom, 80)
            }
            .scrollIndicators(.hidden)
        }
        .onAppear { sessions = DrillService.sessions() }
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
            VStack(alignment: .leading, spacing: 2) {
                SectionLabel(text: "Practice")
                Text("Scam drills")
                    .font(Fonts.display(26))
                    .tracking(-0.6)
                    .foregroundStyle(Ink.fg)
            }
            Spacer()
        }
    }

    private var runCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Practice before a real call.")
                .font(Fonts.body(18, weight: .semibold))
                .foregroundStyle(Ink.fg)
            Text("A drill walks you through asking for the word without reading it out loud.")
                .font(Fonts.body(13.5))
                .foregroundStyle(Ink.fgMuted)
                .lineSpacing(3)
            Button {
                running = true
            } label: {
                Text("Run drill now")
                    .font(Fonts.body(15, weight: .semibold))
                    .foregroundStyle(Ink.accentInk)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Capsule().fill(Ink.accent))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("drills.start")
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Ink.rule, lineWidth: 0.5))
        )
    }

    private var drillPrompt: some View {
        VStack(alignment: .leading, spacing: 18) {
            SectionLabel(text: "Drill in progress")
            Text("Someone is calling.")
                .font(Fonts.display(32))
                .tracking(-1.0)
                .foregroundStyle(Ink.fg)
            Text(scenario)
                .font(Fonts.body(15))
                .foregroundStyle(Ink.fgMuted)
                .lineSpacing(4)
                .accessibilityIdentifier("drills.scenario")
            Text("Ask them: \"What is our word?\" Do not read the word to them.")
                .font(Fonts.body(16, weight: .semibold))
                .foregroundStyle(Ink.accent)
                .lineSpacing(4)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(Ink.tickFill))

            HStack(spacing: 10) {
                resultButton("They knew it", success: true)
                resultButton("They failed", success: false)
            }

            Button("Cancel drill") { running = false }
                .font(Fonts.body(13))
                .foregroundStyle(Ink.fgMuted)
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Ink.rule, lineWidth: 0.5))
        )
    }

    private func resultButton(_ label: String, success: Bool) -> some View {
        Button {
            DrillService.record(group: groupStore.selectedGroup, scenario: scenario, success: success)
            sessions = DrillService.sessions()
            running = false
        } label: {
            Text(label)
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(success ? Ink.accentInk : Ink.fg)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Capsule().fill(success ? Ink.ok : Ink.bgInset))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(success ? "drills.passed" : "drills.failed")
    }

    private var history: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionLabel(text: "History")
            if sessions.isEmpty {
                Text("No drills yet.")
                    .font(Fonts.body(13.5))
                    .foregroundStyle(Ink.fgMuted)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: 18).fill(Ink.bgElev))
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(sessions.prefix(8).enumerated()), id: \.element.id) { index, session in
                        if index > 0 { Rectangle().fill(Ink.rule).frame(height: 0.5).padding(.leading, 16) }
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(session.success ? "Passed" : "Needs practice")
                                    .font(Fonts.body(14.5, weight: .semibold))
                                    .foregroundStyle(session.success ? Ink.ok : Ink.warn)
                                Text("\(session.groupName) · \(session.completedAt.formatted(date: .abbreviated, time: .shortened))")
                                    .font(Fonts.body(12))
                                    .foregroundStyle(Ink.fgMuted)
                            }
                            Spacer()
                        }
                        .padding(16)
                        .accessibilityIdentifier("drills.history-row.\(index)")
                    }
                }
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Ink.bgElev)
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Ink.rule, lineWidth: 0.5))
                )
            }
        }
    }
}
