import SwiftUI
import AVFoundation

struct QRScannerView: View {
    @Environment(GroupStore.self) private var groupStore
    @Environment(\.dismiss) private var dismiss

    @State private var scannedPayload: QRCodeService.ParsedGroup?
    @State private var scanError: String?
    @State private var memberName = ""
    @State private var showNamePrompt = false
    @State private var isTorchOn = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.darkBackground.ignoresSafeArea()

                if showNamePrompt, let payload = scannedPayload {
                    namePromptView(payload: payload)
                } else {
                    scannerView
                }
            }
            .navigationTitle("Join Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    // MARK: - Scanner View

    private var scannerView: some View {
        VStack(spacing: 24) {
            Spacer()

            // Camera preview placeholder with instructions
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.cardBackground)
                    .frame(width: 280, height: 280)

                QRCameraPreview(
                    onCodeScanned: handleScannedCode,
                    isTorchOn: isTorchOn
                )
                .frame(width: 280, height: 280)
                .clipShape(RoundedRectangle(cornerRadius: 16))

                // Scanning overlay
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(Color.tealAccent, lineWidth: 2)
                    .frame(width: 200, height: 200)
            }

            Text("Point your camera at a\nSafewords QR code")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            if let error = scanError {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(.horizontal)
            }

            // Torch toggle
            Button(action: { isTorchOn.toggle() }) {
                Label(
                    isTorchOn ? "Light On" : "Light Off",
                    systemImage: isTorchOn ? "flashlight.on.fill" : "flashlight.off.fill"
                )
                .font(.subheadline)
                .foregroundStyle(isTorchOn ? Color.amberCTA : .secondary)
            }

            Spacer()
        }
    }

    // MARK: - Name Prompt

    private func namePromptView(payload: QRCodeService.ParsedGroup) -> some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 48))
                .foregroundStyle(Color.tealAccent)

            Text("Group Found")
                .font(.title2.bold())

            Text(payload.name)
                .font(.headline)
                .foregroundStyle(Color.tealAccent)

            Text("Rotation: \(payload.interval.displayName)")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 8) {
                Text("Your Name")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                TextField("Enter your name", text: $memberName)
                    .textFieldStyle(.roundedBorder)
            }
            .padding(.horizontal, 32)

            Button(action: joinGroup) {
                Text("Join Group")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(Color.amberCTA)
                    .clipShape(Capsule())
            }
            .disabled(memberName.isEmpty)

            Spacer()
        }
    }

    // MARK: - Actions

    private func handleScannedCode(_ code: String) {
        if let parsed = QRCodeService.parseQRCode(code) {
            scannedPayload = parsed
            scanError = nil
            showNamePrompt = true
        } else {
            scanError = "Invalid QR code. Make sure it's a Safewords group code."
        }
    }

    private func joinGroup() {
        guard let payload = scannedPayload else { return }
        groupStore.joinGroup(
            name: payload.name,
            seed: payload.seed,
            interval: payload.interval,
            memberName: memberName
        )
        dismiss()
    }
}

// MARK: - Camera Preview (AVFoundation)

struct QRCameraPreview: UIViewRepresentable {
    let onCodeScanned: (String) -> Void
    let isTorchOn: Bool

    func makeUIView(context: Context) -> QRCameraUIView {
        let view = QRCameraUIView(onCodeScanned: onCodeScanned)
        return view
    }

    func updateUIView(_ uiView: QRCameraUIView, context: Context) {
        uiView.setTorch(isTorchOn)
    }
}

class QRCameraUIView: UIView, AVCaptureMetadataOutputObjectsDelegate {
    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private let onCodeScanned: (String) -> Void
    private var hasScanned = false

    init(onCodeScanned: @escaping (String) -> Void) {
        self.onCodeScanned = onCodeScanned
        super.init(frame: .zero)
        setupCamera()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }

    private func setupCamera() {
        let session = AVCaptureSession()
        captureSession = session

        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else {
            return
        }

        if session.canAddInput(input) {
            session.addInput(input)
        }

        let output = AVCaptureMetadataOutput()
        if session.canAddOutput(output) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.qr]
        }

        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        layer.addSublayer(previewLayer)
        self.previewLayer = previewLayer

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
        }
    }

    func setTorch(_ on: Bool) {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else { return }
        try? device.lockForConfiguration()
        device.torchMode = on ? .on : .off
        device.unlockForConfiguration()
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard !hasScanned,
              let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              object.type == .qr,
              let value = object.stringValue else {
            return
        }

        hasScanned = true
        captureSession?.stopRunning()

        // Haptic feedback
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)

        onCodeScanned(value)
    }

    deinit {
        captureSession?.stopRunning()
    }
}

#Preview {
    QRScannerView()
        .environment(GroupStore())
}
