import SwiftUI

struct CustomTabBar: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var active: AppScreen

    private struct Tab: Identifiable {
        let id = UUID()
        let key: AppScreen
        let label: String
        let icon: String
    }

    private var tabs: [Tab] {
        var items: [Tab] = [
            .init(key: .home, label: "Word", icon: "checkmark.shield"),
            .init(key: .groups, label: "Groups", icon: "person.2")
        ]
        if groupStore.hasAnyVerifyPrimitive() {
            items.append(.init(key: .verify, label: "Verify", icon: "phone"))
        }
        items.append(.init(key: .settings, label: "Settings", icon: "gearshape"))
        return items
    }

    var body: some View {
        HStack(spacing: 4) {
            ForEach(tabs) { tab in
                Button { active = tab.key } label: {
                    VStack(spacing: 3) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 18, weight: .medium))
                        Text(tab.label)
                            .font(Fonts.body(10.5, weight: .medium))
                            .tracking(0.2)
                    }
                    .foregroundStyle(active == tab.key ? Ink.fg : Ink.fgMuted)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 10).padding(.bottom, 9)
                    .background(
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .fill(active == tab.key ? Ink.bgInset : Color.clear)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(6)
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 28).stroke(Ink.rule, lineWidth: 0.5))
                .shadow(color: .black.opacity(0.35), radius: 40, y: 10)
        )
        .padding(.horizontal, 12)
        .padding(.bottom, 26)
    }
}
