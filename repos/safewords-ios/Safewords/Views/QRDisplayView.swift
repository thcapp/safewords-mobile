import SwiftUI

struct QRDisplayView: View {
    @Environment(\.dismiss) private var dismiss

    let group: Group
    let seed: Data

    @State private var timeRemaining: Int = 60
    @State private var timer: Timer?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                VStack(spacing: 32) {
                    Spacer()

                    // Warning banner
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.shield.fill")
                            .foregroundStyle(Color.amberCTA)
                        Text("Only share this QR code in person")
                            .font(.subheadline.bold())
                            .foregroundStyle(Color.amberCTA)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.amberCTA.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 10))

                    // Group name
                    Text(group.name)
                        .font(.title3.bold())

                    // QR code image
                    if let qrImage = QRCodeService.generateQRCode(for: group, seed: seed, size: 250) {
                        Image(uiImage: qrImage)
                            .interpolation(.none)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 250, height: 250)
                            .padding(16)
                            .background(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    } else {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color.cardBackground)
                            .frame(width: 250, height: 250)
                            .overlay {
                                Text("Failed to generate QR code")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                    }

                    // Info
                    VStack(spacing: 4) {
                        Text("Rotation: \(group.interval.displayName)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)

                        Text("Have the other person scan this code\nwith their Safewords app.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }

                    // Auto-dismiss countdown
                    Text("Auto-closes in \(timeRemaining)s")
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.secondary)

                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Invite Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .tint(Color.tealAccent)
                }
            }
            .onAppear { startTimer() }
            .onDisappear { stopTimer() }
        }
    }

    // MARK: - Timer

    private func startTimer() {
        timeRemaining = 60
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if timeRemaining > 0 {
                timeRemaining -= 1
            } else {
                stopTimer()
                dismiss()
            }
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

#Preview {
    QRDisplayView(
        group: Group(name: "Preview Family", interval: .daily),
        seed: TOTPDerivation.generateSeed()
    )
}
