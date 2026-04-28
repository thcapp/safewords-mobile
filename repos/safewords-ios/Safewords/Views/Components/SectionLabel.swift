import SwiftUI

// Uppercase eyebrow label — tracks 1.4px and uses muted foreground.
struct SectionLabel: View {
    let text: String
    var color: Color = Ink.fgMuted

    var body: some View {
        Text(text.uppercased())
            .font(Fonts.body(11, weight: .medium))
            .tracking(1.4)
            .foregroundStyle(color)
    }
}
