import SwiftUI
import composeApp

@main
struct iOSApp: App {

    init() {
        KoinIosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
