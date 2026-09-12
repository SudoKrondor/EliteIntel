# Elite-Intel Platform Map

**Replaces:** the entire `01-core-platform/` section of the EDO StellarCore documentation
(`platform-overview.md`, `services.md`, `plugin-system.md`, `shell-ui.md`, `lifecycle.md`,
`out-of-process-plugin-contract.md`, `application-folder-structure.md`).

Those documents specified a host shell that had to be built. Elite-Intel is a host that already exists. This
document maps every service and platform concept the BindForge and StarVizion specs depend on onto what
Elite-Intel actually provides today — with the gaps called out honestly rather than assumed away.

Every cross-reference in the ported feature specs that used to point into `01-core-platform/` now points at a
section of this document.

> **Verified against:** `SudoKrondor/EliteIntel` @ `8a4f535` (`v-1.1.0009`, master).
> Package paths and class names below were read from that tree, not inferred.

---

## Service Map

| StellarCore service | Elite-Intel equivalent | State |
|---|---|---|
| Device Service | `elite.intel.devices.DeviceService` + `eventbus.DeviceBus` | **Exists, complete** |
| File Service | `java.nio` directly, plus `ai.hands.Bindings*Service` classes | **Partial** — no central broker |
| Path Service | `elite.intel.util.AppPaths` | **Exists, narrower** |
| Settings Service | `db.dao.GlobalSettingsDao` / `GlobalSettingsManager` (SQLite) | **Exists, different shape** |
| Theme Service | `ui.theme.AppTheme`, `HudPalette`, `HudGlyphs`, `HudForms` | **Exists, not a service** |
| Logging Service | SLF4J/logback as used throughout the app | **Exists** |
| Network Service | per-caller HTTP (EDSM / Spansh integrations) | **Exists, unbrokered** |
| Game Detection Service | — | **Absent — and no longer needed** |

---

## In-Process, Not Out-of-Process

The single most consequential difference.

StellarCore specified isolated plugins that could not touch the filesystem, could not see each other, and
talked to the shell only through declared service interfaces — an arrangement designed to let strangers ship
plugins safely, and the reason `out-of-process-plugin-contract.md` existed at all.

**None of that applies here.** BindForge and StarVizion become packages inside the Elite-Intel application,
compiled into the same JAR, running on the same JVM, free to call any class in the codebase and publish on any
of the six event buses in `elite.intel.eventbus`.

What this deletes outright:

- The plugin manifest, discovery, query, and staged-load protocol.
- The allowed-domains network sandbox.
- The prohibition on direct filesystem access.
- Plugin-to-plugin isolation — and therefore the entire obstacle
  [telemetry.md](../03-data-models/telemetry.md) was originally written to work around.

What this costs: the isolation was also a *discipline*. Nothing now structurally prevents BindForge from
reaching into StarVizion's internals, or from writing a file without going through a backup path. That
discipline has to be maintained by convention and code review instead of by architecture — see
[Data Integrity Principle](#data-integrity-principle), which is now a rule rather than a guarantee.

Elite-Intel's contribution rules also forbid PRs that require users to set up external dependencies, that add
JNI to unsigned libraries unavailable on both Windows and Linux, or that read or modify in-game memory. Any
BindForge or StarVizion design that assumed a free hand with native code must be checked against those rules.

---

## Feature Registration

Elite-Intel has no manifest. A feature is a Swing tab registered in the UI layer alongside the existing ones —
`ui.screen.BindingsTabPanel`, `ActionsTabPanel`, `CommanderTabPanel`, `SettingsTabPanel`, and so on.

BindForge and StarVizion each become a top-level tab. There is no versioned self-description, no declared
service list, and no required-services negotiation. The nav-order and settings-tab-ordering rules in the
StellarCore specs have no equivalent and can be dropped.

---

## Feature Lifecycle

There is no staged plugin load (Stage 1/2/3), no unload, and no dirty-state gate blocking shutdown.
Tabs are constructed with the UI and live for the process lifetime.

**Consequence for the ported specs:** BindForge's *Unsaved Work on Exit* dialog (**Save / Discard**, settled
2026-09-06 — Apply is deliberately not offered there) does not get invoked by a host lifecycle hook,
because there is not one. It has to be wired to application shutdown — and to tab-switch away from
BindForge — by BindForge itself.

Two triggers, not one, and they are not equally trustworthy. A tab switch is an ordinary UI event
BindForge controls. Application shutdown is not: a crash, a kill, or a Windows session ending runs no
handler at all. That asymmetry is the practical argument for the draft being written eagerly rather than
held in memory until a dialog says so — the dialog is a courtesy on the paths where it can run, not the
mechanism that keeps work safe.

---

## Status Badges

StellarCore gave each plugin a nav badge (None / Running / Warning / Error) that it set on its own left-nav
button. Elite-Intel's tabbed UI has no such affordance today.

The ported specs rely on this in a few places — most importantly BindForge raising an **Error** badge on
destructive-change detection. Some visible equivalent is needed; a badge on the tab title is the obvious
candidate, but it does not exist yet and would be new UI work. **Flagged as an open item.**

---

## Device Input

**This is the strongest part of the port.** `elite.intel.devices` is, in substance, the Device Service the
StellarCore spec described — already built, and already in the tree.

From `elite.intel.devices.PACKAGE.md`:

- SDL3 (via LWJGL3) polled on a dedicated platform thread, `POLL_INTERVAL_MS = 16` (~60 Hz).
- Publishes on `DeviceBus`: `DeviceConnectedEvent`, `DeviceDisconnectedEvent`, `DeviceAxisEvent`,
  `DeviceButtonEvent`, `DeviceServiceStateEvent`, `DeviceDuplicateWarningEvent`.
- **Axis range is normalised to [−1.0, +1.0]** — exactly the range the StarVizion spec assumes.
- Delta-only publication: axes on value change, buttons on press/release transition only.
- `model/DeviceIdentity` resolves VID/PID and a `bindsHexId` that matches the `Device=` attribute in
  `.binds` axis XML — the correlation BindForge needs, already solved.
- `model/ButtonInputMapper` translates SDL3 indices to `.binds` tokens (`Joy_N`, `Joy_XAxis` … `Joy_RZAxis`).
- Duplicate VID/PID devices are detected and warned about, with `usbPath` available to tell two identical
  units apart — which is precisely the open question the StellarCore conflict notes raised.
- Explicitly read-only: the package never writes to the game or to `.binds`.

**Divergences the ported specs must absorb:**

1. **Push, not snapshot.** StellarCore specified axis values held in a current-state snapshot that StarVizion
   would read at render frame rate. `DeviceService` *pushes* delta events instead and keeps no queryable axis
   snapshot. StarVizion must maintain its own current-value cache fed from `DeviceAxisEvent`. This is a small
   piece of work, but the spec's wording ("it just reads the current value") is no longer literally true.
2. **Hat switches — hardware behaviour confirmed 2026-09-06.** A hat reports **one 4-bit value**, logical
   `0–7` for 0°–315°, plus a null state for centred. It is not a set of booleans, so the boolean-per-direction
   model is a translation the input layer has to perform rather than a reading it can take. Confirmed on two
   DualShock 4 controllers; `PACKAGE.md` still documents no hat handling, so **where that translation happens
   is still unverified in code.**
3. **Axis ceiling.** `axisToBindsToken` throws for index ≥ 6. Devices with more than six axes — plausible on a
   Virpil or VKB setup — have no `.binds` token mapping. Affects BindForge more than StarVizion. *A
   DualShock 4 reports exactly six, so the ceiling is reached by an ordinary gamepad, not only by exotic
   hardware.*
4. **Keyboard and mouse — corrected 2026-09-07; this entry was half wrong.** `DeviceService` covers
   joysticks, HOTAS, gamepads and pedals, and does not enumerate keyboard or mouse. But **BindForge's
   keyboard capture is already solved elsewhere in the tree**: `util.KeyCaptureMapper` turns a Swing
   `KeyEvent` straight into an Elite token, driven through a `KeyEventDispatcher` by
   `ui.dialog.AssignKeyboardBindingDialog`. On Windows it maps **PS/2 scan codes** rather than characters,
   so it captures physical key position and is layout-independent by construction — which is exactly what
   the [non-ASCII layout problem](../02-features/bindforge/domain-knowledge/EliteDangerous-BindsFileFormat.md#55-keyboard-codes)
   needs. Linux falls back to VK codes assuming QWERTY positions.

   **Mouse has no capture path, and does not need one** — see
   [Bind Editor](../02-features/bindforge/bind-editor.md#mouse-inputs-are-chosen-not-captured).

   **What remains genuinely absent is focus-independent capture for StarVizion.** BindForge's dialog has
   focus while it listens, so Swing-level events are enough. A HUD that shows keys pressed *while the game
   has focus* is a different problem, needs a platform hook, and is constrained by Elite-Intel's rules on
   native code. `ui.inputmonitor.overlay.KeyboardReadout` is the place to look first.

**Prior art already in the tree:** `elite.intel.ui.inputmonitor` — `InputMonitorTabPanel`,
`InputMonitorPalette`, `model/DeviceAxis`, `model/DeviceButton`, and an `overlay/` package containing
`ReadoutWindow`, `AxesReadout`, `ButtonReadout`, `CounterReadout`, `KeyboardReadout` and their settings
dialogs. This is a working, small-scale ancestor of StarVizion. It should be read closely before any
StarVizion implementation begins — it is the most direct evidence of what the host will comfortably support.

---

## File Access

Elite-Intel has **no central file broker**. There is no Controlled Replace pattern, no shared backup-then-write
service, no archive/restore service, and no Watch Path facility available generically.

What exists instead is BindForge-shaped already, in `elite.intel.ai.hands`:

| Class | Role |
|---|---|
| `BindingsLoader`, `KeyBindingsParser` | read and parse `.binds` |
| `BindingsWriter` | write `.binds` |
| `BindingsWorkingCopyRepository` | the **working copy** concept, already implemented |
| `BindingsBackupService` | timestamped backups |
| `BindingsApplyService`, `BindingsApplyException`, `BindingSaveResult` | apply-to-game with typed failure |
| `BindingsMonitor` | external-change detection |
| `BindingConflictScanner`, `BindingConflictRules` | conflict detection |
| `BindingSection`, `BindingDisplayNames` | in-game section, group and row label for a `.binds` tag |
| `db.dao.BindingConflictDao`, `db.managers.BindingConflictManager` | conflict persistence |

This is a substantial head start and a substantial constraint: BindForge is not being written on a blank page,
it is being grown out of an existing implementation whose choices it must either adopt or deliberately replace.
**The single most important next task is a detailed read of `elite.intel.ai.hands` against the ported
[BindForge specs](../02-features/bindforge/overview.md)**, to determine what is reusable, what conflicts, and
what must be rewritten.

**Gaps against the ported specs:** the Archive Backup (ZIP) and Restore Archive operations that BindForge's
File Manager is built on have no equivalent and must be written. `DeviceMappings.xml` and `.buttonMap` handling
appears absent entirely — BindForge's Device Identity and Button Map domains are greenfield.

---

## Game Detection

**Absent, and nothing needs it.**

A search of the tree for process detection (`ProcessHandle`, `tasklist`, `pgrep`) finds only
`WindowsNativeKeyInput` and `Updater` — neither detects Elite Dangerous running. Elite-Intel cannot tell you
whether the game is up.

This was the port's single blocking dependency, because BindForge's **Dormant Mode** hard-blocked writes for the
whole time the game was running. **Dormant Mode was removed in August 2026**, and with it the only consumer of
game-process detection. See
[BindForge — Writing While the Game Is Running](../02-features/bindforge/overview.md#writing-while-the-game-is-running).

Note that StarVizion's mode-based visibility is **unaffected**: it keys off journal *events*, which
`gameapi.JournalParser` already delivers, not off whether the game process exists.

If a future feature does need it, the options remain: infer from journal activity (cheap, cross-platform, but
stop-detection is a timeout inference), or poll `ProcessHandle.allProcesses()` for the known executable names
(more code, unambiguous both ways).

---

## Path Resolution

`elite.intel.util.AppPaths` already provides, under the OS application-data base:

```
elite-intel/db                       getDatabasePath()
elite-intel/bindings                 getBindingsWorkingDir()
elite-intel/bindings/backups         getBindingsBackupDir()
elite-intel/custom-commands          getCustomCommandsFilePath()
elite-intel/custom-commands/backups  getCustomCommandsBackupDir()
elite-intel/playerbackups            getPlayerBackupsDir()
elite-intel/diagnostics              getDiagnosticsDir()
```

Note that `getBindingsWorkingDir()` and `getBindingsBackupDir()` already exist — the working-copy and backup
locations BindForge's specs describe are provisioned.

**Gap:** `AppPaths` resolves Elite-Intel's *own* folders. It does not do game-installation discovery across
storefronts (Steam / Epic / Frontier / Oculus), which the StellarCore Path Service did and which BindForge
genuinely needs for the `DeviceMappings.xml` and `.buttonMap` domains that live inside the game install.
`gameapi.DataDirectoryValidator` is the nearest existing thing and should be examined first. The confirmed
real-world paths are preserved in
[domain-knowledge/EliteDangerous-InstallPaths.md](domain-knowledge/EliteDangerous-InstallPaths.md).

---

## Settings Storage

Settings live in SQLite via `db.dao.GlobalSettingsDao` / `db.managers.GlobalSettingsManager`, with
`ShipSettingsDao` for per-ship values — not per-plugin isolated files.

The StellarCore isolation guarantee ("a plugin's settings physically cannot collide with another's, since they
are different files in different folders") does not hold. BindForge and StarVizion settings live in the same
store as everything else and must be namespaced by key convention.

The Settings UI is `ui.screen.SettingsTabPanel` with panels under `ui.screen.settings/`
(`AiServicesSettingsPanel`, `AudioSettingsPanel`, …). BindForge and StarVizion settings become additional
panels there, following the existing pattern.

---

## Theming

There is no Theme Service and no theme-changed notification. `ui.theme` provides `AppTheme`, `HudPalette`,
`HudGlyphs`, and `HudForms` — a fixed visual language rather than a swappable runtime theme.

The specs' rule "plugins use theme values and never hardcode colours" still applies as good practice, and
`InputMonitorPalette` shows the established pattern for a feature-local palette. But live theme switching is
not a capability to design against.

**This directly affects the mockups**, which were drawn in StellarCore's own visual language. See
[PORTING-NOTES.md](../PORTING-NOTES.md).

---

## Host UI

Elite-Intel is a **Swing** application (`elite.intel.ui`, MVC per `DEVELOPERS.md`) using a **tabbed** layout —
not StellarCore's left-nav shell. Every ported reference to left-nav buttons, nav badges, the gear icon at the
bottom of the nav, or a plugin content area has been reworded or flagged.

StarVizion's Vizlets are unaffected: they are frameless always-on-top windows floating outside the host window,
which is orthogonal to how the host arranges itself.

**Relevant precedent for overlays:** Elite-Intel already ships a native overlay. The `overlay/` module is a C
program (`hud_render.c`, `hud_model.c`, `platform_win32.c`, `platform_x11.c`, `platform_openvr.c`) spawned as a
child process and driven over stdin by a tab-separated line protocol — see `overlay/PROTOCOL.md` and
`ui.overlay.NativeHudOverlay` / `OverlayProtocol`. **It includes an OpenVR backend**
(`overlay/third_party/openvr`), which means StarVizion's VR requirement has a real, existing, shipping
implementation path in this codebase rather than a green field. See
[StarVizion — VR Overlay](../02-features/starvizion/vr-overlay.md).

Whether StarVizion should render through that native overlay, through Swing windows like
`ui.inputmonitor.overlay.ReadoutWindow`, or both, is an open architectural decision — and the single biggest
one StarVizion faces in this port.

---

## Default Window Size — Needs Changing

**Raised 2026-09-06. Note only; no code change made.** The symptom is that content is clipped at the
bottom of panels — the Shortcuts panel on the VEGA tab cuts off the credit balance under the date, and
BindForge's pane would be shorter than its mockups assume.

### Measured, not estimated

| | |
|---|---|
| Set in code | `ui.screen.AppView` — `setSize(new Dimension(1200, 920))`, minimum `600 x 500` |
| Actual outer window | 1200 x 920 |
| Actual client area | 1184 x **911** |
| Developer's primary display | 1920 x 1080, **working area 1920 x 1032** (48px taskbar) |

**Only 9px of that is operating-system chrome.** Elite-Intel draws its own title bar, so the title bar,
the ELITE INTEL header and the tab row all come out of the 911. Reading them off a screenshot at that
size: roughly 44 + 48 + 50 ≈ **150px of chrome above the content**, leaving a usable pane around **760px**.

### The target, and why it is tighter than it looks

A usable pane of **850px** is wanted — enough to stop the clipping and to give BindForge's four sections
room. That needs roughly **1010px of window height**.

**A bare bump of the constant to 1010 breaks on the machine that reported the problem.** The window opens
at y=56; at 1010 tall its bottom lands at 1066, past the 1032px working area, so the bottom of the window
— including whatever was being clipped — would sit under the taskbar or off-screen entirely. 1080p is also
the most common gaming resolution, and Elite players frequently run the game on the same monitor.

### Two ways to get the room, and they are not exclusive

**1. Size against the working area rather than a constant.** Something to the effect of *"the smaller of
the desired height and the screen's usable height, less a margin,"* then centre it. That fixes the
off-screen risk on every display, and on a 1080p screen it yields about 1000px — essentially the whole
working area, which means the window is near full-height and must be positioned at the top to fit.
`Screen.WorkingArea` already excludes the taskbar, so this is arithmetic rather than guesswork.

**2. Take it out of the chrome instead.** 150px above the content is a lot for a title bar, a header and a
tab row, and every pixel trimmed there is a pixel the pane gains **without growing the window at all** —
which is the only version of this that helps someone already at full height on 1080p. Worth measuring the
three bands properly before deciding, rather than trusting the screenshot arithmetic above.

The honest summary: **on a 1080p display you cannot have both the current chrome and an 850px pane.**
Growing the window buys some of it; the rest has to come from the chrome.

### Knock-on: the mockups assume 940px

`EliteIntel_Shell_Mockup.html` sets `--view-h: 940px` for the pane under the tabs. The real figure today is
about 760, and about 850 if this is fixed as intended. **The mockups are being reviewed against roughly
180px of vertical room that does not exist**, so any BindForge screen that looks like it just fits does not.
Left as-is pending the decision above, since the right number depends on which of the two routes is taken.

---

## Logging

Standard SLF4J/logback as used throughout the codebase. Features log directly; there is no brokered submission
step. The StellarCore requirement that plugins never write log files themselves is satisfied trivially.

---

## Network Access

No brokered Network Service and no allowed-domains sandbox. Elite-Intel makes its own outbound calls (EDSM,
Spansh) from the code that needs them.

Neither BindForge nor StarVizion requires network access in v1 — both are local-file and local-device tools —
so this gap costs the port nothing.

---

## Configuration Store

The StellarCore "Configuration Store" concept maps onto the SQLite database under `elite.intel.db`. See
[Settings Storage](#settings-storage).

---

## Application Folder Structure

Superseded by [Path Resolution](#path-resolution). Elite-Intel's on-disk layout is whatever `AppPaths`
provides; there is no plugin-subfolder convention because there are no plugins.

---

## Data Integrity Principle

**Carried over, unchanged in intent, weakened in enforcement.**

The rule stands: no write may leave a corrupt, incomplete, or partially-written file. Every operation either
completes fully or rolls back cleanly, leaving existing files untouched and reporting the reason.

What changes is that StellarCore enforced this *structurally* — plugins could not write files, so routing
through Controlled Replace was unavoidable. Elite-Intel has no such chokepoint. BindForge can call
`java.nio.file.Files.write` directly, and nothing stops it.

The principle therefore has to be upheld by the code itself: backup-then-temp-then-atomic-replace, applied
without exception. There is one write path, not a BindForge one — `BindingsBackupService` and
`BindingsWriter` are already it, and both should be audited against this rule as they are grown, the writer
especially, since it is [widening to cover every device](../02-features/bindforge/overview.md#two-narrow-boundaries--one-stays-one-widens).

---

## Lightweight by Default

Partially applicable. Elite-Intel is a continuously-running voice assistant that already polls the journal and
runs STT/TTS — it is not a shell that idles until a plugin wakes it.

The useful residue of this principle: `DeviceService` should not be started unless something needs it, and
StarVizion should not keep Vizlet windows rendering when none are active. The original framing — one plugin's
cost never paid by a user of another — has no meaning in a single always-on application.
