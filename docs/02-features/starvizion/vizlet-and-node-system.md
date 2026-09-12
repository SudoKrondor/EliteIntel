# StarVizion — Vizlets and the Node System

## The Vizlet

A **Vizlet** is the fundamental unit of StarVizion. It is a single overlay window composed of stacked NeuroNodes.

### Window Properties

- Frameless — no title bar, no border, no window chrome
- Transparent background by default (the user's nodes determine what is visible)
- Always-on-top — renders above other windows including the game
- Not resizable by OS window handles — resized only through the StarVizion editor
- Position and size are persisted across sessions

### User Interaction With a Live Vizlet

- **Left-click drag** — moves the Vizlet to a new position (unless locked)
- **Right-click** — opens a context menu with three options:
  - **Edit** — opens the Vizlet's editor panel inside StarVizion
  - **Lock / Unlock** — toggles whether left-click drag is active
  - **Close** — removes the Vizlet window from the screen

Vizlets are not dockable and do not snap to other Vizlets, monitors, or desktop edges. They float freely.

### Device Disconnect Behavior

**On first startup** — if a device referenced by a saved Vizlet is not present, a warning is shown in the editor. The Vizlet still loads; affected nodes are flagged with a warning indicator.

**During a session** — if a device disconnects while Vizlets are active:
- StarVizion shows a notification (non-blocking, dismissable)
- All Vizlets with nodes bound to the disconnected device perform a soft save (write current state to disk, in case it was not already saved)
- Those Vizlets close their overlay windows
- When the device reconnects, the user re-activates the Vizlets manually

This behavior is intentionally simple. Players who need visual confirmation of their controls rarely run without their HOTAS connected, so over-engineering reconnection logic is not warranted.

---

## Node System

A Vizlet is built from stacked **NeuroNodes**. Every node occupies the full Vizlet window size and contributes to the final visual output. Nodes are ordered by Z-order (higher values render on top). Multiple nodes can coexist within a single Vizlet.

There is exactly one node type: **NeuroNode**. A NeuroNode can be fully Live (driven by live input), fully Static (fixed expressions that never change), or anything in between. There is no separate static node type — a grid drawn at fixed coordinates with fixed colors is still a NeuroNode, just one whose expressions resolve to constants.

### NeuroNode Modes

NeuroNodes operate in one of two modes, determined by whether the node has input bindings — named **Live** and **Static** (earlier design generations called these "Reactive mode" and "Decoration mode"; same concepts, renamed):

**Live mode** — the node has one or more input bindings. Its properties are driven by StarCalc expressions that reference live controller input, game data, or time. The node updates every frame. This is how moving indicators, live readouts, and animated elements are built.

**Static mode** — the node has no input bindings. It cannot reference live controller input. Properties can still be driven by StarCalc expressions that reference window variables or time variables — so a Static node can animate using time, or size itself relative to the window, but it cannot react to a joystick or button. This is how tick marks, axis lines, center markers, end stops, background gradients, animated pulses, and other visual scaffolding are built.

The mode is not an explicit setting — it is inferred from whether the node has any input bindings. The editor adapts its UI to whichever mode applies (see [UI Layout — Vizlet Editor](ui-layout.md#vizlet-editor)).

### Authoring Workflow: NeuroNode-First, Bind-on-Demand

A Vizlet is authored **layer-first**, not input-first. The user creates a Vizlet, adds a NeuroNode, and designs how it looks — a NeuroNode starts in Static mode by default, with no input source chosen yet. Only when the user decides a specific layer needs to react to something does the editor require them to choose an input source for it; making that choice is what turns the layer Live. There is no upfront step where the user must pick a controller, axis, or button before they can start building a Vizlet's visuals.

This deliberately generalizes "input source" beyond just a physical controller — see [Binding a NeuroNode](#binding-a-neuronode) below for the full set of source types a Live NeuroNode can call out to.

### Primitives

| Primitive | Description | Typical Use |
|---|---|---|
| Dot | Circle, defined by position and radius. | Axis position indicator, button state |
| Bar | Filled linear bar. | Throttle, slider, progress |
| Line | Line drawn from an origin point at a given angle and length. Supports tick mark sub-properties. | Axis line, crosshair, scale marks |
| Arc | Circular arc, defined by center, radius, and angle range. Supports tick mark sub-properties. | Radial gauge, heading ring |
| Text | Text string, static or expression-driven. | Numerical readout, label |
| Grid | Repeating grid pattern across the node area. Supports multiple types: square, circle, radial, hexagon, triangle, and others. | Background guide, alignment reference, radar overlay |
| Bitmap | User-supplied image file. | Cockpit art, custom overlay, complex shapes |
| Parametric | User-defined parametric shape — the user writes x(t) and y(t) expressions; the system evaluates them across a parameter range and draws the resulting path. | Any mathematically-defined shape, custom gauges, live geometry |

The Grid primitive covers the common named grid types (square, circle, radial, hexagon, triangle) as a direct configuration option — no math required. For truly custom or mathematical grid patterns not covered by these types, the Parametric primitive is the appropriate tool.

**Common properties (all primitives):** horizontal position, vertical position, rotation (degrees), opacity (0.0–1.0), visible (boolean show/hide). In Live mode, any property can be expression-driven against live input, time, or window variables. In Static mode, properties can reference window and time variables but not device inputs.

### Parametric Primitive

The general-purpose, expression-defined drawing tool. Rather than selecting a pre-defined shape, the user writes two StarCalc expressions — one for x, one for y — and the system evaluates them repeatedly across a parameter range, connecting the resulting points into a path.

A parameter sweeps from a start value to an end value in a set number of evenly-spaced steps. At each step, the x and y expressions are evaluated with the parameter injected as a variable alongside all standard StarCalc variables (input bindings, window variables, time). The resulting points are connected as a continuous path. If the "closed" property is set, the last point connects back to the first, allowing a fill color.

This primitive is the creative escape hatch for geometry — anything expressible as a mathematical path can be drawn with it. For raster art and images that cannot be described mathematically, the Bitmap primitive fills the same role.

### Tick Mark Sub-Properties (Line and Arc)

The Line and Arc primitives support an optional set of tick-mark properties in Static mode: how many ticks, their length, their thickness, their color, and an optional "every Nth tick is larger" major-tick setting. These draw evenly-spaced marks perpendicular to the Line or radially outward from the Arc, without requiring a separate node per mark. For tick marks that need to be Live or mathematically defined, the Parametric primitive is the appropriate tool.

### Bar Fill Options

The Bar primitive's fill can originate from the midpoint (extends outward in both directions — suitable for bidirectional axes resting at center), the low end (suitable for values ranging from zero to maximum, e.g. throttle), or the high end (reverses the direction — full value looks empty, suitable for countdown-style readouts).

### Text Properties

Content (which can itself be a StarCalc expression producing a formatted string), font family, font size, bold, italic, color, and horizontal alignment. StarVizion ships one bundled fallback font used when a Vizlet references a system font not installed on the current machine — the import does not fail; the editor flags which nodes had their font substituted so the user can update them.

### Bitmap Asset Handling

When a user adds a Bitmap node and selects an image file, StarVizion copies that file into its own managed resource folder. The Vizlet's saved data stores the relative path to the copy, not the original location — moving, renaming, or deleting the original file afterward does not break the Vizlet. When exporting a Vizlet or HoloFrame, the resource file is included in the export package. If a referenced resource file is later missing (a manual deletion, a corrupted export), the node renders as an empty placeholder and flags a warning in the editor.

### Dot, Line, Arc, Grid Properties

- **Dot** — radius, fill color, optional border (color, thickness, independent opacity).
- **Line** — length, angle (0 = right, 90 = down), thickness, color. Originates from the node's position and extends in the direction of the angle.
- **Arc** — radius, start angle, sweep (angular extent — 360 produces a full circle), thickness, color. Centered on the node's position.
- **Grid** — pattern type (square, circle, radial, hexagon, triangle), color, spacing (or ring radius for circle/radial), thickness, and an optional dashed/dotted line style with configurable dash length and gap.

### Binding a NeuroNode

Input bindings are optional. A NeuroNode with no bindings operates in Static mode. A NeuroNode with one or more bindings operates in Live mode — properties can reference those bound values in StarCalc expressions.

Each binding maps a user-chosen variable name to an input source. This is deliberately a generic "provider" concept, not hardcoded to physical controllers specifically — a Live NeuroNode calls out to whichever provider its binding names:

| Source Type | Description |
|---|---|
| Input | An axis or button on a connected controller, keyboard, or mouse |
| Journal | A field from Elite Dangerous journal events (future — v1 is input-only; see [Telemetry](../../03-data-models/telemetry.md)) |
| Data | An external data source (future) |

A NeuroNode may also use StarCalc expressions that reference only window or time variables with no device input at all — for example, a Static-mode node that pulses over time.

### Multiple NeuroNodes

A single Vizlet can contain any number of NeuroNodes, each bound to a different input or data source. All render simultaneously, stacked by Z-order, producing a composite visual — for example, a stick-grip Vizlet combining a Dot bound to the stick's X/Y axes, a Bar bound to a throttle axis, four Dots each bound to a face button, and a Text node showing a computed value.

---

## Input Capture

StarVizion reads input from connected devices. Input capture has two distinct concerns handled differently:

**Device enumeration** — discovering available devices, establishing identity, detecting hot-plug events. This is shared infrastructure; StarVizion uses Elite-Intel's Device Service (`elite.intel.devices`) for this, exactly as every other Elite-Intel consumer of device input already does. There is no reason to duplicate device discovery.

**Frame-rate polling** — reading axis values and button states at rendering frame rate (60–144+ times per second) to drive NeuroNode expressions. **Confirmed: the Device Service itself provides this as a high-frequency input stream**, shared infrastructure any Elite-Intel feature can use — not something StarVizion builds and owns on its own. This was an open item; it's resolved specifically because StarVizion is not the only feature with a plausible reason to want it — BindForge visualizing live axis/button movement in its own UI is a real, wanted use case (not yet designed, but real), and other Elite-Intel features may want the same. Building it once, centrally, is consistent with how Elite-Intel already treats `elite.intel.devices` as shared infrastructure — see [Vision — History](../../00-overview/vision.md).

### Supported Input Sources

- Analog game controllers — HOTAS, throttles, sticks, pedals, rudders, wheels, rotaries, sliders — any HID-class game controller the operating system exposes
- Gamepads — both analog axes and digital buttons
- Keyboard — any key, any combination, subject to the focus-scope caveat below
- Mouse — X/Y axes, scroll wheel, all buttons

Multiple devices can be active simultaneously. A single Vizlet may contain nodes bound to different devices.

**Hat switches** are treated as buttons at the input capture layer. Each hat direction is exposed as a separate boolean (pressed / not pressed). StarVizion does not treat hat switches as axes or POV values under any circumstances.

The hardware itself reports one 4-bit value rather than booleans (confirmed 2026-09-06 — see
[StarCalc](starcalc-expression-engine.md#input-variables)), so "the input capture layer" is precisely where the
expansion has to happen. It is not free, and nothing downstream should assume the device handed it flags.

### Polling Requirement

Regardless of which layer owns the polling, axis and button state must be available at render frame rate with no measurable latency added to the game's own input processing. Polling must be non-blocking. If all bindings to a device are removed, polling for that device should stop.

### Multi-Device Identity

When the user has two physically identical controllers (e.g., dual identical HOTAS sticks), StarVizion must distinguish them. Device identity is established by the Device Service using vendor ID, product ID, and connection order/port path — a port path is required to disambiguate, since VID/PID alone cannot tell two identical-model devices apart (Elite Dangerous's own `.binds` files have the same limitation — see [BindForge's binding schema](../../03-data-models/binding-schema.md#device-identity)). Vizlet data stores that identity so Vizlets reload correctly across sessions.

### Hot-Plug

Connect and disconnect events are surfaced by the Device Service. When a bound device disconnects, the affected Vizlets soft-save and close (see [Device Disconnect Behavior](#device-disconnect-behavior) above). When the device reconnects, the user re-activates the affected Vizlets manually. There is no automatic resumption.

### Keyboard Capture — Confirmed Global, v1

Input APIs generally only report keyboard state while the capturing application's own window has focus. True global (focus-independent) keyboard capture is a separate, harder problem, requiring a platform-native low-level input hook. This is a confirmed, well-diagnosed limitation from prior research on this exact feature — a prior prototype's keyboard-visualization Vizlet never worked, and even once its immediate cause was fixed, focus-scoping would have remained a second, unresolved problem.

**Confirmed for the first release: a platform-native global keyboard hook, not focus-scoped-only capture.** A HUD meant to stay visible while Elite Dangerous has focus is useless if it can only see keystrokes while its own window is focused instead, so focus-scoped-only was never a real option for v1. A low-level global hook only *observes* keystrokes — it does not consume or intercept them, so the game continues receiving every keystroke completely normally; nothing is stolen from it, the same way a screen recorder can see the screen without blocking anything else from also seeing it. This is resolved as part of the Device Service's own design, since it's the same shared input infrastructure covering frame-rate polling above — not decided independently by StarVizion.
