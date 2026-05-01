import SwiftUI
import UIKit

@MainActor
enum CardRenderer {
    static let cardSize = CGSize(width: 612, height: 792)

    static func render<Content: View>(_ content: Content) -> UIImage? {
        let renderer = ImageRenderer(
            content: content
                .frame(width: cardSize.width, height: cardSize.height)
                .background(Color.white)
        )
        renderer.scale = 2
        return renderer.uiImage
    }

    static func printImage(_ image: UIImage, jobName: String) {
        let printInfo = UIPrintInfo(dictionary: nil)
        printInfo.outputType = .general
        printInfo.jobName = jobName

        let controller = UIPrintInteractionController.shared
        controller.printInfo = printInfo
        controller.printingItem = image
        controller.present(animated: true)
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
