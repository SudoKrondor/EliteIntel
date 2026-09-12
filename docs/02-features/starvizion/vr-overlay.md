# StarVizion — VR Overlay

StarVizion supports rendering a Vizlet to the desktop, to VR, to both at once, or to the desktop with an external tool handling VR placement. All four are v1 requirements. For the first three, the StellarCore design had StarVizion owning both output paths directly — the desktop overlay window and the VR compositor integration — because the host provided no renderer.

**That premise no longer holds.** Elite-Intel already ships a native overlay: a C program under `overlay/` with `platform_win32.c`, `platform_x11.c` and **`platform_openvr.c`**, spawned as a child process and driven over a documented tab-separated stdin protocol (`overlay/PROTOCOL.md`, `ui.overlay.NativeHudOverlay`). There is also a Swing precedent in `ui.inputmonitor.overlay.ReadoutWindow`.

So whether StarVizion renders through the existing native overlay, through Swing windows, or both is now an **open architectural decision** rather than a settled one — and it is the largest single decision StarVizion faces. See [Host UI](../../01-host-integration/elite-intel-platform-map.md#host-ui).

**Why a direct VR compositor integration, rather than capturing an existing desktop overlay window into VR, for the first three modes:** a desktop-window-capture approach is workable on Windows but has no equivalent that works the same way on Linux, which would break platform parity for a Windows+Linux product. The VR industry-standard overlay API that StarVizion targets is itself genuinely cross-platform, so a direct integration gives identical behavior on both platforms once built.

**Why a fourth, hybrid mode exists anyway:** Desktop+ is a real, existing tool some VR users already run to grab a desktop window and pin/position it precisely in VR space. Relying on it for VR placement — rather than StarVizion's own direct compositor integration — means StarVizion only ever has to render a plain desktop window for that Vizlet, with no VR-side code of its own involved at all. This is confirmed useful enough to support directly rather than leaving to chance, precedented by Elite Intel (this project's own predecessor application — see [Vision — History](../../00-overview/vision.md)), which already shipped exactly this four-mode overlay model successfully. It's a genuine hybrid, not a replacement: the three direct-integration modes remain the only cross-platform option and stay as designed above; **Desktop+ mode is Windows-only**, since the Desktop+ tool itself is, and exists purely as an added convenience for players already using it.

## Rendering Modes

| Mode | Output Target | Platform |
|---|---|---|
| Desktop | A transparent, frameless, always-on-top window on the desktop | Windows + Linux |
| VR | An overlay texture submitted directly to the VR compositor | Windows + Linux |
| Desktop + VR | Both of the above, simultaneously, from the same Vizlet | Windows + Linux |
| Desktop+ | A plain desktop window only; VR placement is handled entirely by the external Desktop+ tool, outside StarVizion | Windows only |

A Vizlet's render mode is a per-Vizlet setting, configured on the [Vizlet Editor's Vizlet tab](ui-layout.md#vizlet-tab) under Render Mode. Different Vizlets can independently use any of the four modes — a Desktop-only Vizlet, a VR-only Vizlet, a Desktop + VR Vizlet, and a Desktop+ Vizlet can all be active at once. A [HoloFrame can optionally override Render Mode for all its members at once](holoframes-and-persistence.md#render-mode-override) — whole-group, never per-member, the same precedence pattern already used for HoloFrame visibility.

**Desktop + VR is not two separate configurations layered together** — it is one Vizlet with both a desktop window (position, size, always-on-top, transparency) and a VR world-space transform (position, width, world/HMD-locked) active and persisted at the same time. Both sets of properties are shown and editable together on the Vizlet tab whenever this mode is selected, rather than the Window and VR fields being mutually exclusive the way they are for the single-mode options.

## Rendering Pipeline

All Vizlets, regardless of output mode, use the same rendering pipeline:

1. StarCalc evaluates all node expressions for the current frame.
2. Each NeuroNode's resolved visual output is rendered to a shared render target.
3. The composed output is sent to whichever destination(s) the Vizlet's render mode specifies — presented via the transparent overlay window for Desktop, submitted to the VR compositor for VR, or both in the same frame for Desktop + VR.

The draw calls are identical regardless of target; only the final output submission differs. This means a Vizlet that works correctly in desktop mode also works correctly in VR mode — and in both at once — with no additional authoring required. Rendering to both destinations simultaneously costs a second submission call per frame, not a second render pass; the expensive part (evaluating expressions and composing the frame) happens once regardless of how many destinations receive it.

## VR Compositor Integration

StarVizion talks to the VR compositor directly — there is no intermediary shell service. Per active Vizlet with VR as part of its render mode (VR, or Desktop + VR), StarVizion:

- Creates an overlay handle for the Vizlet
- Submits the rendered texture to that handle every frame
- Sets the overlay's transform (world-locked or HMD-locked, per a per-Vizlet setting)
- Reads back the overlay's current transform, to detect when the user has manually repositioned it (see [VR Overlay Positioning](#vr-overlay-positioning-and-persistence))
- Destroys the overlay handle when the Vizlet deactivates

**Requirement:** the VR runtime must be active for the VR side of rendering to work. StarVizion checks whether the VR runtime is running and an HMD is connected before activating the VR side. A Vizlet set to VR-only gracefully falls back to desktop-only rendering if the check fails, and sets a warning badge on the nav button. A Vizlet set to Desktop + VR is unaffected on its desktop side when the check fails — it simply runs desktop-only until the VR runtime becomes available, still with the same warning badge, rather than losing its output entirely the way a VR-only Vizlet's fallback does.

## VR Overlay Positioning and Persistence

Each Vizlet in VR mode has a world-space transform — position in meters relative to the HMD or world origin, and a width in meters (aspect ratio is preserved from the Vizlet's pixel dimensions). This is stored alongside the desktop window properties in the Vizlet's saved data.

**Activation restores the saved position automatically.** When a Vizlet activates in VR mode, StarVizion creates the overlay and immediately applies the transform already saved in that Vizlet's file — no per-session setup required.

**Manual repositioning is captured back into the file.** The user can grab and move any overlay using the VR runtime's own standard overlay manipulation controls — the same interaction available for any VR overlay from any application. StarVizion detects when this happens and writes the updated transform back to the Vizlet's file, so a manual adjustment persists the same way the original placement does. Without this, every manual reposition would be lost the next time the Vizlet activates.

HMD-locked positioning — an overlay that follows the headset like a HUD element, rather than staying fixed in world space — is also supported as a per-Vizlet setting.
