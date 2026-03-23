#!/bin/bash
# Verify all Phase 1 environment deliverables are met.
# Run this after bootstrap.sh completes and VcXsrv is running.
set -e

PASS=0
FAIL=0

check() {
  local label="$1"
  local cmd="$2"
  if eval "$cmd" &>/dev/null; then
    echo "  ✓ $label"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $label"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== RoomTour Phase 1 — Environment Verification ==="
echo ""

# 1. ROS2 Jazzy
echo "[1] ROS2 Jazzy"
source /opt/ros/jazzy/setup.bash 2>/dev/null || true
check "ros2 CLI works" "ros2 topic list"
check "RViz2 binary exists" "which rviz2"

# 2. Gazebo Harmonic
echo ""
echo "[2] Gazebo Harmonic"
check "gz CLI works" "gz sim --version"

# 3. ROS-GZ bridge
echo ""
echo "[3] ROS-GZ Bridge"
check "bridge package installed" "ros2 pkg list | grep ros_gz_bridge"

# 4. RViz2 (display)
echo ""
echo "[4] Display / RViz2"
if [ -z "$DISPLAY" ]; then
  echo "  ✗ DISPLAY not set — is VcXsrv running and .bashrc sourced?"
  FAIL=$((FAIL + 1))
else
  check "DISPLAY is set ($DISPLAY)" "true"
  check "rviz2 binary launchable" "which rviz2"
fi

# 5. Python venv + dependencies
echo ""
echo "[5] Python venv"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENV="$REPO_ROOT/venv"

if [ ! -d "$VENV" ]; then
  echo "  ✗ venv not found — run bootstrap.sh first"
  FAIL=$((FAIL + 1))
else
  source "$VENV/bin/activate"
  check "anthropic importable" "python -c 'import anthropic'"
  check "whisper importable" "python -c 'import whisper'"
  check "requests importable" "python -c 'import requests'"
  check "dotenv importable" "python -c 'import dotenv'"
fi

# Summary
echo ""
echo "================================"
echo "  Passed: $PASS  |  Failed: $FAIL"
echo "================================"

if [ "$FAIL" -gt 0 ]; then
  echo "Some checks failed. Review output above."
  exit 1
else
  echo "All checks passed. Phase 1 environment is ready."
fi
