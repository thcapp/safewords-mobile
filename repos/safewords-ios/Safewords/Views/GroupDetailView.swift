import SwiftUI

struct GroupDetailView: View {
    @Environment(GroupStore.self) private var groupStore
    @Environment(\.dismiss) private var dismiss

    let group: Group

    @State private var editedName: String = ""
    @State private var selectedInterval: RotationInterval = .daily
    @State private var showQRCode = false
    @State private var showDeleteConfirmation = false
    @State private var showAddMember = false
    @State private var newMemberName = ""

    private var currentGroup: Group {
        groupStore.groups.first { $0.id == group.id } ?? group
    }

    var body: some View {
        ZStack {
            Color.darkBackground.ignoresSafeArea()

            List {
                // Current safeword section
                safewordSection

                // Members section
                membersSection

                // Settings section
                settingsSection

                // Invite section
                inviteSection

                // Danger zone
                deleteSection
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
        }
        .navigationTitle(currentGroup.name)
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            editedName = currentGroup.name
            selectedInterval = currentGroup.interval
        }
        .sheet(isPresented: $showQRCode) {
            if let seed = groupStore.seed(for: currentGroup.id) {
                QRDisplayView(group: currentGroup, seed: seed)
            }
        }
        .sheet(isPresented: $showAddMember) {
            addMemberSheet
        }
        .alert("Delete Group", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                groupStore.deleteGroup(currentGroup.id)
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete \"\(currentGroup.name)\" and its seed. This action cannot be undone.")
        }
    }

    // MARK: - Sections

    private var safewordSection: some View {
        Section {
            TimelineView(.periodic(from: .now, by: 1.0)) { context in
                let timestamp = context.date.timeIntervalSince1970
                let remaining = TOTPDerivation.getTimeRemaining(
                    interval: currentGroup.interval.seconds,
                    timestamp: timestamp
                )

                VStack(spacing: 12) {
                    if let seed = groupStore.seed(for: currentGroup.id) {
                        let phrase = TOTPDerivation.deriveSafewordCapitalized(
                            seed: seed,
                            interval: currentGroup.interval.seconds,
                            timestamp: timestamp
                        )
                        Text(phrase)
                            .font(.title2.bold())
                            .foregroundStyle(Color.tealAccent)
                    }

                    HStack {
                        Image(systemName: "clock")
                            .font(.caption)
                        Text("Rotates in \(formatTime(remaining))")
                            .font(.caption)
                    }
                    .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }
            .listRowBackground(Color.cardBackground)
        } header: {
            Text("Current Safeword")
        }
    }

    private var membersSection: some View {
        Section {
            ForEach(currentGroup.members) { member in
                HStack(spacing: 12) {
                    // Avatar circle
                    ZStack {
                        Circle()
                            .fill(memberColor(for: member))
                            .frame(width: 36, height: 36)
                        Text(member.initials)
                            .font(.caption.bold())
                            .foregroundStyle(.white)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(member.name)
                            .font(.body)
                        Text(member.role.displayName)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()
                }
                .listRowBackground(Color.cardBackground)
            }
            .onDelete(perform: deleteMembers)

            Button(action: { showAddMember = true }) {
                Label("Add Member", systemImage: "person.badge.plus")
                    .foregroundStyle(Color.tealAccent)
            }
            .listRowBackground(Color.cardBackground)
        } header: {
            Text("Members (\(currentGroup.members.count))")
        }
    }

    private var settingsSection: some View {
        Section {
            HStack {
                Text("Group Name")
                Spacer()
                TextField("Name", text: $editedName)
                    .multilineTextAlignment(.trailing)
                    .foregroundStyle(.secondary)
                    .onSubmit {
                        if !editedName.isEmpty {
                            groupStore.updateGroupName(currentGroup.id, name: editedName)
                        }
                    }
            }
            .listRowBackground(Color.cardBackground)

            Picker("Rotation", selection: $selectedInterval) {
                ForEach(RotationInterval.allCases) { interval in
                    Text(interval.displayName).tag(interval)
                }
            }
            .onChange(of: selectedInterval) { _, newValue in
                groupStore.updateGroupInterval(currentGroup.id, interval: newValue)
            }
            .listRowBackground(Color.cardBackground)
        } header: {
            Text("Settings")
        }
    }

    private var inviteSection: some View {
        Section {
            Button(action: { showQRCode = true }) {
                HStack {
                    Label("Invite Member", systemImage: "qrcode")
                        .foregroundStyle(Color.tealAccent)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .listRowBackground(Color.cardBackground)
        } header: {
            Text("Share")
        } footer: {
            Text("Show the QR code to a family member in person to share the group seed securely.")
        }
    }

    private var deleteSection: some View {
        Section {
            Button(action: { showDeleteConfirmation = true }) {
                HStack {
                    Spacer()
                    Label("Delete Group", systemImage: "trash")
                        .foregroundStyle(.red)
                    Spacer()
                }
            }
            .listRowBackground(Color.cardBackground)
        }
    }

    // MARK: - Add Member Sheet

    private var addMemberSheet: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                VStack(spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Member Name")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        TextField("Name", text: $newMemberName)
                            .textFieldStyle(.roundedBorder)
                    }
                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Add Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        showAddMember = false
                        newMemberName = ""
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        let member = Member(name: newMemberName, role: .member)
                        groupStore.addMember(member, toGroup: currentGroup.id)
                        showAddMember = false
                        newMemberName = ""
                    }
                    .disabled(newMemberName.isEmpty)
                    .tint(Color.amberCTA)
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Helpers

    private func deleteMembers(at offsets: IndexSet) {
        for index in offsets {
            let member = currentGroup.members[index]
            groupStore.removeMember(member.id, fromGroup: currentGroup.id)
        }
    }

    private func memberColor(for member: Member) -> Color {
        let colors: [Color] = [
            .tealAccent, .amberCTA, .purple, .pink,
            .orange, .cyan, .mint, .indigo
        ]
        return colors[member.colorIndex % colors.count]
    }

    private func formatTime(_ seconds: TimeInterval) -> String {
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
    NavigationStack {
        GroupDetailView(group: Group(
            name: "Preview Family",
            interval: .daily,
            members: [
                Member(name: "Alice", role: .creator),
                Member(name: "Bob", role: .member)
            ]
        ))
    }
    .environment(GroupStore())
}
