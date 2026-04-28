import SwiftUI

// Solid circle with a capped initial inside.
struct GroupDot: View {
    let initial: String
    let color: Color
    var size: CGFloat = 36

    var body: some View {
        ZStack {
            Circle().fill(color)
            Text(initial)
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundStyle(.white)
        }
        .frame(width: size, height: size)
    }
}

// Palette used for group/member avatars in the design.
enum DotPalette {
    static let red    = Color(hex: "#E8553A")
    static let blue   = Color(hex: "#6E94E7")
    static let green  = Color(hex: "#9DBF8A")
    static let orange = Color(hex: "#E89B5E")
    static let purple = Color(hex: "#B47AE8")

    static func forIndex(_ i: Int) -> Color {
        let wheel = [red, blue, green, orange, purple]
        return wheel[((i % wheel.count) + wheel.count) % wheel.count]
    }
}
