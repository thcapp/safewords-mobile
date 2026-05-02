import SwiftUI
import AVFoundation
import UIKit

struct QRScannerView: View {
    @Environment(GroupStore.self) private var groupStore
    @Environment(\.dismiss) private var dismiss

    var onJoined: ((Group) -> Void)?
    var onCancel: (() -> Void)?
    var onRecovery: (() -> Void)?

    @State private var authorization = AVCaptureDevice.authorizationStatus(for: .video)
    @State private var scannerID = UUID()
    @State private var scannedPayload: QRCodeService.ParsedGroup?
    @State private var scanError: String?
    @State private var memberName = ""
    @State private var showNamePrompt = false
    @State private var isTorchOn = false

    var body: some View {
        NavigationStack {
            ZStack {
                Ink.bg.ignoresSafeArea()
                content
            }
            .navigationTitle("Join Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { cancel() }
                        .accessibilityIdentifier("qr-scanner.cancel")
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if showNamePrompt, let payload = scannedPayload {
            namePromptView(payload: payload)
        } else {
            switch authorization {
            case .authorized:
                scannerView
            case .notDetermined:
                permissionView
            case .denied, .restricted:
                deniedView
            @unknown default:
                deniedView
            }
        }
    }

    private var permissionView: some View {
        VStack(spacing: 18) {
            Spacer()
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 52, weight: .light))
                .foregroundStyle(Ink.accent)
            Text("Scan a Safewords QR")
                .font(Fonts.display(28))
                .foregroundStyle(Ink.fg)
            Text("Camera access is used only to scan the invite code on this device.")
                .font(Fonts.body(14))
                .foregroundStyle(Ink.fgMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .frame(maxWidth: 280)
            Button("Enable camera") {
                requestCameraAccess()
            }
            .font(Fonts.body(15, weight: .semibold))
            .foregroundStyle(Ink.accentInk)
            .padding(.horizontal, 24)
            .padding(.vertical, 14)
            .background(Capsule().fill(Ink.accent))
            .accessibilityIdentifier("qr-scanner.permission-cta")
            Spacer()
        }
        .padding(24)
    }

    private var deniedView: some View {
        VStack(spacing: 18) {
            Spacer()
            Image(systemName: "camera.badge.ellipsis")
                .font(.system(size: 52, weight: .light))
                .foregroundStyle(Ink.warn)
            Text("Camera is off")
                .font(Fonts.display(28))
                .foregroundStyle(Ink.fg)
            Text("Allow camera access in iOS Settings, or join with a recovery code instead.")
                .font(Fonts.body(14))
                .foregroundStyle(Ink.fgMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .frame(maxWidth: 280)
            Button("Use recovery code") {
                if let onRecovery {
                    onRecovery()
                } else {
                    dismiss()
                }
            }
                .font(Fonts.body(15, weight: .semibold))
                .foregroundStyle(Ink.accentInk)
                .padding(.horizontal, 24)
                .padding(.vertical, 14)
                .background(Capsule().fill(Ink.accent))
                .accessibilityIdentifier("qr-scanner.recovery-fallback")
            Spacer()
        }
        .padding(24)
    }

    private var scannerView: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Ink.bgElev)
                    .frame(width: 280, height: 280)

                QRCameraPreview(
                    onCodeScanned: handleScannedCode,
                    isTorchOn: isTorchOn
                )
                .id(scannerID)
                .frame(width: 280, height: 280)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .accessibilityIdentifier("qr-scanner.preview")

                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(Ink.accent, lineWidth: 2)
                    .frame(width: 200, height: 200)
            }

            Text("Point your camera at a\nSafewords QR code")
                .font(Fonts.body(15, weight: .medium))
                .foregroundStyle(Ink.fgMuted)
                .multilineTextAlignment(.center)

            if let scanError {
                Text(scanError)
                    .font(Fonts.body(12.5))
                    .foregroundStyle(Ink.warn)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Button(action: { isTorchOn.toggle() }) {
                Label(
                    isTorchOn ? "Light On" : "Light Off",
                    systemImage: isTorchOn ? "flashlight.on.fill" : "flashlight.off.fill"
                )
                .font(Fonts.body(14, weight: .medium))
                .foregroundStyle(isTorchOn ? Ink.accent : Ink.fgMuted)
            }

            Spacer()
        }
    }

    private func namePromptView(payload: QRCodeService.ParsedGroup) -> some View {
        VStack(spacing: 22) {
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 48))
                .foregroundStyle(Ink.accent)

            Text("Group found")
                .font(Fonts.display(28))
                .foregroundStyle(Ink.fg)

            Text(payload.name)
                .font(Fonts.body(18, weight: .semibold))
                .foregroundStyle(Ink.accent)

            Text("Rotation: \(payload.interval.displayName)")
                .font(Fonts.body(13))
                .foregroundStyle(Ink.fgMuted)

            VStack(alignment: .leading, spacing: 8) {
                Text("Your name")
                    .font(Fonts.body(12, weight: .medium))
                    .foregroundStyle(Ink.fgMuted)
                TextField("", text: $memberName, prompt: Text("Alex").foregroundColor(Ink.fgFaint))
                    .font(Fonts.body(17, weight: .medium))
                    .foregroundStyle(Ink.fg)
                    .tint(Ink.accent)
                Rectangle().fill(Ink.rule).frame(height: 1)
            }
            .padding(.horizontal, 32)

            if let scanError {
                Text(scanError)
                    .font(Fonts.body(12.5))
                    .foregroundStyle(Ink.warn)
            }

            Button(action: joinGroup) {
                Text("Join Group")
                    .font(Fonts.body(15, weight: .semibold))
                    .foregroundStyle(memberName.isEmpty ? Ink.fgMuted : Ink.accentInk)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(Capsule().fill(memberName.isEmpty ? Ink.bgInset : Ink.accent))
            }
            .disabled(memberName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            Spacer()
        }
    }

    private func requestCameraAccess() {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                authorization = granted ? .authorized : .denied
            }
        }
    }

    private func handleScannedCode(_ code: String) {
        if let parsed = QRCodeService.parseQRCode(code) {
            scannedPayload = parsed
            scanError = nil
            showNamePrompt = true
        } else {
            scanError = "Invalid QR code. Make sure it's a Safewords group code."
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                scannerID = UUID()
            }
        }
    }

    private func joinGroup() {
        guard let payload = scannedPayload else { return }
        guard let group = groupStore.joinGroup(
            name: payload.name,
            seed: payload.seed,
            interval: payload.interval,
            memberName: memberName.trimmingCharacters(in: .whitespacesAndNewlines)
        ) else {
            scanError = "Could not save this group seed. Try again."
            return
        }
        groupStore.selectedGroupID = group.id
        if let onJoined {
            onJoined(group)
        } else {
            dismiss()
        }
    }

    private func cancel() {
        if let onCancel {
            onCancel()
        } else {
            dismiss()
        }
    }
}

// MARK: - Camera Preview (AVFoundation)

struct QRCameraPreview: UIViewRepresentable {
    let onCodeScanned: (String) -> Void
    let isTorchOn: Bool

    func makeUIView(context: Context) -> QRCameraUIView {
        QRCameraUIView(onCodeScanned: onCodeScanned)
    }

    func updateUIView(_ uiView: QRCameraUIView, context: Context) {
        uiView.setTorch(isTorchOn)
    }
}

final class QRCameraUIView: UIView, AVCaptureMetadataOutputObjectsDelegate {
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
