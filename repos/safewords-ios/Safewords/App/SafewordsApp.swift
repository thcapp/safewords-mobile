import SwiftUI

@main
struct SafewordsApp: App {
    @State private var groupStore = GroupStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(groupStore)
                .preferredColorScheme(.dark)
        }
    }
}
