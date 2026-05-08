// ---------------------------------------------------------------------------
// Result monad — functional error handling without throw/catch in business logic
// ---------------------------------------------------------------------------

export type Result<T, E = string> =
  | { ok: true; value: T }
  | { ok: false; error: E };

export function ok<T>(value: T): Result<T, never> {
  return { ok: true, value };
}

export function err<E>(error: E): Result<never, E> {
  return { ok: false, error };
}

/** Wraps any Promise in a Result — the only place try/catch should appear. */
export async function fromAsync<T>(p: Promise<T>): Promise<Result<T>> {
  try {
    return ok(await p);
  } catch (e) {
    return err(e instanceof Error ? e.message : String(e));
  }
}

export function mapResult<T, U, E>(
  result: Result<T, E>,
  fn: (value: T) => U
): Result<U, E> {
  return result.ok ? ok(fn(result.value)) : result;
}

// ---------------------------------------------------------------------------

export type ServerMessage =
  | { type: "ROOM_UPDATE"; room: string }
  | { type: "NAVIGATION_COMPLETED"; destination: string };

export interface RoomNode {
  id: string;
  label: string;
}

export interface RoomEdge {
  id: string;
  source: string;
  target: string;
}

export interface GraphSnapshot {
  nodes: RoomNode[];
  edges: RoomEdge[];
  currentRoom: string | null;
}

export interface ChatResponse {
  response: string;
  sessionId: string;
}
