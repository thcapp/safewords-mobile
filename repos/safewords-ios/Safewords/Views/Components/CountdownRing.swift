import SwiftUI

// Dashed-dial countdown ring: 60 ticks (larger at each 5th), an ember
// progress arc, and a knob on the leading edge. Matches the design's
// CountdownRing primitive (tick spec is in the handoff bundle).
struct CountdownRing<Content: View>: View {
    let progress: Double
    @ViewBuilder let content: () -> Content

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let radius = size / 2 - 12
            let center = CGPoint(x: size / 2, y: size / 2)

            ring(radius: radius, center: center)
        }
    }

    private var clampedProgress: Double {
        min(max(progress, 0), 1)
    }

    private func ring(radius: CGFloat, center: CGPoint) -> some View {
        ZStack {
            ForEach(0..<60, id: \.self) { index in
                CountdownTick(index: index, progress: clampedProgress, radius: radius, center: center)
            }

            Circle()
                .trim(from: 0, to: CGFloat(clampedProgress))
                .stroke(Ink.accent, style: StrokeStyle(lineWidth: 1.5, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .frame(width: radius * 2, height: radius * 2)
                .position(x: center.x, y: center.y)

            CountdownKnob(progress: clampedProgress, radius: radius, center: center)

            content()
        }
    }
}

private struct CountdownTick: View {
    let index: Int
    let progress: Double
    let radius: CGFloat
    let center: CGPoint

    private var isMajor: Bool { index % 5 == 0 }
    private var angle: Double { Double(index) / 60.0 * 2 * .pi - .pi / 2 }
    private var innerRadius: CGFloat { radius - (isMajor ? 6.0 : 3.0) }
    private var outerRadius: CGFloat { radius + (isMajor ? 2.0 : 0.0) }
    private var elapsed: Bool { Double(index) / 60.0 < progress }
    private var lineWidth: CGFloat { isMajor ? 1.0 : 0.6 }

    var body: some View {
        Path { path in
            path.move(to: point(on: innerRadius))
            path.addLine(to: point(on: outerRadius))
        }
        .stroke(
            Ink.fgFaint.opacity(elapsed ? 0.9 : 0.25),
            style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
        )
    }

    private func point(on radius: CGFloat) -> CGPoint {
        CGPoint(
            x: center.x + CGFloat(cos(angle)) * radius,
            y: center.y + CGFloat(sin(angle)) * radius
        )
    }
}

private struct CountdownKnob: View {
    let progress: Double
    let radius: CGFloat
    let center: CGPoint

    private var angle: Double {
        progress * 2 * .pi - .pi / 2
    }

    private var position: CGPoint {
        CGPoint(
            x: center.x + CGFloat(cos(angle)) * radius,
            y: center.y + CGFloat(sin(angle)) * radius
        )
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Ink.accent.opacity(0.25), lineWidth: 1)
                .frame(width: 16, height: 16)
            Circle()
                .fill(Ink.accent)
                .frame(width: 10, height: 10)
        }
        .position(x: position.x, y: position.y)
    }
}

#Preview {
    ZStack {
        Ink.bg.ignoresSafeArea()
        CountdownRing(progress: 0.013) {
            VStack(spacing: 14) {
                SectionLabel(text: "● LIVE · JOHNSON FAMILY", color: Ink.accent)
                VStack(spacing: 2) {
                    Text("Crimson").font(Fonts.display(46)).foregroundStyle(Ink.fg)
                    Text("Anchor").font(Fonts.display(46)).foregroundStyle(Ink.fg)
                    Text("47").font(Fonts.display(46)).foregroundStyle(Ink.fg)
                }
                Text("SEQ · 0142")
                    .font(Fonts.mono(11))
                    .tracking(1.5)
                    .foregroundStyle(Ink.fgFaint)
            }
        }
        .frame(width: 340, height: 340)
    }
}
