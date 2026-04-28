import SwiftUI

// Ink theme tokens — the default visual direction from the design handoff.
// Editorial, near-mono, with a single ember accent.

enum Ink {
    static let bg       = Color(hex: "#0B0B0C")
    static let bgElev   = Color(hex: "#151517")
    static let bgInset  = Color(hex: "#1C1C1F")
    static let fg       = Color(hex: "#F5F2EC")
    static let fgMuted  = Color(hex: "#F5F2EC").opacity(0.55)
    static let fgFaint  = Color(hex: "#F5F2EC").opacity(0.32)
    static let rule     = Color(hex: "#F5F2EC").opacity(0.08)
    static let accent   = Color(hex: "#E8553A")
    static let accentInk = Color(hex: "#0B0B0C")
    static let ok       = Color(hex: "#9DBF8A")
    static let warn     = Color(hex: "#E8A13A")
    static let tickFill = Color(hex: "#E8553A").opacity(0.18)
}

// High-visibility palette for the Plain / a11y mode.
enum A11Y {
    static let bg        = Color(hex: "#0B1220")
    static let bgElev    = Color(hex: "#18243C")
    static let bgInset   = Color(hex: "#24354F")
    static let fg        = Color.white
    static let fgMuted   = Color(hex: "#CBD5E1")
    static let fgFaint   = Color(hex: "#94A3B8")
    static let rule      = Color.white.opacity(0.22)
    static let accent    = Color(hex: "#FFD23F")
    static let accentInk = Color(hex: "#0B1220")
    static let ok        = Color(hex: "#4ADE80")
    static let danger    = Color(hex: "#FF6B6B")
    static let tickFill  = Color(hex: "#FFD23F").opacity(0.22)
}

// Font families. SwiftUI maps these through custom font registrations — for now
// we fall back to system where the design family isn't registered.
enum Fonts {
    // Editorial display — prefer Fraunces if registered, else a serif system font.
    static func display(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        if UIFont(name: "Fraunces", size: size) != nil {
            return .custom("Fraunces", size: size).weight(weight)
        }
        return .system(size: size, weight: weight, design: .serif)
    }

    static func body(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight)
    }

    static func mono(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .monospaced)
    }
}

extension Color {
    init(hex: String) {
        var hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        if hex.count == 3 {
            hex = hex.map { "\($0)\($0)" }.joined()
        }
        var rgb: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&rgb)
        let r = Double((rgb & 0xFF0000) >> 16) / 255.0
        let g = Double((rgb & 0x00FF00) >> 8) / 255.0
        let b = Double(rgb & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}
