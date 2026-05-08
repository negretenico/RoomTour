import { fromAsync, err, type Result, type ChatResponse } from "./types";

export async function sendMessage(
  message: string,
  sessionId?: string
): Promise<Result<ChatResponse>> {
  const response = await fromAsync(
    fetch("/api/v1/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, sessionId }),
    })
  );

  if (!response.ok) return response;
  if (!response.value.ok) return err(`HTTP ${response.value.status}`);

  return fromAsync(response.value.json() as Promise<ChatResponse>);
}
