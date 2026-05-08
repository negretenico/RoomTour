import { Text, View } from "react-native";
// Shared types and hooks are importable — full UI is a future issue
import type { GraphSnapshot } from "@roomtour/shared";

const _typecheck: GraphSnapshot = { nodes: [], edges: [], currentRoom: null };

export default function App() {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text>RoomTour Mobile — coming soon</Text>
    </View>
  );
}
