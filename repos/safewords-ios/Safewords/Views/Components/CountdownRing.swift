import SwiftUI

/// Circular progress ring showing time remaining until the next safeword rotation.
struct CountdownRing<Content: View>: View {
    let progress: Double
    let interval: RotationInterval
    @ViewBuilder let content: () -> Content

    /// Ring line width.
    private let lineWidth: CGFloat = 8

    var body: some View {
        ZStack {
            // Background ring
            Circle()
                .stroke(Color.tealDark.opacity(0.3), lineWidth: lineWidth)

            // Progress ring
            Circle()
                .trim(from: 0, to: CGFloat(min(progress, 1.0)))
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: [
                            Color.tealAccent,
                            Color.tealDark,
                            Color.tealAccent
                        ]),
                        center: .center,
                        startAngle: .degrees(0),
                        endAngle: .degrees(360)
                    ),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.linear(duration: 1.0), value: progress)

            // Glow dot at the leading edge
            GeometryReader { geometry in
                let radius = min(geometry.size.width, geometry.size.height) / 2
                let angle = Angle.degrees(360 * progress - 90)
                let x = radius + radius * CGFloat(cos(angle.radians)) - lineWidth / 2
                let y = radius + radius * CGFloat(sin(angle.radians)) - lineWidth / 2

                Circle()
                    .fill(Color.tealAccent)
                    .frame(width: lineWidth, height: lineWidth)
                    .shadow(color: Color.tealAccent.opacity(0.6), radius: 4)
                    .position(x: x + lineWidth / 2, y: y + lineWidth / 2)
            }

            // Center content
            content()
        }
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()

        CountdownRing(progress: 0.65, interval: .daily) {
            VStack(spacing: 8) {
                Text("Breezy Rocket 75")
                    .font(.title3.bold())
                    .foregroundStyle(Color.tealAccent)
            }
        }
        .frame(width: 280, height: 280)
    }
}
