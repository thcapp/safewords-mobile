import SwiftUI

struct ContentView: View {
    @Environment(GroupStore.self) private var groupStore
    @AppStorage("plainMode") private var plainMode: Bool = true
    @AppStorage("onboarded") private var onboarded: Bool = false
    @AppStorage("requireBiometrics") private var requireBiometrics: Bool = false
    @State private var screen: AppScreen = .home
    @State private var biometricUnlocked = false
    @State private var showingPlainSettings = false

    var body: some View {
        if requireBiometrics && !biometricUnlocked {
            BiometricGateView {
                biometricUnlocked = true
            }
        } else {
            ZStack(alignment: .top) {
                if plainMode && onboarded && !groupStore.groups.isEmpty && !showingPlainSettings {
                    PlainRoot {
                        showingPlainSettings = true
                        screen = .settings
                    }
                } else {
                    mainRoot
                        .onChange(of: screen) { _, newValue in
                            if newValue != .settings {
                                showingPlainSettings = false
                            }
                        }
                }

                if groupStore.demoMode {
                    DemoModeBanner {
                        groupStore.exitDemoMode()
                        onboarded = false
                        showingPlainSettings = false
                        screen = .onboarding
                    }
                    .accessibilityIdentifier("plain-home.demo-banner")
                    .padding(.horizontal, 14)
                    .padding(.top, 8)
                    .zIndex(10)
                }
            }
        }
    }

    private var mainRoot: some View {
        ZStack(alignment: .bottom) {
            Ink.bg.ignoresSafeArea()

            SwiftUI.Group {
                if !onboarded && groupStore.groups.isEmpty && screen != .qrScanner && screen != .recoveryPhrase {
                    OnboardingView(screen: $screen)
                        .onChange(of: screen) { _, new in
                            if new == .home, !groupStore.groups.isEmpty { onboarded = true }
                        }
                } else {
                    switch screen {
                    case .home:       HomeView(screen: $screen)
                    case .groups:     GroupsView(screen: $screen)
                    case .verify:     VerifyView(screen: $screen)
                    case .settings:   SettingsView(screen: $screen)
                    case .onboarding: OnboardingView(screen: $screen)
                    case .addMember:  QRDisplayView(screen: $screen)
                    case .recoveryBackup:
                        RecoveryBackupView(screen: $screen)
                    case .safetyCards:
                        SafetyCardsView(screen: $screen)
                    case .qrScanner:
                        QRScannerView(
                            onJoined: { group in
                                groupStore.selectedGroupID = group.id
                                onboarded = true
                                screen = .home
                            },
                            onCancel: {
                                screen = groupStore.groups.isEmpty ? .onboarding : .groups
                            },
                            onRecovery: {
                                screen = .recoveryPhrase
                            }
                        )
                    case .recoveryPhrase:
                        RecoveryPhraseView(screen: $screen)
                    case .drills:
                        DrillsView(screen: $screen)
                    }
                }
            }

            if tabBarShown {
                CustomTabBar(active: $screen)
                    .environment(groupStore)
                    .transition(.opacity)
            }
        }
        .preferredColorScheme(.dark)
    }

    private var tabBarShown: Bool {
        switch screen {
        case .onboarding, .addMember, .qrScanner, .recoveryPhrase, .recoveryBackup, .safetyCards, .drills: return false
        default: return onboarded || !groupStore.groups.isEmpty
        }
    }
}

private struct DemoModeBanner: View {
    let onSetup: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Demo group")
                    .font(Fonts.body(12, weight: .bold))
                    .foregroundStyle(Ink.accent)
                Text("This is not your real safeword group.")
                    .font(Fonts.body(11.5))
                    .foregroundStyle(Ink.fgMuted)
            }
            Spacer()
            Button("Set up real group", action: onSetup)
                .font(Fonts.body(12.5, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(Capsule().fill(Ink.accent))
        }
        .padding(.leading, 14)
        .padding(.trailing, 8)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(Ink.accent.opacity(0.6), lineWidth: 0.8))
                .shadow(color: .black.opacity(0.35), radius: 22, y: 8)
        )
    }
}

private struct BiometricGateView: View {
    let onUnlock: () -> Void
    @State private var failed = false

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            VStack(spacing: 18) {
                Image(systemName: "faceid")
                    .font(.system(size: 52, weight: .light))
                    .foregroundStyle(Ink.accent)
                Text("Unlock Safewords")
                    .font(Fonts.display(30))
                    .foregroundStyle(Ink.fg)
                Text(failed ? "Authentication failed. Try again." : "Use \(BiometricService.biometryName()) to open the app.")
                    .font(Fonts.body(14))
                    .foregroundStyle(failed ? Ink.warn : Ink.fgMuted)
                    .multilineTextAlignment(.center)
                Button("Unlock") {
                    Task { await unlock() }
                }
                .font(Fonts.body(15, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .padding(.horizontal, 24)
                .padding(.vertical, 14)
                .background(Capsule().fill(Ink.accent))
            }
            .padding(28)
        }
        .task { await unlock() }
        .preferredColorScheme(.dark)
    }

    private func unlock() async {
        let ok = await BiometricService.authenticate(reason: "Unlock Safewords")
        await MainActor.run {
            if ok {
                onUnlock()
            } else {
                failed = true
            }
        }
    }
}

#Preview {
    ContentView()
        .environment(GroupStore())
}
