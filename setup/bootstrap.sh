#!/bin/bash
# Bootstrap script for RoomTour Phase 1 dev environment
# Target: Ubuntu 24.04 (WSL2), ROS2 Jazzy, Gazebo Harmonic
set -e

echo "=== RoomTour Phase 1 Bootstrap ==="
echo "Ubuntu: $(lsb_release -ds)"
echo ""

# ── 1. Locale ────────────────────────────────────────────────────────────────
echo "[1/6] Setting locale..."
sudo apt update -q
sudo apt install -y locales
sudo locale-gen en_US en_US.UTF-8
sudo update-locale LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8
export LANG=en_US.UTF-8

# ── 2. ROS2 Jazzy ────────────────────────────────────────────────────────────
echo "[2/6] Installing ROS2 Jazzy..."
sudo apt install -y software-properties-common curl

# Add ROS2 apt repo
sudo curl -sSL https://raw.githubusercontent.com/ros/rosdistro/master/ros.key \
  -o /usr/share/keyrings/ros-archive-keyring.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/ros-archive-keyring.gpg] \
  http://packages.ros.org/ros2/ubuntu $(. /etc/os-release && echo $UBUNTU_CODENAME) main" \
  | sudo tee /etc/apt/sources.list.d/ros2.list > /dev/null

sudo apt update -q
sudo apt upgrade -y
sudo apt install -y ros-jazzy-desktop

# Source ROS2 in .bashrc if not already there
if ! grep -q "source /opt/ros/jazzy/setup.bash" ~/.bashrc; then
  echo "source /opt/ros/jazzy/setup.bash" >> ~/.bashrc
fi
source /opt/ros/jazzy/setup.bash

# ── 3. Gazebo Harmonic ───────────────────────────────────────────────────────
echo "[3/6] Installing Gazebo Harmonic..."
sudo curl https://packages.osrfoundation.org/gazebo.gpg \
  --output /usr/share/keyrings/pkgs-osrf-archive-keyring.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/pkgs-osrf-archive-keyring.gpg] \
  http://packages.osrfoundation.org/gazebo/ubuntu-stable $(lsb_release -cs) main" \
  | sudo tee /etc/apt/sources.list.d/gazebo-stable.list > /dev/null

sudo apt update -q
sudo apt install -y gz-harmonic

# ── 4. ROS2 ↔ Gazebo bridge ──────────────────────────────────────────────────
echo "[4/6] Installing ROS-GZ bridge..."
sudo apt install -y ros-jazzy-ros-gz

# ── 5. Display (VcXsrv) ──────────────────────────────────────────────────────
echo "[5/6] Configuring display for VcXsrv..."
DISPLAY_LINE='export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '"'"'{print $2}'"'"'):0.0'
LIBGL_LINE='export LIBGL_ALWAYS_INDIRECT=0'

if ! grep -q "LIBGL_ALWAYS_INDIRECT" ~/.bashrc; then
  echo "" >> ~/.bashrc
  echo "# VcXsrv display forwarding (WSL2 + Windows 10)" >> ~/.bashrc
  echo "$DISPLAY_LINE" >> ~/.bashrc
  echo "$LIBGL_LINE" >> ~/.bashrc
fi

# ── 6. Python venv ───────────────────────────────────────────────────────────
echo "[6/6] Setting up Python venv..."
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

python3 -m venv venv
venv/bin/pip install --upgrade pip -q
venv/bin/pip install -r requirements.txt

echo ""
echo "=== Bootstrap complete ==="
echo ""
echo "Next steps:"
echo "  1. Launch VcXsrv on Windows (XLaunch → Multiple windows → Disable access control)"
echo "  2. Open a new WSL2 terminal (to pick up .bashrc changes)"
echo "  3. Run: bash setup/verify.sh"
