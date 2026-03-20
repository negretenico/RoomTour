# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RoomTour is a home drone butler — an autonomous drone that navigates through your home, identifies rooms, and provides AI-powered voice assistance. It integrates Claude API for natural language understanding, ElevenLabs for TTS, and Whisper for STT.

The project is built in 5 phases (see `Spec.md`):
1. **Software Simulation** — Gazebo + ROS2, no hardware needed
2. **Ground Robot Prototype** — Raspberry Pi 5, RealSense D435i, Nav2
3. **Drone Build** — PX4 on Pixhawk 6C, NVIDIA Jetson Orin Nano
4. **Autonomous Flight + Butler Brain** — Full SLAM, lifelog memory, companion app
5. **Polish** — Wake word, proactive briefings, charging dock, smart home

## Repository Structure

```
Assistant/     # Claude API butler brain, voice loop (STT → Claude → TTS), lifelog system
Drone/         # PX4/ROS2 flight control, SLAM, autonomous navigation
Recognition/   # Room classification from depth camera + semantic features
Server/        # Flask web app for butler testing without hardware
```



## Tech Stack

- **Language**: Python 3, C++ 20
- **Robotics**: ROS2 Humble, Gazebo Garden, Nav2, RTAB-Map (visual SLAM)
- **AI/Voice**: Claude API (`claude-sonnet-4-6`), OpenAI Whisper (local STT), ElevenLabs TTS
- **Hardware (later phases)**: Raspberry Pi 5 → NVIDIA Jetson Orin Nano, Intel RealSense D435i
- **Flight stack**: PX4 (communicates via ROS2 to the autonomy layer)
- **Companion app**: React Native or Flutter

## Development Philosophy

Build and test in software before hardware. Key emulation layers:
- **Simulation**: Swap a config flag to switch between real RealSense and Gazebo simulated camera — same Python nodes work for both.
- **Butler testing**: Use the Flask server (`Server/`) to simulate room context and test Claude prompts entirely independently of robotics.
- Ground robot validates all navigation logic before any drone flights.

## Key Architecture Decisions

- **Room detection**: Lightweight classifier using geometric signatures (ceiling height, room volume, dominant surfaces), not pixel-level vision models.
- **Pathfinding**: A* or RRT between rooms in Phase 1; Move Base Flex adapted for 3D flight in Phase 4.
- **Butler memory**: A "lifelog" structured context file fed to Claude on every interaction — includes calendar events, summarized emails, health data, manual notes.
- **Claude system prompt pattern**: `"You are [name]'s home drone butler. Here is their recent context: [lifelog]. Answer naturally, as if you've been with them."`
- **External APIs**: Google Calendar API (calendar), OpenWeatherMap (weather), ElevenLabs (TTS), Picovoice Porcupine (wake word in Phase 5).
