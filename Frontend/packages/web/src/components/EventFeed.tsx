import type { ServerMessage } from "@roomtour/shared";

interface Props {
  events: ServerMessage[];
}

function describe(msg: ServerMessage): string {
  if (msg.type === "ROOM_UPDATE") return `Room: ${msg.room}`;
  if (msg.type === "NAVIGATION_COMPLETED") return `Navigating to ${msg.destination}`;
  return "Unknown event";
}

export default function EventFeed({ events }: Props) {
  if (events.length === 0) {
    return <p className="empty">No events yet.</p>;
  }

  return (
    <ul className="event-feed">
      {events.map((e, i) => (
        <li key={i} className={`event event-${e.type.toLowerCase()}`}>
          {describe(e)}
        </li>
      ))}
    </ul>
  );
}
