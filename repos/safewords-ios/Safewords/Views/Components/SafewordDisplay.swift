import SwiftUI

/// Large formatted safeword phrase display.
/// Shows the phrase with capitalized first letters: "Breezy Rocket 75"
struct SafewordDisplay: View {
    let phrase: String

    /// Font size (default: large for home screen).
    var fontSize: Font = .title2

    /// Text color.
    var textColor: Color = .tealAccent

    var body: some View {
        Text(phrase)
            .font(fontSize.bold())
            .foregroundStyle(textColor)
            .multilineTextAlignment(.center)
            .lineLimit(2)
            .minimumScaleFactor(0.7)
    }
}

// MARK: - Variants

extension SafewordDisplay {
    /// Compact variant for list rows and widgets.
    static func compact(_ phrase: String) -> SafewordDisplay {
        SafewordDisplay(phrase: phrase, fontSize: .subheadline, textColor: .tealAccent)
    }

    /// Large variant for the home screen.
    static func large(_ phrase: String) -> SafewordDisplay {
        SafewordDisplay(phrase: phrase, fontSize: .title, textColor: .tealAccent)
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()

        VStack(spacing: 24) {
            SafewordDisplay.large("Breezy Rocket 75")
            SafewordDisplay(phrase: "Crimson Eagle 47")
            SafewordDisplay.compact("Golden Penguin 12")
        }
    }
}
