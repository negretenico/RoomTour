import { fromAsync, type Result, type ChatResponse } from "./types";

export function sendMessage(
  message: string,
  sessionId?: string
): Promise<Result<ChatResponse>> {
  return fromAsync(
    fetch("/api/v1/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, sessionId }),
    }).then((r) => {
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      return r.json() as Promise<ChatResponse>;
    })
  );
}
