# BindForge Game Mode Sub-Groups

Extracted from CMDR DAWNTREADER's in-game Controls UI (Odyssey). Each section covers one game
mode tab. Sub-groups are listed in the order they appear in the UI. Action names are transcribed
exactly as shown in the game.

**Special UI element notation used below:**
- `[axis — INVERTED/REGULAR, Deadzone slider]` — analogue axis input; the direction label and
  deadzone slider are always present together on the axis row
- `[slider]` — standalone sensitivity / rate / curve slider (no key binding slot)
- `[toggle: ON/OFF]` — on/off toggle switch (no key binding slot)
- `[dropdown: VALUE]` — option selector with the default/observed value shown
- `[Button Mode: HOLD/TOGGLE]` — Button Mode sub-setting that expands beneath an action
- `[selector: OPTIONS]` — axis-purpose selector (chooses what the mouse/stick axis controls)

Actions that appear in more than one sub-group are marked with *(also in …)*.

---

## 1. General Controls

### 1.1 Interface Mode

- UI Panel Up
- UI Panel Down
- UI Panel Left
- UI Panel Right
- Select / Confirm
- UI Back
- UI Nested Toggle
- Next Panel Tab
- Previous Panel Tab
- Next Page
- Previous Page

### 1.2 Galaxy Map

- Galaxy Cam Pitch Axis `[axis — INVERTED, Deadzone slider]`
- Galaxy Cam Pitch Up
- Galaxy Cam Pitch Down
- Galaxy Cam Yaw Axis `[axis — INVERTED, Deadzone slider]`
- Galaxy Cam Yaw Left
- Galaxy Cam Yaw Right
- Galaxy Cam Translate Y-Axis `[axis — INVERTED, Deadzone slider]`
- Galaxy Cam Translate Forward
- Galaxy Cam Translate Backward
- Galaxy Cam Translate X-Axis `[axis — REGULAR, Deadzone slider]`
- Galaxy Cam Translate Left
- Galaxy Cam Translate Right
- Galaxy Cam Translate Z-Axis `[axis — INVERTED, Deadzone slider]`
- Galaxy Cam Translate Up
- Galaxy Cam Translate Down
- Galaxy Cam Zoom Axis `[axis — INVERTED, Deadzone slider]`
- Galaxy Cam Zoom In
- Galaxy Cam Zoom Out
- Galaxy Cam Set Y-Axis to Z-Axis `[Button Mode: HOLD]`
- Galaxy Cam Select Current System

### 1.3 Camera Suite

- Ship - Toggle Camera Suite
- SRV - Toggle Camera Suite
- Commander - Toggle Camera Suite
- Previous Camera
- Next Camera
- Enter Free Camera
- Camera - Cockpit Front
- Camera - Cockpit Back
- Camera - CMDR 1
- Camera - CMDR 2
- Camera - Co-Pilot 1
- Camera - Co-Pilot 2
- Camera - Co-Pilot 3
- Camera - Front
- Camera - Back
- Camera - Low

### 1.4 Free Camera

- Toggle HUD
- Increase Speed
- Decrease Speed
- Forward Axis `[axis — INVERTED, Deadzone slider]`
- Throttle Axis Range `[dropdown: FULL RANGE]`
- Forward Only Throttle Reverse `[Button Mode: TOGGLE]`
- Move Forward
- Move Backward
- Lateral Axis `[axis — REGULAR, Deadzone slider]`
- Move Right
- Move Left
- Lift Axis `[axis — INVERTED, Deadzone slider]`
- Move Up [Analogue] `[Deadzone slider]`
- Move Down [Analogue] `[Deadzone slider]`
- Move Up
- Move Down
- Mouse X-Axis `[toggle: ON/OFF]`
- Pitch Axis `[axis — INVERTED, Deadzone slider]`
- Free Camera Mouse Sensitivity `[slider]`
- Relative Mouse Y-Axis `[toggle: ON/OFF]`
- Pitch Up
- Pitch Down
- Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- Yaw Left
- Yaw Right
- Roll Axis `[axis — REGULAR, Deadzone slider]`
- Roll Left
- Roll Right
- Stabiliser On/Off Toggle
- Camera / Ship Controls Toggle
- Attach / Detach Camera
- Exit Free Camera
- Zoom / Blur Toggle
- Increase Zoom/Focus
- Decrease Zoom/Focus
- Decrease Blur
- Increase Blur

### 1.5 Holo-Me

- Undo
- Redo
- Toggle Mouse Rotation
- Rotate Camera `[Deadzone slider]`

### 1.6 Playlist

- Play / Pause
- Skip Forward
- Skip Backward
- Clear Queue

### 1.7 Store Camera

- Hold To Rotate
- Pitch Axis `[axis — REGULAR, Deadzone slider]`
- Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Store Camera Zoom In
- Store Camera Zoom Out
- Store Toggle

### 1.8 System Colonisation Facility Placement

- Place Facility
- Change Construction Option
- Rotate Facility `[axis — REGULAR, Deadzone slider]`
- Rotate Facility Left
- Rotate Facility Right
- Exit Placement Camera
- Forward Axis `[Deadzone slider]`
- Move Forward
- Move Backward
- Lateral Axis `[Deadzone slider]`
- Move Right
- Move Left
- Lift Axis `[Deadzone slider]`
- Move Up
- Move Down
- Move Up [Analogue] `[Deadzone slider]`
- Move Down [Analogue] `[Deadzone slider]`
- Pitch Axis `[Deadzone slider]`
- Pitch Up
- Pitch Down
- Yaw Axis `[Deadzone slider]`
- Yaw Left
- Yaw Right
- Increase Speed
- Decrease Speed
- Free Camera Mouse Sensitivity `[slider]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- Relative Mouse Y-Axis `[toggle: ON/OFF]`

---

## 2. Ship Controls

### 2.1 Mouse Controls

Most entries in this sub-group are settings-only UI elements with no key binding slots. Exceptions: Reset Mouse and Disable Relative Mouse each have a key binding slot in the XML (currently unbound in this profile) in addition to their ON/OFF toggle.

- Mouse X-Axis `[selector: YAW / PITCH]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- Mouse Y-Axis `[selector: PITCH]`
- Relative Mouse Y-Axis `[toggle: ON/OFF]`
- Reset Mouse `[toggle: ON/OFF]` *(key binding slot — currently unbound)*
- Mouse Sensitivity `[slider]`
- Relative Mouse Rate `[slider]`
- Mouse Deadzone `[slider]`
- Mouse Power Curve `[slider]`
- Show Mouse Widget `[toggle: ON/OFF]`
- Disable Relative Mouse `[toggle: ON/OFF]` *(key binding slot — currently unbound)*
- Button Mode `[dropdown: HOLD]`

### 2.2 Flight Rotation

- Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Yaw Left
- Yaw Right
- Yaw Into Roll `[toggle: ON/OFF]`
- Yaw Into Roll Sensitivity `[slider]`
- Yaw Into Roll - Flight Assist Off `[dropdown: DEFAULT TO STANDARD CONTROLS]`
- Yaw Roll Button `[Button Mode: HOLD]`
- Roll Axis `[axis — REGULAR, Deadzone slider]`
- Roll Left
- Roll Right

### 2.3 Flight Thrust

- Pitch Axis `[axis — INVERTED, Deadzone slider]`
- Pitch Up
- Pitch Down
- Lateral Thrust Axis `[axis — REGULAR, Deadzone slider]`
- Thrust Right
- Thrust Left
- Vertical Thrust Axis `[axis — INVERTED, Deadzone slider]`
- Thrust Up
- Thrust Down
- Thrust Forward and Backward Axis `[axis — REGULAR, Deadzone slider]`
- Thrust Forward
- Thrust Backward

### 2.4 Alternate Flight Controls

- Alternate Controls Toggle `[Button Mode: TOGGLE]`
- Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Roll Axis `[axis — REGULAR, Deadzone slider]`
- Pitch Axis `[axis — REGULAR, Deadzone slider]`
- Lateral Thrust Axis `[axis — REGULAR, Deadzone slider]`
- Vertical Thrust Axis `[axis — REGULAR, Deadzone slider]`

### 2.5 Flight Throttle

- Throttle Axis `[axis — INVERTED, Deadzone slider]`
- Throttle Axis Range `[dropdown: FULL RANGE]`
- Forward Only Throttle Reverse `[Button Mode: TOGGLE]`
- Increase Throttle
- Decrease Throttle
- Throttle Increments `[dropdown: CONTINUOUS]`
- Set Speed to -100%
- Set Speed to -75%
- Set Speed to -50%
- Set Speed to -25%
- Set Speed to 0%
- Set Speed to 25%
- Set Speed to 50%
- Set Speed to 75%
- Set Speed to 100%

### 2.6 Flight Landing Overrides

- Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Yaw Left
- Yaw Right
- Yaw Into Roll `[dropdown: DEFAULT TO STANDARD CONTROLS]`
- Pitch Axis `[axis — REGULAR, Deadzone slider]`
- Pitch Up
- Pitch Down
- Roll Axis `[axis — REGULAR, Deadzone slider]`
- Roll Left
- Roll Right
- Lateral Thrust Axis `[axis — REGULAR, Deadzone slider]`
- Thrust Left
- Thrust Right
- Vertical Thrust Axis `[axis — REGULAR, Deadzone slider]`
- Thrust Up
- Thrust Down
- Thrust Forward and Backward Axis `[axis — REGULAR, Deadzone slider]`
- Thrust Forward
- Thrust Backward

### 2.7 Flight Miscellaneous

- Toggle Flight Assist `[Button Mode: TOGGLE]`
- Engine Boost
- Toggle Frame Shift Drive
- Supercruise
- Hyperspace Jump
- Rotational Correction `[Button Mode: TOGGLE]`
- Toggle Orbit Lines

### 2.8 Targeting

- Select Target Ahead
- Cycle Previous Ship
- Cycle Next Target
- Select Highest Threat
- Cycle Next Hostile Target
- Cycle Previous Hostile Ship
- Select Teammate 1
- Select Teammate 2
- Select Teammate 3
- Select Teammate's Target
- Teammate NavLock
- Cycle Next Subsystem
- Cycle Previous Subsystem
- Target Next System in Route

### 2.9 Weapons

- Primary Fire *(also in Multicrew)*
- Secondary Fire *(also in Multicrew)*
- Cycle Next Fire Group
- Cycle Previous Fire Group
- Deploy Hardpoints
- Firing Deploys Hardpoints `[toggle: ON/OFF]`

### 2.10 Cooling

- Silent Running `[Button Mode: TOGGLE]`
- Deploy Heat Sink

### 2.11 Miscellaneous

- Ship Lights
- Sensor Zoom Axis `[axis — REGULAR, Deadzone slider]`
- Decrease Sensor Zoom
- Increase Sensor Zoom
- Divert Power To Engines *(also in SRV Driving Miscellaneous)*
- Divert Power To Weapons *(also in SRV Driving Miscellaneous)*
- Divert Power To Systems *(also in SRV Driving Miscellaneous)*
- Balance Power Distribution *(also in SRV Driving Miscellaneous)*
- Reset HMD Orientation
- Cargo Scoop `[Button Mode: TOGGLE]` *(also in SRV Driving Miscellaneous)*
- Jettison All Cargo *(also in SRV Driving Miscellaneous)*
- Landing Gear
- Microphone Mute `[Button Mode: HOLD]` — sub-settings: Mute Button Mode `[dropdown: PUSH TO TALK]`, Microphone State Mode `[dropdown: PUSH TO TALK]`
- Use Shield Cell
- Use Chaff Launcher
- Use Shutdown Field Neutraliser
- Charge ECM
- Enable Context Menu in Ship `[toggle: ON/OFF]`
- Weapon Colour
- Engine Colour
- Night Vision
- System Colonisation Suite

### 2.12 Mode Switches

- UI Focus `[dropdown: DIRECTION]`
- UI Focus Mode `[dropdown: DIRECTION]`
- External Panel
- Comms Panel *(also in SRV Driving Mode Switches, On Foot Mode Switches)*
- Auto Focus on Text Input Field `[toggle: ON/OFF]`
- Quick Comms *(also in SRV Driving Mode Switches, On Foot Mode Switches)*
- Role Panel
- Internal Panel
- Looking at External Panel `[dropdown: DOES NOTHING]`
- Looking at Comms Panel `[dropdown: DOES NOTHING]`
- Looking at Role Panel `[dropdown: DOES NOTHING]`
- Looking at Internal Panel `[dropdown: DOES NOTHING]`
- Enable UI Camera Look `[toggle: ON/OFF]`
- Open Galaxy Map *(also in SRV Driving Mode Switches, On Foot Mode Switches)*
- Open System Map *(also in SRV Driving Mode Switches, On Foot Mode Switches)*
- Show DSS Score Screen `[Button Mode: HOLD]`
- Headlook `[Button Mode: TOGGLE]`
- Game Menu
- Friends Menu
- Open Discovery *(also in SRV Driving Mode Switches)*
- Switch Cockpit Mode
- Enter Pilot Mode

### 2.13 Headlook Mode

- Mouse Headlook `[toggle: ON/OFF]`
- Mouse Headlook Invert `[toggle: ON/OFF]`
- Mouse Headlook Sensitivity `[slider]`
- Headlook Default State
- Headlook Button Increments `[dropdown: CONTINUOUS]`
- Headlook Axis Mode `[dropdown: ACCUMULATE]`
- Centre When Headlook Inactive `[toggle: ON/OFF]`
- Headlook Sensitivity `[slider]`
- Headlook Smoothing `[toggle: ON/OFF]`
- Reset Headlook
- Look Up
- Look Down
- Look Up and Down Axis `[axis — INVERTED, Deadzone slider]`
- Look Left
- Look Right
- Look Left and Right Axis `[axis — REGULAR, Deadzone slider]`

### 2.14 Multicrew

- Mode Toggle
- Primary Fire *(also in Weapons)*
- Secondary Fire *(also in Weapons)*
- Primary Utility Fire
- Secondary Utility Fire
- Mouse X-Axis `[toggle: ON/OFF]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- Mouse Y-Axis `[toggle: ON/OFF]`
- Relative Mouse Y-Axis `[toggle: ON/OFF]`
- Third-Person Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Third-Person Yaw Left
- Third-Person Yaw Right
- Third-Person Pitch Axis `[axis — REGULAR, Deadzone slider]`
- Third-Person Pitch Up
- Third-Person Pitch Down
- Multi-Crew Mouse Sensitivity `[slider]`
- Third-Person Field of View Axis `[axis — INVERTED, Deadzone slider]`
- Third-Person Field of View Out
- Third-Person Field of View In
- Cycle Cockpit UI Forwards
- Cycle Cockpit UI Backwards

### 2.15 Fighter Orders

- Recall Fighter
- Defend
- Engage at Will
- Attack Target
- Maintain Formation
- Hold Position
- Follow Me
- Open Orders

### 2.16 Full Spectrum System Scanner

- Camera Pitch `[axis — REGULAR, Deadzone slider]`
- Camera Pitch Increase
- Camera Pitch Decrease
- Camera Yaw `[axis — REGULAR, Deadzone slider]`
- Camera Yaw Increase
- Camera Yaw Decrease
- Zoom In Target
- Zoom Out
- Stepped Zoom In
- Stepped Zoom Out
- Tuning `[axis — REGULAR, Deadzone slider]`
- Tuning Right
- Tuning Left
- Absolute Tuning `[axis — REGULAR, Deadzone slider]`
- FSS Tuning Sensitivity `[slider]`
- Discovery Scan
- Leave FSS
- Mouse X-Axis `[toggle: ON/OFF]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- Mouse Y-Axis `[toggle: ON/OFF]`
- Relative Mouse Y-Axis `[toggle: ON/OFF]`
- Mouse Sensitivity `[slider]`
- FSS Mouse Deadzone `[slider]`
- FSS Mouse Power Curve `[slider]`
- Target Current Signal
- Show Help

### 2.17 Detailed Surface Scanner

- Toggle Front/Back View `[Button Mode: TOGGLE]`
- Exit Mode
- Next Filter
- Previous Filter
- Mouse X-Axis `[toggle: ON/OFF]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- Mouse Y-Axis `[toggle: ON/OFF]`
- Relative Mouse Y-Axis `[toggle: ON/OFF]`
- DSS Mouse Sensitivity `[slider]`
- Third-Person Yaw Axis `[axis — REGULAR, Deadzone slider]`
- Third-Person Yaw Left
- Third-Person Yaw Right
- Third-Person Pitch Axis `[axis — REGULAR, Deadzone slider]`
- Third-Person Pitch Up
- Third-Person Pitch Down
- Third-Person Field of View Axis `[axis — REGULAR, Deadzone slider]`
- Third-Person Field of View Out
- Third-Person Field of View In

---

## 3. SRV Controls

### 3.1 Driving

- Drive Assist `[Button Mode: TOGGLE]`
- Drive Assist Default
- SRV Steering Mouse X-Axis `[toggle: ON/OFF]`
- Relative Mouse X-Axis `[toggle: ON/OFF]`
- SRV Rolling Mouse X-Axis `[toggle: ON/OFF]`
- Relative Mouse X-Roll `[toggle: ON/OFF]`
- SRV Pitch Mouse Y-Axis `[toggle: ON/OFF]`
- Relative Mouse Y-Pitch `[toggle: ON/OFF]`
- Steering Axis `[axis — REGULAR, Deadzone slider]`
- Steering Left Button
- Steering Right Button
- Roll Axis `[axis — REGULAR, Deadzone slider]`
- Roll Left Button
- Roll Right Button
- Pitch Axis `[axis — INVERTED, Deadzone slider]`
- Pitch Up Button
- Pitch Down Button
- Vertical Thrusters `[Button Mode: HOLD]`
- SRV Primary Fire
- SRV Secondary Fire
- Handbrake `[Button Mode: HOLD]`
- Headlights
- Toggle SRV Turret
- Cycle Next Fire Group
- Cycle Previous Fire Group

### 3.2 Driving Targeting

- Select Target Ahead

### 3.3 Driving Turret Controls

- Turret Mouse X-Axis `[selector: YAW]`
- Turret Relative Mouse X-Axis `[toggle: ON/OFF]`
- Turret Mouse Y-Axis `[selector: PITCH INVERTED]`
- Turret Relative Mouse Y-Axis `[toggle: ON/OFF]`
- SRV Turret Yaw Axis `[axis — REGULAR, Deadzone slider]`
- SRV Turret Yaw Left
- SRV Turret Yaw Right
- SRV Turret Pitch Axis `[axis — REGULAR, Deadzone slider]`
- SRV Turret Pitch Up
- SRV Turret Pitch Down
- SRV Turret Mouse Sensitivity `[slider]`
- SRV Turret Mouse Deadzone `[slider]`
- SRV Turret Mouse Power Curve `[slider]`

### 3.4 Drive Throttle

- Drive Speed Axis `[axis — INVERTED, Deadzone slider]`
- Throttle Axis Range `[dropdown: FORWARD ONLY]`
- Forward Only Throttle Reverse `[Button Mode: HOLD]`
- SRV Throttle Increments `[dropdown: CONTINUOUS]`
- Accelerate Button
- Decelerate Button
- Accelerate Axis `[Deadzone slider]`
- Decelerate Axis `[Deadzone slider]`

### 3.5 Driving Miscellaneous

- Divert Power To Engines *(also in Ship Miscellaneous)*
- Divert Power To Weapons *(also in Ship Miscellaneous)*
- Divert Power To Systems *(also in Ship Miscellaneous)*
- Balance Power Distribution *(also in Ship Miscellaneous)*
- Cargo Scoop `[Button Mode: TOGGLE]` *(also in Ship Miscellaneous)*
- Jettison All Cargo *(also in Ship Miscellaneous)*
- Recall/Dismiss Ship
- Enable Context Menu in SRV `[toggle: ON/OFF]`

### 3.6 Driving Mode Switches

- UI Focus
- External Panel
- Comms Panel *(also in Ship Mode Switches, On Foot Mode Switches)*
- Quick Comms *(also in Ship Mode Switches, On Foot Mode Switches)*
- Role Panel
- Internal Panel
- Open Galaxy Map *(also in Ship Mode Switches, On Foot Mode Switches)*
- Open System Map *(also in Ship Mode Switches, On Foot Mode Switches)*
- Open Discovery *(also in Ship Mode Switches)*
- Switch Cockpit Mode
- Headlook `[Button Mode: TOGGLE]`

---

## 4. On Foot Controls

### 4.1 On Foot

- Mouse X-Axis `[selector: ROTATE]`
- Mouse Y-Axis `[selector: PITCH]`
- Mouse Sensitivity `[slider]`
- Forward Axis `[axis — INVERTED, Deadzone slider]`
- Move Forward
- Move Backward
- Strafe Axis `[axis — REGULAR, Deadzone slider]`
- Strafe Left
- Strafe Right
- Rotate Axis `[axis — REGULAR, Deadzone slider]`
- Rotate Sensitivity `[slider]`
- Turn Left
- Turn Right
- Pitch Axis `[axis — REGULAR, Deadzone slider]`
- Pitch Sensitivity `[slider]`
- Look Up
- Look Down
- Sprint `[Button Mode: TOGGLE]`
- Walk `[Button Mode: TOGGLE]`
- Crouch `[Button Mode: TOGGLE]`
- Jump
- Interact
- Secondary Interact
- Open Item Menu `[Button Mode: TOGGLE]`
- Open Emote Wheel `[Button Mode: TOGGLE]`
- Cycle Utility Wheel Mode
- Wheel Horizontal `[Deadzone slider]`
- Wheel Left
- Wheel Right
- Wheel Vertical `[Deadzone slider]`
- Wheel Up
- Wheel Down
- Wheel Accepts Mouse Input `[toggle: ON/OFF]`
- Fire Weapon/Use Tool
- Aim Down Sights `[Button Mode: TOGGLE]`
- Throw Grenade
- Melee Attack
- Reload
- Switch Weapon
- Select Primary Weapon
- Select Secondary Weapon
- Select Tool
- Select Next Weapon
- Select Previous Weapon
- Holster Weapon
- Select Next Grenade Type
- Select Previous Grenade Type
- Toggle Flashlight
- Toggle Night Vision
- Toggle Shields
- Clear Authority Level
- Use Health Pack
- Use Energy Cell
- Select Frag Grenade
- Select EMP Grenade
- Select Shield Grenade
- Select Energylink
- Select Profile Analyser
- Select Suit Specific Tool
- Toggle Tool Mode
- Toggle Help
- Ping / Call Out *(currently unbound)*

### 4.2 On Foot Mode Switches

- Open Galaxy Map *(also in Ship Mode Switches, SRV Driving Mode Switches)*
- Open System Map *(also in Ship Mode Switches, SRV Driving Mode Switches)*
- Comms Panel *(also in Ship Mode Switches, SRV Driving Mode Switches)*
- Quick Comms *(also in Ship Mode Switches, SRV Driving Mode Switches)*
- Open Insight Hub
- Open Conflict Zone Battle Stats

### 4.3 On Foot Miscellaneous

- Enable Context Menu on Foot `[toggle: ON/OFF]`
- Enable Aim Assist `[toggle: ON/OFF]`

### 4.4 On Foot Emotes

- Point
- Wave
- Agree
- Disagree
- Go
- Stop
- Applaud
- Salute
