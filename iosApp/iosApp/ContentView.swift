import SwiftUI
import composeApp

struct ContentView: View {
    let quickActionRoute: String?

    var body: some View {
        ComposeView(quickActionRoute: quickActionRoute)
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let quickActionRoute: String?

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(quickActionRoute: quickActionRoute)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
