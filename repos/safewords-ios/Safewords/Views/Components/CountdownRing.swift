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
            let r = size / 2 - 12
            let cx = size / 2
            let cy = size / 2

            ZStack {
                // 60 ticks
                ForEach(0..<60, id: \.self) { i in
                    let big = i % 5 == 0
                    let a = Double(i) / 60.0 * 2 * .pi - .pi / 2
                    let r1 = r - (big ? 6 : 3)
                    let r2 = r + (big ? 2 : 0)
                    let elapsed = Double(i) / 60.0 < progress
                    Path { p in
                        p.move(to: CGPoint(x: cx + cos(a) * r1, y: cy + sin(a) * r1))
                        p.addLine(to: CGPoint(x: cx + cos(a) * r2, y: cy + sin(a) * r2))
                    }
                    .stroke(
                        Ink.fgFaint.opacity(elapsed ? 0.9 : 0.25),
                        style: StrokeStyle(lineWidth: big ? 1 : 0.6, lineCap: .round)
                    )
                }

                // Progress arc
                Circle()
                    .trim(from: 0, to: CGFloat(min(max(progress, 0), 1)))
                    .stroke(Ink.accent, style: StrokeStyle(lineWidth: 1.5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: r * 2, height: r * 2)
                    .position(x: cx, y: cy)

                // Knob
                let angle = progress * 2 * .pi - .pi / 2
                Circle()
                    .fill(Ink.accent)
                    .frame(width: 10, height: 10)
                    .position(x: cx + cos(angle) * r, y: cy + sin(angle) * r)
                Circle()
                    .stroke(Ink.accent.opacity(0.25), lineWidth: 1)
                    .frame(width: 16, height: 16)
                    .position(x: cx + cos(angle) * r, y: cy + sin(angle) * r)

                content()
            }
        }
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
