import SwiftUI

struct SettingsView: View {
    @Environment(GroupStore.self) private var groupStore
    @State private var defaultInterval: RotationInterval = .daily
    @State private var showResetConfirmation = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                List {
                    // Default interval
                    Section {
                        Picker("Default Rotation", selection: $defaultInterval) {
                            ForEach(RotationInterval.allCases) { interval in
                                Text(interval.displayName).tag(interval)
                            }
                        }
                        .listRowBackground(Color.cardBackground)
                    } header: {
                        Text("Defaults")
                    } footer: {
                        Text("New groups will use this rotation interval by default.")
                    }

                    // Security section
                    Section {
                        HStack {
                            Label("Seed Storage", systemImage: "lock.shield")
                            Spacer()
                            Text("Keychain")
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(Color.cardBackground)

                        HStack {
                            Label("Algorithm", systemImage: "function")
                            Spacer()
                            Text("HMAC-SHA256")
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(Color.cardBackground)

                        HStack {
                            Label("Network Required", systemImage: "wifi.slash")
                            Spacer()
                            Text("Never")
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(Color.cardBackground)
                    } header: {
                        Text("Security")
                    }

                    // About section
                    Section {
                        HStack {
                            Label("Version", systemImage: "info.circle")
                            Spacer()
                            Text("1.0.0")
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(Color.cardBackground)

                        HStack {
                            Label("Groups", systemImage: "person.3")
                            Spacer()
                            Text("\(groupStore.groups.count)")
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(Color.cardBackground)

                        Link(destination: URL(string: "https://safewords.io")!) {
                            HStack {
                                Label("Website", systemImage: "globe")
                                    .foregroundStyle(.primary)
                                Spacer()
                                Image(systemName: "arrow.up.forward.square")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .listRowBackground(Color.cardBackground)
                    } header: {
                        Text("About")
                    }

                    // Reset section
                    Section {
                        Button(action: { showResetConfirmation = true }) {
                            HStack {
                                Spacer()
                                Label("Delete All Data", systemImage: "exclamationmark.triangle")
                                    .foregroundStyle(.red)
                                Spacer()
                            }
                        }
                        .listRowBackground(Color.cardBackground)
                    } footer: {
                        Text("This will delete all groups, seeds, and settings. This cannot be undone.")
                    }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Settings")
            .alert("Delete All Data", isPresented: $showResetConfirmation) {
                Button("Delete Everything", role: .destructive) {
                    resetAllData()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This will permanently delete all groups and seeds from this device. You will need to re-scan QR codes to rejoin groups.")
            }
        }
    }

    private func resetAllData() {
        let groupIDs = groupStore.groups.map(\.id)
        for id in groupIDs {
            groupStore.deleteGroup(id)
        }
        KeychainService.deleteAllSeeds()
    }
}

#Preview {
    SettingsView()
        .environment(GroupStore())
}
