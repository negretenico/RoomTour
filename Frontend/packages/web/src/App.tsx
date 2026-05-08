import { useEffect, useState } from "react";
import { useWebSocket, type GraphSnapshot, type ServerMessage } from "@roomtour/shared";
import GraphPanel from "./components/GraphPanel";
import ChatPanel from "./components/ChatPanel";
import EventFeed from "./components/EventFeed";

const WS_URL = `ws://${window.location.host}/ws`;

export default function App() {
  const [graph, setGraph] = useState<GraphSnapshot>({ nodes: [], edges: [], currentRoom: null });
  const [events, setEvents] = useState<ServerMessage[]>([]);

  useEffect(() => {
    fetch("/api/v1/graph")
      .then((r) => r.json())
      .then((data: GraphSnapshot) => setGraph(data))
      .catch(console.error);
  }, []);

  useWebSocket(WS_URL, (msg) => {
    setEvents((prev) => [msg, ...prev].slice(0, 50));

    if (msg.type === "ROOM_UPDATE") {
      setGraph((prev) => ({ ...prev, currentRoom: msg.room }));
    }
  });

  return (
    <div className="app">
      <div className="panel">
        <h2>Room Graph</h2>
        <GraphPanel graph={graph} />
      </div>
      <div className="panel">
        <h2>Chat</h2>
        <ChatPanel />
      </div>
      <div className="panel">
        <h2>Live Events</h2>
        <EventFeed events={events} />
      </div>
    </div>
  );
}
