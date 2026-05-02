import SwiftUI

struct GroupsView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    header.padding(.horizontal, 20).padding(.top, 62)

                    VStack(spacing: 10) {
                        ForEach(Array(groupStore.groups.enumerated()), id: \.element.id) { idx, group in
                            groupCard(group, active: group.id == groupStore.selectedGroupID, idx: idx)
                        }

                        if let active = groupStore.selectedGroup {
                            HStack {
                                SectionLabel(text: "Members · \(active.name)")
                                Spacer()
                            }
                            .padding(.horizontal, 4)
                            .padding(.top, 20)

                            VStack(spacing: 0) {
                                ForEach(Array(active.members.enumerated()), id: \.element.id) { i, member in
                                    memberRow(member: member, isFirst: i == 0)
                                }
                            }
                            .background(
                                RoundedRectangle(cornerRadius: 20, style: .continuous)
                                    .fill(Ink.bgElev)
                                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
                            )

                            Button { screen = .addMember } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: "qrcode")
                                        .font(.system(size: 15, weight: .medium))
                                    Text("Invite someone to \(active.name)")
                                        .font(Fonts.body(14, weight: .semibold))
                                    Spacer()
                                    Image(systemName: "arrow.right")
                                        .font(.system(size: 12, weight: .medium))
                                }
                                .foregroundStyle(Ink.accent)
                                .padding(16)
                                .background(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .fill(Ink.tickFill)
                                )
                            }
                            .buttonStyle(.plain)
                            .accessibilityIdentifier("groups.invite-cta")
                            .padding(.top, 12)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 24)
                    .padding(.bottom, 140)
                }
            }
            .scrollIndicators(.hidden)
        }
    }

    private var header: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 6) {
                SectionLabel(text: "Groups")
                Text("Your circles")
                    .font(Fonts.display(34))
                    .tracking(-1.1)
                    .foregroundStyle(Ink.fg)
            }
            Spacer()
            Button { screen = .onboarding } label: {
                Image(systemName: "plus")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Ink.fg)
                    .frame(width: 40, height: 40)
                    .background(
                        Circle().fill(Ink.bgElev)
                            .overlay(Circle().stroke(Ink.rule, lineWidth: 0.5))
                    )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("groups.add-button")
        }
    }

    private func groupCard(_ group: Group, active: Bool, idx: Int) -> some View {
        Button {
            groupStore.selectedGroupID = group.id
            screen = .home
        } label: {
            HStack(alignment: .center, spacing: 14) {
                GroupDot(
                    initial: String(group.name.prefix(1)),
                    color: DotPalette.forIndex(idx),
                    size: 44
                )
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(group.name)
                            .font(Fonts.body(16, weight: .semibold))
                            .tracking(-0.2)
                            .foregroundStyle(Ink.fg)
                        if active {
                            Text("ACTIVE")
                                .font(Fonts.body(10, weight: .semibold))
                                .tracking(0.5)
                                .foregroundStyle(Ink.accent)
                                .padding(.horizontal, 7).padding(.vertical, 2)
                                .background(RoundedRectangle(cornerRadius: 6).fill(Ink.tickFill))
                        }
                    }
                    HStack(spacing: 10) {
                        Text("\(group.members.count) members")
                            .font(Fonts.body(12.5)).foregroundStyle(Ink.fgMuted)
                        Text("·").foregroundStyle(Ink.fgFaint)
                        Text("rotates \(intervalLabel(group.interval))")
                            .font(Fonts.body(12.5)).foregroundStyle(Ink.fgMuted)
                    }
                    .lineLimit(1)

                    Text(groupStore.currentSafeword(for: group) ?? "—")
                        .font(Fonts.mono(12.5))
                        .tracking(0.3)
                        .foregroundStyle(active ? Ink.accent : Ink.fg.opacity(0.7))
                        .padding(.top, 4)
                }
                Spacer(minLength: 0)

                Text("SEQ \(sequenceString(for: group))")
                    .font(Fonts.mono(10))
                    .tracking(1)
                    .foregroundStyle(Ink.fgFaint)
                    .rotationEffect(.degrees(-90))
                    .fixedSize()
                    .frame(width: 14)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Ink.bgElev)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(active ? Ink.accent : Ink.rule, lineWidth: 0.5)
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("groups.card.\(group.id.uuidString.lowercased())")
    }

    private func memberRow(member: Member, isFirst: Bool) -> some View {
        VStack(spacing: 0) {
            if !isFirst {
                Rectangle().fill(Ink.rule).frame(height: 0.5).padding(.leading, 60)
            }
            HStack(spacing: 12) {
                GroupDot(initial: String(member.name.prefix(1)), color: DotPalette.forIndex(member.colorIndex), size: 32)
                VStack(alignment: .leading, spacing: 1) {
                    Text(member.name)
                        .font(Fonts.body(14.5, weight: .medium))
                        .foregroundStyle(Ink.fg)
                    Text(deviceLabel(for: member))
                        .font(Fonts.body(11.5))
                        .foregroundStyle(Ink.fgMuted)
                }
                Spacer()
                statusTag(for: member)
            }
            .padding(.horizontal, 16).padding(.vertical, 14)
        }
    }

    private func statusTag(for member: Member) -> some View {
        HStack(spacing: 5) {
            Circle().fill(Ink.ok).frame(width: 6, height: 6)
            Text("SYNCED")
                .font(Fonts.body(11.5, weight: .medium))
                .tracking(0.3)
                .foregroundStyle(Ink.ok)
        }
    }

    private func deviceLabel(for member: Member) -> String {
        switch member.role {
        case .creator: return "iPhone · Last seen just now"
        case .member:  return "Last seen 4m ago"
        }
    }

    private func intervalLabel(_ i: RotationInterval) -> String {
        switch i {
        case .hourly: return "1 hour"
        case .daily: return "1 day"
        case .weekly: return "1 week"
        case .monthly: return "1 month"
        }
    }

    private func sequenceString(for group: Group) -> String {
        let counter = Int(Date().timeIntervalSince1970) / group.interval.seconds
        return String(counter % 10_000)
    }
}
