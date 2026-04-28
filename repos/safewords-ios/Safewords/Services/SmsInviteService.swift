import MessageUI
import SwiftUI
import UIKit

enum SmsInviteService {
    static func inviteText(group: Group, seed: Data?) -> String {
        let seedText = seed.map(RecoveryPhraseService.displayCode(for:)) ?? "open Safewords and scan the QR in person"
        return "Safewords invite for \(group.name): open Safewords, choose Join with recovery phrase, and enter \(seedText). Only share this with trusted family."
    }

    static func fallbackURL(body: String) -> URL? {
        var components = URLComponents()
        components.scheme = "sms"
        components.path = ""
        components.queryItems = [URLQueryItem(name: "body", value: body)]
        return components.url
    }
}

struct MessageComposeView: UIViewControllerRepresentable {
    let body: String
    let onFinish: () -> Void

    func makeUIViewController(context: Context) -> MFMessageComposeViewController {
        let controller = MFMessageComposeViewController()
        controller.body = body
        controller.messageComposeDelegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: MFMessageComposeViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onFinish: onFinish)
    }

    final class Coordinator: NSObject, MFMessageComposeViewControllerDelegate {
        let onFinish: () -> Void

        init(onFinish: @escaping () -> Void) {
            self.onFinish = onFinish
        }

        func messageComposeViewController(
            _ controller: MFMessageComposeViewController,
            didFinishWith result: MessageComposeResult
        ) {
            controller.dismiss(animated: true)
            onFinish()
        }
    }
}
