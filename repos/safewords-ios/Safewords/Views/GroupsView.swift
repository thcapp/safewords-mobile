import SwiftUI

struct GroupsView: View {
    @Environment(GroupStore.self) private var groupStore
    @State private var showCreateGroup = false
    @State private var showJoinGroup = false
    @State private var newGroupName = ""
    @State private var newCreatorName = ""

    var body: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                if groupStore.groups.isEmpty {
                    emptyGroupsList
                } else {
                    groupsList
                }
            }
            .navigationTitle("Groups")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button(action: { showCreateGroup = true }) {
                            Label("Create Group", systemImage: "plus.circle")
                        }
                        Button(action: { showJoinGroup = true }) {
                            Label("Join Group", systemImage: "qrcode.viewfinder")
                        }
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(Color.tealAccent)
                    }
                }
            }
            .sheet(isPresented: $showCreateGroup) {
                createGroupSheet
            }
            .sheet(isPresented: $showJoinGroup) {
                QRScannerView()
            }
        }
    }

    // MARK: - Groups List

    private var groupsList: some View {
        List {
            ForEach(groupStore.groups) { group in
                NavigationLink(destination: GroupDetailView(group: group)) {
                    groupRow(group)
                }
                .listRowBackground(Color.cardBackground)
            }
            .onDelete(perform: deleteGroups)
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
    }

    private func groupRow(_ group: Group) -> some View {
        TimelineView(.periodic(from: .now, by: 1.0)) { context in
            let timestamp = context.date.timeIntervalSince1970
            let remaining = TOTPDerivation.getTimeRemaining(interval: group.interval.seconds, timestamp: timestamp)

            HStack(spacing: 16) {
                // Group icon
                ZStack {
                    Circle()
                        .fill(Color.tealDark.opacity(0.3))
                        .frame(width: 44, height: 44)
                    Image(systemName: "shield.fill")
                        .foregroundStyle(Color.tealAccent)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(group.name)
                        .font(.headline)

                    if let seed = groupStore.seed(for: group.id) {
                        let phrase = TOTPDerivation.deriveSafewordCapitalized(
                            seed: seed,
                            interval: group.interval.seconds,
                            timestamp: timestamp
                        )
                        Text(phrase)
                            .font(.subheadline)
                            .foregroundStyle(Color.tealAccent)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text(group.interval.shortLabel)
                        .font(.caption2)
                        .foregroundStyle(.secondary)

                    Text(formatCompactTime(remaining))
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.vertical, 4)
        }
    }

    // MARK: - Empty State

    private var emptyGroupsList: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "person.3")
                .font(.system(size: 48))
                .foregroundStyle(Color.tealAccent.opacity(0.5))

            Text("No groups yet")
                .font(.title3)
                .foregroundStyle(.secondary)

            HStack(spacing: 16) {
                Button(action: { showCreateGroup = true }) {
                    Label("Create", systemImage: "plus.circle.fill")
                        .font(.subheadline.bold())
                        .foregroundStyle(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.amberCTA)
                        .clipShape(Capsule())
                }

                Button(action: { showJoinGroup = true }) {
                    Label("Join", systemImage: "qrcode.viewfinder")
                        .font(.subheadline.bold())
                        .foregroundStyle(Color.tealAccent)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.tealDark.opacity(0.3))
                        .clipShape(Capsule())
                }
            }

            Spacer()
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

    // MARK: - Actions

    private func deleteGroups(at offsets: IndexSet) {
        for index in offsets {
            groupStore.deleteGroup(groupStore.groups[index].id)
        }
    }

    private func formatCompactTime(_ seconds: TimeInterval) -> String {
        let totalSeconds = Int(seconds)
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let secs = totalSeconds % 60

        if hours > 0 {
            return String(format: "%dh %02dm", hours, minutes)
        }
        return String(format: "%d:%02d", minutes, secs)
    }
}

#Preview {
    GroupsView()
        .environment(GroupStore())
}
