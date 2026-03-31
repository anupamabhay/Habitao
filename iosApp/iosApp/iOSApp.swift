import SwiftUI
import composeApp
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, ObservableObject {
    @Published var quickActionRoute: String?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        if let shortcutItem = launchOptions?[.shortcutItem] as? UIApplicationShortcutItem {
            quickActionRoute = mapQuickAction(shortcutItem.type)
            return false
        }

        return true
    }

    func application(
        _ application: UIApplication,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        quickActionRoute = mapQuickAction(shortcutItem.type)
        completionHandler(true)
    }

    private func mapQuickAction(_ type: String) -> String? {
        switch type {
        case "com.habitao.app.addTask":
            return "add_task"
        case "com.habitao.app.addHabit":
            return "add_habit"
        case "com.habitao.app.addRoutine":
            return "add_routine"
        case "com.habitao.app.globalSearch":
            return "global_search"
        default:
            return nil
        }
    }

    // This makes notifications show up even when the app is in the foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        KoinIosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(quickActionRoute: appDelegate.quickActionRoute)
        }
    }
}
