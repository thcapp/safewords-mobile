// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Safewords",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "SafewordsCore",
            targets: ["SafewordsCore"]
        )
    ],
    targets: [
        // Core library (crypto + models) that can be shared between app and widget
        .target(
            name: "SafewordsCore",
            dependencies: [],
            path: "Safewords",
            exclude: ["App", "Views"],
            resources: [
                .process("Data/adjectives.json"),
                .process("Data/nouns.json")
            ]
        ),
        .testTarget(
            name: "SafewordsTests",
            dependencies: ["SafewordsCore"],
            path: "SafewordsTests"
        )
    ]
)
