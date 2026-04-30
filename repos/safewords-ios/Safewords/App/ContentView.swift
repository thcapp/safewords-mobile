import SwiftUI

struct ContentView: View {
    @Environment(GroupStore.self) private var groupStore
    @AppStorage("plainMode") private var plainMode: Bool = false
    @AppStorage("onboarded") private var onboarded: Bool = false
    @AppStorage("requireBiometrics") private var requireBiometrics: Bool = false
    @State private var screen: AppScreen = .home
    @State private var biometricUnlocked = false

    var body: some View {
        if requireBiometrics && !biometricUnlocked {
            BiometricGateView {
                biometricUnlocked = true
            }
        } else {
            if plainMode {
                PlainRoot()
            } else {
                mainRoot
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
                    .transition(.opacity)
            }
        }
        .preferredColorScheme(.dark)
    }

    private var tabBarShown: Bool {
        switch screen {
        case .onboarding, .addMember, .qrScanner, .recoveryPhrase, .drills: return false
        default: return onboarded || !groupStore.groups.isEmpty
        }
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
