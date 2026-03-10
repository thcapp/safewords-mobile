import SwiftUI

struct ContentView: View {
    @Environment(GroupStore.self) private var groupStore

    var body: some View {
        TabView {
            Tab("Home", systemImage: "shield.fill") {
                HomeView()
            }

            Tab("Groups", systemImage: "person.3.fill") {
                GroupsView()
            }

            Tab("Settings", systemImage: "gearshape.fill") {
                SettingsView()
            }
        }
        .tint(Color.tealAccent)
    }
}

// MARK: - Color Constants

extension Color {
    static let tealAccent = Color(hex: "#2dd4bf")
    static let tealDark = Color(hex: "#0f766e")
    static let amberCTA = Color(hex: "#d97706")
    static let darkBackground = Color(hex: "#0a0a0a")
    static let cardBackground = Color(hex: "#1a1a1a")
    static let cardBorder = Color(hex: "#2a2a2a")

    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)

        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}

#Preview {
    ContentView()
        .environment(GroupStore())
}
