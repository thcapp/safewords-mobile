import SwiftUI

struct HomeView: View {
    @Environment(GroupStore.self) private var groupStore
    @State private var showCreateGroup = false
    @State private var newGroupName = ""
    @State private var newCreatorName = ""

    var body: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                if let group = groupStore.selectedGroup {
                    groupContent(group)
                } else {
                    emptyState
                }
            }
            .navigationTitle("Safewords")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if groupStore.groups.count > 1 {
                    ToolbarItem(placement: .topBarTrailing) {
                        groupPicker
                    }
                }
            }
            .sheet(isPresented: $showCreateGroup) {
                createGroupSheet
            }
        }
    }

    // MARK: - Group Content

    private func groupContent(_ group: Group) -> some View {
        VStack(spacing: 32) {
            Spacer()

            // Group name
            Text(group.name)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
                .tracking(2)

            // Countdown ring with safeword
            TimelineView(.periodic(from: .now, by: 1.0)) { context in
                let timestamp = context.date.timeIntervalSince1970
                let remaining = TOTPDerivation.getTimeRemaining(interval: group.interval.seconds, timestamp: timestamp)
                let progress = 1.0 - (remaining / Double(group.interval.seconds))

                VStack(spacing: 24) {
                    CountdownRing(progress: progress, interval: group.interval) {
                        VStack(spacing: 8) {
                            if let seed = groupStore.seed(for: group.id) {
                                let phrase = TOTPDerivation.deriveSafewordCapitalized(
                                    seed: seed,
                                    interval: group.interval.seconds,
                                    timestamp: timestamp
                                )
                                SafewordDisplay(phrase: phrase)
                            } else {
                                Text("No seed")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .frame(width: 280, height: 280)

                    // Countdown text
                    Text("Rotates in \(formatTimeRemaining(remaining))")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }

            // Interval badge
            Text(group.interval.displayName)
                .font(.caption)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.tealDark.opacity(0.3))
                .foregroundStyle(Color.tealAccent)
                .clipShape(Capsule())

            Spacer()
            Spacer()
        }
        .padding()
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "shield.lefthalf.filled")
                .font(.system(size: 64))
                .foregroundStyle(Color.tealAccent)

            Text("No Groups Yet")
                .font(.title2.bold())
                .foregroundStyle(.primary)

            Text("Create a family group to start\nsharing rotating safewords.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button(action: { showCreateGroup = true }) {
                Label("Create Group", systemImage: "plus.circle.fill")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(Color.amberCTA)
                    .clipShape(Capsule())
            }

            Spacer()
        }
    }

    // MARK: - Group Picker

    @ViewBuilder
    private var groupPicker: some View {
        @Bindable var store = groupStore
        Menu {
            ForEach(groupStore.groups) { group in
                Button(action: { groupStore.selectedGroupID = group.id }) {
                    HStack {
                        Text(group.name)
                        if group.id == groupStore.selectedGroupID {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        } label: {
            Image(systemName: "chevron.down.circle")
                .foregroundStyle(Color.tealAccent)
        }
    }

    // MARK: - Create Group Sheet

    private var createGroupSheet: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                VStack(spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Group Name")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        TextField("Family Name", text: $newGroupName)
                            .textFieldStyle(.roundedBorder)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Your Name")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        TextField("Your display name", text: $newCreatorName)
                            .textFieldStyle(.roundedBorder)
                    }

                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Create Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        showCreateGroup = false
                        newGroupName = ""
                        newCreatorName = ""
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        groupStore.createGroup(
                            name: newGroupName,
                            creatorName: newCreatorName
                        )
                        showCreateGroup = false
                        newGroupName = ""
                        newCreatorName = ""
                    }
                    .disabled(newGroupName.isEmpty || newCreatorName.isEmpty)
                    .tint(Color.amberCTA)
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Helpers

    private func formatTimeRemaining(_ seconds: TimeInterval) -> String {
        let totalSeconds = Int(seconds)
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let secs = totalSeconds % 60

        if hours > 0 {
            return String(format: "%02d:%02d:%02d", hours, minutes, secs)
        }
        return String(format: "%02d:%02d", minutes, secs)
    }
}

#Preview {
    HomeView()
        .environment(GroupStore())
}
