import { useMemo } from "react";
import {
  ReactFlow,
  type Node,
  type Edge,
  Background,
  Controls,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import type { GraphSnapshot } from "@roomtour/shared";

interface Props {
  graph: GraphSnapshot;
}

function circularLayout(
  snapshot: GraphSnapshot
): { nodes: Node[]; edges: Edge[] } {
  const { nodes, edges, currentRoom } = snapshot;
  const radius = 180;
  const cx = 250;
  const cy = 200;
  const count = nodes.length;

  const flowNodes: Node[] = nodes.map((n, i) => {
    const angle = count > 1 ? (2 * Math.PI * i) / count - Math.PI / 2 : 0;
    return {
      id: n.id,
      position: {
        x: cx + radius * Math.cos(angle),
        y: cy + radius * Math.sin(angle),
      },
      data: { label: n.label },
      style: n.id === currentRoom
        ? { background: "#4f46e5", color: "#fff", borderColor: "#3730a3", fontWeight: 600 }
        : undefined,
    };
  });

  const flowEdges: Edge[] = edges.map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
  }));

  return { nodes: flowNodes, edges: flowEdges };
}

export default function GraphPanel({ graph }: Props) {
  const { nodes, edges } = useMemo(() => circularLayout(graph), [graph]);

  if (nodes.length === 0) {
    return <p className="empty">No rooms mapped yet. Use voice to build the graph.</p>;
  }

  return (
    <div className="graph-container">
      <ReactFlow nodes={nodes} edges={edges} fitView>
        <Background />
        <Controls />
      </ReactFlow>
    </div>
  );
}
