# StarVizion — Overview

**Elite-Intel feature:** StarVizion
**Status:** Design consolidated from prior planning; not yet built. An earlier prototype explored some of these ideas but was never wired into a finished, user-facing state — this specification is a fresh design target, not a description of that prototype.

---

## Purpose

StarVizion lets Elite Dangerous pilots create, arrange, and display live input-visualization overlays on their desktop or in VR — without touching the game, without remapping inputs, and without adding any latency to their control chain.

## What StarVizion Does

StarVizion creates **Vizlets**: frameless, borderless, transparent, always-on-top windows that float above the desktop and the game, or — for VR players — overlays rendered directly into the VR compositor. Each Vizlet is a visual composition that can show:

- Live axis positions (joystick, throttle, rudder, sliders, rotaries)
- Button press/release states, including multi-stage triggers and 3-position switches
- Keyboard and mouse states
- Decorative HUD framing elements — scales, grids, cockpit-style graphics
- Computed displays driven by expression formulas that combine multiple inputs
- (Future) Game telemetry from the Elite Dangerous journal — see [Telemetry](../../03-data-models/telemetry.md)

Vizlets are designed for players who fly in VR using springless HOTAS setups with no physical center or stop feedback, who need visual confirmation of their current control states inside the headset. They also serve desktop players who want a live readout of their inputs visible alongside the game.

## What StarVizion Does Not Do

- It does not send, intercept, or remap any input to the game
- It does not modify any Elite Dangerous files
- It does not interact with the running game process
- It is a pure visualization and read-only monitoring tool

## Game Version Scope

StarVizion visualizes controller input, which is game-agnostic. The mode-based visibility system (which shows or hides Vizlets based on what the player is doing in-game — see [UI Layout & Mode-Based Visibility](ui-layout.md#mode-based-visibility)) uses Elite Dangerous journal events. In v1, Elite Dangerous: Odyssey is the only supported game for mode detection. Controller visualization works regardless of what game is running.

## Document Map

- [Vizlets and the Node System](vizlet-and-node-system.md) — the Vizlet window, its NeuroNode building blocks, and the visual primitives available
- [StarCalc Expression Engine](starcalc-expression-engine.md) — the expression language that drives Live and Static behavior
- [HoloFrames and Persistence](holoframes-and-persistence.md) — saving, grouping, exporting, and importing Vizlets
- [UI Layout](ui-layout.md) — the editor UI and mode-based visibility system
- [VR Overlay](vr-overlay.md) — desktop vs. VR rendering
- [Roadmap](roadmap.md) — features beyond the first release, and open questions
