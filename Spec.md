Phase 1 — Software Simulation (Weeks 1–4, ~$0)
The philosophy here is: build everything in software first. Every mistake is free in simulation.
You don't need a drone, or any hardware, to validate the core logic — room detection, pathfinding, voice interaction, and the Claude API integration. Test the butler entirely through a Spring Boot REST web UI that simulates "I am in the kitchen, it is 8am, here is my lifelog" and iterate on the prompt and memory system completely independently of any robotics work.

Deliverables:

Butler brain (roomtour-assistant module): Claude system prompt + lifelog context — COMPLETE
Spring Boot REST server (roomtour-server module): web UI + REST API for hardware-free testing
Room classifier Spring service (roomtour-recognition module): geometric signatures, config-driven simulation mode
A* pathfinding between rooms using a YAML-defined floor plan graph
Voice loop: javax.sound.sampled mic capture → Whisper STT (local HTTP) → Claude API → ElevenLabs TTS
External API integrations: Google Calendar API + OpenWeatherMap, refreshed on schedule, with YAML fallback

Tools: Java 21, Spring Boot 3.3.6, Maven multi-module, Claude API (claude-sonnet-4-6), Anthropic Java SDK, ElevenLabs API, Whisper HTTP server (faster-whisper or whisper.cpp)

Phase 2 — Ground Robot Prototype (Weeks 5–10, ~$200)
Don't start with a drone. Start with a wheeled robot that uses identical sensors and software. A ground robot is safer, cheaper to crash, and lets you validate SLAM and room navigation in your actual physical home before adding flight complexity.
What to buy:
Part | Purpose | Cost
Raspberry Pi 5 (8GB) | Main compute | $80
Intel RealSense D435i | Depth camera + IMU | $60 used
2-wheel robot chassis kit | Mobility platform | $30
L298N motor driver | Motor control | $8
Mini Bluetooth speaker | Voice output | $15
USB microphone | Voice input | $10

Software stack on the Pi: ROS2 + RTAB-Map (visual SLAM) + Nav2 (autonomous navigation) + Phase 1 Java butler system. Java connects to ROS2 via rosbridge WebSocket server — swap butler.ros2.enabled=true, not code.

Deliverables:

Robot drives through your actual home and builds a real 2D occupancy map via RTAB-Map
Room classification on real depth data via RtabRoomClassifier (replaces ConfiguredRoomClassifier)
"Take me to the bedroom" → Java pathfinder → Nav2 goal → robot navigates there autonomously
Butler announces arrival and answers anything via Claude with real calendar and weather context

Phase 3 — Drone Frame Build (Weeks 11–20, ~$800)
Once your logic works on the ground robot, you transplant it onto a drone. This is the longest phase because flight safety requires patience.
Frame and flight hardware to buy:
Part | Purpose | Cost
DJI F450 frame | Sturdy 450mm quad | $35
4× T-Motor MN2212 motors | Reliable, quiet | $80
4× 30A ESC (BLHeli_32) | Motor controllers | $60
Holybro Pixhawk 6C | Flight controller (runs PX4) | $180
Here3+ GPS/compass | Outdoor reference | $120
Intel RealSense D435i | Transplanted from rover | —
NVIDIA Jetson Orin Nano | Replaces Pi (needs more GPU) | $150
4S 3300mAh LiPo | ~18 min flight time | $45
XT60 power distribution | Clean power wiring | $12
Propeller guards | Indoor safety | $20

PX4 handles low-level flight stabilization. Java (roomtour-drone module) sends velocity/position commands via MAVSDK-Java gRPC. FlightSafetyMonitor auto-lands at 15% battery.

Deliverables:

Drone hovers stably indoors in altitude-hold mode using optical flow + depth camera (no GPS)
First tethered flights with safety line
Depth camera feeding SLAM on Jetson, room detection confirmed while airborne
Emergency land command via /land responding within 1 second

Phase 4 — Autonomous Flight + Butler Brain (Weeks 21–32, ~$300)
This is where it comes together. The drone navigates fully autonomously and the butler personality deepens.
Navigation architecture:
RTAB-Map for 3D SLAM and Move Base Flex adapted for 3D flight. Voxel map persisted to disk on first run, reloaded on boot. Room detection uses a lightweight classifier on geometric signatures (ceiling height, room volume, dominant surfaces). The lifelog system — already built in Phase 1 — deepens here with health data from Apple Health or Fitbit. Claude reads it on every interaction: "You are Nico's home drone butler. Here is their recent context: [lifelog]. Answer naturally, as if you've been with them."

Deliverables:

Drone powers on, says "Good morning — you're in the kitchen. You have a 10am call and it's 58°F outside."
"Go to the office" → drone flies there autonomously through correct doorways
Full lifelog: week summary, upcoming events, health trends
Companion iOS/Android app (React Native or Flutter) with live floor plan, room labels, drone position, WebSocket-streamed, and text/voice chat via Spring Boot Server
Obstacle avoidance working in all tested rooms

Phase 5 — Butler Polish (Weeks 33–44, ~$200)
The final phase turns a cool prototype into something that feels like a real product.
Deliverables:

Custom wake word ("Hey Jeeves") via Picovoice Porcupine Java SDK
Proactive briefings: @Scheduled cron wakes drone at configured times, flies to find you, delivers the day ahead
Charging dock: flat landing pad with pogo pin contacts for auto-charging
Smart home integration: Home Assistant REST API via WebClient — "turn off the living room lights" parsed by Claude and executed
Safety hardening: propeller cage, auto-land at 15% battery, keep-out zones (butler.drone.keepout-zones[]), return-to-dock after 10 min idle

Emulation Strategy
The key insight is you never need to wait for hardware to make progress. The layered emulation approach:
Every hardware dependency sits behind an interface with a feature flag. butler.ros2.enabled=false uses SimulatedDroneNavigator (logs planned path, no movement). butler.drone.enabled=false skips MAVSDK entirely. butler.voice.enabled=false skips the mic/speaker loop. You swap a config flag, not code. By the time you fly the real drone, 80% of the logic has been tested through the Spring Boot REST UI and unit tests.

Tech Stack Summary:
- Language: Java 21
- Framework: Spring Boot 3.3.6, Maven multi-module
- Modules: roomtour-assistant (butler brain), roomtour-server (REST/web UI), roomtour-recognition (room classifier), roomtour-drone (flight control)
- AI: Claude API via Anthropic Java SDK, claude-sonnet-4-6
- Voice: Whisper STT (local HTTP), ElevenLabs TTS
- Navigation: ROS2 via rosbridge WebSocket, Nav2, RTAB-Map SLAM
- Flight: PX4 via MAVSDK-Java gRPC
- CI: GitHub Actions, reusable workflows from negretenico/GithubWorkflows
