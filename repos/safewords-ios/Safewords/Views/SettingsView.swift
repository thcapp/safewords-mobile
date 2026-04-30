import SwiftUI

private let settingsSharedDefaults = UserDefaults(suiteName: KeychainService.appGroupID)

struct SettingsView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen

    @AppStorage("plainMode") private var plainMode: Bool = false
    @AppStorage("onboarded") private var onboarded: Bool = false
    @AppStorage("revealStyle") private var revealStyle: String = "always"
    @AppStorage("notifyOnRotation") private var notifyOnRotation: Bool = true
    @AppStorage("previewNextWord") private var previewNextWord: Bool = false
    @AppStorage("requireBiometrics") private var requireBiometrics: Bool = false
    @AppStorage("lockScreenGlance", store: settingsSharedDefaults) private var lockScreenGlance: Bool = true
    @AppStorage("hideWordUntilUnlock", store: settingsSharedDefaults) private var hideWordUntilUnlock: Bool = false

    @State private var showResetConfirmation = false
    @State private var showLeaveConfirmation = false
    @State private var showEmergencySheet = false
    @State private var showWidgetInfo = false
    @State private var showBiometricUnavailable = false
    @State private var emergencyWord = ""

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header.padding(.horizontal, 20).padding(.top, 62)

                    let group = groupStore.selectedGroup
                    section(label: "Rotation · \(group?.name ?? "No group")") {
                        if let group {
                            intervalPicker(group: group)
                        } else {
                            infoRow("Create a group to choose a rotation interval.", value: nil)
                        }
                        divider
                        toggleRow("Notify on rotation", binding: $notifyOnRotation)
                        divider
                        toggleRow("Include preview of next word", binding: $previewNextWord)
                    }

                    section(label: "Accessibility") {
                        toggleRow("High visibility mode", binding: $plainMode)
                        divider
                        toggleRow("Hold to reveal word", binding: revealBinding)
                    }

                    section(label: "Widget & Lock Screen") {
                        actionRow("Home screen widget", value: "Instructions") { showWidgetInfo = true }
                        divider
                        toggleRow("Lock screen glance", binding: $lockScreenGlance)
                        divider
                        toggleRow("Hide word until unlock", binding: $hideWordUntilUnlock)
                    }

                    section(label: "Security") {
                        toggleRow("Require \(BiometricService.biometryName()) to open", binding: biometricBinding)
                        divider
                        actionRow("Emergency override word", value: emergencyValue(for: group)) {
                            emergencyWord = group.flatMap { groupStore.emergencyOverrideWord(groupID: $0.id) } ?? ""
                            showEmergencySheet = true
                        }
                        .disabled(group == nil)
                        divider
                        infoRow("Rotate group seed", value: "v1.2")
                        divider
                        actionRow("Back up seed phrase", value: group == nil ? "No group" : "24 words") {
                            screen = .recoveryBackup
                        }
                        .disabled(group == nil)
                    }

                    section(label: "Practice") {
                        actionRow("Run a scam drill", value: "Now") { screen = .drills }
                        divider
                        actionRow("Drill history", value: "\(DrillService.sessions().count) saved") { screen = .drills }
                    }

                    section(label: "Danger zone") {
                        dangerRow("Leave this group") { showLeaveConfirmation = true }
                            .disabled(group == nil)
                        divider
                        dangerRow("Reset device") { showResetConfirmation = true }
                    }

                    VStack(spacing: 4) {
                        Text("Safewords v1.1 · Offline-first")
                        Text("No server. No account. No data collection.")
                    }
                    .font(Fonts.body(11))
                    .tracking(0.3)
                    .foregroundStyle(Ink.fgFaint)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 24)
                    .padding(.bottom, 140)
                }
            }
            .scrollIndicators(.hidden)
        }
        .alert("Leave this group?", isPresented: $showLeaveConfirmation) {
            Button("Leave group", role: .destructive) { leaveSelectedGroup() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This removes the selected group and its seed from this device.")
        }
        .alert("Delete all data", isPresented: $showResetConfirmation) {
            Button("Delete everything", role: .destructive) { resetAllData() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete all groups and seeds from this device.")
        }
        .alert("Biometrics unavailable", isPresented: $showBiometricUnavailable) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Set up Face ID or Touch ID in iOS Settings before requiring biometrics.")
        }
        .alert("Add the widget", isPresented: $showWidgetInfo) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Long-press your Home Screen, tap +, then add the Safewords widget.")
        }
        .sheet(isPresented: $showEmergencySheet) {
            emergencySheet
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionLabel(text: "Settings")
            Text("Preferences")
                .font(Fonts.display(34))
                .tracking(-1.1)
                .foregroundStyle(Ink.fg)
        }
    }

    private var revealBinding: Binding<Bool> {
        Binding(
            get: { revealStyle == "holdReveal" },
            set: { revealStyle = $0 ? "holdReveal" : "always" }
        )
    }

    private var biometricBinding: Binding<Bool> {
        Binding(
            get: { requireBiometrics },
            set: { enabled in
                if enabled {
                    if BiometricService.canEvaluate() {
                        requireBiometrics = true
                    } else {
                        requireBiometrics = false
                        showBiometricUnavailable = true
                    }
                } else {
                    requireBiometrics = false
                }
            }
        )
    }

    private var emergencySheet: some View {
        VStack(alignment: .leading, spacing: 18) {
            Capsule().fill(Ink.rule).frame(width: 42, height: 4).frame(maxWidth: .infinity)
            Text("Emergency override")
                .font(Fonts.display(28))
                .foregroundStyle(Ink.fg)
            Text("Use this only as a fallback. The rotating word is safer.")
                .font(Fonts.body(14))
                .foregroundStyle(Ink.fgMuted)
                .lineSpacing(3)
            TextField("", text: $emergencyWord, prompt: Text("fallback word").foregroundColor(Ink.fgFaint))
                .font(Fonts.body(18, weight: .medium))
                .foregroundStyle(Ink.fg)
                .tint(Ink.accent)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(Ink.bgInset))
            HStack(spacing: 10) {
                Button("Clear") {
                    if let group = groupStore.selectedGroup {
                        groupStore.setEmergencyOverrideWord(groupID: group.id, word: nil)
                    }
                    showEmergencySheet = false
                }
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(Ink.fgMuted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Capsule().stroke(Ink.rule, lineWidth: 0.5))

                Button("Save") {
                    if let group = groupStore.selectedGroup {
                        groupStore.setEmergencyOverrideWord(groupID: group.id, word: emergencyWord)
                    }
                    showEmergencySheet = false
                }
                .font(Fonts.body(14, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Capsule().fill(Ink.accent))
            }
        }
        .padding(24)
        .background(Ink.bg.ignoresSafeArea())
        .presentationDetents([.medium])
    }

    private func section<Content: View>(label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionLabel(text: label)
                .padding(.horizontal, 20)
                .padding(.top, 22)
                .padding(.bottom, 8)
            VStack(spacing: 0) { content() }
                .background(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(Ink.bgElev)
                        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
                )
                .padding(.horizontal, 16)
        }
    }

    private var divider: some View {
        Rectangle().fill(Ink.rule).frame(height: 0.5).padding(.leading, 16)
    }

    private func intervalPicker(group: Group) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Interval")
                .font(Fonts.body(13))
                .foregroundStyle(Ink.fgMuted)
            HStack(spacing: 6) {
                ForEach(RotationInterval.allCases) { interval in
                    Button {
                        groupStore.updateGroupInterval(group.id, interval: interval)
                    } label: {
                        Text(displayShort(interval))
                            .font(Fonts.body(12.5, weight: .medium))
                            .foregroundStyle(group.interval == interval ? Ink.accentInk : Ink.fg)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(group.interval == interval ? Ink.accent : Ink.bgInset)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
    }

    private func displayShort(_ interval: RotationInterval) -> String {
        switch interval {
        case .hourly: return "1 hour"
        case .daily: return "1 day"
        case .weekly: return "1 week"
        case .monthly: return "1 month"
        }
    }

    private func actionRow(_ label: String, value: String? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            rowContent(label, value: value, accent: false, chevron: true)
        }
        .buttonStyle(.plain)
    }

    private func infoRow(_ label: String, value: String?) -> some View {
        rowContent(label, value: value, accent: false, chevron: false)
    }

    private func rowContent(_ label: String, value: String?, accent: Bool, chevron: Bool) -> some View {
        HStack {
            Text(label)
                .font(Fonts.body(14.5))
                .foregroundStyle(Ink.fg)
            Spacer()
            if let value {
                Text(value)
                    .font(Fonts.body(13.5))
                    .foregroundStyle(accent ? Ink.accent : Ink.fgMuted)
            }
            if chevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Ink.fgFaint)
                    .padding(.leading, 8)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
        .contentShape(Rectangle())
    }

    private func toggleRow(_ label: String, binding: Binding<Bool>) -> some View {
        HStack {
            Text(label).font(Fonts.body(14.5)).foregroundStyle(Ink.fg)
            Spacer()
            Toggle("", isOn: binding)
                .labelsHidden()
                .tint(Ink.accent)
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
    }

    private func dangerRow(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .font(Fonts.body(14.5))
                    .foregroundStyle(Ink.accent)
                Spacer()
            }
            .padding(.horizontal, 16).padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func emergencyValue(for group: Group?) -> String {
        guard let group else { return "No group" }
        if let word = groupStore.emergencyOverrideWord(groupID: group.id), !word.isEmpty {
            return "Set"
        }
        return "Not set"
    }

    private func leaveSelectedGroup() {
        guard let group = groupStore.selectedGroup else { return }
        groupStore.deleteGroup(group.id)
        if groupStore.groups.isEmpty {
            onboarded = false
            screen = .onboarding
        } else {
            screen = .groups
        }
    }

    private func resetAllData() {
        groupStore.resetAllData()
        DrillService.clear()
        onboarded = false
        plainMode = false
        screen = .onboarding
    }
}
