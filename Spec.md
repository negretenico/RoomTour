Phase 1 — Software Simulation (Weeks 1–4, ~$0)
The philosophy here is: build everything in software first. Every mistake is free in simulation.
You'll use Gazebo (a robotics simulator) with a virtual floor plan of your home. You don't need a drone, or any hardware, to validate the core logic — room detection, pathfinding, voice interaction, and the Claude API integration.
Deliverables:

A 3D model of your home floor plan loaded into Gazebo
A room classifier that identifies rooms from simulated depth camera + semantic features (room size, shape, furniture placement)
A\* or RRT pathfinding between rooms, visualized in RViz
A working voice loop: wake word → speech-to-text → Claude API → ElevenLabs TTS → spoken answer
Claude integration with a "butler prompt" that gives it access to your calendar (Google Calendar API), weather (OpenWeatherMap), and a simple daily log you maintain

Tools: Python 3, ROS2 Humble, Gazebo Garden, OpenAI Whisper (local STT), Claude API (claude-sonnet-4-6), ElevenLabs API

Phase 2 — Ground Robot Prototype (Weeks 5–10, ~$200)
Don't start with a drone. Start with a wheeled robot that uses identical sensors and software. A ground robot is safer, cheaper to crash, and lets you validate SLAM and room navigation in your actual physical home before adding flight complexity.
What to buy:
PartPurposeCostRaspberry Pi 5 (8GB)Main compute$80Intel RealSense D435iDepth camera + IMU$60 used2-wheel robot chassis kitMobility platform$30L298N motor driverMotor control$8Mini Bluetooth speakerVoice output$15USB microphoneVoice input$10
Software stack on the Pi: ROS2 + RTAB-Map (visual SLAM) + Nav2 (autonomous navigation) + your Phase 1 voice/butler system ported over.
Deliverables:

Robot drives through your actual home and builds a real 2D occupancy map
Room classification working on real depth data — it knows it's in the kitchen vs the living room
"Take me to the bedroom" → robot navigates there autonomously
You can ask it anything and it answers via Claude with real context (today's weather, your next calendar event, etc.)

Phase 3 — Drone Frame Build (Weeks 11–20, ~$800)
Once your logic works on the ground robot, you transplant it onto a drone. This is the longest phase because flight safety requires patience.
Frame and flight hardware to buy:
PartPurposeCostDJI F450 frameSturdy 450mm quad$354× T-Motor MN2212 motorsReliable, quiet$804× 30A ESC (BLHeli_32)Motor controllers$60Holybro Pixhawk 6CFlight controller (runs PX4)$180Here3+ GPS/compassOutdoor reference$120Intel RealSense D435iTransplanted from rover—NVIDIA Jetson Orin NanoReplaces Pi (needs more GPU)$1504S 3300mAh LiPo~18 min flight time$45XT60 power distributionClean power wiring$12Propeller guardsIndoor safety$20
PX4 is the open-source flight stack you'll run. It handles the low-level flight stabilization so you don't have to — you send it velocity/position commands from ROS2.
Deliverables:

Drone hovers stably indoors in altitude-hold mode (no GPS needed indoors — you'll use optical flow + depth camera)
First tethered flights (connect a thin safety line to the ceiling)
Depth camera feeding SLAM on Jetson, confirming room detection works while airborne
Emergency land command working reliably

Phase 4 — Autonomous Flight + Butler Brain (Weeks 21–32, ~$300)
This is where it comes together. The drone navigates fully autonomously and the butler personality deepens.
Navigation architecture:
You'll run RTAB-Map for 3D SLAM and Move Base Flex adapted for 3D flight. The drone builds a voxel map of your home on first run, then localizes within it on subsequent boots. Room detection uses a lightweight classifier trained on the geometric signature of each room (ceiling height, room volume, dominant surfaces).
The butler memory system is the most interesting software challenge. You'll build a "lifelog" — a structured context file that Claude reads on every interaction. It contains recent calendar events, emails summarized by category, health data from Apple Health or Fitbit, and any notes you've manually added. Claude gets a system prompt that says: "You are [name]'s home drone butler. Here is their recent context: [lifelog]. Answer naturally, as if you've been with them."
Deliverables:

Drone powers on, says "Good morning — you're in the kitchen. You have a 10am call and it's 58°F outside."
"Go to the office" → drone flies there autonomously
Full lifelog system: summarizes your week, upcoming events, health trends
Companion iOS/Android app (built with React Native or Flutter) that shows a live floor plan, room labels, drone position, and a text/voice chat interface
Obstacle avoidance working in all tested rooms

Phase 5 — Butler Polish (Weeks 33–44, ~$200)
The final phase turns a cool prototype into something that feels like a real product.
Deliverables:

Custom wake word ("Hey Jeeves" or whatever you choose) using Picovoice Porcupine
Proactive briefings: drone auto-wakes at your set times ("7am morning brief") and flies to find you, tells you the day ahead
Charging dock: a flat landing pad with pogo pin contacts that auto-charges the battery when docked
Smart home integration: IFTTT or Home Assistant hooks so you can say "turn off the living room lights" through the drone
Personality tuning: refine the Claude system prompt to give it your preferred tone (formal butler, casual assistant, etc.)
Safety hardening: propeller cage enclosure, automatic land-on-low-battery at 15%, keep-out zones for areas with pets or fragile objects

Emulation Strategy
The key insight is you never need to wait for hardware to make progress. Here's the layered emulation approach:
Before any hardware, run Gazebo with a ROS2 simulated drone. The same Python nodes that read from a real RealSense camera also read from a Gazebo simulated one — you swap a config flag, not code. Then your ground robot becomes a "flight simulator" for the navigation logic. By the time you fly the real drone, 80% of the code has been tested.
For the butler AI specifically, test it entirely through a terminal or web UI — no hardware needed at all. Build a simple Flask web app that simulates "I am in the kitchen, it is 8am, here is my lifelog" and have Claude respond. Iterate on the prompt and memory system completely independently of the robotics work.
