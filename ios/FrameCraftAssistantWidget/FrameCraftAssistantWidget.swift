import WidgetKit
import SwiftUI

/// A single-purpose widget tile: tapping it opens the main app straight
/// into the listening screen (via the framecraftassistant:// URL scheme
/// handled in FrameCraftAssistantApp). Works on the Home Screen, Lock
/// Screen, and StandBy (the charging-dock "screensaver" mode) — StandBy
/// support comes for free from the `.systemSmall`/accessory families
/// below; no extra code needed, iOS surfaces any widget there.
struct FrameCraftAssistantWidget: Widget {
    let kind = "FrameCraftAssistantWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            FrameCraftWidgetView(entry: entry)
        }
        .configurationDisplayName("FrameCraft Assistant")
        .description("Tap to talk to your FrameCraft assistant.")
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular])
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> Entry { Entry(date: .now) }

    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) {
        completion(Entry(date: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        // Static tile — nothing to refresh, so one entry that never expires.
        completion(Timeline(entries: [Entry(date: .now)], policy: .never))
    }
}

struct Entry: TimelineEntry {
    let date: Date
}

struct FrameCraftWidgetView: View {
    var entry: Entry

    var body: some View {
        Link(destination: URL(string: "framecraftassistant://listen")!) {
            VStack(spacing: 6) {
                Image(systemName: "mic.circle.fill")
                    .font(.system(size: 28))
                Text("Ask")
                    .font(.caption.weight(.semibold))
            }
        }
        .containerBackground(.fill.tertiary, for: .widget)
    }
}

#Preview(as: .systemSmall) {
    FrameCraftAssistantWidget()
} timeline: {
    Entry(date: .now)
}
