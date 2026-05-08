import { useState, useRef, useEffect } from "react";
import { sendMessage } from "@roomtour/shared";

interface Message {
  role: "user" | "butler";
  text: string;
}

export default function ChatPanel() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [sessionId, setSessionId] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function submit() {
    const text = input.trim();
    if (!text || loading) return;

    setInput("");
    setMessages((prev) => [...prev, { role: "user", text }]);
    setLoading(true);

    const result = await sendMessage(text, sessionId);
    setLoading(false);

    if (result.ok) {
      setSessionId(result.value.sessionId);
      setMessages((prev) => [...prev, { role: "butler", text: result.value.response }]);
    } else {
      setMessages((prev) => [...prev, { role: "butler", text: `Error: ${result.error}` }]);
    }
  }

  return (
    <div className="chat">
      <div className="chat-messages">
        {messages.map((m, i) => (
          <div key={i} className={`message ${m.role}`}>
            <span className="message-role">{m.role === "user" ? "You" : "Jeeves"}</span>
            <p>{m.text}</p>
          </div>
        ))}
        {loading && <div className="message butler"><p>...</p></div>}
        <div ref={bottomRef} />
      </div>
      <div className="chat-input">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="Ask Jeeves..."
          disabled={loading}
        />
        <button onClick={submit} disabled={loading}>Send</button>
      </div>
    </div>
  );
}
