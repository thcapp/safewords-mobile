import SwiftUI

private let settingsSharedDefaults = UserDefaults(suiteName: KeychainService.appGroupID)

struct SettingsView: View {
    @Environment(GroupStore.self) private var groupStore
    @Binding var screen: AppScreen

    @AppStorage("plainMode") private var plainMode: Bool = true
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
    @State private var showPrimitivesSheet = false
    @State private var emergencyWord = ""

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header.padding(.horizontal, 20).padding(.top, 62)

                    let group = groupStore.selectedGroup
                    section(label: "View") {
                        toggleRow("Use Plain home by default", binding: $plainMode, identifier: "settings.toggle-plain-mode")
                        divider
                        infoRow("Advanced view", value: plainMode ? "Off" : "On")
                    }

                    section(label: "Rotation · \(group?.name ?? "No group")", identifier: "settings.section-rotation") {
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
                        toggleRow("Hold to reveal word", binding: revealBinding)
                    }

                    section(label: "Group", identifier: "settings.section-verification") {
                        actionRow("Primitives", value: primitivesValue(for: group)) {
                            showPrimitivesSheet = true
                        }
                        .disabled(group == nil)
                        divider
                        actionRow("Safety cards", value: group == nil ? "No group" : "Print", identifier: "settings.action-safety-cards") {
                            screen = .safetyCards
                        }
                        .disabled(group == nil)
                    }

                    section(label: "Widget & Lock Screen") {
                        actionRow("Home screen widget", value: "Instructions") { showWidgetInfo = true }
                        divider
                        toggleRow("Lock screen glance", binding: $lockScreenGlance)
                        divider
                        toggleRow("Hide word until unlock", binding: $hideWordUntilUnlock)
                    }

                    section(label: "Security", identifier: "settings.section-security") {
                        toggleRow("Require \(BiometricService.biometryName()) to open", binding: biometricBinding, identifier: "settings.toggle-biometrics")
                        divider
                        actionRow("Emergency override word", value: emergencyValue(for: group), identifier: "settings.action-reveal-override") {
                            emergencyWord = group.flatMap { groupStore.emergencyOverrideWord(groupID: $0.id) } ?? ""
                            showEmergencySheet = true
                        }
                        .disabled(group == nil)
                        divider
                        infoRow("Rotate group seed", value: "Later", identifier: "settings.action-rotate-seed")
                        divider
                        actionRow("Back up seed phrase", value: group == nil ? "No group" : "24 words", identifier: "settings.action-recovery-backup") {
                            screen = .recoveryBackup
                        }
                        .disabled(group == nil)
                    }

                    section(label: "Practice") {
                        actionRow("Run a scam drill", value: "Now", identifier: "settings.action-drill") { screen = .drills }
                        divider
                        actionRow("Drill history", value: "\(DrillService.sessions().count) saved") { screen = .drills }
                    }

                    section(label: "Danger zone", identifier: "settings.section-danger") {
                        dangerRow("Leave this group", identifier: "settings.action-leave-group") { showLeaveConfirmation = true }
                            .disabled(group == nil)
                        divider
                        dangerRow("Reset device", identifier: "settings.action-reset-data") { showResetConfirmation = true }
                    }

                    VStack(spacing: 4) {
                        Text("Safewords v1.3.1 · Offline-first")
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
        .sheet(isPresented: $showPrimitivesSheet) {
            PrimitiveSettingsSheet()
                .environment(groupStore)
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

    private func section<Content: View>(label: String, identifier: String? = nil, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if let identifier {
                SectionLabel(text: label)
                    .accessibilityIdentifier(identifier)
                    .padding(.horizontal, 20)
                    .padding(.top, 22)
                    .padding(.bottom, 8)
            } else {
                SectionLabel(text: label)
                    .padding(.horizontal, 20)
                    .padding(.top, 22)
                    .padding(.bottom, 8)
            }
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

    @ViewBuilder
    private func actionRow(_ label: String, value: String? = nil, identifier: String? = nil, action: @escaping () -> Void) -> some View {
        if let identifier {
            Button(action: action) {
                rowContent(label, value: value, accent: false, chevron: true)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier(identifier)
        } else {
            Button(action: action) {
                rowContent(label, value: value, accent: false, chevron: true)
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func infoRow(_ label: String, value: String?, identifier: String? = nil) -> some View {
        if let identifier {
            rowContent(label, value: value, accent: false, chevron: false)
                .accessibilityIdentifier(identifier)
        } else {
            rowContent(label, value: value, accent: false, chevron: false)
        }
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

    private func toggleRow(_ label: String, binding: Binding<Bool>, identifier: String? = nil) -> some View {
        HStack {
            Text(label).font(Fonts.body(14.5)).foregroundStyle(Ink.fg)
            Spacer()
            if let identifier {
                Toggle("", isOn: binding)
                    .labelsHidden()
                    .tint(Ink.accent)
                    .accessibilityIdentifier(identifier)
            } else {
                Toggle("", isOn: binding)
                    .labelsHidden()
                    .tint(Ink.accent)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
    }

    @ViewBuilder
    private func dangerRow(_ label: String, identifier: String? = nil, action: @escaping () -> Void) -> some View {
        if let identifier {
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
            .accessibilityIdentifier(identifier)
        } else {
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
    }

    private func emergencyValue(for group: Group?) -> String {
        guard let group else { return "No group" }
        if let word = groupStore.emergencyOverrideWord(groupID: group.id), !word.isEmpty {
            return "Set"
        }
        return "Not set"
    }

    private func primitivesValue(for group: Group?) -> String {
        guard let group else { return "No group" }
        var enabled: [String] = []
        if group.primitives.rotatingWord.wordFormat == .numeric {
            enabled.append("Digits")
        }
        if group.primitives.staticOverride.enabled {
            enabled.append("Override")
        }
        if group.primitives.challengeAnswer.enabled {
            enabled.append("Challenge")
        }
        return enabled.isEmpty ? "Word" : enabled.joined(separator: ", ")
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
        plainMode = true
        screen = .onboarding
    }
}

private struct PrimitiveSettingsSheet: View {
    @Environment(GroupStore.self) private var groupStore
    @Environment(\.dismiss) private var dismiss

    private var group: Group? {
        groupStore.selectedGroup
    }

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 18) {
                Capsule().fill(Ink.rule).frame(width: 42, height: 4).frame(maxWidth: .infinity)
                SectionLabel(text: "Group primitives")
                Text(group?.name ?? "No group")
                    .font(Fonts.display(30))
                    .tracking(-0.9)
                    .foregroundStyle(Ink.fg)

                if let group {
                    VStack(spacing: 0) {
                        primitiveToggle(
                            "Numeric word format",
                            subtitle: "Show a 6-digit code instead of words.",
                            rowIdentifier: "settings.toggle-numeric",
                            toggleIdentifier: "primitives-sheet.toggle-numeric",
                            isOn: Binding(
                                get: { groupStore.selectedGroup?.primitives.rotatingWord.wordFormat == .numeric },
                                set: { enabled in
                                    groupStore.setWordFormat(groupID: group.id, format: enabled ? .numeric : .adjectiveNounNumber)
                                }
                            )
                        )
                        divider
                        primitiveToggle(
                            "Static override",
                            subtitle: "A fixed emergency phrase derived from the group seed.",
                            rowIdentifier: "settings.toggle-static-override",
                            toggleIdentifier: "primitives-sheet.toggle-static-override",
                            isOn: Binding(
                                get: { groupStore.selectedGroup?.primitives.staticOverride.enabled == true },
                                set: { groupStore.setStaticOverrideEnabled(groupID: group.id, enabled: $0) }
                            )
                        )
                        divider
                        primitiveToggle(
                            "Challenge / answer",
                            subtitle: "Use a deterministic challenge table and match sheet.",
                            rowIdentifier: "settings.toggle-challenge-answer",
                            toggleIdentifier: "primitives-sheet.toggle-challenge-answer",
                            isOn: Binding(
                                get: { groupStore.selectedGroup?.primitives.challengeAnswer.enabled == true },
                                set: { groupStore.setChallengeAnswerEnabled(groupID: group.id, enabled: $0) }
                            )
                        )
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .fill(Ink.bgElev)
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Ink.rule, lineWidth: 0.5))
                    )

                    Text("Derived secrets are not stored in group metadata. They are recomputed from the seed when shown or printed.")
                        .font(Fonts.body(12.5))
                        .foregroundStyle(Ink.fgMuted)
                        .lineSpacing(3)
                } else {
                    Text("Create or join a group before enabling primitives.")
                        .font(Fonts.body(14))
                        .foregroundStyle(Ink.fgMuted)
                }

                Spacer()
                Button("Done") { dismiss() }
                    .font(Fonts.body(14, weight: .semibold))
                    .foregroundStyle(Ink.accentInk)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Capsule().fill(Ink.accent))
                    .accessibilityIdentifier("primitives-sheet.done")
            }
            .padding(22)
        }
    }

    private var divider: some View {
        Rectangle().fill(Ink.rule).frame(height: 0.5).padding(.leading, 16)
    }

    private func primitiveToggle(
        _ title: String,
        subtitle: String,
        rowIdentifier: String,
        toggleIdentifier: String,
        isOn: Binding<Bool>
    ) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(Fonts.body(14.5, weight: .semibold))
                    .foregroundStyle(Ink.fg)
                Text(subtitle)
                    .font(Fonts.body(12.5))
                    .foregroundStyle(Ink.fgMuted)
                    .lineSpacing(2)
            }
            Spacer()
            Toggle("", isOn: isOn)
                .labelsHidden()
                .tint(Ink.accent)
                .accessibilityIdentifier(toggleIdentifier)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .accessibilityIdentifier(rowIdentifier)
    }
}
