#!/bin/bash
# Start the Phase 1 simulation environment.
# Run this at the beginning of every dev session (after VcXsrv is running on Windows).
set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# ── Environment ──────────────────────────────────────────────────────────────
source /opt/ros/jazzy/setup.bash
source "$REPO_ROOT/venv/bin/activate"
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0.0
export LIBGL_ALWAYS_INDIRECT=0

echo "=== RoomTour Simulation ==="
echo "ROS2: $(ros2 --version 2>&1 | head -1)"
echo "Gazebo: $(gz sim --version 2>&1 | head -1)"
echo ""

# ── Launch Gazebo (background) ───────────────────────────────────────────────
echo "Starting Gazebo..."
gz sim empty.sdf &
GZ_PID=$!

# ── Launch ROS-GZ bridge (background) ────────────────────────────────────────
sleep 3  # give Gazebo a moment to start
echo "Starting ROS-GZ bridge..."
ros2 run ros_gz_bridge parameter_bridge /clock@rosgraph_msgs/msg/Clock[gz.msgs.Clock &
BRIDGE_PID=$!

# ── Launch RViz2 (background) ─────────────────────────────────────────────────
sleep 2
echo "Starting RViz2..."
rviz2 &
RVIZ_PID=$!

echo ""
echo "All services running. Press Ctrl+C to stop."
echo "  Gazebo PID:  $GZ_PID"
echo "  Bridge PID:  $BRIDGE_PID"
echo "  RViz2 PID:   $RVIZ_PID"

# ── Cleanup on exit ───────────────────────────────────────────────────────────
trap "echo ''; echo 'Shutting down...'; kill $GZ_PID $BRIDGE_PID $RVIZ_PID 2>/dev/null; exit 0" SIGINT SIGTERM
wait
