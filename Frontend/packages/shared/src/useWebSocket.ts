import { useEffect, useRef } from "react";
import type { ServerMessage } from "./types";

export function useWebSocket(
  url: string,
  onMessage: (msg: ServerMessage) => void
) {
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    const ws = new WebSocket(url);

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data) as ServerMessage;
        onMessageRef.current(msg);
      } catch {
        // ignore malformed frames
      }
    };

    ws.onerror = () => ws.close();

    return () => ws.close();
  }, [url]);
}
