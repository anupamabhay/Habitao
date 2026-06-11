import WidgetKit
import SwiftUI

struct HabitaoSimpleEntry: TimelineEntry {
    let date: Date
}

struct HabitaoSimpleProvider: TimelineProvider {
    func placeholder(in context: Context) -> HabitaoSimpleEntry {
        HabitaoSimpleEntry(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (HabitaoSimpleEntry) -> Void) {
        completion(HabitaoSimpleEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HabitaoSimpleEntry>) -> Void) {
        let entry = HabitaoSimpleEntry(date: Date())
        let nextRefresh = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date()
        completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
    }
}

struct HabitaoWidgetEntryView: View {
    var entry: HabitaoSimpleProvider.Entry

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Today")
                .font(.headline)
            Text("Open Habitao to check habits and tasks")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(entry.date, style: .time)
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding()
    }
}

struct HabitaoWidget: Widget {
    let kind: String = "HabitaoWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HabitaoSimpleProvider()) { entry in
            HabitaoWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Habitao Today")
        .description("Quickly open your habits and tasks for today.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

#Preview(as: .systemSmall) {
    HabitaoWidget()
} timeline: {
    HabitaoSimpleEntry(date: Date())
}

