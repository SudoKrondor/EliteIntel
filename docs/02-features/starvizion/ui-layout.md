# StarVizion — UI Layout

StarVizion occupies Elite-Intel's main tab content area for its editor and management UI. All Vizlet windows themselves float outside the Elite-Intel window, directly on the desktop.

## Left Navigation (Within the StarVizion Tab)

Two tabs:

| Tab | Purpose |
|---|---|
| Vizlets | Library of all saved Vizlets; open, edit, activate, import, delete |
| HoloFrames | Manage groups; activate/deactivate, create, rename, delete |

## Vizlet Library View

Shows all saved Vizlets as cards. Each card shows the Vizlet's name, tags, a thumbnail preview (rendered from the Vizlet's node definitions, not a screenshot), and a status indicator (live / closed / device missing).

**Controls are icon buttons overlaid on the thumbnail, not click gestures** — three fixed corners, so the action is visible without hovering or guessing: a play icon (top-left) activates/deactivates — creating or closing the overlay window; a pencil (bottom-left) opens the editor; a stacked-dots icon (bottom-right) opens a small menu with Edit, Delete (with a confirmation, since it's permanent), and Close. The play icon fills in to show active state. This mirrors a pattern familiar from media-library apps (e.g. Plex's poster cards), chosen specifically so the controls are self-explanatory without needing separate single-click/double-click behavior to be taught. Duplicate and export are not yet placed in this control set — still open.

## HoloFrame Library View

Shown merged with the Vizlet Library in a single two-column screen — Vizlets on the left, HoloFrames on the right — rather than as separate tabs. HoloFrames are shown as cards in the same poster-grid style as Vizlets, not a list — each card's "thumbnail" area shows a live member count (e.g. "3 Vizlets") in place of a rendered preview, since a HoloFrame has no visual content of its own to preview. Below that: the HoloFrame's editable name, its member Vizlets as individual removable chips, and (when set) small badges summarizing its mode assignment and Render Mode override. **Membership is set by drag-and-drop:** dragging a Vizlet card from the left column onto a HoloFrame card on the right adds it as a member (the member count updates immediately); dropping a Vizlet already in that HoloFrame is a no-op. A member is removed via the &times; on its chip, from either the library card or the HoloFrame Editor described below. This part is built in the mockup.

Each card has the same three corner icon-button controls as a Vizlet card (play/edit/dots-menu — see [Vizlet Library View](#vizlet-library-view)), overlaid on the thumbnail area exactly the same way. The play icon here activates/deactivates the whole HoloFrame (all members shown/hidden together, per [HoloFrames](holoframes-and-persistence.md#holoframes)); an active HoloFrame's card is highlighted (full card, not just the button, since unlike a Vizlet a HoloFrame has no separate status indicator to carry that state).

### HoloFrame Editor

Reached the same way a Vizlet's editor is: the card's pencil icon, or Edit from its dots-menu, opens a dedicated screen, mirroring the Vizlet Editor's own pattern rather than inline list editing. Built in the mockup. Three sections:

- **Identity** — ID (read-only, system-set), Name (editable; syncs back to the library card's name on close).
- **Members** — the same removable chips shown on the library card, plus a note pointing back to the library screen for adding members via drag-and-drop.
- **Mode-Based Switching** — a Manual-only/Assigned-to-mode(s) selector; when Assigned, checkboxes for the same mode list used by [per-Vizlet Mode-Based Visibility](#mode-based-visibility), per [Mode-Based HoloFrame Switching](holoframes-and-persistence.md#mode-based-holoframe-switching).
- **Render Mode Override** — a single selector (None / Desktop / VR / Desktop + VR / Desktop+), per [Render Mode Override](holoframes-and-persistence.md#render-mode-override). Whole-group only, matching the doc — there is no per-member override control anywhere in this editor.

Changes to mode assignment and Render Mode override are held in the editor until BACK TO LIBRARY is clicked, at which point they're written back to the HoloFrame and reflected in its library-row badges. This mirrors the mockup's convention elsewhere of writing changes back on navigation rather than a live per-keystroke sync, since neither setting has a meaningful "live preview" the way a Vizlet's own properties do.

## Vizlet Editor

The editor opens when the user clicks Edit on a Vizlet, from the library or from the Vizlet's own right-click context menu. **Every Vizlet's editor has the same structure, regardless of what that Vizlet contains** — a Vizlet is not a typed object (there is no "stick Vizlet" or "throttle Vizlet" category); it is simply a named container of NeuroNodes, and the editor's job is to expose that same container uniformly no matter what's in it or what it's named. A Vizlet's name and tags are descriptive metadata the author chooses — nothing in the system infers scope or meaning from them (naming a Vizlet "Right Stick" does not mean it is restricted to, or expected to contain, any particular device's inputs).

**Live editing principle:** every change made in the editor is reflected in the Vizlet immediately — no OK button, no Apply, no confirmation step. The Vizlet is always live. What is not acceptable is a workflow where the user edits and then has to take a separate action to see the result.

**The editor is two top-level tabs, Vizlet and NeuroNodes, switched right below the editor's own header (back button and Vizlet name).** The Vizlet tab opens by default. Nothing is shared or visible across both tabs simultaneously — they are two distinct full views, not a shared area plus a sub-tabbed panel.

### Vizlet Tab

Everything about the Vizlet itself, as a whole — not about any one NeuroNode within it. This is where the settings that were previously written into the spec but never given a UI location now live:

- **Identity** — name, tags (both editable); author, created timestamp, version (read-only, system-set).
- **Window** — width, height, position (left/top); always-on-top, transparent, and locked toggles (locked here is the same state as the live Vizlet's own [right-click context menu](#live-vizlet-context-menu) Lock/Unlock — just a second place to reach it).
- **Render Mode** — Desktop or VR, per [VR Overlay](vr-overlay.md). When VR is selected: world-locked vs. HMD-locked, and the current VR transform (position X/Y/Z in meters, width in meters) shown read-only — that transform is set by grabbing and moving the overlay in the headset, the same way any VR overlay is repositioned, not by typing coordinates here.
- **Mode-Based Visibility** — the always-visible / visible-in / hidden-in setting and mode selection described below. Lives here, not on the NeuroNodes tab, since it is a Vizlet-level setting, not a per-NeuroNode one.

### NeuroNodes Tab

The NeuroNode-composition workspace — everything needed to build and edit the Vizlet's visual content:

- **Canvas** — the live Vizlet window serves as the real-time preview. The canvas is read-only for positioning purposes: neither a NeuroNode's position/size nor the Vizlet window's own position/size are set by dragging on the canvas — a NeuroNode's position/size are set through numeric fields on this tab, the Vizlet window's own position/size on the Vizlet tab. The canvas does show structural handles for selecting individual NeuroNodes.
- **Node Stack Panel** — a vertical list of all NeuroNodes, ordered top-to-bottom by Z-order (the top entry renders frontmost). Reordered by dragging rows via a drag handle. Each entry shows the primitive type (or an unassigned indicator if no type has been chosen yet — see below), an editable name, a visibility toggle, the Live/Static mode, and a delete action (with confirmation, since it permanently removes the NeuroNode and its bindings/expressions from the Vizlet — there is no undo beyond whatever the Vizlet's own edit history provides). A newly added NeuroNode is inserted directly above whichever NeuroNode is currently selected (or on top of the stack if nothing is selected).
- **Adding a NeuroNode** — the add action creates a generic NeuroNode with no primitive type chosen yet, Static, with no bindings. The Property Panel for an unassigned NeuroNode shows only a Type selector (Dot, Bar, Line, Arc, Text, Grid, Bitmap, Parametric); choosing one populates that primitive's default properties. Selecting Bitmap immediately prompts for an image file — see [Bitmap Asset Handling](vizlet-and-node-system.md#bitmap-asset-handling) for what happens to that file. A NeuroNode's type is fixed once chosen; changing primitive type afterward is not supported — delete and re-add instead.
- **Property Panel** — context-sensitive, adapting to the selected node's mode:
  - *Static mode* (no input bindings): an expression editor section for properties driven by window or time expressions, plus sliders/color pickers/numeric fields for fixed constant properties. No input-bindings section is shown. A node starts here by default — see [Vizlets and the Node System — Authoring Workflow](vizlet-and-node-system.md#authoring-workflow-neuronode-first-bind-on-demand).
  - *Live mode* (has input bindings): an input-bindings section (one row per bound variable, mapping it to an input source), an expression editor section, and a static-properties section for anything not driven by expressions. Adding the first input binding is what moves a node from Static to Live.
- **Device Input Monitor** — a live readout of all connected devices and their current state. Clicking any active input assigns it to the selected node's binding (click-to-bind). Only visible when a Live-mode node is selected.
- **Input Simulation Panel** — sliders and toggles for simulating axis values and button presses without hardware, for testing Live node expressions.

### Live Vizlet Context Menu

Right-clicking an active Vizlet's own overlay window — the floating window itself, on the desktop, not anything in the StarVizion tab — opens a small context menu: **Edit** (opens the Vizlet Editor), **Lock/Unlock** (same state as the Vizlet tab's Locked toggle above — a locked overlay can't be dragged), and **Close** (deactivates it, same as the play icon on its library card). This is the live window's own menu, separate from the library card's dots-menu — the two happen to share Edit and Close, but the card's dots-menu also has Delete (removing the Vizlet entirely), which has no place on the live window's menu since deleting a Vizlet that's still running isn't a sensible action from that surface.

## Expression Editing Design

Expression editing in StarVizion is designed around two principles, both drawn from the graphing-calculator UX pattern popularized by Desmos:

1. **Every field is live.** Typing in an expression field immediately evaluates and updates the Vizlet. There is no submit action. Invalid expressions are flagged inline (a visual error indicator) without disrupting other fields or closing anything.
2. **Expressions build on each other.** A node's local expressions appear as a vertical list above the property expressions, matching a model where values are defined in one row and referenced in the next. The layout reads top to bottom — locals first, then property fields — and each row can reference anything defined above it.

The expression editor is not a modal dialog — it is a persistent panel within the editor UI that does not block interaction with the rest of the editor and does not dismiss when the user clicks elsewhere. Whether it is docked, floating, or collapsible is an implementation detail; what is required is that it stays open without the user having to fight it.

Each expression field has a control that expands the field into a larger editing surface with: the expression text in a larger input area; a clickable list of all available variables (input bindings, locals, window variables, time variables); a clickable list of available functions; a live current-value readout; and a valid/invalid indicator. The specific layout of this expanded surface (inline expansion, side panel, or popover) is an implementation detail.

## Global Settings (StarVizion Settings Tab)

Available in Elite-Intel's Settings tab, under the StarVizion section: Vizlet storage folder (default: the `AppPaths` application data folder), HoloFrame storage folder, grid-snapping toggle (editor alignment aid), alignment-guides toggle (smart guides during drag/resize), default node opacity, and an attribution name that appears as the `author` field in new Vizlet files.

---

## Mode-Based Visibility

Vizlets can be configured to show or hide automatically based on what the player is doing in Elite Dangerous. This is a Vizlet-level setting, configured on the [Vizlet Editor's Vizlet tab](#vizlet-tab) — not a per-NeuroNode setting.

### Detected Modes (v1)

| Mode | Detection Method |
|---|---|
| Ship — Normal Flight | Journal load event with no other mode active |
| Ship — FSS Scanner | Journal jump/FSS-mode-entry event |
| Ship — Detailed Surface Scanner | Journal surface-scan-complete context |
| SRV | Journal touchdown/SRV-launch events |
| On Foot | Journal disembark event |
| Docked / Station | Journal docked event |
| Supercruise | Journal supercruise-entry event |

Mode detection is best-effort. If journal parsing fails or the mode is ambiguous, Vizlets fall back to their manual state (whatever the user last set).

### Per-Vizlet Mode Configuration

Each Vizlet has a mode-visibility setting: always visible (default, independent of detected mode), visible in a user-selected list of modes, or hidden in a user-selected list of modes.

Mode transitions are debounced (a brief delay before changing visibility) to prevent visual flicker during rapid journal events.

### Hiding Never Closes a Window

Whenever a Vizlet becomes hidden — from its own Mode-Based Visibility setting, or from a [HoloFrame](holoframes-and-persistence.md#mode-based-holoframe-switching) deactivating around it — its window stays open the whole time; only its visibility changes. It is never closed and later reopened as part of this. This matters specifically for Desktop + VR: destroying and recreating the VR overlay handle on every mode change would force it to reset its position/connection each time, which reads as visible flicker in the headset during ordinary play. This is distinct from the player explicitly right-clicking a Vizlet and choosing **Close** — that's a deliberate action that really does remove the window, unaffected by any of this.

### Manual Override

The user can always manually toggle any Vizlet on or off regardless of the detected mode. The manual state is preserved until the user changes it again or until a HoloFrame activation overrides it.
